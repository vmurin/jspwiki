/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.auth.user;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.catalina.util.HexUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * Manages {@see DefaultUserProfile}objects using XML files for persistence. Passwords
 * are hashed using SHA1. User entries are simple &lt;user&gt; elements under
 * the root. User profile properties are attributes of the element. For example:
 * <users><user loginName="janne" fullName="Janne Jalkanen"
 * wikiName="JanneJalkanen" email="janne@ecyrd.com"
 * password="{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee"/> </users> Passwords
 * are hashed without salt. In this example, the password is "myP@5sw0rd".
 * @author Andrew Jaquith
 * @since 2.3
 */
public class XMLUserDatabase implements UserDatabase
{

    public static final String    PROP_USERDATABASE = "jspwiki.xmlUserDatabaseFile";

    protected static final String EMAIL             = "email";

    protected static final String FULL_NAME         = "fullName";

    protected static final String LOGIN_NAME        = "loginName";

    protected static final String PASSWORD          = "password";

    protected static final String SHA_PREFIX        = "{SHA}";

    protected static final String USER_TAG          = "user";

    protected static final String WIKI_NAME         = "wikiName";

    protected Document            c_dom             = null;

    protected File                c_file            = null;

    /**
     * Persists the database. The profile changed is looked up by the user ID,
     * which must exist.
     */
    public synchronized void commit()
    {
        if ( c_dom == null ) { throw new IllegalStateException( "FATAL: database doesn't exist in memory" ); }

        // First, neaten up the DOM by adding carriage returns before each
        // element
        Element root = c_dom.getDocumentElement();
        NodeList nodes = root.getChildNodes();
        for( int i = 0; i < nodes.getLength(); i++ )
        {
            Node node = nodes.item( i );
            if ( node instanceof Element )
            {
                Node previous = node.getPreviousSibling();
                if ( previous == null || previous.getNodeType() != Node.TEXT_NODE )
                {
                    Node whitespace = c_dom.createTextNode( "\n  " );
                    c_dom.getDocumentElement().insertBefore( whitespace, node );
                }
                // Add a return after the last element
                if ( i == ( nodes.getLength() - 1 ) )
                {
                    if ( node.getNodeType() != Node.TEXT_NODE )
                    ;
                    Node whitespace = c_dom.createTextNode( "\n" );
                    c_dom.getDocumentElement().appendChild( whitespace );
                }
            }
        }

        // Now, save to disk
        try
        {
            File newFile = new File( c_file.getAbsolutePath() + ".new" );
            Source source = new DOMSource( c_dom );
            System.out.println( source.toString() );
            Result result = new StreamResult( newFile );
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform( source, result );
        }
        catch( TransformerConfigurationException e )
        {
            System.err.println( e.getMessage() );
        }
        catch( TransformerException e )
        {
            System.err.println( e.getMessage() );
        }

        // Copy new file over old version
        File newFile = new File( c_file.getAbsolutePath() + ".new" );
        File backup = new File( c_file.getAbsolutePath() + ".old" );
        if ( backup.exists() )
        {
            if ( !backup.delete() ) { throw new IllegalStateException( "Could not delete old backup: " + backup ); }
        }
        if ( !c_file.renameTo( backup ) ) { throw new IllegalStateException( "Could not create backup: " + backup ); }
        if ( !newFile.renameTo( c_file ) )
        {
            System.err.println( "Could not save database: " + backup + " restoring backup." );
            if ( backup.renameTo( c_file ) ) { throw new IllegalStateException(
                    "Restore failed. Check the file permissions." ); }
            throw new IllegalStateException( "Could not save database: " + c_file + ". Check the file permissions" );
        }
    }

    
    
    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that whose login name, full name, or wiki name matches the supplied string.
     * This method provides a "forgiving" search algorithm for resolving
     * principal names when the exact profile attribute that supplied the name
     * is unknown.
     * @param index the login name, full name, or wiki name
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#find(java.lang.String)
     */
    public UserProfile find( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile;
        profile = findByAttribute( FULL_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        profile = findByAttribute( WIKI_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        profile = findByAttribute( LOGIN_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException("Not in database: " + index);
    }
    
    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that matches a profile having a given e-mail address. If the user
     * database does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the e-mail address of the desired user profile
     * @return the user profile
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByEmail(String)
     */
    public UserProfile findByEmail( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = findByAttribute( EMAIL, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException("Not in database: " + index);
    }

    /**
     * Looks up and returns the first {@link UserProfile} in the user database
     * that matches a profile having a given full name.
     * If the user database does not contain a user with a matching attribute,
     * throws a {@link NoSuchPrincipalException}.
     * @param index the fill name of the desired user profile
     * @return the user profile
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByFullName(java.lang.String)
     */
    public UserProfile findByFullName( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = findByAttribute( FULL_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException("Not in database: " + index);
    }

    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that matches a profile having a given login name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the login name of the desired user profile
     * @return the user profile
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByLoginName(java.lang.String)
     */
    public UserProfile findByLoginName( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = findByAttribute( LOGIN_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException("Not in database: " + index);
    }

    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that matches a profile having a given wiki name. If the user database
     * does not contain a user with a matching attribute, throws a
     * {@link NoSuchPrincipalException}.
     * @param index the wiki name of the desired user profile
     * @return the user profile
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByWikiName(java.lang.String)
     */
    public UserProfile findByWikiName( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = findByAttribute( WIKI_NAME, index );
        if ( profile != null )
        {
            return profile;
        }
        throw new NoSuchPrincipalException("Not in database: " + index);
    }

    /**
     * Looks up the Principals representing a user from the user database.
     * These are defined as a set of WikiPrincipals manufactured from
     * the login name, full name, and wiki name.
     * If the user database does not contain a user with the supplied identifier,
     * throws a {@link NoSuchPrincipalException}.
     * @param name the name of the principal to retrieve; this corresponds
     * to value returned by the user profile's {@link UserProfile#getLoginName()} method.
     * @return the array of Principals representing the user
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#getPrincipals(java.lang.String)
     */
    public Principal[] getPrincipals( String identifier ) throws NoSuchPrincipalException
    {
        try
        {
            UserProfile profile = findByLoginName( identifier );
            ArrayList principals = new ArrayList();
            if ( profile.getLoginName() != null && profile.getLoginName().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getLoginName() ) );
            }
            if ( profile.getFullname() != null && profile.getFullname().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getFullname() ) );
            }
            if ( profile.getWikiName() != null && profile.getWikiName().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getWikiName() ) );
            }
            return (Principal[])principals.toArray(new Principal[principals.size()]);
        }
        catch( NoSuchPrincipalException e )
        {
            throw e;
        }
    }

