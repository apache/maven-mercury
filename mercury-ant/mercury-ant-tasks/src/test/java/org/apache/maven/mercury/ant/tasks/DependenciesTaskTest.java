package org.apache.maven.mercury.ant.tasks;

import java.io.File;

import org.apache.maven.mercury.spi.http.server.HttpTestServer;
import org.apache.maven.mercury.spi.http.server.SimpleTestServer;
import org.apache.maven.mercury.util.FileUtil;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.types.Path;

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
  static final String localRepoDir = "./target/repo";
  static final String remoteRepoDir = "./target/test-classes/remoteRepo";
  static final String remoteRepoUrlPrefix = "http://localhost:";
  static final String remoteRepoUrlSufix = "/repo";
//  static final String remoteRepoUrlPrefix = "http://repo1.maven.org/maven2";
//static final String remoteRepoUrl = "http://people.apache.org/~ogusakov/repos/test";
//static final String remoteRepoUrl = "http://repository.sonatype.org/content/groups/public" );

  static final String pathId = "class-path";
  
  SimpleTestServer jetty;
  String port;
    
  Resolver _resolver;
  Config   _config;
  Dep      _dep;
  
  Dep.Dependency _asm;
  Dep.Dependency _ant;
    
  //-----------------------------------
  final class Resolver
  extends ResolveTask
  {
    @SuppressWarnings("deprecation")
    public Resolver()
    {
      project = new Project();
      project.init();
      target = new Target();
    }
  }
  //-----------------------------------
  @Override
  protected void setUp()
  throws Exception
  {
    _dep = new Dep();
    _dep.setId( "my-lib" );
    
    _asm = _dep.createDependency();
    _asm.setName( "asm:asm-xml:3.0" );
    
    _ant = _dep.createDependency();
    _ant.setName( "ant:ant:1.6.5" );
    
    _config = new Config();
    _config.setId( "conf" );
    
    File lrDir = new File( localRepoDir );
    FileUtil.delete( lrDir );
    lrDir.mkdirs();
    
    Config.Repo localRepo = _config.createRepo();
    localRepo.setId( "localRepo" );
    localRepo.setDir( localRepoDir );
    
    File rrDir = new File( remoteRepoDir );
    jetty = new SimpleTestServer( rrDir, remoteRepoUrlSufix );
    jetty.start();
    port = ""+jetty.getPort();
    
    Config.Repo remoteRepo = _config.createRepo();
    remoteRepo.setId( "remoteRepo" );
    remoteRepo.setUrl( remoteRepoUrlPrefix + port + remoteRepoUrlSufix );
        
    _resolver = new Resolver();
    _resolver.setDepid( _dep.getId() );
    _resolver.setConfigid( _config.getId() );
    
    _resolver.setPathid( pathId );
    
    Project project = _resolver.getProject();
    
    project.addReference( _config.getId(), _config );
    project.addReference( _dep.getId(), _dep );
    
  }
  //-----------------------------------
  @Override
  protected void tearDown()
  throws Exception
  {
    jetty.stop();
    jetty.destroy();
  }
  //-----------------------------------
  public void testReadDependency()
  {
    _resolver.execute();
    
    Project pr = _resolver.getProject();
    
    Path path = (Path)pr.getReference( pathId );
    
    assertNotNull( path );
    
    String [] list = path.list();
    
    assertNotNull( list );

    assertEquals( 6, list.length );
    
    System.out.println("\n==== Files found ====");
    for( String s : list )
      System.out.println(s);
    
  }
  //-----------------------------------
}
