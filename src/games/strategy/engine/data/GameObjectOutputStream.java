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

	/** Creates a new instance of GameObjectOutputStream */
    public GameObjectOutputStream(OutputStream output) throws IOException
	{
		super(output);
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