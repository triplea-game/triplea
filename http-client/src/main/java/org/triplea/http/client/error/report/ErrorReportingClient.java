package org.triplea.http.client.error.report;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.triplea.http.client.ServiceCallResult;
import org.triplea.http.client.error.report.json.message.ErrorReport;
import org.triplea.http.client.error.report.json.message.ErrorReportDetails;
import org.triplea.http.client.error.report.json.message.ErrorReportResponse;
import org.triplea.http.client.throttle.rate.RateLimitingThrottle;
import org.triplea.http.client.throttle.size.MessageSizeThrottle;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Provides high level communication between a TripleA client game and the remote http-server application.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorReportingClient {

  private final ErrorReportingHttpClient errorReportingHttpClient;

  private final Function<ErrorReportDetails, ErrorReport> errorReportFunction;

  private final List<Consumer<ErrorReport>> throttleRules;

  /**
   * Factory method for creating a new http error reporting client, can be used to upload
   * a JSON error report to the server.
   */
  public static ErrorReportingClient withHostUri(final URI hostUri) {
    checkNotNull(hostUri);

    return new ErrorReportingClient(
        ErrorReportingHttpClient.newClient(hostUri),
        ErrorReport::new,
        Arrays.asList(
            new MessageSizeThrottle(),
            new RateLimitingThrottle()));
  }

  /**
   * Sends error report details via http to remote server.
   * 
   * @param reportData The data to be sent.
   */
  public ServiceCallResult<ErrorReportResponse> sendErrorReport(final ErrorReportDetails reportData) {
    final ErrorReport errorReport = errorReportFunction.apply(reportData);
    try {
      throttleRules.forEach(rule -> rule.accept(errorReport));
      final ErrorReportResponse response = errorReportingHttpClient.sendErrorReport(errorReport);
      return ServiceCallResult.<ErrorReportResponse>builder()
          .payload(response)
          .build();
    } catch (final RuntimeException e) {
      return ServiceCallResult.<ErrorReportResponse>builder()
          .thrown(e)
          .build();
    }
  }
}
