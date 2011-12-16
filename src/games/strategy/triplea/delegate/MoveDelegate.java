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
 * MoveDelegate.java
 * 
 * Created on November 2, 2001, 12:24 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * 
 * Responsible for moving units on the board.
 * <p>
 * Responsible for checking the validity of a move, and for moving the units. <br>
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 */
public class MoveDelegate extends BaseDelegate implements IMoveDelegate
{
	private boolean m_firstRun = true; // firstRun means when the game is loaded the first time, not when the game is loaded from a save.
	private boolean m_needToInitialize = true; // needToInitialize means we only do certain things once, so that if a game is saved then loaded, they aren't done again
	private boolean m_nonCombat;
	private final TransportTracker m_transportTracker = new TransportTracker();
	private IntegerMap<Territory> m_PUsLost = new IntegerMap<Territory>();
	// if we are in the process of doing a move
	// this instance will allow us to resume the move
	private MovePerformer m_tempMovePerformer;
	// A collection of UndoableMoves
	private List<UndoableMove> m_movesToUndo = new ArrayList<UndoableMove>();
	
	/** Creates new MoveDelegate */
	public MoveDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		super.start(new TripleADelegateBridge(aBridge));
		m_nonCombat = isNonCombat(aBridge);
		final GameData data = getData();
		if (m_firstRun)
			firstRun();
		if (m_needToInitialize)
		{
			// territory property changes triggered at beginning of combat move // TODO create new delegate called "start of turn" and move them there.
			// First set up a match for what we want to have fire as a default in this delegate. List out as a composite match OR.
			// use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
			HashMap<ICondition, Boolean> testedConditions = null;
			final Match<TriggerAttachment> moveCombatDelegateBeforeBonusTriggerMatch = new CompositeMatchOr<TriggerAttachment>(
						TriggerAttachment.notificationMatch(null, null),
						TriggerAttachment.playerPropertyMatch(null, null),
						TriggerAttachment.relationshipTypePropertyMatch(null, null),
						TriggerAttachment.territoryPropertyMatch(null, null),
						TriggerAttachment.territoryEffectPropertyMatch(null, null),
						TriggerAttachment.removeUnitsMatch(null, null));
			
			final Match<TriggerAttachment> moveCombatDelegateAfterBonusTriggerMatch = new CompositeMatchOr<TriggerAttachment>(
						TriggerAttachment.placeMatch(null, null));
			
			final Match<TriggerAttachment> moveCombatDelegateAllTriggerMatch = new CompositeMatchOr<TriggerAttachment>(moveCombatDelegateBeforeBonusTriggerMatch,
						moveCombatDelegateAfterBonusTriggerMatch);
			
			if (!m_nonCombat && games.strategy.triplea.Properties.getTriggers(data))
			{
				final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(new HashSet<PlayerID>(Collections.singleton(m_player)),
							moveCombatDelegateAllTriggerMatch, aBridge);
				if (!toFirePossible.isEmpty())
				{
					// collect conditions and test them for ALL triggers, both those that we will first before and those we will fire after.
					testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, aBridge);
					final HashSet<TriggerAttachment> toFireBeforeBonus = TriggerAttachment.collectForAllTriggersMatching(new HashSet<PlayerID>(Collections.singleton(m_player)),
								moveCombatDelegateBeforeBonusTriggerMatch, aBridge);
					if (!toFireBeforeBonus.isEmpty())
					{
						// get all triggers that are satisfied based on the tested conditions.
						final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<TriggerAttachment>(
									Match.getMatches(toFireBeforeBonus, TriggerAttachment.isSatisfiedMatch(testedConditions)));
						// now list out individual types to fire, once for each of the matches above.
						TriggerAttachment.triggerNotifications(toFireTestedAndSatisfied, aBridge, null, null);
						TriggerAttachment.triggerPlayerPropertyChange(toFireTestedAndSatisfied, aBridge, null, null);
						TriggerAttachment.triggerRelationshipTypePropertyChange(toFireTestedAndSatisfied, aBridge, null, null);
						TriggerAttachment.triggerTerritoryPropertyChange(toFireTestedAndSatisfied, aBridge, null, null);
						TriggerAttachment.triggerTerritoryEffectPropertyChange(toFireTestedAndSatisfied, aBridge, null, null);
						TriggerAttachment.triggerUnitRemoval(toFireTestedAndSatisfied, aBridge, null, null);
					}
				}
			}
			
			// repair 2-hit units at beginning of turn (some maps have combat move before purchase, so i think it is better to do this at beginning of combat move)
			if (!m_nonCombat && games.strategy.triplea.Properties.getBattleships_Repair_At_Beginning_Of_Round(data))
				MoveDelegate.repairBattleShips(aBridge, m_player, true);
			
			// reset any bonus of units.
			if (!m_nonCombat)
				resetBonusMovement();
			
			// give movement to units which begin the turn in the same territory as units with giveMovement (like air and naval bases)
			if (!m_nonCombat && games.strategy.triplea.Properties.getUnitsMayGiveBonusMovement(data))
				giveBonusMovement(aBridge, m_player);
			
			// take away all movement from allied fighters sitting on damaged carriers
			if (!m_nonCombat)
				removeMovementFromAirOnDamagedAlliedCarriers(aBridge, m_player);
			
