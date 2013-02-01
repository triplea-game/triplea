package games.strategy.chess.player;

import games.strategy.chess.ui.ChessFrame;
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
	}
}
