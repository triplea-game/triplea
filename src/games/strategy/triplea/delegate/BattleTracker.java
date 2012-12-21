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
 * BattleTracker.java
 * 
 * Created on November 15, 2001, 11:18 AM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.IBattle.WhoWon;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *          Used to keep track of where battles have occurred
 */
public class BattleTracker implements java.io.Serializable
{
	private static final long serialVersionUID = 8806010984321554662L;
	
	// List of pending battles
	private final Set<IBattle> m_pendingBattles = new HashSet<IBattle>();
	// List of battle dependencies
	// maps blocked -> Collection of battles that must precede
	private final Map<IBattle, HashSet<IBattle>> m_dependencies = new HashMap<IBattle, HashSet<IBattle>>();
	// enemy and neutral territories that have been conquered
	// blitzed is a subset of this
	private final Set<Territory> m_conquered = new HashSet<Territory>();
	// blitzed territories
	private final Set<Territory> m_blitzed = new HashSet<Territory>();
	// territories where a battle occurred
	private final Set<Territory> m_foughBattles = new HashSet<Territory>();
	// these territories have had battleships bombard during a naval invasion
	// used to make sure that the same battleship doesn't bombard twice
	private final Set<Territory> m_bombardedFromTerritories = new HashSet<Territory>();
	// list of territory we have conquered in a FinishedBattle and where from and if amphibious
	private final HashMap<Territory, Map<Territory, Collection<Unit>>> m_finishedBattlesUnitAttackFromMap = new HashMap<Territory, Map<Territory, Collection<Unit>>>();
	// things like kamikaze suicide attacks disallow bombarding from that sea zone for that turn
	private final Set<Territory> m_noBombardAllowed = new HashSet<Territory>();
	private final Map<Territory, Collection<Unit>> m_defendingAirThatCanNotLand = new HashMap<Territory, Collection<Unit>>();
	private BattleRecords m_battleRecords = null;
	// to keep track of all relationships that have changed this turn (so we can validate things like transports loading in newly created hostile zones)
	private final Collection<Tuple<Tuple<PlayerID, PlayerID>, Tuple<RelationshipType, RelationshipType>>> m_relationshipChangesThisTurn = new ArrayList<Tuple<Tuple<PlayerID, PlayerID>, Tuple<RelationshipType, RelationshipType>>>();
	
	/**
	 * @param t
	 *            referring territory
	 * @param bombing
	 * @return whether a battle is to be fought in the given territory
	 */
	public boolean hasPendingBattle(final Territory t, final boolean bombing)
	{
		return getPendingBattle(t, bombing) != null;
	}
	
	/**
	 * add to the conquered.
	 */
	void addToConquered(final Collection<Territory> territories)
	{
		m_conquered.addAll(territories);
	}
	
	void addToConquered(final Territory territory)
	{
		m_conquered.add(territory);
	}
	
	/**
	 * @param t
	 *            referring territory
	 * @return whether territory was conquered
	 */
	public boolean wasConquered(final Territory t)
	{
		return m_conquered.contains(t);
	}
	
	public Set<Territory> getConquered()
	{
		return m_conquered;
	}
	
	/**
	 * @param t
	 *            referring territory
	 * @return whether territory was conquered by blitz
	 */
	public boolean wasBlitzed(final Territory t)
	{
		return m_blitzed.contains(t);
	}
	
	public boolean wasBattleFought(final Territory t)
	{
		return m_foughBattles.contains(t);
	}
	
	public boolean noBombardAllowedFromHere(final Territory t)
	{
		return m_noBombardAllowed.contains(t);
	}
	
	public void addNoBombardAllowedFromHere(final Territory t)
	{
		m_noBombardAllowed.add(t);
	}
	
	public HashMap<Territory, Map<Territory, Collection<Unit>>> getFinishedBattlesUnitAttackFromMap()
	{
		return m_finishedBattlesUnitAttackFromMap;
	}
	
	public void addRelationshipChangesThisTurn(final PlayerID p1, final PlayerID p2, final RelationshipType oldRelation, final RelationshipType newRelation)
	{
		
		m_relationshipChangesThisTurn.add(new Tuple<Tuple<PlayerID, PlayerID>, Tuple<RelationshipType, RelationshipType>>(
					new Tuple<PlayerID, PlayerID>(p1, p2), new Tuple<RelationshipType, RelationshipType>(oldRelation, newRelation)));
	}
	
	public boolean didAllThesePlayersJustGoToWarThisTurn(final PlayerID p1, final Collection<Unit> enemyUnits, final GameData data)
	{
		final Set<PlayerID> enemies = new HashSet<PlayerID>();
		for (final Unit u : Match.getMatches(enemyUnits, Matches.unitIsEnemyOf(data, p1)))
		{
			enemies.add(u.getOwner());
		}
		for (final PlayerID e : enemies)
		{
			if (!didThesePlayersJustGoToWarThisTurn(p1, e))
				return false;
		}
		return true;
	}
	
	public boolean didThesePlayersJustGoToWarThisTurn(final PlayerID p1, final PlayerID p2)
	{
		// check all relationship changes that are p1 and p2, to make sure that oldRelation is not war, and newRelation is war
		for (final Tuple<Tuple<PlayerID, PlayerID>, Tuple<RelationshipType, RelationshipType>> t : m_relationshipChangesThisTurn)
		{
			final Tuple<PlayerID, PlayerID> players = t.getFirst();
			if (players.getFirst().equals(p1))
			{
				if (!players.getSecond().equals(p2))
					continue;
			}
			else if (players.getSecond().equals(p1))
			{
				if (!players.getFirst().equals(p2))
					continue;
			}
			else
				continue;
			final Tuple<RelationshipType, RelationshipType> relations = t.getSecond();
			if (!Matches.RelationshipTypeIsAtWar.match(relations.getFirst()))
			{
				if (Matches.RelationshipTypeIsAtWar.match(relations.getSecond()))
					return true;
			}
		}
		return false;
	}
	
