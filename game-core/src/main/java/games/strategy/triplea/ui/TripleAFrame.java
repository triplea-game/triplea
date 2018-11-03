package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;

import org.triplea.common.util.concurrent.CompletableFutureUtils;

import com.google.common.base.Preconditions;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.PlayerChatRenderer;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.DefaultNamed;
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
import games.strategy.engine.data.changefactory.ChangeFactory;
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
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.random.PbemDiceRoller;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.thread.ThreadPool;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.pro.ProAi;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import games.strategy.triplea.delegate.AirBattle;
import games.strategy.triplea.delegate.AirThatCantLandUtil;
import games.strategy.triplea.delegate.BaseEditDelegate;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.delegate.data.FightBattleDetails;
import games.strategy.triplea.delegate.data.MoveDescription;
import games.strategy.triplea.delegate.data.TechResults;
import games.strategy.triplea.delegate.data.TechRoll;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.export.ScreenshotExporter;
import games.strategy.triplea.ui.history.HistoryDetailsPanel;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.triplea.ui.menubar.HelpMenu;
import games.strategy.triplea.ui.menubar.TripleAMenuBar;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.ExitStatus;
import games.strategy.util.IntegerMap;
import games.strategy.util.Interruptibles;
import games.strategy.util.LocalizeHtml;
import games.strategy.util.Tuple;
import lombok.extern.java.Log;

/**
 * Main frame for the triple a game.
 */
@Log
public final class TripleAFrame extends JFrame {
  private static final long serialVersionUID = 7640069668264418976L;
  private final LocalPlayers localPlayers;
  private final GameData data;
  private final IGame game;
  private final MapPanel mapPanel;
  private final MapPanelSmallView smallView;
  private final JPanel territoryInfo = new JPanel();
  private final JLabel message = new JLabel("No selection");
  private final ResourceBar resourceBar;
  private final JLabel status = new JLabel("");
  private final JLabel step = new JLabel("xxxxxx");
  private final JLabel round = new JLabel("xxxxxx");
  private final JLabel player = new JLabel("xxxxxx");
  private final ActionButtons actionButtons;
  private final JPanel gameMainPanel = new JPanel();
  private final JPanel rightHandSidePanel = new JPanel();
  private final JTabbedPane tabsPanel = new JTabbedPane();
  private final StatPanel statsPanel;
  private final EconomyPanel economyPanel;
  private ObjectivePanel objectivePanel;
  private final NotesPanel notesPanel;
  private final TerritoryDetailPanel details;
  private final JPanel historyComponent = new JPanel();
  private final JPanel gameSouthPanel;
  private HistoryPanel historyPanel;
  private boolean inHistory = false;
  private boolean inGame = true;
  private HistorySynchronizer historySyncher;
  private final UiContext uiContext;
  private final JPanel mapAndChatPanel;
  private final ChatPanel chatPanel;
  private final CommentPanel commentPanel;
  private final JSplitPane chatSplit;
  private JSplitPane commentSplit;
  private final EditPanel editPanel;
  private final ButtonModel editModeButtonModel;
  private final ButtonModel showCommentLogButtonModel;
  private IEditDelegate editDelegate;
  private final JSplitPane gameCenterPanel;
  private Territory territoryLastEntered;
  private List<Unit> unitsBeingMousedOver;
  private PlayerID lastStepPlayer;
  private PlayerID currentStepPlayer;
  private final Map<PlayerID, Boolean> requiredTurnSeries = new HashMap<>();
  private final ThreadPool messageAndDialogThreadPool = new ThreadPool(1);
  private final MapUnitTooltipManager tooltipManager;
  private boolean isCtrlPressed = false;

  /**
   * Constructs a new instance of a TripleAFrame, but executes required IO-Operations off the EDT.
   */
  public static TripleAFrame create(final IGame game, final LocalPlayers players, @Nullable final Chat chat) {
    Preconditions.checkState(!SwingUtilities.isEventDispatchThread(), "This method must not be called on the EDT");

    final UiContext uiContext = new HeadedUiContext();
    uiContext.setDefaultMapDir(game.getData());
    uiContext.getMapData().verify(game.getData());
    uiContext.setLocalPlayers(players);

    final TripleAFrame frame = Interruptibles.awaitResult(() -> SwingAction
        .invokeAndWaitResult(() -> new TripleAFrame(game, players, uiContext, chat))).result
            .orElseThrow(() -> new IllegalStateException("Error while instantiating TripleAFrame"));
    frame.updateStep();
    return frame;
  }

