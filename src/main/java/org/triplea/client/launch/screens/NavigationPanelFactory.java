package org.triplea.client.launch.screens;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.triplea.client.launch.screens.staging.GameStartedCallback;

import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

/**
 * Contains the common methods for creating the 'south' panel of the welcome screen that contains the
 * "Play" "Back" and "Quit" buttons. This panel is rendered on each of the launch screens, common rendering
 * logic is captured here.
 */
public class NavigationPanelFactory {

  /**
   * Build a panel with back button, and a 'play button' that would launch the game.
   */
  public static JPanel buildWithPlayButton(
      final LaunchScreen previousScreen,
      final Runnable playButtonAction,
      final GameStartedCallback gameStartedCallback) {
    final JButton playButton = JButtonBuilder.builder()
        .biggerFont()
        .title("Play")
        .enabled(true)
        .actionListener(() -> {
          playButtonAction.run();
          gameStartedCallback.gameIsStarted();
        })
        .build();

    return JPanelBuilder.builder()
        .borderEtched()
        .flowLayout()
        .add(playButton)
        .add(Box.createHorizontalStrut(50))
        .add(JButtonBuilder.builder()
            .title("Back")
            .enabled(true)
            .actionListener(() -> LaunchScreenWindow.draw(previousScreen))
            .build())
        .add(Box.createHorizontalStrut(50))
        .add(JButtonBuilder.builder()
            .title("Quit")
            .actionListener(LaunchScreenWindow::dispose)
            .build())
        .add(Box.createVerticalGlue())
        .build();
  }


  /**
   * Builds a panel with a disabled play button, a back button if there is a previous screen.
   */
  public static JPanel buildWithDisabledPlayButton(final LaunchScreen screenWindow) {
    final JButton playButton = JButtonBuilder.builder()
        .biggerFont()
        .title("Play")
        .enabled(false)
        .build();

    return JPanelBuilder.builder()
        .borderEtched()
        .flowLayout()
        .add(playButton)
        .add(Box.createHorizontalStrut(50))
        .addIf(
            screenWindow.getPreviousScreen().isPresent(),
            JButtonBuilder.builder()
                .title("Back")
                .enabled(screenWindow.getPreviousScreen().isPresent())
                .actionListener(() -> LaunchScreenWindow.draw(screenWindow.getPreviousScreenNotOptional()))
                .build())
        .add(Box.createHorizontalStrut(50))
        .add(JButtonBuilder.builder()
            .title("Quit")
            .actionListener(LaunchScreenWindow::dispose)
            .build())
        .add(Box.createVerticalGlue())
        .build();
  }

}
