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
 * ProductionResponseMessage.java
 *
 * Created on August 28, 2004
 */

package games.strategy.triplea.delegate.message;

import java.util.Collection;

import games.strategy.engine.data.Territory;
import games.strategy.engine.message.*;


public class ProductionResponseMessage implements Message
{
	
	StringMessage m_message;
	Collection m_units;
	int m_maxUnits;
	
	/** Creates new ProductionResponseMessage */
    public ProductionResponseMessage(StringMessage message) 
	{
		m_message = message;
    }

    public ProductionResponseMessage(Collection units, int maxUnits) 
	{
		m_units = units;
	    m_maxUnits = maxUnits;
   }

	public Collection getUnits()
	{
		return m_units;
	}
	
	public void setMaxUnits(int maxUnits)
	{
	}
	
	public int getMaxUnits()
	{
	    return m_maxUnits;
	}
	
	public String getMessage()
	{
	    return m_message.getMessage();
	}
	
	public boolean isError()
	{
	    if (m_message != null)
	        return m_message.isError();
	    
	    return false;
	}
	
	public String toString()
	{
		return "ProductionResponseMessage units:" + m_units;
	}
}
