/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.ecyrd.jspwiki.TextUtil;

/**
 *  This is a thin, basic, immutable Principal class.
 *
 *  @author Janne Jalkanen
 *  @since  2.2
 */
public class WikiPrincipal
    implements Principal
{

    public static final Principal GUEST = new WikiPrincipal("Guest");
   
    private final String m_name;

    public WikiPrincipal( String name )
    {
        m_name = name;
    }

    /**
     *  Returns the WikiName of the Principal.
     */
    public String getName()
    {
        return m_name;
    }

    public static Principal parseStringRepresentation( String res )
        throws NoSuchElementException
    {
        Principal principal = null;
    
        if( res != null && res.length() > 0 )
        {
            //
            //  Not all browsers or containers do proper cookie
            //  decoding, which is why we can suddenly get stuff
            //  like "username=3DJanneJalkanen", so we have to
            //  do the conversion here.
            //
            res = TextUtil.urlDecodeUTF8( res );
            StringTokenizer tok = new StringTokenizer( res, " ,=" );
            
            while( tok.hasMoreTokens() )
            {
                String param = tok.nextToken();
                String value = tok.nextToken();
                    
                if( param.equals("username") )
                {
                    principal = new WikiPrincipal( value );
                }
            }
        }
    
        return principal;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof WikiPrincipal)) {
           return false; 
        }
        return (m_name.equals(((WikiPrincipal)obj).getName()));
    }
    
    public String toString() {
        return "WikiPrincipal: " + getName();
    }

}
