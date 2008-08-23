package org.apache.maven.mercury.repository.local.m2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.Quality;
import org.apache.maven.mercury.artifact.version.DefaultArtifactVersion;
import org.apache.maven.mercury.artifact.version.VersionQuery;
import org.apache.maven.mercury.crypto.api.StreamObserver;
import org.apache.maven.mercury.crypto.api.StreamObserverException;
import org.apache.maven.mercury.crypto.api.StreamVerifier;
import org.apache.maven.mercury.crypto.api.StreamVerifierException;
import org.apache.maven.mercury.crypto.api.StreamVerifierFactory;
import org.apache.maven.mercury.repository.api.AbstractRepository;
import org.apache.maven.mercury.repository.api.LocalRepository;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.api.RepositoryWriter;
import org.apache.maven.mercury.repository.metadata.AddVersionOperation;
import org.apache.maven.mercury.repository.metadata.Metadata;
import org.apache.maven.mercury.repository.metadata.MetadataBuilder;
import org.apache.maven.mercury.repository.metadata.MetadataException;
import org.apache.maven.mercury.repository.metadata.MetadataOperation;
import org.apache.maven.mercury.repository.metadata.SetSnapshotOperation;
import org.apache.maven.mercury.repository.metadata.Snapshot;
import org.apache.maven.mercury.repository.metadata.SnapshotOperand;
import org.apache.maven.mercury.repository.metadata.StringOperand;
import org.apache.maven.mercury.transport.api.Server;
import org.apache.maven.mercury.util.FileUtil;
import org.codehaus.plexus.i18n.DefaultLanguage;
import org.codehaus.plexus.i18n.Language;
import org.codehaus.plexus.util.FileUtils;

