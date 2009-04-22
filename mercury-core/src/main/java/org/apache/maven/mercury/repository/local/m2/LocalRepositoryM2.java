/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.maven.mercury.repository.local.m2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.AbstractRepository;
import org.apache.maven.mercury.repository.api.LocalRepository;
import org.apache.maven.mercury.repository.api.NonExistentProtocolException;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.api.RepositoryWriter;
import org.apache.maven.mercury.transport.api.Server;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

public class LocalRepositoryM2
extends AbstractRepository
implements LocalRepository
{
    private static final Language LANG = new DefaultLanguage( LocalRepositoryM2.class );

    private File directory;
    
    public static final String METADATA_FILE_NAME = "maven-metadata-local.xml";
    
    /** indicates that if a-1.0-SNAPSHOT.jar exists, it wins despite any timestamps
     *  required for Maven comatibility 
     **/
    private boolean _snapshotAlwaysWins = false;

    //----------------------------------------------------------------------------------
    private void setDirectory( File directory )
    {
      if( directory == null )
        throw new IllegalArgumentException( LANG.getMessage( "null.directory" ) );
      
      if( !directory.exists() )
        directory.mkdirs();

      if( !directory.isDirectory() )
          throw new IllegalArgumentException( LANG.getMessage( "file.directory", directory.getAbsolutePath() ) );
        
      this.directory = directory;
    }
    //----------------------------------------------------------------------------------
    public LocalRepositoryM2( Server server, DependencyProcessor dependencyProcessor )
    {
        super( server.getId(), DEFAULT_REPOSITORY_TYPE );
        setDirectory( new File( server.getURL().getFile() ) );
        this.server = server;
        
        setDependencyProcessor( dependencyProcessor );
    }
    //----------------------------------------------------------------------------------
    public LocalRepositoryM2( File directory, DependencyProcessor dependencyProcessor )
    throws IOException
    {
        this( directory.getCanonicalPath(), directory, DEFAULT_REPOSITORY_TYPE, dependencyProcessor );
    }
    //----------------------------------------------------------------------------------
    public LocalRepositoryM2( String id, File directory, DependencyProcessor dependencyProcessor )
    {
        this( id, directory, DEFAULT_REPOSITORY_TYPE, dependencyProcessor );
    }
    //----------------------------------------------------------------------------------
    public LocalRepositoryM2( String id, File directory, String type, DependencyProcessor dependencyProcessor )
    {
        super( id, type );

        setDirectory( directory );
        
        setDependencyProcessor( dependencyProcessor );
        
        try
        {
            this.server = new Server( getId(), directory.toURL() );
        }
        catch ( MalformedURLException e )
        {
            throw new IllegalArgumentException(e);
        }
    }
    //----------------------------------------------------------------------------------
    public File getDirectory()
    {
        return directory;
    }
    //----------------------------------------------------------------------------------
    public RepositoryReader getReader() 
    {
      return new LocalRepositoryReaderM2( this, getDependencyProcessor() );
    }
    //----------------------------------------------------------------------------------
    public RepositoryReader getReader( String protocol )
    {
       return getReader();
    }
    //----------------------------------------------------------------------------------
    public RepositoryWriter getWriter()
    {
      return new LocalRepositoryWriterM2(this);
    }
    //----------------------------------------------------------------------------------
    public RepositoryWriter getWriter( String protocol )
        throws NonExistentProtocolException
    {
      return getWriter();
    }
    //----------------------------------------------------------------------------------
    public boolean isLocal()
    {
      return true;
    }
    //----------------------------------------------------------------------------------
    public boolean isReadable()
    {
      return true;
    }
    //----------------------------------------------------------------------------------
    public boolean isWriteable()
    {
      return true;
    }

    public void setSnapshotAlwaysWins( boolean alwaysWins )
    {
        _snapshotAlwaysWins = alwaysWins;
    }

    public boolean getSnapshotAlwaysWins()
    {
        return _snapshotAlwaysWins;
    }
    
    //----------------------------------------------------------------------------------
    public String getType()
    {
      return DEFAULT_REPOSITORY_TYPE;
    }
    //----------------------------------------------------------------------------------
    public String getMetadataName()
    {
      return METADATA_FILE_NAME;
    }
    //----------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------
}
