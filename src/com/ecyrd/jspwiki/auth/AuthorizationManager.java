/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2.1 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth;

import java.security.AccessControlException;
import java.security.AccessController;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Properties;

import javax.security.auth.Subject;

import org.apache.log4j.Category;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 * Manages all access control and authorization.
 * @see AuthenticationManager
 */
public class AuthorizationManager
{
    public static final String                DEFAUT_AUTHORIZER = "com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer";

    public static final String                PROP_AUTHORIZER   = "jspwiki.authorizer";

    private static final AuthorizationManager c_instance        = new AuthorizationManager();

    static Category                           log               = Category.getInstance( AuthorizationManager.class );

    private Authorizer                        m_authorizer      = null;

    private WikiEngine                        m_engine          = null;

    /**
     * Returns true or false, depending on whether a Permission is allowed for
     * this WikiContext.
     * @param permission Any of the available page permissions "view", "edit,
     *            "comment", etc.
     * @see #checkPermission(WikiPage, WikiContext, Permission)
     */
    public boolean checkPermission( WikiContext context, Permission permission )
    {
        if ( context == null ) 
        {
            return checkPermission( null, context, permission );
        }
        return checkPermission( context.getPage(), context, permission );
    }
    
    /**
     * Returns true or false, depending on whether this action is allowed for
     * this WikiPage.
     * @param permission Any of the available page permissions "view", "edit,
     *            "comment", etc.
     * @deprecated use instead:
     *             {@link AuthorizationManager#checkPermission(WikiPage, WikiContext, Permission)}
     */
    public boolean checkPermission( WikiPage page, WikiContext context, String permission )
    {
        Permission pagePermission = new PagePermission( page.getName(), permission );
        return checkPermission( page, context, pagePermission );
    }

    /**
     * Returns true or false, depending on whether this action is allowed for
     * this WikiPage. The access control algorithm works this way:
     * <ol>
     * <li>The ACL for the page is obtained</li>
     * <li>The Subject associated with the current WikiSession is obtained
     * (this is looked up in the HttpSession associated with the supplied
     * HttpServletRequest)</li>
     * <li>If the Subject's principal set includes the Role principal that is the
     * administrator group, always allow the permission</li>
     * <li>If there is no ACL at all, check to see if the Permission is allowed
     * according to the "static" security policy. The security policy speficies
     * what permissions are available by default</li>
     * <li>If there is an ACL, get the list of Principals assigned this
     * permission in the ACL: these will be role, group or user Principals. Then
     * determine whether the user (Subject) is in the role/group or poseseses
     * the user Principal. The matching process works as follows:
     * <ul>
     * <li>If the Principal is a built-in Role, check to see whether the
     * Subject posesses the Role principal in its principal set. Return
     * <code>true</code> if it does; if not, try the next check</li>
     * <li>If the Principal is a Role (and not built-in, OR the result of the
     * previous check was false), call the current Authorizer's
     * <code>isInRole</code> method. Return the result.</li>
     * <li>If the Principal is a Group, iterate through the Subject's principal
     * set and call the current GroupManager's <code>isInRole</code> method
     * </li>
     * on each one; if any of the principals in the set are members of the
     * requested group, return <code>true</code> immediately. Otherwise, if
     * none are members of the requested group, return <code>false</code>.
     * </li>
     * <li>For all other cases, iterate through the Subject's principal set and
     * check to see if any of the principals in the set have a
     * <code>getName()</code> result that matches the the desired name. If so,
     * return <code>true</code> immediately. Otherwise, if none match, return
     * <code>false</code>.</li>
     * </ul>
     * @param page the wiki page
     * @param context the current wiki context. If null, a synthetic WikiContext
     *            containing a WikiSession.GUEST_SESSION is assumed
     * @param permission the permission being checked
     * @see #hasRoleOrPrincipal(WikiContext, Principal)
     * @return the result of the permission check
     * @deprecated use checkPermission(WikiPage,WikiContext) instead
     */
    public boolean checkPermission( WikiPage page, WikiContext context, Permission permission )
    {
        AuthenticationManager authenticator = m_engine.getAuthenticationManager();

        //
        //  A slight sanity check.
        //
        if ( page == null || permission == null )
        {
            return false;
        }

        WikiSession session = WikiSession.GUEST_SESSION;
        if ( context != null )
        {
            session = WikiSession.getWikiSession( context.getHttpRequest() );
        }

        // Always allow the action if it's the Admin
        Subject subject = session.getSubject();
        if ( subject.getPrincipals().contains( Role.ADMIN ) )
        {
            return true;
        }

        //
        // Does the page in question have an access control list?
        // If no ACL, we check the security policy to see what the
        // defaults should be.
        Acl acl = m_engine.getAclManager().getPermissions( page );
        if ( acl == null )
        {
            return checkStaticPermission( subject, permission );
        }

        //
        //  Next, iterate through the Principal objects assigned
        //  this permission.

        Principal[] aclPrincipals = acl.findPrincipals( permission );

        log.debug( "Checking ACL entries..." );
        log.debug( "ACL for this page is: " + acl );
        log.debug( "Checking for principal: " + aclPrincipals );
        log.debug( "Permission: " + permission );

        for( int i = 0; i < aclPrincipals.length; i++ )
        {
            Principal aclPrincipal = aclPrincipals[i];
            if ( hasRoleOrPrincipal( context, aclPrincipal ) )
            {
                return true;
            }
        }
        return false;

    }
    
