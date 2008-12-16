package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.io.IOException;
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
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class Config
extends AbstractDataType
{
  private static final Language _lang = new DefaultLanguage( Config.class );
  
  Collection<Repo> _repos;
  
  Collection<Auth> _auths;
  
  Collection<Repository> _repositories;
  
  public Collection<Repository> getRepositories()
  throws BuildException
  {
    if( Util.isEmpty( _repos ) )
      return null;
    
    if( _repositories != null )
      return _repositories;
    
    _repositories = new ArrayList<Repository>( _repos.size() );
    
    for( Repo repo : _repos )
      _repositories.add( repo.getRepository() );

    return _repositories;
  }
  
  private static Credentials createCredentials( Auth a )
  {
    Credentials cred = null;
    
    if( a._certfile != null )
    {
      File cf = new File( a._certfile );
      if( ! cf.exists() )
        throw new BuildException( _lang.getMessage( "config.no.cert.file", a._certfile ) );
      
      try
      {
        cred = new Credentials( FileUtil.readRawData( cf ), a._name, a._pass );
      }
      catch( IOException e )
      {
        throw new BuildException(e);
      }
    }
    else
      cred = new Credentials( a._name, a._pass );
    
    return cred;
  }
  
  public Repo createRepo()
  {
    if( _repos == null )
    _repos = new ArrayList<Repo>(4);
    
    Repo r = new Repo();
    
    _repos.add( r );
    
    return r;
  }
  
  public Auth createAuth()
  {
    if( _auths == null )
    _auths = new ArrayList<Auth>(4);
    
    Auth a = new Auth();
    
    _auths.add( a );
    
    return a;
  }
  
  public class Repo
  extends AbstractDataType
  {
    String _dir;
    String _url;
    String _type;
    String _authid;
    String _proxyauthid;
    
    boolean _readable = true;
    boolean _writeable = false;
    
    List<Verify> _writeVerifiers;
    List<Verify> _readVerifiers;

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
      this._url = url;
    }

    public void setDir( String dir )
    {
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
      if( _writeVerifiers == null )
        _writeVerifiers = new ArrayList<Verify>(2);
      
      Verify v = new Verify();
      
      _writeVerifiers.add( v );
      
      return v;
    }
    
    public Verify createVerifyread()
    {
      if( _readVerifiers == null )
        _readVerifiers = new ArrayList<Verify>(2);
      
      Verify v = new Verify();
      
      _readVerifiers.add( v );
      
      return v;
    }

    private Set<StreamVerifierFactory> getVerifiers( List<Verify> vlist )
    {
      if( Util.isEmpty( vlist ) )
        return null;
      
      Set<StreamVerifierFactory> facs = new HashSet<StreamVerifierFactory>( vlist.size() );
      
      for( Verify v : vlist )
        facs.add( v.getVerifierFactory() );
      
      return facs;
      
    }
    
    public Repository getRepository()
    {
      Repository r = null;
      
      if( isLocal() )
      {
        DependencyProcessor dp = new MavenDependencyProcessor();
        
        Server server;
        try
        {
          server = new Server( getId(), new File( _dir ).toURL() );

          server.setReaderStreamVerifierFactories( getVerifiers( _readVerifiers ) );
          server.setWriterStreamVerifierFactories( getVerifiers( _writeVerifiers ) );
        }
        catch( MalformedURLException e )
        {
          throw new BuildException( e );
        }
        
        r = new LocalRepositoryM2( server, dp  );
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
        catch( MalformedURLException e )
        {
          throw new BuildException(e);
        }
        
        if( _authid != null )
        {
          Auth au = null;
          
          if( _auths == null )
            throw new BuildException( _lang.getMessage( "config.no.auths", _authid ) );
          
          for( Auth a : _auths )
            if( _authid.equals( a.getId() ) )
              au = a;
          
          if( au == null )
            throw new BuildException( _lang.getMessage( "config.no.auth.for.id", _authid ) );
          
          Credentials serverCred = createCredentials( au );
          
          server.setServerCredentials( serverCred );
        }
        
        if( _proxyauthid != null )
        {
          Auth au = null;
          
          if( _auths == null )
            throw new BuildException( _lang.getMessage( "config.no.proxy.auths", _proxyauthid ) );
          
          for( Auth a : _auths )
            if( _proxyauthid.equals( a.getId() ) )
              au = a;
          
          if( au == null )
            throw new BuildException( _lang.getMessage( "config.no.proxy.auth.for.id", _proxyauthid ) );
          
          Credentials proxyCred = createCredentials( au );
          
          server.setProxyCredentials( proxyCred );

        }
        

        server.setReaderStreamVerifierFactories( getVerifiers( _readVerifiers ) );
        server.setWriterStreamVerifierFactories( getVerifiers( _writeVerifiers ) );

        r  = new RemoteRepositoryM2( server, dp  );
        
      }
      
      return r;
    }
    
  }
  
  public class Auth
  extends AbstractDataType
  {
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
  }
  
  public class Verify
  extends AbstractDataType
  {
    public static final String PGP  = "pgp";
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
      if( _properties == null )
        _properties = new HashMap<String,String>(4);
      
      _properties.put( property.getName(), property.getValue() );
    }
    
    public StreamVerifierFactory getVerifierFactory()
    throws BuildException
    {
      if( _type == null )
        throw new BuildException( _lang.getMessage( "config.repo.verifier.no.type" ) );
      
      if( _properties == null || _properties.isEmpty() )
        throw new BuildException( _lang.getMessage( "config.repo.verifier.no.properties", _type ) );
      
      if( PGP.equals( _type ) )
      {
        String keyRing = _properties.get( "keyring" );
        
        if( keyRing == null )
          throw new BuildException( _lang.getMessage( "config.repo.verifier.pgp.no.keyring" ) );

        String pass = _properties.get( "pass" );
        
        if( pass == null ) // reader configuration
        {
          try
          {
            PgpStreamVerifierFactory fac =
                new PgpStreamVerifierFactory(
                    new StreamVerifierAttributes( PgpStreamVerifierFactory.DEFAULT_EXTENSION, _lenient, _sufficient )
                    , FileUtil.toStream( keyRing )
                                            );
            return fac;
          }
          catch( Exception e )
          {
            throw new BuildException( e );
          }
        }
        else // writer configuration
        {
          String keyId = _properties.get( "key" );
          
          if( keyId == null || keyId.length() != 16 )
            throw new BuildException( _lang.getMessage( "config.repo.verifier.pgp.bad.keyid", keyId, keyRing ) );
          
          try
          {
            PgpStreamVerifierFactory fac =
                new PgpStreamVerifierFactory(
                    new StreamVerifierAttributes( PgpStreamVerifierFactory.DEFAULT_EXTENSION, _lenient, _sufficient )
                    , FileUtil.toStream( keyRing ), keyId, pass
                                            );
            return fac;
          }
          catch( Exception e )
          {
            throw new BuildException( e );
          }
          
        }
      }
      else if( SHA1.equals( _type ) )
      {
        SHA1VerifierFactory fac = new SHA1VerifierFactory( new StreamVerifierAttributes( SHA1VerifierFactory.DEFAULT_EXTENSION, _lenient, _sufficient ) );

        return fac;
      }

      throw new BuildException( _lang.getMessage( "config.repo.verifier.bad.type", _type ) );
    }
  }

}
