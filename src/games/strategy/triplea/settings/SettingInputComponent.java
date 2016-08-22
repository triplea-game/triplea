package games.strategy.triplea.settings;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.text.JTextComponent;

import games.strategy.ui.SwingComponents;

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
   *         describing a specific setting, what it does, and which values they should change it to.
   */
  String getDescription();

  /**
   * @return The settings input object used to read user input from a Swing component.
   */
  SettingsInput getInputElement();

  /**
   * Return true if a valid setting can be read from the input component and applied to the 'settings' data object.
   *
   * @param toUpdate The 'Settings' data object to be updated.
   */
  boolean updateSettings(SettingsObjectType toUpdate);

  /**
   * Method to read the settings value from the SettingsObject that has the value saved.
   *
   * @param settingsType Settings object which has the current stored user setting value
   * @return An extracted value corresponding to the current setting from the 'settings' object.
   */
  String getValue(SettingsObjectType settingsType);



  static <Z extends HasDefaults> SettingInputComponent<Z> build(
      final IntegerValueRange valueRange,
      final String label,
      final String description,
      final JTextComponent component,
      final BiConsumer<Z, String> updater,
      final Function<Z, String> extractor) {

    final String descriptionWithRange = "(" + valueRange.lowerValue + " - " + valueRange.upperValue
        + ", default: " + valueRange.defaultValue + ")\n" + description;

    return SettingInputComponent.build(label, descriptionWithRange, component, updater, extractor,
        InputValidator.inRange(valueRange.lowerValue, valueRange.upperValue));
  }

  /**
   * Factory method to create instances of this interface, backed by TextField component types
   */
  static <Z extends HasDefaults> SettingInputComponent<Z> build(
      final String label,
      final String description,
      final JTextComponent component,
      final BiConsumer<Z, String> updater,
      final Function<Z, String> extractor,
      final InputValidator... validators) {
    final SettingsInput inputComponent = new SettingsInput() {
      @Override
      public JComponent getSwingComponent() {
        return component;
      }

      @Override
      public String getText() {
        return component.getText();
      }

      @Override
      public void setText(final String valueToSet) {
        component.setText(valueToSet);
      }
    };
    return build(label, description, inputComponent, updater, extractor, validators);
  }

  static <Z extends HasDefaults> SettingInputComponent<Z> buildYesOrNoRadioButtons(
      final String label,
      final String description,
      final boolean initialValue,
      final BiConsumer<Z, String> settingsObjectUpdater,
      final Function<Z, String> settingsObjectExtractor) {

    final JRadioButton radioButtonYes = new JRadioButton("Yes");
    final JRadioButton radioButtonNo = new JRadioButton("No");
    SwingComponents.createButtonGroup(radioButtonYes, radioButtonNo);


    return SettingInputComponent.build(
        label,
        description,
        createRadioButtonPanel(radioButtonYes, radioButtonNo, initialValue),
        (() -> radioButtonYes.isSelected() ? Boolean.TRUE.toString() : Boolean.FALSE.toString()),
        settingsObjectUpdater,
        settingsObjectExtractor);

  }

  static JPanel createRadioButtonPanel(
      final JRadioButton buttonYes,
      final JRadioButton buttonNo,
      final boolean yesOptionIsSelected) {
    if (yesOptionIsSelected) {
      buttonYes.setSelected(true);
    } else {
      buttonNo.setSelected(true);
    }
    final JPanel panel = new JPanel();
    panel.add(buttonYes);
    panel.add(buttonNo);
    return panel;
  }


  /**
   * Factory method to create instances of this interface backed by TextField component types
   */
  static <Z extends HasDefaults> SettingInputComponent<Z> build(
      final String label,
      final String description,
      final JPanel componentPanel,
      final Supplier<String> componentReader,
      final BiConsumer<Z, String> settingsObjectUpdater,
      final Function<Z, String> settingsObjectExtractor,
      final InputValidator... validators) {

    final SettingsInput inputComponent = new SettingsInput() {
      @Override
      public JComponent getSwingComponent() {
        return componentPanel;
      }

      @Override
      public String getText() {
        return componentReader.get();
      }

      /**
       * We expect this to only be called in the case when input validation fails. Since user input is constrained
       * valid values only, we never expect this to be the case
       */
      @Override
      public void setText(final String valueToSet) {
        throw new RuntimeException("Unsupported operation");
      }
    };
    return build(label, description, inputComponent, settingsObjectUpdater, settingsObjectExtractor, validators);
  }



  /**
   * Generic factory method to create instances of this interface
   */
  static <Z extends HasDefaults> SettingInputComponent<Z> build(
      final String label,
      final String description,
      final SettingsInput component,
      final BiConsumer<Z, String> updater,
      final Function<Z, String> extractor,
      final InputValidator... validators) {


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
      public boolean updateSettings(final Z toUpdate) {
        final String input = getInputElement().getText();

        for (final InputValidator validator : Arrays.asList(validators)) {
          final boolean isValid = validator.apply(input);

          if (!isValid) {
            return false;
          }
        }
        updater.accept(toUpdate, input);
        return true;
      }

      @Override
      public String getValue(final Z settingsType) {
        return extractor.apply(settingsType);
      }

      @Override
      public void setValue(final String valueToSet) {
        getInputElement().setText(valueToSet);
      }

      @Override
      public String getErrorMessage() {
        final String input = getInputElement().getText();

        final Optional<InputValidator> failedValidator =
            Arrays.asList(validators).stream().filter(validator -> !validator.apply(input)).findFirst();
        if (!failedValidator.isPresent()) {
          return "";
        }
        return input + ", " + failedValidator.get().getErrorMessage();
      }
    };
  }

  /**
   * In cases where we try to update to an invalid value, set Value is called to restore the default/previous valid
   * value
   */
  void setValue(String valueToSet);

  String getErrorMessage();
}
