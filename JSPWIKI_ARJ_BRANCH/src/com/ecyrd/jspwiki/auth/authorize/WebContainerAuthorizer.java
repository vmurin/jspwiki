package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Authorizes users by delegating role membership checks to the 
 * servlet container.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.1 $ $Date: 2005-02-01 02:53:57 $
 */
public class WebContainerAuthorizer implements Authorizer {

  /**
   * Returns <code>true</code> if the
   * {@link javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)}
   * method also returns <code>true</code>. The servlet request object
   * is obtained by calling {@link com.ecyrd.jspwiki.WikiContext#getHttpRequest()}.
   * If <code>context</code> or <code>group</code> are 
   * <code>null</code>, this method will return <code>false</code>.
   * @parameter context the current wiki context
   * @parameter subject the subject to check. In this implementation,
   * the value of this parameter has no effect on the result, and may be <code>null</code>.
   * @parameter the wiki group (role) being tested for
   * @see com.ecyrd.jspwiki.auth.Authorizer#isUserInGroup(com.ecyrd.jspwiki.WikiContext, java.security.Principal, java.lang.String)
   */
  public boolean isUserInRole(WikiContext context, Subject subject,
      Principal role )
    {
        if ( context == null || role == null )
        {
            return false;
        }
        HttpServletRequest request = context.getHttpRequest();
        return request.isUserInRole( role.getName() );
    }

}