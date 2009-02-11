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

package org.apache.maven.mercury.repository.tests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.sonatype.webdav.WebdavServlet;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class WebDavServer
extends Server
{
    public WebDavServer( int port
                         , File base
                         , String remotePathFragment
                         , PlexusContainer container
                         , int debugLevel
                         , String fileCollectionHint
                         , String fileCollectionBase
                         )
    throws Exception
    {
        super( port );

        if ( !base.exists() )
        {
            base.mkdirs();
        }

        if( ! base.isDirectory() )
        {
            throw new IllegalArgumentException( "Specified base is not a directory: " + base.getCanonicalPath() );
        }
        
//        HandlerCollection handlers = new HandlerCollection();
//        setHandler( handlers );
//
//        Context context = new Context( handlers, remotePathFragment );
//        handlers.addHandler( new DefaultHandler() );
        
        
        ContextHandlerCollection contexts = new ContextHandlerCollection();
        setHandler(contexts);
        
        Context context = new Context( contexts, remotePathFragment, Context.SESSIONS );
        context.addServlet(new ServletHolder( new WebdavServlet() ), "/*");
        context.setAttribute( PlexusConstants.PLEXUS_KEY, container );
        context.setResourceBase( base.getCanonicalPath() );
        
        if( fileCollectionBase != null )
        {
            context.setAttribute( "resourceCollectionBase", fileCollectionBase );
            System.out.println("webDav resource base: "+fileCollectionBase);
        }
        else
        {
            context.setAttribute( "resourceCollectionHint", fileCollectionHint );
            System.out.println("webDav resource hint: "+fileCollectionHint);
        }

        context.setAttribute( "debug", debugLevel+"" );

//        Map<String,String> initParams = new HashMap<String, String>(8);
//        
//        initParams.put( "resourceCollectionHint", fileCollectionHint );
//        initParams.put( "debug", debugLevel+"" );
//        
//        context.setInitParams( initParams  );
        
    }

    public int getPort()
    {
        return getConnectors()[0].getLocalPort();
    }
}
