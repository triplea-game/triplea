package org.triplea.client.launch.screens;

import javax.swing.JPanel;

import games.strategy.triplea.settings.ClientSetting;
import swinglib.GridBagHelper;
import swinglib.JCheckBoxBuilder;
import swinglib.JComboBoxBuilder;
import swinglib.JPanelBuilder;
import swinglib.JTextFieldBuilder;

class PlayByEmailSetup {


  private static final int TEXT_FIELD_SIZE = 5;

  static JPanel build() {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain())
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.PLAY_BY_EMAIL_OPTIONS))
        .build();
  }

  private static JPanel buildMain() {
    final String disabled = "Disabled";

    return JPanelBuilder.builder()
        .borderTitled("Play By Email")
        .verticalBoxLayout()
        .add(DiceServerPanel.build(ClientSetting.DICE_SERVER_FOR_PBEM_GAMES))
        .add(
            JPanelBuilder.builder()
                .flowLayout()
                .borderTitled("Play by Email")
                .add(
                    JComboBoxBuilder.builder()

                        .menuOptions(disabled, "Gmail", "Generic SMTP")
                        .compositeBuilder()
                        .label("Provider:")
                        .onSelectionShowPanel("Gmail", JPanelBuilder.builder()
                            .gridBagLayout(2)
                            .addLabel("Subject")
                            .add(JTextFieldBuilder.builder()
                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .addLabel("To")
                            .add(JTextFieldBuilder.builder()
                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .addLabel("Login")
                            .add(JTextFieldBuilder.builder()

                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .addLabel("Password")
                            .add(JTextFieldBuilder.builder()

                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .build())

                        .onSelectionShowPanel("Generic SMTP", JPanelBuilder.builder()
                            .gridBagLayout(2)
                            .addLabel("Subject")
                            .add(JTextFieldBuilder.builder()

                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .addLabel("To")
                            .add(JTextFieldBuilder.builder()
                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .addLabel("Login")
                            .add(JTextFieldBuilder.builder()
                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .addLabel("Password")
                            .add(JTextFieldBuilder.builder()
                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .addLabel("Host")
                            .add(JTextFieldBuilder.builder()
                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .addLabel("Port")
                            .add(JTextFieldBuilder.builder()
                                .columns(TEXT_FIELD_SIZE)
                                .build())
                            .add(new JPanel())
                            .add(JPanelBuilder.builder()
                                .flowLayout()
                                .add(JCheckBoxBuilder.builder()
                                    .build())
                                .addLabel("Use TLS encryption")
                                .build(), GridBagHelper.ColumnSpan.of(2))
                            .build())
                        .build())
                .build())
        .addVerticalGlue()
        .build();
  }
}
