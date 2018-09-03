package org.triplea.server.http.spark;

import static org.mockito.Mockito.times;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.server.reporting.error.upload.ErrorReport;
import org.triplea.server.reporting.error.upload.ErrorReportIngestion;

import spark.Request;

@ExtendWith(MockitoExtension.class)
class SparkServerTest {

  private static final String SAMPLE_PAYLOAD = "unstructured post body";
  private static final String SAMPLE_IP = "ip";
  private static final String SAMPLE_HOST = "HOST";
  @Mock
  private ErrorReportIngestion errorReportIngestion;
  @Mock
  private Supplier<Instant> instantSupplier;
  @InjectMocks
  private SparkServer sparkServer;
  @Mock
  private Request request;
  private static final Instant SAMPLE_INSTANT = LocalDateTime.of(2000, 12, 30, 1, 1)
      .toInstant(ZoneOffset.UTC);

  @Test
  void errorReport() {
    Mockito.when(request.ip()).thenReturn(SAMPLE_IP);
    Mockito.when(request.host()).thenReturn(SAMPLE_HOST);
    Mockito.when(request.body()).thenReturn(SAMPLE_PAYLOAD);
    Mockito.when(instantSupplier.get()).thenReturn(SAMPLE_INSTANT);

    sparkServer.uploadErrorReport(request);

    Mockito.verify(errorReportIngestion, times(1))
        .reportError(ErrorReport.builder()
            .reportContents(SAMPLE_PAYLOAD)
            .reportingHostId(SparkServer.formatHostId(SAMPLE_HOST, SAMPLE_IP))
            .reportedOn(SAMPLE_INSTANT)
            .build());
  }
}
