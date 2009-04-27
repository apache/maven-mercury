package org.apache.maven.mercury.repository.local.map;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.DefaultArtifact;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.maven.mercury.util.Util;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class ReactorStorage
    implements Storage
{
    private static final Language _lang = new DefaultLanguage( ReactorStorage.class );

    File _dir;

    Map<String, String> _reactor;

    public ReactorStorage( File dir, Map<String, String> reactorMap )
        throws StorageException
    {
        if ( !dir.exists() )
            dir.mkdirs();
        else if ( !dir.isDirectory() )
            throw new StorageException( _lang.getMessage( "reactor.storage.bad.dir", dir.getAbsolutePath() ) );

        _dir = dir;

        if ( Util.isEmpty( reactorMap ) )
            _reactor = new HashMap<String, String>( 8 );
        else
            _reactor = reactorMap;
    }

    public static final String calculateKeyString( ArtifactMetadata md )
    {
        return md.getGroupId() + ":" + md.getArtifactId() + ":" + md.getVersion();
    }

    private static final String calculateBinaryName( ArtifactMetadata md )
    {
        String type = md.getType();

        String classifier = md.getClassifier();

        if ( "test-jar".equals( type ) )
        {
            type = "jar";
            classifier = "tests";
        }
        else if ( "maven-plugin".equals( type ) )
        {
            type = "jar";
            classifier = null;
        }

        boolean hasClassifier = classifier != null && classifier.length() > 0;

        return md.getArtifactId() + "-" + md.getVersion() + ( hasClassifier ? "-" + classifier : "" ) + "." + type;
    }

    public Artifact findArtifact( ArtifactMetadata md )
    {

        if ( _reactor.isEmpty() )
            return null;

        String key = calculateKeyString( md );

        String target = _reactor.get( key );

        if ( target == null )
            return null;

        File candidate = new File( _dir, target + "/" + calculateBinaryName( md ) );

        if ( !candidate.exists() )
            return null;

        DefaultArtifact da = new DefaultArtifact( md );
        da.setFile( candidate );

        // TODO: 2009.04.27 oleg - investigate if POM should be set into Artifact
        // looks like none of the use cases require POM file being set

        return da;
    }

    public byte[] findRaw( String key )
        throws StorageException
    {
        ArtifactMetadata md = new ArtifactMetadata( key );

        Artifact a = findArtifact( md );

        try
        {
            if ( a != null )
                return FileUtil.readRawData( a.getFile() );
        }
        catch ( IOException e )
        {
            throw new StorageException( e );
        }

        return null;
    }

    public void add( ArtifactMetadata md, Artifact artifact )
    {
        // noop, all artifacts came from the constructor
    }

    public void add( String key, byte[] bytes )
        throws StorageException
    {
        // noop, all artifacts came from the constructor
    }

    public void add( String key, File file )
        throws StorageException
    {
        // noop, all artifacts came from the constructor
    }

    public void removeRaw( String key )
        throws StorageException
    {
        // noop, all artifacts came from the constructor
    }

}
