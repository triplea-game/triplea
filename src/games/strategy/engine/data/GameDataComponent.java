/*
 * GameDataComponent.java
 *
 * Created on November 6, 2001, 2:50 PM
 */

package games.strategy.engine.data;

import java.io.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 * 
 * Serialization note. 
 * Game data is not serialized.
 * Game data is read from GameDataObjectInputStream
 */
public class GameDataComponent
{
	static final long serialVersionUID = -2066504666509851740L;
	
	private transient GameData m_data;
	
	/** Creates new GameDataComponent */
    public GameDataComponent(GameData data) 
	{
		m_data = data;
    }
	
	GameDataComponent()
	{}
	
	protected GameData getData()
	{
		return m_data;
	}
	
	protected void setGameData(GameData data)
	{
		m_data = data;
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		if(! (stream instanceof GameObjectInputStream))
			throw new IllegalArgumentException("Can only be read by GameObjectInputStream");
	
		GameObjectInputStream in = (GameObjectInputStream) stream;
		m_data = in.getData();
	}
		
}
