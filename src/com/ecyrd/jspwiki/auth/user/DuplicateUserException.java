package com.ecyrd.jspwiki.auth.user;

/**
 * Exception indicating that an identical user already exists in the user database.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.1.2.1 $ $Date: 2005-02-27 06:58:04 $
 */
public final class DuplicateUserException extends Exception
{
    public DuplicateUserException(String message) {
        super( message );
    }
}
