<%@ page import="java.security.Principal" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.NoSuchPrincipalException" %>
<%@ page import="com.ecyrd.jspwiki.auth.WikiSecurityException" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.AnonymousLoginModule" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.UserDatabase" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.UserProfile" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>

<%
    WikiContext wikiContext = wiki.createContext( request, WikiContext.PREFS );
    String pagereq = wikiContext.getPage().getName();
    AuthenticationManager mgr = wiki.getAuthenticationManager();
    
    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String ok = request.getParameter("ok");
    String clear = request.getParameter("clear");
    String email = request.getParameter("email");
    String fullname = request.getParameter("fullname");
    String loginname = request.getParameter("loginname");
    String password = request.getParameter("password");
    String wikiname = request.getParameter("wikiname");

    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        Principal user = wikiContext.getCurrentUser();
        UserDatabase database = wiki.getUserDatabase();
        UserProfile profile = null;
        boolean newProfile = false;
        try
        {
            profile = database.find( user.getName() );
        }
        catch ( NoSuchPrincipalException e )
        {
            newProfile = true;
        }
        pageContext.setAttribute( "newProfile", new Boolean( newProfile ) );
        
        // Existing profiles can't change the loginname, fullname, or wiki name
        ArrayList inputErrors = new ArrayList();
        if ( newProfile )
        {
            if ( fullname == null || fullname.length() < 1 )
            {
              inputErrors.add("Full name cannot be blank");
            }
            if (loginname == null || loginname.length() < 1 )
            {
              inputErrors.add("Login name cannot be blank");
            }
            if (wikiname == null || wikiname.length() < 1 )
            {
              inputErrors.add("Wiki name cannot be blank");
            }
        }
        
        // Passwords for new accounts cannot be null
        // ARJ: TODO: we don't check for the policy yet, but we should..
        if (newProfile && password == null)
        {
              inputErrors.add("Password cannot be blank");
        }
        
        // It's ok if the e-mail is null. Not everybody wants to supply this...
        if (email != null || email.length() < 1 )
        {
            profile.setEmail( email );
        }
        
        // Set the rest of the profile properties
        profile.setFullname( fullname );
        profile.setLoginName( loginname );
        profile.setPassword( password );
        profile.setWikiName( wikiname );
        pageContext.setAttribute("inputErrors", inputErrors);
        
        // Save the profile now & log in the user!
        try
        {
            database.save( profile );
            database.commit();
            mgr.logout( session );
            if ( mgr.loginCustom( loginname, password, request ) )
            {
                AnonymousLoginModule.setUserCookie( response, loginname );
            }
        }
        catch( WikiSecurityException e )
        {
          // Something went horribly wrong! Maybe it's an I/O error...
        }

        response.sendRedirect( wiki.getBaseURL()+"UserPreferences.jsp" );
    }
    else if( clear != null )
    {
        mgr.logout( session );
        AnonymousLoginModule.clearUserCookie( response );
        response.sendRedirect( wiki.getBaseURL()+"UserPreferences.jsp" );
    }       
    else
    {
        response.setContentType("text/html; charset="+wiki.getContentEncoding() );
        String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                                wikiContext.getTemplate(),
                                                                "ViewTemplate.jsp" );
%>

        <wiki:Include page="<%=contentPage%>" />

<%
    } // Else
    NDC.pop();
    NDC.remove();
%>

