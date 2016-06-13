package games.strategy.triplea.ui.settings;

import games.strategy.triplea.ui.settings.scrolling.ScrollSettings;

import javax.swing.JComponent;
import javax.swing.text.JTextComponent;

public interface SettingInputComponent<T> {
  String getLabel();
  String getDescription();
  JTextComponent getInputElement();
  void updateSettings(ScrollSettings toUpdate, JTextComponent inputComponent);
}
