/*
 * GameObjectReader.java
 *
 * Created on October 26, 2001, 9:06 PM
 */

package games.strategy.engine.data;

import java.io.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class GameObjectInputStream extends ObjectInputStream
{
	private final GameData m_data;
	
	/** Creates new GameObjectReader */
    public GameObjectInputStream(GameData data, InputStream input) throws IOException
	{
		super(input);
		m_data = data;
		enableResolveObject(true);
    }
	
	public GameData getData()
	{
		return m_data;
	}
	
	protected Object resolveObject(Object obj) throws IOException
	{	
		if((obj instanceof GameObjectStreamData))
		{
			return ((GameObjectStreamData) obj).getReference(m_data);
		}
		else if(obj instanceof Unit)
		{
			return resolveUnit((Unit) obj);
		}
		else
			return obj;
	}
		
	private Object resolveUnit(Unit unit)
	{
		Unit local = Unit.get(unit.getID());
		if(local != null)
			return local;
		else
		{
			Unit.put(unit);
			return unit;
		}
	}
}
