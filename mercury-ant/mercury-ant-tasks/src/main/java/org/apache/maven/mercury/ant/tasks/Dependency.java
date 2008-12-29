package org.apache.maven.mercury.ant.tasks;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */

public class Dependency
{
    private static final Language LANG = new DefaultLanguage( Dependency.class );

    ArtifactBasicMetadata _amd;
    
    String _pom;

    boolean _optional = false;

    public void setName( String name )
    {
        _amd = new ArtifactBasicMetadata( name );

        _amd.setOptional( _optional );
    }

    public void setOptional( boolean optional )
    {
        this._optional = optional;

        if ( _amd != null )
            _amd.setOptional( optional );
    }

    public void setPom( String pom )
    {
        this._pom = pom;
        
        if( _amd == null )
            throw new UnsupportedOperationException( LANG.getMessage( "dep.dependency.pom.needs.name", pom ) );
    }

    public void setGroupId( String groupId )
    {
        if( _amd == null )
            _amd = new ArtifactBasicMetadata();
        
        _amd.setGroupId( groupId );
    }

    public void setGroupid( String groupId )
    {
        setGroupId( groupId );
    }

    public void setArtifactId( String artifactId )
    {
        if( _amd == null )
            _amd = new ArtifactBasicMetadata();
        
        _amd.setArtifactId( artifactId );
    }

    public void setArtifactid( String artifactId )
    {
        setArtifactId( artifactId );
    }

    public void setVersion( String version )
    {
        if( _amd == null )
            _amd = new ArtifactBasicMetadata();
        
        _amd.setVersion( version );
    }
}
