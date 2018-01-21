package org.triplea.client.launch.screens;

import javax.swing.Box;
import javax.swing.JPanel;

import org.triplea.client.launch.screens.staging.StagingScreen;

import games.strategy.engine.framework.ProcessRunnerUtil;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;
import tools.map.making.MapCreator;

class InitialLaunchScreen {

  private static final int VERTICAL_SPACING = 40;

  static JPanel build() {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain())
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.INITIAL))
        .build();
  }

  private static JPanel buildMain() {
    return JPanelBuilder.builder()
        .flowLayoutWrapper()
        .verticalBoxLayout()
        .horizontalAlignmentCenter()

        .add(Box.createVerticalStrut(VERTICAL_SPACING))
        .add(JButtonBuilder.builder()
            .title("Play Multi-Player")
            .biggerFont()
            .actionListener(() -> LaunchScreenWindow.draw(LaunchScreen.NETWORK_GAME_TYPE_SELECT))
            .build())
        .add(Box.createVerticalStrut(VERTICAL_SPACING))
        .add(JButtonBuilder.builder()
            .title("Play Single Player")
            .biggerFont()
            .actionListener(() -> LaunchScreenWindow.draw(StagingScreen.SINGLE_PLAYER))
            .build())
        .add(Box.createVerticalStrut(VERTICAL_SPACING))
        .add(JButtonBuilder.builder()
            .title("Game Rules & Help")
            .actionListener(() -> SwingComponents.newOpenUrlConfirmationDialog(UrlConstants.GITHUB_HELP))
            .build())
        .add(Box.createVerticalStrut(VERTICAL_SPACING))
        .flowLayoutWrapper()
        .add(JButtonBuilder.builder()
            .title("Settings")
            .actionListener(ClientSetting::showSettingsWindow)
            .build())
        .add(Box.createVerticalStrut(VERTICAL_SPACING))
        .add(JButtonBuilder.builder()
            .title("Map Tools")
            .actionListener(() -> ProcessRunnerUtil.runClass(MapCreator.class))
            .build())
        .add(Box.createVerticalGlue())
        .build();
  }
}
