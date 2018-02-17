package org.triplea.client.launch.screens;

import javax.swing.Box;
import javax.swing.JComponent;

import games.strategy.triplea.settings.ClientSetting;
import swinglib.GridBagHelper;
import swinglib.JButtonBuilder;
import swinglib.JComboBoxBuilder;
import swinglib.JPanelBuilder;
import swinglib.JTextFieldBuilder;

/**
 * Panel that shows options for selection a dice server.
 */
public class DiceServerPanel {

  /**
   * Builder method.
   */
  public static JComponent build(final ClientSetting backingSetting) {
    // TODO: autoset a 'from' field with a current player label

    final JComponent toField = JTextFieldBuilder.builder()
        .columns(30)
        .build();

    // TODO: drop down of known 'to' email addresses
    // TODO: default value for 'to'
    // TODO: default value for which dice server is in use

    return JPanelBuilder.builder()
        .verticalBoxLayout()
        .borderTitled("Dice Server")
        .add(JComboBoxBuilder.builder()
            .menuOptions("triplea.warclub.org", "Disabled")
            .useLastSelectionAsFutureDefault(backingSetting)
            .compositeBuilder()
            .label("Use Dice Server:")
            .onSelectionHidePanel(
                "Disabled",
                JPanelBuilder.builder()
                    .gridBagLayout(2)
                    .addLabel("Your Email")
                    .add(JTextFieldBuilder.builder()
                        .text("todo")
                        .enabled(false)
                        .build()) // TODO: use registered email
                    .addLabel("To:")
                    .add(toField)

                    .add(JPanelBuilder.builder()
                        .flowLayout()
                        .add(JButtonBuilder.builder()
                            .title("Test Server")
                            .actionListener(() -> {
                            })
                            .build())
                        .add(JButtonBuilder.builder()
                            .title("Options")
                            .actionListener(() -> {
                            }) // TODO
                            .build())
                        .add(JButtonBuilder.builder()
                            .title("Help")
                            .actionListener(() -> {
                            }) // TODO
                            .build())
                        .build(), GridBagHelper.ColumnSpan.of(2))
                    .build())
            .build())
        .add(Box.createVerticalGlue())
        .build();
  }
}

