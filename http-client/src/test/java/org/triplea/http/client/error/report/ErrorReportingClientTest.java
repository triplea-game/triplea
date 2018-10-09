package org.triplea.http.client.error.report;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.json.message.ErrorReport;
import org.triplea.http.client.error.report.json.message.ErrorReportDetails;
import org.triplea.http.client.error.report.ErrorReportingClient;
import org.triplea.http.client.error.report.ErrorReportingHttpClient;

@ExtendWith(MockitoExtension.class)
class ErrorReportingClientTest {

  private static final ErrorReportDetails ERROR_DATA = ErrorReportDetails.builder()
      .gameVersion("sample version")
      .build();

  @Mock
  private ErrorReport errorReport;

  @Mock
  private ErrorReportingHttpClient errorReportingHttpClient;
  @Mock
  private Function<ErrorReportDetails, ErrorReport> errorReportFunction;
  @Mock
  private Consumer<ErrorReport> consumer;

  private ErrorReportingClient errorReportingClient;

  @BeforeEach
  void setup() {
    errorReportingClient = new ErrorReportingClient(
        errorReportingHttpClient,
        errorReportFunction,
        singletonList(consumer));
  }

  @Test
  void sendErrorReport() {
    when(errorReportFunction.apply(ERROR_DATA))
        .thenReturn(errorReport);

    errorReportingClient.sendErrorReport(ERROR_DATA);

    verify(errorReportingHttpClient, times(1))
        .sendErrorReport(errorReport);
    verify(consumer, times(1))
        .accept(errorReport);
  }
}
