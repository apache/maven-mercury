package org.apache.maven.mercury.repository.local.map;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class StorageException
    extends Exception
{

    /**
     * 
     */
    public StorageException()
    {
    }

    /**
     * @param message
     */
    public StorageException( String message )
    {
        super( message );
    }

    /**
     * @param cause
     */
    public StorageException( Throwable cause )
    {
        super( cause );
    }

    /**
     * @param message
     * @param cause
     */
    public StorageException( String message, Throwable cause )
    {
        super( message, cause );
    }

}
