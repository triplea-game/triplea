package games.strategy.grid.go.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.AutoSave;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.grid.ui.display.IGridGameDisplay;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.Match;
import games.strategy.util.Triple;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author veqryn
 * 
 */
@AutoSave(beforeStepStart = false, afterStepEnd = true)
public class PlayDelegate extends AbstractDelegate implements IGridPlayDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(m_player.getName() + "'s turn");
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final GoPlayExtendedDelegateState state = new GoPlayExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final GoPlayExtendedDelegateState s = (GoPlayExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	public boolean stuffToDoInThisDelegate()
	{
		return true;
	}
	
	public void signalStatus(final String status)
	{
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(status);
	}
	
	public String play(final IGridPlayData play)
	{
		for (final Territory t : play.getAllSteps())
		{
			if (t.getUnits().getUnitCount() > 1)
				throw new IllegalStateException("Can not have more than 1 unit in any territory");
		}
		final String error = isValidPlay(play, m_player, getData());
		if (error != null)
			return error;
		final Collection<Territory> captured = checkForCaptures(play, m_player, getData());
		performPlay(play, captured, m_player);
		for (final Territory t : play.getAllSteps())
		{
			if (t.getUnits().getUnitCount() > 1)
				throw new IllegalStateException("Can not have more than 1 unit in any territory");
		}
		return null;
	}
	
	/**
	 * Check to see if moving a piece from the start <code>Territory</code> to the end <code>Territory</code> is a valid play.
	 * 
	 * @param start
	 *            <code>Territory</code> where the move should start
	 * @param end
	 *            <code>Territory</code> where the move should end
	 */
	public static String isValidPlay(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		// System.out.println("Start: " + start.getX() + "," + start.getY() + "    End: " + end.getX() + "," + end.getY());
		final String basic = isValidMoveBasic(play, player, data);
		if (basic != null)
			return basic;
		
		final String pieceBasic = isValidPieceMoveBasic(play, player, data);
		if (pieceBasic != null)
			return pieceBasic;
		
		return null;
	}
	
	/**
	 * Triple with first as valid non-capturing moves, second is capture moves, third is invalid moves.
	 */
	public static Triple<List<Territory>, List<Tuple<Territory, Collection<Territory>>>, List<Territory>> getAllValidMovesCaptureMovesAndInvalidMoves(final PlayerID player, final GameData data)
	{
		final List<Territory> validMovesWithoutCapture = new ArrayList<Territory>();
		final List<Tuple<Territory, Collection<Territory>>> validCaptureMoves = new ArrayList<Tuple<Territory, Collection<Territory>>>();
		final List<Territory> invalidMoves = new ArrayList<Territory>();
		for (final Territory t : data.getMap().getTerritories())
		{
			final GridPlayData play = new GridPlayData(t, player);
			if (isValidPlay(play, player, data) == null)
			{
				final Collection<Territory> captures = checkForCaptures(play, player, data);
				if (captures.isEmpty())
					validMovesWithoutCapture.add(t);
				else
					validCaptureMoves.add(new Tuple<Territory, Collection<Territory>>(t, captures));
			}
			else
			{
				invalidMoves.add(t);
			}
		}
		return new Triple<List<Territory>, List<Tuple<Territory, Collection<Territory>>>, List<Territory>>(validMovesWithoutCapture, validCaptureMoves, invalidMoves);
	}
	
	/**
	 * After a move completes, look to see if any captures occur.
	 * 
	 * @param end
	 *            <code>Territory</code> where the move ended. All potential captures must involve this <code>Territory</code>.
	 * @return
	 */
	public static Collection<Territory> checkForCaptures(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		// assume it is a legal move
		final Collection<Territory> captured = new HashSet<Territory>();
		final Territory start = play.getStart();
		// final Set<Territory> chain = getOwnedStoneChainsConnectedToThisTerritory(start, player, data, true);
		final List<Set<Territory>> enemyChains = getEnemyStoneChainsConnectedToThisTerritory(start, player, data);
		for (final Set<Territory> echain : enemyChains)
		{
			final PlayerID enemy = echain.iterator().next().getUnits().iterator().next().getOwner();
			if (areAllLibertiesTakenByEnemy(play, echain, enemy, data))
				captured.addAll(echain);
		}
		return captured;
	}
	
	/*
	private IGridGamePlayer getRemotePlayer(final PlayerID id)
	{
		return (IGridGamePlayer) m_bridge.getRemote(id);
	}
	*/

	/**
	 * Move a piece from the start <code>Territory</code> to the end <code>Territory</code>.
	 * 
	 * @param start
	 *            <code>Territory</code> where the move should start
	 * @param end
	 *            <code>Territory</code> where the move should end
	 */
	private void performPlay(final IGridPlayData play, final Collection<Territory> captured, final PlayerID player)
	{
		final Collection<Unit> units = new ArrayList<Unit>();
		units.add(getData().getUnitTypeList().getUnitType("stone").create(player));
		m_bridge.getHistoryWriter().startEvent(play.toString(), units);
		final CompositeChange change = new CompositeChange();
		final Change addUnit = ChangeFactory.addUnits(play.getStart(), units);
		change.add(addUnit);
		final Set<Unit> capturedUnitsTotal = new HashSet<Unit>();
		for (final Territory at : captured)
		{
			if (at != null)
			{
				final Collection<Unit> capturedUnits = at.getUnits().getUnits();
				if (!capturedUnits.isEmpty())
				{
					final Change capture = ChangeFactory.removeUnits(at, capturedUnits);
					change.add(capture);
					capturedUnitsTotal.addAll(capturedUnits);
				}
			}
		}
		if (!capturedUnitsTotal.isEmpty())
			m_bridge.getHistoryWriter().addChildToEvent(player.getName() + " captures units: " + MyFormatter.unitsToText(capturedUnitsTotal), capturedUnitsTotal);
		final Collection<Territory> refresh = new HashSet<Territory>(play.getAllSteps());
		refresh.addAll(captured);
		m_bridge.addChange(change);
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.refreshTerritories(refresh);
		display.showGridPlayDataMove(play);
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class implements IPlayDelegate, which inherits from IRemote.
		return IGridPlayDelegate.class;
	}
	
	public static Match<Territory> TerritoryHasNoUnits = new Match<Territory>()
	{
		@Override
		public boolean match(final Territory t)
		{
			return t.getUnits().getUnitCount() <= 0;
		}
	};
	
	public static Match<Territory> TerritoryHasUnitsOwnedBy(final PlayerID player)
	{
		final Match<Unit> unitOwnedBy = UnitIsOwnedBy(player);
		return new Match<Territory>()
		{
			@Override
			public boolean match(final Territory t)
			{
				return t.getUnits().someMatch(unitOwnedBy);
			}
		};
	}
	
	public static Match<Unit> UnitIsOwnedBy(final PlayerID player)
	{
		return new Match<Unit>()
		{
			@Override
			public boolean match(final Unit unit)
			{
				return unit.getOwner().equals(player);
			}
		};
	}
	
	public static String isValidMoveBasic(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		if (play.getStart() == null)
			return "Can Not Move Off Board";
		if (play.getStart().getUnits().getUnitCount() > 0)
			return "A Piece Is In That Position";
		
		return null;
	}
	
	public static String isValidPieceMoveBasic(final IGridPlayData play, final PlayerID player, final GameData data)
	{
		final Territory start = play.getStart();
		// does our new piece form a chain with anyone else?
		final Set<Territory> chain = getOwnedStoneChainsConnectedToThisTerritory(start, new HashSet<Territory>(), player, data, true);
		if (areAllLibertiesTakenByEnemy(play, chain, player, data))
		{
			final Collection<Territory> captures = checkForCaptures(play, player, data);
			if (captures.isEmpty())
				return "May Not Suicide Move";
		}
		return null;
	}
	
	public static boolean areAllLibertiesTakenByEnemy(final IGridPlayData play, final Collection<Territory> chain, final PlayerID player, final GameData data)
	{
		final Set<Territory> liberties = getAllNeighborsOfTerritoryChain(chain, data);
		for (final Territory t : liberties)
		{
			if (t.equals(play.getStart()))
			{
				if (player.equals(play.getPlayerID()) && !t.getUnits().someMatch(PlayDelegate.UnitIsOwnedBy(player).invert()))
					return false;
			}
			else
			{
				if (!t.getUnits().someMatch(PlayDelegate.UnitIsOwnedBy(player).invert()))
					return false;
			}
		}
		return true;
	}
	
	public static Set<Territory> getAllNeighborsOfTerritoryChain(final Collection<Territory> chain, final GameData data)
	{
		final Set<Territory> neighbors = new HashSet<Territory>();
		final GameMap map = data.getMap();
		for (final Territory t : chain)
		{
			neighbors.addAll(map.getNeighbors(t));
		}
		neighbors.removeAll(chain);
		return neighbors;
	}
	
	/**
	 * Does not include the start territory in the returned set.
	 */
	public static List<Set<Territory>> getEnemyStoneChainsConnectedToThisTerritory(final Territory start, final PlayerID player,
				final GameData data)
	{
		final Collection<PlayerID> enemies = data.getPlayerList().getPlayers();
		enemies.remove(player);
		final PlayerID enemy = enemies.iterator().next();
		final List<Territory> neighbors = Match.getMatches(data.getMap().getNeighbors(start), PlayDelegate.TerritoryHasUnitsOwnedBy(enemy));
		final List<Set<Territory>> enemyGroups = new ArrayList<Set<Territory>>();
		for (final Territory t : neighbors)
		{
			enemyGroups.add(getOwnedStoneChainsConnectedToThisTerritory(t, new HashSet<Territory>(), enemy, data, true));
		}
		return enemyGroups;
	}
	
	/**
	 * Does not include the start territory in the returned set by default.
	 */
	public static Set<Territory> getOwnedStoneChainsConnectedToThisTerritory(final Territory start, final Set<Territory> chainsSoFar, final PlayerID player, final GameData data,
				final boolean includeStart)
	{
		// we do not care who has a piece in the start territory, we are only checking for groups owned by the player argument
		final List<Territory> neighbors = Match.getMatches(data.getMap().getNeighbors(start), PlayDelegate.TerritoryHasUnitsOwnedBy(player));
		neighbors.removeAll(chainsSoFar);
		chainsSoFar.addAll(neighbors);
		for (final Territory t : neighbors)
		{
			chainsSoFar.addAll(getOwnedStoneChainsConnectedToThisTerritory(t, chainsSoFar, player, data, false));
		}
		if (includeStart)
			chainsSoFar.add(start);
		return chainsSoFar;
	}
}


class GoPlayExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 5105340295270130144L;
	Serializable superState;
	// add other variables here:
}
