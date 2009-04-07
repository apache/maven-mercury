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

import java.util.List;

import org.apache.maven.mercury.artifact.ArtifactExclusionList;
import org.apache.maven.mercury.artifact.ArtifactInclusionList;
import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.artifact.ArtifactQueryList;
import org.apache.maven.mercury.artifact.ArtifactScopeEnum;
import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.artifact.api.Configurable;
import org.apache.maven.mercury.event.MercuryEventListener;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public interface DependencyBuilder
extends Configurable
{
  public static final String TREE_BUILD_EVENT = "tree.build";
  public static final String TREE_NODE_BUILD_EVENT = "tree.node.build";

  public static final String SYSTEM_PROPERTY_ALLOW_CIRCULAR_DEPENDENCIES = "mercury.circular.allow";
  //------------------------------------------------------------------------
  /**
   * build the tree, using the repositories specified in the
   * constructor
   * 
   * @param startMD - root of the tree to build
   * @param targetPlatform - limitations to use when retrieving metadata. Format is G:A=V, where V is Version Range
   * @return the root of the tree built
   * @throws MetadataTreeException
   */
  public abstract MetadataTreeNode buildTree( ArtifactMetadata startMD, ArtifactScopeEnum scope )
  throws MetadataTreeException;

  /**
   * hard to believe, but this actually resolves the conflicts, removing all duplicate GAVs from the tree
   * 
   * @param root the tree to resolve conflicts on
   * @return list of resolved GAVs
   * @throws MetadataTreeException
   */
  public abstract List<ArtifactMetadata> resolveConflicts( MetadataTreeNode root )
  throws MetadataTreeException;

  /**
   * consolidated entry point: give it a collection of GAVs, it 
   * will create a classpath out of it
   * 
   * @param root the tree to resolve conflicts on
   * @return list of resolved GAVs
   * @throws MetadataTreeException
   */
  public abstract List<ArtifactMetadata> resolveConflicts( 
                                          ArtifactScopeEnum   scope
                                        , ArtifactQueryList artifacts
                                        , ArtifactInclusionList inclusions
                                        , ArtifactExclusionList exclusions
                                        )

  throws MetadataTreeException;

  /**
   * consolidated entry point: give it a collection of GAVs, it 
   * will create a tree out of it
   * 
   * @param root the tree to resolve conflicts on
   * @return resolved metadata tree
   * @throws MetadataTreeException
   */
  public abstract MetadataTreeNode resolveConflictsAsTree( 
                                          ArtifactScopeEnum   scope
                                        , ArtifactQueryList artifacts
                                        , ArtifactInclusionList inclusions
                                        , ArtifactExclusionList exclusions
                                        )

  throws MetadataTreeException;

  /**
   *  this one resolves the conflicts, removing all duplicate GAVs from the tree and
   *  returning a copy of the resulting subtree - original tree should be intact
   * 
   * @param root the tree to resolve conflicts on
   * @return resolved subtree
   * @throws MetadataTreeException
   */
  public abstract MetadataTreeNode resolveConflictsAsTree( MetadataTreeNode root )
  throws MetadataTreeException;
  
  /**
   * register a listener for dependency events 
   * 
   * @param listener
   */
  public abstract void register( MercuryEventListener listener );
  
  /**
   * remove a listener 
   * 
   * @param listener
   */
  public abstract void unRegister( MercuryEventListener listener );
  
  /**
   * release all resources 
   * 
   */
  public abstract void close();
  //-----------------------------------------------------
  //-----------------------------------------------------

}
