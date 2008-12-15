package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.metadata.DependencyBuilder;
import org.apache.maven.mercury.metadata.DependencyBuilderFactory;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.virtual.VirtualRepositoryReader;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class ResolveTask
extends AbstractAntTask
{
  private static final Language LANG = new DefaultLanguage( ResolveTask.class );
  
  public static final String TASK_NAME = LANG.getMessage( "resolve.task.name" );
  public static final String TASK_DESC = LANG.getMessage( "resolve.task.desc" );
  
  private boolean _transitive = true;
  
  private String _pathId;
  
  private String _refPathId;
  
  private String _configId;
  
  private String _depId;
  
  private ArtifactScopeEnum scope = ArtifactScopeEnum.compile;
  
  private boolean _failOnError = true;
  
  //----------------------------------------------------------------------------------------
  @Override
  public String getDescription()
  {
    return TASK_DESC;
  }

  @Override
  public String getTaskName()
  {
    return TASK_NAME;
  }
  //----------------------------------------------------------------------------------------
  @Override
  public void execute()
  throws BuildException
  {
    if( _configId == null )
    {
      throwIfEnabled( LANG.getMessage( "config.id.mandatory" ) );
      return;
    }
    
    Object so = getProject().getReference( _configId );

    if( so == null )
    {
      throwIfEnabled( LANG.getMessage( "config.id.object.null", _configId ) );
      return;
    }
    
    if( ! Config.class.isAssignableFrom( so.getClass() ) )
    {
      throwIfEnabled( LANG.getMessage( "config.id.object.wrong", _configId, so.getClass().getName() ) );
      return;
    }
    
    Config config = (Config)so;
    
    if( Util.isEmpty( _pathId ) && Util.isEmpty( _pathId ) )
    {
      throwIfEnabled( LANG.getMessage("no.path.ref") );
      return;
    }
    
    Dep dep = null;
    
    if( _depId == null )
    {
      throwIfEnabled( LANG.getMessage( "no.dep.id" ) );
      return;
    }
    
    Object d = getProject().getReference( _depId );
    
    if( d == null )
    {
      throwIfEnabled( LANG.getMessage( "no.dep", _depId ) );
      return;
    }
    
    if( ! Dep.class.isAssignableFrom( d.getClass() ) )
    {
      throwIfEnabled( LANG.getMessage( "bad.dep", _depId, d.getClass().getName(), Dep.class.getName() ) );
      return;
    }
    
    dep = (Dep)d;

    Path path = null;
    
    if( !Util.isEmpty( _pathId ) )
    {
      if( getProject().getReference( _pathId ) != null )
      {
        throwIfEnabled( LANG.getMessage( "path.exists", _pathId ) );
        return;
      }
    }
    else
    {
      Object p = getProject().getReference( _refPathId );

      if( p == null )
      {
        throwIfEnabled( LANG.getMessage( "no.path.ref", _refPathId ) );
        return;
      }

      path = (Path)p;
    }
    
    try
    {
      Collection<Repository> repos = config.getRepositories();
    
      DependencyBuilder db = DependencyBuilderFactory.create( DependencyBuilderFactory.JAVA_DEPENDENCY_MODEL
                                                            , repos
                                                            , null
                                                            , null
                                                            , null 
                                                            );
      List<ArtifactMetadata> res = db.resolveConflicts( scope, dep.getList() );
      
      if( Util.isEmpty( res ) )
        return;
      
      VirtualRepositoryReader vr = new VirtualRepositoryReader( repos );
      
      ArtifactResults aRes = vr.readArtifacts( res );
      
      if( aRes.hasExceptions() )
      {
        throwIfEnabled( LANG.getMessage( "vr.error", aRes.getExceptions().toString() ) );
        return;
      }
      
      if( ! aRes.hasResults() )
        return;
      
      Map<ArtifactBasicMetadata, List<Artifact>> resMap = aRes.getResults();
      
      FileList pathFileList = new FileList();
      
      File dir = null;
      
      for( ArtifactBasicMetadata key : resMap.keySet() )
      {
        List<Artifact> artifacts = resMap.get( key );

        if( !Util.isEmpty( artifacts ) )
          for( Artifact a : artifacts )
          {
            if( dir == null )
              dir = a.getFile().getParentFile();
            
            String aPath = a.getFile().getCanonicalPath();
            
            FileList.FileName fn = new FileList.FileName();
            
            fn.setName( aPath );
            
            pathFileList.addConfiguredFile( fn );
          }
      }
      
      pathFileList.setDir( dir );

      // now - the path
      if( path == null )
      {
        path = new Path( getProject(), _pathId );
        
        path.addFilelist( pathFileList );
        
        getProject().addReference( _pathId, path );
      }
      else
      {
        Path newPath = new Path( getProject() );

        newPath.addFilelist( pathFileList );
        
        path.append( newPath );
      }
      
    }
    catch( Exception e )
    {
      if( _failOnError )
        throw new BuildException( e.getMessage() );
      else 
        return;
    }
  }

  public void setConfigid( String configid )
  {
    this._configId = configid;
  }

  public void setTransitive( boolean val )
  {
    this._transitive = val;
  }

  public void setPathid( String pathId )
  {
    this._pathId = pathId;
  }

  public void setRefpathid( String refPathId )
  {
    this._refPathId = refPathId;
  }
  
  public void setDepid( String depid )
  {
    this._depId = depid;
  }

  public void setScope( ArtifactScopeEnum scope )
  {
    this.scope = scope;
  }
}