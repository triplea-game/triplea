package org.triplea.swing;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.JCheckBox;

/** Relatively simple builder for a swing JCheckBox. By default check boxes are 'selected'. */
public class JCheckBoxBuilder {

  private boolean isSelected = true;
  private SettingPersistence settingPersistence;
  private Consumer<Boolean> actionListener;
  private final String label;

  public JCheckBoxBuilder() {
    this("");
  }

  public JCheckBoxBuilder(final String label) {
    this.label = label;
  }

  /** Builds the swing component. */
  public JCheckBox build() {

    final JCheckBox checkBox = new JCheckBox(Strings.nullToEmpty(label));
    checkBox.setSelected(
        Optional.ofNullable(settingPersistence)
            .map(SettingPersistence::getSetting)
            .orElse(isSelected));
    checkBox.addActionListener(
        e -> {
          Optional.ofNullable(settingPersistence)
              .ifPresent(s -> s.saveSetting(checkBox.isSelected()));
          Optional.ofNullable(actionListener).ifPresent(l -> l.accept(checkBox.isSelected()));
        });
    return checkBox;
  }

  public JCheckBoxBuilder actionListener(final Consumer<Boolean> actionListener) {
    this.actionListener = actionListener;
    return this;
  }

  public JCheckBoxBuilder selected(final boolean isSelected) {
    this.isSelected = isSelected;
    return this;
  }

  public JCheckBoxBuilder bind(final SettingPersistence settingPersistence) {
    this.settingPersistence = settingPersistence;
    return this;
  }
}
