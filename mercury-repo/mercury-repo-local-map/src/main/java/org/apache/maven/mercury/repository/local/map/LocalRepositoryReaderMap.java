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
package org.apache.maven.mercury.repository.local.map;

import java.util.Collection;
import java.util.List;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.repository.api.AbstracRepositoryReader;
import org.apache.maven.mercury.repository.api.AbstractRepository;
import org.apache.maven.mercury.repository.api.MetadataResults;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.util.Util;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

public class LocalRepositoryReaderMap
    extends AbstracRepositoryReader
    implements RepositoryReader
{
    private static final IMercuryLogger _log = MercuryLoggerManager.getLogger( LocalRepositoryReaderMap.class );

    private static final Language _lang = new DefaultLanguage( LocalRepositoryReaderMap.class );

    // ---------------------------------------------------------------------------------------------------------------
    private static final String[] _protocols = new String[] { "map" };

    private final LocalRepositoryMap _repo;

    // ---------------------------------------------------------------------------------------------------------------
    public LocalRepositoryReaderMap( Repository repo, DependencyProcessor dp )
    {
        if ( repo == null )
            throw new IllegalArgumentException( "localRepo cannot be null" );

        if ( dp == null )
            throw new IllegalArgumentException( "localRepo cannot be null" );

        _mdProcessor = dp;

        _repo = (LocalRepositoryMap) repo;

    }
    // ---------------------------------------------------------------------------------------------------------------
    public Repository getRepository()
    {
        return _repo;
    }
    // ---------------------------------------------------------------------------------------------------------------
    public boolean canHandle( String protocol )
    {
        return AbstractRepository.DEFAULT_LOCAL_READ_PROTOCOL.equals( protocol );
    }
    // ---------------------------------------------------------------------------------------------------------------
    public String[] getProtocols()
    {
        return _protocols;
    }
    // ---------------------------------------------------------------------------------------------------------------
    public ArtifactResults readArtifacts( Collection<ArtifactMetadata> query )
        throws RepositoryException
    {
        if( Util.isEmpty( query ) )
            return null;
        
        if( Util.isEmpty( _repo._storage ) )
            return null;
        
        ArtifactResults res = new ArtifactResults();
        
        for( ArtifactMetadata bmd : query )
        {
            Artifact a;
            try
            {
                a = _repo._storage.findArtifact( bmd );
            }
            catch ( Exception e )
            {
                throw new RepositoryException(e);
            }
            
            if( a != null )
                res.add( bmd, a );
        }

        return res;
    }
    // ---------------------------------------------------------------------------------------------------------------
    public byte[] readRawData( String path )
    throws MetadataReaderException
    {
        return readRawData( path, false );
    }
    // ---------------------------------------------------------------------------------------------------------------
    public byte[] readRawData( String path, boolean exempt )
    throws MetadataReaderException
    {
        try
        {
            return _repo._storage.findRaw( path );
        }
        catch ( StorageException e )
        {
            throw new MetadataReaderException(e);
        }
    }
    // ---------------------------------------------------------------------------------------------------------------
    public byte[] readRawData( ArtifactMetadata bmd, String classifier, String type )
        throws MetadataReaderException
    {
        return readRawData( bmd, classifier, type, false );
    }
    // ---------------------------------------------------------------------------------------------------------------
    public byte[] readRawData( ArtifactMetadata bmd, String classifier, String type, boolean exempt )
        throws MetadataReaderException
    {
        
        String key =  bmd.getGroupId()
            + ":"+bmd.getArtifactId()
            + ":"+bmd.getVersion()
            + ":"+ (classifier == null ? "" : classifier)
            + ":"+ (type == null ? bmd.getType() : type)
        ;
        
        return readRawData( key, exempt );
    }
    // ---------------------------------------------------------------------------------------------------------------
    public MetadataResults readDependencies( Collection<ArtifactMetadata> query )
        throws RepositoryException
    {
        if( Util.isEmpty( query ) )
            return null;
        
        DependencyProcessor dp = _repo.getDependencyProcessor();
        
        MetadataResults res = new MetadataResults( query.size() );
        
        for( ArtifactMetadata bmd : query )
        {
            try
            {
                MetadataReader mdr = _repo._mdReader == null ? this : _repo._mdReader;
                
                List<ArtifactMetadata> deps = dp.getDependencies( bmd, mdr, System.getenv(), System.getProperties() );
                
                res.add( bmd, deps );
            }
            catch ( Exception e )
            {
                _log.error( e.getMessage() );
                
                res.addError( bmd, e );
            }
        }
        
        return res;
    }
    // ---------------------------------------------------------------------------------------------------------------
    public MetadataResults readVersions( Collection<ArtifactMetadata> query )
        throws RepositoryException
    {
        return null;
    }
    // ---------------------------------------------------------------------------------------------------------------
    public void close()
    {
    }
    // ---------------------------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------------------------
}
