package com.ecyrd.jspwiki.auth.login;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.2 $ $Date: 2005-02-14 05:16:48 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Login module tests" );
        suite.addTestSuite( UserDatabaseLoginModuleTest.class );
        suite.addTestSuite( WebContainerLoginModuleTest.class );
        suite.addTestSuite( AnonymousLoginModuleTest.class );
        return suite;
    }
}