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
public interface SettingInputComponent<SettingsObjectType extends HasDefaults> {

  /**
   * @return Short two or three word label of the user setting. Use 'getDescription' to provide more detail.
   */
  String getLabel();


  /**
   * @return Detailed (but concise) text description of what the setting represents. This is a message to the user
   * describing a specific setting, what it does, and which values they should change it to.
   */
  String getDescription();

  /**
   * @return The settings input object used to read user input from a Swing component.
   */
  SettingsInput getInputElement();

  /**
   * Return true if a valid setting can be read from  the input component and applied to the 'settings' data object.
   *
   * @param toUpdate       The 'Settings' data object to be updated.
   * @param inputComponent User input source
   */
  boolean updateSettings(SettingsObjectType toUpdate, SettingsInput inputComponent);

  /**
   * Method to read the settings value from the SettingsObject that has the value saved.
   *
   * @param settingsType Settings object which has the current stored user setting value
   * @return An extracted value corresponding to the current setting from the 'settings' object.
   */
  String getValue(SettingsObjectType settingsType);


  /**
   * Factory method to create instances of this interface, backed by TextField component types
   */
  static <Z extends HasDefaults> SettingInputComponent<Z> build(final String label, final String description,
      JTextComponent component,
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

      @Override
      public void setText(String valueToSet) {
        component.setText(valueToSet);
      }
    };
    return build(label, description, inputComponent, updater, extractor, validators);
  }

  /**
   * Factory method to create instances of this interface backed by TextField component types
   */
  static <Z extends HasDefaults> SettingInputComponent<Z> build(final String label, final String description,
      JPanel componentPanel, Supplier<String> componentReader,
      BiConsumer<Z, String> settingsObjectUpdater, Function<Z, String> settingsObjectExtractor,
      InputValidator... validators) {

    SettingsInput inputComponent = new SettingsInput() {
      @Override
      public JComponent getSwingComponent() {
        return componentPanel;
      }

      @Override
      public String getText() {
        System.out.println("GET TEXT -> " + componentReader.get());
        return componentReader.get();
      }

      /**
       * We expect this to only be called in the case when input validation fails. Since user input is constrained
       * valid values only, we never expect this to be the case
       */
      @Override
      public void setText(String valueToSet) {
        throw new RuntimeException("Unsupported operation");
      }
    };
    return build(label, description, inputComponent, settingsObjectUpdater, settingsObjectExtractor, validators);
  }



  /**
   * Generic factory method to create instances of this interface
   */
  static <Z extends HasDefaults> SettingInputComponent<Z> build(final String label, final String description,
      SettingsInput component,
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

      @Override
      public void setValue(String valueToSet) {
        getInputElement().setText(valueToSet);
      }
    };
  }

  /**
   * In cases where we try to update to an invalid value, set Value is called to restore the default/previous valid value
   */
  void setValue(String valueToSet);
}
