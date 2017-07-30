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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import games.strategy.ui.SwingComponents;
import swinglib.GridBagHelper;
import swinglib.JButtonBuilder;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;
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

  private static JComponent createContents(final Runnable closeListener) {
    final JTabbedPane tabbedPane = SwingComponents.newJTabbedPane(1000, 400);

    Arrays.stream(SettingType.values()).forEach(settingType -> {
      final List<ClientSettingUiBinding> settings = getSettingsByType(settingType);
      verifySettings(settings);

      final JComponent tab = buildTab(settings, closeListener);
      tabbedPane.add(settingType.tabTitle, tab);
    });
    return tabbedPane;
  }

  private static List<ClientSettingUiBinding> getSettingsByType(final SettingType type) {
    return Arrays.stream(ClientSettingUiBinding.values())
        .filter(setting -> setting.type == type)
        .collect(Collectors.toList());
  }

  private static void verifySettings(final Iterable<ClientSettingUiBinding> settings) {
    // do some basic integrity/data validity check.
    settings.forEach(setting -> {
      Preconditions.checkNotNull(Strings.emptyToNull(setting.title));
      Preconditions.checkNotNull(setting.selectionComponent.getJComponent());
    });
  }

  private static JComponent buildTab(final List<ClientSettingUiBinding> settings, final Runnable closeListener) {
    return JPanelBuilder.builder()
        .addCenter(tabMainContents(settings))
        .addSouth(buttonPanel(settings, closeListener))
        .build();
  }

  private static JComponent tabMainContents(final Iterable<ClientSettingUiBinding> settings) {
    final JPanel contents = JPanelBuilder.builder()
        .gridBagLayout()
        .build();

    final GridBagHelper grid = new GridBagHelper(contents, 4);

    // Add settings, one per row, columns of 3:
    // setting title (JLabel) | input component (eg: radio buttons) | description (JTextArea)}

    settings.forEach(setting -> {
      grid.add(JPanelBuilder.builder()
          .horizontalBoxLayout()
          .add(
              JLabelBuilder.builder()
                  .text(setting.title)
                  .leftAlign()
                  .maximumSize(200, 50)
                  .build())
          .build());

      grid.add(setting.selectionComponent.getJComponent());

      grid.add(JPanelBuilder.builder()
          .add(
              JTextAreaBuilder.builder()
                  .text(setting.description)
                  .rows(2)
                  .columns(40)
                  .maximumSize(60, 50)
                  .readOnly()
                  .borderWidth(1)
                  .build())
          .build());

      grid.add(JButtonBuilder.builder()
          .title("Reset to Default")
          .actionListener(setting::resetToDefault)
          .build());
    });
    return SwingComponents.newJScrollPane(contents);
  }

  private static JPanel buttonPanel(final List<ClientSettingUiBinding> settings, final Runnable closeListener) {
    return JPanelBuilder.builder()
        .horizontalBoxLayout()
        .horizontalAlignmentCenter()
        .add(Box.createHorizontalGlue())
        .add(JButtonBuilder.builder()
            .title("Save")
            .actionListener(() -> {
              SaveFunction.SaveResult saveResult = SaveFunction.saveSettings(settings, ClientSetting::flush);
              JOptionPane.showMessageDialog(null, saveResult.message, "Results", saveResult.dialogType);
            })
            .build())
        .add(Box.createHorizontalStrut(40))
        .add(JButtonBuilder.builder()
            .title("Close")
            .actionListener(closeListener)
            .build())
        .add(Box.createHorizontalGlue())
        .build();
  }
}
