/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.maven.mercury.spi.http.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.URL;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.util.IO;

public class SimpleTestServer
    extends Server
{

    final static String LOCAL_PATH = "/testRepo/";

    final static String REMOTE_SEG = "/maven2/repo";

    File base;

    Context context;

    public SimpleTestServer( int port )
        throws Exception
    {
        this( port, LOCAL_PATH, REMOTE_SEG );
    }

    public SimpleTestServer()
        throws Exception
    {
        this( 0, LOCAL_PATH, REMOTE_SEG );
    }

    public SimpleTestServer( String localPathFragment, String remotePathFragment )
        throws Exception
    {
        this( 0, localPathFragment, remotePathFragment );
    }

    public SimpleTestServer( int port, String localPathFragment, String remotePathFragment )
        throws Exception
    {
        super( port );

        HandlerCollection handlers = new HandlerCollection();
        setHandler( handlers );

        context = new Context( handlers, remotePathFragment );
        handlers.addHandler( new DefaultHandler() );

        base = File.createTempFile( "simpleTestServer", "jetty" );
        base.delete();
        base.mkdir();
        base.deleteOnExit();

        URL list = SimpleTestServer.class.getResource( localPathFragment );
        LineNumberReader in = new LineNumberReader( new InputStreamReader( list.openStream() ) );
        String file = null;
        while ( ( file = in.readLine() ) != null )
        {
            if ( !file.startsWith( "file" ) )
            {
                continue;
            }
            OutputStream out = new FileOutputStream( new File( base, file ) );
            IO.copy( SimpleTestServer.class.getResource( localPathFragment + file ).openStream(), out );
            out.close();
        }
        context.addServlet( DefaultServlet.class, "/" );
        context.setResourceBase( base.getCanonicalPath() );
    }

    public SimpleTestServer( File localBase, String remotePathFragment )
        throws Exception
    {
        this( 0, localBase, remotePathFragment );
    }

    public SimpleTestServer( int port, File localBase, String remotePathFragment )
        throws Exception
    {
        super( port );

        HandlerCollection handlers = new HandlerCollection();
        setHandler( handlers );

        context = new Context( handlers, remotePathFragment );
        handlers.addHandler( new DefaultHandler() );

        context.addServlet( DefaultServlet.class, "/" );
        context.setResourceBase( localBase.getCanonicalPath() );
    }

    public int getPort()
    {
        return getConnectors()[0].getLocalPort();
    }

    public void destroy()
    {
        super.destroy();
        destroy( base );
    }

    public void destroy( File f )
    {
        if ( f == null )
            return;
        if ( f.isDirectory() )
        {
            File[] files = f.listFiles();
            for ( int i = 0; files != null && i < files.length; i++ )
            {
                destroy( files[i] );
            }
        }
        f.delete();
    }

    public static void main( String[] args )
        throws Exception
    {
        SimpleTestServer server = new SimpleTestServer();
        server.start();
        server.join();
    }
}
