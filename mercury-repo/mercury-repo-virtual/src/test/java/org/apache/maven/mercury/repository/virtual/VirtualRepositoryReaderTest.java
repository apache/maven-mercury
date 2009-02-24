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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.DefaultArtifact;
import org.apache.maven.mercury.event.MercuryEvent;
import org.apache.maven.mercury.event.MercuryEventListener;
import org.apache.maven.mercury.event.MercuryEvent.EventMask;
import org.apache.maven.mercury.repository.api.ArtifactBasicResults;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.LocalRepository;
import org.apache.maven.mercury.repository.api.RemoteRepository;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.local.m2.MetadataProcessorMock;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.spi.http.server.HttpTestServer;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.maven.mercury.util.Util;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class VirtualRepositoryReaderTest
    extends TestCase
{
    File _testBase;

    LocalRepository _localRepo;

    RemoteRepository _remoteRepo;

    VirtualRepositoryReader _vr;

    // String _remoteUrl; // = "http://people.apache.org/~ogusakov/repos/test";
    String _artifactCoordSn = "org.apache.maven.mercury:mercury-repo-virtual:1.0.0-alpha-2-SNAPSHOT";

    String _artifactCoordLatest = "org.apache.maven.mercury:mercury-repo-virtual:1.0.0-alpha-2-LATEST";

    String _artifactCoordRelease = "ant:ant:1.6.5";
    
    String LOCAL_REPO_ID = "localRepo";

    String _localRepoId;

    String REMOTE_REPO_ID = "remoteRepo";

    String _remoteRepoId;

    HttpTestServer _jetty;

    String _port;

    File _remoteRepoBase = new File( "./target/test-classes/remoteRepo" );

    @Override
    protected void setUp()
        throws Exception
    {

        if ( Util.isWindows() )
        {
            Thread.sleep( 2000L );
        }

        _testBase = new File( "./target/repo" );
        FileUtil.delete( _testBase );

        // windows does not like it
        if ( !Util.isWindows() && _testBase.exists() )
        {
            throw new Exception( "cannot clean folder " + _testBase.getAbsolutePath() );
        }

        _testBase.mkdirs();
        FileUtil.copy( new File( "./src/test/resources/repo" ), _testBase, false );

        if ( !_testBase.exists() || !_testBase.isDirectory() )
        {
            throw new Exception( "cannot create clean folder " + _testBase.getAbsolutePath() );
        }

        _localRepo = new LocalRepositoryM2( LOCAL_REPO_ID, _testBase, new MetadataProcessorMock() );
        
        _localRepoId = _localRepo.getId();

        _jetty = new HttpTestServer( _remoteRepoBase, "/repo" );
        _jetty.start();
        _port = String.valueOf( _jetty.getPort() );
        Server server = new Server( REMOTE_REPO_ID, new URL( "http://localhost:" + _port + "/repo" ) );

        _remoteRepo = new RemoteRepositoryM2( server, new MetadataProcessorMock() );
        
        _remoteRepoId = _remoteRepo.getId();

        if ( Util.isWindows() )
        {
            Thread.sleep( 2000L );
        }

        List<Repository> rl = new ArrayList<Repository>();
        rl.add( _localRepo );
        rl.add( _remoteRepo );

        _vr = new VirtualRepositoryReader( rl );
        
        
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        if ( _jetty != null )
        {
        }
    }

    public void testReadSnapshot()
        throws Exception
    {
        ArtifactBasicMetadata bmd = new ArtifactBasicMetadata( _artifactCoordSn );
        List<ArtifactBasicMetadata> q = new ArrayList<ArtifactBasicMetadata>();
        q.add( bmd );

        ArtifactResults res = _vr.readArtifacts( q );

        assertNotNull( res );

        assertFalse( res.hasExceptions() );

        assertTrue( res.hasResults() );

        assertTrue( res.hasResults( bmd ) );

        Map<ArtifactBasicMetadata, List<Artifact>> m = res.getResults();

        List<Artifact> al = m.get( bmd );

        Artifact a = al.get( 0 );

        assertTrue( bmd.getGroupId().equals( a.getGroupId() ) );
        assertTrue( bmd.getArtifactId().equals( a.getArtifactId() ) );

        assertTrue( a.getVersion().matches( Artifact.SNAPSHOT_TS_REGEX ) );

        byte[] pomBytes = a.getPomBlob();

        assertTrue( pomBytes != null );

        assertEquals( 795, pomBytes.length );

        File ab = a.getFile();

        assertNotNull( ab );

        assertTrue( ab.exists() );

        assertEquals( 6162, ab.length() );
    }

    public void testWrite()
        throws Exception
    {
        Artifact a = new DefaultArtifact( new ArtifactBasicMetadata( "a:a:1.0:text:txt" ) );
        File bin = File.createTempFile( "vr-", "-test.txt" );
        bin.deleteOnExit();
        FileUtil.writeRawData( bin, "test" );
        a.setFile( bin );

        List<Artifact> arts = new ArrayList<Artifact>();
        arts.add( a );

        _localRepo.getWriter().writeArtifacts( arts );

        File af = new File( _testBase, "a/a/1.0/a-1.0-text.txt" );

        assertTrue( af.exists() );

        assertEquals( 4, af.length() );

    }

    public void testReadRelease()
        throws Exception
    {
        ArtifactBasicMetadata bmd = new ArtifactBasicMetadata( _artifactCoordRelease );
        List<ArtifactBasicMetadata> q = new ArrayList<ArtifactBasicMetadata>();
        q.add( bmd );

        Listener l = new Listener();
        _vr.register( l );

        ArtifactBasicResults res = _vr.readVersions( q );

        assertNotNull( res );

        assertFalse( res.hasExceptions() );

        assertTrue( res.hasResults() );

        assertTrue( res.hasResults( bmd ) );

        // let events propagate
        Thread.sleep( 2000L );

        assertTrue( l.localEventCount > 0 );

        assertEquals( 0, l.remoteEventCount );

    }

    // ========================================================================
    class Listener
        implements MercuryEventListener
    {

        int localEventCount = 0;

        int remoteEventCount = 0;

        public void fire( MercuryEvent event )
        {
            String tag = event.getInfo();

            if ( _localRepoId.equals( tag ) )
            {
                ++localEventCount;
            }
            else if ( _remoteRepoId.equals( tag ) )
            {
                ++remoteEventCount;
            }

            // System.out.println(EventManager.toString( event ));
            // System.out.flush();
        }

        public EventMask getMask()
        {
            return null;
        }

    }

}
