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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.builder.api.MetadataReaderException;
import org.apache.maven.mercury.crypto.api.StreamVerifierFactory;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.repository.api.AbstracRepositoryReader;
import org.apache.maven.mercury.repository.api.AbstractRepository;
import org.apache.maven.mercury.repository.api.AbstractRepositoryWriter;
import org.apache.maven.mercury.repository.api.ArtifactBasicResults;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.LocalRepository;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.api.RepositoryWriter;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileLockBundle;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.maven.mercury.util.Util;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

public class LocalRepositoryReaderMap
extends AbstracRepositoryReader
implements RepositoryReader
{
  private static final IMercuryLogger _log = MercuryLoggerManager.getLogger( LocalRepositoryReaderMap.class ); 
  private static final Language _lang = new DefaultLanguage( LocalRepositoryReaderMap.class );
  //---------------------------------------------------------------------------------------------------------------
  private static final String [] _protocols = new String [] { "map" };
  
  private final LocalRepository _repo;
  //---------------------------------------------------------------------------------------------------------------
  public LocalRepositoryReaderMap( Repository repo, DependencyProcessor dp )
  {
      if( repo == null )
          throw new IllegalArgumentException("localRepo cannot be null");
        
      if( dp == null )
          throw new IllegalArgumentException("localRepo cannot be null");
      
      _mdProcessor = dp;
      _repo = (LocalRepository) repo;
  }
  //---------------------------------------------------------------------------------------------------------------
  public Repository getRepository()
  {
    return _repo;
  }
  //---------------------------------------------------------------------------------------------------------------
  public boolean canHandle( String protocol )
  {
    return AbstractRepository.DEFAULT_LOCAL_READ_PROTOCOL.equals( protocol );
  }
  //---------------------------------------------------------------------------------------------------------------
  public String[] getProtocols()
  {
    return _protocols;
  }
  //---------------------------------------------------------------------------------------------------------------
  //---------------------------------------------------------------------------------------------------------------
/* (non-Javadoc)
 * @see org.apache.maven.mercury.repository.api.RepositoryReader#readArtifacts(java.util.Collection)
 */
public ArtifactResults readArtifacts( Collection<ArtifactBasicMetadata> query )
    throws RepositoryException
{
    // TODO Auto-generated method stub
    return null;
}
/* (non-Javadoc)
 * @see org.apache.maven.mercury.repository.api.RepositoryReader#readDependencies(java.util.Collection)
 */
public ArtifactBasicResults readDependencies( Collection<ArtifactBasicMetadata> query )
    throws RepositoryException
{
    // TODO Auto-generated method stub
    return null;
}
/* (non-Javadoc)
 * @see org.apache.maven.mercury.repository.api.RepositoryReader#readRawData(java.lang.String)
 */
public byte[] readRawData( String path )
    throws MetadataReaderException
{
    // TODO Auto-generated method stub
    return null;
}
/* (non-Javadoc)
 * @see org.apache.maven.mercury.repository.api.RepositoryReader#readVersions(java.util.Collection)
 */
public ArtifactBasicResults readVersions( Collection<ArtifactBasicMetadata> query )
    throws RepositoryException
{
    // TODO Auto-generated method stub
    return null;
}
/* (non-Javadoc)
 * @see org.apache.maven.mercury.repository.api.RepositoryOperator#close()
 */
public void close()
{
    // TODO Auto-generated method stub
    
}
/* (non-Javadoc)
 * @see org.apache.maven.mercury.builder.api.MetadataReader#readRawData(org.apache.maven.mercury.artifact.ArtifactBasicMetadata, java.lang.String, java.lang.String)
 */
public byte[] readRawData( ArtifactBasicMetadata bmd, String classifier, String type )
    throws MetadataReaderException
{
    // TODO Auto-generated method stub
    return null;
}
}
