package com.ecyrd.jspwiki.auth.authorize;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.Authorizer;


/**
 * 
 * @author Andrew Jaquith
 */
public interface GroupManager extends Authorizer {

  public void initialize(WikiEngine engine, Properties props);
  
  /**
   * Adds a Group to the group cache. Note that this method fail, and will
   * throw an <code>IllegalArgumentException</code>, if the proposed group
   * is the same name as one of the built-in Roles: e.g., Admin,
   * Authenticated, etc.
   * @param the Group to add
   * @see com.ecyrd.jspwiki.auth.authorize.GroupManager#add(DefaultGroup)
   */
  public void add(Group group);
  
  public void remove(Group group);
  
  public boolean exists(Group group);
  
  public Group find(String name);
  
  public void commit();
  
  public void reload();
  
}