	void clearFinishedBattles(final IDelegateBridge bridge)
	{
		for (final IBattle battle : new ArrayList<IBattle>(m_pendingBattles))
		{
			if (FinishedBattle.class.isAssignableFrom(battle.getClass()))
			{
				final FinishedBattle finished = (FinishedBattle) battle;
				m_finishedBattlesUnitAttackFromMap.put(finished.getTerritory(), finished.getAttackingFromMap());
				finished.fight(bridge);
			}
		}
	}
	
	public void undoBattle(final Route route, final Collection<Unit> units, final PlayerID player, final IDelegateBridge bridge)
	{
		for (final IBattle battle : new ArrayList<IBattle>(m_pendingBattles))
		{
			if (battle.getTerritory().equals(route.getEnd()))
			{
				battle.removeAttack(route, units);
				if (battle.isEmpty())
				{
					removeBattleForUndo(player, battle);
				}
			}
		}
		final RelationshipTracker relationshipTracker = bridge.getData().getRelationshipTracker();
		// if we have no longer conquered it, clear the blitz state
		// EW: Does this have to look at all Territories? or just middle-territories or steps? // answer: veq: yes, we look at all, because we could have conquered the end territory if there are no units there
		for (final Territory current : route.getAllTerritories())
		{
			if (!relationshipTracker.isAllied(current.getOwner(), player) && m_conquered.contains(current))
			{
				m_conquered.remove(current);
				m_blitzed.remove(current);
			}
		}
		// say they weren't in combat
		final CompositeChange change = new CompositeChange();
		final Iterator<Unit> attackIter = units.iterator();
		while (attackIter.hasNext())
		{
			change.add(ChangeFactory.unitPropertyChange(attackIter.next(), false, TripleAUnit.WAS_IN_COMBAT));
		}
		bridge.addChange(change);
	}
	
	private void removeBattleForUndo(final PlayerID player, final IBattle battle)
	{
		if (m_battleRecords != null)
		{
			m_battleRecords.removeBattle(player, battle.getBattleID());
		}
		m_pendingBattles.remove(battle);
		m_dependencies.remove(battle);
		for (final Collection<IBattle> battles : m_dependencies.values())
		{
			battles.remove(battle);
		}
	}
	
	public void addBattle(final Route route, final Collection<Unit> units, final boolean bombing, final PlayerID id, final IDelegateBridge bridge, final UndoableMove changeTracker,
				final Collection<Unit> unitsNotUnloadedTilEndOfRoute)
	{
		this.addBattle(route, units, bombing, id, bridge, changeTracker, unitsNotUnloadedTilEndOfRoute, null, false);
	}
	
	public void addBattle(final Route route, final Collection<Unit> units, final boolean bombing, final PlayerID id, final IDelegateBridge bridge, final UndoableMove changeTracker,
				final Collection<Unit> unitsNotUnloadedTilEndOfRoute, final HashMap<Unit, HashSet<Unit>> targets, final boolean airBattleCompleted)
	{
		final GameData data = bridge.getData();
		if (bombing)
		{
			if (!airBattleCompleted && games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(data)
						&& Match.someMatch(route.getEnd().getUnits().getUnits(), StrategicBombingRaidPreBattle.defendingInterceptors(id, data)))
				addAirBattle(route, units, id, data);
			else
				addBombingBattle(route, units, id, data, targets);
			// say they were in combat
			markWasInCombat(units, bridge, changeTracker);
		}
		else
		{
			final Change change = addMustFightBattleChange(route, units, id, data);
			bridge.addChange(change);
			if (changeTracker != null)
			{
				changeTracker.addChange(change);
			}
			if (games.strategy.util.Match.someMatch(units, Matches.UnitIsLand) || games.strategy.util.Match.someMatch(units, Matches.UnitIsSea))
				addEmptyBattle(route, units, id, bridge, changeTracker, unitsNotUnloadedTilEndOfRoute);
		}
	}
	
	private void markWasInCombat(final Collection<Unit> units, final IDelegateBridge bridge, final UndoableMove changeTracker)
	{
		if (units == null)
			return;
		final CompositeChange change = new CompositeChange();
		final Iterator<Unit> attackIter = units.iterator();
		while (attackIter.hasNext())
		{
			change.add(ChangeFactory.unitPropertyChange(attackIter.next(), true, TripleAUnit.WAS_IN_COMBAT));
		}
		bridge.addChange(change);
		if (changeTracker != null)
		{
			changeTracker.addChange(change);
		}
	}
	
	/*private void addBombingBattle(final Route route, final Collection<Unit> units, final PlayerID attacker, final GameData data)
	{
		addBombingBattle(route, units, attacker, data, null);
	}*/

	private void addBombingBattle(final Route route, final Collection<Unit> units, final PlayerID attacker, final GameData data, final HashMap<Unit, HashSet<Unit>> targets)
	{
		IBattle battle = getPendingBattle(route.getEnd(), true, BattleType.BOMBING_RAID);
		if (battle == null)
		{
			battle = new StrategicBombingRaidBattle(route.getEnd(), data, attacker, this);
			m_pendingBattles.add(battle);
			getBattleRecords(data).addBattle(attacker, battle.getBattleID(), route.getEnd(), battle.getBattleType(), data);
		}
		final Change change = battle.addAttackChange(route, units, targets);
		// when state is moved to the game data, this will change
		if (!change.isEmpty())
		{
			throw new IllegalStateException("Non empty change");
		}
		// dont let land battles in the same territory occur before bombing battles
		final IBattle dependent = getPendingBattle(route.getEnd(), false);
		if (dependent != null)
			addDependency(dependent, battle);
	}
	
	private void addAirBattle(final Route route, final Collection<Unit> units, final PlayerID attacker, final GameData data)
	{
		IBattle battle = getPendingBattle(route.getEnd(), true);
		if (battle == null)
		{
			battle = new StrategicBombingRaidPreBattle(route.getEnd(), data, attacker, this);
			m_pendingBattles.add(battle);
			getBattleRecords(data).addBattle(attacker, battle.getBattleID(), route.getEnd(), battle.getBattleType(), data);
		}
		final Change change = battle.addAttackChange(route, units, null);
		// when state is moved to the game data, this will change
		if (!change.isEmpty())
		{
			throw new IllegalStateException("Non empty change");
		}
		// dont let land battles in the same territory occur before bombing battles
		final IBattle dependent = getPendingBattle(route.getEnd(), false);
		if (dependent != null)
			addDependency(dependent, battle);
	}
	
