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
package games.strategy.kingstable.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.message.IRemote;
import games.strategy.kingstable.attachments.PlayerAttachment;
import games.strategy.kingstable.attachments.TerritoryAttachment;
import games.strategy.kingstable.ui.display.IKingsTableDisplay;

import java.io.Serializable;

/**
 * Responsible for checking for a winner in a game of King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class EndTurnDelegate extends BaseDelegate// implements IEndTurnDelegate
{
	// private boolean gameOver = false;
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
			// CountDownLatch waitToLeaveGame = new CountDownLatch(1);
			signalGameOver(winner.getName() + " wins!");// , waitToLeaveGame);
			// gameOver = true;
			/*
			try {
			    
			    wait();
			    //waitToLeaveGame.await();
			} catch (InterruptedException e) {}
			*/
			// while(true){}
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
		final KingsTableEndTurnExtendedDelegateState state = new KingsTableEndTurnExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final KingsTableEndTurnExtendedDelegateState s = (KingsTableEndTurnExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	public boolean stuffToDoInThisDelegate()
	{
		return false;
	}
	
	/*
	public boolean isGameOver()
	{
	    return gameOver;
	}
	*/
	/**
	 * Notify all players that the game is over.
	 * 
	 * @param status
	 *            the "game over" text to be displayed to each user.
	 */
	private void signalGameOver(final String status)// , CountDownLatch waiting)
	{
		// If the game is over, we need to be able to alert all UIs to that fact.
		// The display object can send a message to all UIs.
		final IKingsTableDisplay display = (IKingsTableDisplay) m_bridge.getDisplayChannelBroadcaster();
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
		boolean defenderHasKing = false;
		PlayerID attacker = null;
		PlayerID defender = null;
		final GameData data = getData();
		for (final PlayerID player : data.getPlayerList().getPlayers())
		{
			final PlayerAttachment pa = (PlayerAttachment) player.getAttachment("playerAttachment");
			if (pa == null)
				attacker = player;
			else if (pa.getNeedsKing())
				defender = player;
			else
				attacker = player;
		}
		if (attacker == null)
			throw new RuntimeException("Invalid game setup - no attacker is specified. Reconfigure the game xml file so that one player has a playerAttachment with needsKing set to false.");
		if (defender == null)
			throw new RuntimeException("Invalid game setup - no defender is specified. Reconfigure the game xml file so that one player has a playerAttachment with needsKing set to true.");
		int numAttackerPieces = 0;
		int numDefenderPieces = 0;
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.getUnits().isEmpty())
				continue;
			final Unit unit = (Unit) t.getUnits().getUnits().toArray()[0];
			if (unit.getType().getName().equals("king"))
				defenderHasKing = true;
			if (unit.getOwner().equals(defender))
				numDefenderPieces++;
			else if (unit.getOwner().equals(attacker))
				numAttackerPieces++;
			final TerritoryAttachment ta = (TerritoryAttachment) t.getAttachment("territoryAttachment");
			// System.out.println(ta.getName());
			if (ta != null && ta.getKingsExit() && !t.getUnits().isEmpty() && unit.getOwner().equals(defender))
				return defender;
		}
		if (!defenderHasKing || numDefenderPieces == 0)
			return attacker;
		if (numAttackerPieces == 0)
			return defender;
		return null;
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class does not implement the IRemote interface, so return null.
		// return IEndTurnDelegate.class;
		return null;
	}
}


class KingsTableEndTurnExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 1054956757425820238L;
	Serializable superState;
	// add other variables here:
}
