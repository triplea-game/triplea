package games.strategy.triplea.ui;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
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

import com.google.common.collect.ImmutableList;

import games.strategy.common.delegate.BaseEditDelegate;
import games.strategy.common.swing.SwingAction;
import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.common.ui.MacWrapper;
import games.strategy.common.ui.MainGameFrame;
import games.strategy.debug.ClientLogger;
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
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.thread.ThreadPool;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.attachments.UserActionAttachment;
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
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.Util;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.IntegerMap;
import games.strategy.util.LocalizeHTML;
import games.strategy.util.Match;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Tuple;

/**
 * Main frame for the triple a game
 */
public class TripleAFrame extends MainGameFrame {
  private static final long serialVersionUID = 7640069668264418976L;
  private GameData data;
  private IGame game;
  private MapPanel mapPanel;
  private MapPanelSmallView smallView;
  private JLabel message = new JLabel("No selection");
  private JLabel status = new JLabel("");
  private JLabel step = new JLabel("xxxxxx");
  private JLabel round = new JLabel("xxxxxx");
  private JLabel player = new JLabel("xxxxxx");
  private ActionButtons actionButtons;
  private JPanel gameMainPanel = new JPanel();
  private JPanel rightHandSidePanel = new JPanel();
  private JTabbedPane tabsPanel = new JTabbedPane();
  private StatPanel statsPanel;
  private EconomyPanel economyPanel;
  private ObjectivePanel objectivePanel;
  private NotesPanel notesPanel;
  private TerritoryDetailPanel details;
  private JPanel historyComponent = new JPanel();
  private JPanel gameSouthPanel;
  private HistoryPanel historyPanel;
  private boolean inHistory = false;
  private boolean inGame = true;
  private HistorySynchronizer historySyncher;
  private IUIContext uiContext;
  private JPanel mapAndChatPanel;
  private ChatPanel chatPanel;
  private CommentPanel commentPanel;
  private JSplitPane chatSplit;
  private JSplitPane commentSplit;
  private EditPanel editPanel;
  private final ButtonModel editModeButtonModel;
  private final ButtonModel showCommentLogButtonModel;
  private IEditDelegate editDelegate;
  private JSplitPane gameCenterPanel;
  private Territory territoryLastEntered;
  private List<Unit> unitsBeingMousedOver;
  private PlayerID lastStepPlayer;
  private PlayerID currentStepPlayer;
  private Map<PlayerID, Boolean> requiredTurnSeries = new HashMap<>();
  private ThreadPool messageAndDialogThreadPool;
  private TripleAMenu menu;

