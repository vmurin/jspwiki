<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="/WEB-INF/freshcookies.tld" prefix="freshcookies" %>
<freshcookies:checkCookies failureUrl="http://www.securitymetrics.org/content/CookieError.jsp" />

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    WikiEngine wiki;
%>

<%
    String skin    = wiki.safeGetParameter( request, "skin" );

    if( skin == null )
    {
        skin = wiki.getTemplateDir();
    }

    WikiContext wikiContext = wiki.createContext( request, WikiContext.VIEW );
    wikiContext.setHttpRequest( request );
 
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    //
    //  Alright, then start responding.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String contentPage = "templates/"+skin+"/LoginTemplate.jsp";
%>

  <wiki:Include page="<%=contentPage%>" />
