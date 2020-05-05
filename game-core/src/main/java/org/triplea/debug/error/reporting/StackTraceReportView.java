package org.triplea.debug.error.reporting;

import java.awt.Component;
import java.util.logging.LogRecord;
import org.triplea.http.client.error.report.ErrorReportClient;

/** Interface for interactions with the stack trace user-reporting window UI. */
public interface StackTraceReportView {

  /** Returns the data user has entered in the error description field. */
  String readUserDescription();

  /** Method where UI components should bind components actions to model methods. */
  void bindActions(StackTraceReportModel viewModel);

  /** Closes the window. */
  void close();

  /** Sets window to visible. */
  void show();

  /**
   * Entry point for creating and display a 'stack trace report' window, a window which provides a
   * basic UI where users can enter an error description before uploading an error report. The
   * window is tied to a stack trace or error that happened and would be uploaded as part of the
   * report. The user description is for developer benefit to get an opportunity for more
   * information around the circumstances of an error.
   */
  static void showWindow(
      final Component parentWindow, final ErrorReportClient uploader, final LogRecord logRecord) {

    final StackTraceReportView window = new StackTraceReportSwingView(parentWindow);

    final StackTraceReportModel viewModel =
        StackTraceReportModel.builder()
            .view(window)
            .stackTraceRecord(logRecord)
            .formatter(new StackTraceErrorReportFormatter())
            .uploader(
                ErrorReportUploadAction.builder()
                    .serviceClient(uploader)
                    .successConfirmation(ConfirmationDialogController::showSuccessConfirmation)
                    .failureConfirmation(ConfirmationDialogController::showFailureConfirmation)
                    .build())
            .preview(new ReportPreviewSwingView(parentWindow))
            .build();

    window.bindActions(viewModel);
    window.show();
  }
}
