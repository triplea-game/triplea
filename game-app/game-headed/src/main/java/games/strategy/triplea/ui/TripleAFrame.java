package games.strategy.triplea.ui;

import static games.strategy.triplea.image.UnitImageFactory.ImageKey;

import com.google.common.base.Preconditions;
import games.strategy.engine.chat.ChatMessagePanel.ChatSoundProfile;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.Step;
import java.awt.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.*;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.EventThreadJOptionPane.ConfirmDialogType;
import org.triplea.thread.ThreadPool;
import org.triplea.util.Tuple;

/** Main frame for the triple a game. */
@lombok.extern.slf4j.Slf4j
public final class TripleAFrame extends javax.swing.JFrame implements QuitHandler {
  private static final long serialVersionUID = 7640069668264418976L;

  @lombok.Getter private final games.strategy.engine.framework.LocalPlayers localPlayers;
  private final games.strategy.engine.data.GameData data;
  @lombok.Getter private final games.strategy.engine.framework.IGame game;
  @lombok.Getter private final games.strategy.triplea.ui.panels.map.MapPanel mapPanel;
  private final games.strategy.ui.ImageScrollerSmallView smallView;

  private final ActionButtonsPanel actionButtonsPanel;
  private final javax.swing.JPanel gameMainPanel = new javax.swing.JPanel();
  private final javax.swing.JPanel rightHandSidePanel = new javax.swing.JPanel();
  private final javax.swing.JTabbedPane tabsPanel = new javax.swing.JTabbedPane();
  private final StatPanel statsPanel;
  private final TechnologyPanel technologyPanel;
  private final EconomyPanel economyPanel;
  private final Runnable clientLeftGame;
  private @Nullable ObjectivePanel objectivePanel;
  @lombok.Getter private final TerritoryDetailPanel territoryDetailPanel;
  private final java.util.concurrent.atomic.AtomicBoolean inHistory =
      new java.util.concurrent.atomic.AtomicBoolean(false);
  private final java.util.concurrent.atomic.AtomicBoolean inGame =
      new java.util.concurrent.atomic.AtomicBoolean(true);
  private final javax.swing.JPanel mapAndChatPanel;
  private final @Nullable games.strategy.engine.chat.ChatPanel chatPanel;
  private final javax.swing.JSplitPane chatSplit;
  @lombok.Getter private final javax.swing.ButtonModel editModeButtonModel;
  private final javax.swing.JSplitPane gameCenterPanel;
  private final CommentPanel commentPanel;
  @lombok.Getter private final BottomBar bottomBar;
  private final java.util.Map<games.strategy.engine.data.GamePlayer, Boolean> requiredTurnSeries =
      new java.util.HashMap<>();
  private final EditPanel editPanel;
  private final games.strategy.engine.data.events.GameDataChangeListener dataChangeListener =
      new games.strategy.engine.data.events.GameDataChangeListener() {
        @Override
        public void gameDataChanged(final games.strategy.engine.data.Change change) {
          // Update the bottomBar, since resources may have changed, e.g. by triggers.
          bottomBar.gameDataChanged();
          javax.swing.SwingUtilities.invokeLater(
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

  @lombok.Getter
  private final javax.swing.Action showHistoryAction =
      org.triplea.swing.SwingAction.of(
          "Show history",
          e -> {
            if (!inHistory.get()) {
              showHistory();
              dataChangeListener.gameDataChanged(
                  games.strategy.engine.data.changefactory.ChangeFactory.EMPTY_CHANGE);
            }
          });

  @lombok.Getter
  private final javax.swing.Action showGameAction =
      new javax.swing.AbstractAction("Show current game") {
        @java.io.Serial private static final long serialVersionUID = -7551760679570164254L;

        {
          setEnabled(false);
        }

        @Override
        public void actionPerformed(final java.awt.event.ActionEvent e) {
          showGame();
          dataChangeListener.gameDataChanged(
              games.strategy.engine.data.changefactory.ChangeFactory.EMPTY_CHANGE);
        }
      };

  @lombok.Getter
  private final AdditionalTerritoryDetails additionalTerritoryDetails =
      new AdditionalTerritoryDetails();

  private final ThreadPool messageAndDialogThreadPool = new ThreadPool(1);
  private final MapUnitTooltipManager tooltipManager;
  @lombok.Getter private @Nullable games.strategy.triplea.ui.history.HistoryPanel historyPanel;
  private int lastPlayerRound = -1;
  private @Nullable games.strategy.engine.framework.HistorySynchronizer historySyncher;
  private boolean isCtrlPressed = false;
  @lombok.Getter private UiContext uiContext;
  private javax.swing.JSplitPane commentSplit;
  @lombok.Getter private games.strategy.triplea.delegate.remote.IEditDelegate editDelegate;
  private games.strategy.engine.data.GamePlayer lastPlayer;

  private TripleAFrame(
      final games.strategy.engine.framework.IGame game,
      final games.strategy.engine.framework.LocalPlayers players,
      final UiContext uiContext,
      @Nullable final games.strategy.engine.chat.Chat chat,
      final Runnable clientLeftGame) {
    super("TripleA - " + game.getData().getGameName());
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    games.strategy.engine.framework.lookandfeel.LookAndFeelSwingFrameListener.register(this);
    setSize(700, 400);
    setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);

    this.clientLeftGame = clientLeftGame;

    localPlayers = players;
    setIconImage(games.strategy.triplea.EngineImageLoader.loadFrameIcon());
    // 200 size is pretty arbitrary, goal is to not allow users to shrink window down to nothing.
    setMinimumSize(new java.awt.Dimension(200, 200));

    this.game = game;
    data = game.getData();
    addZoomKeyboardShortcuts();
    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(
        new java.awt.event.WindowAdapter() {
          @Override
          public void windowClosing(final java.awt.event.WindowEvent e) {
            leaveGame();
          }
        });
    addWindowFocusListener(
        new java.awt.event.WindowAdapter() {
          @Override
          public void windowGainedFocus(final java.awt.event.WindowEvent e) {
            mapPanel.requestFocusInWindow();
          }
        });
    this.uiContext = uiContext;
    this.setCursor(uiContext.getCursor());
    editModeButtonModel = new javax.swing.JToggleButton.ToggleButtonModel();
    editModeButtonModel.setEnabled(false);

    javax.swing.SwingUtilities.invokeLater(
        () -> this.setJMenuBar(games.strategy.triplea.ui.menubar.TripleAMenuBar.get(this)));
    final games.strategy.ui.ImageScrollModel model = new games.strategy.ui.ImageScrollModel();
    model.setMaxBounds(
        uiContext.getMapData().getMapDimensions().width,
        uiContext.getMapData().getMapDimensions().height);
    model.setScrollX(uiContext.getMapData().scrollWrapX());
    model.setScrollY(uiContext.getMapData().scrollWrapY());
    final java.awt.Image small = uiContext.getMapImage().getSmallMapImage();
    smallView = new games.strategy.ui.ImageScrollerSmallView(small, model, uiContext.getMapData());
    mapPanel =
        new games.strategy.triplea.ui.panels.map.MapPanel(
            data, smallView, uiContext, model, this::computeScrollSpeed);
    tooltipManager = new MapUnitTooltipManager(mapPanel);
    mapPanel.addMapSelectionListener(
        new DefaultMapSelectionListener() {
          @Override
          public void mouseEntered(final games.strategy.engine.data.Territory territory) {
            bottomBar.setTerritory(territory);
          }
        });
    mapPanel.addMouseOverUnitListener(
        (units, territory) -> tooltipManager.updateTooltip(getUnitInfo()));
    // link the small and large images
    javax.swing.SwingUtilities.invokeLater(mapPanel::initSmallMap);
    mapAndChatPanel = new javax.swing.JPanel();
    mapAndChatPanel.setLayout(new java.awt.BorderLayout());
    commentPanel = new CommentPanel(this, data);
    chatSplit = new javax.swing.JSplitPane();
    chatSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
    chatSplit.setOneTouchExpandable(true);
    chatSplit.setDividerSize(8);
    chatSplit.setResizeWeight(0.95);
    if (chat != null) {
      commentSplit = new javax.swing.JSplitPane();
      commentSplit.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
      commentSplit.setOneTouchExpandable(true);
      commentSplit.setDividerSize(8);
      commentSplit.setResizeWeight(0.5);
      commentSplit.setTopComponent(commentPanel);
      commentSplit.setBottomComponent(null);
      chatPanel =
          new games.strategy.engine.chat.ChatPanel(
              chat, ChatSoundProfile.GAME, getUiContext().getClipPlayer());
      chatPanel.setPlayerRenderer(
          new games.strategy.engine.chat.PlayerChatRenderer(this.game, uiContext));
      final java.awt.Dimension chatPrefSize =
          new java.awt.Dimension((int) chatPanel.getPreferredSize().getWidth(), 95);
      chatPanel.setPreferredSize(chatPrefSize);
      chatSplit.setTopComponent(mapPanel);
      chatSplit.setBottomComponent(chatPanel);
      mapAndChatPanel.add(chatSplit, java.awt.BorderLayout.CENTER);
    } else {
      mapAndChatPanel.add(mapPanel, java.awt.BorderLayout.CENTER);
      chatPanel = null;
    }
    gameMainPanel.setLayout(new java.awt.BorderLayout());
    this.getContentPane().setLayout(new java.awt.BorderLayout());
    this.getContentPane().add(gameMainPanel, java.awt.BorderLayout.CENTER);

    final boolean usingDiceServer =
        (game.getRandomSource() instanceof games.strategy.engine.random.PbemDiceRoller);
    bottomBar = new BottomBar(uiContext, data, usingDiceServer);

    gameMainPanel.add(bottomBar, java.awt.BorderLayout.SOUTH);
    rightHandSidePanel.setLayout(new java.awt.BorderLayout());
    final java.awt.event.FocusAdapter focusToMapPanelFocusListener =
        new java.awt.event.FocusAdapter() {
          @Override
          public void focusGained(final java.awt.event.FocusEvent e) {
            // give the focus back to the map panel
            mapPanel.requestFocus();
          }
        };
    rightHandSidePanel.addFocusListener(focusToMapPanelFocusListener);
    smallView.addFocusListener(focusToMapPanelFocusListener);
    tabsPanel.addFocusListener(focusToMapPanelFocusListener);
    rightHandSidePanel.add(smallView, java.awt.BorderLayout.NORTH);
    tabsPanel.setBorder(null);
    rightHandSidePanel.add(tabsPanel, java.awt.BorderLayout.CENTER);

    final games.strategy.triplea.ui.panel.move.MovePanel movePanel =
        new games.strategy.triplea.ui.panel.move.MovePanel(this);
    actionButtonsPanel = new ActionButtonsPanel(movePanel, this);

    final org.triplea.swing.CollapsiblePanel placementsPanel =
        new PlacementUnitsCollapsiblePanel(data, uiContext).getPanel();
    rightHandSidePanel.add(
        new org.triplea.swing.jpanel.JPanelBuilder()
            .borderLayout()
            .addNorth(placementsPanel)
            .addSouth(movePanel.getUnitScrollerPanel())
            .build(),
        java.awt.BorderLayout.SOUTH);

    javax.swing.SwingUtilities.invokeLater(() -> mapPanel.addKeyListener(getArrowKeyListener()));

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
          final javax.swing.JTabbedPane pane = (javax.swing.JTabbedPane) evt.getSource();
          // Get current tab
          final int sel = pane.getSelectedIndex();
          if (sel == -1) {
            return;
          }
          if (pane.getComponentAt(sel).equals(editPanel)) {
            final games.strategy.engine.data.GamePlayer player1;
            try (games.strategy.engine.data.GameData.Unlocker ignored = data.acquireReadLock()) {
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
        new java.awt.Dimension(
            (int) smallView.getPreferredSize().getWidth(),
            (int) mapPanel.getPreferredSize().getHeight()));
    gameCenterPanel =
        new javax.swing.JSplitPane(
            javax.swing.JSplitPane.HORIZONTAL_SPLIT, mapAndChatPanel, rightHandSidePanel);
    gameCenterPanel.setOneTouchExpandable(true);
    gameCenterPanel.setContinuousLayout(true);
    gameCenterPanel.setDividerSize(8);
    gameCenterPanel.setResizeWeight(1.0);
    gameMainPanel.add(gameCenterPanel, java.awt.BorderLayout.CENTER);
    gameCenterPanel.resetToPreferredSizes();
    // set up the edit mode overlay text
    this.setGlassPane(
        new javax.swing.JComponent() {
          private static final long serialVersionUID = 6724687534214427291L;

          @Override
          protected void paintComponent(final java.awt.Graphics g) {
            g.setFont(
                new java.awt.Font(
                    games.strategy.triplea.image.MapImage.FONT_FAMILY_DEFAULT,
                    java.awt.Font.BOLD,
                    50));
            g.setColor(new java.awt.Color(255, 255, 255, 175));
            final java.awt.Dimension size = mapPanel.getSize();
            g.drawString(
                "Edit Mode",
                (int) ((size.getWidth() - 200) / 2),
                (int) ((size.getHeight() - 100) / 2));
          }
        });
    // force a data change event to update the UI for edit mode
    dataChangeListener.gameDataChanged(
        games.strategy.engine.data.changefactory.ChangeFactory.EMPTY_CHANGE);
    data.addDataChangeListener(dataChangeListener);
    data.addGameDataEventListener(
        games.strategy.engine.data.GameDataEvent.GAME_STEP_CHANGED, this::updateStep);
    // Clear cached unit images when getting standard tech like jet power.
    data.addGameDataEventListener(
        games.strategy.engine.data.GameDataEvent.TECH_ATTACHMENT_CHANGED,
        this::clearCachedUnitImages);
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
      final games.strategy.engine.framework.IGame game,
      final games.strategy.engine.framework.LocalPlayers players,
      @Nullable final games.strategy.engine.chat.Chat chat,
      final Runnable clientLeftGame) {
    Preconditions.checkState(
        !javax.swing.SwingUtilities.isEventDispatchThread(),
        "This method must not be called on the EDT");

    final UiContext uiContext = new UiContext(game.getData());
    game.setResourceLoader(uiContext.getResourceLoader());
    uiContext.getMapData().verify(game.getData());
    uiContext.setLocalPlayers(players);

    final TripleAFrame frame =
        org.triplea.java.Interruptibles.awaitResult(
                () ->
                    org.triplea.swing.SwingAction.invokeAndWaitResult(
                        () -> {
                          final TripleAFrame newFrame =
                              new TripleAFrame(game, players, uiContext, chat, clientLeftGame);
                          newFrame.setVisible(true);
                          newFrame.toFront();
                          return newFrame;
                        }))
            .result
            .orElseThrow(() -> new IllegalStateException("Error while instantiating TripleAFrame"));
    frame.updateStep();
    return frame;
  }

  private static javax.swing.JOptionPane getOptionPane(final javax.swing.JComponent parent) {
    return (parent instanceof javax.swing.JOptionPane optionPane)
        ? optionPane
        : getOptionPane((javax.swing.JComponent) parent.getParent());
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
      mapAndChatPanel.add(mapPanel, java.awt.BorderLayout.CENTER);
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
      mapAndChatPanel.add(chatSplit, java.awt.BorderLayout.CENTER);
      mapAndChatPanel.validate();
    }
  }

  private void addZoomKeyboardShortcuts() {
    org.triplea.swing.key.binding.SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        this,
        org.triplea.swing.key.binding.KeyCode.EQUALS,
        () ->
            mapPanel.setScale(
                mapPanel.getScale()
                    + (games.strategy.triplea.settings.ClientSetting.mapZoomFactor.getValueOrThrow()
                        / 100f)));

    org.triplea.swing.key.binding.SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        this,
        org.triplea.swing.key.binding.KeyCode.MINUS,
        () ->
            mapPanel.setScale(
                mapPanel.getScale()
                    - (games.strategy.triplea.settings.ClientSetting.mapZoomFactor.getValueOrThrow()
                        / 100f)));
  }

