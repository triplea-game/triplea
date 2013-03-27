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
 * Battle.java
 * 
 * Created on November 15, 2001, 12:39 PM
 * 
 * @version $LastChangedDate$
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.GameOverException;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.ConnectionLostException;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAbilityAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * Handles logic for battles in which fighting actually occurs.
 * 
 * @author Sean Bridges
 * 
 */
public class MustFightBattle extends AbstractBattle implements BattleStepStrings
{
	public static enum ReturnFire
	{
		ALL, SUBS, NONE
	}
	

	public static enum RetreatType
	{
		DEFAULT, SUBS, PLANES, PARTIAL_AMPHIB
	}
	

	// these class exist for testing
	public static abstract class AttackSubs implements IExecutable
	{
		private static final long serialVersionUID = 4872551667582174716L;
	}
	

	public static abstract class DefendSubs implements IExecutable
	{
		private static final long serialVersionUID = 3768066729336520095L;
	}
	
	private static final long serialVersionUID = 5879502298361231540L;
	private Map<Territory, Collection<Unit>> m_attackingFromMap = new HashMap<Territory, Collection<Unit>>(); // maps Territory-> units (stores a collection of who is attacking from where, needed for undoing moves)
	private final Collection<Unit> m_attackingWaitingToDie = new ArrayList<Unit>();
	private Set<Territory> m_attackingFrom = new HashSet<Territory>();
	private final Collection<Territory> m_amphibiousAttackFrom = new ArrayList<Territory>();
	private final Collection<Unit> m_defendingWaitingToDie = new ArrayList<Unit>();
	private Collection<Unit> m_defendingAir = new ArrayList<Unit>();
	private final Collection<Unit> m_killed = new ArrayList<Unit>(); // keep track of all the units that die in the battle to show in the history window
	/** Our current execution state, we keep a stack of executables, this allows us to save our state and resume while in the middle of a battle. */
	private final ExecutionStack m_stack = new ExecutionStack();
	private List<String> m_stepStrings;
	protected List<Unit> m_defendingAA;
	protected List<String> m_AAtypes;
	private final List<Unit> m_attackingUnitsRetreated = new ArrayList<Unit>();
	private final List<Unit> m_defendingUnitsRetreated = new ArrayList<Unit>();
	
	public MustFightBattle(final Territory battleSite, final PlayerID attacker, final GameData data, final BattleTracker battleTracker)
	{
		super(battleSite, attacker, battleTracker, false, BattleType.NORMAL, data);
		m_defendingUnits.addAll(m_battleSite.getUnits().getMatches(Matches.enemyUnit(attacker, data)));
	}
	
	public void resetDefendingUnits(final Territory battleSite, final PlayerID attacker, final GameData data)
	{
		m_defendingUnits.clear();
		m_defendingUnits.addAll(m_battleSite.getUnits().getMatches(Matches.enemyUnit(attacker, data)));
	}
	
	/**
	 * Used for head-less battles
	 * 
	 * @param defending
	 *            - defending units
	 * @param attacking
	 *            - attacking units
	 * @param bombarding
	 *            - bombarding units
	 * @param defender
	 *            - defender PlayerID
	 */
	public void setUnits(final Collection<Unit> defending, final Collection<Unit> attacking, final Collection<Unit> bombarding, final Collection<Unit> amphibious,
				final PlayerID defender, final Collection<TerritoryEffect> territoryEffects)
	{
		m_defendingUnits = new ArrayList<Unit>(defending);
		m_attackingUnits = new ArrayList<Unit>(attacking);
		m_bombardingUnits = new ArrayList<Unit>(bombarding);
		m_amphibiousLandAttackers = new ArrayList<Unit>(amphibious);
		m_isAmphibious = m_amphibiousLandAttackers.size() > 0;
		m_defender = defender;
		m_territoryEffects = territoryEffects;
	}
	
	private boolean canSubsSubmerge()
	{
		return games.strategy.triplea.Properties.getSubmersible_Subs(m_data);
	}
	
	@Override
	public void removeAttack(final Route route, final Collection<Unit> units)
	{
		m_attackingUnits.removeAll(units);
		// the route could be null, in the case of a unit in a territory where a sub is submerged.
		if (route == null)
			return;
		final Territory attackingFrom = route.getTerritoryBeforeEnd();
		Collection<Unit> attackingFromMapUnits = m_attackingFromMap.get(attackingFrom);
		// handle possible null pointer
		if (attackingFromMapUnits == null)
		{
			attackingFromMapUnits = new ArrayList<Unit>();
		}
		attackingFromMapUnits.removeAll(units);
		if (attackingFromMapUnits.isEmpty())
		{
			m_attackingFrom.remove(attackingFrom);
		}
		// deal with amphibious assaults
		if (attackingFrom.isWater())
		{
			if (route.getEnd() != null && !route.getEnd().isWater() && Match.someMatch(units, Matches.UnitIsLand))
			{
				m_amphibiousLandAttackers.removeAll(Match.getMatches(units, Matches.UnitIsLand));
			}
			// if none of the units is a land unit, the attack from
			// that territory is no longer an amphibious assault
			if (Match.noneMatch(attackingFromMapUnits, Matches.UnitIsLand))
			{
				m_amphibiousAttackFrom.remove(attackingFrom);
				// do we have any amphibious attacks left?
				m_isAmphibious = !m_amphibiousAttackFrom.isEmpty();
			}
		}
		final Iterator<Unit> dependentHolders = m_dependentUnits.keySet().iterator();
		while (dependentHolders.hasNext())
		{
			final Unit holder = dependentHolders.next();
			final Collection<Unit> dependents = m_dependentUnits.get(holder);
			dependents.removeAll(units);
		}
	}
	
	@Override
	public boolean isEmpty()
	{
		return m_attackingUnits.isEmpty() && m_attackingWaitingToDie.isEmpty();
	}
	
	@Override
	public Change addAttackChange(final Route route, final Collection<Unit> units, final HashMap<Unit, HashSet<Unit>> targets)
	{
		final CompositeChange change = new CompositeChange();
		// Filter out allied units if WW2V2
		final Match<Unit> ownedBy = Matches.unitIsOwnedBy(m_attacker);
		final Collection<Unit> attackingUnits = isWW2V2() ? Match.getMatches(units, ownedBy) : units;
		final Territory attackingFrom = route.getTerritoryBeforeEnd();
		m_attackingFrom.add(attackingFrom);
		m_attackingUnits.addAll(attackingUnits);
		if (m_attackingFromMap.get(attackingFrom) == null)
		{
			m_attackingFromMap.put(attackingFrom, new ArrayList<Unit>());
		}
		final Collection<Unit> attackingFromMapUnits = m_attackingFromMap.get(attackingFrom);
		attackingFromMapUnits.addAll(attackingUnits);
		
		// are we amphibious
		if (route.getStart().isWater() && route.getEnd() != null && !route.getEnd().isWater() && Match.someMatch(attackingUnits, Matches.UnitIsLand))
		{
			m_amphibiousAttackFrom.add(getAttackFrom(route));
			m_amphibiousLandAttackers.addAll(Match.getMatches(attackingUnits, Matches.UnitIsLand));
			m_isAmphibious = true;
		}
		// TODO add dependencies for transported units?
		final Map<Unit, Collection<Unit>> dependencies = transporting(units);
		if (isAlliedAirDependents())
		{
			dependencies.putAll(MoveValidator.carrierMustMoveWith(units, units, m_data, m_attacker));
			for (final Unit carrier : dependencies.keySet())
			{
				final UnitAttachment ua = UnitAttachment.get(carrier.getUnitType());
				if (ua.getCarrierCapacity() == -1)
					continue;
				final Collection<Unit> fighters = dependencies.get(carrier);
				// Dependencies count both land and air units. Land units could be allied or owned, while air is just allied since owned already launched at beginning of turn
				fighters.retainAll(Match.getMatches(fighters, Matches.UnitIsAir));
				for (final Unit fighter : fighters)
				{
					// Set transportedBy for fighter
					change.add(ChangeFactory.unitPropertyChange(fighter, carrier, TripleAUnit.TRANSPORTED_BY));
				}
				// remove transported fighters from battle display
				m_attackingUnits.removeAll(fighters);
			}
		}
		// Set the dependent paratroopers so they die if the bomber dies.
		// TODO: this might be legacy code that can be deleted since we now keep paratrooper dependencies til they land (but need to double check)
		if (isParatroopers(m_attacker))
		{
			final Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
			final Collection<Unit> paratroops = Match.getMatches(units, Matches.UnitIsAirTransportable);
			if (!airTransports.isEmpty() && !paratroops.isEmpty())
			{
				// Load capable bombers by default>
				final Map<Unit, Unit> unitsToCapableAirTransports = MoveDelegate.mapAirTransports(route, paratroops, airTransports, true, m_attacker);
				final HashMap<Unit, Collection<Unit>> dependentUnits = new HashMap<Unit, Collection<Unit>>();
				final Collection<Unit> singleCollection = new ArrayList<Unit>();
				for (final Unit unit : unitsToCapableAirTransports.keySet())
				{
					final Collection<Unit> unitList = new ArrayList<Unit>();
					unitList.add(unit);
					final Unit bomber = unitsToCapableAirTransports.get(unit);
					singleCollection.add(unit);
					// Set transportedBy for paratrooper
					change.add(ChangeFactory.unitPropertyChange(unit, bomber, TripleAUnit.TRANSPORTED_BY));
					// Set the dependents
					if (dependentUnits.get(bomber) != null)
						dependentUnits.get(bomber).addAll(unitList);
					else
						dependentUnits.put(bomber, unitList);
				}
				dependencies.putAll(dependentUnits);
				UnitSeperator.categorize(airTransports, dependentUnits, false, false);
			}
		}
		addDependentUnits(dependencies);
		// mark units with no movement
		// for all but air
		Collection<Unit> nonAir = Match.getMatches(attackingUnits, Matches.UnitIsNotAir);
		// we dont want to change the movement of transported land units if this is a sea battle
		// so restrict non air to remove land units
		if (m_battleSite.isWater())
			nonAir = Match.getMatches(nonAir, Matches.UnitIsNotLand);
		// TODO This checks for ignored sub/trns and skips the set of the attackers to 0 movement left
		// If attacker stops in an occupied territory, movement stops (battle is optional)
		if (MoveValidator.onlyIgnoredUnitsOnPath(route, m_attacker, m_data, false))
			return change;
		change.add(ChangeFactory.markNoMovementChange(nonAir));
		return change;
	}
	
	public void addDependentUnits(final Map<Unit, Collection<Unit>> dependencies)
	{
		for (final Unit holder : dependencies.keySet())
		{
			final Collection<Unit> transporting = dependencies.get(holder);
			if (m_dependentUnits.get(holder) != null)
				m_dependentUnits.get(holder).addAll(transporting);
			else
				m_dependentUnits.put(holder, new LinkedHashSet<Unit>(transporting));
		}
	}
	
	/**
	 * @deprecated use: route.getTerritoryBeforeEnd();
	 * @param route
	 * @return
	 */
	@Deprecated
	private Territory getAttackFrom(final Route route)
	{
		return route.getTerritoryBeforeEnd();
	}
	
	private String getBattleTitle()
	{
		return m_attacker.getName() + " attack " + m_defender.getName() + " in " + m_battleSite.getName();
	}
	
	private void removeUnitsThatNoLongerExist()
	{
		if (m_headless)
			return;
		// we were having a problem with units that had been killed previously were still part of MFB's variables, so we double check that the stuff still exists here.
		m_defendingUnits.retainAll(m_battleSite.getUnits().getUnits());
		m_attackingUnits.retainAll(m_battleSite.getUnits().getUnits());
	}
	
