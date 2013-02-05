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
package games.strategy.grid.ui;

import games.strategy.common.image.UnitImageFactory;
import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.common.ui.MacWrapper;
import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.ui.Util;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Tuple;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * User interface for King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2012-11-11 02:04:07 +0800 (Sun, 11 Nov 2012) $
 */
public class GridGameFrame extends MainGameFrame
{
	private static final long serialVersionUID = -8888229639450608930L;
	public static final int SQUARE_SIZE = 50;
	public static final int OUTSIDE_BEVEL_SIZE = 50;
	protected GameData m_data;
	protected IGame m_game;
	protected GridMapPanel m_mapPanel;
	protected JLabel m_status;
	protected final JLabel m_error;
	protected boolean m_gameOver;
	protected CountDownLatch m_waiting;
	protected PlayerID m_currentPlayer = PlayerID.NULL_PLAYERID;
	
	/**
	 * Construct a new user interface for a King's Table game.
	 * 
	 * @param game
	 * @param players
	 */
	public GridGameFrame(final IGame game, final Set<IGamePlayer> players, final Class<? extends GridMapPanel> gridMapPanelClass, final Class<? extends BasicGameMenuBar<GridGameFrame>> menuBarClass)
	{
		m_gameOver = false;
		m_waiting = null;
		m_game = game;
		m_data = game.getData();
		// Get the dimension of the gameboard - specified in the game's xml file.
		final int x_dim = m_data.getMap().getXDimension();
		final int y_dim = m_data.getMap().getYDimension();
		// The MapData holds info for the map,
		// including the dimensions (x_dim and y_dim)
		// and the size of each square (50 by 50)
		final GridMapData mapData = new GridMapData(m_data, x_dim, y_dim, SQUARE_SIZE, SQUARE_SIZE, OUTSIDE_BEVEL_SIZE, OUTSIDE_BEVEL_SIZE);
		// MapPanel is the Swing component that actually displays the gameboard.
		// m_mapPanel = new KingsTableMapPanel(mapData);
		try
		{
			final Constructor<? extends GridMapPanel> mapPanelConstructor = gridMapPanelClass.getConstructor(new Class[] { GridMapData.class, GridGameFrame.class });
			final GridMapPanel gridMapPanel = mapPanelConstructor.newInstance(mapData, this);
			m_mapPanel = gridMapPanel;
		} catch (final Exception e)
		{
			e.printStackTrace();
			throw new IllegalStateException("Could not initalize map panel for: " + gridMapPanelClass);
		}
		
		// This label will display whose turn it is
		m_status = new JLabel(" ");
		m_status.setAlignmentX(Component.CENTER_ALIGNMENT);
		m_status.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
		// This label will display any error messages
		m_error = new JLabel(" ");
		m_error.setAlignmentX(Component.CENTER_ALIGNMENT);
		// We need somewhere to put the map panel, status label, and error label
		final JPanel mainPanel = new JPanel();
		mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(m_mapPanel);
		mainPanel.add(m_status);
		mainPanel.add(m_error);
		this.setContentPane(mainPanel);
		// Set up the menu bar and window title
		// this.setJMenuBar(new KingsTableMenu(this));
		try
		{
			final Constructor<? extends BasicGameMenuBar<GridGameFrame>> menuConstructor = menuBarClass.getConstructor(new Class[] { GridGameFrame.class });
			final BasicGameMenuBar<GridGameFrame> menuBar = menuConstructor.newInstance(this);
			this.setJMenuBar(menuBar);
		} catch (final Exception e)
		{
			e.printStackTrace();
			throw new IllegalStateException("Could not initalize Menu Bar for: " + menuBarClass);
		}
		
		this.setTitle(m_game.getData().getGameName());
		// If a user tries to close this frame, treat it as if they have asked to leave the game
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(final WindowEvent e)
			{
				leaveGame();
			}
		});
		// Resize the window, then make it visible
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}
	
	/**
	 * Update the user interface based on a game play.
	 * 
	 * @param territories
	 *            <code>Collection</code> of <code>Territory</code>s whose pieces were have changed.
	 */
	public void refreshTerritories(final Collection<Territory> territories)
	{
		m_mapPanel.refreshTerritories(territories);
	}
	
	/**
	 * Wait for a player to play.
	 * 
	 * @param player
	 *            the player to wait on
	 * @param bridge
	 *            the bridge for player
	 * @return PlayData representing a play, or <code>null</code> if the play was interrupted
	 */
	public GridPlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge)
	{
		/*
		if (m_gameOver)
		{
			m_waiting = new CountDownLatch(1);
			try
			{
				m_waiting.await();
			} catch (final InterruptedException e)
			{
				// ignore
			}
			return null;
		}*/
		GridPlayData play = null;
		try
		{
			while (play == null)
			{
				m_waiting = new CountDownLatch(1);
				play = m_mapPanel.waitForPlay(player, bridge, m_waiting);
			}
		} catch (final InterruptedException e)
		{
			return null;
		}
		return play;
	}
	
	public void changeActivePlayer(final PlayerID player)
	{
		if (player == null)
			m_currentPlayer = PlayerID.NULL_PLAYERID;
		else
			m_currentPlayer = player;
	}
	
	/**
	 * This only applies to the UI for this local machine. Therefore it returns the "last" active player that was played on this machine.
	 * 
	 * @return
	 */
	public PlayerID getActivePlayer()
	{
		return m_currentPlayer;
	}
	
	/**
	 * Get the <code>IGame</code> for the current game.
	 * 
	 * @return the <code>IGame</code> for the current game
	 */
	@Override
	public IGame getGame()
	{
		return m_game;
	}
	
	/**
	 * Process a user request to leave the game.
	 */
	@Override
	public void leaveGame()
	{
		if (!m_gameOver)
		{
			// Make sure the user really wants to leave the game.
			final int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to leave?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION);
			if (rVal != JOptionPane.OK_OPTION)
				return;
		}
		// We need to let the MapPanel know that we're leaving the game.
		// Once the CountDownLatch has counted down to zero,
		// the MapPanel will stop listening for mouse clicks,
		// and its thread will be able to terminate.
		if (m_waiting != null)
		{
			synchronized (m_waiting)
			{
				while (m_waiting.getCount() > 0)
					m_waiting.countDown();
			}
		}
		// Exit the game.
		if (m_game instanceof ServerGame)
		{
			((ServerGame) m_game).stopGame();
		}
		else
		{
			m_game.getMessenger().shutDown();
			((ClientGame) m_game).shutDown();
			// an ugly hack, we need a better
			// way to get the main frame
			MainFrame.getInstance().clientLeftGame();
		}
	}
	
	/**
	 * Process a user request to stop the game.
	 * 
	 * This method is responsible for de-activating this frame.
	 */
	public void stopGame()
	{
		if (GameRunner.isMac())
		{
			// this frame should not handle shutdowns anymore
			MacWrapper.unregisterShutdownHandler();
		}
		this.setVisible(false);
		this.dispose();
		m_game = null;
		if (m_data != null)
			m_data.clearAllListeners();
		m_data = null;
		m_mapPanel = null;
		m_status = null;
		for (final WindowListener l : this.getWindowListeners())
			this.removeWindowListener(l);
	}
	
	/**
	 * Process a user request to exit the program.
	 */
	@Override
	public void shutdown()
	{
		if (!m_gameOver)
		{
			final int rVal = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION);
			if (rVal != JOptionPane.OK_OPTION)
				return;
		}
		stopGame();
		System.exit(0);
	}
	
	/**
	 * Set the game over status for this frame to <code>true</code>.
	 */
	public void setGameOver()// CountDownLatch waiting)
	{
		m_gameOver = true;
		// m_waiting = waiting;
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
	
	/**
	 * Graphically notify the user of an error.
	 * 
	 * @param error
	 *            the error message to display
	 */
	@Override
	public void notifyError(final String error)
	{
		m_error.setText(error);
	}
	
	/**
	 * Graphically notify the user of the current game status.
	 * 
	 * @param error
	 *            the status message to display
	 */
	public void setStatus(final String status)
	{
		m_error.setText(" ");
		m_status.setText(status);
	}
	
	@Override
	public JComponent getMainPanel()
	{
		return m_mapPanel;
	}
	
	public UnitType selectUnit(final Unit startUnit, final Collection<UnitType> options, final Territory territory, final PlayerID player, final GameData data, final String message)
	{
		if (options == null || options.isEmpty())
			return null;
		if (options.size() == 1)
			return options.iterator().next();
		final AtomicReference<UnitType> selected = new AtomicReference<UnitType>();
		final Tuple<JPanel, JList> comps = Util.runInSwingEventThread(new Util.Task<Tuple<JPanel, JList>>()
		{
			public Tuple<JPanel, JList> run()
			{
				final JList list = new JList(new Vector<UnitType>(options));
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				list.setSelectedIndex(0);
				list.setCellRenderer(new UnitCellRenderer(m_mapPanel.getUnitImageFactory(), player, data));
				final JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				if (startUnit != null)
					panel.add(new JLabel("Promoting: " + startUnit.getType().getName() + (territory == null ? "" : " in " + territory.getName())), BorderLayout.NORTH);
				final JScrollPane scroll = new JScrollPane(list);
				panel.add(scroll, BorderLayout.CENTER);
				return new Tuple<JPanel, JList>(panel, list);
			}
		});
		final JPanel panel = comps.getFirst();
		final JList list = comps.getSecond();
		final String[] selectionOptions = { "OK", "Cancel" };
		final int selection = EventThreadJOptionPane.showOptionDialog(this, panel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, selectionOptions, null);
		if (selection == 0) // OK
			selected.set((UnitType) list.getSelectedValue());
		// Unit selected = (Unit) list.getSelectedValue();
		return selected.get();
	}
}


