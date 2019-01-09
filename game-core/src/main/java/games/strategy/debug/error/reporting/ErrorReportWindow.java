package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.logging.LogRecord;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JFrameBuilder;
import swinglib.JLabelBuilder;
import swinglib.JPanelBuilder;
import swinglib.JTextAreaBuilder;

/**
 * This is primarily a layout class that shows a form for entering bug report information, and has buttons to
 * preview, cancel, or upload the bug report.
 */
public class ErrorReportWindow {

  private static final int MIN_SUBMISSION_LENGTH = 10;

  public static void showWindow() {
    showWindow(null);
  }

  public static void showWindow(final JFrame parent) {
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

  private static void updateButtonEnabledStatus(
      final JTextArea description, final JTextArea attached, final JButton preview, final JButton submit) {

    final boolean enableSubmit = (description.getText().length() >= MIN_SUBMISSION_LENGTH)
        || (attached.getText().length() >= MIN_SUBMISSION_LENGTH);
    submit.setEnabled(enableSubmit);
    preview.setEnabled(enableSubmit);
  }

  private static void buildWindow(@Nullable final Component parent, final ErrorReportWindowModel model) {
    final JTextArea description = JTextAreaBuilder.builder()
        .columns(10)
        .rows(2)
        .build();

    final JTextArea additionalInfo = model.getAttachedData().map(
        attached -> JTextAreaBuilder.builder()
            .rows(5)
            .text(attached)
            .build())
        .orElse(new JTextArea());

    final JButton submitButton = JButtonBuilder.builder()
        .title("Upload")
        .toolTip("Upload error report to TripleA server")
        .actionListener(button -> model.submitAction(button, description::getText, additionalInfo::getText))
        .biggerFont()
        .build();

    final JButton previewButton = JButtonBuilder.builder()
        .title("Preview")
        .toolTip("Preview the full error report that will be uploaded")
        .actionListener(() -> model.previewAction(description::getText, additionalInfo::getText))
        .build();

    updateButtonEnabledStatus(description, additionalInfo, submitButton, previewButton);

    JTextAreaBuilder.addTextListener(additionalInfo,
        () -> updateButtonEnabledStatus(description, additionalInfo, submitButton, previewButton));
    JTextAreaBuilder.addTextListener(description,
        () -> updateButtonEnabledStatus(description, additionalInfo, submitButton, previewButton));

    final JFrame frame = JFrameBuilder.builder()
        .title("Report a Problem to TripleA Support")
        .locateRelativeTo(parent)
        // TODO: enable size when frame size support is added
        // .size(600, 450)
        .minSize(300, 350)
        .build();

    frame.add(JPanelBuilder.builder()
        .borderEmpty(10)
        .borderLayout()
        .addNorth(JPanelBuilder.builder()
            .borderEmpty(3)
            .borderLayout()
            .addWest(JPanelBuilder.builder()
                .borderEmpty(3)
                .addLabel("Subject:")
                .build())
            .addCenter(titleField)
            .build())
        .addCenter(JPanelBuilder.builder()
            .addNorth(
                JLabelBuilder.builder()
                    .html("Please describe the problem:")
                    .tooltip("This information will be sent to the TripleA development team. Please "
                        + "describe as exactly as possible where the problem is and the events leading "
                        + "up to it.")
                    .border(5)
                    .build())
            .addCenter(description)
            .addSouth(
                model.getAttachedData().isPresent()
                    ? JPanelBuilder.builder()
                        .addNorth(JLabelBuilder.builder()
                            // TODO: enable this once border support is merged.
                            // .border(BorderBuilder.builder()
                            // .top(30)
                            // .bottom(5)
                            // .build())
                            .text("The following will be included automatically:")
                            .build())
                        .add(SwingComponents.newJScrollPane(additionalInfo))
                        .build()
                    : new JPanel())
            .build())
        .addSouth(JPanelBuilder.builder()
            .horizontalBoxLayout()
            // TODO: enable this once border support is merged.
            // .border(BorderBuilder.builder()
            // .top(30)
            // .bottom(10)
            // .build())
            .addHorizontalStrut(10)
            .add(submitButton)
            .addHorizontalStrut(30)
            .add(previewButton)
            .addHorizontalStrut(60)
            .add(JButtonBuilder.builder().title("Cancel").actionListener(frame::dispose).build())
            .addHorizontalStrut(30)
            .build())
        .build());
    frame.setVisible(true);
  }
}
