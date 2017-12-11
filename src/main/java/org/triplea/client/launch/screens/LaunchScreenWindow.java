package org.triplea.client.launch.screens;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JFrame;

import org.triplea.client.launch.screens.staging.ChatSupport;
import org.triplea.client.launch.screens.staging.StagingScreen;

import games.strategy.engine.ClientContext;
import games.strategy.engine.chat.ChatPanel;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.ServerLauncher;
import games.strategy.engine.framework.startup.mc.ServerConnectionProps;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.triplea.settings.ClientSetting;


/**
 * An enum singleton representing a content agnostic shell for rendering launch window content. Class controls
 * the container {@code JFrame} and the controller logic for rendering screen content, but does not own the actual
 * rendering logic and is otherwise content agnostic.
 * <p>
 * This window should close when any of the following happen: a game is launched,
 * connect to lobby, or user quits the application.
 * </p>
 */
public enum LaunchScreenWindow {
  INSTANCE;

  private JFrame frame;


  public static void dispose() {
    INSTANCE.disposeFrame();
  }

  /**
   * Closes the launch screen window.
   * <p>
   * Synchronized in case Swing queues and releases user action in parallel and at the same time.
   * IE: the close window action is blocked but EDT thread is not, user clicks multiple times to close window
   * and queues the action multiple times. In this rare case we could get an NPE with concurrent execution
   * of `dispose()`
   * </p>
   */
  synchronized void disposeFrame() {
    if (frame != null) {
      frame.dispose();
    }
    // INSTANCE is an enum element, exists in a static context, it will not be garbage collected.
    // To allow GC of the swing window we set it to null.
    frame = null;
  }


  static void drawWithClientNetworking(final StagingScreen stagingScreen,
      final ServerConnectionProps serverProps) {
    final GameData data = parseGameData();
    final ChatSupport chatSupport = new ChatSupport(serverProps);

    // TODO: create client model, wire it forward to building the screen..
    // replaceContents(stagingScreen.buildScreen(stagingScreen.previousScreen, data, chatSupport));
  }

  public static void draw(
      final LaunchScreen previousScreen,
      final StagingScreen stagingScreen,
      final GameData gameData) {
    replaceContents(stagingScreen.buildScreen(previousScreen, gameData));
  }

  public static void draw(final LaunchScreen launchScreen) {
    replaceContents(launchScreen.buildScreen());
  }

  /**
   * Makes the launch screen window visible if it is not already.
   */
  public static void draw(final StagingScreen stagingScreen) {
    final GameData data = parseGameData();
    replaceContents(stagingScreen.buildScreen(stagingScreen.previousScreen, data));
  }

  private static GameData parseGameData() {
    try {
      return GameChooserEntry.newGameChooserEntry().fullyParseGameData();
    } catch (final IllegalArgumentException e) {
      try {
        return GameDataManager.loadGame(new File(ClientSetting.SELECTED_GAME_LOCATION.value()));
      } catch (final IOException e1) {
        throw new RuntimeException(e);
      }
    } catch (final GameParseException e) {
      throw new RuntimeException(e);
    }
  }

  private static void replaceContents(final JComponent newContents) {
    final Dimension size = INSTANCE.frame.getSize();
    INSTANCE.frame.getContentPane().removeAll();
    INSTANCE.frame.getContentPane().add(newContents);
    INSTANCE.frame.setSize(size);
    INSTANCE.frame.pack();
    INSTANCE.frame.setSize(size);
  }

  static void drawWithServerNetworking(
      @Nonnull final StagingScreen stagingScreen,
      @Nonnull final ServerConnectionProps serverProps) {
    final GameData data = parseGameData();

    final ServerModel serverModel = new ServerModel(serverProps);

    final ChatSupport chatSupport = new ChatSupport((ChatPanel) serverModel.getChatPanel());
    final Consumer<GameData> launchAction = gameData -> {
      GameRunner.getGameSelectorModel().load(gameData, "");
      final ILauncher launcher = serverModel.getLauncher();

      launcher.launch(null);
      // TODO: new player mapping hardcoded!
      ((ServerLauncher) serverModel.getLauncher()).signalGameStart(gameData.toBytes());
    };


    replaceContents(stagingScreen.buildScreen(stagingScreen.previousScreen, data, chatSupport, launchAction,
        serverModel, null));
  }

  /**
   * Renders the first launch screen shown to a user.
   */
  public static void show() {
    synchronized (INSTANCE) {
      if (INSTANCE.frame == null) {
        INSTANCE.frame = new JFrame("TripleA - " + ClientContext.engineVersion().getExactVersion());
        INSTANCE.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        INSTANCE.frame.setIconImage(GameRunner.getGameIcon(INSTANCE.frame));
      }

      INSTANCE.frame.setSize(900, 700);
      INSTANCE.frame.setMinimumSize(new Dimension(400, 500));

      INSTANCE.frame.setLocationRelativeTo(null);
      INSTANCE.frame.setVisible(true);
      INSTANCE.frame.toFront();
    }
    draw(LaunchScreen.INITIAL);
  }

}
