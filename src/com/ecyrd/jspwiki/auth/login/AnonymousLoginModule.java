package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.util.HttpUtil;

/**
 * <p>
 * Logs in a user based solely on IP address or assertion of a name supplied in
 * a cookie; no other authentication is performed. Barring a mis-configuration
 * or I/O error, this LoginModule <em>always</em> succeeds.
 * </p>
 * This module must be used with a CallbackHandler (such as
 * {@link WebContainerCallbackHandler}) that supports the following Callback
 * types:
 * </p>
 * <ol>
 * <li>{@link javax.security.auth.callback.NameCallback}- supplies the
 * username obtained from the cookie</li>
 * <li>{@link HttpRequestCallback}- supplies the IP address, which is used as
 * a backup in case no name is supplied</li>
 * </ol>
 * <p>
 * After authentication, a generic WikiPrincipal based on the username or IP
 * address will be created and associated with the Subject. Principal Role.ALL
 * will be added. Also, Role.ASSERTED will be added if a name was supplied,
 * Role.ANONYMOUS otherwise.
 * @see javax.security.auth.spi.LoginModule#commit()
 *      </p>
 * @author Andrew Jaquith
 */
public class AnonymousLoginModule extends AbstractLoginModule
{

    /** The name of the cookie that gets stored to the user browser. */
    public static final String PREFS_COOKIE_NAME = "JSPWikiUserProfile";

    public static final String PROMPT            = "User name";

    /**
     * Logs in the user by calling back to the registered CallbackHandler with a
     * NameCallback. The CallbackHandler must supply a username as its response.
     * @return the result of the login; this will always be <code>true</code>
     * @see javax.security.auth.spi.LoginModule#login()
     */
    public boolean login() throws LoginException
    {
        HttpRequestCallback hcb = new HttpRequestCallback();
        Callback[] callbacks = new Callback[]
        { hcb };
        try
        {
            m_handler.handle( callbacks );
            HttpServletRequest request = hcb.getRequest();
            String name = getUserCookie( request );
            if ( name != null )
            {
                m_principals.add( new WikiPrincipal( name ) );
                m_principals.add( Role.ASSERTED );
            }
            else 
            {
                m_principals.add( new WikiPrincipal( request.getRemoteAddr() ) );
                m_principals.add( Role.ANONYMOUS );
                
            }
            m_principals.add( Role.ALL );
            return true;
        }
        catch( IOException e )
        {
            e.printStackTrace();
            return false;
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback, disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }

    }

    /**
     * Returns the username cookie value.
     * @param request
     * @return
     */
    public static String getUserCookie( HttpServletRequest request )
    {
        return HttpUtil.retrieveCookieValue( request, PREFS_COOKIE_NAME );
    }

    /**
     * Sets the username cookie.
     */
    public static void setUserCookie( HttpServletResponse response, String name )
    {
        Cookie userId = new Cookie( AnonymousLoginModule.PREFS_COOKIE_NAME, name );
        userId.setMaxAge( 1001 * 24 * 60 * 60 ); // 1001 days is default.
        response.addCookie( userId );
    }

}