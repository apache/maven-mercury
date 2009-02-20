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

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */

public class Dependency
{
    private static final Language LANG = new DefaultLanguage( Dependency.class );

    ArtifactBasicMetadata _amd;

    String _pom;

    boolean _optional = false;

    /** dependency processor type, if any */
    String _processor;
    
    private static long _count = 0L;
    
    public void setName( String name )
    {
        if( _pom != null )
            throw new IllegalArgumentException( LANG.getMessage( "dependency.amd.pom.exists", _pom, name ) );
        
        _amd = new ArtifactBasicMetadata( name );

        _amd.setOptional( _optional );
    }

    public void setId( String name )
    {
        setName( name );
    }

    public void setOptional( boolean optional )
    {
        this._optional = optional;

        if ( _amd != null )
        {
            _amd.setOptional( optional );
        }
    }

    public void setPom( String pom )
    {
        if( _amd != null )
            throw new IllegalArgumentException( LANG.getMessage( "dependency.pom.amd.exists", _amd.toString(), pom ) );

        setId( "__ant_fake:_ant_fake:" + (_count++)+"::pom" );
        
        int pos = pom.indexOf( ':' );

        if ( pos != -1 )
        {
            this._processor = pom.substring( 0, pos );
            this._pom = pom.substring( pos + 1 );
        }
        else
        {
            this._pom = pom;
        }
    }

    public void setSource( String pom )
    {
        setPom( pom );
    }

    public void setGroupId( String groupId )
    {
        if ( _amd == null )
        {
            _amd = new ArtifactBasicMetadata();
        }

        _amd.setGroupId( groupId );
    }

    public void setGroupid( String groupId )
    {
        setGroupId( groupId );
    }

    public void setArtifactId( String artifactId )
    {
        if ( _amd == null )
        {
            _amd = new ArtifactBasicMetadata();
        }

        _amd.setArtifactId( artifactId );
    }

    public void setArtifactid( String artifactId )
    {
        setArtifactId( artifactId );
    }

    public void setVersion( String version )
    {
        if ( _amd == null )
        {
            _amd = new ArtifactBasicMetadata();
        }

        _amd.setVersion( version );
    }
}
