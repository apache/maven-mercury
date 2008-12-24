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
package org.apache.maven.mercury.repository.local.map;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.DefaultArtifact;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.repository.api.ArtifactBasicResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.api.RepositoryWriter;
import org.apache.maven.mercury.repository.local.m2.MetadataProcessorMock;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.repository.virtual.VirtualRepositoryReader;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class LocalRepositoryMapTest
    extends TestCase
{
    Storage _storage;

    File _dir;

    LocalRepositoryMap _repo;
    
    RepositoryReader _rr;

    String _repoUrl = "http://repo1.sonatype.org";
    // String repoUrl = "http://repository.sonatype.org/content/groups/public";
    
    RemoteRepositoryM2 _remoteRepo;
    
    ArtifactBasicMetadata bmd;
    
    VirtualRepositoryReader _vr;
    
    File _pom;

    @Override
    protected void setUp()
        throws Exception
    {
        bmd = new ArtifactBasicMetadata("t:t:1.0::pom");
        
        _pom = new File("./target/test-classes/t-1.0.pom");
        
        _storage = new DefaultStorage();
        
        _storage.add( bmd.getGAV(), _pom );
        
        DependencyProcessor dp = new MetadataProcessorMock();
        
        _repo = new LocalRepositoryMap("testMapRepo", dp, _storage );
        
        _rr = _repo.getReader();
        
        Server server = new Server( "temp", new URL(_repoUrl) );
        _remoteRepo = new RemoteRepositoryM2( server, new MetadataProcessorMock() );
        
        List<Repository> reps = new ArrayList<Repository>(3);
        reps.add( _repo );
        reps.add( _remoteRepo );
        
        _vr = new VirtualRepositoryReader( reps );
    }

    public void testReadMap()
        throws Exception
    {
        Collection<ArtifactBasicMetadata> query = new ArrayList<ArtifactBasicMetadata>(1);
        
        query.add( bmd );
        
        ArtifactBasicResults res = _rr.readDependencies( query  );
        
        assertNotNull( res );
        
        assertFalse( res.hasExceptions() );
        
        assertTrue( res.hasResults() );
        
        Map<ArtifactBasicMetadata, List<ArtifactBasicMetadata>>  deps = res.getResults();
        
        assertNotNull( deps );
        
        assertEquals( 1, deps.size() );
        
        List<ArtifactBasicMetadata> myDeps = deps.get( bmd );
        
        assertNotNull( myDeps );
        
        assertEquals( 3, myDeps.size() );
        
        System.out.println( res.getResults() );
    }

}
