package com.ecyrd.jspwiki.auth;

import java.security.Principal;

import javax.security.auth.Subject;

import com.ecyrd.jspwiki.WikiContext;

/**
 * Interface for service providers of authorization information.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.4.2 $ $Date: 2005-02-14 05:08:58 $
 */
public interface Authorizer
{

    /**
     * Determines whether the user represented by a supplied Subject is in a
     * particular role. This method takes three parameters. Context may be null;
     * however, if a Authorizer implementation requires it (e.g.,
     * WebContainerAuthorizer), this method must return false.
     * @param context the current WikiContext
     * @param subject
     * @param role
     * @return
     */
    public boolean isUserInRole( WikiContext context, Subject subject, Principal role );

    /**
     * Looks up and returns a role principal matching a given string.
     * If a matching role cannot be found, this method returns null.
     * Note that it may not be feasible for an Authorizer implementation
     * to return a role principal.
     * @param role the name of the role to retrieve
     * @return the role principal
     */
    public Principal findRole( String role );
    
}