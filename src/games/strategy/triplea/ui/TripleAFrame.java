package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.image.BufferedImage;
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
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.PlayerChatRenderer;
import games.strategy.engine.data.Change;
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
import games.strategy.engine.framework.HistorySynchronizer;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import games.strategy.thread.ThreadPool;
import games.strategy.triplea.Properties;
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
import games.strategy.triplea.delegate.BaseEditDelegate;
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
import games.strategy.triplea.settings.scrolling.ScrollSettings;
import games.strategy.triplea.ui.history.HistoryDetailsPanel;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.triplea.ui.menubar.ExportMenu;
import games.strategy.triplea.ui.menubar.HelpMenu;
import games.strategy.triplea.ui.menubar.TripleAMenuBar;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.util.DisableableEventHandler;
import games.strategy.triplea.util.JFXUtils;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.SwingAction;
import games.strategy.ui.Util;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.IntegerMap;
import games.strategy.util.LocalizeHTML;
import games.strategy.util.Match;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Tuple;
import javafx.animation.AnimationTimer;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;

/**
 * Main frame for the triple a game
 */
public class TripleAFrame extends MainGameFrame {
  private GameData data;
  private IGame game;
  private MapPanel mapPanel;
  private MapPanelSmallView smallView;
  private final Label message = new Label("No selection");
  private final Label status = new Label("");
  private final Label step = new Label("xxxxxx");
  private final Label round = new Label("xxxxxx");
  private final Label player = new Label("xxxxxx");
  private ActionButtons actionButtons;
  private final BorderPane gameMainPanel = new BorderPane();
  private final BorderPane rightHandSidePanel = new BorderPane();
  private final TabPane tabsPanel = new TabPane();
  private StatPanel statsPanel;
  private EconomyPanel economyPanel;
  private ObjectivePanel objectivePanel;
  private NotesPanel notesPanel;
  private TerritoryDetailPanel details;
  private final BorderPane historyComponent = new BorderPane();
  private BorderPane gameSouthPanel;
  private HistoryPanel historyPanel;
  private boolean inHistory = false;
  private boolean inGame = true;
  private HistorySynchronizer historySyncher;
  private IUIContext uiContext;
  private BorderPane mapAndChatPanel;
  private ChatPanel chatPanel;
  private CommentPanel commentPanel;
  private SplitPane chatSplit;
  private SplitPane commentSplit;
  private EditPanel editPanel;
  private final ButtonModel editModeButtonModel;
  private final EventHandler<ActionEvent> showCommentLogButtonModel;
  private IEditDelegate editDelegate;
  private SplitPane gameCenterPanel;
  private Territory territoryLastEntered;
  private List<Unit> unitsBeingMousedOver;
  private PlayerID lastStepPlayer;
  private PlayerID currentStepPlayer;
  private final Map<PlayerID, Boolean> requiredTurnSeries = new HashMap<>();
  private ThreadPool messageAndDialogThreadPool;
  private TripleAMenuBar menu;
  private final ScrollSettings scrollSettings;
  private BorderPane content = new BorderPane();
  private StackPane root = new StackPane();
  private Canvas overlay = new Canvas();
  private boolean editMode = false;

