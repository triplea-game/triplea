package games.strategy.grid.checkers.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.grid.delegate.AbstractPlayByEmailOrForumDelegate;
import games.strategy.grid.ui.display.IGridGameDisplay;

import java.io.Serializable;

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
		if (isDraw(getData()))
		{
			signalGameOver("Game Is A Draw!");
		}
		else
		{
			final PlayerID winner = checkForWinner();
			if (winner != null)
			{
				signalGameOver(winner.getName() + " wins!");
			}
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
		if (doWeWin(m_player, getData()))
			return m_player;
		return null;
	}
	
	public static boolean doWeWin(final PlayerID player, final GameData data)
	{
		if (player == null)
			throw new IllegalArgumentException("Checking for winner can not have null player");
		for (final PlayerID enemy : data.getPlayerList().getPlayers())
		{
			if (player.equals(enemy))
				continue;
			if (!PlayDelegate.canNotMakeMoves(enemy, data))
				return false;
		}
		return true;
	}
	
	private static boolean isDraw(final GameData data)
	{
		for (final PlayerID player : data.getPlayerList().getPlayers())
		{
			if (!PlayDelegate.canNotMakeMoves(player, data))
				return false;
		}
		return true;
	}
}


class EndTurnExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -4306772113527908327L;
	Serializable superState;
	// add other variables here:
}
