package games.strategy.triplea.settings;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.text.JTextComponent;

/**
 * Wrapper API around a 'settings' object, allows for a GUI interface that allows a user to read descriptions about
 * each setting in the object, and to update the value.
 */
public interface SettingInputComponent<T> {
  String getLabel();

  String getDescription();

  JTextComponent getInputElement();

  boolean updateSettings(T toUpdate, JTextComponent inputComponent);

  String getValue(T settingsType);

  // TODO: updater should take the input component, so it can read it.
  static <Z> SettingInputComponent<Z> build(final String label, final String description, JTextComponent component,
      BiConsumer<Z, String> updater, Function<Z, String> extractor, InputValidator... validators) {

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
        String input = inputComponent.getText();

        for (InputValidator validator : Arrays.asList(validators)) {
          boolean isValid = validator.apply(input);

          if (!isValid) {
            return false;
          }
        }
        updater.accept(toUpdate, input);
        return true;
      }

      @Override
      public String getValue(Z settingsType) {
        return extractor.apply(settingsType);
      }
    };
  }
}
