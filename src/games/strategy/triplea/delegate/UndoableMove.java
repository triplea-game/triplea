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
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.MovePanel;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Contains all the data to describe a move and to undo it.
 * 
 * @author Erik von der Osten
 */
public class UndoableMove extends AbstractUndoableMove
{
	private static final long serialVersionUID = 8490182214651531358L;
	private String m_reasonCantUndo;
	private String m_description;
	// this move is dependent on these moves
	// these moves cant be undone until this one has been
	private final Set<UndoableMove> m_iDependOn = new HashSet<UndoableMove>();
	// these moves depend on me
	// we cant be undone until this is empty
	private final Set<UndoableMove> m_dependOnMe = new HashSet<UndoableMove>();
	// list of countries we took over
	private final Set<Territory> m_conquered = new HashSet<Territory>();
	// transports loaded by this move
	private final Set<Unit> m_loaded = new HashSet<Unit>();
	// transports unloaded by this move
	private final Set<Unit> m_unloaded = new HashSet<Unit>();;
	private final Route m_route;
	
	public void addToConquered(final Territory t)
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
	
	public void setCantUndo(final String reason)
	{
		m_reasonCantUndo = reason;
	}
	
	public String getDescription()
	{
		return m_description;
	}
	
	public void setDescription(final String description)
	{
		m_description = description;
	}
	
	public UndoableMove(final GameData data, final Collection<Unit> units, final Route route)
	{
		super(new CompositeChange(), units);
		m_route = route;
	}
	
	public void load(final Unit transport)
	{
		m_loaded.add(transport);
	}
	
	public void unload(final Unit transport)
	{
		m_unloaded.add(transport);
	}
	
	@Override
	protected void undoSpecific(final IDelegateBridge bridge)
	{
		final GameData data = bridge.getData();
		final BattleTracker battleTracker = DelegateFinder.battleDelegate(data).getBattleTracker();
		battleTracker.undoBattle(m_route, m_units, bridge.getPlayerID(), bridge);
		// clean up dependencies
		final Iterator<UndoableMove> iter1 = m_iDependOn.iterator();
		while (iter1.hasNext())
		{
			final UndoableMove other = iter1.next();
			other.m_dependOnMe.remove(this);
		}
		// if we are moving out of a battle zone, mark it
		// this can happen for air units moving out of a battle zone
		final IBattle battleNormal = battleTracker.getPendingBattle(m_route.getStart(), false);
		final IBattle battleBombing = battleTracker.getPendingBattle(m_route.getStart(), true);
		if (battleNormal != null || battleBombing != null)
		{
			final Iterator<Unit> iter2 = m_units.iterator();
			while (iter2.hasNext())
			{
				final Unit unit = iter2.next();
				final Route routeUnitUsedToMove = DelegateFinder.moveDelegate(data).getRouteUsedToMoveInto(unit, m_route.getStart());
				if (battleNormal != null && !battleNormal.isOver())
				{
					// route units used to move will be null in the case
					// where an enemy sub is submerged in the territory, and another unit
					// moved in to attack it, but some of the units in the original
					// territory are moved out. Undoing this last move, the route used to move
					// into the battle zone will be null
					if (routeUnitUsedToMove != null)
					{
						final Change change = battleNormal.addAttackChange(routeUnitUsedToMove, Collections.singleton(unit), null);
						bridge.addChange(change);
					}
				}
				if (battleBombing != null && !battleBombing.isOver())
				{
					HashMap<Unit, HashSet<Unit>> targets = null;
					Unit target = null;
					if (routeUnitUsedToMove != null && routeUnitUsedToMove.getEnd() != null)
					{
						final Territory end = routeUnitUsedToMove.getEnd();
						final Collection<Unit> enemyTargetsTotal = end.getUnits().getMatches(new CompositeMatchAnd<Unit>(
									Matches.enemyUnit(bridge.getPlayerID(), data),
									Matches.UnitIsAtMaxDamageOrNotCanBeDamaged(end).invert(),
									Matches.unitIsBeingTransported().invert()));
						final Collection<Unit> enemyTargets = Match.getMatches(enemyTargetsTotal,
									Matches.unitIsOfTypes(UnitAttachment.getAllowedBombingTargetsIntersection(Match.getMatches(Collections.singleton(unit), Matches.UnitIsStrategicBomber), data)));
						if (enemyTargets.size() > 1 && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)
									&& !games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(data))
						{
							while (target == null)
							{
								target = ((ITripleaPlayer) bridge.getRemote(bridge.getPlayerID())).whatShouldBomberBomb(end, enemyTargets, Collections.singletonList(unit));
							}
						}
						else if (!enemyTargets.isEmpty())
							target = enemyTargets.iterator().next();
						if (target != null)
						{
							targets = new HashMap<Unit, HashSet<Unit>>();
							targets.put(target, new HashSet<Unit>(Collections.singleton(unit)));
						}
					}
					final Change change = battleBombing.addAttackChange(routeUnitUsedToMove, Collections.singleton(unit), targets);
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
	public void initializeDependencies(final List<UndoableMove> undoableMoves)
	{
		for (final UndoableMove other : undoableMoves)
		{
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
	
	// for use with airborne moving
	public void addDependency(final List<UndoableMove> undoableMoves)
	{
		for (final UndoableMove other : undoableMoves)
		{
			addDependency(other);
		}
	}
	
	// for use with airborne moving
	public void addDependency(final UndoableMove undoableMove)
	{
		m_iDependOn.add(undoableMove);
		undoableMove.m_dependOnMe.add(this);
	}
	
	public boolean wasTransportUnloaded(final Unit transport)
	{
		return m_unloaded.contains(transport);
	}
	
	public boolean wasTransportLoaded(final Unit transport)
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
	
	public final Territory getStart()
	{
		return m_route.getStart();
	}
	
	@Override
	protected final MoveDescription getDescriptionObject()
	{
		return new MoveDescription(m_units, m_route);
	}
}
