package org.apache.maven.mercury.ant.tasks;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.metadata.DependencyBuilder;
import org.apache.maven.mercury.metadata.DependencyBuilderFactory;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
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
public class DependenciesTask
extends AbstractAntTask
{
  private static final Language _lang = new DefaultLanguage( DependenciesTask.class );
  
  public static final String TASK_NAME = _lang.getMessage( "dependencies.task.name" );
  public static final String TASK_DESC = _lang.getMessage( "dependencies.task.desc" );
  
  private List<Dependency> _dependencies;
  
  private boolean _transitive = true;
  
  private String _pathId;
  
  private String _refPathId;
  
  private String _settingsid;
  
  private boolean _failonerror = true;
  
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

  @Override
  public void execute()
  throws BuildException
  {
    if( _settingsid == null )
      if( _failonerror )
        throw new BuildException( _lang.getMessage( "settings.id.mandatory" ));
      else 
        return;
    
    Object so = getProject().getReference( _settingsid );

    if( so == null )
      if( _failonerror )
        throw new BuildException( _lang.getMessage( "settings.id.object.null", _settingsid ));
      else 
        return;
    
    if( ! Config.class.isAssignableFrom( so.getClass() ) )
      if( _failonerror )
        throw new BuildException( _lang.getMessage( "settings.id.object.wrong", _settingsid, so.getClass().getName() ));
      else 
        return;
    
    Config settings = (Config)so;
    
    try
    {
      Collection<Repository> repos = settings.getRepositories();
    
      DependencyBuilder db = DependencyBuilderFactory.create( DependencyBuilderFactory.JAVA_DEPENDENCY_MODEL
                                                            , repos
                                                            , null
                                                            , null
                                                            , null 
                                                            );
     
    }
    catch( Exception e )
    {
      if( _failonerror )
        throw new BuildException( e.getMessage() );
      else 
        return;
    }
    
    if( _dependencies != null )
      for( Dependency d : _dependencies )
        System.out.println( d._amd );
  }
  
  public void setFailonerror( boolean failonerror )
  {
    this._failonerror = failonerror;
  }

  public String getSettingsid()
  {
    return _settingsid;
  }

  public void setSettingsid( String settingsid )
  {
    this._settingsid = settingsid;
  }

  public void setTransitive( boolean val )
  {
    this._transitive = val;
  }

  public boolean getTransitive()
  {
    return _transitive;
  }
  
  public String getPathid()
  {
    return _pathId;
  }

  public void setPathid( String pathId )
  {
    this._pathId = pathId;
  }

  public String getRefpathid()
  {
    return _refPathId;
  }

  public void setRefpathid( String refPathId )
  {
    this._refPathId = refPathId;
  }

  public Dependency createDependency()
  {
    if( _dependencies == null )
      _dependencies = new ArrayList<Dependency>(8);
    
    Dependency dep = new Dependency();
    
    _dependencies.add( dep );
    
    return dep;
  }

  public class Dependency
  {
    ArtifactBasicMetadata _amd;

    private void init()
    {
      if( _amd == null )
        _amd = new ArtifactBasicMetadata();
    }

    public void setGroupId( String groupId )
    {
      init();
      
      _amd.setArtifactId( groupId );
    }

    public void setArtifactId( String artifactId )
    {
      init();
      
      _amd.setArtifactId( artifactId );
    }

    public void setVersion( String version )
    {
      init();
      
      _amd.setVersion( version );
    }

    public void setType( String type )
    {
      init();
      
      _amd.setVersion( type );
    }

    public void setClassifier( String classifier )
    {
      init();
      
      _amd.setClassifier( classifier );
    }

    public void setScope( String scope )
    {
      init();
      
      _amd.setScope( scope );
    }

    public void setName( String name )
    {
      _amd = new ArtifactBasicMetadata( name );
    }

  }
}