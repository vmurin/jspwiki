/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.ProviderException;
import org.apache.log4j.Category;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

/**
 *  Builds a simple weblog.
 *  <P>
 *  The pageformat can use the following params:<br>
 *  %p - Page name<br>
 *
 *  <B>Parameters</B>
 *  <UL>
 *    <LI>days - how many days the weblog aggregator should show.
 *    <LI>pageformat - What the entry pages should look like.
 *    <LI>startDate - Date when to start.  Format is "ddMMyy";
 *    <li>maxEntries - How many entries to show at most.
 *  </UL>
 *
 *  The "days" and "startDate" can also be sent in HTTP parameters,
 *  and the names are "weblog.days" and "weblog.startDate", respectively.
 *
 *  @since 1.9.21
 */

// FIXME: Add "entries" param as an alternative to "days".
// FIXME: Entries arrive in wrong order.

public class WeblogPlugin implements WikiPlugin
{
    private static Category     log = Category.getInstance(WeblogPlugin.class);

    public static final int     DEFAULT_DAYS = 7;
    public static final String  DEFAULT_PAGEFORMAT = "%p_blogentry_";

    public static final String  DEFAULT_DATEFORMAT = "ddMMyy";

    public static final String  PARAM_STARTDATE    = "startDate";
    public static final String  PARAM_DAYS         = "days";
    public static final String  PARAM_ALLOWCOMMENTS = "allowComments";
    public static final String  PARAM_MAXENTRIES   = "maxEntries";

