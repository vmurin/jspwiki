package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.security.acl.Permission;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.TestHttpServletRequest;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.acl.AclEntry;
import com.ecyrd.jspwiki.auth.authorize.DefaultGroup;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

/**
 * Tests the AuthorizationManager class.
 * @author Janne Jalkanen
 */
public class AuthorizationManagerTest extends TestCase
{
    private AuthorizationManager m_auth;

    private TestEngine           m_engine;

    private WikiContext          context;

    private WikiSession          session;

    public AuthorizationManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthorizationManager();
    }

    public void tearDown()
    {
        m_engine.deletePage( "Test" );
        m_engine.deletePage( "AdminGroup" );
    }

    /**
     * Set up a simple wiki page without ACL. 
     * Create a WikiContext for this page, with an associated
     * servlet request with userPrincipal Alice that has roles IT and Engineering.
     * Create a sample user Alice who possesses built-in
     * roles ALL and AUTHENTICATED.
     *
     */
    public void testHasRoleOrPrincipal()
    {
        String src = "Sample wiki page without ACL";
        try
        {
            m_engine.saveText( "Test", src );
            WikiPage p = m_engine.getPage( "Test" );
            TestHttpServletRequest request = new TestHttpServletRequest();
            request.setRoles( new String[]
            { "IT", "Engineering" } );
            context = new WikiContext( m_engine, request, p );
        }
        catch( WikiException e )
        {
            assertTrue( "Setup failed", false );
        }

        Principal principal = new WikiPrincipal( "Alice" );
        Role[] roles = new Role[]
        { Role.AUTHENTICATED, Role.ALL };
        WikiSession session = buildSession( context, principal, roles );

        // Test build-in role membership
        assertFalse( "Alice ADMIN", m_auth.hasRoleOrPrincipal( context, Role.ADMIN ) );
        assertTrue( "Alice ALL", m_auth.hasRoleOrPrincipal( context, Role.ALL ) );
        assertFalse( "Alice ANONYMOUS", m_auth.hasRoleOrPrincipal( context, Role.ANONYMOUS ) );
        assertFalse( "Alice ASSERTED", m_auth.hasRoleOrPrincipal( context, Role.ASSERTED ) );
        assertTrue( "Alice AUTHENTICATED", m_auth.hasRoleOrPrincipal( context, Role.AUTHENTICATED ) );
        assertTrue( "Alice IT", m_auth.hasRoleOrPrincipal( context, new Role( "IT" ) ) );
        assertTrue( "Alice Engineering", m_auth.hasRoleOrPrincipal( context, new Role( "Engineering" ) ) );
        assertFalse( "Alice Finance", m_auth.hasRoleOrPrincipal( context, new Role( "Finance" ) ) );

        // Test group membership
        GroupManager groupMgr = m_engine.getGroupManager();
        Group fooGroup = new DefaultGroup( "Foo" );
        groupMgr.add( fooGroup );
        Group barGroup = new DefaultGroup( "Bar" );
        barGroup.add( principal );
        groupMgr.add( barGroup );
        assertFalse( "Alice in Foo", m_auth.hasRoleOrPrincipal( context, fooGroup ) );
        assertTrue( "Alice in Bar", m_auth.hasRoleOrPrincipal( context, barGroup ) );
        
        // Test user principal posession
        assertTrue("Alice has Alice", m_auth.hasRoleOrPrincipal( context, new WikiPrincipal("Alice")));
        assertFalse("Alice has Bob", m_auth.hasRoleOrPrincipal( context, new WikiPrincipal("Bob")));
        
        // Test user principal (non-WikiPrincipal) posession
        assertTrue("Alice has Alice", m_auth.hasRoleOrPrincipal( context, new TestPrincipal("Alice")));
        assertFalse("Alice has Bob", m_auth.hasRoleOrPrincipal( context, new TestPrincipal("Bob")));
    }
    
    private static class TestPrincipal implements Principal
    {
        private final String m_name;
        public TestPrincipal( String name)
        {
            m_name = name;
        }
        
        public String getName() {
            return m_name;
        }
    }
    
    private WikiSession buildSession( WikiContext context, Principal user, Role[] roles )
    {
        WikiSession session = WikiSession.GUEST_SESSION;
        Set principals = session.getSubject().getPrincipals();
        principals.clear();
        principals.add( user );
        for( int i = 0; i < roles.length; i++ )
        {
            principals.add( roles[i] );
        }
        context.setWikiSession( session );
        return session;
    }

    public void testSimplePermissions() throws Exception
    {
        String src = "[{ALLOW edit FooBar}] ";
        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage( "Test" );
        context = new WikiContext( m_engine, p );
        session = WikiSession.GUEST_SESSION;
        context.setWikiSession( session );
        System.out.println( printPermissions( p ) );

        Principal principal = new WikiPrincipal( "FooBar" );
        session.getSubject().getPrincipals().clear();
        session.getSubject().getPrincipals().add( principal );
        session.getSubject().getPrincipals().add( Role.AUTHENTICATED );
        assertTrue( "read 1", m_auth.checkPermission( p, context, new PagePermission( "view" ) ) );
        assertTrue( "edit 1", m_auth.checkPermission( p, context, new PagePermission( "edit" ) ) );

        principal = new WikiPrincipal( "GobbleBlat" );
        session.getSubject().getPrincipals().clear();
        session.getSubject().getPrincipals().add( principal );
        session.getSubject().getPrincipals().add( Role.ANONYMOUS );
        assertTrue( "read 2", m_auth.checkPermission( p, context, new PagePermission( "view" ) ) );
        assertFalse( "edit 2", m_auth.checkPermission( p, context, new PagePermission( "edit" ) ) );
    }

    /**
     * Returns a string representation of the permissions of the page.
     */
    public static String printPermissions( WikiPage p ) throws Exception
    {
        StringBuffer sb = new StringBuffer();

        Acl acl = p.getAcl();

        sb.append( "page = " + p.getName() + "\n" );

        if ( acl != null )
        {
            for( Enumeration enum = acl.entries(); enum.hasMoreElements(); )
            {
                AclEntry entry = (AclEntry) enum.nextElement();

                sb.append( "  user = " + entry.getPrincipal().getName() + ": " );

                for( Enumeration perms = entry.permissions(); perms.hasMoreElements(); )
                {
                    Permission perm = (Permission) perms.nextElement();
                    sb.append( perm.toString() );
                }
                sb.append( ")\n" );
            }
        }
        else
        {
            sb.append( "  no permissions\n" );
        }

        return sb.toString();
    }

    public static Test suite()
    {
        return new TestSuite( AuthorizationManagerTest.class );
    }
}