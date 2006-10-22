package com.ecyrd.jspwiki.auth.authorize;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.4.2.1 $ $Date: 2006-10-22 10:05:15 $
 */
public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite( "Authorizer, group and group database tests" );
        suite.addTestSuite( GroupTest.class );
        suite.addTestSuite( WebContainerAuthorizerTest.class );
        suite.addTestSuite( JDBCGroupDatabaseTest.class );
        suite.addTestSuite( XMLGroupDatabaseTest.class );
        return suite;
    }
}