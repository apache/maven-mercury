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
package org.apache.maven.mercury.repository.local.map;

import java.io.File;
import java.util.Map;

import org.apache.maven.mercury.artifact.ArtifactBasicMetadata;
import org.apache.maven.mercury.builder.api.DependencyProcessor;
import org.apache.maven.mercury.repository.api.AbstractRepository;
import org.apache.maven.mercury.repository.api.LocalRepository;
import org.apache.maven.mercury.repository.api.NonExistentProtocolException;
import org.apache.maven.mercury.repository.api.RepositoryReader;
import org.apache.maven.mercury.repository.api.RepositoryWriter;

/**
 * in-memory only repository for processing POMs on-the-fly
 * 
 * @author Oleg Gusakov
 * @version $Id$
 */
public class LocalRepositoryMap
extends AbstractRepository
implements LocalRepository
{
    public static final String FLAT_REPOSITORY_TYPE = "map";
    
    protected Storage _storage;

    // ----------------------------------------------------------------------------------
    public LocalRepositoryMap( String id, DependencyProcessor dp, Storage storage )
    {
        super( id, FLAT_REPOSITORY_TYPE );
        setDependencyProcessor( dp );
        
        _storage = storage;
    }
    // ----------------------------------------------------------------------------------
    public File getDirectory()
    {
        return null;
    }
    // ----------------------------------------------------------------------------------
    public RepositoryReader getReader()
    {
        return new LocalRepositoryReaderMap( this, getDependencyProcessor() );
    }
    // ----------------------------------------------------------------------------------
    public RepositoryReader getReader( String protocol )
    {
        return getReader();
    }
    // ----------------------------------------------------------------------------------
    public RepositoryWriter getWriter()
    {
        return RepositoryWriter.NULL_WRITER;
    }
    // ----------------------------------------------------------------------------------
    public RepositoryWriter getWriter( String protocol )
        throws NonExistentProtocolException
    {
        return getWriter();
    }
    // ----------------------------------------------------------------------------------
    public boolean isLocal()
    {
        return true;
    }
    // ----------------------------------------------------------------------------------
    public boolean isReadable()
    {
        return true;
    }
    // ----------------------------------------------------------------------------------
    public boolean isWriteable()
    {
        return false;
    }
    // ----------------------------------------------------------------------------------
    public String getType()
    {
        return FLAT_REPOSITORY_TYPE;
    }
    // ----------------------------------------------------------------------------------
    public String getMetadataName()
    {
        return null;
    }
    // ----------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------
}
