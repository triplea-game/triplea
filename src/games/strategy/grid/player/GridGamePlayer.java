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
package games.strategy.grid.player;

import games.strategy.common.player.AbstractHumanPlayer;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridPlayData;

import java.util.Collection;

/**
 * Represents a human player of Grid Games.
 * 
 * @author Lane Schwartz (original) and Veqryn (abstraction)
 * @version $LastChangedDate: 2012-07-25 15:19:19 +0800 (Wed, 25 Jul 2012) $
 */
public class GridGamePlayer extends AbstractHumanPlayer<GridGameFrame> implements IGridGamePlayer
{
	public GridGamePlayer(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	public void start(final String stepName)
	{
		// if (m_ui!=null && ((KingsTableFrame)m_ui).isGameOver())
		if (m_ui != null && m_ui.isGameOver())
			return;
		/*{
		    CountDownLatch waitToLeaveGame = new CountDownLatch(1);
		    try {
		        
		        //wait();
		        waitToLeaveGame.await();
		    } catch (InterruptedException e) {}
		}*/
		if (stepName.endsWith("Play"))
			play();
		else
			throw new IllegalArgumentException("Unrecognized step stepName:" + stepName);
	}
	
	/*
	private boolean gameOver()
	{
	    IEndTurnDelegate endDel = (IEndTurnDelegate) m_bridge.getRemote();
	    return endDel.isGameOver();
	}
	*/
	private void play()
	{
		// change to active player
		m_ui.changeActivePlayer(getPlayerID());
		// Get the relevant delegate
		final IGridPlayDelegate playDel = (IGridPlayDelegate) getPlayerBridge().getRemote();
		GridPlayData play = null;
		while (play == null)
		{
			play = m_ui.waitForPlay(getPlayerID(), getPlayerBridge());
			if (play == null)
			{
				// If play==null, the play was interrupted,
				// most likely by the player trying to leave the game.
				// So, we should not try asking the UI to get a new play.
				return;
			}
			else
			{
				// A play was returned from the user interface.
				// We need to have the relevant delegate process it
				// and see if there are any problems with the play.
				final String error = playDel.play(play.getStart(), play.getEnd());
				if (error != null)
				{
					// If there is a problem with the play, notify the user...
					m_ui.notifyError(error);
					// ... then have the user try again.
					play = null;
				}
				else
				{
					m_ui.notifyError("");
				}
			}
		}
	}
	
	public UnitType selectUnit(final Unit startUnit, final Collection<UnitType> options, final Territory territory, final PlayerID player, final GameData data, final String message)
	{
		return m_ui.selectUnit(startUnit, options, territory, player, data, message);
	}
}
