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
package org.apache.maven.mercury.plexus;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactExclusionList;
import org.apache.maven.mercury.artifact.ArtifactInclusionList;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactQueryList;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.crypto.api.StreamObserverFactory;
import org.apache.maven.mercury.crypto.api.StreamVerifierAttributes;
import org.apache.maven.mercury.crypto.api.StreamVerifierException;
import org.apache.maven.mercury.crypto.api.StreamVerifierFactory;
import org.apache.maven.mercury.crypto.pgp.PgpStreamVerifierFactory;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.metadata.DependencyBuilder;
import org.apache.maven.mercury.metadata.DependencyBuilderFactory;
import org.apache.maven.mercury.metadata.MetadataTreeException;
import org.apache.maven.mercury.repository.api.MetadataResults;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.api.RepositoryWriter;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.repository.virtual.VirtualRepositoryReader;
import org.apache.maven.mercury.transport.api.Credentials;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.Util;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Configuration;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * default implementation of Mercury plexus wrapper
 * 
 * @author Oleg Gusakov
 */

@Component( role = PlexusMercury.class )
public class DefaultPlexusMercury
    extends AbstractLogEnabled
    implements PlexusMercury
{
    private static final IMercuryLogger LOG = MercuryLoggerManager.getLogger( DefaultPlexusMercury.class );

    private static final Language LANG = new DefaultLanguage( DefaultPlexusMercury.class );

    @Configuration( name = "defaultDependencyProcessorHint", value = "maven" )
    String _defaultDpHint = "maven";

    @Configuration( name = "allowCircularDependencies", value = "true" )
    boolean _allowCircularDependencies = true;

    @Requirement
    private Map<String, DependencyProcessor> _dependencyProcessors;

    // ---------------------------------------------------------------
    public DependencyProcessor findDependencyProcessor( String hint )
        throws RepositoryException
    {
        DependencyProcessor dp = null;

        if ( _dependencyProcessors != null )
            dp = _dependencyProcessors.get( hint );

        if ( dp == null )
            throw new RepositoryException( LANG.getMessage( "no.dep.processor.injected", hint ) );

        return dp;
    }

    // ---------------------------------------------------------------
    public DependencyProcessor findDependencyProcessor()
        throws RepositoryException
    {
        return findDependencyProcessor( _defaultDpHint );
    }
    // ---------------------------------------------------------------
    public void setDefaultDependencyProcessorHint( String hint )
    {
        _defaultDpHint = hint;
    }
    // ---------------------------------------------------------------
    public void setAllowCircularDependencies( boolean allow )
    {
        _allowCircularDependencies = allow;
    }
    // ---------------------------------------------------------------
    public RemoteRepositoryM2 constructRemoteRepositoryM2( String id, URL serverUrl, String serverUser,
                                                           String serverPass, URL proxyUrl, String proxyUser,
                                                           String proxyPass,
                                                           Set<StreamObserverFactory> readerStreamObservers,
                                                           Set<StreamVerifierFactory> readerStreamVerifiers,
                                                           Set<StreamObserverFactory> writerStreamObservers,
                                                           Set<StreamVerifierFactory> writerStreamVerifiers )
        throws RepositoryException
    {
        Server server = new Server( id, serverUrl );

        server.setReaderStreamObserverFactories( readerStreamObservers );
        server.setReaderStreamVerifierFactories( readerStreamVerifiers );
        server.setWriterStreamObserverFactories( writerStreamObservers );
        server.setWriterStreamVerifierFactories( writerStreamVerifiers );

        if ( serverUser != null )
        {
            Credentials cred = new Credentials( serverUser, serverPass );
            server.setServerCredentials( cred );
        }

        if ( proxyUrl != null )
        {
            server.setProxy( proxyUrl );

            if ( proxyUser != null )
            {
                Credentials cred = new Credentials( proxyUser, proxyPass );
                server.setProxyCredentials( cred );
            }
        }

        RemoteRepositoryM2 repo = new RemoteRepositoryM2( id, server, findDependencyProcessor() );

        return repo;
    }

    // ---------------------------------------------------------------
    public LocalRepositoryM2 constructLocalRepositoryM2( String id, File rootDir,
                                                         Set<StreamObserverFactory> readerStreamObservers,
                                                         Set<StreamVerifierFactory> readerStreamVerifiers,
                                                         Set<StreamObserverFactory> writerStreamObservers,
                                                         Set<StreamVerifierFactory> writerStreamVerifiers )
        throws RepositoryException
    {
        Server server;
        try
        {
            server = new Server( id, rootDir.toURL() );
        }
        catch ( MalformedURLException e )
        {
            throw new RepositoryException( e );
        }

        server.setReaderStreamObserverFactories( readerStreamObservers );
        server.setReaderStreamVerifierFactories( readerStreamVerifiers );
        server.setWriterStreamObserverFactories( writerStreamObservers );
        server.setWriterStreamVerifierFactories( writerStreamVerifiers );

        LocalRepositoryM2 repo = new LocalRepositoryM2( server, findDependencyProcessor() );

        return repo;
    }

    // ---------------------------------------------------------------
    public void write( Repository repo, Artifact... artifacts )
        throws RepositoryException
    {
        write( repo, Arrays.asList( artifacts ) );
    }

    public void write( Repository repo, Collection<Artifact> artifacts )
        throws RepositoryException
    {
        if ( repo == null )
            throw new RepositoryException( LANG.getMessage( "null.repo" ) );

        RepositoryWriter wr = repo.getWriter();

        wr.writeArtifacts( artifacts );

    }

    // ---------------------------------------------------------------
    // ---------------------------------------------------------------
    public List<Artifact> read( List<Repository> repos, List<? extends ArtifactMetadata> artifacts )
        throws RepositoryException
    {
        if ( Util.isEmpty( repos ) )
            throw new RepositoryException( LANG.getMessage( "null.repo" ) );

        VirtualRepositoryReader vr = new VirtualRepositoryReader( repos );

        ArtifactResults ar = vr.readArtifacts( artifacts );

        if ( ar == null || !ar.hasResults() )
            return null;

        if ( ar.hasExceptions() && LOG.isWarnEnabled() )
            LOG.info( ar.getExceptions().toString() );

        if ( !ar.hasResults() )
            return null;

        Map<ArtifactMetadata, List<Artifact>> am = ar.getResults();

        List<Artifact> al = new ArrayList<Artifact>();
        for ( Map.Entry<ArtifactMetadata, List<Artifact>> e : am.entrySet() )
            al.addAll( e.getValue() );

        return al;

    }

    public List<Artifact> read( List<Repository> repos, ArtifactMetadata... artifacts )
        throws RepositoryException
    {
        return read( repos, Arrays.asList( artifacts ) );
    }

    // ---------------------------------------------------------------
    public PgpStreamVerifierFactory createPgpReaderFactory( boolean lenient, boolean sufficient, InputStream pubRing )
        throws StreamVerifierException
    {
        return new PgpStreamVerifierFactory( new StreamVerifierAttributes( PgpStreamVerifierFactory.DEFAULT_EXTENSION,
                                                                           lenient, sufficient ), pubRing );
    }

    // ---------------------------------------------------------------
    public PgpStreamVerifierFactory createPgpWriterFactory( boolean lenient, boolean sufficient, InputStream secRing,
                                                            String keyId, String keyPass )
        throws StreamVerifierException
    {
        return new PgpStreamVerifierFactory( new StreamVerifierAttributes( PgpStreamVerifierFactory.DEFAULT_EXTENSION,
                                                                           lenient, sufficient ), secRing, keyId,
                                             keyPass );
    }

    public List<ArtifactMetadata> resolve( List<Repository> repos, ArtifactScopeEnum scope, ArtifactMetadata metadata )
        throws RepositoryException
    {
        return resolve( repos, scope, new ArtifactQueryList( metadata ), null, null );
    }

    // ---------------------------------------------------------------
    public List<ArtifactMetadata> resolve( List<Repository> repos
                                           , ArtifactScopeEnum scope
                                           , ArtifactQueryList artifacts
                                           , ArtifactInclusionList inclusions
                                           , ArtifactExclusionList exclusions
                                           )
        throws RepositoryException
    {
        if ( Util.isEmpty( artifacts ) || artifacts.isEmpty() )
            throw new IllegalArgumentException( LANG.getMessage( "no.artifacts" ) );

        try
        {
            DependencyBuilder depBuilder =
                DependencyBuilderFactory.create( DependencyBuilderFactory.JAVA_DEPENDENCY_MODEL, repos, null, null, null
                    , Util.mapOf( new Object [][] { {DependencyBuilder.SYSTEM_PROPERTY_ALLOW_CIRCULAR_DEPENDENCIES, ""+_allowCircularDependencies} } ) 
                                                );

            List<ArtifactMetadata> res = depBuilder.resolveConflicts( scope, artifacts, inclusions, exclusions );
            
            depBuilder.close();

            return res;
        }
        catch ( MetadataTreeException e )
        {
            throw new RepositoryException( e );
        }
    }

    // ---------------------------------------------------------------
    public MetadataTreeNode resolveAsTree( List<Repository> repos
                                           , ArtifactScopeEnum scope
                                           , ArtifactQueryList artifacts
                                           , ArtifactInclusionList inclusions
                                           , ArtifactExclusionList exclusions
                                           )
        throws RepositoryException
    {
        if ( Util.isEmpty( artifacts ) || artifacts.isEmpty() )
            throw new IllegalArgumentException( LANG.getMessage( "no.artifacts" ) );

        try
        {
            DependencyBuilder depBuilder =
                DependencyBuilderFactory.create( DependencyBuilderFactory.JAVA_DEPENDENCY_MODEL, repos, null, null, null
                       , Util.mapOf( new String [][] { {DependencyBuilder.SYSTEM_PROPERTY_ALLOW_CIRCULAR_DEPENDENCIES, ""+_allowCircularDependencies} } ) 
                );

            MetadataTreeNode res = depBuilder.resolveConflictsAsTree( scope, artifacts, inclusions, exclusions );
            
            depBuilder.close();

            return res;
        }
        catch ( MetadataTreeException e )
        {
            throw new RepositoryException( e );
        }
    }

    // ---------------------------------------------------------------
    /**
     * get all available versions of for the artifact query.
     * 
     * @param repo repository instance to search
     * @param query metadata query to search by
     * @return list of found version metadatas
     * @throws PlexusMercuryException
     */
    public List<ArtifactMetadata> readVersions( List<Repository> repos, ArtifactMetadata query )
        throws RepositoryException
    {
        VirtualRepositoryReader vr = new VirtualRepositoryReader( repos );
        List<ArtifactMetadata> q = new ArrayList<ArtifactMetadata>( 1 );
        q.add( query );

        MetadataResults res = vr.readVersions( q );

        if ( res == null )
            return null;

        if ( res.hasExceptions() && LOG.isWarnEnabled() )
            LOG.warn( res.getExceptions().toString() );

        return res.getResult( query );
    }

    // ---------------------------------------------------------------
    public List<Repository> constructRepositories( String localDir, String... urls )
        throws RepositoryException
    {
        try
        {
            int nRemote = urls == null ? 0 : urls.length;

            List<Repository> repos = new ArrayList<Repository>( 1 + nRemote );

            DependencyProcessor dp = findDependencyProcessor();

            LocalRepositoryM2 lr = new LocalRepositoryM2( new File( localDir ), dp );

            repos.add( lr );

            if ( nRemote > 0 )
                for ( String url : urls )
                {
                    RemoteRepositoryM2 rr = new RemoteRepositoryM2( url, dp );

                    repos.add( rr );
                }

            return repos;

        }
        catch ( Exception e )
        {
            throw new RepositoryException( e );
        }
    }

    // ---------------------------------------------------------------
    // ---------------------------------------------------------------
}
