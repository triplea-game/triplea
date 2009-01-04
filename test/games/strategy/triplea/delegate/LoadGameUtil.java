package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.framework.GameRunner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class LoadGameUtil
{

    public static GameData loadGame(String map, String game) 
    {
        File gameRoot  = GameRunner.getRootFolder();
        File mapsFolder = new File(gameRoot, "maps");
        File mapFolder = new File(mapsFolder, map);
        File gamesFolder = new File(mapFolder, "games");
        File gameFile = new File(gamesFolder, game);
        
        if(!gameFile.exists())
            throw new IllegalStateException("revised does not exist");
        try
        {
            InputStream input = new BufferedInputStream(new FileInputStream(gameFile));
            
            try
            {
                return (new GameParser()).parse(input);
            }
            finally
            {
                input.close();    
            }
        } catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