			// placing triggered units at beginning of combat move, but after bonuses and repairing, etc, have been done.
			if (!m_nonCombat && games.strategy.triplea.Properties.getTriggers(data))
			{
				final HashSet<TriggerAttachment> toFireAfterBonus = TriggerAttachment.collectForAllTriggersMatching(new HashSet<PlayerID>(Collections.singleton(m_player)),
							moveCombatDelegateAfterBonusTriggerMatch, aBridge);
				if (!toFireAfterBonus.isEmpty())
				{
					// get all triggers that are satisfied based on the tested conditions.
					final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<TriggerAttachment>(Match.getMatches(toFireAfterBonus, TriggerAttachment.isSatisfiedMatch(testedConditions)));
					// now list out individual types to fire, once for each of the matches above.
					TriggerAttachment.triggerUnitPlacement(toFireTestedAndSatisfied, aBridge, null, null);
				}
			}
			
			m_needToInitialize = false;
		}
		if (m_tempMovePerformer != null)
		{
			m_tempMovePerformer.initialize(this);
			m_tempMovePerformer.resume();
			m_tempMovePerformer = null;
		}
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		super.end();
		if (m_nonCombat)
			removeAirThatCantLand();
		m_movesToUndo.clear();
		// WW2V2, fires at end of combat move
		// WW2V1, fires at end of non combat move
		if ((!m_nonCombat && isWW2V3()) || (m_nonCombat && (!isWW2V2() && !isWW2V3())) || (!m_nonCombat && isWW2V2()))
		{
			if (TechTracker.hasRocket(m_bridge.getPlayerID()))
			{
				final RocketsFireHelper helper = new RocketsFireHelper();
				helper.fireRockets(m_bridge, m_bridge.getPlayerID());
			}
		}
		final CompositeChange change = new CompositeChange();
		/* This code is already done in BattleDelegate, so why is it duplicated here?
		if(!m_nonCombat && (isWW2V3() || isWW2V2() || isPreviousUnitsFight()))
		{
		    change.add(addLingeringUnitsToBattles());
		}*/
		// do at the end of the round
		// if we do it at the start of non combat, then
		// we may do it in the middle of the round, while loading.
		if (m_nonCombat)
		{
			final GameData data = getData();
			for (final Unit u : data.getUnits())
			{
				if (TripleAUnit.get(u).getAlreadyMoved() != 0)
				{
					change.add(ChangeFactory.unitPropertyChange(u, 0, TripleAUnit.ALREADY_MOVED));
				}
				if (TripleAUnit.get(u).getBonusMovement() != 0)
				{
					change.add(ChangeFactory.unitPropertyChange(u, 0, TripleAUnit.BONUS_MOVEMENT));
				}
				if (TripleAUnit.get(u).getSubmerged())
				{
					change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.SUBMERGED));
				}
			}
			change.add(m_transportTracker.endOfRoundClearStateChange(data));
			m_PUsLost.clear();
		}
		if (!change.isEmpty())
		{
			// if no non-combat occurred, we may have cleanup left from combat
			// that we need to spawn an event for
			m_bridge.getHistoryWriter().startEvent("Cleaning up after movement phases");
			m_bridge.addChange(change);
		}
		m_needToInitialize = true;
	}
	
	@Override
	public Serializable saveState()
	{
		// see below
		return saveState(true);
	}
	
	/**
	 * Returns the state of the Delegate. We dont want to save the undoState if
	 * we are saving the state for an undo move (we dont need it, it will just
	 * take up extra space).
	 */
	private Serializable saveState(final boolean saveUndo)
	{
		final MoveExtendedDelegateState state = new MoveExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_firstRun = m_firstRun;
		state.m_needToInitialize = m_needToInitialize;
		state.m_nonCombat = m_nonCombat;
		if (saveUndo)
			state.m_movesToUndo = m_movesToUndo;
		state.m_PUsLost = m_PUsLost;
		state.m_tempMovePerformer = m_tempMovePerformer;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final MoveExtendedDelegateState s = (MoveExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_firstRun = s.m_firstRun;
		m_needToInitialize = s.m_needToInitialize;
		m_nonCombat = s.m_nonCombat;
		// if the undo state wasnt saved, then dont load it
		// prevents overwriting undo state when we restore from an undo move
		if (s.m_movesToUndo != null)
			m_movesToUndo = s.m_movesToUndo;
		m_PUsLost = s.m_PUsLost;
		m_tempMovePerformer = s.m_tempMovePerformer;
	}
	
	/**
	 * Want to make sure that all units in the sea that can be transported are
	 * marked as being transported by something.
	 * 
	 * We assume that all transportable units in the sea are in a transport, no
	 * exceptions.
	 * 
	 */
	private void firstRun()
	{
		m_firstRun = false;
		// check every territory
		final Iterator<Territory> allTerritories = getData().getMap().getTerritories().iterator();
		while (allTerritories.hasNext())
		{
			final Territory current = allTerritories.next();
			// only care about water
			if (!current.isWater())
				continue;
			final Collection<Unit> units = current.getUnits().getUnits();
			if (units.size() == 0 || !Match.someMatch(units, Matches.UnitIsLand))
				continue;
			// map transports, try to fill
			final Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
			final Collection<Unit> land = Match.getMatches(units, Matches.UnitIsLand);
			for (Unit toLoad  : land) {
				final UnitAttachment ua = UnitAttachment.get(toLoad.getType());
				final int cost = ua.getTransportCost();
				if (cost == -1)
					throw new IllegalStateException("Non transportable unit in sea");
				// find the next transport that can hold it
				final Iterator<Unit> transportIter = transports.iterator();
				boolean found = false;
				while (transportIter.hasNext())
				{
					final Unit transport = transportIter.next();
					final int capacity = m_transportTracker.getAvailableCapacity(transport);
					if (capacity >= cost)
					{
						m_bridge.addChange(m_transportTracker.loadTransportChange((TripleAUnit) transport, toLoad, m_player));
						found = true;
						break;
					}
				}
				if (!found)
					throw new IllegalStateException("Cannot load all land units in sea transports. " + "Please make sure you have enough transports. "
								+ "You may need to re-order the xml's placement of transports and land units, " + "as the engine will try to fill them in the order they are given.");
			}
		}
	}
	
	public static boolean isNonCombat(final IDelegateBridge aBridge)
	{
		if (aBridge.getStepName().endsWith("NonCombatMove"))
			return true;
		else if (aBridge.getStepName().endsWith("CombatMove"))
			return false;
		else
			throw new IllegalStateException("Cannot determine combat or not");
	}
	
	private void resetBonusMovement()
	{
		final GameData data = getData();
		final CompositeChange change = new CompositeChange();
		for (final Unit u : data.getUnits())
		{
			if (TripleAUnit.get(u).getBonusMovement() != 0)
			{
				change.add(ChangeFactory.unitPropertyChange(u, 0, TripleAUnit.BONUS_MOVEMENT));
			}
		}
		if (!change.isEmpty())
		{
			m_bridge.getHistoryWriter().startEvent("Reseting Bonus Movement of Units");
			m_bridge.addChange(change);
		}
	}
	
	private void removeMovementFromAirOnDamagedAlliedCarriers(final IDelegateBridge aBridge, final PlayerID player)
	{
		final GameData data = aBridge.getData();
		final Match<Unit> crippledAlliedCarriersMatch = new CompositeMatchAnd<Unit>(Matches.isUnitAllied(player, data), Matches.unitIsOwnedBy(player).invert(), Matches.UnitIsCarrier,
					Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER));
		final Match<Unit> ownedFightersMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir, Matches.UnitCanLandOnCarrier);
		final CompositeChange change = new CompositeChange();
		for (final Territory t : data.getMap().getTerritories())
		{
			final Collection<Unit> ownedFighters = t.getUnits().getMatches(ownedFightersMatch);
			if (ownedFighters.isEmpty())
				continue;
			final Collection<Unit> crippledAlliedCarriers = Match.getMatches(t.getUnits().getUnits(), crippledAlliedCarriersMatch);
			if (crippledAlliedCarriers.isEmpty())
				continue;
			for (final Unit fighter : ownedFighters)
			{
				final TripleAUnit taUnit = (TripleAUnit) fighter;
				if (taUnit.getTransportedBy() != null)
				{
					if (crippledAlliedCarriers.contains(taUnit.getTransportedBy()))
						change.add(ChangeFactory.markNoMovementChange(fighter));
				}
			}
		}
		if (!change.isEmpty())
			aBridge.addChange(change);
	}
	
	private void giveBonusMovement(final IDelegateBridge aBridge, final PlayerID player)
	{
		final GameData data = aBridge.getData();
		final CompositeChange change = new CompositeChange();
		for (final Territory t : data.getMap().getTerritories())
		{
			for (final Unit u : t.getUnits().getUnits())
			{
				if (Matches.UnitCanBeGivenBonusMovementByFacilitiesInItsTerritory(t, player, data).match(u))
				{
					if (!Matches.isUnitAllied(player, data).match(u))
						continue;
					int bonusMovement = Integer.MIN_VALUE;
					final Collection<Unit> givesBonusUnits = new ArrayList<Unit>();
					final Match<Unit> givesBonusUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanGiveBonusMovementToThisUnit(u));
					givesBonusUnits.addAll(Match.getMatches(t.getUnits().getUnits(), givesBonusUnit));
					if (Matches.UnitIsSea.match(u))
					{
						final Match<Unit> givesBonusUnitLand = new CompositeMatchAnd<Unit>(givesBonusUnit, Matches.UnitIsLand);
						final List<Territory> neighbors = new ArrayList<Territory>(data.getMap().getNeighbors(t, Matches.TerritoryIsLand));
						for (Territory current  : neighbors) {
							givesBonusUnits.addAll(Match.getMatches(current.getUnits().getUnits(), givesBonusUnitLand));
						}
					}
					for (final Unit bonusGiver : givesBonusUnits)
					{
						final int tempBonus = UnitAttachment.get(bonusGiver.getType()).getGivesMovement().getInt(u.getType());
						if (tempBonus > bonusMovement)
							bonusMovement = tempBonus;
					}
					if (bonusMovement != Integer.MIN_VALUE && bonusMovement != 0)
					{
						bonusMovement = Math.max(bonusMovement, (UnitAttachment.get(u.getType()).getMovement(player) * -1));
						change.add(ChangeFactory.unitPropertyChange(u, bonusMovement, TripleAUnit.BONUS_MOVEMENT));
					}
				}
			}
		}
		if (!change.isEmpty())
		{
			aBridge.getHistoryWriter().startEvent("Giving bonus movement to units");
			aBridge.addChange(change);
		}
	}
	
	public static void repairBattleShips(final IDelegateBridge aBridge, final PlayerID player, final boolean beforeTurn)
	{
		final GameData data = aBridge.getData();
		final Match<Unit> damagedBattleship = new CompositeMatchAnd<Unit>(Matches.UnitIsTwoHit, Matches.UnitIsDamaged);
		final Match<Unit> damagedBattleshipOwned = new CompositeMatchAnd<Unit>(damagedBattleship, Matches.unitIsOwnedBy(player));
		final Collection<Unit> damaged = new ArrayList<Unit>();
		final Iterator<Territory> iterTerritories = data.getMap().getTerritories().iterator();
		while (iterTerritories.hasNext())
		{
			final Territory current = iterTerritories.next();
			if (!games.strategy.triplea.Properties.getTwoHitPointUnitsRequireRepairFacilities(data))
			{
				if (beforeTurn)
					damaged.addAll(current.getUnits().getMatches(damagedBattleshipOwned)); // we only repair ours
				else
					damaged.addAll(current.getUnits().getMatches(damagedBattleship)); // we repair everyone's
			}
			else
				damaged.addAll(current.getUnits().getMatches(new CompositeMatchAnd<Unit>(damagedBattleshipOwned, Matches.UnitCanBeRepairedByFacilitiesInItsTerritory(current, player, data))));
		}
		if (damaged.size() == 0)
			return;
		// now if damaged includes any carriers that are repairing, and have damaged abilities set for not allowing air units to leave while damaged, we need to remove those air units now
		final Collection<Unit> damagedCarriers = Match.getMatches(damaged, Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER));
		final IntegerMap<Unit> hits = new IntegerMap<Unit>();
		final Iterator<Unit> iterUnits = damaged.iterator();
		while (iterUnits.hasNext())
		{
			final Unit unit = iterUnits.next();
			hits.put(unit, 0);
		}
		aBridge.addChange(ChangeFactory.unitsHit(hits));
		aBridge.getHistoryWriter().startEvent(damaged.size() + " " + MyFormatter.pluralize("unit", damaged.size()) + " repaired.");
		// now cycle through those now-repaired carriers, and remove allied air from being dependant
		final CompositeChange clearAlliedAir = new CompositeChange();
		for (final Unit carrier : damagedCarriers)
		{
			clearAlliedAir.add(MustFightBattle.clearTransportedByForAlliedAirOnCarrier(Collections.singleton(carrier), carrier.getTerritoryUnitIsIn(), carrier.getOwner(), data));
		}
		if (!clearAlliedAir.isEmpty())
			aBridge.addChange(clearAlliedAir);
	}
	
	public List<UndoableMove> getMovesMade()
	{
		return new ArrayList<UndoableMove>(m_movesToUndo);
	}
	
	public String undoMove(final int moveIndex)
	{
		if (m_movesToUndo.isEmpty())
			return "No moves to undo";
		if (moveIndex >= m_movesToUndo.size())
			return "Undo move index out of range";
		final UndoableMove moveToUndo = m_movesToUndo.get(moveIndex);
		if (!moveToUndo.getcanUndo())
			return moveToUndo.getReasonCantUndo();
		moveToUndo.undo(getData(), m_bridge);
		m_movesToUndo.remove(moveIndex);
		updateUndoableMoveIndexes();
		return null;
	}
	
	private void updateUndoableMoveIndexes()
	{
		for (int i = 0; i < m_movesToUndo.size(); i++)
		{
			m_movesToUndo.get(i).setIndex(i);
		}
	}
	
	public String move(final Collection<Unit> units, final Route route)
	{
		return move(units, route, Collections.<Unit> emptyList());
	}
	
	public String move(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded)
	{
		final GameData data = getData();
		final MoveValidationResult result = MoveValidator.validateMove(units, route, m_player, transportsThatCanBeLoaded, m_nonCombat, m_movesToUndo, data);
		final StringBuilder errorMsg = new StringBuilder(100);
		final int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);
		final String numErrorsMsg = numProblems > 0 ? ("; " + numProblems + " " + MyFormatter.pluralize("error", numProblems) + " not shown") : "";
		if (result.hasError())
			return errorMsg.append(result.getError()).append(numErrorsMsg).toString();
		if (result.hasDisallowedUnits())
			return errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString();
		boolean isKamikaze = false;
		final boolean getKamikazeAir = games.strategy.triplea.Properties.getKamikaze_Airplanes(data);
		Collection<Unit> kamikazeUnits = new ArrayList<Unit>();
		// boolean isHariKari = false;
		// confirm kamikaze moves, and remove them from unresolved units
		// if(m_data.getProperties().get(Constants.KAMIKAZE, false))
		if (getKamikazeAir || Match.someMatch(units, Matches.UnitIsKamikaze))
		{
			kamikazeUnits = result.getUnresolvedUnits(MoveValidator.NOT_ALL_AIR_UNITS_CAN_LAND);
			if (kamikazeUnits.size() > 0 && getRemotePlayer().confirmMoveKamikaze())
			{
				for (final Unit unit : kamikazeUnits)
				{
					if (getKamikazeAir || Matches.UnitIsKamikaze.match(unit))
					{
						result.removeUnresolvedUnit(MoveValidator.NOT_ALL_AIR_UNITS_CAN_LAND, unit);
						isKamikaze = true;
					}
				}
			}
		}
		// confirm HariKari moves, and remove them from unresolved units
		/*if(m_data.getProperties().get(Constants.HARI_KARI, false))
		{
		    Collection<Unit> hariKariUnits = result.getUnresolvedUnits(MoveValidator.UNESCORTED_UNITS_WILL_DIE_IN_COMBAT);
		    if (hariKariUnits.size() > 0 && getRemotePlayer().confirmMoveHariKari())
		    {
		        for (Unit unit : hariKariUnits)
		        {
		            result.removeUnresolvedUnit(MoveValidator.UNESCORTED_UNITS_WILL_DIE_IN_COMBAT, unit);
		            isHariKari = true;
		        }
		    }
		}        */
		if (result.hasUnresolvedUnits())
			return errorMsg.append(result.getUnresolvedUnitWarning(0)).append(numErrorsMsg).toString();
		// allow user to cancel move if aa guns will fire
		final AAInMoveUtil aaInMoveUtil = new AAInMoveUtil();
		aaInMoveUtil.initialize(m_bridge);
		final Collection<Territory> aaFiringTerritores = aaInMoveUtil.getTerritoriesWhereAAWillFire(route, units);
		if (!aaFiringTerritores.isEmpty())
		{
			if (!getRemotePlayer().confirmMoveInFaceOfAA(aaFiringTerritores))
				return null;
		}
		// do the move
		final UndoableMove currentMove = new UndoableMove(data, units, route);
		final String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName() + " to " + route.getEnd().getName();
		m_bridge.getHistoryWriter().startEvent(transcriptText);
		if (isKamikaze)
		{
			m_bridge.getHistoryWriter().addChildToEvent("This was a kamikaze move, for at least some of the units", kamikazeUnits);
		}
		/*if(isHariKari)
		{
		    m_bridge.getHistoryWriter().addChildToEvent("This was a Hari-Kari move");
		}*/
		// MoveDescription description = new MoveDescription(units, route);
		// m_bridge.getHistoryWriter().setRenderingData(description);
		m_bridge.getHistoryWriter().setRenderingData(currentMove.getDescriptionObject());
		m_tempMovePerformer = new MovePerformer();
		m_tempMovePerformer.initialize(this);
		m_tempMovePerformer.moveUnits(units, route, m_player, transportsThatCanBeLoaded, currentMove);
		m_tempMovePerformer = null;
		return null;
	}
	
	void updateUndoableMoves(final UndoableMove currentMove)
	{
		currentMove.initializeDependencies(m_movesToUndo);
		m_movesToUndo.add(currentMove);
		updateUndoableMoveIndexes();
	}
	
	public static BattleTracker getBattleTracker(final GameData data)
	{
		return DelegateFinder.battleDelegate(data).getBattleTracker();
	}
	
	private boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(getData());
	}
	
	/*private boolean isPreviousUnitsFight()
	{
		return games.strategy.triplea.Properties.getPreviousUnitsFight(m_data);
	}*/
	private boolean isWW2V3()
	{
		return games.strategy.triplea.Properties.getWW2V3(getData());
	}
	
	private ITripleaPlayer getRemotePlayer()
	{
		return getRemotePlayer(m_player);
	}
	
	private ITripleaPlayer getRemotePlayer(final PlayerID id)
	{
		return (ITripleaPlayer) m_bridge.getRemote(id);
	}
	
	public static Collection<Territory> getEmptyNeutral(final Route route)
	{
		final Match<Territory> emptyNeutral = new CompositeMatchAnd<Territory>(Matches.TerritoryIsEmpty, Matches.TerritoryIsNeutral);
		final Collection<Territory> neutral = route.getMatches(emptyNeutral);
		return neutral;
	}
	
	public Change ensureCanMoveOneSpaceChange(final Unit unit)
	{
		final int alreadyMoved = TripleAUnit.get(unit).getAlreadyMoved();
		final int maxMovement = UnitAttachment.get(unit.getType()).getMovement(unit.getOwner());
		final int bonusMovement = TripleAUnit.get(unit).getBonusMovement();
		return ChangeFactory.unitPropertyChange(unit, Math.min(alreadyMoved, (maxMovement + bonusMovement) - 1), TripleAUnit.ALREADY_MOVED);
	}
	
	/* This code is already done in BattleDelegate, so why is it duplicated here?
	private Change addLingeringUnitsToBattles()
	{
		// if an enemy placed units in a hostile sea zone
		// and then during combat move, we attacked the newly placed units
		// our units in the sea zone need to join the battle
		// also, if our relation state with the nation changed to an at-war type of relation, we will need to add all units in those territories
		CompositeChange change = new CompositeChange();
		BattleTracker tracker = getBattleTracker(m_data);
		for (Territory t : tracker.getPendingBattleSites(false))
		{
			if (!(tracker.getPendingBattle(t, false) instanceof MustFightBattle))
			{
				continue;
			}
			MustFightBattle mfb = (MustFightBattle) tracker.getPendingBattle(t, false);
			Set<Unit> ownedUnits;
			if (t.isWater())
			{
				ownedUnits = new HashSet<Unit>(t.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsLand.invert(), Matches.unitIsOwnedBy(m_player))));
			}
			else
			{
				ownedUnits = new HashSet<Unit>(t.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsSea.invert(), Matches.unitIsOwnedBy(m_player))));
			}
			ownedUnits.removeAll(mfb.getAttackingUnits());
			if (!ownedUnits.isEmpty())
			{
				change.add(mfb.addAttackChange(new Route(t), ownedUnits));
			}
			
		}
		return change;
	}*/
	private void removeAirThatCantLand()
	{
		final GameData data = getData();
		final boolean lhtrCarrierProd = AirThatCantLandUtil.isLHTRCarrierProduction(data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
		final boolean hasProducedCarriers = m_player.getUnits().someMatch(Matches.UnitIsCarrier);
		final AirThatCantLandUtil util = new AirThatCantLandUtil(m_bridge);
		util.removeAirThatCantLand(m_player, lhtrCarrierProd && hasProducedCarriers);
		// if edit mode has been on, we need to clean up after all players
		for (PlayerID player  : data.getPlayerList()) {
			// Check if player still has units to place
			if (!player.equals(m_player)) // && !player.getUnits().isEmpty()
				util.removeAirThatCantLand(player, ((player.getUnits().someMatch(Matches.UnitIsCarrier) || hasProducedCarriers) && lhtrCarrierProd));
		}
	}
	
	/**
	 * This method is static so it can be called from the client side.
	 * 
	 * @param route
	 *            referring route
	 * @param units
	 *            referring units
	 * @param transportsToLoad
	 *            units to be loaded
	 * @return a map of unit -> transport (null if no mapping can be
	 *         done either because there is not sufficient transport capacity or because
	 *         a unit is not with its transport)
	 */
	public static Map<Unit, Unit> mapTransports(final Route route, final Collection<Unit> units, final Collection<Unit> transportsToLoad)
	{
		if (route.isLoad())
			return mapTransportsToLoad(units, transportsToLoad);
		if (route.isUnload())
			return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
		return mapTransportsAlreadyLoaded(units, units);
	}
	
	/**
	 * This method is static so it can be called from the client side.
	 * 
	 * @param route
	 *            referring route
	 * @param units
	 *            referring units
	 * @param transportsToLoad
	 *            units to be loaded
	 * @param isload
	 * @param player
	 *            PlayerID
	 * @return a map of unit -> transport (null if no mapping can be
	 *         done either because there is not sufficient transport capacity or because
	 *         a unit is not with its transport)
	 */
	public static Map<Unit, Unit> mapTransports(final Route route, final Collection<Unit> units, final Collection<Unit> transportsToLoad, final boolean isload, final PlayerID player)
	{
		if (isload)
			return mapTransportsToLoad(units, transportsToLoad);
		if (route.isUnload())
			return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
		return mapTransportsAlreadyLoaded(units, units);
	}
	
	/**
	 * This method is static so it can be called from the client side.
	 * 
	 * @param route
	 *            referring route
	 * @param units
	 *            referring units
	 * @param transportsToLoad
	 *            units to be loaded
	 * @param isload
	 * @param player
	 *            PlayerID
	 * @return a map of unit -> air transport (null if no mapping can be
	 *         done either because there is not sufficient transport capacity or because
	 *         a unit is not with its transport)
	 */
	public static Map<Unit, Unit> mapAirTransports(final Route route, final Collection<Unit> units, final Collection<Unit> transportsToLoad, final boolean isload, final PlayerID player)
	{
		return mapTransports(route, units, transportsToLoad, isload, player);
		// return mapUnitsToAirTransports(units, Match.getMatches(transportsToLoad, Matches.UnitIsAirTransport));
	}
	
	/**
	 * This method is static so it can be called from the client side.
	 * 
	 * @param route
	 *            referring route
	 * @param units
	 *            referring units
	 * @param transportsToLoad
	 * @param isload
	 * @param player
	 *            PlayerID
	 * @return list of max number of each type of unit that may be loaded
	 */
	public static List<Unit> mapAirTransportPossibilities(final Route route, final Collection<Unit> units, final Collection<Unit> transportsToLoad, final boolean isload, final PlayerID player)
	{
		return mapAirTransportsToLoad2(units, Match.getMatches(transportsToLoad, Matches.UnitIsAirTransport));
	}
	
	/**
	 * Returns a map of unit -> transport. Unit must already be loaded in the
	 * transport. If no units are loaded in the transports then an empty Map will
	 * be returned.
	 */
	private static Map<Unit, Unit> mapTransportsAlreadyLoaded(final Collection<Unit> units, final Collection<Unit> transports)
	{
		final Collection<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
		final Collection<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
		final TransportTracker transportTracker = new TransportTracker();
		final Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
		final Iterator<Unit> land = canBeTransported.iterator();
		while (land.hasNext())
		{
			final Unit currentTransported = land.next();
			final Unit transport = transportTracker.transportedBy(currentTransported);
			// already being transported, make sure it is in transports
			if (transport == null)
				continue;
			if (!canTransport.contains(transport))
				continue;
			mapping.put(currentTransported, transport);
		}
		return mapping;
	}
	
	/**
	 * Returns a map of unit -> transport. Tries to find transports to load all
	 * units. If it can't succeed returns an empty Map.
	 * 
	 */
	private static Map<Unit, Unit> mapTransportsToLoad(final Collection<Unit> units, final Collection<Unit> transports)
	{
		final List<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
		int transportIndex = 0;
		final TransportTracker transportTracker = new TransportTracker();
		final Comparator<Unit> c = new Comparator<Unit>()
		{
			public int compare(final Unit o1, final Unit o2)
			{
				final int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
				final int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
				return cost2 - cost1;
			}
		};
		// fill the units with the highest cost first.
		// allows easy loading of 2 infantry and 2 tanks on 2 transports
		// in WW2V2 rules.
		Collections.sort(canBeTransported, c);
		final List<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
		final Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
		final IntegerMap<Unit> addedLoad = new IntegerMap<Unit>();
		for (Unit land  : canBeTransported) {
			final UnitAttachment landUA = UnitAttachment.get(land.getType());
			final int cost = landUA.getTransportCost();
			boolean loaded = false;
			// we want to try to distribute units evenly to all the transports
			// if the user has 2 infantry, and selects two transports to load
			// we should put 1 infantry in each transport.
			// the algorithm below does not guarantee even distribution in all cases
			// but it solves most of the cases
			// TODO review the following loop in light of bug ticket 2827064- previously unloaded trns perhaps shouldn't be included.
			final Iterator<Unit> transportIter = Util.shiftElementsToEnd(canTransport, transportIndex).iterator();
			while (transportIter.hasNext() && !loaded)
			{
				transportIndex++;
				if (transportIndex >= canTransport.size())
					transportIndex = 0;
				final Unit transport = transportIter.next();
				int capacity = transportTracker.getAvailableCapacity(transport);
				capacity -= addedLoad.getInt(transport);
				if (capacity >= cost)
				{
					addedLoad.add(transport, cost);
					mapping.put(land, transport);
					loaded = true;
				}
			}
		}
		return mapping;
	}
	
	private static List<Unit> mapAirTransportsToLoad2(final Collection<Unit> units, final Collection<Unit> transports)
	{
		final Comparator<Unit> c = new Comparator<Unit>()
		{
			public int compare(final Unit o1, final Unit o2)
			{
				final int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
				final int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
				return cost2 - cost1; // descending transportCost
			}
		};
		Collections.sort((List<Unit>) units, c);
		// Define the max of all units that could be loaded
		final List<Unit> totalLoad = new ArrayList<Unit>();
		// Get a list of the unit categories
		final Collection<UnitCategory> unitTypes = UnitSeperator.categorize(units, null, false, true);
		final Collection<UnitCategory> transportTypes = UnitSeperator.categorize(transports, null, false, false);
		for (final UnitCategory unitType : unitTypes)
		{
			final int transportCost = unitType.getTransportCost();
			for (final UnitCategory transportType : transportTypes)
			{
				final int transportCapacity = UnitAttachment.get(transportType.getType()).getTransportCapacity();
				if (transportCost > 0 && transportCapacity >= transportCost)
				{
					final int transportCount = Match.countMatches(transports, Matches.unitIsOfType(transportType.getType()));
					final int ttlTransportCapacity = transportCount * (int) Math.floor(transportCapacity / transportCost);
					totalLoad.addAll(Match.getNMatches(units, ttlTransportCapacity, Matches.unitIsOfType(unitType.getType())));
				}
			}
		}
		return totalLoad;
	}
	
	public Collection<Territory> getTerritoriesWhereAirCantLand(final PlayerID player)
	{
		return new AirThatCantLandUtil(m_bridge).getTerritoriesWhereAirCantLand(player);
	}
	
	public Collection<Territory> getTerritoriesWhereAirCantLand()
	{
		return new AirThatCantLandUtil(m_bridge).getTerritoriesWhereAirCantLand(m_player);
	}
	
	public Collection<Territory> getTerritoriesWhereUnitsCantFight()
	{
		return new UnitsThatCantFightUtil(getData()).getTerritoriesWhereUnitsCantFight(m_player);
	}
	
	/**
	 * @param unit
	 *            referring unit
	 * @param end
	 *            target territory
	 * @return the route that a unit used to move into the given territory
	 */
	public Route getRouteUsedToMoveInto(final Unit unit, final Territory end)
	{
		return MoveDelegate.getRouteUsedToMoveInto(m_movesToUndo, unit, end);
	}
	
	/**
	 * This method is static so it can be called from the client side.
	 * 
	 * @param undoableMoves
	 *            list of moves that have been done
	 * @param unit
	 *            referring unit
	 * @param end
	 *            target territory
	 * @return the route that a unit used to move into the given territory.
	 */
	public static Route getRouteUsedToMoveInto(final List<UndoableMove> undoableMoves, final Unit unit, final Territory end)
	{
		final ListIterator<UndoableMove> iter = undoableMoves.listIterator(undoableMoves.size());
		while (iter.hasPrevious())
		{
			final UndoableMove move = iter.previous();
			if (!move.getUnits().contains(unit))
				continue;
			if (move.getRoute().getEnd().equals(end))
				return move.getRoute();
		}
		return null;
	}
	
	/**
	 * @param t
	 *            referring territory
	 * @return the number of PUs that have been lost by bombing, rockets, etc.
	 */
	public int PUsAlreadyLost(final Territory t)
	{
		return m_PUsLost.getInt(t);
	}
	
	/**
	 * Add more PUs lost to a territory due to bombing, rockets, etc.
	 * 
	 * @param t
	 *            referring territoy
	 * @param amt
	 *            amount of PUs that should be added
	 */
	public void PUsLost(final Territory t, final int amt)
	{
		m_PUsLost.add(t, amt);
	}
	
	public TransportTracker getTransportTracker()
	{
		return m_transportTracker;
	}
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<IMoveDelegate> getRemoteType()
	{
		return IMoveDelegate.class;
	}
	/*
	 * never localy used


	/**
	 * Returns a list of the maximum number of each type of unit that can be loaded on the transports
	 * If it can't succeed returns an empty Map.
	 *
	 *
	private static List<Unit> mapAirTransportsToLoad(Collection<Unit> units, Collection<Unit> transports)
	{
	    Comparator<Unit> c = new Comparator<Unit>()
	    {
	        public int compare(Unit o1, Unit o2)
	        {
	            int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
	            int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
	            return cost2 - cost1; //descending transportCost
	        }
	    };
	    Collections.sort((List<Unit>) units, c);

	    Iterator<Unit> trnIter = transports.iterator();
	    //Spin through each transport and find the possible loads
	    List<Unit> totalLoad = new ArrayList<Unit>();
	    while(trnIter.hasNext())
	    {
	        //(re)set the initial and current capacity of the air transport
	        Unit transport = trnIter.next();
	        UnitAttachment trnA = (UnitAttachment) transport.getType().getAttachment(Constants.UNIT_ATTATCHMENT_NAME);
	        int initCapacity = trnA.getTransportCapacity();
	        int currCapacity = initCapacity;

	        //set up a list for a single potential load
	        List<Unit> aLoad = new ArrayList<Unit>();
	        Iterator<Unit> unitIter = units.iterator();
	        IntegerMap<Unit> addedLoad = new IntegerMap<Unit>();
	        while (unitIter.hasNext())
	        {
	            //For each potential unit, get transport cost
	            Unit unit = unitIter.next();
	            UnitAttachment ua = (UnitAttachment) unit.getType().getAttachment(Constants.UNIT_ATTATCHMENT_NAME);
	            int cost = ua.getTransportCost();
	            //Check the cost against the air transport's current capacity (including previously loaded units)
	            currCapacity -= addedLoad.getInt(transport);
	            if(currCapacity >= cost )
	            {
	                addedLoad.add(transport, cost);
	                aLoad.add(unit);
	            }
	            else
	            {
	                //If there's no available capacity, consider the load full and add to total
	                totalLoad.addAll(aLoad);
	                addedLoad.clear();
	                aLoad.clear();
	                //see if any units like the current unit were previously loaded
	                Iterator<Unit> ttlIter = totalLoad.listIterator();
	                List<Integer> indices = new ArrayList<Integer>();
	                while (ttlIter.hasNext())
	                {
	                    Unit ttlUnit = ttlIter.next();
	                    if(unit != ttlUnit && unit.getType().equals(ttlUnit.getType()))
	                    {
	                        indices.add(totalLoad.indexOf(ttlUnit));
	                    }
	                }
	                //If there are any, add up their transportCosts and see if there is room for another.
	                currCapacity = initCapacity;
	                if(indices.isEmpty())
	                {
	                    if(currCapacity >= cost )
	                    {
	                        addedLoad.add(transport, cost);
	                        aLoad.add(unit);
	                    }
	                }
	                else
	                {
	                    //reload aLoad with any units of the same type & check capacity vs aLoad cost
	                    //this eliminates too many duplicate units in the list
	                    Iterator<Integer> indCosts = indices.listIterator();
	                    while(indCosts.hasNext())
	                    {
	                        Integer index = indCosts.next();
	                        Unit indexedUnit = totalLoad.get(index);
	                        UnitAttachment indexedUnitAtt = (UnitAttachment) indexedUnit.getType().getAttachment(Constants.UNIT_ATTATCHMENT_NAME);
	                        currCapacity -= indexedUnitAtt.getTransportCost();
	                    }
	                    if(currCapacity >= cost )
	                    {
	                        addedLoad.add(transport, cost);
	                        aLoad.add(unit);
	                    }
	                }
	            }
	        }
	        //If there's no available capacity, consider the load full and add to total
	        totalLoad.addAll(aLoad);
	    }

	    return totalLoad;
	}
	 */
}


@SuppressWarnings("serial")
class MoveExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
	public boolean m_firstRun = true;
	public boolean m_needToInitialize;
	public boolean m_nonCombat;
	public IntegerMap<Territory> m_PUsLost;
	public List<UndoableMove> m_movesToUndo;
	public MovePerformer m_tempMovePerformer;
}
