package org.triplea.http.client.error.report;

import java.net.URI;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;

/**
 * Creates an http client that can be used to upload error reports to the TripleA server.
 */
public final class ErrorReportClientFactory {
  private ErrorReportClientFactory() {}

  /**
   * Creates an error report uploader clients, sends error reports and gets a response back.
   */
  public static ServiceClient<ErrorReport, ErrorReportResponse> newErrorUploader(final URI uri) {
    return new ServiceClient<>(new HttpClient<>(
        ErrorReportClient.class,
        ErrorReportClient::sendErrorReport,
        uri));
  }
}