public class LocalRepositoryWriterM2
implements RepositoryWriter
{
  private static final org.slf4j.Logger _log = org.slf4j.LoggerFactory.getLogger( LocalRepositoryWriterM2.class ); 
  private static final Language _lang = new DefaultLanguage( LocalRepositoryReaderM2.class );
  //---------------------------------------------------------------------------------------------------------------
  private static final String [] _protocols = new String [] { "file" };
  
  LocalRepository _repo;
  File _repoDir;
  //---------------------------------------------------------------------------------------------------------------
  public LocalRepositoryWriterM2( LocalRepository repo )
  {
    if( repo == null )
      throw new IllegalArgumentException("localRepo cannot be null");
    
    _repoDir = repo.getDirectory();
    if( _repoDir == null )
      throw new IllegalArgumentException("localRepo directory cannot be null");
    
    if( !_repoDir.exists() )
      throw new IllegalArgumentException("localRepo directory \""+_repoDir.getAbsolutePath()+"\" should exist");

    _repo = repo;
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
  public void close()
  {
  }
  //---------------------------------------------------------------------------------------------------------------
  public void writeArtifact( Collection<Artifact> artifacts )
      throws RepositoryException
  {
    if( artifacts == null || artifacts.size() < 1 )
      return;
    
    Set<StreamVerifierFactory> vFacs = null;
    Server server = _repo.getServer();
    if( server != null && server.hasWriterStreamVerifierFactories() )
      vFacs = server.getWriterStreamVerifierFactories();
    
    if( vFacs == null ) // let it be empty, but not null
      vFacs = new HashSet<StreamVerifierFactory>(1);
      
    for( Artifact artifact : artifacts )
    {
      writeArtifact( artifact, vFacs );
    }
  }
  //---------------------------------------------------------------------------------------------------------------
  public void writeArtifact( Artifact artifact, Set<StreamVerifierFactory> vFacs )
      throws RepositoryException
  {
    if( artifact == null )
      return;
    
    boolean isPom = "pom".equals( artifact.getType() );
    
    byte [] pomBlob = artifact.getPomBlob();
    boolean hasPomBlob = pomBlob != null && pomBlob.length > 0;
    
    InputStream in = artifact.getStream();
    if( in == null )
    {
      File aFile = artifact.getFile();
      if( aFile == null && !isPom )
      {
        throw new RepositoryException( _lang.getMessage( "artifact.no.stream", artifact.toString() ) );
      }

      try
      {
        in = new FileInputStream( aFile );
      }
      catch( FileNotFoundException e )
      {
        if( !isPom )
          throw new RepositoryException( e );
      }
    }
    DefaultArtifactVersion dav = new DefaultArtifactVersion( artifact.getVersion() );
    Quality aq = dav.getQuality();
    boolean isSnapshot = aq.equals( Quality.SNAPSHOT_QUALITY ) || aq.equals( Quality.SNAPSHOT_TS_QUALITY );

    String relGroupPath = artifact.getGroupId().replace( '.', '/' )+"/"+artifact.getArtifactId();
    String relVersionPath = relGroupPath + '/' + (isSnapshot ? (dav.getBase()+'-'+Artifact.SNAPSHOT_VERSION) : artifact.getVersion() );

    try
    {
      if( isPom )
      {
        if( in == null && !hasPomBlob )
          throw new RepositoryException( _lang.getMessage( "pom.artifact.no.stream", artifact.toString() ) );
        
        if( in != null )
        {
          byte [] pomBlobBytes = FileUtil.readRawData( in );
          hasPomBlob = pomBlobBytes != null && pomBlobBytes.length > 0;
          if( hasPomBlob )
            pomBlob = pomBlobBytes;
        }
          
      }

      // create folders
      File gav = new File( _repoDir, relVersionPath );
      gav.mkdirs();

      String fName = _repoDir.getAbsolutePath()+'/'+relVersionPath+'/'+artifact.getBaseName()+'.'+artifact.getType();
      
      if( !isPom )
      {
        // first - take care of the binary
        FileUtil.writeAndSign( fName, in, vFacs );
        
        // if classier - nothing else to do :)
        if( artifact.hasClassifier() )
          return;
        
        // GA metadata
        File mdFile = new File( _repoDir, relGroupPath+'/'+_repo.getMetadataName() );
        Metadata localMd = null;
        
        if( mdFile.exists() )
          localMd = MetadataBuilder.read( new FileInputStream(mdFile) );
        else
        {
          localMd = new Metadata();
          localMd.setGroupId( artifact.getGroupId() );
          localMd.setArtifactId( artifact.getArtifactId() );
        }
        
        MetadataOperation mdOp = null;
        
        if( isSnapshot )
        {
          Snapshot sn = MetadataBuilder.createSnapshot( artifact.getVersion() );
          sn.setLocalCopy( true );
          mdOp = new SetSnapshotOperation( new SnapshotOperand(sn) );
        }
        else
          mdOp = new AddVersionOperation( new StringOperand(artifact.getVersion()) ); 
        
        byte [] resBytes = MetadataBuilder.changeMetadata( localMd, mdOp );

        FileUtil.writeAndSign( mdFile.getAbsolutePath(), resBytes, vFacs );

        // now - GAV metadata
        mdFile = new File( _repoDir, relVersionPath+'/'+_repo.getMetadataName() );
        localMd = null;
        
        if( mdFile.exists() )
          localMd = MetadataBuilder.read( new FileInputStream(mdFile) );
        else
        {
          localMd = new Metadata();
          localMd.setGroupId( artifact.getGroupId() );
          localMd.setArtifactId( artifact.getArtifactId() );
          localMd.setVersion( artifact.getVersion() );
        }
        
        resBytes = MetadataBuilder.changeMetadata( localMd, mdOp );
        FileUtil.writeAndSign( mdFile.getAbsolutePath(), resBytes, vFacs );
      }
      
      if( hasPomBlob )
      {
        FileUtil.writeAndSign( _repoDir.getAbsolutePath()+'/'+relVersionPath
                              +'/'+artifact.getArtifactId()+'-'+artifact.getVersion()+".pom", pomBlob, vFacs
                              );
      }
        
    }
    catch( Exception e )
    {
      throw new RepositoryException( e );
    }
    
  }
  //---------------------------------------------------------------------------------------------------------------
  //---------------------------------------------------------------------------------------------------------------
}
