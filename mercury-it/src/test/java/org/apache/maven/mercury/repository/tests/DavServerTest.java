/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package org.apache.maven.mercury.repository.tests;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.mercury.MavenDependencyProcessor;
import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.DefaultArtifact;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.api.RepositoryWriter;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Credentials;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class DavServerTest
    extends PlexusTestCase
{
    static final String _user = "foo";

    static final String _pass = "bar";

    // repo1
    WebDavServer _dav;

    File _base;

    RemoteRepositoryM2 _davRepo;

    // repo2
    WebDavServer _dav2;

    File _base2;

    RemoteRepositoryM2 _davRepo2;

    static final String _davContext = "/webdav";

    static final String _davContext2 = "/webdav2test";

    // ---------------------------------------------------------------------------------------------
    protected void setUp()
        throws Exception
    {
        super.setUp();

        setUp1();

        setUp2();
    }

    // ---------------------------------------------------------------------------------------------
    protected void setUp1()
        throws Exception
    {
        _base = new File( "./target", _davContext );

        FileUtil.delete( _base );

        _base.mkdirs();

        _dav = new WebDavServer( 0, _base, _davContext, getContainer(), 9, "mercury-test", null );

        _dav.start();

        Credentials user = new Credentials( _user, _pass );

        Server server =
            new Server( "dav", new URL( "http://localhost:" + _dav.getPort() + _davContext ), false, false, user );

        System.out.println( "URL: " + server.getURL() );

        _davRepo = new RemoteRepositoryM2( server, new MavenDependencyProcessor() );
    }

    // ---------------------------------------------------------------------------------------------
    protected void setUp2()
        throws Exception
    {
        _base2 = new File( "./target", _davContext2 );

        FileUtil.delete( _base2 );

        _base2.mkdirs();

        _dav2 = new WebDavServer( 0, _base2, _davContext2, getContainer(), 9, null, _base2.getAbsolutePath() );

        _dav2.start();

        Credentials user = new Credentials( _user, _pass );

        Server server =
            new Server( "dav2", new URL( "http://localhost:" + _dav2.getPort() + _davContext2 ), false, false, user );

        System.out.println( "URL: " + server.getURL() );

        _davRepo2 = new RemoteRepositoryM2( server, new MavenDependencyProcessor() );
    }

    // ---------------------------------------------------------------------------------------------
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( _dav != null )
        {
            _dav.stop();
            _dav.destroy();
            _dav = null;
        }

        if ( _dav2 != null )
        {
            _dav2.stop();
            _dav2.destroy();
            _dav2 = null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    public void testDavWrite()
        throws Exception
    {
        File jar = new File( "./target/test.jar" );
        FileUtil.writeRawData( jar, "test-jar" );

        File pom = new File( "./target/test.pom" );
        FileUtil.writeRawData( pom, "test-pom" );

        DefaultArtifact da = new DefaultArtifact( new ArtifactMetadata( "a:test:1.0" ) );
        da.setFile( jar );
        da.setPomBlob( FileUtil.readRawData( pom ) );

        List<Artifact> al = new ArrayList<Artifact>( 8 );
        al.add( da );

        RepositoryWriter rw = _davRepo.getWriter();

        rw.writeArtifacts( al );

        File davJar = new File( _base, "a/test/1.0/test-1.0.jar" );

        assertTrue( davJar.exists() );

        assertEquals( jar.length(), davJar.length() );
    }

    // ---------------------------------------------------------------------------------------------
    public void testDavRead()
        throws Exception
    {
        testDavWrite();

        ArtifactMetadata bmd = new ArtifactMetadata( "a:test:1.0" );

        List<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>( 1 );

        query.add( bmd );

        RepositoryReader rr = _davRepo.getReader();

        ArtifactResults res = rr.readArtifacts( query );

        assertNotNull( res );

        assertFalse( res.hasExceptions() );

        assertTrue( res.hasResults() );

        List<Artifact> al = res.getResults( bmd );

        assertNotNull( al );

        assertFalse( al.isEmpty() );

        Artifact a = al.get( 0 );

        assertNotNull( a );

        assertNotNull( a.getFile() );

        assertTrue( a.getFile().exists() );

        assertEquals( "test-pom".length(), a.getFile().length() );
    }

    // ---------------------------------------------------------------------------------------------
    public void testDavWrite2()
        throws Exception
    {
        File jar = new File( "./target/test.jar" );
        FileUtil.writeRawData( jar, "test-jar" );

        File pom = new File( "./target/test.pom" );
        FileUtil.writeRawData( pom, "test-pom" );

        DefaultArtifact da = new DefaultArtifact( new ArtifactMetadata( "a:test:1.0" ) );
        da.setFile( jar );
        da.setPomBlob( FileUtil.readRawData( pom ) );

        List<Artifact> al = new ArrayList<Artifact>( 8 );
        al.add( da );

        RepositoryWriter rw = _davRepo2.getWriter();

        rw.writeArtifacts( al );

        File davJar = new File( _base2, "a/test/1.0/test-1.0.jar" );

        assertTrue( davJar.exists() );

        assertEquals( jar.length(), davJar.length() );
    }

    // ---------------------------------------------------------------------------------------------
    public void testDavRead2()
        throws Exception
    {
        testDavWrite2();

        ArtifactMetadata bmd = new ArtifactMetadata( "a:test:1.0" );

        List<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>( 1 );

        query.add( bmd );

        RepositoryReader rr = _davRepo2.getReader();

        ArtifactResults res = rr.readArtifacts( query );

        assertNotNull( res );

        assertFalse( res.hasExceptions() );

        assertTrue( res.hasResults() );

        List<Artifact> al = res.getResults( bmd );

        assertNotNull( al );

        assertFalse( al.isEmpty() );

        Artifact a = al.get( 0 );

        assertNotNull( a );

        assertNotNull( a.getFile() );

        assertTrue( a.getFile().exists() );

        assertEquals( "test-pom".length(), a.getFile().length() );
    }
    // ---------------------------------------------------------------------------------------------
}
