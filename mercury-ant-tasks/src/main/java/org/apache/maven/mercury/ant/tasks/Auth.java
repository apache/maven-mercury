package org.apache.maven.mercury.ant.tasks;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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

    private static final String DEFAULT_AUTH_ID =
        System.getProperty( "mercury.default.auth.id", "mercury.default.auth.id." + System.currentTimeMillis() );

    String _name;

    String _pass;

    String _certfile;

    public void setName( String name )
    {
        this._name = name;
    }

    // compatibility with old syntax
    public void setUsername( String name )
    {
        setName( name );
    }

    public void setPass( String pass )
    {
        this._pass = pass;
    }

    // compatibility with old syntax
    public void setPassword( String pass )
    {
        setPass( pass );
    }

    // compatibility with old syntax
    public void setPassphrase( String pass )
    {
        setPass( pass );
    }

    public void setCertfile( String certfile )
    {
        this._certfile = certfile;
    }

    // compatibility with old syntax
    public void setPrivateKey( String certfile )
    {
        setCertfile( certfile );
    }

    // compatibility with old syntax + case independence
    public void setPrivatekey( String certfile )
    {
        setCertfile( certfile );
    }

    protected static Auth findAuth( Project project, String authId )
    {
        Object ao = ( authId == null ) ? project.getReference( DEFAULT_AUTH_ID ) : project.getReference( authId );

        if ( ao == null )
        {
            return null;
        }

        return (Auth) ao;
    }

    protected Credentials createCredentials()
    {
        Credentials cred = null;

        if ( _certfile != null )
        {
            File cf = new File( _certfile );
            if ( !cf.exists() )
            {
                throw new BuildException( LANG.getMessage( "config.no.cert.file", _certfile ) );
            }

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
        {
            cred = new Credentials( _name, _pass );
        }

        return cred;
    }
}
