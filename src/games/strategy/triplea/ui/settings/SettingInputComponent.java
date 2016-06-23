package games.strategy.triplea.ui.settings;

import java.util.function.BiConsumer;

import javax.swing.text.JTextComponent;

public interface SettingInputComponent<T> {
  String getLabel();

  String getDescription();

  JTextComponent getInputElement();

  boolean updateSettings(T toUpdate, JTextComponent inputComponent);


  static <Z> SettingInputComponent<Z> build(final String label,
      final String description, JTextComponent component, BiConsumer<Z, String> updater) {
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
        return component;
      }

      @Override
      public boolean updateSettings(Z toUpdate, JTextComponent inputComponent) {
        System.out.println("Updating with: " + inputComponent.getText());
        updater.accept(toUpdate, inputComponent.getText());
        return true;
      }
    };
  }
}
