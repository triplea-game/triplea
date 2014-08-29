package games.strategy.triplea.ai.proAI.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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

/**
 * Pro AI transport utilities.
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProTransportUtils
{
	private final ProAI ai;
	private final ProUtils utils;
	
	public ProTransportUtils(final ProAI ai, final ProUtils utils)
	{
		this.ai = ai;
		this.utils = utils;
	}
	
	public int findNumUnitsThatCanBeTransported(final PlayerID player, final Territory t)
	{
		final GameData data = ai.getGameData();
		
		int numUnitsToLoad = 0;
		final Set<Territory> neighbors = data.getMap().getNeighbors(t, Matches.TerritoryIsLand);
		for (final Territory neighbor : neighbors)
		{
			numUnitsToLoad += Match.getMatches(neighbor.getUnits().getUnits(), ProMatches.unitIsOwnedTransportableUnit(player)).size();
		}
		return numUnitsToLoad;
	}
	
	public List<Unit> getUnitsToTransportFromTerritories(final PlayerID player, final Unit transport, final Set<Territory> territoriesToLoadFrom, final List<Unit> unitsToIgnore)
	{
		final List<Unit> attackers = new ArrayList<Unit>();
		
		// Get units if transport already loaded
		if (TransportTracker.isTransporting(transport))
		{
			attackers.addAll(TransportTracker.transporting(transport));
		}
		else
		{
			// Get all units that can be transported
			final List<Unit> units = new ArrayList<Unit>();
			for (final Territory loadFrom : territoriesToLoadFrom)
			{
				units.addAll(loadFrom.getUnits().getMatches(ProMatches.unitIsOwnedTransportableUnitAndCanBeLoaded(player, true)));
			}
			units.removeAll(unitsToIgnore);
			
			// Sort units by attack
			Collections.sort(units, new Comparator<Unit>()
			{
				public int compare(final Unit o1, final Unit o2)
				{
					int attack1 = UnitAttachment.get(o1.getType()).getAttack(player);
					if (UnitAttachment.get(o1.getType()).getArtillery())
						attack1++;
					int attack2 = UnitAttachment.get(o2.getType()).getAttack(player);
					if (UnitAttachment.get(o2.getType()).getArtillery())
						attack2++;
					return attack2 - attack1;
				}
			});
			
			// Get best attackers that can be loaded
			final int capacity = UnitAttachment.get(transport.getType()).getTransportCapacity();
			int capacityCount = 0;
			for (final Unit unit : units)
			{
				final int cost = UnitAttachment.get(unit.getType()).getTransportCost();
				if (cost <= (capacity - capacityCount))
				{
					attackers.add(unit);
					capacityCount += cost;
					if (capacityCount >= capacity)
						break;
				}
			}
		}
		
		return attackers;
	}
	
}