    /**
     * Initializes the user database based on values from a Properties object.
     * The properties object must contain a file path to the XML database file
     * whose key is {@link #PROP_USERDATABASE}.
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     */
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException
    {
        // Get database file location
        String file = props.getProperty( PROP_USERDATABASE );
        if ( file == null ) { throw new IllegalStateException( "Could not initialize user database; property "
                + PROP_USERDATABASE + " not found" ); }
        c_file = new File( file );

        // Read DOM
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating( false );
        factory.setExpandEntityReferences( false );
        factory.setIgnoringComments( true );
        factory.setNamespaceAware( false );
        try
        {
            c_dom = factory.newDocumentBuilder().parse( c_file );
            System.out.println( "User database successfully initialized." );
        }
        catch( ParserConfigurationException e )
        {
            System.err.println( "Configuration error: " + e.getMessage() );
        }
        catch( SAXException e )
        {
            System.err.println( "SAX error: " + e.getMessage() );
        }
        catch( IOException e )
        {
            System.err.println( "IO error: " + e.getMessage() );
        }
        if ( c_dom == null )
        {
            try
            {
                System.out.println( "Creating in-memory DOM." );
                c_dom = factory.newDocumentBuilder().newDocument();
            }
            catch( ParserConfigurationException e )
            {
                System.err.println( "FATAL: could not create in-memory DOM" );
            }
        }
    }

