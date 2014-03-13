package games.strategy.triplea.ai.proAI;

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
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProAttackTerritoryData
{
	private Territory territory;
	private List<Unit> maxUnits;
	private List<Unit> units;
	private double TUVSwing;
	private Double attackValue;
	private boolean canHold;
	
	// Amphib variables
	private List<Unit> maxAmphibUnits;
	private boolean needAmphibUnits;
	private Map<Unit, List<Unit>> amphibAttackMap;
	private List<Unit> navalAttackTransports;
	
	public ProAttackTerritoryData(final Territory territory)
	{
		this.territory = territory;
		maxUnits = new ArrayList<Unit>();
		units = new ArrayList<Unit>();
		TUVSwing = 0;
		canHold = false;
		maxAmphibUnits = new ArrayList<Unit>();
		needAmphibUnits = false;
		amphibAttackMap = new HashMap<Unit, List<Unit>>();
		navalAttackTransports = new ArrayList<Unit>();
	}
	
	public void addUnit(final Unit unit)
	{
		this.units.add(unit);
	}
	
	public void addUnits(final List<Unit> units)
	{
		this.units.addAll(units);
	}
	
	public void addMaxUnits(final List<Unit> units)
	{
		this.maxUnits.addAll(units);
	}
	
	public void addMaxAmphibUnits(final List<Unit> amphibUnits)
	{
		this.maxAmphibUnits.addAll(amphibUnits);
	}
	
	public void addMaxUnit(final Unit unit)
	{
		this.maxUnits.add(unit);
	}
	
	public void setTerritory(final Territory territory)
	{
		this.territory = territory;
	}
	
	public Territory getTerritory()
	{
		return territory;
	}
	
	public void setMaxUnits(final List<Unit> units)
	{
		this.maxUnits = units;
	}
	
	public List<Unit> getMaxUnits()
	{
		return maxUnits;
	}
	
	public double getTUVSwing()
	{
		return TUVSwing;
	}
	
	public void setTUVSwing(final double tUVSwing)
	{
		TUVSwing = tUVSwing;
	}
	
	public void setAttackValue(final double attackValue)
	{
		this.attackValue = attackValue;
	}
	
	public double getAttackValue()
	{
		return attackValue;
	}
	
	public void setUnits(final List<Unit> units)
	{
		this.units = units;
	}
	
	public List<Unit> getUnits()
	{
		return units;
	}
	
	public void setCanHold(final boolean canHold)
	{
		this.canHold = canHold;
	}
	
	public boolean isCanHold()
	{
		return canHold;
	}
	
	public void setMaxAmphibUnits(final List<Unit> maxAmphibUnits)
	{
		this.maxAmphibUnits = maxAmphibUnits;
	}
	
	public List<Unit> getMaxAmphibUnits()
	{
		return maxAmphibUnits;
	}
	
	public void setNeedAmphibUnits(final boolean needAmphibUnits)
	{
		this.needAmphibUnits = needAmphibUnits;
	}
	
	public boolean isNeedAmphibUnits()
	{
		return needAmphibUnits;
	}
	
	public Map<Unit, List<Unit>> getAmphibAttackMap()
	{
		return amphibAttackMap;
	}
	
	public void setAmphibAttackMap(final Map<Unit, List<Unit>> amphibAttackMap)
	{
		this.amphibAttackMap = amphibAttackMap;
	}
	
	public void putAmphibAttackMap(final Unit transport, final List<Unit> amphibUnits)
	{
		this.amphibAttackMap.put(transport, amphibUnits);
	}
	
	public List<Unit> getNavalAttackTransports()
	{
		return navalAttackTransports;
	}
	
	public void setNavalAttackTransports(final List<Unit> navalAttackTransports)
	{
		this.navalAttackTransports = navalAttackTransports;
	}
	
	public void addNavelAttackTransport(final Unit transport)
	{
		this.navalAttackTransports.add(transport);
	}
	
}
