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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.mercury.MavenDependencyProcessor;
import org.apache.maven.mercury.artifact.QualityRange;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.crypto.api.StreamVerifierAttributes;
import org.apache.maven.mercury.crypto.api.StreamVerifierFactory;
import org.apache.maven.mercury.crypto.pgp.PgpStreamVerifierFactory;
import org.apache.maven.mercury.crypto.sha.SHA1VerifierFactory;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryUpdatePolicy;
import org.apache.maven.mercury.repository.api.RepositoryUpdatePolicyFactory;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Credentials;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Property;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * repository data type
 * 
 * @author Oleg Gusakov
 * @version $Id$
 */
public class Repo
    extends AbstractDataType
{
    private static final Language LANG = new DefaultLanguage( Repo.class );

    private static final String DEFAULT_LAYOUT = "default";

    private String _dir;

    private String _url;

    private String _authid;

    private String _layout = DEFAULT_LAYOUT;

    private String _proxyauthid;

    private String _updatePolicy;

    private boolean _readable = true;

    private boolean _writeable = false;

    private boolean _releases = true;

    private boolean _snapshots = true;

    private Auth _auth;

    private Auth _proxyAuth;

    private List<Verify> _writeVerifiers;

    private List<Verify> _readVerifiers;

    private transient boolean _managed = false;

    private transient boolean _registered = false;

    private static final String[] SUPPORTED_LAYOUTS = new String[] { DEFAULT_LAYOUT, "m2", "flat" };
    
    private Validation _validation;
    

    public Repo()
    {
    }

    public Repo( boolean managed, Validation validation )
    {
        _managed = managed;
        
        _validation = validation;
    }

    private void processDefaults()
    {
        if ( _managed )
        {
            return;
        }

        if ( _registered )
        {
            return;
        }

        if ( getId() == null )
        {
            String id = "random." + (int) ( Math.random() * 10000. ) + ( "." + System.nanoTime() );

            super.setId( id );
        }

        Config.addDefaultRepository( getProject(), this );

        _registered = true;
    }

    @Override
    public void setId( String id )
    {
        super.setId( id );

        processDefaults();
    }

    public void setReleases( boolean releases )
    {
        this._releases = releases;

        processDefaults();
    }
    
    public void setSnapshots( boolean snapshots )
    {
        this._snapshots = snapshots;

        processDefaults();
    }

    public void setReadable( boolean readable )
    {
        this._readable = readable;

        processDefaults();
    }

    public void setWriteable( boolean writeable )
    {
        this._writeable = writeable;

        processDefaults();
    }

    public void setUrl( String url )
    {
        this._url = url;

        processDefaults();
    }

    public void setDir( String dir )
    {
        this._dir = dir;

        processDefaults();
    }

    // alternative - old - syntax
    public void setPath( String path )
    {
        setDir( path );
    }

    // alternative - Jason's - syntax
    public void setLocation( String path )
    {
        if ( FileUtil.isLocalResource( path ) )
        {
            setDir( path );
        }
        else
        {
            setUrl( path );
        }
    }
    
    public void setAuthentication( String auth )
    {
        Auth a = new Auth( auth );
        
        String authId = "auth."+System.currentTimeMillis()+"." + (int)(Math.random()*10000);
        
        getProject().addReference( authId, a );
        
        setAuthid( authId );
    }
    
    public void setAuthid( String authid )
    {
        this._authid = authid;

        processDefaults();
    }

    public void setProxyauthid( String proxyauthid )
    {
        this._proxyauthid = proxyauthid;

        processDefaults();
    }

    public void setLayout( String layout )
    {
        if ( layout == null )
        {
            throw new IllegalArgumentException( LANG.getMessage( "repo.null.layout" ) );
        }

        boolean notSupported = true;

        int len = layout.length();

        for ( String l : SUPPORTED_LAYOUTS )
        {
            if ( l.regionMatches( 0, layout, 0, len ) )
            {
                notSupported = false;
                break;
            }
        }

        if ( notSupported )
        {
            throw new IllegalArgumentException( LANG.getMessage( "repo.layout.not.supported", layout ) );
        }

        processDefaults();
    }
    
    public void setUpdatePolicy( String updatePolicy )
    {
        this._updatePolicy = updatePolicy;

        processDefaults();
    }

    boolean isLocal()
    {
        return ( _dir != null );
    }

//    public Verify createVerifywrite()
//    {
//        if ( _writeVerifiers == null )
//        {
//            _writeVerifiers = new ArrayList<Verify>( 2 );
//        }
//
//        Verify v = new Verify();
//
//        _writeVerifiers.add( v );
//
//        return v;
//    }

//    public Verify createVerifyread()
//    {
//        if ( _readVerifiers == null )
//        {
//            _readVerifiers = new ArrayList<Verify>( 2 );
//        }
//
//        Verify v = new Verify();
//
//        _readVerifiers.add( v );
//
//        return v;
//    }

    private Set<StreamVerifierFactory> getVerifiers( List<Verify> vlist )
    {
        if ( Util.isEmpty( vlist ) )
        {
            return null;
        }

        Set<StreamVerifierFactory> facs = new HashSet<StreamVerifierFactory>( vlist.size() );

        for ( Verify v : vlist )
        {
            facs.add( v.getVerifierFactory() );
        }

        return facs;

    }

    public Repository getRepository()
    {
        Repository r = null;
        
        updateReadVerifiers( _validation );
        
        updateWriteVerifiers( _validation );
        
        String id = getId();
        
        if( id == null )
            setId( "temp."+System.currentTimeMillis()+"." + (int)( Math.random()*10000. ) );

        if ( isLocal() )
        {
            DependencyProcessor dp = new MavenDependencyProcessor();

            Server server;
            try
            {
                server = new Server( getId(), new File( _dir ).toURL() );

                server.setReaderStreamVerifierFactories( getVerifiers( _readVerifiers ) );
                server.setWriterStreamVerifierFactories( getVerifiers( _writeVerifiers ) );
                    
            }
            catch ( MalformedURLException e )
            {
                throw new BuildException( e );
            }

            r = new LocalRepositoryM2( server, dp );
            
            QualityRange qr = QualityRange.create( _releases, _snapshots );
            
            r.setRepositoryQualityRange( qr );
        }
        else
        {
            DependencyProcessor dp = new MavenDependencyProcessor();

            Server server;
            try
            {
                server = new Server( getId(), new URL( _url ) );

                server.setReaderStreamVerifierFactories( getVerifiers( _readVerifiers ) );
                server.setWriterStreamVerifierFactories( getVerifiers( _writeVerifiers ) );
            }
            catch ( MalformedURLException e )
            {
                throw new BuildException( e );
            }

            Auth au = null;

            if ( _auth != null )
            {
                au = _auth;
            }
            else if ( _authid != null )
            {
                au = Auth.findAuth( getProject(), _authid );

                if ( au == null )
                {
                    throw new BuildException( LANG.getMessage( "config.no.auth.for.id", _authid ) );
                }
            }

            if ( au != null )
            {
                Credentials serverCred = au.createCredentials();

                server.setServerCredentials( serverCred );

                au = null;
            }

            if ( _proxyAuth != null )
            {
                au = _proxyAuth;
            }
            else if ( _proxyauthid != null )
            {
                au = Auth.findAuth( getProject(), _proxyauthid );

                if ( au == null )
                {
                    throw new BuildException( LANG.getMessage( "config.no.proxy.auth.for.id", _proxyauthid ) );
                }
            }

            if ( au != null )
            {
                Credentials proxyCred = au.createCredentials();

                server.setProxyCredentials( proxyCred );
            }

            r = new RemoteRepositoryM2( server, dp );
            
            _updatePolicy = System.getProperty( RepositoryUpdatePolicy.SYSTEM_PROPERTY_UPDATE_POLICY, _updatePolicy );
            
            if( !Util.isEmpty( _updatePolicy) )
                ((RemoteRepositoryM2)r).setUpdatePolicy( RepositoryUpdatePolicyFactory.create( _updatePolicy ) );
            
            QualityRange qr = QualityRange.create( _releases, _snapshots );
            
            r.setRepositoryQualityRange( qr );
        }

        return r;
    }

    /**
     * @param validation
     */
    private void updateReadVerifiers( Validation validation )
    {
        if( validation == null 
            ||
            (
              validation._sha1Validation == false
              && 
              validation._pgpValidation == false
            ) 
        )
            return;

        if( _readVerifiers == null )
            _readVerifiers = new ArrayList<Verify>(2);
        
        if( validation._sha1Validation )
        {
            Verify v = new Verify( Validation.TYPE_SHA1 );
            
            _readVerifiers.add( v );
        }
        
        if( validation._pgpValidation )
        {
            Verify v = new Verify( Validation.TYPE_PGP, prop("keyring", validation._pgpPublicKeyring ) );
            
            _readVerifiers.add( v );
        }
            
    }

    /**
     * @param validation
     */
    private void updateWriteVerifiers( Validation validation )
    {
        if( validation == null 
            ||
            (
            validation._sha1Signature == false
            &&
            Util.isEmpty( validation._pgpSecretKeyPass ) 
            ) 
        )
            return;

        if( _writeVerifiers == null )
            _writeVerifiers = new ArrayList<Verify>(2);
        
        if( validation._sha1Signature )
        {
            Verify v = new Verify( Validation.TYPE_SHA1 );
            
            _writeVerifiers.add( v );
        }
        
        if( ! Util.isEmpty( validation._pgpSecretKeyPass ) )
        {
            Verify v = new Verify( Validation.TYPE_PGP
                                   , prop( "keyring", validation._pgpSecretKeyring )
                                   , prop( "pass",    validation._pgpSecretKeyPass )
                                   , prop( "key",     validation._pgpSecretKey )
                                 );
            
            _writeVerifiers.add( v );
        }
            
    }

    public Auth createAuth()
    {
        _auth = new Auth();

        return _auth;
    }

    public Auth createAuthentication()
    {
        return createAuth();
    }

    public Auth createProxyauth()
    {
        _auth = new Auth();

        return _auth;
    }

    public Auth createProxyauthentication()
    {
        return createProxyauth();
    }

    public Auth createProxyAuthentication()
    {
        return createProxyauth();
    }
    
    public static final Property prop( String name, String val )
    {
        Property prop = new Property();
        
        prop.setName( name );
        
        prop.setValue( val );
        
        return prop;
    }

    private class Verify
        extends AbstractDataType
    {
        String _type;

        boolean _lenient = true;

        boolean _sufficient = false;

        Map<String, String> _properties;
        
        public Verify( String type, Property... properties )
        {
            setType( type );
            
            if( properties != null )
                for( Property p : properties )
                    addConfiguredProperty( p );
        }

        /**
         * 
         */
        public Verify()
        {
            // TODO Auto-generated constructor stub
        }

        public void setType( String type )
        {
            this._type = type;
        }

        public void setLenient( boolean lenient )
        {
            this._lenient = lenient;
        }

        public void setSufficient( boolean sufficient )
        {
            this._sufficient = sufficient;
        }

        public void addConfiguredProperty( Property property )
        {
            if ( _properties == null )
            {
                _properties = new HashMap<String, String>( 4 );
            }

            _properties.put( property.getName(), property.getValue() );
        }

        public StreamVerifierFactory getVerifierFactory()
            throws BuildException
        {
            if ( _type == null )
            {
                throw new BuildException( LANG.getMessage( "config.repo.verifier.no.type" ) );
            }

            if ( Validation.TYPE_PGP.equals( _type ) )
            {

                if ( Util.isEmpty(  _properties ) )
                {
                    throw new BuildException( LANG.getMessage( "config.repo.verifier.no.properties", _type ) );
                }

                String keyRing = _properties.get( "keyring" );

                if ( keyRing == null )
                {
                    throw new BuildException( LANG.getMessage( "config.repo.verifier.pgp.no.keyring" ) );
                }

                String pass = _properties.get( "pass" );

                if ( pass == null ) // reader configuration
                {
                    try
                    {
                        PgpStreamVerifierFactory fac =
                            new PgpStreamVerifierFactory(
                                                          new StreamVerifierAttributes(
                                                                                        PgpStreamVerifierFactory.DEFAULT_EXTENSION,
                                                                                        _lenient, _sufficient ),
                                                          FileUtil.toStream( keyRing ) );
                        return fac;
                    }
                    catch ( Exception e )
                    {
                        throw new BuildException( e );
                    }
                }
                else
                // writer configuration
                {
                    String keyId = _properties.get( "key" );

                    if ( ( keyId == null ) || ( keyId.length() != 16 ) )
                    {
                        throw new BuildException(
                                                  LANG.getMessage( "config.repo.verifier.pgp.bad.keyid", keyId, keyRing ) );
                    }

                    try
                    {
                        PgpStreamVerifierFactory fac =
                            new PgpStreamVerifierFactory(
                                                          new StreamVerifierAttributes(
                                                                                        PgpStreamVerifierFactory.DEFAULT_EXTENSION,
                                                                                        _lenient, _sufficient ),
                                                          FileUtil.toStream( keyRing ), keyId, pass );
                        return fac;
                    }
                    catch ( Exception e )
                    {
                        throw new BuildException( e );
                    }

                }
            }
            else if ( Validation.TYPE_SHA1.equals( _type ) )
            {
                SHA1VerifierFactory fac =
                    new SHA1VerifierFactory( new StreamVerifierAttributes( SHA1VerifierFactory.DEFAULT_EXTENSION,
                                                                           _lenient, _sufficient ) );

                return fac;
            }

            throw new BuildException( LANG.getMessage( "config.repo.verifier.bad.type", _type ) );
        }
    }

    public void setSha1Validation( boolean val )
    {
        _validation._sha1Validation = val;
    }

    public void setSha1validation( boolean val )
    {
        setSha1Validation( val );
    }

    public void setPgpValidation( boolean val )
    {
        _validation._pgpValidation = val;
    }

    public void setPgpKeyring( String val )
    {
        setPgpValidation( true );
        _validation._pgpPublicKeyring = val;
    }

    public void setPgpkeyring( String val )
    {
        setPgpKeyring( val );
    }

    public void setPgpSecretKeyring( String val )
    {
        _validation._pgpSecretKeyring = val;
    }

    public void setPgpSecretkeyring( String val )
    {
        setPgpSecretKeyring( val );
    }

    public void setPgpSecretKey( String val )
    {
        _validation._pgpSecretKey = val;
    }

    public void setPgpSecretkey( String val )
    {
        setPgpSecretKey( val );
    }

    public void setPgpSecretKeyPass( String val )
    {
        _validation._pgpSecretKeyPass = val;
    }

    public void setPgpSecretKeypass( String val )
    {
        setPgpSecretKeyPass( val );
    }

}
