package games.strategy.triplea.ui.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.ui.settings.folders.FoldersTab;
import games.strategy.triplea.ui.settings.scrolling.ScrollSettingsTab;
import games.strategy.ui.SwingComponents;

public class SettingsWindow extends JFrame {

  public static void showWindow() {
    List<SettingsTab> tabs = Arrays.asList(
        new ScrollSettingsTab(ClientContext.scrollSettings()),
        new FoldersTab(ClientContext.folderSettings())

    );
    SwingComponents.showJFrame(new SettingsWindow(tabs));
  }

  private SettingsWindow(List<SettingsTab> tabs) {

    JTabbedPane pane = new JTabbedPane();
    add(pane, BorderLayout.CENTER);
    tabs.forEach(tab -> pane.addTab(tab.getTabTitle(), createTabWindow(tab)));

    add(SwingComponents.newJButton("Close", e -> dispose()), BorderLayout.SOUTH);
  }


  private Component createTabWindow(SettingsTab settingTab) {
    List<SettingInputComponent> inputs = settingTab.getInputs();


    final JPanel windowContents = new JPanel();
    windowContents.setLayout(new GridLayout(inputs.size(),1));
    windowContents.setAlignmentX(JComponent.LEFT_ALIGNMENT);


    inputs.forEach(input -> {
      final JPanel rowContents = new JPanel();
      rowContents.setLayout(new GridLayout(1,2));

      final JPanel labelInputPanel = new JPanel();
      labelInputPanel.setLayout(new GridLayout(1,2));
      rowContents.add(labelInputPanel);

      JLabel label = new JLabel(input.getLabel());
      labelInputPanel.add(label);

      JPanel inputPanel = new JPanel();
      inputPanel.add(input.getInputElement());
      inputPanel.add(Box.createHorizontalGlue());

      labelInputPanel.add(inputPanel);

      JTextArea description = new JTextArea(input.getDescription(), 2, 20);
      description.setEditable(false);
      rowContents.add(description);

      windowContents.add(rowContents);
    });

    JPanel panel = new JPanel();
    panel.add(windowContents,BorderLayout.CENTER);

    JButton saveButton = SwingComponents.newJButton("Save", e -> {
       settingTab.updateSettings(inputs);
    });
    panel.add(saveButton, BorderLayout.SOUTH);

    return new JScrollPane(panel);
  }

  public static void main(String[] args) {
    SettingsWindow.showWindow();
  }


}
