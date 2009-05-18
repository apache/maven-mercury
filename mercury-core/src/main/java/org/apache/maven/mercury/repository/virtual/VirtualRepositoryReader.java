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
package org.apache.maven.mercury.repository.virtual;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.Quality;
import org.apache.maven.mercury.artifact.api.ArtifactListProcessor;
import org.apache.maven.mercury.artifact.api.ArtifactListProcessorException;
import org.apache.maven.mercury.artifact.version.DefaultArtifactVersion;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.builder.api.MetadataReader;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.mercury.event.EventGenerator;
import org.apache.maven.mercury.event.EventManager;
import org.apache.maven.mercury.event.EventTypeEnum;
import org.apache.maven.mercury.event.GenericEvent;
import org.apache.maven.mercury.event.MercuryEventListener;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.LocalRepository;
import org.apache.maven.mercury.repository.api.MetadataResults;
import org.apache.maven.mercury.repository.api.RemoteRepository;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.api.RepositoryMetadataCache;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.api.RepositoryWriter;
import org.apache.maven.mercury.repository.cache.fs.MetadataCacheFs;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryReaderM2;
import org.apache.maven.mercury.util.LruMemCache;
import org.apache.maven.mercury.util.MemCache;
import org.apache.maven.mercury.util.Util;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * this helper class hides the necessity to talk to localRepo and a bunch of remoteRepos. It also adds discrete
 * convenience methods, hiding batch nature of RepositoryReader
 * 
 * @author Oleg Gusakov
 * @version $Id$
 */
