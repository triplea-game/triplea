/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.triplea.delegate.message;

import games.strategy.engine.data.Territory;
import games.strategy.engine.message.Message;

public class MoveFightersToNewCarrierMessage implements Message
{

    private final int m_numberOfFighters;
    private final Territory m_territory;

    public MoveFightersToNewCarrierMessage(int numberOfFighters, Territory territory)
    {

        m_numberOfFighters = numberOfFighters;
        m_territory = territory;
    }

    public int getNumberOfFighters()
    {
        return m_numberOfFighters;
    }

    public Territory getTerritory()
    {
        return m_territory;
    }

}