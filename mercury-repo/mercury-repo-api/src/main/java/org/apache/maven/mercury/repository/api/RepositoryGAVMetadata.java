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
package org.apache.maven.mercury.repository.api;

import java.text.ParseException;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactCoordinates;
import org.apache.maven.mercury.artifact.version.VersionComparator;
import org.apache.maven.mercury.repository.metadata.Metadata;
import org.apache.maven.mercury.repository.metadata.MetadataException;
import org.apache.maven.mercury.repository.metadata.Snapshot;
import org.apache.maven.mercury.util.TimeUtil;
import org.apache.maven.mercury.util.Util;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * This is a data object to carry GA level repository 
 * metadata, namely - a list of versions and last check timestamp
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class RepositoryGAVMetadata
{
  private static final Language LANG = new DefaultLanguage( RepositoryGAVMetadata.class );
  
  protected ArtifactCoordinates gav;
  
  /** a list of last discovered snapshots, ordered descending */
  protected TreeSet<String> snapshots = new TreeSet<String>( new VersionComparator() );

  /** a list of last discovered versions, ordered ascending */
  protected Collection<String> classifiers;
  
  /** GMT timestamp of the last metadata check */
  protected long lastCheck;
  
  /** is set true by cache implementation when determined that it's time to refresh */
  protected transient boolean expired = false;
  
  protected RepositoryGAVMetadata()
  {
  }

  /**
   * construct from maven 2.x maven-metadata.xml object
   * 
   * @param md
   * @throws MetadataException 
   */
  public RepositoryGAVMetadata( Metadata md )
  throws MetadataException
  {
    if( md == null )
      throw new IllegalArgumentException( LANG.getMessage( "empty.mdbytes" ) );
    
    this.gav = new ArtifactCoordinates( md.getGroupId(), md.getArtifactId(), md.getVersion() );

    List<String> versions = null;
    
    if( md.getVersioning() != null )
      versions = md.getVersioning().getVersions(); 
      
    if( !Util.isEmpty( versions ) )
        this.snapshots.addAll( versions );

    String version = md.getVersion();

    if( version != null && version.endsWith( Artifact.SNAPSHOT_VERSION ) )
    {
        Snapshot sn = md.getVersioning().getSnapshot();
        
        if( sn != null && !Util.isEmpty( sn.getTimestamp() ) &&  !Util.isEmpty( sn.getBuildNumber() ) )
        {
            String ts = version.replaceAll( Artifact.SNAPSHOT_VERSION, sn.getTimestamp()+'-'+sn.getBuildNumber() );
            snapshots.add( ts );
        }
    }

    this.lastCheck = TimeUtil.getUTCTimestampAsLong();
  }

  /**
   * copy constructor
   * 
   * @param md
   * @throws MetadataException 
   */
  public RepositoryGAVMetadata( RepositoryGAVMetadata md )
  throws MetadataException
  {
    this.gav = md.getGAV();

    if( ! Util.isEmpty( md.getSnapshots() ) )
      this.snapshots.addAll( md.getSnapshots() );
    
    this.lastCheck = TimeUtil.getUTCTimestampAsLong();
  }
  
  public TreeSet<String> getSnapshots()
  {
    return snapshots;
  }

  public Collection<String> getClassifiers()
  {
    return classifiers;
  }

  /**
   * find the most recent snapshot timestamp
   * 
   * @return
   */
  public String getSnapshot()
  {
    return snapshots.last();
  }

  public long getLastCheck()
  {
    return lastCheck;
  }
  
  public long getLastCheckMillis()
  throws ParseException
  {
    return TimeUtil.toMillis(  lastCheck );
  }

  public void updateSnapshots( Collection<String> snapshots )
  {
    this.snapshots.clear();

    if( !Util.isEmpty( snapshots ) )
      this.snapshots.addAll( snapshots );
    
    this.lastCheck = TimeUtil.getUTCTimestampAsLong();
  }

  public void updateClassifiers( Collection<String> classifiers )
  {
    this.classifiers = classifiers;
    this.lastCheck = TimeUtil.getUTCTimestampAsLong();
  }
  
  
  public ArtifactCoordinates getGAV()
  {
    return gav;
  }

  public boolean isExpired()
  {
    return expired;
  }

  public void setExpired( boolean expired )
  {
    this.expired = expired;
  }

}
