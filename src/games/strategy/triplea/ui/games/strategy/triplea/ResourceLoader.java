package games.strategy.triplea;

import games.strategy.engine.framework.GameRunner;

import java.io.*;
import java.net.*;
import java.util.*;


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
        dirs.add("/images");
        
        return new ResourceLoader( dirs.toArray(new String[0]));
    }
    
    
    private static List<String> getPaths(String mapName)
    {
        //find the primary directory/file
        
        String dirName = File.separator + "maps" + File.separator + mapName;
        String zipName = dirName + ".zip";
        
        File dir = new File(GameRunner.getRootFolder(), dirName);
        File zip = new File(GameRunner.getRootFolder(), zipName);

        //they cant both exist
        if(dir.exists() && zip.exists())
        {
            throw new IllegalStateException("Found both zip:" + zip + " and dir:" + dir + " for skin:" + mapName);
        }
        //at least one must exist
        if(!dir.exists() && !zip.exists())
        {
            throw new IllegalStateException("Found neither zip:" + zip + " or dir:" + dir + " for skin:" + mapName);
        }
        
        
       
        File addedFile;
        List<String> rVal = new ArrayList<String>();
        if(dir.exists())
        {
            rVal.add(dirName);
            addedFile = dir;
        }
        else
        {
            rVal.add(zipName);
            addedFile = zip;
        }
            
        //find dependencies
        try
        {
            URLClassLoader url = new URLClassLoader(new URL[] {addedFile.toURI().toURL()});
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
