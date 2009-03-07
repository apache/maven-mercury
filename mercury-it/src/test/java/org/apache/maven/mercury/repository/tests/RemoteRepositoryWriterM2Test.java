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
package org.apache.maven.mercury.repository.tests;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.crypto.api.StreamVerifierAttributes;
import org.apache.maven.mercury.crypto.api.StreamVerifierFactory;
import org.apache.maven.mercury.crypto.pgp.PgpStreamVerifierFactory;
import org.apache.maven.mercury.crypto.sha.SHA1VerifierFactory;
import org.apache.maven.mercury.repository.local.m2.MetadataProcessorMock;
import org.apache.maven.mercury.repository.remote.m2.RemoteRepositoryM2;
import org.apache.maven.mercury.transport.api.Credentials;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class RemoteRepositoryWriterM2Test
extends AbstractRepositoryWriterM2Test
{
    static final String _davContext = "/webdav";

    static final String _user = "foo";

    static final String _pass = "bar";

    WebDavServer _dav;

    RemoteRepositoryM2 _davRepo;

  String _basePath = "./target/webdav";

  //------------------------------------------------------------------------------
  @Override
  void setReleases()
  throws Exception
  {
  }
  //------------------------------------------------------------------------------
  @Override
  void setSnapshots()
  throws Exception
  {
  }
  //---------------------------------------------------------------------------------------------
  protected void startDavServer( String basePath, String baseHint )
  throws Exception
  {
      targetDirectory = new File( basePath );

      FileUtil.delete( targetDirectory );

      targetDirectory.mkdirs();
      
      _dav = new WebDavServer( 0, targetDirectory, _davContext, getContainer(), 9, baseHint, null );
      
      _dav.start();
      
      Credentials user = new Credentials(_user,_pass);
      
      server = new Server("dav", new URL("http://localhost:"+_dav.getPort()+_davContext), false, false, user );
      
System.out.println("Server: "+server.getURL() + " ==> " + basePath );
      
      mdProcessor = new MetadataProcessorMock();
      
      repo = new RemoteRepositoryM2( server, mdProcessor );
      
      // verifiers
      factories = new HashSet<StreamVerifierFactory>();       
      factories.add( 
          new PgpStreamVerifierFactory(
                  new StreamVerifierAttributes( PgpStreamVerifierFactory.DEFAULT_EXTENSION, false, false )
                  , getClass().getResourceAsStream( secretKeyFile )
                  , keyId
                  , secretKeyPass
                                      )
                    );
      factories.add( new SHA1VerifierFactory(false,false) );
      server.setWriterStreamVerifierFactories(factories);
        
      reader = repo.getReader();
      writer = repo.getWriter();
      
  }
  //---------------------------------------------------------------------------------------------
  protected void stopDavServer()
  throws Exception
  {
      if( _dav != null )
      {
          _dav.stop();
          _dav.destroy();
          _dav = null;
      }  
  }
  //------------------------------------------------------------------------------
  @Override
  protected void setUp()
  throws Exception
  {
    super.setUp();

    query = new ArrayList<ArtifactMetadata>();

    startDavServer( _basePath, "mercury-test"  );
  }
  //-------------------------------------------------------------------------
  @Override
  protected void tearDown()
  throws Exception
  {
    super.tearDown();
    
    stopDavServer();
  }
  //-------------------------------------------------------------------------
  @Override
  public void testWriteContentionMultipleArtifacts()
      throws Exception
  {
    System.out.println("Mutliple Artifacts contention does not apply to remote repo client");
  }
  
  @Override
  public void testWriteContentionSingleArtifact()
      throws Exception
  {
    System.out.println("Single Artifacts contention does not apply to remote repo client");
  }
  
  //-------------------------------------------------------------------------
  //-------------------------------------------------------------------------
}
