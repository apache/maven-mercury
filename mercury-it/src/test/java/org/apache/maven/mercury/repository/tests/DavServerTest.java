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
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
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
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class DavServerTest
extends PlexusTestCase
{
    static final String _davContext = "/webdav";
    
    static final String _user = "foo";
    
    static final String _pass = "bar";
    
    WebDavServer _dav;
    
    File _base;
    
    RemoteRepositoryM2 _davRepo;

    //---------------------------------------------------------------------------------------------
    protected void setUp()
    throws Exception
    {
        super.setUp();
        
        _base = new File( "./target", _davContext );
        
        FileUtil.delete( _base );
        
        _base.mkdirs();
        
        _dav = new WebDavServer( 0, _base, _davContext, getContainer(), 9, "mercury-test" );
        
        _dav.start();
        
        Credentials user = new Credentials(_user,_pass);
        
        Server server = new Server("dav", new URL("http://localhost:"+_dav.getPort()+_davContext), false, false, user );
        
        System.out.println("URL: "+server.getURL() );
        
        _davRepo = new RemoteRepositoryM2( server, new MavenDependencyProcessor() );
    }
    //---------------------------------------------------------------------------------------------
    protected void tearDown()
    throws Exception
    {
        super.tearDown();
        
        if( _dav != null )
        {
            _dav.stop();
            _dav.destroy();
            _dav = null;
        }  
    }
    //---------------------------------------------------------------------------------------------
    public void testDavWrite()
    throws Exception
    {
        File jar = new File("./target/test.jar");
        FileUtil.writeRawData( jar, "test-jar" );

        File pom = new File("./target/test.pom");
        FileUtil.writeRawData( pom, "test-pom" );

        DefaultArtifact da = new DefaultArtifact( new ArtifactBasicMetadata("a:test:1.0") );
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
    //---------------------------------------------------------------------------------------------
    public void testDavRead()
    throws Exception
    {
        testDavWrite();
        
        ArtifactBasicMetadata bmd = new ArtifactBasicMetadata("a:test:1.0");
        
        List<ArtifactBasicMetadata> query = new ArrayList<ArtifactBasicMetadata>( 1 );

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
    //---------------------------------------------------------------------------------------------
}
