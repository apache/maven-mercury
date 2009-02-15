package org.apache.maven.mercury.ant.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.MavenDependencyProcessor;
import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactQueryList;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.metadata.DependencyBuilder;
import org.apache.maven.mercury.metadata.DependencyBuilderFactory;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.local.map.DefaultStorage;
import org.apache.maven.mercury.repository.local.map.LocalRepositoryMap;
import org.apache.maven.mercury.repository.local.map.Storage;
import org.apache.maven.mercury.repository.local.map.StorageException;
import org.apache.maven.mercury.repository.virtual.VirtualRepositoryReader;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.ResourceCollection;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class Dep
    extends AbstractDataType
    implements ResourceCollection
{
    private static final Language LANG = new DefaultLanguage( Dep.class );
    private static final IMercuryLogger LOG = MercuryLoggerManager.getLogger( Dep.class );

    private List<Dependency> _dependencies;

    private List<Artifact> _artifacts;

    private List<File> _files;

    private String _configId;

    private ArtifactScopeEnum _scope = ArtifactScopeEnum.compile;

    private boolean _transitive = true;

    private LocalRepositoryMap _pomRepo;

    private Storage _pomStorage;
    
    private Dependency _sourceDependency;
    

    /**
     * @param vr
     * @throws MavenEmbedderException 
     */
//    private MavenEmbedder createEmbedder( VirtualRepositoryReader vr )
//    throws MavenEmbedderException
//    {
//        Configuration configuration = new DefaultConfiguration()
//            .setUserSettingsFile( MavenEmbedder.DEFAULT_USER_SETTINGS_FILE )
//            .setGlobalSettingsFile( MavenEmbedder.DEFAULT_GLOBAL_SETTINGS_FILE )
//            .setClassLoader( Thread.currentThread().getContextClassLoader() )
//        ;
//
//        ConfigurationValidationResult validationResult = MavenEmbedder.validateConfiguration( configuration );
//
//        if ( validationResult.isValid() )
//        {
//            // If the configuration is valid then do your thang ...
//        }
//
//        MavenEmbedder embedder = new MavenEmbedder( configuration );
//
//        PlexusContainer container = embedder.getPlexusContainer();
//        
//        return embedder;
//    }

    private List<ArtifactBasicMetadata> getDependencies( VirtualRepositoryReader vr )
        throws RepositoryException
    {
        if ( Util.isEmpty( _dependencies ) )
        {
            return null;
        }

        List<ArtifactBasicMetadata> res = new ArrayList<ArtifactBasicMetadata>( _dependencies.size() );

        for ( Dependency d : _dependencies )
        {
            
            if ( d._amd == null )
            {
                throw new IllegalArgumentException( LANG.getMessage( "dep.dependency.name.mandatory" ) );
            }

            if ( Util.isEmpty( d._pom ) )
            {
                res.add( d._amd );
            }
            else
            {
                String key = d._amd.getGAV();

                ArtifactMetadata deps = null;
                
                File pomFile = new File( d._pom );
                
                if( !pomFile.exists() )
                    throw new RepositoryException("pom file "+d._pom+" does not exist");

                try
                {
                    _pomStorage.add( key, pomFile );

                    deps = vr.readDependencies( d._amd );

                    _pomStorage.removeRaw( key );
                }
                catch ( StorageException e )
                {
                    throw new RepositoryException( e );
                }

                if ( deps != null && !Util.isEmpty( deps.getDependencies() ) )
                {
                    for ( ArtifactBasicMetadata bmd : deps.getDependencies() )
                    {
                        res.add( bmd );
                    }
                }
            }
        }

        return res;
    }

    public Dependency createDependency()
    {
        if ( _dependencies == null )
        {
            _dependencies = new ArrayList<Dependency>( 8 );
        }

        Dependency dep = new Dependency();

        _dependencies.add( dep );

        return dep;
    }
    //----------------------------------------------------------------------------------------
    protected List<Artifact> resolve()
        throws Exception
    {
        Config config = AbstractAntTask.findConfig( getProject(), _configId );

        return resolve( config, _scope );
    }
    //----------------------------------------------------------------------------------------
//    protected ArtifactRepository getLocalRepository( String path )
//    throws Exception
//    {
//        ArtifactRepositoryLayout repoLayout = new DefaultRepositoryLayout();
//    
//        ArtifactRepository r = new DefaultArtifactRepository( "local",
//                                                              "file://" + path,
//                                                              repoLayout );
//    
//        return r;
//    }
    
//    protected MavenProject getProject( File pom, ArtifactRepository localRepo )
//        throws Exception
//    {
//        Properties props = System.getProperties();
//        ProfileActivationContext ctx = new DefaultProfileActivationContext( props, false );
//
//        ProjectBuilderConfiguration pbc = new DefaultProjectBuilderConfiguration();
//        pbc.setLocalRepository( localRepo );
//        pbc.setGlobalProfileManager( new DefaultProfileManager( getContainer(), ctx ) );
//
//        return projectBuilder.build( pom, pbc );
//    }
    //----------------------------------------------------------------------------------------
    protected List<Artifact> resolve( Config config, ArtifactScopeEnum scope )
        throws Exception
    {
        if ( !Util.isEmpty( _artifacts ) )
        {
            return _artifacts;
        }

        if ( Util.isEmpty( _dependencies ) )
        {
            return null;
        }

        List<Repository> repos = config.getRepositories();

        DependencyProcessor dp = new MavenDependencyProcessor();

        _pomStorage = new DefaultStorage();

        _pomRepo = new LocalRepositoryMap( "inMemMdRepo", dp, _pomStorage );

        repos.add( 0, _pomRepo );

        DependencyBuilder db = DependencyBuilderFactory.create( DependencyBuilderFactory.JAVA_DEPENDENCY_MODEL, repos );

        VirtualRepositoryReader vr = new VirtualRepositoryReader( repos );

        List<ArtifactBasicMetadata> depList = getDependencies( vr );
        
        List<ArtifactMetadata> res = _transitive
                                        ? db.resolveConflicts( scope, new ArtifactQueryList( depList ), null, null )
                                        : toArtifactMetadataList( depList )
                                    ;

        if ( Util.isEmpty( res ) )
        {
            return null;
        }

        ArtifactResults aRes = vr.readArtifacts( res );

        if ( aRes == null )
        {
            throw new BuildException( LANG.getMessage( "resolve.cannot.read", config.getId(), res.toString() ) );
        }

        if ( ( aRes == null ) || aRes.hasExceptions() )
        {
            throw new Exception( LANG.getMessage( "vr.error", aRes.getExceptions().toString() ) );
        }

        if ( !aRes.hasResults() )
        {
            return null;
        }


        Map<ArtifactBasicMetadata, List<Artifact>> resMap = aRes.getResults();

        int count = 0;
        for ( ArtifactBasicMetadata key : resMap.keySet() )
        {
            List<Artifact> artifacts = resMap.get( key );
            if ( artifacts != null )
            {
                count += artifacts.size();
            }
        }

        if ( count == 0 )
        {
            return null;
        }

        _artifacts = new ArrayList<Artifact>( count );

        for ( ArtifactBasicMetadata key : resMap.keySet() )
        {
            List<Artifact> artifacts = resMap.get( key );

            if ( !Util.isEmpty( artifacts ) )
            {
                for ( Artifact a : artifacts )
                {
                    _artifacts.add( a );
                }
            }
        }

        return _artifacts;
    }

    /**
     * @param depList
     * @return
     */
    private List<ArtifactMetadata> toArtifactMetadataList( List<ArtifactBasicMetadata> depList )
    {
        if( Util.isEmpty( depList ) )
            return null;
        
        List<ArtifactMetadata> res = new ArrayList<ArtifactMetadata>( depList.size() );
        
        for( ArtifactBasicMetadata bmd : depList )
            res.add( new ArtifactMetadata(bmd) );
        
        return res;
    }

    // attributes
    public void setConfigid( String configid )
    {
        this._configId = configid;
    }

    public void setScope( ArtifactScopeEnum scope )
    {
        this._scope = scope;
    }
    
    @Override
    public void setId( String id )
    {
        super.setId( id );
        
        if( _sourceDependency != null )
            _sourceDependency.setId( id );
    }

    public void setSource( String pom )
    {
        _sourceDependency = createDependency();
        
        if( getId() != null  )
            _sourceDependency.setId( getId() );
        
        _sourceDependency.setPom( pom );
    }

    public void setTransitive( boolean val )
    {
        this._transitive = val;
    }

    protected void setList( List<Dependency> dependencies )
    {
        _dependencies = dependencies;
    }
    //----------------------------------------------------------------------------------------
    public boolean isFilesystemOnly()
    {
        return true;
    }
    //----------------------------------------------------------------------------------------
    public Iterator<File> iterator()
    {
        try
        {
            if ( _files != null )
            {
                return _files.iterator();
            }

            List<Artifact> artifacts = resolve();

            if ( Util.isEmpty( artifacts ) )
            {
                return null;
            }

            _files = new ArrayList<File>( artifacts.size() );

            for ( Artifact a : _artifacts )
            {
                _files.add( a.getFile() );
            }

            return _files.iterator();
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage() );

            return null;
        }
    }
    //----------------------------------------------------------------------------------------
    public int size()
    {
        try
        {
            List<Artifact> artifacts = resolve();

            if ( Util.isEmpty( artifacts ) )
            {
                return 0;
            }

            return artifacts.size();
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage() );

            return 0;
        }
    }
    //----------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------
}