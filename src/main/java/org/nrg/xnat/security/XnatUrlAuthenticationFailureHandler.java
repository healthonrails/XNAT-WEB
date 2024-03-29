/*
 * web: org.nrg.xnat.security.XnatUrlAuthenticationFailureHandler
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xnat.security.exceptions.NewAutoAccountNotAutoEnabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class XnatUrlAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler 
{
	private static final Log logger = LogFactory.getLog(XnatProviderManager.class);

	private String newLdapAccountNotAutoEnabledFailureUrl;
	
	public XnatUrlAuthenticationFailureHandler(String defaultFailureUrl, String newLdapAccountNotAutoEnabledFailureUrl) 
	{
		super(defaultFailureUrl);
		setNewLdapAccountNotAutoEnabledFailureUrl(newLdapAccountNotAutoEnabledFailureUrl);
	}

	public String getNewLdapAccountNotAutoEnabledFailureUrl()
	{
		return newLdapAccountNotAutoEnabledFailureUrl;
	}

	public void setNewLdapAccountNotAutoEnabledFailureUrl(String newLdapAccountNotAutoEnabledFailureUrl) 
	{
		this.newLdapAccountNotAutoEnabledFailureUrl = newLdapAccountNotAutoEnabledFailureUrl;
	}

	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		if (exception instanceof NewAutoAccountNotAutoEnabledException) {
			onAuthenticationFailureNewLdapAccountNotAutoEnabled(request, response, exception);
		} else {
			super.onAuthenticationFailure(request, response, exception);
		}
	}
	
	private void onAuthenticationFailureNewLdapAccountNotAutoEnabled(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException, ServletException
	{
		saveException(request, exception);
		
		if ( isUseForward() ) 
		{
		    logger.debug( "Forwarding to " + getNewLdapAccountNotAutoEnabledFailureUrl() );
		
		    request.getRequestDispatcher( getNewLdapAccountNotAutoEnabledFailureUrl() ).forward( request, response );
		} 
		else 
		{
		    logger.debug( "Redirecting to " + getNewLdapAccountNotAutoEnabledFailureUrl() );
		    
		    getRedirectStrategy().sendRedirect( request, response, getNewLdapAccountNotAutoEnabledFailureUrl() );
		}
	}
}