    /**
     * Saves a {@link UserProfile}to the user database, overwriting the
     * existing profile if it exists. The user name under which the profile
     * should be saved is returned by the supplied profile's
     * {@link UserProfile#getLoginName()}method.
     * @param profile the user profile to save
     * @throws WikiSecurityException if the profile cannot be saved
     */
    public void save( UserProfile profile ) throws WikiSecurityException
    {
        if ( c_dom == null ) { throw new IllegalStateException( "FATAL: database does not exist" ); }
        String index = profile.getLoginName();
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        Element user = null;
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element currentUser = (Element) users.item( i );
            if ( currentUser.getAttribute( LOGIN_NAME ).equals( index ) )
            {
                user = currentUser;
            }
        }
        if ( user == null )
        {
            System.out.println( "Creating new user " + index );
            user = c_dom.createElement( USER_TAG );
            c_dom.getDocumentElement().appendChild( user );
        }
        setAttribute( user, LOGIN_NAME, profile.getLoginName() );
        setAttribute( user, FULL_NAME, profile.getFullname() );
        setAttribute( user, WIKI_NAME, profile.getWikiName() );
        setAttribute( user, EMAIL, profile.getEmail() );

        // Hash and save the new password if it's different from old one
        String newPassword = profile.getPassword();
        if ( newPassword != null && !newPassword.equals("") )
        {
            String oldPassword = user.getAttribute( PASSWORD );
            if ( !oldPassword.equals( newPassword ) )
            {
                setAttribute( user, PASSWORD, SHA_PREFIX + getHash( newPassword ) );
            }
        }

    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#validatePassword(java.lang.String,
     *      java.lang.String)
     */
    public boolean validatePassword( String loginName, String password )
    {
        String hashedPassword = getHash( password );
        try
        {
            UserProfile profile = findByLoginName( loginName );
            String storedPassword = profile.getPassword();
            if ( storedPassword.startsWith( SHA_PREFIX ) )
            {
                storedPassword = storedPassword.substring( SHA_PREFIX.length() );
            }
            return ( hashedPassword.equals( storedPassword ) );
        }
        catch( NoSuchPrincipalException e )
        {
            return false;
        }
    }

    /**
     * Private method that returns the first {@link UserProfile}matching a
     * &lt;user&gt; element's supplied attribute.
     * @param matchAttribute
     * @param index
     * @return the profile, or <code>null</code> if not found
     */
    private UserProfile findByAttribute( String matchAttribute, String index )
    {
        if ( c_dom == null ) { throw new IllegalStateException( "FATAL: database does not exist" ); }
        NodeList users = c_dom.getElementsByTagName( USER_TAG );
        for( int i = 0; i < users.getLength(); i++ )
        {
            Element user = (Element) users.item( i );
            if ( user.getAttribute( matchAttribute ).equals( index ) )
            {
                UserProfile profile = new DefaultUserProfile();
                profile.setLoginName( user.getAttribute( LOGIN_NAME ) );
                profile.setFullname( user.getAttribute( FULL_NAME ) );
                profile.setWikiName( user.getAttribute( WIKI_NAME ) );
                profile.setPassword( user.getAttribute( PASSWORD ) );
                profile.setEmail( user.getAttribute( EMAIL ) );
                return profile;
            }
        }
        return null;
    }

    /**
     * Private method that calculates the SHA-1 hash of a given
     * <code>String</code>
     * @param text the text to hash
     * @return the result hash
     */
    private String getHash( String text )
    {
        String hash = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance( "SHA" );
            md.update( text.getBytes() );
            byte digestedBytes[] = md.digest();
            hash = HexUtils.convert( digestedBytes );
        }
        catch( NoSuchAlgorithmException e )
        {
            System.out.println( "Error creating hash!" );
            hash = text;
        }
        return hash;
    }

    /**
     * Private method that sets an attibute value for a supplied DOM element.
     * @param element the element whose attribute is to be set
     * @param attribute the name of the attribute to set
     * @param value the desired attribute value
     */
    private void setAttribute( Element element, String attribute, String value )
    {
        if ( value != null )
        {
            element.setAttribute( attribute, value );
        }
    }
}