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
 * ProductionRequestMessage.java
 *
 * Created on August 28, 2004
 */

package games.strategy.triplea.delegate.message;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.message.*;


public class ProductionRequestMessage implements Message
{
	Collection m_units;
	Territory m_to;
	
	/** Creates new ProductionRequestMessage */
    public ProductionRequestMessage(Collection units, Territory to) 
	{
		m_units = units;
		m_to = to;
    }

	public Collection getUnits()
	{
		return m_units;
	}
	
	public Territory getTo()
	{
		return m_to;
	}
	
	public String toString()
	{
		return "ProductionRequestMessage units:" + m_units + " to::" + m_to;
	}
}
