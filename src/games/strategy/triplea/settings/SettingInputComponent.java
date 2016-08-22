package games.strategy.triplea.settings;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
   * describing a specific setting, what it does, and which values they should change it to.
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



  static <Z extends HasDefaults> SettingInputComponent buildIntegerText(
      final IntegerValueRange valueRange,
      final String label,
      final String description,
      final JTextComponent component,
      final BiConsumer<Z, String> writer,
      final Function<Z, String> reader) {

    final String descriptionWithRange = "(" + valueRange.lowerValue + " - " + valueRange.upperValue
        + ", default: " + valueRange.defaultValue + ")\n" + description;

    return SettingInputComponent.buildTextComponent(label, descriptionWithRange, component, reader, writer,
        InputValidator.inRange(valueRange.lowerValue, valueRange.upperValue));
  }

  /**
   * Factory method to create instances of this interface, backed by TextField component types
   */
  static <Z extends HasDefaults> SettingInputComponent buildTextComponent(
      final String label,
      final String description,
      final JTextComponent component,
      final Function<Z, String> settingsModelReader,
      final BiConsumer<Z, String> settingsModelWriter,
      final InputValidator... validators) {

    JPanel panel = new JPanel();
    panel.add(component);

    return SettingInputComponent.build(
        panel,
        new LabelDescription(label, description),
        new SwingComponentReaderWriter(component::getText, component::setText),
        new SettingsModelReaderWriter(settingsModelReader, settingsModelWriter),
        validators);

  }

  static <Z extends HasDefaults> SettingInputComponent<Z> buildYesOrNoRadioButtons(
      final String label,
      final String description,
      final boolean initialValue,
      final BiConsumer<Z, String> settingsObjectWriter,
      final Function<Z, String> settingsObjectReader) {

    final JRadioButton radioButtonYes = new JRadioButton("Yes");
    final JRadioButton radioButtonNo = new JRadioButton("No");
    SwingComponents.createButtonGroup(radioButtonYes, radioButtonNo);


    Supplier<String> reader = () -> String.valueOf(radioButtonYes.isSelected());

    Consumer<String> writer = (input) -> {
      if( Boolean.valueOf(input)) {
        radioButtonYes.setSelected(true);
      } else {
        radioButtonNo.setSelected(true);
      }
    };

    return SettingInputComponent.build(
        createRadioButtonPanel(radioButtonYes, radioButtonNo, initialValue),
        new LabelDescription(label, description),
        new SwingComponentReaderWriter(reader, writer),
        new SettingsModelReaderWriter(settingsObjectReader, settingsObjectWriter));
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

  static class LabelDescription {
    private String label;
    private String description;

    LabelDescription(String label, String description) {
      this.label = label;
      this.description = description;
    }
  }


  class SwingComponentReaderWriter{
    private Supplier<String> reader;
    private Consumer<String> writer;
    SwingComponentReaderWriter(Supplier<String> reader, Consumer<String> writer) {
      this.reader = reader;
      this.writer = writer;
    }
  }

  class SettingsModelReaderWriter<Type extends HasDefaults> {
    private final Function<Type, String> settingsReader;
    private final BiConsumer<Type, String> settingsWriter;

    SettingsModelReaderWriter(Function<Type, String> settingsReader,
        BiConsumer<Type, String> settingsWriter) {
      this.settingsWriter = settingsWriter;
      this.settingsReader = settingsReader;
    }


  }

  static <Type extends HasDefaults> SettingInputComponent build(
      JPanel componentPanel,
      LabelDescription labelDescription,
      SwingComponentReaderWriter swingReaderWriter,
      SettingsModelReaderWriter modelReaderWriter,
      final InputValidator... validators) {

    return new SettingInputComponent<Type>() {
      @Override
      public String getLabel() {
        return labelDescription.label;
      }

      @Override
      public String getDescription() {
        return labelDescription.description;
      }

      @Override
      public SettingsInput getInputElement() {
        return new SettingsInput() {

          @Override
          public JComponent getSwingComponent() {
            return componentPanel;
          }

          @Override
          public String getText() {
            return swingReaderWriter.reader.get();
          }

          @Override
          public void setText(String valueToSet) {
            swingReaderWriter.writer.accept(valueToSet);
          }
        };
      }

      @Override
      public boolean updateSettings(Type toUpdate) {
        final String input = getInputElement().getText();

        for (final InputValidator validator : Arrays.asList(validators)) {
          final boolean isValid = validator.apply(input);

          if (!isValid) {
            return false;
          }
        }
        modelReaderWriter.settingsWriter.accept(toUpdate, input);
        return true;
      }


      @Override
      public String getValue(HasDefaults settingsType) {
        return (String) modelReaderWriter.settingsReader.apply(settingsType);
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
