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

public final class SettingInputComponentFactory {

  private SettingInputComponentFactory() {

  }

  public static <Z extends HasDefaults> SettingInputComponent<Z> buildIntegerText(
      final IntegerValueRange valueRange,
      final String label,
      final String description,
      final JTextComponent component,
      final BiConsumer<Z, String> writer,
      final Function<Z, String> reader) {
    final JPanel panel = new JPanel();
    panel.add(component);
    return build(
        panel,
        new LabelDescription(label, description),
        Optional.of(valueRange),
        new SwingComponentReaderWriter(component::getText, component::setText),
        new SettingsModelReaderWriter<>(reader, writer),
        InputValidator.inRange(valueRange.lowerValue, valueRange.upperValue));
  }

  /**
   * Factory method to create instances of this interface, backed by TextField component types
   */
  public static <Z extends HasDefaults> SettingInputComponent<Z> buildTextComponent(
      final String label,
      final String description,
      final JTextComponent component,
      final Function<Z, String> settingsModelReader,
      final BiConsumer<Z, String> settingsModelWriter,
      final InputValidator... validators) {

    JPanel panel = new JPanel();
    panel.add(component);

    return build(
        panel,
        new LabelDescription(label, description),
        Optional.empty(),
        new SwingComponentReaderWriter(component::getText, component::setText),
        new SettingsModelReaderWriter<>(settingsModelReader, settingsModelWriter),
        validators);

  }

  public static <Z extends HasDefaults> SettingInputComponent<Z> buildYesOrNoRadioButtons(
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
      if (Boolean.valueOf(input)) {
        radioButtonYes.setSelected(true);
      } else {
        radioButtonNo.setSelected(true);
      }
    };

    return build(
        createRadioButtonPanel(radioButtonYes, radioButtonNo, initialValue),
        new LabelDescription(label, description),
        Optional.empty(),
        new SwingComponentReaderWriter(reader, writer),
        new SettingsModelReaderWriter<>(settingsObjectReader, settingsObjectWriter));
  }

  private static JPanel createRadioButtonPanel(
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

  private static class LabelDescription {
    private String label;
    private String description;

    LabelDescription(String label, String description) {
      this.label = label;
      this.description = description;
    }
  }


  private static class SwingComponentReaderWriter {
    private Supplier<String> reader;
    private Consumer<String> writer;

    SwingComponentReaderWriter(Supplier<String> reader, Consumer<String> writer) {
      this.reader = reader;
      this.writer = writer;
    }
  }

  private static class SettingsModelReaderWriter<Type extends HasDefaults> {
    private final Function<Type, String> settingsReader;
    private final BiConsumer<Type, String> settingsWriter;

    SettingsModelReaderWriter(Function<Type, String> settingsReader,
        BiConsumer<Type, String> settingsWriter) {
      this.settingsWriter = settingsWriter;
      this.settingsReader = settingsReader;
    }


  }

  private static <T extends HasDefaults> SettingInputComponent<T> build(
      JPanel componentPanel,
      LabelDescription labelDescription,
      final Optional<ValueRange> valueRange,
      SwingComponentReaderWriter swingReaderWriter,
      SettingsModelReaderWriter<T> modelReaderWriter,
      final InputValidator... validators) {

    return new SettingInputComponent<T>() {
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
      public boolean updateSettings(T toUpdate) {
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
      public String getValue(T settingsType) {
        return modelReaderWriter.settingsReader.apply(settingsType);
      }

      @Override
      public Optional<ValueRange> getValueRange() {
        return valueRange;
      }

      @Override
      public void setValue(final String valueToSet) {
        getInputElement().setText(valueToSet);
      }

      @Override
      public String getErrorMessage() {
        final String input = getInputElement().getText();

        final Optional<InputValidator> failedValidator =
            Arrays.stream(validators).filter(validator -> !validator.apply(input)).findFirst();
        if (!failedValidator.isPresent()) {
          return "";
        }
        return input + ", " + failedValidator.get().getErrorMessage();
      }
    };
  }

}
