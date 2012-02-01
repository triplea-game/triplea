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
/*
 * BattleCalculator.java
 * 
 * Created on November 29, 2001, 2:27 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UnitSupportAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.triplea.weakAI.WeakAI;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
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

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          Utiltity class for determing casualties and selecting casualties. The code
 *          was being dduplicated all over the place.
 */
public class BattleCalculator
{
	// private static IntegerMap<UnitType> s_costsForTuvForAllPlayersMergedAndAveraged; //There is a problem with this variable, that it isn't being cleared out when we switch maps.
	// we want to sort in a determined way so that those looking at the dice results
	// can tell what dice is for who
	// we also want to sort by movement, so casualties will be choosen as the
	// units with least movement
	public static void sortPreBattle(final List<Unit> units, final GameData data)
	{
		final Comparator<Unit> comparator = new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				if (u1.getUnitType().equals(u2.getUnitType()))
					return UnitComparator.getDecreasingMovementComparator().compare(u1, u2);
				return u1.getUnitType().getName().compareTo(u2.getUnitType().getName());
			}
		};
		Collections.sort(units, comparator);
	}
	
	public static int getAAHits(final Collection<Unit> units, final IDelegateBridge bridge, final int[] dice)
	{
		final int attackingAirCount = Match.countMatches(units, Matches.UnitIsAir);
		int hitCount = 0;
		for (int i = 0; i < attackingAirCount; i++)
		{
			if (1 > dice[i])
				hitCount++;
		}
		return hitCount;
	}
	
	/**
	 * Choose plane casualties according to specified rules
	 */
	public static Collection<Unit> getAACasualties(final Collection<Unit> planes, final Collection<Unit> defendingAA, final DiceRoll dice, final IDelegateBridge bridge, final PlayerID defender,
				final PlayerID attacker, final GUID battleID, final Territory terr)
	{
		final GameData data = bridge.getData();
		if (Properties.getLow_Luck(data) || Properties.getLL_AA_ONLY(data))
		{
			if (isChooseAA(data))
			{
				return chooseAACasualties(planes, dice, bridge, attacker, battleID, terr);
			}
			return getLowLuckAACasualties(planes, defendingAA, dice, terr, bridge);
		}
		else
		{
			// isRollAAIndividually() is the default behavior
			final Boolean rollAAIndividually = isRollAAIndividually(data);
			// Random AA Casualties
			if (!rollAAIndividually && isRandomAACasualties(data))
				return (RandomAACasualties(planes, dice, bridge));
			// allow player to select casualties from entire set
			if (!rollAAIndividually && isChooseAA(data))
			{
				return chooseAACasualties(planes, dice, bridge, attacker, battleID, terr);
			}
			return (IndividuallyFiredAACasualties(planes, defendingAA, dice, terr, bridge));
		}
	}
	
	private static Collection<Unit> chooseAACasualties(final Collection<Unit> planes, final DiceRoll dice, final IDelegateBridge bridge, final PlayerID attacker, final GUID battleID,
				final Territory terr)
	{
		final String text = "Select " + dice.getHits() + " casualties from aa fire in " + terr.getName();
		final CasualtyDetails casualtyMsg = selectCasualties(attacker, planes, bridge, text, dice, false, battleID);
		return casualtyMsg.getKilled();
	}
	
	/**
	 * http://triplea.sourceforge.net/mywiki/Forum#nabble-td4658925%7Ca4658925
	 * 
	 * returns two lists, the first list is the air units that can be evenly divided into groups of 3 or 6 (depending on radar)
	 * the second list is all the air units that do not fit in the first list
	 * 
	 */
	public static Tuple<List<Unit>, List<Unit>> categorizeLowLuckAirUnits(final Collection<Unit> units, final Territory location, final int diceSides, final int groupSize)
	{
		final Collection<UnitCategory> categorizedAir = UnitSeperator.categorize(units, null, false, true);
		final List<Unit> groupsOfSize = new ArrayList<Unit>();
		final List<Unit> toRoll = new ArrayList<Unit>();
		for (final UnitCategory uc : categorizedAir)
		{
			final int remainder = uc.getUnits().size() % groupSize;
			final int splitPosition = uc.getUnits().size() - remainder;
			groupsOfSize.addAll(uc.getUnits().subList(0, splitPosition));
			toRoll.addAll(uc.getUnits().subList(splitPosition, uc.getUnits().size()));
		}
		return new Tuple<List<Unit>, List<Unit>>(groupsOfSize, toRoll);
	}
	
	private static Collection<Unit> getLowLuckAACasualties(final Collection<Unit> planes, final Collection<Unit> defendingAA, final DiceRoll dice, final Territory location,
				final IDelegateBridge bridge)
	{
		final Tuple<Integer, Integer> attackThenDiceSides = DiceRoll.getAAattackAndMaxDiceSides(defendingAA, bridge.getData());
		final int highestAttack = attackThenDiceSides.getFirst();
		final int chosenDiceSize = attackThenDiceSides.getSecond();
		final Collection<Unit> hitUnits = new ArrayList<Unit>();
		if (highestAttack < 1)
			return hitUnits;
		final int groupSize = chosenDiceSize / highestAttack;
		final Tuple<List<Unit>, List<Unit>> airSplit = categorizeLowLuckAirUnits(planes, location, chosenDiceSize, groupSize);
		int hitsLeft = dice.getHits();
		// the non rolling air units
		if (hitsLeft < airSplit.getFirst().size() / groupSize)
		{
			final List<Unit> tempPossibleHitUnits = new ArrayList<Unit>();
			for (int i = 0; i < airSplit.getFirst().size(); i += groupSize)
			{
				tempPossibleHitUnits.add(airSplit.getFirst().get(i));
			}
			final int[] hitRandom = bridge.getRandom(tempPossibleHitUnits.size(), hitsLeft, "Deciding which planes should die due to AA fire");
			int pos = 0;
			for (int i = 0; i < hitRandom.length; i++)
			{
				pos += hitRandom[i];
				hitUnits.add(tempPossibleHitUnits.remove(pos % tempPossibleHitUnits.size()));
			}
			hitsLeft = 0;
		}
		else
		{
			for (int i = 0; i < airSplit.getFirst().size(); i += groupSize)
			{
				hitUnits.add(airSplit.getFirst().get(i));
				hitsLeft--;
			}
		}
		if (hitsLeft == airSplit.getSecond().size())
		{
			hitUnits.addAll(airSplit.getSecond());
		}
		else if (hitsLeft != 0)
		{
			// the remainder
			// roll all at once to prevent frequent random calls, important for pbem games
			final int[] hitRandom = bridge.getRandom(airSplit.getSecond().size(), hitsLeft, "Deciding which planes should die due to AA fire");
			int pos = 0;
			for (int i = 0; i < hitRandom.length; i++)
			{
				pos += hitRandom[i];
				hitUnits.add(airSplit.getSecond().remove(pos % airSplit.getSecond().size()));
			}
			hitsLeft = 0;
		}
		if (hitUnits.size() != dice.getHits())
		{
			throw new IllegalStateException("wrong number of casulaties, expected:" + dice + " but hit:" + hitUnits);
		}
		return hitUnits;
	}
	
	/**
	 * Choose plane casualties randomly
	 */
	public static Collection<Unit> RandomAACasualties(final Collection<Unit> planes, final DiceRoll dice, final IDelegateBridge bridge)
	{
		final Collection<Unit> casualties = new ArrayList<Unit>();
		final int hits = dice.getHits();
		final List<Unit> planesList = new ArrayList<Unit>(planes);
		// We need to choose which planes die randomly
		if (hits < planesList.size())
		{
			for (int i = 0; i < hits; i++)
			{
				final int pos = bridge.getRandom(planesList.size(), "Deciding which planes should die due to AA fire");
				final Unit unit = planesList.get(pos);
				planesList.remove(pos);
				casualties.add(unit);
			}
		}
		else
		{
			casualties.addAll(planesList);
		}
		return casualties;
	}
	
	/**
	 * Choose plane casualties based on individual AA shots at each aircraft.
	 */
	public static Collection<Unit> IndividuallyFiredAACasualties(final Collection<Unit> planes, final Collection<Unit> defendingAA, final DiceRoll dice, final Territory location,
				final IDelegateBridge bridge)
	{
		// if we have aa guns that are not infinite, then we need to randomly decide the aa casualties since there are not enough rolls to have a single roll for each aircraft, or too many rolls
		if (DiceRoll.getTotalAAattacks(defendingAA, planes, bridge.getData()) != planes.size())
			return RandomAACasualties(planes, dice, bridge);
		final Tuple<Integer, Integer> attackThenDiceSides = DiceRoll.getAAattackAndMaxDiceSides(defendingAA, bridge.getData());
		final int highestAttack = attackThenDiceSides.getFirst();
		// int chosenDiceSize = attackThenDiceSides[1];
		final Collection<Unit> casualties = new ArrayList<Unit>();
		final int hits = dice.getHits();
		final List<Unit> planesList = new ArrayList<Unit>(planes);
		// We need to choose which planes die based on their position in the list and the individual AA rolls
		if (hits < planesList.size())
		{
			final List<Die> rolls = dice.getRolls(highestAttack);
			for (int i = 0; i < rolls.size(); i++)
			{
				final Die die = rolls.get(i);
				if (die.getType() == DieType.HIT)
				{
					final Unit unit = planesList.get(i);
					casualties.add(unit);
				}
			}
			planesList.removeAll(casualties);
		}
		else
		{
			casualties.addAll(planesList);
		}
		return casualties;
	}
	
	public static CasualtyDetails selectCasualties(final PlayerID player, final Collection<Unit> targets, final IDelegateBridge bridge, final String text, final DiceRoll dice,
				final boolean defending, final GUID battleID)
	{
		return selectCasualties(null, player, targets, bridge, text, dice, defending, battleID, false, dice.getHits());
	}
	
	/**
	 * 
	 * @param battleID
	 *            may be null if we are not in a battle (eg, if this is an aa fire due to moving
	 */
	public static CasualtyDetails selectCasualties(final String step, final PlayerID player, final Collection<Unit> targets, final IDelegateBridge bridge, final String text, final DiceRoll dice,
				final boolean defending, final GUID battleID, final boolean headLess, final int extraHits)
	{
		final GameData data = bridge.getData();
		final boolean isEditMode = EditDelegate.getEditMode(data);
		// if (isEditMode || dice.getHits() == 0)
		if (dice.getHits() == 0)
			return new CasualtyDetails(Collections.<Unit> emptyList(), Collections.<Unit> emptyList(), true);
		int hitsRemaining = dice.getHits();
		Map<Unit, Collection<Unit>> dependents;
		if (headLess)
			dependents = Collections.emptyMap();
		else
			dependents = getDependents(targets, data);
		if (isTransportCasualtiesRestricted(data))
		{
			hitsRemaining = extraHits;
		}
		// TODO check if isEditMode is necessary
		if (!isEditMode && allTargetsOneTypeNotTwoHit(targets, dependents))
		{
			final List<Unit> killed = new ArrayList<Unit>();
			final Iterator<Unit> iter = targets.iterator();
			for (int i = 0; i < hitsRemaining; i++)
			{
				if (i >= targets.size())
					break;
				killed.add(iter.next());
			}
			return new CasualtyDetails(killed, Collections.<Unit> emptyList(), true);
		}
		// Create production cost map, Maybe should do this elsewhere, but in
		// case prices change, we do it here.
		final IntegerMap<UnitType> costs = getCostsForTUV(player, data);
		final CasualtyList defaultCasualties = getDefaultCasualties(targets, hitsRemaining, defending, player, costs, data);
		ITripleaPlayer tripleaPlayer;
		if (player.isNull())
			tripleaPlayer = new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
		else
			tripleaPlayer = (ITripleaPlayer) bridge.getRemote(player);
		final CasualtyDetails casualtySelection;
		if (hitsRemaining >= targets.size())
		{
			casualtySelection = new CasualtyDetails(defaultCasualties, true);
		}
		else
		{
			casualtySelection = tripleaPlayer.selectCasualties(targets, dependents, hitsRemaining, text, dice, player, defaultCasualties, battleID);
		}
		List<Unit> killed = casualtySelection.getKilled();
		// if partial retreat is possible, kill amphibious units first
		if (isPartialAmphibiousRetreat(data))
			killed = killAmphibiousFirst(killed, targets);
		final List<Unit> damaged = casualtySelection.getDamaged();
		int numhits = killed.size();
		for (final Unit unit : killed)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua.isTwoHit() && (unit.getHits() == 0))
			{
				numhits++;
				damaged.remove(unit);
			}
		}
		// check right number
		if (!isEditMode && !(numhits + damaged.size() == (hitsRemaining > targets.size() ? targets.size() : hitsRemaining)))
		{
			tripleaPlayer.reportError("Wrong number of casualties selected");
			return selectCasualties(player, targets, bridge, text, dice, defending, battleID);
		}
		// check we have enough of each type
		if (!targets.containsAll(killed) || !targets.containsAll(damaged))
		{
			tripleaPlayer.reportError("Cannot remove enough units of those types");
			return selectCasualties(player, targets, bridge, text, dice, defending, battleID);
		}
		return casualtySelection;
	}
	
	private static List<Unit> killAmphibiousFirst(final List<Unit> killed, final Collection<Unit> targets)
	{
		final Collection<Unit> allAmphibUnits = new ArrayList<Unit>();
		final Collection<Unit> killedNonAmphibUnits = new ArrayList<Unit>();
		final Collection<UnitType> amphibTypes = new ArrayList<UnitType>();
		// Get a list of all selected killed units that are NOT amphibious
		final Match<Unit> aMatch = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitWasNotAmphibious);
		killedNonAmphibUnits.addAll(Match.getMatches(killed, aMatch));
		// If all killed units are amphibious, just return them
		if (killedNonAmphibUnits.isEmpty())
			return killed;
		// Get a list of all units that are amphibious and remove those that are killed
		allAmphibUnits.addAll(Match.getMatches(targets, Matches.UnitWasAmphibious));
		allAmphibUnits.removeAll(Match.getMatches(killed, Matches.UnitWasAmphibious));
		final Iterator<Unit> allAmphibUnitsIter = allAmphibUnits.iterator();
		// Get a collection of the unit types of the amphib units
		while (allAmphibUnitsIter.hasNext())
		{
			final Unit unit = allAmphibUnitsIter.next();
			final UnitType ut = unit.getType();
			if (!amphibTypes.contains(ut))
				amphibTypes.add(ut);
		}
		// For each killed unit- see if there is an amphib unit that can be killed instead
		for (final Unit unit : killedNonAmphibUnits)
		{
			if (amphibTypes.contains(unit.getType()))
			{ // add a unit from the collection
				final List<Unit> oneAmphibUnit = Match.getNMatches(allAmphibUnits, 1, Matches.unitIsOfType(unit.getType()));
				if (oneAmphibUnit.size() > 0)
				{
					final Unit amphibUnit = oneAmphibUnit.iterator().next();
					killed.remove(unit);
					killed.add(amphibUnit);
					allAmphibUnits.remove(amphibUnit);
					continue;
				}
				else
				// If there are no more units of that type, remove the type from the collection
				{
					amphibTypes.remove(unit.getType());
				}
			}
		}
		return killed;
	}
	
	private static boolean s_enableCasualtySortingCaching = false;
	
	public static void EnableCasualtySortingCaching()
	{
		s_enableCasualtySortingCaching = true;
	}
	
	public static void DisableCasualtySortingCaching()
	{
		s_enableCasualtySortingCaching = false;
		synchronized (s_cachedLock)
		{
			s_cachedSortedCasualties.clear(); // Don't keep all this stuff in memory (basically, we just want this caching so if a battle is simulated 5000 times, we only sort units once)
		}
	}
	
	// Key is the hash of the possible casualties collection[targets], value is the cached sorted result[perfectlySortedUnitsList]
	private static HashMap<Integer, List<Unit>> s_cachedSortedCasualties = new HashMap<Integer, List<Unit>>();
	private static final Object s_cachedLock = new Object();
	
	/**
	 * A unit with two hitpoints will be listed twice if they will die. The first time they are listed it is as damaged. The second time they are listed, it is dead.
	 * 
	 * @param targets
	 * @param hits
	 * @param defending
	 * @param player
	 * @param costs
	 * @param data
	 * @return
	 */
	private static CasualtyList getDefaultCasualties(final Collection<Unit> targets, final int hits, final boolean defending, final PlayerID player, final IntegerMap<UnitType> costs,
				final GameData data)
	{
		final CasualtyList defaultCasualtySelection = new CasualtyList();
		// Remove two hit bb's selecting them first for default casualties
		int numSelectedCasualties = 0;
		final Iterator<Unit> targetsIter = targets.iterator();
		while (targetsIter.hasNext())
		{
			// Stop if we have already selected as many hits as there are targets
			if (numSelectedCasualties >= hits)
			{
				return defaultCasualtySelection;
			}
			final Unit unit = targetsIter.next();
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			if (ua.isTwoHit() && (unit.getHits() == 0))
			{
				numSelectedCasualties++;
				defaultCasualtySelection.addToDamaged(unit);
			}
		}
		// Sort units by power and cost in ascending order
		final List<Unit> sorted = new ArrayList<Unit>(sortUnitsForCasualtiesWithSupport(targets, defending, player, costs, data, false));
		// Select units
		final Iterator<Unit> sortedIter = sorted.iterator();
		while (sortedIter.hasNext())
		{
			// Stop if we have already selected as many hits as there are targets
			if (numSelectedCasualties >= hits)
			{
				return defaultCasualtySelection;
			}
			final Unit unit = sortedIter.next();
			defaultCasualtySelection.addToKilled(unit);
			numSelectedCasualties++;
		}
		return defaultCasualtySelection;
	}
	
	/**
	 * he purpose of this is to return a list in the PERFECT order of which units should be selected to die first,
	 * And that means that certain units MUST BE INTERLEAVED.
	 * This list assumes that you have already taken any extra hit points away from any 2 hitpoint units.
	 * Example: You have a 1 attack Artillery unit that supports, and a 1 attack infantry unit that can receive support.
	 * The best selection of units to die is first to take whichever unit has excess, then cut that down til they are both the same size,
	 * then to take 1 artillery followed by 1 infantry, followed by 1 artillery, then 1 inf, etc, until everyone is dead.
	 * If you just return all infantry followed by all artillery, or the other way around, you will be missing out on some important support provided.
	 * (Veqryn)
	 */
	public static Collection<Unit> sortUnitsForCasualtiesWithSupport(final Collection<Unit> targets, final boolean defending, final PlayerID player, final IntegerMap<UnitType> costs,
				final GameData data, final boolean bonus)
	{
		if (s_enableCasualtySortingCaching)
		{
			synchronized (s_cachedLock)
			{
				/*if (s_cachedSortedCasualties.get(targets.hashCode()).isEmpty() || !s_cachedSortedCasualties.get(targets.hashCode()).containsAll(targets)
				    		|| !targets.containsAll(s_cachedSortedCasualties.get(targets.hashCode())) || s_cachedSortedCasualties.get(targets.hashCode()).size() != targets.size())
					s_cachedSortedCasualties.clear();
				else*/
				if (s_cachedSortedCasualties.containsKey(targets.hashCode()))
					return s_cachedSortedCasualties.get(targets.hashCode());
			}
		}
		final List<Unit> sortedUnitsList = new ArrayList<Unit>(targets);
		Collections.sort(sortedUnitsList, new UnitBattleComparator(defending, costs, data, bonus));
		final List<Unit> perfectlySortedUnitsList = new ArrayList<Unit>();
		int artillerySupportAvailable = DiceRoll.getArtillerySupportAvailable(sortedUnitsList, defending, player);
		int supportableAvailable = DiceRoll.getSupportableAvailable(sortedUnitsList, defending, player);
		if (artillerySupportAvailable == 0 || supportableAvailable == 0)
			return sortedUnitsList;
		// reset, as we don't want to count units which support themselves
		artillerySupportAvailable = 0;
		supportableAvailable = 0;
		final List<List<Unit>> unitsByPowerAll = new ArrayList<List<Unit>>();
		final List<List<Unit>> unitsByPowerBoth = new ArrayList<List<Unit>>();
		final List<List<Unit>> unitsByPowerGives = new ArrayList<List<Unit>>();
		final List<List<Unit>> unitsByPowerReceives = new ArrayList<List<Unit>>();
		final List<List<Unit>> unitsByPowerNone = new ArrayList<List<Unit>>();
		// Decide what is the biggest size for the lists that we will support
		final int maxDiceTimesRolls = 1 + 2 * data.getDiceSides();
		// Find what the biggest unit we have is. If they are bigger than maxDiceTimesRolls, set to maxDiceTimesRolls.
		final int maxPower = Math.max(0, Math.min(getUnitPowerForSorting(sortedUnitsList.get(sortedUnitsList.size() - 1), defending, data), maxDiceTimesRolls));
		// Fill the lists with the six dice numbers (plus Zero, and any above if we have units with multiple rolls), or unit powers, which we will populate with the units
		for (int i = 0; i <= maxPower; i++)
		{
			unitsByPowerAll.add(new ArrayList<Unit>());
			unitsByPowerBoth.add(new ArrayList<Unit>());
			unitsByPowerGives.add(new ArrayList<Unit>());
			unitsByPowerReceives.add(new ArrayList<Unit>());
			unitsByPowerNone.add(new ArrayList<Unit>());
		}
		// in order to merge lists, we need to separate sortedUnitsList into multiple lists by power
		for (final Unit current : sortedUnitsList)
		{
			int unitPower = getUnitPowerForSorting(current, defending, data);
			unitPower = Math.max(0, Math.min(unitPower, maxPower)); // getUnitPowerForSorting will return numbers over max_dice IF that units Power * DiceRolls goes over max_dice
			// TODO: if a unit supports itself, it should be in a different power list, as it will always support itself. getUnitPowerForSorting() should test for this and return a higher number.
			unitsByPowerAll.get(unitPower).add(current);
			if (UnitAttachment.get(current.getType()).getArtillery() && UnitAttachment.get(current.getType()).getArtillerySupportable())
				unitsByPowerBoth.get(unitPower).add(current);
			else if (UnitAttachment.get(current.getType()).getArtillery())
			{
				unitsByPowerGives.get(unitPower).add(current);
				artillerySupportAvailable += DiceRoll.getArtillerySupportAvailable(current, defending, player);
			}
			else if (UnitAttachment.get(current.getType()).getArtillerySupportable())
			{
				unitsByPowerReceives.get(unitPower).add(current);
				supportableAvailable += DiceRoll.getSupportableAvailable(current, defending, player);
			}
			else
				unitsByPowerNone.get(unitPower).add(current);
		}
		// now merge the lists
		final List<Unit> tempList1 = new ArrayList<Unit>();
		final List<Unit> tempList2 = new ArrayList<Unit>();
		for (int i = 0; i <= maxPower; i++)
		{
			int iArtillery = DiceRoll.getArtillerySupportAvailable(unitsByPowerGives.get(i), defending, player);
			int aboveArtillery = artillerySupportAvailable - iArtillery;
			artillerySupportAvailable -= iArtillery;
			int iSupportable = DiceRoll.getSupportableAvailable(unitsByPowerReceives.get(i), defending, player);
			int aboveSupportable = supportableAvailable - iSupportable;
			supportableAvailable -= iSupportable;
			if ((iArtillery == 0 && iSupportable == 0) || (iArtillery == 0 && aboveSupportable >= aboveArtillery) || ((iSupportable == 0 || iArtillery == 0) && aboveSupportable == aboveArtillery)
						|| (iSupportable == 0 && aboveSupportable <= aboveArtillery) || (i == maxDiceTimesRolls))
				perfectlySortedUnitsList.addAll(unitsByPowerAll.get(i));
			else
			{
				int count = 0;
				while (0 < unitsByPowerBoth.get(i).size() || 0 < unitsByPowerGives.get(i).size() || 0 < unitsByPowerReceives.get(i).size() || 0 < unitsByPowerNone.get(i).size())
				{
					count++;
					if (count > 100000)
						throw new IllegalStateException("Infinite loop in sortUnitsForCasualtiesWithSupport.");
					tempList1.clear();
					tempList2.clear();
					// four variables: we have artillery, we have support, above has artillery, above has support. need every combination covered.
					if (iArtillery == 0 && aboveArtillery - aboveSupportable > 0 && unitsByPowerReceives.get(i).size() > 0)
					{
						while (aboveArtillery - aboveSupportable > 0 && unitsByPowerReceives.get(i).size() > 0)
						{
							final int last = unitsByPowerReceives.get(i).size() - 1;
							tempList2.add(unitsByPowerReceives.get(i).get(last));
							aboveSupportable += DiceRoll.getSupportableAvailable(unitsByPowerReceives.get(i).get(last), defending, player);
							unitsByPowerReceives.get(i).remove(last);
						}
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerGives.get(i));
						tempList1.addAll(unitsByPowerReceives.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerGives.get(i).clear();
						unitsByPowerReceives.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, data, bonus));
						Collections.sort(tempList2, new UnitBattleComparator(defending, costs, data, bonus));
						perfectlySortedUnitsList.addAll(tempList1);
						perfectlySortedUnitsList.addAll(tempList2);
						continue;
					}
					if (iSupportable == 0 && aboveSupportable - aboveArtillery > 0 && unitsByPowerGives.get(i).size() > 0)
					{
						while (aboveSupportable - aboveArtillery > 0 && unitsByPowerGives.get(i).size() > 0)
						{
							final int last = unitsByPowerGives.get(i).size() - 1;
							tempList2.add(unitsByPowerGives.get(i).get(last));
							aboveArtillery += DiceRoll.getArtillerySupportAvailable(unitsByPowerGives.get(i).get(last), defending, player);
							unitsByPowerGives.get(i).remove(last);
						}
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerGives.get(i));
						tempList1.addAll(unitsByPowerReceives.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerGives.get(i).clear();
						unitsByPowerReceives.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, data, bonus));
						Collections.sort(tempList2, new UnitBattleComparator(defending, costs, data, bonus));
						perfectlySortedUnitsList.addAll(tempList1);
						perfectlySortedUnitsList.addAll(tempList2);
						continue;
					}
					if (iSupportable + aboveSupportable > iArtillery + aboveArtillery && unitsByPowerReceives.get(i).size() > 0)
					{
						while (iSupportable + aboveSupportable > iArtillery + aboveArtillery && unitsByPowerReceives.get(i).size() > 0)
						{
							final int first = 0;
							tempList1.add(unitsByPowerReceives.get(i).get(first));
							iSupportable -= DiceRoll.getSupportableAvailable(unitsByPowerReceives.get(i).get(first), defending, player);
							unitsByPowerReceives.get(i).remove(first);
						}
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, data, bonus));
						perfectlySortedUnitsList.addAll(tempList1);
						continue;
					}
					if (iSupportable + aboveSupportable < iArtillery + aboveArtillery)
					{
						while (iSupportable + aboveSupportable < iArtillery + aboveArtillery && unitsByPowerGives.get(i).size() > 0)
						{
							final int first = 0;
							tempList1.add(unitsByPowerGives.get(i).get(first));
							iArtillery -= DiceRoll.getArtillerySupportAvailable(unitsByPowerGives.get(i).get(first), defending, player);
							unitsByPowerGives.get(i).remove(first);
						}
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, data, bonus));
						perfectlySortedUnitsList.addAll(tempList1);
						continue;
					}
					if (iSupportable + aboveSupportable == iArtillery + aboveArtillery)
					{
						tempList1.addAll(unitsByPowerNone.get(i));
						tempList1.addAll(unitsByPowerBoth.get(i));
						unitsByPowerNone.get(i).clear();
						unitsByPowerBoth.get(i).clear();
						if (!unitsByPowerGives.get(i).isEmpty())
							tempList2.add(unitsByPowerGives.get(i).get(0));
						if (!unitsByPowerReceives.get(i).isEmpty())
							tempList2.add(unitsByPowerReceives.get(i).get(0));
						Collections.sort(tempList2, new UnitBattleComparator(defending, costs, data, bonus));
						final Unit u = tempList2.get(0);
						tempList1.add(u);
						final UnitAttachment ua = UnitAttachment.get(u.getType());
						if (ua.getArtillery())
						{
							unitsByPowerGives.get(i).remove(0);
							iArtillery -= DiceRoll.getArtillerySupportAvailable(u, defending, player);
						}
						else
						{
							unitsByPowerReceives.get(i).remove(0);
							iSupportable -= DiceRoll.getSupportableAvailable(u, defending, player);
						}
						Collections.sort(tempList1, new UnitBattleComparator(defending, costs, data, bonus));
						perfectlySortedUnitsList.addAll(tempList1);
						continue;
					}
					// and we should never get down here
					throw new IllegalStateException("Possibility not accounted for in sortUnitsForCasualtiesWithSupport.");
				}
			}
		}
		if (perfectlySortedUnitsList.isEmpty() || !perfectlySortedUnitsList.containsAll(sortedUnitsList) || !sortedUnitsList.containsAll(perfectlySortedUnitsList)
					|| perfectlySortedUnitsList.size() != sortedUnitsList.size())
			throw new IllegalStateException("Possibility not accounted for in sortUnitsForCasualtiesWithSupport.");
		if (s_enableCasualtySortingCaching)
		{
			synchronized (s_cachedLock)
			{
				if (!s_cachedSortedCasualties.containsKey(targets.hashCode()))
					s_cachedSortedCasualties.put(targets.hashCode(), perfectlySortedUnitsList);
			}
		}
		return perfectlySortedUnitsList;
	}
	
	public static Map<Unit, Collection<Unit>> getDependents(final Collection<Unit> targets, final GameData data)
	{
		// just worry about transports
		final TransportTracker tracker = new TransportTracker();
		final Map<Unit, Collection<Unit>> dependents = new HashMap<Unit, Collection<Unit>>();
		for (final Unit target : targets)
		{
			dependents.put(target, tracker.transportingAndUnloaded(target));
		}
		return dependents;
	}
	
	/**
	 * Return map where keys are unit types and values are PU costs of that unit type, based on a player.
	 * 
	 * Any production rule that produces multiple units
	 * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
	 * will have their costs rounded up on a per unit basis (so NWO artillery will become 4).
	 * Therefore, this map should NOT be used for Purchasing information!
	 * 
	 * @param player
	 *            The player to get costs schedule for
	 * @param data
	 *            The game data.
	 * @return a map of unit types to PU cost
	 */
	public static IntegerMap<UnitType> getCostsForTUV(final PlayerID player, final GameData data)
	{
		final IntegerMap<UnitType> costs = new IntegerMap<UnitType>();
		final ProductionFrontier frontier = player.getProductionFrontier();
		// any one will do then
		if (frontier == null)
			return getCostsForTuvForAllPlayersMergedAndAveraged(data);
		for (final ProductionRule rule : frontier.getRules())
		{
			final int costPerGroup = rule.getCosts().getInt(data.getResourceList().getResource(Constants.PUS));
			final UnitType type = (UnitType) rule.getResults().keySet().iterator().next();
			final int numberProduced = rule.getResults().getInt(type);
			// we average the cost for a single unit, rounding up
			final int roundedCostPerSingle = (int) Math.ceil((double) costPerGroup / (double) numberProduced);
			costs.put(type, roundedCostPerSingle);
		}
		// since our production frontier may not cover all the units we control, and not the enemy units,
		// we will add any unit types not in our list, based on the list for everyone
		final IntegerMap<UnitType> costsAll = getCostsForTuvForAllPlayersMergedAndAveraged(data);
		for (final UnitType ut : costsAll.keySet())
		{
			if (!costs.keySet().contains(ut))
				costs.put(ut, costsAll.getInt(ut));
		}
		return costs;
	}
	
	/*
	 * This clears out the variable map keeping track of the average costs of units on this game. Should use this any time you load a game or switch games.
	 *
	public static void clearCostsForTuvForAllPlayersMergedAndAveraged() {
		s_costsForTuvForAllPlayersMergedAndAveraged.clear();
	}*/
	/**
	 * Return a map where key are unit types and values are the AVERAGED for all RULES (not for all players).
	 * 
	 * Any production rule that produces multiple units
	 * (like artillery in NWO, costs 7 but makes 2 artillery, meaning effective price is 3.5 each)
	 * will have their costs rounded up on a per unit basis.
	 * Therefore, this map should NOT be used for Purchasing information!
	 * 
	 * @param data
	 * @return
	 */
	public static IntegerMap<UnitType> getCostsForTuvForAllPlayersMergedAndAveraged(final GameData data)
	{
		/*if (s_costsForTuvForAllPlayersMergedAndAveraged != null && s_costsForTuvForAllPlayersMergedAndAveraged.size() > 0)
			return s_costsForTuvForAllPlayersMergedAndAveraged;*/
		final IntegerMap<UnitType> costs = new IntegerMap<UnitType>();
		final HashMap<UnitType, List<Integer>> differentCosts = new HashMap<UnitType, List<Integer>>();
		for (final ProductionRule rule : data.getProductionRuleList().getProductionRules())
		{
			// only works for the first result, so we are assuming each purchase frontier only gives one type of unit
			final UnitType ut = (UnitType) rule.getResults().keySet().iterator().next();
			final int numberProduced = rule.getResults().getInt(ut);
			final int costPerGroup = rule.getCosts().getInt(data.getResourceList().getResource(Constants.PUS));
			// we round up the cost
			final int roundedCostPerSingle = (int) Math.ceil((double) costPerGroup / (double) numberProduced);
			if (differentCosts.containsKey(ut))
				differentCosts.get(ut).add(roundedCostPerSingle);
			else
			{
				final List<Integer> listTemp = new ArrayList<Integer>();
				listTemp.add(roundedCostPerSingle);
				differentCosts.put(ut, listTemp);
			}
		}
		for (final UnitType ut : differentCosts.keySet())
		{
			int totalCosts = 0;
			final List<Integer> costsForType = differentCosts.get(ut);
			for (final int cost : costsForType)
			{
				totalCosts += cost;
			}
			final int averagedCost = (int) Math.round(((double) totalCosts / (double) costsForType.size()));
			costs.put(ut, averagedCost);
		}
		// s_costsForTuvForAllPlayersMergedAndAveraged = costs; //There is a problem with this variable, that it isn't being cleared out when we switch maps.
		return costs;
	}
	
	/**
	 * Return the total unit value
	 * 
	 * @param units
	 *            A collection of units
	 * @param costs
	 *            An integer map of unit types to costs.
	 * @return the total unit value.
	 */
	public static int getTUV(final Collection<Unit> units, final IntegerMap<UnitType> costs)
	{
		int tuv = 0;
		for (final Unit u : units)
		{
			final int unitValue = costs.getInt(u.getType());
			tuv += unitValue;
		}
		return tuv;
	}
	
	/**
	 * Return the total unit value for a certain player and his allies
	 * 
	 * @param units
	 *            A collection of units
	 * @param player
	 *            The player to calculate the TUV for.
	 * @param costs
	 *            An integer map of unit types to costs
	 * @return the total unit value.
	 */
	public static int getTUV(final Collection<Unit> units, final PlayerID player, final IntegerMap<UnitType> costs, final GameData data)
	{
		final Collection<Unit> playerUnits = Match.getMatches(units, Matches.alliedUnit(player, data));
		return getTUV(playerUnits, costs);
	}
	
	/**
	 * Checks if the given collections target are all of one category as defined
	 * by UnitSeperator.categorize and they are not two hit units.
	 * 
	 * @param targets
	 *            a collection of target units
	 * @param dependents
	 *            map of depend units for target units
	 */
	private static boolean allTargetsOneTypeNotTwoHit(final Collection<Unit> targets, final Map<Unit, Collection<Unit>> dependents)
	{
		final Set<UnitCategory> categorized = UnitSeperator.categorize(targets, dependents, false, false);
		if (categorized.size() == 1)
		{
			final UnitCategory unitCategory = categorized.iterator().next();
			if (!unitCategory.isTwoHit() || unitCategory.getDamaged())
			{
				return true;
			}
		}
		return false;
	}
	
	public static int getRolls(final Collection<Unit> units, final Territory location, final PlayerID id, final boolean defend, final Set<List<UnitSupportAttachment>> supportRulesCopy,
				final IntegerMap<UnitSupportAttachment> supportLeftCopy)
	{
		int count = 0;
		int unitRoll = 0;
		for (final Unit unit : units)
		{
			unitRoll = getRolls(unit, location, id, defend, supportRulesCopy, supportLeftCopy);
			count += unitRoll;
		}
		return count;
	}
	
	public static int getRolls(final Collection<Unit> units, final Territory location, final PlayerID id, final boolean defend)
	{
		return getRolls(units, location, id, defend, new HashSet<List<UnitSupportAttachment>>(), new IntegerMap<UnitSupportAttachment>());
	}
	
	public static int getRolls(final Unit unit, final Territory location, final PlayerID id, final boolean defend, final Set<List<UnitSupportAttachment>> supportRulesCopy,
				final IntegerMap<UnitSupportAttachment> supportLeftCopy)
	{
		final UnitAttachment unitAttachment = UnitAttachment.get(unit.getType());
		int rolls = 0;
		if (defend)
			rolls = unitAttachment.getDefenseRolls(id);
		else
			rolls = unitAttachment.getAttackRolls(id);
		// Don't forget that units can have zero attack, and then be given attack power by support, and therefore be able to roll
		if (rolls == 0 && unitAttachment.getAttack(id) == 0)
		{
			if (DiceRoll.getSupport(unit.getType(), supportRulesCopy, supportLeftCopy) > 0)
				rolls += 1;
		}
		if (rolls == 0 && unitAttachment.getAttack(id) == 0)
		{
			if (TerritoryEffectHelper.getTerritoryCombatBonus(unit.getType(), location, defend) > 0)
				rolls += 1;
		}
		return rolls;
	}
	
	public static int getRolls(final Unit unit, final Territory location, final PlayerID id, final boolean defend)
	{
		return getRolls(unit, location, id, defend, new HashSet<List<UnitSupportAttachment>>(), new IntegerMap<UnitSupportAttachment>());
	}
	
	/**
	 * @return Can transports be used as cannon fodder
	 */
	private static boolean isTransportCasualtiesRestricted(final GameData data)
	{
		return games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
	}
	
	/**
	 * @return Random AA Casualties - casualties randomly assigned
	 */
	private static boolean isRandomAACasualties(final GameData data)
	{
		return games.strategy.triplea.Properties.getRandomAACasualties(data);
	}
	
	/**
	 * @return Roll AA Individually - roll against each aircraft
	 */
	private static boolean isRollAAIndividually(final GameData data)
	{
		return games.strategy.triplea.Properties.getRollAAIndividually(data);
	}
	
	/**
	 * @return Choose AA - attacker selects casualties
	 */
	private static boolean isChooseAA(final GameData data)
	{
		return games.strategy.triplea.Properties.getChoose_AA_Casualties(data);
	}
	
	/**
	 * @return Can the attacker retreat non-amphibious units
	 */
	private static boolean isPartialAmphibiousRetreat(final GameData data)
	{
		return games.strategy.triplea.Properties.getPartialAmphibiousRetreat(data);
	}
	
	// nothing but static
	private BattleCalculator()
	{
	}
	
	/**
	 * This returns the exact Power that a unit has according to what DiceRoll.rollDiceLowLuck() would give it.
	 * As such, it needs to exactly match DiceRoll, otherwise this method will become useless.
	 * It does NOT take into account SUPPORT.
	 * It DOES take into account ROLLS.
	 * It needs to be updated to take into account isMarine.
	 */
	public static int getUnitPowerForSorting(final Unit current, final boolean defending, final GameData data)
	{
		/* this is needed if i plan to have it account for support
		Set<List<UnitSupportAttachment>> supportRules = new HashSet<List<UnitSupportAttachment>>();
		IntegerMap<UnitSupportAttachment> supportLeft = new IntegerMap<UnitSupportAttachment>();
		DiceRoll.getSupport(sortedUnitsList,supportRules,supportLeft,data,defending);
		*/
		final boolean lhtrBombers = games.strategy.triplea.Properties.getLHTR_Heavy_Bombers(data);
		final UnitAttachment ua = UnitAttachment.get(current.getType());
		int rolls;
		if (defending)
			rolls = ua.getDefenseRolls(current.getOwner());
		else
			rolls = ua.getAttackRolls(current.getOwner());
		// int strength = 0;
		int strengthWithoutSupport = 0;
		// Find the strength the unit has without support
		// lhtr heavy bombers take best of n dice for both attack and defense
		if (rolls > 1 && lhtrBombers && ua.getIsStrategicBomber())
		{
			if (defending)
				strengthWithoutSupport = ua.getDefense(current.getOwner());
			else
				strengthWithoutSupport = ua.getAttack(current.getOwner());
			// just add one like LL if we are LHTR bombers
			strengthWithoutSupport = Math.min(Math.max(strengthWithoutSupport + 1, 0), data.getDiceSides());
			// strength += DiceRoll.getSupport(current.getType(), supportRules, supportLeft);
			// strength = Math.min(Math.max(strength+1, 0), Constants.MAX_DICE);
		}
		else
		{
			for (int i = 0; i < rolls; i++)
			{
				int tempStrength;
				if (defending)
					tempStrength = ua.getDefense(current.getOwner());
				else
					tempStrength = ua.getAttack(current.getOwner());
				if (defending)
				{
					// if (DiceRoll.isFirstTurnLimitedRoll(player))
					// tempStrength = Math.min(1, tempStrength);
				}
				else
				{
					/* TODO: figure out how to find if we are in a battle, and if that battle is amphibious
					if (ua.getIsMarine() && battle.isAmphibious())
					{
					    Collection<Unit> landUnits = battle.getAmphibiousLandAttackers();
					    if(landUnits.contains(current))
					        ++tempStrength;
					} */
				}
				strengthWithoutSupport += Math.min(Math.max(tempStrength, 0), data.getDiceSides());
				// tempStrength += DiceRoll.getSupport(current.getType(), supportRules, supportLeft);
				// strength += Math.min(Math.max(tempStrength, 0), Constants.MAX_DICE);
			}
		}
		return strengthWithoutSupport;
		/*
		//Find the strength this unit gives to other units
		Iterator<UnitSupportAttachment> iter = UnitSupportAttachment.get(data).iterator();
		while(iter.hasNext())
		{
			UnitSupportAttachment rule = iter.next();
			if(rule.getPlayers().isEmpty())
				continue;
			if( defending && rule.getDefence() ||
					!defending && rule.getOffence() )
			{
				CompositeMatchAnd<Unit> canSupport = new CompositeMatchAnd<Unit>(Matches.unitIsOfType((UnitType)rule.getAttachedTo()),Matches.unitOwnedBy(rule.getPlayers()));
				List<Unit> supporters = Match.getMatches(sortedUnitsList, canSupport);
				int numSupport = supporters.size();
				if(rule.getImpArtTech())
					numSupport += Match.getMatches(supporters, Matches.unitOwnerHasImprovedArtillerySupportTech()).size();
				String bonusType = rule.getBonusType();
				//supportLeft.put(rule, numSupport*rule.getNumber());
				Iterator<List<UnitSupportAttachment>> iter2 = supportRules.iterator();
				List<UnitSupportAttachment> ruleType = null;
				boolean found = false;
				while( iter2.hasNext()){
					ruleType = iter2.next();
					if( ruleType.get(0).getBonusType().equals(bonusType) ){
						found = true;
						break;
					}
				}
				if( !found ) {
					ruleType = new ArrayList<UnitSupportAttachment>();
					supportRules.add(ruleType);
				}
				ruleType.add(rule);
			}
		}*/
	}
}
