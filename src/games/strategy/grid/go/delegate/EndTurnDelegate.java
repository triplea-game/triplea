package games.strategy.grid.go.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.GridGame;
import games.strategy.grid.ui.display.IGridGameDisplay;
import games.strategy.util.IntegerMap;

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
public class EndTurnDelegate extends AbstractDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		final GameData data = getData();
		final PlayDelegate localPlayDelegate = GridGame.playDelegate(data);
		if (localPlayDelegate == null)
			return;
		final int passes = localPlayDelegate.getPassesInARow();
		if (passes < 2)
			return;
		final Map<Territory, PlayerID> currentState = getCurrentAreaScoreState(data);
		final IntegerMap<PlayerID> score = new IntegerMap<PlayerID>();
		for (final Entry<Territory, PlayerID> entry : currentState.entrySet())
		{
			score.add(entry.getValue(), 1);
		}
		final Collection<PlayerID> players = data.getPlayerList().getPlayers();
		final Iterator<PlayerID> iter = players.iterator();
		final PlayerID p1 = iter.next();
		final PlayerID p2 = iter.next();
		final int score1 = score.getInt(p1);
		final int score2 = score.getInt(p2);
		// p2 wins tie
		if (score1 > score2)
			signalGameOver(p1.getName() + " wins with " + score1 + " against " + p2.getName() + " with " + score2 + ".5");
		else
			signalGameOver(p2.getName() + " wins with " + score2 + ".5 against " + p1.getName() + " with " + score1);
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		return null;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
	}
	
	public boolean stuffToDoInThisDelegate()
	{
		return false;
	}
	
	public static Map<Territory, PlayerID> getCurrentAreaScoreState(final GameData data)
	{
		final Map<Territory, PlayerID> currentAreaScoreState = new HashMap<Territory, PlayerID>();
		for (final Territory t : data.getMap().getTerritories())
		{
			final Set<PlayerID> towners = new HashSet<PlayerID>();
			getAllNeighboringPlayers(t, towners, new HashSet<Territory>(), data);
			// zero or two owners, we do not count it, because it belongs to no one
			if (towners.size() == 1)
			{
				currentAreaScoreState.put(t, towners.iterator().next());
			}
		}
		return currentAreaScoreState;
	}
	
	public static Set<PlayerID> getAllNeighboringPlayers(final Territory start, final Set<PlayerID> playersSoFar, final Set<Territory> checkedAlready, final GameData data)
	{
		if (playersSoFar.size() >= 2)
			return playersSoFar;
		checkedAlready.add(start);
		final Collection<Unit> units = start.getUnits().getUnits();
		if (!units.isEmpty())
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
				getAllNeighboringPlayers(t, playersSoFar, checkedAlready, data);
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
		return null;
	}
}
