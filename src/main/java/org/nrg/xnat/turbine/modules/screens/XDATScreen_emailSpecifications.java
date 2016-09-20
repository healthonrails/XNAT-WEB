/*
 * web: org.nrg.xnat.turbine.modules.screens.XDATScreen_emailSpecifications
 * XNAT http://www.xnat.org
 * Copyright (c) 2016, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.om.ArcArchivespecification;
import org.nrg.xdat.turbine.modules.screens.AdminScreen;
import org.nrg.xnat.turbine.utils.ArcSpecManager;

public class XDATScreen_emailSpecifications extends AdminScreen {

    /* (non-Javadoc)
     * @see org.apache.turbine.modules.screens.VelocitySecureScreen#doBuildTemplate(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
     */
    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        ArcArchivespecification arcSpec = ArcSpecManager.GetInstance();
        context.put("arc", arcSpec);
    }

}
