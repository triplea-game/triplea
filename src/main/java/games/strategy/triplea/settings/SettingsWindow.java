package games.strategy.triplea.settings;

import java.awt.Dimension;
import java.awt.Frame;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import com.google.common.base.Preconditions;

import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;
import swinglib.JScrollPaneBuilder;
import swinglib.JTextAreaBuilder;

/**
 * UI window with controls to update game settings and preferences, {@see ClientSetting}.
 * Settings are grouped by type, the window consists of a TabbedPane and in it we load
 * one tab per non-hidden {@code SettingType}.
 * All data needed to render the settings UI is pulled from the {@code ClientSetting} enum.
 */
enum SettingsWindow {
  INSTANCE;

  private JDialog dialog;

  public void close() {
    if (dialog != null) {
      dialog.dispose();
      dialog = null;
      Arrays.stream(ClientSettingSwingUiBinding.values())
          .forEach(ClientSettingSwingUiBinding::dispose);
    }
  }

  public void open() {
    Preconditions.checkState(SwingUtilities.isEventDispatchThread());
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

  private static JComponent createContents(final Runnable closeListener) {
    final JTabbedPane tabbedPane = SwingComponents.newJTabbedPane(1000, 400);

    Arrays.stream(SettingType.values()).forEach(settingType -> {
      final List<ClientSettingSwingUiBinding> settings = getSettingsByType(settingType);

      final JComponent tab = buildTab(settings, closeListener);
      tabbedPane.add(settingType.tabTitle, tab);
    });
    return tabbedPane;
  }

  private static List<ClientSettingSwingUiBinding> getSettingsByType(final SettingType type) {
    return Arrays.stream(ClientSettingSwingUiBinding.values())
        .filter(setting -> setting.type == type)
        .collect(Collectors.toList());
  }

  private static JComponent buildTab(final List<ClientSettingSwingUiBinding> settings, final Runnable closeListener) {
    return JPanelBuilder.builder()
        .borderLayout()
        .addCenter(tabMainContents(settings))
        .addSouth(buttonPanel(settings, closeListener))
        .build();
  }

  private static JComponent tabMainContents(final Iterable<ClientSettingSwingUiBinding> settings) {
    final JPanelBuilder contents = JPanelBuilder.builder()
        .gridBagLayout(3);

    // Add settings, one per row, columns of 3:
    // setting title (JLabel) | input component (eg: radio buttons) | description (JTextArea)}

    settings.forEach(setting -> {
      contents.add(JPanelBuilder.builder()
          .horizontalBoxLayout()
          .add(
              JLabelBuilder.builder()
                  .text(setting.title)
                  .leftAlign()
                  .maximumSize(200, 50)
                  .build())
          .build());

      contents.add(setting.buildSelectionComponent());

      contents.add(JScrollPaneBuilder.builder()
          .view(JTextAreaBuilder.builder()
              .text(setting.description)
              .rows(2)
              .columns(40)
              .readOnly()
              .build())
          .build());
    });
    return SwingComponents.newJScrollPane(contents.build());
  }

  private static JPanel buttonPanel(final List<ClientSettingSwingUiBinding> settings, final Runnable closeListener) {
    return JPanelBuilder.builder()
        .horizontalBoxLayout()
        .horizontalAlignmentCenter()
        .add(Box.createHorizontalGlue())
        .add(JButtonBuilder.builder()
            .title("Save")
            .actionListener(() -> {
              final SaveFunction.SaveResult saveResult = SaveFunction.saveSettings(settings, ClientSetting::flush);
              JOptionPane.showMessageDialog(null, saveResult.message, "Results", saveResult.dialogType);
            })
            .build())
        .add(Box.createHorizontalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Close")
            .actionListener(closeListener)
            .build())
        .add(Box.createHorizontalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Reset")
            .actionListener(() -> settings.forEach(
                ClientSettingSwingUiBinding::reset))
            .build())
        .add(Box.createHorizontalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Reset To Default")
            .actionListener(() -> settings.forEach(
                ClientSettingSwingUiBinding::resetToDefault))
            .build())
        .add(Box.createHorizontalGlue())
        .build();
  }
}
