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
 * Before joining a network game we need to specify which host we want to connect to and port.
 * Ideally we will remember last options chosen and autocomplete or present those options in a drop-down.
 * Once all options are selected the next screen will attempt to connect.
 */
public class JoinNetworkGameSetup {

  /**
   * Builder method for the panel with network game options.
   */
  public static JPanel build() {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain())
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.JOIN_NETWORK_GAME_OPTIONS))
        .build();
  }

  private static JPanel buildMain() {
    final JTextField playerNameField = JTextFieldBuilder.builder()
        .text(ClientSetting.PLAYER_NAME.value())
        .columns(12)
        .build();
    final JTextField serverField = JTextFieldBuilder.builder()
        .text("") // TODO: save and store last used, also add a drop down box of
        // successfully used hosts
        .columns(12)
        .build();
    final JTextField portField = JTextFieldBuilder.builder()
        .text(GameRunner.PORT)
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
                    new JLabel("Client Options"))
                .addEach(
                    new JLabel("Name"),
                    playerNameField)
                .addEach(
                    new JLabel("Server Address"),
                    serverField)
                .addEach(
                    new JLabel("Server Port"),
                    portField)
                .addEach(
                    new JLabel(""),
                    JButtonBuilder.builder()
                        .title("Connect")
                        .actionListener(() -> {
                          final ServerConnectionProps connectionProps = new ServerConnectionProps();
                          connectionProps.setHost(serverField.getText().trim());
                          connectionProps.setName("name");
                          connectionProps.setPassword(""); // TODO: << hardcoded
                          connectionProps.setPort(Integer.parseInt(portField.getText())); // TODO: << int parsing

                          LaunchScreenWindow.drawWithClientNetworking(
                              StagingScreen.NETWORK_CLIENT,
                              connectionProps);
                        })
                        .build())
                .build())
        .build();
  }
}
