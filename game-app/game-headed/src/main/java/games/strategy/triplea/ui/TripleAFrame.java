package games.strategy.triplea.ui;

import static games.strategy.triplea.image.UnitImageFactory.ImageKey;

import com.google.common.base.Preconditions;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.chat.PlayerChatRenderer;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.DefaultNamed;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.engine.framework.HistorySynchronizer;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameFileSelector;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.engine.random.PbemDiceRoller;
import games.strategy.triplea.EngineImageLoader;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.AirBattle;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.ScrambleLogic;
import games.strategy.triplea.delegate.battle.casualty.CasualtySelector;
import games.strategy.triplea.delegate.data.BattleListing;
import games.strategy.triplea.delegate.data.FightBattleDetails;
import games.strategy.triplea.delegate.data.TechResults;
import games.strategy.triplea.delegate.data.TechRoll;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.export.ScreenshotExporter;
import games.strategy.triplea.ui.history.HistoryDetailsPanel;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.triplea.ui.history.HistoryPanel;
import games.strategy.triplea.ui.menubar.TripleAMenuBar;
import games.strategy.triplea.ui.panel.move.MovePanel;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.triplea.util.TuvCostsCalculator;
import games.strategy.ui.ImageScrollModel;
import games.strategy.ui.ImageScrollerSmallView;
import games.strategy.ui.Util;
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
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
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
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;
import org.triplea.swing.CollapsiblePanel;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.EventThreadJOptionPane.ConfirmDialogType;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;
import org.triplea.thread.ThreadPool;
import org.triplea.util.ExitStatus;
import org.triplea.util.LocalizeHtml;
import org.triplea.util.Tuple;

/** Main frame for the triple a game. */
@Slf4j
public final class TripleAFrame extends JFrame implements QuitHandler {
  private static final long serialVersionUID = 7640069668264418976L;

  @Getter private final LocalPlayers localPlayers;
  private final GameData data;
  @Getter private final IGame game;
  @Getter private final MapPanel mapPanel;
  private final ImageScrollerSmallView smallView;

  private final ActionButtonsPanel actionButtonsPanel;
  private final JPanel gameMainPanel = new JPanel();
  private final JPanel rightHandSidePanel = new JPanel();
  private final JTabbedPane tabsPanel = new JTabbedPane();
  private final StatPanel statsPanel;
  private final TechnologyPanel technologyPanel;
  private final EconomyPanel economyPanel;
  private final Runnable clientLeftGame;
  private @Nullable ObjectivePanel objectivePanel;
  @Getter private final TerritoryDetailPanel territoryDetailPanel;
  @Getter private @Nullable HistoryPanel historyPanel;
  private final AtomicBoolean inHistory = new AtomicBoolean(false);
  private final AtomicBoolean inGame = new AtomicBoolean(true);
  private @Nullable HistorySynchronizer historySyncher;
  @Getter private UiContext uiContext;
  private final JPanel mapAndChatPanel;
  private final @Nullable ChatPanel chatPanel;
  private final CommentPanel commentPanel;
  private final JSplitPane chatSplit;
  private JSplitPane commentSplit;
  private final EditPanel editPanel;
  @Getter private final ButtonModel editModeButtonModel;
  @Getter private IEditDelegate editDelegate;
  private final JSplitPane gameCenterPanel;
  @Getter private final BottomBar bottomBar;
  private final Map<GamePlayer, Boolean> requiredTurnSeries = new HashMap<>();
  private final ThreadPool messageAndDialogThreadPool = new ThreadPool(1);
  private final MapUnitTooltipManager tooltipManager;
  private boolean isCtrlPressed = false;

  private final GameDataChangeListener dataChangeListener =
      new GameDataChangeListener() {
        @Override
        public void gameDataChanged(final Change change) {
          // Update the bottomBar, since resources may have changed, e.g. by triggers.
          bottomBar.gameDataChanged();
          SwingUtilities.invokeLater(
              () -> {
                if (mapPanel.getEditMode()) {
                  if (tabsPanel.indexOfComponent(editPanel) == -1) {
                    showEditMode();
                  }
                } else {
                  if (tabsPanel.indexOfComponent(editPanel) != -1) {
                    hideEditMode();
                  }
                }
                rightHandSidePanel.setVisible(true);
              });
        }

        private void hideEditMode() {
          if (tabsPanel.getSelectedComponent() == editPanel) {
            tabsPanel.setSelectedIndex(0);
          }
          tabsPanel.remove(editPanel);
          editModeButtonModel.setSelected(false);
          getGlassPane().setVisible(false);
        }
      };

  @Getter
  private final Action showHistoryAction =
      SwingAction.of(
          "Show history",
          e -> {
            showHistory();
            dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
          });

  @Getter
  private final Action showGameAction =
      new AbstractAction("Show current game") {
        @Serial private static final long serialVersionUID = -7551760679570164254L;

        {
          setEnabled(false);
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
          showGame();
          dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
        }
      };

  @Getter
  private final AdditionalTerritoryDetails additionalTerritoryDetails =
      new AdditionalTerritoryDetails();

