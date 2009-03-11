package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class GameDataUtils
{
    
    public static GameData cloneGameData(GameData data)
    {
        return cloneGameData(data, false);
    }

    /**
     * Create a deep copy of GameData.
     * 
     * <Strong>You should have the game datas read or write lock before calling this method</STRONG>
     */
    public static GameData cloneGameData(GameData data, boolean copyDelegates)
    {
        
        try
        {
            GameDataManager manager = new GameDataManager();
            ByteArrayOutputStream sink = new ByteArrayOutputStream(10000);
            manager.saveGame(sink, data, copyDelegates);
            sink.close();
            ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
            sink = null;
            return manager.loadGame(source);
        } catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
        
    }
    
    /**
     * Translate units,territories and other game data objects from one
     * game data into another. 
     */
    public static Object translateIntoOtherGameData(Object object, GameData translateInto) 
    {
      
            try
            {
                ByteArrayOutputStream sink = new ByteArrayOutputStream(1024);
                GameObjectOutputStream out = new GameObjectOutputStream(sink);
                out.writeObject(object);
                out.flush();
                out.close();

                ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
                sink = null;

                GameObjectStreamFactory factory = new GameObjectStreamFactory(translateInto);
                ObjectInputStream in = factory.create(source);
                try
                {
                    return in.readObject();
                } catch (ClassNotFoundException ex)
                {
                    //should never happen
                    throw new RuntimeException(ex);
                }
            } catch (IOException ioe)
            {
                throw new RuntimeException(ioe);
            }
      
    }
    
}
