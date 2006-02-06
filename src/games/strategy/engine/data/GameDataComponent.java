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
 */
public class GameDataComponent implements java.io.Serializable
{
	static final long serialVersionUID = -2066504666509851740L;

	private GameData m_data;

	/** Creates new GameDataComponent */
    public GameDataComponent(GameData data)
	{
		m_data = data;
    }


	protected GameData getData()
	{
		return m_data;
	}

	protected void setGameData(GameData data)
	{
		m_data = data;
	}

	private void writeObject(ObjectOutputStream stream) throws IOException
	{
		writeInternal(stream);
	}


    protected final void writeInternal(ObjectOutput stream) throws IOException
    {
        //if were writing to a game object stream
		//then we get the game data from the context
		//else we write it.
		if(stream instanceof GameObjectOutputStream)
			return;
		else
		    stream.writeObject(m_data);
    }

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException
	{
        readInternal(stream);
	}
    
    protected final void readInternal(ObjectInput stream) throws IOException, ClassNotFoundException
    {
        if(stream instanceof GameObjectInputStream)
        {
            GameObjectInputStream in = (GameObjectInputStream) stream;
            m_data = in.getData();
        }
        else
            m_data = (GameData) stream.readObject();
    }

}
