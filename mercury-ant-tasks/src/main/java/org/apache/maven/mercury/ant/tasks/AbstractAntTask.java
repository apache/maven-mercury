package org.apache.maven.mercury.ant.tasks;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class AbstractAntTask
    extends Task
{
    private static final Language LANG = new DefaultLanguage( AbstractAntTask.class );

    protected boolean _failOnError = true;

    // ----------------------------------------------------------------------------------------
    public void setFailonerror( boolean failonerror )
    {
        this._failOnError = failonerror;
    }

    // ----------------------------------------------------------------------------------------
    protected void throwIfEnabled( String msg )
        throws BuildException
    {
        if ( _failOnError )
        {
            throw new BuildException( msg );
        }
    }

    // ----------------------------------------------------------------------------------------
    public static final Config findConfig( Project project, String configId )
        throws Exception
    {
        Config config = null;

        if ( configId == null )
        {
            config = Config.getDefaultConfig( project );
        }
        else
        {
            Object so = project.getReference( configId );

            if ( so == null )
            {
                throw new Exception( LANG.getMessage( "config.id.object.null", configId ) );
            }

            if ( !Config.class.isAssignableFrom( so.getClass() ) )
            {
                throw new Exception( LANG.getMessage( "config.id.object.wrong", configId, so.getClass().getName() ) );
            }

            config = (Config) so;
        }

        return config;
    }
    // ----------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
}
