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
package org.apache.wiki.plugin;

import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.wiki.PageLock;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageAlreadyExistsException;
import org.apache.wiki.content.WikiName;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;


/**
 *  Builds a simple weblog.
 *
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>entrytext</b> - text of the link </li>
 *  <li><b>page</b> - if set, the entry is added to the named blog page. The default is the current page. </li>
 *  </ul>
 *  
 *  @since 1.9.21
 */
public class WeblogEntryPlugin implements WikiPlugin
{
    private static Logger     log = LoggerFactory.getLogger(WeblogEntryPlugin.class);

    private static final int MAX_BLOG_ENTRIES = 10000; // Just a precaution.

    /** Parameter name for setting the entrytext  Value is <tt>{@value}</tt>. */
    public static final String PARAM_ENTRYTEXT = "entrytext";
    /** 
     * Optional parameter: page that actually contains the blog.
     * This lets us provide a "new entry" link for a blog page 
     * somewhere else than on the page itself.
     */
    // "page" for uniform naming with WeblogPlugin...
    /** Parameter name for setting the page Value is <tt>{@value}</tt>. */
    public static final String PARAM_BLOGNAME = "page"; 

    /**
     *  Returns a new page name for entries.  It goes through the list of
     *  all blog pages, and finds out the next in line.
     *  
     *  @param engine A WikiEngine
     *  @param blogName The page (or blog) name.
     *  @return A new name.
     *  @throws ProviderException If something goes wrong.
     */
    public String getNewEntryPage( WikiEngine engine, String blogName )
        throws ProviderException
    {
        SimpleDateFormat fmt = new SimpleDateFormat(WeblogPlugin.DEFAULT_DATEFORMAT);
        String today = fmt.format( new Date() );
            
        int entryNum = findFreeEntry( engine.getContentManager(),
                                      blogName,
                                      today );

                        
        String blogPage = WeblogPlugin.makeEntryPage( blogName,
                                                      today,
                                                      ""+entryNum );

        return blogPage;
    }

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        ResourceBundle rb = context.getBundle(WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);
        
        String weblogName = (String)params.get( PARAM_BLOGNAME );
        if( weblogName == null )
        {
            weblogName = context.getPage().getName();
        }
        WikiEngine engine = context.getEngine();
        
        StringBuilder sb = new StringBuilder();

        String entryText = (String) params.get(PARAM_ENTRYTEXT);
        if( entryText == null ) 
            entryText = rb.getString("weblogentryplugin.newentry");
        
        String url = context.getURL( WikiContext.NONE, "NewBlogEntry.jsp", "page="+engine.encodeName(weblogName) );
            
        sb.append("<a href=\""+url+"\">"+entryText+"</a>");

        return sb.toString();
    }

    private int findFreeEntry( ContentManager mgr,
                               String baseName,
                               String date )
        throws ProviderException
    {
        Collection<WikiPage> everyone = mgr.getAllPages( null );
        int max = 0;

        String startString = WeblogPlugin.makeEntryPage( baseName, date, "" );
        
        for( WikiPage p : everyone )
        {
            if( p.getName().startsWith(startString) )
            {
                try
                {
                    String probableId = p.getName().substring( startString.length() );

                    int id = Integer.parseInt(probableId);

                    if( id > max )
                    {
                        max = id;
                    }
                }
                catch( NumberFormatException e )
                {
                    log.debug("Was not a log entry: "+p.getName() );
                }
            }
        }

        //
        //  Find the first page that has no page lock.
        //
        int idx = max+1;

        while( idx < MAX_BLOG_ENTRIES )
        {
            String name = WeblogPlugin.makeEntryPage( baseName, 
                                                      date, 
                                                      Integer.toString(idx) );
            WikiPage page;
            try
            {
                page = mgr.getEngine().createPage( WikiName.valueOf(name) );
            }
            catch( PageAlreadyExistsException e )
            {
                throw new ProviderException( e.getMessage(), e );
            }
                                          
            PageLock lock = mgr.getCurrentLock( page );

            if( lock == null )
            {
                break;
            }

            idx++;
        }
        
        return idx;
    }
    
}
