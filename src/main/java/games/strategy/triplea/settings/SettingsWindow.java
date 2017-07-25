package games.strategy.triplea.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.border.Border;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.ui.SwingComponents;


/**
 * UI window with controls to update game settings and preferences, {@see ClientSettings}.
 * Settings are grouped by type, the window consists of a TabbedPane and in it we load
 * one tab per non-hidden {@code SettingType}.
 * All data needed to render the settings UI is pulled from the {@code ClientSettings} enum.
 */
class SettingsWindow {
  SettingsWindow() {
  }

  public static void main(final String[] args) {
    show();
  }

  static void show() {
    final JDialog dialog = new JDialog((JFrame) null, "Settings");
    dialog.setContentPane(createContents(() -> dialog.dispose()));
    dialog.setMinimumSize(new Dimension(300,250));
    dialog.pack();
    dialog.setVisible(true);
  }

  private static JComponent createContents(final Runnable closerListener) {
    final JTabbedPane tabbedPane = SwingComponents.newJTabbedPane(1000, 400);

    final List<SelectionComponent> selectionComponents = new ArrayList<>();
    Arrays.stream(SettingType.values())
        .filter(settingType -> settingType != SettingType.HIDDEN)
        .forEach(nonHiddenType -> {
          final List<ClientSettings> settings = Arrays.stream(ClientSettings.values())
              .filter(setting -> setting.type == nonHiddenType)
              .sorted(Comparator.comparing(a -> a.title))
              .collect(Collectors.toList());

          selectionComponents.addAll(
              settings.stream()
                  .map(setting -> setting.userInputComponent)
                  .collect(Collectors.toList()));

          tabbedPane.add(nonHiddenType.tabTitle, createSettingsTab(settings, closerListener));
        });
    return tabbedPane;
  }

  // TODO: move this off to SwingComponents
  private static class GridBagHelper {
    private final JComponent parent;
    private final int columns;
    private final GridBagConstraints constraints;

    private int elementCount = 0;

    public GridBagHelper(final JComponent parent, final int columns) {
      this.parent = parent;
      this.columns = columns;
      constraints = new GridBagConstraints();
    }

    public void addComponents(final JComponent ... children) {
      Preconditions.checkArgument(children.length > 0);
      for(final JComponent child : children) {

        final int x = elementCount % columns;
        final int y = elementCount / columns;

        constraints.gridx = x;
        constraints.gridy = y;

        constraints.ipadx = 3;
        constraints.ipady = 3;

        constraints.anchor = GridBagConstraints.WEST;
        parent.add(child, constraints);
        elementCount++;
      }
    }
  }

  private static JComponent createSettingsTab(final List<ClientSettings> settings, final Runnable closeListener) {
    settings.forEach(setting -> {
      Preconditions.checkNotNull(Strings.emptyToNull(setting.title));
      Preconditions.checkNotNull(setting.userInputComponent.getJComponent());
    });

    final JPanel contents = new JPanel(new GridBagLayout());

    final GridBagHelper grid = new GridBagHelper(contents,3);

    settings.forEach(setting -> {

      final JPanel panel = SwingComponents.newJPanelWithHorizontalBoxLayout();
      final JLabel label = new JLabel(setting.title);
      label.setAlignmentX(JComponent.LEFT_ALIGNMENT);
      label.setMaximumSize(new Dimension(200, 50));
      panel.add(label);
      //      panel.add(Box.createHorizontalStrut(5));

      final JComponent component = setting.userInputComponent.getJComponent();
//      component.setMaximumSize(new Dimension(200, 50));

      final JTextArea description = new JTextArea(setting.description, 2, 50);
      description.setMaximumSize(new Dimension(200, 50));
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
    outerPanel.add(SwingComponents.newJScrollPane(floatTopLeft(contents)), BorderLayout.CENTER);

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

      settings.forEach(setting -> {
        if (setting.userInputComponent.isValid() && !setting.userInputComponent.readValue().equals(setting.value())) {
          successMsg.append(String.format("%s was updated to: %s\n",
              setting.title, setting.userInputComponent.readValue()));
          setting.save(setting.userInputComponent.readValue());
        } else if (!setting.userInputComponent.isValid()) {
          failMsg.append(String.format("Could not set %s to %s, %s\n",
              setting.title, setting.userInputComponent.readValue(),
              setting.userInputComponent.validValueDescription()));
          setting.userInputComponent.setValue(setting.value());
        }
        setting.userInputComponent.clearError();
        ClientSettings.flush();
      });

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
        JOptionPane.showMessageDialog(outerPanel, success + "<br />" + fail, "Some changes were not saved",
            JOptionPane.WARNING_MESSAGE);
      }
    });

    buttonPanel.add(saveButton);
    buttonPanel.add(Box.createHorizontalStrut(20));
    final JButton cancelButton = SwingComponents.newJButton("Cancel", e -> closeListener.run());
    buttonPanel.add(cancelButton);

    buttonPanel.add(Box.createHorizontalStrut(30));
    final JButton restoreDefaultsButton =
        SwingComponents.newJButton("Restore Defaults", e -> {
          settings.forEach(ClientSettings::restoreToDefaultValue);
          ClientSettings.flush();
          settings.forEach(setting -> {
            setting.userInputComponent.setValue(setting.value());
            setting.userInputComponent.clearError();
          });
          JOptionPane.showMessageDialog(null,
              "All " + settings.get(0).type.tabTitle + " settings were restored to their default values.");
        });
    buttonPanel.add(restoreDefaultsButton);

    return outerPanel;
  }

  private static JPanel floatTopLeft(final JComponent component) {
    final JPanel floatLeft = SwingComponents.newJPanelWithHorizontalBoxLayout();
    floatLeft.add(component);
    floatLeft.add(Box.createHorizontalGlue());
    final JPanel floatTop = SwingComponents.newJPanelWithVerticalBoxLayout();
    floatTop.add(floatLeft);
    floatTop.add(Box.createVerticalGlue());
    return floatTop;
  }

  private static JPanel floatCenterLeft(final JComponent component) {
    final JPanel floatLeft = SwingComponents.newJPanelWithHorizontalBoxLayout();
    floatLeft.add(component);
    floatLeft.add(Box.createHorizontalGlue());
    final JPanel floatTop = SwingComponents.newJPanelWithVerticalBoxLayout();
    floatTop.add(Box.createVerticalGlue());
    floatTop.add(floatLeft);
    floatTop.add(Box.createVerticalGlue());
    return floatTop;
  }

}
