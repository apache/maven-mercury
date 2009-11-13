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
package org.apache.maven.mercury.builder.api;

import org.apache.maven.mercury.artifact.ArtifactMetadata;
import org.apache.maven.mercury.repository.api.RepositoryException;

/**
 * This interface implementation is supplied to MetadataProcessor to simplify it's access to remote repositories
 * 
 * @author Oleg Gusakov
 * @version $Id$
 */
public interface MetadataReader
{
    /**
     * read raw content pointed by bmd, possibly modified by classifier and type
     * 
     * @param bmd coordinates
     * @param classifier - replaces the getClassifier() from bmd if not null
     * @param type - replaces the getType() from bmd if not null
     * @param exempt - if this read should be exempt from stream verification, default - false
     * @return
     * @throws MetadataReaderException
     * @throws RepositoryException
     */
    byte[] readRawData( ArtifactMetadata bmd, String classifier, String type, boolean exempt )
        throws MetadataReaderException;

    byte[] readRawData( ArtifactMetadata bmd, String classifier, String type )
        throws MetadataReaderException;

    /**
     * read metadata for the artifact, pointed by bmd. It will return POM bytes regardless of actual
     * bmd type
     * 
     * @param bmd metadata to read
     * @param exempt - if this read should be exempt from stream verification, default - false
     * @return
     * @throws MetadataReaderException
     */
    byte[] readMetadata( ArtifactMetadata bmd, boolean exempt )
        throws MetadataReaderException;

    byte[] readMetadata( ArtifactMetadata bmd )
        throws MetadataReaderException;
}
