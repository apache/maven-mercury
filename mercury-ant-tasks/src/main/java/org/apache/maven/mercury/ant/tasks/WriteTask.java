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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.DefaultArtifact;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class WriteTask
    extends AbstractAntTask
{
    private static final Language LANG = new DefaultLanguage( WriteTask.class );

    public static final String TASK_NAME = LANG.getMessage( "write.task.name" );

    public static final String TASK_DESC = LANG.getMessage( "write.task.desc" );

    private String _repoid;

    private String _file;

    private String _name;

    private String _pom;

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
    private Repository findRepo()
        throws BuildException
    {
        Hashtable<String, Object> refs = getProject().getReferences();

        for ( String key : refs.keySet() )
        {
            Object o = refs.get( key );

            if ( o == null )
            {
                continue;
            }

            if ( !Config.class.isAssignableFrom( o.getClass() ) )
            {
                continue;
            }

            Config conf = (Config) o;

            Collection<Repository> repos = conf.getRepositories();

            if ( Util.isEmpty( repos ) )
            {
                continue;
            }

            for ( Repository r : repos )
            {
                if ( _repoid.equals( r.getId() ) )
                {
                    return r;
                }
            }
        }

        return null;
    }

    // ----------------------------------------------------------------------------------------
    @Override
    public void execute()
        throws BuildException
    {
        if ( _repoid == null )
        {
            throwIfEnabled( LANG.getMessage( "write.repo.id.mandatory" ) );
            return;
        }

        Repository repo = findRepo();

        if ( repo == null )
        {
            throwIfEnabled( LANG.getMessage( "write.repo.not.found", _repoid ) );
            return;
        }

        if ( _file == null )
        {
            throwIfEnabled( LANG.getMessage( "write.file.mandatory" ) );
            return;
        }

        File file = new File( _file );

        if ( !file.exists() )
        {
            throwIfEnabled( LANG.getMessage( "write.file.not.found", _file, _repoid ) );
            return;
        }

        if ( Util.isEmpty( _name ) && Util.isEmpty( _pom ) )
        {
            throwIfEnabled( LANG.getMessage( "write.no.name.no.pom", _file, _repoid ) );
            return;
        }

        if ( !Util.isEmpty( _name ) && !Util.isEmpty( _pom ) )
        {
            throwIfEnabled( LANG.getMessage( "write.no.name.no.pom", _file, _repoid ) );
            return;
        }

        if ( !Util.isEmpty( _pom ) )
        {
            throwIfEnabled( LANG.getMessage( "write.pom.not.supported", _file, _repoid ) );
            return;
        }

        try
        {
            DefaultArtifact a = null;

            if ( !Util.isEmpty( _name ) )
            {
                a = new DefaultArtifact( new ArtifactBasicMetadata( _name ) );

                String pomStr =
                    "?xml version='1.0' encoding='UTF-8'?>\n"
                        + "<project xmlns='http://maven.apache.org/POM/4.0.0' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xsi:schemaLocation='http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd'>\n"
                        + "<modelVersion>4.0.0</modelVersion>\n"
                        + "<groupId>"
                        + a.getGroupId()
                        + "</groupId>\n"
                        + "<artifactId>"
                        + a.getArtifactId()
                        + "</artifactId>\n"
                        + ( Util.isEmpty( a.getClassifier() ) ? "" : "<classifier>" + a.getClassifier()
                            + "</classifier>\n" ) + "<version>" + a.getVersion() + "</version>\n"
                        + ( Util.isEmpty( a.getType() ) ? "" : "<packaging>" + a.getType() + "</packaging>\n" )
                        + "</project>\n";

                a.setPomBlob( pomStr.getBytes() );

                a.setFile( file );

                ArrayList<Artifact> al = new ArrayList<Artifact>( 1 );
                al.add( a );

                repo.getWriter().writeArtifacts( al );
            }
            else
            {
                throwIfEnabled( LANG.getMessage( "write.pom.not.supported", _file, _repoid ) );
                return;
            }

        }
        catch ( Exception e )
        {
            throwIfEnabled( e.getMessage() );
        }
    }

    public void setRepoid( String repoid )
    {
        this._repoid = repoid;
    }

    public void setFile( String file )
    {
        this._file = file;
    }

    public void setName( String name )
    {
        this._name = name;
    }

    public void setPom( String pom )
    {
        this._pom = pom;
    }

}