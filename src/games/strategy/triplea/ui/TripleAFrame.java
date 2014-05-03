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
/*
 * TripleAFrame.java
 * 
 * Created on November 5, 2001, 1:32 PM
 */
package games.strategy.triplea.ui;

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.common.ui.MacWrapper;
import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.PlayerChatRenderer;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.data.events.GameStepListener;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.HistorySynchronizer;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.sound.DefaultSoundChannel;
import games.strategy.sound.SoundPath;
import games.strategy.thread.ThreadPool;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.attatchments.AbstractConditionsAttachment;
import games.strategy.triplea.attatchments.AbstractTriggerAttachment;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.attatchments.UserActionAttachment;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import games.strategy.triplea.delegate.AirThatCantLandUtil;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.ui.history.HistoryDetailsPanel;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.Util;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.IntegerMap;
import games.strategy.util.LocalizeHTML;
import games.strategy.util.Tuple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * 
 * @author Sean Bridges
 * 
 *         Main frame for the triple a game
 */
public class TripleAFrame extends MainGameFrame
{
	private static final long serialVersionUID = 7640069668264418976L;
	private GameData m_data;
	private IGame m_game;
	private MapPanel m_mapPanel;
	private MapPanelSmallView m_smallView;
	private JLabel m_message = new JLabel("No selection");
	private JLabel m_status = new JLabel("");
	private JLabel m_step = new JLabel("xxxxxx");
	private JLabel m_round = new JLabel("xxxxxx");
	private JLabel m_player = new JLabel("xxxxxx");
	private ActionButtons m_actionButtons;
	private JPanel m_gameMainPanel = new JPanel();
	private JPanel m_rightHandSidePanel = new JPanel();
	private JTabbedPane m_tabsPanel = new JTabbedPane();
	private StatPanel m_statsPanel;
	private EconomyPanel m_economyPanel;
	private ObjectivePanel m_objectivePanel;
	private NotesPanel m_notesPanel;
	private TerritoryDetailPanel m_details;
	private JPanel m_historyComponent = new JPanel();
	private JPanel m_gameSouthPanel;
	private HistoryPanel m_historyPanel;
	private boolean m_inHistory = false;
	private boolean m_inGame = true;
	private HistorySynchronizer m_historySyncher;
	private IUIContext m_uiContext;
	private JPanel m_mapAndChatPanel;
	private ChatPanel m_chatPanel;
	private CommentPanel m_commentPanel;
	private JSplitPane m_chatSplit;
	private JSplitPane m_commentSplit;
	private EditPanel m_editPanel;
	private final ButtonModel m_editModeButtonModel;
	private final ButtonModel m_showCommentLogButtonModel;
	private IEditDelegate m_editDelegate;
	private JSplitPane m_gameCenterPanel;
	private Territory m_territoryLastEntered;
	private List<Unit> m_unitsBeingMousedOver;
	private PlayerID m_lastStepPlayer;
	private PlayerID m_currentStepPlayer;
	private Map<PlayerID, Boolean> m_requiredTurnSeries = new HashMap<PlayerID, Boolean>();
	private ThreadPool m_messageAndDialogThreadPool;
	private TripleaMenu m_menu;
	
	/** Creates new TripleAFrame */
	public TripleAFrame(final IGame game, final LocalPlayers players) throws IOException
	{
		super("TripleA - " + game.getData().getGameName(), players);
		m_game = game;
		m_data = game.getData();
		m_messageAndDialogThreadPool = new ThreadPool(1, "Message And Dialog Thread Pool");
		addZoomKeyboardShortcuts();
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(WINDOW_LISTENER);
		m_uiContext = new UIContext();
		m_uiContext.setDefaultMapDir(game.getData());
		m_uiContext.getMapData().verify(m_data);
		m_uiContext.setLocalPlayers(players);
		this.setCursor(m_uiContext.getCursor());
		// initialize m_editModeButtonModel before createMenuBar()
		m_editModeButtonModel = new JToggleButton.ToggleButtonModel();
		m_editModeButtonModel.setEnabled(false);
		m_showCommentLogButtonModel = new JToggleButton.ToggleButtonModel();
		m_showCommentLogButtonModel.addActionListener(m_showCommentLogAction);
		m_showCommentLogButtonModel.setSelected(false);
		m_menu = new TripleaMenu(this);
		this.setJMenuBar(m_menu);
		final ImageScrollModel model = new ImageScrollModel();
		model.setScrollX(m_uiContext.getMapData().scrollWrapX());
		model.setScrollY(m_uiContext.getMapData().scrollWrapY());
		model.setMaxBounds(m_uiContext.getMapData().getMapDimensions().width, m_uiContext.getMapData().getMapDimensions().height);
		final Image small = m_uiContext.getMapImage().getSmallMapImage();
		m_smallView = new MapPanelSmallView(small, model);
		m_mapPanel = new MapPanel(m_data, m_smallView, m_uiContext, model);
		m_mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);
		m_mapPanel.addMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
		this.addKeyListener(m_arrowKeyActionListener);
		m_mapPanel.addKeyListener(m_arrowKeyActionListener);
		
