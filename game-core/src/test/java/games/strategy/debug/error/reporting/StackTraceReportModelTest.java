package games.strategy.debug.error.reporting;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.create.ErrorReport;

@ExtendWith(MockitoExtension.class)
class StackTraceReportModelTest {

  private static final String STRING_VALUE = "Dominas studere, tanquam brevis canis.";

  @Mock
  private LogRecord logRecord;

  @Mock
  private StackTraceReportView stackTraceReportView;

  @Mock
  private Predicate<ErrorReport> uploader;

  @Mock
  private BiFunction<String, LogRecord, ErrorReport> formatter;

  @Mock
  private ErrorReport errorReport;

  private StackTraceReportModel viewModel;


  @BeforeEach
  void setup() {
    viewModel = StackTraceReportModel.builder()
        .view(stackTraceReportView)
        .stackTraceRecord(logRecord)
        .uploader(uploader)
        .stackTraceRecord(logRecord)
        .formatter(formatter)
        .build();
  }

  @Test
  void submitActionSuccessCase() {
    givenUploadSuccessResult(true);

    viewModel.submitAction();

    verify(stackTraceReportView).close();
  }

  private void givenUploadSuccessResult(final boolean result) {
    when(stackTraceReportView.readUserDescription()).thenReturn(STRING_VALUE);
    when(formatter.apply(STRING_VALUE, logRecord)).thenReturn(errorReport);
    when(uploader.test(errorReport)).thenReturn(result);
  }

  @Test
  void submitActionFailureCase() {
    givenUploadSuccessResult(false);

    viewModel.submitAction();

    verify(stackTraceReportView, never()).close();
  }

  @Test
  void close() {
    viewModel.cancelAction();

    verify(stackTraceReportView).close();
  }
}