  /** Creates new TripleAFrame */
  public TripleAFrame(final IGame game, final LocalPlayers players) {
    super("TripleA - " + game.getData().getGameName(), players);
    this.game = game;
    data = game.getData();
    messageAndDialogThreadPool = new ThreadPool(1);
    addZoomKeyboardShortcuts();
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    WindowListener WINDOW_LISTENER = new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        leaveGame();
      }
    };
    this.addWindowListener(WINDOW_LISTENER);
    uiContext = new UIContext();
    uiContext.setDefaultMapDir(game.getData());
    uiContext.getMapData().verify(data);
    uiContext.setLocalPlayers(players);
    this.setCursor(uiContext.getCursor());
    // initialize m_editModeButtonModel before createMenuBar()
    editModeButtonModel = new JToggleButton.ToggleButtonModel();
    editModeButtonModel.setEnabled(false);
    showCommentLogButtonModel = new JToggleButton.ToggleButtonModel();
    AbstractAction m_showCommentLogAction = new AbstractAction() {
      private static final long serialVersionUID = 3964381772343872268L;

      @Override
      public void actionPerformed(final ActionEvent ae) {
        if (showCommentLogButtonModel.isSelected()) {
          showCommentLog();
        } else {
          hideCommentLog();
        }
      }

      private void hideCommentLog() {
        if (chatPanel != null) {
          commentSplit.setBottomComponent(null);
          chatSplit.setBottomComponent(chatPanel);
          chatSplit.validate();
        } else {
          mapAndChatPanel.removeAll();
          chatSplit.setTopComponent(null);
          chatSplit.setBottomComponent(null);
          mapAndChatPanel.add(mapPanel, BorderLayout.CENTER);
          mapAndChatPanel.validate();
        }
      }

      private void showCommentLog() {
        if (chatPanel != null) {
          commentSplit.setBottomComponent(chatPanel);
          chatSplit.setBottomComponent(commentSplit);
          chatSplit.validate();
        } else {
          mapAndChatPanel.removeAll();
          chatSplit.setTopComponent(mapPanel);
          chatSplit.setBottomComponent(commentPanel);
          mapAndChatPanel.add(chatSplit, BorderLayout.CENTER);
          mapAndChatPanel.validate();
        }
      }
    };
    showCommentLogButtonModel.addActionListener(m_showCommentLogAction);
    showCommentLogButtonModel.setSelected(false);
    menu = new TripleAMenu(this);
    this.setJMenuBar(menu);
    final ImageScrollModel model = new ImageScrollModel();
    model.setScrollX(uiContext.getMapData().scrollWrapX());
    model.setScrollY(uiContext.getMapData().scrollWrapY());
    model.setMaxBounds(uiContext.getMapData().getMapDimensions().width,
        uiContext.getMapData().getMapDimensions().height);
    final Image small = uiContext.getMapImage().getSmallMapImage();
    smallView = new MapPanelSmallView(small, model);
    mapPanel = new MapPanel(data, smallView, uiContext, model);
    mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);
    MouseOverUnitListener MOUSE_OVER_UNIT_LISTENER = new MouseOverUnitListener() {
      @Override
      public void mouseEnter(final List<Unit> units, final Territory territory, final MouseDetails me) {
        unitsBeingMousedOver = units;
      }
    };
    mapPanel.addMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
    // link the small and large images
    mapPanel.initSmallMap();
    mapAndChatPanel = new JPanel();
    mapAndChatPanel.setLayout(new BorderLayout());
    commentPanel = new CommentPanel(this, data);
    chatSplit = new JSplitPane();
    chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    chatSplit.setOneTouchExpandable(true);
    chatSplit.setDividerSize(8);
    chatSplit.setResizeWeight(0.95);
    if (MainFrame.getInstance() != null && MainFrame.getInstance().getChat() != null) {
      commentSplit = new JSplitPane();
      commentSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
      commentSplit.setOneTouchExpandable(true);
      commentSplit.setDividerSize(8);
      commentSplit.setResizeWeight(0.5);
      commentSplit.setTopComponent(commentPanel);
      commentSplit.setBottomComponent(null);
      chatPanel = new ChatPanel(MainFrame.getInstance().getChat());
      chatPanel.setPlayerRenderer(new PlayerChatRenderer(this.game, uiContext));
      final Dimension chatPrefSize = new Dimension((int) chatPanel.getPreferredSize().getWidth(), 95);
      chatPanel.setPreferredSize(chatPrefSize);
      chatSplit.setTopComponent(mapPanel);
      chatSplit.setBottomComponent(chatPanel);
      mapAndChatPanel.add(chatSplit, BorderLayout.CENTER);
    } else {
      mapAndChatPanel.add(mapPanel, BorderLayout.CENTER);
    }
    gameMainPanel.setLayout(new BorderLayout());
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(gameMainPanel, BorderLayout.CENTER);
    gameSouthPanel = new JPanel();
    gameSouthPanel.setLayout(new BorderLayout());
    // m_gameSouthPanel.add(m_message, BorderLayout.WEST);
    message.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    message.setPreferredSize(message.getPreferredSize());
    message.setText("some text to set a reasonable preferred size");
    status.setText("some text to set a reasonable preferred size for movement error messages");
    message.setPreferredSize(message.getPreferredSize());
    status.setPreferredSize(message.getPreferredSize());
    message.setText("");
    status.setText("");
    // m_gameSouthPanel.add(m_status, BorderLayout.CENTER);
    final JPanel bottomMessagePanel = new JPanel();
    bottomMessagePanel.setLayout(new GridBagLayout());
    bottomMessagePanel.setBorder(BorderFactory.createEmptyBorder());
    bottomMessagePanel.add(message, new GridBagConstraints(0, 0, 1, 1, .35, 1, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    bottomMessagePanel.add(status, new GridBagConstraints(1, 0, 1, 1, .65, 1, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    gameSouthPanel.add(bottomMessagePanel, BorderLayout.CENTER);
    status.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    final JPanel stepPanel = new JPanel();
    stepPanel.setLayout(new GridBagLayout());
    stepPanel.add(step, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));
    stepPanel.add(player, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));
    stepPanel.add(round, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));
    step.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    round.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    player.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    step.setHorizontalTextPosition(SwingConstants.LEADING);
    gameSouthPanel.add(stepPanel, BorderLayout.EAST);
    gameMainPanel.add(gameSouthPanel, BorderLayout.SOUTH);
    rightHandSidePanel.setLayout(new BorderLayout());
    final FocusAdapter focusToMapPanelFocusListener = new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        // give the focus back to the map panel
        mapPanel.requestFocusInWindow();
      }
    };
    rightHandSidePanel.addFocusListener(focusToMapPanelFocusListener);
    smallView.addFocusListener(focusToMapPanelFocusListener);
    tabsPanel.addFocusListener(focusToMapPanelFocusListener);
    rightHandSidePanel.add(smallView, BorderLayout.NORTH);
    tabsPanel.setBorder(null);
    rightHandSidePanel.add(tabsPanel, BorderLayout.CENTER);

    MovePanel movePanel = new MovePanel(data, mapPanel, this);
    actionButtons = new ActionButtons(data, mapPanel, movePanel, this);

    List<KeyListener> keyListeners = ImmutableList.of(this.getArrowKeyListener(), movePanel.getUndoMoveKeyListener(), getFlagToggleKeyListener(this));
    for (KeyListener keyListener : keyListeners) {
      mapPanel.addKeyListener(keyListener);
      // TODO: figure out if it is really needed to double add the key listener to both the frame and also the map panel
      this.addKeyListener(keyListener);
    }

    tabsPanel.addTab("Actions", actionButtons);
    actionButtons.setBorder(null);
    statsPanel = new StatPanel(data, uiContext);
    tabsPanel.addTab("Stats", statsPanel);
    economyPanel = new EconomyPanel(data);
    tabsPanel.addTab("Economy", economyPanel);
    objectivePanel = new ObjectivePanel(data);
    if (objectivePanel.isEmpty()) {
      objectivePanel.removeDataChangeListener();
      objectivePanel = null;
    } else {
      tabsPanel.addTab(objectivePanel.getName(), objectivePanel);
    }
    notesPanel = new NotesPanel(menu.getGameNotesJEditorPane());
    tabsPanel.addTab("Notes", notesPanel);
    details = new TerritoryDetailPanel(mapPanel, data, uiContext, this);
    tabsPanel.addTab("Territory", null, details, TerritoryDetailPanel.getHoverText());
    editPanel = new EditPanel(data, mapPanel, this);
    // Register a change listener
    tabsPanel.addChangeListener(new ChangeListener() {
      // This method is called whenever the selected tab changes
      @Override
      public void stateChanged(final ChangeEvent evt) {
        final JTabbedPane pane = (JTabbedPane) evt.getSource();
        // Get current tab
        final int sel = pane.getSelectedIndex();
        if (sel == -1) {
          return;
        }
        if (pane.getComponentAt(sel).equals(notesPanel)) {
          notesPanel.layoutNotes();
        } else {
          // for memory management reasons the notes are in a SoftReference,
          // so we must remove our hard reference link to them so it can be reclaimed if needed
          notesPanel.removeNotes();
        }
        if (pane.getComponentAt(sel).equals(editPanel)) {
          PlayerID player = null;
          data.acquireReadLock();
          try {
            player = data.getSequence().getStep().getPlayerID();
          } finally {
            data.releaseReadLock();
          }
          actionButtons.getCurrent().setActive(false);
          editPanel.display(player);
        } else {
          actionButtons.getCurrent().setActive(true);
          editPanel.setActive(false);
        }
      }
    });
    rightHandSidePanel.setPreferredSize(new Dimension((int) smallView.getPreferredSize().getWidth(),
        (int) mapPanel.getPreferredSize().getHeight()));
    gameCenterPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapAndChatPanel, rightHandSidePanel);
    gameCenterPanel.setOneTouchExpandable(true);
    gameCenterPanel.setDividerSize(8);
    gameCenterPanel.setResizeWeight(1.0);
    gameMainPanel.add(gameCenterPanel, BorderLayout.CENTER);
    gameCenterPanel.resetToPreferredSizes();
    // set up the edit mode overlay text
    this.setGlassPane(new JComponent() {
      private static final long serialVersionUID = 6724687534214427291L;

      @Override
      protected void paintComponent(final Graphics g) {
        g.setFont(new Font("Ariel", Font.BOLD, 50));
        g.setColor(new Color(255, 255, 255, 175));
        final Dimension size = mapPanel.getSize();
        g.drawString("Edit Mode", (int) ((size.getWidth() - 200) / 2), (int) ((size.getHeight() - 100) / 2));
      }
    });
    // force a data change event to update the UI for edit mode
    m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    data.addDataChangeListener(m_dataChangeListener);
    game.addGameStepListener(m_stepListener);
    updateStep();
    uiContext.addShutdownWindow(this);
  }
  
  
  public static KeyListener getFlagToggleKeyListener(TripleAFrame frame) {
    return new KeyListener() {
      private boolean blockInputs = false;
      @Override
      public void keyTyped(final KeyEvent e) {/*Do nothing*/}

      @Override
      public void keyPressed(final KeyEvent e) {
        if(!blockInputs){
          toggleFlags(e.getKeyCode());
          blockInputs = true;
        }
      }

      @Override
      public void keyReleased(final KeyEvent e) {
        toggleFlags(e.getKeyCode());
        blockInputs = false;
      }
      
      private void toggleFlags(int keyCode){
        if (keyCode == KeyEvent.VK_L){
          UnitsDrawer.enabledFlags = !UnitsDrawer.enabledFlags;
          frame.getMapPanel().resetMap();
        }
      }
    };

  }

  private void addZoomKeyboardShortcuts() {
    final String zoom_map_in = "zoom_map_in";
    // do both = and + (since = is what you get when you hit ctrl+ )
    ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke('+', java.awt.event.InputEvent.META_MASK), zoom_map_in);
    ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke('+', java.awt.event.InputEvent.CTRL_MASK), zoom_map_in);
    ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke('=', java.awt.event.InputEvent.META_MASK), zoom_map_in);
    ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke('=', java.awt.event.InputEvent.CTRL_MASK), zoom_map_in);
    ((JComponent) getContentPane()).getActionMap().put(zoom_map_in, new AbstractAction(zoom_map_in) {
      private static final long serialVersionUID = -7565304172320049817L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (getScale() < 100) {
          setScale(getScale() + 10);
        }
      }
    });
    final String zoom_map_out = "zoom_map_out";
    ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke('-', java.awt.event.InputEvent.META_MASK), zoom_map_out);
    ((JComponent) getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke('-', java.awt.event.InputEvent.CTRL_MASK), zoom_map_out);
    ((JComponent) getContentPane()).getActionMap().put(zoom_map_out, new AbstractAction(zoom_map_out) {
      private static final long serialVersionUID = 7677111833274819304L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        if (getScale() > 16) {
          setScale(getScale() - 10);
        }
      }
    });
  }

  /**
   * @param value
   *        - a number between 15 and 100
   */
  void setScale(final double value) {
    getMapPanel().setScale(value / 100);
  }

  /**
   * @return a scale between 15 and 100
   */
  private double getScale() {
    return getMapPanel().getScale() * 100;
  }

  @Override
  public void stopGame() {
    // we have already shut down
    if (uiContext == null) {
      return;
    }
    menu.dispose();
    menu = null;
    this.setVisible(false);
    TripleAFrame.this.dispose();
    if (GameRunner.isMac()) {
      // this frame should not handle shutdowns anymore
      MacWrapper.unregisterShutdownHandler();
    }
    messageAndDialogThreadPool.shutDown();
    uiContext.shutDown();
    if (chatPanel != null) {
      chatPanel.setPlayerRenderer(null);
      chatPanel.setChat(null);
    }
    if (historySyncher != null) {
      historySyncher.deactivate();
      historySyncher = null;
    }
    // clear out dynamix's properties
    // Dynamix_AI.clearCachedGameDataAll(); TODO: errors cus dynamix sucks
    ProAI.gameOverClearCache();
  }

  @Override
  public void shutdown() {
    final int rVal = EventThreadJOptionPane.showConfirmDialog(this,
        "Are you sure you want to exit?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION,
        getUIContext().getCountDownLatchHandler());
    if (rVal != JOptionPane.OK_OPTION) {
      return;
    }
    stopGame();
    System.exit(0);
  }

  @Override
  public void leaveGame() {
    final int rVal = EventThreadJOptionPane.showConfirmDialog(this,
        "Are you sure you want to leave?\nUnsaved game data will be lost.", "Exit", JOptionPane.YES_NO_OPTION,
        getUIContext().getCountDownLatchHandler());
    if (rVal != JOptionPane.OK_OPTION) {
      return;
    }
    if (game instanceof ServerGame) {
      ((ServerGame) game).stopGame();
    } else {
      game.getMessenger().shutDown();
      ((ClientGame) game).shutDown();
      // an ugly hack, we need a better
      // way to get the main frame
      MainFrame.getInstance().clientLeftGame();
    }
  }

  public MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener() {
    @Override
    public void mouseEntered(final Territory territory) {
      territoryLastEntered = territory;
      refresh();
    }

    void refresh() {
      final StringBuilder buf = new StringBuilder(" ");
      buf.append(territoryLastEntered == null ? "none" : territoryLastEntered.getName());
      if (territoryLastEntered != null) {
        final TerritoryAttachment ta = TerritoryAttachment.get(territoryLastEntered);
        if (ta != null) {
          final Iterator<TerritoryEffect> iter = ta.getTerritoryEffect().iterator();
          if (iter.hasNext()) {
            buf.append(" (");
          }
          while (iter.hasNext()) {
            buf.append(iter.next().getName());
            if (iter.hasNext()) {
              buf.append(", ");
            } else {
              buf.append(")");
            }
          }
          final int production = ta.getProduction();
          final int unitProduction = ta.getUnitProduction();
          final ResourceCollection resource = ta.getResources();
          if (unitProduction > 0 && unitProduction != production) {
            buf.append(", UnitProd: ").append(unitProduction);
          }
          if (production > 0 || (resource != null && resource.toString().length() > 0)) {
            buf.append(", Prod: ");
            if (production > 0) {
              buf.append(production).append(" PUs");
              if (resource != null && resource.toString().length() > 0) {
                buf.append(", ");
              }
            }
            if (resource != null) {
              buf.append(resource.toString());
            }
          }
        }
      }
      message.setText(buf.toString());
    }
  };

  public void clearStatusMessage() {
    if (status == null) {
      return;
    }
    status.setText("");
    status.setIcon(null);
  }

  public void setStatusErrorMessage(final String msg) {
    if (status == null) {
      return;
    }
    status.setText(msg);
    if (!msg.equals("")) {
      status.setIcon(new ImageIcon(mapPanel.getErrorImage()));
    } else {
      status.setIcon(null);
    }
  }

  public void setStatusWarningMessage(final String msg) {
    if (status == null) {
      return;
    }
    status.setText(msg);
    if (!msg.equals("")) {
      status.setIcon(new ImageIcon(mapPanel.getWarningImage()));
    } else {
      status.setIcon(null);
    }
  }

  public void setStatusInfoMessage(final String msg) {
    if (status == null) {
      return;
    }
    status.setText(msg);
    if (!msg.equals("")) {
      status.setIcon(new ImageIcon(mapPanel.getInfoImage()));
    } else {
      status.setIcon(null);
    }
  }

  public IntegerMap<ProductionRule> getProduction(final PlayerID player, final boolean bid) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToProduce(player);
    return actionButtons.waitForPurchase(bid);
  }

  public HashMap<Unit, IntegerMap<RepairRule>> getRepair(final PlayerID player, final boolean bid,
      final Collection<PlayerID> allowedPlayersToRepair) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToRepair(player);
    return actionButtons.waitForRepair(bid, allowedPlayersToRepair);
  }

  public MoveDescription getMove(final PlayerID player, final IPlayerBridge bridge, final boolean nonCombat,
      final String stepName) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToMove(player, nonCombat, stepName);
    // workaround for panel not receiving focus at beginning of n/c move phase
    if (!getBattlePanel().getBattleFrame().isVisible()) {
      requestWindowFocus();
    }
    return actionButtons.waitForMove(bridge);
  }

  private void requestWindowFocus() {
    SwingAction.invokeAndWait(() -> {
      requestFocusInWindow();
      transferFocus();
    });
  }

  public PlaceData waitForPlace(final PlayerID player, final boolean bid, final IPlayerBridge bridge) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToPlace(player);
    return actionButtons.waitForPlace(bid, bridge);
  }

  public void waitForMoveForumPoster(final PlayerID player, final IPlayerBridge bridge) {
    if (actionButtons == null || messageAndDialogThreadPool == null) {
      return;
    }
    // m_messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToMoveForumPosterPanel(player);
    actionButtons.waitForMoveForumPosterPanel(this, bridge);
  }

  public void waitForEndTurn(final PlayerID player, final IPlayerBridge bridge) {
    if (actionButtons == null || messageAndDialogThreadPool == null) {
      return;
    }
    // m_messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToEndTurn(player);
    actionButtons.waitForEndTurn(this, bridge);
  }

  public FightBattleDetails getBattle(final PlayerID player, final Map<BattleType, Collection<Territory>> battles) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToBattle(player, battles);
    return actionButtons.waitForBattleSelection();
  }

  /**
   * We do NOT want to block the next player from beginning their turn.
   */
  @Override
  public void notifyError(final String message) {
    final String displayMessage = LocalizeHTML.localizeImgLinksInHTML(message);
    if (messageAndDialogThreadPool == null) {
      return;
    }
    messageAndDialogThreadPool.runTask(new Runnable() {
      @Override
      public void run() {
        EventThreadJOptionPane.showMessageDialog(TripleAFrame.this, displayMessage, "Error", JOptionPane.ERROR_MESSAGE,
            true, getUIContext().getCountDownLatchHandler());
      }
    });
  }

  /**
   * We do NOT want to block the next player from beginning their turn.
   *
   * @param message
   * @param title
   */
  public void notifyMessage(final String message, final String title) {
    if (message == null || title == null) {
      return;
    }
    if (title.indexOf(AbstractConditionsAttachment.TRIGGER_CHANCE_FAILURE) != -1
        && message.indexOf(AbstractConditionsAttachment.TRIGGER_CHANCE_FAILURE) != -1
        && !getUIContext().getShowTriggerChanceFailure()) {
      return;
    }
    if (title.indexOf(AbstractConditionsAttachment.TRIGGER_CHANCE_SUCCESSFUL) != -1
        && message.indexOf(AbstractConditionsAttachment.TRIGGER_CHANCE_SUCCESSFUL) != -1
        && !getUIContext().getShowTriggerChanceSuccessful()) {
      return;
    }
    if (title.equals(AbstractTriggerAttachment.NOTIFICATION) && !getUIContext().getShowTriggeredNotifications()) {
      return;
    }
    if (title.indexOf(AbstractEndTurnDelegate.END_TURN_REPORT_STRING) != -1
        && message.indexOf(AbstractEndTurnDelegate.END_TURN_REPORT_STRING) != -1
        && !getUIContext().getShowEndOfTurnReport()) {
      return;
    }
    final String displayMessage = LocalizeHTML.localizeImgLinksInHTML(message);
    if (messageAndDialogThreadPool != null) {
      messageAndDialogThreadPool.runTask(new Runnable() {
        @Override
        public void run() {
          EventThreadJOptionPane.showMessageDialog(TripleAFrame.this, displayMessage, title,
              JOptionPane.INFORMATION_MESSAGE, true, getUIContext().getCountDownLatchHandler());
        }
      });
    }
  }

  public boolean getOKToLetAirDie(final PlayerID m_id, final Collection<Territory> airCantLand,
      final boolean movePhase) {
    if (airCantLand == null || airCantLand.isEmpty() || messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final String airUnitPlural = (airCantLand.size() == 1) ? "" : "s";
    final String territoryPlural = (airCantLand.size() == 1) ? "y" : "ies";
    final StringBuilder sb = new StringBuilder("<html>" + airCantLand.size() + " air unit" + airUnitPlural
        + " cannot land in the following territor" + territoryPlural + ":<ul> ");
    for (final Territory t : airCantLand) {
      sb.append("<li>").append(t.getName()).append("</li>");
    }
    sb.append("</ul></html>");
    final boolean lhtrProd = AirThatCantLandUtil.isLHTRCarrierProduction(data)
        || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
    int carrierCount = 0;
    for (final PlayerID p : GameStepPropertiesHelper.getCombinedTurns(data, m_id)) {
      carrierCount += p.getUnits().getMatches(Matches.UnitIsCarrier).size();
    }
    final boolean canProduceCarriersUnderFighter = lhtrProd && carrierCount != 0;
    if (canProduceCarriersUnderFighter && carrierCount > 0) {
      sb.append("\nYou have ").append(carrierCount).append(" ").append(MyFormatter.pluralize("carrier", carrierCount))
          .append(" on which planes can land");
    }
    final String ok = movePhase ? "End Move Phase" : "Kill Planes";
    final String cancel = movePhase ? "Keep Moving" : "Change Placement";
    final String[] options = {cancel, ok};
    this.mapPanel.centerOn(airCantLand.iterator().next());
    final int choice =
        EventThreadJOptionPane.showOptionDialog(this, sb.toString(), "Air cannot land", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE, null, options, cancel, getUIContext().getCountDownLatchHandler());
    return choice == 1;
  }

  public boolean getOKToLetUnitsDie(final Collection<Territory> unitsCantFight, final boolean movePhase) {
    if (unitsCantFight == null || unitsCantFight.isEmpty() || messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final StringBuilder buf = new StringBuilder("Units in the following territories will die: ");
    final Iterator<Territory> iter = unitsCantFight.iterator();
    while (iter.hasNext()) {
      buf.append((iter.next()).getName());
      buf.append(" ");
    }
    final String ok = movePhase ? "Done Moving" : "Kill Units";
    final String cancel = movePhase ? "Keep Moving" : "Change Placement";
    final String[] options = {cancel, ok};
    this.mapPanel.centerOn(unitsCantFight.iterator().next());
    final int choice =
        EventThreadJOptionPane.showOptionDialog(this, buf.toString(), "Units cannot fight", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE, null, options, cancel, getUIContext().getCountDownLatchHandler());
    return choice == 1;
  }

  public boolean acceptAction(final PlayerID playerSendingProposal, final String acceptanceQuestion,
      final boolean politics) {
    if (messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final int choice = EventThreadJOptionPane.showConfirmDialog(this, acceptanceQuestion,
        "Accept " + (politics ? "Political " : "") + "Proposal from " + playerSendingProposal.getName() + "?",
        JOptionPane.YES_NO_OPTION, getUIContext().getCountDownLatchHandler());
    return choice == JOptionPane.YES_OPTION;
  }

  public boolean getOK(final String message) {
    if (messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final int choice = EventThreadJOptionPane.showConfirmDialog(this, message, message, JOptionPane.OK_CANCEL_OPTION,
        getUIContext().getCountDownLatchHandler());
    return choice == JOptionPane.OK_OPTION;
  }

  public void notifyTechResults(final TechResults msg) {
    if (messageAndDialogThreadPool == null) {
      return;
    }
    messageAndDialogThreadPool.runTask(new Runnable() {
      @Override
      public void run() {
        final AtomicReference<TechResultsDisplay> displayRef = new AtomicReference<>();
        SwingAction.invokeAndWait(() -> {
          final TechResultsDisplay display = new TechResultsDisplay(msg, uiContext, data);
          displayRef.set(display);
        });
        EventThreadJOptionPane.showOptionDialog(TripleAFrame.this, displayRef.get(), "Tech roll", JOptionPane.OK_OPTION,
            JOptionPane.PLAIN_MESSAGE, null, new String[] {"OK"}, "OK", getUIContext().getCountDownLatchHandler());
      }
    });
  }

  public boolean getStrategicBombingRaid(final Territory location) {
    if (messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final String message =
        (games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(data) ? "Bomb/Escort" : "Bomb") + " in "
            + location.getName();
    final String bomb =
        (games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(data) ? "Bomb/Escort" : "Bomb");
    final String normal = "Attack";
    final String[] choices = {bomb, normal};
    int choice = -1;
    while (choice < 0 || choice > 1) {
      choice = EventThreadJOptionPane.showOptionDialog(this, message, "Bomb?", JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.INFORMATION_MESSAGE, null, choices, bomb, getUIContext().getCountDownLatchHandler());
    }
    return choice == JOptionPane.OK_OPTION;
  }

  public Unit getStrategicBombingRaidTarget(final Territory territory, final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    if (potentialTargets == null || potentialTargets.size() == 0) {
      return null;
    }
    if (potentialTargets.size() == 1 || messageAndDialogThreadPool == null) {
      return potentialTargets.iterator().next();
    }
    messageAndDialogThreadPool.waitForAll();
    final AtomicReference<Unit> selected = new AtomicReference<>();
    final String message = "Select bombing target in " + territory.getName();
    final Tuple<JPanel, JList<Unit>> comps = Util.runInSwingEventThread(new Util.Task<Tuple<JPanel, JList<Unit>>>() {
      @Override
      public Tuple<JPanel, JList<Unit>> run() {
        final JList<Unit> list = new JList<>(new Vector<>(potentialTargets));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        if (bombers != null) {
          panel.add(new JLabel("For Units: " + MyFormatter.unitsToTextNoOwner(bombers)), BorderLayout.NORTH);
        }
        final JScrollPane scroll = new JScrollPane(list);
        panel.add(scroll, BorderLayout.CENTER);
        return Tuple.of(panel, list);
      }
    });
    final JPanel panel = comps.getFirst();
    final JList<?> list = comps.getSecond();
    final String[] options = {"OK", "Cancel"};
    final int selection = EventThreadJOptionPane.showOptionDialog(this, panel, message, JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE, null, options, null, getUIContext().getCountDownLatchHandler());
    if (selection == 0) {
      selected.set((Unit) list.getSelectedValue());
    }
    // Unit selected = (Unit) list.getSelectedValue();
    return selected.get();
  }

  public int[] selectFixedDice(final int numDice, final int hitAt, final boolean hitOnlyIfEquals, final String title,
      final int diceSides) {
    if (messageAndDialogThreadPool == null) {
      return new int[numDice];
    }
    messageAndDialogThreadPool.waitForAll();
    final DiceChooser chooser = Util.runInSwingEventThread(new Util.Task<DiceChooser>() {
      @Override
      public DiceChooser run() {
        return new DiceChooser(getUIContext(), numDice, hitAt, hitOnlyIfEquals, diceSides);
      }
    });
    do {
      EventThreadJOptionPane.showMessageDialog(null, chooser, title, JOptionPane.PLAIN_MESSAGE,
          getUIContext().getCountDownLatchHandler());
    } while (chooser.getDice() == null);
    return chooser.getDice();
  }

  public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory,
      final String unitMessage) {
    if (candidates == null || candidates.isEmpty()) {
      return null;
    }
    if (candidates.size() == 1 || messageAndDialogThreadPool == null) {
      return candidates.iterator().next();
    }
    messageAndDialogThreadPool.waitForAll();
    final Tuple<JPanel, JList<Territory>> comps = Util.runInSwingEventThread(new Util.Task<Tuple<JPanel, JList<Territory>>>() {
      @Override
      public Tuple<JPanel, JList<Territory>> run() {
        mapPanel.centerOn(currentTerritory);
        final JList<Territory> list = new JList<>(new Vector<>(candidates));
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
        return Tuple.of(panel, list);
      }
    });
    final JPanel panel = comps.getFirst();
    final JList<?> list = comps.getSecond();
    final String[] options = {"OK"};
    final String title = "Select territory for air units to land, current territory is " + currentTerritory.getName();
    EventThreadJOptionPane.showOptionDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
        null, options, null, getUIContext().getCountDownLatchHandler());
    final Territory selected = (Territory) list.getSelectedValue();
    return selected;
  }

  public Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(final PlayerID player,
      final List<Territory> territoryChoices, final List<Unit> unitChoices, final int unitsPerPick) {
    if (messageAndDialogThreadPool == null) {
      return Tuple.of(territoryChoices.iterator().next(),
          new HashSet<>(Match.getNMatches(unitChoices, unitsPerPick, Match.getAlwaysMatch())));
    }
    // total hacks
    messageAndDialogThreadPool.waitForAll();
    {
      final CountDownLatch latch1 = new CountDownLatch(1);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          if (!inGame) {
            showGame();
          }
          if (tabsPanel.indexOfTab("Actions") == -1) {
            // add actions tab
            tabsPanel.insertTab("Actions", null, actionButtons, null, 0);
          }
          tabsPanel.setSelectedIndex(0);
          latch1.countDown();
        }
      });
      try {
        latch1.await();
      } catch (final InterruptedException e) {
        ClientLogger.logQuietly(e);
      }
    }
    actionButtons.changeToPickTerritoryAndUnits(player);
    final Tuple<Territory, Set<Unit>> rVal =
        actionButtons.waitForPickTerritoryAndUnits(territoryChoices, unitChoices, unitsPerPick);
    final int index = tabsPanel == null ? -1 : tabsPanel.indexOfTab("Actions");
    if (index != -1 && inHistory) {
      final CountDownLatch latch2 = new CountDownLatch(1);
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          if (tabsPanel != null) {
            // remove actions tab
            tabsPanel.remove(index);
          }
          latch2.countDown();
        }
      });
      try {
        latch2.await();
      } catch (final InterruptedException e) {
        ClientLogger.logQuietly(e);
      }
    }
    if (actionButtons != null && actionButtons.getCurrent() != null) {
      actionButtons.getCurrent().setActive(false);
    }
    return rVal;
  }

  public HashMap<Territory, IntegerMap<Unit>> selectKamikazeSuicideAttacks(
      final HashMap<Territory, Collection<Unit>> possibleUnitsToAttack, final Resource attackResourceToken,
      final int maxNumberOfAttacksAllowed) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Should not be called from dispatch thread");
    }
    final HashMap<Territory, IntegerMap<Unit>> selection = new HashMap<>();
    if (possibleUnitsToAttack == null || possibleUnitsToAttack.isEmpty() || attackResourceToken == null
        || maxNumberOfAttacksAllowed <= 0 || messageAndDialogThreadPool == null) {
      return selection;
    }
    messageAndDialogThreadPool.waitForAll();
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Collection<IndividualUnitPanelGrouped> unitPanels = new ArrayList<>();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final HashMap<String, Collection<Unit>> possibleUnitsToAttackStringForm =
            new HashMap<>();
        for (final Entry<Territory, Collection<Unit>> entry : possibleUnitsToAttack.entrySet()) {
          final List<Unit> units = new ArrayList<>(entry.getValue());
          Collections.sort(units,
              new UnitBattleComparator(false, BattleCalculator.getCostsForTuvForAllPlayersMergedAndAveraged(data),
                  TerritoryEffectHelper.getEffects(entry.getKey()), data, true, false));
          Collections.reverse(units);
          possibleUnitsToAttackStringForm.put(entry.getKey().getName(), units);
        }
        mapPanel.centerOn(data.getMap().getTerritory(possibleUnitsToAttackStringForm.keySet().iterator().next()));
        final IndividualUnitPanelGrouped unitPanel = new IndividualUnitPanelGrouped(possibleUnitsToAttackStringForm,
            data, uiContext, "Select Units to Suicide Attack using " + attackResourceToken.getName(),
            maxNumberOfAttacksAllowed, true, false);
        unitPanels.add(unitPanel);
        final String optionAttack = "Attack";
        final String optionNone = "None";
        final String optionWait = "Wait";
        final Object[] options = {optionAttack, optionNone, optionWait};
        final JOptionPane optionPane = new JOptionPane(unitPanel, JOptionPane.PLAIN_MESSAGE,
            JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[2]);
        final JDialog dialog =
            new JDialog((Frame) getParent(), "Select units to Suicide Attack using " + attackResourceToken.getName());
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setLocationRelativeTo(getParent());
        dialog.setAlwaysOnTop(true);
        dialog.pack();
        dialog.setVisible(true);
        dialog.requestFocusInWindow();
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
          @Override
          public void propertyChange(final PropertyChangeEvent e) {
            if (!dialog.isVisible()) {
              return;
            }
            final String option = ((String) optionPane.getValue());
            if (option.equals(optionNone)) {
              unitPanels.clear();
              selection.clear();
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              continueLatch.countDown();
            } else if (option.equals(optionAttack)) {
              if (unitPanels.size() != 1) {
                throw new IllegalStateException("unitPanels should only contain 1 entry");
              }
              for (final IndividualUnitPanelGrouped terrChooser : unitPanels) {
                for (final Entry<String, IntegerMap<Unit>> entry : terrChooser.getSelected().entrySet()) {
                  selection.put(data.getMap().getTerritory(entry.getKey()), entry.getValue());
                }
              }
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              continueLatch.countDown();
            } else {
              unitPanels.clear();
              selection.clear();
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              ThreadUtil.sleep(500);
              run();
            }
          }
        });
      }
    });
    mapPanel.getUIContext().addShutdownLatch(continueLatch);
    try {
      continueLatch.await();
    } catch (final InterruptedException ex) {
      // ignore interrupted exception
    } finally {
      mapPanel.getUIContext().removeShutdownLatch(continueLatch);
    }
    return selection;
  }

  public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Should not be called from dispatch thread");
    }
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final HashMap<Territory, Collection<Unit>> selection = new HashMap<>();
    final Collection<Tuple<Territory, UnitChooser>> choosers = new ArrayList<>();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mapPanel.centerOn(scrambleTo);
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        final JLabel whereTo = new JLabel("Scramble To: " + scrambleTo.getName());
        whereTo.setFont(new Font("Arial", Font.ITALIC, 12));
        panel.add(whereTo, BorderLayout.NORTH);
        final JPanel panel2 = new JPanel();
        panel2.setBorder(BorderFactory.createEmptyBorder());
        panel2.setLayout(new FlowLayout());
        for (final Territory from : possibleScramblers.keySet()) {
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
          final int maxAllowed =
              Math.min(BattleDelegate.getMaxScrambleCount(possibleScramblers.get(from).getFirst()), possible.size());
          final UnitChooser chooser =
              new UnitChooser(possible, Collections.emptyMap(), data, false, uiContext);
          chooser.setMaxAndShowMaxButton(maxAllowed);
          choosers.add(Tuple.of(from, chooser));
          panelChooser.add(chooser);
          chooserScrollPane = new JScrollPane(panelChooser);
          panel2.add(chooserScrollPane);
        }
        panel.add(panel2, BorderLayout.CENTER);
        final String optionScramble = "Scramble";
        final String optionNone = "None";
        final String optionWait = "Wait";
        final Object[] options = {optionScramble, optionNone, optionWait};
        final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
            JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[2]);
        final JDialog dialog = new JDialog((Frame) getParent(), "Select units to scramble to " + scrambleTo.getName());
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setLocationRelativeTo(getParent());
        dialog.setAlwaysOnTop(true);
        dialog.pack();
        dialog.setVisible(true);
        dialog.requestFocusInWindow();
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
          @Override
          public void propertyChange(final PropertyChangeEvent e) {
            if (!dialog.isVisible()) {
              return;
            }
            final String option = ((String) optionPane.getValue());
            if (option.equals(optionNone)) {
              choosers.clear();
              selection.clear();
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              continueLatch.countDown();
            } else if (option.equals(optionScramble)) {
              for (final Tuple<Territory, UnitChooser> terrChooser : choosers) {
                selection.put(terrChooser.getFirst(), terrChooser.getSecond().getSelected());
              }
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              continueLatch.countDown();
            } else {
              choosers.clear();
              selection.clear();
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              ThreadUtil.sleep(500);
              run();
            }
          }
        });
      }
    });
    mapPanel.getUIContext().addShutdownLatch(continueLatch);
    try {
      continueLatch.await();
    } catch (final InterruptedException ex) {
      // ignore interrupted exception
    } finally {
      mapPanel.getUIContext().removeShutdownLatch(continueLatch);
    }
    return selection;
  }

  public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible,
      final String message) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Should not be called from dispatch thread");
    }
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Collection<Unit> selection = new ArrayList<>();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mapPanel.centerOn(current);
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
        final UnitChooser chooser =
            new UnitChooser(possible, Collections.emptyMap(), data, false, uiContext);
        chooser.setMaxAndShowMaxButton(maxAllowed);
        panelChooser.add(chooser);
        chooserScrollPane = new JScrollPane(panelChooser);
        panel.add(chooserScrollPane, BorderLayout.CENTER);
        final String optionSelect = "Select";
        final String optionNone = "None";
        final String optionWait = "Wait";
        final Object[] options = {optionSelect, optionNone, optionWait};
        final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
            JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[2]);
        final JDialog dialog = new JDialog((Frame) getParent(), message);
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setLocationRelativeTo(getParent());
        dialog.setAlwaysOnTop(true);
        dialog.pack();
        dialog.setVisible(true);
        dialog.requestFocusInWindow();
        optionPane.addPropertyChangeListener(new PropertyChangeListener() {
          @Override
          public void propertyChange(final PropertyChangeEvent e) {
            if (!dialog.isVisible()) {
              return;
            }
            final String option = ((String) optionPane.getValue());
            if (option.equals(optionNone)) {
              selection.clear();
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              continueLatch.countDown();
            } else if (option.equals(optionSelect)) {
              selection.addAll(chooser.getSelected());
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              continueLatch.countDown();
            } else {
              selection.clear();
              dialog.setVisible(false);
              dialog.removeAll();
              dialog.dispose();
              ThreadUtil.sleep(500);
              run();
            }
          }
        });
      }
    });
    mapPanel.getUIContext().addShutdownLatch(continueLatch);
    try {
      continueLatch.await();
    } catch (final InterruptedException ex) {
      ex.printStackTrace();
    } finally {
      mapPanel.getUIContext().removeShutdownLatch(continueLatch);
    }
    return selection;
  }

  public PoliticalActionAttachment getPoliticalActionChoice(final PlayerID player, final boolean firstRun,
      final IPoliticsDelegate iPoliticsDelegate) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToPolitics(player);
    requestWindowFocus();
    return actionButtons.waitForPoliticalAction(firstRun, iPoliticsDelegate);
  }

  public UserActionAttachment getUserActionChoice(final PlayerID player, final boolean firstRun,
      final IUserActionDelegate iUserActionDelegate) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToUserActions(player);
    requestWindowFocus();
    return actionButtons.waitForUserActionAction(firstRun, iUserActionDelegate);
  }

  public TechRoll getTechRolls(final PlayerID id) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToTech(id);
    // workaround for panel not receiving focus at beginning of tech phase
    requestWindowFocus();
    return actionButtons.waitForTech();
  }

  public Territory getRocketAttack(final Collection<Territory> candidates, final Territory from) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    mapPanel.centerOn(from);
    final AtomicReference<Territory> selected = new AtomicReference<>();
    SwingAction.invokeAndWait(() -> {
      final JList<Territory> list = new JList<>(new Vector<>(candidates));
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.setSelectedIndex(0);
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      final JScrollPane scroll = new JScrollPane(list);
      panel.add(scroll, BorderLayout.CENTER);
      if (from != null) {
        panel.add(BorderLayout.NORTH, new JLabel("Targets for rocket in " + from.getName()));
      }
      final String[] options = {"OK", "Dont attack"};
      final String message = "Select Rocket Target";
      final int selection = JOptionPane.showOptionDialog(TripleAFrame.this, panel, message,
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null);
      if (selection == 0) {
        selected.set(list.getSelectedValue());
      }
    });
    return selected.get();
  }

  public static int save(final String filename, final GameData m_data) {
    try (FileOutputStream fos = new FileOutputStream(filename);
        ObjectOutputStream oos = new ObjectOutputStream(fos);) {
      oos.writeObject(m_data);
      return 0;
    } catch (final Throwable t) {
      System.err.println(t.getMessage());
      return -1;
    }
  }

  GameStepListener m_stepListener = new GameStepListener() {
    @Override
    public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player,
        final int round, final String stepDisplayName) {
      updateStep();
    }
  };

  private void updateStep() {
    final IUIContext context = uiContext;
    if (context == null || context.isShutDown()) {
      return;
    }
    data.acquireReadLock();
    try {
      if (data.getSequence().getStep() == null) {
        return;
      }
    } finally {
      data.releaseReadLock();
    }
    // we need to invoke and wait here since
    // if we switch to the history as a result of a history
    // change, we need to ensure that no further history
    // events are run until our historySynchronizer is set up
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingAction.invokeAndWait(() -> updateStep());
      return;
    }
    int round;
    String stepDisplayName;
    PlayerID player;
    data.acquireReadLock();
    try {
      round = data.getSequence().getRound();
      stepDisplayName = data.getSequence().getStep().getDisplayName();
      player = data.getSequence().getStep().getPlayerID();
    } finally {
      data.releaseReadLock();
    }
    this.round.setText("Round:" + round + " ");
    step.setText(stepDisplayName);
    final boolean isPlaying = localPlayers.playing(player);
    if (player != null) {
      this.player.setText((isPlaying ? "" : "REMOTE: ") + player.getName());
    }
    if (player != null && !player.isNull()) {
      this.round.setIcon(new ImageIcon(uiContext.getFlagImageFactory().getFlag(player)));
      lastStepPlayer = currentStepPlayer;
      currentStepPlayer = player;
    }
    // if the game control has passed to someone else and we are not just showing the map
    // show the history
    if (player != null && !player.isNull()) {
      if (isPlaying) {
        if (inHistory) {
          requiredTurnSeries.put(player, true);
          // if the game control is with us
          // show the current game
          showGame();
          // System.out.println("Changing step to " + stepDisplayName + " for " + player.getName());
        }
      } else {
        if (!inHistory && !uiContext.getShowMapOnly()) {
          if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("We should be in dispatch thread");
          }
          showHistory();
        }
      }
    }
  }

  public void requiredTurnSeries(final PlayerID player) {
    if (player == null) {
      return;
    }
    ThreadUtil.sleep(300);
    SwingAction.invokeAndWait(() -> {
      final Boolean play = requiredTurnSeries.get(player);
      if (play != null && play.booleanValue()) {
        ClipPlayer.play(SoundPath.CLIP_REQUIRED_YOUR_TURN_SERIES, player);
        requiredTurnSeries.put(player, false);
      }
      // center on capital of player, if it is a new player
      if (!player.equals(lastStepPlayer)) {
        lastStepPlayer = player;
        data.acquireReadLock();
        try {
          mapPanel.centerOn(TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data));
        } finally {
          data.releaseReadLock();
        }
      }
    });
  }

  GameDataChangeListener m_dataChangeListener = new GameDataChangeListener() {
    @Override
    public void gameDataChanged(final Change change) {
      try {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (uiContext == null) {
              return;
            }
            if (getEditMode()) {
              if (tabsPanel.indexOfComponent(editPanel) == -1) {
                showEditMode();
              }
            } else {
              if (tabsPanel.indexOfComponent(editPanel) != -1) {
                hideEditMode();
              }
            }
            if (uiContext.getShowMapOnly()) {
              hideRightHandSidePanel();
              // display troop movement
              final HistoryNode node = data.getHistory().getLastNode();
              if (node instanceof Renderable) {
                final Object details = ((Renderable) node).getRenderingData();
                if (details instanceof MoveDescription) {
                  final MoveDescription moveMessage = (MoveDescription) details;
                  final Route route = moveMessage.getRoute();
                  mapPanel.setRoute(null);
                  mapPanel.setRoute(route);
                  final Territory terr = route.getEnd();
                  if (!mapPanel.isShowing(terr)) {
                    mapPanel.centerOn(terr);
                  }
                }
              }
            } else {
              showRightHandSidePanel();
            }
          }
        });
      } catch (final Exception e) {
        ClientLogger.logQuietly(e);
      }
    }
  };

  private KeyListener getArrowKeyListener() {
    return new KeyListener() {
      @Override
      public void keyPressed(final KeyEvent e) {

        // scroll map according to wasd/arrowkeys
        final int diffPixel = computeScrollSpeed(e);
        final int x = mapPanel.getXOffset();
        final int y = mapPanel.getYOffset();
        final int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
          getMapPanel().setTopLeft(x + diffPixel, y);
        } else if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
          getMapPanel().setTopLeft(x - diffPixel, y);
        } else if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
          getMapPanel().setTopLeft(x, y + diffPixel);
        } else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
          getMapPanel().setTopLeft(x, y - diffPixel);
        }
        // I for info
        if (keyCode == KeyEvent.VK_I || keyCode == KeyEvent.VK_V) {
          String unitInfo = "";
          if (unitsBeingMousedOver != null && !unitsBeingMousedOver.isEmpty()) {
            final Unit unit = unitsBeingMousedOver.get(0);
            final UnitAttachment ua = UnitAttachment.get(unit.getType());
            if (ua != null) {
              unitInfo = "<b>Unit:</b><br>" + unit.getType().getName() + ": "
                  + ua.toStringShortAndOnlyImportantDifferences(unit.getOwner(), true, false);
            }
          }
          String terrInfo = "";
          if (territoryLastEntered != null) {
            final TerritoryAttachment ta = TerritoryAttachment.get(territoryLastEntered);
            if (ta != null) {
              terrInfo = "<b>Territory:</b><br>" + ta.toStringForInfo(true, true) + "<br>";
            } else {
              terrInfo = "<b>Territory:</b><br>" + territoryLastEntered.getName() + "<br>Water Territory";
            }
          }
          String tipText = unitInfo;
          if (unitInfo.length() > 0 && terrInfo.length() > 0) {
            tipText = tipText + "<br><br><br><br><br>";
          }
          tipText = tipText + terrInfo;
          if (tipText.length() > 0) {
            final Point currentPoint = MouseInfo.getPointerInfo().getLocation();
            final PopupFactory popupFactory = PopupFactory.getSharedInstance();
            final JToolTip info = new JToolTip();
            info.setTipText("<html>" + tipText + "</html>");
            final Popup popup = popupFactory.getPopup(mapPanel, info, currentPoint.x, currentPoint.y);
            popup.show();
            final Runnable disposePopup = new Runnable() {
              @Override
              public void run() {
                ThreadUtil.sleep(5000);
                popup.hide();
              }
            };
            new Thread(disposePopup, "popup waiter").start();
          }
        }
        // and then we do stuff for any custom current action tab
        actionButtons.keyPressed(e);
      }

      @Override
      public void keyTyped(final KeyEvent e) {}

      @Override
      public void keyReleased(final KeyEvent e) {}
    };
  }

  private static int computeScrollSpeed(final KeyEvent e) {
    int multiplier = 1;

    if (e.isControlDown()) {
      multiplier = 4;
    }

    final int starterDiffPixel = 70;
    return (starterDiffPixel * multiplier);
  }

  private void showEditMode() {
    tabsPanel.addTab("Edit", editPanel);
    if (editDelegate != null) {
      tabsPanel.setSelectedComponent(editPanel);
    }
    editModeButtonModel.setSelected(true);
    getGlassPane().setVisible(true);
  }

  private void hideEditMode() {
    if (tabsPanel.getSelectedComponent() == editPanel) {
      tabsPanel.setSelectedIndex(0);
    }
    tabsPanel.remove(editPanel);
    editModeButtonModel.setSelected(false);
    getGlassPane().setVisible(false);
  }

  public void showActionPanelTab() {
    tabsPanel.setSelectedIndex(0);
  }

  public void showRightHandSidePanel() {
    rightHandSidePanel.setVisible(true);
  }

  public void hideRightHandSidePanel() {
    rightHandSidePanel.setVisible(false);
  }

  public HistoryPanel getHistoryPanel() {
    return historyPanel;
  }

  private void showHistory() {
    inHistory = true;
    inGame = false;
    setWidgetActivation();
    final GameData clonedGameData;
    data.acquireReadLock();
    try {
      // we want to use a clone of the data, so we can make changes to it
      // as we walk up and down the history
      clonedGameData = GameDataUtils.cloneGameData(data);
      if (clonedGameData == null) {
        return;
      }
      data.removeDataChangeListener(m_dataChangeListener);
      clonedGameData.testLocksOnRead();
      if (historySyncher != null) {
        throw new IllegalStateException("Two history synchers?");
      }
      historySyncher = new HistorySynchronizer(clonedGameData, game);
      clonedGameData.addDataChangeListener(m_dataChangeListener);
    } finally {
      data.releaseReadLock();
    }
    statsPanel.setGameData(clonedGameData);
    economyPanel.setGameData(clonedGameData);
    if (objectivePanel != null && !objectivePanel.isEmpty()) {
      objectivePanel.setGameData(clonedGameData);
    }
    details.setGameData(clonedGameData);
    mapPanel.setGameData(clonedGameData);
    final HistoryDetailsPanel historyDetailPanel = new HistoryDetailsPanel(clonedGameData, mapPanel);
    tabsPanel.removeAll();
    tabsPanel.add("History", historyDetailPanel);
    tabsPanel.add("Stats", statsPanel);
    tabsPanel.add("Economy", economyPanel);
    if (objectivePanel != null && !objectivePanel.isEmpty()) {
      tabsPanel.add(objectivePanel.getName(), objectivePanel);
    }
    tabsPanel.add("Notes", notesPanel);
    tabsPanel.add("Territory", details);
    if (getEditMode()) {
      tabsPanel.add("Edit", editPanel);
    }
    if (actionButtons.getCurrent() != null) {
      actionButtons.getCurrent().setActive(false);
    }
    historyComponent.removeAll();
    historyComponent.setLayout(new BorderLayout());
    // create history tree context menu
    // actions need to clear the history panel popup state when done
    final JPopupMenu popup = new JPopupMenu();
    popup.add(new AbstractAction("Show Summary Log") {
      private static final long serialVersionUID = -6730966512179268157L;

      @Override
      public void actionPerformed(final ActionEvent ae) {
        final HistoryLog historyLog = new HistoryLog();
        historyLog.printRemainingTurn(historyPanel.getCurrentPopupNode(), false, data.getDiceSides(), null);
        historyLog.printTerritorySummary(historyPanel.getCurrentPopupNode(), clonedGameData);
        historyLog.printProductionSummary(clonedGameData);
        historyPanel.clearCurrentPopupNode();
        historyLog.setVisible(true);
      }
    });
    popup.add(new AbstractAction("Show Detailed Log") {
      private static final long serialVersionUID = -8709762764495294671L;

      @Override
      public void actionPerformed(final ActionEvent ae) {
        final HistoryLog historyLog = new HistoryLog();
        historyLog.printRemainingTurn(historyPanel.getCurrentPopupNode(), true, data.getDiceSides(), null);
        historyLog.printTerritorySummary(historyPanel.getCurrentPopupNode(), clonedGameData);
        historyLog.printProductionSummary(clonedGameData);
        historyPanel.clearCurrentPopupNode();
        historyLog.setVisible(true);
      }
    });
    popup.add(new AbstractAction("Save Screenshot") {
      private static final long serialVersionUID = 1222760138263428443L;

      @Override
      public void actionPerformed(final ActionEvent ae) {
        saveScreenshot(historyPanel.getCurrentPopupNode());
        historyPanel.clearCurrentPopupNode();
      }
    });
    popup.add(new AbstractAction("Save Game at this point (BETA)") {
      private static final long serialVersionUID = 1430512376199927896L;

      @Override
      public void actionPerformed(final ActionEvent ae) {
        JOptionPane.showMessageDialog(TripleAFrame.this,
            "Please first left click on the spot you want to save from, Then right click and select 'Save Game From History'"
                + "\n\nIt is recommended that when saving the game from the History panel:"
                + "\n * Your CURRENT GAME is at the start of some player's turn, and that no moves have been made and no actions taken yet."
                + "\n * The point in HISTORY that you are trying to save at, is at the beginning of a player's turn, or the beginning of a round."
                + "\nSaving at any other point, could potentially create errors."
                + "\nFor example, saving while your current game is in the middle of a move or battle phase will always create errors in the savegame."
                + "\nAnd you will also get errors in the savegame if you try to create a save at a point in history such as a move or battle phase.",
            "Save Game from History", JOptionPane.INFORMATION_MESSAGE);
        data.acquireReadLock();
        // m_data.acquireWriteLock();
        try {
          final File f = BasicGameMenuBar.getSaveGameLocationDialog(TripleAFrame.this);
          if (f != null) {
            try (FileOutputStream fout = new FileOutputStream(f)) {
              final GameData datacopy = GameDataUtils.cloneGameData(data, true);
              datacopy.getHistory().gotoNode(historyPanel.getCurrentPopupNode());
              datacopy.getHistory().removeAllHistoryAfterNode(historyPanel.getCurrentPopupNode());
              // TODO: the saved current delegate is still the current delegate,
              // rather than the delegate at that history popup node
              // TODO: it still shows the current round number, rather than the round at the history popup node
              // TODO: this could be solved easily if rounds/steps were changes,
              // but that could greatly increase the file size :(
              // TODO: this also does not undo the runcount of each delegate step
              @SuppressWarnings("unchecked")
              final Enumeration<HistoryNode> enumeration =
                  ((DefaultMutableTreeNode) datacopy.getHistory().getRoot()).preorderEnumeration();
              enumeration.nextElement();
              int round = 0;
              String stepDisplayName = datacopy.getSequence().getStep(0).getDisplayName();
              PlayerID currentPlayer = datacopy.getSequence().getStep(0).getPlayerID();
              while (enumeration.hasMoreElements()) {
                final HistoryNode node = enumeration.nextElement();
                if (node instanceof Round) {
                  round = Math.max(0, ((Round) node).getRoundNo() - datacopy.getSequence().getRoundOffset());
                  currentPlayer = null;
                  stepDisplayName = node.getTitle();
                } else if (node instanceof Step) {
                  currentPlayer = ((Step) node).getPlayerID();
                  stepDisplayName = node.getTitle();
                }
              }
              datacopy.getSequence().setRoundAndStep(round, stepDisplayName, currentPlayer);
              new GameDataManager().saveGame(fout, datacopy);
              JOptionPane.showMessageDialog(TripleAFrame.this, "Game Saved", "Game Saved",
                  JOptionPane.INFORMATION_MESSAGE);
            } catch (final IOException e) {
              ClientLogger.logQuietly(e);
            }
          }
        } finally {
          data.releaseReadLock();
        }
        historyPanel.clearCurrentPopupNode();
      }
    });
    final JSplitPane split = new JSplitPane();
    split.setOneTouchExpandable(true);
    split.setDividerSize(8);
    historyPanel = new HistoryPanel(clonedGameData, historyDetailPanel, popup, uiContext);
    split.setLeftComponent(historyPanel);
    split.setRightComponent(gameCenterPanel);
    split.setDividerLocation(150);
    historyComponent.add(split, BorderLayout.CENTER);
    historyComponent.add(gameSouthPanel, BorderLayout.SOUTH);
    getContentPane().removeAll();
    getContentPane().add(historyComponent, BorderLayout.CENTER);
    validate();
  }

  public void showGame() {
    inGame = true;
    uiContext.setShowMapOnly(false);
    // Are we coming from showHistory mode or showMapOnly mode?
    if (inHistory) {
      inHistory = false;
      if (historySyncher != null) {
        historySyncher.deactivate();
        historySyncher = null;
      }
      historyPanel.goToEnd();
      historyPanel = null;
      mapPanel.getData().removeDataChangeListener(m_dataChangeListener);
      statsPanel.setGameData(data);
      economyPanel.setGameData(data);
      if (objectivePanel != null && !objectivePanel.isEmpty()) {
        objectivePanel.setGameData(data);
      }
      details.setGameData(data);
      mapPanel.setGameData(data);
      data.addDataChangeListener(m_dataChangeListener);
      tabsPanel.removeAll();
    }
    setWidgetActivation();
    tabsPanel.add("Action", actionButtons);
    tabsPanel.add("Stats", statsPanel);
    tabsPanel.add("Economy", economyPanel);
    if (objectivePanel != null && !objectivePanel.isEmpty()) {
      tabsPanel.add(objectivePanel.getName(), objectivePanel);
    }
    tabsPanel.add("Notes", notesPanel);
    tabsPanel.add("Territory", details);
    if (getEditMode()) {
      tabsPanel.add("Edit", editPanel);
    }
    if (actionButtons.getCurrent() != null) {
      actionButtons.getCurrent().setActive(true);
    }
    gameMainPanel.removeAll();
    gameMainPanel.setLayout(new BorderLayout());
    gameMainPanel.add(gameCenterPanel, BorderLayout.CENTER);
    gameMainPanel.add(gameSouthPanel, BorderLayout.SOUTH);
    getContentPane().removeAll();
    getContentPane().add(gameMainPanel, BorderLayout.CENTER);
    mapPanel.setRoute(null);
    validate();
  }

  public void showMapOnly() {
    // Are we coming from showHistory mode or showGame mode?
    if (inHistory) {
      inHistory = false;
      if (historySyncher != null) {
        historySyncher.deactivate();
        historySyncher = null;
      }
      historyPanel.goToEnd();
      historyPanel = null;
      mapPanel.getData().removeDataChangeListener(m_dataChangeListener);
      mapPanel.setGameData(data);
      data.addDataChangeListener(m_dataChangeListener);
      gameMainPanel.removeAll();
      gameMainPanel.setLayout(new BorderLayout());
      gameMainPanel.add(mapAndChatPanel, BorderLayout.CENTER);
      gameMainPanel.add(rightHandSidePanel, BorderLayout.EAST);
      gameMainPanel.add(gameSouthPanel, BorderLayout.SOUTH);
      getContentPane().removeAll();
      getContentPane().add(gameMainPanel, BorderLayout.CENTER);
      mapPanel.setRoute(null);
    } else {
      inGame = false;
    }
    uiContext.setShowMapOnly(true);
    setWidgetActivation();
    validate();
  }

  public boolean saveScreenshot(final HistoryNode node, final File file) {
    // get current history node. if we are in history view, get the selected node.
    final MapPanel mapPanel = getMapPanel();
    boolean retval = true;
    // get round/step/player from history tree
    int round = 0;
    final Object[] pathFromRoot = node.getPath();
    for (final Object pathNode : pathFromRoot) {
      final HistoryNode curNode = (HistoryNode) pathNode;
      if (curNode instanceof Round) {
        round = ((Round) curNode).getRoundNo();
      }
    }
    final double scale = uiContext.getScale();
    // print map panel to image
    final BufferedImage mapImage =
        Util.createImage((int) (scale * mapPanel.getImageWidth()), (int) (scale * mapPanel.getImageHeight()), false);
    final Graphics2D mapGraphics = mapImage.createGraphics();
    try {
      final GameData data = mapPanel.getData();
      data.acquireReadLock();
      try {
        // workaround to get the whole map
        // (otherwise the map is cut if current window is not on top of map)
        final int xOffset = mapPanel.getXOffset();
        final int yOffset = mapPanel.getYOffset();
        mapPanel.setTopLeft(0, 0);
        mapPanel.print(mapGraphics);
        mapPanel.setTopLeft(xOffset, yOffset);
      } finally {
        data.releaseReadLock();
      }
      // overlay title
      Color title_color = uiContext.getMapData().getColorProperty(MapData.PROPERTY_SCREENSHOT_TITLE_COLOR);
      if (title_color == null) {
        title_color = Color.BLACK;
      }
      final String s_title_x = uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_X);
      final String s_title_y = uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_Y);
      final String s_title_size = uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_TITLE_FONT_SIZE);
      int title_x;
      int title_y;
      int title_size;
      try {
        title_x = (int) (Integer.parseInt(s_title_x) * scale);
        title_y = (int) (Integer.parseInt(s_title_y) * scale);
        title_size = Integer.parseInt(s_title_size);
      } catch (final NumberFormatException nfe) {
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
      if (uiContext.getMapData().getBooleanProperty(MapData.PROPERTY_SCREENSHOT_TITLE_ENABLED)) {
        mapGraphics.drawString(data.getGameName() + " Round " + round, title_x, title_y);
      }
      // overlay stats, if enabled
      final boolean stats_enabled =
          uiContext.getMapData().getBooleanProperty(MapData.PROPERTY_SCREENSHOT_STATS_ENABLED);
      if (stats_enabled) {
        // get screenshot properties from map data
        Color stats_text_color =
            uiContext.getMapData().getColorProperty(MapData.PROPERTY_SCREENSHOT_STATS_TEXT_COLOR);
        if (stats_text_color == null) {
          stats_text_color = Color.BLACK;
        }
        Color stats_border_color =
            uiContext.getMapData().getColorProperty(MapData.PROPERTY_SCREENSHOT_STATS_BORDER_COLOR);
        if (stats_border_color == null) {
          stats_border_color = Color.WHITE;
        }
        final String s_stats_x = uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_STATS_X);
        final String s_stats_y = uiContext.getMapData().getProperty(MapData.PROPERTY_SCREENSHOT_STATS_Y);
        int stats_x;
        int stats_y;
        try {
          stats_x = (int) (Integer.parseInt(s_stats_x) * scale);
          stats_y = (int) (Integer.parseInt(s_stats_y) * scale);
        } catch (final NumberFormatException nfe) {
          // choose reasonable defaults
          stats_x = (int) (120 * scale);
          stats_y = (int) (70 * scale);
        }
        // Fetch stats table and save current properties before modifying them
        // NOTE: This is a bit of a hack, but creating a fresh JTable and
        // populating it with statsPanel data seemed hard. This was easier.
        final JTable table = statsPanel.getStatsTable();
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
        // use tableWidth not hdrWidth!
        final int hdrWidth = tableWidth;
        final int hdrHeight = table.getTableHeader().getSize().height;
        // create image for capturing table header
        final BufferedImage tblHdrImage = Util.createImage(hdrWidth, hdrHeight, false);
        final Graphics2D tblHdrGraphics = tblHdrImage.createGraphics();
        // create image for capturing table (support transparencies)
        final BufferedImage tblImage = Util.createImage(tableWidth, tableHeight, true);
        final Graphics2D tblGraphics = tblImage.createGraphics();
        // create a custom renderer that paints selected cells transparently
        final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
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
        statsPanel.setStatsBgImage(null);
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
      try {
        ImageIO.write(mapImage, "png", file);
      } catch (final Exception e2) {
        e2.printStackTrace();
        JOptionPane.showMessageDialog(TripleAFrame.this, e2.getMessage(), "Error saving Screenshot",
            JOptionPane.OK_OPTION);
        retval = false;
      }
      // Clean up objects. There might be some overkill here,
      // but there were memory leaks that are fixed by some/all of these.
    } finally {
      mapImage.flush();
      mapGraphics.dispose();
    }
    return retval;
  }

  public void saveScreenshot(final HistoryNode node) {
    final FileFilter pngFilter = new FileFilter() {
      @Override
      public boolean accept(final File f) {
        if (f.isDirectory()) {
          return true;
        } else {
          return f.getName().endsWith(".png");
        }
      }

      @Override
      public String getDescription() {
        return "Saved Screenshots, *.png";
      }
    };
    final JFileChooser fileChooser = new SaveGameFileChooser();
    fileChooser.setFileFilter(pngFilter);
    final int rVal = fileChooser.showSaveDialog(this);
    if (rVal == JFileChooser.APPROVE_OPTION) {
      File f = fileChooser.getSelectedFile();
      if (!f.getName().toLowerCase().endsWith(".png")) {
        f = new File(f.getParent(), f.getName() + ".png");
      }
      // A small warning so users will not over-write a file,
      if (f.exists()) {
        final int choice =
            JOptionPane.showConfirmDialog(this, "A file by that name already exists. Do you wish to over write it?",
                "Over-write?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) {
          return;
        }
      }
      final File file = f;
      final Runnable t = new Runnable() {
        @Override
        public void run() {
          if (saveScreenshot(node, file)) {
            JOptionPane.showMessageDialog(TripleAFrame.this, "Screenshot Saved", "Screenshot Saved",
                JOptionPane.INFORMATION_MESSAGE);
          }
        }
      };
      SwingAction.invokeAndWait(t);
    }
  }

  private void setWidgetActivation() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          setWidgetActivation();
        }
      });
      return;
    }
    if (m_showHistoryAction != null) {
      m_showHistoryAction.setEnabled(!(inHistory || uiContext.getShowMapOnly()));
    }
    if (m_showGameAction != null) {
      m_showGameAction.setEnabled(!inGame);
    }
    if (m_showMapOnlyAction != null) {
      // We need to check and make sure there are no local human players
      boolean foundHuman = false;
      for (final IGamePlayer gamePlayer : localPlayers.getLocalPlayers()) {
        if (gamePlayer instanceof TripleAPlayer) {
          foundHuman = true;
        }
      }
      if (!foundHuman) {
        m_showMapOnlyAction.setEnabled(inGame || inHistory);
      } else {
        m_showMapOnlyAction.setEnabled(false);
      }
    }
    if (editModeButtonModel != null) {
      if (editDelegate == null || uiContext.getShowMapOnly()) {
        editModeButtonModel.setEnabled(false);
      } else {
        editModeButtonModel.setEnabled(true);
      }
    }
  }

  // setEditDelegate is called by TripleAPlayer at the start and end of a turn
  public void setEditDelegate(final IEditDelegate editDelegate) {
    this.editDelegate = editDelegate;
    // force a data change event to update the UI for edit mode
    m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    setWidgetActivation();
  }

  public IEditDelegate getEditDelegate() {
    return editDelegate;
  }

  public ButtonModel getEditModeButtonModel() {
    return editModeButtonModel;
  }

  public ButtonModel getShowCommentLogButtonModel() {
    return showCommentLogButtonModel;
  }

  public boolean getEditMode() {
    boolean isEditMode = false;
    // use GameData from mapPanel since it will follow current history node
    mapPanel.getData().acquireReadLock();
    try {
      isEditMode = BaseEditDelegate.getEditMode(mapPanel.getData());
    } finally {
      mapPanel.getData().releaseReadLock();
    }
    return isEditMode;
  }

  private AbstractAction m_showHistoryAction = new AbstractAction("Show history") {
    private static final long serialVersionUID = -3960551522512897374L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      showHistory();
      m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    }
  };
  private AbstractAction m_showGameAction = new AbstractAction("Show current game") {
    private static final long serialVersionUID = -7551760679570164254L;

    {
      setEnabled(false);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      showGame();
      m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    }
  };
  private AbstractAction m_showMapOnlyAction = new AbstractAction("Show map only") {
    private static final long serialVersionUID = -6621157075878333141L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      showMapOnly();
      m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    }
  };
  private final AbstractAction m_saveScreenshotAction = new AbstractAction("Export Screenshot...") {
    private static final long serialVersionUID = -5908032486008953815L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      HistoryNode curNode = null;
      if (historyPanel == null) {
        curNode = data.getHistory().getLastNode();
      } else {
        curNode = historyPanel.getCurrentNode();
      }
      saveScreenshot(curNode);
    }
  };

  public Collection<Unit> moveFightersToCarrier(final Collection<Unit> fighters, final Territory where) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    mapPanel.centerOn(where);
    final AtomicReference<ScrollableTextField> textRef = new AtomicReference<>();
    final AtomicReference<JPanel> panelRef = new AtomicReference<>();
    SwingAction.invokeAndWait(() -> {
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      final ScrollableTextField text = new ScrollableTextField(0, fighters.size());
      text.setBorder(new EmptyBorder(8, 8, 8, 8));
      panel.add(text, BorderLayout.CENTER);
      panel.add(new JLabel("How many fighters do you want to move from " + where.getName() + " to new carrier?"),
          BorderLayout.NORTH);
      panelRef.set(panel);
      textRef.set(text);
      panelRef.set(panel);
    });
    final int choice = EventThreadJOptionPane.showOptionDialog(this, panelRef.get(), "Place fighters on new carrier?",
        JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null, new String[] {"OK", "Cancel"}, "OK",
        getUIContext().getCountDownLatchHandler());
    if (choice == 0) {
      // arrayList.subList() is not serializable
      return new ArrayList<>(new ArrayList<>(fighters).subList(0, textRef.get().getValue()));
    } else {
      return new ArrayList<>(0);
    }
  }

  public BattlePanel getBattlePanel() {
    // m_messageAndDialogThreadPool.waitForAll();
    return actionButtons.getBattlePanel();
  }

  Action getShowGameAction() {
    return m_showGameAction;
  }

  Action getShowHistoryAction() {
    return m_showHistoryAction;
  }

  Action getShowMapOnlyAction() {
    return m_showMapOnlyAction;
  }

  Action getSaveScreenshotAction() {
    return m_saveScreenshotAction;
  }

  public IUIContext getUIContext() {
    return uiContext;
  }

  MapPanel getMapPanel() {
    return mapPanel;
  }

  @Override
  public JComponent getMainPanel() {
    return mapPanel;
  }

  // Beagle Code Called to Change Mapskin
  void updateMap(final String mapdir) throws IOException {
    uiContext.setMapDir(data, mapdir);
    // when changing skins, always show relief images
    if (uiContext.getMapData().getHasRelief()) {
      TileImageFactory.setShowReliefImages(true);
    }
    mapPanel.setGameData(data);
    // update mappanels to use new image
    mapPanel.changeImage(uiContext.getMapData().getMapDimensions());
    final Image small = uiContext.getMapImage().getSmallMapImage();
    smallView.changeImage(small);
    mapPanel.changeSmallMapOffscreenMap();
    // redraw territories
    mapPanel.resetMap();
  }

  @Override
  public IGame getGame() {
    return game;
  }

  public StatPanel getStatPanel() {
    return statsPanel;
  }

  @Override
  public void setShowChatTime(final boolean showTime) {
    chatPanel.setShowChatTime(showTime);
  }
}
