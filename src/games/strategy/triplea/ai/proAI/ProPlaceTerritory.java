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

public class ProPlaceTerritory
{
	private Territory territory;
	private List<Unit> defendingUnits;
	private ProBattleResultData minBattleResult;
	private double defenseValue;
	private double strategicValue;
	private List<Unit> placeUnits;
	
	public ProPlaceTerritory(final Territory territory)
	{
		this.territory = territory;
		defendingUnits = new ArrayList<Unit>();
		minBattleResult = new ProBattleResultData();
		defenseValue = 0;
		strategicValue = 0;
		placeUnits = new ArrayList<Unit>();
	}
	
	@Override
	public String toString()
	{
		return territory.toString();
	}
	
	public List<Unit> getAllDefenders()
	{
		final List<Unit> defenders = new ArrayList<Unit>(defendingUnits);
		defenders.addAll(placeUnits);
		return defenders;
	}
	
	public Territory getTerritory()
	{
		return territory;
	}
	
	public void setTerritory(final Territory territory)
	{
		this.territory = territory;
	}
	
	public List<Unit> getDefendingUnits()
	{
		return defendingUnits;
	}
	
	public void setDefendingUnits(final List<Unit> defendingUnits)
	{
		this.defendingUnits = defendingUnits;
	}
	
	public double getDefenseValue()
	{
		return defenseValue;
	}
	
	public void setDefenseValue(final double defenseValue)
	{
		this.defenseValue = defenseValue;
	}
	
	public double getStrategicValue()
	{
		return strategicValue;
	}
	
	public void setStrategicValue(final double strategicValue)
	{
		this.strategicValue = strategicValue;
	}
	
	public List<Unit> getPlaceUnits()
	{
		return placeUnits;
	}
	
	public void setPlaceUnits(final List<Unit> placeUnits)
	{
		this.placeUnits = placeUnits;
	}
	
	public void setMinBattleResult(final ProBattleResultData minBattleResult)
	{
		this.minBattleResult = minBattleResult;
	}
	
	public ProBattleResultData getMinBattleResult()
	{
		return minBattleResult;
	}
	
}
