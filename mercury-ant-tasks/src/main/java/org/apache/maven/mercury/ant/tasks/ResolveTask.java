package org.apache.maven.mercury.ant.tasks;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.FileList;
import org.apache.tools.ant.types.FileSet;
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
    private static final Language LANG = new DefaultLanguage( ResolveTask.class );

    ArtifactScopeEnum[] SCOPES =
        new ArtifactScopeEnum[] { ArtifactScopeEnum.compile, ArtifactScopeEnum.test, ArtifactScopeEnum.runtime };

    public static final String TASK_NAME = LANG.getMessage( "resolve.task.name" );

    public static final String TASK_DESC = LANG.getMessage( "resolve.task.desc" );

    private String _pathId;

    private boolean _hasPathId = false;

    private String _refPathId;

    private String _configId;

    private String _depId;

    private String _id;

    private boolean _transitive = true;

    private ArtifactScopeEnum _scope;

    private List<Dependency> _dependencies;

    private Dependency _sourceDependency;
    
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
            throwIfEnabled( LANG.getMessage( "no.dep.id" ) );
            return;
        }

        if ( _depId != null )
        {
            Object d = getProject().getReference( _depId );

            if ( d == null )
            {
                throwIfEnabled( LANG.getMessage( "no.dep", _depId ) );
                return;
            }

            if ( !Dep.class.isAssignableFrom( d.getClass() ) )
            {
                throwIfEnabled( LANG.getMessage( "bad.dep", _depId, d.getClass().getName(), Dep.class.getName() ) );
                return;
            }

            dep = (Dep) d;
        }
        else
        // inner dependency set
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
                throwIfEnabled( LANG.getMessage( "path.exists", _pathId ) );
                return;
            }
        }
        else if ( !Util.isEmpty( _refPathId ) )
        {
            Object p = getProject().getReference( _refPathId );

            if ( p == null )
            {
                throwIfEnabled( LANG.getMessage( "no.path.ref", _refPathId ) );
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

            dep.setTransitive( _transitive );

            ArtifactScopeEnum[] scopes = _scope == null ? SCOPES : new ArtifactScopeEnum[] { _scope };

            for ( ArtifactScopeEnum sc : scopes )
            {
                Collection<Artifact> artifacts = dep.resolve( config, sc );

                if ( Util.isEmpty( artifacts ) )
                    continue;

                if ( ArtifactScopeEnum.compile.equals( sc ) )
                {
                    createPath( Config.DEFAULT_PATH_ID, Config.DEFAULT_FILESET_ID, artifacts );
                    
                    if( _hasPathId && ! Config.DEFAULT_PATH_ID.equals( _pathId ) )
                        createPath( _pathId, Config.DEFAULT_FILESET_ID, artifacts );
                }

                createPath( Config.DEFAULT_PATH_ID + "." + sc.getScope(), Config.DEFAULT_FILESET_ID + "."
                    + sc.getScope(), artifacts );
            }

        }
        catch ( Exception e )
        {
            throwIfEnabled( e );
        }
    }

    private File findRoot( File f )
    {
        File root = f;
        
        while( root.getParentFile() != null )
            root = root.getParentFile();
        
        return root;
    }

    private void createPath( String pathId, String filesetId, Collection<Artifact> artifacts )
        throws IOException
    {

        if ( Util.isEmpty( artifacts ) )
        {
            return;
        }

        FileList pathFileList = new FileList();

        FileSet fSet = (FileSet) getProject().getReference( filesetId );

        if ( fSet == null )
        {
            fSet = new FileSet();

            fSet.setProject( getProject() );

            getProject().addReference( filesetId, fSet );
        }

        File dir = null;

        for ( Artifact a : artifacts )
        {
            if ( dir == null )
                dir = findRoot( a.getFile() );
                
            String aPath = a.getFile().getCanonicalPath();

            FileList.FileName fn = new FileList.FileName();

            fn.setName( aPath );

            pathFileList.addConfiguredFile( fn );
            
        }

        pathFileList.setDir( dir );
        
        
        // fileset is trickier as it wants a dir
        fSet.setDir( dir );
        
        String dirName = dir.getCanonicalPath(); 
        
        int dirLen = dirName.length();
        
        for( String fn : pathFileList.getFiles( getProject() ) )
        {
            fSet.createInclude().setName( fn.substring( dirLen ) );
        }

        Path path = (Path) getProject().getReference( pathId );

        // now - the path
        if ( path == null )
        {
            path = new Path( getProject(), pathId );

            path.addFilelist( pathFileList );

            getProject().addReference( pathId, path );
        }
        else
        {
            Path newPath = new Path( getProject() );

            newPath.addFilelist( pathFileList );

            path.append( newPath );
        }

    }

    // attributes
    public void setConfigid( String configid )
    {
        this._configId = configid;
    }

    public void setPathid( String pathId )
    {
        setPathId( pathId );
    }

    public void setPathId( String pathId )
    {
        this._pathId = pathId;
        
        _hasPathId = true;
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

    public void setName( String name )
    {
        setId( name );
    }

    public void setId( String id )
    {
        this._id = id;

        if ( _sourceDependency != null )
        {
            _sourceDependency.setId( id );
        }
    }

    public void setSource( String pom )
    {
        _sourceDependency = createDependency();

        if ( _id != null )
        {
            _sourceDependency.setId( _id );
        }

        _sourceDependency.setPom( pom );
    }

    public void setTransitive( boolean transitive )
    {
        this._transitive = transitive;
    }

    public Dependency createDependency()
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