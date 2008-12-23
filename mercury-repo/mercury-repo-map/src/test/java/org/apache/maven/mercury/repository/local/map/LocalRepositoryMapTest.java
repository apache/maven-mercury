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
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.DefaultArtifact;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.repository.api.RepositoryWriter;
import org.apache.maven.mercury.util.FileUtil;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class LocalRepositoryMapTest
    extends TestCase
{
  private static final IMercuryLogger _log = MercuryLoggerManager.getLogger( LocalRepositoryMapTest.class ); 

  File _dir;
  LocalRepositoryMap _repo;
  
  String repoUrl = "http://repo1.sonatype.org";
//  String repoUrl = "http://repository.sonatype.org/content/groups/public";
  
  Artifact a;
  Artifact b;

  @Override
  protected void setUp()
      throws Exception
  {
    _dir = File.createTempFile( "test-flat-", "-repo" );
    _dir.delete();
    _dir.mkdirs();
  }
  
  public void testReadMap()
  throws Exception
  {
  }
  

}
