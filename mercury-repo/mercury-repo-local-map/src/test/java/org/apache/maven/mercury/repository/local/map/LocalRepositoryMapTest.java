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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.ArtifactBasicResults;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.local.m2.MetadataProcessorMock;
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
    
    ArtifactMetadata bmd;
    
    File _pom;

    @Override
    protected void setUp()
        throws Exception
    {
        bmd = new ArtifactMetadata("t:t:1.0::pom");
        
        _pom = new File("./target/test-classes/t-1.0.pom");

        _dir = File.createTempFile( "temp-", "-mercury-default-storage" );
        _dir.delete();
        _storage = new DefaultStorage( _dir );
        
        _storage.add( bmd.getGAV(), _pom );
        
        DependencyProcessor dp = new MetadataProcessorMock();
        
        _repo = new LocalRepositoryMap("testMapRepo", dp, _storage );
        
        _rr = _repo.getReader();
        
    }

    @Override
    protected void tearDown()
        throws Exception
    {
        FileUtil.delete( _dir );
    }

    public void testReadMap()
        throws Exception
    {
        Collection<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>(1);
        
        query.add( bmd );
        
        ArtifactBasicResults res = _rr.readDependencies( query  );
        
        assertNotNull( res );
        
        assertFalse( res.hasExceptions() );
        
        assertTrue( res.hasResults() );
        
        Map<ArtifactMetadata, List<ArtifactMetadata>>  deps = res.getResults();
        
        assertNotNull( deps );
        
        assertEquals( 1, deps.size() );
        
        List<ArtifactMetadata> myDeps = deps.get( bmd );
        
        assertNotNull( myDeps );
        
        assertEquals( 3, myDeps.size() );
        
        System.out.println( res.getResults() );
    }

}
