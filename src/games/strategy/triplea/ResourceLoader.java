package games.strategy.triplea;

import games.strategy.engine.framework.GameRunner;

import java.io.*;
import java.net.*;


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
        File dir = new File(GameRunner.getRootFolder(), "/maps/" + mapName);
        File zip = new File(GameRunner.getRootFolder(), "/maps/" + mapName + ".zip");
        
        //if we have a zip file, but no directory, use the zip file
        if(!dir.exists() && zip.exists())
        {
            return getMapresourceLoader(mapName + ".zip");
        }
        
        
        return new ResourceLoader( new String[] {"/maps/" + mapName, "/images"} );
    }

    private ResourceLoader(String[]  paths)
    {
        URL[] urls = new URL[paths.length];
        
        for(int i =0; i < paths.length; i++)
        {
            File root = GameRunner.getRootFolder();
            File f = new File(root, paths[i]);
            
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
        return m_loader.getResource(path);
    }
    
    public InputStream getResourceAsStream(String path)
    {
        return m_loader.getResourceAsStream(path);
    }
    
    
   
}
