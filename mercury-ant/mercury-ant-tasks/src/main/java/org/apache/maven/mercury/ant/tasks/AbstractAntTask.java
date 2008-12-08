package org.apache.maven.mercury.ant.tasks;

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
  private String name;
  
  public String getName()
  {
    return name;
  }

  public void setName( String name )
  {
    this.name = name;
  }
  
  

}