  /** Creates new TripleAFrame */
  public TripleAFrame(final IGame game, final LocalPlayers players) {
    super("TripleA - " + game.getData().getGameName(), players);
    root.getChildren().addAll(content, overlay);
    setScene(new Scene(root));
    scrollSettings = ClientContext.scrollSettings();
    this.game = game;
    data = game.getData();
    messageAndDialogThreadPool = new ThreadPool(1);
    addZoomKeyboardShortcuts();
    onCloseRequestProperty().set(e -> leaveGame());
    uiContext = new UIContext();
    uiContext.setDefaultMapDir(game.getData());
    uiContext.getMapData().verify(data);
    uiContext.setLocalPlayers(players);
    getScene().setCursor(uiContext.getCursor());
    // initialize m_editModeButtonModel before createMenuBar()
    editModeButtonModel = new JToggleButton.ToggleButtonModel();
    editModeButtonModel.setEnabled(false);
    showCommentLogButtonModel = e -> {
      if (((CheckMenuItem) e.getSource()).isSelected()) {
        if (chatPanel != null) {
          commentSplit.getItems().clear();
          commentSplit.getItems().add(chatPanel);
          chatSplit.getItems().clear();
          chatSplit.getItems().add(commentSplit);
        } else {
          mapAndChatPanel.getChildren().clear();// TODO maybe this isn't working...
          chatSplit.getItems().clear();
          chatSplit.getItems().add(commentPanel);
          chatSplit.getItems().add(mapPanel);
          mapAndChatPanel.setCenter(chatSplit);
        }
      } else {
        if (chatPanel != null) {
          commentSplit.getItems().clear();
          chatSplit.getItems().clear();
          chatSplit.getItems().add(chatPanel);
        } else {
          mapAndChatPanel.getChildren().clear();
          chatSplit.getItems().clear();
          mapAndChatPanel.setCenter(mapPanel);
        }
      }
    };
    menu = new TripleAMenuBar(this);
    content.getChildren().add(menu);
    final ImageScrollModel model = new ImageScrollModel();
    model.setScrollX(uiContext.getMapData().scrollWrapX());
    model.setScrollY(uiContext.getMapData().scrollWrapY());
    model.setMaxBounds(uiContext.getMapData().getMapDimensions().width,
        uiContext.getMapData().getMapDimensions().height);
    final Image small = uiContext.getMapImage().getSmallMapImage();
    smallView = new MapPanelSmallView(small, model);
    mapPanel = new MapPanel(data, smallView, uiContext, model);
    mapPanel.addMapSelectionListener(MAP_SELECTION_LISTENER);
    final MouseOverUnitListener MOUSE_OVER_UNIT_LISTENER = (units, territory, me) -> unitsBeingMousedOver = units;
    mapPanel.addMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
    // link the small and large images
    mapPanel.initSmallMap();
    mapAndChatPanel = new BorderPane();
    commentPanel = new CommentPanel(this, data);
    chatSplit = new SplitPane();
    chatSplit.setOrientation(Orientation.VERTICAL);
    // chatSplit.setOneTouchExpandable(true);
    // chatSplit.setDividerSize(8);
    // chatSplit.setResizeWeight(0.95);
    if (MainFrame.getInstance() != null && MainFrame.getInstance().getChat() != null) {
      commentSplit = new SplitPane();
      commentSplit.setOrientation(Orientation.VERTICAL);
      // commentSplit.setOneTouchExpandable(true);
      // commentSplit.setDividerSize(8);
      // commentSplit.setResizeWeight(0.5);
      commentSplit.getItems().clear();
      commentSplit.getItems().add(null);// TODO
      commentSplit.getItems().add(commentPanel);
      chatPanel = new ChatPanel(MainFrame.getInstance().getChat());
      chatPanel.setPlayerRenderer(new PlayerChatRenderer(this.game, uiContext));
      chatPanel.setPrefHeight(95);
      chatSplit.getItems().add(mapPanel);
      chatSplit.getItems().add(mapPanel);
      mapAndChatPanel.setCenter(chatSplit);
    } else {
      mapAndChatPanel.setCenter(mapPanel);
    }
    content.setCenter(gameMainPanel);
    gameSouthPanel = new BorderPane();
    // m_gameSouthPanel.add(m_message, BorderLayout.WEST);
    // message.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    // m_gameSouthPanel.add(m_status, BorderLayout.CENTER);
    final GridPane bottomMessagePanel = new GridPane();
    bottomMessagePanel.setBorder(Border.EMPTY);
    bottomMessagePanel.add(message, 0, 0);
    ColumnConstraints cc1 = new ColumnConstraints();
    cc1.setPercentWidth(0.35);
    bottomMessagePanel.getColumnConstraints().add(cc1);
    RowConstraints rc1 = new RowConstraints();
    rc1.setPercentHeight(1);// TODO not sure if this is necessary
    bottomMessagePanel.getRowConstraints().add(rc1);
    GridPane.setConstraints(message, 0, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS,
        Insets.EMPTY);
    // bottomMessagePanel.add(message, new GridBagConstraints(0, 0, 1, 1, .35, 1, GridBagConstraints.WEST,
    // GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    bottomMessagePanel.add(status, 1, 0);
    ColumnConstraints cc2 = new ColumnConstraints();
    cc2.setPercentWidth(0.65);
    bottomMessagePanel.getColumnConstraints().add(cc2);
    RowConstraints rc2 = new RowConstraints();
    rc2.setPercentHeight(1);// TODO not sure if this is necessary
    bottomMessagePanel.getRowConstraints().add(rc2);
    GridPane.setConstraints(message, 1, 0, 1, 1, HPos.CENTER, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS,
        Insets.EMPTY);
    // bottomMessagePanel.add(status, new GridBagConstraints(1, 0, 1, 1, .65, 1, GridBagConstraints.CENTER,
    // GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    gameSouthPanel.setCenter(bottomMessagePanel);
    // status.setBorder(new EtchedBorder(EtchedBorder.RAISED)); TODO with CSS
    final GridPane stepPanel = new GridPane();
    stepPanel.add(step, 1, 0);
    ColumnConstraints cc3 = new ColumnConstraints();
    cc3.setPercentWidth(0);
    stepPanel.getColumnConstraints().add(cc3);
    RowConstraints rc3 = new RowConstraints();
    rc3.setPercentHeight(0);// TODO not sure if this is necessary
    stepPanel.getRowConstraints().add(rc3);
    GridPane.setConstraints(step, 1, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS,
        Insets.EMPTY);
    // stepPanel.add(step, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
    // new Insets(0, 0, 0, 0), 0, 0));
    stepPanel.add(player, 1, 0);
    ColumnConstraints cc4 = new ColumnConstraints();
    cc4.setPercentWidth(0);
    stepPanel.getColumnConstraints().add(cc4);
    RowConstraints rc4 = new RowConstraints();
    rc4.setPercentHeight(0);// TODO not sure if this is necessary
    stepPanel.getRowConstraints().add(rc4);
    GridPane.setConstraints(player, 1, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS,
        Insets.EMPTY);
    // stepPanel.add(player, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
    // new Insets(0, 0, 0, 0), 0, 0));
    stepPanel.add(round, 2, 0);
    ColumnConstraints cc5 = new ColumnConstraints();
    cc5.setPercentWidth(0);
    stepPanel.getColumnConstraints().add(cc4);
    RowConstraints rc5 = new RowConstraints();
    rc5.setPercentHeight(0);// TODO not sure if this is necessary
    stepPanel.getRowConstraints().add(rc5);
    GridPane.setConstraints(round, 2, 0, 1, 1, HPos.LEFT, VPos.CENTER, Priority.ALWAYS, Priority.ALWAYS,
        Insets.EMPTY);
    // stepPanel.add(round, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH,
    // new Insets(0, 0, 0, 0), 0, 0));
    // step.setBorder(new EtchedBorder(EtchedBorder.RAISED));TODO CSS
    // round.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    // player.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    // step.setHorizontalTextPosition(SwingConstants.LEADING);
    gameSouthPanel.setRight(stepPanel);
    gameMainPanel.setBottom(gameSouthPanel);
    InvalidationListener focusToMapPanelFocusListener = e -> mapPanel.requestFocus();
    rightHandSidePanel.focusTraversableProperty().addListener(focusToMapPanelFocusListener);
    smallView.focusedProperty().addListener(e -> mapPanel.requestFocus());
    tabsPanel.focusTraversableProperty().addListener(focusToMapPanelFocusListener);
    rightHandSidePanel.setTop(smallView);
    tabsPanel.setBorder(null);
    rightHandSidePanel.setCenter(tabsPanel);

    final MovePanel movePanel = new MovePanel(data, mapPanel, this);
    actionButtons = new ActionButtons(data, mapPanel, movePanel, this);

    registerArrowKeyListener(mapPanel);
    movePanel.registerCustomKeyListeners(mapPanel);
    registerFlagToggleKeyListener(mapPanel);
    tabsPanel.getTabs().add(new Tab("Actions", actionButtons));
    actionButtons.setBorder(null);
    statsPanel = new StatPanel(data, uiContext);
    tabsPanel.getTabs().add(new Tab("Stats", statsPanel));
    economyPanel = new EconomyPanel(data);
    tabsPanel.getTabs().add(new Tab("Economy", economyPanel));
    objectivePanel = new ObjectivePanel(data);
    if (objectivePanel.isEmpty()) {
      objectivePanel.removeDataChangeListener();
      objectivePanel = null;
    } else {
      tabsPanel.getTabs().add(new Tab(objectivePanel.getName(), objectivePanel));
    }
    notesPanel = new NotesPanel(HelpMenu.gameNotesPane);
    tabsPanel.getTabs().add(new Tab("Notes", notesPanel));
    details = new TerritoryDetailPanel(mapPanel, data, uiContext, this);
    Tab territoryTab = new Tab("Territory", details);
    territoryTab.setTooltip(new Tooltip(TerritoryDetailPanel.getHoverText()));
    tabsPanel.getTabs().add(territoryTab);
    editPanel = new EditPanel(data, mapPanel, this);
    // Register a change listener
    tabsPanel.getSelectionModel().selectedItemProperty().addListener((value, oldValue, newValue) -> {
      // Get current tab
      final int sel = tabsPanel.getSelectionModel().getSelectedIndex();
      if (sel == -1) {
        return;
      }
      if (newValue.getContent().equals(notesPanel)) {
        notesPanel.layoutNotes();
      } else {
        // for memory management reasons the notes are in a SoftReference,
        // so we must remove our hard reference link to them so it can be reclaimed if needed
        notesPanel.removeNotes();
      }
      if (newValue.getContent().equals(editPanel)) {
        PlayerID player1 = null;
        data.acquireReadLock();
        try {
          player1 = data.getSequence().getStep().getPlayerID();
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
    rightHandSidePanel.setPrefSize(smallView.getPrefWidth(), mapPanel.getPrefHeight());
    gameCenterPanel = new SplitPane();
    gameCenterPanel.setOrientation(Orientation.HORIZONTAL);
    gameCenterPanel.getItems().addAll(mapAndChatPanel, rightHandSidePanel);
    // gameCenterPanel.setOneTouchExpandable(true);
    // gameCenterPanel.setDividerSize(8);
    // gameCenterPanel.setResizeWeight(1.0);
    gameMainPanel.setCenter(gameCenterPanel);
    // gameCenterPanel.resetToPreferredSizes();
    // set up the edit mode overlay text
    new AnimationTimer() {
      @Override
      public void handle(long now) {
        GraphicsContext g = overlay.getGraphicsContext2D();
        if (editMode) {
          g.setFont(Font.font("Ariel", FontWeight.BOLD, 50));
          g.setFill(new Color(1, 1, 1, 0.69));
          g.strokeText("Edit Mode", (int) ((mapPanel.getWidth() - 200) / 2), (int) ((mapPanel.getHeight() - 100) / 2));
        } else {
          g.clearRect(0, 0, overlay.getWidth(), overlay.getHeight());
        }
      }
    };
    // force a data change event to update the UI for edit mode
    m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    data.addDataChangeListener(m_dataChangeListener);
    game.addGameStepListener(m_stepListener);
    updateStep();
    uiContext.addShutdownWindow(this);
  }


  public static void registerFlagToggleKeyListener(MapPanel mapPanel) {
    UnitFlagTimer timer = new UnitFlagTimer(mapPanel);
    mapPanel.addEventHandler(KeyEvent.KEY_PRESSED, timer::keyPressed);
    mapPanel.addEventHandler(KeyEvent.KEY_RELEASED, timer::keyReleased);
  }

  static class UnitFlagTimer {

    private boolean blockInputs = false;
    private long timeSinceLastPressEvent = 0;
    private boolean running = true;

    MapPanel mapPanel;

    public void keyPressed(final KeyEvent e) {
      timeSinceLastPressEvent = 0;
      if (!blockInputs) {
        resetFlagsOnTimeOut(e.getCode());
        toggleFlags(e.getCode());
        blockInputs = true;
      }
    }

    private void resetFlagsOnTimeOut(final KeyCode keyCode) {
      new Thread(() -> {
        running = true;
        while (running) {
          timeSinceLastPressEvent++;
          if (timeSinceLastPressEvent > 5) {
            running = false;
            toggleFlags(keyCode);
            blockInputs = false;
          }
          ThreadUtil.sleep(100);
        }
      }).start();
    }

    public void keyReleased(final KeyEvent e) {
      toggleFlags(e.getCode());
      blockInputs = false;
      running = false;
    }

    private void toggleFlags(final KeyCode keyCode) {
      if (keyCode == KeyCode.L) {
        UnitsDrawer.enabledFlags = !UnitsDrawer.enabledFlags;
        mapPanel.resetMap();
      }
    }

    UnitFlagTimer(MapPanel mapPanel) {
      this.mapPanel = mapPanel;
    }
  }

  private void addZoomKeyboardShortcuts() {
    // do both = and + (since = is what you get when you hit ctrl+ )
    getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.PLUS && getScale() < 100) {
        setScale(getScale() + 10);
      }
    });
    getScene().getRoot().addEventHandler(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.MINUS && getScale() > 16) {
        setScale(getScale() - 10);
      }
    });
  }

  /**
   * @param value
   *        - a number between 15 and 100
   */
  public void setScale(final double value) {
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
    this.hide();
    if (SystemProperties.isMac()) {
      // this frame should not handle shutdowns anymore
      MacQuitMenuWrapper.unregisterShutdownHandler();
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
    ProAI.gameOverClearCache();
  }

  @Override
  public void shutdown() {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Exit Program");
    alert.setHeaderText("Are you sure you want to exit TripleA?\nUnsaved game data will be lost.");
    alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
    alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(e -> {
      stopGame();
      System.exit(0);
    });
  }

  @Override
  public void leaveGame() {
    Alert alert = new Alert(AlertType.CONFIRMATION);
    alert.setTitle("Leave Game");
    alert.setHeaderText("Are you sure you want to leave the current game?\nUnsaved game data will be lost.");
    alert.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
    alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(e -> {
      if (game instanceof ServerGame) {
        ((ServerGame) game).stopGame();
      } else {
        game.getMessenger().shutDown();
        ((ClientGame) game).shutDown();
        // an ugly hack, we need a better
        // way to get the main frame
        MainFrame.getInstance().clientLeftGame();
      }
    });
  }

  public MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener() {
    @Override
    public void mouseEntered(final Territory territory) {
      territoryLastEntered = territory;
      refresh();
    }

    void refresh() {
      final StringBuilder buf = new StringBuilder();
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
    status.setGraphic(null);
  }

  public void setStatusErrorMessage(final String msg) {
    setStatus(msg, mapPanel.getErrorImage());
  }

  private void setStatus(final String msg, final Optional<Image> image) {
    if (status == null) {
      return;
    }
    status.setText(msg);

    if (!msg.equals("") && image.isPresent()) {
      status.setGraphic(new ImageView(SwingFXUtils.toFXImage((BufferedImage) image.get(), null)));
    } else {
      status.setGraphic(null);
    }
  }

  public void setStatusWarningMessage(final String msg) {
    setStatus(msg, mapPanel.getWarningImage());
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
      requestFocus();
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
    messageAndDialogThreadPool.runTask(
        () -> {
          Alert alert = new Alert(AlertType.ERROR);
          WebView webView = new WebView();
          webView.getEngine().loadContent(displayMessage);
          alert.getDialogPane().setContent(webView);
          alert.show();
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
      messageAndDialogThreadPool.runTask(
          () -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(title);
            WebView webView = new WebView();
            webView.getEngine().loadContent(displayMessage);
            alert.getDialogPane().setContent(webView);
            alert.show();
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
    final ButtonType ok = new ButtonType(movePhase ? "End Move Phase" : "Kill Planes");
    final ButtonType cancel = new ButtonType(movePhase ? "Keep Moving" : "Change Placement");
    mapPanel.centerOn(airCantLand.iterator().next());
    WebView webview = new WebView();
    webview.getEngine().loadContent(sb.toString());
    return JFXUtils.getDialogWithContent(webview, AlertType.CONFIRMATION, "Air cannot land", "", "", ok, cancel)
        .showAndWait().orElse(cancel).equals(cancel);
  }

  public boolean getOKToLetUnitsDie(final Collection<Territory> unitsCantFight, final boolean movePhase) {
    if (unitsCantFight == null || unitsCantFight.isEmpty() || messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final StringBuilder buf = new StringBuilder("Units in the following territories will die: ");
    Joiner.on(' ').appendTo(buf, FluentIterable.from(unitsCantFight).transform(unit -> unit.getName()));
    final ButtonType yes = new ButtonType(movePhase ? "Done Moving" : "Kill Units");
    final ButtonType no = new ButtonType(movePhase ? "Keep Moving" : "Change Placement");
    this.mapPanel.centerOn(unitsCantFight.iterator().next());
    return JFXUtils.getDialog(AlertType.WARNING, "Units cannot fight", buf.toString(), "", yes, no).showAndWait()
        .orElse(no).equals(no);
  }

  public boolean acceptAction(final PlayerID playerSendingProposal, final String acceptanceQuestion,
      final boolean politics) {
    if (messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    return JFXUtils
        .getDialog(AlertType.CONFIRMATION,
            "Accept " + (politics ? "Political " : "") + "Proposal from " + playerSendingProposal.getName() + "?",
            acceptanceQuestion, "", ButtonType.YES, ButtonType.NO)
        .showAndWait().orElse(ButtonType.NO).equals(ButtonType.YES);
  }

  public boolean getOK(final String message) {
    if (messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    return JFXUtils.getDialog(AlertType.CONFIRMATION, message, message, "", ButtonType.OK, ButtonType.CANCEL)
        .showAndWait().orElse(ButtonType.CANCEL).equals(ButtonType.OK);
  }

  public void notifyTechResults(final TechResults msg) {
    if (messageAndDialogThreadPool == null) {
      return;
    }
    messageAndDialogThreadPool.runTask(() -> {
      final AtomicReference<TechResultsDisplay> displayRef = new AtomicReference<>();
      SwingAction.invokeAndWait(() -> {
        final TechResultsDisplay display = new TechResultsDisplay(msg, uiContext, data);
        displayRef.set(display);
      });
      JFXUtils.getDialogWithContent(displayRef.get(), AlertType.INFORMATION, "Tech roll", "", "").showAndWait();
    });
  }

  public boolean getStrategicBombingRaid(final Territory location) {
    if (messageAndDialogThreadPool == null) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final String message =
        (Properties.getRaidsMayBePreceededByAirBattles(data) ? "Bomb/Escort" : "Bomb") + " in "
            + location.getName();

    final ButtonType normal = new ButtonType("Attack");
    final ButtonType bomb =
        new ButtonType(Properties.getRaidsMayBePreceededByAirBattles(data) ? "Bomb/Escort" : "Bomb");
    Alert alert = JFXUtils.getDialog(AlertType.CONFIRMATION, "Bomb", "", "", bomb, normal);
    Optional<ButtonType> result;
    do {
      result = alert.showAndWait();
    } while (!result.isPresent());
    return result.equals(bomb);
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
    final ListView<Unit> list = new ListView<>(FXCollections.observableArrayList(potentialTargets));
    list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    list.getSelectionModel().select(0);
    final BorderPane panel = new BorderPane();
    if (bombers != null) {
      panel.setTop(new Label("For Units: " + MyFormatter.unitsToTextNoOwner(bombers)));
    }
    panel.setCenter(new ScrollPane(list));
    JFXUtils.getDialogWithContent(panel, AlertType.CONFIRMATION, message, message, "", ButtonType.OK, ButtonType.CANCEL)
        .showAndWait().filter(ButtonType.OK::equals)
        .ifPresent(e -> selected.set(list.getSelectionModel().getSelectedItem()));
    return selected.get();
  }

  public int[] selectFixedDice(final int numDice, final int hitAt, final boolean hitOnlyIfEquals, final String title,
      final int diceSides) {
    if (messageAndDialogThreadPool == null) {
      return new int[numDice];
    }
    messageAndDialogThreadPool.waitForAll();
    final DiceChooser chooser = Util.runInSwingEventThread(
        () -> new DiceChooser(getUIContext(), numDice, hitAt, hitOnlyIfEquals, diceSides));
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
    mapPanel.centerOn(currentTerritory);
    final ListView<Territory> list = new ListView<>(FXCollections.observableArrayList(candidates));
    list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    list.getSelectionModel().select(0);
    final BorderPane panel = new BorderPane();
    final ScrollPane scroll = new ScrollPane(list);
    final TextArea text = new TextArea(unitMessage);
    text.setPrefColumnCount(20);
    text.setPrefRowCount(8);
    text.setWrapText(true);
    text.setEditable(false);
    panel.setTop(text);
    panel.setCenter(scroll);
    final String title = "Select territory for air units to land, current territory is " + currentTerritory.getName();
    JFXUtils.getDialogWithContent(panel, AlertType.INFORMATION, "Select Territory", title, "", ButtonType.OK)
        .showAndWait();
    final Territory selected = list.getSelectionModel().getSelectedItem();
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
      SwingUtilities.invokeLater(() -> {
        if (!inGame) {
          showGame();
        }
        Tab actionTab = new Tab("Actions");
        if (!tabsPanel.getTabs().contains(actionTab)) {
          // add actions tab
          tabsPanel.getTabs().add(actionTab);
          actionTab.setContent(actionButtons);
        }
        tabsPanel.getSelectionModel().select(0);
        latch1.countDown();
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
    final int index = tabsPanel == null ? -1 : tabsPanel.getTabs().indexOf(new Tab("Actions"));// TODO check if this
                                                                                               // actually works
    if (index != -1 && inHistory) {
      final CountDownLatch latch2 = new CountDownLatch(1);
      SwingUtilities.invokeLater(() -> {
        if (tabsPanel != null) {
          // remove actions tab
          tabsPanel.getTabs().remove(index);
        }
        latch2.countDown();
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
          } else {
            unitPanels.clear();
            selection.clear();
            dialog.setVisible(false);
            dialog.removeAll();
            dialog.dispose();
            ThreadUtil.sleep(500);
            run();
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
        optionPane.addPropertyChangeListener(e -> {
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
          } else {
            selection.clear();
            dialog.setVisible(false);
            dialog.removeAll();
            dialog.dispose();
            ThreadUtil.sleep(500);
            run();
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

  GameStepListener m_stepListener = (stepName, delegateName, player1, round1, stepDisplayName) -> updateStep();

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
    });
  }

  GameDataChangeListener m_dataChangeListener = new GameDataChangeListener() {
    @Override
    public void gameDataChanged(final Change change) {
      try {
        SwingUtilities.invokeLater(() -> {
          if (uiContext == null) {
            return;
          }
          if (getEditMode()) {
            if (!tabsPanel.getTabs().contains(editPanel)) {
              showEditMode();
            }
          } else {
            if (tabsPanel.getTabs().contains(editPanel)) {
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
        ClientLogger.logQuietly(e);
      }
    }
  };

  private void registerArrowKeyListener(MapPanel mapPanel2) {
    mapPanel.addEventHandler(KeyEvent.KEY_PRESSED, e -> {

      // scroll map according to wasd/arrowkeys
      final int diffPixel = computeScrollSpeed(e);
      final int x = mapPanel.getXOffset();
      final int y = mapPanel.getYOffset();
      final KeyCode keyCode = e.getCode();

      if (keyCode == KeyCode.RIGHT || keyCode == KeyCode.D) {
        getMapPanel().setTopLeft(x + diffPixel, y);
      } else if (keyCode == KeyCode.LEFT || keyCode == KeyCode.A) {
        getMapPanel().setTopLeft(x - diffPixel, y);
      } else if (keyCode == KeyCode.DOWN || keyCode == KeyCode.S) {
        getMapPanel().setTopLeft(x, y + diffPixel);
      } else if (keyCode == KeyCode.UP || keyCode == KeyCode.W) {
        getMapPanel().setTopLeft(x, y - diffPixel);
      }
      // I for info
      if (keyCode == KeyCode.I || keyCode == KeyCode.V) {
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
          Alert alert = new Alert(AlertType.NONE, "<html>" + tipText + "</html>");
          alert.show();// TODO
          new Thread(() -> {
            ThreadUtil.sleep(5000);
            alert.hide();
          }, "popup waiter").start();
        }
      }
    });
  }

  private int computeScrollSpeed(final KeyEvent e) {
    int multiplier = 1;

    if (e.isControlDown()) {
      multiplier = scrollSettings.getFasterArrowKeyScrollMultiplier();
    }


    final int starterDiffPixel = scrollSettings.getArrowKeyScrollSpeed();
    return (starterDiffPixel * multiplier);
  }

  private void showEditMode() {
    tabsPanel.getTabs().add(new Tab("Edit", editPanel));
    if (editDelegate != null) {
      tabsPanel.getTabs().forEach(e -> {
        if (e.getContent().equals(editPanel)) {
          tabsPanel.getSelectionModel().select(e);
        }
      });
    }
    editModeButtonModel.setSelected(true);
    editMode = true;
  }

  private void hideEditMode() {
    if (tabsPanel.getSelectionModel().getSelectedItem().getContent() == editPanel) {
      tabsPanel.getSelectionModel().select(0);
    }
    tabsPanel.getTabs().remove(editPanel);
    editModeButtonModel.setSelected(false);
    editMode = false;
  }

  public void showActionPanelTab() {
    tabsPanel.getSelectionModel().select(0);
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
    tabsPanel.getTabs().clear();
    tabsPanel.getTabs().add(new Tab("History", historyDetailPanel));
    tabsPanel.getTabs().add(new Tab("Stats", statsPanel));
    tabsPanel.getTabs().add(new Tab("Economy", economyPanel));
    if (objectivePanel != null && !objectivePanel.isEmpty()) {
      tabsPanel.getTabs().add(new Tab(objectivePanel.getName(), objectivePanel));
    }
    tabsPanel.getTabs().add(new Tab("Notes", notesPanel));
    tabsPanel.getTabs().add(new Tab("Territory", details));
    if (getEditMode()) {
      tabsPanel.getTabs().add(new Tab("Edit", editPanel));
    }
    if (actionButtons.getCurrent() != null) {
      actionButtons.getCurrent().setActive(false);
    }
    historyComponent.getChildren().clear();
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
        ExportMenu.saveScreenshot(historyPanel.getCurrentPopupNode(), TripleAFrame.this, data);
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
          final File f = TripleAMenuBar.getSaveGameLocationDialog(TripleAFrame.this);
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
    final SplitPane split = new SplitPane();
//    split.setDividerSize(8);TODO CSS
    historyPanel = new HistoryPanel(clonedGameData, historyDetailPanel, popup, uiContext);
    split.getItems().add(historyPanel);
    split.getItems().add(gameCenterPanel);
    split.getDividerPositions()[0] = 150;
    historyComponent.setCenter(split);
    historyComponent.setBottom(gameSouthPanel);
    content.getChildren().clear();
    content.setCenter(historyComponent);
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
      tabsPanel.getTabs().clear();
    }
    setWidgetActivation();
    tabsPanel.getTabs().add(new Tab("Action", actionButtons));
    tabsPanel.getTabs().add(new Tab("Stats", statsPanel));
    tabsPanel.getTabs().add(new Tab("Economy", economyPanel));
    if (objectivePanel != null && !objectivePanel.isEmpty()) {
      tabsPanel.getTabs().add(new Tab(objectivePanel.getName(), objectivePanel));
    }
    tabsPanel.getTabs().add(new Tab("Notes", notesPanel));
    tabsPanel.getTabs().add(new Tab("Territory", details));
    if (getEditMode()) {
      tabsPanel.getTabs().add(new Tab("Edit", editPanel));
    }
    if (actionButtons.getCurrent() != null) {
      actionButtons.getCurrent().setActive(true);
    }
    gameMainPanel.getChildren().clear();
    gameMainPanel.setCenter(gameCenterPanel);
    gameMainPanel.setBottom(gameSouthPanel);
    content.getChildren().clear();
    content.setCenter(gameMainPanel);
    mapPanel.setRoute(null);
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
      gameMainPanel.getChildren().clear();
      gameMainPanel.setCenter(mapAndChatPanel);
      gameMainPanel.setRight(rightHandSidePanel);
      gameMainPanel.setBottom(gameSouthPanel);
      content.getChildren().clear();
      content.setCenter(gameMainPanel);
      mapPanel.setRoute(null);
    } else {
      inGame = false;
    }
    uiContext.setShowMapOnly(true);
    setWidgetActivation();
  }



  private void setWidgetActivation() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> setWidgetActivation());
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
        m_showMapOnlyAction.setEnabled(!(inGame || inHistory));
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

  public EventHandler<ActionEvent> getShowCommentLogButtonModel() {
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

  private final DisableableEventHandler<ActionEvent> m_showHistoryAction = new DisableableEventHandler<>(e -> {
    showHistory();
    m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
  });
  private final DisableableEventHandler<ActionEvent> m_showGameAction = new DisableableEventHandler<>(e -> {
    showGame();
    m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
  });
  private final DisableableEventHandler<ActionEvent> m_showMapOnlyAction = new DisableableEventHandler<>(e -> {
    showMapOnly();
    m_dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
  });

  public Collection<Unit> moveFightersToCarrier(final Collection<Unit> fighters, final Territory where) {
    if (messageAndDialogThreadPool == null) {
      return null;
    }
    messageAndDialogThreadPool.waitForAll();
    mapPanel.centerOn(where);
    final AtomicReference<ScrollableTextField> textRef = new AtomicReference<>();
    final AtomicReference<BorderPane> panelRef = new AtomicReference<>();
    SwingAction.invokeAndWait(() -> {
      final BorderPane panel = new BorderPane();
      final ScrollableTextField text = new ScrollableTextField(0, fighters.size());
      // text.setBorder(new EmptyBorder(8, 8, 8, 8));TODO CSS
      panel.setCenter(text);
      panel.setTop(new Label("How many fighters do you want to move from " + where.getName() + " to new carrier?"));
      panelRef.set(panel);
      textRef.set(text);
      panelRef.set(panel);
    });
    Collection<Unit> result = new ArrayList<>(0);
    JFXUtils.getDialogWithContent(panelRef.get(), AlertType.CONFIRMATION, "Place fighters",
        "Place fighters on new carrier?", "", ButtonType.OK, ButtonType.CANCEL)
        .showAndWait().filter(ButtonType.OK::equals)
        // arrayList.subList() is not serializable
        .ifPresent(e -> result = new ArrayList<>(new ArrayList<>(fighters).subList(0, textRef.get().getValue())));
    return result;
  }

  public BattlePanel getBattlePanel() {
    return actionButtons.getBattlePanel();
  }

  public EventHandler<ActionEvent> getShowGameAction() {
    return m_showGameAction;
  }

  public EventHandler<ActionEvent> getShowHistoryAction() {
    return m_showHistoryAction;
  }

  public EventHandler<ActionEvent> getShowMapOnlyAction() {
    return m_showMapOnlyAction;
  }

  public IUIContext getUIContext() {
    return uiContext;
  }

  public MapPanel getMapPanel() {
    return mapPanel;
  }

  @Override
  public Node getMainPanel() {
    return mapPanel;
  }

  // Beagle Code Called to Change Mapskin
  public void updateMap(final String mapdir) throws IOException {
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

  public Optional<InGameLobbyWatcherWrapper> getInGameLobbyWatcher() {
    if (ServerGame.class.isAssignableFrom(getGame().getClass())) {
      final ServerGame serverGame = (ServerGame) getGame();
      return Optional.ofNullable(serverGame.getInGameLobbyWatcher());
    } else {
      return Optional.empty();
    }
  }
}
