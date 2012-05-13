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
package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.attatchments.TechAbilityAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 
 * Code to fire AA guns while in combat and non combat move.
 * 
 * @author Sean Bridges
 */
class AAInMoveUtil implements Serializable
{
	private static final long serialVersionUID = 1787497998642717678L;
	private transient boolean m_nonCombat;
	private transient IDelegateBridge m_bridge;
	private transient PlayerID m_player;
	private Collection<Unit> m_casualties;
	private final ExecutionStack m_executionStack = new ExecutionStack();
	
	AAInMoveUtil()
	{
	}
	
	public AAInMoveUtil initialize(final IDelegateBridge bridge)
	{
		m_nonCombat = MoveDelegate.isNonCombat(bridge);
		m_bridge = bridge;
		m_player = bridge.getPlayerID();
		return this;
	}
	
	private GameData getData()
	{
		return m_bridge.getData();
	}
	
	private boolean isAlwaysONAAEnabled()
	{
		return games.strategy.triplea.Properties.getAlwaysOnAA(getData());
	}
	
	private boolean isAATerritoryRestricted()
	{
		return games.strategy.triplea.Properties.getAATerritoryRestricted(getData());
	}
	
	private ITripleaPlayer getRemotePlayer(final PlayerID id)
	{
		return (ITripleaPlayer) m_bridge.getRemote(id);
	}
	
	private ITripleaPlayer getRemotePlayer()
	{
		return getRemotePlayer(m_player);
	}
	
	/**
	 * Fire aa guns. Returns units to remove.
	 */
	Collection<Unit> fireAA(final Route route, final Collection<Unit> units, final Comparator<Unit> decreasingMovement, final UndoableMove currentMove)
	{
		if (m_executionStack.isEmpty())
			populateExecutionStack(route, units, decreasingMovement, currentMove);
		m_executionStack.execute(m_bridge);
		return m_casualties;
	}
	