  private TripleAFrame(
      final IGame game,
      final LocalPlayers players,
      final UiContext uiContext,
      @Nullable final Chat chat,
      final Runnable clientLeftGame) {
    super("TripleA - " + game.getData().getGameName());
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    this.clientLeftGame = clientLeftGame;

    localPlayers = players;
    setIconImage(EngineImageLoader.loadFrameIcon());
    // 200 size is pretty arbitrary, goal is to not allow users to shrink window down to nothing.
    setMinimumSize(new Dimension(200, 200));

    this.game = game;
    data = game.getData();
    addZoomKeyboardShortcuts();
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(final WindowEvent e) {
            leaveGame();
          }
        });
    addWindowFocusListener(
        new WindowAdapter() {
          @Override
          public void windowGainedFocus(final WindowEvent e) {
            mapPanel.requestFocusInWindow();
          }
        });
    this.uiContext = uiContext;
    this.setCursor(uiContext.getCursor());
    editModeButtonModel = new JToggleButton.ToggleButtonModel();
    editModeButtonModel.setEnabled(false);

    SwingUtilities.invokeLater(() -> this.setJMenuBar(new TripleAMenuBar(this)));
    final ImageScrollModel model = new ImageScrollModel();
    model.setMaxBounds(
        uiContext.getMapData().getMapDimensions().width,
        uiContext.getMapData().getMapDimensions().height);
    model.setScrollX(uiContext.getMapData().scrollWrapX());
    model.setScrollY(uiContext.getMapData().scrollWrapY());
    final Image small = uiContext.getMapImage().getSmallMapImage();
    smallView = new ImageScrollerSmallView(small, model, uiContext.getMapData());
    mapPanel = new MapPanel(data, smallView, uiContext, model, this::computeScrollSpeed);
    tooltipManager = new MapUnitTooltipManager(mapPanel);
    mapPanel.addMapSelectionListener(
        new DefaultMapSelectionListener() {
          @Override
          public void mouseEntered(final Territory territory) {
            bottomBar.setTerritory(territory);
          }
        });
    mapPanel.addMouseOverUnitListener(
        (units, territory) -> tooltipManager.updateTooltip(getUnitInfo()));
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
      chatPanel = new ChatPanel(chat, ChatSoundProfile.GAME, getUiContext().getClipPlayer());
      chatPanel.setPlayerRenderer(new PlayerChatRenderer(this.game, uiContext));
      final Dimension chatPrefSize =
          new Dimension((int) chatPanel.getPreferredSize().getWidth(), 95);
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

    final boolean usingDiceServer = (game.getRandomSource() instanceof PbemDiceRoller);
    bottomBar = new BottomBar(uiContext, data, usingDiceServer);

    gameMainPanel.add(bottomBar, BorderLayout.SOUTH);
    rightHandSidePanel.setLayout(new BorderLayout());
    final FocusAdapter focusToMapPanelFocusListener =
        new FocusAdapter() {
          @Override
          public void focusGained(final FocusEvent e) {
            // give the focus back to the map panel
            mapPanel.requestFocus();
          }
        };
    rightHandSidePanel.addFocusListener(focusToMapPanelFocusListener);
    smallView.addFocusListener(focusToMapPanelFocusListener);
    tabsPanel.addFocusListener(focusToMapPanelFocusListener);
    rightHandSidePanel.add(smallView, BorderLayout.NORTH);
    tabsPanel.setBorder(null);
    rightHandSidePanel.add(tabsPanel, BorderLayout.CENTER);

    final MovePanel movePanel = new MovePanel(this);
    actionButtonsPanel = new ActionButtonsPanel(movePanel, this);

    final CollapsiblePanel placementsPanel =
        new PlacementUnitsCollapsiblePanel(data, uiContext).getPanel();
    rightHandSidePanel.add(
        new JPanelBuilder()
            .borderLayout()
            .addNorth(placementsPanel)
            .addSouth(movePanel.getUnitScrollerPanel())
            .build(),
        BorderLayout.SOUTH);

    SwingUtilities.invokeLater(() -> mapPanel.addKeyListener(getArrowKeyListener()));

    actionButtonsPanel.setBorder(null);
    statsPanel = new StatPanel(data, uiContext);
    technologyPanel = new TechnologyPanel(data, uiContext);
    economyPanel = new EconomyPanel(data, uiContext);
    objectivePanel = new ObjectivePanel(data, uiContext);
    if (objectivePanel.isEmpty()) {
      objectivePanel.removeDataChangeListener();
      objectivePanel = null;
    }
    territoryDetailPanel = new TerritoryDetailPanel(this);
    editPanel = new EditPanel(this);
    addTabs(null);
    // Register a change listener
    tabsPanel.addChangeListener(
        evt -> {
          final JTabbedPane pane = (JTabbedPane) evt.getSource();
          // Get current tab
          final int sel = pane.getSelectedIndex();
          if (sel == -1) {
            return;
          }
          if (pane.getComponentAt(sel).equals(editPanel)) {
            final GamePlayer player1;
            try (GameData.Unlocker ignored = data.acquireReadLock()) {
              player1 = data.getSequence().getStep().getPlayerId();
            }
            actionButtonsPanel.getCurrent().ifPresent(actionPanel -> actionPanel.setActive(false));
            editPanel.display(player1);
          } else {
            actionButtonsPanel.getCurrent().ifPresent(actionPanel -> actionPanel.setActive(true));
            editPanel.setActive(false);
          }
        });
    rightHandSidePanel.setPreferredSize(
        new Dimension(
            (int) smallView.getPreferredSize().getWidth(),
            (int) mapPanel.getPreferredSize().getHeight()));
    gameCenterPanel =
        new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapAndChatPanel, rightHandSidePanel);
    gameCenterPanel.setOneTouchExpandable(true);
    gameCenterPanel.setContinuousLayout(true);
    gameCenterPanel.setDividerSize(8);
    gameCenterPanel.setResizeWeight(1.0);
    gameMainPanel.add(gameCenterPanel, BorderLayout.CENTER);
    gameCenterPanel.resetToPreferredSizes();
    // set up the edit mode overlay text
    this.setGlassPane(
        new JComponent() {
          private static final long serialVersionUID = 6724687534214427291L;

          @Override
          protected void paintComponent(final Graphics g) {
            g.setFont(new Font(MapImage.FONT_FAMILY_DEFAULT, Font.BOLD, 50));
            g.setColor(new Color(255, 255, 255, 175));
            final Dimension size = mapPanel.getSize();
            g.drawString(
                "Edit Mode",
                (int) ((size.getWidth() - 200) / 2),
                (int) ((size.getHeight() - 100) / 2));
          }
        });
    // force a data change event to update the UI for edit mode
    dataChangeListener.gameDataChanged(ChangeFactory.EMPTY_CHANGE);
    data.addDataChangeListener(dataChangeListener);
    data.addGameDataEventListener(GameDataEvent.GAME_STEP_CHANGED, this::updateStep);
    // Clear cached unit images when getting standard tech like jet power.
    data.addGameDataEventListener(
        GameDataEvent.TECH_ATTACHMENT_CHANGED, this::clearCachedUnitImages);
    uiContext.addShutdownWindow(this);
    mapPanel.addZoomMapListener(bottomBar);
  }

  private void clearCachedUnitImages() {
    uiContext.getUnitImageFactory().clearCache();
  }

  /**
   * Constructs a new instance of a TripleAFrame, but executes required IO-Operations off the EDT.
   */
  public static TripleAFrame create(
      final IGame game,
      final LocalPlayers players,
      @Nullable final Chat chat,
      final Runnable clientLeftGame) {
    Preconditions.checkState(
        !SwingUtilities.isEventDispatchThread(), "This method must not be called on the EDT");

    final UiContext uiContext = new UiContext(game.getData());
    game.setResourceLoader(uiContext.getResourceLoader());
    uiContext.getMapData().verify(game.getData());
    uiContext.setLocalPlayers(players);

    final TripleAFrame frame =
        Interruptibles.awaitResult(
                () ->
                    SwingAction.invokeAndWaitResult(
                        () -> new TripleAFrame(game, players, uiContext, chat, clientLeftGame)))
            .result
            .orElseThrow(() -> new IllegalStateException("Error while instantiating TripleAFrame"));
    frame.updateStep();
    return frame;
  }

  public void hideCommentLog() {
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

  public void showCommentLog() {
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

  private void addZoomKeyboardShortcuts() {
    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        this,
        KeyCode.EQUALS,
        () ->
            mapPanel.setScale(
                mapPanel.getScale() + (ClientSetting.mapZoomFactor.getValueOrThrow() / 100f)));

    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        this,
        KeyCode.MINUS,
        () ->
            mapPanel.setScale(
                mapPanel.getScale() - (ClientSetting.mapZoomFactor.getValueOrThrow() / 100f)));
  }

  private void addTabs(HistoryDetailsPanel historyDetailPanel) {
    if (historyDetailPanel != null) {
      tabsPanel.add("History", historyDetailPanel);
    } else {
      addTab("Actions", actionButtonsPanel, KeyCode.C);
    }
    addTab("Players", statsPanel, KeyCode.P);
    if (!TechAdvance.getTechAdvances(data.getTechnologyFrontier(), null).isEmpty()) {
      addTab("Technology", technologyPanel, KeyCode.E);
    }
    addTab("Resources", economyPanel, KeyCode.R);
    if (objectivePanel != null) {
      String objectivePanelName = new ObjectiveProperties(uiContext.getResourceLoader()).getName();
      addTab(objectivePanelName, objectivePanel, KeyCode.O);
    }
    addTab("Territory", territoryDetailPanel, KeyCode.T);
    if (mapPanel.getEditMode()) {
      showEditMode();
    }
  }

  private void addTab(final String title, final Component component, final KeyCode hotkey) {
    tabsPanel.addTab(title, null, component, "Hotkey: CTRL+" + hotkey);
    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        this,
        hotkey,
        () -> tabsPanel.setSelectedIndex(List.of(tabsPanel.getComponents()).indexOf(component)));
  }

  /** Stops the game and closes this frame window. */
  public void stopGame() {
    this.setVisible(false);
    TripleAFrame.this.dispose();
    messageAndDialogThreadPool.shutdown();
    uiContext.shutDown();
    if (chatPanel != null) {
      chatPanel.setPlayerRenderer(null);
      chatPanel.deleteChat();
    }
    if (historySyncher != null) {
      historySyncher.deactivate();
      historySyncher = null;
    }
    bottomBar.setTerritory(null);
    GameShutdownRegistry.runShutdownActions();
  }

  /**
   * If the frame is visible, prompts the user if they wish to exit the application. If they answer
   * yes or the frame is not visible, the game will be stopped, and the process will be terminated.
   */
  @Override
  public boolean shutdown() {
    if (isVisible()) {

      final boolean confirmed =
          EventThreadJOptionPane.showConfirmDialog(
              this,
              "Are you sure you want to exit TripleA?\nUnsaved game data will be lost.",
              "Exit Program",
              ConfirmDialogType.YES_NO);
      if (!confirmed) {
        return false;
      }
    }

    stopGame();
    ExitStatus.SUCCESS.exit();
    return true;
  }

  /**
   * Prompts the user if they wish to leave the game. If they answer yes, the game will be stopped,
   * and the application will return to the main menu.
   */
  public void leaveGame() {
    final boolean confirmed =
        EventThreadJOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to leave the current game?\nUnsaved game data will be lost.",
            "Leave Game",
            ConfirmDialogType.YES_NO);
    if (!confirmed) {
      return;
    }
    if (game instanceof ServerGame) {
      ((ServerGame) game).stopGame();
    } else {
      game.getMessengers().shutDown();
      ((ClientGame) game).shutDown();
      // an ugly hack, we need a better way to get the main frame
      new Thread(clientLeftGame).start();
    }
  }

  void clearStatusMessage() {
    bottomBar.setStatusAndClearIcon("");
  }

  public void setStatusErrorMessage(final String msg) {
    final Optional<Image> errorImage = mapPanel.getErrorImage();
    if (errorImage.isPresent()) bottomBar.setStatus(msg, errorImage.get());
    else bottomBar.setStatusAndClearIcon(msg);
  }

  public void setStatusWarningMessage(final String msg) {
    final Optional<Image> warningImage = mapPanel.getWarningImage();
    if (warningImage.isPresent()) bottomBar.setStatus(msg, warningImage.get());
    else bottomBar.setStatusAndClearIcon(msg);
  }

  public IntegerMap<ProductionRule> getProduction(
      final GamePlayer player, final boolean bid, final boolean keepCurrentPurchase) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToProduce(player, keepCurrentPurchase);
    return actionButtonsPanel.waitForPurchase(bid);
  }

  public Map<Unit, IntegerMap<RepairRule>> getRepair(
      final GamePlayer player,
      final boolean bid,
      final Collection<GamePlayer> allowedPlayersToRepair) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToRepair(player);
    return actionButtonsPanel.waitForRepair(bid, allowedPlayersToRepair);
  }

  public MoveDescription getMove(
      final GamePlayer player,
      final PlayerBridge bridge,
      final boolean nonCombat,
      final String stepName) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToMove(player, nonCombat, stepName);
    // workaround for panel not receiving focus at beginning of n/c move phase
    if (!getBattlePanel().isBattleShowing()) {
      requestWindowFocus();
    }
    return actionButtonsPanel.waitForMove(bridge);
  }

  private void requestWindowFocus() {
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () -> {
                  requestFocusInWindow();
                  transferFocus();
                }));
  }

  public PlaceData waitForPlace(
      final GamePlayer player, final boolean bid, final PlayerBridge bridge) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToPlace(player);
    return actionButtonsPanel.waitForPlace(bid, bridge);
  }

  public void waitForMoveForumPoster(final GamePlayer player, final PlayerBridge bridge) {
    if (actionButtonsPanel == null) {
      return;
    }
    actionButtonsPanel.changeToMoveForumPosterPanel(player);
    actionButtonsPanel.waitForMoveForumPosterPanel(this, bridge);
  }

  public void waitForEndTurn(final GamePlayer player, final PlayerBridge bridge) {
    if (actionButtonsPanel == null) {
      return;
    }
    actionButtonsPanel.changeToEndTurn(player);
    actionButtonsPanel.waitForEndTurn(this, bridge);
  }

  public FightBattleDetails getBattle(final GamePlayer player, final BattleListing battles) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToBattle(player, battles);
    return actionButtonsPanel.waitForBattleSelection();
  }

  /** We do NOT want to block the next player from beginning their turn. */
  public void notifyError(final String message) {
    final String displayMessage =
        LocalizeHtml.localizeImgLinksInHtml(message, uiContext.getMapLocation());
    showMessageDialog(displayMessage, "Error", JOptionPane.ERROR_MESSAGE);
  }

  /** We do NOT want to block the next player from beginning their turn. */
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
    if (title.equals(AbstractTriggerAttachment.NOTIFICATION)
        && !getUiContext().getShowTriggeredNotifications()) {
      return;
    }
    if (title.contains(AbstractEndTurnDelegate.END_TURN_REPORT_STRING)
        && message.contains(AbstractEndTurnDelegate.END_TURN_REPORT_STRING)
        && !getUiContext().getShowEndOfTurnReport()) {
      return;
    }
    final String displayMessage =
        LocalizeHtml.localizeImgLinksInHtml(message, uiContext.getMapLocation());
    showMessageDialog(displayMessage, title, JOptionPane.INFORMATION_MESSAGE);
  }

  private void showMessageDialog(String displayMessage, String title, int type) {
    try {
      messageAndDialogThreadPool.submit(
          () ->
              EventThreadJOptionPane.showMessageDialogWithScrollPane(
                  TripleAFrame.this,
                  displayMessage,
                  title,
                  type,
                  getUiContext().getCountDownLatchHandler()));
    } catch (RejectedExecutionException e) {
      // The thread pool may have been shutdown. Nothing to do.
    }
  }

  /**
   * Prompts the user with a list of territories that have air units that either cannot land
   * (movement action) or be placed (placement action) in the territory. The user is asked whether
   * they wish to end the current movement/placement action, which will destroy the air units in the
   * specified territories, or if they wish to continue the current movement/placement action in
   * order to possibly move/place those units in a different territory.
   *
   * @param gamePlayer The player performing the movement/placement action.
   * @param airCantLand The collection of territories that have air units that either cannot land or
   *     be placed in them.
   * @param movePhase {@code true} if a movement action is active; otherwise {@code false} if a
   *     placement action is active.
   * @return {@code true} if the user wishes to end the current movement/placement action, and thus
   *     destroy the affected air units; otherwise {@code false} if the user wishes to continue the
   *     current movement/placement action.
   */
  public boolean getOkToLetAirDie(
      final GamePlayer gamePlayer,
      final Collection<Territory> airCantLand,
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
    final boolean lhtrProd =
        Properties.getLhtrCarrierProductionRules(data.getProperties())
            || Properties.getLandExistingFightersOnNewCarriers(data.getProperties());
    final int carrierCount =
        GameStepPropertiesHelper.getCombinedTurns(data, gamePlayer).stream()
            .map(GamePlayer::getUnitCollection)
            .map(units -> units.getMatches(Matches.unitIsCarrier()))
            .mapToInt(List::size)
            .sum();
    final boolean canProduceCarriersUnderFighter = lhtrProd && carrierCount != 0;
    if (canProduceCarriersUnderFighter && carrierCount > 0) {
      sb.append("\nYou have ")
          .append(carrierCount)
          .append(" ")
          .append(MyFormatter.pluralize("carrier", carrierCount))
          .append(" on which planes can land");
    }
    final String ok = movePhase ? "End Move Phase" : "Kill Planes";
    final String cancel = movePhase ? "Keep Moving" : "Change Placement";
    final String[] options = {cancel, ok};
    mapPanel.centerOn(CollectionUtils.getAny(airCantLand));
    final int choice =
        EventThreadJOptionPane.showOptionDialog(
            this,
            sb.toString(),
            "Air cannot land",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            cancel,
            getUiContext().getCountDownLatchHandler());
    return choice == JOptionPane.NO_OPTION;
  }

  /**
   * Prompts the user with a list of territories that have units that cannot fight in the territory.
   * The user is asked whether they wish to end the current movement action, which will destroy the
   * units in the specified territories, or if they wish to continue the current movement action in
   * order to possibly move those units to a different territory.
   *
   * @param unitsCantFight The collection of territories that have units that cannot fight.
   * @return {@code true} if the user wishes to end the current movement action, and thus destroy
   *     the affected units; otherwise {@code false} if the user wishes to continue the current
   *     movement action.
   */
  public boolean getOkToLetUnitsDie(final Collection<Territory> unitsCantFight) {
    if (unitsCantFight == null || unitsCantFight.isEmpty()) {
      return true;
    }
    messageAndDialogThreadPool.waitForAll();
    final String message =
        unitsCantFight.stream()
            .map(DefaultNamed::getName)
            .collect(Collectors.joining(" ", "Units in the following territories will die: ", ""));
    final String ok = "Done Moving";
    final String cancel = "Keep Moving";
    final String[] options = {cancel, ok};
    this.mapPanel.centerOn(CollectionUtils.getAny(unitsCantFight));
    final int choice =
        EventThreadJOptionPane.showOptionDialog(
            this,
            message,
            "Units cannot fight",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            cancel,
            getUiContext().getCountDownLatchHandler());
    return choice == JOptionPane.NO_OPTION;
  }

  /** Asks a given player if they wish confirm a given political action. */
  public boolean acceptAction(
      final GamePlayer playerSendingProposal,
      final String acceptanceQuestion,
      final boolean politics) {
    messageAndDialogThreadPool.waitForAll();

    return EventThreadJOptionPane.showConfirmDialog(
        this,
        acceptanceQuestion,
        "Accept "
            + (politics ? "Political " : "")
            + "Proposal from "
            + playerSendingProposal.getName()
            + "?",
        ConfirmDialogType.YES_NO);
  }

  public boolean getOk(final Object message, final String title) {
    messageAndDialogThreadPool.waitForAll();
    return EventThreadJOptionPane.showConfirmDialog(
        this, message, title, ConfirmDialogType.OK_CANCEL);
  }

  /** Displays a message to the user informing them of the results of rolling for technologies. */
  public void notifyTechResults(final TechResults msg) {
    final Supplier<TechResultsDisplay> action = () -> new TechResultsDisplay(msg, uiContext, data);
    messageAndDialogThreadPool.submit(
        () ->
            Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action))
                .result
                .ifPresent(
                    display ->
                        EventThreadJOptionPane.showOptionDialog(
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
   * Prompts the user if the applicable air units in the specified territory are to perform a
   * strategic bombing raid or are to participate in the attack.
   *
   * @param location The territory under attack.
   * @return {@code true} if the applicable air units will perform a strategic bombing raid in the
   *     specified territory; otherwise {@code false}.
   */
  public boolean getStrategicBombingRaid(final Territory location) {
    messageAndDialogThreadPool.waitForAll();
    final String message =
        (Properties.getRaidsMayBePreceededByAirBattles(data.getProperties())
                ? "Bomb/Escort"
                : "Bomb")
            + " in "
            + location.getName();
    final String bomb =
        (Properties.getRaidsMayBePreceededByAirBattles(data.getProperties())
            ? "Bomb/Escort"
            : "Bomb");
    final String normal = "Attack";
    final String[] choices = {bomb, normal};
    int choice = -1;
    while (choice < 0 || choice > 1) {
      choice =
          EventThreadJOptionPane.showOptionDialog(
              this,
              message,
              "Bomb?",
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.INFORMATION_MESSAGE,
              null,
              choices,
              bomb,
              getUiContext().getCountDownLatchHandler());
    }
    return choice == JOptionPane.OK_OPTION;
  }

  /**
   * Prompts the user to select a strategic bombing raid target in the specified territory.
   *
   * @param territory The territory under attack.
   * @param potentialTargets The potential bombing targets.
   * @param bombers The units participating in the strategic bombing raid.
   * @return The selected target or {@code null} if no target was selected.
   */
  public @Nullable Unit getStrategicBombingRaidTarget(
      final Territory territory,
      final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    if (potentialTargets.size() == 1) {
      return CollectionUtils.getAny(potentialTargets);
    }
    messageAndDialogThreadPool.waitForAll();
    final AtomicReference<Unit> selected = new AtomicReference<>();
    final String message = "Select bombing target in " + territory.getName();
    final Supplier<Tuple<JPanel, JList<Unit>>> action =
        () -> {
          final JList<Unit> list = new JList<>(SwingComponents.newListModel(potentialTargets));
          list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
          list.setSelectedIndex(0);
          list.setCellRenderer(new UnitRenderer());
          final JPanel panel = new JPanel();
          panel.setLayout(new BorderLayout());
          if (bombers != null) {
            panel.add(
                new JLabel("For Units: " + MyFormatter.unitsToTextNoOwner(bombers)),
                BorderLayout.NORTH);
          }
          final JScrollPane scroll = new JScrollPane(list);
          panel.add(scroll, BorderLayout.CENTER);
          return Tuple.of(panel, list);
        };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action))
        .result
        .map(
            comps -> {
              final JPanel panel = comps.getFirst();
              final JList<?> list = comps.getSecond();
              final String[] options = {"OK", "Cancel"};
              final int selection =
                  EventThreadJOptionPane.showOptionDialog(
                      this,
                      panel,
                      message,
                      JOptionPane.OK_CANCEL_OPTION,
                      JOptionPane.PLAIN_MESSAGE,
                      null,
                      options,
                      null,
                      getUiContext().getCountDownLatchHandler());
              if (selection == 0) {
                selected.set((Unit) list.getSelectedValue());
              }
              return selected.get();
            })
        .orElse(null);
  }

  /** Create a unit option with icon and description. */
  private class UnitRenderer extends JLabel implements ListCellRenderer<Unit> {

    private static final long serialVersionUID = 1749164256040268579L;

    UnitRenderer() {
      setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(
        final JList<? extends Unit> list,
        final Unit unit,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      setText(unit.toString() + ", damage=" + unit.getUnitDamage());
      setIcon(uiContext.getUnitImageFactory().getIcon(ImageKey.of(unit)));
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
   * Prompts the user to select a collection of dice rolls. The user is presented with a dialog that
   * allows them to choose from all possible rolls for a die with the specified number of sides.
   * Each roll is rendered depending on whether it will result in a hit or a miss.
   *
   * @param numDice The number of dice the user is required to select.
   * @param hitAt The value at which the roll is considered a hit.
   * @param title The dialog title.
   * @param diceSides The number of sides on a die.
   * @return The selected dice rolls.
   */
  public int[] selectFixedDice(
      final int numDice, final int hitAt, final String title, final int diceSides) {
    messageAndDialogThreadPool.waitForAll();
    return Interruptibles.awaitResult(
            () ->
                SwingAction.invokeAndWaitResult(
                    () -> new DiceChooser(getUiContext(), numDice, hitAt, diceSides)))
        .result
        .map(
            chooser -> {
              do {
                EventThreadJOptionPane.showMessageDialog(
                    null,
                    chooser,
                    title,
                    JOptionPane.PLAIN_MESSAGE,
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
      return CollectionUtils.getAny(candidates);
    }
    messageAndDialogThreadPool.waitForAll();
    final Supplier<SelectTerritoryComponent> action =
        () -> {
          var panel = new SelectTerritoryComponent(currentTerritory, candidates, mapPanel);
          panel.setLabelText(unitMessage);
          return panel;
        };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action))
        .result
        .map(
            panel -> {
              final String[] options = {"OK"};
              final String title =
                  "Select territory for air units to land, current territory is "
                      + currentTerritory.getName();
              EventThreadJOptionPane.showOptionDialog(
                  this,
                  panel,
                  title,
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.PLAIN_MESSAGE,
                  null,
                  options,
                  null,
                  getUiContext().getCountDownLatchHandler());
              return panel.getSelection();
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
   * @return A tuple whose first element is the selected territory and whose second element is the
   *     collection of selected units.
   */
  public Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(
      final GamePlayer player,
      final List<Territory> territoryChoices,
      final List<Unit> unitChoices,
      final int unitsPerPick) {
    // total hacks
    messageAndDialogThreadPool.waitForAll();
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () -> {
                  uiContext.setCurrentPlayer(player);
                  bottomBar.updateFromCurrentPlayer();
                  if (inGame.compareAndSet(false, true)) {
                    showGame();
                  }
                  if (tabsPanel.indexOfTab("Actions") == -1) {
                    // add actions tab
                    tabsPanel.insertTab("Actions", null, actionButtonsPanel, null, 0);
                  }
                  tabsPanel.setSelectedIndex(0);
                }));
    actionButtonsPanel.changeToPickTerritoryAndUnits(player);
    final Tuple<Territory, Set<Unit>> territoryAndUnits =
        actionButtonsPanel.waitForPickTerritoryAndUnits(
            territoryChoices, unitChoices, unitsPerPick);
    final int index = tabsPanel.indexOfTab("Actions");
    if (index != -1 && inHistory.get()) {
      // remove actions tab
      Interruptibles.await(() -> SwingAction.invokeAndWait(() -> tabsPanel.remove(index)));
    }
    actionButtonsPanel.getCurrent().ifPresent(actionPanel -> actionPanel.setActive(false));
    return territoryAndUnits;
  }

  /**
   * Prompts the user to select the units that may participate in a suicide attack.
   *
   * @param possibleUnitsToAttack The possible units that may participate in the suicide attack from
   *     which the user may select.
   * @param attackResourceToken The resource that is expended to conduct the suicide attack.
   * @param maxNumberOfAttacksAllowed The maximum number of units that can be selected.
   * @return A map of units that will participate in the suicide attack grouped by the territory
   *     from which they are attacking.
   */
  public Map<Territory, IntegerMap<Unit>> selectKamikazeSuicideAttacks(
      final Map<Territory, Collection<Unit>> possibleUnitsToAttack,
      final Resource attackResourceToken,
      final int maxNumberOfAttacksAllowed) {
    Util.ensureNotOnEventDispatchThread();
    final Map<Territory, IntegerMap<Unit>> selection = new HashMap<>();
    if (possibleUnitsToAttack == null
        || possibleUnitsToAttack.isEmpty()
        || attackResourceToken == null
        || maxNumberOfAttacksAllowed <= 0) {
      return selection;
    }
    messageAndDialogThreadPool.waitForAll();
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Collection<IndividualUnitPanelGrouped> unitPanels = new ArrayList<>();
    SwingUtilities.invokeLater(
        () -> {
          final Map<String, Collection<Unit>> possibleUnitsToAttackStringForm = new HashMap<>();
          final TuvCostsCalculator tuvCalculator = new TuvCostsCalculator();
          for (final Map.Entry<Territory, Collection<Unit>> entry :
              possibleUnitsToAttack.entrySet()) {
            final List<Unit> units = new ArrayList<>(entry.getValue());
            final List<Unit> sortedUnits =
                CasualtySelector.getCasualtyOrderOfLoss(
                    units,
                    units.get(0).getOwner(),
                    CombatValueBuilder.mainCombatValue()
                        .enemyUnits(List.of())
                        .friendlyUnits(List.of())
                        .side(BattleState.Side.OFFENSE)
                        .gameSequence(data.getSequence())
                        .supportAttachments(data.getUnitTypeList().getSupportRules())
                        .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                        .gameDiceSides(data.getDiceSides())
                        .territoryEffects(TerritoryEffectHelper.getEffects(entry.getKey()))
                        .build(),
                    entry.getKey(),
                    tuvCalculator.getCostsForTuv(units.get(0).getOwner()),
                    data);
            // OOL is ordered with the first unit the owner would want to remove but in a kamikaze
            // the player who picks is the attacker, so flip the order
            Collections.reverse(sortedUnits);
            possibleUnitsToAttackStringForm.put(entry.getKey().getName(), sortedUnits);
          }
          mapPanel.centerOn(
              data.getMap()
                  .getTerritoryOrNull(
                      CollectionUtils.getAny(possibleUnitsToAttackStringForm.keySet())));
          final IndividualUnitPanelGrouped unitPanel =
              new IndividualUnitPanelGrouped(
                  possibleUnitsToAttackStringForm,
                  uiContext,
                  "Select Units to Suicide Attack using " + attackResourceToken.getName(),
                  maxNumberOfAttacksAllowed,
                  true,
                  false);
          unitPanels.add(unitPanel);
          final String optionAttack = "Attack";
          final String optionNone = "None";
          final Object[] options = {optionAttack, optionNone};
          final JOptionPane optionPane =
              new JOptionPane(
                  unitPanel,
                  JOptionPane.PLAIN_MESSAGE,
                  JOptionPane.YES_NO_CANCEL_OPTION,
                  null,
                  options,
                  options[1]);
          final JDialog dialog =
              new JDialog(
                  (Frame) getParent(),
                  "Select units to Suicide Attack using " + attackResourceToken.getName());
          dialog.setContentPane(optionPane);
          dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
          dialog.setLocationRelativeTo(getParent());
          dialog.setAlwaysOnTop(true);
          dialog.pack();
          dialog.setVisible(true);
          dialog.requestFocusInWindow();
          optionPane.addPropertyChangeListener(
              e -> {
                if (!dialog.isVisible()) {
                  return;
                }
                // Note: getValue() can return either an int (user pressed escape), or a String.
                final Object option = optionPane.getValue();
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
                    for (final Map.Entry<String, IntegerMap<Unit>> entry :
                        terrChooser.getSelected().entrySet()) {
                      selection.put(
                          data.getMap().getTerritoryOrNull(entry.getKey()), entry.getValue());
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
   * Prompts the user to select units which will participate in a scramble to the specified
   * territory.
   *
   * @param scrambleTo The territory to which units are to be scrambled.
   * @param possibleScramblers The possible units that may participate in the scramble from which
   *     the user may select.
   * @return A map of units that will participate in the scramble grouped by the territory from
   *     which they are scrambling.
   */
  public Map<Territory, Collection<Unit>> scrambleUnitsQuery(
      final Territory scrambleTo,
      final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers) {
    messageAndDialogThreadPool.waitForAll();
    Util.ensureNotOnEventDispatchThread();
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Map<Territory, Collection<Unit>> selection = new HashMap<>();
    final Collection<Tuple<Territory, UnitChooser>> choosers = new ArrayList<>();
    SwingUtilities.invokeLater(
        () -> {
          mapPanel.centerOn(scrambleTo);
          final JDialog dialog =
              new JDialog(this, "Select units to scramble to " + scrambleTo.getName());
          final JPanel panel = new JPanel();
          panel.setLayout(new BorderLayout());
          final JButton scrambleButton = new JButton("Scramble");
          scrambleButton.addActionListener(
              e -> getOptionPane((JComponent) e.getSource()).setValue(scrambleButton));
          final JButton noneButton = new JButton("None");
          noneButton.addActionListener(
              e -> getOptionPane((JComponent) e.getSource()).setValue(noneButton));
          final Object[] options = {scrambleButton, noneButton};
          final JOptionPane optionPane =
              new JOptionPane(
                  panel,
                  JOptionPane.PLAIN_MESSAGE,
                  JOptionPane.YES_NO_CANCEL_OPTION,
                  null,
                  options,
                  options[1]);
          final JLabel whereTo = new JLabel("Scramble To: " + scrambleTo.getName());
          whereTo.setFont(new Font(MapImage.FONT_FAMILY_DEFAULT, Font.ITALIC, 12));
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
            whereFrom.setFont(new Font(MapImage.FONT_FAMILY_DEFAULT, Font.BOLD, 12));
            panelChooser.add(whereFrom);
            panelChooser.add(new JLabel(" "));
            final Collection<Unit> possible = possibleScramblers.get(from).getSecond();
            final int maxAllowed =
                Math.min(
                    ScrambleLogic.getMaxScrambleCount(possibleScramblers.get(from).getFirst()),
                    possible.size());
            final UnitChooser chooser = new UnitChooser(possible, Map.of(), false, uiContext);
            chooser.setMaxAndShowMaxButton(maxAllowed);
            chooser.addChangeListener(
                field -> {
                  final Map<GamePlayer, ResourceCollection> playerFuelCost = new HashMap<>();
                  for (final Tuple<Territory, UnitChooser> tuple : choosers) {
                    final Map<GamePlayer, ResourceCollection> map =
                        Route.getScrambleFuelCostCharge(
                            tuple.getSecond().getSelected(false),
                            tuple.getFirst(),
                            scrambleTo,
                            data);
                    for (final Map.Entry<GamePlayer, ResourceCollection> playerAndCost :
                        map.entrySet()) {
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
                  for (final Map.Entry<GamePlayer, ResourceCollection> entry :
                      playerFuelCost.entrySet()) {
                    final JLabel label = new JLabel(entry.getKey().getName() + ": ");
                    fuelCostPanel.add(
                        label,
                        new GridBagConstraints(
                            0,
                            count,
                            1,
                            1,
                            0,
                            0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.NONE,
                            new Insets(0, 0, 0, 0),
                            0,
                            0));
                    fuelCostPanel.add(
                        uiContext.getResourceImageFactory().getResourcesPanel(entry.getValue()),
                        new GridBagConstraints(
                            1,
                            count++,
                            1,
                            1,
                            0,
                            0,
                            GridBagConstraints.WEST,
                            GridBagConstraints.NONE,
                            new Insets(0, 0, 0, 0),
                            0,
                            0));
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
          optionPane.addPropertyChangeListener(
              e -> {
                if (!dialog.isVisible()) {
                  return;
                }
                // Note: getValue() can return either an int (user pressed escape), or a JButton.
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

  private static JOptionPane getOptionPane(final JComponent parent) {
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
   * @return The collection of units selected by the user.
   */
  public Collection<Unit> selectUnitsQuery(
      final Territory current, final Collection<Unit> possible, final String message) {
    messageAndDialogThreadPool.waitForAll();
    Util.ensureNotOnEventDispatchThread();
    final CountDownLatch continueLatch = new CountDownLatch(1);
    final Collection<Unit> selection = new ArrayList<>();
    SwingUtilities.invokeLater(
        () -> {
          mapPanel.centerOn(current);
          final JPanel panel = new JPanel();
          panel.setLayout(new BorderLayout());
          final JLabel messageLabel = new JLabel(message);
          messageLabel.setFont(new Font(MapImage.FONT_FAMILY_DEFAULT, Font.ITALIC, 12));
          panel.add(messageLabel, BorderLayout.NORTH);
          final JPanel panelChooser = new JPanel();
          panelChooser.setLayout(new BoxLayout(panelChooser, BoxLayout.Y_AXIS));
          panelChooser.setBorder(BorderFactory.createLineBorder(getBackground()));
          final JLabel whereFrom = new JLabel("From: " + current.getName());
          whereFrom.setHorizontalAlignment(SwingConstants.LEFT);
          whereFrom.setFont(new Font(MapImage.FONT_FAMILY_DEFAULT, Font.BOLD, 12));
          panelChooser.add(whereFrom);
          panelChooser.add(new JLabel(" "));
          final int maxAllowed =
              Math.min(AirBattle.getMaxInterceptionCount(current, possible), possible.size());
          final UnitChooser chooser = new UnitChooser(possible, Map.of(), false, uiContext);
          chooser.setMaxAndShowMaxButton(maxAllowed);
          panelChooser.add(chooser);
          final JScrollPane chooserScrollPane = new JScrollPane(panelChooser);
          panel.add(chooserScrollPane, BorderLayout.CENTER);
          final String optionSelect = "Select";
          final String optionNone = "None";
          final Object[] options = {optionSelect, optionNone};
          final JOptionPane optionPane =
              new JOptionPane(
                  panel,
                  JOptionPane.PLAIN_MESSAGE,
                  JOptionPane.YES_NO_CANCEL_OPTION,
                  null,
                  options,
                  options[1]);
          final JDialog dialog = new JDialog((Frame) getParent(), message);
          dialog.setContentPane(optionPane);
          dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
          dialog.setLocationRelativeTo(getParent());
          dialog.setAlwaysOnTop(true);
          dialog.pack();
          dialog.setVisible(true);
          dialog.requestFocusInWindow();
          optionPane.addPropertyChangeListener(
              e -> {
                if (!dialog.isVisible()) {
                  return;
                }
                // Note: getValue() can return either an int (user pressed escape), or a String.
                final Object option = optionPane.getValue();
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

  public PoliticalActionAttachment getPoliticalActionChoice(
      final GamePlayer player, final boolean firstRun, final IPoliticsDelegate politicsDelegate) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToPolitics(player);
    requestWindowFocus();
    return actionButtonsPanel.waitForPoliticalAction(firstRun, politicsDelegate);
  }

  public UserActionAttachment getUserActionChoice(
      final GamePlayer player,
      final boolean firstRun,
      final IUserActionDelegate userActionDelegate) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToUserActions(player);
    requestWindowFocus();
    return actionButtonsPanel.waitForUserActionAction(firstRun, userActionDelegate);
  }

  public TechRoll getTechRolls(final GamePlayer gamePlayer) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToTech(gamePlayer);
    // workaround for panel not receiving focus at beginning of tech phase
    requestWindowFocus();
    return actionButtonsPanel.waitForTech();
  }

  /**
   * Prompts the user to select the territory on which they wish to conduct a rocket attack.
   *
   * @param candidates The collection of territories on which the user may conduct a rocket attack.
   * @param from The territory from which the rocket attack is conducted.
   * @return The selected territory or {@code null} if no territory was selected.
   */
  public Territory getRocketAttack(final Collection<Territory> candidates, final Territory from) {
    messageAndDialogThreadPool.waitForAll();
    mapPanel.centerOn(from);

    final Supplier<Territory> action =
        () -> {
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
          final String[] options = {"OK", "Don't attack"};
          final String message = "Select Rocket Target";
          final int selection =
              JOptionPane.showOptionDialog(
                  TripleAFrame.this,
                  panel,
                  message,
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.PLAIN_MESSAGE,
                  null,
                  options,
                  null);
          return (selection == 0) ? list.getSelectedValue() : null;
        };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action))
        .result
        .orElse(null);
  }

  private void updateStep() {
    if (SwingUtilities.isEventDispatchThread()) {
      ThreadRunner.runInNewThread(this::updateStepFromEdt);
    } else {
      updateStepFromEdt();
    }
  }

  private void updateStepFromEdt() {
    Preconditions.checkState(
        !SwingUtilities.isEventDispatchThread(), "This method must not be invoked on the EDT!");
    if (uiContext.isShutDown()) {
      return;
    }
    final int round;
    final String stepDisplayName;
    final GamePlayer player;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      round = data.getSequence().getRound();
      final GameStep step = data.getSequence().getStep();
      if (step == null) {
        return;
      }
      stepDisplayName = step.getDisplayName();
      player = step.getPlayerId();
    }

    uiContext.setCurrentPlayer(player);

    SwingUtilities.invokeLater(
        () -> {
          bottomBar.setStepInfo(round, stepDisplayName);
          bottomBar.updateFromCurrentPlayer();
        });
    bottomBar.gameDataChanged();
    // if the game control has passed to someone else and we are not just showing the map, show the
    // history
    if (player != null && !player.isNull()) {
      if (!uiContext.isCurrentPlayerRemote()) {
        if (inHistory.get()) {
          requiredTurnSeries.put(player, true);
          // if the game control is with us, show the current game
          showGame();
        }
      } else {
        if (inHistory.compareAndSet(false, true)) {
          showHistory();
        }
      }
    }
  }

  /**
   * Invoked at the start of a player's turn to play a sound alerting the player it is their turn
   * and to center the map on the player's capital.
   */
  public void requiredTurnSeries(final GamePlayer player) {
    if (player == null || !Interruptibles.sleep(300)) {
      return;
    }
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () -> {
                  final Boolean play = requiredTurnSeries.get(player);
                  if (play != null && play) {
                    uiContext
                        .getClipPlayer()
                        .play(SoundPath.CLIP_REQUIRED_YOUR_TURN_SERIES, player);
                    requiredTurnSeries.put(player, false);
                  }
                  // center on capital of player, if it is a new none-AI player
                  if (uiContext.setCurrentPlayer(player)) {
                    bottomBar.updateFromCurrentPlayer();
                    if (!player.isAi()) {
                      // assume missing offset means there is a new mapPanel build up for which
                      // centering is not updating without repaint
                      final boolean repaintRequired =
                          (mapPanel.getXOffset() == 0 && mapPanel.getYOffset() == 0);
                      try (GameData.Unlocker ignored = data.acquireReadLock()) {
                        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(
                                player, data.getMap())
                            .ifPresent(territory -> mapPanel.centerOn(territory));
                        if (repaintRequired) mapPanel.repaint();
                      }
                    }
                  }
                }));
  }

  private String getUnitInfo() {
    if (!mapPanel.getMouseHoverUnits().isEmpty()) {
      final Unit unit = mapPanel.getMouseHoverUnits().get(0);
      return MapUnitTooltipManager.getTooltipTextForUnit(
          unit.getType(), unit.getOwner(), mapPanel.getMouseHoverUnits().size(), uiContext);
    }
    return "";
  }

  private KeyListener getArrowKeyListener() {
    return new KeyListener() {
      @Override
      public void keyPressed(final KeyEvent e) {
        isCtrlPressed = e.isControlDown();
        // scroll map according to wasd/arrow keys
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
      }

      @Override
      public void keyTyped(final KeyEvent e) {
        // not needed interface method
      }

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

  public void showActionPanelTab() {
    tabsPanel.setSelectedIndex(0);
  }

  private void showHistory() {
    inHistory.set(true);
    inGame.set(false);
    setWidgetActivation();
    final GameData clonedGameData;
    try (GameData.Unlocker ignored = data.acquireWriteLock()) {
      // we want to use a clone of the data, so we can make changes to it as we walk up and down the
      // history
      final var cloneOptions = GameDataManager.Options.builder().withHistory(true).build();
      clonedGameData = GameDataUtils.cloneGameData(data, cloneOptions).orElse(null);
      if (clonedGameData == null) {
        return;
      }
    }
    clonedGameData.removeDataChangeListener(dataChangeListener);
    if (historySyncher != null) {
      throw new IllegalStateException("Two history synchers?");
    }
    historySyncher = new HistorySynchronizer(clonedGameData, game);
    clonedGameData.addDataChangeListener(dataChangeListener);
    statsPanel.setGameData(clonedGameData);
    if (!TechAdvance.getTechAdvances(clonedGameData.getTechnologyFrontier(), null).isEmpty()) {
      technologyPanel.setGameData(clonedGameData);
    }
    economyPanel.setGameData(clonedGameData);
    if (objectivePanel != null) {
      objectivePanel.setGameData(clonedGameData);
    }
    territoryDetailPanel.setGameData(clonedGameData);
    mapPanel.setGameData(clonedGameData);
    final HistoryDetailsPanel historyDetailPanel = getHistoryDetailsPanel(clonedGameData);
    // create history tree context menu
    final JSplitPane historyComponentSplitPane = new JSplitPane();
    historyComponentSplitPane.setOneTouchExpandable(true);
    historyComponentSplitPane.setContinuousLayout(true);
    historyComponentSplitPane.setDividerSize(8);
    historyComponentSplitPane.setLeftComponent(historyPanel);
    historyComponentSplitPane.setRightComponent(gameCenterPanel);
    historyComponentSplitPane.setDividerLocation(150);
    final JPanel historyComponent =
        new JPanelBuilder()
            .borderLayout()
            .addCenter(historyComponentSplitPane)
            .addSouth(bottomBar)
            .build();
    SwingUtilities.invokeLater(
        () -> {
          tabsPanel.removeAll();
          addTabs(historyDetailPanel);
          actionButtonsPanel.getCurrent().ifPresent(actionPanel -> actionPanel.setActive(false));
          getContentPane().removeAll();
          getContentPane().add(historyComponent, BorderLayout.CENTER);
          validate();
        });
  }

  @Nonnull
  private HistoryDetailsPanel getHistoryDetailsPanel(GameData clonedGameData) {
    final HistoryDetailsPanel historyDetailPanel =
        new HistoryDetailsPanel(clonedGameData, mapPanel);
    // actions need to clear the history panel popup state when done
    final HistoryPanel popupHistoryPanel =
        new HistoryPanel(clonedGameData, historyDetailPanel, uiContext);
    final JPopupMenu popup =
        new HistoryPanelPopupMenuBuilder()
            .add("Show Summary Log", () -> showHistoryLog(popupHistoryPanel, false, clonedGameData))
            .add("Show Detailed Log", () -> showHistoryLog(popupHistoryPanel, true, clonedGameData))
            .add(
                "Export Gameboard Picture",
                () -> {
                  ScreenshotExporter.exportScreenshot(
                      TripleAFrame.this, clonedGameData, popupHistoryPanel.getCurrentPopupNode());
                  popupHistoryPanel.clearCurrentPopupNode();
                })
            .add(
                "Save Game at this point (BETA)",
                () -> {
                  JOptionPane.showMessageDialog(
                      TripleAFrame.this,
                      "Please first left click on the spot you want to save from, "
                          + "Then right click and select 'Save Game From History'"
                          + "\n\nIt is recommended that when saving the game from the "
                          + "History panel:"
                          + "\n * Your CURRENT GAME is at the start of some player's turn, "
                          + "and that no moves have been made and no actions taken yet."
                          + "\n * The point in HISTORY that you are trying to save at, is at the "
                          + "beginning of a player's turn, or the beginning of a round."
                          + "\nSaving at any other point, could potentially create errors."
                          + "\nFor example, saving while your current game is in the middle of a "
                          + "move or battle phase will always create errors in the save game."
                          + "\nAnd you will also get errors in the save game if you try to create a "
                          + "save at a point in history such as a move or battle phase.",
                      "Save Game from History",
                      JOptionPane.INFORMATION_MESSAGE);

                  final Optional<Path> f =
                      GameFileSelector.getSaveGameLocation(TripleAFrame.this, clonedGameData);
                  if (f.isPresent()) {
                    try (OutputStream fileOutputStream = Files.newOutputStream(f.get())) {
                      clonedGameData
                          .getHistory()
                          .removeAllHistoryAfterNode(popupHistoryPanel.getCurrentPopupNode());
                      // TODO: the saved current delegate is still the current delegate,
                      // rather than the delegate at that history popup node
                      // TODO: it still shows the current round number, rather than the round at
                      // the history popup node
                      // TODO: this could be solved easily if rounds/steps were changes,
                      // but that could greatly increase the file size :(
                      // TODO: this also does not undo the run count of each delegate step
                      final Enumeration<?> enumeration =
                          ((DefaultMutableTreeNode) clonedGameData.getHistory().getRoot())
                              .preorderEnumeration();
                      enumeration.nextElement();
                      int round = 0;
                      String stepDisplayName =
                          clonedGameData.getSequence().getStep(0).getDisplayName();
                      GamePlayer currentPlayer =
                          clonedGameData.getSequence().getStep(0).getPlayerId();
                      int roundOffset = clonedGameData.getSequence().getRoundOffset();
                      while (enumeration.hasMoreElements()) {
                        final HistoryNode node = (HistoryNode) enumeration.nextElement();
                        if (node instanceof Round) {
                          round = Math.max(0, ((Round) node).getRoundNo() - roundOffset);
                          currentPlayer = null;
                          stepDisplayName = node.getTitle();
                        } else if (node instanceof Step) {
                          currentPlayer = ((Step) node).getPlayerId().orElse(null);
                          stepDisplayName = node.getTitle();
                        }
                      }
                      clonedGameData
                          .getSequence()
                          .setRoundAndStep(round, stepDisplayName, currentPlayer);
                      GameDataManager.saveGame(fileOutputStream, clonedGameData);
                      JOptionPane.showMessageDialog(
                          TripleAFrame.this,
                          "Game Saved",
                          "Game Saved",
                          JOptionPane.INFORMATION_MESSAGE);
                    } catch (final IOException e) {
                      log.error("Failed to save game: " + f.get().toAbsolutePath(), e);
                    }
                  }
                  popupHistoryPanel.clearCurrentPopupNode();
                })
            .build();
    popupHistoryPanel.setPopup(popup);
    historyPanel = popupHistoryPanel;
    return historyDetailPanel;
  }

  private static class HistoryPanelPopupMenuBuilder {
    private final JPopupMenu popup = new JPopupMenu();

    public HistoryPanelPopupMenuBuilder add(String title, Runnable action) {
      popup.add(
          new AbstractAction(title) {
            @Override
            public void actionPerformed(ActionEvent e) {
              action.run();
            }
          });
      return this;
    }

    public JPopupMenu build() {
      return popup;
    }
  }

  private void showHistoryLog(
      final HistoryPanel popupHistoryPanel, boolean verboseLog, GameData clonedGameData) {
    final HistoryLog historyLog = new HistoryLog(this);
    final HistoryNode currentPopupNodeOrLastNode = popupHistoryPanel.getCurrentPopupNode();
    historyLog.printRemainingTurn(
        currentPopupNodeOrLastNode, verboseLog, data.getDiceSides(), null);
    historyLog.printTerritorySummary(currentPopupNodeOrLastNode, clonedGameData);
    historyLog.printProductionSummary(clonedGameData);
    popupHistoryPanel.clearCurrentPopupNode();
    historyLog.setVisible(true);
  }

  private void showGame() {
    inGame.set(true);
    // Are we coming from showHistory mode or showMapOnly mode?
    SwingUtilities.invokeLater(
        () -> {
          if (inHistory.compareAndSet(true, false)) {
            if (historySyncher != null) {
              historySyncher.deactivate();
              historySyncher = null;
            }
            historyPanel = null;
            mapPanel.getData().removeDataChangeListener(dataChangeListener);
            if (!TechAdvance.getTechAdvances(data.getTechnologyFrontier(), null).isEmpty()) {
              technologyPanel.setGameData(data);
            }
            statsPanel.setGameData(data);
            economyPanel.setGameData(data);
            if (objectivePanel != null) {
              objectivePanel.setGameData(data);
            }
            territoryDetailPanel.setGameData(data);
            mapPanel.setGameData(data);
            data.addDataChangeListener(dataChangeListener);
            tabsPanel.removeAll();
          }
          setWidgetActivation();
          addTabs(null);
          actionButtonsPanel.getCurrent().ifPresent(actionPanel -> actionPanel.setActive(true));
          gameMainPanel.removeAll();
          gameMainPanel.setLayout(new BorderLayout());
          gameMainPanel.add(gameCenterPanel, BorderLayout.CENTER);
          gameMainPanel.add(bottomBar, BorderLayout.SOUTH);
          getContentPane().removeAll();
          getContentPane().add(gameMainPanel, BorderLayout.CENTER);
          validate();
          requestWindowFocus();
        });
    mapPanel.setRoute(null);
  }

  private void setWidgetActivation() {
    SwingAction.invokeNowOrLater(
        () -> {
          showHistoryAction.setEnabled(!inHistory.get());
          showGameAction.setEnabled(!inGame.get());
          if (editModeButtonModel != null) {
            editModeButtonModel.setEnabled(editDelegate != null);
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

  /**
   * Prompts the user to select from the specified collection of fighters those they wish to move to
   * an adjacent newly-constructed carrier.
   *
   * @param fighters The collection of fighters from which to choose.
   * @param where The territory on which to center the map.
   * @return The collection of fighters to move to the newly-constructed carrier.
   */
  public Collection<Unit> moveFightersToCarrier(
      final Collection<Unit> fighters, final Territory where) {
    messageAndDialogThreadPool.waitForAll();
    mapPanel.centerOn(where);
    final AtomicReference<JScrollPane> panelRef = new AtomicReference<>();
    final AtomicReference<UnitChooser> chooserRef = new AtomicReference<>();
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () -> {
                  final UnitChooser chooser = new UnitChooser(fighters, Map.of(), false, uiContext);
                  final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
                  final int availHeight = screenResolution.height - 120;
                  final int availWidth = screenResolution.width - 40;
                  final JScrollPane scroll = new JScrollPane(chooser);
                  scroll.setBorder(BorderFactory.createEmptyBorder());
                  scroll.setPreferredSize(
                      new Dimension(
                          (scroll.getPreferredSize().width > availWidth
                              ? availWidth
                              : (scroll.getPreferredSize().width
                                  + (scroll.getPreferredSize().height > availHeight ? 20 : 0))),
                          (scroll.getPreferredSize().height > availHeight
                              ? availHeight
                              : (scroll.getPreferredSize().height
                                  + (scroll.getPreferredSize().width > availWidth ? 26 : 0)))));
                  panelRef.set(scroll);
                  chooserRef.set(chooser);
                }));
    final int option =
        EventThreadJOptionPane.showOptionDialog(
            this,
            panelRef.get(),
            "Move air units to carrier",
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION,
            null,
            new String[] {"OK", "Cancel"},
            "OK",
            getUiContext().getCountDownLatchHandler());
    if (option == JOptionPane.OK_OPTION) {
      return chooserRef.get().getSelected();
    }
    return new ArrayList<>();
  }

  public BattlePanel getBattlePanel() {
    return actionButtonsPanel.getBattlePanel();
  }

  /** Displays the map located in the directory/archive {@code mapdir}. */
  public void changeMapSkin(final String skinName) {
    uiContext = UiContext.changeMapSkin(data, skinName);
    game.setResourceLoader(uiContext.getResourceLoader());
    // when changing skins, always show relief images
    if (uiContext.getMapData().getHasRelief()) {
      TileImageFactory.setShowReliefImages(true);
    }

    mapPanel.setGameData(data);
    // update map panels to use new image
    mapPanel.changeImage(uiContext.getMapData().getMapDimensions());
    final Image small = uiContext.getMapImage().getSmallMapImage();
    smallView.changeImage(small);
    mapPanel.changeSmallMapOffscreenMap();
    // redraw territories
    mapPanel.resetMap();
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
