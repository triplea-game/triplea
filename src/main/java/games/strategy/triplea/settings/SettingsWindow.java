package games.strategy.triplea.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.ai.AiTab;
import games.strategy.triplea.settings.battle.calc.BattleCalcTab;
import games.strategy.triplea.settings.battle.options.BattleOptionsTab;
import games.strategy.triplea.settings.folders.FoldersTab;
import games.strategy.triplea.settings.scrolling.ScrollSettingsTab;
import games.strategy.ui.SwingComponents;

/**
 * Window that contains a tabbed panel with preference categories, each tab contains fields that allow users to update
 * game settings. The window handles generic logic around preferences, each tab will specify configuration values for
 * the settings.
 *
 * Overall layout:
 * - Primary element is a JTabbed pain, the contents are organized into rows, one row per option presented to the user.
 * Each row consists of: label, swing input, detailed description
 * - Then we have some buttons:
 * - default (revert) settings
 * - save settings
 * - close window
 */
public class SettingsWindow extends SwingComponents.ModalJDialog {

  private static final long serialVersionUID = 8108714206041495198L;
  private static final int MAX_WIDTH = 1100;
  private static final int TEXT_LABEL_WIDTH = MAX_WIDTH / 4;
  private static final int ROW_HEIGHT = 60;

  /**
   * Shows the settings window. The window is modal (a user therefore cannot open multiple at a time)
   */
  public static void showWindow() {
    SwingComponents.showWindow(new SettingsWindow(
        new BattleOptionsTab(ClientContext.battleOptionsSettings()),
        new BattleCalcTab(ClientContext.battleCalcSettings()),
        new AiTab(ClientContext.aiSettings()),
        new ScrollSettingsTab(ClientContext.scrollSettings()),
        new FoldersTab(ClientContext.folderSettings())));
  }

  private SettingsWindow(final SettingsTab<?>... tabs) {
    add(buildTabbedPane(tabs), BorderLayout.CENTER);
  }

  private JTabbedPane buildTabbedPane(final SettingsTab<?>... tabs) {
    final JTabbedPane pane = SwingComponents.newJTabbedPane();
    Arrays.asList(tabs).forEach(tab -> pane.addTab(tab.getTabTitle(), createTabWindow(tab)));
    return pane;
  }

  private <T extends HasDefaults> Component createTabWindow(final SettingsTab<T> settingTab) {
    final List<SettingInputComponent<T>> inputs = settingTab.getInputs();

    final JPanel settingsPanel = SwingComponents.newJPanelWithVerticalBoxLayout();
    final int topOfWindowPadding = 20;
    settingsPanel.add(Box.createVerticalStrut(topOfWindowPadding));

    inputs.forEach(input -> {
      settingsPanel.add(createInputElementRow(input));

      final int paddingBetweenRows = 15;
      settingsPanel.add(Box.createVerticalStrut(paddingBetweenRows));
    });

    final JPanel panel = SwingComponents.newJPanelWithVerticalBoxLayout();
    panel.add(new JScrollPane(settingsPanel));
    panel.add(createButtonsPanel(settingTab));
    return panel;
  }

  private static JPanel createInputElementRow(final SettingInputComponent<?> input) {
    final JPanel contentRow = createContentRow(
        createTextAndInputPanel(input),
        createInputValueRangeDescription(input),
        createInputDescription(input));
    return SwingComponents.createRowWithTopAndBottomPadding(contentRow, 3, 5);
  }


  private static JPanel createContentRow(final JComponent textAndInputComponent,
      final JComponent valueRangeDescriptionComponent, final JComponent descriptionComponent) {
    final JPanel contentRow = SwingComponents.newJPanelWithHorizontalBoxLayout();
    contentRow.setMaximumSize(new Dimension(MAX_WIDTH, ROW_HEIGHT));

    final int leftHandPadding = 20;
    contentRow.add(Box.createHorizontalStrut(leftHandPadding));
    contentRow.add(textAndInputComponent);

    // the vertical struct gives the row height
    contentRow.add(Box.createVerticalStrut(ROW_HEIGHT));

    contentRow.add(valueRangeDescriptionComponent);
    contentRow.add(SwingComponents.newJScrollPane(descriptionComponent));
    contentRow.setBorder(new BevelBorder(BevelBorder.LOWERED));
    return contentRow;
  }


  private static JPanel createTextAndInputPanel(final SettingInputComponent<?> input) {
    final JPanel labelInputPanel = SwingComponents.newJPanelWithGridLayout(1, 2);
    final JLabel label = new JLabel(input.getLabel());
    labelInputPanel.add(label);

    final JPanel inputPanel = new JPanel();
    inputPanel.add(input.getInputElement().getSwingComponent());
    inputPanel.add(Box.createHorizontalGlue());

    inputPanel.setMinimumSize(new Dimension(TEXT_LABEL_WIDTH, ROW_HEIGHT));
    inputPanel.setPreferredSize(new Dimension(TEXT_LABEL_WIDTH, ROW_HEIGHT));
    inputPanel.setMaximumSize(new Dimension(TEXT_LABEL_WIDTH, ROW_HEIGHT));


    labelInputPanel.add(inputPanel);
    return labelInputPanel;

  }

  private static JComponent createInputValueRangeDescription(final SettingInputComponent<?> input) {
    return SwingComponents.newMultilineLabel(input.getValueRangeDescription(), 2, 10);
  }

  private static JTextArea createInputDescription(final SettingInputComponent<?> input) {
    final JTextArea description = new JTextArea(input.getDescription(), 2, 50);
    description.setLineWrap(true);
    description.setWrapStyleWord(true);
    description.setEditable(false);
    return description;
  }

  /**
   * Each element is arranged in a row, with glue in between every element.
   */
  private <T extends HasDefaults> JPanel createButtonsPanel(final SettingsTab<T> settingTab) {
    final JPanel buttonsPanel = SwingComponents.newJPanelWithHorizontalBoxLayout();

    // instead of glue, use one vertical strut to give the buttons panel a minimum height
    final int buttonPanelHeight = 50;
    buttonsPanel.add(Box.createVerticalStrut(buttonPanelHeight));

    buttonsPanel.add(SwingComponents.newJButton("Use Defaults",
        e -> SwingComponents.promptUser("Revert to default settings?",
            "Are you sure you would like revert '" + settingTab.getTabTitle() + "' back to default settings?", () -> {
              settingTab.getSettingsObject().setToDefault();
              SystemPreferences.flush();
              dispose();
              SwingComponents.showDialog("Defaults Restored",
                  "Reverted the '" + settingTab.getTabTitle() + "' settings back to defaults");
            })));

    buttonsPanel.add(Box.createHorizontalGlue());
    buttonsPanel.add(SwingComponents.newJButton("Save", e -> {
      settingTab.updateSettings(settingTab.getInputs());
      SystemPreferences.flush();
    }));

    buttonsPanel.add(Box.createHorizontalGlue());
    buttonsPanel.add(SwingComponents.newJButton("Close", e -> dispose()));

    buttonsPanel.add(Box.createHorizontalGlue());
    return buttonsPanel;
  }

}
