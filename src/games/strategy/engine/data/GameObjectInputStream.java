/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * GameObjectReader.java
 *
 * Created on October 26, 2001, 9:06 PM
 */

package games.strategy.engine.data;

import java.io.*;
import games.strategy.engine.framework.GameObjectStreamFactory;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class GameObjectInputStream extends ObjectInputStream
{
	private final GameObjectStreamFactory m_dataSource;

	/** Creates new GameObjectReader */
    public GameObjectInputStream(GameObjectStreamFactory dataSource, InputStream input) throws IOException
	{
		super(input);

		m_dataSource = dataSource;
		enableResolveObject(true);
    }

	public GameData getData()
	{
		return m_dataSource.getData();
	}

	protected Object resolveObject(Object obj) throws IOException
	{
    if(obj instanceof GameData)
    {
       return m_dataSource.getData();
    }
		else if((obj instanceof GameObjectStreamData))
		{
			return ((GameObjectStreamData) obj).getReference(getData());
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
