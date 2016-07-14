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

  private SettingsWindow(SettingsTab<?>... tabs) {
    add(buildTabbedPane(tabs), BorderLayout.CENTER);
  }

  private JTabbedPane buildTabbedPane(SettingsTab<?>... tabs) {
    JTabbedPane pane = SwingComponents.newJTabbedPane();
    Arrays.asList(tabs).forEach(tab -> pane.addTab(tab.getTabTitle(), createTabWindow(tab)));
    return pane;
  }

  private <T extends HasDefaults> Component createTabWindow(SettingsTab<T> settingTab) {
    List<SettingInputComponent<T>> inputs = settingTab.getInputs();

    JPanel settingsPanel = SwingComponents.newJPanelWithVerticalBoxLayout();
    int topOfWindowPadding = 20;
    settingsPanel.add(Box.createVerticalStrut(topOfWindowPadding));

    inputs.forEach(input -> {
      settingsPanel.add(createInputElementRow(input));

      int paddingBetweenRows = 15;
      settingsPanel.add(Box.createVerticalStrut(paddingBetweenRows));
    });

    JPanel panel = SwingComponents.newJPanelWithVerticalBoxLayout();
    panel.add(new JScrollPane(settingsPanel));
    panel.add(createButtonsPanel(settingTab));
    return panel;
  }

  private static JPanel createInputElementRow(SettingInputComponent<?> input) {
    JPanel contentRow = createContentRow(createTextAndInputPanel(input), createInputDescription(input));
    return SwingComponents.createRowWithTopAndBottomPadding(contentRow, 3, 5);
  }


  private static JPanel createContentRow(JComponent textAndInputComponent, JComponent descriptionComponent) {
    JPanel contentRow = SwingComponents.newJPanelWithHorizontalBoxLayout();
    contentRow.setMaximumSize(new Dimension(MAX_WIDTH, ROW_HEIGHT));

    int leftHandPadding = 20;
    contentRow.add(Box.createHorizontalStrut(leftHandPadding));
    contentRow.add(textAndInputComponent);

    // the vertical struct gives the row height
    contentRow.add(Box.createVerticalStrut(ROW_HEIGHT));

    contentRow.add(descriptionComponent);
    contentRow.setBorder(new BevelBorder(BevelBorder.LOWERED));
    return contentRow;
  }


  private static JPanel createTextAndInputPanel(SettingInputComponent<?> input) {
    JPanel labelInputPanel = SwingComponents.newJPanelWithGridLayout(1, 2);
    JLabel label = new JLabel(input.getLabel());
    labelInputPanel.add(label);

    JPanel inputPanel = new JPanel();
    inputPanel.add(input.getInputElement().getSwingComponent());
    inputPanel.add(Box.createHorizontalGlue());

    inputPanel.setMinimumSize(new Dimension(TEXT_LABEL_WIDTH, ROW_HEIGHT));
    inputPanel.setPreferredSize(new Dimension(TEXT_LABEL_WIDTH, ROW_HEIGHT));
    inputPanel.setMaximumSize(new Dimension(TEXT_LABEL_WIDTH, ROW_HEIGHT));


    labelInputPanel.add(inputPanel);
    return labelInputPanel;

  }

  private static JTextArea createInputDescription(SettingInputComponent<?> input) {
    JTextArea description = new JTextArea(input.getDescription(), 2, 50);

    // TODO: JTextArea.setLineWrap(boolean) does not wrap on word boundaries, instead it'll split words across lines
    description.setLineWrap(true);
    description.setEditable(false);
    return description;
  }

  /**
   * Each element is arranged in a row, with glue in between every element
   */
  private <T extends HasDefaults> JPanel createButtonsPanel(SettingsTab<T> settingTab) {
    JPanel buttonsPanel = SwingComponents.newJPanelWithHorizontalBoxLayout();

    // instead of glue, use one vertical strut to give the buttons panel a minimum height
    int buttonPanelHeight = 50;
    buttonsPanel.add(Box.createVerticalStrut(buttonPanelHeight));

    buttonsPanel.add(SwingComponents.newJButton("Use Defaults",
        e -> SwingComponents.promptUser("Revert to default settings?",
            "Are you sure you would like revert '" + settingTab.getTabTitle() + "' back to default settings?", () -> {
              settingTab.getSettingsObject().setToDefault();
              SystemPreferences.flush();
              dispose();
              SwingComponents.showDialog("Reverted the '" + settingTab.getTabTitle() + "' settings back to defaults");
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
