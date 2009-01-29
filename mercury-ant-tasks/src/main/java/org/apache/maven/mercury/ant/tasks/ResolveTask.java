package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class ResolveTask
    extends AbstractAntTask
{
    private static final Language _lang = new DefaultLanguage( ResolveTask.class );

    public static final String TASK_NAME = _lang.getMessage( "resolve.task.name" );

    public static final String TASK_DESC = _lang.getMessage( "resolve.task.desc" );

    private String _pathId;

    private String _fileSetId;

    private String _refPathId;

    private String _configId;

    private String _depId;

    private ArtifactScopeEnum _scope = ArtifactScopeEnum.compile;

    private List<Dependency> _dependencies;

    // ----------------------------------------------------------------------------------------
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

    // ----------------------------------------------------------------------------------------
    @Override
    public void execute()
        throws BuildException
    {
        // Dependencies
        Dep dep = null;

        if ( ( _depId == null ) && Util.isEmpty( _dependencies ) )
        {
            throwIfEnabled( _lang.getMessage( "no.dep.id" ) );
            return;
        }

        if ( _depId != null )
        {
            Object d = getProject().getReference( _depId );

            if ( d == null )
            {
                throwIfEnabled( _lang.getMessage( "no.dep", _depId ) );
                return;
            }

            if ( !Dep.class.isAssignableFrom( d.getClass() ) )
            {
                throwIfEnabled( _lang.getMessage( "bad.dep", _depId, d.getClass().getName(), Dep.class.getName() ) );
                return;
            }

            dep = (Dep) d;
        }
        else // inner dependency set
        {
            dep = new Dep();

            dep.setList( _dependencies );
        }

        // Path
        Path path = null;

        if ( !Util.isEmpty( _pathId ) )
        {
            if ( getProject().getReference( _pathId ) != null )
            {
                throwIfEnabled( _lang.getMessage( "path.exists", _pathId ) );
                return;
            }
        }
        else if( !Util.isEmpty( _refPathId ) )
        {
            Object p = getProject().getReference( _refPathId );

            if ( p == null )
            {
                throwIfEnabled( _lang.getMessage( "no.path.ref", _refPathId ) );
                return;
            }

            path = (Path) p;
        }
        else
        {
            _pathId = Config.DEFAULT_PATH_ID;
        }

        try
        {
            Config config = AbstractAntTask.findConfig( getProject(), _configId );

            Collection<Artifact> artifacts = dep.resolve( config, _scope );

            if ( artifacts == null )
            {
                return;
            }

            FileList pathFileList = new FileList();

            File dir = null;

            for ( Artifact a : artifacts )
            {
                if ( dir == null )
                {
                    dir = a.getFile().getParentFile();
                }

                String aPath = a.getFile().getCanonicalPath();

                FileList.FileName fn = new FileList.FileName();

                fn.setName( aPath );

                pathFileList.addConfiguredFile( fn );
            }

            pathFileList.setDir( dir );

            // now - the path
            if ( path == null )
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
        catch ( Exception e )
        {
            if ( _failOnError )
            {
                throw new BuildException( e.getMessage() );
            }
            else
            {
                return;
            }
        }
    }

    // attributes
    public void setConfigid( String configid )
    {
        this._configId = configid;
    }

    public void setPathid( String pathId )
    {
        this._pathId = pathId;
    }

    public void setPathId( String pathId )
    {
        this._pathId = pathId;
    }

    public void setFilesetid( String fileSetIdId )
    {
        this._fileSetId = fileSetIdId;
    }

    public void setFilesetId( String fileSetIdId )
    {
        this._fileSetId = fileSetIdId;
    }

    public void setFileSetId( String fileSetIdId )
    {
        this._fileSetId = fileSetIdId;
    }

    public void setRefpathid( String refPathId )
    {
        this._refPathId = refPathId;
    }

    public void setRefpathId( String refPathId )
    {
        this._refPathId = refPathId;
    }

    public void setRefPathId( String refPathId )
    {
        this._refPathId = refPathId;
    }

    public void setDepid( String depid )
    {
        this._depId = depid;
    }

    public void setDepId( String depid )
    {
        this._depId = depid;
    }

    public void setScope( ArtifactScopeEnum scope )
    {
        this._scope = scope;
    }

    public Dependency createDependency(  )
    {
        if ( Util.isEmpty( _dependencies ) )
        {
            _dependencies = new ArrayList<Dependency>( 8 );
        }

        Dependency dependency = new Dependency();

        _dependencies.add( dependency );

        return dependency;
    }
}