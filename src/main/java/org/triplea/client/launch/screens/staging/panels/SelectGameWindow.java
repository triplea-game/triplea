package org.triplea.client.launch.screens.staging.panels;

import javax.swing.JOptionPane;

import org.triplea.client.launch.screens.LaunchScreen;
import org.triplea.client.launch.screens.LaunchScreenWindow;
import org.triplea.client.launch.screens.staging.StagingScreen;

import games.strategy.engine.data.GameParseException;
import games.strategy.engine.framework.ui.GameChooser;
import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.triplea.settings.ClientSetting;

/**
 * Opens a dialog window where users can select from the available games they have installed.
 * Maps contain multiple XML files, each is considered a game, those games are listed in this window.
 */
public class SelectGameWindow {

  /**
   * Returns a Runnable that can be attached to a Swing button,
   * the Runnable will open the select map window.
   */
  public static Runnable openSelectGameWindow(final LaunchScreen previousScreen, final StagingScreen stagingScreen) {
    return () -> {
      try {
        final GameChooserEntry entry;
        entry = GameChooser.chooseGame(
            JOptionPane.getFrameForComponent(null),
            ClientSetting.SELECTED_GAME_LOCATION.value());
        if (entry != null) {
          ClientSetting.SELECTED_GAME_LOCATION.save(entry.getLocation());
          try {
            LaunchScreenWindow.draw(previousScreen, stagingScreen, entry.fullyParseGameData());
          } catch (final GameParseException e) {
            throw new IllegalStateException(String.format("Could not parse: %s", entry.toString()), e);
          }
        }
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    };
  }
}