class UnitCellRenderer extends DefaultListCellRenderer
{
	private static final long serialVersionUID = 3247984570687473808L;
	private final UnitImageFactory m_imageFactory;
	private final PlayerID m_player;
	private final GameData m_data;
	private final Hashtable<UnitType, ImageIcon> iconTable = new Hashtable<UnitType, ImageIcon>();
	
	public UnitCellRenderer(final UnitImageFactory imageFactory, final PlayerID player, final GameData data)
	{
		m_imageFactory = imageFactory;
		m_player = player;
		m_data = data;
	}
	
	@Override
	public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean hasFocus)
	{
		final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
		PlayerID player;
		UnitType type;
		if (value instanceof Unit)
		{
			type = ((Unit) value).getType();
			player = ((Unit) value).getOwner();
		}
		else if (value instanceof UnitType)
		{
			type = (UnitType) value;
			player = m_player;
		}
		else
		{
			type = null;
			player = m_player;
		}
		if (type != null)
		{
			ImageIcon icon = iconTable.get(type);
			if (icon == null)
			{
				icon = new ImageIcon(m_imageFactory.getImage(type, player, m_data));
				iconTable.put(type, icon);
			}
			label.setIcon(icon);
			label.setText(type.getName());
			label.setToolTipText(type.getName());
		}
		else
		{
			label.setIcon(null);
		}
		return (label);
	}
}
