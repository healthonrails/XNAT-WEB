/*
 * org.nrg.xnat.restlet.XnatRestletExtensionList
 *
 * Copyright (c) 2016, Washington University School of Medicine
 * All Rights Reserved
 *
 * XNAT is an open-source project of the Neuroinformatics Research Group.
 * Released under the Simplified BSD.
 *
 * Last modified 1/19/16 3:49 PM
 */

package org.nrg.xnat.restlet;

import java.util.HashSet;
import java.util.Set;

public class XnatRestletExtensions extends HashSet<String> {
    public XnatRestletExtensions(Set<String> packages) {
       super();
       this.setPackages(packages);
    }
    public void setPackages(Set<String> packages) {
        clear();
        addAll(packages);
    }
}
