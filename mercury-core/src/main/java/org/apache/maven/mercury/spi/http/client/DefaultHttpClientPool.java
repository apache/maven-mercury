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

package org.apache.maven.mercury.spi.http.client;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;
import org.mortbay.jetty.client.HttpClient;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class DefaultHttpClientPool
    implements HttpClientPool
{
    private static final Language LANG = new DefaultLanguage( DefaultHttpClientPool.class );

    private static int _poolSize = DEFAULT_POOL_SIZE;

    private static ConcurrentLinkedQueue<HttpClient> _pool = new ConcurrentLinkedQueue<HttpClient>();

    public DefaultHttpClientPool()
    {
        this( DEFAULT_POOL_SIZE );
    }

    public DefaultHttpClientPool( int sz )
    {
        for ( int i = 0; i < sz; i++ )
            _pool.offer( new HttpClient() );
    }

    public HttpClient getHttpClient()
        throws HttpClientPoolException
    {
        if ( _pool.isEmpty() )
            throw new HttpClientPoolException( LANG.getMessage( "pool.empty" ) );

        return _pool.poll();
    }

    public void returnHttpClient( HttpClient client )
        throws HttpClientPoolException
    {
        _pool.offer( client );
    }

}
