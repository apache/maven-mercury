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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.MetadataResults;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.local.m2.MetadataProcessorMock;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class LocalRepositoryReactorMapTest
    extends TestCase
{

    Storage _storage;

    File _dir;

    LocalRepositoryMap _repo;

    RepositoryReader _rr;

    Map<String, String> _reactor;

    @Override
    protected void setUp()
        throws Exception
    {
        _reactor = new HashMap<String, String>( 2 );
        _reactor.put( "a:a:1.0-SNAPSHOT", "a/target" );
        _reactor.put( "a:b:1.0-SNAPSHOT", "b/target" );

        _dir = new File( "./target/test-classes/repoReactor" );

        _storage = new ReactorStorage( _dir, _reactor );

        DependencyProcessor dp = new MetadataProcessorMock();

        _repo = new LocalRepositoryMap( "testReactorRepo", dp, _storage );

        _rr = _repo.getReader();
    }

    public void testReadMap()
        throws Exception
    {
        ArtifactMetadata bmd = new ArtifactMetadata( "a:a:1.0-SNAPSHOT" );

        Collection<ArtifactMetadata> query = new ArrayList<ArtifactMetadata>( 1 );

        query.add( bmd );

        MetadataResults res = _rr.readDependencies( query );

        assertNotNull( res );

        assertFalse( res.hasExceptions() );

        assertTrue( res.hasResults() );

        Map<ArtifactMetadata, List<ArtifactMetadata>> deps = res.getResults();

        assertNotNull( deps );

        assertEquals( 1, deps.size() );

        List<ArtifactMetadata> myDeps = deps.get( bmd );

        assertNotNull( myDeps );

        assertEquals( 1, myDeps.size() );

        System.out.println( res.getResults() );
    }

}
