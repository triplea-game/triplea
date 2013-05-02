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

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.common.image.UnitImageFactory;
import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.common.ui.MacWrapper;
import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.PlayerChatRenderer;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.HistorySynchronizer;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.grid.delegate.remote.IGridEditDelegate;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.Util;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Tuple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * User interface for Grid Games.
 * 
 * @author Lane Schwartz (original) and Veqryn (abstraction and major rewrite)
 * @version $LastChangedDate: 2012-11-11 02:04:07 +0800 (Sun, 11 Nov 2012) $
 */
public class GridGameFrame extends MainGameFrame
{
	private static final long serialVersionUID = -8888229639450608930L;
	protected GameData m_data;
	protected IGame m_game;
	protected final GridMapData m_mapData;
	protected GridMapPanel m_mapPanel;
	protected GridGameMenu<GridGameFrame> m_menuBar;
	protected JLabel m_status;
	protected JLabel m_error;
	protected boolean m_gameOver;
	protected CountDownLatch m_waiting;
	protected PlayerID m_currentPlayer = PlayerID.NULL_PLAYERID;
	
	protected IGridEditDelegate m_editDelegate;
	private final ButtonModel m_editModeButtonModel;
	
	protected JPanel m_gameMainPanel = new JPanel();
	protected JPanel m_gameSouthPanel;
	protected JPanel m_rightHandSidePanel = new JPanel();
	protected JPanel m_mapAndChatPanel;
	protected ChatPanel m_chatPanel;
	protected JSplitPane m_chatSplit;
	protected JSplitPane m_gameCenterPanel;
	protected HistoryPanel m_historyPanel;
	protected boolean m_inHistory = false;
	protected boolean m_inGame = true;
	protected HistorySynchronizer m_historySyncher;
	protected JPanel m_historyComponent = new JPanel();
	
