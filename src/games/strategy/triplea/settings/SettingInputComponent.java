package games.strategy.triplea.settings;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.text.JTextComponent;

import games.strategy.triplea.settings.validators.InputValidator;

public interface SettingInputComponent<T> {
  String getLabel();

  String getDescription();

  JTextComponent getInputElement();

  boolean updateSettings(T toUpdate, JTextComponent inputComponent);

  List<InputValidator> getValidators();

  String getValue(T settingsType);

  static <Z> SettingInputComponent<Z> build(final String label,
      final String description, JTextComponent component, BiConsumer<Z, String> updater,
      Function<Z, String> extractor,
      InputValidator ... validators) {
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
        String input = inputComponent.getText();

        for(InputValidator validator : getValidators()) {
          boolean isValid = validator.apply(input);

          if(!isValid) {
            return false;
          }
        }
        updater.accept(toUpdate, input);
        return true;
      }

      @Override
      public List<InputValidator> getValidators() {
        return Arrays.asList(validators);
      }

      @Override
      public String getValue(Z settingsType) {
        return extractor.apply(settingsType);
      }
    };
  }
}
