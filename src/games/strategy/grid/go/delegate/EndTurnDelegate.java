package games.strategy.grid.go.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.AbstractPlayByEmailOrForumDelegate;
import games.strategy.grid.go.Go;
import games.strategy.grid.go.delegate.remote.IGoEndTurnDelegate;
import games.strategy.grid.ui.GridEndTurnData;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.display.IGridGameDisplay;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 
 * @author veqryn
 * 
 */
public class EndTurnDelegate extends AbstractPlayByEmailOrForumDelegate implements IGoEndTurnDelegate
{
	protected Tuple<PlayerID, IGridEndTurnData> m_groupsThatShouldDie = null;
	protected boolean m_canScore = false;
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		if (haveTwoPassedInARow())
		{
			final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
			display.setStatus("End Game: " + m_player.getName() + " must click all dead groups. Pless 'C' to confirm, or 'R' to reject and continue playing.");
			display.refreshTerritories(getData().getMap().getTerritories());
		}
	}
	
	@Override
	public void end()
	{
		super.end();
		if (m_canScore && haveTwoPassedInARow())
		{
			final Tuple<Tuple<PlayerID, Integer>, Tuple<PlayerID, Integer>> scores = getFinalScores((m_groupsThatShouldDie == null ? new HashSet<Territory>() :
						m_groupsThatShouldDie.getSecond().getTerritoryUnitsRemovalAdjustment()), getData());
			final PlayerID p1 = scores.getFirst().getFirst();
			final int score1 = scores.getFirst().getSecond();
			final PlayerID p2 = scores.getSecond().getFirst();
			final int score2 = scores.getSecond().getSecond();
			// p2 wins tie
			if (score1 > score2)
				signalGameOver(p1.getName() + " wins with " + score1 + " against " + p2.getName() + " with " + score2 + ".5");
			else
				signalGameOver(p2.getName() + " wins with " + score2 + ".5 against " + p1.getName() + " with " + score1);
			final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
			display.showGridEndTurnData(m_groupsThatShouldDie.getSecond());
			// display.refreshTerritories(getData().getMap().getTerritories());
		}
	}
	
	public static Tuple<Tuple<PlayerID, Integer>, Tuple<PlayerID, Integer>> getFinalScores(final Collection<Territory> deadGroups, final GameData data)
	{
		final Map<Territory, PlayerID> currentState = getCurrentAreaScoreState(deadGroups, data);
		final IntegerMap<PlayerID> score = new IntegerMap<PlayerID>();
		for (final Entry<Territory, PlayerID> entry : currentState.entrySet())
		{
			score.add(entry.getValue(), 1);
		}
		// the players had better be in order, or this will not work
		final Collection<PlayerID> players = data.getPlayerList().getPlayers();
		final Iterator<PlayerID> iter = players.iterator();
		final PlayerID p1 = iter.next();
		final PlayerID p2 = iter.next();
		int score1 = score.getInt(p1);
		int score2 = score.getInt(p2) + data.getProperties().get("White Bonus Komi", 0);
		final boolean countCaptures = data.getProperties().get("Captured Pieces Count Towards Score", false);
		if (countCaptures)
		{
			final Set<Unit> capturedAll = getCapturedUnits(data);
			score1 += Match.countMatches(capturedAll, PlayDelegate.UnitIsOwnedBy(p2));
			score2 += Match.countMatches(capturedAll, PlayDelegate.UnitIsOwnedBy(p1));
		}
		return new Tuple<Tuple<PlayerID, Integer>, Tuple<PlayerID, Integer>>(new Tuple<PlayerID, Integer>(p1, score1), new Tuple<PlayerID, Integer>(p2, score2));
	}
	
	@Override
	public Serializable saveState()
	{
		final GoEndTurnExtendedDelegateState state = new GoEndTurnExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_groupsThatShouldDie = this.m_groupsThatShouldDie;
		state.m_canScore = this.m_canScore;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final GoEndTurnExtendedDelegateState s = (GoEndTurnExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		this.m_groupsThatShouldDie = s.m_groupsThatShouldDie;
		this.m_canScore = s.m_canScore;
	}
	
	@Override
	public boolean stuffToDoInThisDelegate()
	{
		// we are either end game, or we have forum poster
		return haveTwoPassedInARow() || super.stuffToDoInThisDelegate();
	}
	
	public static Map<Territory, PlayerID> getCurrentAreaScoreState(final Collection<Territory> deadGroups, final GameData data)
	{
		final boolean onlySurroundedTerritoryNotAllArea = data.getProperties().get("Territory Not Area Counts Towards Score", false);
		final Map<Territory, PlayerID> currentAreaScoreState = new HashMap<Territory, PlayerID>();
		for (final Territory t : data.getMap().getTerritories())
		{
			final Set<PlayerID> towners = new HashSet<PlayerID>();
			getAllNeighboringPlayers(t, towners, new HashSet<Territory>(), deadGroups, data);
			// zero or two owners, we do not count it, because it belongs to no one
			if (towners.size() == 1)
			{
				if (!onlySurroundedTerritoryNotAllArea || t.getUnits().getUnitCount() <= 0)
					currentAreaScoreState.put(t, towners.iterator().next());
			}
		}
		return currentAreaScoreState;
	}
	
	public static Set<PlayerID> getAllNeighboringPlayers(final Territory start, final Set<PlayerID> playersSoFar, final Set<Territory> checkedAlready, final Collection<Territory> deadGroups,
				final GameData data)
	{
		if (playersSoFar.size() >= 2)
			return playersSoFar;
		checkedAlready.add(start);
		final Collection<Unit> units = start.getUnits().getUnits();
		if (!units.isEmpty() && (deadGroups == null || !deadGroups.contains(start)))
		{
			for (final Unit u : units)
			{
				playersSoFar.add(u.getOwner());
			}
		}
		else
		{
			final Set<Territory> neighbors = new HashSet<Territory>(data.getMap().getNeighbors(start));
			neighbors.removeAll(checkedAlready);
			for (final Territory t : neighbors)
			{
				getAllNeighboringPlayers(t, playersSoFar, checkedAlready, deadGroups, data);
			}
		}
		return playersSoFar;
	}
	
	/**
	 * Notify all players that the game is over.
	 * 
	 * @param status
	 *            the "game over" text to be displayed to each user.
	 */
	private void signalGameOver(final String status)
	{
		// If the game is over, we need to be able to alert all UIs to that fact.
		// The display object can send a message to all UIs.
		m_bridge.getHistoryWriter().startEvent(status);
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(status);
		display.setGameOver();
		m_bridge.stopGameSequence();
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IGoEndTurnDelegate.class;
	}
	
	public void signalStatus(final String status)
	{
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(status);
	}
	
	public IGridEndTurnData getTerritoryAdjustment()
	{
		return (m_groupsThatShouldDie == null ? null : m_groupsThatShouldDie.getSecond());
	}
	
	public boolean haveTwoPassedInARow()
	{
		final PlayDelegate localPlayDel = Go.playDelegate(getData());
		if (localPlayDel != null)
			return localPlayDel.haveTwoPassedInARow();
		return false;
	}
	
	static Set<Unit> getCapturedUnits(final GameData data)
	{
		final PlayDelegate localPlayDel = Go.playDelegate(data);
		if (localPlayDel != null)
			return localPlayDel.getCapturedUnits();
		return null;
	}
	
	public String territoryAdjustment(final IGridEndTurnData groupsThatShouldDie)
	{
		// just ignore whatever user/ai input if the game isn't actually done yet
		if (!haveTwoPassedInARow())
			return null;
		if (groupsThatShouldDie == null)
			return "Can Not Have Null For End Turn Data";
		if (!groupsThatShouldDie.getWantToContinuePlaying())
		{
			final Tuple<PlayerID, IGridEndTurnData> oldGroupsFromLastPlayer = m_groupsThatShouldDie;
			if (oldGroupsFromLastPlayer != null)
			{
				if (!oldGroupsFromLastPlayer.getSecond().getTerritoryUnitsRemovalAdjustment().equals(groupsThatShouldDie.getTerritoryUnitsRemovalAdjustment()))
				{
					// the players disagree, so let it go back to the other player
				}
				else
				{
					// the players agree, so let the game be scored
					m_canScore = true;
				}
			}
			m_groupsThatShouldDie = new Tuple<PlayerID, IGridEndTurnData>(m_player, new GridEndTurnData(groupsThatShouldDie));
		}
		else
		{
			m_groupsThatShouldDie = null;
			// reset passes, so that play can continue
			Go.playDelegate(getData()).setPassesInARow(0);
		}
		final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.showGridEndTurnData(groupsThatShouldDie);
		return null;
	}
}


class GoEndTurnExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 7019422380469796157L;
	Serializable superState;
	// add other variables here:
	protected Tuple<PlayerID, IGridEndTurnData> m_groupsThatShouldDie;
	protected boolean m_canScore;
}
