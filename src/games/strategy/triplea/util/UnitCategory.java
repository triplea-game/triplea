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

package games.strategy.triplea.util;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitTypeComparator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class UnitCategory implements Comparable
{
	private final UnitType m_type;
	// Collection of UnitOwners, the type of our dependents, not the dependents
	private Collection<UnitOwner> m_dependents;
	private final int m_movement; // movement of the units
	private final int m_transportCost; // movement of the units
	private final Territory m_originatingTerr; // movement of the units
	private final PlayerID m_owner;
	// the units in the category, may be duplicates.
	private final List<Unit> m_units = new ArrayList<Unit>();
	
	private boolean m_damaged = false;
	private boolean m_disabled = false;
	
	public UnitCategory(Unit unit, boolean categorizeDependents, boolean categorizeMovement, boolean categorizeTransportcost, boolean categorizeTerritory)
	{
		TripleAUnit taUnit = (TripleAUnit) unit;
		m_type = taUnit.getType();
		m_owner = taUnit.getOwner();
		m_movement = categorizeMovement ? taUnit.getMovementLeft() : -1;
		m_transportCost = categorizeTransportcost ? UnitAttachment.get((unit).getUnitType()).getTransportCost() : -1;
		m_originatingTerr = categorizeTerritory ? taUnit.getOriginatedFrom() : null;
		m_damaged = (taUnit.getHits() > 0);
		m_disabled = Matches.UnitIsDisabled().match(unit);
		if (categorizeDependents)
			createDependents(taUnit.getDependents());
		else
			m_dependents = Collections.emptyList();
	}
	
	public UnitCategory(Unit unit, Collection<Unit> dependents, int movement, int transportCost)
	{
		this(unit, dependents, movement, false, false, transportCost, null);
	}
	
	public UnitCategory(UnitType type, PlayerID owner)
	{
		m_type = type;
		m_dependents = Collections.emptyList();
		m_movement = -1;
		m_transportCost = -1;
		m_owner = owner;
		m_originatingTerr = null;
	}
	
	public UnitCategory(Unit unit, Collection<Unit> dependents, int movement, boolean damaged, boolean disabled, int transportCost, Territory t)
	{
		m_type = unit.getType();
		m_movement = movement;
		m_transportCost = transportCost;
		m_owner = unit.getOwner();
		m_damaged = damaged;
		m_disabled = disabled;
		m_units.add(unit);
		m_originatingTerr = t;
		createDependents(dependents);
	}
	
	public boolean getDamaged()
	{
		return m_damaged;
	}
	
	public boolean getDisabled()
	{
		return m_disabled;
	}
	
	public boolean isTwoHit()
	{
		return UnitAttachment.get(m_type).isTwoHit();
	}
	
	private void createDependents(Collection<Unit> dependents)
	{
		m_dependents = new ArrayList<UnitOwner>();
		
		if (dependents == null)
			return;
		
		Iterator<Unit> iter = dependents.iterator();
		
		while (iter.hasNext())
		{
			Unit current = iter.next();
			m_dependents.add(new UnitOwner(current));
		}
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null)
			return false;
		if (!(o instanceof UnitCategory))
			return false;
		
		UnitCategory other = (UnitCategory) o;
		
		// equality of categories does not compare the number
		// of units in the category, so don't compare on m_units
		boolean equalsIgnoreDamaged = equalsIgnoreDamaged(other);
		boolean equalsIgnoreDisabled = equalsIgnoreDisabled(other);
		
		// return equalsIgnoreDamaged && other.m_damaged == this.m_damaged;
		return equalsIgnoreDamaged && equalsIgnoreDisabled && other.m_damaged == this.m_damaged && other.m_disabled == this.m_disabled;
	}
	
	public boolean equalsIgnoreDamaged(UnitCategory other)
	{
		boolean equalsIgnoreDamaged =
					other.m_type.equals(this.m_type) &&
								other.m_movement == this.m_movement &&
								other.m_owner.equals(this.m_owner) &&
								Util.equals(this.m_dependents, other.m_dependents);
		return equalsIgnoreDamaged;
	}
	
	public boolean equalsIgnoreDisabled(UnitCategory other)
	{
		boolean equalsIgnoreDisabled =
					other.m_type.equals(this.m_type) &&
								other.m_movement == this.m_movement &&
								other.m_owner.equals(this.m_owner) &&
								Util.equals(this.m_dependents, other.m_dependents);
		return equalsIgnoreDisabled;
	}
	
	public boolean equalsIgnoreMovement(UnitCategory other)
	{
		boolean equalsIgnoreMovement =
					other.m_type.equals(this.m_type) &&
								other.m_owner.equals(this.m_owner) &&
								other.m_damaged == this.m_damaged &&
								other.m_disabled == this.m_disabled &&
								Util.equals(this.m_dependents, other.m_dependents);
		return equalsIgnoreMovement;
	}
	
	public boolean equalsIgnoreDependents(UnitCategory other)
	{
		boolean equalsIgnoreDependents =
					other.m_type.equals(this.m_type) &&
								other.m_owner.equals(this.m_owner) &&
								other.m_movement == this.m_movement &&
								other.m_damaged == this.m_damaged &&
								other.m_disabled == this.m_disabled;
		;
		return equalsIgnoreDependents;
	}
	
	@Override
	public int hashCode()
	{
		return m_type.hashCode() | m_owner.hashCode();
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("Entry type:").append(m_type.getName())
					.append(" owner:").append(m_owner.getName())
					.append(" damaged:").append(m_damaged)
					.append(" disabled:").append(m_disabled)
					.append(" dependents:").append(m_dependents)
					.append(" movement:").append(m_movement);
		return sb.toString();
	}
	
	/**
	 * Collection of UnitOwners, the type of our dependents, not the dependents
	 */
	public Collection<UnitOwner> getDependents()
	{
		return m_dependents;
	}
	
	public List<Unit> getUnits()
	{
		return m_units;
	}
	
	public int getMovement()
	{
		return m_movement;
	}
	
	public int getTransportCost()
	{
		return m_transportCost;
	}
	
	public PlayerID getOwner()
	{
		return m_owner;
	}
	
	public void addUnit(Unit unit)
	{
		m_units.add(unit);
	}
	
	void removeUnit(Unit unit)
	{
		m_units.remove(unit);
	}
	
	public UnitType getType()
	{
		return m_type;
	}
	
	public int compareTo(Object o)
	{
		if (o == null)
			return -1;
		
		UnitCategory other = (UnitCategory) o;
		
		if (!other.m_owner.equals(this.m_owner))
			return this.m_owner.getName().compareTo(other.m_owner.getName());
		
		int typeCompare = new UnitTypeComparator().compare(this.getType(),
					other.getType());
		if (typeCompare != 0)
			return typeCompare;
		
		if (m_movement != other.m_movement)
			return m_movement - other.m_movement;
		
		if (!Util.equals(this.m_dependents, other.m_dependents))
		{
			return m_dependents.toString().compareTo(other.m_dependents.
						toString());
		}
		
		if (this.m_damaged != other.m_damaged)
		{
			if (m_damaged)
				return 1;
			return -1;
		}
		
		if (this.m_disabled != other.m_disabled)
		{
			if (m_disabled)
				return 1;
			return -1;
		}
		
		return 0;
	}
}
