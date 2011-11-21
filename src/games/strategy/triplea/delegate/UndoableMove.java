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

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.ui.MovePanel;
import games.strategy.util.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Contains all the data to describe a move and to undo it.
 * 
 * @author Erik von der Osten
 */
@SuppressWarnings("serial")
public class UndoableMove extends AbstractUndoableMove
{
	private String m_reasonCantUndo;
	private String m_description;
	
	// this move is dependent on these moves
	// these moves cant be undone until this one has been
	private Set<UndoableMove> m_iDependOn = new HashSet<UndoableMove>();
	// these moves depend on me
	// we cant be undone until this is empty
	private Set<UndoableMove> m_dependOnMe = new HashSet<UndoableMove>();
	
	// list of countries we took over
	private Set<Territory> m_conquered = new HashSet<Territory>();
	
	// transports loaded by this move
	private Set<Unit> m_loaded = new HashSet<Unit>();
	
	// transports unloaded by this move
	private Set<Unit> m_unloaded = new HashSet<Unit>();;
	
	private final Route m_route;
	
	public void addToConquered(Territory t)
	{
		m_conquered.add(t);
	}
	
	public Route getRoute()
	{
		return m_route;
	}
	
	public boolean getcanUndo()
	{
		return m_reasonCantUndo == null && m_dependOnMe.isEmpty();
	}
	
	public String getReasonCantUndo()
	{
		if (m_reasonCantUndo != null)
			return m_reasonCantUndo;
		else if (!m_dependOnMe.isEmpty())
			return "Move " + (m_dependOnMe.iterator().next().getIndex() + 1) + " must be undone first";
		else
			throw new IllegalStateException("no reason");
		
	}
	
	public void setCantUndo(String reason)
	{
		m_reasonCantUndo = reason;
	}
	
	public String getDescription()
	{
		return m_description;
	}
	
	public void setDescription(String description)
	{
		m_description = description;
	}
	
	public UndoableMove(GameData data, Collection<Unit> units, Route route)
	{
		super(new CompositeChange(), units);
		m_route = route;
	}
	
	public void load(Unit transport)
	{
		m_loaded.add(transport);
	}
	
	public void unload(Unit transport)
	{
		m_unloaded.add(transport);
	}
	
	@Override
	protected void undoSpecific(IDelegateBridge bridge)
	{
		GameData data = bridge.getData();
		BattleTracker battleTracker = DelegateFinder.battleDelegate(data).getBattleTracker();
		
		battleTracker.undoBattle(m_route, m_units, bridge.getPlayerID(), bridge);
		
		// clean up dependencies
		Iterator<UndoableMove> iter1 = m_iDependOn.iterator();
		while (iter1.hasNext())
		{
			UndoableMove other = iter1.next();
			other.m_dependOnMe.remove(this);
		}
		
		// if we are moving out of a battle zone, mark it
		// this can happen for air units moving out of a battle zone
		IBattle battleLand = battleTracker.getPendingBattle(m_route.getStart(), false);
		IBattle battleAir = battleTracker.getPendingBattle(m_route.getStart(), true);
		if (battleLand != null || battleAir != null)
		{
			Iterator<Unit> iter2 = m_units.iterator();
			while (iter2.hasNext())
			{
				Unit unit = iter2.next();
				Route routeUnitUsedToMove = DelegateFinder.moveDelegate(data).getRouteUsedToMoveInto(unit, m_route.getStart());
				if (battleLand != null && !battleLand.isOver())
				{
					// route units used to move will be null in the case
					// where an enemy sub is submerged in the territory, and another unit
					// moved in to attack it, but some of the units in the original
					// territory are moved out. Undoing this last move, the route used to move
					// into the battle zone will be null
					if (routeUnitUsedToMove != null)
					{
						Change change = battleLand.addAttackChange(routeUnitUsedToMove, Collections.singleton(unit));
						bridge.addChange(change);
					}
				}
				if (battleAir != null && !battleAir.isOver())
				{
					Change change = battleAir.addAttackChange(routeUnitUsedToMove, Collections.singleton(unit));
					bridge.addChange(change);
				}
			}
		}
		
		// Clear any temporary dependents
		MovePanel.clearDependents(m_units);
	}
	
	/**
	 * Update the dependencies.
	 * 
	 * @param undoableMoves
	 *            list of moves that should be undone
	 */
	public void initializeDependencies(List<UndoableMove> undoableMoves)
	{
		Iterator<UndoableMove> iter = undoableMoves.iterator();
		while (iter.hasNext())
		{
			UndoableMove other = iter.next();
			
			if (other == null)
			{
				System.err.println(undoableMoves);
				throw new IllegalStateException("other should not be null");
			}
			
			if ( // if the other move has moves that depend on this
			!Util.intersection(other.getUnits(), this.getUnits()).isEmpty() ||
						// if the other move has transports that we are loading
						!Util.intersection(other.m_units, this.m_loaded).isEmpty() ||
						// or we are moving through a previously conqueured territory
						// we should be able to take this out later
						// we need to add logic for this move to take over the same territories
						// when the other move is undone
						!Util.intersection(other.m_conquered, m_route.getAllTerritories()).isEmpty() ||
						// or we are unloading transports that have moved in another turn
						!Util.intersection(other.m_units, this.m_unloaded).isEmpty() || !Util.intersection(other.m_unloaded, this.m_unloaded).isEmpty())
			{
				m_iDependOn.add(other);
				other.m_dependOnMe.add(this);
			}
		}
	}
	
	public boolean wasTransportUnloaded(Unit transport)
	{
		return m_unloaded.contains(transport);
	}
	
	public boolean wasTransportLoaded(Unit transport)
	{
		return m_loaded.contains(transport);
	}
	
	@Override
	public String toString()
	{
		return "UndoableMove index;" + m_index + " description:" + m_description;
	}
	
	@Override
	public final String getMoveLabel()
	{
		return m_route.getStart() + " -> " + m_route.getEnd();
	}
	
	@Override
	public final Territory getEnd()
	{
		return m_route.getEnd();
	}
	
	@Override
	protected final MoveDescription getDescriptionObject()
	{
		return new MoveDescription(m_units, m_route);
	}
}
