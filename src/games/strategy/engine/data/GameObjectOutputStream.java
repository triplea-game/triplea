/*
 * GameObjectOutputStream.java
 *
 * Created on January 3, 2002, 2:47 PM
 */

package games.strategy.engine.data;

import java.io.*;

/**
 *
 * @author  Sean Bridges
 */
public class GameObjectOutputStream extends ObjectOutputStream
{
	
	private final GameData m_data;

	/** Creates a new instance of GameObjectOutputStream */
    public GameObjectOutputStream(GameData data, OutputStream output) throws IOException
	{
		super(output);
		m_data = data;
		enableReplaceObject(true);
    }
	
	protected Object replaceObject(Object obj) throws IOException
	{
		if(obj instanceof Named)
		{
			Named named = (Named) obj;
			if(GameObjectStreamData.canSerialize(named))
			{
				return new GameObjectStreamData(named);
			}
		}
		return obj;
	}
}