package com.ecyrd.jspwiki.auth.user;

import java.io.IOException;
import java.security.Principal;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.PropertyConfigurator;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.auth.WikiPrincipal;

/**
 *  Tests the DefaultUserProfile class.
 *  @author Janne Jalkanen
 */
public class UserProfileTest extends TestCase
{
    public UserProfileTest( String s )
    {
        super( s );
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            PropertyConfigurator.configure(props);
        }
        catch( IOException e ) {}
    }

    public void setUp()
        throws Exception
    {
    }

    public void tearDown()
    {
    }

    public void testEquals()
    {
        UserProfile p = new DefaultUserProfile();
        UserProfile p2 = new DefaultUserProfile();

        p.setFullname("Alice");
        p2.setFullname("Bob");

        assertFalse( p.equals( p2 ) );
    }

    public void testEquals2()
    {
        UserProfile p = new DefaultUserProfile();
        UserProfile p2 = new DefaultUserProfile();

        p.setFullname("Alice");
        p2.setFullname("Alice");

        assertTrue( p.equals( p2 ) );
    }

    public void testStringRepresentation()
        throws Exception
    {
        Principal p = WikiPrincipal.parseStringRepresentation("username=JanneJalkanen");

        assertEquals( "name", "JanneJalkanen",p.getName() );
    }

    /**
     *  Sometimes not all servlet containers offer you correctly
     *  decoded cookies.  Reported by KalleKivimaa.
     */
    public void testBrokenStringRepresentation()
        throws Exception
    {
        Principal p = WikiPrincipal.parseStringRepresentation("username%3DJanneJalkanen");

        assertEquals( "name", "JanneJalkanen",p.getName() );
    }

    public void testUTFStringRepresentation()
        throws Exception
    {
        Principal p = new WikiPrincipal( "Määmöö" );
        Principal p2 = WikiPrincipal.parseStringRepresentation( p.getName() );
        assertEquals( "name", "Määmöö", p2.getName() );
    }

    public void testUTFURLStringRepresentation()
        throws Exception
    {
        Principal p = WikiPrincipal.parseStringRepresentation("username="+TextUtil.urlEncodeUTF8("Määmöö"));

        assertEquals( "name", "Määmöö",p.getName() );
    }


    public static Test suite()
    {
        return new TestSuite( UserProfileTest.class );
    }
}
