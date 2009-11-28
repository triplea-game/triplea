package games.strategy.triplea.xml;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;

import java.io.InputStream;

public class LoadGameUtil
{

    public static GameData loadGame(String map, String game) 
    {
    	
        InputStream is = LoadGameUtil.class.getResourceAsStream(game);
        
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
