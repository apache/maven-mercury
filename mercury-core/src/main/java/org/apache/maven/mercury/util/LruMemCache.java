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

package org.apache.maven.mercury.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class LruMemCache<K, V>
    implements MemCache<K, V>
{
    ConcurrentHashMap<K, V> _cache;
    
    K _lastKey;
    
    int _sz;
    
    public LruMemCache()
    {
        this( DEFAULT_CACHE_SIZE );
    }
    
    public LruMemCache( int sz )
    {
        _sz = sz;
        _cache = new ConcurrentHashMap<K, V>( sz );
    }

    public V get( K key )
    {
        return _cache.get( key );
    }

    public void put( K key, V val )
    {
        // MRU is easier - do it first ..
        synchronized( _cache )
        {
            if( _cache.size() == _sz )
            {// free up one slot
                _cache.remove( _lastKey );
                _lastKey = key;
            }
        }

        _cache.put( key, val );
    }

}
