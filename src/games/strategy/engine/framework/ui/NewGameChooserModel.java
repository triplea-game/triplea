package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameParseException;
import games.strategy.engine.framework.GameRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.DefaultListModel;

import org.xml.sax.SAXException;

public class NewGameChooserModel extends DefaultListModel
{

    public NewGameChooserModel()
    {
        populate();
    }

    public NewGameChooserEntry get(int i)
    {
        return (NewGameChooserEntry) super.get(i);
    }
    
    private void populate()
    {
        File maps = new File(GameRunner.getRootFolder(), "maps");
        List<NewGameChooserEntry> entries = new ArrayList<NewGameChooserEntry>();
        for(File map : maps.listFiles()) 
        {
            if(map.isDirectory()) 
            {
                populateFromDirectory(map, entries);
            }
            else if(map.isFile() && map.getName().toLowerCase().endsWith(".zip")) 
            {
                populateFromZip(map, entries);
            }
        }     
        
        //remove any null entries
        do {} while(entries.remove(null));
        
        Collections.sort(entries, new Comparator<NewGameChooserEntry>()
        {
        
            public int compare(NewGameChooserEntry o1, NewGameChooserEntry o2)
            {
                return o1.getGameData().getGameName().compareTo(o2.getGameData().getGameName());
            }
        
        });
        for(NewGameChooserEntry entry : entries) 
        {
            addElement(entry);
        }
    }
    
    private void populateFromZip(File map, List<NewGameChooserEntry> entries)
    {
        try
        {
            FileInputStream fis = new FileInputStream(map);
            try
            {
                ZipInputStream zis = new ZipInputStream(fis);
                ZipEntry entry = zis.getNextEntry();
                while(entry != null) {
                                                   
                    if(entry.getName().startsWith("games/") && entry.getName().toLowerCase().endsWith(".xml")) 
                    {
                        URLClassLoader loader = new URLClassLoader(new URL[] {map.toURL()});
                        URL url = loader.getResource(entry.getName());
                        try
                        {
                            entries.add(createEntry(url.toURI()));
                        } catch (URISyntaxException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    zis.closeEntry();
                    entry = zis.getNextEntry();
                }
                
            } finally
            {
                fis.close();
            }
        } catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        
            
        
    }
    
    public NewGameChooserEntry findByName(String name) 
    {
        for(int i = 0; i < size(); i++) 
        {
            if(get(i).getGameData().getGameName().equals(name)) {
                return get(i);
            }
        }
        return null;
    }
     
    
    private NewGameChooserEntry createEntry(URI uri)
    {
        try
        {
            return new NewGameChooserEntry(uri);
        } catch (IOException e)
        {                    
            e.printStackTrace();
        } catch (GameParseException e)
        {                 
            e.printStackTrace();
        } catch (SAXException e)
        {                 
            e.printStackTrace();
        }
        return null;
    }

    private void populateFromDirectory(File mapDir, List<NewGameChooserEntry> entries)
    {
        File games = new File(mapDir, "games");
        if(!games.exists()) 
        {
            //no games in this map dir
            return;
        }
        
        for(File game : games.listFiles()) 
        {
            if(game.isFile() && game.getName().toLowerCase().endsWith("xml")) {
               entries.add(createEntry(game.toURI()));
            }
        }

        
    }
    
    
    
}