	private void populateExecutionStack(final Route route, final Collection<Unit> units, final Comparator<Unit> decreasingMovement, final UndoableMove currentMove)
	{
		final List<Unit> targets = new ArrayList<Unit>(units);
		// select units with lowest movement first
		Collections.sort(targets, decreasingMovement);
		final List<IExecutable> executables = new ArrayList<IExecutable>();
		final Iterator<Territory> iter = getTerritoriesWhereAAWillFire(route, units).iterator();
		while (iter.hasNext())
		{
			final Territory location = iter.next();
			executables.add(new IExecutable()
			{
				private static final long serialVersionUID = -1545771595683434276L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					fireAA(location, targets, currentMove);
				}
			});
		}
		Collections.reverse(executables);
		m_executionStack.push(executables);
	}
	
	Collection<Territory> getTerritoriesWhereAAWillFire(final Route route, final Collection<Unit> units)
	{
		final boolean alwaysOnAA = isAlwaysONAAEnabled();
		// Just the attacked territory will have AA firing
		if (!alwaysOnAA && isAATerritoryRestricted())
			return Collections.emptyList();
		// No AA in nonCombat unless 'Always on AA'
		if (m_nonCombat && !alwaysOnAA)
			return Collections.emptyList();
		// can't rely on m_player being the unit owner in Edit Mode
		// look at the units being moved to determine allies and enemies
		final PlayerID movingPlayer = movingPlayer(units);
		final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed = TechAbilityAttachment.getAirborneTargettedByAA(movingPlayer, getData());
		// don't iterate over the end
		// that will be a battle
		// and handled else where in this tangled mess
		final Match<Unit> hasAA = Matches.UnitIsAAthatCanFire(units, airborneTechTargetsAllowed, movingPlayer, Matches.UnitIsAAforFlyOverOnly, getData());
		// AA guns in transports shouldn't be able to fire
		final List<Territory> territoriesWhereAAWillFire = new ArrayList<Territory>();
		for (final Territory current : route.getMiddleSteps())
		{
			if (current.getUnits().someMatch(hasAA))
			{
				territoriesWhereAAWillFire.add(current);
			}
		}
		// check start as well, prevent user from moving to and from AA sites one at a time
		// if there was a battle fought there then don't fire, this covers the case where we fight, and always on AA wants to fire after the battle.
		// TODO: there is a bug in which if you move an air unit to a battle site in the middle of non combat, it wont fire
		if (route.getStart().getUnits().someMatch(hasAA) && !getBattleTracker().wasBattleFought(route.getStart()))
			territoriesWhereAAWillFire.add(route.getStart());
		
		if (games.strategy.triplea.Properties.getForceAAattacksForLastStepOfFlyOver(getData()) && route.getEnd().getUnits().someMatch(hasAA))
			territoriesWhereAAWillFire.add(route.getEnd());
		return territoriesWhereAAWillFire;
	}
	
	private BattleTracker getBattleTracker()
	{
		return DelegateFinder.battleDelegate(getData()).getBattleTracker();
	}
	
	private PlayerID movingPlayer(final Collection<Unit> units)
	{
		if (Match.someMatch(units, Matches.unitIsOwnedBy(m_player)))
			return m_player;
		else
			return units.iterator().next().getOwner();
	}
	
	/**
	 * Fire the aa units in the given territory, hits are removed from units
	 */
	private void fireAA(final Territory territory, final Collection<Unit> units, final UndoableMove currentMove)
	{
		if (units.isEmpty())
			return;
		final PlayerID movingPlayer = movingPlayer(units);
		final HashMap<String, HashSet<UnitType>> airborneTechTargetsAllowed = TechAbilityAttachment.getAirborneTargettedByAA(movingPlayer, getData());
		final List<Unit> defendingAA = territory.getUnits().getMatches(Matches.UnitIsAAthatCanFire(units, airborneTechTargetsAllowed, movingPlayer, Matches.UnitIsAAforFlyOverOnly, getData()));
		final Set<String> AAtypes = UnitAttachment.getAllOfTypeAAs(defendingAA); // TODO: order this list in some way
		for (final String currentTypeAA : AAtypes)
		{
			final Collection<Unit> currentPossibleAA = Match.getMatches(defendingAA, Matches.UnitIsAAofTypeAA(currentTypeAA));
			final Set<UnitType> targetUnitTypesForThisTypeAA = UnitAttachment.get(currentPossibleAA.iterator().next().getType()).getTargetsAA(getData());
			final Set<UnitType> airborneTypesTargettedToo = airborneTechTargetsAllowed.get(currentTypeAA);
			final Match<Unit> aaTargetsMatch = new CompositeMatchOr<Unit>(Matches.unitIsOfTypes(targetUnitTypesForThisTypeAA),
						new CompositeMatchAnd<Unit>(Matches.UnitIsAirborne, Matches.unitIsOfTypes(airborneTypesTargettedToo)));
			
			// once we fire the AA guns, we can't undo
			// otherwise you could keep undoing and redoing
			// until you got the roll you wanted
			currentMove.setCantUndo("Move cannot be undone after AA has fired.");
			final DiceRoll[] dice = new DiceRoll[1];
			final IExecutable rollDice = new IExecutable()
			{
				private static final long serialVersionUID = 4714364489659654758L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					dice[0] = DiceRoll.rollAA(units, currentPossibleAA, aaTargetsMatch, m_bridge, territory);
				}
			};
			final IExecutable selectCasualties = new IExecutable()
			{
				private static final long serialVersionUID = -8633263235214834617L;
				
				public void execute(final ExecutionStack stack, final IDelegateBridge bridge)
				{
					final int hitCount = dice[0].getHits();
					if (hitCount == 0)
					{
						getRemotePlayer().reportMessage("No aa hits in " + territory.getName(), "No aa hits in " + territory.getName());
					}
					else
						selectCasualties(dice[0], units, currentPossibleAA, aaTargetsMatch, territory, null);
				}
			};
			// push in reverse order of execution
			m_executionStack.push(selectCasualties);
			m_executionStack.push(rollDice);
		}
	}
	
	/**
	 * hits are removed from units. Note that units are removed in the order
	 * that the iterator will move through them.
	 */
	private void selectCasualties(final DiceRoll dice, final Collection<Unit> units, final Collection<Unit> defendingAA, final Match<Unit> targetUnitTypesForThisTypeAAMatch,
				final Territory territory,
				final GUID battleID)
	{
		Collection<Unit> casualties = null;
		final Collection<Unit> validAttackingUnitsForThisRoll = Match.getMatches(units, targetUnitTypesForThisTypeAAMatch);
		casualties = BattleCalculator.getAACasualties(validAttackingUnitsForThisRoll, defendingAA, dice, m_bridge, territory.getOwner(), m_player, battleID, territory);
		getRemotePlayer().reportMessage(casualties.size() + " AA hits in " + territory.getName(), casualties.size() + " AA hits in " + territory.getName());
		m_bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " lost in " + territory.getName(), casualties);
		units.removeAll(casualties);
		if (m_casualties == null)
			m_casualties = casualties;
		else
			m_casualties.addAll(casualties);
	}
}
