/*
 * GameObjectInoutStreamFactory.java
 *
 * Created on January 1, 2002, 4:50 PM
 */

package games.strategy.engine.framework;

import java.io.*;

import games.strategy.net.IObjectStreamFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectInputStream;
import games.strategy.engine.data.GameObjectOutputStream;

/**
 *
 * @author  Sean Bridges
 */
public class GameObjectStreamFactory implements IObjectStreamFactory 
{
	private GameData m_data;
	
    public GameObjectStreamFactory(GameData data) 
	{
		m_data = data;
    }

	public ObjectInputStream create(InputStream stream) throws IOException
	{
		return new GameObjectInputStream(m_data, stream);
	}
	
	public ObjectOutputStream create(OutputStream stream) throws IOException
	{
		return new GameObjectOutputStream(m_data, stream);
	}
}
