package org.apache.maven.mercury.ant.tasks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.mercury.repository.api.Repository;
import org.apache.maven.mercury.util.Util;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.lang.DefaultLanguage;
import org.codehaus.plexus.lang.Language;

/**
 * @author Oleg Gusakov
 * @version $Id$
 */
public class Config
extends AbstractDataType
{
    private static final Language LANG = new DefaultLanguage( Config.class );
    
    public static final String DEFAULT_LOCAL_DIR = System.getProperty( "user.home" )+"/.m2/repository"; 
    
    public static final String DEFAULT_CENTRAL_URL = System.getProperty( "mercury.central.url", "http://repo1.maven.org/maven2" ); 
    
    public static final String DEFAULT_CONFIG_ID = System.getProperty( "mercury.default.config.id"
                                                                        , "mercury.default.config.id."+System.currentTimeMillis() 
                                                                      ); 
    
    public static final String DEFAULT_PATH_ID = System.getProperty( "mercury.default.path.id", "mercury.path" ); 

    Collection<Repo> _repos;

    Collection<Auth> _auths;

    List<Repository> _repositories;

    public Config()
    {
    }
    
    private Config( String localDir, String remoteUrl )
    {
        Repo local = createRepo();
        local.setId( "defaultLocalRepo" );
        local.setDir( localDir == null ? DEFAULT_LOCAL_DIR : localDir  );
        
        Repo central = createRepo();
        central.setId( "central" );
        central.setUrl( remoteUrl == null ? DEFAULT_CENTRAL_URL : remoteUrl );
    }

    public List<Repository> getRepositories()
        throws BuildException
    {
        if ( Util.isEmpty( _repos ) )
            return null;

        if ( _repositories != null )
            return _repositories;

        _repositories = new ArrayList<Repository>( _repos.size() );

        for ( Repo repo : _repos )
            _repositories.add( repo.getRepository() );

        return _repositories;
    }
    
    private void init()
    {
        if( getId() != null )
            return;
        
        setId(DEFAULT_CONFIG_ID);
         
        getProject().addReference( DEFAULT_CONFIG_ID, this );
    }

    public Repo createRepo()
    {
        init();
        
        Repo r = new Repo(true);

        listRepo( r );

        return r;
    }

    public Repo createRepository()
    {
        return createRepo();
    }

    protected void listRepo( Repo repo )
    {
        if ( _repos == null )
            _repos = new ArrayList<Repo>( 4 );

        _repos.add( repo );
    }
    
    public static void addDefaultRepository( Project project, Repo repo )
    {
        Object co = project.getReference( DEFAULT_CONFIG_ID );

        if( co == null )
        {
            co = new Config();
            
            project.addReference( DEFAULT_CONFIG_ID, co );
        }
        
        Config config = (Config) co;
        
        config.listRepo( repo );
    }
    
    public static Config getDefaultConfig( Project project )
    {
        Object co = project.getReference( DEFAULT_CONFIG_ID );
        if( co == null )
        {
            co = new Config( null, null );
            
            project.addReference( DEFAULT_CONFIG_ID, co );
        }
        
        Config config = (Config) co;
        
        return config;
    }

    public Auth createAuth()
    {
        init();
        
        if ( _auths == null )
            _auths = new ArrayList<Auth>( 4 );

        Auth a = new Auth();

        _auths.add( a );

        return a;
    }

    //======================================================================================


}