	/**
	 * No enemies.
	 */
	private void addEmptyBattle(final Route route, final Collection<Unit> units, final PlayerID id, final IDelegateBridge bridge, final UndoableMove changeTracker,
				final Collection<Unit> unitsNotUnloadedTilEndOfRoute)
	{
		final GameData data = bridge.getData();
		final Collection<Unit> canConquer = Match.getMatches(units, Matches.unitIsBeingTransportedByOrIsDependentOfSomeUnitInThisList(units, route, id, data, false).invert());
		if (Match.noneMatch(canConquer, Matches.UnitIsNotAir))
			return;
		final Collection<Unit> presentFromStartTilEnd = new ArrayList<Unit>(canConquer);
		if (unitsNotUnloadedTilEndOfRoute != null)
			presentFromStartTilEnd.removeAll(unitsNotUnloadedTilEndOfRoute);
		final boolean canConquerMiddleSteps = Match.someMatch(presentFromStartTilEnd, Matches.UnitIsNotAir);
		final boolean scramblingEnabled = games.strategy.triplea.Properties.getScramble_Rules_In_Effect(data);
		final CompositeMatch<Territory> conquerable = new CompositeMatchAnd<Territory>();
		conquerable.add(Matches.territoryIsEmptyOfCombatUnits(data, id));
		conquerable.add(new CompositeMatchOr<Territory>(Matches.territoryIsOwnedByPlayerWhosRelationshipTypeCanTakeOverOwnedTerritoryAndPassableAndNotWater(id),
					Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(id, data)));
		final Collection<Territory> conquered = new ArrayList<Territory>();
		if (canConquerMiddleSteps)
		{
			conquered.addAll(route.getMatches(conquerable));
			// in case we begin in enemy territory, and blitz out of it, check the first territory
			if (route.getStart() != route.getEnd() && conquerable.match(route.getStart()))
				conquered.add(route.getStart());
		}
		// we handle the end of the route later
		conquered.remove(route.getEnd());
		final Collection<Territory> blitzed = Match.getMatches(conquered, Matches.TerritoryIsBlitzable(id, data));
		m_blitzed.addAll(Match.getMatches(blitzed, Matches.isTerritoryEnemy(id, data)));
		m_conquered.addAll(Match.getMatches(conquered, Matches.isTerritoryEnemy(id, data)));
		for (final Territory current : conquered)
		{
			IBattle nonFight = getPendingBattle(current, false);
			// TODO: if we ever want to scramble to a blitzed territory, then we need to fix this stuff (currently doesn't work because then the territory is never conquered because the units have left the territory by the time we fight)
			/*if (scramblingEnabled)
			{
				if (nonFight == null)
				{
					nonFight = new NonFightingBattle(current, id, this, data);
					m_pendingBattles.add(nonFight);
					getBattleRecords(data).addBattle(id, nonFight.getBattleID(), current, nonFight.getBattleType(), data);
				}
				final Change change = nonFight.addAttackChange(Route.subRoute(route, current), units, null);
				bridge.addChange(change);
				if (changeTracker != null)
				{
					changeTracker.addChange(change);
				}
			}
			else
			{*/
			if (nonFight == null)
			{
				nonFight = new FinishedBattle(current, id, this, false, BattleType.NORMAL, data, BattleRecord.BattleResultDescription.CONQUERED, WhoWon.ATTACKER, units);
				m_pendingBattles.add(nonFight);
				getBattleRecords(data).addBattle(id, nonFight.getBattleID(), current, nonFight.getBattleType(), data);
			}
			final Change change = nonFight.addAttackChange(route, units, null);
			bridge.addChange(change);
			if (changeTracker != null)
			{
				changeTracker.addChange(change);
			}
			takeOver(current, id, bridge, changeTracker, units);
			// }
		}
		// check the last territory
		if (conquerable.match(route.getEnd()))
		{
			IBattle precede = getDependentAmphibiousAssault(route);
			if (precede == null)
			{
				precede = getPendingBattle(route.getEnd(), true);
			}
			// if we have a preceding battle, then we must use a non-fighting-battle
			// if we have scrambling on, and this is an amphibious attack, we may wish to scramble to kill the transports, so must use non-fighting-battle also
			if (precede != null || (scramblingEnabled && route.isUnload() && route.hasExactlyOneStep()))
			{
				IBattle nonFight = getPendingBattle(route.getEnd(), false);
				if (nonFight == null)
				{
					nonFight = new NonFightingBattle(route.getEnd(), id, this, data);
					m_pendingBattles.add(nonFight);
					getBattleRecords(data).addBattle(id, nonFight.getBattleID(), route.getEnd(), nonFight.getBattleType(), data);
				}
				final Change change = nonFight.addAttackChange(route, units, null);
				bridge.addChange(change);
				if (changeTracker != null)
				{
					changeTracker.addChange(change);
				}
				if (precede != null)
					addDependency(nonFight, precede);
			}
			else
			{
				if (Matches.isTerritoryEnemy(id, data).match(route.getEnd()))
				{
					if (Matches.TerritoryIsBlitzable(id, data).match(route.getEnd()))
					{
						m_blitzed.add(route.getEnd());
					}
					m_conquered.add(route.getEnd());
				}
				IBattle nonFight = getPendingBattle(route.getEnd(), false);
				if (nonFight == null)
				{
					nonFight = new FinishedBattle(route.getEnd(), id, this, false, BattleType.NORMAL, data, BattleRecord.BattleResultDescription.CONQUERED, WhoWon.ATTACKER, units);
					m_pendingBattles.add(nonFight);
					getBattleRecords(data).addBattle(id, nonFight.getBattleID(), route.getEnd(), nonFight.getBattleType(), data);
				}
				final Change change = nonFight.addAttackChange(route, units, null);
				bridge.addChange(change);
				if (changeTracker != null)
				{
					changeTracker.addChange(change);
				}
				takeOver(route.getEnd(), id, bridge, changeTracker, units);
			}
		}
		// TODO: else what?
	}
	
