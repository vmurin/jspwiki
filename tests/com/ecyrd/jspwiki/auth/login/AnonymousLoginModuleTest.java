package com.ecyrd.jspwiki.auth.login;

import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TestHttpServletRequest;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.XMLUserDatabase;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.1 $ $Date: 2005-02-01 00:42:35 $
 */
public class AnonymousLoginModuleTest extends TestCase
{
    UserDatabase db;

    Subject      subject;

    public final void testLogin()
    {
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setRemoteAddr( "53.33.128.9" );
        try
        {
            // Test using IP address (AnonymousLoginModule succeeds)
            CallbackHandler handler = new WebContainerCallbackHandler( request, db );
            LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "53.33.128.9" ) ) );
            assertTrue( principals.contains( Role.ANONYMOUS ) );
            assertTrue( principals.contains( Role.ALL ) );

            // Test using Cookie and IP address (AnonymousLoginModule succeeds)
            Cookie cookie = new Cookie( AnonymousLoginModule.PREFS_COOKIE_NAME, "Bullwinkle" );
            request.setCookies( new Cookie[]
            { cookie } );
            subject = new Subject();
            handler = new WebContainerCallbackHandler( request, db );
            context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "Bullwinkle" ) ) );
            assertTrue( principals.contains( Role.ASSERTED ) );
            assertTrue( principals.contains( Role.ALL ) );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    public final void testLogout()
    {
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setRemoteAddr( "53.33.128.9" );
        try
        {
            CallbackHandler handler = new WebContainerCallbackHandler( request, db );
            LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "53.33.128.9" ) ) );
            assertTrue( principals.contains( Role.ANONYMOUS ) );
            assertTrue( principals.contains( Role.ALL ) );
            context.logout();
            assertEquals( 0, principals.size() );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.put( XMLUserDatabase.PROP_USERDATABASE, "./etc/userdatabase.xml" );
        db = new XMLUserDatabase();
        subject = new Subject();
        try
        {
            db.initialize( null, props );
        }
        catch( NoRequiredPropertyException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

}