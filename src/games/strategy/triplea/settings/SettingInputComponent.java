package games.strategy.triplea.settings;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

/**
 * Wrapper API around a 'settings' object, allows for a GUI interface that allows a user to read descriptions about
 * each setting in the object, and to update the value.
 */
public interface SettingInputComponent<T> {
  String getLabel();

  String getDescription();

  SettingsInput getInputElement();

  boolean updateSettings(T toUpdate, SettingsInput inputComponent);

  String getValue(T settingsType);


  /** Factory method to create instances of this interface, backed by TextField component types */
  static <Z> SettingInputComponent<Z> build(final String label, final String description, JTextComponent component,
      BiConsumer<Z, String> updater, Function<Z, String> extractor, InputValidator... validators) {
    SettingsInput inputComponent = new SettingsInput() {
      @Override
      public JComponent getSwingComponent() {
        return component;
      }

      @Override
      public String getText() {
        return component.getText();
      }
    };
    return build(label, description, inputComponent, updater, extractor, validators);
  }

  /** Factory method to create instances of this interface, backed by TextField component types */
  static <Z> SettingInputComponent<Z> build(final String label, final String description,
      JPanel componentPanel, Supplier<String> componentReader,
      BiConsumer<Z, String> updater, Function<Z, String> extractor, InputValidator... validators) {

    SettingsInput inputComponent = new SettingsInput() {
      @Override
      public JComponent getSwingComponent() {
        return componentPanel;
      }

      @Override
      public String getText() {
        return componentReader.get();
      }
    };
    return build(label, description, inputComponent, updater, extractor, validators);
  }



  /** Factory method to create instances of this interface */
  static <Z> SettingInputComponent<Z> build(final String label, final String description, SettingsInput component,
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
      public SettingsInput getInputElement() {
        return component;
      }


      @Override
      public boolean updateSettings(Z toUpdate, SettingsInput inputComponent) {
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
