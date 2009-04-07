package org.apache.maven.mercury.artifact.api;

public class ConfigurationException
    extends Exception
{

    public ConfigurationException()
    {
    }

    public ConfigurationException( String message )
    {
        super( message );
    }

    public ConfigurationException( Throwable cause )
    {
        super( cause );
    }

    public ConfigurationException( String message, Throwable cause )
    {
        super( message, cause );
    }

}
