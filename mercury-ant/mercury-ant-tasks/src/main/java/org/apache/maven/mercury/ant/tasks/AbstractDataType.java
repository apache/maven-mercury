package org.apache.maven.mercury.ant.tasks;

import org.apache.tools.ant.ProjectComponent;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public abstract class AbstractDataType
extends ProjectComponent
{
  private String id;
  
  public String getId()
  {
    return id;
  }
  
  public void setId( String id )
  {
      this.id = id;
  }
}