  private TripleAFrame(final IGame game, final LocalPlayers players,
      final UiContext uiContext, @Nullable final Chat chat) {
    super("TripleA - " + game.getData().getGameName());

    localPlayers = players;
    setIconImage(GameRunner.getGameIcon(this));
    // 200 size is pretty arbitrary, goal is to not allow users to shrink window down to nothing.
    setMinimumSize(new Dimension(200, 200));

    this.game = game;
    data = game.getData();
    addZoomKeyboardShortcuts();
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    final WindowListener windowListener = new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        leaveGame();
      }
    };
    this.addWindowListener(windowListener);
    this.uiContext = uiContext;
    this.setCursor(uiContext.getCursor());
    editModeButtonModel = new JToggleButton.ToggleButtonModel();
    editModeButtonModel.setEnabled(false);
    showCommentLogButtonModel = new JToggleButton.ToggleButtonModel();
    showCommentLogButtonModel.setSelected(false);
    showCommentLogButtonModel.addActionListener(e -> {
      if (showCommentLogButtonModel.isSelected()) {
        showCommentLog();
      } else {
        hideCommentLog();
      }
    });
    SwingUtilities.invokeLater(() -> this.setJMenuBar(new TripleAMenuBar(this)));
    final ImageScrollModel model = new ImageScrollModel();
    model.setScrollX(uiContext.getMapData().scrollWrapX());
    model.setScrollY(uiContext.getMapData().scrollWrapY());
    model.setMaxBounds(uiContext.getMapData().getMapDimensions().width,
        uiContext.getMapData().getMapDimensions().height);
    final Image small = uiContext.getMapImage().getSmallMapImage();
    smallView = new MapPanelSmallView(small, model, uiContext.getMapData());
    mapPanel = new MapPanel(data, smallView, uiContext, model, this::computeScrollSpeed);
    tooltipManager = new MapUnitTooltipManager(mapPanel);
    mapPanel.addMapSelectionListener(mapSelectionListener);
    mapPanel.addMouseOverUnitListener((units, territory) -> {
      unitsBeingMousedOver = units;
      tooltipManager.updateTooltip(getUnitInfo());
    });
    // link the small and large images
    SwingUtilities.invokeLater(mapPanel::initSmallMap);
    mapAndChatPanel = new JPanel();
    mapAndChatPanel.setLayout(new BorderLayout());
    commentPanel = new CommentPanel(this, data);
    chatSplit = new JSplitPane();
    chatSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
    chatSplit.setOneTouchExpandable(true);
    chatSplit.setDividerSize(8);
    chatSplit.setResizeWeight(0.95);
    if (chat != null) {
      commentSplit = new JSplitPane();
      commentSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
      commentSplit.setOneTouchExpandable(true);
      commentSplit.setDividerSize(8);
      commentSplit.setResizeWeight(0.5);
      commentSplit.setTopComponent(commentPanel);
      commentSplit.setBottomComponent(null);
      chatPanel = new ChatPanel(chat);
      chatPanel.setPlayerRenderer(new PlayerChatRenderer(this.game, uiContext));
      final Dimension chatPrefSize = new Dimension((int) chatPanel.getPreferredSize().getWidth(), 95);
      chatPanel.setPreferredSize(chatPrefSize);
      chatSplit.setTopComponent(mapPanel);
      chatSplit.setBottomComponent(chatPanel);
      mapAndChatPanel.add(chatSplit, BorderLayout.CENTER);
    } else {
      mapAndChatPanel.add(mapPanel, BorderLayout.CENTER);
      chatPanel = null;
    }
    gameMainPanel.setLayout(new BorderLayout());
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(gameMainPanel, BorderLayout.CENTER);

    gameSouthPanel = new JPanel();
    gameSouthPanel.setLayout(new BorderLayout());
    territoryInfo.setLayout(new GridBagLayout());
    territoryInfo.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    territoryInfo.setPreferredSize(new Dimension(0, 0));
    resourceBar = new ResourceBar(data, uiContext);
    message.setFont(message.getFont().deriveFont(Collections.singletonMap(
        TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD)));
    status.setPreferredSize(new Dimension(0, 0));
    status.setText("");

    final JPanel bottomMessagePanel = new JPanel();
    bottomMessagePanel.setLayout(new GridBagLayout());
    bottomMessagePanel.setBorder(BorderFactory.createEmptyBorder());
    bottomMessagePanel.add(resourceBar, new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    bottomMessagePanel.add(territoryInfo, new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    bottomMessagePanel.add(status, new GridBagConstraints(2, 0, 1, 1, 1, 1, GridBagConstraints.EAST,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    gameSouthPanel.add(bottomMessagePanel, BorderLayout.CENTER);
    status.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    final JPanel stepPanel = new JPanel();
    stepPanel.setLayout(new GridBagLayout());
    stepPanel.add(player, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));
    stepPanel.add(step, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));
    stepPanel.add(round, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));
    if (game.getRandomSource() instanceof PbemDiceRoller) {
      final JLabel diceServerLabel = new JLabel("Dice Server On");
      diceServerLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
      stepPanel.add(diceServerLabel, new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.EAST,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    }
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

    final MovePanel movePanel = new MovePanel(data, mapPanel, this);
    actionButtons = new ActionButtons(data, mapPanel, movePanel, this);

    SwingUtilities.invokeLater(() -> mapPanel.addKeyListener(getFullScreenListener()));
    SwingUtilities.invokeLater(() -> mapPanel.addKeyListener(getArrowKeyListener()));
    SwingUtilities.invokeLater(() -> mapPanel.addKeyListener(movePanel.getCustomKeyListeners()));
    SwingUtilities.invokeLater(() -> mapPanel.addKeyListener(getFlagToggleKeyListener(this)));

    addTab("Actions", actionButtons, 'A');
    actionButtons.setBorder(null);
    statsPanel = new StatPanel(data, uiContext);
    addTab("Players", statsPanel, 'P');
    economyPanel = new EconomyPanel(data, uiContext);
    addTab("Resources", economyPanel, 'R');
    objectivePanel = new ObjectivePanel(data);
    if (objectivePanel.isEmpty()) {
      objectivePanel.removeDataChangeListener();
      objectivePanel = null;
    } else {
      addTab(objectivePanel.getName(), objectivePanel, 'O');
    }
    notesPanel = new NotesPanel(HelpMenu.gameNotesPane);
    addTab("Notes", notesPanel, 'N');
    details = new TerritoryDetailPanel(mapPanel, data, uiContext, this);
    addTab("Territory", details, 'T');
    editPanel = new EditPanel(data, mapPanel, this);
    // Register a change listener
    tabsPanel.addChangeListener(evt -> {
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
        data.acquireReadLock();
        final PlayerID player1;
        try {
          player1 = data.getSequence().getStep().getPlayerId();
        } finally {
          data.releaseReadLock();
        }
        actionButtons.getCurrent().setActive(false);
        editPanel.display(player1);
      } else {
        actionButtons.getCurrent().setActive(true);
        editPanel.setActive(false);
      }
    });
    rightHandSidePanel.setPreferredSize(
        new Dimension((int) smallView.getPreferredSize().getWidth(), (int) mapPanel.getPreferredSize().getHeight()));
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
    dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    data.addDataChangeListener(dataChangeListener);
    game.addGameStepListener(stepListener);
    uiContext.addShutdownWindow(this);
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

  private static KeyListener getFlagToggleKeyListener(final TripleAFrame frame) {
    return new KeyListener() {
      private boolean blockInputs = false;
      private long timeSinceLastPressEvent = 0;
      private boolean running = true;

      @Override
      public void keyTyped(final KeyEvent e) {/* Do nothing */}

      @Override
      public void keyPressed(final KeyEvent e) {
        timeSinceLastPressEvent = 0;
        if (!blockInputs) {
          resetFlagsOnTimeOut(e.getKeyCode());
          toggleFlags(e.getKeyCode());
          blockInputs = true;
        }
      }

      private void resetFlagsOnTimeOut(final int keyCode) {
        new Thread(() -> {
          running = true;
          while (running) {
            timeSinceLastPressEvent++;
            if (timeSinceLastPressEvent > 5) {
              running = false;
              toggleFlags(keyCode);
              blockInputs = false;
            }
            Interruptibles.sleep(100);
          }
        }).start();
      }

      @Override
      public void keyReleased(final KeyEvent e) {
        toggleFlags(e.getKeyCode());
        blockInputs = false;
        running = false;
      }

      private void toggleFlags(final int keyCode) {
        if (keyCode == KeyEvent.VK_L) {
          UnitsDrawer.enabledFlags = !UnitsDrawer.enabledFlags;
          frame.getMapPanel().resetMap();
        }
      }
    };
  }

  private void addZoomKeyboardShortcuts() {
    SwingComponents.addKeyListenerWithMetaAndCtrlMasks(this, '=', () -> {
      if (getScale() < 100) {
        setScale(getScale() + 10);
      }
    });

    SwingComponents.addKeyListenerWithMetaAndCtrlMasks(this, '-', () -> {
      if (getScale() > 16) {
        setScale(getScale() - 10);
      }
    });
  }

  private void addTab(final String title, final Component component, final char hotkey) {
    tabsPanel.addTab(title, null, component, "Hotkey: CTRL+" + hotkey);
    SwingComponents.addKeyListenerWithMetaAndCtrlMasks(this, hotkey,
        () -> tabsPanel.setSelectedIndex((Arrays.asList(tabsPanel.getComponents())).indexOf(component)));
  }

  public LocalPlayers getLocalPlayers() {
    return localPlayers;
  }

  /**
   * Sets the map scale.
   *
   * @param value a number between 10 and 200.
   */
  public void setScale(final double value) {
    getMapPanel().setScale(value / 100);
  }

  /**
   * Returns a scale between 10 and 200.
   */
  private double getScale() {
    return getMapPanel().getScale() * 100;
  }

  /**
   * Stops the game and closes this frame window.
   */
  public void stopGame() {
    // we have already shut down
    if (uiContext == null) {
      return;
    }
    this.setVisible(false);
    TripleAFrame.this.dispose();
    if (SystemProperties.isMac()) {
      // this frame should not handle shutdowns anymore
      MacQuitMenuWrapper.unregisterShutdownHandler();
    }
    messageAndDialogThreadPool.shutdown();
    uiContext.shutDown();
    if (chatPanel != null) {
      chatPanel.setPlayerRenderer(null);
      chatPanel.setChat(null);
    }
    if (historySyncher != null) {
      historySyncher.deactivate();
      historySyncher = null;
    }
    ProAi.gameOverClearCache();
  }

  /**
   * Prompts the user if they wish to exit the application. If they answer yes, the game will be stopped, and the
   * process will be terminated.
   */
  public void shutdown() {
    final int selectedOption = EventThreadJOptionPane.showConfirmDialog(this,
        "Are you sure you want to exit TripleA?\nUnsaved game data will be lost.", "Exit Program",
        JOptionPane.YES_NO_OPTION, getUiContext().getCountDownLatchHandler());
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    stopGame();
    ExitStatus.SUCCESS.exit();
  }

  /**
   * Prompts the user if they wish to leave the game. If they answer yes, the game will be stopped, and the application
   * will return to the main menu.
   */
  public void leaveGame() {
    final int selectedOption = EventThreadJOptionPane.showConfirmDialog(this,
        "Are you sure you want to leave the current game?\nUnsaved game data will be lost.", "Leave Game",
        JOptionPane.YES_NO_OPTION, getUiContext().getCountDownLatchHandler());
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    if (game instanceof ServerGame) {
      ((ServerGame) game).stopGame();
    } else {
      game.getMessenger().shutDown();
      ((ClientGame) game).shutDown();
      // an ugly hack, we need a better
      // way to get the main frame
      new Thread(GameRunner::clientLeftGame).start();
    }
  }

  public final MapSelectionListener mapSelectionListener = new DefaultMapSelectionListener() {
    @Override
    public void mouseEntered(final Territory territory) {
      territoryLastEntered = territory;
      refresh();
    }

    void refresh() {
      territoryInfo.removeAll();

      message.setText((territoryLastEntered == null) ? "none" : territoryLastEntered.getName());

      // If territory is null or doesn't have an attachment then just display the name or "none"
      if (territoryLastEntered == null || TerritoryAttachment.get(territoryLastEntered) == null) {
        territoryInfo.add(message, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        territoryInfo.revalidate();
        territoryInfo.repaint();
        return;
      }

      // Display territory effects, territory name, and resources
      final TerritoryAttachment ta = TerritoryAttachment.get(territoryLastEntered);
      final List<TerritoryEffect> territoryEffects = ta.getTerritoryEffect();
      int count = 0;
      final StringBuilder territoryEffectText = new StringBuilder();
      for (final TerritoryEffect territoryEffect : territoryEffects) {
        try {
          final JLabel territoryEffectLabel = new JLabel();
          territoryEffectLabel.setIcon(uiContext.getTerritoryEffectImageFactory().getIcon(territoryEffect, false));
          territoryEffectLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
          territoryInfo.add(territoryEffectLabel,
              new GridBagConstraints(count++, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                  new Insets(0, 0, 0, 0), 0, 0));
        } catch (final IllegalStateException e) {
          territoryEffectText.append(territoryEffect.getName()).append(", ");
        }
      }

      territoryInfo.add(message, new GridBagConstraints(count++, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

      if (territoryEffectText.length() > 0) {
        territoryEffectText.setLength(territoryEffectText.length() - 2);
        final JLabel territoryEffectTextLabel = new JLabel();
        territoryEffectTextLabel.setText(" (" + territoryEffectText + ")");
        territoryInfo.add(territoryEffectTextLabel,
            new GridBagConstraints(count++, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
      }

      final int production = ta.getProduction();
      final ResourceCollection resourceCollection = ta.getResources();
      final IntegerMap<Resource> resources = new IntegerMap<>();
      if (production > 0) {
        resources.add(new Resource(Constants.PUS, data), production);
      }
      if (resourceCollection != null) {
        resources.add(resourceCollection.getResourcesCopy());
      }
      for (final Resource resource : resources.keySet()) {
        final JLabel resourceLabel =
            uiContext.getResourceImageFactory().getLabel(resource, resources);
        resourceLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        territoryInfo.add(resourceLabel,
            new GridBagConstraints(count++, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0), 0, 0));
      }
      territoryInfo.revalidate();
      territoryInfo.repaint();
    }
  };

  void clearStatusMessage() {
    status.setText("");
    status.setIcon(null);
  }

  public void setStatusErrorMessage(final String msg) {
    setStatus(msg, mapPanel.getErrorImage());
  }

  private void setStatus(final String msg, final Optional<Image> image) {
    status.setText(msg);

    if (!msg.isEmpty() && image.isPresent()) {
      status.setIcon(new ImageIcon(image.get()));
    } else {
      status.setIcon(null);
    }
  }

  public void setStatusWarningMessage(final String msg) {
    setStatus(msg, mapPanel.getWarningImage());
  }

  public IntegerMap<ProductionRule> getProduction(final PlayerID player, final boolean bid) {
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToProduce(player);
    return actionButtons.waitForPurchase(bid);
  }

  public Map<Unit, IntegerMap<RepairRule>> getRepair(final PlayerID player, final boolean bid,
      final Collection<PlayerID> allowedPlayersToRepair) {
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToRepair(player);
    return actionButtons.waitForRepair(bid, allowedPlayersToRepair);
  }

  public MoveDescription getMove(final PlayerID player, final IPlayerBridge bridge, final boolean nonCombat,
      final String stepName) {
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToMove(player, nonCombat, stepName);
    // workaround for panel not receiving focus at beginning of n/c move phase
    if (!getBattlePanel().getBattleFrame().isVisible()) {
      requestWindowFocus();
    }
    return actionButtons.waitForMove(bridge);
  }

  private void requestWindowFocus() {
    Interruptibles.await(() -> SwingAction.invokeAndWait(() -> {
      requestFocusInWindow();
      transferFocus();
    }));
  }

  public PlaceData waitForPlace(final PlayerID player, final boolean bid, final IPlayerBridge bridge) {
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToPlace(player);
    return actionButtons.waitForPlace(bid, bridge);
  }

  public void waitForMoveForumPoster(final PlayerID player, final IPlayerBridge bridge) {
    if (actionButtons == null) {
      return;
    }
    actionButtons.changeToMoveForumPosterPanel(player);
    actionButtons.waitForMoveForumPosterPanel(this, bridge);
  }

  public void waitForEndTurn(final PlayerID player, final IPlayerBridge bridge) {
    if (actionButtons == null) {
      return;
    }
    actionButtons.changeToEndTurn(player);
    actionButtons.waitForEndTurn(this, bridge);
  }

  public FightBattleDetails getBattle(final PlayerID player, final Map<BattleType, Collection<Territory>> battles) {
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToBattle(player, battles);
    return actionButtons.waitForBattleSelection();
  }

  /**
   * We do NOT want to block the next player from beginning their turn.
   */
  public void notifyError(final String message) {
    final String displayMessage = LocalizeHtml.localizeImgLinksInHtml(message);
    messageAndDialogThreadPool.submit(() -> EventThreadJOptionPane.showMessageDialogWithScrollPane(TripleAFrame.this,
        displayMessage, "Error", JOptionPane.ERROR_MESSAGE, getUiContext().getCountDownLatchHandler()));
  }

  /**
   * We do NOT want to block the next player from beginning their turn.
   */
  public void notifyMessage(final String message, final String title) {
    if (message == null || title == null) {
      return;
    }
    if (title.contains(AbstractConditionsAttachment.TRIGGER_CHANCE_FAILURE)
        && message.contains(AbstractConditionsAttachment.TRIGGER_CHANCE_FAILURE)
        && !getUiContext().getShowTriggerChanceFailure()) {
      return;
    }
    if (title.contains(AbstractConditionsAttachment.TRIGGER_CHANCE_SUCCESSFUL)
        && message.contains(AbstractConditionsAttachment.TRIGGER_CHANCE_SUCCESSFUL)
        && !getUiContext().getShowTriggerChanceSuccessful()) {
      return;
    }
    if (title.equals(AbstractTriggerAttachment.NOTIFICATION) && !getUiContext().getShowTriggeredNotifications()) {
      return;
    }
    if (title.contains(AbstractEndTurnDelegate.END_TURN_REPORT_STRING)
        && message.contains(AbstractEndTurnDelegate.END_TURN_REPORT_STRING)
        && !getUiContext().getShowEndOfTurnReport()) {
      return;
    }
    final String displayMessage = LocalizeHtml.localizeImgLinksInHtml(message);
    messageAndDialogThreadPool.submit(() -> EventThreadJOptionPane.showMessageDialogWithScrollPane(TripleAFrame.this,
        displayMessage, title, JOptionPane.INFORMATION_MESSAGE, getUiContext().getCountDownLatchHandler()));
  }

  /**
   * Prompts the user with a list of territories that have air units that either cannot land (movement action) or be
   * placed (placement action) in the territory. The user is asked whether they wish to end the current
   * movement/placement action, which will destroy the air units in the specified territories, or if they wish to
   * continue the current movement/placement action in order to possibly move/place those units in a different
   * territory.
   *
   * @param id The player performing the movement/placement action.
   * @param airCantLand The collection of territories that have air units that either cannot land or be placed in them.
   * @param movePhase {@code true} if a movement action is active; otherwise {@code false} if a placement action is
   *        active.
   *
   * @return {@code true} if the user wishes to end the current movement/placement action, and thus destroy the affected
   *         air units; otherwise {@code false} if the user wishes to continue the current movement/placement action.
   */
  public boolean getOkToLetAirDie(final PlayerID id, final Collection<Territory> airCantLand,
      final boolean movePhase) {
    if (airCantLand == null || airCantLand.isEmpty()) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final StringBuilder sb = new StringBuilder("<html>Air units cannot land in:<ul> ");
    for (final Territory t : airCantLand) {
      sb.append("<li>").append(t.getName()).append("</li>");
    }
    sb.append("</ul></html>");
    final boolean lhtrProd = AirThatCantLandUtil.isLhtrCarrierProduction(data)
        || AirThatCantLandUtil.isLandExistingFightersOnNewCarriers(data);
    final int carrierCount = GameStepPropertiesHelper.getCombinedTurns(data, id).stream()
        .map(PlayerID::getUnits)
        .map(units -> units.getMatches(Matches.unitIsCarrier()))
        .mapToInt(List::size)
        .sum();
    final boolean canProduceCarriersUnderFighter = lhtrProd && carrierCount != 0;
    if (canProduceCarriersUnderFighter && carrierCount > 0) {
      sb.append("\nYou have ").append(carrierCount).append(" ").append(MyFormatter.pluralize("carrier", carrierCount))
          .append(" on which planes can land");
    }
    final String ok = movePhase ? "End Move Phase" : "Kill Planes";
    final String cancel = movePhase ? "Keep Moving" : "Change Placement";
    final String[] options = {cancel, ok};
    mapPanel.centerOn(airCantLand.iterator().next());
    final int choice =
        EventThreadJOptionPane.showOptionDialog(this, sb.toString(), "Air cannot land", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE, null, options, cancel, getUiContext().getCountDownLatchHandler());
    return choice == JOptionPane.NO_OPTION;
  }

  /**
   * Prompts the user with a list of territories that have units that cannot fight in the territory. The user is asked
   * whether they wish to end the current movement action, which will destroy the units in the specified territories, or
   * if they wish to continue the current movement action in order to possibly move those units to a different
   * territory.
   *
   * @param unitsCantFight The collection of territories that have units that cannot fight.
   *
   * @return {@code true} if the user wishes to end the current movement action, and thus destroy the affected units;
   *         otherwise {@code false} if the user wishes to continue the current movement action.
   */
  public boolean getOkToLetUnitsDie(final Collection<Territory> unitsCantFight) {
    if (unitsCantFight == null || unitsCantFight.isEmpty()) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final String message = unitsCantFight.stream()
        .map(DefaultNamed::getName)
        .collect(Collectors.joining(" ", "Units in the following territories will die: ", ""));
    final String ok = "Done Moving";
    final String cancel = "Keep Moving";
    final String[] options = {cancel, ok};
    this.mapPanel.centerOn(unitsCantFight.iterator().next());
    final int choice = EventThreadJOptionPane.showOptionDialog(this, message,
        "Units cannot fight", JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE, null, options, cancel,
        getUiContext().getCountDownLatchHandler());
    return choice == JOptionPane.NO_OPTION;
  }

  public boolean acceptAction(final PlayerID playerSendingProposal, final String acceptanceQuestion,
      final boolean politics) {
    messageAndDialogThreadPool.waitForAll();
    final int choice = EventThreadJOptionPane.showConfirmDialog(this, acceptanceQuestion,
        "Accept " + (politics ? "Political " : "") + "Proposal from " + playerSendingProposal.getName() + "?",
        JOptionPane.YES_NO_OPTION, getUiContext().getCountDownLatchHandler());
    return choice == JOptionPane.YES_OPTION;
  }

  public boolean getOk(final String message) {
    messageAndDialogThreadPool.waitForAll();
    final int choice = EventThreadJOptionPane.showConfirmDialog(this, message, message, JOptionPane.OK_CANCEL_OPTION,
        getUiContext().getCountDownLatchHandler());
    return choice == JOptionPane.OK_OPTION;
  }

  /**
   * Displays a message to the user informing them of the results of rolling for technologies.
   */
  public void notifyTechResults(final TechResults msg) {
    final Supplier<TechResultsDisplay> action = () -> new TechResultsDisplay(msg, uiContext, data);
    messageAndDialogThreadPool
        .submit(() -> Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action)).result
            .ifPresent(display -> EventThreadJOptionPane.showOptionDialog(
                TripleAFrame.this,
                display,
                "Tech roll",
                JOptionPane.OK_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                new String[] {"OK"},
                "OK",
                getUiContext().getCountDownLatchHandler())));
  }

  /**
   * Prompts the user if the applicable air units in the specified territory are to perform a strategic bombing raid or
   * are to participate in the attack.
   *
   * @param location The territory under attack.
   *
   * @return {@code true} if the applicable air units will perform a strategic bombing raid in the specified territory;
   *         otherwise {@code false}.
   */
  public boolean getStrategicBombingRaid(final Territory location) {
    messageAndDialogThreadPool.waitForAll();
    final String message =
        (Properties.getRaidsMayBePreceededByAirBattles(data) ? "Bomb/Escort" : "Bomb") + " in "
            + location.getName();
    final String bomb =
        (Properties.getRaidsMayBePreceededByAirBattles(data) ? "Bomb/Escort" : "Bomb");
    final String normal = "Attack";
    final String[] choices = {bomb, normal};
    int choice = -1;
    while (choice < 0 || choice > 1) {
      choice = EventThreadJOptionPane.showOptionDialog(this, message, "Bomb?", JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.INFORMATION_MESSAGE, null, choices, bomb, getUiContext().getCountDownLatchHandler());
    }
    return choice == JOptionPane.OK_OPTION;
  }

  /**
   * Prompts the user to select a strategic bombing raid target in the specified territory.
   *
   * @param territory The territory under attack.
   * @param potentialTargets The potential bombing targets.
   * @param bombers The units participating in the strategic bombing raid.
   *
   * @return The selected target or {@code null} if no target was selected.
   */
  public @Nullable Unit getStrategicBombingRaidTarget(
      final Territory territory,
      final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    if (potentialTargets.size() == 1) {
      return potentialTargets.iterator().next();
    }
    messageAndDialogThreadPool.waitForAll();
    final AtomicReference<Unit> selected = new AtomicReference<>();
    final String message = "Select bombing target in " + territory.getName();
    final Supplier<Tuple<JPanel, JList<Unit>>> action = () -> {
      final JList<Unit> list = new JList<>(SwingComponents.newListModel(potentialTargets));
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.setSelectedIndex(0);
      list.setCellRenderer(new UnitRenderer());
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      if (bombers != null) {
        panel.add(new JLabel("For Units: " + MyFormatter.unitsToTextNoOwner(bombers)), BorderLayout.NORTH);
      }
      final JScrollPane scroll = new JScrollPane(list);
      panel.add(scroll, BorderLayout.CENTER);
      return Tuple.of(panel, list);
    };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action)).result
        .map(comps -> {
          final JPanel panel = comps.getFirst();
          final JList<?> list = comps.getSecond();
          final String[] options = {"OK", "Cancel"};
          final int selection = EventThreadJOptionPane.showOptionDialog(this, panel, message, JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE, null, options, null, getUiContext().getCountDownLatchHandler());
          if (selection == 0) {
            selected.set((Unit) list.getSelectedValue());
          }
          return selected.get();
        })
        .orElse(null);
  }

  /**
   * Create a unit option with icon and description.
   */
  private class UnitRenderer extends JLabel implements ListCellRenderer<Unit> {

    private static final long serialVersionUID = 1749164256040268579L;

    public UnitRenderer() {
      setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(final JList<? extends Unit> list, final Unit unit, final int index,
        final boolean isSelected, final boolean cellHasFocus) {

      setText(unit.toString() + ", damage=" + TripleAUnit.get(unit).getUnitDamage());
      final Optional<ImageIcon> icon = uiContext.getUnitImageFactory().getIcon(unit.getType(), unit.getOwner(),
          Matches.unitHasTakenSomeBombingUnitDamage().test(unit), Matches.unitIsDisabled().test(unit));
      icon.ifPresent(this::setIcon);
      setBorder(new EmptyBorder(0, 0, 0, 10));

      // Set selected option to highlighted color
      if (isSelected) {
        setBackground(list.getSelectionBackground());
        setForeground(list.getSelectionForeground());
      } else {
        setBackground(list.getBackground());
        setForeground(list.getForeground());
      }

      return this;
    }
  }

  /**
   * Prompts the user to select a collection of dice rolls. The user is presented with a dialog that allows them to
   * choose from all possible rolls for a die with the specified number of sides. Each roll is rendered depending on
   * whether it will result in a hit or a miss.
   *
   * @param numDice The number of dice the user is required to select.
   * @param hitAt The value at which the roll is considered a hit.
   * @param title The dialog title.
   * @param diceSides The number of sides on a die.
   *
   * @return The selected dice rolls.
   */
  public int[] selectFixedDice(final int numDice, final int hitAt, final String title, final int diceSides) {
    messageAndDialogThreadPool.waitForAll();
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(
        () -> new DiceChooser(getUiContext(), numDice, hitAt, diceSides))).result
            .map(chooser -> {
              do {
                EventThreadJOptionPane.showMessageDialog(null, chooser, title, JOptionPane.PLAIN_MESSAGE,
                    getUiContext().getCountDownLatchHandler());
              } while (chooser.getDice() == null);
              return chooser.getDice();
            })
            .orElseGet(() -> new int[numDice]);
  }

  /**
   * Prompts the user to select a territory in which to land an air unit.
   *
   * @param candidates The collection of territories from which the user may select.
   * @param currentTerritory The territory in which the air unit is currently located.
   * @param unitMessage An additional message to display to the user.
   *
   * @return The selected territory or {@code null} if no territory was selected.
   */
  public @Nullable Territory selectTerritoryForAirToLand(
      final Collection<Territory> candidates,
      final Territory currentTerritory,
      final String unitMessage) {
    if (candidates == null || candidates.isEmpty()) {
      return null;
    }
    if (candidates.size() == 1) {
      return candidates.iterator().next();
    }
    messageAndDialogThreadPool.waitForAll();
    final Supplier<Tuple<JPanel, JList<Territory>>> action = () -> {
      mapPanel.centerOn(currentTerritory);
      final JList<Territory> list = new JList<>(SwingComponents.newListModel(candidates));
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
    };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action)).result
        .map(comps -> {
          final JPanel panel = comps.getFirst();
          final JList<?> list = comps.getSecond();
          final String[] options = {"OK"};
          final String title =
              "Select territory for air units to land, current territory is " + currentTerritory.getName();
          EventThreadJOptionPane.showOptionDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE, null, options, null, getUiContext().getCountDownLatchHandler());
          return (Territory) list.getSelectedValue();
        })
        .orElse(null);
  }

  /**
   * Prompts the user to pick a territory and a collection of associated units.
   *
   * @param player The player making the selection.
   * @param territoryChoices The collection of territories from which the user may select.
   * @param unitChoices The collection of units from which the user may select.
   * @param unitsPerPick The number of units the user must select.
   *
   * @return A tuple whose first element is the selected territory and whose second element is the collection of
   *         selected units.
   */
  public Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(final PlayerID player,
      final List<Territory> territoryChoices, final List<Unit> unitChoices, final int unitsPerPick) {
    // total hacks
    messageAndDialogThreadPool.waitForAll();
    {
      final CountDownLatch latch1 = new CountDownLatch(1);
      SwingUtilities.invokeLater(() -> {
        if (!inGame) {
          showGame();
        }
        if (tabsPanel.indexOfTab("Actions") == -1) {
          // add actions tab
          tabsPanel.insertTab("Actions", null, actionButtons, null, 0);
        }
        tabsPanel.setSelectedIndex(0);
        latch1.countDown();
      });
      Interruptibles.await(latch1);
    }
    actionButtons.changeToPickTerritoryAndUnits(player);
    final Tuple<Territory, Set<Unit>> territoryAndUnits =
        actionButtons.waitForPickTerritoryAndUnits(territoryChoices, unitChoices, unitsPerPick);
    final int index = tabsPanel.indexOfTab("Actions");
    if (index != -1 && inHistory) {
      final CountDownLatch latch2 = new CountDownLatch(1);
      SwingUtilities.invokeLater(() -> {
        // remove actions tab
        tabsPanel.remove(index);
        latch2.countDown();
      });
      Interruptibles.await(latch2);
    }
    if (actionButtons.getCurrent() != null) {
      actionButtons.getCurrent().setActive(false);
    }
    return territoryAndUnits;
  }

  /**
   * Prompts the user to select the units that may participate in a suicide attack.
   *
   * @param possibleUnitsToAttack The possible units that may participate in the suicide attack from which the user may
   *        select.
   * @param attackResourceToken The resource that is expended to conduct the suicide attack.
   * @param maxNumberOfAttacksAllowed The maximum number of units that can be selected.
   *
   * @return A map of units that will participate in the suicide attack grouped by the territory from which they are
   *         attacking.
   */
  public Map<Territory, IntegerMap<Unit>> selectKamikazeSuicideAttacks(
      final Map<Territory, Collection<Unit>> possibleUnitsToAttack, final Resource attackResourceToken,
      final int maxNumberOfAttacksAllowed) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Should not be called from dispatch thread");
    }
    final Map<Territory, IntegerMap<Unit>> selection = new HashMap<>();
    if (possibleUnitsToAttack == null || possibleUnitsToAttack.isEmpty() || attackResourceToken == null
        || maxNumberOfAttacksAllowed <= 0) {
      return selection;
    }
    messageAndDialogThreadPool.waitForAll();
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Collection<IndividualUnitPanelGrouped> unitPanels = new ArrayList<>();
    SwingUtilities.invokeLater(() -> {
      final Map<String, Collection<Unit>> possibleUnitsToAttackStringForm = new HashMap<>();
      for (final Entry<Territory, Collection<Unit>> entry : possibleUnitsToAttack.entrySet()) {
        final List<Unit> units = new ArrayList<>(entry.getValue());
        units.sort(new UnitBattleComparator(false, TuvUtils.getCostsForTuv(units.get(0).getOwner(), data),
            TerritoryEffectHelper.getEffects(entry.getKey()), data, true, false));
        Collections.reverse(units);
        possibleUnitsToAttackStringForm.put(entry.getKey().getName(), units);
      }
      mapPanel.centerOn(data.getMap().getTerritory(possibleUnitsToAttackStringForm.keySet().iterator().next()));
      final IndividualUnitPanelGrouped unitPanel = new IndividualUnitPanelGrouped(possibleUnitsToAttackStringForm,
          uiContext, "Select Units to Suicide Attack using " + attackResourceToken.getName(),
          maxNumberOfAttacksAllowed, true, false);
      unitPanels.add(unitPanel);
      final String optionAttack = "Attack";
      final String optionNone = "None";
      final Object[] options = {optionAttack, optionNone};
      final JOptionPane optionPane = new JOptionPane(unitPanel, JOptionPane.PLAIN_MESSAGE,
          JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[1]);
      final JDialog dialog =
          new JDialog((Frame) getParent(), "Select units to Suicide Attack using " + attackResourceToken.getName());
      dialog.setContentPane(optionPane);
      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      dialog.setLocationRelativeTo(getParent());
      dialog.setAlwaysOnTop(true);
      dialog.pack();
      dialog.setVisible(true);
      dialog.requestFocusInWindow();
      optionPane.addPropertyChangeListener(e -> {
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
        }
      });
    });
    mapPanel.getUiContext().addShutdownLatch(continueLatch);
    Interruptibles.await(continueLatch);
    mapPanel.getUiContext().removeShutdownLatch(continueLatch);
    return selection;
  }

  /**
   * Prompts the user to select units which will participate in a scramble to the specified territory.
   *
   * @param scrambleTo The territory to which units are to be scrambled.
   * @param possibleScramblers The possible units that may participate in the scramble from which the user may select.
   *
   * @return A map of units that will participate in the scramble grouped by the territory from which they are
   *         scrambling.
   */
  public Map<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
    messageAndDialogThreadPool.waitForAll();
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Should not be called from dispatch thread");
    }
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Map<Territory, Collection<Unit>> selection = new HashMap<>();
    final Collection<Tuple<Territory, UnitChooser>> choosers = new ArrayList<>();
    SwingUtilities.invokeLater(() -> {
      mapPanel.centerOn(scrambleTo);
      final JDialog dialog = new JDialog(this, "Select units to scramble to " + scrambleTo.getName());
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      final JButton scrambleButton = new JButton("Scramble");
      scrambleButton.addActionListener(e -> getOptionPane((JComponent) e.getSource()).setValue(scrambleButton));
      final JButton noneButton = new JButton("None");
      noneButton.addActionListener(e -> getOptionPane((JComponent) e.getSource()).setValue(noneButton));
      final Object[] options = {scrambleButton, noneButton};
      final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
          JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[1]);
      final JLabel whereTo = new JLabel("Scramble To: " + scrambleTo.getName());
      whereTo.setFont(new Font("Arial", Font.ITALIC, 12));
      panel.add(whereTo, BorderLayout.NORTH);
      final JPanel panel2 = new JPanel();
      panel2.setBorder(BorderFactory.createEmptyBorder());
      panel2.setLayout(new FlowLayout());
      final JPanel fuelCostPanel = new JPanel(new GridBagLayout());
      panel.add(fuelCostPanel, BorderLayout.SOUTH);
      for (final Territory from : possibleScramblers.keySet()) {
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
        final UnitChooser chooser = new UnitChooser(possible, Collections.emptyMap(), false, uiContext);
        chooser.setMaxAndShowMaxButton(maxAllowed);
        chooser.addChangeListener(field -> {
          final Map<PlayerID, ResourceCollection> playerFuelCost = new HashMap<>();
          for (final Tuple<Territory, UnitChooser> tuple : choosers) {
            final Map<PlayerID, ResourceCollection> map = Route
                .getScrambleFuelCostCharge(tuple.getSecond().getSelected(false), tuple.getFirst(), scrambleTo, data);
            for (final Entry<PlayerID, ResourceCollection> playerAndCost : map.entrySet()) {
              if (playerFuelCost.containsKey(playerAndCost.getKey())) {
                playerFuelCost.get(playerAndCost.getKey()).add(playerAndCost.getValue());
              } else {
                playerFuelCost.put(playerAndCost.getKey(), playerAndCost.getValue());
              }
            }
          }
          fuelCostPanel.removeAll();
          boolean hasEnoughFuel = true;
          int count = 0;
          for (final Entry<PlayerID, ResourceCollection> entry : playerFuelCost.entrySet()) {
            final JLabel label = new JLabel(entry.getKey().getName() + ": ");
            fuelCostPanel.add(label,
                new GridBagConstraints(0, count, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
            fuelCostPanel.add(uiContext.getResourceImageFactory().getResourcesPanel(entry.getValue()),
                new GridBagConstraints(1, count++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
                    new Insets(0, 0, 0, 0), 0, 0));
            if (!entry.getKey().getResources().has(entry.getValue().getResourcesCopy())) {
              hasEnoughFuel = false;
              label.setForeground(Color.RED);
            }
          }
          scrambleButton.setEnabled(hasEnoughFuel);
          dialog.pack();
        });
        choosers.add(Tuple.of(from, chooser));
        panelChooser.add(chooser);
        final JScrollPane chooserScrollPane = new JScrollPane(panelChooser);
        panel2.add(chooserScrollPane);
      }
      panel.add(panel2, BorderLayout.CENTER);
      dialog.setContentPane(optionPane);
      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      dialog.setLocationRelativeTo(getParent());
      dialog.setAlwaysOnTop(true);
      dialog.pack();
      dialog.setVisible(true);
      dialog.requestFocusInWindow();
      optionPane.addPropertyChangeListener(e -> {
        if (!dialog.isVisible()) {
          return;
        }
        final Object option = optionPane.getValue();
        if (option == noneButton) {
          choosers.clear();
          selection.clear();
          dialog.setVisible(false);
          dialog.removeAll();
          dialog.dispose();
          continueLatch.countDown();
        } else if (option == scrambleButton) {
          for (final Tuple<Territory, UnitChooser> terrChooser : choosers) {
            selection.put(terrChooser.getFirst(), terrChooser.getSecond().getSelected());
          }
          dialog.setVisible(false);
          dialog.removeAll();
          dialog.dispose();
          continueLatch.countDown();
        }
      });
    });
    mapPanel.getUiContext().addShutdownLatch(continueLatch);
    Interruptibles.await(continueLatch);
    mapPanel.getUiContext().removeShutdownLatch(continueLatch);
    return selection;
  }

  private JOptionPane getOptionPane(final JComponent parent) {
    return !(parent instanceof JOptionPane)
        ? getOptionPane((JComponent) parent.getParent())
        : (JOptionPane) parent;
  }

  /**
   * Prompts the user to select from a predefined collection of units in a specific territory.
   *
   * @param current The territory containing the units.
   * @param possible The collection of possible units from which the user may select.
   * @param message An additional message to display to the user.
   *
   * @return The collection of units selected by the user.
   */
  public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible,
      final String message) {
    messageAndDialogThreadPool.waitForAll();
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Should not be called from dispatch thread");
    }
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Collection<Unit> selection = new ArrayList<>();
    SwingUtilities.invokeLater(() -> {
      mapPanel.centerOn(current);
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      final JLabel messageLabel = new JLabel(message);
      messageLabel.setFont(new Font("Arial", Font.ITALIC, 12));
      panel.add(messageLabel, BorderLayout.NORTH);
      final JPanel panelChooser = new JPanel();
      panelChooser.setLayout(new BoxLayout(panelChooser, BoxLayout.Y_AXIS));
      panelChooser.setBorder(BorderFactory.createLineBorder(getBackground()));
      final JLabel whereFrom = new JLabel("From: " + current.getName());
      whereFrom.setHorizontalAlignment(SwingConstants.LEFT);
      whereFrom.setFont(new Font("Arial", Font.BOLD, 12));
      panelChooser.add(whereFrom);
      panelChooser.add(new JLabel(" "));
      final int maxAllowed = Math.min(AirBattle.getMaxInterceptionCount(current, possible), possible.size());
      final UnitChooser chooser = new UnitChooser(possible, Collections.emptyMap(), false, uiContext);
      chooser.setMaxAndShowMaxButton(maxAllowed);
      panelChooser.add(chooser);
      final JScrollPane chooserScrollPane = new JScrollPane(panelChooser);
      panel.add(chooserScrollPane, BorderLayout.CENTER);
      final String optionSelect = "Select";
      final String optionNone = "None";
      final Object[] options = {optionSelect, optionNone};
      final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
          JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[1]);
      final JDialog dialog = new JDialog((Frame) getParent(), message);
      dialog.setContentPane(optionPane);
      dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      dialog.setLocationRelativeTo(getParent());
      dialog.setAlwaysOnTop(true);
      dialog.pack();
      dialog.setVisible(true);
      dialog.requestFocusInWindow();
      optionPane.addPropertyChangeListener(e -> {
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
        }
      });
    });
    mapPanel.getUiContext().addShutdownLatch(continueLatch);
    Interruptibles.await(continueLatch);
    mapPanel.getUiContext().removeShutdownLatch(continueLatch);
    return selection;
  }

  public PoliticalActionAttachment getPoliticalActionChoice(final PlayerID player, final boolean firstRun,
      final IPoliticsDelegate politicsDelegate) {
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToPolitics(player);
    requestWindowFocus();
    return actionButtons.waitForPoliticalAction(firstRun, politicsDelegate);
  }

  public UserActionAttachment getUserActionChoice(final PlayerID player, final boolean firstRun,
      final IUserActionDelegate userActionDelegate) {
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToUserActions(player);
    requestWindowFocus();
    return actionButtons.waitForUserActionAction(firstRun, userActionDelegate);
  }

  public TechRoll getTechRolls(final PlayerID id) {
    messageAndDialogThreadPool.waitForAll();
    actionButtons.changeToTech(id);
    // workaround for panel not receiving focus at beginning of tech phase
    requestWindowFocus();
    return actionButtons.waitForTech();
  }

  /**
   * Prompts the user to select the territory on which they wish to conduct a rocket attack.
   *
   * @param candidates The collection of territories on which the user may conduct a rocket attack.
   * @param from The territory from which the rocket attack is conducted.
   *
   * @return The selected territory or {@code null} if no territory was selected.
   */
  public Territory getRocketAttack(final Collection<Territory> candidates, final Territory from) {
    messageAndDialogThreadPool.waitForAll();
    mapPanel.centerOn(from);

    final Supplier<Territory> action = () -> {
      final JList<Territory> list = new JList<>(SwingComponents.newListModel(candidates));
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
      return (selection == 0) ? list.getSelectedValue() : null;
    };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action)).result
        .orElse(null);
  }

  final GameStepListener stepListener = (stepName, delegateName, player1, round1, stepDisplayName) -> updateStep();

  private void updateStep() {
    Preconditions.checkState(!SwingUtilities.isEventDispatchThread(),
        "This method must not be invoked on the EDT!");
    if (uiContext == null || uiContext.isShutDown()) {
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
    final int round;
    final String stepDisplayName;
    final PlayerID player;
    data.acquireReadLock();
    try {
      round = data.getSequence().getRound();
      stepDisplayName = data.getSequence().getStep().getDisplayName();
      player = data.getSequence().getStep().getPlayerId();
    } finally {
      data.releaseReadLock();
    }
    final boolean isPlaying = localPlayers.playing(player);
    SwingUtilities.invokeLater(() -> {
      this.round.setText("Round:" + round + " ");
      step.setText(stepDisplayName);
      if (player != null) {
        this.player.setText((isPlaying ? "" : "REMOTE: ") + player.getName());
      }
    });
    if (player != null && !player.isNull()) {
      final CompletableFuture<?> future = CompletableFuture
          .supplyAsync(() -> uiContext.getFlagImageFactory().getFlag(player))
          .thenApplyAsync(ImageIcon::new)
          .thenAccept(icon -> SwingUtilities.invokeLater(() -> this.round.setIcon(icon)));
      CompletableFutureUtils.logExceptionWhenComplete(future, "Failed to set round icon for " + player);
      lastStepPlayer = currentStepPlayer;
      currentStepPlayer = player;
    }
    resourceBar.gameDataChanged(null);
    // if the game control has passed to someone else and we are not just showing the map
    // show the history
    if (player != null && !player.isNull()) {
      if (isPlaying) {
        if (inHistory) {
          requiredTurnSeries.put(player, true);
          // if the game control is with us
          // show the current game
          showGame();
        }
      } else {
        if (!inHistory && !uiContext.getShowMapOnly()) {
          showHistory();
        }
      }
    }
  }

  /**
   * Invoked at the start of a player's turn to play a sound alerting the player it is their turn and to center the map
   * on the player's capital.
   */
  public void requiredTurnSeries(final PlayerID player) {
    if (player == null || !Interruptibles.sleep(300)) {
      return;
    }
    Interruptibles.await(() -> SwingAction.invokeAndWait(() -> {
      final Boolean play = requiredTurnSeries.get(player);
      if (play != null && play) {
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
    }));
  }

  final GameDataChangeListener dataChangeListener = new GameDataChangeListener() {
    @Override
    public void gameDataChanged(final Change change) {
      try {
        SwingUtilities.invokeLater(() -> {
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
              final Object details1 = ((Renderable) node).getRenderingData();
              if (details1 instanceof MoveDescription) {
                final MoveDescription moveMessage = (MoveDescription) details1;
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
        });
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Failed to process game data change", e);
      }
    }
  };

  private KeyListener getFullScreenListener() {
    return new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent e) {
        if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_Z) {
          if (gameCenterPanel.getDividerLocation() <= gameCenterPanel.getMaximumDividerLocation()) {
            gameCenterPanel.setDividerLocation(1.0);
          } else {
            gameCenterPanel.setDividerLocation(gameCenterPanel.getLastDividerLocation());
          }
        }
      }
    };
  }

  private String getUnitInfo() {
    if (unitsBeingMousedOver != null && !unitsBeingMousedOver.isEmpty()) {
      final Unit unit = unitsBeingMousedOver.get(0);
      return MapUnitTooltipManager.getTooltipTextForUnit(unit.getType(), unit.getOwner(), unitsBeingMousedOver.size());
    }
    return "";
  }

  private KeyListener getArrowKeyListener() {
    return new KeyListener() {
      @Override
      public void keyPressed(final KeyEvent e) {
        isCtrlPressed = e.isControlDown();
        // scroll map according to wasd/arrowkeys
        final int diffPixel = computeScrollSpeed();
        final int x = mapPanel.getXOffset();
        final int y = mapPanel.getYOffset();
        final int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_RIGHT) {
          getMapPanel().setTopLeft(x + diffPixel, y);
        } else if (keyCode == KeyEvent.VK_LEFT) {
          getMapPanel().setTopLeft(x - diffPixel, y);
        } else if (keyCode == KeyEvent.VK_DOWN) {
          getMapPanel().setTopLeft(x, y + diffPixel);
        } else if (keyCode == KeyEvent.VK_UP) {
          getMapPanel().setTopLeft(x, y - diffPixel);
        }
        // I for info
        if (keyCode == KeyEvent.VK_I || keyCode == KeyEvent.VK_V) {
          final String unitInfo = getUnitInfo();
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
            new Thread(() -> {
              Interruptibles.sleep(5000);
              popup.hide();
            }, "popup waiter").start();
          }
        }
      }

      @Override
      public void keyTyped(final KeyEvent e) {}

      @Override
      public void keyReleased(final KeyEvent e) {
        isCtrlPressed = e.isControlDown();
      }
    };
  }

  private int computeScrollSpeed() {
    return ClientSetting.arrowKeyScrollSpeed.getValueOrThrow()
        * (isCtrlPressed ? ClientSetting.fasterArrowKeyScrollMultiplier.getValueOrThrow() : 1);
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
      data.removeDataChangeListener(dataChangeListener);
      if (historySyncher != null) {
        throw new IllegalStateException("Two history synchers?");
      }
      historySyncher = new HistorySynchronizer(clonedGameData, game);
      clonedGameData.addDataChangeListener(dataChangeListener);
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
    SwingUtilities.invokeLater(() -> {
      final HistoryDetailsPanel historyDetailPanel = new HistoryDetailsPanel(clonedGameData, mapPanel);
      tabsPanel.removeAll();
      tabsPanel.add("History", historyDetailPanel);
      addTab("Players", statsPanel, 'P');
      addTab("Resources", economyPanel, 'R');
      if (objectivePanel != null && !objectivePanel.isEmpty()) {
        addTab(objectivePanel.getName(), objectivePanel, 'O');
      }
      addTab("Notes", notesPanel, 'N');
      addTab("Territory", details, 'T');
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
      popup.add(new AbstractAction("Export Map Snapshot") {
        private static final long serialVersionUID = 1222760138263428443L;

        @Override
        public void actionPerformed(final ActionEvent ae) {
          ScreenshotExporter.exportScreenshot(TripleAFrame.this, data, historyPanel.getCurrentPopupNode());
          historyPanel.clearCurrentPopupNode();
        }
      });
      popup.add(new AbstractAction("Save Game at this point (BETA)") {
        private static final long serialVersionUID = 1430512376199927896L;

        @Override
        public void actionPerformed(final ActionEvent ae) {
          JOptionPane.showMessageDialog(TripleAFrame.this,
              "Please first left click on the spot you want to save from, Then right click and select "
                  + "'Save Game From History'"
                  + "\n\nIt is recommended that when saving the game from the History panel:"
                  + "\n * Your CURRENT GAME is at the start of some player's turn, and that no moves have been made "
                  + "and no actions taken yet."
                  + "\n * The point in HISTORY that you are trying to save at, is at the beginning of a player's turn, "
                  + "or the beginning of a round."
                  + "\nSaving at any other point, could potentially create errors."
                  + "\nFor example, saving while your current game is in the middle of a move or battle phase will "
                  + "always create errors in the savegame."
                  + "\nAnd you will also get errors in the savegame if you try to create a save at a point in history "
                  + "such as a move or battle phase.",
              "Save Game from History", JOptionPane.INFORMATION_MESSAGE);
          data.acquireReadLock();
          try {
            final File f = TripleAMenuBar.getSaveGameLocation(TripleAFrame.this);
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
                final Enumeration<?> enumeration =
                    ((DefaultMutableTreeNode) datacopy.getHistory().getRoot()).preorderEnumeration();
                enumeration.nextElement();
                int round = 0;
                String stepDisplayName = datacopy.getSequence().getStep(0).getDisplayName();
                PlayerID currentPlayer = datacopy.getSequence().getStep(0).getPlayerId();
                while (enumeration.hasMoreElements()) {
                  final HistoryNode node = (HistoryNode) enumeration.nextElement();
                  if (node instanceof Round) {
                    round = Math.max(0, ((Round) node).getRoundNo() - datacopy.getSequence().getRoundOffset());
                    currentPlayer = null;
                    stepDisplayName = node.getTitle();
                  } else if (node instanceof Step) {
                    currentPlayer = ((Step) node).getPlayerId();
                    stepDisplayName = node.getTitle();
                  }
                }
                datacopy.getSequence().setRoundAndStep(round, stepDisplayName, currentPlayer);
                GameDataManager.saveGame(fout, datacopy);
                JOptionPane.showMessageDialog(TripleAFrame.this, "Game Saved", "Game Saved",
                    JOptionPane.INFORMATION_MESSAGE);
              } catch (final IOException e) {
                log.log(Level.SEVERE, "Failed to save game: " + f.getAbsolutePath(), e);
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
    });
  }

  private void showGame() {
    inGame = true;
    uiContext.setShowMapOnly(false);
    // Are we coming from showHistory mode or showMapOnly mode?
    SwingUtilities.invokeLater(() -> {
      if (inHistory) {
        inHistory = false;
        if (historySyncher != null) {
          historySyncher.deactivate();
          historySyncher = null;
        }
        historyPanel.goToEnd();
        historyPanel = null;
        mapPanel.getData().removeDataChangeListener(dataChangeListener);
        statsPanel.setGameData(data);
        economyPanel.setGameData(data);
        if (objectivePanel != null && !objectivePanel.isEmpty()) {
          objectivePanel.setGameData(data);
        }
        details.setGameData(data);
        mapPanel.setGameData(data);
        data.addDataChangeListener(dataChangeListener);
        tabsPanel.removeAll();
      }
      setWidgetActivation();
      addTab("Actions", actionButtons, 'A');
      addTab("Players", statsPanel, 'P');
      addTab("Resources", economyPanel, 'R');
      if (objectivePanel != null && !objectivePanel.isEmpty()) {
        addTab(objectivePanel.getName(), objectivePanel, 'O');
      }
      addTab("Notes", notesPanel, 'N');
      addTab("Territory", details, 'T');
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
      validate();
    });
    mapPanel.setRoute(null);
  }

  private void showMapOnly() {
    // Are we coming from showHistory mode or showGame mode?
    if (inHistory) {
      inHistory = false;
      if (historySyncher != null) {
        historySyncher.deactivate();
        historySyncher = null;
      }
      historyPanel.goToEnd();
      historyPanel = null;
      mapPanel.getData().removeDataChangeListener(dataChangeListener);
      mapPanel.setGameData(data);
      data.addDataChangeListener(dataChangeListener);
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

  private void setWidgetActivation() {
    SwingAction.invokeNowOrLater(() -> {
      showHistoryAction.setEnabled(!(inHistory || uiContext.getShowMapOnly()));
      showGameAction.setEnabled(!inGame);
      // We need to check and make sure there are no local human players
      boolean foundHuman = false;
      for (final IGamePlayer gamePlayer : localPlayers.getLocalPlayers()) {
        if (gamePlayer instanceof TripleAPlayer) {
          foundHuman = true;
        }
      }
      if (!foundHuman) {
        showMapOnlyAction.setEnabled(inGame || inHistory);
      } else {
        showMapOnlyAction.setEnabled(false);
      }
      if (editModeButtonModel != null) {
        if (editDelegate == null || uiContext.getShowMapOnly()) {
          editModeButtonModel.setEnabled(false);
        } else {
          editModeButtonModel.setEnabled(true);
        }
      }
    });
  }

  // setEditDelegate is called by TripleAPlayer at the start and end of a turn
  public void setEditDelegate(final IEditDelegate editDelegate) {
    this.editDelegate = editDelegate;
    // force a data change event to update the UI for edit mode
    dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
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

  private boolean getEditMode() {
    final boolean isEditMode;
    // use GameData from mapPanel since it will follow current history node
    mapPanel.getData().acquireReadLock();
    try {
      isEditMode = BaseEditDelegate.getEditMode(mapPanel.getData());
    } finally {
      mapPanel.getData().releaseReadLock();
    }
    return isEditMode;
  }

  private final Action showHistoryAction = SwingAction.of("Show history", e -> {
    showHistory();
    dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
  });

  private final Action showGameAction = new AbstractAction("Show current game") {
    private static final long serialVersionUID = -7551760679570164254L;

    {
      setEnabled(false);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      showGame();
      dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    }
  };

  private final Action showMapOnlyAction = SwingAction.of("Show map only", e -> {
    showMapOnly();
    dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
  });

  /**
   * Prompts the user to select from the specified collection of fighters those they wish to move to an adjacent
   * newly-constructed carrier.
   *
   * @param fighters The collection of fighters from which to choose.
   * @param where The territory on which to center the map.
   *
   * @return The collection of fighters to move to the newly-constructed carrier.
   */
  public Collection<Unit> moveFightersToCarrier(final Collection<Unit> fighters, final Territory where) {
    messageAndDialogThreadPool.waitForAll();
    mapPanel.centerOn(where);
    final AtomicReference<JScrollPane> panelRef = new AtomicReference<>();
    final AtomicReference<UnitChooser> chooserRef = new AtomicReference<>();
    Interruptibles.await(() -> SwingAction.invokeAndWait(() -> {
      final UnitChooser chooser = new UnitChooser(fighters, Collections.emptyMap(), false, uiContext);
      final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
      final int availHeight = screenResolution.height - 120;
      final int availWidth = screenResolution.width - 40;
      final JScrollPane scroll = new JScrollPane(chooser);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      scroll.setPreferredSize(new Dimension(
          (scroll.getPreferredSize().width > availWidth ? availWidth
              : (scroll.getPreferredSize().width + (scroll.getPreferredSize().height > availHeight ? 20 : 0))),
          (scroll.getPreferredSize().height > availHeight ? availHeight
              : (scroll.getPreferredSize().height + (scroll.getPreferredSize().width > availWidth ? 26 : 0)))));
      panelRef.set(scroll);
      chooserRef.set(chooser);
    }));
    final int option = EventThreadJOptionPane.showOptionDialog(this, panelRef.get(),
        "Move air units to carrier", JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null,
        new String[] {"OK", "Cancel"}, "OK", getUiContext().getCountDownLatchHandler());
    if (option == JOptionPane.OK_OPTION) {
      return chooserRef.get().getSelected();
    }
    return new ArrayList<>();
  }

  public BattlePanel getBattlePanel() {
    return actionButtons.getBattlePanel();
  }

  public Action getShowGameAction() {
    return showGameAction;
  }

  public Action getShowHistoryAction() {
    return showHistoryAction;
  }

  public Action getShowMapOnlyAction() {
    return showMapOnlyAction;
  }

  public UiContext getUiContext() {
    return uiContext;
  }

  public MapPanel getMapPanel() {
    return mapPanel;
  }

  /**
   * Displays the map located in the directory/archive {@code mapdir}.
   */
  public void updateMap(final String mapdir) {
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

  public IGame getGame() {
    return game;
  }

  public void setShowChatTime(final boolean showTime) {
    chatPanel.setShowChatTime(showTime);
  }

  public Optional<InGameLobbyWatcherWrapper> getInGameLobbyWatcher() {
    return ServerGame.class.isAssignableFrom(getGame().getClass())
        ? Optional.ofNullable(((ServerGame) getGame()).getInGameLobbyWatcher())
        : Optional.empty();
  }

  public boolean hasChat() {
    return chatPanel != null;
  }
}