    /**
     * Determines if the Subject associated with a supplied WikiContext contains
     * a desired user principal or built-in role principal, OR is a member a
     * group or external role. The rules as as follows:
     * <ul>
     * <li>If the desired principal is a built-in Role, the algorithm simply
     * checks to see if the Subject possesses it in its Principal set</li>
     * <li>If the desired principal is a Role but not built-in, the external
     * authorizer's <code>isInRole</code> method is called</li>
     * <li>If the desired principal is a Group, the group authorizer's
     * <code>isInRole</code> method is called</li>
     * <li>For all other cases, iterate through the Subject's principal set and
     * check to see if any of the principals in the set have a
     * <code>getName()</code> result that matches the the desired name. If so,
     * return <code>true</code> immediately. Otherwise, if none match, return
     * <code>false</code>.</li>
     * </ul>
     * @param context the current wiki context. If null, always returns false
     * @param principal the principal (role, group, or user principal) to look for.
     * If null, always returns false
     * @return the result
     */
    protected boolean hasRoleOrPrincipal( WikiContext context, Principal principal )
    {
        if (context == null || context.getWikiSession() == null || principal == null)
        {
            return false;
        }
        Subject subject = context.getWikiSession().getSubject();
        if ( principal instanceof Role)
        {
            Role role = (Role) principal;
            // If built-in role, check to see if user possesses it.
            if ( Role.isBuiltInRole( role ) && subject.getPrincipals().contains( role ) )
            {
                return true;
            }
            // No luck; try the external authorizer (e.g., container)
            return (m_authorizer.isUserInRole( context, subject, role ) );
        }
        else if ( principal instanceof Group )
        {
            Group group = (Group) principal;
            return m_engine.getGroupManager().isUserInRole( context, subject, group );
        }
        return hasUserPrincipal( subject, principal );
    }

