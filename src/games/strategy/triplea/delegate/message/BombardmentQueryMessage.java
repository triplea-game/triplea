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
 * Created on November 18, 2004, 6:00 PM
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
public class BombardmentQueryMessage implements Message
{

    private Unit m_unit;
    private Territory m_unitTerritory;
    private Collection m_territories;
    private boolean m_noneAvailable;

    /**
     * Constructor
     */
    public BombardmentQueryMessage(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
    {
	m_unit = unit;
	m_unitTerritory = unitTerritory;
	m_territories = territories;
	m_noneAvailable = noneAvailable;
    }

    /**
     * A list of territories that could be bombarded
     */
    public Collection getTerritories()
    {
	return m_territories;
    }

    /**
     * Unit which is bombarding.
     */
    public Unit getUnit()
    {
	return m_unit;
    }

    /**
     * Territory where unit is located
     */
    public Territory getUnitTerritory()
    {
	return m_unitTerritory;
    }

    /**
     * Should we allow user to choose not to bombard.
     */
    public boolean isNoneAvailable() {
	return m_noneAvailable;
    }

    public String toString()
    {
	return "BombardmentQueryMessage: " + m_unit + " " + m_territories + " " + m_noneAvailable;
    }
}
