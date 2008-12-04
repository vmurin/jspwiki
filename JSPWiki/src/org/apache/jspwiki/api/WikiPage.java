/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.   
 */
package org.apache.jspwiki.api;

import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import com.ecyrd.jspwiki.auth.acl.Acl;

/**
 *  A WikiPage represents a Node in the repository.  Unlike in
 *  JSPWiki 2.x, a WikiPage also represents an attachment - in fact,
 *  there is no difference between a WikiPage and an attachment, only
 *  that they hold different ContentTypes.
 */
public interface WikiPage
{   
    // FIXME: Need to check which ones of these actually do have a good representation in JCR
    public static final String ATTR_AUTHOR = "wiki:author";
    public static final String ATTR_CONTENT = "wiki:content";
    public static final String ATTR_CONTENTTYPE = "wiki:contentType";
    public static final String ATTR_LASTMODIFIED = "wiki:lastModified";
    
    /**
     *  Returns the name of the page.
     *  
     *  @return The page name.
     */
    public String getName();

    /**
     *  Easy accessor to the wiki:contentType attribute.  For example, jspwiki code
     *  shall return "text/x-jspwiki".
     *  FIXME: The default MIME type is probably not correct. 
     *  @return The Wiki Content type.
     * @throws WikiException 
     */
    // NEW
    public String getContentType() throws WikiException;
    
    /**
     *  Easy accessor to setting the wiki:contentType attribute.
     * @throws WikiException 
     */
    // NEW
    public void setContentType( String contentType ) throws WikiException;
    
    /**
     *  Returns the content of the page as a stream.
     * @throws WikiException 
     */
    // NEW
    public InputStream getContentAsStream() throws WikiException;
    
    /**
     *  Returns the content as a string, if it can be construed as a string.
     * 
     *  @return A string.
     * @throws WikiException 
     */
    //NEW
    public String getContentAsString() throws WikiException;
    
    /**
     *  Set the page content.  This is a shortcut to setting a property "wiki:content".
     *  
     *  @param content The page content as a String.
     * @throws WikiException 
     */
    // NEW
    public void setContent( String content ) throws WikiException;
    
    /**
     *  Set the page content from an input stream.
     *  
     *  @param in The inputstream to read from.
     * @throws WikiException 
     */
    // NEW
    public void setContent( InputStream in ) throws WikiException;
    
    /**
     *  Returns the referrers (that is, those pages which reference this page in any form).
     *  Each String is a path to the WikiPage.
     */
    // NEW
    public Set<String> getReferrers();
    
    /**
     *  A WikiPage may have a number of attributes, which might or might not be 
     *  available.  Typically attributes are things that do not need to be stored
     *  with the wiki page to the page repository, but are generated
     *  on-the-fly.  A provider is not required to save them, but they
     *  can do that if they really want.
     *  
     *  @param key The key using which the attribute is fetched
     *  @return The attribute.  If the attribute has not been set, returns null.
     * @throws WikiException 
     */
    // TODO: Should this also work with the old SET -attributes?
    // FIXME: Need to define the relationship between JCR Properties and these.
    public Object getAttribute( String key ) throws WikiException;

    /**
     *  Sets an metadata attribute.  When the page is stored, so are the attributes.
     *  
     *  @see #getAttribute(String)
     *  @param key The key for the attribute used to fetch the attribute later on.
     *  @param attribute The attribute value
     */
    public void setAttribute( String key, Object attribute ) throws WikiException;

    /**
     * Returns the full attributes Map, in case external code needs
     * to iterate through the attributes.
     * 
     * @return The attribute Map.  Please note that this is a direct
     *         reference, not a copy.
     */
    // FIXME: This cannot be performed with JCR with this signature
    //public Map<String,Object> getAttributes();

    /**
     *  Removes an attribute from the page, if it exists.
     *  
     *  @param  key The key for the attribute
     *  @return If the attribute existed, returns the object.
     *  @since 2.1.111
     */
    public Object removeAttribute( String key ) throws WikiException;

    /**
     *  Returns the date when this page was last modified.
     *  
     *  @return The last modification date
     * @throws WikiException 
     */
    public Date getLastModified() throws WikiException;

    /**
     *  Sets the last modification date.  In general, this is only
     *  changed by the provider.
     *  
     *  @param date The date
     */
    public void setLastModified( Date date ) throws WikiException;

    /**
     *  Returns the version that this WikiPage instance represents.
     *  
     *  @return the version number of this page.
     */
    public int getVersion();

    /**
     *  Returns the size of the page.
     *  
     *  @return the size of the page. 
     *  @since 2.1.109
     */
    public long getSize();

    /**
     *  Returns the Acl for this page.  May return <code>null</code>, 
     *  in case there is no Acl defined, or it has not
     *  yet been set.
     *  
     *  @return The access control list.  May return null, if there is 
     *          no acl.
     */
    public Acl getAcl();


    /**
     *  Returns author name, or null, if no author has been defined.
     *  
     *  @return Author name, or possibly null.
     * @throws WikiException 
     */
    public String getAuthor() throws WikiException;
    
    /**
     *  Returns the wiki nanme for this page
     *  
     *  @return The name of the wiki.
     */
    public String getWiki();
    
    /**
     *  Compares a page with another.  The primary sorting order
     *  is according to page name, and if they have the same name,
     *  then according to the page version.
     *  
     *  @param o The object to compare against
     *  @return -1, 0 or 1
     */
    public int compareTo( Object o );
    
    /**
     *  {@inheritDoc}
     */
    public int hashCode();

    /** @throws WikiException 
     * @since 3.0 */
    // NEW
    public void save() throws WikiException;
}
