package org.apache.maven.mercury.ant.tasks;

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
    private static final Language LANG = new DefaultLanguage( ResolveTask.class );

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
