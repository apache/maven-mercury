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
 * a pool to use instead actual HttpClient. A singleton that stores all HttpClient's 
 * known to mercury transport layer.
 * 
 * At this point - writing files to the server can tolerate creating a new HttpClient, so
 * the code supporting it is not utilized.
 * 
 * @author Oleg Gusakov
 * @version $Id$
 */
public class HttpClientPool
{
    public static final String SYSTEM_PROPERTY_HTTP_CLIENT_POOL_SIZE = "mercury.http.client.pool.size";
    
    /** default initial pool size */
    public static final int POOL_SIZE = Integer.valueOf( System.getProperty( SYSTEM_PROPERTY_HTTP_CLIENT_POOL_SIZE, "5" ) );
    
    public static final String SYSTEM_PROPERTY_HTTP_DAV_CLIENT_POOL_SIZE = "mercury.http.dav.client.pool.size";
    
    /** default initial dav pool size */
    public static final int DAV_POOL_SIZE = Integer.valueOf( System.getProperty( SYSTEM_PROPERTY_HTTP_DAV_CLIENT_POOL_SIZE, "3" ) );

    private static PoolImpl _readPool  = new PoolImpl( POOL_SIZE, null );
    private static PoolImpl _writePool = new PoolImpl( DAV_POOL_SIZE, "org.mortbay.jetty.client.webdav.WebdavListener");

    public static HttpClient getHttpClient( boolean davEnabledClient )
    throws HttpClientException
    {
        if( davEnabledClient )
            return _writePool.getHttpClient();
        
        return _readPool.getHttpClient();
    }

    public static void returnHttpClient( HttpClient client, boolean davEnabledClient )
    {
        if( davEnabledClient )
            _writePool.returnHttpClient( client );
        else
            _readPool.returnHttpClient( client );
    }

}

class PoolImpl
{
    private static final Language LANG = new DefaultLanguage( PoolImpl.class );

    private final int POOL_SIZE;

    /** current pool size. Will not grow beyond POOL_SIZE */
    private static int _poolSize = 0;

    /** the pool itself */
    private ConcurrentLinkedQueue<HttpClient> _pool = new ConcurrentLinkedQueue<HttpClient>();
    
    private String _listener; 

    PoolImpl( int maxSize, String listener )
    {
        this.POOL_SIZE = maxSize;
        
        this._listener = listener;
    }

    synchronized HttpClient getHttpClient()
    throws HttpClientException
    {
        if ( _pool.isEmpty() )
        {
            if( _poolSize < POOL_SIZE )
            {
                ++_poolSize;
                
                // TODO Oleg 2009.05.06: add "http client configuration" configuration
                HttpClient hc = new HttpClient();

                hc.setConnectorType( HttpClient.CONNECTOR_SELECT_CHANNEL );

                if( _listener != null )
                    hc.registerListener( _listener );

                try
                {
                    hc.start();
                }
                catch ( Exception e )
                {
                    throw new HttpClientException( null, e.getMessage() );
                }
                
                _pool.offer( hc );
            }
            else    
                throw new HttpClientException( null, LANG.getMessage( "pool.empty", ""+_poolSize ) );
        }

        return _pool.poll();
    }
    
    void returnHttpClient( HttpClient client )
    {
        if( client != null)
            _pool.offer( client );
    }
}