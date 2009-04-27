package org.apache.maven.mercury.artifact;

/*
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

import java.io.File;
import java.io.InputStream;
import java.util.regex.Pattern;

/**
 * TODO: Oleg: don't know if this should be interface or class, so there is a little disparate: ArtifactMetadata is a
 * class, Artifact is an interface. Will clear out
 * 
 * @author Oleg Gusakov
 * @version $Id$
 */
public interface Artifact
    extends Comparable<Artifact>
{
    static final String LATEST_VERSION = "LATEST";

    static final String SNAPSHOT_VERSION = "SNAPSHOT";

    static final String RELEASE_VERSION = "RELEASE";

    static final String SNAPSHOT_TS_REGEX = ".+-\\d{8}\\.\\d{6}-\\d+";

    static final Pattern VERSION_FILE_PATTERN = Pattern.compile( "^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$" );

    // TODO: into artifactScope handler

    static final String SCOPE_COMPILE = ArtifactScopeEnum.compile.toString();

    static final String SCOPE_TEST = ArtifactScopeEnum.test.toString();

    static final String SCOPE_RUNTIME = ArtifactScopeEnum.runtime.toString();

    static final String SCOPE_PROVIDED = ArtifactScopeEnum.provided.toString();

    static final String SCOPE_SYSTEM = ArtifactScopeEnum.system.toString();

    String getGroupId();

    String getArtifactId();

    String getVersion();

    void setVersion( String version );

    /**
     * Get the artifactScope of the artifact. If the artifact is a standalone rather than a dependency, it's
     * artifactScope will be <code>null</code>. The artifactScope may not be the same as it was declared on the original
     * dependency, as this is the result of combining it with the main project artifactScope.
     * 
     * @return the artifactScope
     */
    String getScope();

    String getType();

    String getClassifier();

    // only providing this since classifier is *very* optional...
    boolean hasClassifier();

    File getFile();

    // in case binary is supplied as a Stream, not a File
    InputStream getStream();

    void setFile( File destination );

    byte[] getPomBlob();

    void setPomBlob( byte[] pomBlob );

    String getBaseName();

    String getBaseName( String classifier );

    // ----------------------------------------------------------------------

    String getId();

    void setGroupId( String groupId );

    void setArtifactId( String artifactId );
}