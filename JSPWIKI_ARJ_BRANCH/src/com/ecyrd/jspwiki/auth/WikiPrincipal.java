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
