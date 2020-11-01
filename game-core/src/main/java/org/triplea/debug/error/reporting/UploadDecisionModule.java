package org.triplea.debug.error.reporting;

import javax.swing.JFrame;
import lombok.experimental.UtilityClass;
import org.triplea.debug.LoggerRecord;
import org.triplea.debug.error.reporting.formatting.ErrorReportTitleFormatter;
import org.triplea.http.client.error.report.CanUploadRequest;
import org.triplea.http.client.error.report.ErrorReportClient;
import org.triplea.injection.ClientContext;

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
            .gameVersion(ClientContext.engineVersion().toString())
            .build();

    final var canUploadErrorReportResponse = uploader.canUploadErrorReport(canUploadRequest);

    if (canUploadErrorReportResponse.getCanUpload()) {
      StackTraceReportView.showWindow(parentWindow, uploader, logRecord);
    } else {
      CanNotUploadSwingView.showView(parentWindow, canUploadErrorReportResponse);
    }
  }
}