    /**
     * Protected method that determines whether any of the user principals
     * posessed by a Subject have the same name as a supplied Principal.
     * Principals in the subject's principal set that are of types Role or Group
     * are <em>not</em> considered, since this would otherwise introduce the
     * potential for spoofing. Principal.
     * @param subject the Subject whose principal set will be inspected
     * @param principal the desired principal
     * @return true if any of the Subject's principals have the same name as the
     *         supplied principal, otherwise false
     */
    protected boolean hasUserPrincipal( Subject subject, Principal principal )
    {
        String principalName = principal.getName();
        for( Iterator it = subject.getPrincipals().iterator(); it.hasNext(); )
        {
            Principal userPrincipal = (Principal) it.next();
            if ( !( userPrincipal instanceof Role || userPrincipal instanceof Group ) )
            {
                if ( userPrincipal.getName().equals( principalName ) )
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Initializes AuthorizationManager with an engine and set of properties.
     * Expects to find property 'jspwiki.authorizer' with a valid Authorizer
     * implementation name to take care of group lookup operations.
     */
    public void initialize( WikiEngine engine, Properties properties ) throws WikiException
    {
        m_engine = engine;
        m_authorizer = getAuthorizerImplementation( properties );
    }

    /**
     * Attempts to locate and initialize a Authorizer to use with this manager.
     * Throws a WikiException if no entry is found, or if one fails to
     * initialize.
     * @param props jspwiki.properties, containing a
     *            'jspwiki.authorization.provider' class name
     * @return a Authorizer used to get page authorization information
     * @throws WikiException
     */
    private Authorizer getAuthorizerImplementation( Properties props ) throws WikiException
    {
        String authClassName = props.getProperty( PROP_AUTHORIZER, DEFAUT_AUTHORIZER );
        return (Authorizer) locateImplementation( authClassName );
    }

    private Object locateImplementation( String clazz ) throws WikiException
    {
        if ( clazz != null )
        {
            try
            {
                // TODO: this should probably look in package ...modules
                Class authClass = ClassUtil.findClass( "com.ecyrd.jspwiki.auth.modules", clazz );
                Object impl = authClass.newInstance();
                return impl;
            }
            catch( ClassNotFoundException e )
            {
                log.fatal( "Authorizer " + clazz + " cannot be found", e );
                throw new WikiException( "Authorizer " + clazz + " cannot be found" );
            }
            catch( InstantiationException e )
            {
                log.fatal( "Authorizer " + clazz + " cannot be created", e );
                throw new WikiException( "Authorizer " + clazz + " cannot be created" );
            }
            catch( IllegalAccessException e )
            {
                log.fatal( "You are not allowed to access this authorizer class", e );
                throw new WikiException( "You are not allowed to access this authorizer class" );
            }
        }
        else
        {
            throw new NoRequiredPropertyException( "Unable to find a " + PROP_AUTHORIZER + " entry in the properties.",
                    PROP_AUTHORIZER );
        }
    }

    public static final AuthorizationManager getInstance()
    {
        return c_instance;
    }

    /**
     * Determines whether a Subject posesses a given "static" Permission as
     * defined in the security policy file. This method uses standard Java 2
     * security calls to do its work. Note that the current access control
     * context's <code>codeBase</code> is effectively <em>this class</em>,
     * not that of the caller. Therefore, this method will work best when what
     * matters in the policy is <em>who</em> makes the permission check, not
     * what the caller's code source is. Internally, this method works by
     * excuting <code>Subject.doAsPrivileged</code> with a privileged action
     * that simply calls
     * @link AccessController#checkPermission(java.security.Permission). A
     *       caught exception (or lack thereof) determines whether the privilege
     *       is absent (or present).
     * @param subject
     * @param permission
     * @return
     */
    protected static final boolean checkStaticPermission( final Subject subject, final Permission permission )
    {
        try
        {
            Subject.doAsPrivileged( subject, new PrivilegedAction()
            {
                public Object run()
                {
                    AccessController.checkPermission( permission );
                    return null;
                }
            }, null );
            return true;
        }
        catch( AccessControlException e )
        {
            return false;
        }
    }
    
    /**
     * <p>Given a supplied string representing a principal name from an ACL, this
     * method resolves the correct type of Principal (role, group, or user).
     * This method is guaranteed to always return a Principal.
     * The algorithm is straightforward:</p>
     * <ol>
     * <li>If the name matches one of the built-in Role names,
     * return the built-in Role</li>
     * <li>If the name matches one supplied by the current
     * Authorizer, return the Role</li>
     * <li>If the name matches a group managed by the 
     * current GroupManager, return the Group</li>
     * <li>Otherwise, assume that the name represents a user
     * principal. Using the current UserDatabase, find the
     * first user whose login name, full name, or wiki name
     * matches the supplied name</li>
     * <li>Finally, if a user cannot be found, manufacture
     * and return a generic WikiPrincipal</li>
     * </ol>
     * @param name the name of the principal
     * @return the fully-resolved principal
     */
    public Principal resolvePrincipal( String name ) {
        
        // Check built-in Roles first
        Role role = new Role(name);
        if (Role.isBuiltInRole(role)) {
            return role;
        }
        
        // Check Authorizer Roles
        Principal principal = m_authorizer.findRole( name );
        if (principal != null) 
        {
            return principal;
        }
        
        // Check Groups
        principal = m_engine.getGroupManager().findRole( name );
        if (principal != null)
        {
            return principal;
        }
        
        // Ok, no luck---this must be a user principal
        Principal[] principals = null;
        UserProfile profile = null;
        UserDatabase db = m_engine.getUserDatabase();
        try
        {
            profile = db.find( name );
            principals = db.getPrincipals( profile.getLoginName() );
            for (int i = 0; i < principals.length; i++) 
            {
                principal = principals[i];
                if (principal.getName().equals( name ))
                {
                    return principal;
                }
            }
            return new WikiPrincipal( name );
        }
        catch( NoSuchPrincipalException e )
        {
            return new WikiPrincipal( name );
        }
    }

}