/*
 * web: org.nrg.xnat.helpers.merge.MergeSessionsA
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.helpers.merge;

import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.dicom.mizer.exceptions.MizerException;
import org.nrg.framework.status.StatusProducer;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.bean.CatCatalogBean;
import org.nrg.xdat.model.XnatImagesessiondataI;
import org.nrg.xdat.model.XnatResourcecatalogI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xft.utils.FileUtils.OldFileHandlerI;
import org.nrg.xnat.utils.CatalogUtils;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class MergeSessionsA<A extends XnatImagesessiondataI> extends StatusProducer implements Callable<A> {
    public static final String CAT_ENTRY_MATCH = "Session already exists with the same resources.";
    public static final String HAS_FILES       = "Session already exists with matching files.";
    protected final File srcDIR, destDIR;
    protected final A src, dest;
    protected final String destRootPath, srcRootPath;
    protected final boolean allowSessionMerge, overwriteFiles;
    protected final SaveHandlerI<A> saver;
    @SuppressWarnings("unused")
    protected ArrayList<Callable<A>> befores    = new ArrayList<>();
    protected AnonymizerA            anonymizer = null;
    protected final Object     control;
    final           UserI      user;
    final           EventMetaI c;

    private static final Logger logger = LoggerFactory.getLogger(MergeSessionsA.class);

    public MergeSessionsA(Object control, final File srcDIR, final A src, final String srcRootPath, final File destDIR, final A existing, final String destRootPath, boolean allowSessionMerge, boolean overwriteFiles, SaveHandlerI<A> saver, final UserI u, final EventMetaI c) {
        super(control);
        this.control = control;
        this.srcDIR = srcDIR;
        this.allowSessionMerge = allowSessionMerge;
        this.overwriteFiles = overwriteFiles;
        this.dest = existing;
        this.src = src;
        this.destDIR = destDIR;
        this.srcRootPath = srcRootPath;
        this.destRootPath = destRootPath;
        this.saver = saver;
        this.user = u;
        this.c = c;
    }

    protected void setAnonymizer(AnonymizerA a) {
        this.anonymizer = a;
    }

    public interface SaveHandlerI<A> {
        void save(A session) throws Exception;
    }

    public void checkForConflict() throws ClientException, ServerException {
        ClientException t = null;
        if (destDIR.exists() || dest != null) {
            if (!allowSessionMerge) {
                failed(HAS_FILES);
                throw new ClientException(Status.CLIENT_ERROR_CONFLICT, HAS_FILES, new Exception());
            }

            if (!overwriteFiles && dest != null) {
                if ((new SessionOverwriteCheck(src, dest, src.getPrearchivepath(), dest.getPrearchivepath(), user, c)).call()) {
                    failed(CAT_ENTRY_MATCH);
                    t = new ClientException(Status.CLIENT_ERROR_CONFLICT, CAT_ENTRY_MATCH, new IOException());
                }
            }
        }

        if (destDIR.exists() && !overwriteFiles) {
            if (FileUtils.FindFirstMatch(srcDIR, destDIR, new FileFilter() {
                public boolean accept(File pathname) {
                    return !(pathname.getName().endsWith(".xml") || pathname.getName().endsWith(".log"));
                }
            }) != null) {
                failed(HAS_FILES);
                t = new ClientException(Status.CLIENT_ERROR_CONFLICT, HAS_FILES, new Exception());
            }
        }

        if (t != null) {
            throw t;
        }
    }

    public A call() throws ClientException, ServerException {
        processing("Preparing to move uploaded resources into destination directory.");
        File backupDIR  = null;
        this.checkForConflict();
        final File rootBackup = createPrimaryBackupDirectory(this.getCacheBKDirName(), src.getProject(), destDIR.getName());
        if (destDIR.exists()) {
            backupDIR = backupDestDIR(destDIR, rootBackup);
        }

        try {
            final XnatImagesessiondataI session = getPostAnonSession();

            if (dest != null) {
                if (dest instanceof XnatImagesessiondata) {
                    //ugly hack
                    //this is a work around for a bug in XFT's SAX XML writer
                    //it looks like the XML write is invalidating the cached list of scans stored within the Imagesessiondata object.
                    //so the next thing that tries to access them (the merge) doesn't find any,
                    //this was exposed because we started accessing the scans before this point (checkForConflict) which populated the cached list of scans in the session object, which was then invalidated by the sax write.
                    //when we didn't access the scans prior to this line, then none of this was necessary
                    //as a temporary workaround, we'll generate the xml off of a copy of the session.
                    //a more permanent solution will probably be Hibernate related.
                    @SuppressWarnings("unchecked") A full_copy = (A) BaseElement.GetGeneratedItem((((XnatImagesessiondata) dest).getCurrentDBVersion()));
                    backupXML(full_copy, rootBackup);
                } else {
                    backupXML(dest, rootBackup);
                }
            }

            //merge session xmls... nothing is modified until after file system is merged.
            @SuppressWarnings("unchecked")
            final Results<A> update = mergeSessions((A) session, srcRootPath, dest, destRootPath, rootBackup);

            A merged = update.getResult();

            //If we wrote to the src directory's catalogs, would the overwrite persist them into the new space (overwriting the old ones).
            //What if the same catalog had two different catalog file names.  This would cause duplicate catalogs.
            //Could merge the catalogs based on label, delete the old one, write the new one to the src space, then copy it all in.
            //DONE
            for (Callable<Boolean> followup : update.getBeforeDirMerge()) {
                try {
                    followup.call();
                } catch (Exception e) {
                    logger.error("", e);
                }
            }

            mergeDirectories(srcDIR, destDIR, overwriteFiles);

            finalize(merged);

            processing("Updating stored metadata.");
            saver.save(merged);

            for (Callable<Boolean> followup : update.getAfter()) {
                try {
                    followup.call();
                } catch (Exception e) {
                    logger.error("", e);
                }
            }

            postSave(merged);

            return merged;
        } catch (MizerException e) {
            logger.error("An error occurred when anonymizing the data. This occurred prior to moving files, so no rollback is performed.", e);
            failed("An error occurred when anonymizing the data: " + e.getMessage());
            throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
        } catch (Throwable e) {
            logger.error("An error occurred updating existing metadata", e);
            if (backupDIR != null) {
                rollback(backupDIR, destDIR, rootBackup);
            } else {
                rollback(destDIR, srcDIR, rootBackup);
            }
            failed("Error updating existing metadata");
            throw new ServerException(Status.SERVER_ERROR_INTERNAL, e);
        }
    }

    public void postSave(A session) {

    }

    protected A getPostAnonSession() throws Exception {
        // Anonymize the data.
        anonymizer.call();

        // Return the default session XML.
        return src;
    }

    private void backupXML(A dest2, File rootBackup) throws ServerException {
        File backup = new File(rootBackup, "dest.xml");
        try {
            processing("Backing up destination XML Specification document.");
            FileWriter fw = new FileWriter(backup);
            dest2.toXML(fw);
            fw.close();
        } catch (Exception e) {
            failed("Failed to update XML Specification document.");
            throw new ServerException(e.getMessage(), e);
        }
    }

    private File backupDestDIR(File destDIR2, File rootBackup) throws ServerException {
        File backup = new File(rootBackup, "dest_backup");
        backup.mkdirs();

        processing("Backing up destination directory");
        try {
            FileUtils.CopyDir(destDIR2, backup, false);
        } catch (Exception e) {
            this.failed("Failed to backup destination directory");
            throw new ServerException(e.getMessage(), e);
        }

        return backup;
    }

    @SuppressWarnings("unused")
    private File backupSourceDIR(File sourceDIR2, File rootBackup) throws ServerException {
        File backup = new File(rootBackup, "src_backup");
        backup.mkdirs();

        this.processing("Backing up source directory");
        try {
            FileUtils.CopyDir(sourceDIR2, backup, false);
        } catch (Exception e) {
            this.failed("Failed to backup source directory");
            throw new ServerException(e.getMessage(), e);
        }

        return backup;
    }

    private File createPrimaryBackupDirectory(String cacheBKDirName,
                                              String project, String folderName) {
        File f = org.nrg.xnat.utils.FileUtils.buildCachepath(project, cacheBKDirName, folderName);
        f.mkdirs();
        return f;
    }

    private void rollback(File backupDIR, File destDIR2, File rootBackup) throws ServerException {
        File backup = new File(rootBackup, "modified_dest");
        backup.mkdirs();

        this.processing("Restoring previous version of destination directory.");
        try {
            FileUtils.MoveDir(destDIR2, backup, true);
        } catch (IOException e) {
            logger.error("", e);
        }

        try {
            FileUtils.MoveDir(backupDIR, destDIR2, allowSessionMerge);
        } catch (Exception e) {
            this.failed("Failed to restore previous version of destination directory.");
            throw new ServerException(e.getMessage(), e);
        }
    }

    /**
     * Fix scans
     *
     * @param session The session to be finalized.
     *
     * @throws ClientException When something goes wrong on the client side.
     * @throws ServerException When something goes wrong on the server side.
     */
    public abstract void finalize(A session) throws ClientException, ServerException;

    public abstract String getCacheBKDirName();

    public abstract Results<A> mergeSessions(final A src, final String srcRootPath, final A dest, final String destRootPath, final File rootbackup) throws ClientException, ServerException;

    public MergeSessionsA.Results<File> mergeCatalogs(final String srcRootPath, final XnatResourcecatalogI srcRes, final String destRootPath, final XnatResourcecatalogI destRes) throws Exception {
        final CatCatalogBean srcCat = CatalogUtils.getCleanCatalog(srcRootPath, srcRes, false, user, c);

        //WARNING: this command will create a catalog if it doesn't already exist
        final CatCatalogBean cat = CatalogUtils.getCleanCatalog(destRootPath, destRes, false, user, c);

        MergeCatCatalog merge = new MergeCatCatalog(srcCat, cat, allowSessionMerge, c, CatalogUtils.getCatalogFile(destRootPath, destRes));

        MergeSessionsA.Results<Boolean> r = merge.call();
        if (r.result != null && r.result) {
            try {
                //write merged destination file to src directory for merge process to move
                CatalogUtils.writeCatalogToFile(cat, CatalogUtils.getCatalogFile(srcRootPath, srcRes));

                return new MergeSessionsA.Results<>(CatalogUtils.getCatalogFile(destRootPath, destRes), r);
            } catch (Exception e) {
                failed("Failed to update XML Specification document.");
                throw new ServerException(e.getMessage(), e);
            }
        }

        return null;
    }

    public static class Results<A> {
        A result;
        final List<Callable<Boolean>> after          = new ArrayList<>();
        final List<Callable<Boolean>> beforeDirMerge = new ArrayList<>();

        public Results() {
        }

        public Results(A s) {
            result = s;
        }

        public Results(A s, Results r) {
            result = s;
            this.addAll(r);
        }

        public Results<A> setResult(A s) {
            result = s;
            return this;
        }

        public A getResult() {
            return result;
        }

        public List<Callable<Boolean>> getAfter() {
            return after;
        }

        public List<Callable<Boolean>> getBeforeDirMerge() {
            return beforeDirMerge;
        }

        @SuppressWarnings("unchecked")
        public Results<A> addAll(Results r) {
            this.after.addAll(r.getAfter());
            this.beforeDirMerge.addAll(r.getBeforeDirMerge());
            return this;
        }
    }

    public void mergeDirectories(File srcDIR2, File destDIR2, boolean overwrite) throws ClientException, ServerException {
        try {
            FileUtils.MoveDir(srcDIR2, destDIR2, overwrite, new FileFilter() {
                public boolean accept(File pathname) {
                    return (!pathname.getName().endsWith(".log"));
                }
            }, new OldFileHandlerI() {
                @Override
                public boolean handle(final File f) {
                    if (CatalogUtils.maintainFileHistory()) {
                        try {
                            FileUtils.MoveToHistory(f, EventUtils.getTimestamp(c));
                        } catch (FileNotFoundException e) {
                            logger.error("Couldn't find the file " + f.getPath(), e);
                            return false;
                        } catch (IOException e) {
                            logger.error("An unknown error occurred trying to access the file " + f.getPath(), e);
                            return false;
                        }
                    }
                    return true;
                }
            });

            if (!FileUtils.HasFiles(srcDIR2.getParentFile())) {
                FileUtils.DeleteFile(srcDIR2.getParentFile());
            }
        } catch (IOException e) {
            failed("Unable to merge uploaded data into destination directory.");
            throw new ServerException(e.getMessage(), e);
        }
    }
}
