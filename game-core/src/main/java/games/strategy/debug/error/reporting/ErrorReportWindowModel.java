package games.strategy.debug.error.reporting;

import java.awt.Component;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.LogRecord;

import javax.annotation.Nullable;
import javax.swing.JFrame;

import swinglib.DialogBuilder;

/**
 * UI model class, manages the state and interactions between {@code ErrorReportWindow} and the rest of the system.
 */
class ErrorReportWindowModel {

  private final JFrame parent;

  @Nullable
  private final String attachedData;


  ErrorReportWindowModel(final JFrame parent, final LogRecord logRecord) {
    this(parent, formatLogRecord(logRecord));
  }

  ErrorReportWindowModel(final JFrame parent) {
    this(parent, (String) null);
  }

  ErrorReportWindowModel(final JFrame parent, @Nullable final String consoleText) {
    this.parent = parent;
    attachedData = consoleText;
  }

  private static String formatLogRecord(final LogRecord logRecord) {
    return String.format(
        "Error: %s\n"
            + "ClassName: %s\n"
            + "MethodName: %s\n"
            + "StackTrace: %s\n",
        logRecord.getThrown(),
        logRecord.getSourceClassName(),
        logRecord.getSourceMethodName(),
        Arrays.toString(logRecord.getThrown().getStackTrace()));
  }


  Optional<String> getAttachedData() {
    return Optional.ofNullable(attachedData);
  }


  void previewAction(final Supplier<String> userInputReader,
      final Supplier<String> additionalInformationReader) {

    PreviewWindow.build(
        parent,
        UserErrorReport.builder()
            .description(userInputReader.get())
            .errorData(additionalInformationReader.get())
            .build()
            .toErrorReport()
            .toString())
        .setVisible(true);
  }

  /**
   * On submit we'll ask the user for confirmation if they'd like to send the error report. If yes,
   * then the error report will be uploaded.
   */
  Runnable submitAction(
      final Component button,
      final Supplier<String> userInputReader,
      final Supplier<String> additionalInformationReader) {

    final Runnable confirmAction = () -> ErrorReportConfiguration.newReportHandler().accept(parent,
        UserErrorReport.builder()
            .description(userInputReader.get())
            .errorData(additionalInformationReader.get())
            .build());

    return () -> DialogBuilder.builder()
        .parent(parent)
        .title("Confirm Upload")
        .confirmationQuestion("Please confirm you are ready to begin uploading the error report.")
        .confirmAction(confirmAction)
        .showDialog();
  }
}
