/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file                                                                                            
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.mercury.spi.http.client;

import java.net.URL;
import java.util.HashSet;

import org.apache.maven.mercury.crypto.api.StreamVerifierFactory;
import org.apache.maven.mercury.spi.http.client.retrieve.DefaultRetriever;
import org.apache.maven.mercury.spi.http.server.AuthenticatingTestServer;
import org.apache.maven.mercury.transport.api.Credentials;
import org.apache.maven.mercury.transport.api.Server;

public class AuthenticatingJettyRetrieverTest extends JettyRetrieverTest
{    
    public void setUp ()
    throws Exception
    {
        server = new AuthenticatingTestServer();
        server.start();
        _port=String.valueOf(server.getPort()); 

        HashSet<Server> remoteServerTypes = new HashSet<Server>();
        remoteServerType = new Server( "test", new URL(__HOST_FRAGMENT+_port), false, false, 
                                       new Credentials(((AuthenticatingTestServer)server).getUsername(), ((AuthenticatingTestServer)server).getPassword()));        
        factories = new HashSet<StreamVerifierFactory>();

        remoteServerTypes.add(remoteServerType);

        retriever = new DefaultRetriever();
        retriever.setServers(remoteServerTypes);
    }


    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

}