    public static String makeEntryPage( String pageName,
                                        String date,
                                        String entryNum )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName)+date+"_"+entryNum;
    }

    public static String makeEntryPage( String pageName )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName);
    }

    public static String makeEntryPage( String pageName, String date )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName)+date;
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String weblogName = context.getPage().getName();
        Calendar   startTime;
        Calendar   stopTime;
        int        numDays;
        WikiEngine engine = context.getEngine();

        //
        //  Parse parameters.
        //
        String days;
        String startDay = null;
        boolean hasComments = false;
        int    maxEntries;

        if( (days = context.getHttpParameter( "weblog."+PARAM_DAYS )) == null )
        {
            days = (String) params.get( PARAM_DAYS );
        }

        numDays = TextUtil.parseIntParameter( days, DEFAULT_DAYS );


        if( (startDay = (String)params.get(PARAM_STARTDATE)) == null )
        {
            startDay = context.getHttpParameter( "weblog."+PARAM_STARTDATE );
        }

        if( TextUtil.isPositive( (String)params.get(PARAM_ALLOWCOMMENTS) ) )
        {
            hasComments = true;
        }

        maxEntries = TextUtil.parseIntParameter( (String)params.get(PARAM_MAXENTRIES),
                                                 Integer.MAX_VALUE );

        //
        //  Determine the date range which to include.
        //

        startTime = Calendar.getInstance();
        stopTime  = Calendar.getInstance();

        if( startDay != null )
        {
            SimpleDateFormat fmt = new SimpleDateFormat( DEFAULT_DATEFORMAT );
            try
            {
                Date d = fmt.parse( startDay );
                startTime.setTime( d );
                stopTime.setTime( d );
            }
            catch( ParseException e )
            {
                return "Illegal time format: "+startDay;
            }
        }

        //
        //  We make a wild guess here that nobody can do millisecond
        //  accuracy here.
        //
        startTime.add( Calendar.DAY_OF_MONTH, -numDays );
        startTime.set( Calendar.HOUR, 0 );
        startTime.set( Calendar.MINUTE, 0 );
        startTime.set( Calendar.SECOND, 0 );
        stopTime.set( Calendar.HOUR, 23 );
        stopTime.set( Calendar.MINUTE, 59 );
        stopTime.set( Calendar.SECOND, 59 );

        StringBuffer sb = new StringBuffer();
        
        try
        {
            List blogEntries = findBlogEntries( engine.getPageManager(),
                                                weblogName,
                                                startTime.getTime(),
                                                stopTime.getTime() );

            Collections.sort( blogEntries, new PageDateComparator() );

            SimpleDateFormat entryDateFmt = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

            sb.append("<div class=\"weblog\">\n");
            for( Iterator i = blogEntries.iterator(); i.hasNext() && maxEntries-- > 0 ; )
            {
                WikiPage p = (WikiPage) i.next();

                sb.append("<div class=\"weblogheading\">");

                Date entryDate = p.getLastModified();
                sb.append( entryDateFmt.format(entryDate) );

                sb.append("</div>\n");

                sb.append("<div class=\"weblogentry\">");

                //
                //  Append the text of the latest version.  Reset the
                //  context to that page.
                //

                WikiContext entryCtx = new WikiContext( engine, p );

                sb.append( engine.getHTML( entryCtx, 
                                           engine.getPage(p.getName()) ) );
                
                sb.append("</div>\n");

                String author = p.getAuthor();

                if( author != null )
                {
                    if( engine.pageExists(author) )
                    {
                        author = "<a href=\""+engine.getViewURL( author )+"\">"+author+"</a>";
                    }
                }
                else
                {
                    author = "AnonymousCoward";
                }

                sb.append("<div class=\"weblogpermalink\">");
                sb.append("By "+author+"&nbsp;&nbsp;");
                sb.append( "<a href=\""+engine.getViewURL(p.getName())+"\">Permalink</a>" );
                String commentPageName = TextUtil.replaceString( p.getName(),
                                                                 "blogentry",
                                                                 "comments" );

                if( engine.pageExists( commentPageName ) )
                {
                    sb.append( "&nbsp;&nbsp;" );
                    sb.append( "<a href=\""+engine.getViewURL(commentPageName)+"\">View Comments</a>" );
                }

                if( hasComments )
                {
                    sb.append( "&nbsp;&nbsp;" );
                    sb.append( "<a href=\""+engine.getEditURL(commentPageName)+"&comment=true\">Comment this entry</a>" );
                }

                sb.append("</div>");
            }
            
            sb.append("</div>\n");
        }
        catch( ProviderException e )
        {
            log.error( "Could not locate blog entries", e );
            throw new PluginException( "Could not locate blog entries: "+e.getMessage() );
        }

        return sb.toString();
    }

    /**
     *  Attempts to locate all pages that correspond to the 
     *  blog entry pattern.  Will only consider the days on the dates; not the hours and minutes.
     *
     *  Returns a list of pages with their FIRST revisions.
     */
    public List findBlogEntries( PageManager mgr,
                                 String baseName, Date start, Date end )
        throws ProviderException
    {
        Collection everyone = mgr.getAllPages();
        ArrayList  result = new ArrayList();

        baseName = makeEntryPage( baseName );
        SimpleDateFormat fmt = new SimpleDateFormat(DEFAULT_DATEFORMAT);
 
        for( Iterator i = everyone.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage)i.next();

            String pageName = p.getName();

            if( pageName.startsWith( baseName ) )
            {
                //
                //  Check the creation date from the page name.
                //  We do this because RCSFileProvider is very slow at getting a
                //  specific page version.
                //
                try
                {
                    //log.debug("Checking: "+pageName);
                    int firstScore = pageName.indexOf('_',baseName.length()-1 );
                    if( firstScore != -1 && firstScore+1 < pageName.length() )
                    {
                        int secondScore = pageName.indexOf('_', firstScore+1);

                        if( secondScore != -1 )
                        {
                            String creationDate = pageName.substring( firstScore+1, secondScore );

                            //log.debug("   Creation date: "+creationDate);

                            Date pageDay = fmt.parse( creationDate );
                
                            //
                            //  Add the first version of the page into the list.  This way
                            //  the page modified date becomes the page creation date.
                            //
                            if( pageDay != null && pageDay.after(start) && pageDay.before(end) )
                            {
                                WikiPage firstVersion = mgr.getPageInfo( pageName, 1 );
                                result.add( firstVersion );
                            }
                        }
                    }
                }
                catch( Exception e )
                {
                    log.debug("Page name :"+pageName+" was suspected as a blog entry but it isn't because of parsing errors",e);
                }
            }
        }
        
        return result;
    }

    /**
     *  Reverse comparison.
     */
    private class PageDateComparator implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            if( o1 == null || o2 == null ) 
            { 
                return 0; 
            }
            
            WikiPage page1 = (WikiPage)o1;
            WikiPage page2 = (WikiPage)o2;

            return page2.getLastModified().compareTo( page1.getLastModified() );
        }
    }
}
