/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * SelectCasualtyQueryMessage.java
 * 
 * Created on November 19, 2001, 2:59 PM
 */
package games.strategy.triplea.delegate.dataObjects;

import games.strategy.engine.data.Unit;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author Sean Bridges & Mark Christopher Duncan
 * @version 1.1
 */
public class CasualtyDetails extends CasualtyList implements Serializable
{
	private static final long serialVersionUID = 2261683015991514918L;
	// since this now extends CasualtyList, it has access to the protected fields of m_killed and m_damaged
	private final boolean m_autoCalculated;
	
	/**
	 * Creates new SelectCasualtyMessage
	 * 
	 * @param killed
	 *            killed units
	 * @param damaged
	 *            damaged units (Can have multiple of the same unit, to show multiple hits to that unit.)
	 * @param autoCalculated
	 *            whether casualties should be selected automatically
	 */
	public CasualtyDetails(final List<Unit> killed, final List<Unit> damaged, final boolean autoCalculated)
	{
		super(killed, damaged);
		m_autoCalculated = autoCalculated;
	}
	
	public CasualtyDetails(final CasualtyList casualties, final boolean autoCalculated)
	{
		super((casualties == null ? null : casualties.getKilled()), (casualties == null ? null : casualties.getDamaged()));// since null for the lists is illegal, we will throw an error in super, as intended
		m_autoCalculated = autoCalculated;
	}
	
	public CasualtyDetails(final boolean autoCalculated)
	{
		super();
		m_autoCalculated = autoCalculated;
	}
	
	/**
	 * Empty details, with autoCalculated as true.
	 */
	public CasualtyDetails()
	{
		super();
		m_autoCalculated = true;
	}
	
	public boolean getAutoCalculated()
	{
		return m_autoCalculated;
	}
}