  /**
   * If the frame is visible, prompts the user if they wish to exit the application. If they answer
   * yes or the frame is not visible, the game will be stopped, and the process will be terminated.
   */
  @Override
  public boolean shutdown() {
    if (isVisible()) {

      final boolean confirmed =
          org.triplea.swing.EventThreadJOptionPane.showConfirmDialog(
              this,
              "Are you sure you want to exit TripleA?\nUnsaved game data will be lost.",
              "Exit Program",
              ConfirmDialogType.YES_NO);
      if (!confirmed) {
        return false;
      }
    }

    stopGame();
    org.triplea.util.ExitStatus.SUCCESS.exit();
    return true;
  }

  /** Stops the game and closes this frame window. */
  public void stopGame() {
    this.setVisible(false);
    TripleAFrame.this.getUiContext().getClipPlayer().stopAllSounds();
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
    games.strategy.engine.framework.GameShutdownRegistry.runShutdownActions();
  }

  /**
   * Prompts the user if they wish to leave the game. If they answer yes, the game will be stopped,
   * and the application will return to the main menu.
   */
  public void leaveGame() {
    final boolean confirmed =
        org.triplea.swing.EventThreadJOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to leave the current game?\nUnsaved game data will be lost.",
            "Leave Game",
            ConfirmDialogType.YES_NO);
    if (!confirmed) {
      return;
    }
    if (game instanceof games.strategy.engine.framework.ServerGame serverGame) {
      serverGame.stopGame();
    } else {
      game.getMessengers().shutDown();
      ((games.strategy.engine.framework.ClientGame) game).shutDown();
      // an ugly hack, we need a better way to get the main frame
      new Thread(clientLeftGame).start();
    }
  }

  public void setStatusErrorMessage(final String msg) {
    final java.util.Optional<java.awt.Image> errorImage = mapPanel.getErrorImage();
    if (errorImage.isPresent()) bottomBar.setStatus(msg, errorImage.get());
    else bottomBar.setStatusAndClearIcon(msg);
  }

  void clearStatusMessage() {
    bottomBar.setStatusAndClearIcon("");
  }

  public void setStatusWarningMessage(final String msg) {
    final java.util.Optional<java.awt.Image> warningImage = mapPanel.getWarningImage();
    if (warningImage.isPresent()) bottomBar.setStatus(msg, warningImage.get());
    else bottomBar.setStatusAndClearIcon(msg);
  }

  public IntegerMap<ProductionRule> getProduction(
      final games.strategy.engine.data.GamePlayer player,
      final boolean bid,
      final boolean keepCurrentPurchase) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToProduce(player, keepCurrentPurchase);
    return actionButtonsPanel.waitForPurchase(bid);
  }

  public Map<Unit, IntegerMap<RepairRule>> getRepair(
      final games.strategy.engine.data.GamePlayer player,
      final boolean bid,
      final java.util.Collection<games.strategy.engine.data.GamePlayer> allowedPlayersToRepair) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToRepair(player);
    return actionButtonsPanel.waitForRepair(bid, allowedPlayersToRepair);
  }

  public games.strategy.engine.data.MoveDescription getMove(
      final games.strategy.engine.data.GamePlayer player,
      final games.strategy.engine.player.PlayerBridge bridge,
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

  public BattlePanel getBattlePanel() {
    return actionButtonsPanel.getBattlePanel();
  }

  private void requestWindowFocus() {
    org.triplea.java.Interruptibles.await(
        () ->
            org.triplea.swing.SwingAction.invokeAndWait(
                () -> {
                  requestFocusInWindow();
                  transferFocus();
                }));
  }

  public PlaceData waitForPlace(
      final games.strategy.engine.data.GamePlayer player,
      final boolean bid,
      final games.strategy.engine.player.PlayerBridge bridge) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToPlace(player);
    return actionButtonsPanel.waitForPlace(bid, bridge);
  }

  public void waitForMoveForumPoster(
      final games.strategy.engine.data.GamePlayer player,
      final games.strategy.engine.player.PlayerBridge bridge) {
    if (actionButtonsPanel == null) {
      return;
    }
    actionButtonsPanel.changeToMoveForumPosterPanel(player);
    actionButtonsPanel.waitForMoveForumPosterPanel(this, bridge);
  }

  public void waitForEndTurn(
      final games.strategy.engine.data.GamePlayer player,
      final games.strategy.engine.player.PlayerBridge bridge) {
    if (actionButtonsPanel == null) {
      return;
    }
    actionButtonsPanel.changeToEndTurn(player);
    actionButtonsPanel.waitForEndTurn(this, bridge);
  }

  public games.strategy.triplea.delegate.data.FightBattleDetails getBattle(
      final games.strategy.engine.data.GamePlayer player,
      final games.strategy.triplea.delegate.data.BattleListing battles) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToBattle(player, battles);
    return actionButtonsPanel.waitForBattleSelection();
  }

  /** We do NOT want to block the next player from beginning their turn. */
  public void notifyError(final String message) {
    final String displayMessage =
        org.triplea.util.LocalizeHtml.localizeImgLinksInHtml(message, uiContext.getMapLocation());
    showMessageDialog(displayMessage, "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
  }

  private void showMessageDialog(String displayMessage, String title, int type) {
    try {
      messageAndDialogThreadPool.submit(
          () ->
              org.triplea.swing.EventThreadJOptionPane.showMessageDialogWithScrollPane(
                  TripleAFrame.this,
                  displayMessage,
                  title,
                  type,
                  getUiContext().getCountDownLatchHandler()));
    } catch (java.util.concurrent.RejectedExecutionException e) {
      // The thread pool may have been shutdown. Nothing to do.
    }
  }

  /** We do NOT want to block the next player from beginning their turn. */
  public void notifyMessage(final String message, final String title) {
    if (message == null || title == null) {
      return;
    }
    if (title.contains(
            games.strategy.triplea.attachments.AbstractConditionsAttachment.TRIGGER_CHANCE_FAILURE)
        && message.contains(
            games.strategy.triplea.attachments.AbstractConditionsAttachment.TRIGGER_CHANCE_FAILURE)
        && !getUiContext().getShowTriggerChanceFailure()) {
      return;
    }
    if (title.contains(
            games.strategy.triplea.attachments.AbstractConditionsAttachment
                .TRIGGER_CHANCE_SUCCESSFUL)
        && message.contains(
            games.strategy.triplea.attachments.AbstractConditionsAttachment
                .TRIGGER_CHANCE_SUCCESSFUL)
        && !getUiContext().getShowTriggerChanceSuccessful()) {
      return;
    }
    if (title.equals(games.strategy.triplea.attachments.AbstractTriggerAttachment.NOTIFICATION)
        && !getUiContext().getShowTriggeredNotifications()) {
      return;
    }
    if (title.contains(
            games.strategy.triplea.delegate.AbstractEndTurnDelegate.END_TURN_REPORT_STRING)
        && message.contains(
            games.strategy.triplea.delegate.AbstractEndTurnDelegate.END_TURN_REPORT_STRING)
        && !getUiContext().getShowEndOfTurnReport()) {
      return;
    }
    final String displayMessage =
        org.triplea.util.LocalizeHtml.localizeImgLinksInHtml(message, uiContext.getMapLocation());
    showMessageDialog(displayMessage, title, javax.swing.JOptionPane.INFORMATION_MESSAGE);
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
      final games.strategy.engine.data.GamePlayer gamePlayer,
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
        games.strategy.triplea.Properties.getLhtrCarrierProductionRules(data.getProperties())
            || games.strategy.triplea.Properties.getLandExistingFightersOnNewCarriers(
                data.getProperties());
    final int carrierCount =
        games.strategy.triplea.delegate.GameStepPropertiesHelper.getCombinedTurns(data, gamePlayer)
            .stream()
            .map(games.strategy.engine.data.GamePlayer::getUnitCollection)
            .map(units -> units.getMatches(games.strategy.triplea.delegate.Matches.unitIsCarrier()))
            .mapToInt(List::size)
            .sum();
    final boolean canProduceCarriersUnderFighter = lhtrProd && carrierCount != 0;
    if (canProduceCarriersUnderFighter && carrierCount > 0) {
      sb.append("\nYou have ")
          .append(carrierCount)
          .append(" ")
          .append(games.strategy.triplea.formatter.MyFormatter.pluralize("carrier", carrierCount))
          .append(" on which planes can land");
    }
    final String ok = movePhase ? "End Move Phase" : "Kill Planes";
    final String cancel = movePhase ? "Keep Moving" : "Change Placement";
    final String[] options = {cancel, ok};
    mapPanel.centerOn(org.triplea.java.collections.CollectionUtils.getAny(airCantLand));
    final int choice =
        org.triplea.swing.EventThreadJOptionPane.showOptionDialog(
            this,
            sb.toString(),
            "Air cannot land",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE,
            null,
            options,
            cancel,
            getUiContext().getCountDownLatchHandler());
    return choice == javax.swing.JOptionPane.NO_OPTION;
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
            .map(games.strategy.engine.data.DefaultNamed::getName)
            .collect(
                java.util.stream.Collectors.joining(
                    " ", "Units in the following territories will die: ", ""));
    final String ok = "Done Moving";
    final String cancel = "Keep Moving";
    final String[] options = {cancel, ok};
    this.mapPanel.centerOn(org.triplea.java.collections.CollectionUtils.getAny(unitsCantFight));
    final int choice =
        org.triplea.swing.EventThreadJOptionPane.showOptionDialog(
            this,
            message,
            "Units cannot fight",
            javax.swing.JOptionPane.YES_NO_OPTION,
            javax.swing.JOptionPane.WARNING_MESSAGE,
            null,
            options,
            cancel,
            getUiContext().getCountDownLatchHandler());
    return choice == javax.swing.JOptionPane.NO_OPTION;
  }

  /** Asks a given player if they wish confirm a given political action. */
  public boolean acceptAction(
      final games.strategy.engine.data.GamePlayer playerSendingProposal,
      final String acceptanceQuestion,
      final boolean politics) {
    messageAndDialogThreadPool.waitForAll();

    return org.triplea.swing.EventThreadJOptionPane.showConfirmDialog(
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
    return org.triplea.swing.EventThreadJOptionPane.showConfirmDialog(
        this, message, title, ConfirmDialogType.OK_CANCEL);
  }

  /** Displays a message to the user informing them of the results of rolling for technologies. */
  public void notifyTechResults(final games.strategy.triplea.delegate.data.TechResults msg) {
    final Supplier<TechResultsDisplay> action = () -> new TechResultsDisplay(msg, uiContext, data);
    messageAndDialogThreadPool.submit(
        () ->
            org.triplea.java.Interruptibles.awaitResult(
                    () -> org.triplea.swing.SwingAction.invokeAndWaitResult(action))
                .result
                .ifPresent(
                    display -> {
                      javax.swing.SwingUtilities.invokeLater(this.mapPanel::resetMap);
                      org.triplea.swing.EventThreadJOptionPane.showOptionDialog(
                          TripleAFrame.this,
                          display,
                          "Tech roll",
                          javax.swing.JOptionPane.OK_OPTION,
                          javax.swing.JOptionPane.PLAIN_MESSAGE,
                          null,
                          new String[] {"OK"},
                          "OK",
                          getUiContext().getCountDownLatchHandler());
                    }));
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
        (games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(data.getProperties())
                ? "Bomb/Escort"
                : "Bomb")
            + " in "
            + location.getName();
    final String bomb =
        (games.strategy.triplea.Properties.getRaidsMayBePreceededByAirBattles(data.getProperties())
            ? "Bomb/Escort"
            : "Bomb");
    final String normal = "Attack";
    final String[] choices = {bomb, normal};
    int choice = -1;
    while (choice < 0 || choice > 1) {
      choice =
          org.triplea.swing.EventThreadJOptionPane.showOptionDialog(
              this,
              message,
              "Bomb?",
              javax.swing.JOptionPane.OK_CANCEL_OPTION,
              javax.swing.JOptionPane.INFORMATION_MESSAGE,
              null,
              choices,
              bomb,
              getUiContext().getCountDownLatchHandler());
    }
    return choice == javax.swing.JOptionPane.OK_OPTION;
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
      return org.triplea.java.collections.CollectionUtils.getAny(potentialTargets);
    }
    messageAndDialogThreadPool.waitForAll();
    final AtomicReference<Unit> selected = new java.util.concurrent.atomic.AtomicReference<>();
    final String message = "Select bombing target in " + territory.getName();
    final Supplier<Tuple<javax.swing.JPanel, JList<Unit>>> action =
        () -> {
          final JList<Unit> list =
              new javax.swing.JList<>(
                  org.triplea.swing.SwingComponents.newListModel(potentialTargets));
          list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
          list.setSelectedIndex(0);
          list.setCellRenderer(new UnitRenderer());
          final javax.swing.JPanel panel = new javax.swing.JPanel();
          panel.setLayout(new java.awt.BorderLayout());
          if (bombers != null) {
            panel.add(
                new javax.swing.JLabel(
                    "For Units: "
                        + games.strategy.triplea.formatter.MyFormatter.unitsToTextNoOwner(bombers)),
                java.awt.BorderLayout.NORTH);
          }
          final javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(list);
          panel.add(scroll, java.awt.BorderLayout.CENTER);
          return org.triplea.util.Tuple.of(panel, list);
        };
    return org.triplea.java.Interruptibles.awaitResult(
            () -> org.triplea.swing.SwingAction.invokeAndWaitResult(action))
        .result
        .map(
            comps -> {
              final javax.swing.JPanel panel = comps.getFirst();
              final javax.swing.JList<?> list = comps.getSecond();
              final String[] options = {"OK", "Cancel"};
              final int selection =
                  org.triplea.swing.EventThreadJOptionPane.showOptionDialog(
                      this,
                      panel,
                      message,
                      javax.swing.JOptionPane.OK_CANCEL_OPTION,
                      javax.swing.JOptionPane.PLAIN_MESSAGE,
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
    return org.triplea.java.Interruptibles.awaitResult(
            () ->
                org.triplea.swing.SwingAction.invokeAndWaitResult(
                    () -> new DiceChooser(getUiContext(), numDice, hitAt, diceSides)))
        .result
        .map(
            chooser -> {
              do {
                org.triplea.swing.EventThreadJOptionPane.showMessageDialog(
                    null,
                    chooser,
                    title,
                    javax.swing.JOptionPane.PLAIN_MESSAGE,
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
      return org.triplea.java.collections.CollectionUtils.getAny(candidates);
    }
    messageAndDialogThreadPool.waitForAll();
    final Supplier<SelectTerritoryComponent> action =
        () -> {
          var panel = new SelectTerritoryComponent(currentTerritory, candidates, mapPanel);
          panel.setLabelText(unitMessage);
          return panel;
        };
    return org.triplea.java.Interruptibles.awaitResult(
            () -> org.triplea.swing.SwingAction.invokeAndWaitResult(action))
        .result
        .map(
            panel -> {
              final String[] options = {"OK"};
              final String title =
                  "Select territory for air units to land, current territory is "
                      + currentTerritory.getName();
              org.triplea.swing.EventThreadJOptionPane.showOptionDialog(
                  this,
                  panel,
                  title,
                  javax.swing.JOptionPane.OK_CANCEL_OPTION,
                  javax.swing.JOptionPane.PLAIN_MESSAGE,
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
      final games.strategy.engine.data.GamePlayer player,
      final List<Territory> territoryChoices,
      final List<Unit> unitChoices,
      final int unitsPerPick) {
    // total hacks
    messageAndDialogThreadPool.waitForAll();
    org.triplea.java.Interruptibles.await(
        () ->
            org.triplea.swing.SwingAction.invokeAndWait(
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
      org.triplea.java.Interruptibles.await(
          () -> org.triplea.swing.SwingAction.invokeAndWait(() -> tabsPanel.remove(index)));
    }
    actionButtonsPanel.getCurrent().ifPresent(actionPanel -> actionPanel.setActive(false));
    return territoryAndUnits;
  }

  private void showGame() {
    inGame.set(true);
    final boolean inHistoryCompareTrueAndSetFalse = inHistory.compareAndSet(true, false);
    if (inHistoryCompareTrueAndSetFalse) {
      if (historySyncher != null) {
        historySyncher.deactivate();
        historySyncher = null;
      }
      historyPanel = null;
      updatePanelsGameData(data);
    }
    // Are we coming from showHistory mode or showMapOnly mode?
    javax.swing.SwingUtilities.invokeLater(
        () -> {
          if (inHistoryCompareTrueAndSetFalse) {
            tabsPanel.removeAll();
          }
          setWidgetActivation();
          addTabs(null);
          actionButtonsPanel.getCurrent().ifPresent(actionPanel -> actionPanel.setActive(true));
          gameMainPanel.removeAll();
          gameMainPanel.setLayout(new java.awt.BorderLayout());
          gameMainPanel.add(gameCenterPanel, java.awt.BorderLayout.CENTER);
          gameMainPanel.add(bottomBar, java.awt.BorderLayout.SOUTH);
          getContentPane().removeAll();
          getContentPane().add(gameMainPanel, java.awt.BorderLayout.CENTER);
          validate();
          requestWindowFocus();
        });
    mapPanel.setRoute(null);
  }

  private void updatePanelsGameData(final games.strategy.engine.data.GameData newGameData) {
    mapPanel.setGameData(newGameData);
    bottomBar.setGameDataForCurrentTerritory(newGameData);
    if (!games.strategy.triplea.delegate.TechAdvance.getTechAdvances(
            newGameData.getTechnologyFrontier(), null)
        .isEmpty()) {
      technologyPanel.setGameData(newGameData);
    }
    statsPanel.setGameData(newGameData);
    economyPanel.setGameData(newGameData);
    if (objectivePanel != null) {
      objectivePanel.setGameData(newGameData);
    }
    territoryDetailPanel.setGameData(newGameData);
  }

  private void setWidgetActivation() {
    org.triplea.swing.SwingAction.invokeNowOrLater(
        () -> {
          showHistoryAction.setEnabled(!inHistory.get());
          showGameAction.setEnabled(!inGame.get());
          if (editModeButtonModel != null) {
            editModeButtonModel.setEnabled(editDelegate != null);
          }
        });
  }

  private void addTabs(games.strategy.triplea.ui.history.HistoryDetailsPanel historyDetailPanel) {
    if (historyDetailPanel != null) {
      tabsPanel.add("History", historyDetailPanel);
    } else {
      addTab("Actions", actionButtonsPanel, org.triplea.swing.key.binding.KeyCode.C);
    }
    addTab("Players", statsPanel, org.triplea.swing.key.binding.KeyCode.P);
    if (!games.strategy.triplea.delegate.TechAdvance.getTechAdvances(
            data.getTechnologyFrontier(), null)
        .isEmpty()) {
      addTab("Technology", technologyPanel, org.triplea.swing.key.binding.KeyCode.Y);
    }
    addTab("Resources", economyPanel, org.triplea.swing.key.binding.KeyCode.R);
    if (objectivePanel != null) {
      String objectivePanelName = new ObjectiveProperties(uiContext.getResourceLoader()).getName();
      addTab(objectivePanelName, objectivePanel, org.triplea.swing.key.binding.KeyCode.O);
    }
    addTab("Territory", territoryDetailPanel, org.triplea.swing.key.binding.KeyCode.T);
    if (mapPanel.getEditMode()) {
      showEditMode();
    }
  }

  private void addTab(
      final String title,
      final java.awt.Component component,
      final org.triplea.swing.key.binding.KeyCode hotkey) {
    tabsPanel.addTab(title, null, component, "Hotkey: CTRL+" + hotkey);
    org.triplea.swing.key.binding.SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        this,
        hotkey,
        () ->
            tabsPanel.setSelectedIndex(
                java.util.List.of(tabsPanel.getComponents()).indexOf(component)));
  }

  private void showEditMode() {
    tabsPanel.addTab("Edit", editPanel);
    if (editDelegate != null) {
      tabsPanel.setSelectedComponent(editPanel);
    }
    editModeButtonModel.setSelected(true);
    getGlassPane().setVisible(true);
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
    games.strategy.ui.Util.ensureNotOnEventDispatchThread();
    final Map<Territory, IntegerMap<Unit>> selection = new java.util.HashMap<>();
    if (possibleUnitsToAttack == null
        || possibleUnitsToAttack.isEmpty()
        || attackResourceToken == null
        || maxNumberOfAttacksAllowed <= 0) {
      return selection;
    }
    messageAndDialogThreadPool.waitForAll();
    final java.util.concurrent.CountDownLatch continueLatch =
        new java.util.concurrent.CountDownLatch(1);
    final java.util.Collection<IndividualUnitPanelGrouped> unitPanels = new java.util.ArrayList<>();
    javax.swing.SwingUtilities.invokeLater(
        () -> {
          final Map<String, Collection<Unit>> possibleUnitsToAttackStringForm =
              new java.util.HashMap<>();
          final games.strategy.triplea.util.TuvCostsCalculator tuvCalculator =
              new games.strategy.triplea.util.TuvCostsCalculator();
          for (final java.util.Map.Entry<Territory, Collection<Unit>> entry :
              possibleUnitsToAttack.entrySet()) {
            final List<Unit> units = new java.util.ArrayList<>(entry.getValue());
            final List<Unit> sortedUnits =
                games.strategy.triplea.delegate.battle.casualty.CasualtySelector
                    .getCasualtyOrderOfLoss(
                        units,
                        units.get(0).getOwner(),
                        games.strategy.triplea.delegate.power.calculator.CombatValueBuilder
                            .mainCombatValue()
                            .enemyUnits(java.util.List.of())
                            .friendlyUnits(java.util.List.of())
                            .side(games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE)
                            .gameSequence(data.getSequence())
                            .supportAttachments(data.getUnitTypeList().getSupportRules())
                            .lhtrHeavyBombers(
                                games.strategy.triplea.Properties.getLhtrHeavyBombers(
                                    data.getProperties()))
                            .gameDiceSides(data.getDiceSides())
                            .territoryEffects(
                                games.strategy.triplea.delegate.TerritoryEffectHelper.getEffects(
                                    entry.getKey()))
                            .build(),
                        entry.getKey(),
                        tuvCalculator.getCostsForTuv(units.get(0).getOwner()),
                        data);
            // OOL is ordered with the first unit the owner would want to remove but in a kamikaze
            // the player who picks is the attacker, so flip the order
            java.util.Collections.reverse(sortedUnits);
            possibleUnitsToAttackStringForm.put(entry.getKey().getName(), sortedUnits);
          }
          mapPanel.centerOn(
              data.getMap()
                  .getTerritoryOrNull(
                      org.triplea.java.collections.CollectionUtils.getAny(
                          possibleUnitsToAttackStringForm.keySet())));
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
          final javax.swing.JOptionPane optionPane =
              new javax.swing.JOptionPane(
                  unitPanel,
                  javax.swing.JOptionPane.PLAIN_MESSAGE,
                  javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                  null,
                  options,
                  options[1]);
          final javax.swing.JDialog dialog =
              new javax.swing.JDialog(
                  (Frame) getParent(),
                  "Select units to Suicide Attack using " + attackResourceToken.getName());
          dialog.setContentPane(optionPane);
          dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
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
                    for (final java.util.Map.Entry<String, IntegerMap<Unit>> entry :
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
    org.triplea.java.Interruptibles.await(continueLatch);
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
    games.strategy.ui.Util.ensureNotOnEventDispatchThread();
    final java.util.concurrent.CountDownLatch continueLatch =
        new java.util.concurrent.CountDownLatch(1);
    final Map<Territory, Collection<Unit>> selection = new java.util.HashMap<>();
    final Collection<Tuple<Territory, UnitChooser>> choosers = new java.util.ArrayList<>();
    javax.swing.SwingUtilities.invokeLater(
        () -> {
          mapPanel.centerOn(scrambleTo);
          final javax.swing.JDialog dialog =
              new javax.swing.JDialog(this, "Select units to scramble to " + scrambleTo.getName());
          final javax.swing.JPanel panel = new javax.swing.JPanel();
          panel.setLayout(new java.awt.BorderLayout());
          final javax.swing.JButton scrambleButton = new javax.swing.JButton("Scramble");
          scrambleButton.addActionListener(
              e -> getOptionPane((javax.swing.JComponent) e.getSource()).setValue(scrambleButton));
          final javax.swing.JButton noneButton = new javax.swing.JButton("None");
          noneButton.addActionListener(
              e -> getOptionPane((javax.swing.JComponent) e.getSource()).setValue(noneButton));
          final Object[] options = {scrambleButton, noneButton};
          final javax.swing.JOptionPane optionPane =
              new javax.swing.JOptionPane(
                  panel,
                  javax.swing.JOptionPane.PLAIN_MESSAGE,
                  javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                  null,
                  options,
                  options[1]);
          final javax.swing.JLabel whereTo =
              new javax.swing.JLabel("Scramble To: " + scrambleTo.getName());
          whereTo.setFont(
              new java.awt.Font(
                  games.strategy.triplea.image.MapImage.FONT_FAMILY_DEFAULT,
                  java.awt.Font.ITALIC,
                  12));
          panel.add(whereTo, java.awt.BorderLayout.NORTH);
          final javax.swing.JPanel panel2 = new javax.swing.JPanel();
          panel2.setBorder(javax.swing.BorderFactory.createEmptyBorder());
          panel2.setLayout(new java.awt.FlowLayout());
          final javax.swing.JPanel fuelCostPanel =
              new javax.swing.JPanel(new java.awt.GridBagLayout());
          panel.add(fuelCostPanel, java.awt.BorderLayout.SOUTH);
          for (final Territory from : possibleScramblers.keySet()) {
            final javax.swing.JPanel panelChooser = new javax.swing.JPanel();
            panelChooser.setLayout(
                new javax.swing.BoxLayout(panelChooser, javax.swing.BoxLayout.Y_AXIS));
            panelChooser.setBorder(javax.swing.BorderFactory.createLineBorder(getBackground()));
            final javax.swing.JLabel whereFrom = new javax.swing.JLabel("From: " + from.getName());
            whereFrom.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
            whereFrom.setFont(
                new java.awt.Font(
                    games.strategy.triplea.image.MapImage.FONT_FAMILY_DEFAULT,
                    java.awt.Font.BOLD,
                    12));
            panelChooser.add(whereFrom);
            panelChooser.add(new javax.swing.JLabel(" "));
            final Collection<Unit> possible = possibleScramblers.get(from).getSecond();
            final int maxAllowed =
                Math.min(
                    games.strategy.triplea.delegate.battle.ScrambleLogic.getMaxScrambleCount(
                        possibleScramblers.get(from).getFirst()),
                    possible.size());
            final UnitChooser chooser =
                new UnitChooser(possible, java.util.Map.of(), false, uiContext);
            chooser.setMaxAndShowMaxButton(maxAllowed);
            chooser.addChangeListener(
                field -> {
                  final Map<games.strategy.engine.data.GamePlayer, ResourceCollection>
                      playerFuelCost = new java.util.HashMap<>();
                  for (final Tuple<Territory, UnitChooser> tuple : choosers) {
                    final Map<games.strategy.engine.data.GamePlayer, ResourceCollection> map =
                        games.strategy.engine.data.Route.getScrambleFuelCostCharge(
                            tuple.getSecond().getSelected(false),
                            tuple.getFirst(),
                            scrambleTo,
                            data);
                    for (final java.util.Map.Entry<
                            games.strategy.engine.data.GamePlayer, ResourceCollection>
                        playerAndCost : map.entrySet()) {
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
                  for (final java.util.Map.Entry<
                          games.strategy.engine.data.GamePlayer, ResourceCollection>
                      entry : playerFuelCost.entrySet()) {
                    final javax.swing.JLabel label =
                        new javax.swing.JLabel(entry.getKey().getName() + ": ");
                    fuelCostPanel.add(
                        label,
                        new java.awt.GridBagConstraints(
                            0,
                            count,
                            1,
                            1,
                            0,
                            0,
                            java.awt.GridBagConstraints.WEST,
                            java.awt.GridBagConstraints.NONE,
                            new java.awt.Insets(0, 0, 0, 0),
                            0,
                            0));
                    fuelCostPanel.add(
                        uiContext.getResourceImageFactory().getResourcesPanel(entry.getValue()),
                        new java.awt.GridBagConstraints(
                            1,
                            count++,
                            1,
                            1,
                            0,
                            0,
                            java.awt.GridBagConstraints.WEST,
                            java.awt.GridBagConstraints.NONE,
                            new java.awt.Insets(0, 0, 0, 0),
                            0,
                            0));
                    if (!entry.getKey().getResources().has(entry.getValue().getResourcesCopy())) {
                      hasEnoughFuel = false;
                      label.setForeground(java.awt.Color.RED);
                    }
                  }
                  scrambleButton.setEnabled(hasEnoughFuel);
                  dialog.pack();
                });
            choosers.add(org.triplea.util.Tuple.of(from, chooser));
            panelChooser.add(chooser);
            final javax.swing.JScrollPane chooserScrollPane =
                new javax.swing.JScrollPane(panelChooser);
            panel2.add(chooserScrollPane);
          }
          panel.add(panel2, java.awt.BorderLayout.CENTER);
          dialog.setContentPane(optionPane);
          dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
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
    org.triplea.java.Interruptibles.await(continueLatch);
    mapPanel.getUiContext().removeShutdownLatch(continueLatch);
    return selection;
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
    games.strategy.ui.Util.ensureNotOnEventDispatchThread();
    final java.util.concurrent.CountDownLatch continueLatch =
        new java.util.concurrent.CountDownLatch(1);
    final Collection<Unit> selection = new java.util.ArrayList<>();
    javax.swing.SwingUtilities.invokeLater(
        () -> {
          mapPanel.centerOn(current);
          final javax.swing.JPanel panel = new javax.swing.JPanel();
          panel.setLayout(new java.awt.BorderLayout());
          final javax.swing.JLabel messageLabel = new javax.swing.JLabel(message);
          messageLabel.setFont(
              new java.awt.Font(
                  games.strategy.triplea.image.MapImage.FONT_FAMILY_DEFAULT,
                  java.awt.Font.ITALIC,
                  12));
          panel.add(messageLabel, java.awt.BorderLayout.NORTH);
          final javax.swing.JPanel panelChooser = new javax.swing.JPanel();
          panelChooser.setLayout(
              new javax.swing.BoxLayout(panelChooser, javax.swing.BoxLayout.Y_AXIS));
          panelChooser.setBorder(javax.swing.BorderFactory.createLineBorder(getBackground()));
          final javax.swing.JLabel whereFrom = new javax.swing.JLabel("From: " + current.getName());
          whereFrom.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
          whereFrom.setFont(
              new java.awt.Font(
                  games.strategy.triplea.image.MapImage.FONT_FAMILY_DEFAULT,
                  java.awt.Font.BOLD,
                  12));
          panelChooser.add(whereFrom);
          panelChooser.add(new javax.swing.JLabel(" "));
          final int maxAllowed =
              Math.min(
                  games.strategy.triplea.delegate.battle.AirBattle.getMaxInterceptionCount(
                      current, possible),
                  possible.size());
          final UnitChooser chooser =
              new UnitChooser(possible, java.util.Map.of(), false, uiContext);
          chooser.setMaxAndShowMaxButton(maxAllowed);
          panelChooser.add(chooser);
          final javax.swing.JScrollPane chooserScrollPane =
              new javax.swing.JScrollPane(panelChooser);
          panel.add(chooserScrollPane, java.awt.BorderLayout.CENTER);
          final String optionSelect = "Select";
          final String optionNone = "None";
          final Object[] options = {optionSelect, optionNone};
          final javax.swing.JOptionPane optionPane =
              new javax.swing.JOptionPane(
                  panel,
                  javax.swing.JOptionPane.PLAIN_MESSAGE,
                  javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                  null,
                  options,
                  options[1]);
          final javax.swing.JDialog dialog = new javax.swing.JDialog((Frame) getParent(), message);
          dialog.setContentPane(optionPane);
          dialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
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
    org.triplea.java.Interruptibles.await(continueLatch);
    mapPanel.getUiContext().removeShutdownLatch(continueLatch);
    return selection;
  }

  public games.strategy.triplea.attachments.PoliticalActionAttachment getPoliticalActionChoice(
      final games.strategy.engine.data.GamePlayer player,
      final boolean firstRun,
      final games.strategy.triplea.delegate.remote.IPoliticsDelegate politicsDelegate) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToPolitics(player);
    requestWindowFocus();
    return actionButtonsPanel.waitForPoliticalAction(firstRun, politicsDelegate);
  }

  public games.strategy.triplea.attachments.UserActionAttachment getUserActionChoice(
      final games.strategy.engine.data.GamePlayer player,
      final boolean firstRun,
      final games.strategy.triplea.delegate.remote.IUserActionDelegate userActionDelegate) {
    messageAndDialogThreadPool.waitForAll();
    actionButtonsPanel.changeToUserActions(player);
    requestWindowFocus();
    return actionButtonsPanel.waitForUserActionAction(firstRun, userActionDelegate);
  }

  public games.strategy.triplea.delegate.data.TechRoll getTechRolls(
      final games.strategy.engine.data.GamePlayer gamePlayer) {
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
          final JList<Territory> list =
              new javax.swing.JList<>(org.triplea.swing.SwingComponents.newListModel(candidates));
          list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
          list.setSelectedIndex(0);
          final javax.swing.JPanel panel = new javax.swing.JPanel();
          panel.setLayout(new java.awt.BorderLayout());
          final javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(list);
          panel.add(scroll, java.awt.BorderLayout.CENTER);
          if (from != null) {
            panel.add(
                java.awt.BorderLayout.NORTH,
                new javax.swing.JLabel("Targets for rocket in " + from.getName()));
          }
          final String[] options = {"OK", "Don't attack"};
          final String message = "Select Rocket Target";
          final int selection =
              javax.swing.JOptionPane.showOptionDialog(
                  TripleAFrame.this,
                  panel,
                  message,
                  javax.swing.JOptionPane.OK_CANCEL_OPTION,
                  javax.swing.JOptionPane.PLAIN_MESSAGE,
                  null,
                  options,
                  null);
          return (selection == 0) ? list.getSelectedValue() : null;
        };
    return org.triplea.java.Interruptibles.awaitResult(
            () -> org.triplea.swing.SwingAction.invokeAndWaitResult(action))
        .result
        .orElse(null);
  }

  private void updateStep() {
    if (javax.swing.SwingUtilities.isEventDispatchThread()) {
      org.triplea.java.ThreadRunner.runInNewThread(this::updateStepFromEdt);
    } else {
      updateStepFromEdt();
    }
  }

  private void updateStepFromEdt() {
    Preconditions.checkState(
        !javax.swing.SwingUtilities.isEventDispatchThread(),
        "This method must not be invoked on the EDT!");
    if (uiContext.isShutDown()) {
      return;
    }
    final int round;
    final String stepDisplayName;
    final games.strategy.engine.data.GamePlayer player;
    try (games.strategy.engine.data.GameData.Unlocker ignored = data.acquireReadLock()) {
      round = data.getSequence().getRound();
      final games.strategy.engine.data.GameStep step = data.getSequence().getStep();
      if (step == null) {
        return;
      }
      stepDisplayName = step.getDisplayName();
      player = step.getPlayerId();
    }

    uiContext.setCurrentPlayer(player);

    javax.swing.SwingUtilities.invokeLater(
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
        if (!inHistory.get()) {
          showHistory();
        }
      }
    }
  }

  /**
   * Invoked at the start of a player's turn to play a sound alerting the player it is their turn
   * and to center the map on the player's capital.
   */
  public void performStartPlayerTurnActionsIfNeeded(
      final games.strategy.engine.data.GamePlayer player) {
    if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
      try {
        org.triplea.swing.SwingAction.invokeAndWait(
            () -> performStartPlayerTurnActionsIfNeeded(player));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return;
    }
    if (player == null || !org.triplea.java.Interruptibles.sleep(300)) {
      return;
    }
    // Play start-player-turn sound if the previous player was remote
    final Boolean play = requiredTurnSeries.get(player);
    if (play != null && play) {
      uiContext
          .getClipPlayer()
          .play(org.triplea.sound.SoundPath.CLIP_REQUIRED_YOUR_TURN_SERIES, player);
      requiredTurnSeries.put(player, false);
    }
    final int round;
    try (games.strategy.engine.data.GameData.Unlocker ignored = data.acquireReadLock()) {
      round = data.getSequence().getRound();
    }
    // Check if a new player has its turn or if it is the same player but during a different round
    if (!java.util.Objects.equals(player, lastPlayer) || (round != lastPlayerRound)) {
      lastPlayer = player;
      lastPlayerRound = round;
      bottomBar.updateFromCurrentPlayer();
      try (games.strategy.engine.data.GameData.Unlocker ignored = data.acquireReadLock()) {
        games.strategy.triplea.attachments.TerritoryAttachment
            .getFirstOwnedCapitalOrFirstUnownedCapital(player, data.getMap())
            .ifPresent(territory -> mapPanel.centerOn(territory));
        mapPanel.repaint();
      }
    }
  }

  private String getUnitInfo() {
    if (!mapPanel.getMouseHoverUnits().isEmpty()) {
      final games.strategy.engine.data.Unit unit = mapPanel.getMouseHoverUnits().get(0);
      return MapUnitTooltipManager.getTooltipTextForUnit(
          unit.getType(), unit.getOwner(), mapPanel.getMouseHoverUnits().size(), uiContext);
    }
    return "";
  }

  public void showActionPanelTab() {
    tabsPanel.setSelectedIndex(0);
  }

  private java.awt.event.KeyListener getArrowKeyListener() {
    return new java.awt.event.KeyListener() {
      @Override
      public void keyTyped(final java.awt.event.KeyEvent e) {
        // not needed interface method
      }

      @Override
      public void keyPressed(final java.awt.event.KeyEvent e) {
        isCtrlPressed = e.isControlDown();
        // scroll map according to wasd/arrow keys
        final int diffPixel = computeScrollSpeed();
        final int x = mapPanel.getXOffset();
        final int y = mapPanel.getYOffset();
        final int keyCode = e.getKeyCode();

        if (keyCode == java.awt.event.KeyEvent.VK_RIGHT) {
          getMapPanel().setTopLeft(x + diffPixel, y);
        } else if (keyCode == java.awt.event.KeyEvent.VK_LEFT) {
          getMapPanel().setTopLeft(x - diffPixel, y);
        } else if (keyCode == java.awt.event.KeyEvent.VK_DOWN) {
          getMapPanel().setTopLeft(x, y + diffPixel);
        } else if (keyCode == java.awt.event.KeyEvent.VK_UP) {
          getMapPanel().setTopLeft(x, y - diffPixel);
        }
      }

      @Override
      public void keyReleased(final java.awt.event.KeyEvent e) {
        isCtrlPressed = e.isControlDown();
      }
    };
  }

  private int computeScrollSpeed() {
    return games.strategy.triplea.settings.ClientSetting.arrowKeyScrollSpeed.getValueOrThrow()
        * (isCtrlPressed
            ? games.strategy.triplea.settings.ClientSetting.fasterArrowKeyScrollMultiplier
                .getValueOrThrow()
            : 1);
  }

  private void showHistory() {
    inHistory.set(true);
    inGame.set(false);
    setWidgetActivation();
    final games.strategy.engine.data.GameData clonedGameData;
    try (games.strategy.engine.data.GameData.Unlocker ignored = data.acquireWriteLock()) {
      // we want to use a clone of the data, so we can make changes to it as we walk up and down the
      // history
      final var cloneOptions =
          games.strategy.engine.framework.GameDataManager.Options.builder()
              .withHistory(true)
              .build();
      clonedGameData =
          games.strategy.engine.framework.GameDataUtils.cloneGameData(data, cloneOptions)
              .orElse(null);
      if (clonedGameData == null) {
        return;
      }
    }
    historySyncher = new games.strategy.engine.framework.HistorySynchronizer(clonedGameData, game);
    updatePanelsGameData(clonedGameData);
    org.triplea.java.Interruptibles.await(
        () ->
            org.triplea.swing.SwingAction.invokeAndWait(
                () -> {
                  final games.strategy.triplea.ui.history.HistoryDetailsPanel historyDetailPanel =
                      getHistoryDetailsPanel(clonedGameData);
                  // create history tree context menu
                  final javax.swing.JSplitPane historyComponentSplitPane =
                      new javax.swing.JSplitPane();
                  historyComponentSplitPane.setOneTouchExpandable(true);
                  historyComponentSplitPane.setContinuousLayout(true);
                  historyComponentSplitPane.setDividerSize(8);
                  historyComponentSplitPane.setLeftComponent(historyPanel);
                  historyComponentSplitPane.setRightComponent(gameCenterPanel);
                  historyComponentSplitPane.setDividerLocation(150);
                  final javax.swing.JPanel historyComponent =
                      new org.triplea.swing.jpanel.JPanelBuilder()
                          .borderLayout()
                          .addCenter(historyComponentSplitPane)
                          .addSouth(bottomBar)
                          .build();

                  tabsPanel.removeAll();
                  addTabs(historyDetailPanel);
                  actionButtonsPanel
                      .getCurrent()
                      .ifPresent(actionPanel -> actionPanel.setActive(false));
                  getContentPane().removeAll();
                  getContentPane().add(historyComponent, java.awt.BorderLayout.CENTER);
                  validate();
                }));
  }

  @javax.annotation.Nonnull
  private games.strategy.triplea.ui.history.HistoryDetailsPanel getHistoryDetailsPanel(
      games.strategy.engine.data.GameData clonedGameData) {
    final games.strategy.triplea.ui.history.HistoryDetailsPanel historyDetailPanel =
        new games.strategy.triplea.ui.history.HistoryDetailsPanel(clonedGameData, mapPanel);
    // actions need to clear the history panel popup state when done
    final games.strategy.triplea.ui.history.HistoryPanel popupHistoryPanel =
        new games.strategy.triplea.ui.history.HistoryPanel(
            clonedGameData, historyDetailPanel, uiContext);
    final javax.swing.JPopupMenu popup =
        new HistoryPanelPopupMenuBuilder()
            .add("Show Summary Log", () -> showHistoryLog(popupHistoryPanel, false, clonedGameData))
            .add("Show Detailed Log", () -> showHistoryLog(popupHistoryPanel, true, clonedGameData))
            .add(
                "Export Gameboard Picture",
                () -> {
                  games.strategy.triplea.ui.export.ScreenshotExporter.exportScreenshot(
                      TripleAFrame.this, clonedGameData, popupHistoryPanel.getCurrentPopupNode());
                  popupHistoryPanel.clearCurrentPopupNode();
                })
            .add(
                "Save Game at this point (BETA)",
                () -> {
                  javax.swing.JOptionPane.showMessageDialog(
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
                      javax.swing.JOptionPane.INFORMATION_MESSAGE);

                  final Optional<Path> f =
                      games.strategy.engine.framework.startup.ui.panels.main.game.selector
                          .GameFileSelector.getSaveGameLocation(TripleAFrame.this, clonedGameData);
                  if (f.isPresent()) {
                    try (java.io.OutputStream fileOutputStream =
                        java.nio.file.Files.newOutputStream(f.get())) {
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
                      final java.util.Enumeration<?> enumeration =
                          ((javax.swing.tree.DefaultMutableTreeNode)
                                  clonedGameData.getHistory().getRoot())
                              .preorderEnumeration();
                      enumeration.nextElement();
                      int round = 0;
                      String stepDisplayName =
                          clonedGameData.getSequence().getStep(0).getDisplayName();
                      games.strategy.engine.data.GamePlayer currentPlayer =
                          clonedGameData.getSequence().getStep(0).getPlayerId();
                      int roundOffset = clonedGameData.getSequence().getRoundOffset();
                      while (enumeration.hasMoreElements()) {
                        final games.strategy.engine.history.HistoryNode node =
                            (games.strategy.engine.history.HistoryNode) enumeration.nextElement();
                        if (node instanceof games.strategy.engine.history.Round nodeRound) {
                          round = Math.max(0, nodeRound.getRoundNo() - roundOffset);
                          currentPlayer = null;
                          stepDisplayName = nodeRound.getTitle();
                        } else if (node instanceof Step step) {
                          currentPlayer = step.getPlayerId().orElse(null);
                          stepDisplayName = node.getTitle();
                        }
                      }
                      clonedGameData
                          .getSequence()
                          .setRoundAndStep(round, stepDisplayName, currentPlayer);
                      games.strategy.engine.framework.GameDataManager.saveGame(
                          fileOutputStream, clonedGameData);
                      javax.swing.JOptionPane.showMessageDialog(
                          TripleAFrame.this,
                          "Game Saved",
                          "Game Saved",
                          javax.swing.JOptionPane.INFORMATION_MESSAGE);
                    } catch (final java.io.IOException e) {
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

  private void showHistoryLog(
      final games.strategy.triplea.ui.history.HistoryPanel popupHistoryPanel,
      boolean verboseLog,
      games.strategy.engine.data.GameData clonedGameData) {
    final games.strategy.triplea.ui.history.HistoryLog historyLog =
        new games.strategy.triplea.ui.history.HistoryLog(this);
    final games.strategy.engine.history.HistoryNode currentPopupNodeOrLastNode =
        popupHistoryPanel.getCurrentPopupNode();
    historyLog.printRemainingTurn(
        currentPopupNodeOrLastNode, verboseLog, data.getDiceSides(), null);
    historyLog.printTerritorySummary(currentPopupNodeOrLastNode, clonedGameData);
    historyLog.printProductionSummary(clonedGameData);
    popupHistoryPanel.clearCurrentPopupNode();
    historyLog.setVisible(true);
  }

  // setEditDelegate is called by TripleAPlayer at the start and end of a turn
  public void setEditDelegate(
      final games.strategy.triplea.delegate.remote.IEditDelegate editDelegate) {
    this.editDelegate = editDelegate;
    // force a data change event to update the UI for edit mode
    dataChangeListener.gameDataChanged(
        games.strategy.engine.data.changefactory.ChangeFactory.EMPTY_CHANGE);
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
    final java.util.concurrent.atomic.AtomicReference<javax.swing.JScrollPane> panelRef =
        new java.util.concurrent.atomic.AtomicReference<>();
    final java.util.concurrent.atomic.AtomicReference<UnitChooser> chooserRef =
        new java.util.concurrent.atomic.AtomicReference<>();
    org.triplea.java.Interruptibles.await(
        () ->
            org.triplea.swing.SwingAction.invokeAndWait(
                () -> {
                  final UnitChooser chooser =
                      new UnitChooser(fighters, java.util.Map.of(), false, uiContext);
                  final java.awt.Dimension screenResolution =
                      java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                  final int availHeight = screenResolution.height - 120;
                  final int availWidth = screenResolution.width - 40;
                  final javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(chooser);
                  scroll.setBorder(javax.swing.BorderFactory.createEmptyBorder());
                  scroll.setPreferredSize(
                      new java.awt.Dimension(
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
        org.triplea.swing.EventThreadJOptionPane.showOptionDialog(
            this,
            panelRef.get(),
            "Move air units to carrier",
            javax.swing.JOptionPane.PLAIN_MESSAGE,
            javax.swing.JOptionPane.OK_CANCEL_OPTION,
            null,
            new String[] {"OK", "Cancel"},
            "OK",
            getUiContext().getCountDownLatchHandler());
    if (option == javax.swing.JOptionPane.OK_OPTION) {
      return chooserRef.get().getSelected();
    }
    return new java.util.ArrayList<>();
  }

  /** Displays the map located in the directory/archive {@code mapdir}. */
  public void changeMapSkin(final String skinName) {
    uiContext = UiContext.changeMapSkin(data, skinName);
    game.setResourceLoader(uiContext.getResourceLoader());
    // when changing skins, always show relief images
    if (uiContext.getMapData().getHasRelief()) {
      games.strategy.triplea.image.TileImageFactory.setShowReliefImages(true);
    }

    mapPanel.setGameData(data);
    // update map panels to use new image
    mapPanel.changeImage(uiContext.getMapData().getMapDimensions());
    final java.awt.Image small = uiContext.getMapImage().getSmallMapImage();
    smallView.changeImage(small);
    mapPanel.changeSmallMapOffscreenMap();
    // redraw territories
    mapPanel.resetMap();
  }

  public Optional<games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper>
      getInGameLobbyWatcher() {
    return games.strategy.engine.framework.ServerGame.class.isAssignableFrom(getGame().getClass())
        ? java.util.Optional.ofNullable(
            ((games.strategy.engine.framework.ServerGame) getGame()).getInGameLobbyWatcher())
        : java.util.Optional.empty();
  }

  private static class HistoryPanelPopupMenuBuilder {
    private final javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();

    public HistoryPanelPopupMenuBuilder add(String title, Runnable action) {
      popup.add(
          new javax.swing.AbstractAction(title) {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
              action.run();
            }
          });
      return this;
    }

    public javax.swing.JPopupMenu build() {
      return popup;
    }
  }

  /** Create a unit option with icon and description. */
  private class UnitRenderer extends javax.swing.JLabel implements ListCellRenderer<Unit> {

    private static final long serialVersionUID = 1749164256040268579L;

    UnitRenderer() {
      setOpaque(true);
    }

    @Override
    public java.awt.Component getListCellRendererComponent(
        final JList<? extends Unit> list,
        final Unit unit,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      setText(unit.toString() + ", damage=" + unit.getUnitDamage());
      setIcon(uiContext.getUnitImageFactory().getIcon(ImageKey.of(unit)));
      setBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 10));

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

  public boolean hasChat() {
    return chatPanel != null;
  }
}
