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

import org.apache.maven.mercury.spi.http.server.AuthenticatingTestServer;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.tools.ant.BuildFileTest;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class MercuryAntTest
    extends BuildFileTest
{
    static final String _localRepoDir = "./target/repo";

    static File _localRepoDirFile;

    static final String _writeRepoDir = "./target/test-repo";

    static File _writeRepoDirFile;

    static final String _verifyRepoDir = "./target/test-verify-repo";

    static File _verifyRepoDirFile;

    static final String _compileDir = "./target/compile-classes";

    static File _compileDirFile;

    static final String _jarDir = "./target/compile-target";

    static File _jarDirFile;

    static final String _remoteRepoDir = "./target/test-classes/remoteRepo";

    static File _remoteRepoDirFile;

    static final String _remoteRepoUrlPrefix = "http://localhost:";

    static final String _remoteRepoUrlSufix = "/maven2";

    static final String _pathId = "class-path";

    AuthenticatingTestServer _jetty;

    int _port;

    // -----------------------------------
    public MercuryAntTest( String name )
    {
        super( name );
    }

    // -----------------------------------
    @Override
    protected void setUp()
        throws Exception
    {
        _remoteRepoDirFile = new File( _remoteRepoDir );
        _jetty = new AuthenticatingTestServer( 0, _remoteRepoDirFile, _remoteRepoUrlSufix, false );
        _jetty.start();
        _port = _jetty.getPort();

        _writeRepoDirFile = new File( _writeRepoDir );
        FileUtil.delete( _writeRepoDirFile );
        _writeRepoDirFile.mkdirs();

        _verifyRepoDirFile = new File( _verifyRepoDir );
        FileUtil.delete( _verifyRepoDirFile );
        _verifyRepoDirFile.mkdirs();

        _compileDirFile = new File( _compileDir );
        FileUtil.delete( _compileDirFile );
        _compileDirFile.mkdirs();

        _jarDirFile = new File( _jarDir );
        FileUtil.delete( _jarDirFile );
        _jarDirFile.mkdirs();

        configureProject( "build.xml" );
        getProject().setProperty( "repo.port", "" + _port );
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
    public void testCompileFail()
    {
        String title = "compile-fail";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        try
        {
            executeTarget( title );

            fail( title + " did not raise an exception" );
        }
        catch ( Throwable e )
        {
            System.out.println( "Expected exception: " + e.getMessage() );
        }

        assertFalse( af.exists() );
    }

    // -----------------------------------
    public void testCompile()
    {
        String title = "compile";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        File jar = new File( _jarDirFile, "t.jar" );

        assertFalse( jar.exists() );

        executeTarget( title );

        assertTrue( af.exists() );

        assertTrue( jar.exists() );
    }

    // -----------------------------------
    public void testCompileThinPath()
    {
        String title = "compile-thin-path";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        executeTarget( title );

        assertTrue( af.exists() );
    }

    // -----------------------------------
    public void testCompileThinPath2()
    {
        String title = "compile-thin-path-2";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        executeTarget( title );

        assertTrue( af.exists() );
    }

    // -----------------------------------
    public void testCompileThinPath3()
        throws Exception
    {
        String title = "compile-thin-path-3";
        System.out.println( "========> start " + title );
        System.out.flush();

        restart( _port, _remoteRepoDirFile, "/maven2", true );

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        File asm = new File( "target/path-3/asm/asm/3.0/asm-3.0.jar" );

        FileUtil.delete( asm );

        asm.delete();

        assertFalse( asm.exists() );

        executeTarget( title );

        assertTrue( af.exists() );

        assertTrue( asm.exists() );
    }

    // -----------------------------------
    public void testCompileThinPathPom()
        throws Exception
    {
        String title = "compile-thin-path-pom";
        System.out.println( "========> start " + title );
        System.out.flush();

        restart( _port, _remoteRepoDirFile, "/maven2", true );

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        File asm = new File( "target/path-pom/asm/asm/3.0/asm-3.0.jar" );

        FileUtil.delete( asm );

        asm.delete();

        assertFalse( asm.exists() );

        executeTarget( title );

        assertTrue( af.exists() );

        assertTrue( asm.exists() );
    }

    // -----------------------------------
    public void testCompileOldSyntax()
        throws Exception
    {
        String title = "compile-old-syntax";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        File asm = new File( "target/path-old/asm/asm/3.0/asm-3.0.jar" );

        FileUtil.delete( asm );

        asm.delete();

        assertFalse( asm.exists() );

        executeTarget( title );

        assertTrue( af.exists() );

        assertTrue( asm.exists() );
    }

    // -----------------------------------
    public void testCompileOldSyntaxWithAuth()
        throws Exception
    {
        String title = "compile-old-syntax-with-auth";
        System.out.println( "========> start " + title );
        System.out.flush();

        restart( _port, _remoteRepoDirFile, "/maven2", true );

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        File asm = new File( "target/path-old-auth/asm/asm/3.0/asm-3.0.jar" );

        FileUtil.delete( asm );

        asm.delete();

        assertFalse( asm.exists() );

        executeTarget( title );

        assertTrue( af.exists() );

        assertTrue( asm.exists() );
    }

    // -----------------------------------
    public void testBadAuthRepo()
        throws Exception
    {
        String title = "compile-auth";
        System.out.println( "========> start " + title );
        System.out.flush();

        restart( _port, _remoteRepoDirFile, "/maven2", true );

        try
        {
            executeTarget( "compile-bad-auth" );
            fail( "accessing authenticated repo without password succeded - failing test" );
        }
        catch ( Exception e )
        {
            System.out.println( "Expected exception: " + e.getMessage() );
        }
    }

    // -----------------------------------
    public void testAuthRepo()
        throws Exception
    {
        String title = "compile-auth";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        File jar = new File( _jarDirFile, "t-auth.jar" );

        assertFalse( jar.exists() );

        restart( _port, _remoteRepoDirFile, "/maven2", true );

        executeTarget( "compile-auth" );

        assertTrue( af.exists() );
        assertTrue( jar.exists() );
    }

    // -----------------------------------
    public void testWriteToRepository()
    {
        String title = "deploy";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _writeRepoDirFile, "/t/t/1.0/t-1.0.jar" );
        assertFalse( af.exists() );

        File ap = new File( _writeRepoDirFile, "/t/t/1.0/t-1.0.pom" );
        assertFalse( ap.exists() );

        executeTarget( title );

        assertTrue( af.exists() );
        assertTrue( ap.exists() );
    }

    // -----------------------------------
    public void testVerifyWritePgp()
    {
        String title = "write-verify-pgp";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _verifyRepoDirFile, "/t/t/1.0/t-1.0.jar" );
        assertFalse( af.exists() );

        File sig = new File( _verifyRepoDirFile, "/t/t/1.0/t-1.0.jar.asc" );
        assertFalse( sig.exists() );

        executeTarget( "deploy-verify" );

        assertTrue( af.exists() );
        assertTrue( sig.exists() );
    }

    // -----------------------------------
    public void testVerifyReadBadPgp()
        throws Exception
    {
        String title = "verify-read-bad-pgp";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _verifyRepoDirFile, "t/bad/1.0/bad-1.0.jar" );
        assertFalse( af.exists() );

        try
        {
            executeTarget( "bad-pgp" );

            fail( "reading bad pgp signature did not trigger an exception. Failing the test" );
        }
        catch ( Exception e )
        {
            System.out.println( "Expected exception: " + e.getMessage() );
        }

        assertFalse( af.exists() );
    }

    // -----------------------------------
    public void testVerifyReadGoodPgp()
        throws Exception
    {
        String title = "verify-read-good-pgp";
        System.out.println( "========> start " + title );
        System.out.flush();

        if ( File.pathSeparatorChar == ';' )
        {
            System.out.println( "PGP test temporarily disabled under Windows,\nbecause all files were signed under Unix\nand SVN properties make checkout to convert EOL,\nbreaking signatures"
                + title );
            return;
        }

        File af = new File( _verifyRepoDirFile, "t/t/1.0/t-1.0.jar" );
        assertFalse( af.exists() );

        executeTarget( "good-pgp" );

        assertTrue( af.exists() );
    }

    // -----------------------------------
    // -----------------------------------
    public void testDefaultPathId()
        throws Exception
    {
        String title = "test-default-path-id";
        System.out.println( "========> start " + title );
        System.out.flush();

        File af = new File( _compileDirFile, "T.class" );

        assertFalse( af.exists() );

        File asm = new File( "target/defaul-path-id/asm/asm/3.0/asm-3.0.jar" );

        FileUtil.delete( asm );

        assertFalse( asm.exists() );

        executeTarget( title );

        assertTrue( af.exists() );

        assertTrue( asm.exists() );
    }
}
