package com.ecyrd.jspwiki.auth.user;

/**
 * Exception indicating that an identical user already exists in the user
 * database.
 * @author Andrew Jaquith
 * @version $Revision: 1.1.2.2 $ $Date: 2005-05-08 18:06:34 $
 * @since 2.3
 */
public final class DuplicateUserException extends Exception
{
    public DuplicateUserException( String message )
    {
        super( message );
    }
}