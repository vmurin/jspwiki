<%@page import="javax.servlet.http.Cookie" %>
<%
  // Kill the session
  session.invalidate();

  // Remove JSESSIONID in case it is still kicking around
  Cookie sessionCookie = new Cookie("JSESSIONID", null);
  sessionCookie.setMaxAge(0);
  
  // Redirect to the webroot
  response.addCookie(sessionCookie);
  response.sendRedirect(".");
%>
