/*
 * web: org.nrg.xnat.helpers.uri.archive.ProjSubjSessionURIA
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.helpers.uri.archive;

import com.google.common.collect.Lists;
import org.nrg.xdat.model.XnatAbstractresourceI;
import org.nrg.xdat.model.XnatImageassessordataI;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.model.XnatReconstructedimagedataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.helpers.uri.URIManager.ArchiveItemURI;
import org.nrg.xnat.helpers.uri.archive.impl.ProjSubjURI;

import java.util.List;
import java.util.Map;

@SuppressWarnings("Duplicates")
public abstract class ProjSubjSessionURIA extends ProjSubjURI implements ArchiveItemURI {
    public ProjSubjSessionURIA(Map<String, Object> props, String uri) {
        super(props, uri);
    }

    @Override
    public List<XnatAbstractresourceI> getResources(boolean includeAll) {
        List<XnatAbstractresourceI> res  = Lists.newArrayList();
        final XnatImagesessiondata  expt = getSession();
        res.addAll(expt.getResources_resource());

        if (includeAll) {
            for (XnatImagescandataI scan : expt.getScans_scan()) {
                res.addAll(scan.getFile());
            }
            for (XnatReconstructedimagedataI scan : expt.getReconstructions_reconstructedimage()) {
                res.addAll(scan.getOut_file());
            }
            for (XnatImageassessordataI scan : expt.getAssessors_assessor()) {
                res.addAll(scan.getOut_file());
            }
        }

        return res;
    }

    public XnatImagesessiondata getSession() {
        populateSession();
        return assessed;
    }

    protected void populateSession() {
        populateSubject();

        if (assessed == null) {
            final XnatProjectdata proj = getProject();

            final String exptID = (String) props.get(URIManager.ASSESSED_ID);

            if (proj != null) {
                assessed = (XnatImagesessiondata) XnatImagesessiondata.GetExptByProjectIdentifier(proj.getId(), exptID, null, false);
            }

            if (assessed == null) {
                assessed = (XnatImagesessiondata) XnatImagesessiondata.getXnatExperimentdatasById(exptID, null, false);
                if (assessed != null && (proj != null && !assessed.hasProject(proj.getId()))) {
                    assessed = null;
                }
            }
        }
    }

    private XnatImagesessiondata assessed = null;
}
