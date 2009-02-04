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
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Credentials;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;
import org.codehaus.plexus.PlexusTestCase;

/**
 * This set of UTs covers a comprehensive use case,
 * involving majority of Mercury repository functionality
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class ComprehensiveRepositoryTest
extends PlexusTestCase
{
    WebDavServer _server1;
    File         _base1;
    static final String _context1 = "/webdav1";
    int _port1;
    RemoteRepositoryM2 _rr1;
    
    WebDavServer _server2;
    File         _base2;
    static final String _context2 = "/webdav2";
    int _port2;
    RemoteRepositoryM2 _rr2;
    
    File _lbase1;
    static final String _local1 = "./target/webdav1local";
    LocalRepositoryM2 _lr1;
    
    File _lbase2;
    static final String _local2 = "./target/webdav2local";
    LocalRepositoryM2 _lr2;
    
    static final String _resourceBase = "./target/test-classes";
    
    
    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        
        DependencyProcessor dp = new MavenDependencyProcessor();
        Credentials user = new Credentials("foo","bar");
        
        _base1 = new File( "./target/webdav1" );
        FileUtil.delete( _base1 );
        _base1.mkdirs();
        _server1 = new WebDavServer( 0, _base1, _context1, getContainer(), 9, "mercury-test-1" );
        _server1.start();
        _port1 = _server1.getPort();
        
        Server server = new Server("rr1", new URL("http://localhost:"+_port1+_context1), false, false, user );
        _rr1 = new RemoteRepositoryM2( server, dp );
        
        _base2 = new File( "./target/webdav2" );
        FileUtil.delete( _base2 );
        _base2.mkdirs();
        _server2 = new WebDavServer( 0, _base2, _context2, getContainer(), 9, "mercury-test-2" );
        _server2.start();
        _port2 = _server2.getPort();
        
        server = new Server("rr2", new URL("http://localhost:"+_port2+_context2), false, false, user );
        _rr2 = new RemoteRepositoryM2( server, dp );
        
        _lbase1 = new File( _local1 );
        FileUtil.delete( _lbase1 );
        _lbase1.mkdirs();
        _lr1 = new LocalRepositoryM2( "lr1", _lbase1, dp );
        
        _lbase2 = new File( _local2 );
        FileUtil.delete( _lbase2 );
        _lbase2.mkdirs();
        _lr2 = new LocalRepositoryM2( "lr2", _lbase2, dp );
        
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        super.tearDown();
        
        if( _server1 != null )
            try
            {
                _server1.stop();
                _server1.destroy();
            }
            catch( Exception e ) {}
            finally { _server1 = null; }
            
        if( _server2 != null )
            try
            {
                _server2.stop();
                _server2.destroy();
            }
            catch( Exception e ) {}
            finally { _server2 = null; }
    }
    
    public void testWriteReadArtifact()
    throws Exception
    {
        File af = new File( _resourceBase, "maven-core-2.0.9.jar" );
        File ap = new File( _resourceBase, "maven-core-2.0.9.pom" );
        
        DefaultArtifact da = new DefaultArtifact( new ArtifactBasicMetadata("org.apache.maven:maven-core:2.0.9") );
        
        da.setPomBlob( FileUtil.readRawData( ap ) );
        da.setFile( af );
        List<Artifact> al = new ArrayList<Artifact>();
        al.add( da );
        
        _rr2.getWriter().writeArtifacts( al );
    }
}
