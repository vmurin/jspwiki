package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.Properties;

import javax.security.auth.Subject;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.auth.WikiPrincipal;

public class DefaultGroupManagerTest extends TestCase
{
    TestEngine   m_engine;

    GroupManager m_manager;

    public DefaultGroupManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        m_engine = new TestEngine( props );
        m_manager = new DefaultGroupManager();
        m_manager.initialize( m_engine, props );

        String text1 = "Foobar.\n\n[{SET members=Alice, Bob, Charlie}]\n\nBlood.";
        m_engine.saveText( "TestGroup", text1 );

        String text2 = "[{SET members=Bob}]";
        m_engine.saveText( "TestGroup2", text2 );
    }

    public void tearDown()
    {
        m_engine.deletePage( "TestGroup" );
        m_engine.deletePage( "TestGroup2" );
    }

    public void testGroupFormation() throws Exception
    {
        Principal p = new WikiPrincipal( "Alice" );
        Subject s = new Subject();
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( null, s, new DefaultGroup( "TestGroup" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "TestGroup2" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "NonExistantGroup" ) ) );

        s = new Subject();
        p = new WikiPrincipal( "Bob" );
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( null, s, new DefaultGroup( "TestGroup" ) ) );
        assertTrue( m_manager.isUserInRole( null, s, new DefaultGroup( "TestGroup2" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "NonExistantGroup" ) ) );

        s = new Subject();
        p = new WikiPrincipal( "Charlie" );
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( null, s, new DefaultGroup( "TestGroup" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "TestGroup2" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "NonExistantGroup" ) ) );

        s = new Subject();
        p = new WikiPrincipal( "Biff" );
        s.getPrincipals().add(p);
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "TestGroup" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "TestGroup2" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "NonExistantGroup" ) ) );
    }

    public static Test suite()
    {
        return new TestSuite( DefaultGroupManagerTest.class );
    }

}