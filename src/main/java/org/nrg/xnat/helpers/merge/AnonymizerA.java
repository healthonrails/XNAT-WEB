/*
 * web: org.nrg.xnat.helpers.merge.AnonymizerA
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.helpers.merge;

import org.nrg.config.entities.Configuration;
import org.nrg.dicom.mizer.exceptions.MizerException;
import org.nrg.dicom.mizer.service.MizerService;
import org.nrg.dicom.mizer.service.impl.MizerContextWithScript;
import org.nrg.dicomtools.exceptions.AttributeException;
import org.nrg.xdat.XDAT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

public abstract class AnonymizerA implements Callable<Boolean> {
    abstract String getSubject();

    abstract String getLabel();

    /**
     * Get the appropriate edit script.
     *
     * @return The configuration containing the anon script.
     */
    abstract Configuration getScript();

    /**
     * Check if editing is enabled.
     *
     * @return Returns true if anonymization is enabled.
     */
    abstract boolean isEnabled();

    /**
     * Sometimes the session passed in isn't associated with a project,
     * for instance if the session is in the prearchive so
     * subclasses must specify how to get the project name.
     *
     * @return The name of the associated project (if any).
     */
    abstract String getProjectName();

    /**
     * Get the list of files that need to be anonymized.
     *
     * @return The list of files to be anonymized.
     *
     * @throws IOException When an error occurs accessing the files.
     */
    abstract List<File> getFilesToAnonymize() throws IOException;

    private void anonymize(List<File> files, String projectName, String subject, String label, long id, String script, boolean record) throws MizerException {
        if( script != null) {
            if( isEnabled()) {

                final MizerService service = XDAT.getContextService().getBeanSafely(MizerService.class);
                service.anonymize( files, getProjectName(), getSubject(), getLabel(), id, script, record);

            }
            else {
                // anonymization is disabled.
                if (_log.isDebugEnabled()) {
                    _log.debug("Anonymization is disabled for the script {}, nothing to do.", script.toString());
                }
            }
        } else {
            // this project does not have an anon script
            _log.debug("No anon script found for project {}, nothing to do.", getProjectName());
        }

    }

    @Override
    public Boolean call() throws Exception {
        if (getScript() == null) {
            _log.debug("No anon script found for current configuration, nothing to do.");
            return false;
        }
        if (!isEnabled()) {
            _log.debug("Anonymization is not enabled in the current configuration, nothing to do.");
            return false;
        }
        final List<File> files = getFilesToAnonymize();
        if (files.size() == 0) {
            _log.debug("Found no files to be anonymized.");
            return false;
        }
        _log.debug("Found {} files to be anonymized.", files.size());
        Configuration script = getScript();
        anonymize( files, getProjectName(), getSubject(), getLabel(), script.getId(), script.getContents(), true);
        return true;
    }

    private static final Logger _log = LoggerFactory.getLogger(AnonymizerA.class);
}
