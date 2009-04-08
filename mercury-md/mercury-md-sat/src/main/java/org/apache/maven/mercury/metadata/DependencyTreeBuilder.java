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
package org.apache.maven.mercury.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.ArtifactExclusionList;
import org.apache.maven.mercury.artifact.ArtifactInclusionList;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactQueryList;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.artifact.api.ArtifactListProcessor;
import org.apache.maven.mercury.artifact.api.ConfigurationException;
import org.apache.maven.mercury.artifact.version.VersionException;
import org.apache.maven.mercury.event.EventGenerator;
import org.apache.maven.mercury.event.EventManager;
import org.apache.maven.mercury.event.EventTypeEnum;
import org.apache.maven.mercury.event.GenericEvent;
import org.apache.maven.mercury.event.MercuryEventListener;
import org.apache.maven.mercury.logging.IMercuryLogger;
import org.apache.maven.mercury.logging.MercuryLoggerManager;
import org.apache.maven.mercury.metadata.sat.DefaultSatSolver;
import org.apache.maven.mercury.metadata.sat.SatException;
import org.apache.maven.mercury.repository.api.MetadataResults;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.apache.maven.mercury.repository.virtual.VirtualRepositoryReader;
import org.apache.maven.mercury.util.Util;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * This is the new entry point into Artifact resolution process.
 * 
 * @author Oleg Gusakov
 * @version $Id$
 */
