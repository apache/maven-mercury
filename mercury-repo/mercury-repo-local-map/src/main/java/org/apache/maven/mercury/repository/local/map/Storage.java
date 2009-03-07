package org.apache.maven.mercury.repository.local.map;

import java.io.File;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactMetadata;

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
    public abstract void add( ArtifactMetadata bmd, Artifact artifact );

    /**
     * find an artifact by it's metadata
     * 
     * @param bmd
     * @return
     */
    public abstract Artifact findArtifact( ArtifactMetadata bmd );

    /**
     * find raw data in this stotage
     * 
     * @param key
     * @return
     * @throws StorageException 
     */
    public abstract byte[] findRaw( String key ) throws StorageException;

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
     * @throws StorageException 
     */
    public abstract void add( String key, File file )
    throws StorageException;

    /**
     * delete this datum
     * 
     * @param key
     */
    public abstract void removeRaw( String key )
    throws StorageException;

}