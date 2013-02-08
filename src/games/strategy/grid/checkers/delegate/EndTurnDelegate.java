package games.strategy.grid.checkers.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.ui.display.IGridGameDisplay;

import java.io.Serializable;

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
		final PlayerID winner = checkForWinner();
		if (winner != null)
		{
			signalGameOver(winner.getName() + " wins!");
		}
		else if (isDraw(m_player, getData()))
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
		return false;
	}
	
	private boolean isDraw(final PlayerID player, final GameData data)
	{
		return false;
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
