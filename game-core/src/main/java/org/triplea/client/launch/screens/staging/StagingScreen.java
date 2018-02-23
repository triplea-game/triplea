package org.triplea.client.launch.screens.staging;

import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.triplea.client.launch.screens.LaunchScreen;
import org.triplea.client.launch.screens.LaunchScreenWindow;
import org.triplea.client.launch.screens.NavigationPanelFactory;
import org.triplea.client.launch.screens.staging.panels.HandicapPanel;
import org.triplea.client.launch.screens.staging.panels.SelectGameWindow;
import org.triplea.client.launch.screens.staging.panels.playerselection.PlayerSelectionModel;
import org.triplea.client.launch.screens.staging.panels.playerselection.PlayerSelectionPanel;

import com.google.common.base.Preconditions;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;
import swinglib.JSplitPaneBuilder;
import swinglib.JTabbedPaneBuilder;

/**
 * Represents the first screen and set of options presented to a user.
 */
public enum StagingScreen {
  SINGLE_PLAYER(LaunchScreen.INITIAL),

  NETWORK_CLIENT(LaunchScreen.JOIN_NETWORK_GAME_OPTIONS),

  NETWORK_HOST(LaunchScreen.HOST_NETWORK_GAME_OPTIONS);

  public final LaunchScreen previousScreen;


  StagingScreen(final LaunchScreen previousScreen) {
    this.previousScreen = previousScreen;
  }

  /**
   * Builder for single player case.
   * TODO: implement fully.
   */
  public JComponent buildScreen(final LaunchScreen previousScreen, final GameData gameData) {
    return buildScreen(
        previousScreen,
        gameData,
        null,
        data -> {
          // TODO: create a PlayerListing object, create a LocalLauncher, invoke 'getLauncher()' and start the game
          ClientLogger.logError("TODO: not yet completed, the multiplayer case should work though : )");
        },
        null,
        null);
  }

  /**
   * Builder method for creating a build screen.
   * TODO: we may want to refactor this.. builder api for params, maybe an enum switch to specify configuration?
   */
  // server if serverModel != null, client if clientModel != null
  public JComponent buildScreen(
      @Nonnull final LaunchScreen previousScreen,
      @Nonnull final GameData gameData,
      @Nullable final ChatSupport chatSupport,
      @Nonnull final Consumer<GameData> launchAction,
      @Nullable final ServerModel serverModel,
      @Nullable final ClientModel clientModel) {

    Preconditions.checkNotNull(previousScreen);
    Preconditions.checkNotNull(gameData);
    Preconditions.checkNotNull(previousScreen);

    final PlayerSelectionModel playerSelectionModel =
        new PlayerSelectionModel(gameData, "currentPlayer", serverModel, clientModel);

    final GameStartedCallback gameStartedCallback = () -> {
    };

    final GameSelectorModel model = new GameSelectorModel();
    model.load(gameData, "");

    final JPanel selectionPanel = JPanelBuilder.builder()
        .borderLayout()
        .addCenter(JPanelBuilder.builder()
            .borderLayout()
            .addNorth(mapSelectionControls(previousScreen, this))
            .addCenter(JPanelBuilder.builder()
                .borderLayout()
                .addNorth(gameInfoPanel(gameData))
                .addCenter(playerAndBidSelectionTabs(gameData, this, playerSelectionModel))
                .build())
            .addSouthIf(chatSupport != null, () -> chatSupport.getChatPanel())
            .build())
        .build();

    return JPanelBuilder.builder()
        .addCenter((chatSupport != null) ? JSplitPaneBuilder.builder()
            .addTop(selectionPanel)
            .addBottom(chatSupport.getChatPanel())
            .build() : selectionPanel)
        .addSouth((this == NETWORK_CLIENT) ? NavigationPanelFactory.buildWithDisabledPlayButton(previousScreen)
            : NavigationPanelFactory.buildWithPlayButton(
                previousScreen,
                () -> launchAction.accept(gameData),
                gameStartedCallback))
        .build();
  }

  private static JPanel mapSelectionControls(final LaunchScreen previousScreen, final StagingScreen currentScreen) {
    return JPanelBuilder.builder()
        .flowLayout()
        .add(JButtonBuilder.builder()
            .title("Select Map")
            .actionListener(SelectGameWindow.openSelectGameWindow(previousScreen, currentScreen))
            .build())
        .add(JButtonBuilder.builder()
            .title("Download Maps")
            .actionListener(DownloadMapsWindow::showDownloadMapsWindow)
            .build())
        .add(JButtonBuilder.builder()
            .title("Open Save Game")
            .actionListener(() -> {
              GameRunner.showSaveGameFileChooser().ifPresent(file -> {
                ClientSetting.SELECTED_GAME_LOCATION.save(file.getAbsolutePath());
                try {
                  final GameData newData = GameDataManager.loadGame(file);
                  LaunchScreenWindow.draw(previousScreen, currentScreen, newData);
                } catch (final IOException e) {
                  ClientLogger.logError("Failed to load: " + file.getAbsolutePath(), e);
                }
              });
            })
            .build())
        .add(Box.createVerticalGlue())
        .borderEtched()
        .build();
  }

  private static JPanel gameInfoPanel(final GameData gameData) {
    return JPanelBuilder.builder()
        .flowLayout()
        .borderEtched()
        .add(
            JPanelBuilder.builder()
                .verticalBoxLayout()
                .add(new JLabel("<html><h1>" + gameData.getGameName() + " </h1></html>"))
                .add(new JLabel("Version: " + gameData.getGameVersion()))
                .add(new JLabel("Round: " + gameData.getSequence().getRound()))
                .build())
        .build();
  }

  private static JComponent playerAndBidSelectionTabs(
      final GameData gameData,
      final StagingScreen stagingScreen,
      final PlayerSelectionModel playerSelectionModel) {

    final PlayerSelectionPanel playerSelectionPanel = new PlayerSelectionPanel(stagingScreen, playerSelectionModel);

    return JSplitPaneBuilder.builder()
        .dividerLocation(240)
        .addLeft(SwingComponents.newJScrollPane(MapOptionsPanel.build(gameData, stagingScreen)))
        .addRight(
            JTabbedPaneBuilder.builder()
                .addTab("Player Selection",
                    SwingComponents.newJScrollPane(JPanelBuilder.builder()
                        .verticalBoxLayout()
                        .addEach(playerSelectionPanel.build())
                        .build()))
                .addTab("Handicap",
                    SwingComponents.newJScrollPane(
                        JPanelBuilder.builder()
                            .verticalBoxLayout()
                            .addEach(HandicapPanel.build(gameData))
                            .build()))
                .build())
        .build();
  }

}
