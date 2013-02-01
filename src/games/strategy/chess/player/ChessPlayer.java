package games.strategy.chess.player;

import games.strategy.chess.delegate.remote.IPlayDelegate;
import games.strategy.chess.ui.ChessFrame;
import games.strategy.chess.ui.PlayData;
import games.strategy.common.player.AbstractHumanPlayer;

public class ChessPlayer extends AbstractHumanPlayer<ChessFrame> implements IChessPlayer
{
	public ChessPlayer(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	public void start(final String stepName)
	{
		if (m_ui != null && m_ui.isGameOver())
			return;
		if (stepName.endsWith("Play"))
			play();
		else
			throw new IllegalArgumentException("Unrecognized step stepName:" + stepName);
	}
	
	private void play()
	{
		// Get the relevant delegate
		final IPlayDelegate playDel = (IPlayDelegate) getPlayerBridge().getRemote();
		PlayData play = null;
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
				final String error = playDel.play(play.getStart(), play.getEnd(), play.getUnit());
				if (error != null)
				{
					// If there is a problem with the play, notify the user...
					m_ui.notifyError(error);
					// ... then have the user try again.
					play = null;
				}
			}
		}
	}
}
