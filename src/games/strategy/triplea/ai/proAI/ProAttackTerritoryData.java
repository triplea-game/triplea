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
import java.util.List;

public class ProAttackTerritoryData
{
	private Territory territory;
	private List<Unit> maxUnits;
	private List<Unit> units;
	private double TUVSwing;
	private Double attackValue;
	private boolean canHold;
	
	public ProAttackTerritoryData(Territory territory)
	{
		this.territory = territory;
		maxUnits = new ArrayList<Unit>();
		units = new ArrayList<Unit>();
		TUVSwing = 0;
		canHold = false;
	}
	
	public void addUnit(Unit unit)
	{
		this.units.add(unit);
	}
	
	public void addMaxUnits(List<Unit> units)
	{
		this.maxUnits.addAll(units);
	}
	
	public void addMaxUnit(Unit unit)
	{
		this.maxUnits.add(unit);
	}
	
	public void setTerritory(Territory territory)
	{
		this.territory = territory;
	}
	
	public Territory getTerritory()
	{
		return territory;
	}
	
	public void setMaxUnits(List<Unit> units)
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
	
	public void setTUVSwing(double tUVSwing)
	{
		TUVSwing = tUVSwing;
	}
	
	public void setAttackValue(double attackValue)
	{
		this.attackValue = attackValue;
	}
	
	public double getAttackValue()
	{
		return attackValue;
	}
	
	public void setUnits(List<Unit> units)
	{
		this.units = units;
	}
	
	public List<Unit> getUnits()
	{
		return units;
	}
	
	public void setCanHold(boolean canHold)
	{
		this.canHold = canHold;
	}
	
	public boolean isCanHold()
	{
		return canHold;
	}
	
}
