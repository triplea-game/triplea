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
 * MustMoveWithReply.java
 *
 * Created on December 3, 2001, 6:25 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.util.IntegerMap;
import games.strategy.engine.message.Message;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A response to a must move query.
 * Returns a mapping of unit -> collection of units.
 * Units that must move are land units in transports, 
 * and friendly aircracft that must move with carriers.
 */
public class MustMoveWithDetails implements Message
{
	/**
	 * Maps Unit -> Collection of units.
	 */
	private Map m_mapping;
	private IntegerMap m_movement;
	
	
	/** Creates new MustMoveWithReplay */
    public MustMoveWithDetails(Map mapping, IntegerMap movement) 
	{
		m_mapping = mapping;
		m_movement = movement;
    }

	public Map getMustMoveWith()
	{
		return m_mapping;
	}

	public IntegerMap getMovement()
	{
		return m_movement;
	}
}
