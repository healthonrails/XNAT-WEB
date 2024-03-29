/*
 * web: org.nrg.xnat.security.alias.AliasTokenAuthenticationProvider
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnat.security.alias;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xdat.entities.AliasToken;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.services.AliasTokenService;
import org.nrg.xdat.services.XdatUserAuthService;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.security.provider.XnatAuthenticationProvider;
import org.nrg.xnat.security.tokens.XnatAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class AliasTokenAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider implements XnatAuthenticationProvider {
    @Autowired
    public AliasTokenAuthenticationProvider(final AliasTokenService aliasTokenService, final XdatUserAuthService userAuthService) {
        super();
        _aliasTokenService = aliasTokenService;
        _userAuthService = userAuthService;
    }

    /**
     * Performs authentication with the same contract as {@link AuthenticationManager#authenticate(Authentication)}.
     *
     * @param authentication the authentication request object.
     *
     * @return a fully authenticated object including credentials. May return <code>null</code> if the
     *         <code>AuthenticationProvider</code> is unable to support authentication of the passed
     *         <code>Authentication</code> object. In such a case, the next <code>AuthenticationProvider</code> that
     *         supports the presented <code>Authentication</code> class will be tried.
     *
     * @throws AuthenticationException if authentication fails.
     */
    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String     alias = (String) authentication.getPrincipal();
        final AliasToken token = _aliasTokenService.locateToken(alias);
        if (token == null) {
            throw new BadCredentialsException("No valid alias token found for alias: " + alias);
        }
        // Translate the token into the actual user name and allow the DAO to retrieve the user object.
        log.debug("Authenticating token for {}", alias);
        return super.authenticate(authentication);
    }

    /**
     * Returns <code>true</code> if this <Code>AuthenticationProvider</code> supports the indicated
     * <Code>Authentication</code> object.
     * <p>
     * Returning <code>true</code> does not guarantee an <code>AuthenticationProvider</code> will be able to
     * authenticate the presented instance of the <code>Authentication</code> class. It simply indicates it can support
     * closer evaluation of it. An <code>AuthenticationProvider</code> can still return <code>null</code> from the
     * {@link #authenticate(org.springframework.security.core.Authentication)} method to indicate another <code>AuthenticationProvider</code> should be
     * tried.
     * </p>
     * <p>Selection of an <code>AuthenticationProvider</code> capable of performing authentication is
     * conducted at runtime the <code>ProviderManager</code>.</p>
     *
     * @param authentication DOCUMENT ME!
     *
     * @return <code>true</code> if the implementation can more closely evaluate the <code>Authentication</code> class
     *         presented
     */
    @Override
    public boolean supports(final Class<?> authentication) {
        return AliasTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Indicates whether the provider should be visible to and selectable by users. <b>false</b> usually indicates an
     * internal authentication provider, e.g. token authentication.
     *
     * @return <b>true</b> if the provider should be visible to and usable by users.
     */
    @Override
    public boolean isVisible() {
        return false;
    }

    /**
     * This is a no-op method: the alias token provider is never visible and can't be set to be visible.
     *
     * @param visible This parameter's value is ignored.
     */
    @Override
    public void setVisible(final boolean visible) {
        //
    }

    /**
     * Gets the provider's name.
     *
     * @return The provider's name.
     */
    @Override
    public String getName() {
        return XdatUserAuthService.TOKEN;
    }

    @Override
    public String getProviderId() {
        return XdatUserAuthService.TOKEN;
    }

    @Override
    public String getAuthMethod() {
        return XdatUserAuthService.TOKEN;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
     * @deprecated Ordering of authentication providers is set through the {@link SiteConfigPreferences#getEnabledProviders()} property.
     */
    @Deprecated
    @Override
    public int getOrder() {
        log.info("The order property is deprecated and will be removed in a future version of XNAT.");
        return 0;
    }

    /**
     * @deprecated Ordering of authentication providers is set through the {@link SiteConfigPreferences#setEnabledProviders(List)} property.
     */
    @Deprecated
    @Override
    public void setOrder(final int order) {
        log.info("The order property is deprecated and will be removed in a future version of XNAT.");
    }

    /**
     * Auto-enabling is not supported for this provider. This implementation will always return false.
     *
     * @return This implementation will always return false.
     */
    @Override
    public boolean isAutoEnabled() {
        return false;
    }

    /**
     * Auto-enabling is not supported for this provider. Attempting to change this property has no effect.
     *
     * @param autoEnabled The value set for this implementation is ignored.
     */
    @Override
    public void setAutoEnabled(final boolean autoEnabled) {
        log.info("This provider does not support auto-enabling.");
    }

    /**
     * Auto-verification is not supported for this provider. This implementation will always return false.
     *
     * @return This implementation will always return false.
     */
    @Override
    public boolean isAutoVerified() {
        return false;
    }

    /**
     * Auto-verification is not supported for this provider. Attempting to change this property has no effect.
     *
     * @param autoVerified The value set for this implementation is ignored.
     */
    @Override
    public void setAutoVerified(final boolean autoVerified) {
        throw new UnsupportedOperationException("This provider does not support auto-verification.");
    }

    @Override
    public XnatAuthenticationToken createToken(final String username, final String password) {
        log.debug("Creating new alias token authentication token for alias {}", username);
        return new AliasTokenAuthenticationToken(username, password);
    }

    @Override
    public boolean supports(final Authentication authentication) {
        final String providerId      = getProviderId();
        final String tokenProviderId = ((XnatAuthenticationToken) authentication).getProviderId();
        log.debug("Checking whether this provider with ID {} supports an authentication token with provider ID {}", providerId, tokenProviderId);
        return supports(authentication.getClass()) && StringUtils.equals(providerId, tokenProviderId);
    }

    @Override
    protected void additionalAuthenticationChecks(final UserDetails userDetails, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        if (authentication.getCredentials() == null) {
            log.debug("Authentication failed: no credentials provided");
            throw new BadCredentialsException("The submitted alias token was empty.");
        }

        if (!UserI.class.isAssignableFrom(userDetails.getClass())) {
            throw new AuthenticationServiceException("User details class is not of a type I know how to handle: " + userDetails.getClass());
        }

        final UserI xdatUserDetails = (UserI) userDetails;
        log.debug("Validating alias token login for user {}", xdatUserDetails.getUsername());
        Users.validateUserLogin(xdatUserDetails);

        final String alias  = ((AliasTokenAuthenticationToken) authentication).getAlias();
        final String secret = ((AliasTokenAuthenticationToken) authentication).getSecret();
        final String userId = _aliasTokenService.validateToken(alias, secret);
        if (StringUtils.isBlank(userId) || !userId.equals(userDetails.getUsername())) {
            throw new BadCredentialsException("The submitted alias token was invalid: " + alias);
        }
        log.info("Validated alias token login for user {} with alias {}", userId, alias);
    }

    /**
     * Allows subclasses to actually retrieve the <code>UserDetails</code> from an implementation-specific
     * location, with the option of throwing an <code>AuthenticationException</code> immediately if the presented
     * credentials are incorrect (this is especially useful if it is necessary to bind to a resource as the user in
     * order to obtain or generate a <code>UserDetails</code>).<p>Subclasses are not required to perform any
     * caching, as the <code>AbstractUserDetailsAuthenticationProvider</code> will by default cache the
     * <code>UserDetails</code>. The caching of <code>UserDetails</code> does present additional complexity as this
     * means subsequent requests that rely on the cache will need to still have their credentials validated, even if
     * the correctness of credentials was assured by subclasses adopting a binding-based strategy in this method.
     * Accordingly it is important that subclasses either disable caching (if they want to ensure that this method is
     * the only method that is capable of authenticating a request, as no <code>UserDetails</code> will ever be
     * cached) or ensure subclasses implement {@link #additionalAuthenticationChecks(UserDetails, UsernamePasswordAuthenticationToken)}
     * to compare the credentials of a cached <code>UserDetails</code> with subsequent authentication requests.</p>
     * <p>Most of the time subclasses will not perform credentials inspection in this method, instead
     * performing it in {@link #additionalAuthenticationChecks(UserDetails, UsernamePasswordAuthenticationToken)} so
     * that code related to credentials validation need not be duplicated across two methods.</p>
     *
     * @param username       The username to retrieve
     * @param authentication The authentication request, which subclasses <em>may</em> need to perform a binding-based
     *                       retrieval of the <code>UserDetails</code>
     *
     * @return the user information (never <code>null</code> - instead an exception should the thrown)
     *
     * @throws AuthenticationException If the credentials could not be validated (generally a
     *                                 <code>BadCredentialsException</code>, an <code>AuthenticationServiceException</code> or
     *                                 <code>UsernameNotFoundException</code>)
     */
    @Override
    protected UserDetails retrieveUser(final String username, final UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        final AliasToken token = _aliasTokenService.locateToken(username);
        if (token == null) {
            throw new UsernameNotFoundException("Unable to locate token with alias: " + username);
        }
        /*
         * We don't really know which provider the user was authenticated under when this token was created.
         * The hack is to return the user details for the most recent successful login of the user, as that is likely
         * the provider that was used. Not perfect, but better than just hard-coding to localdb provider cause then
         * it won't work for a token created by an user authenticated by some other means).
         */
        return _userAuthService.getUserDetailsByUsernameAndMostRecentSuccessfulLogin(token.getXdatUserId());
    }

    private final AliasTokenService   _aliasTokenService;
    private final XdatUserAuthService _userAuthService;
}