class DependencyTreeBuilder
    implements DependencyBuilder, EventGenerator
{
    public static final ArtifactMetadata DUMMY_ROOT = new ArtifactMetadata( "__fake:__fake:1.0" );

    private static final Language LANG = new DefaultLanguage( DependencyTreeBuilder.class );
    
    public static final String SYSTEM_PROPERTY_DUMP_DEPENDENCY_TREE = "mercury.dump.tree";
    
    private static final String _depTreeDumpFileName = System.getProperty( SYSTEM_PROPERTY_DUMP_DEPENDENCY_TREE );

    private static final boolean _dumpDepTree = _depTreeDumpFileName != null;
    
    private static final DependencyTreeDumper _dumper = _dumpDepTree ? new DependencyTreeDumper(_depTreeDumpFileName ) : null;
    
    private static final IMercuryLogger LOG = MercuryLoggerManager.getLogger( DependencyTreeBuilder.class );

    private Collection<MetadataTreeArtifactFilter> _filters;

    private List<Comparator<MetadataTreeNode>> _comparators;

    private Map<String, ArtifactListProcessor> _processors;

    private VirtualRepositoryReader _reader;

    private Map<String, MetadataTreeNode> _existingNodes;

    private EventManager _eventManager;
    
    private boolean _buildIndividualTrees = true;
    
    private boolean _allowCircularDependencies = Boolean.parseBoolean( System.getProperty( SYSTEM_PROPERTY_ALLOW_CIRCULAR_DEPENDENCIES, "false" ) );
    
    /** mandated versions in the format G:A -> V */
    private Map<String, ArtifactMetadata> _versionMap;
    
    class TruckLoad
    {
        List<ArtifactMetadata> cp;
        MetadataTreeNode root;
        
        public TruckLoad()
        {
        }
        
        public TruckLoad( List<ArtifactMetadata> cp )
        {
            this.cp = cp;
        }
        
        public TruckLoad( MetadataTreeNode root )
        {
            this.root = root;
        }
    }

    /**
     * creates an instance of MetadataTree. Use this instance to
     * <ul>
     * <li>buildTree - process all the dependencies</li>
     * <li>resolveConflicts</li>
     * <ul>
     * 
     * @param filters - can veto any artifact before it's added to the tree
     * @param comparators - used to define selection policies. If null is passed, classic comparators - nearest/newest
     *            first - will be used.
     * @param repositories - order is <b>very</b> important. Ordering allows m2eclipse, for instance, insert a workspace
     *            repository
     * @throws RepositoryException
     */
    protected DependencyTreeBuilder( Collection<Repository> repositories,
                                     Collection<MetadataTreeArtifactFilter> filters,
                                     List<Comparator<MetadataTreeNode>> comparators,
                                     Map<String, ArtifactListProcessor> processors )
        throws RepositoryException
    {
        this._filters = filters;
        this._comparators = comparators;

        // if used does not want to bother.
        // if it's an empty list - user does not want any comparators - so be it
        if ( _comparators == null )
        {
            _comparators = new ArrayList<Comparator<MetadataTreeNode>>( 2 );
            _comparators.add( new ClassicDepthComparator() );
            _comparators.add( new ClassicVersionComparator() );
        }

        if ( processors != null )
            _processors = processors;

        this._reader = new VirtualRepositoryReader( repositories );
    }

    // ------------------------------------------------------------------------
    public MetadataTreeNode buildTree( ArtifactMetadata startMD, ArtifactScopeEnum treeScope )
        throws MetadataTreeException
    {
        if ( startMD == null )
            throw new MetadataTreeException( "null start point" );

        try
        {
            _reader.setEventManager( _eventManager );
            _reader.setProcessors( _processors );
            _reader.init();
        }
        catch ( RepositoryException e )
        {
            throw new MetadataTreeException( e );
        }

        _existingNodes = new HashMap<String, MetadataTreeNode>( 256 );

        GenericEvent treeBuildEvent = null;
        if ( _eventManager != null )
            treeBuildEvent = new GenericEvent( EventTypeEnum.dependencyBuilder, TREE_BUILD_EVENT, startMD.getGAV() );

        MetadataTreeNode root = createNode( startMD, null, startMD, treeScope );

        if ( _eventManager != null )
            treeBuildEvent.stop();

        if ( _eventManager != null )
            _eventManager.fireEvent( treeBuildEvent );

        MetadataTreeNode.reNumber( root, 1 );

        return root;
    }

    // ------------------------------------------------------------------------
    public List<ArtifactMetadata> resolveConflicts( 
                                        ArtifactScopeEnum     scope
                                      , ArtifactQueryList     artifacts
                                      , ArtifactInclusionList inclusions
                                      , ArtifactExclusionList exclusions
                                                  )
    throws MetadataTreeException
    {
        TruckLoad tl = resolveConflictsInternally( scope, artifacts, inclusions, exclusions, false );
        
        return tl == null ? null : tl.cp;
    }
    // ------------------------------------------------------------------------
    public MetadataTreeNode resolveConflictsAsTree( 
                                        ArtifactScopeEnum     scope
                                      , ArtifactQueryList     artifacts
                                      , ArtifactInclusionList inclusions
                                      , ArtifactExclusionList exclusions
                                                  )
    throws MetadataTreeException
    {
        TruckLoad tl = resolveConflictsInternally( scope, artifacts, inclusions, exclusions, true );
        
        return tl == null ? null : tl.root;
    }
    // ------------------------------------------------------------------------
    public TruckLoad resolveConflictsInternally( 
                                        ArtifactScopeEnum     scope
                                      , ArtifactQueryList     artifacts
                                      , ArtifactInclusionList inclusions
                                      , ArtifactExclusionList exclusions
                                      , boolean asTree
                                                  )

    throws MetadataTreeException
    {
        if ( artifacts == null )
            throw new MetadataTreeException( LANG.getMessage( "empty.md.collection" ) );

        List<ArtifactMetadata> startMDs = artifacts.getMetadataList();
        
        if ( Util.isEmpty( startMDs ) )
            throw new MetadataTreeException( LANG.getMessage( "empty.md.collection" ) );

        int nodeCount = startMDs.size();

        if ( nodeCount == 1 && inclusions == null && exclusions == null )
        {
            ArtifactMetadata bmd = startMDs.get( 0 );
            MetadataTreeNode rooty = buildTree( bmd, scope );

            TruckLoad tl = null;
            
            if( asTree )
            {
                MetadataTreeNode tr = resolveConflictsAsTree( rooty );
                
                tl = new TruckLoad( tr );
            }
            else
            {
                List<ArtifactMetadata> res = resolveConflicts( rooty );
                
                tl = new TruckLoad( res );
    
                if(_dumpDepTree )
                    _dumper.dump( scope, artifacts, inclusions, exclusions, rooty, res );
            }

            return tl;
        }

        DUMMY_ROOT.setDependencies( startMDs );
        DUMMY_ROOT.setInclusions( inclusions == null ? null : inclusions.getMetadataList() );
        DUMMY_ROOT.setExclusions( exclusions == null ? null : exclusions.getMetadataList() );
        
        MetadataTreeNode root = null;
        
        if( _buildIndividualTrees )
        {
            List<MetadataTreeNode> deps = new ArrayList<MetadataTreeNode>( nodeCount );
           
            for ( ArtifactMetadata bmd : startMDs )
            {
                if( scope != null && !scope.encloses( bmd.getArtifactScope() ) )
                    continue;
                
                try
                {
                    if( ! DUMMY_ROOT.allowDependency( bmd ) )
                        continue;
                }
                catch ( VersionException e )
                {
                    throw new MetadataTreeException(e);
                }
           
                if( inclusions != null )
                {
                    List<ArtifactMetadata> inc = inclusions.getMetadataList();
                    
                    if( bmd.hasInclusions() )
                        bmd.getInclusions().addAll( inc );
                    else
                        bmd.setInclusions( inc );
                }
    
                if( exclusions != null )
                {
                    List<ArtifactMetadata> excl = exclusions.getMetadataList();
                    
                    if( bmd.hasExclusions() )
                        bmd.getExclusions().addAll( excl );
                    else
                        bmd.setExclusions( excl );
                }
                
                MetadataTreeNode rooty = buildTree( bmd, scope );
    
                deps.add( rooty );
            }
            
            if( Util.isEmpty( deps ) ) // all dependencies are filtered out 
                return null;
    
            // combine into one tree
            root = new MetadataTreeNode( DUMMY_ROOT, null, null );
    
            for ( MetadataTreeNode kid : deps )
                root.addChild( kid );
    
        }
        else
        {
            DUMMY_ROOT.setDependencies( startMDs );
            root = buildTree( DUMMY_ROOT, scope );
        }
        
        
        TruckLoad tl = null;
        
        if( asTree )
        {
            MetadataTreeNode tr = resolveConflictsAsTree( root );
            
            tl = new TruckLoad( tr );
        }
        else
        {
            List<ArtifactMetadata> cp = resolveConflicts( root ); 

            if( cp != null )
                cp.remove( DUMMY_ROOT );
    
                if(_dumpDepTree )
                    _dumper.dump( scope, artifacts, inclusions, exclusions, root, cp );
                
                tl = new TruckLoad( cp );
        }

        return tl;
    }
    // -----------------------------------------------------
    private MetadataTreeNode createNode( ArtifactMetadata nodeMD, MetadataTreeNode parent
                                         , ArtifactMetadata nodeQuery, ArtifactScopeEnum globalScope
                                       )
        throws MetadataTreeException
    {
        GenericEvent nodeBuildEvent = null;

        if ( _eventManager != null )
            nodeBuildEvent = new GenericEvent( EventTypeEnum.dependencyBuilder, TREE_NODE_BUILD_EVENT, nodeMD.getGAV() );

        try
        {
            try
            {
                checkForCircularDependency( nodeMD, parent );
            }
            catch ( MetadataTreeCircularDependencyException e )
            {
                if( _allowCircularDependencies )
                {
                    String line = LANG.getMessage( "attention.line" );
                    LOG.info( line + e.getMessage() + line );
                    return null;
                }
                else
                    throw e;
            }

            ArtifactMetadata mr;

            MetadataTreeNode existingNode = _existingNodes.get( nodeQuery.toString() );

            if ( existingNode != null )
                return MetadataTreeNode.deepCopy( existingNode );

            if( DUMMY_ROOT.equals( nodeMD ))
                mr = DUMMY_ROOT;
            else
                mr = _reader.readDependencies( nodeMD );

            if ( mr == null )
                throw new MetadataTreeException( LANG.getMessage( "artifact.md.not.found", nodeMD.toString() ) );

            MetadataTreeNode node = new MetadataTreeNode( mr, parent, nodeQuery );

            List<ArtifactMetadata> allDependencies = mr.getDependencies();

            if ( allDependencies == null || allDependencies.size() < 1 )
                return node;
            
            if( !Util.isEmpty( _versionMap ) )
                for( ArtifactMetadata am :  allDependencies )
                {
                    String key = am.toManagementString();
                    ArtifactMetadata ver = _versionMap.get( key );
                    if( ver != null )
                    {
                        if( LOG.isDebugEnabled() )
                            LOG.debug( "managed replacement: "+am+" -> "+ver );
                        
                        if ( _eventManager != null )
                        {
                            GenericEvent replaceEvent = new GenericEvent( EventTypeEnum.dependencyBuilder, TREE_NODE_VERSION_REPLACE_EVENT, "managed replacement: "+am+" -> "+ver );
                            replaceEvent.stop();
                            _eventManager.fireEvent( replaceEvent );
                        }
 
                        am.setVersion( ver.getVersion() );
                        am.setInclusions( ver.getInclusions() );
                        am.setExclusions( ver.getExclusions() );
                    }
                }

            List<ArtifactMetadata> dependencies = new ArrayList<ArtifactMetadata>( allDependencies.size() );
            if ( globalScope != null )
                for ( ArtifactMetadata md : allDependencies )
                {
                    ArtifactScopeEnum mdScope = md.getArtifactScope();
                    if ( globalScope.encloses( mdScope ) )
                        dependencies.add( md );
                }
            else
                dependencies.addAll( allDependencies );

            if ( Util.isEmpty( dependencies ) )
                return node;

            MetadataResults res = _reader.readVersions( dependencies );
            
            if( res == null )
                throw new MetadataTreeException( LANG.getMessage( "no.versions", dependencies.toString() ) );

            Map<ArtifactMetadata, List<ArtifactMetadata>> expandedDeps = res.getResults();

            for ( ArtifactMetadata md : dependencies )
            {

                if ( LOG.isDebugEnabled() )
                    LOG.debug( "node " + nodeQuery + ", dep " + md );

                List<ArtifactMetadata> versions = expandedDeps.get( md );
                if ( versions == null || versions.size() < 1 )
                {
                    if ( md.isOptional() )
                        continue;

                    throw new MetadataTreeException( LANG.getMessage( "not.optional.missing" ) + md + " <== "+ showPath( node ) );
                }

                boolean noVersions = true;
                boolean noGoodVersions = true;

                for ( ArtifactMetadata ver : versions )
                {
                    if ( veto( ver, _filters ) || vetoInclusionsExclusions( node, ver ) )
                    {
                        // there were good versions, but this one is filtered out
                        noGoodVersions = false;
                        continue;
                    }

                    MetadataTreeNode kid = createNode( ver, node, md, globalScope );
                    if( kid != null )
                        node.addChild( kid );

                    noVersions = false;

                    noGoodVersions = false;
                }

                if ( noVersions && !noGoodVersions )
                {
                    // there were good versions, but they were all filtered out
                    continue;
                }
                else if ( noGoodVersions )
                {
                    if ( md.isOptional() )
                        continue;
                    
                    throw new MetadataTreeException( LANG.getMessage( "not.optional.missing" ) + md + " <== "+ showPath( node ) );
                }
                else
                    node.addQuery( md );
            }

            _existingNodes.put( nodeQuery.toString(), node );

            return node;
        }
        catch ( RepositoryException e )
        {
            if ( _eventManager != null )
                nodeBuildEvent.setResult( e.getMessage() );

            throw new MetadataTreeException( e );
        }
        catch ( VersionException e )
        {
            if ( _eventManager != null )
                nodeBuildEvent.setResult( e.getMessage() );

            throw new MetadataTreeException( e );
        }
        catch ( MetadataTreeException e )
        {
            if ( _eventManager != null )
                nodeBuildEvent.setResult( e.getMessage() );
            throw e;
        }
        finally
        {
            if ( _eventManager != null )
            {
                nodeBuildEvent.stop();
                _eventManager.fireEvent( nodeBuildEvent );
            }
        }
    }

    // -----------------------------------------------------
    private void checkForCircularDependency( ArtifactMetadata md, MetadataTreeNode parent )
        throws MetadataTreeCircularDependencyException
    {
        MetadataTreeNode p = parent;
        int count = 0;
        while ( p != null )
        {
            count++;
            // System.out.println("circ "+md+" vs "+p.md);
            if ( md.sameGA( p.getMd() ) )
            {
                p = parent;
                StringBuilder sb = new StringBuilder( 128 );
                sb.append( md.toString() );
                while ( p != null )
                {
                    sb.append( " <- " + p.getMd().toString() );

                    if ( md.sameGA( p.getMd() ) )
                    {
                        throw new MetadataTreeCircularDependencyException( "circular dependency " + count
                            + " levels up. " + sb.toString() + " <= " + ( p.getParent() == null ? "no parent" : p.getParent().getMd() ) );
                    }
                    p = p.getParent();
                }
            }
            p = p.getParent();
        }
    }

    // -----------------------------------------------------
    private boolean veto( ArtifactMetadata md, Collection<MetadataTreeArtifactFilter> filters )
    {
        if ( filters != null && filters.size() > 1 )
            for ( MetadataTreeArtifactFilter filter : filters )
                if ( filter.veto( md ) )
                    return true;
        return false;
    }

    // -----------------------------------------------------
    private boolean vetoInclusionsExclusions( MetadataTreeNode node, ArtifactMetadata ver )
        throws VersionException
    {
        for ( MetadataTreeNode n = node; n != null; n = n.getParent() )
        {
            ArtifactMetadata md = n.getQuery();

            if ( !md.allowDependency( ver ) ) // veto it
                return true;
        }
        return false; // allow because all parents are OK with it
    }

    // -----------------------------------------------------
    public List<ArtifactMetadata> resolveConflicts( MetadataTreeNode root )
        throws MetadataTreeException
    {
        if ( root == null )
            throw new MetadataTreeException( LANG.getMessage( "empty.tree" ) );
        
        root.createNames( 0, 0 );

        try
        {
            DefaultSatSolver solver = new DefaultSatSolver( root, _eventManager );

            solver.applyPolicies( getComparators() );

            List<ArtifactMetadata> res = solver.solve();

            return res;
        }
        catch ( SatException e )
        {
            throw new MetadataTreeException( e );
        }

    }

    // -----------------------------------------------------
    public MetadataTreeNode resolveConflictsAsTree( MetadataTreeNode root )
        throws MetadataTreeException
    {
        if ( root == null )
            throw new MetadataTreeException( LANG.getMessage( "empty.tree" ) );

        try
        {
            DefaultSatSolver solver = new DefaultSatSolver( root, _eventManager );

            solver.applyPolicies( getComparators() );

            MetadataTreeNode res = solver.solveAsTree();

            return res;
        }
        catch ( SatException e )
        {
            throw new MetadataTreeException( e );
        }

    }

    // -----------------------------------------------------
    private List<Comparator<MetadataTreeNode>> getComparators()
    {
        if ( Util.isEmpty( _comparators ) )
            _comparators = new ArrayList<Comparator<MetadataTreeNode>>( 2 );

        if ( _comparators.size() < 1 )
        {
            _comparators.add( new ClassicDepthComparator() );
            _comparators.add( new ClassicVersionComparator() );
        }

        return _comparators;
    }

    // -----------------------------------------------------
    private String showPath( MetadataTreeNode node )
        throws MetadataTreeCircularDependencyException
    {
        StringBuilder sb = new StringBuilder( 256 );

        String comma = "";

        MetadataTreeNode p = node;

        while ( p != null )
        {
            sb.append( comma + p.getMd().toString() );

            comma = " <== ";

            p = p.getParent();
        }

        return sb.toString();
    }

    public void register( MercuryEventListener listener )
    {
        if ( _eventManager == null )
            _eventManager = new EventManager();

        _eventManager.register( listener );
    }

    public void unRegister( MercuryEventListener listener )
    {
        if ( _eventManager != null )
            _eventManager.unRegister( listener );
    }

    public void setEventManager( EventManager eventManager )
    {
        if ( _eventManager == null )
            _eventManager = eventManager;
        else
            _eventManager.getListeners().addAll( eventManager.getListeners() );

    }
    
    public void close()
    {
        if( _reader != null )
            _reader.close();
    }

    @SuppressWarnings("unchecked")
    public void setOption( String name, Object val )
        throws ConfigurationException
    {
        if( SYSTEM_PROPERTY_ALLOW_CIRCULAR_DEPENDENCIES.equals( name ) )
            _allowCircularDependencies = Boolean.parseBoolean( (String)val );
        else if( CONFIGURATION_PROPERTY_VERSION_MAP.equals( name ) )
            _versionMap = (Map<String, ArtifactMetadata>) val;
    }
}
