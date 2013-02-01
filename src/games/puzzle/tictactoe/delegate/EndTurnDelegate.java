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
package games.puzzle.tictactoe.delegate;

import games.puzzle.tictactoe.ui.display.ITicTacToeDisplay;
import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;

/**
 * Responsible for checking for a winner in a game of Tic Tac Toe.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
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
			if (winner.equals(PlayerID.NULL_PLAYERID))
				signalGameOver("Cat's game!");
			else
				signalGameOver(winner.getName() + " wins!");
		}
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final TicTacToeEndTurnExtendedDelegateState state = new TicTacToeEndTurnExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final TicTacToeEndTurnExtendedDelegateState s = (TicTacToeEndTurnExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
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
		final ITicTacToeDisplay display = (ITicTacToeDisplay) m_bridge.getDisplayChannelBroadcaster();
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
		final GameMap map = getData().getMap();
		final int boardWidth = map.getXDimension();
		final int boardHeight = map.getYDimension();
		final Territory[][] grid = new Territory[boardWidth][boardHeight];
		for (int y = 0; y < boardHeight; y++)
			for (int x = 0; x < boardWidth; x++)
				grid[x][y] = map.getTerritoryFromCoordinates(x, y);
		PlayerID player;
		// Check for horizontal win
		for (int y = 0; y < boardHeight; y++)
		{
			if (grid[0][y] != null)
			{
				player = grid[0][y].getOwner();
				if (player != null && !player.equals(PlayerID.NULL_PLAYERID))
				{
					for (int x = 0; x < boardWidth; x++)
					{
						if (grid[x][y] == null || player != grid[x][y].getOwner())
						{
							player = null;
							break;
						}
					}
					if (player != null)
					{
						return player;
						// signalGameOver(player.getName() + " wins horizontally!");
					}
				}
			}
		}
		// Check for vertical win
		for (int x = 0; x < boardWidth; x++)
		{
			if (grid[x][0] != null)
			{
				player = grid[x][0].getOwner();
				if (player != null && !player.equals(PlayerID.NULL_PLAYERID))
				{
					for (int y = 0; y < boardHeight; y++)
					{
						if (grid[x][y] == null || player != grid[x][y].getOwner())
						{
							player = null;
							break;
						}
					}
					if (player != null)
					{
						return player;
						// signalGameOver(player.getName() + " wins vertically!");
					}
				}
			}
		}
		// Check for diagonal win
		player = grid[0][0].getOwner();
		if (player != null && !player.equals(PlayerID.NULL_PLAYERID))
		{
			for (int x = 0; x < boardWidth && x < boardHeight; x++)
			{
				if (player != grid[x][x].getOwner())
				{
					player = null;
					break;
				}
			}
			if (player != null)
			{
				return player;
				// signalGameOver(player.getName() + " wins diagonally!");
			}
		}
		// Check for diagonal win
		player = grid[0][(boardWidth - 1)].getOwner();
		if (player != null && !player.equals(PlayerID.NULL_PLAYERID))
		{
			for (int x = boardWidth - 1; x >= 0 && x < boardHeight; x--)
			{
				if (player != grid[x][(boardWidth - 1) - x].getOwner())
				{
					player = null;
					break;
				}
			}
			if (player != null)
			{
				return player;
				// signalGameOver(player.getName() + " wins diagonally!");
			}
		}
		// Check for empty squares
		for (int x = 0; x < boardWidth; x++)
		{
			for (int y = 0; y < boardHeight; y++)
			{
				player = grid[x][y].getOwner();
				if (player == null || player.equals(PlayerID.NULL_PLAYERID))
				{
					// Game is not over - no one has won
					return null;
				}
			}
		}
		// If no one has won,
		// and there are no empty squares
		// the game is over
		// and it must be cat's game
		// signalGameOver("Cat's game!");
		return PlayerID.NULL_PLAYERID;
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class does not implement the IRemote interface, so return null.
		return null;
	}
}


class TicTacToeEndTurnExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 5491407308288449271L;
	Serializable superState;
	// add other variables here:
}
