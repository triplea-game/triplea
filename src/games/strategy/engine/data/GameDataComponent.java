/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * GameDataComponent.java
 * 
 * Created on November 6, 2001, 2:50 PM
 */
package games.strategy.engine.data;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class GameDataComponent implements java.io.Serializable
{
	static final long serialVersionUID = -2066504666509851740L;
	private GameData m_data;
	
	/**
	 * Creates new GameDataComponent
	 * 
	 * @param data
	 *            game data
	 */
	public GameDataComponent(final GameData data)
	{
		m_data = data;
	}
	
	public GameData getData()
	{
		return m_data;
	}
	
	private void writeObject(final ObjectOutputStream stream) throws IOException
	{
		writeInternal(stream);
	}
	
	protected final void writeInternal(final ObjectOutput stream) throws IOException
	{
		// if were writing to a game object stream
		// then we get the game data from the context
		// else we write it.
		if (stream instanceof GameObjectOutputStream)
			return;
		stream.writeObject(m_data);
	}
	
	private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
		readInternal(stream);
	}
	
	protected final void readInternal(final ObjectInput stream) throws IOException, ClassNotFoundException
	{
		if (stream instanceof GameObjectInputStream)
		{
			final GameObjectInputStream in = (GameObjectInputStream) stream;
			m_data = in.getData();
		}
		else
			m_data = (GameData) stream.readObject();
	}
}
