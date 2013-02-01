package games.strategy.chess.ui;

import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.gamePlayer.IGamePlayer;

import java.util.Set;

import javax.swing.JComponent;

public class ChessFrame extends MainGameFrame
{
	private static final long serialVersionUID = 5408794084347965065L;
	private boolean m_gameOver;
	
	public ChessFrame(final IGame game, final Set<IGamePlayer> players)
	{
	}
	
	@Override
	public IGame getGame()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void leaveGame()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void shutdown()
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void notifyError(final String error)
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public JComponent getMainPanel()
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Determine whether the game is over.
	 * 
	 * @return <code>true</code> if the game is over, <code>false</code> otherwise
	 */
	public boolean isGameOver()
	{
		return m_gameOver;
	}
}
