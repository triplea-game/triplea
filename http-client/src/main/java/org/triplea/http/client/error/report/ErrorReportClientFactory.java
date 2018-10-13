package org.triplea.http.client.error.report;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServiceClient;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportResponse;

/**
 * Creates an http client that can be used to upload error reports to the TripleA server.
 */
public class ErrorReportClientFactory {

  /**
   * Creates an error report uploader clients, sends error reports and gets a response back.
   */
  public ServiceClient<ErrorReport, ErrorReportResponse> newErrorUploader() {
    return new ServiceClient<>(new HttpClient<>(
        ErrorReportClient.class,
        ErrorReportClient::sendErrorReport));
  }
}
