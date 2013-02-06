package games.strategy.grid;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.player.GridGamePlayer;
import games.strategy.grid.player.IGridGamePlayer;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridMapData;
import games.strategy.grid.ui.GridMapPanel;
import games.strategy.grid.ui.display.GridGameDisplay;
import games.strategy.grid.ui.display.IGridGameDisplay;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

/**
 * Abstract Game Loader for grid games.
 * 
 * @author veqryn
 * 
 */
abstract public class GridGame implements IGameLoader
{
	private static final long serialVersionUID = -7194416906783331148L;
	// When serializing, do not save transient member variables
	protected transient GridGameDisplay m_display;
	protected transient IGame m_game;
	
	abstract public Set<IGamePlayer> createPlayers(final Map<String, String> playerNames);
	
	/**
	 * Return an array of player types that can play on the server.
	 */
	abstract public String[] getServerPlayerTypes();
	
	abstract protected Class<? extends GridMapPanel> getGridMapPanelClass();
	
	abstract protected Class<? extends GridMapData> getGridMapDataClass();
	
	abstract protected Class<? extends BasicGameMenuBar<GridGameFrame>> getGridTableMenuClass();
	
	protected void initializeGame()
	{
	}
	
	public void startGame(final IGame game, final Set<IGamePlayer> players) throws Exception
	{
		try
		{
			m_game = game;
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					final GridGameFrame frame = new GridGameFrame(game, players, getGridMapPanelClass(), getGridMapDataClass(), getGridTableMenuClass());
					m_display = new GridGameDisplay(frame);
					m_game.addDisplay(m_display);
					initializeGame();
					frame.setVisible(true);
					connectPlayers(players, frame);
					final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
					final int availHeight = screenResolution.height - 30;
					final int availWidth = screenResolution.width;
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							// frame.setExtendedState(Frame.MAXIMIZED_BOTH);
							final Dimension currentSize = frame.getPreferredSize();
							if (currentSize.height > availHeight - 100 && currentSize.width > availWidth - 200)
								frame.setExtendedState(Frame.MAXIMIZED_BOTH);
							else if (currentSize.height > availHeight)
								frame.setExtendedState(Frame.MAXIMIZED_VERT);
							else if (currentSize.width > availWidth)
								frame.setExtendedState(Frame.MAXIMIZED_HORIZ);
							frame.toFront();
						}
					});
				}
			});
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		} catch (final InvocationTargetException e)
		{
			if (e.getCause() instanceof Exception)
				throw (Exception) e.getCause();
			else
			{
				e.printStackTrace();
				throw new IllegalStateException(e.getCause().getMessage());
			}
		}
	}
	
	private void connectPlayers(final Set<IGamePlayer> players, final GridGameFrame frame)
	{
		for (final IGamePlayer player : players)
		{
			if (player instanceof GridGamePlayer)
				((GridGamePlayer) player).setFrame(frame);
		}
	}
	
	public void shutDown()
	{
		if (m_display != null)
		{
			m_game.removeDisplay(m_display);
			m_display.shutDown();
			m_display = null;
		}
	}
	
	/**
	 * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
	 */
	public Class<? extends IChannelSubscribor> getDisplayType()
	{
		return IGridGameDisplay.class;
	}
	
	public Class<? extends IRemote> getRemotePlayerType()
	{
		return IGridGamePlayer.class;
	}
	
	public IUnitFactory getUnitFactory()
	{
		return new DefaultUnitFactory();
	}
}
