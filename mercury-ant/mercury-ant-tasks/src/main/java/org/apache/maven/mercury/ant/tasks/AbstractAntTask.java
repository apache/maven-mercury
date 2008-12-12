package org.apache.maven.mercury.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * 
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class AbstractAntTask
extends Task
{
  protected boolean _failOnError = true;
  //----------------------------------------------------------------------------------------
  public void setFailonerror( boolean failonerror )
  {
    this._failOnError = failonerror;
  }
  //----------------------------------------------------------------------------------------
  protected void throwIfEnabled( String msg )
  throws BuildException
  {
    if( _failOnError )
      throw new BuildException( msg );
  }
  //----------------------------------------------------------------------------------------
  //----------------------------------------------------------------------------------------
}
