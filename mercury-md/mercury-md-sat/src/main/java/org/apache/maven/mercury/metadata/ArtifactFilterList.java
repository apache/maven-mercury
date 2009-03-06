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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.util.Util;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class ArtifactFilterList
{
    List<MetadataTreeArtifactFilter> _filters = new ArrayList<MetadataTreeArtifactFilter>(4);

    public ArtifactFilterList()
    {
    }

    public ArtifactFilterList( Map<String, Collection<String>> filter )
    {
        if( Util.isEmpty( filter ) )
            return;
        
        _filters.add( new MetadataTreeArtifactFilterMap(filter) );
    }

}
