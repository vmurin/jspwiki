package com.ecyrd.jspwiki.auth.acl;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.1 $ $Date: 2005-02-01 02:53:13 $
 */
public interface AclManager
{

    public void initialize( WikiEngine engine, Properties props );

    public Acl parseAcl( WikiPage page, String ruleLine ) throws WikiSecurityException;

}