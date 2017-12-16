package org.triplea.client.launch.screens;

import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import games.strategy.engine.config.client.LobbyServerPropertiesFetcher;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

class NetworkGameTypeSelectionScreen {

  static JPanel build(final GameRunner gameRunner) {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain(gameRunner))
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.NETWORK_GAME_TYPE_SELECT))
        .build();
  }

  private static JPanel buildMain(final GameRunner gameRunner) {
    return JPanelBuilder.builder()
        .flowLayoutWrapper()
        .verticalBoxLayout()
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .biggerFont()
            .title("Play Online")
            .actionListener(openLobbyWindow(gameRunner))
            .build())
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .biggerFont()
            .title("Play By Email")
            .actionListener(() -> LaunchScreenWindow.draw(LaunchScreen.PLAY_BY_EMAIL_OPTIONS))
            .build())
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .biggerFont()
            .title("Play By Forum")
            .actionListener(() -> LaunchScreenWindow.draw(LaunchScreen.PLAY_BY_FORUM_OPTIONS))
            .build())
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Host Network Game")
            .actionListener(() -> LaunchScreenWindow.draw(LaunchScreen.HOST_NETWORK_GAME_OPTIONS))
            .build())
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Join Network Game")
            .actionListener(() -> LaunchScreenWindow.draw(LaunchScreen.JOIN_NETWORK_GAME_OPTIONS))
            .build())
        .add(Box.createVerticalGlue())
        .build();
  }


  /**
   * Show a popup for lobby login, then do login and show the {@code LobbyFrame}.
   */
  private static Runnable openLobbyWindow(final GameRunner gameRunner) {
    return () -> {
      final LobbyServerProperties lobbyServerProperties =
          new LobbyServerPropertiesFetcher().fetchLobbyServerProperties();
      final LobbyLogin login = new LobbyLogin(
          JOptionPane.getFrameForComponent(null),
          lobbyServerProperties,
          gameRunner);
      final LobbyClient client = login.login();
      if (client == null) {
        return;
      }
      final LobbyFrame lobbyFrame = new LobbyFrame(client, lobbyServerProperties, gameRunner);
      lobbyFrame.setVisible(true);
      LaunchScreenWindow.dispose();
    };
  }
}
