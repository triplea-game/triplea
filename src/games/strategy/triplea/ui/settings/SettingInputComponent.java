package games.strategy.triplea.ui.settings;

import games.strategy.triplea.ui.settings.scrolling.ScrollSettings;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.util.function.BiConsumer;

public interface SettingInputComponent<T> {
  String getLabel();

  String getDescription();

  JTextComponent getInputElement();

  void updateSettings(T toUpdate, JTextComponent inputComponent);


  static <Z> SettingInputComponent<Z> build(final String label,
      final String description, int initialValue, BiConsumer<Z, String> updater) {
    return build(label, description, String.valueOf(initialValue), updater);
  }

  static <Z> SettingInputComponent<Z> build(final String label,
      final String description, String initialValue, BiConsumer<Z, String> updater) {
    return new SettingInputComponent<Z>() {
      @Override
      public String getLabel() {
        return label;
      }

      @Override
      public String getDescription() {
        return description;
      }

      @Override
      public JTextComponent getInputElement() {
        return new JTextField(initialValue, 10);
      }


      @Override
      public void updateSettings(Z toUpdate, JTextComponent inputComponent) {
        updater.accept(toUpdate, inputComponent.getText());
      }
    };
  }
}
