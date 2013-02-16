package games.strategy.grid.chess.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.grid.delegate.AbstractPlayByEmailOrForumDelegate;
import games.strategy.grid.ui.display.IGridGameDisplay;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * 
 * @author veqryn
 * 
 */
public class EndTurnDelegate extends AbstractPlayByEmailOrForumDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		final PlayerID winner = checkForWinner();
		if (winner != null)
		{
			signalGameOver(winner.getName() + " wins!");
		}
		else if (isDraw(m_player, getData(), 1))
		{
			signalGameOver("Game Is A Draw!");
		}
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final EndTurnExtendedDelegateState state = new EndTurnExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final EndTurnExtendedDelegateState s = (EndTurnExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
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
	 * Check to see if anyone has won the game.
	 * 
	 * @return the player who has won, or <code>null</code> if there is no winner yet
	 */
	private PlayerID checkForWinner()
	{
		if (doWeWin(m_player, getData(), 1))
			return m_player;
		return null;
	}
	
	public static boolean doWeWin(final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		if (player == null)
			throw new IllegalArgumentException("Checking for winner can not have null player");
		final Collection<PlayerID> enemies = new ArrayList<PlayerID>(data.getPlayerList().getPlayers());
		enemies.remove(player);
		final Iterator<PlayerID> iter = enemies.iterator();
		while (iter.hasNext())
		{
			final PlayerID e = iter.next();
			if (PlayDelegate.getKingTerritories(e, data).isEmpty() ||
						(PlayDelegate.areWeInCheck(e, data, testForCheckTurnsAhead) && !PlayDelegate.canWeMakeAValidMoveThatIsNotPuttingUsInCheck(e, data, testForCheckTurnsAhead)))
				iter.remove();
		}
		if (enemies.isEmpty())
			return true;
		return false;
	}
	
	private boolean isDraw(final PlayerID player, final GameData data, final int testForCheckTurnsAhead)
	{
		// assume it is not checkmate, since we already checked for that
		final Collection<PlayerID> enemies = new ArrayList<PlayerID>(data.getPlayerList().getPlayers());
		enemies.remove(player);
		boolean haveMovesAvailable = false;
		for (final PlayerID enemy : enemies)
		{
			if (!PlayDelegate.areWeInCheck(enemy, data, testForCheckTurnsAhead))
			{
				for (final Territory t1 : data.getMap().getTerritories())
				{
					for (final Territory t2 : data.getMap().getTerritories())
					{
						if (PlayDelegate.isValidPlay(t1, t2, enemy, data, 2) == null)
						{
							haveMovesAvailable = true;
							break;
						}
					}
					if (haveMovesAvailable)
						break;
				}
			}
			else
			{
				haveMovesAvailable = true;
			}
			if (haveMovesAvailable)
				break;
		}
		if (!haveMovesAvailable)
			return true;
		return false;
	}
}


class EndTurnExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -4466893582763431210L;
	Serializable superState;
	// add other variables here:
}
