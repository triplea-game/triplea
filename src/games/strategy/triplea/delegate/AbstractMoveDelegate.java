package games.strategy.triplea.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.player.ITripleaPlayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * An abstraction of MoveDelegate in order to allow other delegates to extend this.
 * 
 * @author veqryn
 * 
 */
public abstract class AbstractMoveDelegate extends BaseDelegate implements IMoveDelegate
{
	protected boolean m_nonCombat;
	protected List<UndoableMove> m_movesToUndo = new ArrayList<UndoableMove>();// A collection of UndoableMoves
	protected final TransportTracker m_transportTracker = new TransportTracker();
	protected MovePerformer m_tempMovePerformer;// if we are in the process of doing a move. this instance will allow us to resume the move
	
	
	public static enum MoveType
	{
		DEFAULT, SPECIAL
	}
	
	public AbstractMoveDelegate()
	{
	}
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(final IDelegateBridge aBridge)
	{
		super.start(aBridge);
		m_nonCombat = isNonCombat(aBridge);
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
		m_movesToUndo.clear();
	}
	
	@Override
	public Serializable saveState()
	{
		final AbstractMoveExtendedDelegateState state = new AbstractMoveExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_nonCombat = m_nonCombat;
		state.m_movesToUndo = m_movesToUndo;
		state.m_tempMovePerformer = m_tempMovePerformer;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final AbstractMoveExtendedDelegateState s = (AbstractMoveExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_nonCombat = s.m_nonCombat;
		// if the undo state wasnt saved, then dont load it. prevents overwriting undo state when we restore from an undo move
		if (s.m_movesToUndo != null)
			m_movesToUndo = s.m_movesToUndo;
		m_tempMovePerformer = s.m_tempMovePerformer;
	}
	
	public TransportTracker getTransportTracker()
	{
		return m_transportTracker;
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
	
	protected void updateUndoableMoves(final UndoableMove currentMove)
	{
		currentMove.initializeDependencies(m_movesToUndo);
		m_movesToUndo.add(currentMove);
		updateUndoableMoveIndexes();
	}
	
	protected PlayerID getUnitsOwner(final Collection<Unit> units)
	{
		// if we are not in edit mode, return m_player. if we are in edit mode, we use whoever's units these are.
		if (units.isEmpty() || !EditDelegate.getEditMode(getData()))
			return m_player;
		else
			return units.iterator().next().getOwner();
	}
	
	public String move(final Collection<Unit> units, final Route route)
	{
		return move(units, route, Collections.<Unit> emptyList());
	}
	
	public String move(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded)
	{
		return move(units, route, transportsThatCanBeLoaded, new HashMap<Unit, Collection<Unit>>());
	}
	
	public abstract String move(final Collection<Unit> units, final Route route, final Collection<Unit> m_transportsThatCanBeLoaded, final Map<Unit, Collection<Unit>> newDependents);
	
	public static MoveValidationResult validateMove(final MoveType moveType, final Collection<Unit> units, final Route route, final PlayerID player, final Collection<Unit> transportsToLoad,
				final Map<Unit, Collection<Unit>> newDependents, final boolean isNonCombat, final List<UndoableMove> undoableMoves, final GameData data)
	{
		if (moveType == MoveType.SPECIAL)
			return SpecialMoveDelegate.validateMove(units, route, player, transportsToLoad, newDependents, isNonCombat, undoableMoves, data);
		return MoveValidator.validateMove(units, route, player, transportsToLoad, newDependents, isNonCombat, undoableMoves, data);
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
	
	public static boolean isNonCombat(final IDelegateBridge aBridge)
	{
		if (aBridge.getStepName().endsWith("NonCombatMove"))
			return true;
		else if (aBridge.getStepName().endsWith("CombatMove"))
			return false;
		else
			throw new IllegalStateException("Cannot determine combat or not");
	}
	
	public static BattleTracker getBattleTracker(final GameData data)
	{
		return DelegateFinder.battleDelegate(data).getBattleTracker();
	}
	
	protected boolean isWW2V2()
	{
		return games.strategy.triplea.Properties.getWW2V2(getData());
	}
	
	protected boolean isWW2V3()
	{
		return games.strategy.triplea.Properties.getWW2V3(getData());
	}
	
	protected ITripleaPlayer getRemotePlayer()
	{
		return getRemotePlayer(m_player);
	}
	
	protected ITripleaPlayer getRemotePlayer(final PlayerID id)
	{
		return (ITripleaPlayer) m_bridge.getRemote(id);
	}
	
	public abstract int PUsAlreadyLost(final Territory t);
	
	public abstract void PUsLost(final Territory t, final int amt);
	
	/*
	 * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
	 */
	@Override
	public Class<IMoveDelegate> getRemoteType()
	{
		return IMoveDelegate.class;
	}
}


class AbstractMoveExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -4072966724295569322L;
	Serializable superState;
	// add other variables here:
	public boolean m_nonCombat;
	public List<UndoableMove> m_movesToUndo;
	public MovePerformer m_tempMovePerformer;
}
