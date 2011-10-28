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
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * Code to fire AA guns while in combat and non combat move.
 * 
 * @author Sean Bridges
 */
class AAInMoveUtil implements Serializable
{
	private transient boolean m_nonCombat;
	private transient IDelegateBridge m_bridge;
	private transient PlayerID m_player;
	private Collection<Unit> m_casualties;
	
	private ExecutionStack m_executionStack = new ExecutionStack();
	
	AAInMoveUtil()
	{
	}
	
	public AAInMoveUtil initialize(IDelegateBridge bridge)
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
		return games.strategy.triplea.Properties.getAlways_On_AA(getData());
	}
	
	private boolean isAATerritoryRestricted()
	{
		return games.strategy.triplea.Properties.getAATerritoryRestricted(getData());
	}
	
	private ITripleaPlayer getRemotePlayer(PlayerID id)
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
	Collection<Unit> fireAA(Route route, Collection<Unit> units, Comparator<Unit> decreasingMovement, final UndoableMove currentMove)
	{
		if (m_executionStack.isEmpty())
			populateExecutionStack(route, units, decreasingMovement, currentMove);
		
		m_executionStack.execute(m_bridge);
		return m_casualties;
		
	}
	
	private void populateExecutionStack(Route route, Collection<Unit> units, Comparator<Unit> decreasingMovement, final UndoableMove currentMove)
	{
		final List<Unit> targets = Match.getMatches(units, Matches.UnitIsAir);
		
		// select units with lowest movement first
		Collections.sort(targets, decreasingMovement);
		
		List<IExecutable> executables = new ArrayList<IExecutable>();
		
		Iterator<Territory> iter = getTerritoriesWhereAAWillFire(route, units).iterator();
		
		while (iter.hasNext())
		{
			final Territory location = iter.next();
			
			executables.add(new IExecutable()
			{
				
				public void execute(ExecutionStack stack, IDelegateBridge bridge)
				{
					fireAA(location, targets, currentMove);
				}
				
			});
		}
		
		Collections.reverse(executables);
		m_executionStack.push(executables);
	}
	
	Collection<Territory> getTerritoriesWhereAAWillFire(Route route, Collection<Unit> units)
	{
		boolean alwaysOnAA = isAlwaysONAAEnabled();
		
		// Just the attacked territory will have AA firing
		if (!alwaysOnAA && isAATerritoryRestricted())
			return Collections.emptyList();
		
		// No AA in nonCombat unless 'Always on AA'
		if (m_nonCombat && !alwaysOnAA)
			return Collections.emptyList();
		
		// No air, return empty list
		if (Match.noneMatch(units, Matches.UnitIsAir))
			return Collections.emptyList();
		
		// can't rely on m_player being the unit owner in Edit Mode
		// look at the units being moved to determine allies and enemies
		PlayerID ally = units.iterator().next().getOwner();
		
		// don't iterate over the end
		// that will be a battle
		// and handled else where in this tangled mess
		CompositeMatch<Unit> hasAA = new CompositeMatchAnd<Unit>();
		hasAA.add(Matches.UnitIsAAforCombat);
		hasAA.add(Matches.enemyUnit(ally, getData()));
		
		List<Territory> territoriesWhereAAWillFire = new ArrayList<Territory>();
		
		for (int i = 0; i < route.getLength() - 1; i++)
		{
			Territory current = route.at(i);
			
			// AA guns in transports shouldn't be able to fire
			// TODO COMCO- Chance to add rule to support air suppression naval units here
			if (current.getUnits().someMatch(hasAA) && !current.isWater())
			{
				territoriesWhereAAWillFire.add(current);
			}
		}
		
		// check start as well, prevent user from moving to and from AA sites
		// one at a time
		// if there was a battle fought there then don't fire
		// this covers the case where we fight, and always on AA wants to fire
		// after the battle.
		// TODO
		// there is a bug in which if you move an air unit to a battle site
		// in the middle of non combat, it wont fire
		if (route.getStart().getUnits().someMatch(hasAA) && !route.getStart().isWater()
					&& !getBattleTracker().wasBattleFought(route.getStart()))
			territoriesWhereAAWillFire.add(route.getStart());
		
		return territoriesWhereAAWillFire;
	}
	
	private BattleTracker getBattleTracker()
	{
		return DelegateFinder.battleDelegate(getData()).getBattleTracker();
	}
	
	/**
	 * Fire the aa units in the given territory, hits are removed from units
	 */
	private void fireAA(final Territory territory, final Collection<Unit> units, final UndoableMove currentMove)
	{
		
		if (units.isEmpty())
			return;
		
		// once we fire the AA guns, we can't undo
		// otherwise you could keep undoing and redoing
		// until you got the roll you wanted
		currentMove.setCantUndo("Move cannot be undone after AA has fired.");
		
		final DiceRoll[] dice = new DiceRoll[1];
		
		IExecutable rollDice = new IExecutable()
		{
			
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				dice[0] = DiceRoll.rollAA(units, m_bridge, territory, Matches.UnitIsAAforCombat);
			}
		};
		
		IExecutable selectCasualties = new IExecutable()
		{
			
			public void execute(ExecutionStack stack, IDelegateBridge bridge)
			{
				int hitCount = dice[0].getHits();
				
				if (hitCount == 0)
				{
					getRemotePlayer().reportMessage("No aa hits in " + territory.getName());
				}
				else
					selectCasualties(dice[0], units, territory, null);
			}
			
		};
		
		// push in reverse order of execution
		m_executionStack.push(selectCasualties);
		m_executionStack.push(rollDice);
		
	}
	
	/**
	 * hits are removed from units. Note that units are removed in the order
	 * that the iterator will move through them.
	 */
	private void selectCasualties(DiceRoll dice, Collection<Unit> units, Territory territory, GUID battleID)
	{
		Collection<Unit> casualties = null;
		
		casualties = BattleCalculator.getAACasualties(units, dice, m_bridge, territory.getOwner(), m_player, battleID, territory, Matches.UnitIsAAforCombat);
		
		getRemotePlayer().reportMessage(casualties.size() + " AA hits in " + territory.getName());
		
		m_bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(casualties) + " lost in " + territory.getName(), casualties);
		units.removeAll(casualties);
		
		if (m_casualties == null)
			m_casualties = casualties;
		else
			m_casualties.addAll(casualties);
	}
}
