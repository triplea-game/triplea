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
import games.strategy.engine.GameOverException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.grid.delegate.remote.IGridEditDelegate;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.IGridPlayData;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.ButtonModel;
import javax.swing.SwingUtilities;

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
		enableEditModeMenu();
		/*{
		    CountDownLatch waitToLeaveGame = new CountDownLatch(1);
		    try {
		        
		        //wait();
		        waitToLeaveGame.await();
		    } catch (InterruptedException e) {}
		}*/
		boolean badStep = false;
		if (stepName.endsWith("Play"))
			play();
		else if (stepName.endsWith("EndTurn"))
			endTurn();
		else
			badStep = true;
		
		disableEditModeMenu();
		
		if (badStep)
			throw new IllegalArgumentException("Unrecognized step stepName:" + stepName);
	}
	
	/*
	private boolean gameOver()
	{
	    IEndTurnDelegate endDel = (IEndTurnDelegate) m_bridge.getRemote();
	    return endDel.isGameOver();
	}
	*/
	protected void play()
	{
		// change to active player
		m_ui.changeActivePlayer(getPlayerID());
		// Get the relevant delegate
		final IGridPlayDelegate playDel;
		try
		{
			playDel = (IGridPlayDelegate) getPlayerBridge().getRemote();
		} catch (final GameOverException goe)
		{
			return;
		}
		// final PlayerID me = getPlayerID();
		IGridPlayData play = null;
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
				final String error = playDel.play(play);
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
	
	protected void endTurn()
	{
		// change to active player
		m_ui.changeActivePlayer(getPlayerID());
		// do forum posting, if we can, otherwise we just end and skip this
		m_ui.waitForEndTurn(getPlayerID(), getPlayerBridge());
		// we do nothing, unless someone overrides this method
	}
	
	public UnitType selectUnit(final Unit startUnit, final Collection<UnitType> options, final Territory territory, final PlayerID player, final GameData data, final String message)
	{
		return m_ui.selectUnit(startUnit, options, territory, player, data, message);
	}
	
	private void enableEditModeMenu()
	{
		try
		{
			m_ui.setEditDelegate((IGridEditDelegate) getPlayerBridge().getRemote("edit"));
		} catch (final Exception e)
		{
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_ui.getEditModeButtonModel().addActionListener(m_editModeAction);
				m_ui.getEditModeButtonModel().setEnabled(true);
			}
		});
	}
	
	private void disableEditModeMenu()
	{
		m_ui.setEditDelegate(null);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_ui.getEditModeButtonModel().setEnabled(false);
				m_ui.getEditModeButtonModel().removeActionListener(m_editModeAction);
			}
		});
	}
	
	private final AbstractAction m_editModeAction = new AbstractAction()
	{
		private static final long serialVersionUID = -6514835498581811786L;
		
		public void actionPerformed(final ActionEvent ae)
		{
			final boolean editMode = ((ButtonModel) ae.getSource()).isSelected();
			try
			{
				// Set edit mode
				// All GameDataChangeListeners will be notified upon success
				final IGridEditDelegate editDelegate = (IGridEditDelegate) getPlayerBridge().getRemote("edit");
				editDelegate.setEditMode(editMode);
			} catch (final Exception e)
			{
				e.printStackTrace();
				// toggle back to previous state since setEditMode failed
				m_ui.getEditModeButtonModel().setSelected(!m_ui.getEditModeButtonModel().isSelected());
			}
		}
	};
}
