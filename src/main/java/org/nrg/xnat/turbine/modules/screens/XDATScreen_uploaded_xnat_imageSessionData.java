/*
 * web: org.nrg.xnat.turbine.modules.screens.XDATScreen_uploaded_xnat_imageSessionData
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.turbine.modules.screens;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.base.BaseElement;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.om.base.BaseXnatSubjectdata;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xnat.turbine.utils.XNATUtils;
import org.nrg.xnat.utils.PetTracerListUtils;

@Slf4j
@SuppressWarnings("unused")
public final class XDATScreen_uploaded_xnat_imageSessionData extends EditScreenA {
    /**
     * {@inheritDoc}
     */
    @Override
    public String getElementName() {
        return XnatImagesessiondata.SCHEMA_ELEMENT_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void finalProcessing(final RunData data, final Context context) {
        if (!hasEditItem()) {
            log.error("finalProcessing invoked with null item");
            data.setMessage("Unable to load session item");
            data.setScreenTemplate(PREARC_PAGE);
            return;
        }

        final String dataType = getEditItem().getXSIType();
        if (null == dataType) {
            log.error("item has null data type");
            data.setMessage("session item has null data type");
            data.setScreenTemplate(PREARC_PAGE);
            return;
        }

        context.put("datatype", dataType);
        context.put("protocols", XNATUtils.getProjectsForCreate(dataType, data));
        //context.put("pages", pages.get(dataType));
        //context.put("scanFields", scanFields.get(dataType));

        XnatImagesessiondata session = (XnatImagesessiondata) BaseElement.GetGeneratedItem(getEditItem());
        context.put("notes", session.getNote());
        final ItemI part = TurbineUtils.GetParticipantItem(data);
        if (part != null) {
            final XnatSubjectdata xsd = (part instanceof XnatSubjectdata) ? (XnatSubjectdata)part : new XnatSubjectdata(part);
            session.setSubjectId(xsd.getId());
            context.put("part", xsd);
        } else {
            context.put("part",session.getSubjectData());
        }         

        String tag;
        if(data.getParameters().containsKey("tag")){
            tag = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("tag",data));
        }else{
            tag = getStringIdentifierForPassedItem(data);
            data.getParameters().add("tag", tag);
        }
        data.getSession().setAttribute(tag, session);
        context.put("tag", tag);
        

        
        if(TurbineUtils.HasPassedParameter("src", data)){
        	context.put("src", TurbineUtils.GetPassedParameter("src", data));
        }
        
		//review label
		if(session.getLabel()==null){
			if(session.getId()!=null){
				session.setLabel(session.getId());
			}else if(session.getDcmpatientid()!=null){
				session.setLabel(session.getDcmpatientid());
	        }
		}
		
		// XNAT-2916 - Set the subject label for the session if one exists in the system.
		if(!StringUtils.isEmpty(session.getSubjectId()) && !StringUtils.isEmpty(session.getProject())){
			BaseXnatSubjectdata subj = BaseXnatSubjectdata.GetSubjectByProjectIdentifierCaseInsensitive(session.getProject(), session.getSubjectId(), null, false);
			if(null != subj){
				if(!StringUtils.equals(session.getSubjectId(),subj.getId()) && !StringUtils.equals(session.getSubjectId(),subj.getLabel())){
					session.setSubjectId(subj.getLabel());
				}
			}
		}
	            
		context.put("edit_screen", "XDATScreen_uploaded_xnat_imageSessionData.vm");
        context.put("petTracerList", PetTracerListUtils.getPetTracerList(session.getProject()));
    }

    private final static String PREARC_PAGE = "XDATScreen_prearchives.vm";
}
