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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class Config
    extends AbstractDataType
{
    private static final Language LANG = new DefaultLanguage( Config.class );

    public static final String SYSTEM_PROPERTY_CENTRAL_URL = "mercury.central.url";

    public static final String SYSTEM_PROPERTY_LOCAL_DIR_NAME = "mercury.repo.local";

    public static final String DEFAULT_LOCAL_DIR_NAME = "/.m2/repository";

    public static final String DEFAULT_PATH_ID = "mercury.classpath";

    public static final String DEFAULT_FILESET_ID = "mercury.fileset";

    public static final String DEFAULT_CONFIG_ID =
        System.getProperty( "mercury.default.config.id", "mercury.default.config.id." + System.currentTimeMillis() );

    Collection<Repo> _repos;

    Collection<Auth> _auths;

    List<Repository> _repositories;

    public Config()
    {
    }

    private Config( String localDir, String remoteUrl )
    {
        Repo local = createRepo();
        local.setId( "defaultLocalRepo" );

        String localDirName =
            ( localDir == null ) ? System.getProperty( SYSTEM_PROPERTY_LOCAL_DIR_NAME, System.getProperty( "user.home" )
                + DEFAULT_LOCAL_DIR_NAME ) : localDir;

        local.setDir( localDirName );

        Repo central = createRepo();
        central.setId( "central" );

        String centralUrl =
            ( remoteUrl == null ) ? System.getProperty( SYSTEM_PROPERTY_CENTRAL_URL, "http://repo1.maven.org/maven2" )
                            : remoteUrl;

        central.setUrl( centralUrl );
    }

    public List<Repository> getRepositories()
        throws BuildException
    {
        if ( Util.isEmpty( _repos ) )
        {
            return null;
        }

        if ( _repositories != null )
        {
            return _repositories;
        }

        _repositories = new ArrayList<Repository>( _repos.size() );

        for ( Repo repo : _repos )
        {
            _repositories.add( repo.getRepository() );
        }

        return _repositories;
    }

    private void init()
    {
        if ( getId() != null )
        {
            return;
        }

        setId( DEFAULT_CONFIG_ID );

        if ( getProject() != null )
        {
            getProject().addReference( DEFAULT_CONFIG_ID, this );
        }
    }

    public Repo createRepo()
    {
        init();

        Repo r = new Repo( true );

        listRepo( r );

        return r;
    }

    public Repo createRepository()
    {
        return createRepo();
    }

    protected void listRepo( Repo repo )
    {
        if ( _repos == null )
        {
            _repos = new ArrayList<Repo>( 4 );
        }

        _repos.add( repo );
    }

    public static void addDefaultRepository( Project project, Repo repo )
    {
        Object co = project.getReference( DEFAULT_CONFIG_ID );

        if ( co == null )
        {
            co = new Config();

            project.addReference( DEFAULT_CONFIG_ID, co );
        }

        Config config = (Config) co;

        config.listRepo( repo );
    }

    public static Config getDefaultConfig( Project project )
    {
        Object co = project.getReference( DEFAULT_CONFIG_ID );
        if ( co == null )
        {
            co = new Config( null, null );

            project.addReference( DEFAULT_CONFIG_ID, co );
        }

        Config config = (Config) co;

        return config;
    }

    public Auth createAuth()
    {
        init();

        if ( _auths == null )
        {
            _auths = new ArrayList<Auth>( 4 );
        }

        Auth a = new Auth();

        _auths.add( a );

        return a;
    }

    // ======================================================================================

}
