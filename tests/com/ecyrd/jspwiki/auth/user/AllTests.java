package com.ecyrd.jspwiki.auth.user;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.2 $ $Date: 2005-02-14 05:18:26 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "User profile and database tests" );
        suite.addTestSuite( UserProfileTest.class );
        suite.addTestSuite( XMLUserDatabaseTest.class );
        return suite;
    }
}