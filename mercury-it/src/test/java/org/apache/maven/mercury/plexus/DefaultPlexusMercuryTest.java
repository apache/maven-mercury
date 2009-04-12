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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.maven.mercury.MavenDependencyProcessor;
import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactExclusionList;
import org.apache.maven.mercury.artifact.ArtifactInclusionList;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactQueryList;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.artifact.DefaultArtifact;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.crypto.api.StreamVerifierFactory;
import org.apache.maven.mercury.crypto.pgp.PgpStreamVerifierFactory;
import org.apache.maven.mercury.crypto.sha.SHA1VerifierFactory;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.repository.virtual.VirtualRepositoryReader;
import org.apache.maven.mercury.spi.http.server.HttpTestServer;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

/**
 * @author Oleg Gusakov
 * @version $Id: DefaultPlexusMercuryTest.java 723125 2008-12-03 23:19:50Z ogusakov $
 */
public class DefaultPlexusMercuryTest
    extends PlexusTestCase
{
    PlexusMercury pm;

    RemoteRepositoryM2 remoteRepo;

    LocalRepositoryM2 localRepo;

    List<Repository> repos;

    Artifact a;

    protected static final String keyId = "0EDB5D91141BC4F2";

    protected static final String secretKeyFile = "/pgp/secring.gpg";

    protected static final String publicKeyFile = "/pgp/pubring.gpg";

    protected static final String secretKeyPass = "testKey82";

    String artifactCoord = "org.apache.maven.mercury:mercury-repo-virtual:1.0.0-alpha-2-SNAPSHOT";

    private File localRepoDir;

    public static final String SYSTEM_PARAMETER_PLEXUS_MERCURY_TEST_USER = "plexus.mercury.test.user";

    static String remoteServerUser = System.getProperty( SYSTEM_PARAMETER_PLEXUS_MERCURY_TEST_USER, "admin" );

    public static final String SYSTEM_PARAMETER_PLEXUS_MERCURY_TEST_PASS = "plexus.mercury.test.pass";

    static String remoteServerPass = System.getProperty( SYSTEM_PARAMETER_PLEXUS_MERCURY_TEST_PASS, "admin123" );

    PgpStreamVerifierFactory pgpRF;

    PgpStreamVerifierFactory pgpWF;

    SHA1VerifierFactory sha1F;

    HashSet<StreamVerifierFactory> vFacSha1;

    VirtualRepositoryReader vrr;

    PlexusContainer plexus;

    HttpTestServer _jetty;

    String _port;

    File _remoteRepoBase = new File( "./target/test-classes/remoteRepo" );

    // -------------------------------------------------------------------------------------
    // @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        // prep. Artifact
        File artifactBinary = File.createTempFile( "test-repo-writer", "bin" );
        artifactBinary.deleteOnExit();
        FileUtil.writeRawData( getClass().getResourceAsStream( "/maven-core-2.0.9.jar" ), artifactBinary );

        a = new DefaultArtifact( new ArtifactMetadata( "org.apache.maven.mercury:mercury-core:2.0.9" ) );

        a.setPomBlob( FileUtil.readRawData( getClass().getResourceAsStream( "/maven-core-2.0.9.pom" ) ) );
        a.setFile( artifactBinary );

        // prep Repository
        pm = getContainer().lookup( PlexusMercury.class );

        pgpRF = pm.createPgpReaderFactory( true, true, getClass().getResourceAsStream( publicKeyFile ) );
        pgpWF =
            pm.createPgpWriterFactory( true, true, getClass().getResourceAsStream( secretKeyFile ), keyId,
                                       secretKeyPass );

        sha1F = new SHA1VerifierFactory( true, false );

        _jetty = new HttpTestServer( _remoteRepoBase, "/repo" );
        _jetty.start();
        _port = String.valueOf( _jetty.getPort() );

        String remoteServerUrl = "http://localhost:" + _port + "/repo";
        remoteRepo =
            pm.constructRemoteRepositoryM2( "testRepo", new URL( remoteServerUrl ), remoteServerUser, remoteServerPass,
                                            null, null, null, null, FileUtil.vSet( pgpRF, sha1F ), null,
                                            FileUtil.vSet( pgpWF, sha1F ) );

        // localRepoDir = File.createTempFile( "local-", "-repo" );
        localRepoDir = new File( "./target/local" );
        FileUtil.delete( localRepoDir );
        localRepoDir.mkdirs();
        //    
        // localRepo = new LocalRepositoryM2( "testLocalRepo", localRepoDir, pm.findDependencyProcessor() );

        localRepo = pm.constructLocalRepositoryM2( "testLocal", localRepoDir, null, null, null, null );

        repos = new ArrayList<Repository>();
        repos.add( localRepo );
        repos.add( remoteRepo );

        vrr = new VirtualRepositoryReader( repos );

    }

    // -------------------------------------------------------------------------------------
    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();

        if ( _jetty != null )
            try
            {
                _jetty.stop();
                _jetty.destroy();
            }
            finally
            {
                _jetty = null;
            }
    }

    // ----------------------------------------------------------------------------------------------
    private static boolean assertHasArtifact( List<ArtifactMetadata> res, String gav )
    {
        ArtifactMetadata gavMd = new ArtifactMetadata( gav );

        for ( ArtifactMetadata md : res )
            if ( md.sameGAV( gavMd ) )
                return true;

        return false;
    }

    // -------------------------------------------------------------------------------------
    public void testFindDepProcessorWithHint()
        throws RepositoryException, ComponentLookupException
    {
        DependencyProcessor dp = null;

        dp = pm.findDependencyProcessor( "default" );

        assertNotNull( dp );

        assertTrue( MavenDependencyProcessor.class.isAssignableFrom( dp.getClass() ) );
    }

    // -------------------------------------------------------------------------------------
    public void testFindDepProcessor()
        // should run after the previous one
        throws RepositoryException, ComponentLookupException
    {
        DependencyProcessor dp = null;

        dp = pm.findDependencyProcessor();

        assertNotNull( dp );

        assertTrue( MavenDependencyProcessor.class.isAssignableFrom( dp.getClass() ) );
    }

    // -------------------------------------------------------------------------------------
    public void testWrite()
        throws RepositoryException
    {
        pm.write( localRepo, a );

        File af = new File( localRepoDir, "org/apache/maven/mercury/mercury-core/2.0.9/mercury-core-2.0.9.jar" );

        assertTrue( af.exists() );
    }

    // -------------------------------------------------------------------------------------
    public void testReadVersions()
        throws RepositoryException
    {
        ArtifactMetadata bmd = new ArtifactMetadata( artifactCoord );

        List<ArtifactMetadata> res = pm.readVersions( repos, bmd );

        assertNotNull( res );

        assertFalse( res.isEmpty() );

        ArtifactMetadata a = res.get( 0 );

        assertEquals( "1.0.0-alpha-2-20081104.001322-2", a.getVersion() );

        List<Artifact> al = pm.read( repos, a );

        assertNotNull( al );

        assertFalse( al.isEmpty() );

        assertEquals( 1, al.size() );

    }

    // -------------------------------------------------------------------------------------
    public void testRead()
        throws RepositoryException
    {
        ArtifactMetadata bmd = new ArtifactMetadata( artifactCoord );

        Collection<Artifact> res = pm.read( repos, bmd );

        assertNotNull( res );

        assertFalse( res.isEmpty() );

        Artifact a = res.toArray( new Artifact[1] )[0];

        assertNotNull( a );

        File fBin = a.getFile();

        assertNotNull( fBin );

        assertTrue( fBin.exists() );

        byte[] pomBytes = a.getPomBlob();

        assertNotNull( pomBytes );

        assertTrue( pomBytes.length > 10 );
    }

    // -------------------------------------------------------------------------------------
    public void testReadNonExistent()
    {
        ArtifactMetadata bmd = new ArtifactMetadata( "does.not:exist:1.0" );

        Collection<Artifact> res = null;
        try
        {
            res = pm.read( repos, bmd );
        }
        catch ( RepositoryException e )
        {
            fail( "reading non-existent artifact should not raise an exception, got " + e.getMessage() );
        }

        assertNull( res );
    }

    // -------------------------------------------------------------------------------------
    public void testResolveNonExistent()
    {
        ArtifactMetadata bmd = new ArtifactMetadata( "does.not:exist:1.0" );

        Collection<ArtifactMetadata> res = null;
        try
        {
            res = pm.resolve( repos, ArtifactScopeEnum.compile, bmd );
        }
        catch ( RepositoryException e )
        {
            fail( "reading non-existent artifact should not raise an exception, got " + e.getMessage() );
        }
    }

    // -------------------------------------------------------------------------------------
    public void testResolve()
        throws Exception
    {
        Server central = new Server( "central", new URL( "http://repo1.maven.org/maven2" ) );
        // Server central = new Server( "central", new URL("http://repository.sonatype.org/content/groups/public") );

        repos.add( new RemoteRepositoryM2( central, pm.findDependencyProcessor() ) );

        String artifactId = "asm:asm-xml:3.0";

        List<ArtifactMetadata> res =
            pm.resolve( repos, ArtifactScopeEnum.compile, new ArtifactQueryList( artifactId ), null, null );

        System.out.println( "Resolved as " + res );

        assertEquals( 4, res.size() );

        assertTrue( assertHasArtifact( res, "asm:asm-xml:3.0" ) );
        assertTrue( assertHasArtifact( res, "asm:asm-util:3.0" ) );
        assertTrue( assertHasArtifact( res, "asm:asm-tree:3.0" ) );
        assertTrue( assertHasArtifact( res, "asm:asm:3.0" ) );
    }

    // -------------------------------------------------------------------------------------
    public void testResolveAsTree()
        throws Exception
    {
        Server central = new Server( "central", new URL( "http://repo1.maven.org/maven2" ) );
        // Server central = new Server( "central", new URL("http://repository.sonatype.org/content/groups/public") );

        repos.add( new RemoteRepositoryM2( central, pm.findDependencyProcessor() ) );

        String artifactId = "asm:asm-xml:3.0";

        MetadataTreeNode res =
            pm.resolveAsTree( repos, ArtifactScopeEnum.compile, new ArtifactQueryList( artifactId ), null, null );

        System.out.println( "Resolved as tree:" );
        MetadataTreeNode.showNode( res, 0 );
        
        assertNotNull( res );
        
        assertTrue( res.hasChildren() );
        
        int nodes = res.countNodes();

        /* tree structure:
            0 asm:asm-xml:3.0::jar
              1 asm:asm-util:3.0::jar
                2 asm:asm-tree:3.0::jar
                  3 asm:asm:3.0::jar
         */
        
        assertEquals( 4, nodes );

        assertTrue( res.getMd().equals( new ArtifactMetadata( "asm:asm-xml:3.0" ) ) );
        assertTrue( res.getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm-util:3.0" ) ) );
        assertTrue( res.getChildren().get( 0 ).getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm-tree:3.0" ) ) );
        assertTrue( res.getChildren().get( 0 ).getChildren().get( 0 ).getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm:3.0" ) ) );
    }
    // -------------------------------------------------------------------------------------
    public void testResolveListAsTree()
        throws Exception
    {
        Server central = new Server( "central", new URL( "http://repo1.maven.org/maven2" ) );
        // Server central = new Server( "central", new URL("http://repository.sonatype.org/content/groups/public") );

        repos.add( new RemoteRepositoryM2( central, pm.findDependencyProcessor() ) );

        String artifactId = "asm:asm-xml:3.0";

        String artifactId2 = "cobertura:cobertura:1.8";

        MetadataTreeNode res =
            pm.resolveAsTree( repos, ArtifactScopeEnum.test, new ArtifactQueryList( artifactId, artifactId2 ), null, null );

        System.out.println( "Resolved as tree:" );
        MetadataTreeNode.showNode( res, 0 );
        
        assertNotNull( res );
        
        assertTrue( res.hasChildren() );
        
        int nodes = res.countNodes();

        /* tree structure:
            0 asm:asm-xml:3.0::jar
              1 asm:asm-util:3.0::jar
                2 asm:asm-tree:3.0::jar
                  3 asm:asm:3.0::jar
         */
        
//        assertEquals( 4, nodes );
//
//        assertTrue( res.getMd().equals( new ArtifactMetadata( "asm:asm-xml:3.0" ) ) );
//        assertTrue( res.getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm-util:3.0" ) ) );
//        assertTrue( res.getChildren().get( 0 ).getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm-tree:3.0" ) ) );
//        assertTrue( res.getChildren().get( 0 ).getChildren().get( 0 ).getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm:3.0" ) ) );

        List<ArtifactMetadata> res2 =
            pm.resolve( repos, ArtifactScopeEnum.test, new ArtifactQueryList( artifactId, artifactId2 ), null, null );
        
        System.out.println("\n============== as List =========");
        if( res2 != null )
            for( ArtifactMetadata a : res2 )
                System.out.println( a );
    }

    // -------------------------------------------------------------------------------------
    public void testResolvePomAsTree()
        throws Exception
    {
        Server central = new Server( "central", new URL( "http://repo1.maven.org/maven2" ) );
        // Server central = new Server( "central", new URL("http://repository.sonatype.org/content/groups/public") );

        repos.add( new RemoteRepositoryM2( central, pm.findDependencyProcessor() ) );

        String artifactId = "asm:asm-xml:3.0::pom";

        MetadataTreeNode res =
            pm.resolveAsTree( repos, ArtifactScopeEnum.compile, new ArtifactQueryList( artifactId ), null, null );

        System.out.println( "Resolved as tree:" );
        MetadataTreeNode.showNode( res, 0 );
        
        assertNotNull( res );
        
        assertTrue( res.hasChildren() );
        
        int nodes = res.countNodes();

        /* tree structure:
            0 asm:asm-xml:3.0::pom
              1 asm:asm-util:3.0::jar
                2 asm:asm-tree:3.0::jar
                  3 asm:asm:3.0::jar
         */
        
        assertEquals( 4, nodes );

        assertTrue( res.getMd().equals( new ArtifactMetadata( "asm:asm-xml:3.0::pom" ) ) );
        assertTrue( res.getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm-util:3.0" ) ) );
        assertTrue( res.getChildren().get( 0 ).getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm-tree:3.0" ) ) );
        assertTrue( res.getChildren().get( 0 ).getChildren().get( 0 ).getChildren().get( 0 ).getMd().equals( new ArtifactMetadata( "asm:asm:3.0" ) ) );
    }

    // -------------------------------------------------------------------------------------
    @SuppressWarnings( "unchecked" )
    public void testResolveWithExclusion()
        throws Exception
    {
        // Server central = new Server( "central", new URL("http://repo1.maven.org/maven2") );
        // Server central = new Server( "central", new URL("http://repository.sonatype.org/content/groups/public") );

        // repos.add( new RemoteRepositoryM2(central, pm.findDependencyProcessor()) );

        String artifactId = "asm:asm-xml:3.0";

        List<ArtifactMetadata> res =
            pm.resolve( repos, ArtifactScopeEnum.compile, new ArtifactQueryList( artifactId ), null,
                        new ArtifactExclusionList( "asm:asm:3.0" ) );

        System.out.println( "Resolved as " + res );

        assertEquals( 3, res.size() );

        assertTrue( assertHasArtifact( res, "asm:asm-xml:3.0" ) );
        assertTrue( assertHasArtifact( res, "asm:asm-util:3.0" ) );
        assertTrue( assertHasArtifact( res, "asm:asm-tree:3.0" ) );
        assertFalse( assertHasArtifact( res, "asm:asm:3.0" ) );
    }

    // -------------------------------------------------------------------------------------
    @SuppressWarnings( "unchecked" )
    public void testResolveWithInclusion()
        throws Exception
    {
        // Server central = new Server( "central", new URL("http://repo1.maven.org/maven2") );
        // Server central = new Server( "central", new URL("http://repository.sonatype.org/content/groups/public") );

        // repos.add( new RemoteRepositoryM2(central, pm.findDependencyProcessor()) );

        String artifactId = "asm:asm-xml:3.0";

        List<ArtifactMetadata> res =
            pm.resolve( repos, ArtifactScopeEnum.compile, new ArtifactQueryList( artifactId ),
                        new ArtifactInclusionList( "asm:asm-xml:3.0", "asm:asm-util:3.0" ), null );

        System.out.println( "Resolved as " + res );

        assertEquals( 2, res.size() );

        assertTrue( assertHasArtifact( res, "asm:asm-xml:3.0" ) );
        assertTrue( assertHasArtifact( res, "asm:asm-util:3.0" ) );
        assertFalse( assertHasArtifact( res, "asm:asm-tree:3.0" ) );
        assertFalse( assertHasArtifact( res, "asm:asm:3.0" ) );
    }
    // -------------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------------
}
