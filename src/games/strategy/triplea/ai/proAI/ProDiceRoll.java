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
package games.strategy.triplea.ai.proAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.LinkedIntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProDiceRoll
{
	
	public static Map<Unit, Tuple<Integer, Integer>> getUnitPowerAndRollsForNormalBattles(final List<Unit> unitsGettingPowerFor, final List<Unit> allFriendlyUnitsAliveOrWaitingToDie,
				final List<Unit> allEnemyUnitsAliveOrWaitingToDie, final boolean defending, final boolean bombing, final PlayerID player, final GameData data, final Territory location,
				final Collection<TerritoryEffect> territoryEffects, final boolean isAmphibiousBattle, final Collection<Unit> amphibiousLandAttackers,
				final Map<Unit, IntegerMap<Unit>> unitPowerSupportMap, final Map<Unit, IntegerMap<Unit>> unitRollsSupportMap)
	{
		final Map<Unit, Tuple<Integer, Integer>> rVal = new HashMap<Unit, Tuple<Integer, Integer>>();
		if (unitsGettingPowerFor == null || unitsGettingPowerFor.isEmpty())
			return rVal;
		
		// get all supports, friendly and enemy
		final Set<List<UnitSupportAttachment>> supportRulesFriendly = new HashSet<List<UnitSupportAttachment>>();
		final IntegerMap<UnitSupportAttachment> supportLeftFriendly = new IntegerMap<UnitSupportAttachment>();
		final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeftFriendly = new HashMap<UnitSupportAttachment, LinkedIntegerMap<Unit>>();
		getSupport(allFriendlyUnitsAliveOrWaitingToDie, supportRulesFriendly, supportLeftFriendly, supportUnitsLeftFriendly, data, defending, true);
		final Set<List<UnitSupportAttachment>> supportRulesEnemy = new HashSet<List<UnitSupportAttachment>>();
		final IntegerMap<UnitSupportAttachment> supportLeftEnemy = new IntegerMap<UnitSupportAttachment>();
		final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeftEnemy = new HashMap<UnitSupportAttachment, LinkedIntegerMap<Unit>>();
		getSupport(allEnemyUnitsAliveOrWaitingToDie, supportRulesEnemy, supportLeftEnemy, supportUnitsLeftEnemy, data, !defending, false);
		final IntegerMap<UnitSupportAttachment> supportLeftFriendlyRolls = new IntegerMap<UnitSupportAttachment>(supportLeftFriendly); // copy for rolls
		final IntegerMap<UnitSupportAttachment> supportLeftEnemyRolls = new IntegerMap<UnitSupportAttachment>(supportLeftEnemy);
		final int diceSides = data.getDiceSides();
		
		for (final Unit current : unitsGettingPowerFor)
		{
			// find our initial strength
			int strength;
			final UnitAttachment ua = UnitAttachment.get(current.getType());
			if (defending)
			{
				strength = ua.getDefense(current.getOwner());
				if (isFirstTurnLimitedRoll(current.getOwner(), data))
					strength = Math.min(1, strength);
				else
					strength += getSupport(current, current.getType(), supportRulesFriendly, supportLeftFriendly, supportUnitsLeftFriendly, unitPowerSupportMap, strength, true, false);
				strength += getSupport(current, current.getType(), supportRulesEnemy, supportLeftEnemy, supportUnitsLeftEnemy, unitPowerSupportMap, strength, true, false);
			}
			else
			{
				strength = ua.getAttack(current.getOwner());
				if (ua.getIsMarine() != 0 && isAmphibiousBattle)
				{
					if (amphibiousLandAttackers.contains(current))
						strength += ua.getIsMarine();
				}
				if (ua.getIsSea() && isAmphibiousBattle && Matches.TerritoryIsLand.match(location))
				{
					strength = ua.getBombard(current.getOwner()); // change the strength to be bombard, not attack/defense, because this is a bombarding naval unit
				}
				strength += getSupport(current, current.getType(), supportRulesFriendly, supportLeftFriendly, supportUnitsLeftFriendly, unitPowerSupportMap, strength, true, false);
				strength += getSupport(current, current.getType(), supportRulesEnemy, supportLeftEnemy, supportUnitsLeftEnemy, unitPowerSupportMap, strength, true, false);
			}
			strength += TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
			strength = Math.min(Math.max(strength, 0), diceSides);
			
			// now determine our rolls
			int rolls;
			if (!bombing && strength == 0)
				rolls = 0;
			else
			{
				if (defending)
					rolls = ua.getDefenseRolls(current.getOwner());
				else
					rolls = ua.getAttackRolls(current.getOwner());
				rolls += getSupport(current, current.getType(), supportRulesFriendly, supportLeftFriendlyRolls, supportUnitsLeftFriendly, unitRollsSupportMap, strength, false, true);
				rolls += getSupport(current, current.getType(), supportRulesEnemy, supportLeftEnemyRolls, supportUnitsLeftEnemy, unitRollsSupportMap, strength, false, true);
				rolls = Math.max(0, rolls);
				if (rolls == 0)
					strength = 0;
			}
			rVal.put(current, new Tuple<Integer, Integer>(strength, rolls));
		}
		return rVal;
	}
	
	public static void getSupport(final List<Unit> unitsGivingTheSupport, final Set<List<UnitSupportAttachment>> supportsAvailable, final IntegerMap<UnitSupportAttachment> supportLeft,
				final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeft, final GameData data, final boolean defence, final boolean allies)
	{
		if (unitsGivingTheSupport == null || unitsGivingTheSupport.isEmpty())
			return;
		for (final UnitSupportAttachment rule : UnitSupportAttachment.get(data))
		{
			if (rule.getPlayers().isEmpty())
				continue;
			if (!((defence && rule.getDefence()) || (!defence && rule.getOffence())))
				continue;
			if (!((allies && rule.getAllied()) || (!allies && rule.getEnemy())))
				continue;
			final CompositeMatchAnd<Unit> canSupport = new CompositeMatchAnd<Unit>(Matches.unitIsOfType((UnitType) rule.getAttachedTo()), Matches.unitOwnedBy(rule.getPlayers()));
			final List<Unit> supporters = Match.getMatches(unitsGivingTheSupport, canSupport);
			int numSupport = supporters.size();
			if (numSupport <= 0)
				continue;
			if (rule.getImpArtTech())
				numSupport += Match.getMatches(supporters, Matches.unitOwnerHasImprovedArtillerySupportTech()).size();
			final String bonusType = rule.getBonusType();
			supportLeft.put(rule, numSupport * rule.getNumber());
			// TODO: consider rule.getImpArtTech()
			supportUnitsLeft.put(rule, new LinkedIntegerMap<Unit>(supporters, rule.getNumber()));
			final Iterator<List<UnitSupportAttachment>> iter2 = supportsAvailable.iterator();
			List<UnitSupportAttachment> ruleType = null;
			boolean found = false;
			while (iter2.hasNext())
			{
				ruleType = iter2.next();
				if (ruleType.get(0).getBonusType().equals(bonusType))
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				ruleType = new ArrayList<UnitSupportAttachment>();
				supportsAvailable.add(ruleType);
			}
			if (ruleType != null)
				ruleType.add(rule);
		}
		sortSupportRules(supportsAvailable);
	}
	
	public static int getSupport(final Unit unit, final UnitType type, final Set<List<UnitSupportAttachment>> supportsAvailable, final IntegerMap<UnitSupportAttachment> supportLeft,
				final Map<UnitSupportAttachment, LinkedIntegerMap<Unit>> supportUnitsLeft, final Map<Unit, IntegerMap<Unit>> unitSupportMap, final int unitStrength, final boolean strength,
				final boolean rolls)
	{
		int givenSupport = 0;
		for (final List<UnitSupportAttachment> bonusType : supportsAvailable)
		{
			for (final UnitSupportAttachment rule : bonusType)
			{
				if (!((strength && rule.getStrength()) || (rolls && rule.getRoll())))
					continue;
				final HashSet<UnitType> types = rule.getUnitType();
				if (types != null && types.contains(type) && supportLeft.getInt(rule) > 0)
				{
					givenSupport += rule.getBonus();
					supportLeft.add(rule, -1);
					if (supportUnitsLeft.get(rule).keySet().iterator().hasNext()) // shouldn't need this check but just in case
					{
						final Unit u = supportUnitsLeft.get(rule).keySet().iterator().next();
						supportUnitsLeft.get(rule).add(u, -1);
						if (supportUnitsLeft.get(rule).getInt(u) <= 0)
							supportUnitsLeft.get(rule).removeKey(u);
						if (unitSupportMap.containsKey(u))
							unitSupportMap.get(u).add(unit, rule.getBonus());
						else
							unitSupportMap.put(u, new IntegerMap<Unit>(Collections.singletonList(unit), rule.getBonus()));
					}
					break;
				}
			}
		}
		return givenSupport;
	}
	
	private static void sortSupportRules(final Set<List<UnitSupportAttachment>> support)
	{
		// first, sort the lists inside each set
		final Comparator<UnitSupportAttachment> compList = new Comparator<UnitSupportAttachment>()
		{
			public int compare(final UnitSupportAttachment u1, final UnitSupportAttachment u2)
			{
				// we want to apply the biggest bonus first
				final Integer v1 = Integer.valueOf(Math.abs(u1.getBonus()));
				final Integer v2 = Integer.valueOf(Math.abs(u2.getBonus()));
				int compareTo = v2.compareTo(v1);
				if (compareTo != 0)
					return compareTo;
				
				// if the bonuses are the same, we want to make sure any support which only supports 1 single unittype goes first
				// the reason being that we could have Support1 which supports both infantry and mech infantry, and Support2 which only supports mech infantry
				// if the Support1 goes first, and the mech infantry is first in the unit list (highly probable), then Support1 will end up using all of itself up on the mech infantry
				// then when the Support2 comes up, all the mech infantry are used up, and it does nothing.
				// instead, we want Support2 to come first, support all mech infantry that it can, then have Support1 come in and support whatever is left, that way no support is wasted
				// TODO: this breaks down completely if we have Support1 having a higher bonus than Support2, because it will come first. It should come first, unless we would have support wasted otherwise. This ends up being a pretty tricky math puzzle.
				final HashSet<UnitType> types1 = u1.getUnitType();
				final HashSet<UnitType> types2 = u2.getUnitType();
				final Integer s1 = types1 == null ? 0 : types1.size();
				final Integer s2 = types2 == null ? 0 : types2.size();
				compareTo = s1.compareTo(s2);
				if (compareTo != 0)
					return compareTo;
				
				// Make sure stronger supports are ordered first
				final UnitType unitType1 = (UnitType) u1.getAttachedTo();
				final UnitType unitType2 = (UnitType) u2.getAttachedTo();
				final UnitAttachment ua1 = UnitAttachment.get(unitType1);
				final UnitAttachment ua2 = UnitAttachment.get(unitType2);
				final PlayerID player = u1.getPlayers().get(0);
				Integer unitPower1 = ua1.getAttackRolls(player) * ua1.getAttack(player);
				Integer unitPower2 = ua2.getAttackRolls(player) * ua2.getAttack(player);
				if (u1.getDefence())
				{
					unitPower1 = ua1.getDefenseRolls(player) * ua1.getDefense(player);
					unitPower2 = ua2.getDefenseRolls(player) * ua2.getDefense(player);
				}
				return unitPower2.compareTo(unitPower1);
			}
		};
		final Iterator<List<UnitSupportAttachment>> iter = support.iterator();
		while (iter.hasNext())
		{
			Collections.sort(iter.next(), compList);
		}
	}
	
	public static boolean isFirstTurnLimitedRoll(final PlayerID player, final GameData data)
	{
		// If player is null, Round > 1, or player has negate rule set: return false
		if (player.isNull() || data.getSequence().getRound() != 1 || isNegateDominatingFirstRoundAttack(player))
			return false;
		return isDominatingFirstRoundAttack(data.getSequence().getStep().getPlayerID());
	}
	
	private static boolean isDominatingFirstRoundAttack(final PlayerID player)
	{
		if (player == null)
		{
			return false;
		}
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (ra == null)
			return false;
		return ra.getDominatingFirstRoundAttack();
	}
	
	private static boolean isNegateDominatingFirstRoundAttack(final PlayerID player)
	{
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if (ra == null)
			return false;
		return ra.getNegateDominatingFirstRoundAttack();
	}
	
}
