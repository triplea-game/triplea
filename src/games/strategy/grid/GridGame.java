package games.strategy.grid;

import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.EditDelegate;
import games.strategy.grid.player.GridGamePlayer;
import games.strategy.grid.player.IGridGamePlayer;
import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridGameMenu;
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
	
	abstract protected Class<? extends GridGameMenu<GridGameFrame>> getGridTableMenuClass();
	
	/**
	 * Should be evenly divided by 2, or else your map might get drawn funny.
	 * (And depending on what your xMapPanel ui class is doing, it might have to be evenly divided by 5 and/or 10 too)
	 * 
	 * @return
	 */
	public int getSquareWidth()
	{
		return 50;
	}
	
	/**
	 * Should be evenly divided by 2, or else your map might get drawn funny.
	 * (And depending on what your xMapPanel ui class is doing, it might have to be evenly divided by 5 and/or 10 too)
	 * 
	 * @return
	 */
	public int getSquareHeight()
	{
		return 50;
	}
	
	public int getBevelSize()
	{
		return 25;
	}
	
	protected void initializeGame()
	{
	}
	
	public void startGame(final IGame game, final Set<IGamePlayer> players) throws Exception
	{
		try
		{
			m_game = game;
			if (game.getData().getDelegateList().getDelegate("edit") == null)
			{
				// an evil awful hack
				// we don't want to change the game xml
				// and invalidate mods so hack it
				// and force the addition here
				final EditDelegate delegate = new EditDelegate();
				delegate.initialize("edit", "edit");
				m_game.getData().getDelegateList().addDelegate(delegate);
				if (game instanceof ServerGame)
				{
					((ServerGame) game).addDelegateMessenger(delegate);
				}
			}
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					final GridGameFrame frame = new GridGameFrame(game, players, getGridMapPanelClass(), getGridMapDataClass(), getGridTableMenuClass(), getSquareWidth(), getSquareHeight(),
								getBevelSize());
					m_display = new GridGameDisplay(frame);
					m_game.addDisplay(m_display);
					initializeGame();
					connectPlayers(players, frame);
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
							final int availHeight = screenResolution.height - 30;
							final int availWidth = screenResolution.width;
							// frame.setExtendedState(Frame.MAXIMIZED_BOTH);
							final Dimension currentSize = frame.getPreferredSize();
							// add a little, since we have stuff like history tab, etc, that increases the width
							currentSize.height = Math.min(availHeight, currentSize.height + 10);
							currentSize.width = Math.min(availWidth, currentSize.width + 10);
							frame.setPreferredSize(currentSize);
							frame.setSize(currentSize);
							if (currentSize.height > availHeight - 100 && currentSize.width > availWidth - 200)
								frame.setExtendedState(Frame.MAXIMIZED_BOTH);
							else if (currentSize.height > availHeight)
								frame.setExtendedState(Frame.MAXIMIZED_VERT);
							else if (currentSize.width > availWidth)
								frame.setExtendedState(Frame.MAXIMIZED_HORIZ);
							frame.setLocationRelativeTo(null);
							frame.setVisible(true);
							frame.toFront();
							frame.minimizeRightSidePanel();
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
	
	/**
	 * Only use this method from WITHIN a non-static Delegate class method.
	 * (Because only the host has the delegates)
	 */
	protected static final IDelegate findDelegate(final GameData data, final String delegate_name)
	{
		final IDelegate delegate = data.getDelegateList().getDelegate(delegate_name);
		if (delegate == null)
			throw new IllegalStateException(delegate_name + " delegate not found");
		return delegate;
	}
}
