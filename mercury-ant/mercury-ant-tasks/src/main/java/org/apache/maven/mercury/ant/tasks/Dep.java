package org.apache.maven.mercury.ant.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.Artifact;
import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.metadata.DependencyBuilder;
import org.apache.maven.mercury.metadata.DependencyBuilderFactory;
import org.apache.maven.mercury.repository.api.ArtifactResults;
import org.apache.maven.mercury.repository.api.Repository;
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
    private static final Language _lang = new DefaultLanguage( Dep.class );
    private static final IMercuryLogger _log = MercuryLoggerManager.getLogger( Dep.class ); 

    private List<Dependency> _dependencies;
    
    private List<Artifact> _artifacts;
    
    private String _configId;

    private ArtifactScopeEnum _scope = ArtifactScopeEnum.compile;

    private boolean _transitive = true;

    protected List<ArtifactBasicMetadata> getDependencies()
    {
        if ( Util.isEmpty( _dependencies ) )
            return null;

        List<ArtifactBasicMetadata> res = new ArrayList<ArtifactBasicMetadata>( _dependencies.size() );

        for ( Dependency d : _dependencies )
            res.add( d._amd );

        return res;
    }

    public Dependency createDependency()
    {
        if ( _dependencies == null )
            _dependencies = new ArrayList<Dependency>( 8 );

        Dependency dep = new Dependency();

        _dependencies.add( dep );

        return dep;
    }

    public class Dependency
    {
        ArtifactBasicMetadata _amd;

        boolean _optional = false;

        public void setName( String name )
        {
            _amd = new ArtifactBasicMetadata( name );

            _amd.setOptional( _optional );
        }

        public void setOptional( boolean optional )
        {
            this._optional = optional;

            if ( _amd != null )
                _amd.setOptional( optional );
        }

    }
    //----------------------------------------------------------------------------------------
    protected List<Artifact> resolve()
    throws Exception
    {
        return resolve( AbstractAntTask.findConfig( getProject(), _configId ), _scope );
    }
    //----------------------------------------------------------------------------------------
    protected List<Artifact> resolve( Config config, ArtifactScopeEnum scope )
    throws Exception
    {
        if( ! Util.isEmpty(_artifacts) )
            return _artifacts;
        
        if( Util.isEmpty( _dependencies ) )
            return null;

        Collection<Repository> repos = config.getRepositories();

        DependencyBuilder db =
            DependencyBuilderFactory.create( DependencyBuilderFactory.JAVA_DEPENDENCY_MODEL, repos, null, null, null );
        
        List<ArtifactMetadata> res = db.resolveConflicts( scope, getDependencies() );

        if ( Util.isEmpty( res ) )
            return null;

        VirtualRepositoryReader vr = new VirtualRepositoryReader( repos );

        ArtifactResults aRes = vr.readArtifacts( res );

        if ( aRes == null )
            throw new BuildException( _lang.getMessage( "resolve.cannot.read", config.getId(), res.toString() ) );

        if ( aRes == null || aRes.hasExceptions() )
        {
            throw new Exception( _lang.getMessage( "vr.error", aRes.getExceptions().toString() ) );
        }

        if ( !aRes.hasResults() )
            return null;
        

        Map<ArtifactBasicMetadata, List<Artifact>> resMap = aRes.getResults();
        
        int count = 0;
        for ( ArtifactBasicMetadata key : resMap.keySet() )
        {
            List<Artifact> artifacts = resMap.get( key );
            if( artifacts != null )
                count += artifacts.size();
        }
        
        if( count == 0 )
            return null;
        
        _artifacts = new ArrayList<Artifact>( count );

        for ( ArtifactBasicMetadata key : resMap.keySet() )
        {
            List<Artifact> artifacts = resMap.get( key );

            if ( !Util.isEmpty( artifacts ) )
                for ( Artifact a : artifacts )
                    _artifacts.add( a );
        }

        return _artifacts;
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

    public Iterator iterator()
    {
        try
        {
            List<Artifact> artifacts = resolve();
            
            if( Util.isEmpty( artifacts ) )
                return null;
            
            return artifacts.iterator();
        }
        catch ( Exception e )
        {
            _log.error( e.getMessage() );
            
            return null;
        }
    }

    public int size()
    {
        try
        {
            List<Artifact> artifacts = resolve();
            
            if( Util.isEmpty( artifacts ) )
                return 0;
            
            return artifacts.size();
        }
        catch ( Exception e )
        {
            _log.error( e.getMessage() );
            
            return 0;
        }
    }
}