package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.LogRecord;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import org.triplea.swing.BorderBuilder;
import org.triplea.swing.DocumentListenerBuilder;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JLabelBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingComponents;

import lombok.Getter;

/**
 * This is primarily a layout class that shows a form for entering bug report information, and has buttons to
 * preview, cancel, or upload the bug report.
 * <p>
 * Bug report submission has two parts: 1) user text and 2) error data. An error message or console event will add the
 * data from either an error message or console.
 * The error data is supplied automatically on error message or when shown from console
 * </p>
 */
public final class ErrorReportWindow {

  private static final int MIN_SUBMISSION_LENGTH = 10;

  private ErrorReportWindow() {}

  public static void showWindow() {
    showWindow(null);
  }

  private static void showWindow(final JFrame parent) {
    final ErrorReportWindowModel model = new ErrorReportWindowModel(parent);
    buildWindow(parent, model);
  }

  // TODO: add button to error message to show this window
  public static void showWindow(final JFrame parent, final LogRecord logRecord) {
    final ErrorReportWindowModel model = new ErrorReportWindowModel(parent, logRecord);
    buildWindow(parent, model);
  }

  // TODO: add button to console, call this method and send console contents here.
  public static void showWindow(final JFrame parent, final String consoleText) {
    final ErrorReportWindowModel model = new ErrorReportWindowModel(parent, consoleText);
    buildWindow(parent, model);
  }

  private static void buildWindow(@Nullable final Component parent, final ErrorReportWindowModel model) {
    final Components components = new Components(model);

    final JFrame frame = JFrameBuilder.builder()
        .title("Report a Problem to TripleA Support")
        .locateRelativeTo(parent)
        .size(600, 450)
        .minSize(300, 350)
        .build();

    final Panels panels = new Panels(frame, components);

    frame.add(
        JPanelBuilder.builder()
            .border(10)
            .addCenter(
                JPanelBuilder.builder()
                    .addNorth(components.getDescriptionFieldLabel())
                    .addCenter(components.getDescriptionField())
                    .addSouth(panels.getAdditionalInfoFieldPanel().orElseGet(JPanel::new))
                    .build())
            .addSouth(
                panels.getButtonsPanel())
            .build());
    frame.setVisible(true);
  }


  /**
   * Factory class for individual UI components.
   */
  @Getter
  private static final class Components {
    private final JLabel descriptionFieldLabel;

    private final JTextArea descriptionField;
    @Nullable
    private final JTextArea additionalInfoField;

    private final JButton submitButton;
    private final JButton previewButton;


    private Components(final ErrorReportWindowModel model) {

      descriptionFieldLabel = JLabelBuilder.builder()
          .html("Please describe the problem:")
          .tooltip("This information will be sent to the TripleA development team. Please "
              + "describe as exactly as possible where the problem is and the events leading "
              + "up to it.")
          .border(5)
          .build();

      descriptionField = JTextAreaBuilder.builder()
          .columns(10)
          .rows(2)
          .build();

      final Optional<JTextArea> additionalInfo = model.getAttachedData().map(
          attached -> JTextAreaBuilder.builder()
              .rows(5)
              .text(attached)
              .build());
      additionalInfoField = additionalInfo.orElse(null);

      final Supplier<String> additionalInfoReader = () -> additionalInfo.map(JTextComponent::getText).orElse("");

      submitButton = JButtonBuilder.builder()
          .title("Upload")
          .toolTip("Upload error report to TripleA server")
          .actionListener(button -> model.submitAction(descriptionField::getText, additionalInfoReader))
          .biggerFont()
          .build();

      previewButton = JButtonBuilder.builder()
          .title("Preview")
          .toolTip("Preview the full error report that will be uploaded")
          .actionListener(() -> model.previewAction(descriptionField::getText, additionalInfoReader))
          .build();

      updateButtonEnabledStatus(
          descriptionField::getText,
          additionalInfoReader,
          submitButton,
          previewButton);

      final Runnable listenerAction = () -> updateButtonEnabledStatus(
          descriptionField::getText,
          additionalInfoReader,
          submitButton,
          previewButton);

      DocumentListenerBuilder.attachDocumentListener(descriptionField, listenerAction);
      additionalInfo.ifPresent(area -> DocumentListenerBuilder.attachDocumentListener(area, listenerAction));
    }

    private static void updateButtonEnabledStatus(
        final Supplier<String> description,
        final Supplier<String> attached,
        final JButton preview,
        final JButton submit) {

      final boolean enableSubmit = (description.get().length() >= MIN_SUBMISSION_LENGTH)
          || (attached.get().length() >= MIN_SUBMISSION_LENGTH);
      submit.setEnabled(enableSubmit);
      preview.setEnabled(enableSubmit);
    }

    Optional<JTextArea> getAdditionalInfoField() {
      return Optional.ofNullable(additionalInfoField);
    }
  }


  /**
   * Factory class to create 'complex' panels that represent a logical UI element.
   */
  @Getter
  private static final class Panels {
    @Nullable
    private final JPanel additionalInfoFieldPanel;
    private final JPanel buttonsPanel;

    private Panels(final JFrame frame, final Components components) {
      additionalInfoFieldPanel = components.getAdditionalInfoField().map(
          textArea -> JPanelBuilder.builder()
              .addNorth(JLabelBuilder.builder()
                  .border(BorderBuilder.builder()
                      .top(30)
                      .bottom(5)
                      .build())
                  .text("The following will be included automatically:")
                  .build())
              .add(SwingComponents.newJScrollPane(textArea))
              .build())
          .orElse(null);

      buttonsPanel = JPanelBuilder.builder()
          .horizontalBoxLayout()
          .border(
              BorderBuilder.builder()
                  .top(30)
                  .bottom(10)
                  .build())
          .addHorizontalStrut(10)
          .add(components.getSubmitButton())
          .addHorizontalStrut(30)
          .add(components.getPreviewButton())
          .addHorizontalStrut(60)
          .add(JButtonBuilder.builder().title("Cancel").actionListener(frame::dispose).build())
          .addHorizontalStrut(30)
          .build();
    }

    Optional<JPanel> getAdditionalInfoFieldPanel() {
      return Optional.ofNullable(additionalInfoFieldPanel);
    }
  }
}
