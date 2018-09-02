package org.triplea.server.error.report.upload;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class ErrorReportIngestionTest {

  private static final ErrorReport ERROR_REPORT = ErrorReport.builder()
      .build();
  @Mock
  private Predicate<ErrorReport> errorReportThrottling;
  @Mock
  private Consumer<ErrorReport> ingestionStrategy;
  @InjectMocks
  private ErrorReportIngestion errorReportIngestion;

  @Test
  void ingestionStrategyIsCalledWhenNotThrottled() {
    givenThrottleAllowsTheMessage();

    errorReportIngestion.reportError(ERROR_REPORT);

    verify(ingestionStrategy, times(1))
        .accept(ERROR_REPORT);
  }

  private void givenThrottleAllowsTheMessage() {
    when(errorReportThrottling.test(ERROR_REPORT))
        .thenReturn(true);
  }

  @Test
  void whenThrottleReturnsFalseThenWeDoNotIngestTheMessage() {
    givenThrottleBlocksTheMessage();

    errorReportIngestion.reportError(ERROR_REPORT);

    verify(ingestionStrategy, times(0))
        .accept(ERROR_REPORT);
  }

  private void givenThrottleBlocksTheMessage() {
    when(errorReportThrottling.test(ERROR_REPORT))
        .thenReturn(false);
  }
}