public class VirtualRepositoryReader
    implements MetadataReader, EventGenerator
{
    public static final String EVENT_READ_ARTIFACTS = "read.artifacts";

    public static final String EVENT_READ_ARTIFACTS_FROM_REPO = "read.artifacts.from.repo";

    public static final String EVENT_READ_ARTIFACTS_FROM_REPO_QUALIFIED = "read.artifacts.from.repo.qualified";

    public static final String EVENT_READ_ARTIFACTS_FROM_REPO_UNQUALIFIED = "read.artifacts.from.repo.unqualified";

    public static final String EVENT_READ_VERSIONS = "read.versions";

    public static final String EVENT_READ_VERSIONS_FROM_REPO = "read.versions.from.repo";

    public static final String EVENT_READ_DEPENDENCIES = "read.dependencies";

    public static final String EVENT_READ_DEPENDENCIES_FROM_REPO = "read.dependencies.from.repo";

    public static final String EVENT_READ_RAW = "vr.read.raw";

    public static final String EVENT_READ_RAW_FROM_REPO = "read.raw.from.repo";

    /** file system cache subfolder */
    public static final String METADATA_CACHE_DIR = ".cache";

    /** minimum # of queue elements to consider parallelization */
    private static int MIN_PARALLEL = 5;

    private static final Language LANG = new DefaultLanguage( VirtualRepositoryReader.class );

    private static final IMercuryLogger LOG = MercuryLoggerManager.getLogger( VirtualRepositoryReader.class );

    // ----------------------------------------------------------------------------------------------------------------------------
    private List<Repository> _repositories = new ArrayList<Repository>( 8 );

    private RepositoryReader[] _repositoryReaders;

    private LocalRepository _localRepository;

    private RepositoryWriter _localRepositoryWriter;

    private RemoteRepositoryReaderM2 _idleReader;

    private RepositoryMetadataCache _mdCache;

    private Map<String, ArtifactListProcessor> _processors;

    private boolean _initialized = false;

    private EventManager _eventManager;
    
    public static final String SYSTEM_PROPERTY_VERSION_CACHE_SIZE = "mercury.version.cache.size";
    
    private static final int _versionCacheSize = Integer.valueOf( System.getProperty( SYSTEM_PROPERTY_VERSION_CACHE_SIZE, "1024" ) ); 
    
    private static final MemCache<ArtifactMetadata, List<ArtifactMetadata> > _cachedVersions =
        _versionCacheSize == 0 ? null
        : new LruMemCache<ArtifactMetadata, List<ArtifactMetadata>>( _versionCacheSize )
        ;

    // ----------------------------------------------------------------------------------------------------------------------------
    public VirtualRepositoryReader( Collection<Repository> repositories )
        throws RepositoryException
    {
        if ( !Util.isEmpty( repositories ) )
        {
            this._repositories.addAll( repositories );
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public VirtualRepositoryReader( LocalRepository localRepository, Collection<RemoteRepository> remoteRepositories )
        throws RepositoryException
    {
        if ( _localRepository == null )
        {
            throw new RepositoryException( "null local repo" );
        }

        this._localRepository = localRepository;

        this._repositories.add( localRepository );

        if ( remoteRepositories != null && remoteRepositories.size() > 0 )
        {
            this._repositories.addAll( remoteRepositories );
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public VirtualRepositoryReader( Repository... repositories )
        throws RepositoryException
    {
        if ( repositories != null && repositories.length > 0 )
        {
            for ( Repository r : repositories )
            {
                this._repositories.add( r );
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public static final RepositoryMetadataCache getCache( File localRepositoryRoot )
        throws IOException
    {
        // TODO: 2008-10-13 og: man - I miss plexus! Badly want an IOC container. This
        // should be configured, not hardcoded
        return MetadataCacheFs.getCache( new File( localRepositoryRoot, METADATA_CACHE_DIR ) );
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public void addRepository( Repository repo )
        throws RepositoryException
    {
        if ( _initialized )
        {
            throw new RepositoryException( "cannot add repositories after VirtualReader has been initialized" );
        }

        _repositories.add( repo );
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public void setProcessors( Map<String, ArtifactListProcessor> processors )
    {
        _processors = processors;
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    /**
     * very important call - makes VRR sort out all the information it collected so far
     */
    public void init()
        throws RepositoryException
    {
        if ( _initialized )
        {
            return;
        }

        _repositoryReaders = new RepositoryReader[_repositories.size()];

        // move local repo's upfront - they are faster!
        int i = 0;
        for ( Repository r : _repositories )
        {
            if ( !r.isLocal() || !r.isReadable() )
            {
                continue;
            }

            RepositoryReader rr = r.getReader();

            rr.setMetadataReader( this );

            _repositoryReaders[i++] = rr;

            if ( r.isWriteable() )
            {
                // we select the first writable repo in the list
                if ( _localRepository != null )
                {
                    continue;
                }

                _localRepository = (LocalRepository) r.getReader().getRepository();
                _localRepositoryWriter = _localRepository.getWriter();

                if ( _mdCache == null )
                {
                    try
                    {
                        _mdCache = getCache( _localRepository.getDirectory() );

                        if ( _eventManager != null )
                        {
                            _mdCache.setEventManager( _eventManager );
                        }
                    }
                    catch ( IOException e )
                    {
                        throw new RepositoryException( e.getMessage() );
                    }
                }
            }
        }

        // remote ones
        for ( Repository r : _repositories )
        {
            if ( r.isLocal() || !r.isReadable() )
            {
                continue;
            }

            RepositoryReader rr = r.getReader();

            if ( _mdCache != null )
            {
                rr.setMetadataCache( _mdCache );
            }

            rr.setMetadataReader( this );

            _repositoryReaders[i++] = rr;
        }

        try
        {
            _idleReader =
                (RemoteRepositoryReaderM2) new RemoteRepositoryM2( "http://localhost",
                                                                   DependencyProcessor.NULL_PROCESSOR ).getReader();
        }
        catch ( MalformedURLException e )
        {
            throw new RepositoryException( e );
        }

        _initialized = true;
    }

    /**
     * close all readers is they are started
     */
    public void close()
    {
        if ( !_initialized )
            return;

        try
        {
            for ( RepositoryReader rr : _repositoryReaders )
                rr.close();
        }
        finally
        {
            _initialized = false;
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------
    private MetadataResults readCachedVersions( Collection<ArtifactMetadata> query, List<ArtifactMetadata> leftOvers )
    {
        if( _cachedVersions == null )
        {
            leftOvers.addAll( query );
            return null;
        }
        
        MetadataResults res = null;
        
        for( ArtifactMetadata key : query )
        {
            List<ArtifactMetadata> vl = _cachedVersions.get( key );
            if( Util.isEmpty( vl ) )
                leftOvers.add( key );
            else
            {
                if( res == null )
                    res = new MetadataResults( key, vl );
                else
                    res.add( key, vl );
            }
        }
        
        return res;
    }
    //----------------------------------------------------------------------------------------------------------------------------
    private MetadataResults cacheVersions( MetadataResults res )
    {
        if(true)
            return res;
        
        if( _cachedVersions == null || res == null || res.hasExceptions() || ! res.hasResults() )
        {
            return res;
        }
        
        for( ArtifactMetadata key :res.getResults().keySet() )
        {
            List<ArtifactMetadata> vl = res.getResult( key );
            
            if( Util.isEmpty( vl ) )
                continue;
            
            for( ArtifactMetadata md : vl )
                md.setTracker( null );
            
            _cachedVersions.put( key, vl );
        }
        
        return res;
    }
    // ----------------------------------------------------------------------------------------------------------------------------
    public MetadataResults readVersions( Collection<ArtifactMetadata> query )
        throws IllegalArgumentException, RepositoryException
    {
        if ( query == null )
        {
            throw new IllegalArgumentException( "null bmd supplied" );
        }

        init();

        GenericEvent event = null;

        try
        {
            if ( _eventManager != null )
            {
                event = new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_VERSIONS );
            }

            ArtifactListProcessor tp = _processors == null ? null : _processors.get( ArtifactListProcessor.FUNCTION_TP );

            GenericEvent eventRead = null;

            List<ArtifactMetadata> qList = new ArrayList<ArtifactMetadata>( query.size() );
//            qList.addAll( query );
            MetadataResults res = readCachedVersions( query, qList );
            
            if( Util.isEmpty( qList ) )
                return res;

            for ( RepositoryReader rr : _repositoryReaders )
            {
                try
                {
                    // all found
                    if ( qList.isEmpty() )
                    {
                        break;
                    }

                    if ( _eventManager != null )
                    {
                        eventRead =
                            new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_VERSIONS_FROM_REPO,
                                              rr.getRepository().getId() );
                    }

                    MetadataResults repoRes = rr.readVersions( qList );

                    if ( repoRes != null && repoRes.hasExceptions() )
                    {
                        if ( LOG.isWarnEnabled() )
                        {
                            LOG.warn( repoRes.getExceptions().toString() );
                        }
                    }

                    if ( repoRes != null && repoRes.hasResults() )
                    {
                        for ( ArtifactMetadata key : repoRes.getResults().keySet() )
                        {
                            List<ArtifactMetadata> rorRes = repoRes.getResult( key );

                            if ( tp != null )
                            {
                                try
                                {
                                    tp.configure( key );
                                    rorRes = tp.process( rorRes );
                                }
                                catch ( ArtifactListProcessorException e )
                                {
                                    throw new RepositoryException( e );
                                }
                            }

                            if ( Util.isEmpty( rorRes ) )
                            {
                                eventRead.setResult( "none found" );
                                continue;
                            }

                            for ( ArtifactMetadata bmd : rorRes )
                            {
                                bmd.setTracker( rr );
                            }

                            if ( res == null )
                            {
                                res = new MetadataResults( key, rorRes );
                            }
                            else
                            {
                                res.add( key, rorRes );
                            }

                            if ( ( !key.isVirtual() && key.isSingleton() )
                                || ( key.isVirtual() && rr.getRepository().isSufficient() ) )
                            {
                                // fixed release is found or virtual is found
                                // in a sufficient repo - no more scanning
                                qList.remove( key );
                            }
                        }
                    }

                    if ( _eventManager != null )
                    {
                        eventRead.setResult( "repo done" );
                    }
                }
                finally
                {
                    if ( _eventManager != null )
                    {
                        eventRead.stop();
                        _eventManager.fireEvent( eventRead );
                    }
                }
            }

            if ( res != null && res.hasResults() )
            {
                processSingletons( res );
            }

            return cacheVersions( res );
        }
        finally
        {
            if ( _eventManager != null )
            {
                event.stop();
                _eventManager.fireEvent( event );
            }
        }
    }

    private void processSingletons( MetadataResults res )
    {
        if ( !res.hasResults() )
        {
            return;
        }

        Map<ArtifactMetadata, List<ArtifactMetadata>> m = res.getResults();

        for ( ArtifactMetadata key : m.keySet() )
        {
            processSingletons( key, res.getResult( key ) );
        }
    }

    /** order all found versions per query, then leave only the last one hanging */
    private void processSingletons( ArtifactMetadata key, List<ArtifactMetadata> res )
    {
        if ( Util.isEmpty( res ) || !DefaultArtifactVersion.isVirtual( key.getVersion() ) )
        {
            return;
        }

        TreeSet<ArtifactMetadata> ts = new TreeSet<ArtifactMetadata>( new Comparator<ArtifactMetadata>()
        {
            public int compare( ArtifactMetadata o1, ArtifactMetadata o2 )
            {
                String v1 = o1.getVersion();
                String v2 = o2.getVersion();

                if ( o1.isVirtualSnapshot() && o1.getTimeStamp() != null )
                    v1 = v1.replace( Artifact.SNAPSHOT_VERSION, o1.getTimeStamp() + "-00" );

                if ( o2.isVirtualSnapshot() && o2.getTimeStamp() != null )
                    v2 = v2.replace( Artifact.SNAPSHOT_VERSION, o2.getTimeStamp() + "-00" );

                DefaultArtifactVersion av1 = new DefaultArtifactVersion( v1 );
                DefaultArtifactVersion av2 = new DefaultArtifactVersion( v2 );

                return av1.compareTo( av2 );
            }

        } );
        ts.addAll( res );

        ArtifactMetadata single = ts.last();

        // Oleg: -SNAPSHOT should always win
        // if( key.isVirtualSnapshot() )
        // {
        // ArtifactMetadata first = ts.first();
        //            
        // if( first.isVirtualSnapshot() )
        // single = first;
        // }

        res.clear();

        res.add( single );
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public ArtifactMetadata readDependencies( ArtifactMetadata bmd )
        throws IllegalArgumentException, RepositoryException
    {
        if ( bmd == null )
        {
            throw new IllegalArgumentException( "null bmd supplied" );
        }

        GenericEvent event = null;

        try
        {
            if ( _eventManager != null )
            {
                event =
                    new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_DEPENDENCIES, bmd.toString() );
            }

            init();

            List<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>( 1 );
            query.add( bmd );

            ArtifactMetadata md = new ArtifactMetadata( bmd );

            RepositoryReader[] repos = _repositoryReaders;

            Object tracker = bmd.getTracker();

            // do we know where this metadata came from ?
            if ( tracker != null && RepositoryReader.class.isAssignableFrom( tracker.getClass() ) )
            {
                repos = new RepositoryReader[] { (RepositoryReader) tracker };
            }

            GenericEvent eventRead = null;

            for ( RepositoryReader rr : repos )
            {
                try
                {
                    if ( _eventManager != null )
                    {
                        eventRead =
                            new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_DEPENDENCIES_FROM_REPO,
                                              rr.getRepository().getId() );
                    }

                    MetadataResults res = rr.readDependencies( query );

                    if ( res != null )
                    {
                        if ( res.hasExceptions() )
                        {
                            if ( LOG.isWarnEnabled() )
                            {
                                LOG.warn( bmd + " dependecies: error : " + res.getExceptions().toString() );
                            }
                        }

                        if ( res.hasResults( bmd ) )
                        {
                            md.setDependencies( res.getResult( bmd ) );
                            md.setTracker( rr );

                            if ( _eventManager != null )
                            {
                                eventRead.setInfo( eventRead.getInfo() + ", found: " + md.getDependencies() );
                            }

                            if ( LOG.isDebugEnabled() )
                            {
                                LOG.debug( bmd + " dependecies found : " + md.getDependencies() );
                            }

                            return md;
                        }
                    }
                }
                finally
                {
                    if ( _eventManager != null )
                    {
                        eventRead.stop();
                        _eventManager.fireEvent( eventRead );
                    }
                }
            }

            if ( _eventManager != null )
            {
                event.setResult( "not found" );
            }

            return md;
        }
        finally
        {
            if ( _eventManager != null )
            {
                event.stop();
                _eventManager.fireEvent( event );
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    /**
     * split query into repository buckets
     */
    private Map<RepositoryReader, List<ArtifactMetadata>> sortByRepo( Collection<? extends ArtifactMetadata> query )
    {
        HashMap<RepositoryReader, List<ArtifactMetadata>> res = null;

        List<ArtifactMetadata> rejects = null;

        for ( ArtifactMetadata bmd : query )
        {
            Object tracker = bmd.getTracker();

            // do we know where this metadata came from ?
            if ( tracker != null && RepositoryReader.class.isAssignableFrom( tracker.getClass() ) )
            {
                RepositoryReader rr = (RepositoryReader) tracker;

                if ( res == null )
                {
                    res = new HashMap<RepositoryReader, List<ArtifactMetadata>>();
                }

                List<ArtifactMetadata> rl = res.get( rr );

                if ( rl == null )
                {
                    rl = new ArrayList<ArtifactMetadata>();
                    res.put( rr, rl );
                }

                rl.add( bmd );

            }
            else
            {
                if ( rejects == null )
                {
                    rejects = new ArrayList<ArtifactMetadata>();
                }

                rejects.add( bmd );
            }
        }

        if ( rejects != null )
        {
            if ( res == null )
            {
                res = new HashMap<RepositoryReader, List<ArtifactMetadata>>();
            }

            res.put( RepositoryReader.NULL_READER, rejects );
        }

        return res;
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    // TODO: Oleg: this is a copy of readArtifacts - optimize for the particular
    // purpose - reading non-qualified virtuals, remove
    //
    // Now this can also be used to read artifacts without pooling them if there is such
    //
    public ArtifactResults readArtifactsNoBatch( Collection<? extends ArtifactMetadata> query )
        throws RepositoryException
    {
        GenericEvent event = null;

        try
        {
            ArtifactResults res = null;

            if ( _eventManager != null )
            {
                event = new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_ARTIFACTS, "" );
            }

            if ( Util.isEmpty( query ) )
            {
                return res;
            }

            Map<RepositoryReader, List<ArtifactMetadata>> buckets = sortByRepo( query );

            List<ArtifactMetadata> leftovers = buckets == null ? null : buckets.get( RepositoryReader.NULL_READER );

            if ( buckets == null )
            {
                throw new RepositoryException( LANG.getMessage( "internal.error.sorting.query", query.toString() ) );
            }

            init();

            res = new ArtifactResults();

            // first read repository-qualified Artifacts
            for ( RepositoryReader rr : buckets.keySet() )
            {
                if ( RepositoryReader.NULL_READER.equals( rr ) )
                {
                    continue;
                }

                String repoId = rr.getRepository().getId();

                GenericEvent eventRead = null;

                try
                {
                    if ( _eventManager != null )
                    {
                        eventRead =
                            new GenericEvent( EventTypeEnum.virtualRepositoryReader,
                                              EVENT_READ_ARTIFACTS_FROM_REPO_QUALIFIED, repoId );
                    }

                    List<ArtifactMetadata> rrQuery = buckets.get( rr );

                    ArtifactResults rrRes = rr.readArtifacts( rrQuery );

                    if ( rrRes.hasExceptions() )
                    {
                        throw new RepositoryException( LANG.getMessage( "error.reading.existing.artifact",
                                                                        rrRes.getExceptions().toString(),
                                                                        rr.getRepository().getId() ) );
                    }

                    if ( rrRes.hasResults() )
                    {
                        for ( ArtifactMetadata bm : rrRes.getResults().keySet() )
                        {
                            List<Artifact> al = rrRes.getResults( bm );

                            res.addAll( bm, al );

                            // don't write local artifacts back to the same repo
                            if ( _localRepository != null && repoId.equals( _localRepository.getId() ) )
                            {
                                continue;
                            }

                            if ( _localRepositoryWriter != null )
                            {
                                _localRepositoryWriter.writeArtifacts( al );
                            }
                        }
                    }
                }
                finally
                {
                    if ( _eventManager != null )
                    {
                        eventRead.stop();
                        _eventManager.fireEvent( eventRead );
                    }
                }
            }

            // then process unqualified virtuals
            if ( !Util.isEmpty( leftovers ) )
            {
                List<ArtifactMetadata> virtuals = null;

                for ( ArtifactMetadata md : leftovers )
                {
                    if ( DefaultArtifactVersion.isVirtual( md.getVersion() ) )
                    {
                        if ( virtuals == null )
                        {
                            virtuals = new ArrayList<ArtifactMetadata>();
                        }

                        virtuals.add( md );
                    }
                }

                if ( virtuals != null )
                {
                    // this makes them qualified because tracker will point to
                    // the repository
                    MetadataResults virtRes = readVersions( virtuals );

                    leftovers.removeAll( virtuals );

                    virtuals.clear();

                    if ( virtRes != null )
                    {
                        if ( virtRes.hasResults() )
                        {
                            Map<ArtifactMetadata, ArtifactMetadata> sMap =
                                new HashMap<ArtifactMetadata, ArtifactMetadata>();

                            for ( ArtifactMetadata md : virtRes.getResults().keySet() )
                            {
                                ArtifactMetadata v = virtRes.getResult( md ).get( 0 );
                                virtuals.add( v );
                                sMap.put( v, md );
                            }

                            // recursive call, this time for qualified artifacts
                            ArtifactResults ares = readArtifacts( virtuals );

                            if ( ares != null )
                            {
                                if ( ares.hasResults() )
                                {
                                    Map<ArtifactMetadata, List<Artifact>> aresMap = ares.getResults();

                                    // remap
                                    for ( ArtifactMetadata md : aresMap.keySet() )
                                    {
                                        res.add( sMap.get( md ), aresMap.get( md ).get( 0 ) );
                                    }
                                }

                                if ( ares.hasExceptions() )
                                {
                                    res.getExceptions().putAll( ares.getExceptions() );
                                }
                            }

                            if ( virtRes.hasExceptions() )
                            {
                                res.addError( virtRes.getExceptions() );
                            }
                        }

                        if ( virtRes.hasExceptions() )
                        {
                            res.addError( virtRes.getExceptions() );
                        }
                    }
                }

            }

            // then search all repos for unqualified Artifacts
            if ( !Util.isEmpty( leftovers ) )
            {
                for ( RepositoryReader rr : _repositoryReaders )
                {
                    if ( leftovers.isEmpty() )
                    {
                        break;
                    }

                    String repoId = rr.getRepository().getId();

                    GenericEvent eventRead = null;

                    try
                    {
                        if ( _eventManager != null )
                        {
                            eventRead =
                                new GenericEvent( EventTypeEnum.virtualRepositoryReader,
                                                  EVENT_READ_ARTIFACTS_FROM_REPO_UNQUALIFIED, repoId );
                        }

                        ArtifactResults rrRes = rr.readArtifacts( leftovers );

                        if ( rrRes.hasExceptions() )
                        {
                            res.addError( rrRes );
                        }
                        else if ( rrRes.hasResults() )
                        {
                            for ( ArtifactMetadata bm : rrRes.getResults().keySet() )
                            {
                                List<Artifact> al = rrRes.getResults( bm );

                                res.addAll( bm, al );

                                leftovers.remove( bm );

                                // don't write local artifacts back to the same repo
                                if ( _localRepository != null && repoId.equals( _localRepository.getId() ) )
                                {
                                    continue;
                                }

                                if ( _localRepositoryWriter != null )
                                {
                                    _localRepositoryWriter.writeArtifacts( al );
                                }

                            }
                        }
                    }
                    finally
                    {
                        if ( _eventManager != null )
                        {
                            eventRead.stop();
                            _eventManager.fireEvent( eventRead );
                        }
                    }
                }
            }

            return res;
        }
        finally
        {
            if ( _eventManager != null )
            {
                event.stop();
                _eventManager.fireEvent( event );
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public ArtifactResults readArtifacts( Collection<? extends ArtifactMetadata> query )
        throws RepositoryException
    {
        GenericEvent event = null;

        try
        {
            ArtifactResults res = null;

            if ( _eventManager != null )
            {
                event = new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_ARTIFACTS, "" );
            }

            if ( Util.isEmpty( query ) )
            {
                return res;
            }

            int qSize = query.size();

            List<ArtifactMetadata> qualified = new ArrayList<ArtifactMetadata>( qSize );

            List<ArtifactMetadata> leftovers = new ArrayList<ArtifactMetadata>( qSize );

            for ( ArtifactMetadata md : query )
            {
                Object tracker = md.getTracker();

                // do we know where this metadata came from ?
                if ( tracker != null && RemoteRepositoryReaderM2.class.isAssignableFrom( tracker.getClass() )
                    && !md.isVirtual() && md.isSingleton() )
                    qualified.add( md );
                else
                    leftovers.add( md );
            }

            if ( qualified.isEmpty() && leftovers.isEmpty() )
            {
                throw new RepositoryException( LANG.getMessage( "internal.error.sorting.query", query.toString() ) );
            }

            init();

            res = new ArtifactResults();

            // first read repository-qualified Artifacts
            GenericEvent eventRead = null;

            if ( qualified.size() > 0 )
                try
                {
                    if ( _eventManager != null )
                        eventRead =
                            new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_ARTIFACTS, ""
                                + qualified );

                    ArtifactResults rrRes = new ArtifactResults();

                    _idleReader.readQualifiedArtifacts( qualified, rrRes );

                    if ( rrRes.hasExceptions() )
                    {
                        throw new RepositoryException( LANG.getMessage( "error.reading.existing.artifact",
                                                                        rrRes.getExceptions().toString(),
                                                                        "multiple.server.internal.repo" ) );
                    }

                    if ( rrRes.hasResults() )
                    {
                        for ( ArtifactMetadata bm : rrRes.getResults().keySet() )
                        {
                            List<Artifact> al = rrRes.getResults( bm );

                            res.addAll( bm, al );

                            if ( _localRepositoryWriter != null )
                            {
                                _localRepositoryWriter.writeArtifacts( al );
                            }
                        }
                    }
                }
                catch ( Exception e )
                {
                    throw new RepositoryException( e );
                }
                finally
                {
                    if ( _eventManager != null )
                    {
                        eventRead.stop();
                        _eventManager.fireEvent( eventRead );
                    }
                }

            // then process unqualified virtuals
            if ( !Util.isEmpty( leftovers ) )
            {
                List<ArtifactMetadata> virtuals = null;

                for ( ArtifactMetadata md : leftovers )
                {
                    if ( DefaultArtifactVersion.isVirtual( md.getVersion() ) )
                    {
                        if ( virtuals == null )
                        {
                            virtuals = new ArrayList<ArtifactMetadata>();
                        }

                        virtuals.add( md );
                    }
                }

                if ( virtuals != null )
                {
                    MetadataResults virtRes = readVersions( virtuals );

                    leftovers.removeAll( virtuals );

                    virtuals.clear();

                    if ( virtRes != null )
                    {
                        if ( virtRes.hasResults() )
                        {
                            Map<ArtifactMetadata, ArtifactMetadata> sMap =
                                new HashMap<ArtifactMetadata, ArtifactMetadata>();

                            for ( ArtifactMetadata md : virtRes.getResults().keySet() )
                            {
                                ArtifactMetadata v = virtRes.getResult( md ).get( 0 );
                                virtuals.add( v );
                                sMap.put( v, md );
                            }

                            ArtifactResults ares = readArtifactsNoBatch( virtuals );

                            if ( ares != null )
                            {
                                if ( ares.hasResults() )
                                {
                                    Map<ArtifactMetadata, List<Artifact>> aresMap = ares.getResults();

                                    // remap
                                    for ( ArtifactMetadata md : aresMap.keySet() )
                                    {
                                        res.add( sMap.get( md ), aresMap.get( md ).get( 0 ) );
                                    }
                                }

                                if ( ares.hasExceptions() )
                                {
                                    res.getExceptions().putAll( ares.getExceptions() );
                                }
                            }

                            if ( virtRes.hasExceptions() )
                            {
                                res.addError( virtRes.getExceptions() );
                            }
                        }

                        if ( virtRes.hasExceptions() )
                        {
                            res.addError( virtRes.getExceptions() );
                        }
                    }
                }

            }

            // then search all repos for unqualified Artifacts
            if ( !Util.isEmpty( leftovers ) )
            {
                for ( RepositoryReader rr : _repositoryReaders )
                {
                    if ( leftovers.isEmpty() )
                    {
                        break;
                    }

                    String repoId = rr.getRepository().getId();

                    try
                    {
                        if ( _eventManager != null )
                        {
                            eventRead =
                                new GenericEvent( EventTypeEnum.virtualRepositoryReader,
                                                  EVENT_READ_ARTIFACTS_FROM_REPO_UNQUALIFIED, repoId );
                        }

                        ArtifactResults rrRes = rr.readArtifacts( leftovers );

                        if ( rrRes.hasExceptions() )
                        {
                            res.addError( rrRes );
                        }
                        else if ( rrRes.hasResults() )
                        {
                            for ( ArtifactMetadata bm : rrRes.getResults().keySet() )
                            {
                                List<Artifact> al = rrRes.getResults( bm );

                                res.addAll( bm, al );

                                leftovers.remove( bm );

                                // don't write local artifacts back to the same repo
                                if ( _localRepository != null && repoId.equals( _localRepository.getId() ) )
                                {
                                    continue;
                                }

                                if ( _localRepositoryWriter != null )
                                {
                                    _localRepositoryWriter.writeArtifacts( al );
                                }

                            }
                        }
                    }
                    finally
                    {
                        if ( _eventManager != null )
                        {
                            eventRead.stop();
                            _eventManager.fireEvent( eventRead );
                        }
                    }
                }
            }
            
            res.reOrderResults( query );

            return res;
        }
        finally
        {
            if ( _eventManager != null )
            {
                event.stop();
                _eventManager.fireEvent( event );
            }
        }
    }
    

    // ----------------------------------------------------------------------------------------------------------------------------
    // MetadataReader implementation
    // ----------------------------------------------------------------------------------------------------------------------------
    public byte[] readMetadata( ArtifactMetadata bmd )
        throws MetadataReaderException
    {
        return readMetadata( bmd, false );
    }

    public byte[] readMetadata( ArtifactMetadata bmd, boolean exempt )
        throws MetadataReaderException
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "Asking for pom: " + bmd );
        }

        byte[] res = readRawData( bmd, "", "pom", exempt );

        return res;
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    // MetadataReader implementation
    // ----------------------------------------------------------------------------------------------------------------------------
    public byte[] readRawData( ArtifactMetadata bmd, String classifier, String type )
        throws MetadataReaderException
    {
        return readRawData( bmd, classifier, type, false );
    }

    public byte[] readRawData( ArtifactMetadata bmd, String classifier, String type, boolean exempt )
        throws MetadataReaderException
    {

        GenericEvent event = null;
        String eventTag = null;

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "request for " + bmd + ", classifier=" + classifier + ", type=" + type );
        }

        if ( bmd == null )
        {
            throw new IllegalArgumentException( "null bmd supplied" );
        }

        try
        {
            if ( _eventManager != null )
            {
                eventTag =
                    bmd.toString() + ( Util.isEmpty( classifier ) ? "" : ", classifier=" + classifier )
                        + ( Util.isEmpty( type ) ? "" : ", type=" + type );
                event = new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_RAW, eventTag );
            }

            ArtifactMetadata bmdQuery = new ArtifactMetadata( bmd );

            if ( !Util.isEmpty( type ) )
                bmdQuery.setType( type );

            // pom cannot have classifiers
            if ( "pom".equals( bmdQuery.getType() ) )
                bmdQuery.setClassifier( null );

            try
            {
                init();
            }
            catch ( RepositoryException e )
            {
                throw new MetadataReaderException( e );
            }

            byte[] res = null;
            Quality vq = new Quality( bmd.getVersion() );

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "quality calculated as " + ( vq.getQuality() == null ? "null" : vq.getQuality().name() ) );
            }

            if ( Quality.SNAPSHOT_QUALITY.equals( vq ) )
            {
                List<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>( 1 );

                ArtifactMetadata nBmd = new ArtifactMetadata( bmdQuery );

                if ( !Util.isEmpty( type ) )
                    nBmd.setType( type );

                query.add( nBmd );

                try
                {
                    MetadataResults vRes = readVersions( query );
                    if ( Util.isEmpty( vRes ) )
                    {
                        if ( LOG.isDebugEnabled() )
                        {
                            LOG.debug( "no snapshots found - throw exception" );
                        }

                        throw new MetadataReaderException( LANG.getMessage( "no.snapshots", bmd.toString(), classifier,
                                                                            type ) );
                    }

                    if ( vRes.hasResults( nBmd ) )
                    {
                        List<ArtifactMetadata> versions = vRes.getResult( nBmd );

                        processSingletons( nBmd, versions );

                        // TreeSet<ArtifactMetadata> snapshots =
                        // new TreeSet<ArtifactMetadata>( new MetadataVersionComparator() );
                        // snapshots.addAll( versions );
                        //
                        // bmdQuery = snapshots.last();

                        bmdQuery = versions.get( 0 );
                    }
                    else
                    {
                        if ( LOG.isDebugEnabled() )
                        {
                            LOG.debug( "no snapshots found - throw exception" );
                        }

                        throw new MetadataReaderException( LANG.getMessage( "no.snapshots", bmd.toString(), classifier,
                                                                            type ) );
                    }
                }
                catch ( Exception e )
                {
                    throw new MetadataReaderException( e );
                }
            }

            for ( RepositoryReader rr : _repositoryReaders )
            {
                GenericEvent eventRead = null;

                try
                {
                    if ( _eventManager != null )
                    {
                        eventRead =
                            new GenericEvent( EventTypeEnum.virtualRepositoryReader, EVENT_READ_RAW_FROM_REPO,
                                              rr.getRepository().getId() + ": " + eventTag );
                    }

                    res = rr.readRawData( bmdQuery, classifier, type, false );
                    if ( res != null )
                    {
                        if ( LOG.isDebugEnabled() )
                        {
                            LOG.debug( bmdQuery + " found in " + rr.getRepository().getServer() );
                        }

                        if ( _eventManager != null )
                        {
                            eventRead.setInfo( eventRead.getInfo() );
                        }

                        return res;
                    }

                    if ( _eventManager != null )
                    {
                        eventRead.setResult( "not found" );
                    }
                }
                finally
                {
                    if ( _eventManager != null )
                    {
                        eventRead.stop();
                        _eventManager.fireEvent( eventRead );
                    }
                }
            }

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "no data found, returning null" );
            }

            return null;
        }
        finally
        {
            if ( _eventManager != null )
            {
                event.stop();
                _eventManager.fireEvent( event );
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public RepositoryMetadataCache getCache()
    {
        return _mdCache;
    }

    // ----------------------------------------------------------------------------------------------------------------------------
    public void register( MercuryEventListener listener )
    {
        if ( _eventManager == null )
        {
            _eventManager = new EventManager();
        }

        _eventManager.register( listener );
    }

    public void setEventManager( EventManager eventManager )
    {
        _eventManager = eventManager;
    }

    public void unRegister( MercuryEventListener listener )
    {
        if ( _eventManager != null )
        {
            _eventManager.unRegister( listener );
        }
    }
    // ----------------------------------------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------------------------------------
}
