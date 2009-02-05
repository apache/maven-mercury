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
import org.apache.maven.mercury.repository.api.ArtifactBasicResults;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.local.m2.LocalRepositoryM2;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.repository.virtual.VirtualRepositoryReader;
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
    
    List<Repository> _rrs;
    List<Repository> _lrs;
    List<Repository> _repos;
    
    
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
        
        _rrs = new ArrayList<Repository>(2);
        _rrs.add( _rr1 );
        _rrs.add( _rr2 );
        
        _lbase1 = new File( _local1 );
        FileUtil.delete( _lbase1 );
        _lbase1.mkdirs();
        _lr1 = new LocalRepositoryM2( "lr1", _lbase1, dp );
        
        _lbase2 = new File( _local2 );
        FileUtil.delete( _lbase2 );
        _lbase2.mkdirs();
        _lr2 = new LocalRepositoryM2( "lr2", _lbase2, dp );
        
        _lrs = new ArrayList<Repository>(2);
        _lrs.add( _lr1 );
        _lrs.add( _lr2 );
        
        _repos = new ArrayList<Repository>();
        _repos.addAll( _rrs );
        _repos.addAll( _lrs );
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
    
    public void writeArtifact( String name, File af, File ap, Repository repo )
    throws Exception
    {
        DefaultArtifact da = new DefaultArtifact( new ArtifactBasicMetadata(name) );
        
        da.setPomBlob( FileUtil.readRawData( ap ) );
        da.setFile( af );
        List<Artifact> al = new ArrayList<Artifact>();
        al.add( da );
        
        repo.getWriter().writeArtifacts( al );
    }
    
    public List<Artifact> readArtifact( String name , List<Repository> repos )
    throws Exception
    {
        ArtifactBasicMetadata bmd = new ArtifactBasicMetadata(name);
        
        List<ArtifactBasicMetadata> al = new ArrayList<ArtifactBasicMetadata>();
        al.add( bmd );
        
        VirtualRepositoryReader vr = new VirtualRepositoryReader( repos );
        
        ArtifactResults  res = vr.readArtifacts( al );
        
        assertNotNull( res );
        
        if( res.hasExceptions() )
            System.out.println( res.getExceptions() );
        
        assertTrue( res.hasResults(bmd) );
        
        return res.getResults( bmd );
    }
    
    public List<ArtifactBasicMetadata> readVersions( String name , List<Repository> repos )
    throws Exception
    {
        ArtifactBasicMetadata bmd = new ArtifactBasicMetadata(name);
        
        List<ArtifactBasicMetadata> al = new ArrayList<ArtifactBasicMetadata>();
        al.add( bmd );
        
        VirtualRepositoryReader vr = new VirtualRepositoryReader( repos );
        
        ArtifactBasicResults  res = vr.readVersions( al );
        
        assertNotNull( res );
        
        if( res.hasExceptions() )
            System.out.println( res.getExceptions() );
        
        assertTrue( res.hasResults(bmd) );
        
        return res.getResult( bmd );
    }
    
    public void testWriteReadArtifact()
    throws Exception
    {
        String name = "org.apache.maven:maven-core:2.0.9";
        
        File af = new File( _resourceBase, "maven-core-2.0.9.jar" );
        File ap = new File( _resourceBase, "maven-core-2.0.9.pom" );
        
        File aJar1 = new File( _base1, "org/apache/maven/maven-core/2.0.9/maven-core-2.0.9.jar");
        File aJar2 = new File( _base2, "org/apache/maven/maven-core/2.0.9/maven-core-2.0.9.jar");
        
        assertFalse( aJar1.exists() );
        assertFalse( aJar2.exists() );
        
        writeArtifact( name, af, ap, _rr2 );
        
        assertFalse( aJar1.exists() );
        assertTrue( aJar2.exists() );
        
        List<Artifact> al = readArtifact( name, _rrs );
        
        System.out.println(al);
        
        File localRepo1Jar = new File( _lbase1, "org/apache/maven/maven-core/2.0.9/maven-core-2.0.9.jar" );
        File localRepo2Jar = new File( _lbase2, "org/apache/maven/maven-core/2.0.9/maven-core-2.0.9.jar" );
        
        assertFalse( localRepo1Jar.exists() );
        assertFalse( localRepo2Jar.exists() );
        
        al = readArtifact( name, _repos );
        
        assertTrue( localRepo1Jar.exists() );
        assertFalse( localRepo2Jar.exists() );
    }
    
    public void testWriteReadTimeStamp()
    throws Exception
    {
        String name = "org.apache.maven:maven-core:2.0.9-20090204.232323-23";
        
        File af = new File( _resourceBase, "maven-core-2.0.9.jar" );
        File ap = new File( _resourceBase, "maven-core-2.0.9.pom" );
        
        File aJar1 = new File( _base1, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20090204.232323-23.jar");
        File aJar2 = new File( _base2, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20090204.232323-23.jar");
        
        assertFalse( aJar1.exists() );
        assertFalse( aJar2.exists() );
        
        writeArtifact( name, af, ap, _rr2 );
        
        assertFalse( aJar1.exists() );
        assertTrue( aJar2.exists() );
        
        List<Artifact> al = readArtifact( name, _rrs );
        
        System.out.println(al);
        
        File localRepo1Jar = new File( _lbase1, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20090204.232323-23.jar" );
        File localRepo2Jar = new File( _lbase2, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20090204.232323-23.jar" );
        
        assertFalse( localRepo1Jar.exists() );
        assertFalse( localRepo2Jar.exists() );
        
        al = readArtifact( name, _repos );
        
        assertTrue( localRepo1Jar.exists() );
        assertFalse( localRepo2Jar.exists() );
    }
    
    public void testWriteReadLocalTimeStamp()
    throws Exception
    {
        String name = "org.apache.maven:maven-core:2.0.9-20090204.232323-23";
//        ArtifactBasicMetadata bmd = new ArtifactBasicMetadata( name );
        
        File af = new File( _resourceBase, "maven-core-2.0.9.jar" );
        File ap = new File( _resourceBase, "maven-core-2.0.9.pom" );
        
        File aJar1 = new File( _lbase1, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20090204.232323-23.jar");
        File aJar2 = new File( _lbase2, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20090204.232323-23.jar");
        
        assertFalse( aJar1.exists() );
        assertFalse( aJar2.exists() );
        
        writeArtifact( name, af, ap, _lr2 );
        
        assertFalse( aJar1.exists() );
        assertTrue( aJar2.exists() );
        
        List<Artifact> al = readArtifact( name, _repos );
        
        System.out.println(al);
        
        File localRepo1Jar = new File( _lbase1, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20090204.232323-23.jar" );
        File localRepo2Jar = new File( _lbase2, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-20090204.232323-23.jar" );
        
        assertTrue( localRepo1Jar.exists() );
        assertTrue( localRepo2Jar.exists() );
    }
    
    public void testWriteReadSnapshot()
    throws Exception
    {
        String name = "org.apache.maven:maven-core:2.0.9-SNAPSHOT";
        
        File af = new File( _resourceBase, "maven-core-2.0.9.jar" );
        File ap = new File( _resourceBase, "maven-core-2.0.9.pom" );
        
        File aJar1 = new File( _base1, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-SNAPSHOT.jar");
        File aJar2 = new File( _base2, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-SNAPSHOT.jar");
        
        assertFalse( aJar1.exists() );
        assertFalse( aJar2.exists() );
        
        writeArtifact( name, af, ap, _rr2 );
        
        assertFalse( aJar1.exists() );
        assertTrue( aJar2.exists() );
        
        List<Artifact> al = readArtifact( name, _rrs );
        
        System.out.println(al);
        
        File localRepo1Jar = new File( _lbase1, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-SNAPSHOT.jar" );
        File localRepo2Jar = new File( _lbase2, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-SNAPSHOT.jar" );
        
        assertFalse( localRepo1Jar.exists() );
        assertFalse( localRepo2Jar.exists() );
        
        al = readArtifact( name, _repos );
        
        assertTrue( localRepo1Jar.exists() );
        assertFalse( localRepo2Jar.exists() );
    }
    
    public void testWriteTimestampReadSnapshotSingleRepo()
    throws Exception
    {
        String nameTS1 = "org.apache.maven:maven-core:2.0.9-20090204.232323-23";
        String nameTS2 = "org.apache.maven:maven-core:2.0.9-20090204.232324-24";
        String nameSN = "org.apache.maven:maven-core:2.0.9-SNAPSHOT";
        
        File af = new File( _resourceBase, "maven-core-2.0.9.jar" );
        File ap = new File( _resourceBase, "maven-core-2.0.9.pom" );
        
        File aJar1 = new File( _base1, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-SNAPSHOT.jar");
        File aJar2 = new File( _base2, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-SNAPSHOT.jar");
        
        assertFalse( aJar1.exists() );
        assertFalse( aJar2.exists() );
        
        writeArtifact( nameTS1, af, ap, _rr2 );
        writeArtifact( nameTS2, af, ap, _rr1 );
        
//        assertFalse( aJar1.exists() );
//        assertTrue( aJar2.exists() );
        
        List<ArtifactBasicMetadata> vl = readVersions( nameSN, _rrs );
        
        System.out.println(vl);
        
        List<Artifact> al = readArtifact( nameSN, _rrs );
        
        System.out.println(al);
        
        Artifact aSN = al.get( 0 );
        
        assertNotNull( aSN.getFile() );
        
        assertTrue( aSN.getFile().exists() );
        
//        File localRepo1Jar = new File( _lbase1, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-SNAPSHOT.jar" );
//        File localRepo2Jar = new File( _lbase2, "org/apache/maven/maven-core/2.0.9-SNAPSHOT/maven-core-2.0.9-SNAPSHOT.jar" );
//        
//        assertFalse( localRepo1Jar.exists() );
//        assertFalse( localRepo2Jar.exists() );
//        
//        al = readArtifact( name, _repos );
//        
//        assertTrue( localRepo1Jar.exists() );
//        assertFalse( localRepo2Jar.exists() );
    }
}
