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
/*
 * TerritoryEffectHelper.java
 * 
 * Created on November 2, 2001, 12:26 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TerritoryEffectAttachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Placeholder for all calculations to do with TerritoryEffects
 * 
 * @author Edwin van der Wal
 * @version 1.0
 */
public class TerritoryEffectHelper
{
	public static Collection<TerritoryEffect> getEffects(final Territory location)
	{
		// if (location == null)
		// return new ArrayList<TerritoryEffect>();
		final TerritoryAttachment ta = TerritoryAttachment.get(location);
		if (ta != null)
			return TerritoryAttachment.get(location).getTerritoryEffect();
		else
			return new ArrayList<TerritoryEffect>();
	}
	
	public static int getTerritoryCombatBonus(final UnitType type, final Collection<TerritoryEffect> effects, final boolean defending)
	{
		if (type == null || effects == null || effects.isEmpty())
			return 0;
		int combatBonus = 0;
		for (final TerritoryEffect effect : effects)
		{
			combatBonus += TerritoryEffectAttachment.get(effect).getCombatEffect(type, defending);
		}
		return combatBonus;
	}
	
	public static boolean unitLoosesBlitz(final Unit unit, final Territory location)
	{
		return unitTypeLoosesBlitz(unit.getType(), location);
	}
	
	public static boolean unitTypeLoosesBlitz(final UnitType type, final Territory location)
	{
		if (location == null || type == null)
			throw new IllegalStateException("Location and UnitType can not be null");
		for (final TerritoryEffect effect : getEffects(location))
		{
			if (TerritoryEffectAttachment.get(effect).getNoBlitz().contains(type))
				return true;
		}
		return false;
	}
	
	public static boolean unitKeepsBlitz(final Unit unit, final Territory location)
	{
		return unitTypeKeepsBlitz(unit.getType(), location);
	}
	
	public static boolean unitTypeKeepsBlitz(final UnitType type, final Territory location)
	{
		return !unitTypeLoosesBlitz(type, location);
	}
	
	public static Set<UnitType> getUnitTypesThatLostBlitz(final Collection<Territory> steps)
	{
		final Set<UnitType> rVal = new HashSet<UnitType>();
		for (final Territory location : steps)
		{
			for (final TerritoryEffect effect : getEffects(location))
			{
				rVal.addAll(TerritoryEffectAttachment.get(effect).getNoBlitz());
			}
		}
		return rVal;
	}
	
	public static Set<UnitType> getUnitTypesForUnitsNotAllowedIntoTerritory(final Territory location)
	{
		final Set<UnitType> rVal = new HashSet<UnitType>();
		for (final TerritoryEffect effect : getEffects(location))
		{
			rVal.addAll(TerritoryEffectAttachment.get(effect).getUnitsNotAllowed());
		}
		return rVal;
	}
	
	public static Set<UnitType> getUnitTypesForUnitsNotAllowedIntoTerritory(final Collection<Territory> steps)
	{
		final Set<UnitType> rVal = new HashSet<UnitType>();
		for (final Territory location : steps)
		{
			rVal.addAll(getUnitTypesForUnitsNotAllowedIntoTerritory(location));
		}
		return rVal;
	}
}
