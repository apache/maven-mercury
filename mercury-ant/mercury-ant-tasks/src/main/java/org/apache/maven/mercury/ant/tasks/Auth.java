package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.io.IOException;

import org.apache.maven.mercury.transport.api.Credentials;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class Auth
    extends AbstractDataType
{
    private static final Language LANG = new DefaultLanguage( Auth.class );
    
    private static final String DEFAULT_AUTH_ID = System.getProperty( "mercury.default.auth.id"
                                                                        , "mercury.default.auth.id."+System.currentTimeMillis() 
                                                                      ); 
    String _name;

    String _pass;

    String _certfile;

    public void setName( String name )
    {
        this._name = name;
    }

    public void setPass( String pass )
    {
        this._pass = pass;
    }

    public void setCertfile( String certfile )
    {
        this._certfile = certfile;
    }
    
    protected static Auth findAuth( Project project, String authId )
    {
        Object ao = authId == null ? project.getReference( DEFAULT_AUTH_ID ) : project.getReference( authId );

        if( ao == null )
            return null;
        
        return (Auth) ao;
    }

    protected Credentials createCredentials()
    {
        Credentials cred = null;

        if ( _certfile != null )
        {
            File cf = new File( _certfile );
            if ( !cf.exists() )
                throw new BuildException( LANG.getMessage( "config.no.cert.file", _certfile ) );

            try
            {
                cred = new Credentials( FileUtil.readRawData( cf ), _name, _pass );
            }
            catch ( IOException e )
            {
                throw new BuildException( e );
            }
        }
        else
            cred = new Credentials( _name, _pass );

        return cred;
    }
}
