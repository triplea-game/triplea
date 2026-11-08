package org.triplea.debug.error.reporting;

import javax.swing.JFrame;
import lombok.experimental.UtilityClass;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.debug.LoggerRecord;
import org.triplea.debug.error.reporting.formatting.ErrorReportTitleFormatter;
import org.triplea.http.client.error.report.CanUploadRequest;
import org.triplea.http.client.error.report.ErrorReportClient;

/**
 * Decision module to handle the case where a user wishes to report an error to TripleA. First we
 * need to invoke the 'can-upload' API on the server which will tell us if there is an existing bug
 * report. If so we render a window that will let them see the bug report, otherwise we show the bug
 * upload report form.
 */
@UtilityClass
public class UploadDecisionModule {

  public static void processUploadDecision(
      final JFrame parentWindow, final ErrorReportClient uploader, final LoggerRecord logRecord) {

    final var canUploadRequest =
        CanUploadRequest.builder()
            .errorTitle(ErrorReportTitleFormatter.createTitle(logRecord))
            .gameVersion(ProductVersionReader.getCurrentVersion().toString())
            .build();

    final var canUploadErrorReportResponse = uploader.canUploadErrorReport(canUploadRequest);

    if (canUploadErrorReportResponse.getCanUpload()) {
      showStackTraceReportWindow(parentWindow, uploader, logRecord);
    } else {
      CanNotUploadSwingView.showView(parentWindow, canUploadErrorReportResponse);
    }
  }

  /**
   * Entry point for creating and display a 'stack trace report' window, a window which provides a
   * basic UI where users can enter an error description before uploading an error report. The
   * window is tied to a stack trace or error that happened and would be uploaded as part of the
   * report. The user description is for developer benefit to get an opportunity for more
   * information around the circumstances of an error.
   */
  private static void showStackTraceReportWindow(
      final JFrame parentWindow,
      final ErrorReportClient uploader,
      final LoggerRecord logRecord) {

    final StackTraceReportView window = new StackTraceReportSwingView(parentWindow);

    final StackTraceReportModel viewModel =
        StackTraceReportModel.builder()
            .view(window)
            .stackTraceRecord(logRecord)
            .uploader(
                ErrorReportUploadAction.builder()
                    .serviceClient(uploader)
                    .successConfirmation(ConfirmationDialogController::showSuccessConfirmation)
                    .failureConfirmation(ConfirmationDialogController::showFailureConfirmation)
                    .build())
            .preview(new ReportPreviewSwingView(parentWindow))
            .engineVersion(ProductVersionReader.getCurrentVersion())
            .build();

    window.bindActions(viewModel);
    window.show();
  }
}
