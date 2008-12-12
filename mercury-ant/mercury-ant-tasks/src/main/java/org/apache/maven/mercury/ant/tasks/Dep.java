package org.apache.maven.mercury.ant.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.util.Util;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class Dep
extends AbstractDataType
{
  private static final Language _lang = new DefaultLanguage( Dep.class );
  
  private List<Dependency> _dependencies;
  
  private boolean _transitive = true;
  
  public void setTransitive( boolean val )
  {
    this._transitive = val;
  }
  
  protected List<ArtifactBasicMetadata> getList()
  {
    if( Util.isEmpty( _dependencies ) )
      return null;
    
    List<ArtifactBasicMetadata> res = new ArrayList<ArtifactBasicMetadata>( _dependencies.size() );

    for( Dependency d : _dependencies )
      res.add( d._amd );
    
    return res;
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
    
    boolean _optional = false;

    public void setName( String name )
    {
      _amd = new ArtifactBasicMetadata( name );

      _amd.setOptional( _optional );
    }

    public void setOptional( boolean optional )
    {
      this._optional = optional;

      if( _amd != null )
        _amd.setOptional( optional );
    }
    
  }
}