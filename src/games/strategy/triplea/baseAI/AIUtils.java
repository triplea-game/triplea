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
package games.strategy.triplea.baseAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.Collection;
import java.util.Comparator;

/**
 * 
 * Handy utility methods for the writers of an AI.
 * 
 * @author sgb
 */
public class AIUtils
{
	/**
	 * How many PU's does it cost the given player to produce the given unit type.
	 * <p>
	 * 
	 * If the player cannot produce the given unit, return Integer.MAX_VALUE
	 * <p>
	 */
	public static int getCost(final UnitType unitType, final PlayerID player, final GameData data)
	{
		if (unitType == null)
			throw new IllegalArgumentException("null unit type");
		if (player == null)
			throw new IllegalArgumentException("null player id");
		final Resource PUs = data.getResourceList().getResource(Constants.PUS);
		final ProductionRule rule = getProductionRule(unitType, player, data);
		if (rule == null)
		{
			return Integer.MAX_VALUE;
		}
		else
		{
			return rule.getCosts().getInt(PUs);
		}
	}
	
	/**
	 * 
	 * @return a comparator that sorts cheaper units before expensive ones
	 */
	public static Comparator<Unit> getCostComparator()
	{
		return new Comparator<Unit>()
		{
			public int compare(final Unit o1, final Unit o2)
			{
				return getCost(o1.getType(), o1.getOwner(), o1.getData()) - getCost(o2.getType(), o2.getOwner(), o2.getData());
			}
		};
	}
	
	/**
	 * Get the production rule for the given player, for the given unit type.
	 * <p>
	 * If no such rule can be found, then return null.
	 */
	public static ProductionRule getProductionRule(final UnitType unitType, final PlayerID player, final GameData data)
	{
		if (unitType == null)
			throw new IllegalArgumentException("null unit type");
		if (player == null)
			throw new IllegalArgumentException("null player id");
		final ProductionFrontier frontier = player.getProductionFrontier();
		for (final ProductionRule rule : frontier)
		{
			if (rule.getResults().getInt(unitType) == 1)
			{
				return rule;
			}
		}
		return null;
	}
	
	/**
	 * Get a quick and dirty estimate of the strength of some units in a battle.
	 * <p>
	 * 
	 * @param units
	 *            - the units to measure
	 * @param attacking
	 *            - are the units on attack or defense
	 * @param sea
	 *            - calculate the strength of the units in a sea or land battle?
	 * @return
	 */
	public static float strength(final Collection<Unit> units, final boolean attacking, final boolean sea)
	{
		int strength = 0;
		for (final Unit u : units)
		{
			final UnitAttachment unitAttatchment = UnitAttachment.get(u.getType());
			if (unitAttatchment.getIsFactory() || unitAttatchment.getIsInfrastructure())
			{
				// nothing
			}
			else if (unitAttatchment.getIsSea() == sea)
			{
				// 2 points since we can absorb a hit
				strength += 2;
				// two hit
				if (unitAttatchment.isTwoHit())
					strength += 1.5;
				// the number of pips on the dice
				if (attacking)
					strength += unitAttatchment.getAttack(u.getOwner());
				else
					strength += unitAttatchment.getDefense(u.getOwner());
				if (attacking)
				{
					// a unit with attack of 0 isnt worth much
					// we dont want transports to try and gang up on subs
					if (unitAttatchment.getAttack(u.getOwner()) == 0)
					{
						strength -= 1.2;
					}
				}
			}
		}
		if (attacking)
		{
			final int art = Match.countMatches(units, Matches.UnitIsArtillery);
			final int artSupport = Match.countMatches(units, Matches.UnitIsArtillerySupportable);
			strength += Math.min(art, artSupport);
		}
		return strength;
	}
}
