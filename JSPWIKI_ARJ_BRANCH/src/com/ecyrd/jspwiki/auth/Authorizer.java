package com.ecyrd.jspwiki.auth;

import java.security.Principal;

import javax.security.auth.Subject;

import com.ecyrd.jspwiki.WikiContext;

/**
 * Interface for service providers of authorization information.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.4.1 $ $Date: 2005-02-01 02:38:57 $
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

}