	public void takeOver(final Territory territory, final PlayerID id, final IDelegateBridge bridge, final UndoableMove changeTracker, final Collection<Unit> arrivingUnits)
	{
		final GameData data = bridge.getData();
		final Collection<Unit> arrivedUnits = (arrivingUnits == null ? null : new ArrayList<Unit>(arrivingUnits));
		// final OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
		final RelationshipTracker relationshipTracker = data.getRelationshipTracker();
		final boolean isTerritoryOwnerAnEnemy = relationshipTracker.canTakeOverOwnedTerritory(id, territory.getOwner()); // .isAtWar(id, territory.getOwner());
		// If this is a convoy (we wouldn't be in this method otherwise) check to make sure attackers have more than just transports. If they don't, exit here.
		if (territory.isWater() && arrivedUnits != null)
		{
			int totalMatches = 0;
			// 0 production waters aren't to be taken over
			final TerritoryAttachment ta = TerritoryAttachment.get(territory);
			if (ta == null)
				return;
			// Total Attacking Sea units = all units - land units - air units - submerged subs
			// Also subtract transports & subs (if they can't control sea zones)
			totalMatches = arrivedUnits.size() - Match.countMatches(arrivedUnits, Matches.UnitIsLand) - Match.countMatches(arrivedUnits, Matches.UnitIsAir)
						- Match.countMatches(arrivedUnits, Matches.unitIsSubmerged(data));
			// If transports are restricted from controlling sea zones, subtract them
			final CompositeMatch<Unit> transportsCanNotControl = new CompositeMatchAnd<Unit>();
			transportsCanNotControl.add(Matches.UnitIsTransportAndNotDestroyer);
			transportsCanNotControl.add(Matches.UnitIsTransportButNotCombatTransport);
			if (!games.strategy.triplea.Properties.getTransportControlSeaZone(data))
				totalMatches -= Match.countMatches(arrivedUnits, transportsCanNotControl);
			// TODO check if istrn and NOT isDD
			// If subs are restricted from controlling sea zones, subtract them
			if (games.strategy.triplea.Properties.getSubControlSeaZoneRestricted(data))
				totalMatches -= Match.countMatches(arrivedUnits, Matches.UnitIsSub);
			if (totalMatches == 0)
				return;
		}
		// If it was a Convoy Route- check ownership of the associated neighboring territory and set message
		final TerritoryAttachment ta = TerritoryAttachment.get(territory);
		if (ta.getConvoyRoute())
		{
			// we could be part of a convoy route for another territory
			final Collection<Territory> attachedConvoyTo = TerritoryAttachment.getWhatTerritoriesThisIsUsedInConvoysFor(territory, data);
			for (final Territory convoy : attachedConvoyTo)
			{
				final TerritoryAttachment cta = TerritoryAttachment.get(convoy);
				if (!cta.getConvoyRoute())
					continue;
				final PlayerID convoyOwner = convoy.getOwner();
				if (relationshipTracker.isAllied(id, convoyOwner))
				{
					if (Match.getMatches(cta.getConvoyAttached(), Matches.isTerritoryAllied(convoyOwner, data)).size() <= 0)
						bridge.getHistoryWriter().addChildToEvent(convoyOwner.getName() + " gains " + cta.getProduction()
									+ " production in " + convoy.getName() + " for the liberation the convoy route in " + territory.getName());
				}
				else if (relationshipTracker.isAtWar(id, convoyOwner))
				{
					if (Match.getMatches(cta.getConvoyAttached(), Matches.isTerritoryAllied(convoyOwner, data)).size() == 1)
						bridge.getHistoryWriter().addChildToEvent(convoyOwner.getName() + " loses " + cta.getProduction()
									+ " production in " + convoy.getName() + " due to the capture of the convoy route in " + territory.getName());
				}
			}
		}
		// if neutral, we may charge money to enter
		if (territory.getOwner().isNull() && !territory.isWater() && games.strategy.triplea.Properties.getNeutralCharge(data) != 0)
		{
			final Resource PUs = data.getResourceList().getResource(Constants.PUS);
			final int PUChargeIdeal = -games.strategy.triplea.Properties.getNeutralCharge(data);
			final int PUChargeReal = Math.min(0, Math.max(PUChargeIdeal, -id.getResources().getQuantity(PUs)));
			final Change neutralFee = ChangeFactory.changeResourcesChange(id, PUs, PUChargeReal);
			bridge.addChange(neutralFee);
			if (changeTracker != null)
				changeTracker.addChange(neutralFee);
			if (PUChargeIdeal == PUChargeReal)
			{
				bridge.getHistoryWriter().addChildToEvent(
							id.getName() + " loses " + -PUChargeReal + " " + MyFormatter.pluralize("PU", -PUChargeReal) + " for violating " + territory.getName() + "s neutrality.");
			}
			else
			{
				System.out.println("Player, " + id.getName() + " attacks a Neutral territory, and should have had to pay " + PUChargeIdeal + ", but did not have enough PUs to pay! This is a bug.");
				bridge.getHistoryWriter().addChildToEvent(
							id.getName() + " loses " + -PUChargeReal + " " + MyFormatter.pluralize("PU", -PUChargeReal) + " for violating " + territory.getName()
										+ "s neutrality.  Correct amount to charge is: " + PUChargeIdeal + ".  Player should not have been able to make this attack!");
			}
		}
		// if its a capital we take the money
		// NOTE: this is not checking to see if it is an enemy. instead it is relying on the fact that the capital should be owned by the person it is attached to
		if (isTerritoryOwnerAnEnemy && ta.getCapital() != null)
		{
			// if the capital is owned by the capitols player
			// take the money
			final PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
			final PlayerAttachment pa = PlayerAttachment.get(id);
			final PlayerAttachment paWhoseCapital = PlayerAttachment.get(whoseCapital);
			final List<Territory> capitalsList = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(whoseCapital, data));
			if (paWhoseCapital != null && paWhoseCapital.getRetainCapitalNumber() < capitalsList.size()) // we are losing one right now, so it is < not <=
			{
				// do nothing, we keep our money since we still control enough capitals
				bridge.getHistoryWriter().addChildToEvent(id.getName() + " captures one of " + whoseCapital.getName() + " capitals");
			}
			else if (whoseCapital.equals(territory.getOwner()))
			{
				final Resource PUs = data.getResourceList().getResource(Constants.PUS);
				final int capturedPUCount = whoseCapital.getResources().getQuantity(PUs);
				if (pa != null)
				{
					if (isPacificTheater(data))
					{
						final Change changeVP = ChangeFactory.attachmentPropertyChange(pa, (capturedPUCount + pa.getCaptureVps()), "captureVps");
						bridge.addChange(changeVP);
						if (changeTracker != null)
							changeTracker.addChange(changeVP);
					}
				}
				final Change remove = ChangeFactory.changeResourcesChange(whoseCapital, PUs, -capturedPUCount);
				bridge.addChange(remove);
				if (paWhoseCapital != null && paWhoseCapital.getDestroysPUs())
				{
					bridge.getHistoryWriter().addChildToEvent(
								id.getName() + " destroys " + capturedPUCount + MyFormatter.pluralize("PU", capturedPUCount) + " while taking " + whoseCapital.getName() + " capital");
					if (changeTracker != null)
						changeTracker.addChange(remove);
				}
				else
				{
					bridge.getHistoryWriter().addChildToEvent(
								id.getName() + " captures " + capturedPUCount + MyFormatter.pluralize("PU", capturedPUCount) + " while taking " + whoseCapital.getName() + " capital");
					if (changeTracker != null)
						changeTracker.addChange(remove);
					final Change add = ChangeFactory.changeResourcesChange(id, PUs, capturedPUCount);
					bridge.addChange(add);
					if (changeTracker != null)
						changeTracker.addChange(add);
				}
				// remove all the tokens of the captured player
				final Resource tokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
				if (tokens != null)
				{
					final int m_currTokens = whoseCapital.getResources().getQuantity(Constants.TECH_TOKENS);
					final Change removeTokens = ChangeFactory.changeResourcesChange(whoseCapital, tokens, -m_currTokens);
					bridge.addChange(removeTokens);
					if (changeTracker != null)
						changeTracker.addChange(removeTokens);
				}
			}
		}
		// is this an allied territory
		// revert to original owner if it is, unless they dont own there captital
		PlayerID terrOrigOwner;
		terrOrigOwner = ta.getOccupiedTerrOf();
		if (terrOrigOwner == null)
			terrOrigOwner = OriginalOwnerTracker.getOriginalOwner(territory); // origOwnerTracker.getOriginalOwner(territory);
		PlayerID newOwner;
		// if the original owner is the current owner, and the current owner is our enemy / canTakeOver, then we do not worry about this.
		if (isTerritoryOwnerAnEnemy && terrOrigOwner != null && relationshipTracker.isAllied(terrOrigOwner, id) && !terrOrigOwner.equals(territory.getOwner()))
		{
			if (territory.equals(TerritoryAttachment.getCapital(terrOrigOwner, data)))
				newOwner = terrOrigOwner;
			else
			{
				final List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(terrOrigOwner, data));
				if (!capitalsListOwned.isEmpty())
					newOwner = terrOrigOwner;
				else
				{
					final List<Territory> capitalsListOriginal = new ArrayList<Territory>(TerritoryAttachment.getAllCapitals(terrOrigOwner, data));
					final Iterator<Territory> iter = capitalsListOriginal.iterator();
					newOwner = id;
					while (iter.hasNext())
					{
						final Territory current = iter.next();
						if (current.getOwner().equals(PlayerID.NULL_PLAYERID))
							newOwner = terrOrigOwner; // if a neutral controls our capital, our territories get liberated (ie: china in ww2v3)
					}
				}
			}
		}
		else
			newOwner = id;
		// if we have specially set this territory to have whenCapturedByGoesTo, then we set that here (except we don't set it if we are liberating allied owned territory)
		if (isTerritoryOwnerAnEnemy && newOwner.equals(id) && Matches.TerritoryHasWhenCapturedByGoesTo().match(territory))
		{
			for (final String value : ta.getWhenCapturedByGoesTo())
			{
				final String[] s = value.split(":");
				final PlayerID capturingPlayer = data.getPlayerList().getPlayerID(s[0]);
				final PlayerID goesToPlayer = data.getPlayerList().getPlayerID(s[1]);
				if (capturingPlayer.equals(goesToPlayer))
					continue;
				if (capturingPlayer.equals(id))
				{
					newOwner = goesToPlayer;
					break;
				}
			}
		}
		if (isTerritoryOwnerAnEnemy)
		{
			final Change takeOver = ChangeFactory.changeOwner(territory, newOwner);
			bridge.getHistoryWriter().addChildToEvent(takeOver.toString());
			bridge.addChange(takeOver);
			// play a sound
			if (territory.isWater())
				ClipPlayer.play(SoundPath.CLIP_TERRITORY_CAPTURE_SEA, id.getName());
			else if (ta.getCapital() != null)
				ClipPlayer.play(SoundPath.CLIP_TERRITORY_CAPTURE_CAPITAL, id.getName());
			else if (m_blitzed.contains(territory) && Match.someMatch(arrivedUnits, Matches.UnitCanBlitz))
				ClipPlayer.play(SoundPath.CLIP_TERRITORY_CAPTURE_BLITZ, id.getName());
			else
				ClipPlayer.play(SoundPath.CLIP_TERRITORY_CAPTURE_LAND, id.getName());
			if (changeTracker != null)
			{
				changeTracker.addChange(takeOver);
				changeTracker.addToConquered(territory);
			}
		}
		// Remove any bombing raids against captured territory
		// TODO: see if necessary
		if (Match.someMatch(territory.getUnits().getUnits(), new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, id), Matches.UnitCanBeDamaged)))
		{
			final IBattle bombingBattle = getPendingBattle(territory, true);
			if (bombingBattle != null)
			{
				final BattleResults results = new BattleResults(bombingBattle, WhoWon.DRAW, data);
				getBattleRecords(data).addResultToBattle(id, bombingBattle.getBattleID(), null, 0, 0, BattleRecord.BattleResultDescription.NO_BATTLE, results, 0);
				removeBattle(bombingBattle);
				throw new IllegalStateException("Bombing Raids should be dealt with first! Be sure the battle has dependencies set correctly!");
			}
		}
		captureOrDestroyUnits(territory, id, newOwner, bridge, changeTracker, arrivedUnits);
		// is this territory our capitol or a capitol of our ally
		// Also check to make sure playerAttachment even HAS a capital to fix abend
		if (isTerritoryOwnerAnEnemy && terrOrigOwner != null && ta.getCapital() != null && TerritoryAttachment.getCapital(terrOrigOwner, data).equals(territory)
					&& relationshipTracker.isAllied(terrOrigOwner, id))
		{
			// if it is give it back to the original owner
			final Collection<Territory> originallyOwned = OriginalOwnerTracker.getOriginallyOwned(data, terrOrigOwner); // origOwnerTracker.getOriginallyOwned(data, terrOrigOwner);
			final List<Territory> friendlyTerritories = Match.getMatches(originallyOwned, Matches.isTerritoryAllied(terrOrigOwner, data));
			// give back the factories as well.
			for (final Territory item : friendlyTerritories)
			{
				if (item.getOwner() == terrOrigOwner)
					continue;
				final Change takeOverFriendlyTerritories = ChangeFactory.changeOwner(item, terrOrigOwner);
				bridge.addChange(takeOverFriendlyTerritories);
				bridge.getHistoryWriter().addChildToEvent(takeOverFriendlyTerritories.toString());
				if (changeTracker != null)
					changeTracker.addChange(takeOverFriendlyTerritories);
				final Collection<Unit> units = Match.getMatches(item.getUnits().getUnits(), Matches.UnitIsInfrastructure);
				if (!units.isEmpty())
				{
					final Change takeOverNonComUnits = ChangeFactory.changeOwner(units, terrOrigOwner, territory);
					bridge.addChange(takeOverNonComUnits);
					if (changeTracker != null)
						changeTracker.addChange(takeOverNonComUnits);
				}
			}
		}
		// say they were in combat
		// if the territory being taken over is water, then do not say any land units were in combat (they may want to unload from the transport and attack)
		if (Matches.TerritoryIsWater.match(territory) && arrivedUnits != null)
			arrivedUnits.removeAll(Match.getMatches(arrivedUnits, Matches.UnitIsLand));
		markWasInCombat(arrivedUnits, bridge, changeTracker);
	}
	
	public static void captureOrDestroyUnits(final Territory territory, final PlayerID id, final PlayerID newOwner, final IDelegateBridge bridge, final UndoableMove changeTracker,
				final Collection<Unit> arrivingUnits)
	{
		final GameData data = bridge.getData();
		// destroy any units that should be destroyed on capture
		if (games.strategy.triplea.Properties.getUnitsCanBeDestroyedInsteadOfCaptured(data))
		{
			final CompositeMatch<Unit> enemyToBeDestroyed = new CompositeMatchAnd<Unit>(Matches.enemyUnit(id, data), Matches.UnitDestroyedWhenCapturedByOrFrom(id));
			final Collection<Unit> destroyed = territory.getUnits().getMatches(enemyToBeDestroyed);
			if (!destroyed.isEmpty())
			{
				final Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
				bridge.getHistoryWriter().addChildToEvent("Some non-combat units are destroyed: ", destroyed);
				bridge.addChange(destroyUnits);
				if (changeTracker != null)
					changeTracker.addChange(destroyUnits);
			}
		}
		// destroy any capture on entering units, IF the property to destroy them instead of capture is turned on
		if (games.strategy.triplea.Properties.getOnEnteringUnitsDestroyedInsteadOfCaptured(data))
		{
			final Collection<Unit> destroyed = territory.getUnits().getMatches(Matches.UnitCanBeCapturedOnEnteringToInThisTerritory(id, territory, data));
			if (!destroyed.isEmpty())
			{
				final Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
				bridge.getHistoryWriter().addChildToEvent(id.getName() + " destroys some units instead of capturing them", destroyed);
				bridge.addChange(destroyUnits);
				if (changeTracker != null)
					changeTracker.addChange(destroyUnits);
			}
		}
		// destroy any disabled units owned by the enemy that are NOT infrastructure or factories
		if (true)
		{
			final CompositeMatch<Unit> enemyToBeDestroyed = new CompositeMatchAnd<Unit>(Matches.enemyUnit(id, data), Matches.UnitIsDisabled(), Matches.UnitIsInfrastructure.invert());
			final Collection<Unit> destroyed = territory.getUnits().getMatches(enemyToBeDestroyed);
			if (!destroyed.isEmpty())
			{
				final Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
				bridge.getHistoryWriter().addChildToEvent(id.getName() + " destroys some disabled combat units", destroyed);
				bridge.addChange(destroyUnits);
				if (changeTracker != null)
					changeTracker.addChange(destroyUnits);
			}
		}
		// take over non combatants
		final CompositeMatch<Unit> enemyNonCom = new CompositeMatchAnd<Unit>(Matches.enemyUnit(id, data), Matches.UnitIsInfrastructure);
		final CompositeMatch<Unit> willBeCaptured = new CompositeMatchOr<Unit>(enemyNonCom, Matches.UnitCanBeCapturedOnEnteringToInThisTerritory(id, territory, data));
		final Collection<Unit> nonCom = territory.getUnits().getMatches(willBeCaptured);
		// change any units that change unit types on capture
		if (games.strategy.triplea.Properties.getUnitsCanBeChangedOnCapture(data))
		{
			final Collection<Unit> toReplace = Match.getMatches(nonCom, Matches.UnitWhenCapturedChangesIntoDifferentUnitType());
			for (final Unit u : toReplace)
			{
				final LinkedHashMap<String, Tuple<String, IntegerMap<UnitType>>> map = UnitAttachment.get(u.getType()).getWhenCapturedChangesInto();
				final PlayerID currentOwner = u.getOwner();
				for (final String value : map.keySet())
				{
					final String[] s = value.split(":");
					if (!(s[0].equals("any") || data.getPlayerList().getPlayerID(s[0]).equals(currentOwner)))
						continue;
					// we could use "id" or "newOwner" here... not sure which to use
					if (!(s[1].equals("any") || data.getPlayerList().getPlayerID(s[1]).equals(id)))
						continue;
					final CompositeChange changes = new CompositeChange();
					final Collection<Unit> toAdd = new ArrayList<Unit>();
					final Tuple<String, IntegerMap<UnitType>> toCreate = map.get(value);
					final boolean translateAttributes = toCreate.getFirst().equalsIgnoreCase("true");
					for (final UnitType ut : toCreate.getSecond().keySet())
					{
						// if (ut.equals(u.getType()))
						// continue;
						toAdd.addAll(ut.create(toCreate.getSecond().getInt(ut), newOwner));
					}
					if (!toAdd.isEmpty())
					{
						if (translateAttributes)
						{
							final Change translate = TripleAUnit.translateAttributesToOtherUnits(u, toAdd, territory);
							if (!translate.isEmpty())
								changes.add(translate);
						}
						changes.add(ChangeFactory.removeUnits(territory, Collections.singleton(u)));
						changes.add(ChangeFactory.addUnits(territory, toAdd));
						changes.add(ChangeFactory.markNoMovementChange(toAdd));
						bridge.getHistoryWriter().addChildToEvent(id.getName() + " converts " + u.toStringNoOwner() + " into different units", toAdd);
						bridge.addChange(changes);
						if (changeTracker != null)
							changeTracker.addChange(changes);
						// don't forget to remove this unit from the list
						nonCom.remove(u);
						break;
					}
				}
			}
		}
		// FYI: a dummy delegate will not do anything with this change, meaning that the battle calculator will think this unit lived even though it died or was captured, etc!
		final Change capture = ChangeFactory.changeOwner(nonCom, newOwner, territory);
		bridge.addChange(capture);
		if (changeTracker != null)
			changeTracker.addChange(capture);
		final Change noMovementChange = ChangeFactory.markNoMovementChange(nonCom);
		bridge.addChange(noMovementChange);
		if (changeTracker != null)
			changeTracker.addChange(noMovementChange);
	}
	
	private Change addMustFightBattleChange(final Route route, final Collection<Unit> units, final PlayerID id, final GameData data)
	{
		// it is possible to add a battle with a route that is just
		// the start territory, ie the units did not move into the country
		// they were there to start with
		// this happens when you have submerged subs emerging
		Territory site = route.getEnd();
		if (site == null)
			site = route.getStart();
		// this will be taken care of by the non fighting battle
		if (!Matches.territoryHasEnemyUnits(id, data).match(site))
			return ChangeFactory.EMPTY_CHANGE;
		// if just an enemy factory &/or AA then no battle
		final Collection<Unit> enemyUnits = Match.getMatches(site.getUnits().getUnits(), Matches.enemyUnit(id, data));
		if (route.getEnd() != null && Match.allMatch(enemyUnits, Matches.UnitIsInfrastructure))
			return ChangeFactory.EMPTY_CHANGE;
		IBattle battle = getPendingBattle(site, false);
		// If there are no pending battles- add one for units already in the combat zone
		if (battle == null)
		{
			battle = new MustFightBattle(site, id, data, this);
			m_pendingBattles.add(battle);
			getBattleRecords(data).addBattle(id, battle.getBattleID(), site, battle.getBattleType(), data);
		}
		// Add the units that moved into the battle
		final Change change = battle.addAttackChange(route, units, null);
		// make amphibious assaults dependent on possible naval invasions
		// its only a dependency if we are unloading
		final IBattle precede = getDependentAmphibiousAssault(route);
		if (precede != null && Match.someMatch(units, Matches.UnitIsLand))
		{
			addDependency(battle, precede);
		}
		// dont let land battles in the same territory occur before bombing
		// battles
		final IBattle bombing = getPendingBattle(route.getEnd(), true);
		if (bombing != null)
			addDependency(battle, bombing);
		return change;
	}
	
	private IBattle getDependentAmphibiousAssault(final Route route)
	{
		if (!route.isUnload())
			return null;
		return getPendingBattle(route.getStart(), false);
	}
	
	public IBattle getPendingBattle(final Territory t, final boolean bombing)
	{
		return getPendingBattle(t, bombing, null);
	}
	
	public IBattle getPendingBattle(final Territory t, final boolean bombing, final BattleType type)
	{
		for (final IBattle battle : m_pendingBattles)
		{
			if (battle.getTerritory().equals(t) && battle.isBombingRun() == bombing)
			{
				if (type == null)
					return battle;
				else if (type.equals(battle.getBattleType()))
					return battle;
			}
		}
		return null;
	}
	
	/**
	 * @param bombing
	 *            whether only battles where there is bombing
	 * @return a collection of territories where battles are pending
	 */
	public Collection<Territory> getPendingBattleSites(final boolean bombing)
	{
		final Collection<IBattle> pending = new HashSet<IBattle>(m_pendingBattles);
		final Collection<Territory> battles = new ArrayList<Territory>();
		for (final IBattle battle : pending)
		{
			if (battle != null && !battle.isEmpty() && battle.isBombingRun() == bombing)
				battles.add(battle.getTerritory());
		}
		return battles;
	}
	
	/**
	 * @param blocked
	 *            the battle that is blocked
	 * @return the battle that must occur before dependent can occur
	 */
	public Collection<IBattle> getDependentOn(final IBattle blocked)
	{
		final Collection<IBattle> dependent = m_dependencies.get(blocked);
		if (dependent == null)
			return Collections.emptyList();
		return Match.getMatches(dependent, new InverseMatch<IBattle>(Matches.BattleIsEmpty));
	}
	
	/**
	 * @param blocking
	 *            the battle that is blocking the other battles
	 * @return the battles that cannot occur until the given battle occurs
	 */
	public Collection<IBattle> getBlocked(final IBattle blocking)
	{
		final Iterator<IBattle> iter = m_dependencies.keySet().iterator();
		final Collection<IBattle> allBlocked = new ArrayList<IBattle>();
		while (iter.hasNext())
		{
			final IBattle current = iter.next();
			final Collection<IBattle> currentBlockedBy = getDependentOn(current);
			if (currentBlockedBy.contains(blocking))
				allBlocked.add(current);
		}
		return allBlocked;
	}
	
	public void addDependency(final IBattle blocked, final IBattle blocking)
	{
		if (m_dependencies.get(blocked) == null)
		{
			m_dependencies.put(blocked, new HashSet<IBattle>());
		}
		m_dependencies.get(blocked).add(blocking);
	}
	
	private void removeDependency(final IBattle blocked, final IBattle blocking)
	{
		final Collection<IBattle> dependencies = m_dependencies.get(blocked);
		dependencies.remove(blocking);
		if (dependencies.isEmpty())
		{
			m_dependencies.remove(blocked);
		}
	}
	
	public void removeBattle(final IBattle battle)
	{
		if (battle != null)
		{
			final Iterator<IBattle> blocked = getBlocked(battle).iterator();
			while (blocked.hasNext())
			{
				final IBattle current = blocked.next();
				removeDependency(current, battle);
			}
			m_pendingBattles.remove(battle);
			m_foughBattles.add(battle.getTerritory());
		}
	}
	
	/**
	 * Marks the set of territories as having been the source of a naval
	 * bombardment.
	 * 
	 * @param territories
	 *            a collection of territories
	 */
	public void addPreviouslyNavalBombardmentSource(final Collection<Territory> territories)
	{
		m_bombardedFromTerritories.addAll(territories);
	}
	
	public boolean wasNavalBombardmentSource(final Territory territory)
	{
		return m_bombardedFromTerritories.contains(territory);
	}
	
	private boolean isPacificTheater(final GameData data)
	{
		return data.getProperties().get(Constants.PACIFIC_THEATER, false);
	}
	
	public void clear()
	{
		m_finishedBattlesUnitAttackFromMap.clear();
		m_bombardedFromTerritories.clear();
		m_pendingBattles.clear();
		m_blitzed.clear();
		m_foughBattles.clear();
		m_conquered.clear();
		m_dependencies.clear();
		m_defendingAirThatCanNotLand.clear();
		m_noBombardAllowed.clear();
		m_relationshipChangesThisTurn.clear();
	}
	
	public void addToDefendingAirThatCanNotLand(final Collection<Unit> units, final Territory szTerritoryTheyAreIn)
	{
		Collection<Unit> current = m_defendingAirThatCanNotLand.get(szTerritoryTheyAreIn);
		if (current == null)
			current = new ArrayList<Unit>();
		current.addAll(units);
		m_defendingAirThatCanNotLand.put(szTerritoryTheyAreIn, current);
	}
	
	public Map<Territory, Collection<Unit>> getDefendingAirThatCanNotLand()
	{
		return m_defendingAirThatCanNotLand;
	}
	
	public void clearBattleRecords()
	{
		if (m_battleRecords != null)
		{
			m_battleRecords.clear();
			m_battleRecords = null;
		}
	}
	
	public BattleRecords getBattleRecords(final GameData data)
	{
		if (m_battleRecords == null)
		{
			m_battleRecords = new BattleRecords(data);
		}
		return m_battleRecords;
	}
	
	public void sendBattleRecordsToGameData(final IDelegateBridge aBridge)
	{
		if (m_battleRecords != null && !m_battleRecords.isEmpty())
		{
			aBridge.getHistoryWriter().startEvent("Recording Battle Statistics");
			aBridge.addChange(ChangeFactory.addBattleRecords(m_battleRecords, aBridge.getData()));
		}
	}
	
	@Override
	public String toString()
	{
		return "BattleTracker:" + "\n" + "Conquered:" + m_conquered + "\n" + "Blitzed:" + m_blitzed + "\n" + "Fought:" + m_foughBattles + "\n" + "Pending:" + m_pendingBattles;
	}
	/*
	 //TODO: never used, should it be removed?
	private void addNeutralBattle(Route route, Collection<Unit> units, final PlayerID id, final GameData data, IDelegateBridge bridge,
	        UndoableMove changeTracker)
	{
	    //TODO check for pre existing battles at the sight
	    //here and in empty battle

	    Collection<Territory> neutral = route.getMatches(Matches.TerritoryIsNeutral);
	    neutral = Match.getMatches(neutral, Matches.TerritoryIsEmpty);
	    //deal with the end seperately
	    neutral.remove(route.getEnd());

	    m_conquered.addAll(neutral);

	    Iterator iter = neutral.iterator();
	    while (iter.hasNext())
	    {
	        Territory current = (Territory) iter.next();
	        takeOver(current, id, bridge, data, changeTracker, units);
	    }

	    //deal with end territory, may be the case that
	    //a naval battle must precede there
	    //Also check if there are only factory/AA units left in the neutral territory.
	    Collection<Unit> endUnits = route.getEnd().getUnits().getUnits();
	    if (Matches.TerritoryIsNeutral.match(route.getEnd()) && (Matches.TerritoryIsEmpty.match(route.getEnd()) ||
	            Match.allMatch(endUnits, Matches.UnitIsAAOrIsFactoryOrIsInfrastructure)))
	    {
	        Battle precede = getDependentAmphibiousAssault(route);
	        if (precede == null)
	        {
	            m_conquered.add(route.getEnd());
	            takeOver(route.getEnd(), id, bridge, data, changeTracker, units);
	        } else
	        {
	            Battle nonFight = getPendingBattle(route.getEnd(), false);
	            if (nonFight == null)
	            {
	                nonFight = new NonFightingBattle(route.getEnd(), id, this, true, data);
	                m_pendingBattles.add(nonFight);
	            }

	            Change change = nonFight.addAttackChange(route, units);
	            bridge.addChange(change);
	            if(changeTracker != null)
	            {
	                changeTracker.addChange(change);
	            }
	            addDependency(nonFight, precede);
	        }
	    }
	}
	*/
}
