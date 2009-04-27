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
package org.apache.maven.mercury.repository.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.mercury.artifact.ArtifactMetadata;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class MetadataResults
    extends AbstractRepOpResult
{
    Map<ArtifactMetadata, List<ArtifactMetadata>> _result = new HashMap<ArtifactMetadata, List<ArtifactMetadata>>( 8 );

    /**
     * first result is ready
     */
    public MetadataResults( ArtifactMetadata query, List<ArtifactMetadata> result )
    {
        this._result.put( query, result );
    }

    /**
     * optimization opportunity
     * 
     * @param size
     */
    public MetadataResults( int size )
    {
    }

    private MetadataResults()
    {
    }

    public static MetadataResults add( final MetadataResults res, final ArtifactMetadata key, final Exception err )
    {
        MetadataResults ret = res;
        if ( res == null )
            ret = new MetadataResults();

        ret.addError( key, err );

        return ret;
    }

    public static MetadataResults add( final MetadataResults res, final ArtifactMetadata key,
                                       final List<ArtifactMetadata> result )
    {
        MetadataResults ret = res;
        if ( res == null )
            ret = new MetadataResults();

        ret.add( key, result );

        return ret;
    }

    public static MetadataResults add( final MetadataResults res, final ArtifactMetadata key,
                                       final ArtifactMetadata result )
    {
        MetadataResults ret = res;
        if ( res == null )
            ret = new MetadataResults();

        ret.add( key, result );

        return ret;
    }

    private List<ArtifactMetadata> getOrCreate( ArtifactMetadata query )
    {
        List<ArtifactMetadata> res = _result.get( query );
        if ( res == null )
        {
            res = new ArrayList<ArtifactMetadata>( 8 );
            _result.put( query, res );
        }
        return res;
    }

    /**
     * add results if they are not there yet
     * 
     * @param query
     * @param result
     */
    public void add( ArtifactMetadata query, List<ArtifactMetadata> result )
    {
        List<ArtifactMetadata> res = getOrCreate( query );
        for ( ArtifactMetadata r : result )
        {
            if ( res.contains( r ) )
                continue;

            res.add( r );
        }
    }

    public void add( ArtifactMetadata query, ArtifactMetadata result )
    {
        List<ArtifactMetadata> res = getOrCreate( query );
        res.add( result );
    }

    public Map<ArtifactMetadata, List<ArtifactMetadata>> getResults()
    {
        return _result;
    }

    public List<ArtifactMetadata> getResult( ArtifactMetadata query )
    {
        return _result.get( query );
    }

    @Override
    public boolean hasResults()
    {
        return !_result.isEmpty();
    }

    @Override
    public boolean hasResults( ArtifactMetadata key )
    {
        return !_result.isEmpty() && _result.containsKey( key ) && !_result.get( key ).isEmpty();
    }

}
