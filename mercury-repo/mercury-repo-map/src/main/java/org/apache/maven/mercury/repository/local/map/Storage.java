package org.apache.maven.mercury.repository.local.map;

import java.io.File;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;

/**
 * storage for the repository
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public interface Storage
{
    /**
     * store an artifact
     * 
     * @param bmd metadata
     * @param artifact artifact behind it
     */
    public abstract void add( ArtifactBasicMetadata bmd, Artifact artifact )
    throws StorageException;

    /**
     * find an artifact by it's metadata
     * 
     * @param bmd
     * @return
     */
    public abstract Artifact findArtifact( ArtifactBasicMetadata bmd )
    throws StorageException;

    /**
     * find raw data in this stotage
     * 
     * @param key
     * @return
     * @throws StorageException 
     */
    public abstract byte[] findRaw( String key )
    throws StorageException;

    /**
     * 
     * @param key
     * @param bytes
     * @throws StorageException 
     */
    public abstract void add( String key, byte [] bytes )
    throws StorageException;
    
    /**
     * 
     * @param key
     * @param file
     */
    public abstract void add( String key, File file )
    throws StorageException;

}