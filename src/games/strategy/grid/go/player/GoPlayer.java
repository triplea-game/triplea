package games.strategy.grid.go.player;

import games.strategy.grid.go.delegate.remote.IGoEndTurnDelegate;
import games.strategy.grid.go.delegate.remote.IGoPlayDelegate;
import games.strategy.grid.go.ui.GoMapPanel;
import games.strategy.grid.go.ui.GoMapPanel.GO_DELEGATE_PHASE;
import games.strategy.grid.player.GridGamePlayer;
import games.strategy.grid.ui.IGridEndTurnData;
import games.strategy.grid.ui.IGridPlayData;

import java.util.concurrent.CountDownLatch;

public class GoPlayer extends GridGamePlayer
{
	public GoPlayer(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void play()
	{
		final IGoPlayDelegate playDel;
		try
		{
			playDel = (IGoPlayDelegate) getPlayerBridge().getRemote();
		} catch (final ClassCastException e)
		{
			System.err.println("PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: " + getPlayerBridge().getRemote().getClass());
			e.printStackTrace();
			return;
		}
		// if (playDel.haveTwoPassedInARow())
		// return;
		
		// change to active player
		m_ui.changeActivePlayer(getPlayerID());
		final GoMapPanel mapPanel = ((GoMapPanel) m_ui.getMainPanel());
		mapPanel.changePhase(GO_DELEGATE_PHASE.PLAY);
		// Get the relevant delegate
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
	
	@Override
	protected void endTurn()
	{
		final IGoEndTurnDelegate endTurnDel;
		try
		{
			endTurnDel = (IGoEndTurnDelegate) getPlayerBridge().getRemote();
		} catch (final ClassCastException e)
		{
			System.err.println("PlayerBridge step name: " + getPlayerBridge().getStepName() + ", Remote class name: " + getPlayerBridge().getRemote().getClass());
			e.printStackTrace();
			return;
		}
		// if (!endTurnDel.haveTwoPassedInARow())
		// return;
		
		m_ui.changeActivePlayer(getPlayerID());
		final GoMapPanel mapPanel = ((GoMapPanel) m_ui.getMainPanel());
		mapPanel.changePhase(GO_DELEGATE_PHASE.ENDGAME);
		
		// Get the relevant delegate
		// final PlayerID me = getPlayerID();
		IGridEndTurnData endTurnData = null;
		while (endTurnData == null)
		{
			endTurnData = m_ui.waitForEndTurn(getPlayerID(), getPlayerBridge());
			if (endTurnData == null)
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
				final String error = endTurnDel.territoryAdjustment(endTurnData);
				if (error != null)
				{
					// If there is a problem with the play, notify the user...
					m_ui.notifyError(error);
					// ... then have the user try again.
					endTurnData = null;
				}
				else
				{
					m_ui.notifyError("");
				}
			}
		}
		
		// TODO: move all end game stuff to play delegate. it is such a hack sitting here
		IGridEndTurnData forumPoster = null;
		CountDownLatch waiting = null;
		try
		{
			while (forumPoster == null)
			{
				if (mapPanel == null)
					return; // we are exiting the game
				mapPanel.removeShutdownLatch(waiting);
				waiting = new CountDownLatch(1);
				mapPanel.addShutdownLatch(waiting);
				forumPoster = mapPanel.waitForEndTurnForumPoster(getPlayerID(), getPlayerBridge(), waiting);
			}
		} catch (final InterruptedException e)
		{
			return;
		} finally
		{
			mapPanel.removeShutdownLatch(waiting);
		}
	}
	
}
