package org.triplea.client.launch.screens;

import javax.swing.Box;
import javax.swing.JPanel;

import org.triplea.client.launch.screens.staging.StagingScreen;

import games.strategy.engine.framework.GameRunner;
import games.strategy.triplea.UrlConstants;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

class InitialLaunchScreen {

  static JPanel build(final GameRunner gameRunner) {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain(gameRunner))
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.INITIAL))
        .build();
  }

  private static JPanel buildMain(final GameRunner gameRunner) {
    return JPanelBuilder.builder()
        .flowLayoutWrapper()
        .verticalBoxLayout()
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Play Multi-Player")
            .biggerFont()
            .actionListener(() -> LaunchScreenWindow.draw(LaunchScreen.NETWORK_GAME_TYPE_SELECT))
            .build())
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Play Single Player")
            .biggerFont()
            .actionListener(() -> LaunchScreenWindow.draw(StagingScreen.SINGLE_PLAYER, gameRunner))
            .build())
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Game Rules & Help")
            .actionListener(() -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_HELP))
            .build())
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .title("More Options")
            .actionListener(() -> LaunchScreenWindow.draw(LaunchScreen.MORE_OPTIONS))
            .build())
        .add(Box.createVerticalGlue())
        .horizontalAlignmentCenter()
        .build();
  }
}
