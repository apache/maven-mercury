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

import java.io.File;
import java.io.IOException;

import org.apache.maven.mercury.spi.http.server.AuthenticatingTestServer;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.tools.ant.BuildFileTest;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class MercuryBootstrapTest
    extends BuildFileTest
{
    static final String _localRepoDir = "./target/repo-bootstrap";

    static File _localRepoDirFile;

    static final String _writeRepoDir = "./target/test-repo-flat";

    static File _writeRepoDirFile;

    static final String _remoteRepoDir = "./target/test-classes/remoteRepoBoot";

    static File _remoteRepoDirFile;

    static final String _remoteRepoUrlPrefix = "http://localhost:";

    static final String _remoteRepoUrlSufix = "/maven2";

    static final String _pathId = "class-path";

    File _testCopyDir;

    File _testCopyRuntimeDir;

    AuthenticatingTestServer _jetty;

    int _port;

    static final int DEFAULT_PORT = 22883;

    // -----------------------------------
    public MercuryBootstrapTest( String name )
    {
        super( name );
    }

    // -----------------------------------
    @Override
    protected void setUp()
        throws Exception
    {
        _localRepoDirFile = new File( _localRepoDir );
        FileUtil.delete( _localRepoDirFile );
        _localRepoDirFile.mkdirs();

        _testCopyDir = new File( "target/test-copy" );
        FileUtil.delete( _testCopyDir );
        _testCopyDir.mkdirs();

        _testCopyRuntimeDir = new File( "target/test-copy-runtime" );
        FileUtil.delete( _testCopyRuntimeDir );
        _testCopyRuntimeDir.mkdirs();

        _remoteRepoDirFile = new File( _remoteRepoDir );
        _jetty = new AuthenticatingTestServer( 0, _remoteRepoDirFile, _remoteRepoUrlSufix, false );
        _jetty.start();
        _port = _jetty.getPort();

        _writeRepoDirFile = new File( _writeRepoDir );
        FileUtil.delete( _writeRepoDirFile );
        _writeRepoDirFile.mkdirs();

        configureProject( "mercury.xml" );

        // System.setProperty( Config.SYSTEM_PROPERTY_LOCAL_DIR_NAME, _localRepoDir );
        getProject().setProperty( "localRepo", _localRepoDir );

        // System.setProperty( Config.SYSTEM_PROPERTY_CENTRAL_URL, _remoteRepoUrlPrefix+_port+_remoteRepoUrlSufix );
        getProject().setProperty( "remoteRepo", _remoteRepoUrlPrefix + _port + _remoteRepoUrlSufix );
    }

    // -----------------------------------
    private void restart( int port, File localBase, String remotePathFragment, boolean secured )
        throws Exception
    {
        tearDown();

        _jetty = new AuthenticatingTestServer( port, localBase, remotePathFragment, secured );
        _jetty.start();

        this._port = port;
    }

    // -----------------------------------
    @Override
    protected void tearDown()
        throws Exception
    {
        if ( _jetty != null )
        {
            _jetty.stop();
            _jetty.destroy();

            System.out.println( "Jetty on :" + _port + " destroyed\n<========\n\n" );
        }
    }

    // -----------------------------------
    public void testDownload()
    {
        String title = "download";
        System.out.println( "========> start " + title );
        System.out.flush();

        File a0 = new File( _localRepoDirFile, "g0/a0/v0/a0-v0.jar" );
        File a1 = new File( _localRepoDirFile, "g1/a1/v1/a1-v1.jar" );
        File a2 = new File( _localRepoDirFile, "g2/a2/v2/a2-v2.jar" );

        assertFalse( a0.exists() );
        assertFalse( a1.exists() );
        assertFalse( a2.exists() );

        executeTarget( title );

        assertTrue( a0.exists() );
        assertTrue( a1.exists() );
        assertTrue( a2.exists() );
    }

    // -----------------------------------
    public void testDownloadPom()
    {
        String title = "download-pom";
        System.out.println( "========> start " + title );
        System.out.flush();

        File a0 = new File( _localRepoDirFile, "g0/a0/v0/a0-v0.jar" );
        File a1 = new File( _localRepoDirFile, "g1/a1/v1/a1-v1.jar" );
        File a2 = new File( _localRepoDirFile, "g2/a2/v2/a2-v2.jar" );

        assertFalse( a0.exists() );
        assertFalse( a1.exists() );
        assertFalse( a2.exists() );

        executeTarget( title );

        assertTrue( a0.exists() );
        assertTrue( a1.exists() );
        assertTrue( a2.exists() );
    }

    // -----------------------------------
    public void testDownloadPomNonTransitive()
        throws IOException
    {
        String title = "download-pom-non-transtive";
        System.out.println( "========> start " + title );
        System.out.flush();

        // mercury.classpath
        File a0 = new File( _localRepoDirFile, "g0/a0/v0/a0-v0.jar" );
        File a1 = new File( _localRepoDirFile, "g1/a1/v1/a1-v1.jar" );
        File a2 = new File( _localRepoDirFile, "g2/a2/v2/a2-v2.jar" );

        assertFalse( a0.exists() );
        assertFalse( a1.exists() );
        assertFalse( a2.exists() );

        // mercury.fileset
        File tc0 = new File( _testCopyDir, "a0-v0.jar" );
        File tc1 = new File( _testCopyDir, "a1-v1.jar" );
        File tc2 = new File( _testCopyDir, "a2-v2.jar" );

        assertFalse( tc0.exists() );
        assertFalse( tc1.exists() );
        assertFalse( tc2.exists() );

        // mercury.fileset.runtime
        File tcr0 = new File( _testCopyRuntimeDir, "a0-v0.jar" );
        File tcr1 = new File( _testCopyRuntimeDir, "a1-v1.jar" );
        File tcr2 = new File( _testCopyRuntimeDir, "a2-v2.jar" );

        assertFalse( tcr0.exists() );
        assertFalse( tcr1.exists() );
        assertFalse( tcr2.exists() );

        executeTarget( title );

        assertTrue( a0.exists() );
        assertTrue( a1.exists() );
        assertFalse( a2.exists() );

        String cp = getProject().getProperty( "cp" );

        // System.out.println( "cp = " + cp );

        // mercury.classpath
        assertTrue( cp.indexOf( a0.getCanonicalPath() ) != -1 );
        assertTrue( cp.indexOf( a1.getCanonicalPath() ) != -1 );
        assertTrue( cp.indexOf( a2.getCanonicalPath() ) == -1 );

        // mercury.fileset
        assertTrue( tc0.exists() );
        assertTrue( tc1.exists() );
        assertFalse( tc2.exists() );

        // mercury.fileset.runtime
        assertTrue( tcr0.exists() );
        assertTrue( tcr1.exists() );
        assertFalse( tcr2.exists() );
    }

    // -----------------------------------
    public void testExclusions()
    {
        String title = "exclusions";
        System.out.println( "========> start " + title );
        System.out.flush();

        File a0 = new File( _localRepoDirFile, "g0/a0/v0/a0-v0.jar" );
        File a1 = new File( _localRepoDirFile, "g1/a1/v1/a1-v1.jar" );
        File a2 = new File( _localRepoDirFile, "g2/a2/v2/a2-v2.jar" );

        assertFalse( a0.exists() );
        assertFalse( a1.exists() );
        assertFalse( a2.exists() );

        executeTarget( title );

        assertTrue( a0.exists() );
        assertTrue( a1.exists() );
        assertFalse( a2.exists() );

        String cp = getProject().getProperty( "cp" );
        assertNotNull( cp );

        // mercury.classpath
        assertTrue( cp.indexOf( "a0" ) != -1 );
        assertTrue( cp.indexOf( "a1" ) != -1 );
        assertTrue( cp.indexOf( "a2" ) == -1 );

        // System.out.println( "cp = " + cp );
    }

    // -----------------------------------
    public void testRepoPgp()
    {
        String title = "repo-pgp";
        System.out.println( "========> start " + title );
        System.out.flush();

        File a0 = new File( _localRepoDirFile, "g0/a0/v0/a0-v0.jar" );
        File a1 = new File( _localRepoDirFile, "g1/a1/v1/a1-v1.jar" );
        File a2 = new File( _localRepoDirFile, "g2/a2/v2/a2-v2.jar" );

        assertFalse( a0.exists() );
        assertFalse( a1.exists() );
        assertFalse( a2.exists() );

        executeTarget( title );

        // TODO: prep. the test data to test pgp sigs
        //
        // assertTrue( a0.exists() );
        // assertTrue( a1.exists() );
        // assertTrue( a2.exists() );
    }
    // -----------------------------------
    // -----------------------------------
}
