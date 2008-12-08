package org.apache.maven.mercury.ant.tasks;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;

import junit.framework.TestCase;

/**
 *
 *
 * @author Oleg Gusakov
 * @version $Id$
 *
 */
public class DependenciesTaskTest
    extends TestCase
{
  final class Contributor
  extends DependenciesTask
  {
    @SuppressWarnings("deprecation")
    public Contributor()
    {
      project = new Project();
      project.init();
      target = new Target();
    }
  }
    
  Contributor _contributor;
  
  DependenciesTask.Dependency _dep;
    
  @Override
  protected void setUp()
  throws Exception
  {
    _contributor = new Contributor();
    
    _dep = _contributor.createDependency();
    _dep.setName( "ant:ant:1.6.5" );
    
  }
  
  public void testReadDependency()
  {
    _contributor.execute();
  }

}
