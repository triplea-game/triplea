package games.strategy.triplea.settings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.google.common.base.Preconditions;

import games.strategy.engine.framework.GameRunner;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;
import swinglib.JScrollPaneBuilder;
import swinglib.JTextAreaBuilder;

/**
 * UI window with controls to update game settings and preferences.
 * Settings are grouped by type, the window consists of a TabbedPane and in it we load
 * one tab per non-hidden {@code SettingType}.
 * All data needed to render the settings UI is pulled from the {@code ClientSetting} enum.
 *
 * @see ClientSetting
 */
@SuppressWarnings("ImmutableEnumChecker") // Enum singleton pattern
public enum SettingsWindow {
  INSTANCE;

  private @Nullable JDialog dialog;
  private @Nullable JTabbedPane tabbedPane;
  private final List<SettingType> settingTypes = Collections.unmodifiableList(Arrays.asList(SettingType.values()));


  public static void updateLookAndFeel() {
    Optional.ofNullable(INSTANCE.dialog).ifPresent(SwingUtilities::updateComponentTreeUI);
    Optional.ofNullable(INSTANCE.tabbedPane).ifPresent(SwingUtilities::updateComponentTreeUI);
  }

  /**
   * Disposes window and nulls out references.
   */
  public void close() {
    if (dialog != null) {
      dialog.dispose();
      dialog = null;
      Arrays.stream(ClientSettingSwingUiBinding.values())
          .forEach(ClientSettingSwingUiBinding::dispose);
    }
  }

  /**
   * Opens the settings Swing window.
   */
  public void open() {
    Preconditions.checkState(SwingUtilities.isEventDispatchThread());
    if (dialog == null) {
      dialog = GameRunner.newDialog("Settings");
      dialog.setContentPane(createContents());
      dialog.setMinimumSize(new Dimension(400, 50));
      dialog.pack();
      dialog.setLocationRelativeTo(dialog.getOwner());
      dialog.setVisible(true);
      SwingComponents.addWindowClosingListener(dialog, this::close);
      SwingComponents.addEscapeKeyListener(dialog, this::close);
    } else {
      // window is already visible, bring it to the front
      dialog.toFront();
    }
  }

  private JComponent createContents() {
    tabbedPane = SwingComponents.newJTabbedPane(1000, 400);
    settingTypes.forEach(settingType -> tabbedPane.add(settingType.tabTitle,
        buildTabPanel(getSettingsByType(settingType))));

    return JPanelBuilder.builder()
        .borderLayout()
        .borderEmpty(10)
        .addCenter(tabbedPane)
        .addSouth(buildButtonPanel())
        .build();
  }

  private static List<ClientSettingSwingUiBinding> getSettingsByType(final SettingType type) {
    return Arrays.stream(ClientSettingSwingUiBinding.values())
        .filter(setting -> setting.getType().equals(type))
        .collect(Collectors.toList());
  }

  private static JComponent buildTabPanel(final Iterable<ClientSettingSwingUiBinding> settings) {
    final JPanel panel = JPanelBuilder.builder()
        .borderEmpty(10)
        .build();
    panel.setLayout(new GridBagLayout());

    // Hack to ensure scroll panes have a border when using a Substance L&F. The default Substance scroll pane border is
    // invisible, which makes it difficult to visualize the different setting descriptions. Using the text field border
    // provides a satisfactory display. Most non-Substance L&Fs use a {@code null} default text field border, and so the
    // default scroll pane border in those cases is not changed.
    final Optional<Border> descriptionScrollPaneBorder = Optional.ofNullable(UIManager.getBorder("TextField.border"));

    int row = 0;
    for (final ClientSettingSwingUiBinding setting : settings) {
      final int topInset = (row == 0) ? 0 : 10;
      panel.add(
          JLabelBuilder.builder()
              .text(setting.getTitle())
              .build(),
          new GridBagConstraints(0, row, 1, 1, 0.0, 0.0,
              GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(topInset, 0, 0, 0), 0, 0));
      panel.add(
          setting.buildSelectionComponent(),
          new GridBagConstraints(1, row, 1, 1, 0.0, 0.0,
              GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(topInset, 10, 0, 0), 0, 0));
      panel.add(
          JScrollPaneBuilder.builder()
              .border(descriptionScrollPaneBorder)
              .view(JTextAreaBuilder.builder()
                  .text(setting.description)
                  .rows(2)
                  .readOnly()
                  .build())
              .build(),
          new GridBagConstraints(2, row, 1, 1, 1.0, 0.0,
              GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(topInset, 10, 0, 0), 0, 0));
      ++row;
    }

    // Use some glue having weighty != 0 and all other components having weighty == 0 to ensure all
    // setting components are pushed to the top of the panel rather than being centered
    panel.add(Box.createGlue(), new GridBagConstraints(0, row, 3, 1, 1.0, 1.0,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

    return SwingComponents.newJScrollPane(panel);
  }

  private JComponent buildButtonPanel() {
    return JPanelBuilder.builder()
        .borderEmpty(20, 0, 0, 0)
        .horizontalBoxLayout()
        .horizontalAlignmentCenter()
        .addHorizontalGlue()
        .add(JButtonBuilder.builder()
            .title("Save")
            .actionListener(this::saveSettings)
            .build())
        .addHorizontalStrut(5)
        .add(JButtonBuilder.builder()
            .title("Close")
            .actionListener(this::close)
            .build())
        .addHorizontalStrut(5)
        .add(JButtonBuilder.builder()
            .title("Reset")
            .actionListener(this::resetSettings)
            .build())
        .addHorizontalStrut(5)
        .add(JButtonBuilder.builder()
            .title("Reset to Default")
            .actionListener(this::resetSettingsToDefault)
            .build())
        .addHorizontalGlue()
        .build();
  }

  private void saveSettings() {
    getSelectedSettings().ifPresent(settings -> {
      final SaveFunction.SaveResult saveResult = SaveFunction.saveSettings(settings, ClientSetting::flush);
      JOptionPane.showMessageDialog(dialog, saveResult.message, "Results", saveResult.dialogType);
    });
  }

  private void resetSettings() {
    getSelectedSettings().ifPresent(settings -> settings.forEach(ClientSettingSwingUiBinding::reset));
  }

  private void resetSettingsToDefault() {
    getSelectedSettings().ifPresent(settings -> settings.forEach(ClientSettingSwingUiBinding::resetToDefault));
  }

  private Optional<List<ClientSettingSwingUiBinding>> getSelectedSettings() {
    assert tabbedPane != null;

    final int selectedTabIndex = tabbedPane.getSelectedIndex();
    return (selectedTabIndex != -1)
        ? Optional.of(getSettingsByType(settingTypes.get(selectedTabIndex)))
        : Optional.empty();
  }
}
