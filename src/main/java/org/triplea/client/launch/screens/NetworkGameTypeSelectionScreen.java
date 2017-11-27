package org.triplea.client.launch.screens;

import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import games.strategy.engine.config.client.LobbyServerPropertiesFetcher;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

class NetworkGameTypeSelectionScreen {

  static JPanel build() {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain())
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.NETWORK_GAME_TYPE_SELECT))
        .build();
  }

  private static JPanel buildMain() {
    return JPanelBuilder.builder()
        .flowLayoutWrapper()
        .verticalBoxLayout()
        .add(Box.createVerticalStrut(40))
        .add(JButtonBuilder.builder()
            .biggerFont()
            .title("Play Online")
            .actionListener(openLobbyWindow())
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
  private static Runnable openLobbyWindow() {
    return () -> {
      final LobbyServerProperties lobbyServerProperties =
          new LobbyServerPropertiesFetcher().fetchLobbyServerProperties();
      final LobbyLogin login = new LobbyLogin(
          JOptionPane.getFrameForComponent(null),
          lobbyServerProperties);
      final LobbyClient client = login.login();
      if (client == null) {
        return;
      }
      final LobbyFrame lobbyFrame = new LobbyFrame(client, lobbyServerProperties);
      lobbyFrame.setVisible(true);
      LaunchScreenWindow.dispose();
    };
  }
}
