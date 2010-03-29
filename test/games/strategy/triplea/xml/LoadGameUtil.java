package games.strategy.triplea.xml;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class LoadGameUtil
{

    public static GameData loadGame(String map, String game) 
    {
    	
        InputStream is = LoadGameUtil.class.getResourceAsStream(game);
        if(is == null) {
        	File f = new File(new File(GameRunner.getRootFolder(), "maps"), game);
        	if(f.exists()) { 
        		try {
					is = new FileInputStream(f);
				} catch (FileNotFoundException e) {
					throw new IllegalStateException(e);
				}
        	}
        		
        }
        
        if(is == null)
            throw new IllegalStateException(game + " does not exist");
        try
        {
            
            
            try
            {
                return (new GameParser()).parse(is);
            }
            finally
            {
                is.close();    
            }
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
