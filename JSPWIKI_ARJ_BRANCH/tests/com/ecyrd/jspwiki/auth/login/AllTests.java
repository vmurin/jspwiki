package com.ecyrd.jspwiki.auth.login;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.1 $ $Date: 2005-02-01 00:42:35 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Test for com.ecyrd.jspwiki.auth.login" );
        suite.addTestSuite( UserDatabaseLoginModuleTest.class );
        suite.addTestSuite( WebContainerLoginModuleTest.class );
        suite.addTestSuite( AnonymousLoginModuleTest.class );
        return suite;
    }
}