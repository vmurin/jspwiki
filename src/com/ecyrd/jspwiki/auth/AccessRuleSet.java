/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import org.apache.log4j.Category;

import com.ecyrd.jspwiki.UserProfile;

/**
 * Logic:
 * <ul>
 * <li>AccessRuleSet is never empty: always defaults first.
 * <li>Special entry DENY xxx ALL: clears rule list and adds a deny-all entry
 * <li>Special entry ALLOW xxx ALL: clears rule list and adds an allow-all entry
 * <li>Any other entries are added in parsing order (order they appear in on page).
 * <li>Several values may be given in one rule; these permissions are ORred.
 * <li>The first non-matching rule causes permission to be denied.
 * <li>Special permission "admin" always allows access
 * <li>In jspwiki.properties, any number of jspwiki.authorizer.defaultrule.# = ALLOW READ x,y
 * </ul>
 * 
 * <p>Examples:
 * <p>No rules on page:  allow read, write by all
 * <p><pre>
 * [{ALLOW READ basen, guest}]
 * [{ALLOW READ ohyeahsomeoneelse}]
 * [{ALLOW WRITE basen}]
 * 
 * [{ALLOW READ ALL}]
 * [{DENY READ limited-access}]
 *
 * </pre>
 *
 *
 */
public class AccessRuleSet
    implements Cloneable
{
    private ArrayList m_readChain;
    private ArrayList m_writeChain;

    private static Category log = Category.getInstance( AccessRuleSet.class );


    public AccessRuleSet()
    {
        m_readChain = new ArrayList();
        m_writeChain = new ArrayList();
    }

    /**
     * Returns true if this rule set is empty.
     */
    public boolean isEmpty()
    {
        return( (m_readChain.size() == 0) && (m_writeChain.size() == 0) );
    }

    /**
     * Clears the rule set.
     */
    public void clear()
    {
        m_readChain.clear();
        m_writeChain.clear();
    }


    /**
     * Adds the contents of another AccessRuleSet to this one.
     * Any rules are concatenated; thus, rules B added after rules A
     * have been set are encountered first, and B overrides A.
     *
     * <p>Adds the actual objects contained in the newRules chains,
     * does not clone. (Fix?)
     */
    public void add( AccessRuleSet newRules )
    {
        m_readChain.addAll( newRules.getReadChain() );
        m_writeChain.addAll( newRules.getWriteChain() );
    }


    /**
     * Returns a shallow copy of this object.
     * The contents of the READ and WRITE chains are shared between this
     * object and the new one. Since they should be immutable, this should
     * be OK. (FIX?)
     */
    public AccessRuleSet copy()
    {
        AccessRuleSet newSet = new AccessRuleSet();
        ArrayList newRead = newSet.getReadChain();
        Iterator it = m_readChain.iterator();
        while( it.hasNext() )
        {
            newRead.add( it.next() );
        }
        ArrayList newWrite = newSet.getWriteChain();
        it = m_writeChain.iterator();
        while( it.hasNext() )
        {
            newWrite.add( it.next() );
        }

        return( newSet );
    }


    /**
     * Returns the READ rule chain.
     */
    protected ArrayList getReadChain()
    {
        return( m_readChain );
    }

    /**
     * Returns the WRITE rule chain.
     */
    protected ArrayList getWriteChain()
    {
        return( m_writeChain );
    }


    /**
     * Returns true if the given link, encountered on a WikiPage, is
     * one of the types recognized for authorization purposes. 
     * Currently supported are:
     *
     * <ul>
     * <li>{ALLOW READ role,role...}
     * <li>{ALLOW WRITE role,role,...}
     * <li>{DENY READ role,role,...}
     * <li>{DENY WRITE role,role,...}
     * <li>{REQUIRE READ permission,permission,...}
     * <li>{REQUIRE WRITE permission,permission,...}
     * </ul>
     *
     * <p>One or more permission names may be listed, separated with commas.
     * This is a simple implementation - no OR operation.  */
    public static boolean isAccessRule( String link )
    {
        return( link.startsWith( "{ALLOW " ) ||
                link.startsWith( "{DENY " )  ||
                link.startsWith( "{REQUIRE " ) );
    }

    /**
     * Given a rule string, adds the corresponding access rule to this object.
     * The rule can, but does not need to, have the beginning and ending brace
     * as recognized by isAccessRule(). This is important for Authorizers that
     * want to build default rules from strings not defined on a WikiPage.
     */
    public void addRule( String cmd )
    {
        if( cmd == null )
            return;
        String ruleLine = cmd;
        if( ruleLine.startsWith( "{" ) )
            ruleLine = ruleLine.substring( 1 );
        if( ruleLine.endsWith( "}" ) )
            ruleLine = ruleLine.substring( 0, ruleLine.length() - 1 );
        
        try
        {
            StringTokenizer fieldToks = new StringTokenizer( ruleLine );
            String policy  = fieldToks.nextToken();
            String chain   = fieldToks.nextToken();

            ArrayList qualifiers = new ArrayList();
            while( fieldToks.hasMoreTokens() )
            {
                String roleOrPerm = fieldToks.nextToken( "," );
                qualifiers.add( roleOrPerm.trim() );
            }

            addRuleSet( chain, policy, qualifiers );
        }
        catch( NoSuchElementException nsee )
        {
            log.warn( "Invalid access rule: " + cmd + " - defaults will be used." );
            return;
        }
    }


    /**
     * Parses a comma-separated list into separate keywords.
     * The trimmed Strings are returned in an ArrayList.
     */
    private ArrayList getRoles( String roleList )
    {
        ArrayList roles = new ArrayList();
        StringTokenizer tok = new StringTokenizer( roleList, "," );
        while( tok.hasMoreTokens() )
        {
            String addable = tok.nextToken().trim();
            roles.add( addable );
            log.debug( "XXX added role " + addable + " to page" );
        }
        return( roles );
    }


    /**
     * Adds allow/deny rules to the read or write chain. 
     * For each role name in roles, an allow or deny rule 
     * is inserted, depending on the policy.
     */
    private void addRuleSet( String chainName, String policy, ArrayList keywords )
    {
        if( keywords == null || keywords.size() == 0 )
        {
            return;
        }

        ArrayList chain = null;
        if( "READ".equals( chainName ) )
        {
            chain = m_readChain;
        }
        else if( "WRITE".equals( chainName ) )
        {
            chain = m_writeChain;
        }
        else
        {
            return;
        }

        // ALLOW and DENY are 'positive match' rules: if they match, we
        // allow or deny access, otherwise we continue to the next rule.
        // Role ALL is a special case, as described in the class description.
        for( int i = 0; i < keywords.size(); i++ )
        {
            String role = (String)keywords.get(i);
            if( "ALLOW".equals( policy ) )
            {
                if( Authorizer.AUTH_ROLE_ALL.equals( role ) )
                {
                    chain.clear();
                    chain.add( new AlwaysAllowRule() );
                }
                else
                    chain.add( new RoleAllowRule( role ) );
            }
            else if( "DENY".equals( policy ) )
            {
                if( Authorizer.AUTH_ROLE_ALL.equals( role ) )
                {
                    chain.clear();
                    chain.add( new AlwaysDenyRule() );
                }
                else
                    chain.add( new RoleDenyRule( (String)keywords.get(i) ) );
            }
            else if( "REQUIRE".equals( policy ) )
            {
                if( Authorizer.AUTH_ROLE_ALL.equals( role ) == false )
                    chain.add( new RequirePermissionRule( (String)keywords.get(i) ) );
            }
        }
    }


    
    /**
     * Goes through the collected rules and determines whether the UserPrincipal
     * is allowed the named access.
     *
     * <p>The chain is traversed from the last added entry toward the start.
     * The first positive or negative match is returned. If the start is
     * reached without either, a negative is returned. This traversal means
     * that the latest entry on a page always applies.
     *
     * <p>Users with role <i>admin</i> are always allowed access everywhere.
     */
    private boolean hasAccess( UserProfile wup, String chainName )
    {
        if( wup == null )
            return( false );
        if( wup.hasRole( Authorizer.AUTH_ROLE_ADMIN ) )
            return( true );

        log.debug( "Checking for " + chainName + " access" );
        ArrayList chain = null;
        if( "READ".equals( chainName ) )
            chain = m_readChain;
        else if( "WRITE".equals( chainName ) )
            chain = m_writeChain;
        else
            return( false );

        for( int i = chain.size() - 1; i >= 0; i-- )
        {
            AccessRule rule = (AccessRule)chain.get( i );
            log.debug( "rule: " + rule );
            int result = rule.evaluate( wup );
            if( result != AccessRule.CONTINUE )
            {
                log.debug( "Matches, value is " + result );
                return( (result == AccessRule.ALLOW) );
            }
            else
                log.debug( "inconclusive, continuing" );
        }

        log.debug( "no " + chainName + " rules matched, denying access." );
        return( false );
    }

    public boolean hasReadAccess( UserProfile wup )
    {
        return( hasAccess( wup, "READ" ) );
    }

    public boolean hasWriteAccess( UserProfile wup )
    {
        return( hasAccess( wup, "WRITE" ) );
    }

    
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( super.toString() + " READ: " );
        for( int i = 0; i < m_readChain.size(); i++ )
        {
            AccessRule rule = (AccessRule)m_readChain.get( i );
            buf.append( rule.toString() );
        }
        buf.append( " WRITE: " );
        for( int i = 0; i < m_writeChain.size(); i++ )
        {
            AccessRule rule = (AccessRule)m_writeChain.get( i );
            buf.append( rule.toString() );
        }

        return( buf.toString() );
    }

}
