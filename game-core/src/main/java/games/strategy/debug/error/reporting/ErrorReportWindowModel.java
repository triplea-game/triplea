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
    this.parent = parent;
    attachedData = formatLogRecord(logRecord);
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

  ErrorReportWindowModel(final JFrame parent, final String consoleText) {
    this.parent = parent;
    attachedData = consoleText;
  }

  ErrorReportWindowModel(final JFrame parent) {
    this.parent = parent;
    attachedData = null;
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