	/**
	 * Construct a new user interface for a King's Table game.
	 * 
	 * @param game
	 * @param players
	 */
	public GridGameFrame(final IGame game, final Set<IGamePlayer> players, final Class<? extends GridMapPanel> gridMapPanelClass, final Class<? extends GridMapData> gridMapDataClass,
				final Class<? extends GridGameMenu<GridGameFrame>> menuBarClass, final int squareWidth, final int squareHeight, final int bevelSize)
	{
		super("TripleA - " + game.getData().getGameName(), players);
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
		// m_mapData = new GridMapData(m_data, x_dim, y_dim, SQUARE_SIZE, SQUARE_SIZE, OUTSIDE_BEVEL_SIZE, OUTSIDE_BEVEL_SIZE);
		try
		{
			final Constructor<? extends GridMapData> mapDataConstructor = gridMapDataClass.getConstructor(new Class[] {
						GameMap.class, int.class, int.class, int.class, int.class, int.class, int.class });
			final GridMapData gridMapData = mapDataConstructor.newInstance(m_data.getMap(), x_dim, y_dim, squareWidth, squareHeight, bevelSize, bevelSize);
			m_mapData = gridMapData;
		} catch (final Exception e)
		{
			e.printStackTrace();
			throw new IllegalStateException("Could not initalize map data for: " + gridMapDataClass);
		}
		// create the scroll model
		final ImageScrollModel model = new ImageScrollModel();
		model.setScrollX(false);
		model.setScrollY(false);
		model.setMaxBounds(m_mapData.getMapDimensions().width, m_mapData.getMapDimensions().height);
		
		// MapPanel is the Swing component that actually displays the gameboard.
		// m_mapPanel = new KingsTableMapPanel(mapData);
		try
		{
			final Constructor<? extends GridMapPanel> mapPanelConstructor = gridMapPanelClass.getConstructor(new Class[] {
						GameData.class, GridMapData.class, GridGameFrame.class, ImageScrollModel.class });
			final GridMapPanel gridMapPanel = mapPanelConstructor.newInstance(m_data, m_mapData, this, model);
			m_mapPanel = gridMapPanel;
		} catch (final Exception e)
		{
			e.printStackTrace();
			throw new IllegalStateException("Could not initalize map panel for: " + gridMapPanelClass);
		}
		// add arrow key listener
		this.addKeyListener(m_arrowKeyActionListener);
		m_mapPanel.addKeyListener(m_arrowKeyActionListener);
		
		// This label will display whose turn it is
		m_status = new JLabel("Some Text To Set A Reasonable preferred Size");
		m_status.setAlignmentX(Component.CENTER_ALIGNMENT);
		m_status.setPreferredSize(m_status.getPreferredSize());
		m_status.setText(" ");
		// This label will display any error messages
		m_error = new JLabel("Some Text To Set A Reasonable preferred Size");
		m_error.setAlignmentX(Component.CENTER_ALIGNMENT);
		m_error.setPreferredSize(m_error.getPreferredSize());
		m_error.setText(" ");
		
		// initialize m_editModeButtonModel before setJMenuBar()
		m_editModeButtonModel = new JToggleButton.ToggleButtonModel();
		m_editModeButtonModel.setEnabled(false);
		
		// next we add the chat panel, but only if there is one (only because we are hosting/network game)
		m_mapAndChatPanel = new JPanel();
		m_mapAndChatPanel.setLayout(new BorderLayout());
		m_chatSplit = new JSplitPane();
		m_chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
		m_chatSplit.setOneTouchExpandable(true);
		m_chatSplit.setDividerSize(8);
		m_chatSplit.setResizeWeight(0.95);
		if (MainFrame.getInstance().getChat() != null)
		{
			m_chatPanel = new ChatPanel(MainFrame.getInstance().getChat());
			m_chatPanel.setPlayerRenderer(new PlayerChatRenderer(m_game, null));
			final Dimension chatPrefSize = new Dimension((int) m_chatPanel.getPreferredSize().getWidth(), 95);
			m_chatPanel.setPreferredSize(chatPrefSize);
			m_chatSplit.setTopComponent(m_mapPanel);
			m_chatSplit.setBottomComponent(m_chatPanel);
			m_mapAndChatPanel.add(m_chatSplit, BorderLayout.CENTER);
		}
		else
		{
			m_mapAndChatPanel.add(m_mapPanel, BorderLayout.CENTER);
		}
		
		// status and error panel
		m_gameMainPanel.setLayout(new BorderLayout());
		m_gameMainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
		m_gameSouthPanel = new JPanel();
		m_gameSouthPanel.setLayout(new BoxLayout(m_gameSouthPanel, BoxLayout.Y_AXIS));
		m_gameSouthPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		m_gameSouthPanel.add(m_status);
		m_gameSouthPanel.add(m_error);
		m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
		
		// now make right hand side panel, and add it to center panel
		m_rightHandSidePanel.setLayout(new BorderLayout());
		final Dimension rightSidePanel = new Dimension(200, 200);
		// m_rightHandSidePanel.setMinimumSize(rightSidePanel);
		m_rightHandSidePanel.setPreferredSize(rightSidePanel);
		m_gameCenterPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_mapAndChatPanel, m_rightHandSidePanel);
		m_gameCenterPanel.setOneTouchExpandable(true);
		m_gameCenterPanel.setDividerSize(8);
		m_gameCenterPanel.setResizeWeight(1.0);
		m_gameMainPanel.add(m_gameCenterPanel, BorderLayout.CENTER);
		m_gameCenterPanel.resetToPreferredSizes();
		
		// set up the edit mode overlay text
		this.setGlassPane(new JComponent()
		{
			private static final long serialVersionUID = 9077566112856052017L;
			
			@Override
			protected void paintComponent(final Graphics g)
			{
				g.setFont(new Font("Ariel", Font.BOLD, 50));
				g.setColor(Color.GRAY);
				g.drawString("Edit Mode", 200, 200);
			}
		});
		
