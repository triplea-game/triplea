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
 * Please refer to the comments on GameObjectOutputStream
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
    //when loading units, we want to maintain == relationships for many 
    //of the game data objects.
    //this is to prevent the situation where we have 2 Territory objects for the 
    //the same territory, or two object for the same player id or ...
    //thus, in one vm you can add some units to a territory, and when you serialize the change
    //and look at the Territory object in another vm, the units have not been added 
      
    if (obj instanceof GameData)
    {
      return m_dataSource.getData();
    }
    else if ( (obj instanceof GameObjectStreamData))
    {
      return ( (GameObjectStreamData) obj).getReference(getData());
    }
    else if (obj instanceof Unit)
    {
      return resolveUnit( (Unit) obj);
    }
    else
      return obj;
  }

  private Object resolveUnit(Unit unit)
  {

    Unit local = m_dataSource.getData().getUnits().get(unit.getID());
    if (local != null)
      return local;
    else
    {
      getData().getUnits().put(unit);
      return unit;
    }
  }
}
