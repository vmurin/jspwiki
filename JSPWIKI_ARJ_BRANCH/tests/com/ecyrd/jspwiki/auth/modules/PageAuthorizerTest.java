package com.ecyrd.jspwiki.auth.modules;

import java.security.Principal;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

public class PageAuthorizerTest
    extends TestCase
{
    TestEngine m_engine;

    public PageAuthorizerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        m_engine = new TestEngine(props);

        String text1 = "Foobar.\n\n[{SET defaultpermissions='ALLOW EDIT Charlie;DENY VIEW Bob'}]\n\nBlood.";
        String text2 = "Foo";

        m_engine.saveText( "DefaultPermissions", text1 );
        m_engine.saveText( "TestPage", text2 );
    }

    public void tearDown()
    {
        m_engine.deletePage( "DefaultPermissions" );
        m_engine.deletePage( "TestPage" );
    }

    public void testDefaultPermissions()
    {
        AuthorizationManager mgr = m_engine.getAuthorizationManager();

        WikiPage p = m_engine.getPage("TestPage");
        WikiContext context = new WikiContext( m_engine, p );
        WikiSession session = WikiSession.GUEST_SESSION;
        context.setWikiSession( session );

        // Charlie is anonymous
        Principal principal = new WikiPrincipal( "Charlie");
        session.getSubject().getPrincipals().clear();
        session.getSubject().getPrincipals().add( principal );
        session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertTrue( "Charlie", mgr.checkPermission( context,
                new PagePermission( "TestPage", "view" ) ) );
        assertFalse( "Charlie", mgr.checkPermission( context,
                new PagePermission( "TestPage", "edit" ) ) );

        // Bob is logged in
        principal = new WikiPrincipal( "Bob");
        session.getSubject().getPrincipals().clear();
        session.getSubject().getPrincipals().add( principal );
        session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "Bob", mgr.checkPermission( context,
                new PagePermission( "TestPage", "view" ) ) );
        assertTrue( "Bob", mgr.checkPermission( context,
                new PagePermission( "TestPage", "edit" ) ) );
    }


    public static Test suite()
    {
        return new TestSuite( PageAuthorizerTest.class );
    }

}
