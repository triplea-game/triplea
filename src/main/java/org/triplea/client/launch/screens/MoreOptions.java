package org.triplea.client.launch.screens;

import javax.swing.Box;
import javax.swing.JPanel;

import games.strategy.engine.framework.ProcessRunnerUtil;
import games.strategy.triplea.settings.ClientSetting;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;
import tools.map.making.MapCreator;

class MoreOptions {

  static JPanel build() {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain())
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.MORE_OPTIONS))
        .build();
  }

  private static JPanel buildMain() {
    return JPanelBuilder.builder()
        .flowLayoutWrapper()
        .verticalBoxLayout()
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Settings")
            .actionListener(ClientSetting::showSettingsWindow)
            .build())
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Map Tools")
            .actionListener(() -> ProcessRunnerUtil.runClass(MapCreator.class))
            .build())
        .add(Box.createVerticalGlue())
        .build();
  }
}
