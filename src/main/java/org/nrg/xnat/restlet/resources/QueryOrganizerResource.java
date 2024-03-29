/*
 * web: org.nrg.xnat.restlet.resources.QueryOrganizerResource
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.restlet.resources;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xft.TypeConverter.JavaMapping;
import org.nrg.xft.TypeConverter.TypeConverter;
import org.nrg.xft.XFTTable;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperField;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import org.nrg.xft.utils.DateUtils;
import org.nrg.xft.utils.XftStringUtils;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
public abstract class QueryOrganizerResource extends SecureResource {
	public QueryOrganizerResource(Context context, Request _request, Response _response) {
		super(context, _request, _response);
	}

	public CriteriaCollection processStringQuery(String xmlPath, String values){
		ArrayList<String> al=XftStringUtils.CommaDelimitedStringToArrayList(values);
		CriteriaCollection cc= new CriteriaCollection("OR");
		for(String value:al){
			if(value.contains("%") || value.contains("*")){
				value= StringUtils.replace(value, "*", "%");
				cc.addClause(xmlPath, "LIKE", value);
			}else{
				cc.addClause(xmlPath, value);
			}
		}
		return cc;
	}
	
	public CriteriaCollection processDateQuery(String column, String dates){
		ArrayList<String> al=XftStringUtils.CommaDelimitedStringToArrayList(dates);
		CriteriaCollection cc= new CriteriaCollection("OR");
		for(String date:al){
			if(date.contains("-")){
				String date1;
				try {
					date1=DateUtils.parseDate(date.substring(0,date.indexOf("-"))).toString();
				} catch (ParseException e) {
					date1=date.substring(0,date.indexOf("-"));
				}
	
				String date2;
				try {
					date2=DateUtils.parseDate(date.substring(date.indexOf("-")+1)).toString();
				} catch (ParseException e) {
					date2=date.substring(date.indexOf("-")+1);
				}

				CriteriaCollection subCC = new CriteriaCollection("AND");
				subCC.addClause(column, ">=", date1);
				subCC.addClause(column, "<=", date2);
				cc.add(subCC);
			}else{
				String date1;
				try {
					date1=DateUtils.parseDate(date).toString();
				} catch (ParseException e) {
					date1=date;
				}
				cc.addClause(column, date1);
			}
		}
		return cc;
	}
	
	public CriteriaCollection processNumericQuery(String column, String values){
		ArrayList<String> al=XftStringUtils.CommaDelimitedStringToArrayList(values);
		CriteriaCollection cc= new CriteriaCollection("OR");
		for(String date:al){
			if(date.contains("-")){
				String date1=date.substring(0,date.indexOf("-"));
	
				String date2=date.substring(date.indexOf("-")+1);
				CriteriaCollection subCC = new CriteriaCollection("AND");
				subCC.addClause(column, ">=", date1);
				subCC.addClause(column, "<=", date2);
				cc.add(subCC);
			}else{
				cc.addClause(column, date);
			}
		}
		return cc;
	}
	
	public CriteriaCollection processBooleanQuery(String column, String values){
		ArrayList<String> al=XftStringUtils.CommaDelimitedStringToArrayList(values);
		CriteriaCollection cc= new CriteriaCollection("OR");
		for(String value:al){
			cc.addClause(column, value);
		}
		return cc;
	}
	
	public CriteriaCollection processQueryCriteria(String xPath, String values){
		CriteriaCollection cc= new CriteriaCollection("OR");
		try {
			GenericWrapperField gwf =GenericWrapperElement.GetFieldForXMLPath(xPath);
            assert gwf != null;
            String type=gwf.getType(new TypeConverter(new JavaMapping("")));

            switch (type) {
                case STRING:
                    cc.add(this.processStringQuery(xPath, values));
                    break;
                case DOUBLE:
                    cc.add(this.processNumericQuery(xPath, values));
                    break;
                case INTEGER:
                    cc.add(this.processNumericQuery(xPath, values));
                    break;
                case DATE:
                    cc.add(this.processDateQuery(xPath, values));
                    break;
                case BOOL:
                    cc.add(this.processBooleanQuery(xPath, values));
                    break;
            }
		} catch (XFTInitException e) {
			log.error("An error occurred during XFT initialization",e);
		} catch (ElementNotFoundException e) {
			log.error("Couldn't find an element in the xPath {}", xPath,e);
		} catch (FieldNotFoundException e) {
			log.error("Couldn't find the field specified by the xPath {}", xPath,e);
		}
		return cc;
	}
	
	public abstract ArrayList<String> getDefaultFields(GenericWrapperElement e);
	
	public ArrayList<String> columns=null;
	
	public void populateQuery(QueryOrganizer qo){
		final String queryVariable = getQueryVariable("columns");
		final GenericWrapperElement rootElement = qo.getRootElement();
		if(StringUtils.isNotBlank(queryVariable) && !queryVariable.equals("DEFAULT")){
			try {
				this.columns =XftStringUtils.CommaDelimitedStringToArrayList(URLDecoder.decode(queryVariable, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				log.error("",e);
				this.columns =getDefaultFields(rootElement);
			}
		}else{
			this.columns =getDefaultFields(rootElement);
		}
		
		for(String key: this.columns){
			try {
				if(key.contains("/")){
					qo.addField(key);
				}else if(this.fieldMapping.containsKey(key)){
					qo.addField(this.fieldMapping.get(key));
				}else{
					log.error("Unknown alias \"{}\" processing query for root element: {}", key, rootElement.getName());
				}
			} catch (ElementNotFoundException e) {
				log.error("",e);
			}
		}
		
		CriteriaCollection cc = new CriteriaCollection("AND");
		
		if(this.fieldMapping.size()>0){
			for(String key: fieldMapping.keySet()){
				if(!key.equals("xsiType") && hasQueryVariable(key)){
					cc.add(this.processQueryCriteria(this.fieldMapping.get(key),getQueryVariable(key)));
				}
			}
		}
		
		for(String key:getQueryVariableKeys()){
			if(key.contains("/")){
				cc.add(this.processQueryCriteria(key,getQueryVariable(key)));
			}
		}
		
		if(isQueryVariable("req_format", "form", false))
		{
			if(this.fieldMapping.size()>0){
				for(String key: fieldMapping.keySet()){
					if(hasBodyVariable(key)){
						cc.add(this.processQueryCriteria(this.fieldMapping.get(key),getBodyVariable(key)));
					}
				}
			}
			
			for(String key:getBodyVariableKeys()){
				if(key.contains("/")){
					cc.add(this.processQueryCriteria(key,getBodyVariable(key)));
				}
			}
			
			if(hasBodyVariable("columns")){
				this.columns =XftStringUtils.CommaDelimitedStringToArrayList(getBodyVariable("columns"));
				for(String col: this.columns){
					if(col.contains("/")){
						try {
							qo.addField(col);
						} catch (ElementNotFoundException e) {
							log.error("",e);
						}
					}else if(this.fieldMapping.containsKey(col)){
						try {
							qo.addField(this.fieldMapping.get(col));
						} catch (ElementNotFoundException e) {
							log.error("",e);
						}
					}
				}
			}
		}
		
		if(cc.size()>0){
			qo.setWhere(cc);
		}
	}
	
	public XFTTable formatHeaders(XFTTable table,QueryOrganizer qo,String idpath,String URIpath){
		final ArrayList<String> newColumns = new ArrayList<>();
		for(String column:table.getColumns()){
			String xPath=qo.getXPATHforAlias(column.toLowerCase());
			if(xPath==null){
				newColumns.add(column);
			}else{
				String key=this.getLabelForFieldMapping(xPath);
				if(key==null){
					newColumns.add(xPath);
				}else{
					newColumns.add(key);
				}
			}
		}
		
		int idIndex=table.getColumnIndex(qo.getFieldAlias(idpath));
		
		if(URIpath!=null)
			newColumns.add("URI");
		
		XFTTable clone = new XFTTable();			
		clone.initTable(newColumns);
		for(Object[]row :table.rows()){
			Object[]newRow;
			if(URIpath!=null)
				newRow=new Object[row.length+1];
			else
				newRow=new Object[row.length];

            System.arraycopy(row, 0, newRow, 0, row.length);
			
			String id=(String)row[idIndex];

			if(URIpath!=null)newRow[row.length]=URIpath+id ;
			
			clone.insertRow(newRow);
		}
		
		return clone;
	}
	
	private final static String STRING="java.lang.String";
	private final static String DOUBLE="java.lang.Double";
	private final static String INTEGER="java.lang.Integer";
	private final static String DATE="java.util.Date";
	private final static String BOOL="java.lang.Boolean";
	
	public abstract String getDefaultElementName();

	public String getRootElementName(){
		try {
			GenericWrapperElement rootElementName=GenericWrapperElement.GetElement(this.getDefaultElementName());
			if(this.getQueryVariable("xsiType")!=null && !getQueryVariable("xsiType").contains(",")){
				return this.getQueryVariable("xsiType");
			}

			ArrayList<String> fields= new ArrayList<>();
			
			for(String key:getQueryVariableKeys()){
				if(key.contains("/")){
					fields.add(key);
				}else if(this.fieldMapping.containsKey(key)){
					fields.add(this.fieldMapping.get(key));
				}else if(key.equals("columns")){
					for(String col:XftStringUtils.CommaDelimitedStringToArrayList(getQueryVariable("columns"))){
						if(col.contains("/")){
							fields.add(col);
						}else if(this.fieldMapping.containsKey(col)){
							fields.add(this.fieldMapping.get(col));
						}
					}
				}
			}
			
			for(String field:fields){
				try {
					GenericWrapperElement ge=XftStringUtils.GetRootElement(field);
                    assert ge != null;
                    if(!ge.getXSIType().equals(rootElementName.getXSIType()) && ge.isExtensionOf(rootElementName)){
						rootElementName=ge;
					}
				} catch (ElementNotFoundException e) {
					log.error("",e);
				}
			}
			
			return rootElementName.getXSIType();
		} catch (Throwable e) {
			log.error("",e);
			return this.getDefaultElementName();
		}
	}
	
	@Override
	public Representation represent(Variant variant) {
		try {
			if(this.getQueryVariable("fields")!=null){
                XFTTable table=new XFTTable();
				String[] headers = {"key","xpath"};
				table.initTable(headers);
				
				for(Map.Entry<String,String> e: this.fieldMapping.entrySet()){
					Object[] row= new Object[2];
					row[0]=e.getKey();
					row[1]=e.getValue();
					table.rows().add(row);
				}
								
				return this.representTable(table, this.overrideVariant(variant), null);
			}else{
				return null;				
			}
		} catch (Exception e) {
			log.error("",e);
			return null;
		}
	}
}
