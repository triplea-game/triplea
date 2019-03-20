package org.triplea.http.client.error.report;

import java.net.URI;

import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServiceClient;

/**
 * Creates an http client that can be used to upload error reports to the TripleA server.
 */
public final class ErrorUploadClientFactory {
  private ErrorUploadClientFactory() {}

  /**
   * Creates an error report uploader clients, sends error reports and gets a response back.
   */
  public static ServiceClient<ErrorUploadRequest, ErrorUploadResponse> newErrorUploader(final URI uri) {
    return new ServiceClient<>(new HttpClient<>(
        ErrorUploadClient.class,
        ErrorUploadClient::sendErrorReport,
        uri));
  }
}
