package com.ecyrd.jspwiki.auth.user;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.1 $ $Date: 2005-02-01 00:41:38 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Test for com.ecyrd.jspwiki.auth.user" );
        suite.addTestSuite( UserProfileTest.class );
        suite.addTestSuite( XMLUserDatabaseTest.class );
        return suite;
    }
}