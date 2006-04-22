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

package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Unit;

import java.util.Collection;


public class PlaceableUnits implements java.io.Serializable
{
	
	private String m_errorMessage;
	private Collection<Unit> m_units;
	private int m_maxUnits;
	
	/** Creates new ProductionResponseMessage */
    public PlaceableUnits(String errorMessage) 
	{
		m_errorMessage = errorMessage;
    }

    public PlaceableUnits(Collection<Unit> units, int maxUnits) 
	{
		m_units = units;
	    m_maxUnits = maxUnits;
   }

	public Collection<Unit> getUnits()
	{
		return m_units;
	}
	

	/**
     * 
     * @return -1 if no limit
	 */
	public int getMaxUnits()
	{
	    return m_maxUnits;
	}
	
	public String getErrorMessage()
	{
	    return m_errorMessage;
	}
	
	public boolean isError()
	{
	   return m_errorMessage != null;
	}
	
	public String toString()
	{
		return "ProductionResponseMessage units:" + m_units;
	}
}
