/*
 * JSPWiki - a JSP-based WikiWiki clone. Copyright (C) 2001-2003 Janne Jalkanen
 * (Janne.Jalkanen@iki.fi) This program is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of the
 * License, or (at your option) any later version. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. You should have
 * received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.ecyrd.jspwiki.auth;

import java.security.Permission;
import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.login.WebContainerCallbackHandler;
import com.ecyrd.jspwiki.auth.login.WikiCallbackHandler;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

/**
 * @author Andrew R. Jaquith
 * @author Janne Jalkanen
 * @author Erik Bunn
 * @since 2.3
 */
public class AuthenticationManager
{

    public static final String                 LOGIN_CONTAINER     = "JSPWiki-container";

    public static final String                 LOGIN_CUSTOM        = "JSPWiki-custom";

    /** If true, logs the IP address of the editor on saving. */
    public static final String                 PROP_STOREIPADDRESS = "jspwiki.storeIPAddress";

    static Logger                              log                 = Logger.getLogger( AuthenticationManager.class );

    private String                             m_administrator     = null;

    private WikiEngine                         m_engine            = null;

    /** If true, logs the IP address of the editor */
    private boolean                            m_storeIPAddress    = true;

    /**
     * Creates an AuthenticationManager instance for the given WikiEngine and
     * the specified set of properties. All initialization for the modules is
     * done here.
     */
    public void initialize( WikiEngine engine, Properties props ) throws WikiException
    {
        m_engine = engine;

        m_storeIPAddress = TextUtil.getBooleanProperty( props, PROP_STOREIPADDRESS, m_storeIPAddress );
    }

    /**
     * Attempts to perform a login for the given username/password combination.
     * @param username The user name. This is a login name, not a WikiName. In
     *            most cases they are the same, but in some cases, they might
     *            not be.
     * @param password The password
     * @return true, if the username/password is valid
     */
    public boolean loginCustom( String username, String password, HttpServletRequest request )
    {
        if ( request == null )
        {
            log.error( "No Http request provided, cannot log in." );
            return false;
        }
        WikiSession wikiSession = WikiSession.getWikiSession( request );
        Subject subject = wikiSession.getSubject();
        subject.getPrincipals().clear();
        CallbackHandler handler = new WikiCallbackHandler( m_engine.getUserDatabase(), username, password );
        return doLogin( wikiSession, handler, LOGIN_CUSTOM );
    }

    /**
     * Logs in the user by attempting to obtain a Subject from web container via
     * the currrent wiki context. This method logs in the user if the
     * user's status is "unknown" to the WikiSession, or if the Http servlet
     * container's authentication status has changed. This method assumes that
     * the WikiContext has been previously initialized with an
     * HttpServletRequest and a WikiSession. If neither of these conditions are
     * true, an IllegalStateException is thrown. This method is a
     * <em>privileged</em> action; the caller must posess the (name here)
     * permission.
     * @param contect wiki context for this user.
     * @throws IllegalStateException if the wiki context's
     *             <code>getHttpRequest</code> or <code>getWikiSession</code>
     *             methods return null
     * @throws IllegalArgumentException if the <code>context</code> parameter
     *             is null
     * @since 2.3
     */
    public boolean loginContainer( WikiContext context )
    {
        // TODO: this should be a privileged action

        // If the WikiSession's Subject doesn't have any principals,
        // do a "login".
        if ( context == null )
        {
            throw new IllegalArgumentException( "Context may not be null" );
        }
        WikiSession wikiSession = context.getWikiSession();
        HttpServletRequest request = context.getHttpRequest();
        if ( wikiSession == null )
        {
            throw new IllegalStateException( "Wiki context's WikiSession may not be null" );
        }
        if ( request == null )
        {
            throw new IllegalStateException( "Wiki context's HttpRequest may not be null" );
        }
        CallbackHandler handler = new WebContainerCallbackHandler( request, m_engine.getUserDatabase() );
        return doLogin( wikiSession, handler, LOGIN_CONTAINER );
    }

    /**
     * Logs the user out by invalidating the Http session associated with the
     * Wiki context. As a consequence, this will also automatically unbind the
     * WikiSession, and with it all of its Principal objects and Subject. This
     * is a cheap-and-cheerful way to do it without invoking JAAS LoginModules.
     * The logout operation will also remove the JSESSIONID cookie from
     * the user's browser session, if it was set.
     * @param context the current wiki context
     */
    public static void logout( HttpSession session )
    {
        if ( session == null )
        {
            log.error( "No HTTP session provided; cannot log out." );
            return;
        }
        // Clear the session
        session.invalidate();

        // Remove JSESSIONID in case it is still kicking around
        Cookie sessionCookie = new Cookie("JSESSIONID", null);
        sessionCookie.setMaxAge(0);
    }

    /**
     * Log in to the application using a given JAAS LoginConfiguration.
     * @param wikiSession
     * @param handler
     * @param application
     * @return the result of the login
     * @throws WikiSecurityException
     */
    private boolean doLogin( WikiSession wikiSession, CallbackHandler handler, String application )
    {
        //TODO: inject Role.ADMIN if user is named admin or part of role
        try
        {
            LoginContext loginContext = new LoginContext( application, handler );
            loginContext.login();
            Subject subject = loginContext.getSubject();
            wikiSession.setSubject( subject );
            return true;
        }
        catch( LoginException e )
        {
            log.error( "Couldn't log in. Is something wrong with your jaas.config file?\nMessage="
                    + e.getLocalizedMessage() );
            return false;
        }
    }

    /**
     * Determines whether authentication is required to view wiki pages. This is
     * done by checking for the PagePermission.VIEW permission using a null
     * WikiContext. It delegates the check to
     * {@link AuthorizationManager#checkPermission(WikiContext, Permission)}.
     * @return <code>true</code> if logins are required
     */
    public boolean strictLogins()
    {
        return ( m_engine.getAuthorizationManager().checkPermission( null, PagePermission.VIEW ) );
    }

}