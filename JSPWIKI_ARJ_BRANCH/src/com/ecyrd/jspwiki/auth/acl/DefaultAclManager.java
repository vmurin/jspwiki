package com.ecyrd.jspwiki.auth.acl;

import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.1 $ $Date: 2005-02-01 02:53:13 $
 */
public class DefaultAclManager implements AclManager
{
    static Logger      log = Logger.getLogger( AuthenticationManager.class );

    private WikiEngine m_engine = null;
    
    private UserDatabase m_userDatabase = null;
    
    private GroupManager m_groupManager = null;

    /**
     * @see com.ecyrd.jspwiki.auth.acl.AclManager#initialize(com.ecyrd.jspwiki.WikiEngine, java.util.Properties)
     */
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
        m_userDatabase = engine.getUserDatabase();
        m_groupManager = engine.getGroupManager();
    }

    /**
     *  A helper method for parsing textual AccessControlLists.  The line
     *  is in form "(ALLOW) <permission> <principal>,<principal>,<principal>".
     * This method was moved from Authorizer.
     *
     *  @param page The current wiki page.  If the page already has an ACL,
     *              it will be used as a basis for this ACL in order to
     *              avoid the creation of a new one.
     *  @param ruleLine The rule line, as described above.
     *
     
     *  @return A valid Access Control List.  May be empty.
     *
     *  @throws WikiSecurityException, if the ruleLine was faulty somehow.
     *
     *  @since 2.1.121
     */
    public Acl parseAcl( WikiPage page, String ruleLine ) throws WikiSecurityException
    {
        Acl acl = page.getAcl();
        if ( acl == null )
            acl = new AclImpl();

        try
        {
            StringTokenizer fieldToks = new StringTokenizer( ruleLine );
            String policy = fieldToks.nextToken();
            boolean isGroup = ( policy.equals( "GROUP" ) );
            String chain = fieldToks.nextToken();
            String pageName = page.getName();

            while( fieldToks.hasMoreTokens() )
            {
                String principalName = fieldToks.nextToken( "," ).trim();
                Principal principal;
                if ( isGroup )
                {
                    principal = m_groupManager.find( principalName );
                }
                else
                {
                    principal = resolvePrincipal( principalName );
                }

                AclEntry oldEntry = acl.getEntry( principal );

                if ( oldEntry != null )
                {
                    log.debug( "Adding to old acl list: " + principal + ", " + chain );
                    oldEntry.addPermission( new PagePermission( pageName, chain ) );
                }
                else
                {
                    log.debug( "Adding new acl entry for " + chain );
                    AclEntry entry = new AclEntryImpl();

                    entry.setPrincipal( principal );
                    entry.addPermission( new PagePermission( pageName, chain ) );

                    acl.addEntry( entry );
                }
            }

            page.setAcl( acl );

            log.debug( acl.toString() );
        }
        catch( NoSuchElementException nsee )
        {
            log.warn( "Invalid access rule: " + ruleLine + " - defaults will be used." );
            throw new WikiSecurityException( "Invalid access rule: " + ruleLine );
        }
        //        catch( NotOwnerException noe )
        //        {
        //            throw new InternalWikiException("Someone has implemented access control on access control lists without telling me.");
        //        }
        catch( IllegalArgumentException iae )
        {
            throw new WikiSecurityException( "Invalid permission type: " + ruleLine );
        }

        return acl;
    }
    
    private Principal resolvePrincipal( String name ) {
        Principal[] principals = null;
        UserProfile profile = null;
        try
        {
            profile = m_userDatabase.find( name );
            principals = m_userDatabase.getPrincipals( profile.getLoginName() );
            for (int i = 0; i < principals.length; i++) 
            {
                Principal principal = principals[i];
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