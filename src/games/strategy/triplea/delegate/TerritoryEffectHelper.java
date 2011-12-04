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
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.TerritoryEffectAttachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

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
		final TerritoryAttachment ta = TerritoryAttachment.get(location);
		if (ta != null)
			return TerritoryAttachment.get(location).getTerritoryEffect();
		else
			return new ArrayList<TerritoryEffect>();
	}
	
	public static int getTerritoryCombatBonus(final UnitType type, final Territory location, final boolean defending)
	{
		if (location == null || type == null)
			return 0;
		int combatBonus = 0;
		final Iterator<TerritoryEffect> effectsIter = getEffects(location).iterator();
		while (effectsIter.hasNext())
		{
			combatBonus += TerritoryEffectAttachment.get(effectsIter.next()).getCombatEffect(type, defending);
		}
		return combatBonus;
	}
}
