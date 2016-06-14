package games.strategy.triplea.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.ai.AiTab;
import games.strategy.triplea.settings.battle.calc.BattleCalcTab;
import games.strategy.triplea.settings.folders.FoldersTab;
import games.strategy.triplea.settings.scrolling.ScrollSettingsTab;
import games.strategy.ui.SwingComponents;

public class SettingsWindow extends JDialog {

  public static void showWindow() {
    List<SettingsTab> tabs = Arrays.asList(
        new ScrollSettingsTab(ClientContext.scrollSettings()),
        new FoldersTab(ClientContext.folderSettings()),
        new AiTab(ClientContext.aiSettings()),
        new BattleCalcTab(ClientContext.battleCalcSettings())
    );
    SwingComponents.showJFrame(new SettingsWindow(tabs));
  }

  private SettingsWindow(List<SettingsTab> tabs) {
    super((Frame) null, true);

    JTabbedPane pane = new JTabbedPane();
    add(pane, BorderLayout.CENTER);
    tabs.forEach(tab -> pane.addTab(tab.getTabTitle(), createTabWindow(tab)));

    add(SwingComponents.newJButton("Close", e -> dispose()), BorderLayout.SOUTH);
  }


  private Component createTabWindow(SettingsTab settingTab) {
    List<SettingInputComponent> inputs = settingTab.getInputs();

    // use a grid instead of a box layout, in the variations done, grid looks to be best about using 100% width available
    // If another layout can be found that uses 100% width by default and left aligns, it would likely be a better option.
    JPanel settingsPanel = SwingComponents.newJPanelWithGridLayout(inputs.size(), 1);
    inputs.forEach(input -> settingsPanel.add(createInputElementRow(input)));

    JPanel panel = new JPanel();
    panel.add(settingsPanel, BorderLayout.CENTER);
    panel.add(createButtonsPanel(settingTab));

    return new JScrollPane(panel);
  }

  private JPanel createButtonsPanel(SettingsTab settingTab) {
    JPanel buttonsPanel = new JPanel();
    buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));

    JButton useDefaults = SwingComponents.newJButton("To Default",
        e -> SwingComponents.promptUser("Revert to default?",
            "Are you sure you would like revert '" + settingTab.getTabTitle() + "' back to default settings?", () -> {
              settingTab.getSettingsObject().setToDefault();
              SystemPreferences.flush();
              dispose();
              SwingComponents.showDialog("Reverted the '" + settingTab.getTabTitle() + "' settings back to defaults");
            }));
    buttonsPanel.add(useDefaults);

    buttonsPanel.add(Box.createVerticalStrut(100));

    JButton saveButton = SwingComponents.newJButton("Save", e -> {
      settingTab.updateSettings(settingTab.getInputs());
      SystemPreferences.flush();
    });
    buttonsPanel.add(saveButton);
    return buttonsPanel;
  }

  private static JPanel createInputElementRow(SettingInputComponent input) {
    final JPanel rowContents = SwingComponents.newJPanelWithGridLayout(1, 2);
    rowContents.add(createTextAndInputPanel(input));
    rowContents.add(createInputDescription(input));
    return rowContents;
  }

  private static JPanel createTextAndInputPanel(SettingInputComponent input) {
    JPanel labelInputPanel = SwingComponents.newJPanelWithGridLayout(1, 2);
    JLabel label = new JLabel(input.getLabel());
        labelInputPanel.add(label);

    JPanel inputPanel = new JPanel();
    inputPanel.add(input.getInputElement());
    inputPanel.add(Box.createHorizontalGlue());

    labelInputPanel.add(inputPanel);
    return labelInputPanel;

  }
  private static JTextArea createInputDescription(SettingInputComponent input) {
    JTextArea description = new JTextArea(input.getDescription(), 2, 20);
    description.setEditable(false);
    return description;
  }

}
