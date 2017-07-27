package games.strategy.triplea.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.ui.SwingComponents;


/**
 * UI window with controls to update game settings and preferences, {@see ClientSetting}.
 * Settings are grouped by type, the window consists of a TabbedPane and in it we load
 * one tab per non-hidden {@code SettingType}.
 * All data needed to render the settings UI is pulled from the {@code ClientSetting} enum.
 */
enum SettingsWindow {
  INSTANCE;

  private JDialog dialog;

  SettingsWindow() {
  }

  public synchronized void close() {
    if (dialog != null) {
      dialog.dispose();
      dialog = null;
    }
  }

  public synchronized void open() {
    if (dialog == null) {
      dialog = new JDialog((Frame) null, "Settings");
      dialog.setContentPane(createContents(this::close));
      dialog.setMinimumSize(new Dimension(400, 50));
      dialog.pack();
      dialog.setVisible(true);
      SwingComponents.addWindowClosingListener(dialog, this::close);
      SwingComponents.addEscapeKeyListener(dialog, this::close);
    } else {
      // window is already visible, bring it to the front
      dialog.toFront();
    }
  }

  private static JComponent createContents(final Runnable closerListener) {
    final JTabbedPane tabbedPane = SwingComponents.newJTabbedPane(1000, 400);

    Arrays.stream(SettingType.values()).forEach(settingType ->
        tabbedPane.add(settingType.tabTitle, createSettingsTab(settingType, closerListener)));
    return tabbedPane;
  }

  private static JComponent createSettingsTab(final SettingType settingType, final Runnable closeListener) {
    final List<ClientSettingUiBinding> settings = Arrays.stream(ClientSettingUiBinding.values())
        .filter(setting -> setting.type == settingType)
        .collect(Collectors.toList());

    settings.forEach(setting -> {
      Preconditions.checkNotNull(Strings.emptyToNull(setting.title));
      Preconditions.checkNotNull(setting.selectionComponent.getJComponent());
    });

    final JPanel contents = new JPanel(new GridBagLayout());

    final SwingComponents.GridBagHelper grid = new SwingComponents.GridBagHelper(contents,3);

    settings.forEach(setting -> {

      final JPanel panel = SwingComponents.newJPanelWithHorizontalBoxLayout();
      final JLabel label = new JLabel(setting.title);
      label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      label.setMaximumSize(new Dimension(200, 50));
      panel.add(label);

      final JComponent component = setting.selectionComponent.getJComponent();

      final JTextArea description = new JTextArea(setting.description, 2, 40);
      description.setMaximumSize(new Dimension(120, 50));
      description.setEditable(false);
      description.setWrapStyleWord(true);
      description.setLineWrap(true);
      description.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

      // this 'borderPanel' will create some extra padding around the description text area
      final JPanel borderPanel = new JPanel();
      borderPanel.add(description);
      grid.addComponents(panel, component, borderPanel);
    });

    final JPanel outerPanel = new JPanel();
    outerPanel.setLayout(new BorderLayout());
    outerPanel.add(SwingComponents.newJScrollPane(contents), BorderLayout.CENTER);

    final JPanel bottomPanel = SwingComponents.newJPanelWithHorizontalBoxLayout();
    outerPanel.add(bottomPanel, BorderLayout.SOUTH);


    bottomPanel.add(Box.createHorizontalGlue());

    final JPanel buttonPanel = SwingComponents.newJPanelWithHorizontalBoxLayout();
    buttonPanel.setAlignmentX(JComponent.CENTER_ALIGNMENT);
    bottomPanel.add(buttonPanel);
    bottomPanel.add(Box.createHorizontalGlue());

    final JButton saveButton = SwingComponents.newJButton("Save", e -> {
      final StringBuilder successMsg = new StringBuilder();
      final StringBuilder failMsg = new StringBuilder();

      // save all the values, save stuff that is valid and that was updated
      settings.forEach(setting -> {
        if (setting.isValid()) {
          // read and save all settings
          setting.readValues().forEach((settingKey,settingValue) -> {
            if (!settingKey.value().equals(settingValue)) {
              settingKey.save(settingValue);
              successMsg.append(String.format("%s was updated to: %s\n", setting.title, settingValue));
            }
          });
          ClientSetting.flush();
        } else if (!setting.isValid()) {
          final Map<ClientSetting, String> values = setting.readValues();
          values.forEach((entry, value) -> {
            failMsg.append(String.format("Could not set %s to %s, %s\n",
                setting.title, value, setting.validValueDescription()));
          });
        }
      });
      ClientSetting.flush();

      final String success = successMsg.toString();
      final String fail = failMsg.toString();
      if (success.isEmpty() && fail.isEmpty()) {
        JOptionPane.showMessageDialog(outerPanel, "No changes to save", "No changes saved",
            JOptionPane.WARNING_MESSAGE);
      } else if (fail.isEmpty()) {
        JOptionPane.showMessageDialog(outerPanel, success, "Success", JOptionPane.INFORMATION_MESSAGE);
      } else if (success.isEmpty()) {
        JOptionPane.showMessageDialog(outerPanel, fail, "No changes saved", JOptionPane.WARNING_MESSAGE);
      } else {
        JOptionPane.showMessageDialog(outerPanel, success + "\n" + fail, "Some changes were not saved",
            JOptionPane.WARNING_MESSAGE);
      }
    });

    buttonPanel.add(saveButton);

    buttonPanel.add(Box.createHorizontalStrut(20));

    buttonPanel.add(SwingComponents.newJButton("Close", event -> {
      closeListener.run();
    }));

    buttonPanel.add(Box.createHorizontalStrut(20));

    final JButton cancelButton = SwingComponents.newJButton("Cancel", e -> closeListener.run());
    buttonPanel.add(cancelButton);

    buttonPanel.add(Box.createHorizontalStrut(30));
    final JButton restoreDefaultsButton =
        SwingComponents.newJButton("Restore Defaults", e -> {
          settings.forEach(ClientSettingUiBinding::resetToDefault);
          ClientSetting.flush();
          JOptionPane.showMessageDialog(null,
              "All " + settings.get(0).type.tabTitle + " settings were restored to their default values.");
        });
    buttonPanel.add(restoreDefaultsButton);

    return outerPanel;
  }

}
