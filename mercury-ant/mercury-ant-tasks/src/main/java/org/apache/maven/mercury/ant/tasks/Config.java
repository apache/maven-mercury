package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.mercury.MavenDependencyProcessor;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.Util;

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
  Collection<Repo> _repos;
  
  Collection<Repository> _repositories;
  
  public Collection<Repository> getRepositories()
  throws MalformedURLException
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
        
        LocalRepositoryM2 r = new LocalRepositoryM2( repo.getId(), new File( repo._dir ), dp  );
        
        _repositories.add( r );
      }
      else
      {
        DependencyProcessor dp = new MavenDependencyProcessor();
        
        Server server = new Server( repo.getId(), new URL( repo._url ) );
        
        RemoteRepositoryM2 r  = new RemoteRepositoryM2( server, dp  );
        
        _repositories.add( r );
      }
    }

    return _repositories;
  }
  
  public Repo createRepo()
  {
    if( _repos == null )
    _repos = new ArrayList<Repo>(4);
    
    Repo r = new Repo();
    
    _repos.add( r );
    
    return r;
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
