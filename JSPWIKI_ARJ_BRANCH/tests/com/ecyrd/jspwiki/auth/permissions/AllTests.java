package com.ecyrd.jspwiki.auth.permissions;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.2 $ $Date: 2005-02-14 05:17:39 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Permissions tests" );
        suite.addTestSuite( PagePermissionTest.class );
        suite.addTestSuite( WikiPermissionTest.class );
        return suite;
    }
}