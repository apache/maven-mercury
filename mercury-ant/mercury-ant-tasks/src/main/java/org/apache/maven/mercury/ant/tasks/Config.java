package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.mercury.MavenDependencyProcessor;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Credentials;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
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
  private static final Language LANG = new DefaultLanguage( Config.class );
  
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
    {
      if( repo.isLocal() )
      {
        DependencyProcessor dp = new MavenDependencyProcessor();
        
        LocalRepositoryM2 r = new LocalRepositoryM2( repo.getId(), new File( repo._dir ), dp );
        
        _repositories.add( r );
      }
      else
      {
        DependencyProcessor dp = new MavenDependencyProcessor();
        
        Server server;
        try
        {
          server = new Server( repo.getId(), new URL( repo._url ) );
        }
        catch( MalformedURLException e )
        {
          throw new BuildException(e);
        }
        
        if( repo._authid != null )
        {
          Auth au = null;
          
          if( _auths == null )
            throw new BuildException( LANG.getMessage( "config.no.auths", repo._authid ) );
          
          for( Auth a : _auths )
            if( repo._authid.equals( a.getId() ) )
              au = a;
          
          if( au == null )
            throw new BuildException( LANG.getMessage( "config.no.auth.for.id", repo._authid ) );
          
          Credentials serverCred = createCredentials( au );
          
          server.setServerCredentials( serverCred );
        }
        
        if( repo._proxyauthid != null )
        {
          Auth au = null;
          
          if( _auths == null )
            throw new BuildException( LANG.getMessage( "config.no.proxy.auths", repo._proxyauthid ) );
          
          for( Auth a : _auths )
            if( repo._proxyauthid.equals( a.getId() ) )
              au = a;
          
          if( au == null )
            throw new BuildException( LANG.getMessage( "config.no.proxy.auth.for.id", repo._proxyauthid ) );
          
          Credentials proxyCred = createCredentials( au );
          
          server.setProxyCredentials( proxyCred );
        }
        
        RemoteRepositoryM2 r  = new RemoteRepositoryM2( server, dp );
        
        _repositories.add( r );
      }
    }

    return _repositories;
  }
  
  private static Credentials createCredentials( Auth a )
  {
    Credentials cred = null;
    
    if( a._certfile != null )
    {
      File cf = new File( a._certfile );
      if( ! cf.exists() )
        throw new BuildException( LANG.getMessage( "config.no.cert.file", a._certfile ) );
      
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

}