		// link the small and large images
		m_mapPanel.initSmallMap();
		m_mapAndChatPanel = new JPanel();
		m_mapAndChatPanel.setLayout(new BorderLayout());
		m_commentPanel = new CommentPanel(this, m_data);
		m_chatSplit = new JSplitPane();
		m_chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
		m_chatSplit.setOneTouchExpandable(true);
		m_chatSplit.setDividerSize(8);
		m_chatSplit.setResizeWeight(0.95);
		if (MainFrame.getInstance() != null && MainFrame.getInstance().getChat() != null)
		{
			m_commentSplit = new JSplitPane();
			m_commentSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
			m_commentSplit.setOneTouchExpandable(true);
			m_commentSplit.setDividerSize(8);
			m_commentSplit.setResizeWeight(0.5);
			m_commentSplit.setTopComponent(m_commentPanel);
			m_commentSplit.setBottomComponent(null);
			m_chatPanel = new ChatPanel(MainFrame.getInstance().getChat());
			m_chatPanel.setPlayerRenderer(new PlayerChatRenderer(m_game, m_uiContext));
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
		m_gameMainPanel.setLayout(new BorderLayout());
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);
		m_gameSouthPanel = new JPanel();
		m_gameSouthPanel.setLayout(new BorderLayout());
		// m_gameSouthPanel.add(m_message, BorderLayout.WEST);
		m_message.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		m_message.setPreferredSize(m_message.getPreferredSize());
		m_message.setText("some text to set a reasonable preferred size");
		m_status.setText("some text to set a reasonable preferred size for movement error messages");
		m_message.setPreferredSize(m_message.getPreferredSize());
		m_status.setPreferredSize(m_message.getPreferredSize());
		m_message.setText("");
		m_status.setText("");
		// m_gameSouthPanel.add(m_status, BorderLayout.CENTER);
		final JPanel bottomMessagePanel = new JPanel();
		bottomMessagePanel.setLayout(new GridBagLayout());
		bottomMessagePanel.setBorder(BorderFactory.createEmptyBorder());
		bottomMessagePanel.add(m_message, new GridBagConstraints(0, 0, 1, 1, .35, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		bottomMessagePanel.add(m_status, new GridBagConstraints(1, 0, 1, 1, .65, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		m_gameSouthPanel.add(bottomMessagePanel, BorderLayout.CENTER);
		m_status.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		final JPanel stepPanel = new JPanel();
		stepPanel.setLayout(new GridBagLayout());
		stepPanel.add(m_step, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		stepPanel.add(m_player, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		stepPanel.add(m_round, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		m_step.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		m_round.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		m_player.setBorder(new EtchedBorder(EtchedBorder.RAISED));
		m_step.setHorizontalTextPosition(SwingConstants.LEADING);
		m_gameSouthPanel.add(stepPanel, BorderLayout.EAST);
		m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
		m_rightHandSidePanel.setLayout(new BorderLayout());
		final FocusAdapter focusToMapPanelFocusListener = new FocusAdapter()
		{
			@Override
			public void focusGained(final FocusEvent e)
			{
				// give the focus back to the map panel
				m_mapPanel.requestFocusInWindow();
			}
		};
		m_rightHandSidePanel.addFocusListener(focusToMapPanelFocusListener);
		m_smallView.addFocusListener(focusToMapPanelFocusListener);
		m_tabsPanel.addFocusListener(focusToMapPanelFocusListener);
		m_rightHandSidePanel.add(m_smallView, BorderLayout.NORTH);
		m_tabsPanel.setBorder(null);
		m_rightHandSidePanel.add(m_tabsPanel, BorderLayout.CENTER);
		m_actionButtons = new ActionButtons(m_data, m_mapPanel, this);
		m_tabsPanel.addTab("Actions", m_actionButtons);
		m_actionButtons.setBorder(null);
		m_statsPanel = new StatPanel(m_data, m_uiContext);
		m_tabsPanel.addTab("Stats", m_statsPanel);
		m_economyPanel = new EconomyPanel(m_data);
		m_tabsPanel.addTab("Economy", m_economyPanel);
		m_objectivePanel = new ObjectivePanel(m_data);
		if (m_objectivePanel.isEmpty())
		{
			m_objectivePanel.removeDataChangeListener();
			m_objectivePanel = null;
		}
		else
		{
			m_tabsPanel.addTab(m_objectivePanel.getName(), m_objectivePanel);
		}
		m_notesPanel = new NotesPanel(m_data, m_menu.getGameNotesJEditorPane());
		m_tabsPanel.addTab("Notes", m_notesPanel);
		m_details = new TerritoryDetailPanel(m_mapPanel, m_data, m_uiContext, this);
		m_tabsPanel.addTab("Territory", m_details);
		m_editPanel = new EditPanel(m_data, m_mapPanel, this);
		// Register a change listener
		m_tabsPanel.addChangeListener(new ChangeListener()
		{
			// This method is called whenever the selected tab changes
			public void stateChanged(final ChangeEvent evt)
			{
				final JTabbedPane pane = (JTabbedPane) evt.getSource();
				// Get current tab
				final int sel = pane.getSelectedIndex();
				if (sel == -1)
					return;
				if (pane.getComponentAt(sel).equals(m_notesPanel))
				{
					m_notesPanel.layoutNotes();
				}
				else
				{ // for memory management reasons the notes are in a SoftReference, so we must remove our hard reference link to them so it can be reclaimed if needed
					m_notesPanel.removeNotes();
				}
				
				if (pane.getComponentAt(sel).equals(m_editPanel))
				{
					PlayerID player = null;
					m_data.acquireReadLock();
					try
					{
						player = m_data.getSequence().getStep().getPlayerID();
					} finally
					{
						m_data.releaseReadLock();
					}
					m_actionButtons.getCurrent().setActive(false);
					m_editPanel.display(player);
				}
				else
				{
					m_actionButtons.getCurrent().setActive(true);
					m_editPanel.setActive(false);
				}
			}
		});
		m_rightHandSidePanel.setPreferredSize(new Dimension((int) m_smallView.getPreferredSize().getWidth(), (int) m_mapPanel.getPreferredSize().getHeight()));
		m_gameCenterPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, m_mapAndChatPanel, m_rightHandSidePanel);
		m_gameCenterPanel.setOneTouchExpandable(true);
		m_gameCenterPanel.setDividerSize(8);
		m_gameCenterPanel.setResizeWeight(1.0);
		m_gameMainPanel.add(m_gameCenterPanel, BorderLayout.CENTER);
		m_gameCenterPanel.resetToPreferredSizes();
		// set up the edit mode overlay text
		this.setGlassPane(new JComponent()
		{
			private static final long serialVersionUID = 6724687534214427291L;
			
			@Override
			protected void paintComponent(final Graphics g)
			{
				g.setFont(new Font("Ariel", Font.BOLD, 50));
				g.setColor(new Color(255, 255, 255, 175));
				final Dimension size = m_mapPanel.getSize();
				g.drawString("Edit Mode", (int) ((size.getWidth() - 200) / 2), (int) ((size.getHeight() - 100) / 2));
			}
		});
		// force a data change event to update the UI for edit mode
		m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		m_data.addDataChangeListener(m_dataChangeListener);
		game.addGameStepListener(m_stepListener);
		updateStep();
		m_uiContext.addShutdownWindow(this);
	}
	
	private void addZoomKeyboardShortcuts()
	{
		final String zoom_map_in = "zoom_map_in";
		// do both = and + (since = is what you get when you hit ctrl+ )
		((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+', java.awt.event.InputEvent.META_MASK), zoom_map_in);
		((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+', java.awt.event.InputEvent.CTRL_MASK), zoom_map_in);
		((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('=', java.awt.event.InputEvent.META_MASK), zoom_map_in);
		((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('=', java.awt.event.InputEvent.CTRL_MASK), zoom_map_in);
		((JComponent) getContentPane()).getActionMap().put(zoom_map_in, new AbstractAction(zoom_map_in)
		{
			private static final long serialVersionUID = -7565304172320049817L;
			
			public void actionPerformed(final ActionEvent e)
			{
				if (getScale() < 100)
					setScale(getScale() + 10);
			}
		});
		final String zoom_map_out = "zoom_map_out";
		((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-', java.awt.event.InputEvent.META_MASK), zoom_map_out);
		((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('-', java.awt.event.InputEvent.CTRL_MASK), zoom_map_out);
		((JComponent) getContentPane()).getActionMap().put(zoom_map_out, new AbstractAction(zoom_map_out)
		{
			private static final long serialVersionUID = 7677111833274819304L;
			
			public void actionPerformed(final ActionEvent e)
			{
				if (getScale() > 16)
					setScale(getScale() - 10);
			}
		});
	}
	
	/**
	 * 
	 * @param value
	 *            - a number between 15 and 100
	 */
	void setScale(final double value)
	{
		getMapPanel().setScale(value / 100);
	}
	
	/**
	 * 
	 * @return a scale between 15 and 100
	 */
	private double getScale()
	{
		return getMapPanel().getScale() * 100;
	}
	
	@Override
	public void stopGame()
	{
		// we have already shut down
		if (m_uiContext == null)
			return;
		m_menu.dispose();
		m_menu = null;
		this.dispose();
		this.setVisible(false);
		if (GameRunner.isMac())
		{
			// this frame should not handle shutdowns anymore
			MacWrapper.unregisterShutdownHandler();
		}
		m_messageAndDialogThreadPool.shutDown();
		m_uiContext.shutDown();
		if (m_chatPanel != null)
		{
			m_chatPanel.setPlayerRenderer(null);
			m_chatPanel.setChat(null);
		}
		if (m_historySyncher != null)
		{
			m_historySyncher.deactivate();
			m_historySyncher = null;
		}
		// there is a bug in java (1.50._06 for linux at least)
		// where frames are not garbage collected.
		//
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6364875
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6368950
		//
		// so remove all references to everything
		// to minimize the damage
		m_game.removeGameStepListener(m_stepListener);
		m_game = null;
		m_uiContext = null;
		if (m_data != null)
			m_data.clearAllListeners();
		m_data = null;
		m_territoryLastEntered = null;
		if (m_unitsBeingMousedOver != null)
			m_unitsBeingMousedOver.clear();
		m_unitsBeingMousedOver = null;
		m_lastStepPlayer = null;
		m_currentStepPlayer = null;
		if (m_requiredTurnSeries != null)
			m_requiredTurnSeries.clear();
		m_requiredTurnSeries = null;
		m_editDelegate = null;
		if (m_tabsPanel != null)
			m_tabsPanel.removeAll();
		if (m_commentPanel != null)
			m_commentPanel.cleanUp();
		MAP_SELECTION_LISTENER = null;
		m_actionButtons = null;
		m_chatPanel = null;
		m_chatSplit = null;
		m_commentSplit = null;
		m_commentPanel = null;
		m_details = null;
		m_gameMainPanel = null;
		m_stepListener = null;
		m_gameSouthPanel = null;
		m_historyPanel = null;
		m_historyComponent = null;
		m_mapPanel = null;
		m_mapAndChatPanel = null;
		m_message = null;
		m_status = null;
		m_rightHandSidePanel = null;
		if (m_gameCenterPanel != null)
			m_gameCenterPanel.removeAll();
		m_gameCenterPanel = null;
		m_smallView = null;
		m_statsPanel = null;
		m_economyPanel = null;
		m_objectivePanel = null;
		m_notesPanel = null;
		m_step = null;
		m_round = null;
		m_player = null;
		m_tabsPanel = null;
		m_showGameAction = null;
		m_showHistoryAction = null;
		m_showMapOnlyAction = null;
		m_showCommentLogAction = null;
		m_localPlayers = null;
		m_editPanel = null;
		m_messageAndDialogThreadPool = null;
		removeWindowListener(WINDOW_LISTENER);
		WINDOW_LISTENER = null;
		// clear out dynamix's properties
		// Dynamix_AI.clearCachedGameDataAll(); TODO: errors cus dynamix sucks
		ProAI.clearCache();
	}
	
	@Override
	public void shutdown()
	{
		final int rVal = EventThreadJOptionPane.showConfirmDialog(this, "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION, getUIContext()
					.getCountDownLatchHandler());
		if (rVal != JOptionPane.OK_OPTION)
			return;
		stopGame();
		System.exit(0);
	}
	
	@Override
	public void leaveGame()
	{
		final int rVal = EventThreadJOptionPane.showConfirmDialog(this, "Are you sure you want to leave?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION, getUIContext()
					.getCountDownLatchHandler());
		if (rVal != JOptionPane.OK_OPTION)
			return;
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
	
	private WindowListener WINDOW_LISTENER = new WindowAdapter()
	{
		@Override
		public void windowClosing(final WindowEvent e)
		{
			leaveGame();
		}
	};
	
	private final MouseOverUnitListener MOUSE_OVER_UNIT_LISTENER = new MouseOverUnitListener()
	{
		public void mouseEnter(final List<Unit> units, final Territory territory, final MouseDetails me)
		{
			m_unitsBeingMousedOver = units;
		}
	};
	
	public MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
	{
		@Override
		public void mouseEntered(final Territory territory)
		{
			m_territoryLastEntered = territory;
			refresh();
		}
		
		void refresh()
		{
			final StringBuilder buf = new StringBuilder(" ");
			buf.append(m_territoryLastEntered == null ? "none" : m_territoryLastEntered.getName());
			if (m_territoryLastEntered != null)
			{
				final TerritoryAttachment ta = TerritoryAttachment.get(m_territoryLastEntered);
				if (ta != null)
				{
					final Iterator<TerritoryEffect> iter = ta.getTerritoryEffect().iterator();
					if (iter.hasNext())
					{
						buf.append(" (");
					}
					while (iter.hasNext())
					{
						buf.append(iter.next().getName());
						if (iter.hasNext())
							buf.append(", ");
						else
							buf.append(")");
					}
					final int production = ta.getProduction();
					final int unitProduction = ta.getUnitProduction();
					final ResourceCollection resource = ta.getResources();
					if (unitProduction > 0 && unitProduction != production)
						buf.append(", UnitProd: " + unitProduction);
					if (production > 0 || (resource != null && resource.toString().length() > 0))
					{
						buf.append(", Prod: ");
						if (production > 0)
						{
							buf.append(production + " PUs");
							if (resource != null && resource.toString().length() > 0)
								buf.append(", ");
						}
						if (resource != null)
							buf.append(resource.toString());
					}
				}
			}
			m_message.setText(buf.toString());
		}
	};
	
	public void clearStatusMessage()
	{
		if (m_status == null)
			return;
		m_status.setText("");
		m_status.setIcon(null);
	}
	
	public void setStatusErrorMessage(final String msg)
	{
		if (m_status == null)
			return;
		m_status.setText(msg);
		if (!msg.equals(""))
			m_status.setIcon(new ImageIcon(m_mapPanel.getErrorImage()));
		else
			m_status.setIcon(null);
	}
	
	public void setStatusWarningMessage(final String msg)
	{
		if (m_status == null)
			return;
		m_status.setText(msg);
		if (!msg.equals(""))
			m_status.setIcon(new ImageIcon(m_mapPanel.getWarningImage()));
		else
			m_status.setIcon(null);
	}
	
	public void setStatusInfoMessage(final String msg)
	{
		if (m_status == null)
			return;
		m_status.setText(msg);
		if (!msg.equals(""))
			m_status.setIcon(new ImageIcon(m_mapPanel.getInfoImage()));
		else
			m_status.setIcon(null);
	}
	
	public IntegerMap<ProductionRule> getProduction(final PlayerID player, final boolean bid)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToProduce(player);
		return m_actionButtons.waitForPurchase(bid);
	}
	
	public HashMap<Unit, IntegerMap<RepairRule>> getRepair(final PlayerID player, final boolean bid, final Collection<PlayerID> allowedPlayersToRepair)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToRepair(player);
		return m_actionButtons.waitForRepair(bid, allowedPlayersToRepair);
	}
	
	public MoveDescription getMove(final PlayerID player, final IPlayerBridge bridge, final boolean nonCombat, final String stepName)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToMove(player, nonCombat, stepName);
		// workaround for panel not receiving focus at beginning of n/c move phase
		if (!getBattlePanel().getBattleFrame().isVisible())
		{
			if (!SwingUtilities.isEventDispatchThread())
			{
				try
				{
					SwingUtilities.invokeAndWait(new Runnable()
					{
						public void run()
						{
							requestFocusInWindow();
							transferFocus();
						}
					});
				} catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				requestFocusInWindow();
				transferFocus();
			}
		}
		return m_actionButtons.waitForMove(bridge);
	}
	
	public PlaceData waitForPlace(final PlayerID player, final boolean bid, final IPlayerBridge bridge)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToPlace(player);
		return m_actionButtons.waitForPlace(bid, bridge);
	}
	
	public void waitForMoveForumPoster(final PlayerID player, final IPlayerBridge bridge)
	{
		// m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToMoveForumPosterPanel(player);
		m_actionButtons.waitForMoveForumPosterPanel(this, bridge);
	}
	
	public void waitForEndTurn(final PlayerID player, final IPlayerBridge bridge)
	{
		// m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToEndTurn(player);
		m_actionButtons.waitForEndTurn(this, bridge);
	}
	
	public FightBattleDetails getBattle(final PlayerID player, final Map<BattleType, Collection<Territory>> battles)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToBattle(player, battles);
		return m_actionButtons.waitForBattleSelection();
	}
	
	/**
	 * We do NOT want to block the next player from beginning their turn.
	 */
	@Override
	public void notifyError(final String message)
	{
		final String displayMessage = LocalizeHTML.localizeImgLinksInHTML(message);
		m_messageAndDialogThreadPool.runTask(new Runnable()
		{
			public void run()
			{
				EventThreadJOptionPane.showMessageDialog(TripleAFrame.this, displayMessage, "Error", JOptionPane.ERROR_MESSAGE, true, getUIContext().getCountDownLatchHandler());
			}
		});
	}
	
	/**
	 * We do NOT want to block the next player from beginning their turn.
	 * 
	 * @param message
	 * @param title
	 */
	public void notifyMessage(final String message, final String title)
	{
		if (message == null || title == null)
			return;
		if (title.indexOf(AbstractConditionsAttachment.TRIGGER_CHANCE_FAILURE) != -1 && message.indexOf(AbstractConditionsAttachment.TRIGGER_CHANCE_FAILURE) != -1
					&& !getUIContext().getShowTriggerChanceFailure())
			return;
		if (title.indexOf(AbstractConditionsAttachment.TRIGGER_CHANCE_SUCCESSFUL) != -1 && message.indexOf(AbstractConditionsAttachment.TRIGGER_CHANCE_SUCCESSFUL) != -1
					&& !getUIContext().getShowTriggerChanceSuccessful())
			return;
		if (title.equals(AbstractTriggerAttachment.NOTIFICATION) && !getUIContext().getShowTriggeredNotifications())
			return;
		if (title.indexOf(AbstractEndTurnDelegate.END_TURN_REPORT_STRING) != -1 && message.indexOf(AbstractEndTurnDelegate.END_TURN_REPORT_STRING) != -1 && !getUIContext().getShowEndOfTurnReport())
			return;
		final String displayMessage = LocalizeHTML.localizeImgLinksInHTML(message);
		m_messageAndDialogThreadPool.runTask(new Runnable()
		{
			public void run()
			{
				EventThreadJOptionPane.showMessageDialog(TripleAFrame.this, displayMessage, title, JOptionPane.INFORMATION_MESSAGE, true, getUIContext().getCountDownLatchHandler());
			}
		});
	}
	
	public boolean getOKToLetAirDie(final PlayerID m_id, final Collection<Territory> airCantLand, final boolean movePhase)
	{
		if (airCantLand == null || airCantLand.isEmpty())
			return true;
		m_messageAndDialogThreadPool.waitForAll();
		final StringBuilder buf = new StringBuilder("Air in following territories cant land: ");
		for (final Territory t : airCantLand)
		{
			buf.append(t.getName());
			buf.append(" ");
		}
		final boolean lhtrProd = AirThatCantLandUtil.isLHTRCarrierProduction(m_data) || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(m_data);
		int carrierCount = 0;
		for (final PlayerID p : GameStepPropertiesHelper.getCombinedTurns(m_data, m_id))
		{
			carrierCount += p.getUnits().getMatches(Matches.UnitIsCarrier).size();
		}
		final boolean canProduceCarriersUnderFighter = lhtrProd && carrierCount != 0;
		if (canProduceCarriersUnderFighter && carrierCount > 0)
		{
			buf.append("\nYou have " + carrierCount + " " + MyFormatter.pluralize("carrier", carrierCount) + " on which planes can land");
		}
		final String ok = movePhase ? "End Move Phase" : "Kill Planes";
		final String cancel = movePhase ? "Keep Moving" : "Change Placement";
		final String[] options = { cancel, ok };
		this.m_mapPanel.centerOn(airCantLand.iterator().next());
		final int choice = EventThreadJOptionPane.showOptionDialog(this, buf.toString(), "Air cannot land", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, cancel,
					getUIContext().getCountDownLatchHandler());
		return choice == 1;
	}
	
	public boolean getOKToLetUnitsDie(final PlayerID m_id, final Collection<Territory> unitsCantFight, final boolean movePhase)
	{
		if (unitsCantFight == null || unitsCantFight.isEmpty())
			return true;
		m_messageAndDialogThreadPool.waitForAll();
		final StringBuilder buf = new StringBuilder("Units in the following territories will die: ");
		final Iterator<Territory> iter = unitsCantFight.iterator();
		while (iter.hasNext())
		{
			buf.append((iter.next()).getName());
			buf.append(" ");
		}
		final String ok = movePhase ? "Done Moving" : "Kill Units";
		final String cancel = movePhase ? "Keep Moving" : "Change Placement";
		final String[] options = { cancel, ok };
		this.m_mapPanel.centerOn(unitsCantFight.iterator().next());
		final int choice = EventThreadJOptionPane.showOptionDialog(this, buf.toString(), "Units cannot fight", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, options, cancel,
					getUIContext().getCountDownLatchHandler());
		return choice == 1;
	}
	
	public boolean acceptAction(final PlayerID playerSendingProposal, final String acceptanceQuestion, final boolean politics)
	{
		m_messageAndDialogThreadPool.waitForAll();
		final int choice = EventThreadJOptionPane.showConfirmDialog(this, acceptanceQuestion, "Accept " + (politics ? "Political " : "") + "Proposal from " + playerSendingProposal.getName() + "?",
					JOptionPane.YES_NO_OPTION, getUIContext().getCountDownLatchHandler());
		return choice == JOptionPane.YES_OPTION;
	}
	
	public boolean getOK(final String message)
	{
		m_messageAndDialogThreadPool.waitForAll();
		final int choice = EventThreadJOptionPane.showConfirmDialog(this, message, message, JOptionPane.OK_CANCEL_OPTION, getUIContext().getCountDownLatchHandler());
		return choice == JOptionPane.OK_OPTION;
	}
	
	public void notifyTechResults(final TechResults msg)
	{
		m_messageAndDialogThreadPool.runTask(new Runnable()
		{
			public void run()
			{
				final AtomicReference<TechResultsDisplay> displayRef = new AtomicReference<TechResultsDisplay>();
				try
				{
					SwingUtilities.invokeAndWait(new Runnable()
					{
						public void run()
						{
							final TechResultsDisplay display = new TechResultsDisplay(msg, m_uiContext, m_data);
							displayRef.set(display);
						}
					});
				} catch (final InterruptedException e)
				{
					throw new IllegalStateException();
				} catch (final InvocationTargetException e)
				{
					throw new IllegalStateException();
				}
				EventThreadJOptionPane.showOptionDialog(TripleAFrame.this, displayRef.get(), "Tech roll", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] { "OK" }, "OK",
							getUIContext().getCountDownLatchHandler());
			}
		});
	}
	
	public boolean getStrategicBombingRaid(final Territory location)
	{
		m_messageAndDialogThreadPool.waitForAll();
		final String message = (games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(m_data) ? "Bomb/Escort" : "Bomb") + " in " + location.getName();
		final String bomb = (games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(m_data) ? "Bomb/Escort" : "Bomb");
		final String normal = "Attack";
		final String[] choices = { bomb, normal };
		int choice = -1;
		while (choice < 0 || choice > 1)
		{
			choice = EventThreadJOptionPane.showOptionDialog(this, message, "Bomb?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE, null, choices, bomb, getUIContext()
						.getCountDownLatchHandler());
		}
		return choice == JOptionPane.OK_OPTION;
	}
	
	public Unit getStrategicBombingRaidTarget(final Territory territory, final Collection<Unit> potentialTargets, final Collection<Unit> bombers)
	{
		if (potentialTargets == null || potentialTargets.size() == 0)
			return null;
		if (potentialTargets.size() == 1)
			return potentialTargets.iterator().next();
		m_messageAndDialogThreadPool.waitForAll();
		final AtomicReference<Unit> selected = new AtomicReference<Unit>();
		final String message = "Select bombing target in " + territory.getName();
		final Tuple<JPanel, JList> comps = Util.runInSwingEventThread(new Util.Task<Tuple<JPanel, JList>>()
		{
			public Tuple<JPanel, JList> run()
			{
				final JList list = new JList(new Vector<Unit>(potentialTargets));
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				list.setSelectedIndex(0);
				final JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				if (bombers != null)
					panel.add(new JLabel("For Units: " + MyFormatter.unitsToTextNoOwner(bombers)), BorderLayout.NORTH);
				final JScrollPane scroll = new JScrollPane(list);
				panel.add(scroll, BorderLayout.CENTER);
				return new Tuple<JPanel, JList>(panel, list);
			}
		});
		final JPanel panel = comps.getFirst();
		final JList list = comps.getSecond();
		final String[] options = { "OK", "Cancel" };
		final int selection = EventThreadJOptionPane.showOptionDialog(this, panel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null, getUIContext()
					.getCountDownLatchHandler());
		if (selection == 0) // OK
			selected.set((Unit) list.getSelectedValue());
		// Unit selected = (Unit) list.getSelectedValue();
		return selected.get();
	}
	
	public int[] selectFixedDice(final int numDice, final int hitAt, final boolean hitOnlyIfEquals, final String title, final int diceSides)
	{
		m_messageAndDialogThreadPool.waitForAll();
		final DiceChooser chooser = Util.runInSwingEventThread(new Util.Task<DiceChooser>()
		{
			public DiceChooser run()
			{
				return new DiceChooser(getUIContext(), numDice, hitAt, hitOnlyIfEquals, diceSides, m_data);
			}
		});
		do
		{
			EventThreadJOptionPane.showMessageDialog(null, chooser, title, JOptionPane.PLAIN_MESSAGE, getUIContext().getCountDownLatchHandler());
		} while (chooser.getDice() == null);
		return chooser.getDice();
	}
	
	public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory, final String unitMessage)
	{
		m_messageAndDialogThreadPool.waitForAll();
		final Tuple<JPanel, JList> comps = Util.runInSwingEventThread(new Util.Task<Tuple<JPanel, JList>>()
		{
			public Tuple<JPanel, JList> run()
			{
				m_mapPanel.centerOn(currentTerritory);
				final JList list = new JList(new Vector<Territory>(candidates));
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				list.setSelectedIndex(0);
				final JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				final JScrollPane scroll = new JScrollPane(list);
				final JTextArea text = new JTextArea(unitMessage, 8, 30);
				text.setLineWrap(true);
				text.setEditable(false);
				text.setWrapStyleWord(true);
				panel.add(text, BorderLayout.NORTH);
				panel.add(scroll, BorderLayout.CENTER);
				return new Tuple<JPanel, JList>(panel, list);
			}
		});
		final JPanel panel = comps.getFirst();
		final JList list = comps.getSecond();
		final String[] options = { "OK" };
		final String title = "Select territory for air units to land, current territory is " + currentTerritory.getName();
		EventThreadJOptionPane.showOptionDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null, getUIContext().getCountDownLatchHandler());
		final Territory selected = (Territory) list.getSelectedValue();
		return selected;
	}
	
	public Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(final PlayerID player, final List<Territory> territoryChoices, final List<Unit> unitChoices, final int unitsPerPick)
	{
		// total hacks
		m_messageAndDialogThreadPool.waitForAll();
		{
			final CountDownLatch latch1 = new CountDownLatch(1);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (!m_inGame)
						showGame();
					if (m_tabsPanel.indexOfTab("Actions") == -1)
						m_tabsPanel.insertTab("Actions", null, m_actionButtons, null, 0); // add actions tab
					m_tabsPanel.setSelectedIndex(0);
					latch1.countDown();
				}
			});
			try
			{
				latch1.await();
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		m_actionButtons.changeToPickTerritoryAndUnits(player);
		final Tuple<Territory, Set<Unit>> rVal = m_actionButtons.waitForPickTerritoryAndUnits(territoryChoices, unitChoices, unitsPerPick);
		final int index = m_tabsPanel == null ? -1 : m_tabsPanel.indexOfTab("Actions");
		if (index != -1 && m_inHistory)
		{
			final CountDownLatch latch2 = new CountDownLatch(1);
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (m_tabsPanel != null)
						m_tabsPanel.remove(index); // remove actions tab
					latch2.countDown();
				}
			});
			try
			{
				latch2.await();
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		if (m_actionButtons != null && m_actionButtons.getCurrent() != null)
			m_actionButtons.getCurrent().setActive(false);
		return rVal;
	}
	
	public HashMap<Territory, IntegerMap<Unit>> selectKamikazeSuicideAttacks(final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack, final Resource attackResourceToken,
				final int maxNumberOfAttacksAllowed)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Should not be called from dispatch thread");
		}
		final HashMap<Territory, IntegerMap<Unit>> selection = new HashMap<Territory, IntegerMap<Unit>>();
		if (possibleUnitsToAttack == null || possibleUnitsToAttack.isEmpty() || attackResourceToken == null || maxNumberOfAttacksAllowed <= 0)
			return selection;
		m_messageAndDialogThreadPool.waitForAll();
		final CountDownLatch continueLatch = new CountDownLatch(1);
		final Collection<IndividualUnitPanelGrouped> unitPanels = new ArrayList<IndividualUnitPanelGrouped>();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				final HashMap<String, Collection<Unit>> possibleUnitsToAttackStringForm = new HashMap<String, Collection<Unit>>();
				for (final Entry<Territory, Collection<Unit>> entry : possibleUnitsToAttack.entrySet())
				{
					final List<Unit> units = new ArrayList<Unit>(entry.getValue());
					Collections.sort(units, new UnitBattleComparator(false, BattleCalculator.getCostsForTuvForAllPlayersMergedAndAveraged(m_data), TerritoryEffectHelper.getEffects(entry.getKey()),
								m_data, false));
					Collections.reverse(units);
					possibleUnitsToAttackStringForm.put(entry.getKey().getName(), units);
				}
				m_mapPanel.centerOn(m_data.getMap().getTerritory(possibleUnitsToAttackStringForm.keySet().iterator().next()));
				
				final IndividualUnitPanelGrouped unitPanel = new IndividualUnitPanelGrouped(possibleUnitsToAttackStringForm, m_data, m_uiContext, "Select Units to Suicide Attack using "
							+ attackResourceToken.getName(), maxNumberOfAttacksAllowed, true, false);
				unitPanels.add(unitPanel);
				final String optionAttack = "Attack";
				final String optionNone = "None";
				final String optionWait = "Wait";
				final Object[] options = { optionAttack, optionNone, optionWait };
				final JOptionPane optionPane = new JOptionPane(unitPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[2]);
				final JDialog dialog = new JDialog((Frame) getParent(), "Select units to Suicide Attack using " + attackResourceToken.getName());
				dialog.setContentPane(optionPane);
				dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				dialog.setLocationRelativeTo(getParent());
				dialog.setAlwaysOnTop(true);
				dialog.pack();
				dialog.setVisible(true);
				dialog.requestFocusInWindow();
				// final int option = JOptionPane.showOptionDialog(getParent(), unitPanel, "Select units to Suicide Attack using " + attackResourceToken.getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
				optionPane.addPropertyChangeListener(new PropertyChangeListener()
				{
					public void propertyChange(final PropertyChangeEvent e)
					{
						if (!dialog.isVisible())
							return;
						final String option = ((String) optionPane.getValue());
						if (option.equals(optionNone))
						{
							unitPanels.clear();
							selection.clear();
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							continueLatch.countDown();
							return;
						}
						else if (option.equals(optionAttack))
						{
							if (unitPanels.size() != 1)
								throw new IllegalStateException("unitPanels should only contain 1 entry");
							for (final IndividualUnitPanelGrouped terrChooser : unitPanels)
							{
								for (final Entry<String, IntegerMap<Unit>> entry : terrChooser.getSelected().entrySet())
								{
									selection.put(m_data.getMap().getTerritory(entry.getKey()), entry.getValue());
								}
							}
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							continueLatch.countDown();
						}
						else
						// if (option.equals(optionWait))
						{
							unitPanels.clear();
							selection.clear();
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							try
							{
								Thread.sleep(500);
							} catch (final InterruptedException e2)
							{
								e2.printStackTrace();
							}
							run();
						}
					}
				});
			}
		});
		m_mapPanel.getUIContext().addShutdownLatch(continueLatch);
		try
		{
			continueLatch.await();
		} catch (final InterruptedException ex)
		{
			ex.printStackTrace();
		} finally
		{
			m_mapPanel.getUIContext().removeShutdownLatch(continueLatch);
		}
		return selection;
	}
	
	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		m_messageAndDialogThreadPool.waitForAll();
		if (SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Should not be called from dispatch thread");
		}
		final CountDownLatch continueLatch = new CountDownLatch(1);
		final HashMap<Territory, Collection<Unit>> selection = new HashMap<Territory, Collection<Unit>>();
		final Collection<Tuple<Territory, UnitChooser>> choosers = new ArrayList<Tuple<Territory, UnitChooser>>();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_mapPanel.centerOn(scrambleTo);
				final JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				final JLabel whereTo = new JLabel("Scramble To: " + scrambleTo.getName());
				whereTo.setFont(new Font("Arial", Font.ITALIC, 12));
				panel.add(whereTo, BorderLayout.NORTH);
				final JPanel panel2 = new JPanel();
				panel2.setBorder(BorderFactory.createEmptyBorder());
				panel2.setLayout(new FlowLayout());
				for (final Territory from : possibleScramblers.keySet())
				{
					JScrollPane chooserScrollPane;
					final JPanel panelChooser = new JPanel();
					panelChooser.setLayout(new BoxLayout(panelChooser, BoxLayout.Y_AXIS));
					panelChooser.setBorder(BorderFactory.createLineBorder(getBackground()));
					final JLabel whereFrom = new JLabel("From: " + from.getName());
					whereFrom.setHorizontalAlignment(SwingConstants.LEFT);
					whereFrom.setFont(new Font("Arial", Font.BOLD, 12));
					panelChooser.add(whereFrom);
					panelChooser.add(new JLabel(" "));
					final Collection<Unit> possible = possibleScramblers.get(from).getSecond();
					final int maxAllowed = Math.min(BattleDelegate.getMaxScrambleCount(possibleScramblers.get(from).getFirst()), possible.size());
					final UnitChooser chooser = new UnitChooser(possible, Collections.<Unit, Collection<Unit>> emptyMap(), m_data, false, m_uiContext);
					chooser.setMaxAndShowMaxButton(maxAllowed);
					choosers.add(new Tuple<Territory, UnitChooser>(from, chooser));
					panelChooser.add(chooser);
					chooserScrollPane = new JScrollPane(panelChooser);
					panel2.add(chooserScrollPane);
				}
				panel.add(panel2, BorderLayout.CENTER);
				final String optionScramble = "Scramble";
				final String optionNone = "None";
				final String optionWait = "Wait";
				final Object[] options = { optionScramble, optionNone, optionWait };
				final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[2]);
				final JDialog dialog = new JDialog((Frame) getParent(), "Select units to scramble to " + scrambleTo.getName());
				dialog.setContentPane(optionPane);
				dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				dialog.setLocationRelativeTo(getParent());
				dialog.setAlwaysOnTop(true);
				dialog.pack();
				dialog.setVisible(true);
				dialog.requestFocusInWindow();
				// final int option = JOptionPane.showOptionDialog(getParent(), panel, "Select units to scramble to " + scrambleTo.getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
				optionPane.addPropertyChangeListener(new PropertyChangeListener()
				{
					public void propertyChange(final PropertyChangeEvent e)
					{
						if (!dialog.isVisible())
							return;
						final String option = ((String) optionPane.getValue());
						if (option.equals(optionNone))
						{
							choosers.clear();
							selection.clear();
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							continueLatch.countDown();
							return;
						}
						else if (option.equals(optionScramble))
						{
							for (final Tuple<Territory, UnitChooser> terrChooser : choosers)
							{
								selection.put(terrChooser.getFirst(), terrChooser.getSecond().getSelected());
							}
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							continueLatch.countDown();
						}
						else
						// if (option.equals(optionWait))
						{
							choosers.clear();
							selection.clear();
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							try
							{
								Thread.sleep(500);
							} catch (final InterruptedException e2)
							{
								e2.printStackTrace();
							}
							run();
						}
					}
				});
			}
		});
		m_mapPanel.getUIContext().addShutdownLatch(continueLatch);
		try
		{
			continueLatch.await();
		} catch (final InterruptedException ex)
		{
			ex.printStackTrace();
		} finally
		{
			m_mapPanel.getUIContext().removeShutdownLatch(continueLatch);
		}
		return selection;
	}
	
	public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message)
	{
		m_messageAndDialogThreadPool.waitForAll();
		if (SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Should not be called from dispatch thread");
		}
		final CountDownLatch continueLatch = new CountDownLatch(1);
		final Collection<Unit> selection = new ArrayList<Unit>();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_mapPanel.centerOn(current);
				final JPanel panel = new JPanel();
				panel.setLayout(new BorderLayout());
				final JLabel messageLabel = new JLabel(message);
				messageLabel.setFont(new Font("Arial", Font.ITALIC, 12));
				panel.add(messageLabel, BorderLayout.NORTH);
				JScrollPane chooserScrollPane;
				final JPanel panelChooser = new JPanel();
				panelChooser.setLayout(new BoxLayout(panelChooser, BoxLayout.Y_AXIS));
				panelChooser.setBorder(BorderFactory.createLineBorder(getBackground()));
				final JLabel whereFrom = new JLabel("From: " + current.getName());
				whereFrom.setHorizontalAlignment(SwingConstants.LEFT);
				whereFrom.setFont(new Font("Arial", Font.BOLD, 12));
				panelChooser.add(whereFrom);
				panelChooser.add(new JLabel(" "));
				final int maxAllowed = possible.size();
				final UnitChooser chooser = new UnitChooser(possible, Collections.<Unit, Collection<Unit>> emptyMap(), m_data, false, m_uiContext);
				chooser.setMaxAndShowMaxButton(maxAllowed);
				panelChooser.add(chooser);
				chooserScrollPane = new JScrollPane(panelChooser);
				panel.add(chooserScrollPane, BorderLayout.CENTER);
				final String optionSelect = "Select";
				final String optionNone = "None";
				final String optionWait = "Wait";
				final Object[] options = { optionSelect, optionNone, optionWait };
				final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[2]);
				final JDialog dialog = new JDialog((Frame) getParent(), message);
				dialog.setContentPane(optionPane);
				dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
				dialog.setLocationRelativeTo(getParent());
				dialog.setAlwaysOnTop(true);
				dialog.pack();
				dialog.setVisible(true);
				dialog.requestFocusInWindow();
				// final int option = JOptionPane.showOptionDialog(getParent(), panel, message, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
				optionPane.addPropertyChangeListener(new PropertyChangeListener()
				{
					public void propertyChange(final PropertyChangeEvent e)
					{
						if (!dialog.isVisible())
							return;
						final String option = ((String) optionPane.getValue());
						if (option.equals(optionNone))
						{
							selection.clear();
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							continueLatch.countDown();
							return;
						}
						else if (option.equals(optionSelect))
						{
							selection.addAll(chooser.getSelected());
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							continueLatch.countDown();
						}
						else
						// if (option.equals(optionWait))
						{
							selection.clear();
							dialog.setVisible(false);
							dialog.removeAll();
							dialog.dispose();
							try
							{
								Thread.sleep(500);
							} catch (final InterruptedException e2)
							{
								e2.printStackTrace();
							}
							run();
						}
					}
				});
			}
		});
		m_mapPanel.getUIContext().addShutdownLatch(continueLatch);
		try
		{
			continueLatch.await();
		} catch (final InterruptedException ex)
		{
			ex.printStackTrace();
		} finally
		{
			m_mapPanel.getUIContext().removeShutdownLatch(continueLatch);
		}
		return selection;
	}
	
	public PoliticalActionAttachment getPoliticalActionChoice(final PlayerID player, final boolean firstRun, final IPoliticsDelegate iPoliticsDelegate)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToPolitics(player);
		if (!SwingUtilities.isEventDispatchThread())
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						requestFocusInWindow();
						transferFocus();
					}
				});
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			requestFocusInWindow();
			transferFocus();
		}
		return m_actionButtons.waitForPoliticalAction(firstRun, iPoliticsDelegate);
	}
	
	public UserActionAttachment getUserActionChoice(final PlayerID player, final boolean firstRun, final IUserActionDelegate iUserActionDelegate)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToUserActions(player);
		if (!SwingUtilities.isEventDispatchThread())
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						requestFocusInWindow();
						transferFocus();
					}
				});
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			requestFocusInWindow();
			transferFocus();
		}
		return m_actionButtons.waitForUserActionAction(firstRun, iUserActionDelegate);
	}
	
	public TechRoll getTechRolls(final PlayerID id)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_actionButtons.changeToTech(id);
		// workaround for panel not receiving focus at beginning of tech phase
		if (!SwingUtilities.isEventDispatchThread())
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						requestFocusInWindow();
						transferFocus();
					}
				});
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			requestFocusInWindow();
			transferFocus();
		}
		return m_actionButtons.waitForTech();
	}
	
	public Territory getRocketAttack(final Collection<Territory> candidates, final Territory from)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_mapPanel.centerOn(from);
		final AtomicReference<Territory> selected = new AtomicReference<Territory>();
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					final JList list = new JList(new Vector<Territory>(candidates));
					list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					list.setSelectedIndex(0);
					final JPanel panel = new JPanel();
					panel.setLayout(new BorderLayout());
					final JScrollPane scroll = new JScrollPane(list);
					panel.add(scroll, BorderLayout.CENTER);
					if (from != null)
					{
						panel.add(BorderLayout.NORTH, new JLabel("Targets for rocket in " + from.getName()));
					}
					final String[] options = { "OK", "Dont attack" };
					final String message = "Select Rocket Target";
					final int selection = JOptionPane.showOptionDialog(TripleAFrame.this, panel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
					if (selection == 0) // OK
						selected.set((Territory) list.getSelectedValue());
				}
			});
		} catch (final InterruptedException e)
		{
			throw new IllegalStateException(e);
		} catch (final InvocationTargetException e)
		{
			throw new IllegalStateException(e);
		}
		return selected.get();
	}
	
	public static int save(final String filename, final GameData m_data)
	{
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try
		{
			fos = new FileOutputStream(filename);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(m_data);
			return 0;
		} catch (final Throwable t)
		{
			// t.printStackTrace();
			System.err.println(t.getMessage());
			return -1;
		} finally
		{
			try
			{
				if (fos != null)
					fos.flush();
			} catch (final Exception ignore)
			{
			}
			try
			{
				if (oos != null)
					oos.close();
			} catch (final Exception ignore)
			{
			}
		}
	}
	
	GameStepListener m_stepListener = new GameStepListener()
	{
		public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String stepDisplayName)
		{
			updateStep();
		}
	};
	
	private void updateStep()
	{
		final IUIContext context = m_uiContext;
		if (context == null || context.isShutDown())
			return;
		m_data.acquireReadLock();
		try
		{
			if (m_data.getSequence().getStep() == null)
				return;
		} finally
		{
			m_data.releaseReadLock();
		}
		// we need to invoke and wait here since
		// if we switch to the history as a result of a history
		// change, we need to ensure that no further history
		// events are run until our historySynchronizer is set up
		if (!SwingUtilities.isEventDispatchThread())
		{
			try
			{
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						updateStep();
					}
				});
			} catch (final InterruptedException e)
			{
				e.printStackTrace();
			} catch (final InvocationTargetException e)
			{
				e.getCause().printStackTrace();
				throw new IllegalStateException(e.getCause().getMessage());
			}
			return;
		}
		int round;
		String stepDisplayName;
		PlayerID player;
		m_data.acquireReadLock();
		try
		{
			round = m_data.getSequence().getRound();
			stepDisplayName = m_data.getSequence().getStep().getDisplayName();
			player = m_data.getSequence().getStep().getPlayerID();
		} finally
		{
			m_data.releaseReadLock();
		}
		m_round.setText("Round:" + round + " ");
		m_step.setText(stepDisplayName);
		final boolean isPlaying = m_localPlayers.playing(player);
		if (player != null)
			m_player.setText((isPlaying ? "" : "REMOTE: ") + player.getName());
		if (player != null && !player.isNull())
		{
			m_round.setIcon(new ImageIcon(m_uiContext.getFlagImageFactory().getFlag(player)));
			m_lastStepPlayer = m_currentStepPlayer;
			m_currentStepPlayer = player;
		}
		// if the game control has passed to someone else and we are not just showing the map
		// show the history
		if (player != null && !player.isNull())
		{
			if (isPlaying)
			{
				if (m_inHistory)
				{
					m_requiredTurnSeries.put(player, true);
					// if the game control is with us
					// show the current game
					showGame();
					// System.out.println("Changing step to " + stepDisplayName + " for " + player.getName());
				}
			}
			else
			{
				if (!m_inHistory && !m_uiContext.getShowMapOnly())
				{
					if (!SwingUtilities.isEventDispatchThread())
						throw new IllegalStateException("We should be in dispatch thread");
					showHistory();
				}
			}
		}
	}
	
	public void requiredTurnSeries(final PlayerID player)
	{
		if (player == null)
			return;
		try
		{
			try
			{
				Thread.sleep(300);
			} catch (final InterruptedException e1)
			{
			}
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					final Boolean play = m_requiredTurnSeries.get(player);
					// System.out.println("Starting for " + player.getName() + ", with requiredTurnSeries equal to " + (play == null ? "null" : play) + ", with m_lastStepPlayer equal to " + (m_lastStepPlayer == null ? "null" : m_lastStepPlayer.getName()));
					if (play != null && play.booleanValue())
					{
						DefaultSoundChannel.playSoundOnLocalMachine(SoundPath.CLIP_REQUIRED_YOUR_TURN_SERIES, player.getName()); // play sound
						m_requiredTurnSeries.put(player, false);
						// System.out.println("Playing Sound for " + player.getName());
					}
					// center on capital of player, if it is a new player
					if (!player.equals(m_lastStepPlayer))
					{
						m_lastStepPlayer = player;
						m_data.acquireReadLock();
						try
						{
							m_mapPanel.centerOn(TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, m_data));
							// System.out.println("Centering on " + player.getName());
						} finally
						{
							m_data.releaseReadLock();
						}
					}
				}
			});
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		} catch (final InvocationTargetException e)
		{
			e.printStackTrace();
		}
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
						if (m_uiContext == null)
							return;
						if (getEditMode())
						{
							if (m_tabsPanel.indexOfComponent(m_editPanel) == -1)
							{
								showEditMode();
							}
						}
						else
						{
							if (m_tabsPanel.indexOfComponent(m_editPanel) != -1)
							{
								hideEditMode();
							}
						}
						if (m_uiContext.getShowMapOnly())
						{
							hideRightHandSidePanel();
							// display troop movement
							final HistoryNode node = m_data.getHistory().getLastNode();
							if (node instanceof Renderable)
							{
								final Object details = ((Renderable) node).getRenderingData();
								if (details instanceof MoveDescription)
								{
									final MoveDescription moveMessage = (MoveDescription) details;
									final Route route = moveMessage.getRoute();
									m_mapPanel.setRoute(null);
									m_mapPanel.setRoute(route);
									final Territory terr = route.getEnd();
									if (!m_mapPanel.isShowing(terr))
										m_mapPanel.centerOn(terr);
								}
							}
						}
						else
						{
							showRightHandSidePanel();
						}
					}
				});
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
	};
	
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
				getMapPanel().setTopLeft(x + diffPixel, y);
			else if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A)
				getMapPanel().setTopLeft(x - diffPixel, y);
			else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S)
				getMapPanel().setTopLeft(x, y + diffPixel);
			else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W)
				getMapPanel().setTopLeft(x, y - diffPixel);
			
			// I for info
			if (keyCode == KeyEvent.VK_I || keyCode == KeyEvent.VK_V)
			{
				String unitInfo = "";
				if (m_unitsBeingMousedOver != null && !m_unitsBeingMousedOver.isEmpty())
				{
					final Unit unit = m_unitsBeingMousedOver.get(0);
					final UnitAttachment ua = UnitAttachment.get(unit.getType());
					if (ua != null)
						unitInfo = "<b>Unit:</b><br>" + unit.getType().getName() + ": " + ua.toStringShortAndOnlyImportantDifferences(unit.getOwner(), true, false);
				}
				String terrInfo = "";
				if (m_territoryLastEntered != null)
				{
					final TerritoryAttachment ta = TerritoryAttachment.get(m_territoryLastEntered);
					if (ta != null)
						terrInfo = "<b>Territory:</b><br>" + ta.toStringForInfo(true, true) + "<br>";
					else
						terrInfo = "<b>Territory:</b><br>" + m_territoryLastEntered.getName() + "<br>Water Territory";
				}
				String tipText = unitInfo;
				if (unitInfo.length() > 0 && terrInfo.length() > 0)
					tipText = tipText + "<br><br><br><br><br>";
				tipText = tipText + terrInfo;
				if (tipText.length() > 0)
				{
					final Point currentPoint = MouseInfo.getPointerInfo().getLocation();
					final PopupFactory popupFactory = PopupFactory.getSharedInstance();
					final JToolTip info = new JToolTip();
					info.setTipText("<html>" + tipText + "</html>");
					final Popup popup = popupFactory.getPopup(m_mapPanel, info, currentPoint.x, currentPoint.y);
					popup.show();
					final Runnable disposePopup = new Runnable()
					{
						public void run()
						{
							try
							{
								Thread.sleep(5000);
							} catch (final InterruptedException e)
							{
							}
							popup.hide();
						}
					};
					new Thread(disposePopup, "popup waiter").start();
				}
			}
			
			// and then we do stuff for any custom current action tab
			m_actionButtons.keyPressed(e);
		}
		
		public void keyTyped(final KeyEvent e)
		{
		}
		
		public void keyReleased(final KeyEvent e)
		{
		}
	};
	
	private void showEditMode()
	{
		m_tabsPanel.addTab("Edit", m_editPanel);
		if (m_editDelegate != null)
			m_tabsPanel.setSelectedComponent(m_editPanel);
		m_editModeButtonModel.setSelected(true);
		getGlassPane().setVisible(true);
	}
	
	private void hideEditMode()
	{
		if (m_tabsPanel.getSelectedComponent() == m_editPanel)
			m_tabsPanel.setSelectedIndex(0);
		m_tabsPanel.remove(m_editPanel);
		m_editModeButtonModel.setSelected(false);
		getGlassPane().setVisible(false);
	}
	
	public void showActionPanelTab()
	{
		m_tabsPanel.setSelectedIndex(0);
	}
	
	public void showRightHandSidePanel()
	{
		m_rightHandSidePanel.setVisible(true);
	}
	
	public void hideRightHandSidePanel()
	{
		m_rightHandSidePanel.setVisible(false);
	}
	
	public HistoryPanel getHistoryPanel()
	{
		return m_historyPanel;
	}
	
	private void showHistory()
	{
		m_inHistory = true;
		m_inGame = false;
		setWidgetActivation();
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
		m_statsPanel.setGameData(clonedGameData);
		m_economyPanel.setGameData(clonedGameData);
		if (m_objectivePanel != null && !m_objectivePanel.isEmpty())
			m_objectivePanel.setGameData(clonedGameData);
		m_details.setGameData(clonedGameData);
		m_mapPanel.setGameData(clonedGameData);
		final HistoryDetailsPanel historyDetailPanel = new HistoryDetailsPanel(clonedGameData, m_mapPanel);
		m_tabsPanel.removeAll();
		m_tabsPanel.add("History", historyDetailPanel);
		m_tabsPanel.add("Stats", m_statsPanel);
		m_tabsPanel.add("Economy", m_economyPanel);
		if (m_objectivePanel != null && !m_objectivePanel.isEmpty())
			m_tabsPanel.add(m_objectivePanel.getName(), m_objectivePanel);
		m_tabsPanel.add("Notes", m_notesPanel);
		m_tabsPanel.add("Territory", m_details);
		if (getEditMode())
			m_tabsPanel.add("Edit", m_editPanel);
		if (m_actionButtons.getCurrent() != null)
			m_actionButtons.getCurrent().setActive(false);
		m_historyComponent.removeAll();
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
				historyLog.printTerritorySummary(m_historyPanel.getCurrentPopupNode(), clonedGameData);
				historyLog.printProductionSummary(clonedGameData);
				m_historyPanel.clearCurrentPopupNode();
				historyLog.setVisible(true);
			}
		});
		popup.add(new AbstractAction("Show Detailed Log")
		{
			private static final long serialVersionUID = -8709762764495294671L;
			
			public void actionPerformed(final ActionEvent ae)
			{
				final HistoryLog historyLog = new HistoryLog();
				historyLog.printRemainingTurn(m_historyPanel.getCurrentPopupNode(), true, m_data.getDiceSides(), null);
				historyLog.printTerritorySummary(m_historyPanel.getCurrentPopupNode(), clonedGameData);
				historyLog.printProductionSummary(clonedGameData);
				m_historyPanel.clearCurrentPopupNode();
				historyLog.setVisible(true);
			}
		});
		popup.add(new AbstractAction("Save Screenshot")
		{
			private static final long serialVersionUID = 1222760138263428443L;
			
			public void actionPerformed(final ActionEvent ae)
			{
				saveScreenshot(m_historyPanel.getCurrentPopupNode());
				m_historyPanel.clearCurrentPopupNode();
			}
		});
		popup.add(new AbstractAction("Save Game at this point (BETA)")
		{
			private static final long serialVersionUID = 1430512376199927896L;
			
			public void actionPerformed(final ActionEvent ae)
			{
				JOptionPane.showMessageDialog(TripleAFrame.this, "Please first left click on the spot you want to save from, Then right click and select 'Save Game From History'"
							+ "\n\nIt is recommended that when saving the game from the History panel:"
							+ "\n * Your CURRENT GAME is at the start of some player's turn, and that no moves have been made and no actions taken yet."
							+ "\n * The point in HISTORY that you are trying to save at, is at the beginning of a player's turn, or the beginning of a round."
							+ "\nSaving at any other point, could potentially create errors."
							+ "\nFor example, saving while your current game is in the middle of a move or battle phase will always create errors in the savegame."
							+ "\nAnd you will also get errors in the savegame if you try to create a save at a point in history such as a move or battle phase.",
							"Save Game from History", JOptionPane.INFORMATION_MESSAGE);
				m_data.acquireReadLock();
				// m_data.acquireWriteLock();
				try
				{
					final File f = BasicGameMenuBar.getSaveGameLocationDialog(TripleAFrame.this);
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
									round = Math.max(0, ((Round) node).getRoundNo() - datacopy.getSequence().getRoundOffset());
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
							JOptionPane.showMessageDialog(TripleAFrame.this, "Game Saved", "Game Saved", JOptionPane.INFORMATION_MESSAGE);
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
		m_historyPanel = new HistoryPanel(clonedGameData, historyDetailPanel, popup, m_uiContext);
		split.setLeftComponent(m_historyPanel);
		split.setRightComponent(m_gameCenterPanel);
		split.setDividerLocation(150);
		m_historyComponent.add(split, BorderLayout.CENTER);
		m_historyComponent.add(m_gameSouthPanel, BorderLayout.SOUTH);
		getContentPane().removeAll();
		getContentPane().add(m_historyComponent, BorderLayout.CENTER);
		validate();
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public void show()
	{
		super.show();
	}
	
	public void showGame()
	{
		m_inGame = true;
		m_uiContext.setShowMapOnly(false);
		// Are we coming from showHistory mode or showMapOnly mode?
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
			m_statsPanel.setGameData(m_data);
			m_economyPanel.setGameData(m_data);
			if (m_objectivePanel != null && !m_objectivePanel.isEmpty())
				m_objectivePanel.setGameData(m_data);
			m_details.setGameData(m_data);
			m_mapPanel.setGameData(m_data);
			m_data.addDataChangeListener(m_dataChangeListener);
			m_tabsPanel.removeAll();
		}
		setWidgetActivation();
		m_tabsPanel.add("Action", m_actionButtons);
		m_tabsPanel.add("Stats", m_statsPanel);
		m_tabsPanel.add("Economy", m_economyPanel);
		if (m_objectivePanel != null && !m_objectivePanel.isEmpty())
			m_tabsPanel.add(m_objectivePanel.getName(), m_objectivePanel);
		m_tabsPanel.add("Notes", m_notesPanel);
		m_tabsPanel.add("Territory", m_details);
		if (getEditMode())
			m_tabsPanel.add("Edit", m_editPanel);
		if (m_actionButtons.getCurrent() != null)
			m_actionButtons.getCurrent().setActive(true);
		m_gameMainPanel.removeAll();
		m_gameMainPanel.setLayout(new BorderLayout());
		m_gameMainPanel.add(m_gameCenterPanel, BorderLayout.CENTER);
		m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
		getContentPane().removeAll();
		getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);
		m_mapPanel.setRoute(null);
		validate();
	}
	
	public void showMapOnly()
	{
		// Are we coming from showHistory mode or showGame mode?
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
			m_gameMainPanel.removeAll();
			m_gameMainPanel.setLayout(new BorderLayout());
			m_gameMainPanel.add(m_mapAndChatPanel, BorderLayout.CENTER);
			m_gameMainPanel.add(m_rightHandSidePanel, BorderLayout.EAST);
			m_gameMainPanel.add(m_gameSouthPanel, BorderLayout.SOUTH);
			getContentPane().removeAll();
			getContentPane().add(m_gameMainPanel, BorderLayout.CENTER);
			m_mapPanel.setRoute(null);
		}
		else
		{
			m_inGame = false;
		}
		m_uiContext.setShowMapOnly(true);
		setWidgetActivation();
		validate();
	}
	
	@SuppressWarnings("unused")
	public boolean saveScreenshot(final HistoryNode node, final File file)
	{
		// get current history node. if we are in history view, get the selected node.
		final MapPanel mapPanel = getMapPanel();
		boolean retval = true;
		// get round/step/player from history tree
		int round = 0;
		String step = null;
		PlayerID player = null;
		final Object[] pathFromRoot = node.getPath();
		for (final Object pathNode : pathFromRoot)
		{
			final HistoryNode curNode = (HistoryNode) pathNode;
			if (curNode instanceof Round)
				round = ((Round) curNode).getRoundNo();
			if (curNode instanceof Step)
			{
				player = ((Step) curNode).getPlayerID();
				step = curNode.getTitle();
			}
		}
		final double scale = m_uiContext.getScale();
		// print map panel to image
		final BufferedImage mapImage = Util.createImage((int) (scale * mapPanel.getImageWidth()), (int) (scale * mapPanel.getImageHeight()), false);
		final Graphics2D mapGraphics = mapImage.createGraphics();
		try
		{
			final GameData data = mapPanel.getData();
			data.acquireReadLock();
			try
			{
				// workaround to get the whole map
				// (otherwise the map is cut if current window is not on top of map)
				final int xOffset = mapPanel.getXOffset();
				final int yOffset = mapPanel.getYOffset();
				mapPanel.setTopLeft(0, 0);
				mapPanel.print(mapGraphics);
				mapPanel.setTopLeft(xOffset, yOffset);
			} finally
			{
				data.releaseReadLock();
			}
			// overlay title
			Color title_color = m_uiContext.getMapData().getColorProperty(MapData.PROPERTY_SCREENSHOT_TITLE_COLOR);
			if (title_color == null)
				title_color = Color.BLACK;
			final String s_title_x = m_uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_X);
			final String s_title_y = m_uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_Y);
			final String s_title_size = m_uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_FONT_SIZE);
			int title_x;
			int title_y;
			int title_size;
			try
			{
				title_x = (int) (Integer.parseInt(s_title_x) * scale);
				title_y = (int) (Integer.parseInt(s_title_y) * scale);
				title_size = Integer.parseInt(s_title_size);
			} catch (final NumberFormatException nfe)
			{
				// choose safe defaults
				title_x = (int) (15 * scale);
				title_y = (int) (15 * scale);
				title_size = 15;
			}
			// everything else should be scaled down onto map image
			final AffineTransform transform = new AffineTransform();
			transform.scale(scale, scale);
			mapGraphics.setTransform(transform);
			mapGraphics.setFont(new Font("Ariel", Font.BOLD, title_size));
			mapGraphics.setColor(title_color);
			if (m_uiContext.getMapData().getBooleanProperty(MapData.PROPERTY_SCREENSHOT_TITLE_ENABLED))
				mapGraphics.drawString(data.getGameName() + " Round " + round, title_x, title_y); // + ": " + (player != null ? player.getName() : "") + " - " + step
			// overlay stats, if enabled
			final boolean stats_enabled = m_uiContext.getMapData().getBooleanProperty(MapData.PROPERTY_SCREENSHOT_STATS_ENABLED);
			if (stats_enabled)
			{
				// get screenshot properties from map data
				Color stats_text_color = m_uiContext.getMapData().getColorProperty(MapData.PROPERTY_SCREENSHOT_STATS_TEXT_COLOR);
				if (stats_text_color == null)
					stats_text_color = Color.BLACK;
				Color stats_border_color = m_uiContext.getMapData().getColorProperty(MapData.PROPERTY_SCREENSHOT_STATS_BORDER_COLOR);
				if (stats_border_color == null)
					stats_border_color = Color.WHITE;
				final String s_stats_x = m_uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_STATS_X);
				final String s_stats_y = m_uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_STATS_Y);
				int stats_x;
				int stats_y;
				try
				{
					stats_x = (int) (Integer.parseInt(s_stats_x) * scale);
					stats_y = (int) (Integer.parseInt(s_stats_y) * scale);
				} catch (final NumberFormatException nfe)
				{
					// choose reasonable defaults
					stats_x = (int) (120 * scale);
					stats_y = (int) (70 * scale);
				}
				// Fetch stats table and save current properties before modifying them
				// NOTE: This is a bit of a hack, but creating a fresh JTable and
				// populating it with statsPanel data seemed hard. This was easier.
				final JTable table = m_statsPanel.getStatsTable();
				final javax.swing.table.TableCellRenderer oldRenderer = table.getDefaultRenderer(Object.class);
				final Font oldTableFont = table.getFont();
				final Font oldTableHeaderFont = table.getTableHeader().getFont();
				final Dimension oldTableSize = table.getSize();
				final Color oldTableFgColor = table.getForeground();
				final Color oldTableSelFgColor = table.getSelectionForeground();
				final int oldCol0Width = table.getColumnModel().getColumn(0).getPreferredWidth();
				final int oldCol2Width = table.getColumnModel().getColumn(2).getPreferredWidth();
				// override some stats table properties for screenshot
				table.setOpaque(false);
				table.setFont(new Font("Ariel", Font.BOLD, 15));
				table.setForeground(stats_text_color);
				table.setSelectionForeground(table.getForeground());
				table.setGridColor(stats_border_color);
				table.getTableHeader().setFont(new Font("Ariel", Font.BOLD, 15));
				table.getColumnModel().getColumn(0).setPreferredWidth(80);
				table.getColumnModel().getColumn(2).setPreferredWidth(90);
				table.setSize(table.getPreferredSize());
				table.doLayout();
				// initialize table/header dimensions
				final int tableWidth = table.getSize().width;
				final int tableHeight = table.getSize().height;
				final int hdrWidth = tableWidth; // use tableWidth not hdrWidth!
				final int hdrHeight = table.getTableHeader().getSize().height;
				// create image for capturing table header
				final BufferedImage tblHdrImage = Util.createImage(hdrWidth, hdrHeight, false);
				final Graphics2D tblHdrGraphics = tblHdrImage.createGraphics();
				// create image for capturing table (support transparencies)
				final BufferedImage tblImage = Util.createImage(tableWidth, tableHeight, true);
				final Graphics2D tblGraphics = tblImage.createGraphics();
				// create a custom renderer that paints selected cells transparently
				final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer()
				{
					private static final long serialVersionUID = 1978774284876746635L;
					
					{
						setOpaque(false);
					}
				};
				// set our custom renderer on the JTable
				table.setDefaultRenderer(Object.class, renderer);
				// print table and header to images and draw them on map
				table.getTableHeader().print(tblHdrGraphics);
				table.print(tblGraphics);
				mapGraphics.drawImage(tblHdrImage, stats_x, stats_y, null);
				mapGraphics.drawImage(tblImage, stats_x, stats_y + (int) (hdrHeight * scale), null);
				// Clean up objects. There might be some overkill here,
				// but there were memory leaks that are fixed by some/all of these.
				tblHdrGraphics.dispose();
				tblGraphics.dispose();
				m_statsPanel.setStatsBgImage(null);
				tblHdrImage.flush();
				tblImage.flush();
				// restore table properties
				table.setDefaultRenderer(Object.class, oldRenderer);
				table.setOpaque(true);
				table.setForeground(oldTableFgColor);
				table.setSelectionForeground(oldTableSelFgColor);
				table.setFont(oldTableFont);
				table.getTableHeader().setFont(oldTableHeaderFont);
				table.setSize(oldTableSize);
				table.getColumnModel().getColumn(0).setPreferredWidth(oldCol0Width);
				table.getColumnModel().getColumn(2).setPreferredWidth(oldCol2Width);
				table.doLayout();
			}
			// save Image as .png
			try
			{
				ImageIO.write(mapImage, "png", file);
			} catch (final Exception e2)
			{
				e2.printStackTrace();
				JOptionPane.showMessageDialog(TripleAFrame.this, e2.getMessage(), "Error saving Screenshot", JOptionPane.OK_OPTION);
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
	
	public void saveScreenshot(final HistoryNode node)
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
					if (saveScreenshot(node, file))
						JOptionPane.showMessageDialog(TripleAFrame.this, "Screenshot Saved", "Screenshot Saved", JOptionPane.INFORMATION_MESSAGE);
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
	
	private void setWidgetActivation()
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
		if (m_showHistoryAction != null)
		{
			m_showHistoryAction.setEnabled(!(m_inHistory || m_uiContext.getShowMapOnly()));
		}
		if (m_showGameAction != null)
		{
			m_showGameAction.setEnabled(!m_inGame);
		}
		if (m_showMapOnlyAction != null)
		{
			// We need to check and make sure there are no local human players
			boolean foundHuman = false;
			for (final IGamePlayer gamePlayer : m_localPlayers.getLocalPlayers())
			{
				if (gamePlayer instanceof TripleAPlayer)
				{
					foundHuman = true;
				}
			}
			if (!foundHuman)
			{
				m_showMapOnlyAction.setEnabled(m_inGame || m_inHistory);
			}
			else
			{
				m_showMapOnlyAction.setEnabled(false);
			}
		}
		if (m_editModeButtonModel != null)
		{
			if (m_editDelegate == null || m_uiContext.getShowMapOnly())
			{
				m_editModeButtonModel.setEnabled(false);
			}
			else
			{
				m_editModeButtonModel.setEnabled(true);
			}
		}
	}
	
	// setEditDelegate is called by TripleAPlayer at the start and end of a turn
	public void setEditDelegate(final IEditDelegate editDelegate)
	{
		m_editDelegate = editDelegate;
		// force a data change event to update the UI for edit mode
		m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		setWidgetActivation();
	}
	
	public IEditDelegate getEditDelegate()
	{
		return m_editDelegate;
	}
	
	public ButtonModel getEditModeButtonModel()
	{
		return m_editModeButtonModel;
	}
	
	public ButtonModel getShowCommentLogButtonModel()
	{
		return m_showCommentLogButtonModel;
	}
	
	public boolean getEditMode()
	{
		boolean isEditMode = false;
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
	
	private AbstractAction m_showCommentLogAction = new AbstractAction()
	{
		private static final long serialVersionUID = 3964381772343872268L;
		
		public void actionPerformed(final ActionEvent ae)
		{
			if (((ButtonModel) ae.getSource()).isSelected())
			{
				showCommentLog();
			}
			else
			{
				hideCommentLog();
			}
		}
		
		private void hideCommentLog()
		{
			if (m_chatPanel != null)
			{
				m_commentSplit.setBottomComponent(null);
				m_chatSplit.setBottomComponent(m_chatPanel);
				m_chatSplit.validate();
			}
			else
			{
				m_mapAndChatPanel.removeAll();
				m_chatSplit.setTopComponent(null);
				m_chatSplit.setBottomComponent(null);
				m_mapAndChatPanel.add(m_mapPanel, BorderLayout.CENTER);
				m_mapAndChatPanel.validate();
			}
		}
		
		private void showCommentLog()
		{
			if (m_chatPanel != null)
			{
				m_commentSplit.setBottomComponent(m_chatPanel);
				m_chatSplit.setBottomComponent(m_commentSplit);
				m_chatSplit.validate();
			}
			else
			{
				m_mapAndChatPanel.removeAll();
				m_chatSplit.setTopComponent(m_mapPanel);
				m_chatSplit.setBottomComponent(m_commentPanel);
				m_mapAndChatPanel.add(m_chatSplit, BorderLayout.CENTER);
				m_mapAndChatPanel.validate();
			}
		}
	};
	private AbstractAction m_showHistoryAction = new AbstractAction("Show history")
	{
		private static final long serialVersionUID = -3960551522512897374L;
		
		public void actionPerformed(final ActionEvent e)
		{
			showHistory();
			m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		}
	};
	private AbstractAction m_showGameAction = new AbstractAction("Show current game")
	{
		private static final long serialVersionUID = -7551760679570164254L;
		
		{
			setEnabled(false);
		}
		
		public void actionPerformed(final ActionEvent e)
		{
			showGame();
			m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		}
	};
	private AbstractAction m_showMapOnlyAction = new AbstractAction("Show map only")
	{
		private static final long serialVersionUID = -6621157075878333141L;
		
		public void actionPerformed(final ActionEvent e)
		{
			showMapOnly();
			m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
		}
	};
	private final AbstractAction m_saveScreenshotAction = new AbstractAction("Export Screenshot...")
	{
		private static final long serialVersionUID = -5908032486008953815L;
		
		public void actionPerformed(final ActionEvent e)
		{
			HistoryNode curNode = null;
			if (m_historyPanel == null)
				curNode = m_data.getHistory().getLastNode();
			else
				curNode = m_historyPanel.getCurrentNode();
			saveScreenshot(curNode);
		}
	};
	
	public Collection<Unit> moveFightersToCarrier(final Collection<Unit> fighters, final Territory where)
	{
		m_messageAndDialogThreadPool.waitForAll();
		m_mapPanel.centerOn(where);
		final AtomicReference<ScrollableTextField> textRef = new AtomicReference<ScrollableTextField>();
		final AtomicReference<JPanel> panelRef = new AtomicReference<JPanel>();
		try
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					final JPanel panel = new JPanel();
					panel.setLayout(new BorderLayout());
					final ScrollableTextField text = new ScrollableTextField(0, fighters.size());
					text.setBorder(new EmptyBorder(8, 8, 8, 8));
					panel.add(text, BorderLayout.CENTER);
					panel.add(new JLabel("How many fighters do you want to move from " + where.getName() + " to new carrier?"), BorderLayout.NORTH);
					panelRef.set(panel);
					textRef.set(text);
					panelRef.set(panel);
				}
			});
		} catch (final InterruptedException e)
		{
			throw new IllegalStateException(e);
		} catch (final InvocationTargetException e)
		{
			throw new IllegalStateException(e);
		}
		final int choice = EventThreadJOptionPane.showOptionDialog(this, panelRef.get(), "Place fighters on new carrier?", JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new String[] {
					"OK", "Cancel" }, "OK", getUIContext().getCountDownLatchHandler());
		if (choice == 0)
		{
			// arrayList.subList() is not serializable
			return new ArrayList<Unit>(new ArrayList<Unit>(fighters).subList(0, textRef.get().getValue()));
		}
		else
			return new ArrayList<Unit>(0);
	}
	
	public BattlePanel getBattlePanel()
	{
		// m_messageAndDialogThreadPool.waitForAll();
		return m_actionButtons.getBattlePanel();
	}
	
	/*
	public AbstractMovePanel getMovePanel()
	{
		//m_messageAndDialogThreadPool.waitForAll();
		return m_actionButtons.getMovePanel();
	}
	
	public TechPanel getTechPanel()
	{
		//m_messageAndDialogThreadPool.waitForAll();
		return m_actionButtons.getTechPanel();
	}
	
	public PlacePanel getPlacePanel()
	{
		//m_messageAndDialogThreadPool.waitForAll();
		return m_actionButtons.getPlacePanel();
	}
	
	public PurchasePanel getPurchasePanel()
	{
		//m_messageAndDialogThreadPool.waitForAll();
		return m_actionButtons.getPurchasePanel();
	}*/
	
	Action getShowGameAction()
	{
		return m_showGameAction;
	}
	
	Action getShowHistoryAction()
	{
		return m_showHistoryAction;
	}
	
	Action getShowMapOnlyAction()
	{
		return m_showMapOnlyAction;
	}
	
	Action getSaveScreenshotAction()
	{
		return m_saveScreenshotAction;
	}
	
	public IUIContext getUIContext()
	{
		return m_uiContext;
	}
	
	MapPanel getMapPanel()
	{
		return m_mapPanel;
	}
	
	@Override
	public JComponent getMainPanel()
	{
		return m_mapPanel;
	}
	
	// Beagle Code Called to Change Mapskin
	void updateMap(final String mapdir) throws IOException
	{
		m_uiContext.setMapDir(m_data, mapdir);
		// when changing skins, always show relief images
		if (m_uiContext.getMapData().getHasRelief())
		{
			TileImageFactory.setShowReliefImages(true);
		}
		m_mapPanel.setGameData(m_data);
		// update mappanels to use new image
		m_mapPanel.changeImage(m_uiContext.getMapData().getMapDimensions());
		final Image small = m_uiContext.getMapImage().getSmallMapImage();
		m_smallView.changeImage(small);
		m_mapPanel.changeSmallMapOffscreenMap();
		m_mapPanel.resetMap(); // redraw territories
	}
	
	@Override
	public IGame getGame()
	{
		return m_game;
	}
	
	public StatPanel getStatPanel()
	{
		return m_statsPanel;
	}
	
	@Override
	public void setShowChatTime(final boolean showTime)
	{
		m_chatPanel.setShowChatTime(showTime);
	}
}
