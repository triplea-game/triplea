package org.triplea.client.launch.screens;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.triplea.client.launch.screens.staging.StagingScreen;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.startup.mc.ServerConnectionProps;
import games.strategy.triplea.settings.ClientSetting;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;
import swinglib.JTextFieldBuilder;

/**
 * Screen for configuring local game hosting options before opening the final staging screen.
 */
class HostNetworkGameSetup {

  static JPanel build() {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain())
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.HOST_NETWORK_GAME_OPTIONS))
        .build();
  }

  private static JPanel buildMain() {
    final JTextField playerNameField = JTextFieldBuilder.builder()
        .text(ClientSetting.PLAYER_NAME.value())
        .columns(12)
        .build();

    final JTextField portField = JTextFieldBuilder.builder()
        .text(GameRunner.PORT)
        .columns(5)
        .build();

    final JTextField passwordField = JTextFieldBuilder.builder()
        // TODO: password text
        .columns(5)
        .build();



    return JPanelBuilder.builder()
        .verticalBoxLayout()
        .add(Box.createVerticalStrut(30))
        .add(
            JPanelBuilder.builder()
                .gridBagLayout(2)
                .addEach(
                    new JLabel(""),
                    new JLabel("Server Options"))
                .addEach(
                    new JLabel("Player Name"),
                    playerNameField)
                .addEach(
                    new JLabel("Port"),
                    portField)
                .addEach(
                    new JLabel("Use Password"),
                    passwordField)
                .addEach(
                    new JLabel(""),
                    JButtonBuilder.builder()
                        .title("Host")
                        .actionListener(() -> {
                          final ServerConnectionProps props = new ServerConnectionProps();
                          props.setName(playerNameField.getText().trim());
                          props.setPassword(passwordField.getText().trim());
                          props.setPort(extractPortValue(portField));
                          LaunchScreenWindow.drawWithServerNetworking(StagingScreen.NETWORK_HOST, props);
                        })
                        .build())
                .build())
        .build();
  }

  // TODO: build a new class that has this functionality built in, provide a builder for it so the API
  // looks like any other swing component
  private static int extractPortValue(final JTextField value) {
    try {
      return Integer.parseInt(value.getText());
    } catch (final NumberFormatException e) {
      value.setText("3300");
      // TODO: extract to constant
      return 3300;
    }
  }
}
