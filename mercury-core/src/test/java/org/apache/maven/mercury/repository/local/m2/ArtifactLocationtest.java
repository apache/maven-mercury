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

package org.apache.maven.mercury.repository.local.m2;

import junit.framework.TestCase;

import org.apache.maven.mercury.artifact.ArtifactMetadata;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class ArtifactLocationtest
    extends TestCase
{

    protected void setUp()
        throws Exception
    {
    }

    public void testChopTS()
        throws Exception
    {
        ArtifactLocation loc =
            new ArtifactLocation( "test", new ArtifactMetadata( "a:a:3.0-alpha-1-20080920.015600-7" ) );

        String chop = loc.getVersionWithoutTS();

        assertEquals( "3.0-alpha-1", chop );

        loc = new ArtifactLocation( "test", new ArtifactMetadata( "a:a:3.0-20080920.015600-7" ) );

        chop = loc.getVersionWithoutTS();

        assertEquals( "3.0", chop );

        loc = new ArtifactLocation( "test", new ArtifactMetadata( "a:a:3.0-20080920.0156007" ) );

        chop = loc.getVersionWithoutTS();

        assertEquals( "3.0-20080920.0156007", chop );
    }

    public void testChopTSstatic()
        throws Exception
    {
        String chop = ArtifactLocation.calculateVersionDir( "3.0-alpha-1-20080920.015600-7" );

        assertEquals( "3.0-alpha-1-SNAPSHOT", chop );

        chop = ArtifactLocation.calculateVersionDir( "3.0-20080920.015600-7" );

        assertEquals( "3.0-SNAPSHOT", chop );

        chop = ArtifactLocation.calculateVersionDir( "3.0-20080920.01560-7" );

        assertEquals( "3.0-20080920.01560-7", chop );

        chop = ArtifactLocation.calculateVersionDir( "3.0-20080920.015600" );

        assertEquals( "3.0-20080920.015600", chop );

        chop = ArtifactLocation.calculateVersionDir( "3.0" );

        assertEquals( "3.0", chop );

    }

}
