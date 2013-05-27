package games.strategy.common.ui;

import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;

public class InGameLobbyWatcherWrapper
{
	private volatile InGameLobbyWatcher m_lobbyWatcher = null;
	
	public void setInGameLobbyWatcher(final InGameLobbyWatcher watcher)
	{
		m_lobbyWatcher = watcher;
	}
	
	public InGameLobbyWatcher getInGameLobbyWatcher()
	{
		return m_lobbyWatcher;
	}
	
	public void shutDown()
	{
		if (m_lobbyWatcher != null)
			m_lobbyWatcher.shutDown();
	}
	
	public boolean isActive()
	{
		return m_lobbyWatcher != null && m_lobbyWatcher.isActive();
	}
	
	public String getComments()
	{
		return m_lobbyWatcher == null ? "" : m_lobbyWatcher.getComments();
	}
	
	public void setGame(final IGame game)
	{
		if (m_lobbyWatcher != null)
			m_lobbyWatcher.setGame(game);
	}
	
	public void setGameComments(final String comments)
	{
		if (m_lobbyWatcher != null)
			m_lobbyWatcher.setGameComments(comments);
	}
	
	public void setGameSelectorModel(final GameSelectorModel model)
	{
		if (m_lobbyWatcher != null)
			m_lobbyWatcher.setGameSelectorModel(model);
	}
	
	public void setGameStatus(final GameStatus status, final IGame game)
	{
		if (m_lobbyWatcher != null)
			m_lobbyWatcher.setGameStatus(status, game);
	}
	
	public void setPassworded(final boolean passworded)
	{
		if (m_lobbyWatcher != null)
			m_lobbyWatcher.setPassworded(passworded);
	}
}
