package org.triplea.client.launch.screens;

import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import swinglib.GridBagHelper;
import swinglib.JButtonBuilder;
import swinglib.JComboBoxBuilder;
import swinglib.JPanelBuilder;
import swinglib.JTextFieldBuilder;

class PlayByForumSetup {

  static JPanel build() {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(buildMain())
        .addSouth(NavigationPanelFactory.buildWithDisabledPlayButton(LaunchScreen.PLAY_BY_FORUM_OPTIONS))
        .build();
  }

  private static JPanel buildMain() {
    final JTextField topicIdField = JTextFieldBuilder.builder()
        .columns(15)
        .build();
    final JTextField loginField = JTextFieldBuilder.builder()
        .columns(30)
        .build();
    final JTextField passwordField = JTextFieldBuilder.builder()
        .columns(30)
        .build();

    final JButton viewForumButton = JButtonBuilder.builder()
        .title("View Forum")
        .enabled(false)
        .actionListener(
            () -> OpenFileUtility.openUrl(UrlConstants.TRIPLEA_FORUM.toString() + "/topic/" + topicIdField.getText()))
        .build();


    final JButton testPostButton = JButtonBuilder.builder()
        .title("Test Post")
        .enabled(false)
        .actionListener(() -> {
        })
        .build();

    final Supplier<Boolean> enableTestPostButton =
        () -> !topicIdField.getText().isEmpty() && !loginField.getText().isEmpty()
            && !passwordField.getText().isEmpty();


    JTextFieldBuilder.keyTypedListener(e -> testPostButton.setEnabled(enableTestPostButton.get()),
        topicIdField, loginField, passwordField);
    JTextFieldBuilder.keyTypedListener(e -> viewForumButton.setEnabled(!topicIdField.getText().isEmpty()),
        topicIdField);


    final String disabledSelection = "Disabled";
    final String forumUrl = "forums.triplea-game.org";
    final String axisAndAlliesForum = "AxisAndAllies.org";


    return JPanelBuilder
        .builder()
        .verticalBoxLayout()
        .addVerticalStrut(5)
        .add(DiceServerPanel.build(ClientSetting.DICE_SERVER_FOR_FORUM_GAMES))
        .addVerticalStrut(5)
        .add(JPanelBuilder.builder()
            .verticalBoxLayout()
            .borderTitled("Play By Forum")
            .add(
                JComboBoxBuilder.builder()
                    .menuOptions(disabledSelection, forumUrl, axisAndAlliesForum)
                    .useLastSelectionAsFutureDefault(ClientSetting.FORUM_COMBO_BOX_SELECTION)
                    .compositeBuilder()
                    .label("Forum:")
                    .onSelectionHidePanel(disabledSelection, JPanelBuilder.builder()
                        .gridBagLayout(3)
                        .addLabel("Topic Id:")
                        .add(topicIdField)
                        .add(viewForumButton)

                        .addLabel("Login:")
                        .add(loginField, GridBagHelper.ColumnSpan.of(2))

                        .addLabel("Password:")
                        .add(passwordField, GridBagHelper.ColumnSpan.of(2))

                        .add(testPostButton, GridBagHelper.ColumnSpan.of(3))
                        .build())
                    .build())
            .build())
        .addVerticalGlue()
        .build();
  }
}
