package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.mercury.MavenDependencyProcessor;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.crypto.api.StreamVerifierAttributes;
import org.apache.maven.mercury.crypto.api.StreamVerifierFactory;
import org.apache.maven.mercury.crypto.pgp.PgpStreamVerifierFactory;
import org.apache.maven.mercury.crypto.sha.SHA1VerifierFactory;
import org.apache.maven.mercury.repository.api.Repository;
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
    
    String _dir;

    String _url;

    String _type;

    String _authid;

    String _proxyauthid;

    boolean _readable = true;

    boolean _writeable = false;

    Auth _auth;

    Auth _proxyAuth;

    List<Verify> _writeVerifiers;

    List<Verify> _readVerifiers;

    transient boolean _managed = false;

    transient boolean _registered = false;

    public Repo()
    {
    }

    public Repo( boolean managed )
    {
        _managed = managed;
    }
    
    @Override
    public void setId( String id )
    {
        super.setId( id );
        
        if( _managed )
            return;
        
        Config.addDefaultRepository( getProject(), this );

    }

    public void setReadable( boolean readable )
    {
        this._readable = readable;
    }

    public void setWriteable( boolean writeable )
    {
        this._writeable = writeable;
    }

    public void setUrl( String url )
    {
        if( getId() == null )
            throw new IllegalArgumentException( LANG.getMessage( "repo.no.id", url ) );
        
        this._url = url;
    }

    public void setDir( String dir )
    {
        if( getId() == null )
            throw new IllegalArgumentException( LANG.getMessage( "repo.no.id", dir ) );
        
        this._dir = dir;
    }

    public void setType( String type )
    {
        this._type = type;
    }

    public void setAuthid( String authid )
    {
        this._authid = authid;
    }

    public void setProxyauthid( String proxyauthid )
    {
        this._proxyauthid = proxyauthid;
    }

    boolean isLocal()
    {
        return _dir != null;
    }

    public Verify createVerifywrite()
    {
        if ( _writeVerifiers == null )
            _writeVerifiers = new ArrayList<Verify>( 2 );

        Verify v = new Verify();

        _writeVerifiers.add( v );

        return v;
    }

    public Verify createVerifyread()
    {
        if ( _readVerifiers == null )
            _readVerifiers = new ArrayList<Verify>( 2 );

        Verify v = new Verify();

        _readVerifiers.add( v );

        return v;
    }

    private Set<StreamVerifierFactory> getVerifiers( List<Verify> vlist )
    {
        if ( Util.isEmpty( vlist ) )
            return null;

        Set<StreamVerifierFactory> facs = new HashSet<StreamVerifierFactory>( vlist.size() );

        for ( Verify v : vlist )
            facs.add( v.getVerifierFactory() );

        return facs;

    }

    public Repository getRepository()
    {
        Repository r = null;

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
            
            if( _auth != null )
            {
                au = _auth;
            }
            else if ( _authid != null )
            {
                au = Auth.findAuth( getProject(), _authid );

                if ( au == null )
                    throw new BuildException( LANG.getMessage( "config.no.auth.for.id", _authid ) );
            }
            
            if( au != null )
            {
                Credentials serverCred = au.createCredentials();
    
                server.setServerCredentials( serverCred );
                
                au = null;
            }
            
            if( _proxyAuth != null )
            {
                au = _proxyAuth;
            }
            else if ( _proxyauthid != null )
            {
                au = Auth.findAuth( getProject(), _proxyauthid );

                if ( au == null )
                    throw new BuildException( LANG.getMessage( "config.no.proxy.auth.for.id", _proxyauthid ) );
            }
            
            if( au != null )
            {
                Credentials proxyCred = au.createCredentials();

                server.setProxyCredentials( proxyCred );
            }

            r = new RemoteRepositoryM2( server, dp );
        }

        return r;
    }

    public Auth createAuth()
    {
        _auth = new Auth();

        return _auth;
    }

    public Auth createProxyauth()
    {
        _auth = new Auth();

        return _auth;
    }

    public class Verify
    extends AbstractDataType
{
    public static final String PGP = "pgp";

    public static final String SHA1 = "sha1";

    String _type;

    boolean _lenient = true;

    boolean _sufficient = false;

    Map<String, String> _properties;

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
            _properties = new HashMap<String, String>( 4 );

        _properties.put( property.getName(), property.getValue() );
    }

    public StreamVerifierFactory getVerifierFactory()
        throws BuildException
    {
        if ( _type == null )
            throw new BuildException( LANG.getMessage( "config.repo.verifier.no.type" ) );

        if ( _properties == null || _properties.isEmpty() )
            throw new BuildException( LANG.getMessage( "config.repo.verifier.no.properties", _type ) );

        if ( PGP.equals( _type ) )
        {
            String keyRing = _properties.get( "keyring" );

            if ( keyRing == null )
                throw new BuildException( LANG.getMessage( "config.repo.verifier.pgp.no.keyring" ) );

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

                if ( keyId == null || keyId.length() != 16 )
                    throw new BuildException( LANG.getMessage( "config.repo.verifier.pgp.bad.keyid", keyId,
                                                                keyRing ) );

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
        else if ( SHA1.equals( _type ) )
        {
            SHA1VerifierFactory fac =
                new SHA1VerifierFactory( new StreamVerifierAttributes( SHA1VerifierFactory.DEFAULT_EXTENSION,
                                                                       _lenient, _sufficient ) );

            return fac;
        }

        throw new BuildException( LANG.getMessage( "config.repo.verifier.bad.type", _type ) );
    }
}

}
