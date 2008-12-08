package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.mercury.MavenDependencyProcessor;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.LocalRepository;
import org.apache.maven.mercury.repository.api.Repository;
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
  Collection<AbstractRepository> _repositories;
  
  private void init()
  {
    if( _repositories != null )
      return;
    
    _repositories = new ArrayList<AbstractRepository>(4);
    
  }
  
  @Override
  public void setId( String id )
  {
    super.setId( id );
    
    getProject().addReference( id, this );
  }
  
  public Collection<Repository> getRepositories()
  throws MalformedURLException
  {
    if( Util.isEmpty( _repositories ) )
      return null;
    
    Collection<Repository> repos = new ArrayList<Repository>( _repositories.size() );
    
    for( AbstractRepository ar : _repositories )
    {
      if( LocalRepositoryM2.class.isAssignableFrom( ar.getClass() ) )
      {
        LocalRepositoryM2 lr = (LocalRepositoryM2)ar;
        
        DependencyProcessor dp = new MavenDependencyProcessor();
        
        org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2 r 
            = new org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2( lr.getId(), new File( lr.getPath() ), dp  );
        
        repos.add( r );
      }
      else
      {
        RemoteRepositoryM2 rr = (RemoteRepositoryM2)ar;
        
        DependencyProcessor dp = new MavenDependencyProcessor();
        
        Server server = new Server( rr.getId(), new URL( rr.getUrl() ) );
        
        org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2 r 
            = new org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2( server, dp  );
        
        repos.add( r );
      }
    }
    
    return repos;
  }
  
  public LocalRepositoryM2 createLocalRepositoryM2()
  {
    init();
    
    LocalRepositoryM2 r = new LocalRepositoryM2();
    
    _repositories.add( r );
    
    return r;
  }
  
  public class LocalRepositoryM2
  extends AbstractRepository
  {
    String _path;

    public String getPath()
    {
      return _path;
    }

    public void setPath( String path )
    {
      this._path = path;
    }
  }
  
  public RemoteRepositoryM2 createRemoteRepositoryM2()
  {
    init();
    
    RemoteRepositoryM2 r = new RemoteRepositoryM2();
    
    _repositories.add( r );
    
    return r;
  }

  public class RemoteRepositoryM2
  extends AbstractRepository
  {
    String _url;

    public String getUrl()
    {
      return _url;
    }

    public void setUrl(
        String url )
    {
      this._url = url;
    }
  }

}
