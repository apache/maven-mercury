/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */

package org.apache.maven.mercury.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.TreeSet;

import org.apache.maven.mercury.artifact.ArtifactExclusionList;
import org.apache.maven.mercury.artifact.ArtifactInclusionList;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactQueryList;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.metadata.forest.Forest;
import org.apache.maven.mercury.metadata.forest.Node;
import org.apache.maven.mercury.metadata.forest.Tree;
import org.apache.maven.mercury.metadata.forest.io.xpp3.ForrestXpp3Reader;
import org.apache.maven.mercury.metadata.forest.io.xpp3.ForrestXpp3Writer;
import org.apache.maven.mercury.util.TimeUtil;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class DependencyTreeDumper
{
    String _dumpFileName;

    File _dumpFile;

    Writer _wr;

    public DependencyTreeDumper( String name )
    {
        _dumpFileName = name;
    }

    /**
     * this one does not work because the writer runs out of stack memory on huge trees
     * 
     * @param scope
     * @param artifacts
     * @param inclusions
     * @param exclusions
     * @param dirty
     * @param result
     * @throws MetadataTreeException
     */
    public synchronized void realDump( ArtifactScopeEnum scope, ArtifactQueryList artifacts,
                                       ArtifactInclusionList inclusions, ArtifactExclusionList exclusions,
                                       MetadataTreeNode dirty, List<ArtifactMetadata> result )
        throws MetadataTreeException
    {
        try
        {
            if ( artifacts == null || artifacts.getMetadataList() == null )
                return;

            Forest forest;
            if ( _dumpFile.exists() )
            {
                forest = new ForrestXpp3Reader().read( new FileInputStream( _dumpFile ) );
            }
            else
                forest = new Forest();

            Tree tree = new Tree();

            forest.addTree( tree );

            tree.setScope( scope == null ? "null" : scope.toString() );

            tree.setTimestamp( TimeUtil.getUTCTimestamp() );

            for ( ArtifactMetadata md : artifacts.getMetadataList() )
            {
                Node n = new Node();
                n.setName( md.toString() );
                n.setScope( md.getScope() );
                tree.addRequest( n );
            }

            if ( inclusions != null )
                for ( ArtifactMetadata md : inclusions.getMetadataList() )
                {
                    Node n = new Node();
                    n.setName( md.toString() );
                    n.setScope( md.getScope() );
                    tree.addInclusion( n );

                }

            if ( exclusions != null )
                for ( ArtifactMetadata md : exclusions.getMetadataList() )
                {
                    Node n = new Node();
                    n.setName( md.toString() );
                    n.setScope( md.getScope() );
                    tree.addExclusion( n );

                }

            Node root = createNode( dirty, 0L );

            tree.addDirtyTree( root );

            if ( result != null )
                for ( ArtifactMetadata md : result )
                {
                    Node n = new Node();
                    n.setName( md.toString() );
                    n.setScope( md.getScope() );
                    tree.addResult( n );
                }

            new ForrestXpp3Writer().write( new FileWriter( _dumpFile ), forest );
        }
        catch ( Exception e )
        {
            throw new MetadataTreeException( e );
        }
    }

    public synchronized void dump( ArtifactScopeEnum scope, ArtifactQueryList artifacts,
                                   ArtifactInclusionList inclusions, ArtifactExclusionList exclusions,
                                   MetadataTreeNode dirty, List<ArtifactMetadata> result )
        throws MetadataTreeException
    {
        try
        {
            _dumpFile =
                new File( _dumpFileName + "-" + TimeUtil.getUTCTimestamp() + "-" + System.currentTimeMillis() + ".xml" );
            _wr = new FileWriter( _dumpFile );
            _wr.write( "<tree>\n" );
        }
        catch ( IOException e )
        {
            throw new MetadataTreeException( e );
        }
        try
        {
            if ( artifacts != null && artifacts.getMetadataList() != null )
            {
                if ( scope != null )
                    _wr.write( "<scope>" + scope + "</scope>\n" );

                _wr.write( "<timestamp>" + TimeUtil.getUTCTimestamp() + "</timestamp>\n" );

                _wr.write( "<request>\n" );
                for ( ArtifactMetadata md : artifacts.getMetadataList() )
                {
                    _wr.write( "  <node scope='" + md.getScope() + "'>" + md.toString() + "</node>\n" );
                }
                _wr.write( "</request>\n" );

                if ( inclusions != null )
                {
                    _wr.write( "<inclusions>\n" );
                    for ( ArtifactMetadata md : inclusions.getMetadataList() )
                    {
                        _wr.write( "  <inclusion>" + md.toString() + "</inclusion>\n" );
                    }
                    _wr.write( "</inclusions>\n" );
                }

                if ( exclusions != null )
                {
                    _wr.write( "<exclusions>\n" );
                    for ( ArtifactMetadata md : exclusions.getMetadataList() )
                    {
                        _wr.write( "  <exclusion>" + md.toString() + "</exclusion>\n" );
                    }
                    _wr.write( "</exclusions>\n" );
                }

                _wr.write( "<source>\n" );
                showNode( dirty, 0L );
                _wr.write( "</source>\n" );

                if ( result != null )
                {
                    TreeSet<String> ts = new TreeSet<String>();
                    for ( ArtifactMetadata md : result )
                        ts.add( md.toString() );

                    _wr.write( "<result>\n" );
                    for ( String name : ts )
                    {
                        _wr.write( "  <node>" + name + "</node>\n" );
                    }
                    _wr.write( "</result>\n" );
                }

            }
        }
        catch ( Exception e )
        {
            throw new MetadataTreeException( e );
        }
        finally
        {
            if ( _wr != null )
                try
                {
                    _wr.write( "</tree>\n" );
                    _wr.flush();
                    _wr.close();
                }
                catch ( Exception ee )
                {
                }

            _wr = null;
        }
    }

    private void showNode( MetadataTreeNode mtn, long level )
        throws IOException
    {
        if ( mtn == null )
            return;

        ArtifactMetadata md = mtn.getMd();
        for ( int i = 0; i < level; i++ )
            _wr.write( "  " );

        _wr.write( "  <node level='" + level + "'>" + md.toString() + "</node>\n" );

        if ( mtn.hasChildren() )
        {
            _wr.write( "  <kids>\n" );
            for ( MetadataTreeNode kid : mtn.getChildren() )
                showNode( kid, level + 1 );
            _wr.write( "  </kids>\n" );
        }
    }

    private static Node createNode( MetadataTreeNode mtn, long level )
    {
        if ( mtn == null )
            return null;

        Node n = new Node();

        ArtifactMetadata md = mtn.getMd();

        n.setName( md.toString() );

        n.setLevel( level );

        if ( mtn.hasChildren() )
        {
            for ( MetadataTreeNode kid : mtn.getChildren() )
                n.addChildren( createNode( kid, level + 1 ) );
        }

        return n;

    }
}
