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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.MetadataTreeNode;
import org.apache.maven.mercury.artifact.api.ArtifactListProcessor;
import org.apache.maven.mercury.artifact.api.ConfigurationException;
import org.apache.maven.mercury.artifact.api.ConfigurationUtil;
import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.repository.api.RepositoryException;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class DependencyBuilderFactory
{
    public static final String JAVA_DEPENDENCY_MODEL = "java";

    public static final String OSGI_DEPENDENCY_MODEL = "osgi";

    private static final Language LANG = new DefaultLanguage( DependencyBuilderFactory.class );

    public static final DependencyBuilder create( String dependencyModel, Collection<Repository> repositories )
        throws RepositoryException
    {
        return create( dependencyModel, repositories, null, null, null );
    }

    public static final DependencyBuilder create( String dependencyModel, Collection<Repository> repositories,
                                                  Collection<MetadataTreeArtifactFilter> filters,
                                                  List<Comparator<MetadataTreeNode>> comparators,
                                                  Map<String, ArtifactListProcessor> processors )
        throws RepositoryException
    {
        return create( dependencyModel, repositories, null, null, null, null );
    }

    // TODO: oleg: switch to request/response paradigm - too many parameters
    //
    public static final DependencyBuilder create( String dependencyModel, Collection<Repository> repositories,
                                                  Collection<MetadataTreeArtifactFilter> filters,
                                                  List<Comparator<MetadataTreeNode>> comparators,
                                                  Map<String, ArtifactListProcessor> processors,
                                                  Map<String, Object> config )
        throws RepositoryException
    {
        if ( JAVA_DEPENDENCY_MODEL.equals( dependencyModel ) )
        {
            DependencyBuilder db = new DependencyTreeBuilder( repositories, filters, comparators, processors );

            try
            {
                ConfigurationUtil.configure( db, config );
            }
            catch ( ConfigurationException e )
            {
                throw new RepositoryException( e );
            }

            return db;
        }

        throw new IllegalArgumentException( LANG.getMessage( "dependency.model.not.implemented", dependencyModel ) );
    }

}
