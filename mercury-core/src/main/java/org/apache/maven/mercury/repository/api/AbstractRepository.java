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
package org.apache.maven.mercury.repository.api;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.mercury.artifact.Quality;
import org.apache.maven.mercury.artifact.QualityRange;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.transport.api.Server;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * parent of all repositories and also a helper class for registration of readers/writers
 */
public abstract class AbstractRepository
    implements Repository
{
    private static final Language LANG = new DefaultLanguage( AbstractRepository.class );

    // ---------------------------------------------------------------------------
    public static final String DEFAULT_REMOTE_READ_PROTOCOL = "http";

    public static final String DEFAULT_REMOTE_WRITE_PROTOCOL = "http";

    public static final String DEFAULT_LOCAL_READ_PROTOCOL = "file";

    public static final String DEFAULT_LOCAL_WRITE_PROTOCOL = "file";

    public static final String DEFAULT_REPOSITORY_TYPE = "m2";

    private String id;

    private String defaultReadProtocol = DEFAULT_REMOTE_READ_PROTOCOL;

    private String defaultWriteProtocol = DEFAULT_REMOTE_WRITE_PROTOCOL;

    // ---------------------------------------------------------------------------
    private static Map<String, RepositoryReaderFactory> readerRegistry =
        Collections.synchronizedMap( new HashMap<String, RepositoryReaderFactory>( 4 ) );

    private static Map<String, RepositoryWriterFactory> writerRegistry =
        Collections.synchronizedMap( new HashMap<String, RepositoryWriterFactory>( 4 ) );

    // ---------------------------------------------------------------------------
    protected String type = DEFAULT_REPOSITORY_TYPE;

    protected QualityRange repositoryQualityRange = QualityRange.ALL;

    protected QualityRange versionRangeQualityRange = QualityRange.ALL;

    protected DependencyProcessor dependencyProcessor;

    protected Server server;

    protected boolean isSufficient = false;

    private static final byte[] __HEX_DIGITS = "0123456789abcdef".getBytes();

    // ---------------------------------------------------------------------------
    public AbstractRepository( String id, String type )
    {
        this.id = hashId( id );

        this.type = type;
    }

    // ---------------------------------------------------------------------------
    public static String hashId( String id )
    {
        try
        {
            if ( id == null || ( id.indexOf( '/' ) == -1 && id.indexOf( '\\' ) == -1 ) )
                return id;

            MessageDigest digest = MessageDigest.getInstance( "SHA-1" );

            digest.update( id.getBytes() );

            byte[] bytes = digest.digest();

            int len = bytes.length;

            byte[] raw = new byte[len * 2];

            for ( int i = 0, j = 0; i < len; i++ )
            {
                raw[j++] = __HEX_DIGITS[( 0xF0 & bytes[i] ) >>> 4];
                raw[j++] = __HEX_DIGITS[0x0F & bytes[i]];
            }

            return new String( raw );
        }
        catch ( NoSuchAlgorithmException e )
        {
            throw new IllegalArgumentException( e );
        }
    }

    // ---------------------------------------------------------------------------
    public String getId()
    {
        return id;
    }

    // ---------------------------------------------------------------------------
    public QualityRange getRepositoryQualityRange()
    {
        return repositoryQualityRange == null ? QualityRange.ALL : repositoryQualityRange;
    }

    // ---------------------------------------------------------------------------
    public void setRepositoryQualityRange( QualityRange repositoryQualityRange )
    {
        this.repositoryQualityRange = repositoryQualityRange;
    }

    // ---------------------------------------------------------------------------
    public QualityRange getVersionRangeQualityRange()
    {
        return versionRangeQualityRange;
    }

    // ---------------------------------------------------------------------------
    public void setVersionRangeQualityRange( QualityRange versionRangeQualityRange )
    {
        this.versionRangeQualityRange = versionRangeQualityRange;
    }

    // ---------------------------------------------------------------------------
    public String getDefaultReadProtocol()
    {
        return defaultReadProtocol;
    }

    // ---------------------------------------------------------------------------
    public void setDefaultReadProtocol( String defaultReadProtocol )
    {
        this.defaultReadProtocol = defaultReadProtocol;
    }

    // ---------------------------------------------------------------------------
    public String getDefaultWriteProtocol()
    {
        return defaultWriteProtocol;
    }

    // ---------------------------------------------------------------------------
    public void setDefaultWriteProtocol( String defaultWriteProtocol )
    {
        this.defaultWriteProtocol = defaultWriteProtocol;
    }

    // ---------------------------------------------------------------------------
    public static void register( String type, RepositoryReaderFactory readerFactory )
        throws IllegalArgumentException
    {
        if ( type == null || type.length() < 1 )
            throw new IllegalArgumentException( LANG.getMessage( "null.reader.type" ) );

        if ( readerFactory == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.reader.factory" ) );

        readerRegistry.put( type, readerFactory );
    }

    // ---------------------------------------------------------------------------
    public static void register( String type, RepositoryWriterFactory writerFactory )
        throws IllegalArgumentException
    {
        if ( type == null || type.length() < 1 )
            throw new IllegalArgumentException( LANG.getMessage( "null.writer.type" ) );

        if ( writerFactory == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.writer.factory" ) );

        writerRegistry.put( type, writerFactory );
    }

    // ---------------------------------------------------------------------------
    public static void unregisterReader( String type )
        throws IllegalArgumentException
    {
        if ( type == null || type.length() < 1 )
            throw new IllegalArgumentException( LANG.getMessage( "null.reader.type" ) );

        readerRegistry.remove( type );
    }

    // ---------------------------------------------------------------------------
    public static void unregisterWriter( String type )
        throws IllegalArgumentException
    {
        if ( type == null || type.length() < 1 )
            throw new IllegalArgumentException( LANG.getMessage( "null.writer.type" ) );

        writerRegistry.remove( type );
    }

    // ---------------------------------------------------------------------------
    public static RepositoryReader getReader( String type, Repository repo, DependencyProcessor mdProcessor )
        throws IllegalArgumentException, RepositoryException
    {
        if ( type == null || type.length() < 1 )
            throw new IllegalArgumentException( LANG.getMessage( "null.reader.type" ) );

        if ( repo == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.reader.repo" ) );

        RepositoryReaderFactory rf = readerRegistry.get( type );

        if ( rf == null )
            throw new RepositoryException( LANG.getMessage( "null.reader.factory.found" ) );

        return rf.getReader( repo, mdProcessor );
    }

    // ---------------------------------------------------------------------------
    public static RepositoryWriter getWriter( String type, Repository repo )
        throws IllegalArgumentException, RepositoryException
    {
        if ( type == null || type.length() < 1 )
            throw new IllegalArgumentException( LANG.getMessage( "null.writer.type" ) );

        if ( repo == null )
            throw new IllegalArgumentException( LANG.getMessage( "null.writer.repo" ) );

        RepositoryWriterFactory wf = writerRegistry.get( type );

        if ( wf == null )
            throw new RepositoryException( LANG.getMessage( "null.writer.factory.found" ) );

        return wf.getWriter( repo );
    }

    // ---------------------------------------------------------------------------
    public boolean isSnapshots()
    {
        return repositoryQualityRange.isAcceptedQuality( Quality.SNAPSHOT_QUALITY );
    }

    // ---------------------------------------------------------------------------
    public boolean isReleases()
    {
        return repositoryQualityRange.isAcceptedQuality( Quality.RELEASE_QUALITY );
    }

    // ---------------------------------------------------------------------------
    public boolean isAcceptedQuality( Quality quality )
    {
        return repositoryQualityRange.isAcceptedQuality( quality );
    }

    public boolean isSufficient()
    {
        return isSufficient;
    }

    public void setSufficient( boolean isSufficient )
    {
        this.isSufficient = isSufficient;
    }

    // ---------------------------------------------------------------------------
    public boolean hasServer()
    {
        return server != null;
    }

    // ---------------------------------------------------------------------------
    public Server getServer()
    {
        return server;
    }

    // ---------------------------------------------------------------------------
    public boolean hasDependencyProcessor()
    {
        return dependencyProcessor == null;
    }

    public DependencyProcessor getDependencyProcessor()
    {
        return dependencyProcessor;
    }

    public void setDependencyProcessor( DependencyProcessor dependencyProcessor )
    {
        this.dependencyProcessor = dependencyProcessor;
    }
    // ---------------------------------------------------------------------------
    // ---------------------------------------------------------------------------
}
