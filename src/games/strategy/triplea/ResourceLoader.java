package games.strategy.triplea;

import games.strategy.engine.framework.GameRunner;
import games.strategy.util.Match;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;


/**
 * 
 * Utility for managing where images and property files for maps and units should be loaded from.
 * 
 * Based on java Classloaders.
 *
 */
public class ResourceLoader
{
    private final ClassLoader m_loader;
    
    public static ResourceLoader getMapresourceLoader(String mapName)
    {
        List<String> dirs = getPaths(mapName);
        dirs.add(new File(GameRunner.getRootFolder(), "/images").getAbsolutePath());
        
        return new ResourceLoader( dirs.toArray(new String[0]));
    }
    
    
    private static List<String> getPaths(String mapName)
    {
        //find the primary directory/file
        
        String dirName = File.separator + mapName;
        String zipName = dirName + ".zip";
        
        List<File> candidates = new ArrayList<File>();
        candidates.add(new File(GameRunner.getRootFolder() + File.separator + "maps", dirName));
        candidates.add(new File(GameRunner.getRootFolder() + File.separator + "maps", zipName));
        candidates.add(new File(GameRunner.getUserMapsFolder(), dirName));
        candidates.add(new File(GameRunner.getUserMapsFolder(), zipName));

       
        Collection<File> existing = Match.getMatches(candidates, new Match<File>() {
			public boolean match(File f) {
				return f.exists();
			}        	
        });
       
        if(existing.size() > 1)
        {
            throw new IllegalStateException("Found too many files for:" + mapName + " found:" + existing);
        }
        //at least one must exist
        if(existing.isEmpty())
        {
            throw new IllegalStateException("Could not find file for map:" + mapName);
        }
        
        File match = existing.iterator().next();
        
        
    	String fileName = match.getName();
    	if(fileName.indexOf('.') > 0) { 
    		fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    	}
    		
        if(!fileName.equals(mapName)) {
            throw new IllegalStateException("Map case is incorrect, xml:" + mapName + " file:" + match.getName());
        }
     
        
               
        List<String> rVal = new ArrayList<String>();
        rVal.add(match.getAbsolutePath());
            
        //find dependencies
        try
        {
            URLClassLoader url = new URLClassLoader(new URL[] {match.toURI().toURL()});
            URL dependencesURL = url.getResource("dependencies.txt");
            if(dependencesURL != null)
            {
                java.util.Properties dependenciesFile = new java.util.Properties(  );
                InputStream stream = dependencesURL.openStream();
                try
                {
                    dependenciesFile.load(stream);
                    String dependencies = dependenciesFile.getProperty("dependencies");
                    StringTokenizer tokens = new StringTokenizer(dependencies, ",", false ) ;
                    while(tokens.hasMoreTokens())
                    {
                        //add the dependencies recursivly
                        rVal.addAll(getPaths(tokens.nextToken()));
                    }
                    
                }
                finally
                {
                    stream.close();
                }
            }
            
            
        } catch (Exception e)
        {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage());
        }
        
        
        return rVal;
    }
    

    private ResourceLoader(String[]  paths)
    {
        URL[] urls = new URL[paths.length];
        
        for(int i =0; i < paths.length; i++)
        {            
            File f = new File(paths[i]);
            
            if(!f.exists())
            {
                System.err.println(f + " does not exist");
            }
            if(!f.isDirectory()  && !f.getName().endsWith(".zip"))
            {
                System.err.println(f + " is not a directory or a zip file");
            }
            
            try
            {
                urls[i] = f.toURI().toURL();
            } catch (MalformedURLException e)
            {
                e.printStackTrace();
                throw new IllegalStateException(e.getMessage());
            }            
        }
        
        
        
        m_loader = new URLClassLoader(urls);
    }
    
    public URL getResource(String path)
    {        
        URL rVal = m_loader.getResource(path);
        if(rVal == null)
        {
            return null;
        }
        File f;
        try {
            f = new File(URLDecoder.decode(rVal.getFile(), "utf-8")).getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        
        if(!f.getPath().endsWith(path.replace('/', File.separatorChar))) 
        {
            throw new IllegalStateException("The file:" + f.getPath() + "  does not have the correct case.  It must match the case declared in the xml:" + path );
        }
        return rVal;
    }
    
    public InputStream getResourceAsStream(String path)
    {
        return m_loader.getResourceAsStream(path);
    }
    
    
   
}
