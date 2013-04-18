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
package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Used to store information about a dice roll.
 * # of rolls at 5, at 4, etc.
 * <p>
 * 
 * Externalizable so we can efficiently write out our dice as ints rather than as full objects.
 */
public class DiceRoll implements Externalizable
{
	private static final long serialVersionUID = -1167204061937566271L;
	private List<Die> m_rolls;
	// this does not need to match the Die with isHit true
	// since for low luck we get many hits with few dice
	private int m_hits;
	
	public static void sortAAHighToLow(final List<Unit> units, final GameData data, final boolean defending)
	{
		final Comparator<Unit> comparator = new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				final Tuple<Integer, Integer> tuple1 = getAAattackAndMaxDiceSides(Collections.singleton(u1), data, defending);
				final Tuple<Integer, Integer> tuple2 = getAAattackAndMaxDiceSides(Collections.singleton(u2), data, defending);
				if (tuple1.getFirst() == 0)
				{
					if (tuple2.getFirst() == 0)
						return 0;
					return 1;
				}
				else if (tuple2.getFirst() == 0)
					return -1;
				final float value1 = ((float) tuple1.getFirst()) / ((float) tuple1.getSecond());
				final float value2 = ((float) tuple2.getFirst()) / ((float) tuple2.getSecond());
				if (value1 < value2)
					return 1;
				else if (value1 > value2)
					return -1;
				return 0;
			}
		};
		Collections.sort(units, comparator);
	}
	
	/**
	 * Returns a Tuple with 2 values, the first is the max attack, the second is the max dice sides for the AA unit with that attack value
	 */
	public static Tuple<Integer, Integer> getAAattackAndMaxDiceSides(final Collection<Unit> defendingEnemyAA, final GameData data, final boolean defending)
	{
		int highestAttack = 0;
		final int diceSize = data.getDiceSides();
		int chosenDiceSize = diceSize;
		for (final Unit u : defendingEnemyAA)
		{
			final UnitAttachment ua = UnitAttachment.get(u.getType());
			int uaDiceSides = defending ? ua.getAttackAAmaxDieSides() : ua.getOffensiveAttackAAmaxDieSides();
			if (uaDiceSides < 1)
				uaDiceSides = diceSize;
			int attack = defending ? ua.getAttackAA(u.getOwner()) : ua.getOffensiveAttackAA(u.getOwner());
			if (attack > uaDiceSides)
				attack = uaDiceSides;
			if ((((float) attack) / ((float) uaDiceSides)) > (((float) highestAttack) / ((float) chosenDiceSize)))
			{
				highestAttack = attack;
				chosenDiceSize = uaDiceSides;
			}
		}
		if (highestAttack > chosenDiceSize / 2 && chosenDiceSize > 1)
			highestAttack = chosenDiceSize / 2; // sadly the whole low luck section falls apart if AA are hitting at greater than half the value of dice, and I don't feel like rewriting it
		return new Tuple<Integer, Integer>(highestAttack, chosenDiceSize);
	}
	
	public static int getTotalAAattacks(final Collection<Unit> defendingEnemyAA, final Collection<Unit> validAttackingUnitsForThisRoll, final GameData data)
	{
		if (defendingEnemyAA.isEmpty() || validAttackingUnitsForThisRoll.isEmpty())
			return 0;
		int totalAAattacksNormal = 0;
		int totalAAattacksSurplus = 0;
		for (final Unit aa : defendingEnemyAA)
		{
			final UnitAttachment ua = UnitAttachment.get(aa.getType());
			if (ua.getMaxAAattacks() == -1)
				totalAAattacksNormal = validAttackingUnitsForThisRoll.size();
			else
			{
				if (ua.getMayOverStackAA())
					totalAAattacksSurplus += ua.getMaxAAattacks();
				else
					totalAAattacksNormal += ua.getMaxAAattacks();
			}
		}
		totalAAattacksNormal = Math.min(totalAAattacksNormal, validAttackingUnitsForThisRoll.size());
		return totalAAattacksNormal + totalAAattacksSurplus;
	}
	
	public static DiceRoll rollAA(final Collection<Unit> validAttackingUnitsForThisRoll, final Collection<Unit> defendingAAForThisRoll, final IDelegateBridge bridge, final Territory location,
				final boolean defending)
	{
		final List<Unit> defendingAA = Match.getMatches(defendingAAForThisRoll,
					(defending ? Matches.UnitAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero : Matches.UnitOffensiveAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero));
		if (defendingAA.isEmpty())
			return new DiceRoll(new ArrayList<Die>(0), 0);
		final GameData data = bridge.getData();
		final int totalAAattacksTotal = getTotalAAattacks(defendingAA, validAttackingUnitsForThisRoll, data);
		if (totalAAattacksTotal <= 0)
			return new DiceRoll(new ArrayList<Die>(0), 0);
		// determine dicesides for everyone (we are not going to consider the possibility of different dicesides within the same typeAA)
		final Tuple<Integer, Integer> attackThenDiceSidesForAll = getAAattackAndMaxDiceSides(defendingAA, data, defending);
		// final int highestAttackPower = attackThenDiceSidesForAll.getFirst();
		final int chosenDiceSizeForAll = attackThenDiceSidesForAll.getSecond();
		int hits = 0;
		final List<Die> sortedDice = new ArrayList<Die>();
		final String typeAA = UnitAttachment.get(defendingAA.get(0).getType()).getTypeAA();
		
		// LOW LUCK
		if (games.strategy.triplea.Properties.getLow_Luck(data) || games.strategy.triplea.Properties.getLL_AA_ONLY(data))
		{
			final String annotation = "Roll " + typeAA + " in " + location.getName();
			final Triple<Integer, Integer, Boolean> triple = getTotalAAPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(null, null, defending, defendingAA,
						validAttackingUnitsForThisRoll, data, false);
			final int totalPower = triple.getFirst();
			hits += getLowLuckHits(bridge, sortedDice, totalPower, chosenDiceSizeForAll, defendingAA.get(0).getOwner(), annotation);
			/* Turns out all this junk below is not actually needed, because in this method we are only determining the number of hits, and any die rolls we need to do. 
			final boolean allSameAttackPower = triple.getThird();
			// if we have a group of 6 fighters and 2 bombers, and dicesides is 6, and attack was 1, then we would want 1 fighter to die for sure. this is what groupsize is for.
			// if the attack is greater than 1 though, and all use the same attack power, then the group size can be smaller (ie: attack is 2, and we have 3 fighters and 2 bombers, we would want 1 fighter to die for sure).
			final int groupSize;
			if (allSameAttackPower)
			{
				groupSize = chosenDiceSizeForAll / highestAttackPower;
			}
			else
			{
				groupSize = chosenDiceSizeForAll;
			}
			if (Properties.getChoose_AA_Casualties(data))
			{
				hits += getLowLuckHits(bridge, sortedDice, power, chosenDiceSizeForAll, annotation, totalAAattacksTotal);
			}
			else
			{
				final Tuple<List<Unit>, List<Unit>> airSplit = BattleCalculator.categorizeLowLuckAirUnits(validAttackingUnitsForThisRoll, location, chosenDiceSizeForAll, groupSize);
				// this will not roll any dice, since the first group is
				// a multiple of 3 or 6
				final int firstGroupSize = airSplit.getFirst().size();
				hits += getLowLuckHits(bridge, sortedDice, power, chosenDiceSizeForAll, annotation, Math.min(firstGroupSize, totalAAattacksTotal));
				totalAAattacksTotal = Math.max(0, totalAAattacksTotal - firstGroupSize);
				// this will roll dice, unless it is empty
				final int secondGroupSize = airSplit.getSecond().size();
				hits += getLowLuckHits(bridge, sortedDice, power, chosenDiceSizeForAll, annotation, Math.min(secondGroupSize, totalAAattacksTotal));
				totalAAattacksTotal = Math.max(0, totalAAattacksTotal - secondGroupSize);
			}*/
		}
		else
		{
			final String annotation = "Roll " + typeAA + " in " + location.getName();
			final int[] dice = bridge.getRandom(chosenDiceSizeForAll, totalAAattacksTotal, defendingAA.get(0).getOwner(), DiceType.COMBAT, annotation);
			hits += getTotalAAPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(dice, sortedDice, defending, defendingAA, validAttackingUnitsForThisRoll, data, true).getSecond();
		}
		final DiceRoll roll = new DiceRoll(sortedDice, hits);
		final String annotation = typeAA + " fire in " + location + " : " + MyFormatter.asDice(roll);
		bridge.getHistoryWriter().addChildToEvent(annotation, roll);
		return roll;
	}
	
	/**
	 * Basically I wanted 1 single method for both Low Luck and Dice, because if we have 2 methods then there is a chance they will go out of sync.
	 * 
	 * @param dice
	 *            = Rolled Dice numbers from bridge. Can be null if we do not want to return hits or fill the sortedDice
	 * @param sortedDice
	 *            List of dice we are recording. Can be null if we do not want to return hits or fill the sortedDice
	 * @param defendingAA
	 * @param validAttackingUnitsForThisRoll
	 * @param data
	 * @param fillInSortedDiceAndRecordHits
	 * @return an object containing 3 things: first is the total power of the defendingAA who will be rolling, second is number of hits,
	 *         third is true/false are all rolls using the same hitAt (example: if all the rolls are at 1, we would return true, but if one roll is at 1 and another roll is at 2, then we return false)
	 */
	public static Triple<Integer, Integer, Boolean> getTotalAAPowerThenHitsAndFillSortedDiceThenIfAllUseSameAttack(final int[] dice, final List<Die> sortedDice, final boolean defending,
				final Collection<Unit> defendingAAForThisRoll, final Collection<Unit> validAttackingUnitsForThisRoll, final GameData data, final boolean fillInSortedDiceAndRecordHits)
	{
		final List<Unit> defendingAA = Match.getMatches(defendingAAForThisRoll,
					(defending ? Matches.UnitAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero : Matches.UnitOffensiveAttackAAisGreaterThanZeroAndMaxAAattacksIsNotZero));
		if (defendingAA.size() <= 0)
			return new Triple<Integer, Integer, Boolean>(0, 0, false);
		sortAAHighToLow(defendingAA, data, defending); // we want to make sure the higher powers fire
		
		// this is confusing, but what we want to do is the following:
		// any aa that are NOT infinite attacks, and NOT overstack, will fire first individually ((because their power/dicesides might be different [example: radar tech on a german aa gun, in the same territory as an italian aagun without radar, neither is infinite])
		// all aa that have "infinite attacks" will have the one with the highest power/dicesides of them all, fire at whatever aa units have not yet been fired at
		// HOWEVER, if the non-infinite attackers are less powerful than the infinite attacker, then the non-infinite will not fire, and the infinite one will do all the attacks for both groups.
		// the total number of shots from these first 2 groups can not exceed the number of air units being shot at
		// last, any aa that can overstack will fire after, individually
		// (an aa guns that is both infinite, and overstacks, ignores the overstack part because that totally doesn't make any sense)
		
		// set up all 3 groups of aa guns
		final List<Unit> normalNonInfiniteAA = new ArrayList<Unit>(defendingAA);
		final List<Unit> infiniteAA = Match.getMatches(defendingAA, Matches.UnitMaxAAattacksIsInfinite);
		final List<Unit> overstackAA = Match.getMatches(defendingAA, Matches.UnitMayOverStackAA);
		overstackAA.removeAll(infiniteAA);
		normalNonInfiniteAA.removeAll(infiniteAA);
		normalNonInfiniteAA.removeAll(overstackAA);
		// determine maximum total attacks
		final int totalAAattacksTotal = getTotalAAattacks(defendingAA, validAttackingUnitsForThisRoll, data);
		// determine individual totals
		final int normalNonInfiniteAAtotalAAattacks = getTotalAAattacks(normalNonInfiniteAA, validAttackingUnitsForThisRoll, data);
		final int infiniteAAtotalAAattacks = Math.min((validAttackingUnitsForThisRoll.size() - normalNonInfiniteAAtotalAAattacks), getTotalAAattacks(infiniteAA, validAttackingUnitsForThisRoll, data));
		final int overstackAAtotalAAattacks = getTotalAAattacks(overstackAA, validAttackingUnitsForThisRoll, data);
		if (totalAAattacksTotal != (normalNonInfiniteAAtotalAAattacks + infiniteAAtotalAAattacks + overstackAAtotalAAattacks))
			throw new IllegalStateException("Total attacks should be: " + totalAAattacksTotal + " but instead is: "
						+ (normalNonInfiniteAAtotalAAattacks + infiniteAAtotalAAattacks + overstackAAtotalAAattacks));
		// determine dicesides for everyone (we are not going to consider the possibility of different dicesides within the same typeAA)
		// final Tuple<Integer, Integer> attackThenDiceSidesForAll = getAAattackAndMaxDiceSides(defendingAA, data);
		// final int chosenDiceSizeForAll = attackThenDiceSidesForAll.getSecond();
		
		// determine highest attack for infinite group
		final Tuple<Integer, Integer> attackThenDiceSidesForInfinite = getAAattackAndMaxDiceSides(infiniteAA, data, defending);
		final int hitAtForInfinite = attackThenDiceSidesForInfinite.getFirst(); // not zero based
		// final int powerForInfinite = highestAttackForInfinite; // not zero based
		
		// if we are low luck, we only want to know the power and total attacks, while if we are dice we will be filling the sorted dice
		final boolean recordSortedDice = fillInSortedDiceAndRecordHits && dice != null && dice.length > 0 && sortedDice != null;
		int totalPower = 0;
		int hits = 0;
		int i = 0;
		final Set<Integer> rolledAt = new HashSet<Integer>();
		// non-infinite, non-overstack aa
		int runningMaximum = normalNonInfiniteAAtotalAAattacks;
		final Iterator<Unit> normalAAiter = normalNonInfiniteAA.iterator();
		while (i < runningMaximum && normalAAiter.hasNext())
		{
			final Unit aaGun = normalAAiter.next();
			int numAttacks = UnitAttachment.get(aaGun.getType()).getMaxAAattacks(); // should be > 0 at this point
			final int hitAt = getAAattackAndMaxDiceSides(Collections.singleton(aaGun), data, defending).getFirst();
			if (hitAt < hitAtForInfinite)
				continue;
			while (i < runningMaximum && numAttacks > 0)
			{
				if (recordSortedDice)
				{
					final boolean hit = dice[i] < hitAt; // dice are zero based
					sortedDice.add(new Die(dice[i], hitAt, hit ? DieType.HIT : DieType.MISS));
					if (hit)
						hits++;
				}
				i++;
				numAttacks--;
				totalPower += hitAt;
				rolledAt.add(hitAt);
			}
		}
		// infinite aa
		runningMaximum += infiniteAAtotalAAattacks;
		while (i < runningMaximum)
		{
			// we use the highest attack of this group, since each is infinite. (this is the default behavior in revised)
			if (recordSortedDice)
			{
				final boolean hit = dice[i] < hitAtForInfinite; // dice are zero based
				sortedDice.add(new Die(dice[i], hitAtForInfinite, hit ? DieType.HIT : DieType.MISS));
				if (hit)
					hits++;
			}
			i++;
			totalPower += hitAtForInfinite;
			rolledAt.add(hitAtForInfinite);
		}
		// overstack aa
		runningMaximum += overstackAAtotalAAattacks;
		final Iterator<Unit> overstackAAiter = overstackAA.iterator();
		while (i < runningMaximum && overstackAAiter.hasNext())
		{
			final Unit aaGun = overstackAAiter.next();
			int numAttacks = UnitAttachment.get(aaGun.getType()).getMaxAAattacks(); // should be > 0 at this point
			final int hitAt = getAAattackAndMaxDiceSides(Collections.singleton(aaGun), data, defending).getFirst(); // zero based, so subtract 1
			while (i < runningMaximum && numAttacks > 0)
			{
				if (recordSortedDice)
				{
					final boolean hit = dice[i] < hitAt; // dice are zero based
					sortedDice.add(new Die(dice[i], hitAt, hit ? DieType.HIT : DieType.MISS));
					if (hit)
						hits++;
				}
				i++;
				numAttacks--;
				totalPower += hitAt;
				rolledAt.add(hitAt);
			}
		}
		return new Triple<Integer, Integer, Boolean>(totalPower, hits, (rolledAt.size() == 1));
	}
	
	private static int getLowLuckHits(final IDelegateBridge bridge, final List<Die> sortedDice, final int totalPower, final int chosenDiceSize, final PlayerID playerRolling, final String annotation)
	{
		int hits = totalPower / chosenDiceSize;
		final int hitsFractional = totalPower % chosenDiceSize;
		if (hitsFractional > 0)
		{
			final int[] dice = bridge.getRandom(chosenDiceSize, 1, playerRolling, DiceType.COMBAT, annotation);
			final boolean hit = hitsFractional > dice[0];
			if (hit)
			{
				hits++;
			}
			final Die die = new Die(dice[0], hitsFractional, hit ? DieType.HIT : DieType.MISS);
			sortedDice.add(die);
		}
		return hits;
	}
	
	/**
	 * Roll dice for units.
	 * 
	 * @param annotation
	 * 
	 */
	public static DiceRoll rollDice(final List<Unit> units, final boolean defending, final PlayerID player, final IDelegateBridge bridge, final IBattle battle, final String annotation,
				final Collection<TerritoryEffect> territoryEffects)
	{
		// Decide whether to use low luck rules or normal rules.
		if (games.strategy.triplea.Properties.getLow_Luck(bridge.getData()))
		{
			return rollDiceLowLuck(units, defending, player, bridge, battle, annotation, territoryEffects);
		}
		else
		{
			return rollDiceNormal(units, defending, player, bridge, battle, annotation, territoryEffects);
		}
	}
	
	/**
	 * Roll n-sided dice.
	 * 
	 * @param annotation
	 *            0 based, add 1 to get actual die roll
	 */
	public static DiceRoll rollNDice(final IDelegateBridge bridge, final int rollCount, final int sides, final PlayerID playerRolling, final DiceType diceType, final String annotation)
	{
		if (rollCount == 0)
		{
			return new DiceRoll(new ArrayList<Die>(), 0);
		}
		int[] random;
		random = bridge.getRandom(sides, rollCount, playerRolling, diceType, annotation);
		final List<Die> dice = new ArrayList<Die>();
		int diceIndex = 0;
		for (int i = 0; i < rollCount; i++)
		{
			dice.add(new Die(random[diceIndex], 1, DieType.IGNORED));
			diceIndex++;
		}
		final DiceRoll rVal = new DiceRoll(dice, rollCount);
		return rVal;
	}
	
	public static int getTotalPower(final List<Unit> units, final boolean defending, final PlayerID player, final Territory location, final Collection<TerritoryEffect> territoryEffects,
				final GameData data, final boolean isAmphibiousBattle, final Collection<Unit> amphibiousLandAttackers)
	{
		final boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);
		final int extraRollBonus = Math.max(1, data.getDiceSides() / 6); // bonus is normally 1 for most games
		// int artillerySupportAvailable = getArtillerySupportAvailable(units, defending, player);
		final Set<List<UnitSupportAttachment>> supportRules = new HashSet<List<UnitSupportAttachment>>();
		final IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<UnitSupportAttachment>();
		getSupport(units, supportRules, supportLeft, data, defending);
		final int rollCount = BattleCalculator.getRolls(units, location, player, defending, new HashSet<List<UnitSupportAttachment>>(supportRules), new IntegerMap<UnitSupportAttachment>(supportLeft),
					territoryEffects);
		if (rollCount <= 0)
			return 0;
		int power = 0;
		// We iterate through the units to find the total strength of the units
		for (final Unit current : units)
		{
			final UnitAttachment ua = UnitAttachment.get(current.getType());
			// make a copy for getRolls
			final int rolls = BattleCalculator.getRolls(current, location, player, defending, new HashSet<List<UnitSupportAttachment>>(supportRules),
						new IntegerMap<UnitSupportAttachment>(supportLeft), territoryEffects);
			int totalStr = 0;
			for (int i = 0; i < rolls; i++)
			{
				if (i > 1 && (lhtrBombers || ua.getChooseBestRoll()))
				{
					// LHTR means pick the best dice roll, which doesn't really make sense in LL. So instead, we will just add +1 onto the power to simulate the gains of having the best die picked.
					if (totalStr < data.getDiceSides())
					{
						final int maxToAdd = Math.min(extraRollBonus, (data.getDiceSides() - totalStr));
						power += maxToAdd;
						totalStr += maxToAdd;
					}
					continue;
				}
				else
				{
					int strength;
					if (defending)
					{
						strength = ua.getDefense(current.getOwner());
						// If it's a sneak attack, defenders roll at a 1
						if (isFirstTurnLimitedRoll(player, data))
						{
							strength = Math.min(1, strength);
						}
						else
						{
							strength += getSupport(current.getType(), supportRules, supportLeft);
						}
					}
					else
					{
						strength = ua.getAttack(current.getOwner());
						if (ua.getIsMarine() && isAmphibiousBattle)
						{
							if (amphibiousLandAttackers.contains(current))
								++strength;
						}
						if (ua.getIsSea() && isAmphibiousBattle)
						{
							strength = ua.getBombard(current.getOwner());
						}
						strength += getSupport(current.getType(), supportRules, supportLeft);
					}
					strength += TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
					totalStr += strength;
					power += Math.min(Math.max(strength, 0), data.getDiceSides());
				}
			}
		}
		return power;
	}
	
	/**
	 * Roll dice for units using low luck rules. Low luck rules based on rules in DAAK.
	 */
	private static DiceRoll rollDiceLowLuck(final List<Unit> units, final boolean defending, final PlayerID player, final IDelegateBridge bridge, final IBattle battle, final String annotation,
				final Collection<TerritoryEffect> territoryEffects)
	{
		final GameData data = bridge.getData();
		final Territory location = battle.getTerritory();
		final boolean isAmphibiousBattle = battle.isAmphibious();
		final Collection<Unit> amphibiousLandAttackers = battle.getAmphibiousLandAttackers();
		// make a copy to send to getRolls (due to need to know number of rolls based on support, as zero attack units will or will not get a roll depending)
		int power = getTotalPower(units, defending, player, location, territoryEffects, data, isAmphibiousBattle, amphibiousLandAttackers);
		if (power == 0)
		{
			return new DiceRoll(new ArrayList<Die>(0), 0);
		}
		int hitCount = 0;
		// Get number of hits
		hitCount = power / data.getDiceSides();
		int[] random = new int[0];
		final List<Die> dice = new ArrayList<Die>();
		// We need to roll dice for the fractional part of the dice.
		power = power % data.getDiceSides();
		if (power != 0)
		{
			random = bridge.getRandom(data.getDiceSides(), 1, player, DiceType.COMBAT, annotation);
			final boolean hit = power > random[0];
			if (hit)
			{
				hitCount++;
			}
			dice.add(new Die(random[0], power, hit ? DieType.HIT : DieType.MISS));
		}
		// Create DiceRoll object
		final DiceRoll rVal = new DiceRoll(dice, hitCount);
		bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
		return rVal;
	}
	
	/**
	 * @param units
	 * @param defending
	 * @param player
	 * @return
	 */
	public static int getArtillerySupportAvailable(final List<Unit> units, final boolean defending, final PlayerID player)
	{
		int artillerySupportAvailable = 0;
		if (!defending)
		{
			final Collection<Unit> arty = Match.getMatches(units, Matches.UnitIsArtillery);
			final Iterator<Unit> iter = arty.iterator();
			while (iter.hasNext())
			{
				final Unit current = iter.next();
				final UnitAttachment ua = UnitAttachment.get(current.getType());
				artillerySupportAvailable += ua.getUnitSupportCount();
			}
			// If ImprovedArtillery, double number of units to support
			if (isImprovedArtillerySupport(player))
				artillerySupportAvailable *= 2;
		}
		return artillerySupportAvailable;
	}
	
	public static int getArtillerySupportAvailable(final Unit u, final boolean defending, final PlayerID player)
	{
		if (Matches.UnitIsArtillery.match(u) && !defending)
		{
			final UnitAttachment ua = UnitAttachment.get(u.getType());
			int artillerySupportAvailable = ua.getUnitSupportCount();
			if (isImprovedArtillerySupport(player))
				artillerySupportAvailable *= 2;
			return artillerySupportAvailable;
		}
		return 0;
	}
	
	public static int getSupportableAvailable(final List<Unit> units, final boolean defending, final PlayerID player)
	{
		if (!defending)
			return Match.countMatches(units, Matches.UnitIsArtillerySupportable);
		return 0;
	}
	
	public static int getSupportableAvailable(final Unit u, final boolean defending, final PlayerID player)
	{
		if (Matches.UnitIsArtillerySupportable.match(u) && !defending)
			return 1;
		return 0;
	}
	
	/*
	 * populates support rule set, grouped in lists of non-stacking rules
	 * populates rule use counter
	 * handling defence here for simplicity
	 */
	public static void getSupport(final List<Unit> units, final Set<List<UnitSupportAttachment>> support, final IntegerMap<UnitSupportAttachment> supportLeft, final GameData data,
				final boolean defending)
	{
		final Iterator<UnitSupportAttachment> iter = UnitSupportAttachment.get(data).iterator();
		while (iter.hasNext())
		{
			final UnitSupportAttachment rule = iter.next();
			if (rule.getPlayers().isEmpty())
				continue;
			if (defending && rule.getDefence() || !defending && rule.getOffence())
			{
				final CompositeMatchAnd<Unit> canSupport = new CompositeMatchAnd<Unit>(Matches.unitIsOfType((UnitType) rule.getAttachedTo()), Matches.unitOwnedBy(rule.getPlayers()));
				final List<Unit> supporters = Match.getMatches(units, canSupport);
				int numSupport = supporters.size();
				if (rule.getImpArtTech())
					numSupport += Match.getMatches(supporters, Matches.unitOwnerHasImprovedArtillerySupportTech()).size();
				if (numSupport <= 0)
					continue;
				final String bonusType = rule.getBonusType();
				supportLeft.put(rule, numSupport * rule.getNumber());
				final Iterator<List<UnitSupportAttachment>> iter2 = support.iterator();
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
					support.add(ruleType);
				}
				if (ruleType != null)
					ruleType.add(rule);
			}
		}
		sortSupportRules(support);
	}
	
	/*
	 * get support bonus for individual unit
	 * decrements the rule counter.
	 */
	public static int getSupport(final UnitType type, final Set<List<UnitSupportAttachment>> support, final IntegerMap<UnitSupportAttachment> supportLeft)
	{
		int strength = 0;
		final Iterator<List<UnitSupportAttachment>> iter = support.iterator();
		while (iter.hasNext())
		{
			final Iterator<UnitSupportAttachment> iter2 = iter.next().iterator();
			while (iter2.hasNext())
			{
				final UnitSupportAttachment rule = iter2.next();
				final HashSet<UnitType> types = rule.getUnitType();
				if (types != null && types.contains(type) && supportLeft.getInt(rule) > 0)
				{
					strength += rule.getBonus();
					supportLeft.add(rule, -1);
					break;
				}
			}
		}
		return strength;
	}
	
	public static void sortByStrength(final List<Unit> units, final boolean defending)
	{
		final Comparator<Unit> comp = new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				Integer v1, v2;
				if (defending)
				{
					v1 = Integer.valueOf(UnitAttachment.get(u1.getType()).getDefense(u1.getOwner()));
					v2 = Integer.valueOf(UnitAttachment.get(u2.getType()).getDefense(u2.getOwner()));
				}
				else
				{
					v1 = Integer.valueOf(UnitAttachment.get(u1.getType()).getAttack(u1.getOwner()));
					v2 = Integer.valueOf(UnitAttachment.get(u2.getType()).getAttack(u2.getOwner()));
				}
				return v1.compareTo(v2);
			}
		};
		Collections.sort(units, comp);
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
				final int compareTo = v2.compareTo(v1);
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
				return s1.compareTo(s2);
			}
		};
		final Iterator<List<UnitSupportAttachment>> iter = support.iterator();
		while (iter.hasNext())
		{
			Collections.sort(iter.next(), compList);
		}
	}
	
	/**
	 * @author veqryn
	 * 
	 * @param unitsList
	 * @param defending
	 * @param player
	 * @param bridge
	 * @param battle
	 * @param annotation
	 * @return
	 */
	public static DiceRoll airBattle(final List<Unit> unitsList, final boolean defending, final PlayerID player, final IDelegateBridge bridge, final IBattle battle, final String annotation)
	{
		final GameData data = bridge.getData();
		final boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);
		final List<Unit> units = new ArrayList<Unit>(unitsList);
		final int rollCount = AirBattle.getAirBattleRolls(unitsList, defending);
		if (rollCount == 0)
		{
			return new DiceRoll(new ArrayList<Die>(), 0);
		}
		int[] random;
		final List<Die> dice = new ArrayList<Die>();
		int hitCount = 0;
		if (games.strategy.triplea.Properties.getLow_Luck(data))
		{
			final int extraRollBonus = Math.max(1, data.getDiceSides() / 6); // bonus is normally 1 for most games
			final Iterator<Unit> iter = units.iterator();
			int power = 0;
			// We iterate through the units to find the total strength of the units
			while (iter.hasNext())
			{
				final Unit current = iter.next();
				final UnitAttachment ua = UnitAttachment.get(current.getType());
				final int rolls = AirBattle.getAirBattleRolls(current, defending);
				int totalStrength = 0;
				final int strength = Math.min(data.getDiceSides(), Math.max(0, (defending ? ua.getAirDefense(current.getOwner()) : ua.getAirAttack(current.getOwner()))));
				for (int i = 0; i < rolls; i++)
				{
					// LHTR means pick the best dice roll, which doesn't really make sense in LL. So instead, we will just add +1 onto the power to simulate the gains of having the best die picked.
					if (i > 1 && (lhtrBombers || ua.getChooseBestRoll()))
					{
						totalStrength += extraRollBonus;
						continue;
					}
					totalStrength += strength;
				}
				power += Math.min(Math.max(totalStrength, 0), data.getDiceSides());
			}
			// Get number of hits
			hitCount = power / data.getDiceSides();
			random = new int[0];
			// We need to roll dice for the fractional part of the dice.
			power = power % data.getDiceSides();
			if (power != 0)
			{
				random = bridge.getRandom(data.getDiceSides(), 1, player, DiceType.COMBAT, annotation);
				final boolean hit = power > random[0];
				if (hit)
				{
					hitCount++;
				}
				dice.add(new Die(random[0], power, hit ? DieType.HIT : DieType.MISS));
			}
		}
		else
		{
			random = bridge.getRandom(data.getDiceSides(), rollCount, player, DiceType.COMBAT, annotation);
			final Iterator<Unit> iter = units.iterator();
			int diceIndex = 0;
			while (iter.hasNext())
			{
				final Unit current = iter.next();
				final UnitAttachment ua = UnitAttachment.get(current.getType());
				final int strength = Math.min(data.getDiceSides(), Math.max(0, (defending ? ua.getAirDefense(current.getOwner()) : ua.getAirAttack(current.getOwner()))));
				final int rolls = AirBattle.getAirBattleRolls(current, defending);
				// lhtr heavy bombers take best of n dice for both attack and defense
				if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll()))
				{
					int minIndex = 0;
					int min = data.getDiceSides();
					for (int i = 0; i < rolls; i++)
					{
						if (random[diceIndex + i] < min)
						{
							min = random[diceIndex + i];
							minIndex = i;
						}
					}
					final boolean hit = strength > random[diceIndex + minIndex];
					dice.add(new Die(random[diceIndex + minIndex], strength, hit ? DieType.HIT : DieType.MISS));
					for (int i = 0; i < rolls; i++)
					{
						if (i != minIndex)
							dice.add(new Die(random[diceIndex + i], strength, DieType.IGNORED));
					}
					if (hit)
						hitCount++;
					diceIndex += rolls;
				}
				else
				{
					for (int i = 0; i < rolls; i++)
					{
						final boolean hit = strength > random[diceIndex];
						dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
						if (hit)
							hitCount++;
						diceIndex++;
					}
				}
			}
		}
		final DiceRoll rVal = new DiceRoll(dice, hitCount);
		bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
		return rVal;
	}
	
	/**
	 * Roll dice for units per normal rules.
	 */
	private static DiceRoll rollDiceNormal(final List<Unit> unitsList, final boolean defending, final PlayerID player, final IDelegateBridge bridge, final IBattle battle, final String annotation,
				final Collection<TerritoryEffect> territoryEffects)
	{
		final GameData data = bridge.getData();
		final List<Unit> units = new ArrayList<Unit>(unitsList);
		sortByStrength(units, defending);
		final boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);
		final Territory location = battle.getTerritory();
		// int artillerySupportAvailable = getArtillerySupportAvailable(units, defending, player);
		final Set<List<UnitSupportAttachment>> supportRules = new HashSet<List<UnitSupportAttachment>>();
		final IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<UnitSupportAttachment>();
		getSupport(units, supportRules, supportLeft, data, defending);
		// make a copy to send to getRolls (due to need to know number of rolls based on support, as zero attack units will or will not get a roll depending)
		final int rollCount = BattleCalculator.getRolls(units, location, player, defending, new HashSet<List<UnitSupportAttachment>>(supportRules), new IntegerMap<UnitSupportAttachment>(supportLeft),
					territoryEffects);
		if (rollCount == 0)
		{
			return new DiceRoll(new ArrayList<Die>(), 0);
		}
		int[] random;
		random = bridge.getRandom(data.getDiceSides(), rollCount, player, DiceType.COMBAT, annotation);
		final List<Die> dice = new ArrayList<Die>();
		final Iterator<Unit> iter = units.iterator();
		int hitCount = 0;
		int diceIndex = 0;
		while (iter.hasNext())
		{
			final Unit current = iter.next();
			final UnitAttachment ua = UnitAttachment.get(current.getType());
			// make a copy for getRolls
			final int rolls = BattleCalculator.getRolls(current, location, player, defending, new HashSet<List<UnitSupportAttachment>>(supportRules),
						new IntegerMap<UnitSupportAttachment>(supportLeft), territoryEffects);
			// lhtr heavy bombers take best of n dice for both attack and defense
			if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll()))
			{
				int strength;
				if (defending)
					strength = ua.getDefense(current.getOwner());
				else
					strength = ua.getAttack(current.getOwner());
				strength += getSupport(current.getType(), supportRules, supportLeft);
				strength += TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
				strength = Math.min(Math.max(strength, 0), data.getDiceSides());
				int minIndex = 0;
				int min = data.getDiceSides();
				for (int i = 0; i < rolls; i++)
				{
					if (random[diceIndex + i] < min)
					{
						min = random[diceIndex + i];
						minIndex = i;
					}
				}
				final boolean hit = strength > random[diceIndex + minIndex];
				dice.add(new Die(random[diceIndex + minIndex], strength, hit ? DieType.HIT : DieType.MISS));
				for (int i = 0; i < rolls; i++)
				{
					if (i != minIndex)
						dice.add(new Die(random[diceIndex + i], strength, DieType.IGNORED));
				}
				if (hit)
					hitCount++;
				diceIndex += rolls;
			}
			else
			{
				for (int i = 0; i < rolls; i++)
				{
					int strength;
					if (defending)
					{
						strength = ua.getDefense(current.getOwner());
						if (isFirstTurnLimitedRoll(player, data))
						{
							strength = Math.min(1, strength);
						}
						else
						{
							strength += getSupport(current.getType(), supportRules, supportLeft);
						}
					}
					else
					{
						strength = ua.getAttack(current.getOwner());
						if (ua.getIsMarine() && battle.isAmphibious())
						{
							final Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
							if (landUnits.contains(current))
								++strength;
						}
						// get bombarding unit's strength
						if (ua.getIsSea() && battle.isAmphibious())
						{
							strength = ua.getBombard(current.getOwner());
						}
						strength += getSupport(current.getType(), supportRules, supportLeft);
					}
					strength += TerritoryEffectHelper.getTerritoryCombatBonus(current.getType(), territoryEffects, defending);
					strength = Math.min(Math.max(strength, 0), data.getDiceSides());
					final boolean hit = strength > random[diceIndex];
					dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
					if (hit)
						hitCount++;
					diceIndex++;
				}
			}
		}
		final DiceRoll rVal = new DiceRoll(dice, hitCount);
		bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
		return rVal;
	}
	
	/**
	 * Roll dice for units per normal rules.
	 */
	/*
	private static DiceRoll rollDiceNormalold(List<Unit> units, boolean defending, PlayerID player, IDelegateBridge bridge, GameData data, Battle battle, String annotation)
	{
	    
		boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);

	    int artillerySupportAvailable = getArtillerySupportAvailable(units, defending, player);
	    //int rollCount = BattleCalculator.getRolls(units, player, defending);
	    int rollCount = BattleCalculator.getRolls(units, player, defending, artillerySupportAvailable);
	    
	    if (rollCount == 0)
	    {
	        return new DiceRoll(new ArrayList<Die>(), 0);
	    }

	    int[] random;
	   
	    random = bridge.getRandom(Constants.MAX_DICE, rollCount, annotation);

	    List<Die> dice = new ArrayList<Die>();
	    
	    Iterator<Unit> iter = units.iterator();

	    int hitCount = 0;
	    int diceIndex = 0;
	    while (iter.hasNext())
	    {
	        Unit current = (Unit) iter.next();
	        UnitAttachment ua = UnitAttachment.get(current.getType());
	        
	        int rolls = BattleCalculator.getRolls(current, player, defending, artillerySupportAvailable);

	        //lhtr heavy bombers take best of n dice for both attack and defense
	        if(rolls > 1 && lhtrBombers && ua.isStrategicBomber())
	        {
	            int strength;
	            if(defending)
	                strength = ua.getDefense(current.getOwner());
	            else
	                strength = ua.getAttack(current.getOwner());
	            
	            //it is easier to assume two for now
	            //if it is something else, the code below gets a
	            //bit more general
	            if(rolls != 2)
	                throw new IllegalStateException("Only expecting 2 dice for lhtr heavy bombers");
	            

	            
	            if(random[diceIndex] <= random[diceIndex+1])
	            {
	                boolean hit = strength > random[diceIndex];
	                dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
	                dice.add(new Die(random[diceIndex+1], strength, DieType.IGNORED));
	                if(hit)
	                    hitCount++;
	            }
	            else
	            {
	                boolean hit = strength >= random[diceIndex + 1];
	                dice.add(new Die(random[diceIndex],  strength, DieType.IGNORED));
	                dice.add(new Die(random[diceIndex+1], strength, hit ? DieType.HIT : DieType.MISS));
	                if(hit)
	                    hitCount++;

	            }
	                
	            //2 dice
	            diceIndex++;
	            diceIndex++;
	            
	        }
	        else
	        {
	            for (int i = 0; i < rolls; i++)
	            {
	                int strength;
	                if (defending)
	                    //If it's a sneak attack, defenders roll at a 1
	                {
	                    strength = ua.getDefense(current.getOwner());
	                    if (isFirstTurnLimitedRoll(player))
	                    {
	                        strength = Math.min(1, strength);
	                    }
	                }
	                else
	                {
	                    strength = ua.getAttack(current.getOwner());
	                    if (ua.isArtillerySupportable() && artillerySupportAvailable > 0 && strength < Constants.MAX_DICE)
	                    {
	                    	//TODO probably need a map here to properly add artilleryBonus
	                        strength++;
	                        artillerySupportAvailable--;
	                    }
	                    if (ua.getIsMarine() && battle.isAmphibious())
	                    {
	                        Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
	                        if(landUnits.contains(current))
	                            ++strength;
	                    }
	                    //get bombarding unit's strength
	                    if (ua.isSea() && battle.isAmphibious())
	                    	strength = ua.getBombard(current.getOwner());
	                }
	
	                boolean hit = strength > random[diceIndex];
	                dice.add(new Die(random[diceIndex], strength, hit ? DieType.HIT : DieType.MISS));
	
	                if (hit)
	                    hitCount++;
	                diceIndex++;
	            }
	        }
	    }

	    DiceRoll rVal = new DiceRoll(dice, hitCount);
	    bridge.getHistoryWriter().addChildToEvent(annotation + " : " + MyFormatter.asDice(random), rVal);
	    return rVal;
	}
	*/
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
	
	public static boolean isAmphibious(final Collection<Unit> m_units)
	{
		final Iterator<Unit> unitIter = m_units.iterator();
		while (unitIter.hasNext())
		{
			final TripleAUnit checkedUnit = (TripleAUnit) unitIter.next();
			if (checkedUnit.getWasAmphibious())
			{
				return true;
			}
		}
		return false;
	}
	
	private static boolean isImprovedArtillerySupport(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.getImprovedArtillerySupport();
	}
	
	/**
	 * @param units
	 * @param player
	 * @param battle
	 * @return
	 */
	public static String getAnnotation(final List<Unit> units, final PlayerID player, final IBattle battle)
	{
		final StringBuilder buffer = new StringBuilder(80);
		buffer.append(player.getName()).append(" roll dice for ").append(MyFormatter.unitsToTextNoOwner(units));
		if (battle != null)
			buffer.append(" in ").append(battle.getTerritory().getName()).append(", round ").append((battle.getBattleRound() + 1));
		return buffer.toString();
	}
	
	/**
	 * 
	 * @param dice
	 *            int[] the dice, 0 based
	 * @param hits
	 *            int - the number of hits
	 * @param rollAt
	 *            int - what we roll at, [0,Constants.MAX_DICE]
	 * @param hitOnlyIfEquals
	 *            boolean - do we get a hit only if we are equals, or do we hit
	 *            when we are equal or less than for example a 5 is a hit when
	 *            rolling at 6 for equal and less than, but is not for equals
	 */
	public DiceRoll(final int[] dice, final int hits, final int rollAt, final boolean hitOnlyIfEquals)
	{
		m_hits = hits;
		m_rolls = new ArrayList<Die>(dice.length);
		for (int i = 0; i < dice.length; i++)
		{
			boolean hit;
			if (hitOnlyIfEquals)
				hit = (rollAt == dice[i]);
			else
				hit = dice[i] <= rollAt;
			m_rolls.add(new Die(dice[i], rollAt, hit ? DieType.HIT : DieType.MISS));
		}
	}
	
	// only for externalizable
	public DiceRoll()
	{
	}
	
	private DiceRoll(final List<Die> dice, final int hits)
	{
		m_rolls = new ArrayList<Die>(dice);
		m_hits = hits;
	}
	
	public int getHits()
	{
		return m_hits;
	}
	
	/**
	 * @param rollAt
	 *            the strength of the roll, eg infantry roll at 2, expecting a
	 *            number in [1,6]
	 * @return in int[] which shouldnt be modifed, the int[] is 0 based, ie
	 *         0..MAX_DICE
	 */
	public List<Die> getRolls(final int rollAt)
	{
		final List<Die> rVal = new ArrayList<Die>();
		for (final Die die : m_rolls)
		{
			if (die.getRolledAt() == rollAt)
				rVal.add(die);
		}
		return rVal;
	}
	
	public int size()
	{
		return m_rolls.size();
	}
	
	public Die getDie(final int index)
	{
		return m_rolls.get(index);
	}
	
	public void writeExternal(final ObjectOutput out) throws IOException
	{
		final int[] dice = new int[m_rolls.size()];
		for (int i = 0; i < m_rolls.size(); i++)
		{
			dice[i] = m_rolls.get(i).getCompressedValue();
		}
		out.writeObject(dice);
		out.writeInt(m_hits);
	}
	
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
	{
		final int[] dice = (int[]) in.readObject();
		m_rolls = new ArrayList<Die>(dice.length);
		for (int i = 0; i < dice.length; i++)
		{
			m_rolls.add(Die.getFromWriteValue(dice[i]));
		}
		m_hits = in.readInt();
	}
	
	@Override
	public String toString()
	{
		return "DiceRoll dice:" + m_rolls + " hits:" + m_hits;
	}
}