		// finally, set the content pane for this frame
		// this.setContentPane(m_mainPanel);
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);
		
		// Set up the menu bar and window title
		try
		{
			final Constructor<? extends GridGameMenu<GridGameFrame>> menuConstructor = menuBarClass.getConstructor(new Class[] { GridGameFrame.class });
			m_menuBar = menuConstructor.newInstance(this);
			this.setJMenuBar(m_menuBar);
		} catch (final Exception e)
		{
			e.printStackTrace();
			throw new IllegalStateException("Could not initalize Menu Bar for: " + menuBarClass);
		}
		
		this.setTitle(m_game.getData().getGameName());
		// If a user tries to close this frame, treat it as if they have asked to leave the game
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(final WindowEvent e)
			{
				leaveGame();
			}
		});
		
		m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		m_data.addDataChangeListener(m_dataChangeListener);
		
		this.pack();
		// minimizeRightSidePanel(); // for whatever reason it is better to do this after we show the frame
	}
	
	public void minimizeRightSidePanel()
	{
		// click the minimize button so that the right side starts minimized
		final BasicSplitPaneUI ui = (BasicSplitPaneUI) m_gameCenterPanel.getUI();
		final Container divider = ui.getDivider();
		final JButton max = (JButton) divider.getComponent(1);
		max.doClick();
	}
	
	public void maximizeRightSidePanel()
	{
		m_gameCenterPanel.resetToPreferredSizes();
		/*
		final BasicSplitPaneUI ui = (BasicSplitPaneUI) m_gameCenterPanel.getUI();
		final Container divider = ui.getDivider();
		final JButton max = (JButton) divider.getComponent(0);
		max.doClick();
		*/
	}
	
	public static void renderUnits(final Container container, final GridBagConstraints mainConstraints, final Collection<Unit> units, final GridMapPanel mapPanel, final GameData data)
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		for (final Unit u : units)
		{
			final JLabel label = new JLabel(new ImageIcon(mapPanel.getUnitImageFactory().getImage(u.getType(), u.getOwner(), data)));
			label.setToolTipText(u.getType().getName());
			panel.add(label);
		}
		container.add(panel, mainConstraints);
	}
	
	public static void renderObject(final Container container, final GridBagConstraints mainConstraints, final Object renderObject, final GridMapPanel mapPanel, final GameData data)
	{
		if (renderObject == null)
			return;
		if (renderObject instanceof Collection)
		{
			@SuppressWarnings("unchecked")
			final Collection<Object> objects = (Collection<Object>) renderObject;
			final Iterator<Object> objIter = objects.iterator();
			if (objIter.hasNext())
			{
				final Object obj = objIter.next();
				if (obj instanceof Unit)
				{
					@SuppressWarnings("unchecked")
					final Collection<Unit> units = (Collection<Unit>) renderObject;
					renderUnits(container, mainConstraints, units, mapPanel, data);
				}
			}
		}
	}
	
	public void updateRightSidePanel(final String message, final Object renderObject)
	{
		final JPanel rightSide = new JPanel();
		final JTextArea title = new JTextArea();
		final JScrollPane scroll = new JScrollPane(title);
		rightSide.setLayout(new GridBagLayout());
		title.setWrapStyleWord(true);
		title.setBackground(this.getBackground());
		title.setLineWrap(true);
		title.setBorder(null);
		title.setEditable(false);
		scroll.setBorder(null);
		final Insets insets = new Insets(5, 0, 0, 0);
		title.setText(message);
		rightSide.add(scroll, new GridBagConstraints(0, 0, 1, 1, 1, 0.1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0));
		final GridBagConstraints mainConstraints = new GridBagConstraints(0, 1, 1, 1, 1, 0.9, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0);
		renderObject(rightSide, mainConstraints, renderObject, m_mapPanel, m_data);
		rightSide.add(Box.createGlue());
		updateRightSidePanel(rightSide);
	}
	
	public void updateRightSidePanel(final Component component)
	{
		m_rightHandSidePanel.removeAll();
		m_rightHandSidePanel.add(component, BorderLayout.CENTER);
		m_rightHandSidePanel.validate();
	}
	
	Action getShowGameAction()
	{
		return m_showGameAction;
	}
	
	Action getShowHistoryAction()
	{
		return m_showHistoryAction;
	}
	
	Action getSaveScreenshotAction()
	{
		return m_saveScreenshotAction;
	}
	
	protected final AbstractAction m_showHistoryAction = new AbstractAction("Show history")
	{
		private static final long serialVersionUID = -7099175363241411428L;
		
		public void actionPerformed(final ActionEvent e)
		{
			showHistory();
			m_showGameAction.setEnabled(true);
			this.setEnabled(false);
			m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		}
	};
	protected final AbstractAction m_showGameAction = new AbstractAction("Show current game")
	{
		private static final long serialVersionUID = 7470409812651698208L;
		
		{
			setEnabled(false);
		}
		
		public void actionPerformed(final ActionEvent e)
		{
			showGame();
			m_showHistoryAction.setEnabled(true);
			this.setEnabled(false);
			m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		}
	};
	protected final AbstractAction m_saveScreenshotAction = new AbstractAction("Export Screenshot...")
	{
		private static final long serialVersionUID = -5908032486008953815L;
		
		public void actionPerformed(final ActionEvent e)
		{
			HistoryNode curNode = null;
			if (m_historyPanel == null)
				curNode = m_data.getHistory().getLastNode();
			else
				curNode = m_historyPanel.getCurrentNode();
			saveScreenshot(curNode, m_data);
		}
	};
	
	public void showGame()
	{
		m_inGame = true;
		if (m_inHistory)
		{
			m_inHistory = false;
			if (m_historySyncher != null)
			{
				m_historySyncher.deactivate();
				m_historySyncher = null;
			}
			m_historyPanel.goToEnd();
			m_historyPanel = null;
			m_mapPanel.getData().removeDataChangeListener(m_dataChangeListener);
			m_mapPanel.setGameData(m_data);
			m_data.addDataChangeListener(m_dataChangeListener);
		}
		m_gameMainPanel.removeAll();
		m_gameMainPanel.setLayout(new BorderLayout());
		m_gameMainPanel.add(m_gameCenterPanel, BorderLayout.CENTER);
		m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
		getContentPane().removeAll();
		getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);
		validate();
	}
	
	protected void showHistory()
	{
		m_inHistory = true;
		m_inGame = false;
		final GameData clonedGameData;
		m_data.acquireReadLock();
		try
		{
			// we want to use a clone of the data, so we can make changes to it
			// as we walk up and down the history
			clonedGameData = GameDataUtils.cloneGameData(m_data);
			if (clonedGameData == null)
				return;
			m_data.removeDataChangeListener(m_dataChangeListener);
			clonedGameData.testLocksOnRead();
			if (m_historySyncher != null)
				throw new IllegalStateException("Two history synchers?");
			m_historySyncher = new HistorySynchronizer(clonedGameData, m_game);
			clonedGameData.addDataChangeListener(m_dataChangeListener);
		} finally
		{
			m_data.releaseReadLock();
		}
		m_mapPanel.setGameData(clonedGameData);
		final GridHistoryDetailsPanel historyDetailPanel = new GridHistoryDetailsPanel(clonedGameData, m_mapPanel);
		m_rightHandSidePanel.removeAll();
		m_rightHandSidePanel.add(historyDetailPanel);
		m_historyComponent.removeAll();
		m_historyComponent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 0));
		m_historyComponent.setLayout(new BorderLayout());
		// create history tree context menu
		// actions need to clear the history panel popup state when done
		final JPopupMenu popup = new JPopupMenu();
		popup.add(new AbstractAction("Show Summary Log")
		{
			private static final long serialVersionUID = -6730966512179268157L;
			
			public void actionPerformed(final ActionEvent ae)
			{
				final HistoryLog historyLog = new HistoryLog();
				historyLog.printRemainingTurn(m_historyPanel.getCurrentPopupNode(), false, m_data.getDiceSides(), null);
				for (final PlayerID player : m_data.getPlayerList().getPlayers())
				{
					final Collection<PlayerID> players = new ArrayList<PlayerID>();
					players.add(player);
					historyLog.printTerritorySummary(clonedGameData, players);
				}
				// historyLog.printProductionSummary(clonedGameData);
				m_historyPanel.clearCurrentPopupNode();
				historyLog.setVisible(true);
			}
		});
		popup.add(new AbstractAction("Save Screenshot")
		{
			private static final long serialVersionUID = 1222760138263428443L;
			
			public void actionPerformed(final ActionEvent ae)
			{
				saveScreenshot(m_historyPanel.getCurrentPopupNode(), clonedGameData);
				m_historyPanel.clearCurrentPopupNode();
			}
		});
		popup.add(new AbstractAction("Save Game at this point (BETA)")
		{
			private static final long serialVersionUID = 1430512376199927896L;
			
			public void actionPerformed(final ActionEvent ae)
			{
				m_data.acquireReadLock();
				// m_data.acquireWriteLock();
				try
				{
					final File f = BasicGameMenuBar.getSaveGameLocationDialog(GridGameFrame.this);
					if (f != null)
					{
						FileOutputStream fout = null;
						try
						{
							fout = new FileOutputStream(f);
							final GameData datacopy = GameDataUtils.cloneGameData(m_data, true);
							datacopy.getHistory().gotoNode(m_historyPanel.getCurrentPopupNode());
							datacopy.getHistory().removeAllHistoryAfterNode(m_historyPanel.getCurrentPopupNode());
							// TODO: the saved current delegate is still the current delegate, rather than the delegate at that history popup node
							// TODO: it still shows the current round number, rather than the round at the history popup node
							// TODO: this could be solved easily if rounds/steps were changes, but that could greatly increase the file size :(
							// TODO: this also does not undo the runcount of each delegate step
							@SuppressWarnings("rawtypes")
							final Enumeration enumeration = ((DefaultMutableTreeNode) datacopy.getHistory().getRoot()).preorderEnumeration();
							enumeration.nextElement();
							int round = 0;
							String stepDisplayName = datacopy.getSequence().getStep(0).getDisplayName();
							PlayerID currentPlayer = datacopy.getSequence().getStep(0).getPlayerID();
							while (enumeration.hasMoreElements())
							{
								final HistoryNode node = (HistoryNode) enumeration.nextElement();
								if (node instanceof Round)
								{
									round = ((Round) node).getRoundNo();
									currentPlayer = null;
									stepDisplayName = node.getTitle();
								}
								else if (node instanceof Step)
								{
									currentPlayer = ((Step) node).getPlayerID();
									stepDisplayName = node.getTitle();
								}
							}
							datacopy.getSequence().setRoundAndStep(round, stepDisplayName, currentPlayer);
							new GameDataManager().saveGame(fout, datacopy);
							JOptionPane.showMessageDialog(GridGameFrame.this, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
						} catch (final IOException e)
						{
							e.printStackTrace();
						} finally
						{
							if (fout != null)
							{
								try
								{
									fout.close();
								} catch (final IOException e)
								{
									e.printStackTrace();
								}
							}
						}
					}
				} finally
				{
					// m_data.releaseWriteLock();
					m_data.releaseReadLock();
				}
				m_historyPanel.clearCurrentPopupNode();
			}
		});
		final JSplitPane split = new JSplitPane();
		split.setOneTouchExpandable(true);
		split.setDividerSize(8);
		m_historyPanel = new HistoryPanel(clonedGameData, historyDetailPanel, popup, null);
		split.setLeftComponent(m_historyPanel);
		split.setRightComponent(m_gameCenterPanel);
		split.setDividerLocation(150);
		m_historyComponent.add(split, BorderLayout.CENTER);
		m_historyComponent.add(m_gameSouthPanel, BorderLayout.SOUTH);
		getContentPane().removeAll();
		getContentPane().add(m_historyComponent, BorderLayout.CENTER);
		validate();
	}
	
	public void saveScreenshot(final HistoryNode node, final GameData data)
	{
		final FileFilter pngFilter = new FileFilter()
		{
			@Override
			public boolean accept(final File f)
			{
				if (f.isDirectory())
					return true;
				else
					return f.getName().endsWith(".png");
			}
			
			@Override
			public String getDescription()
			{
				return "Saved Screenshots, *.png";
			}
		};
		final JFileChooser fileChooser = new SaveGameFileChooser();
		fileChooser.setFileFilter(pngFilter);
		final int rVal = fileChooser.showSaveDialog(this);
		if (rVal == JFileChooser.APPROVE_OPTION)
		{
			File f = fileChooser.getSelectedFile();
			if (!f.getName().toLowerCase().endsWith(".png"))
				f = new File(f.getParent(), f.getName() + ".png");
			// A small warning so users will not over-write a file,
			if (f.exists())
			{
				final int choice = JOptionPane.showConfirmDialog(this, "A file by that name already exists. Do you wish to over write it?", "Over-write?", JOptionPane.YES_NO_OPTION,
							JOptionPane.WARNING_MESSAGE);
				if (choice != JOptionPane.OK_OPTION)
					return;
			}
			final File file = f;
			final Runnable t = new Runnable()
			{
				public void run()
				{
					if (saveScreenshot(node, data, file))
						JOptionPane.showMessageDialog(GridGameFrame.this, "Screenshot Saved", "Screenshot Saved", JOptionPane.INFORMATION_MESSAGE);
				}
			};
			if (!SwingUtilities.isEventDispatchThread())
			{
				try
				{
					SwingUtilities.invokeAndWait(t);
				} catch (final Exception e2)
				{
					e2.printStackTrace();
				}
			}
			else
			{
				t.run();
			}
		}
	}
	
	public boolean saveScreenshot(final HistoryNode node, final GameData data, final File file)
	{
		// get current history node. if we are in history view, get the selected node.
		boolean retval = true;
		// get round/step/player from history tree
		int round = 0;
		// String step = null;
		// PlayerID player = null;
		final Object[] pathFromRoot = node.getPath();
		for (final Object pathNode : pathFromRoot)
		{
			final HistoryNode curNode = (HistoryNode) pathNode;
			if (curNode instanceof Round)
				round = ((Round) curNode).getRoundNo();
			if (curNode instanceof Step)
			{
				// player = ((Step) curNode).getPlayerID();
				// step = curNode.getTitle();
			}
		}
		final double scale = 1;
		// print map panel to image
		final BufferedImage mapImage = Util.createImage((int) (scale * m_mapPanel.getImageWidth()), (int) (scale * m_mapPanel.getImageHeight()), false);
		final Graphics2D mapGraphics = mapImage.createGraphics();
		try
		{
			data.acquireReadLock();
			try
			{
				// workaround to get the whole map
				// (otherwise the map is cut if current window is not on top of map)
				final int xOffset = m_mapPanel.getXOffset();
				final int yOffset = m_mapPanel.getYOffset();
				m_mapPanel.setTopLeft(0, 0);
				m_mapPanel.print(mapGraphics);
				m_mapPanel.setTopLeft(xOffset, yOffset);
			} finally
			{
				data.releaseReadLock();
			}
			// overlay title
			final Color title_color = Color.BLACK;
			final int title_x = (int) (m_mapData.getBevelWidth() * scale);
			final int title_y = (int) (20 * scale);
			final int title_size = (int) (16 * scale);
			// everything else should be scaled down onto map image
			final AffineTransform transform = new AffineTransform();
			transform.scale(scale, scale);
			mapGraphics.setTransform(transform);
			mapGraphics.setFont(new Font("Ariel", Font.BOLD, title_size));
			mapGraphics.setColor(title_color);
			mapGraphics.drawString(data.getGameName() + "    Round " + round, title_x, title_y); // + ": " + (player != null ? player.getName() : "") + " - " + step
			
			// save Image as .png
			try
			{
				ImageIO.write(mapImage, "png", file);
			} catch (final Exception e2)
			{
				e2.printStackTrace();
				JOptionPane.showMessageDialog(GridGameFrame.this, e2.getMessage(), "Error saving Screenshot", JOptionPane.OK_OPTION);
				retval = false;
			}
			// Clean up objects. There might be some overkill here,
			// but there were memory leaks that are fixed by some/all of these.
		} finally
		{
			mapImage.flush();
			mapGraphics.dispose();
		}
		return retval;
	}
	
	/**
	 * Update the user interface based on a game play.
	 * 
	 * @param territories
	 *            <code>Collection</code> of <code>Territory</code>s whose pieces were have changed.
	 */
	public void refreshTerritories(final Collection<Territory> territories)
	{
		if (m_mapPanel != null)
			m_mapPanel.refreshTerritories(territories);
	}
	
	public void updateAllImages()
	{
		if (m_mapPanel != null)
			m_mapPanel.updateAllImages();
	}
	
	/**
	 * Update the user interface based on a game play.
	 */
	public void showGridPlayDataMove(final IGridPlayData move)
	{
		m_mapPanel.showGridPlayDataMove(move);
	}
	
	public void showGridEndTurnData(final IGridEndTurnData endTurnData)
	{
		m_mapPanel.showGridEndTurnData(endTurnData);
	}
	
	/**
	 * Set up the tiles.
	 */
	public void initializeGridMapData(final GameMap map)
	{
		m_mapData.initializeGridMapData(map);
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
	public IGridPlayData waitForPlay(final PlayerID player, final IPlayerBridge bridge)
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
		IGridPlayData play = null;
		try
		{
			while (play == null)
			{
				if (m_mapPanel == null)
					return null; // we are exiting the game
				m_mapPanel.removeShutdownLatch(m_waiting);
				m_waiting = new CountDownLatch(1);
				m_mapPanel.addShutdownLatch(m_waiting);
				play = m_mapPanel.waitForPlay(player, bridge, m_waiting);
			}
		} catch (final InterruptedException e)
		{
			return null;
		} finally
		{
			if (m_mapPanel != null)
				m_mapPanel.removeShutdownLatch(m_waiting);
		}
		return play;
	}
	
	public IGridEndTurnData waitForEndTurn(final PlayerID player, final IPlayerBridge bridge)
	{
		IGridEndTurnData endTurn = null;
		try
		{
			while (endTurn == null)
			{
				if (m_mapPanel == null)
					return null; // we are exiting the game
				m_mapPanel.removeShutdownLatch(m_waiting);
				m_waiting = new CountDownLatch(1);
				m_mapPanel.addShutdownLatch(m_waiting);
				endTurn = m_mapPanel.waitForEndTurn(player, bridge, m_waiting);
			}
		} catch (final InterruptedException e)
		{
			return null;
		} finally
		{
			m_mapPanel.removeShutdownLatch(m_waiting);
		}
		return endTurn;
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
		m_mapPanel.shutDown();
		m_mapPanel = null;
		m_status = null;
		m_gameSouthPanel = null;
		if (m_chatPanel != null)
		{
			m_chatPanel.setPlayerRenderer(null);
			m_chatPanel.setChat(null);
		}
		m_chatPanel = null;
		m_chatSplit = null;
		m_gameMainPanel = null;
		m_mapPanel = null;
		m_mapAndChatPanel = null;
		m_status = null;
		m_rightHandSidePanel = null;
		for (final WindowListener l : this.getWindowListeners())
			this.removeWindowListener(l);
		m_localPlayers = null;
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
		m_error.setText((error == null ? " " : error));
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
		m_status.setText((status == null ? " " : status));
	}
	
	@Override
	public JComponent getMainPanel()
	{
		return m_mapPanel;
	}
	
	public GridMapPanel getMapPanel()
	{
		return m_mapPanel;
	}
	
	@Override
	public void setShowChatTime(final boolean showTime)
	{
		m_chatPanel.setShowChatTime(showTime);
	}
	
	public void showRightHandSidePanel()
	{
		m_rightHandSidePanel.setVisible(true);
	}
	
	public void hideRightHandSidePanel()
	{
		m_rightHandSidePanel.setVisible(false);
	}
	
	final KeyListener m_arrowKeyActionListener = new KeyListener()
	{
		final int diffPixel = 50;
		
		public void keyPressed(final KeyEvent e)
		{
			// scroll map according to wasd/arrowkeys
			final int x = m_mapPanel.getXOffset();
			final int y = m_mapPanel.getYOffset();
			final int keyCode = e.getKeyCode();
			if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D)
				m_mapPanel.setTopLeft(x + diffPixel, y);
			else if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A)
				m_mapPanel.setTopLeft(x - diffPixel, y);
			else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S)
				m_mapPanel.setTopLeft(x, y + diffPixel);
			else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W)
				m_mapPanel.setTopLeft(x, y - diffPixel);
			
			// minimize or maximize the right side panel
			if (keyCode == KeyEvent.VK_N)
			{
				minimizeRightSidePanel();
			}
			if (keyCode == KeyEvent.VK_M)
			{
				maximizeRightSidePanel();
			}
			
			// do other map panel specific things
			m_mapPanel.doKeyListenerEvents(e);
		}
		
		public void keyTyped(final KeyEvent e)
		{
		}
		
		public void keyReleased(final KeyEvent e)
		{
		}
	};
	
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
		final int selection = EventThreadJOptionPane.showOptionDialog(this, panel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, selectionOptions, null,
					m_mapPanel.getCountDownLatchHandler());
		if (selection == 0) // OK
			selected.set((UnitType) list.getSelectedValue());
		// Unit selected = (Unit) list.getSelectedValue();
		return selected.get();
	}
	
	protected void showEditMode()
	{
		if (m_editDelegate != null)
		{
			m_editModeButtonModel.setSelected(true);
			getGlassPane().setVisible(true);
			m_menuBar.enableEditOptionsMenu();
		}
	}
	
	protected void hideEditMode()
	{
		m_editModeButtonModel.setSelected(false);
		getGlassPane().setVisible(false);
		m_menuBar.disableEditOptionsMenu();
	}
	
	protected void setWidgetActivation()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setWidgetActivation();
				}
			});
			return;
		}
		if (m_editModeButtonModel != null)
		{
			if (m_editDelegate == null)
			{
				m_editModeButtonModel.setEnabled(false);
			}
			else
			{
				m_editModeButtonModel.setEnabled(true);
			}
		}
	}
	
	// setEditDelegate is called by the Player at the start and end of a turn
	public void setEditDelegate(final IGridEditDelegate editDelegate)
	{
		m_editDelegate = editDelegate;
		// force a data change event to update the UI for edit mode
		m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		setWidgetActivation();
	}
	
	public IGridEditDelegate getEditDelegate()
	{
		return m_editDelegate;
	}
	
	public ButtonModel getEditModeButtonModel()
	{
		return m_editModeButtonModel;
	}
	
	public boolean getEditMode()
	{
		boolean isEditMode = false;
		if (m_mapPanel == null)
			return false;
		// use GameData from mapPanel since it will follow current history node
		m_mapPanel.getData().acquireReadLock();
		try
		{
			isEditMode = BaseEditDelegate.getEditMode(m_mapPanel.getData());
		} finally
		{
			m_mapPanel.getData().releaseReadLock();
		}
		return isEditMode;
	}
	
	GameDataChangeListener m_dataChangeListener = new GameDataChangeListener()
	{
		public void gameDataChanged(final Change change)
		{
			try
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						if (getEditMode())
						{
							showEditMode();
						}
						else
						{
							hideEditMode();
						}
					}
				});
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	};
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