	@Override
	public void fight(final IDelegateBridge bridge)
	{
		// remove units that may already be dead due to a previous event (like they died from a strategic bombing raid, rocket attack, etc)
		removeUnitsThatNoLongerExist();
		// we have already started
		if (m_stack.isExecuting())
		{
			final ITripleaDisplay display = getDisplay(bridge);
			display.showBattle(m_battleID, m_battleSite, getBattleTitle(), removeNonCombatants(m_attackingUnits, true, m_attacker, false, false),
						removeNonCombatants(m_defendingUnits, false, m_defender, false, false), m_killed, m_attackingWaitingToDie, m_defendingWaitingToDie, m_dependentUnits, m_attacker, m_defender,
						isAmphibious(), getBattleType());
			display.listBattleSteps(m_battleID, m_stepStrings);
			m_stack.execute(bridge);
			return;
		}
		bridge.getHistoryWriter().startEvent("Battle in " + m_battleSite, m_battleSite);
		removeAirNoLongerInTerritory();
		writeUnitsToHistory(bridge);
		// it is possible that no attacking units are present, if so end now
		// changed to only look at units that can be destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
		if (Match.getMatches(m_attackingUnits, Matches.UnitIsNotInfrastructure).size() == 0)
		{
			endBattle(bridge);
			defenderWins(bridge);
			return;
		}
		// it is possible that no defending units exist
		// changed to only look at units that can be destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
		if (Match.getMatches(m_defendingUnits, Matches.UnitIsNotInfrastructure).size() == 0)
		{
			endBattle(bridge);
			attackerWins(bridge);
			return;
		}
		addDependentUnits(transporting(m_defendingUnits));
		addDependentUnits(transporting(m_attackingUnits));
		// determine any AA
		final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed = TechAbilityAttachment.getAirborneTargettedByAA(m_attacker, m_data);
		m_defendingAA = Match.getMatches(m_defendingUnits, Matches.UnitIsAAthatCanFire(m_attackingUnits, airborneTechTargetsAllowed, m_attacker, Matches.UnitIsAAforCombatOnly, m_data));
		m_AAtypes = UnitAttachment.getAllOfTypeAAs(m_defendingAA);// comes ordered alphabetically
		Collections.reverse(m_AAtypes); // stacks are backwards
		// list the steps
		m_stepStrings = determineStepStrings(true, bridge);
		final ITripleaDisplay display = getDisplay(bridge);
		display.showBattle(m_battleID, m_battleSite, getBattleTitle(), removeNonCombatants(m_attackingUnits, true, m_attacker, false, false),
					removeNonCombatants(m_defendingUnits, false, m_defender, false, false), m_killed, m_attackingWaitingToDie, m_defendingWaitingToDie, m_dependentUnits, m_attacker, m_defender,
					isAmphibious(), getBattleType());
		display.listBattleSteps(m_battleID, m_stepStrings);
		if (!m_headless)
		{
			// take the casualties with least movement first
			if (isAmphibious())
				sortAmphib(m_attackingUnits, m_data);
			else
				BattleCalculator.sortPreBattle(m_attackingUnits, m_data);
			BattleCalculator.sortPreBattle(m_defendingUnits, m_data);
			// play a sound
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsSea) || Match.someMatch(m_defendingUnits, Matches.UnitIsSea))
			{
				if (Match.allMatch(m_attackingUnits, Matches.UnitIsSub) || (Match.someMatch(m_attackingUnits, Matches.UnitIsSub) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub)))
					ClipPlayer.play(SoundPath.CLIP_BATTLE_SEA_SUBS, m_attacker.getName());
				else
					ClipPlayer.play(SoundPath.CLIP_BATTLE_SEA_NORMAL, m_attacker.getName());
			}
			else if (Match.allMatch(m_attackingUnits, Matches.UnitIsAir) && Match.allMatch(m_defendingUnits, Matches.UnitIsAir))
			{
				ClipPlayer.play(SoundPath.CLIP_BATTLE_AIR, m_attacker.getName());
			}
			else
			{
				ClipPlayer.play(SoundPath.CLIP_BATTLE_LAND, m_attacker.getName()); // must be land battle
			}
		}
		// push on stack in opposite order of execution
		pushFightLoopOnStack(bridge);
		pushFightStartOnStack();
		m_stack.execute(bridge);
	}
	
	private void writeUnitsToHistory(final IDelegateBridge bridge)
	{
		if (m_headless)
			return;
		final Set<PlayerID> playerSet = m_battleSite.getUnits().getPlayersWithUnits();
		String transcriptText;
		// find all attacking players (unsorted)
		final Collection<PlayerID> attackers = new ArrayList<PlayerID>();
		final Collection<Unit> allAttackingUnits = new ArrayList<Unit>();
		transcriptText = "";
		for (final PlayerID current : playerSet)
		{
			if (m_data.getRelationshipTracker().isAllied(m_attacker, current) || current.equals(m_attacker))
				attackers.add(current);
		}
		// find all attacking units (unsorted)
		for (final Iterator<PlayerID> attackersIter = attackers.iterator(); attackersIter.hasNext();)
		{
			final PlayerID current = attackersIter.next();
			String delim;
			if (attackersIter.hasNext())
				delim = "; ";
			else
				delim = "";
			final Collection<Unit> attackingUnits = Match.getMatches(m_attackingUnits, Matches.unitIsOwnedBy(current));
			final String verb = current.equals(m_attacker) ? "attack" : "loiter and taunt";
			transcriptText += current.getName() + " " + verb + " with " + MyFormatter.unitsToTextNoOwner(attackingUnits) + delim;
			allAttackingUnits.addAll(attackingUnits);
			// If any attacking transports are in the battle, set their status to later restrict load/unload
			if (current.equals(m_attacker))
			{
				final CompositeChange change = new CompositeChange();
				final Collection<Unit> transports = Match.getMatches(attackingUnits, Matches.UnitCanTransport);
				final Iterator<Unit> attackTranIter = transports.iterator();
				while (attackTranIter.hasNext())
				{
					change.add(ChangeFactory.unitPropertyChange(attackTranIter.next(), true, TripleAUnit.WAS_IN_COMBAT));
				}
				bridge.addChange(change);
			}
		}
		// write attacking units to history
		if (m_attackingUnits.size() > 0)
			bridge.getHistoryWriter().addChildToEvent(transcriptText, allAttackingUnits);
		// find all defending players (unsorted)
		final Collection<PlayerID> defenders = new ArrayList<PlayerID>();
		final Collection<Unit> allDefendingUnits = new ArrayList<Unit>();
		transcriptText = "";
		for (final PlayerID current : playerSet)
		{
			if (m_data.getRelationshipTracker().isAllied(m_defender, current) || current.equals(m_defender))
			{
				defenders.add(current);
			}
		}
		// find all defending units (unsorted)
		for (final Iterator<PlayerID> defendersIter = defenders.iterator(); defendersIter.hasNext();)
		{
			final PlayerID current = defendersIter.next();
			Collection<Unit> defendingUnits;
			String delim;
			if (defendersIter.hasNext())
				delim = "; ";
			else
				delim = "";
			defendingUnits = Match.getMatches(m_defendingUnits, Matches.unitIsOwnedBy(current));
			transcriptText += current.getName() + " defend with " + MyFormatter.unitsToTextNoOwner(defendingUnits) + delim;
			allDefendingUnits.addAll(defendingUnits);
		}
		// write defending units to history
		if (m_defendingUnits.size() > 0)
			bridge.getHistoryWriter().addChildToEvent(transcriptText, allDefendingUnits);
	}
	
	private void removeAirNoLongerInTerritory()
	{
		if (m_headless)
			return;
		// remove any air units that were once in this attack, but have now
		// moved out of the territory
		// this is an ilegant way to handle this bug
		final CompositeMatch<Unit> airNotInTerritory = new CompositeMatchAnd<Unit>();
		airNotInTerritory.add(new InverseMatch<Unit>(Matches.unitIsInTerritory(m_battleSite)));
		m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits, airNotInTerritory));
	}
	
	public List<String> determineStepStrings(final boolean showFirstRun, final IDelegateBridge bridge)
	{
		final List<String> steps = new ArrayList<String>();
		if (showFirstRun)
		{
			if (canFireAA())
			{
				for (final String typeAA : UnitAttachment.getAllOfTypeAAs(m_defendingAA))
				{
					steps.add(typeAA + AA_GUNS_FIRE_SUFFIX);
					steps.add(SELECT_PREFIX + typeAA + CASUALTIES_SUFFIX);
					steps.add(REMOVE_PREFIX + typeAA + CASUALTIES_SUFFIX);
				}
			}
			if (!m_battleSite.isWater() && !getBombardingUnits().isEmpty())
			{
				steps.add(NAVAL_BOMBARDMENT);
				steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
			}
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsSuicide))
			{
				steps.add(SUICIDE_ATTACK);
				steps.add(m_defender.getName() + SELECT_CASUALTIES_SUICIDE);
			}
			if (Match.someMatch(m_defendingUnits, Matches.UnitIsSuicide) && !isDefendingSuicideAndMunitionUnitsDoNotFire())
			{
				steps.add(SUICIDE_DEFEND);
				steps.add(m_attacker.getName() + SELECT_CASUALTIES_SUICIDE);
			}
			if (!m_battleSite.isWater() && isParatroopers(m_attacker))
			{
				final Collection<Unit> bombers = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAirTransport);
				if (!bombers.isEmpty())
				{
					final Collection<Unit> dependents = getDependentUnits(bombers);
					if (!dependents.isEmpty())
					{
						steps.add(LAND_PARATROOPS);
					}
				}
			}
		}
		// Check if defending subs can submerge before battle
		if (isSubRetreatBeforeBattle())
		{
			if (!Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
				steps.add(m_attacker.getName() + SUBS_SUBMERGE);
			if (!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
				steps.add(m_defender.getName() + SUBS_SUBMERGE);
		}
		// See if there any unescorted trns
		if (m_battleSite.isWater() && isTransportCasualtiesRestricted())
		{
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsTransport) || Match.someMatch(m_defendingUnits, Matches.UnitIsTransport))
				steps.add(REMOVE_UNESCORTED_TRANSPORTS);
		}
		final boolean defenderSubsFireFirst = defenderSubsFireFirst();
		if (defenderSubsFireFirst && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
		{
			steps.add(m_defender.getName() + SUBS_FIRE);
			steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
			steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
		}
		// attacker subs sneak attack
		// Attacking subs have no sneak attack if Destroyers are present
		if (m_battleSite.isWater())
		{
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
			{
				steps.add(m_attacker.getName() + SUBS_FIRE);
				steps.add(m_defender.getName() + SELECT_SUB_CASUALTIES);
			}
		}
		final boolean onlyAttackerSneakAttack = !defenderSubsFireFirst && returnFireAgainstAttackingSubs() == ReturnFire.NONE && returnFireAgainstDefendingSubs() == ReturnFire.ALL;
		if (onlyAttackerSneakAttack)
		{
			steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
		}
		// defender subs sneak attack
		// Defending subs have no sneak attack in Pacific/Europe Theaters or if Destroyers are present
		if (m_battleSite.isWater())
		{
			if (!defenderSubsFireFirst && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
			{
				steps.add(m_defender.getName() + SUBS_FIRE);
				steps.add(m_attacker.getName() + SELECT_SUB_CASUALTIES);
			}
		}
		if (m_battleSite.isWater() && !defenderSubsFireFirst && !onlyAttackerSneakAttack && (returnFireAgainstDefendingSubs() != ReturnFire.ALL || returnFireAgainstAttackingSubs() != ReturnFire.ALL))
		{
			steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
		}
		// Air only Units can't attack subs without Destroyers present
		if (isAirAttackSubRestricted())
		{
			final Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
			units.addAll(m_attackingUnits);
			// if(!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.allMatch(m_attackingUnits, Matches.UnitIsAir))
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_defendingUnits, units))
				steps.add(SUBMERGE_SUBS_VS_AIR_ONLY);
		}
		// Air Units can't attack subs without Destroyers present
		if (m_battleSite.isWater() && isAirAttackSubRestricted())
		{
			final Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
			units.addAll(m_attackingUnits);
			// if(!Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
			if (Match.someMatch(m_attackingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_defendingUnits, units))
				steps.add(AIR_ATTACK_NON_SUBS);
		}
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotSub))
		{
			steps.add(m_attacker.getName() + FIRE);
			steps.add(m_defender.getName() + SELECT_CASUALTIES);
		}
		// Air Units can't attack subs without Destroyers present
		if (m_battleSite.isWater() && isAirAttackSubRestricted())
		{
			final Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
			units.addAll(m_defendingUnits);
			units.addAll(m_defendingWaitingToDie);
			// if(!Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer) && Match.someMatch(m_defendingUnits, Matches.UnitIsAir) && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
			if (Match.someMatch(m_defendingUnits, Matches.UnitIsAir) && !canAirAttackSubs(m_attackingUnits, units))
				steps.add(AIR_DEFEND_NON_SUBS);
		}
		if (Match.someMatch(m_defendingUnits, Matches.UnitIsNotSub))
		{
			steps.add(m_defender.getName() + FIRE);
			steps.add(m_attacker.getName() + SELECT_CASUALTIES);
		}
		// remove casualties
		steps.add(REMOVE_CASUALTIES);
		// retreat subs
		if (m_battleSite.isWater())
		{
			if (canSubsSubmerge())
			{
				if (!isSubRetreatBeforeBattle())
				{
					// Removing the logic of seeing if we can submerge subs now, because there is a chance we could kill that destroyer during combat, then submerge
					// if (canAttackerRetreatSubs())
					// {
					if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
					{
						steps.add(m_attacker.getName() + SUBS_SUBMERGE);
					}
					// }
					// if (canDefenderRetreatSubs())
					// {
					if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
					{
						steps.add(m_defender.getName() + SUBS_SUBMERGE);
					}
					// }
				}
			}
			else
			{
				if (canAttackerRetreatSubs())
				{
					if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
					{
						steps.add(m_attacker.getName() + SUBS_WITHDRAW);
					}
				}
				if (canDefenderRetreatSubs())
				{
					if (Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
					{
						steps.add(m_defender.getName() + SUBS_WITHDRAW);
					}
				}
			}
		}
		// if we are a sea zone, then we may not be able to retreat
		// (ie a sub travelled under another unit to get to the battle site)
		// or an enemy sub retreated to our sea zone
		// however, if all our sea units die, then
		// the air units can still retreat, so if we have any air units attacking in
		// a sea zone, we always have to have the retreat
		// option shown
		// later, if our sea units die, we may ask the user to retreat
		final boolean someAirAtSea = m_battleSite.isWater() && Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
		if (canAttackerRetreat() || someAirAtSea)
		{
			steps.add(m_attacker.getName() + ATTACKER_WITHDRAW);
		}
		else if (canAttackerRetreatPartialAmphib())
		{
			steps.add(m_attacker.getName() + NONAMPHIB_WITHDRAW);
		}
		else if (canAttackerRetreatPlanes())
		{
			steps.add(m_attacker.getName() + PLANES_WITHDRAW);
		}
		return steps;
	}
	
	private boolean defenderSubsFireFirst()
	{
		return returnFireAgainstAttackingSubs() == ReturnFire.ALL && returnFireAgainstDefendingSubs() == ReturnFire.NONE;
	}
	
	private void pushFightStartOnStack()
	{
		final IExecutable fireAAGuns = new IExecutable()
		{
			private static final long serialVersionUID = -1370090785530214199L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				fireAAGuns(bridge);
			}
		};
		final IExecutable fireNavalBombardment = new IExecutable()
		{
			private static final long serialVersionUID = -2255283529092427441L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				fireNavalBombardment(bridge);
			}
		};
		final IExecutable fireSuicideUnitsAttack = new IExecutable()
		{
			private static final long serialVersionUID = 6578247830066963474L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				fireSuicideUnitsAttack(bridge);
			}
		};
		final IExecutable fireSuicideUnitsDefend = new IExecutable()
		{
			private static final long serialVersionUID = 2731651892447063082L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				fireSuicideUnitsDefend(bridge);
			}
		};
		final IExecutable removeNonCombatants = new IExecutable()
		{
			private static final long serialVersionUID = 3389635458184415797L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				// we can remove any AA guns at this point
				removeNonCombatants(bridge, true, false);
			}
		};
		final IExecutable landParatroops = new IExecutable()
		{
			private static final long serialVersionUID = 7193352768857658286L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				landParatroops(bridge);
			}
		};
		final IExecutable markNoMovementLeft = new IExecutable()
		{
			private static final long serialVersionUID = -6676306363537467594L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				markNoMovementLeft(bridge);
			}
		};
		// push in opposite order of execution
		m_stack.push(markNoMovementLeft);
		m_stack.push(landParatroops);
		m_stack.push(removeNonCombatants);
		m_stack.push(fireSuicideUnitsDefend);
		m_stack.push(fireSuicideUnitsAttack);
		m_stack.push(fireNavalBombardment);
		m_stack.push(fireAAGuns);
	}
	
	private void pushFightLoopOnStack(final IDelegateBridge bridge)
	{
		if (m_isOver)
			return;
		final List<IExecutable> steps = getBattleExecutables();
		// add in the reverse order we create them
		Collections.reverse(steps);
		for (final IExecutable step : steps)
		{
			m_stack.push(step);
		}
		return;
	}
	
	List<IExecutable> getBattleExecutables()
	{
		// the code here is a bit odd to read
		// basically, we need to break the code into seperate atomic pieces.
		// If there is a network error, or some other unfortunate event,
		// then we need to keep track of what pieces we have executed, and what is left
		// to do
		// each atomic step is in its own IExecutable
		// the definition of atomic is that either
		// 1) the code does not call to an IDisplay,IPlayer, or IRandomSource
		// 2) if the code calls to an IDisplay, IPlayer, IRandomSource, and an exception is
		// called from one of those methods, the exception will be propogated out of execute(),
		// and the execute method can be called again
		// it is allowed for an iexecutable to add other iexecutables to the stack
		//
		// if you read the code in linear order, ignore wrapping stuff in annonymous iexecutables, then the code
		// can be read as it will execute
		// store the steps in a list
		// we need to push them in reverse order that we
		// create them, and its easier to track if we just add them
		// to a list while creating. then reverse the list and add
		// to the stack at the end
		final List<IExecutable> steps = new ArrayList<IExecutable>();
		addFightStepsNonEditMode(steps);
		/* FYI: according to the rules that I know, you can submerge subs the same turn you kill the last destroyer
		// we must grab these here, when we clear waiting to die, we might remove
		// all the opposing destroyers, and this would change the canRetreatSubs rVal
		final boolean canAttackerRetreatSubs = canAttackerRetreatSubs();
		final boolean canDefenderRetreatSubs = canDefenderRetreatSubs();*/
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 8611067962952500496L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				clearWaitingToDie(bridge);
			}
		});
		steps.add(new IExecutable()
		{
			// not compatible with 0.9.0.2 saved games. this is new for 1.2.6.0
			private static final long serialVersionUID = 6387198382888361848L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				checkSuicideUnits(bridge);
			}
		});
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 5259103822937067667L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				// changed to only look at units that can be destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
				if (Match.getMatches(m_attackingUnits, Matches.UnitIsNotInfrastructure).size() == 0)
				{
					if (!isTransportCasualtiesRestricted())
					{
						endBattle(bridge);
						defenderWins(bridge);
					}
					else
					{
						// Get all allied transports in the territory
						final CompositeMatch<Unit> matchAllied = new CompositeMatchAnd<Unit>();
						matchAllied.add(Matches.UnitIsTransport);
						matchAllied.add(Matches.UnitIsNotCombatTransport);
						matchAllied.add(Matches.isUnitAllied(m_attacker, m_data));
						final List<Unit> alliedTransports = Match.getMatches(m_battleSite.getUnits().getUnits(), matchAllied);
						// If no transports, just end the battle
						if (alliedTransports.isEmpty())
						{
							endBattle(bridge);
							defenderWins(bridge);
						}
						else if (m_round <= 1)
						{
							// TODO Need to determine how combined forces on attack work- trn left in terr by prev player, ally moves in and attacks
							// add back in the non-combat units (Trns)
							m_attackingUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.unitIsOwnedBy(m_attacker));
						}
						else
						{
							endBattle(bridge);
							defenderWins(bridge);
						}
					}
				}
				// changed to only look at units that can be destroyed in combat, and therefore not include factories, aaguns, and infrastructure.
				else if (Match.getMatches(m_defendingUnits, Matches.UnitIsNotInfrastructure).size() == 0)
				{
					if (isTransportCasualtiesRestricted())
					{
						// If there are undefended attacking transports, determine if they automatically die
						checkUndefendedTransports(bridge, m_defender);
					}
					checkForUnitsThatCanRollLeft(bridge, false);
					endBattle(bridge);
					attackerWins(bridge);
				}
				else if ((Match.allMatch(m_attackingUnits, Matches.unitHasAttackValueOfAtLeast(1).invert())) && Match.allMatch(m_defendingUnits, Matches.unitHasDefendValueOfAtLeast(1).invert()))
				{
					endBattle(bridge);
					nobodyWins(bridge);
				}
			}
		});
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 6775880082912594489L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver && canAttackerRetreatSubs() && !isSubRetreatBeforeBattle())
					attackerRetreatSubs(bridge);
			}
		});
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -1544916305666912480L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver)
				{
					if (canDefenderRetreatSubs() && !isSubRetreatBeforeBattle())
						defenderRetreatSubs(bridge);
					// Here we test if there are any defenders left. If no defenders, then battle is over.
					// The reason we test a "second" time here, is because otherwise the attackers can retreat even though the battle is over (illegal).
					if (m_defendingUnits.isEmpty())
					{
						endBattle(bridge);
						attackerWins(bridge);
					}
				}
			}
		});
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -1150863964807721395L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver && canAttackerRetreatPlanes() && !canAttackerRetreatPartialAmphib())
					attackerRetreatPlanes(bridge);
			}
		});
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -1150863964807721395L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver && canAttackerRetreatPartialAmphib())
					attackerRetreatNonAmphibUnits(bridge);
			}
		});
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 669349383898975048L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver)
				{
					attackerRetreat(bridge);
				}
			}
		});
		final IExecutable loop = new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 3118458517320468680L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				pushFightLoopOnStack(bridge);
			}
		};
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = -3993599528368570254L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				if (!m_isOver)
				{
					m_stepStrings = determineStepStrings(false, bridge);
					final ITripleaDisplay display = getDisplay(bridge);
					display.listBattleSteps(m_battleID, m_stepStrings);
					m_round++;
					// continue fighting
					// the recursive step
					// this should always be the base of the stack
					// when we execute the loop, it will populate the stack with the battle steps
					if (!m_stack.isEmpty())
						throw new IllegalStateException("Stack not empty:" + m_stack);
					m_stack.push(loop);
				}
			}
		});
		return steps;
	}
	
	private void addFightStepsNonEditMode(final List<IExecutable> steps)
	{
		/** Ask to retreat defending subs before battle */
		if (isSubRetreatBeforeBattle())
		{
			steps.add(new IExecutable()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 6775880082912594489L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					if (!m_isOver)
						attackerRetreatSubs(bridge);
				}
			});
			steps.add(new IExecutable()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 7056448091800764539L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					if (!m_isOver)
						defenderRetreatSubs(bridge);
				}
			});
		}
		/** Remove Suicide Units */
		steps.add(new IExecutable()
		{
			private static final long serialVersionUID = 99988L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				checkSuicideUnits(bridge);
			}
		});
		/** Remove undefended trns */
		if (isTransportCasualtiesRestricted())
			steps.add(new IExecutable()
			{
				private static final long serialVersionUID = 99989L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					checkUndefendedTransports(bridge, m_defender);
					checkUndefendedTransports(bridge, m_attacker);
					checkForUnitsThatCanRollLeft(bridge, true);
					checkForUnitsThatCanRollLeft(bridge, false);
				}
			});
		/** Submerge subs if -vs air only & air restricted from attacking subs */
		if (isAirAttackSubRestricted())
			steps.add(new IExecutable()
			{
				private static final long serialVersionUID = 99990L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					submergeSubsVsOnlyAir(bridge);
				}
			});
		final ReturnFire returnFireAgainstAttackingSubs = returnFireAgainstAttackingSubs();
		final ReturnFire returnFireAgainstDefendingSubs = returnFireAgainstDefendingSubs();
		if (defenderSubsFireFirst())
		{
			steps.add(new DefendSubs()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 99992L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					defendSubs(bridge, returnFireAgainstDefendingSubs);
				}
			});
		}
		steps.add(new AttackSubs()
		{
			private static final long serialVersionUID = 99991L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				attackSubs(bridge, returnFireAgainstAttackingSubs);
			}
		});
		if (!defenderSubsFireFirst())
		{
			steps.add(new DefendSubs()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 99992L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					defendSubs(bridge, returnFireAgainstDefendingSubs);
				}
			});
		}
		/** Attacker air fire on NON subs */
		if (isAirAttackSubRestricted())
			steps.add(new IExecutable()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 99993L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					attackAirOnNonSubs(bridge);
				}
			});
		/** Attacker fire remaining units */
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 99994L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				attackNonSubs(bridge);
			}
		});
		/** Defender air fire on NON subs */
		if (isAirAttackSubRestricted())
			steps.add(new IExecutable()
			{
				// compatible with 0.9.0.2 saved games
				private static final long serialVersionUID = 1560702114917865123L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					defendAirOnNonSubs(bridge);
				}
			});
		steps.add(new IExecutable()
		{
			// compatible with 0.9.0.2 saved games
			private static final long serialVersionUID = 1560702114917865290L;
			
			public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
			{
				defendNonSubs(bridge);
			}
		});
	}
	
	private ReturnFire returnFireAgainstAttackingSubs()
	{
		final boolean attackingSubsSneakAttack = !Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer);
		final boolean defendingSubsSneakAttack = defendingSubsSneakAttack2();
		final ReturnFire returnFireAgainstAttackingSubs;
		if (!attackingSubsSneakAttack)
		{
			returnFireAgainstAttackingSubs = ReturnFire.ALL;
		}
		else if (defendingSubsSneakAttack || isWW2V2())
		{
			returnFireAgainstAttackingSubs = ReturnFire.SUBS;
		}
		else
		{
			returnFireAgainstAttackingSubs = ReturnFire.NONE;
		}
		return returnFireAgainstAttackingSubs;
	}
	
	private ReturnFire returnFireAgainstDefendingSubs()
	{
		/** Attacker subs fire */
		/*calculate here, this holds for the fight round, but can't be computed later
		since destroyers may die*/
		final boolean attackingSubsSneakAttack = !Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer);
		final boolean defendingSubsSneakAttack = defendingSubsSneakAttack2();
		final ReturnFire returnFireAgainstDefendingSubs;
		if (!defendingSubsSneakAttack)
		{
			returnFireAgainstDefendingSubs = ReturnFire.ALL;
		}
		else if (attackingSubsSneakAttack || isWW2V2())
		{
			returnFireAgainstDefendingSubs = ReturnFire.SUBS;
		}
		else
		{
			returnFireAgainstDefendingSubs = ReturnFire.NONE;
		}
		return returnFireAgainstDefendingSubs;
	}
	
	private boolean defendingSubsSneakAttack2()
	{
		return (isWW2V2() || isDefendingSubsSneakAttack()) && !Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer);
	}
	
	/**
	 * @return
	 */
	private boolean canAttackerRetreatPlanes()
	{
		return (isWW2V2() || isAttackerRetreatPlanes() || isPartialAmphibiousRetreat()) && m_isAmphibious && Match.someMatch(m_attackingUnits, Matches.UnitIsAir);
	}
	
	/**
	 * @return
	 */
	private boolean canAttackerRetreatPartialAmphib()
	{
		if (m_isAmphibious && isPartialAmphibiousRetreat())
		{
			final List<Unit> landUnits = Match.getMatches(m_attackingUnits, Matches.UnitIsLand); // Only include land units when checking for allow amphibious retreat
			for (final Unit unit : landUnits)
			{
				final TripleAUnit taUnit = (TripleAUnit) unit;
				if (!taUnit.getWasAmphibious())
					return true;
			}
		}
		return false;
	}
	
	Collection<Territory> getAttackerRetreatTerritories()
	{
		// TODO: when attacking with paratroopers (air + carried land), there are several bugs in retreating.
		// TODO: air should always be able to retreat. paratrooped land units can only retreat if there are other non-paratrooper non-amphibious land units.
		// If attacker is all planes, just return collection of current
		// territory
		if (m_headless || Match.allMatch(m_attackingUnits, Matches.UnitIsAir))
		{
			final Collection<Territory> oneTerritory = new ArrayList<Territory>(2);
			oneTerritory.add(m_battleSite);
			return oneTerritory;
		}
		// its possible that a sub retreated to a territory we came from, if so we can no longer retreat there
		// or if we are moving out of a territory containing enemy units, we can not retreat back there
		final CompositeMatchAnd<Unit> enemyUnitsThatPreventRetreat = new CompositeMatchAnd<Unit>(Matches.enemyUnit(m_attacker, m_data), Matches.UnitIsNotInfrastructure, Matches
					.unitIsBeingTransported().invert(), Matches.unitIsNotSubmerged(m_data));
		if (games.strategy.triplea.Properties.getIgnoreSubInMovement(m_data))
			enemyUnitsThatPreventRetreat.add(Matches.UnitIsNotSub);
		if (games.strategy.triplea.Properties.getIgnoreTransportInMovement(m_data))
			enemyUnitsThatPreventRetreat.add(Matches.UnitIsNotTransportButCouldBeCombatTransport);
		Collection<Territory> possible = Match.getMatches(m_attackingFrom, Matches.territoryHasUnitsThatMatch(enemyUnitsThatPreventRetreat).invert());
		// In WW2V2 and WW2V3 we need to filter out territories where only planes
		// came from since planes cannot define retreat paths
		if (isWW2V2() || isWW2V3())
		{
			possible = Match.getMatches(possible, new Match<Territory>()
			{
				@Override
				public boolean match(final Territory t)
				{
					final Collection<Unit> units = m_attackingFromMap.get(t);
					return !Match.allMatch(units, Matches.UnitIsAir);
				}
			});
		}
		/*else
		{*/
		// the air unit may have come from a conquered or enemy territory, don't allow retreating
		final Match<Territory> conqueuredOrEnemy = new CompositeMatchOr<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(m_attacker, m_data),
					new CompositeMatchAnd<Territory>(
								// Matches.TerritoryIsLand,
								Matches.TerritoryIsWater, Matches.territoryWasFoughOver(m_battleTracker)));
		possible.removeAll(Match.getMatches(possible, conqueuredOrEnemy));
		// }
		// the battle site is in the attacking from
		// if sea units are fighting a submerged sub
		possible.remove(m_battleSite);
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsLand) && !m_battleSite.isWater())
			possible = Match.getMatches(possible, Matches.TerritoryIsLand);
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsSea))
			possible = Match.getMatches(possible, Matches.TerritoryIsWater);
		return possible;
	}
	
	private boolean canAttackerRetreat()
	{
		if (onlyDefenselessDefendingTransportsLeft()) // || onlyDefenselessAttackingTransportsLeft()
		{
			return false;
		}
		// if (m_amphibious && !isPartialAmphibiousRetreat())
		if (m_isAmphibious)
			return false;
		final Collection<Territory> options = getAttackerRetreatTerritories();
		if (options.size() == 0)
			return false;
		return true;
	}
	
	private boolean onlyDefenselessDefendingTransportsLeft()
	{
		if (!isTransportCasualtiesRestricted())
		{
			return false;
		}
		return Match.allMatch(m_defendingUnits, Matches.UnitIsTransportButNotCombatTransport);
	}
	
	/*private boolean onlyDefenselessAttackingTransportsLeft()
	{
		if (!isTransportCasualtiesRestricted())
		{
			return false;
		}
		return Match.allMatch(m_attackingUnits, Matches.UnitIsTransportButNotCombatTransport);
	}*/

	private boolean canAttackerRetreatSubs()
	{
		if (Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer))
			return false;
		if (Match.someMatch(m_defendingWaitingToDie, Matches.UnitIsDestroyer))
			return false;
		return canAttackerRetreat() || canSubsSubmerge();
	}
	
	// Added for test case calls
	void externalRetreat(final Collection<Unit> retreaters, final Territory retreatTo, final Boolean defender, final IDelegateBridge bridge)
	{
		m_isOver = true;
		retreatUnits(retreaters, retreatTo, defender, bridge);
	}
	
	private void attackerRetreat(final IDelegateBridge bridge)
	{
		if (!canAttackerRetreat())
			return;
		final Collection<Territory> possible = getAttackerRetreatTerritories();
		if (!m_isOver)
		{
			if (m_isAmphibious)
				queryRetreat(false, RetreatType.PARTIAL_AMPHIB, bridge, possible);
			else
				queryRetreat(false, RetreatType.DEFAULT, bridge, possible);
		}
	}
	
	private void attackerRetreatPlanes(final IDelegateBridge bridge)
	{
		// planes retreat to the same square the battle is in, and then should
		// move during non combat to their landing site, or be scrapped if they
		// can't find one.
		final Collection<Territory> possible = new ArrayList<Territory>(2);
		possible.add(m_battleSite);
		// retreat planes
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsAir))
			queryRetreat(false, RetreatType.PLANES, bridge, possible);
	}
	
	private void attackerRetreatNonAmphibUnits(final IDelegateBridge bridge)
	{
		final Collection<Territory> possible = getAttackerRetreatTerritories();
		queryRetreat(false, RetreatType.PARTIAL_AMPHIB, bridge, possible);
	}
	
	private boolean canDefenderRetreatSubs()
	{
		// if (m_headless)
		// return false;
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer))
			return false;
		if (Match.someMatch(m_attackingWaitingToDie, Matches.UnitIsDestroyer))
			return false;
		return getEmptyOrFriendlySeaNeighbors(m_defender, Match.getMatches(m_defendingUnits, Matches.UnitIsSub)).size() != 0 || canSubsSubmerge();
	}
	
	private void attackerRetreatSubs(final IDelegateBridge bridge)
	{
		if (!canAttackerRetreatSubs())
			return;
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
			queryRetreat(false, RetreatType.SUBS, bridge, getAttackerRetreatTerritories());
	}
	
	private void defenderRetreatSubs(final IDelegateBridge bridge)
	{
		if (!canDefenderRetreatSubs())
			return;
		if (!m_isOver && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
			queryRetreat(true, RetreatType.SUBS, bridge, getEmptyOrFriendlySeaNeighbors(m_defender, Match.getMatches(m_defendingUnits, Matches.UnitIsSub)));
	}
	
	private Collection<Territory> getEmptyOrFriendlySeaNeighbors(final PlayerID player, final Collection<Unit> unitsToRetreat)
	{
		Collection<Territory> possible = m_data.getMap().getNeighbors(m_battleSite);
		if (m_headless)
			return possible;
		final CompositeMatch<Territory> match = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, m_data));
		// make sure we can move through the any canals
		final Match<Territory> canalMatch = new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				final Route r = new Route();
				r.setStart(m_battleSite);
				r.add(t);
				if (MoveValidator.validateCanal(r, unitsToRetreat, m_defender, m_data) != null)
					return false;
				return true;
			}
		};
		match.add(canalMatch);
		possible = Match.getMatches(possible, match);
		return possible;
	}
	
	private void queryRetreat(final boolean defender, final RetreatType retreatType, final IDelegateBridge bridge, Collection<Territory> availableTerritories)
	{
		boolean subs;
		boolean planes;
		boolean partialAmphib;
		planes = retreatType == RetreatType.PLANES;
		subs = retreatType == RetreatType.SUBS;
		final boolean canSubsSubmerge = canSubsSubmerge();
		final boolean submerge = subs && canSubsSubmerge;
		final boolean canDefendingSubsSubmergeOrRetreat = subs && defender && games.strategy.triplea.Properties.getSubmarinesDefendingMaySubmergeOrRetreat(m_data);
		partialAmphib = retreatType == RetreatType.PARTIAL_AMPHIB;
		if (availableTerritories.isEmpty() && !(submerge || canDefendingSubsSubmergeOrRetreat))
			return;
		Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		if (subs)
		{
			units = Match.getMatches(units, Matches.UnitIsSub);
		}
		else if (planes)
		{
			units = Match.getMatches(units, Matches.UnitIsAir);
		}
		else if (partialAmphib)
		{
			units = Match.getMatches(units, Matches.UnitWasNotAmphibious);
		}
		if (Match.someMatch(units, Matches.UnitIsSea))
		{
			availableTerritories = Match.getMatches(availableTerritories, Matches.TerritoryIsWater);
		}
		if (canDefendingSubsSubmergeOrRetreat)
		{
			availableTerritories.add(m_battleSite);
		}
		else if (submerge)
		{
			availableTerritories.clear();
			availableTerritories.add(m_battleSite);
		}
		if (planes)
		{
			availableTerritories.clear();
			availableTerritories.add(m_battleSite);
		}
		if (units.size() == 0)
			return;
		final PlayerID retreatingPlayer = defender ? m_defender : m_attacker;
		String text;
		if (subs)
			text = retreatingPlayer.getName() + " retreat subs?";
		else if (planes)
			text = retreatingPlayer.getName() + RETREAT_PLANES;
		else if (partialAmphib)
			text = retreatingPlayer.getName() + " retreat non-amphibious units?";
		else
			text = retreatingPlayer.getName() + " retreat?";
		String step;
		if (defender)
		{
			step = m_defender.getName() + (canSubsSubmerge ? SUBS_SUBMERGE : SUBS_WITHDRAW);
		}
		else
		{
			if (subs)
				step = m_attacker.getName() + (canSubsSubmerge ? SUBS_SUBMERGE : SUBS_WITHDRAW);
			else if (planes)
				step = m_attacker.getName() + PLANES_WITHDRAW;
			else if (partialAmphib)
				step = m_attacker.getName() + NONAMPHIB_WITHDRAW;
			else
				step = m_attacker.getName() + ATTACKER_WITHDRAW;
		}
		getDisplay(bridge).gotoBattleStep(m_battleID, step);
		final Territory retreatTo = getRemote(retreatingPlayer, bridge).retreatQuery(m_battleID, (submerge || canDefendingSubsSubmergeOrRetreat), m_battleSite, availableTerritories, text);
		if (retreatTo != null && !availableTerritories.contains(retreatTo) && !subs)
		{
			System.err.println("Invalid retreat selection :" + retreatTo + " not in " + MyFormatter.defaultNamedToTextList(availableTerritories));
			Thread.dumpStack();
			return;
		}
		if (retreatTo != null)
		{
			// if attacker retreating non subs then its all over
			if (!defender && !subs && !planes && !partialAmphib)
			{
				// this is illegal in ww2v2 revised and beyond (the fighters should die). still checking if illegal in classic.
				// ensureAttackingAirCanRetreat(bridge);
				m_isOver = true;
			}
			if (subs && m_battleSite.equals(retreatTo) && (submerge || canDefendingSubsSubmergeOrRetreat))
			{
				if (!m_headless)
					ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_SUBMERGE, m_attacker.getName());
				submergeUnits(units, defender, bridge);
				final String messageShort = retreatingPlayer.getName() + " submerges subs";
				getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
			}
			else if (planes)
			{
				if (!m_headless)
					ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_AIR, m_attacker.getName());
				retreatPlanes(units, defender, bridge);
				final String messageShort = retreatingPlayer.getName() + " retreats planes";
				getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
			}
			else if (partialAmphib)
			{
				if (!m_headless)
				{
					if (Match.someMatch(units, Matches.UnitIsSea))
						ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_SEA, m_attacker.getName());
					else if (Match.someMatch(units, Matches.UnitIsLand))
						ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_LAND, m_attacker.getName());
					else
						ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_AIR, m_attacker.getName());
				}
				// remove amphib units from those retreating
				units = Match.getMatches(units, Matches.UnitWasNotAmphibious);
				retreatUnitsAndPlanes(units, retreatTo, defender, bridge);
				final String messageShort = retreatingPlayer.getName() + " retreats non-amphibious units";
				getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, retreatingPlayer);
			}
			else
			{
				if (!m_headless)
				{
					if (Match.someMatch(units, Matches.UnitIsSea))
						ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_SEA, m_attacker.getName());
					else if (Match.someMatch(units, Matches.UnitIsLand))
						ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_LAND, m_attacker.getName());
					else
						ClipPlayer.play(SoundPath.CLIP_BATTLE_RETREAT_AIR, m_attacker.getName());
				}
				retreatUnits(units, retreatTo, defender, bridge);
				final String messageShort = retreatingPlayer.getName() + " retreats";
				String messageLong;
				if (subs)
					messageLong = retreatingPlayer.getName() + " retreats subs to " + retreatTo.getName();
				else if (planes)
					messageLong = retreatingPlayer.getName() + " retreats planes to " + retreatTo.getName();
				else if (partialAmphib)
					messageLong = retreatingPlayer.getName() + " retreats non-amphibious units to " + retreatTo.getName();
				else
					messageLong = retreatingPlayer.getName() + " retreats all units to " + retreatTo.getName();
				getDisplay(bridge).notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
			}
		}
	}
	
	@Override
	public List<Unit> getRemainingAttackingUnits()
	{
		final ArrayList<Unit> remaining = new ArrayList<Unit>(m_attackingUnits);
		remaining.addAll(m_attackingUnitsRetreated);
		return remaining;
	}
	
	@Override
	public List<Unit> getRemainingDefendingUnits()
	{
		final ArrayList<Unit> remaining = new ArrayList<Unit>(m_defendingUnits);
		remaining.addAll(m_defendingUnitsRetreated);
		return remaining;
	}
	
	private Change retreatFromDependents(final Collection<Unit> units, final IDelegateBridge bridge, final Territory retreatTo, final Collection<IBattle> dependentBattles)
	{
		final CompositeChange change = new CompositeChange();
		for (final IBattle dependent : dependentBattles)
		{
			final Route route = new Route();
			route.setStart(m_battleSite);
			route.add(dependent.getTerritory());
			final Collection<Unit> retreatedUnits = dependent.getDependentUnits(units);
			dependent.removeAttack(route, retreatedUnits);
			reLoadTransports(units, change);
			change.add(ChangeFactory.moveUnits(dependent.getTerritory(), retreatTo, retreatedUnits));
		}
		return change;
	}
	
	// Retreat landed units from allied territory when their transport retreats
	private Change retreatFromNonCombat(Collection<Unit> units, final IDelegateBridge bridge, final Territory retreatTo)
	{
		final CompositeChange change = new CompositeChange();
		units = Match.getMatches(units, Matches.UnitIsTransport);
		final Collection<Unit> retreated = getTransportDependents(units, m_data);
		if (!retreated.isEmpty())
		{
			Territory retreatedFrom = null;
			for (final Unit unit : units)
			{
				retreatedFrom = getTransportTracker().getTerritoryTransportHasUnloadedTo(unit);
				if (retreatedFrom != null)
				{
					reLoadTransports(units, change);
					change.add(ChangeFactory.moveUnits(retreatedFrom, retreatTo, retreated));
				}
			}
		}
		return change;
	}
	
	public void reLoadTransports(final Collection<Unit> units, final CompositeChange change)
	{
		final Collection<Unit> transports = Match.getMatches(units, Matches.UnitCanTransport);
		// Put units back on their transports
		for (final Unit transport : transports)
		{
			final Collection<Unit> unloaded = getTransportTracker().unloaded(transport);
			for (final Unit load : unloaded)
			{
				final Change loadChange = getTransportTracker().loadTransportChange((TripleAUnit) transport, load, m_attacker);
				change.add(loadChange);
			}
		}
	}
	
	private void retreatPlanes(final Collection<Unit> retreating, final boolean defender, final IDelegateBridge bridge)
	{
		final String transcriptText = MyFormatter.unitsToText(retreating) + " retreated";
		final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		final Collection<Unit> unitsRetreated = defender ? m_defendingUnitsRetreated : m_attackingUnitsRetreated;
		/** @todo Does this need to happen with planes retreating too? */
		units.removeAll(retreating);
		unitsRetreated.removeAll(retreating);
		if (units.isEmpty() || m_isOver)
		{
			endBattle(bridge);
			if (defender)
				attackerWins(bridge);
			else
				defenderWins(bridge);
		}
		else
		{
			getDisplay(bridge).notifyRetreat(m_battleID, retreating);
		}
		bridge.getHistoryWriter().addChildToEvent(transcriptText, retreating);
	}
	
	private void submergeUnits(final Collection<Unit> submerging, final boolean defender, final IDelegateBridge bridge)
	{
		final String transcriptText = MyFormatter.unitsToText(submerging) + " Submerged";
		final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		final Collection<Unit> unitsRetreated = defender ? m_defendingUnitsRetreated : m_attackingUnitsRetreated;
		final CompositeChange change = new CompositeChange();
		for (final Unit u : submerging)
		{
			change.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.SUBMERGED));
		}
		bridge.addChange(change);
		units.removeAll(submerging);
		unitsRetreated.addAll(submerging);
		if (!units.isEmpty() && !m_isOver)
		{
			getDisplay(bridge).notifyRetreat(m_battleID, submerging);
		}
		bridge.getHistoryWriter().addChildToEvent(transcriptText, submerging);
	}
	
	private void retreatUnits(Collection<Unit> retreating, final Territory to, final boolean defender, final IDelegateBridge bridge)
	{
		retreating.addAll(getDependentUnits(retreating));
		// our own air units dont retreat with land units
		final Match<Unit> notMyAir = new CompositeMatchOr<Unit>(Matches.UnitIsNotAir, new InverseMatch<Unit>(Matches.unitIsOwnedBy(m_attacker)));
		retreating = Match.getMatches(retreating, notMyAir);
		String transcriptText;
		// in WW2V1, defending subs can retreat so show owner
		if (isWW2V2())
			transcriptText = MyFormatter.unitsToTextNoOwner(retreating) + " retreated to " + to.getName();
		else
			transcriptText = MyFormatter.unitsToText(retreating) + " retreated to " + to.getName();
		bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<Unit>(retreating));
		final CompositeChange change = new CompositeChange();
		change.add(ChangeFactory.moveUnits(m_battleSite, to, retreating));
		if (m_isOver)
		{
			final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(this);
			// If there are no dependent battles, check landings in allied territories
			if (dependentBattles.isEmpty())
				change.add(retreatFromNonCombat(retreating, bridge, to));
			// Else retreat the units from combat when their transport retreats
			else
				change.add(retreatFromDependents(retreating, bridge, to, dependentBattles));
		}
		bridge.addChange(change);
		final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		final Collection<Unit> unitsRetreated = defender ? m_defendingUnitsRetreated : m_attackingUnitsRetreated;
		units.removeAll(retreating);
		unitsRetreated.addAll(retreating);
		if (units.isEmpty() || m_isOver)
		{
			endBattle(bridge);
			if (defender)
				attackerWins(bridge);
			else
				defenderWins(bridge);
		}
		else
		{
			getDisplay(bridge).notifyRetreat(m_battleID, retreating);
		}
	}
	
	private void retreatUnitsAndPlanes(final Collection<Unit> retreating, final Territory to, final boolean defender, final IDelegateBridge bridge)
	{
		// Remove air from battle
		final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
		final Collection<Unit> unitsRetreated = defender ? m_defendingUnitsRetreated : m_attackingUnitsRetreated;
		units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		// add all land units' dependents
		// retreating.addAll(getDependentUnits(retreating));
		retreating.addAll(getDependentUnits(units));
		// our own air units dont retreat with land units
		final Match<Unit> notMyAir = new CompositeMatchOr<Unit>(Matches.UnitIsNotAir, new InverseMatch<Unit>(Matches.unitIsOwnedBy(m_attacker)));
		final Collection<Unit> nonAirRetreating = Match.getMatches(retreating, notMyAir);
		final String transcriptText = MyFormatter.unitsToTextNoOwner(nonAirRetreating) + " retreated to " + to.getName();
		bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<Unit>(nonAirRetreating));
		final CompositeChange change = new CompositeChange();
		change.add(ChangeFactory.moveUnits(m_battleSite, to, nonAirRetreating));
		if (m_isOver)
		{
			final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(this);
			// If there are no dependent battles, check landings in allied territories
			if (dependentBattles.isEmpty())
				change.add(retreatFromNonCombat(nonAirRetreating, bridge, to));
			// Else retreat the units from combat when their transport retreats
			else
				change.add(retreatFromDependents(nonAirRetreating, bridge, to, dependentBattles));
		}
		bridge.addChange(change);
		units.removeAll(nonAirRetreating);
		unitsRetreated.addAll(nonAirRetreating);
		if (units.isEmpty() || m_isOver)
		{
			endBattle(bridge);
			if (defender)
				attackerWins(bridge);
			else
				defenderWins(bridge);
		}
		else
		{
			getDisplay(bridge).notifyRetreat(m_battleID, retreating);
		}
	}
	
	private void fire(final String stepName, final Collection<Unit> firingUnits, final Collection<Unit> attackableUnits, final boolean defender, final ReturnFire returnFire,
				final IDelegateBridge bridge, final String text)
	{
		final PlayerID firing = defender ? m_defender : m_attacker;
		final PlayerID defending = !defender ? m_defender : m_attacker;
		if (firingUnits.isEmpty())
		{
			return;
		}
		m_stack.push(new Fire(attackableUnits, returnFire, firing, defending, firingUnits, stepName, text, this, defender, m_dependentUnits, m_stack, m_headless, m_territoryEffects));
	}
	
	/**
	 * Check for suicide units and kill them immediately (they get to shoot back, which is the point)
	 * 
	 * @param bridge
	 * @param player
	 * @param defender
	 */
	private void checkSuicideUnits(final IDelegateBridge bridge)
	{
		if (isDefendingSuicideAndMunitionUnitsDoNotFire())
		{
			remove(Match.getMatches(m_battleSite.getUnits().getUnits(), new CompositeMatchAnd<Unit>(Matches.UnitIsSuicide, Matches.unitIsOwnedBy(m_attacker))), bridge, m_battleSite, false);
			getDisplay(bridge).deadUnitNotification(m_battleID, m_attacker, Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide), m_dependentUnits);
			m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide));
		}
		else
		{
			remove(Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsSuicide), bridge, m_battleSite, false);
			// and remove them from the battle display
			getDisplay(bridge).deadUnitNotification(m_battleID, m_attacker, Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide), m_dependentUnits);
			getDisplay(bridge).deadUnitNotification(m_battleID, m_defender, Match.getMatches(m_defendingUnits, Matches.UnitIsSuicide), m_dependentUnits);
			// and remove them from the map display
			m_defendingUnits.removeAll(Match.getMatches(m_defendingUnits, Matches.UnitIsSuicide));
			m_attackingUnits.removeAll(Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide));
		}
	}
	
	/**
	 * Check for unescorted TRNS and kill them immediately
	 * 
	 * @param bridge
	 * @param player
	 * @param defender
	 */
	private void checkUndefendedTransports(final IDelegateBridge bridge, final PlayerID player)
	{
		// if we are the attacker, we can retreat instead of dying
		if (player.equals(m_attacker) && (!getAttackerRetreatTerritories().isEmpty() || Match.someMatch(m_attackingUnits, Matches.UnitIsAir)))
			return;
		// Get all allied transports in the territory
		final CompositeMatch<Unit> matchAllied = new CompositeMatchAnd<Unit>();
		matchAllied.add(Matches.UnitIsTransport);
		matchAllied.add(Matches.UnitIsNotCombatTransport);
		matchAllied.add(Matches.isUnitAllied(player, m_data));
		matchAllied.add(Matches.UnitIsSea);
		final List<Unit> alliedTransports = Match.getMatches(m_battleSite.getUnits().getUnits(), matchAllied);
		// If no transports, just return
		if (alliedTransports.isEmpty())
			return;
		// Get all ALLIED, sea & air units in the territory (that are NOT submerged)
		final CompositeMatch<Unit> alliedUnitsMatch = new CompositeMatchAnd<Unit>();
		alliedUnitsMatch.add(Matches.isUnitAllied(player, m_data));
		alliedUnitsMatch.add(Matches.UnitIsNotLand);
		alliedUnitsMatch.add(new InverseMatch<Unit>(Matches.unitIsSubmerged(m_data)));
		final Collection<Unit> alliedUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), alliedUnitsMatch);
		// If transports are unescorted, check opposing forces to see if the Trns die automatically
		if (alliedTransports.size() == alliedUnits.size())
		{
			// Get all the ENEMY sea and air units (that can attack) in the territory
			final CompositeMatch<Unit> enemyUnitsMatch = new CompositeMatchAnd<Unit>();
			enemyUnitsMatch.add(Matches.UnitIsNotLand);
			// enemyUnitsMatch.add(Matches.UnitIsNotTransportButCouldBeCombatTransport);
			enemyUnitsMatch.add(Matches.unitIsNotSubmerged(m_data));
			enemyUnitsMatch.add(Matches.unitCanAttack(player));
			final Collection<Unit> enemyUnits = Match.getMatches(m_battleSite.getUnits().getUnits(), enemyUnitsMatch);
			// If there are attackers set their movement to 0 and kill the transports
			if (enemyUnits.size() > 0)
			{
				final Change change = ChangeFactory.markNoMovementChange(Match.getMatches(enemyUnits, Matches.UnitIsSea));
				bridge.addChange(change);
				remove(alliedTransports, bridge, m_battleSite, false);
				// and remove them from the battle display
				if (player.equals(m_defender))
					m_defendingUnits.removeAll(alliedTransports);
				else
					m_attackingUnits.removeAll(alliedTransports);
			}
		}
	}
	
	private void checkForUnitsThatCanRollLeft(final IDelegateBridge bridge, final boolean attacker)
	{
		// if we are the attacker, we can retreat instead of dying
		if (attacker && (!getAttackerRetreatTerritories().isEmpty() || Match.someMatch(m_attackingUnits, Matches.UnitIsAir)))
			return;
		if (m_attackingUnits.isEmpty() || m_defendingUnits.isEmpty())
			return;
		final CompositeMatch<Unit> notSubmergedAndType = new CompositeMatchAnd<Unit>(Matches.unitIsNotSubmerged(m_data));
		if (Matches.TerritoryIsLand.match(m_battleSite))
			notSubmergedAndType.add(Matches.UnitIsSea.invert());
		else
			notSubmergedAndType.add(Matches.UnitIsLand.invert());
		final Collection<Unit> unitsToKill;
		final boolean hasUnitsThatCanRollLeft;
		if (attacker)
		{
			hasUnitsThatCanRollLeft = Match.someMatch(m_attackingUnits, new CompositeMatchAnd<Unit>(notSubmergedAndType, Matches.UnitIsSupporterOrHasCombatAbility(attacker, m_data)));
			unitsToKill = Match.getMatches(m_attackingUnits, new CompositeMatchAnd<Unit>(notSubmergedAndType, Matches.UnitIsNotInfrastructure));
		}
		else
		{
			hasUnitsThatCanRollLeft = Match.someMatch(m_defendingUnits, new CompositeMatchAnd<Unit>(notSubmergedAndType, Matches.UnitIsSupporterOrHasCombatAbility(attacker, m_data)));
			unitsToKill = Match.getMatches(m_defendingUnits, new CompositeMatchAnd<Unit>(notSubmergedAndType, Matches.UnitIsNotInfrastructure));
		}
		final boolean enemy = !attacker;
		final boolean enemyHasUnitsThatCanRollLeft;
		if (enemy)
			enemyHasUnitsThatCanRollLeft = Match.someMatch(m_attackingUnits, new CompositeMatchAnd<Unit>(notSubmergedAndType, Matches.UnitIsSupporterOrHasCombatAbility(enemy, m_data)));
		else
			enemyHasUnitsThatCanRollLeft = Match.someMatch(m_defendingUnits, new CompositeMatchAnd<Unit>(notSubmergedAndType, Matches.UnitIsSupporterOrHasCombatAbility(enemy, m_data)));
		if (!hasUnitsThatCanRollLeft && enemyHasUnitsThatCanRollLeft)
		{
			// final Change change = ChangeFactory.markNoMovementChange(Match.getMatches(enemyUnits, Matches.UnitIsSea)); // I don't think this is needed, they should not have any movement left anyway
			// bridge.addChange(change);
			remove(unitsToKill, bridge, m_battleSite, false);
			// and remove them from the battle display
			if (attacker)
				m_attackingUnits.removeAll(unitsToKill);
			else
				m_defendingUnits.removeAll(unitsToKill);
		}
	}
	
	/**
	 * Submerge attacking/defending SUBS if they're alone OR with TRNS against only AIRCRAFT
	 * 
	 * @param bridge
	 * @param player
	 * @param defender
	 */
	private void submergeSubsVsOnlyAir(final IDelegateBridge bridge)
	{
		// if All attackers are AIR submerge any defending subs ..m_defendingUnits.removeAll(m_killed);
		if (Match.allMatch(m_attackingUnits, Matches.UnitIsAir) && Match.someMatch(m_defendingUnits, Matches.UnitIsSub))
		{
			// Get all defending subs (including allies) in the territory.
			/* If the unit is in the m_defendingUnits list, it must be either an ally or an enemy of the attacker, either way we should submerge.
			final CompositeMatch<Unit> matchDefendingSubs = new CompositeMatchAnd<Unit>();
			matchDefendingSubs.add(Matches.UnitIsSub);
			matchDefendingSubs.add(Matches.isUnitAllied(m_defender, m_data));*/
			final List<Unit> defendingSubs = Match.getMatches(m_defendingUnits, Matches.UnitIsSub);
			// submerge defending subs
			submergeUnits(defendingSubs, true, bridge);
			// getDisplay(bridge).notifyRetreat(messageShort, messageShort, step, m_defender);
		} // checking defending air on attacking subs
		else if (Match.allMatch(m_defendingUnits, Matches.UnitIsAir) && Match.someMatch(m_attackingUnits, Matches.UnitIsSub))
		{
			// Get all attacking subs in the territory
			/* it doesn't make sense that m_attackingUnits would be filled with any units that are not directly owned by (or dependent of units owned by) the attacker.
			final CompositeMatch<Unit> matchAttackingSubs = new CompositeMatchAnd<Unit>();
			matchAttackingSubs.add(Matches.UnitIsSub);
			matchAttackingSubs.add(Matches.isUnitAllied(m_attacker, m_data));*/
			final List<Unit> attackingSubs = Match.getMatches(m_attackingUnits, Matches.UnitIsSub);
			// submerge attacking subs
			submergeUnits(attackingSubs, false, bridge);
		}
	}
	
	private void defendNonSubs(final IDelegateBridge bridge)
	{
		if (m_attackingUnits.size() == 0)
			return;
		Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
		units.addAll(m_defendingUnits);
		units.addAll(m_defendingWaitingToDie);
		units = Match.getMatches(units, Matches.UnitIsNotSub);
		// if restricted, remove aircraft from attackers
		if (isAirAttackSubRestricted() && !canAirAttackSubs(m_attackingUnits, units))
		{
			units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		}
		if (units.isEmpty())
			return;
		fire(m_attacker.getName() + SELECT_CASUALTIES, units, m_attackingUnits, true, ReturnFire.ALL, bridge, "Defenders fire, ");
	}
	
	// If there are no attacking DDs but defending SUBs, fire AIR at non-SUB forces ONLY
	private void attackAirOnNonSubs(final IDelegateBridge bridge)
	{
		if (m_defendingUnits.size() == 0)
			return;
		Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size() + m_attackingWaitingToDie.size());
		units.addAll(m_attackingUnits);
		units.addAll(m_attackingWaitingToDie);
		// See if allied air can participate in combat
		if (isAlliedAirDependents())
			units = Match.getMatches(units, Matches.unitIsOwnedBy(m_attacker));
		if (!canAirAttackSubs(m_defendingUnits, units))
		{
			units = Match.getMatches(units, Matches.UnitIsAir);
			final Collection<Unit> enemyUnitsNotSubs = Match.getMatches(m_defendingUnits, Matches.UnitIsNotSub);
			fire(m_defender.getName() + SELECT_CASUALTIES, units, enemyUnitsNotSubs, false, ReturnFire.ALL, bridge, "Attacker's aircraft fire,");
		}
	}
	
	private boolean canAirAttackSubs(final Collection<Unit> firedAt, final Collection<Unit> firing)
	{
		if (m_battleSite.isWater() && Match.someMatch(firedAt, Matches.UnitIsSub) && Match.noneMatch(firing, Matches.UnitIsDestroyer))
		{
			return false;
		}
		return true;
	}
	
	private void defendAirOnNonSubs(final IDelegateBridge bridge)
	{
		if (m_attackingUnits.size() == 0)
			return;
		Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
		units.addAll(m_defendingUnits);
		units.addAll(m_defendingWaitingToDie);
		// units = Match.getMatches(units, Matches.unitIsOwnedBy(m_defender)); //why is this here? allied air units can still shoot!
		if (!canAirAttackSubs(m_attackingUnits, units))
		{
			units = Match.getMatches(units, Matches.UnitIsAir);
			final Collection<Unit> enemyUnitsNotSubs = Match.getMatches(m_attackingUnits, Matches.UnitIsNotSub);
			if (enemyUnitsNotSubs.isEmpty())
				return;
			fire(m_defender.getName() + SELECT_CASUALTIES, units, enemyUnitsNotSubs, true, ReturnFire.ALL, bridge, "Defender's aircraft fire,");
		}
	}
	
	// If there are no attacking DDs, but defending SUBs, remove attacking AIR as they've already fired- otherwise fire all attackers.
	private void attackNonSubs(final IDelegateBridge bridge)
	{
		if (m_defendingUnits.size() == 0)
			return;
		Collection<Unit> units = Match.getMatches(m_attackingUnits, Matches.UnitIsNotSub);
		units.addAll(Match.getMatches(m_attackingWaitingToDie, Matches.UnitIsNotSub));
		// See if allied air can participate in combat
		if (isAlliedAirDependents())
			units = Match.getMatches(units, Matches.unitIsOwnedBy(m_attacker));
		// if restricted, remove aircraft from attackers
		if (isAirAttackSubRestricted() && !canAirAttackSubs(m_defendingUnits, units))
		{
			units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		}
		if (units.isEmpty())
			return;
		fire(m_defender.getName() + SELECT_CASUALTIES, units, m_defendingUnits, false, ReturnFire.ALL, bridge, "Attackers fire,");
	}
	
	private void attackSubs(final IDelegateBridge bridge, final ReturnFire returnFire)
	{
		final Collection<Unit> firing = Match.getMatches(m_attackingUnits, Matches.UnitIsSub);
		if (firing.isEmpty())
			return;
		final Collection<Unit> attacked = Match.getMatches(m_defendingUnits, Matches.UnitIsNotAir);
		// if there are destroyers in the attacked units, we can return fire.
		fire(m_defender.getName() + SELECT_SUB_CASUALTIES, firing, attacked, false, returnFire, bridge, "Subs fire,");
	}
	
	private void defendSubs(final IDelegateBridge bridge, final ReturnFire returnFire)
	{
		if (m_attackingUnits.size() == 0)
			return;
		Collection<Unit> firing = new ArrayList<Unit>(m_defendingUnits.size() + m_defendingWaitingToDie.size());
		firing.addAll(m_defendingUnits);
		firing.addAll(m_defendingWaitingToDie);
		firing = Match.getMatches(firing, Matches.UnitIsSub);
		if (firing.isEmpty())
			return;
		final Collection<Unit> attacked = Match.getMatches(m_attackingUnits, Matches.UnitIsNotAir);
		if (attacked.isEmpty())
			return;
		fire(m_attacker.getName() + SELECT_SUB_CASUALTIES, firing, attacked, true, returnFire, bridge, "Subs defend, ");
	}
	
	/*
	private void attackAny(IDelegateBridge bridge)
	{
		if (m_defendingUnits.size() == 0)
			return;
		
		Collection<Unit> units = new ArrayList<Unit>(m_attackingUnits.size()
					+ m_attackingWaitingToDie.size());
		units.addAll(m_attackingUnits);
		units.addAll(m_attackingWaitingToDie);
		
		if (isAirAttackSubRestricted() && !canAirAttackSubs(m_defendingUnits, units))
		{
			units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		}
		
		if (units.isEmpty())
			return;
		
		fire(m_defender.getName() + SELECT_CASUALTIES, units,
					m_defendingUnits, false, ReturnFire.ALL, bridge, "Attackers fire,");
	}
	*/
	/*
	private void defendAny(IDelegateBridge bridge)
	{
		
		if (m_attackingUnits.size() == 0)
			return;
		
		Collection<Unit> units = new ArrayList<Unit>(m_defendingUnits.size()
					+ m_defendingWaitingToDie.size());
		units.addAll(m_defendingUnits);
		units.addAll(m_defendingWaitingToDie);
		// if restricted, remove aircraft from attackers
		
		if (isAirAttackSubRestricted() && !canAirAttackSubs(m_attackingUnits, units))
		{
			units.removeAll(Match.getMatches(units, Matches.UnitIsAir));
		}
		
		if (units.isEmpty())
			return;
		
		fire(m_attacker.getName() + SELECT_CASUALTIES, units,
					m_attackingUnits, true, ReturnFire.ALL, bridge, "Defenders fire, ");
	}
	*/
	void removeCasualties(final Collection<Unit> killed, final ReturnFire returnFire, final boolean defender, final IDelegateBridge bridge, final boolean isAA)
	{
		if (killed.isEmpty())
			return;
		if (returnFire == ReturnFire.ALL)
		{
			// move to waiting to die
			if (defender)
				m_defendingWaitingToDie.addAll(killed);
			else
				m_attackingWaitingToDie.addAll(killed);
		}
		else if (returnFire == ReturnFire.SUBS)
		{
			// move to waiting to die
			if (defender)
				m_defendingWaitingToDie.addAll(Match.getMatches(killed, Matches.UnitIsSub));
			else
				m_attackingWaitingToDie.addAll(Match.getMatches(killed, Matches.UnitIsSub));
			remove(Match.getMatches(killed, Matches.UnitIsNotSub), bridge, m_battleSite, isAA);
		}
		else if (returnFire == ReturnFire.NONE)
		{
			remove(killed, bridge, m_battleSite, isAA);
		}
		// remove from the active fighting
		if (defender)
			m_defendingUnits.removeAll(killed);
		else
			m_attackingUnits.removeAll(killed);
	}
	
	private void fireNavalBombardment(final IDelegateBridge bridge)
	{
		// TODO - check within the method for the bombarding limitations
		final Collection<Unit> bombard = getBombardingUnits();
		final Collection<Unit> attacked = Match.getMatches(m_defendingUnits, Matches.UnitIsNotInfrastructureAndNotCapturedOnEntering(m_attacker, m_battleSite, m_data));
		// bombarding units cant move after bombarding
		if (!m_headless)
		{
			final Change change = ChangeFactory.markNoMovementChange(bombard);
			bridge.addChange(change);
		}
		// TODO
		/**
		 * This code is actually a bug- the property is intended to tell if the return fire is
		 * RESTRICTED- but it's used as if it's ALLOWED. The reason is the default values on the
		 * property definition. However, fixing this will entail a fix to the XML to reverse
		 * all values. We'll leave it as is for now and try to figure out a patch strategy later.
		 */
		final boolean canReturnFire = (isNavalBombardCasualtiesReturnFire());
		if (bombard.size() > 0 && attacked.size() > 0)
		{
			if (!m_headless)
				ClipPlayer.play(SoundPath.CLIP_BATTLE_BOMBARD, m_attacker.getName());
			fire(SELECT_NAVAL_BOMBARDMENT_CASUALTIES, bombard, attacked, false, canReturnFire ? ReturnFire.ALL : ReturnFire.NONE, bridge, "Bombard");
		}
	}
	
	private void fireSuicideUnitsAttack(final IDelegateBridge bridge)
	{
		// TODO: add a global toggle for returning fire (Veqryn)
		final CompositeMatch<Unit> attackableUnits = new CompositeMatchAnd<Unit>(Matches.UnitIsNotInfrastructureAndNotCapturedOnEntering(m_attacker, m_battleSite, m_data),
					Matches.UnitIsSuicide.invert(), Matches.unitIsBeingTransported().invert());
		final Collection<Unit> suicideAttackers = Match.getMatches(m_attackingUnits, Matches.UnitIsSuicide);
		final Collection<Unit> attackedDefenders = Match.getMatches(m_defendingUnits, attackableUnits);
		// comparatively simple rules for isSuicide units. if AirAttackSubRestricted and you have no destroyers, you can't attack subs with anything.
		if (isAirAttackSubRestricted() && !Match.someMatch(m_attackingUnits, Matches.UnitIsDestroyer) && Match.someMatch(attackedDefenders, Matches.UnitIsSub))
			attackedDefenders.removeAll(Match.getMatches(attackedDefenders, Matches.UnitIsSub));
		if (Match.allMatch(suicideAttackers, Matches.UnitIsSub))
			attackedDefenders.removeAll(Match.getMatches(attackedDefenders, Matches.UnitIsAir));
		if (suicideAttackers.size() == 0 || attackedDefenders.size() == 0)
			return;
		final boolean canReturnFire = (!isSuicideAndMunitionCasualtiesRestricted());
		fire(m_defender.getName() + SELECT_CASUALTIES_SUICIDE, suicideAttackers, attackedDefenders, false, canReturnFire ? ReturnFire.ALL : ReturnFire.NONE, bridge, SUICIDE_ATTACK);
	}
	
	private void fireSuicideUnitsDefend(final IDelegateBridge bridge)
	{
		if (isDefendingSuicideAndMunitionUnitsDoNotFire())
			return;
		// TODO: add a global toggle for returning fire (Veqryn)
		final CompositeMatch<Unit> attackableUnits = new CompositeMatchAnd<Unit>(Matches.UnitIsNotInfrastructure,
					Matches.UnitIsSuicide.invert(), Matches.unitIsBeingTransported().invert());
		final Collection<Unit> suicideDefenders = Match.getMatches(m_defendingUnits, Matches.UnitIsSuicide);
		final Collection<Unit> attackedAttackers = Match.getMatches(m_attackingUnits, attackableUnits);
		// comparatively simple rules for isSuicide units. if AirAttackSubRestricted and you have no destroyers, you can't attack subs with anything.
		if (isAirAttackSubRestricted() && !Match.someMatch(m_defendingUnits, Matches.UnitIsDestroyer) && Match.someMatch(attackedAttackers, Matches.UnitIsSub))
			attackedAttackers.removeAll(Match.getMatches(attackedAttackers, Matches.UnitIsSub));
		if (Match.allMatch(suicideDefenders, Matches.UnitIsSub))
			suicideDefenders.removeAll(Match.getMatches(suicideDefenders, Matches.UnitIsAir));
		if (suicideDefenders.size() == 0 || attackedAttackers.size() == 0)
			return;
		final boolean canReturnFire = (!isSuicideAndMunitionCasualtiesRestricted());
		fire(m_attacker.getName() + SELECT_CASUALTIES_SUICIDE, suicideDefenders, attackedAttackers, true, canReturnFire ? ReturnFire.ALL : ReturnFire.NONE, bridge, SUICIDE_DEFEND);
	}
	
	/**
	 * @return
	 */
	private boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(m_data);
	}
	
	private boolean isWW2V3()
	{
		return games.strategy.triplea.Properties.getWW2V3(m_data);
	}
	
	private boolean isPartialAmphibiousRetreat()
	{
		return games.strategy.triplea.Properties.getPartialAmphibiousRetreat(m_data);
	}
	
	private boolean isParatroopers(final PlayerID player)
	{
		final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.getParatroopers();
	}
	
	/**
	 * @return
	 */
	private boolean isAlliedAirDependents()
	{
		return games.strategy.triplea.Properties.getAlliedAirDependents(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isDefendingSubsSneakAttack()
	{
		return games.strategy.triplea.Properties.getDefendingSubsSneakAttack(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isAttackerRetreatPlanes()
	{
		return games.strategy.triplea.Properties.getAttackerRetreatPlanes(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isNavalBombardCasualtiesReturnFire()
	{
		return games.strategy.triplea.Properties.getNavalBombardCasualtiesReturnFireRestricted(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isSuicideAndMunitionCasualtiesRestricted()
	{
		return games.strategy.triplea.Properties.getSuicideAndMunitionCasualtiesRestricted(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isDefendingSuicideAndMunitionUnitsDoNotFire()
	{
		return games.strategy.triplea.Properties.getDefendingSuicideAndMunitionUnitsDoNotFire(m_data);
	}
	
	/*
	private boolean isSurvivingAirMoveToLand()
	{
		return games.strategy.triplea.Properties.getSurvivingAirMoveToLand(m_data);
	}
	 */

	/**
	 * @return
	 */
	private boolean isAirAttackSubRestricted()
	{
		return games.strategy.triplea.Properties.getAirAttackSubRestricted(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isSubRetreatBeforeBattle()
	{
		return games.strategy.triplea.Properties.getSubRetreatBeforeBattle(m_data);
	}
	
	/**
	 * @return
	 */
	private boolean isTransportCasualtiesRestricted()
	{
		return games.strategy.triplea.Properties.getTransportCasualtiesRestricted(m_data);
	}
	
	/**
	 * @return territories where there are amphibious attacks
	 */
	public Collection<Territory> getAmphibiousAttackTerritories()
	{
		return m_amphibiousAttackFrom;
	}
	
	private void fireAAGuns(final IDelegateBridge bridge)
	{
		m_stack.push(new FireAA());
	}
	
	
	class FireAA implements IExecutable
	{
		private static final long serialVersionUID = -6406659798754841382L;
		private DiceRoll m_dice;
		private CasualtyDetails m_casualties;
		Collection<Unit> m_casualtiesSoFar = new ArrayList<Unit>();
		
		// private List<Unit> m_hitUnits = new ArrayList<Unit>();
		public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
		{
			if (!canFireAA())
				return;
			for (final String currentTypeAA : m_AAtypes)
			{
				final Collection<Unit> currentPossibleAA = Match.getMatches(m_defendingAA, Matches.UnitIsAAofTypeAA(currentTypeAA));
				final Set<UnitType> targetUnitTypesForThisTypeAA = UnitAttachment.get(currentPossibleAA.iterator().next().getType()).getTargetsAA(m_data);
				final Set<UnitType> airborneTypesTargettedToo = TechAbilityAttachment.getAirborneTargettedByAA(m_attacker, m_data).get(currentTypeAA);
				final Collection<Unit> validAttackingUnitsForThisRoll = Match.getMatches(m_attackingUnits, new CompositeMatchOr<Unit>(Matches.unitIsOfTypes(targetUnitTypesForThisTypeAA),
							new CompositeMatchAnd<Unit>(Matches.UnitIsAirborne, Matches.unitIsOfTypes(airborneTypesTargettedToo))));
				
				final IExecutable rollDice = new IExecutable()
				{
					private static final long serialVersionUID = 6435935558879109347L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						validAttackingUnitsForThisRoll.removeAll(m_casualtiesSoFar);
						if (!validAttackingUnitsForThisRoll.isEmpty())
						{
							m_dice = DiceRoll.rollAA(validAttackingUnitsForThisRoll, currentPossibleAA, bridge, m_battleSite);
							if (!m_headless)
						{
							if (currentTypeAA.equals("AA"))
							{
								if (m_dice.getHits() > 0)
									ClipPlayer.play(SoundPath.CLIP_BATTLE_AA_HIT, m_defender.getName());
								else
									ClipPlayer.play(SoundPath.CLIP_BATTLE_AA_MISS, m_defender.getName());
							}
							else
							{
								if (m_dice.getHits() > 0)
									ClipPlayer.play(SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAA.toLowerCase() + SoundPath.CLIP_BATTLE_X_HIT, m_defender.getName());
								else
									ClipPlayer.play(SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAA.toLowerCase() + SoundPath.CLIP_BATTLE_X_MISS, m_defender.getName());
							}
						}
					}
				}
				};
				final IExecutable selectCasualties = new IExecutable()
				{
					private static final long serialVersionUID = 7943295620796835166L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						if (!validAttackingUnitsForThisRoll.isEmpty())
						{
							final CasualtyDetails details = selectCasualties(validAttackingUnitsForThisRoll, currentPossibleAA, bridge, currentTypeAA);
							markDamaged(details.getDamaged(), bridge);
							m_casualties = details;
							m_casualtiesSoFar.addAll(details.getKilled());
						}
					}
				};
				final IExecutable notifyCasualties = new IExecutable()
				{
					private static final long serialVersionUID = -6759782085212899725L;
					
					public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
					{
						if (!validAttackingUnitsForThisRoll.isEmpty())
						{
							notifyCasualtiesAA(bridge, currentTypeAA);
							removeCasualties(m_casualties.getKilled(), ReturnFire.NONE, false, bridge, true);
						}
					}
				};
				// push in reverse order of execution
				stack.push(notifyCasualties);
				stack.push(selectCasualties);
				stack.push(rollDice);
			}
		}
		
		private CasualtyDetails selectCasualties(final Collection<Unit> validAttackingUnitsForThisRoll, final Collection<Unit> defendingAA, final IDelegateBridge bridge, final String currentTypeAA)
		{
			// send defender the dice roll so he can see what the dice are while he waits for attacker to select casualties
			getDisplay(bridge).notifyDice(m_battleID, m_dice, SELECT_PREFIX + currentTypeAA + CASUALTIES_SUFFIX);
			return BattleCalculator.getAACasualties(validAttackingUnitsForThisRoll, defendingAA, m_dice, bridge, m_defender, m_attacker, m_battleID, m_battleSite);
		}
		
		private void notifyCasualtiesAA(final IDelegateBridge bridge, final String currentTypeAA)
		{
			if (m_headless)
				return;
			getDisplay(bridge).casualtyNotification(m_battleID, REMOVE_PREFIX + currentTypeAA + CASUALTIES_SUFFIX, m_dice, m_attacker, new ArrayList<Unit>(m_casualties.getKilled()),
						new ArrayList<Unit>(m_casualties.getDamaged()), m_dependentUnits);
			getRemote(m_attacker, bridge).confirmOwnCasualties(m_battleID, "Press space to continue");
			final Runnable r = new Runnable()
			{
				public void run()
				{
					try
					{
						getRemote(m_defender, bridge).confirmEnemyCasualties(m_battleID, "Press space to continue", m_attacker);
					} catch (final ConnectionLostException cle)
					{
						// somone else will deal with this
						cle.printStackTrace(System.out);
					} catch (final GameOverException e)
					{
						// ignore
					}
				}
			};
			final Thread t = new Thread(r, "click to continue waiter");
			t.start();
			try
			{
				bridge.leaveDelegateExecution();
				t.join();
			} catch (final InterruptedException e)
			{
				// ignore
			} finally
			{
				bridge.enterDelegateExecution();
			}
		}
	}
	
	private boolean canFireAA()
	{
		if (m_defendingAA == null)
		{
			final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed = TechAbilityAttachment.getAirborneTargettedByAA(m_attacker, m_data);
			return m_battleSite.getUnits().getMatches(Matches.UnitIsAAthatCanFire(m_attackingUnits, airborneTechTargetsAllowed, m_attacker, Matches.UnitIsAAforCombatOnly, m_data)).size() > 0;
		}
		else
			return m_defendingAA.size() > 0;
	}
	
	/**
	 * @return a collection containing all the combatants in units non
	 *         combatants include such things as factories, aaguns, land units
	 *         in a water battle.
	 */
	private List<Unit> removeNonCombatants(final Collection<Unit> units, final boolean attacking, final PlayerID player, final boolean doNotIncludeAA, final boolean doNotIncludeSeaBombardmentUnits)
	{
		final List<Unit> unitList = new ArrayList<Unit>(units);
		if (m_battleSite.isWater())
			unitList.removeAll(Match.getMatches(unitList, Matches.UnitIsLand));
		
		// still allow infrastructure type units that can provide support have combat abilities
		// remove infrastructure units that can't take part in combat (air/naval bases, etc...)
		unitList.removeAll(Match.getMatches(unitList, Matches.UnitCanBeInBattle(attacking, !m_battleSite.isWater(), m_data, true, doNotIncludeAA, doNotIncludeSeaBombardmentUnits).invert()));
		// remove any disabled units from combat
		unitList.removeAll(Match.getMatches(unitList, Matches.UnitIsDisabled()));
		// remove capturableOnEntering units (veqryn)
		unitList.removeAll(Match.getMatches(unitList, Matches.UnitCanBeCapturedOnEnteringToInThisTerritory(m_attacker, m_battleSite, m_data)));
		// remove any allied air units that are stuck on damaged carriers (veqryn)
		unitList.removeAll(Match.getMatches(unitList, new CompositeMatchAnd<Unit>(Matches.unitIsBeingTransported(), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier)));
		// remove any units that were in air combat (veqryn)
		unitList.removeAll(Match.getMatches(unitList, Matches.UnitWasInAirBattle));
		return unitList;
	}
	
	private void removeNonCombatants(final IDelegateBridge bridge, final boolean doNotIncludeAA, final boolean doNotIncludeSeaBombardmentUnits)
	{
		final List<Unit> notRemovedDefending = removeNonCombatants(m_defendingUnits, false, m_defender, doNotIncludeAA, doNotIncludeSeaBombardmentUnits);
		final List<Unit> notRemovedAttacking = removeNonCombatants(m_attackingUnits, true, m_attacker, doNotIncludeAA, doNotIncludeSeaBombardmentUnits);
		final Collection<Unit> toRemoveDefending = Util.difference(m_defendingUnits, notRemovedDefending);
		final Collection<Unit> toRemoveAttacking = Util.difference(m_attackingUnits, notRemovedAttacking);
		m_defendingUnits = notRemovedDefending;
		m_attackingUnits = notRemovedAttacking;
		if (!m_headless)
		{
			if (!toRemoveDefending.isEmpty())
				getDisplay(bridge).changedUnitsNotification(m_battleID, m_defender, toRemoveDefending, null, null);
			if (!toRemoveAttacking.isEmpty())
				getDisplay(bridge).changedUnitsNotification(m_battleID, m_attacker, toRemoveAttacking, null, null);
		}
	}
	
	private void landParatroops(final IDelegateBridge bridge)
	{
		if (isParatroopers(m_attacker))
		{
			final Collection<Unit> airTransports = Match.getMatches(m_battleSite.getUnits().getUnits(), Matches.UnitIsAirTransport);
			if (!airTransports.isEmpty())
			{
				final Collection<Unit> dependents = getDependentUnits(airTransports);
				if (!dependents.isEmpty())
				{
					final Iterator<Unit> dependentsIter = dependents.iterator();
					final CompositeChange change = new CompositeChange();
					// remove dependency from paratroops
					// unload the transports
					while (dependentsIter.hasNext())
					{
						final Unit unit = dependentsIter.next();
						change.add(DelegateFinder.moveDelegate(m_data).getTransportTracker().unloadAirTransportChange((TripleAUnit) unit, m_battleSite, m_attacker, false));
					}
					/*while (dependentsIter.hasNext())
					{
						final Unit unit = dependentsIter.next();
						change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
					}*/

					bridge.addChange(change);
					// remove bombers from m_dependentUnits
					for (final Unit unit : airTransports)
					{
						m_dependentUnits.remove(unit);
					}
				}
			}
		}
	}
	
	private void markNoMovementLeft(final IDelegateBridge bridge)
	{
		if (m_headless)
			return;
		final Collection<Unit> attackingNonAir = Match.getMatches(m_attackingUnits, Matches.UnitIsAir.invert());
		final Change noMovementChange = ChangeFactory.markNoMovementChange(attackingNonAir);
		if (!noMovementChange.isEmpty())
			bridge.addChange(noMovementChange);
	}
	
	// Figure out what units a transport is transported and has unloaded
	public Collection<Unit> getTransportDependents(final Collection<Unit> targets, final GameData data)
	{
		if (m_headless)
		{
			return Collections.emptyList();
		}
		final Collection<Unit> dependents = new ArrayList<Unit>();
		if (Match.someMatch(targets, Matches.UnitCanTransport))
		{
			// just worry about transports
			final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
			for (final Unit target : targets)
			{
				dependents.addAll(tracker.transportingAndUnloaded(target));
			}
		}
		return dependents;
	}
	
	private void remove(final Collection<Unit> killed, final IDelegateBridge bridge, final Territory battleSite, final boolean isAA)
	{
		if (killed.size() == 0)
			return;
		final Collection<Unit> dependent = getDependentUnits(killed);
		killed.addAll(dependent);
		final Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
		m_killed.addAll(killed);
		final String transcriptText = MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
		bridge.getHistoryWriter().addChildToEvent(transcriptText, killed);
		bridge.addChange(killedChange);
		final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(this);
		// If there are NO dependent battles, check for unloads in allied territories
		if (dependentBattles.isEmpty())
			removeFromNonCombatLandings(killed, bridge);
		// otherwise remove them and the units involved
		else
			removeFromDependents(killed, bridge, dependentBattles);
	}
	
	private void removeFromDependents(final Collection<Unit> units, final IDelegateBridge bridge, final Collection<IBattle> dependents)
	{
		for (final IBattle dependent : dependents)
		{
			dependent.unitsLostInPrecedingBattle(this, units, bridge);
		}
	}
	
	// Remove landed units from allied territory when their transport sinks
	private void removeFromNonCombatLandings(final Collection<Unit> units, final IDelegateBridge bridge)
	{
		for (final Unit transport : Match.getMatches(units, Matches.UnitIsTransport))
		{
			final Collection<Unit> lost = getTransportDependents(Collections.singleton(transport), m_data);
			if (lost.isEmpty())
			{
				continue;
			}
			final Territory landedTerritory = getTransportTracker().getTerritoryTransportHasUnloadedTo(transport);
			if (landedTerritory == null)
			{
				throw new IllegalStateException("not unloaded?:" + units);
			}
			m_attackingUnits.removeAll(lost);
			remove(lost, bridge, landedTerritory, false);
		}
	}
	
	private void clearWaitingToDie(final IDelegateBridge bridge)
	{
		final Collection<Unit> units = new ArrayList<Unit>();
		units.addAll(m_attackingWaitingToDie);
		units.addAll(m_defendingWaitingToDie);
		remove(units, bridge, m_battleSite, false);
		m_defendingWaitingToDie.clear();
		m_attackingWaitingToDie.clear();
	}
	
	private void defenderWins(final IDelegateBridge bridge)
	{
		m_whoWon = WhoWon.DEFENDER;
		getDisplay(bridge).battleEnd(m_battleID, m_defender.getName() + " win");
		bridge.getHistoryWriter().addChildToEvent(m_defender.getName() + " win", m_defendingUnits);
		m_battleResultDescription = BattleRecord.BattleResultDescription.LOST;
		showCasualties(bridge);
		if (!m_headless)
			m_battleTracker.getBattleRecords(m_data).addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResultDescription,
						new BattleResults(this, m_data), 0);
		checkDefendingPlanesCanLand(bridge, m_defender);
		BattleTracker.captureOrDestroyUnits(m_battleSite, m_defender, m_defender, bridge, null, m_defendingUnits);
		if (!m_headless)
			ClipPlayer.play(SoundPath.CLIP_BATTLE_FAILURE, m_attacker.getName());
	}
	
	private void nobodyWins(final IDelegateBridge bridge)
	{
		m_whoWon = WhoWon.DRAW;
		getDisplay(bridge).battleEnd(m_battleID, "Stalemate");
		bridge.getHistoryWriter().addChildToEvent(m_defender.getName() + " and " + m_attacker.getName() + " reach a stalemate");
		m_battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
		showCasualties(bridge);
		if (!m_headless)
			m_battleTracker.getBattleRecords(m_data).addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResultDescription,
						new BattleResults(this, m_data), 0);
		if (!m_headless)
			ClipPlayer.play(SoundPath.CLIP_BATTLE_FAILURE, m_attacker.getName());
	}
	
	/*
	 * If the attacker retreats, and this is a sea zone, then any attacking fighters with
	 * 0 movement get a 1 movement bonus to allow them to retreat.
	 * 
	 * This handles the case where fighters will die if they have 0 movement when they arrive
	 * in the attacking zone, but they arrived with a carrier which retreated
	private void ensureAttackingAirCanRetreat(final IDelegateBridge bridge)
	{
		final CompositeMatch<Unit> canLandOnCarrier = new CompositeMatchAnd<Unit>();
		canLandOnCarrier.add(Matches.UnitIsAir);
		// this only applies to air units that can land on a carrier
		canLandOnCarrier.add(Matches.UnitCanLandOnCarrier);
		final Collection<Unit> air = Match.getMatches(m_attackingUnits, canLandOnCarrier);
		// TODO interesting quirk- kamikaze aircraft may move their full movement, then one more on retreat due to this
		for (final Unit unit : air)
		{
			bridge.addChange(MoveDelegate.ensureCanMoveOneSpaceChange(unit));
		}
	}
	 */

	/**
	 * The defender has won, but there may be defending fighters that cant stay
	 * in the sea zone due to insufficient carriers.
	 */
	private void checkDefendingPlanesCanLand(final IDelegateBridge bridge, final PlayerID defender)
	{
		if (m_headless)
			return;
		// not water, not relevant.
		if (!m_battleSite.isWater())
			return;
		// TODO: why do we keep checking throughout this entire class if the units in m_defendingUnits are allied with defender, and if the units in m_attackingUnits are allied with the attacker? Does it really matter?
		final CompositeMatch<Unit> alliedDefendingAir = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.UnitWasScrambled.invert()); // Matches.isUnitAllied(m_defender, m_data),
		m_defendingAir = Match.getMatches(m_defendingUnits, alliedDefendingAir);
		// no planes, exit
		if (m_defendingAir.isEmpty())
			return;
		int carrierCost = AirMovementValidator.carrierCost(m_defendingAir);
		final int carrierCapacity = AirMovementValidator.carrierCapacity(m_defendingUnits, m_battleSite);
		// add dependant air to carrier cost
		carrierCost += AirMovementValidator.carrierCost(Match.getMatches(getDependentUnits(m_defendingUnits), alliedDefendingAir));
		// all planes can land, exit
		if (carrierCapacity >= carrierCost)
			return;
		// find out what we must remove
		// remove all the air that can land on carriers from defendingAir
		carrierCost = 0;
		// add dependant air to carrier cost
		carrierCost += AirMovementValidator.carrierCost(Match.getMatches(getDependentUnits(m_defendingUnits), alliedDefendingAir));
		for (final Unit currentUnit : new ArrayList<Unit>(m_defendingAir))
		{
			if (!Matches.UnitCanLandOnCarrier.match(currentUnit))
			{
				m_defendingAir.remove(currentUnit);
				continue;
			}
			carrierCost += UnitAttachment.get(currentUnit.getType()).getCarrierCost();
			if (carrierCapacity >= carrierCost)
			{
				m_defendingAir.remove(currentUnit);
			}
		}
		// Moved this choosing to after all battles, as we legally should be able to land in a territory if we win there.
		m_battleTracker.addToDefendingAirThatCanNotLand(m_defendingAir, m_battleSite);
		
		/* Moved to BattleDelegate.java
		// Get all land territories where there are no pending battles
		final Set<Territory> neighbors = m_data.getMap().getNeighbors(m_battleSite);
		final CompositeMatch<Territory> alliedLandTerritories = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.isTerritoryAllied(m_defender, m_data),
					Matches.territoryHasNoEnemyUnits(m_defender, m_data));
		// Get those that are neighbors
		final Collection<Territory> canLandHere = Match.getMatches(neighbors, alliedLandTerritories);
		// Get all sea territories where there are allies and no pending battles
		final CompositeMatch<Territory> neighboringSeaZonesWithAlliedUnits = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(m_attacker, m_data),
					Matches.territoryHasNoEnemyUnits(m_defender, m_data));
		// Get those that are neighbors
		final Collection<Territory> areSeaNeighbors = Match.getMatches(neighbors, neighboringSeaZonesWithAlliedUnits);
		// Set up match criteria for allied carriers
		final CompositeMatch<Unit> alliedCarrier = new CompositeMatchAnd<Unit>();
		alliedCarrier.add(Matches.UnitIsCarrier);
		alliedCarrier.add(Matches.alliedUnit(m_defender, m_data));
		// Set up match criteria for allied planes
		final CompositeMatch<Unit> alliedPlane = new CompositeMatchAnd<Unit>();
		alliedPlane.add(Matches.UnitIsAir);
		alliedPlane.add(Matches.alliedUnit(m_defender, m_data));
		// See if neighboring carriers have any capacity available
		for (final Territory currentTerritory : areSeaNeighbors)
		{
			// get the capacity of the carriers and cost of fighters
			final Collection<Unit> alliedCarriers = currentTerritory.getUnits().getMatches(alliedCarrier);
			final Collection<Unit> alliedPlanes = currentTerritory.getUnits().getMatches(alliedPlane);
			final int alliedCarrierCapacity = AirMovementValidator.carrierCapacity(alliedCarriers, currentTerritory);
			final int alliedPlaneCost = AirMovementValidator.carrierCost(alliedPlanes);
			// if there is free capacity, add the territory to landing possibilities
			if (alliedCarrierCapacity - alliedPlaneCost >= 1)
			{
				canLandHere.add(currentTerritory);
			}
		}
		if (isWW2V2() || isSurvivingAirMoveToLand())
		{
			Territory territory = null;
			while (canLandHere.size() > 1 && m_defendingAir.size() > 0)
			{
				territory = getRemote(m_defender, bridge).selectTerritoryForAirToLand(canLandHere, m_battleSite, MyFormatter.unitsToText(m_defendingAir));
				// added for test script
				if (territory == null)
				{
					territory = canLandHere.iterator().next();
				}
				if (territory.isWater())
				{
					landPlanesOnCarriers(bridge, alliedDefendingAir, m_defendingAir, canLandHere, alliedCarrier, alliedPlane, territory);
				}
				else
				{
					moveAirAndLand(bridge, m_defendingAir, territory);
					return;
				}
				// remove the territory from those available
				canLandHere.remove(territory);
			}
			// Land in the last remaining territory
			if (canLandHere.size() > 0 && m_defendingAir.size() > 0)
			{
				territory = canLandHere.iterator().next();
				if (territory.isWater())
				{
					landPlanesOnCarriers(bridge, alliedDefendingAir, m_defendingAir, canLandHere, alliedCarrier, alliedPlane, territory);
				}
				else
				{
					moveAirAndLand(bridge, m_defendingAir, territory);
					return;
				}
			}
		}
		else if (canLandHere.size() > 0)
		{
			// now defending air has what cant stay, is there a place we can go?
			// check for an island in this sea zone
			for (final Territory currentTerritory : canLandHere)
			{
				// only one neighbor, its an island.
				if (m_data.getMap().getNeighbors(currentTerritory).size() == 1)
				{
					moveAirAndLand(bridge, m_defendingAir, currentTerritory);
					return;
				}
			}
		}
		if (m_defendingAir.size() > 0)
		{
			// no where to go, they must die
			bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToText(m_defendingAir) + " could not land and were killed", m_defendingAir);
			final Change change = ChangeFactory.removeUnits(m_battleSite, m_defendingAir);
			bridge.addChange(change);
		}
		*/
	}
	
	/* Moved to BattleDelegate.java
	// Refactored this method
	private void landPlanesOnCarriers(final IDelegateBridge bridge, final CompositeMatch<Unit> alliedDefendingAir, final Collection<Unit> defendingAir, final Collection<Territory> canLandHere,
				final CompositeMatch<Unit> alliedCarrier, final CompositeMatch<Unit> alliedPlane, final Territory territory)
	{
		// Get the capacity of the carriers in the selected zone
		final Collection<Unit> alliedCarriersSelected = territory.getUnits().getMatches(alliedCarrier);
		final Collection<Unit> alliedPlanesSelected = territory.getUnits().getMatches(alliedPlane);
		final int alliedCarrierCapacitySelected = AirMovementValidator.carrierCapacity(alliedCarriersSelected, territory);
		final int alliedPlaneCostSelected = AirMovementValidator.carrierCost(alliedPlanesSelected);
		// Find the available capacity of the carriers in that territory
		final int territoryCapacity = alliedCarrierCapacitySelected - alliedPlaneCostSelected;
		if (territoryCapacity > 0)
		{
			// move that number of planes from the battlezone
			final Collection<Unit> movingAir = Match.getNMatches(defendingAir, territoryCapacity, alliedDefendingAir);
			moveAirAndLand(bridge, movingAir, territory);
		}
	}
	
	// Refactored this method
	private void moveAirAndLand(final IDelegateBridge bridge, final Collection<Unit> defendingAir, final Territory territory)
	{
		bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToText(defendingAir) + " forced to land in " + territory.getName(), defendingAir);
		final Change change = ChangeFactory.moveUnits(m_battleSite, territory, defendingAir);
		bridge.addChange(change);
		// remove those that landed in case it was a carrier
		m_defendingAir.removeAll(defendingAir);
	}*/

	private void attackerWins(final IDelegateBridge bridge)
	{
		m_whoWon = WhoWon.ATTACKER;
		getDisplay(bridge).battleEnd(m_battleID, m_attacker.getName() + " win");
		if (m_headless)
			return;
		// do we need to change ownership
		if (Match.someMatch(m_attackingUnits, Matches.UnitIsNotAir))
		{
			m_battleTracker.addToConquered(m_battleSite);
			m_battleTracker.takeOver(m_battleSite, m_attacker, bridge, null, m_attackingUnits);
			m_battleResultDescription = BattleRecord.BattleResultDescription.CONQUERED;
		}
		else
		{
			m_battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
		}
		// Clear the transported_by for successfully offloaded units
		final Collection<Unit> transports = Match.getMatches(m_attackingUnits, Matches.UnitIsTransport);
		if (!transports.isEmpty())
		{
			final CompositeChange change = new CompositeChange();
			final Collection<Unit> dependents = getTransportDependents(transports, m_data);
			if (!dependents.isEmpty())
			{
				for (final Unit unit : dependents)
				{
					// clear the loaded by ONLY for Combat unloads. NonCombat unloads are handled elsewhere.
					if (Matches.UnitWasUnloadedThisTurn.match(unit))
						change.add(ChangeFactory.unitPropertyChange(unit, null, TripleAUnit.TRANSPORTED_BY));
				}
				bridge.addChange(change);
			}
		}
		final CompositeChange clearAlliedAir = clearTransportedByForAlliedAirOnCarrier(m_attackingUnits, m_battleSite, m_attacker, m_data);
		if (!clearAlliedAir.isEmpty())
			bridge.addChange(clearAlliedAir);
		bridge.getHistoryWriter().addChildToEvent(m_attacker.getName() + " win", m_attackingUnits);
		showCasualties(bridge);
		if (!m_headless)
			m_battleTracker.getBattleRecords(m_data).addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, m_battleResultDescription,
						new BattleResults(this, m_data), 0);
		if (!m_headless)
		{
			if (Matches.TerritoryIsWater.match(m_battleSite))
			{
				if (Match.allMatch(m_attackingUnits, Matches.UnitIsAir))
					ClipPlayer.play(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, m_attacker.getName());
				else
					ClipPlayer.play(SoundPath.CLIP_BATTLE_SEA_SUCCESSFUL, m_attacker.getName()); // assume some naval
			}
			else
			{
				// no sounds for a successful land battle, because land battle means we are going to capture a territory, and we have capture sounds for that
				if (Match.allMatch(m_attackingUnits, Matches.UnitIsAir))
					ClipPlayer.play(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, m_attacker.getName());
			}
		}
	}
	
	public static CompositeChange clearTransportedByForAlliedAirOnCarrier(final Collection<Unit> attackingUnits, final Territory battleSite, final PlayerID attacker, final GameData data)
	{
		final CompositeChange change = new CompositeChange();
		// Clear the transported_by for successfully won battles where there was an allied air unit held as cargo by an carrier unit
		final Collection<Unit> carriers = Match.getMatches(attackingUnits, Matches.UnitIsCarrier);
		if (!carriers.isEmpty() && games.strategy.triplea.Properties.getAlliedAirDependents(data))
		{
			final Match<Unit> alliedFighters = new CompositeMatchAnd<Unit>(Matches.isUnitAllied(attacker, data), Matches.unitIsOwnedBy(attacker).invert(), Matches.UnitIsAir,
						Matches.UnitCanLandOnCarrier);
			final Collection<Unit> alliedAirInTerr = Match.getMatches(battleSite.getUnits().getUnits(), alliedFighters);
			for (final Unit fighter : alliedAirInTerr)
			{
				final TripleAUnit taUnit = (TripleAUnit) fighter;
				if (taUnit.getTransportedBy() != null)
				{
					final Unit carrierTransportingThisUnit = taUnit.getTransportedBy();
					if (!Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER).match(carrierTransportingThisUnit))
						change.add(ChangeFactory.unitPropertyChange(fighter, null, TripleAUnit.TRANSPORTED_BY));
				}
			}
		}
		return change;
	}
	
	private void showCasualties(final IDelegateBridge bridge)
	{
		if (m_killed.isEmpty())
			return;
		// a handy summary of all the units killed
		IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_attacker, m_data);
		final int tuvLostAttacker = BattleCalculator.getTUV(m_killed, m_attacker, costs, m_data);
		costs = BattleCalculator.getCostsForTUV(m_defender, m_data);
		final int tuvLostDefender = BattleCalculator.getTUV(m_killed, m_defender, costs, m_data);
		final int tuvChange = tuvLostDefender - tuvLostAttacker;
		bridge.getHistoryWriter().addChildToEvent("Battle casualty summary: Battle score (TUV change) for attacker is " + tuvChange, m_killed);
		m_attackerLostTUV += tuvLostAttacker;
		m_defenderLostTUV += tuvLostDefender;
	}
	
	private void endBattle(final IDelegateBridge bridge)
	{
		clearWaitingToDie(bridge);
		m_isOver = true;
		m_battleTracker.removeBattle(this);
	}
	
	@Override
	public String toString()
	{
		return "Battle in:" + m_battleSite + " battle type:" + m_battleType + " defender:" + m_defender.getName() + " attacked by:" + m_attacker.getName() + " from:" + m_attackingFrom
					+ " attacking with: " + m_attackingUnits;
	}
	
	// In an amphibious assault, sort on who is unloading from xports first
	// This will allow the marines with higher scores to get killed last
	public void sortAmphib(final List<Unit> units, final GameData data)
	{
		final Comparator<Unit> decreasingMovement = UnitComparator.getLowestToHighestMovementComparator();
		final Comparator<Unit> comparator = new Comparator<Unit>()
		{
			public int compare(final Unit u1, final Unit u2)
			{
				int amphibComp = 0;
				if (u1.getUnitType().equals(u2.getUnitType()))
				{
					final UnitAttachment ua = UnitAttachment.get(u1.getType());
					final UnitAttachment ua2 = UnitAttachment.get(u2.getType());
					if (ua.getIsMarine() && ua2.getIsMarine())
						amphibComp = compareAccordingToAmphibious(u1, u2);
					if (amphibComp == 0)
						return decreasingMovement.compare(u1, u2);
					return amphibComp;
				}
				return u1.getUnitType().getName().compareTo(u2.getUnitType().getName());
			}
		};
		Collections.sort(units, comparator);
	}
	
	private int compareAccordingToAmphibious(final Unit u1, final Unit u2)
	{
		if (m_amphibiousLandAttackers.contains(u1) && !m_amphibiousLandAttackers.contains(u2))
			return -1;
		else if (m_amphibiousLandAttackers.contains(u2) && !m_amphibiousLandAttackers.contains(u1))
			return 1;
		return 0;
	}
	
	public Collection<Territory> getAttackingFrom()
	{
		return m_attackingFrom;
	}
	
	public Map<Territory, Collection<Unit>> getAttackingFromMap()
	{
		return m_attackingFromMap;
	}
	
	// used for setting stuff when we make a scrambling battle when there was no previous battle there, and we need retreat spaces
	public void setAttackingFromAndMap(final Map<Territory, Collection<Unit>> attackingFromMap)
	{
		m_attackingFromMap = attackingFromMap;
		m_attackingFrom = new HashSet<Territory>(attackingFromMap.keySet());
	}
	
	@Override
	public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units, final IDelegateBridge bridge)
	{
		final Collection<Unit> lost = getDependentUnits(units);
		// if all the amphibious attacking land units are lost, then we are
		// no longer a naval invasion
		m_amphibiousLandAttackers.removeAll(lost);
		if (m_amphibiousLandAttackers.isEmpty())
		{
			m_isAmphibious = false;
			m_bombardingUnits.clear();
		}
		m_attackingUnits.removeAll(lost);
		remove(lost, bridge, m_battleSite, false);
		if (m_attackingUnits.isEmpty())
		{
			final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(m_attacker, m_data);
			final int tuvLostAttacker = BattleCalculator.getTUV(lost, m_attacker, costs, m_data);
			m_attackerLostTUV += tuvLostAttacker;
			m_whoWon = WhoWon.DEFENDER;
			if (!m_headless)
				m_battleTracker.getBattleRecords(m_data).addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV, m_defenderLostTUV, BattleRecord.BattleResultDescription.LOST,
							new BattleResults(this, m_data), 0);
			m_battleTracker.removeBattle(this);
		}
	}
	
	/**
	 * Returns a map of transport -> collection of transported units.
	 */
	private Map<Unit, Collection<Unit>> transporting(final Collection<Unit> units)
	{
		return getTransportTracker().transporting(units);
	}
}
