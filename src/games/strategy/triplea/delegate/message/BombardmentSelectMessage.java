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
 * BombardmentQueryMessage.java
 *
 * Created on November 19, 2004, 9:20 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.engine.message.Message;
import games.strategy.engine.data.*;

/**
 *
 * @author  Ali Ibrahim
 * @version 1.0
 */
public class BombardmentSelectMessage implements Message
{

    private Territory m_territory;

    /**
     * Constructor
     */
    public BombardmentSelectMessage(Territory territory)
    {
	m_territory = territory;
    }

    /**
     * The territory selected for bombardment. If the user does not
     * wish to bombard the value will be null.
     */
    public Territory getTerritory()
    {
	return m_territory;
    }

    public String toString()
    {
	return "BombardmentSelectMessage: " + m_territory;
    }
}
