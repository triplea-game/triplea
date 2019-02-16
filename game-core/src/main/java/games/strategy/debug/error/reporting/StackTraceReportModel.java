package games.strategy.debug.error.reporting;

import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.logging.LogRecord;

import javax.annotation.Nonnull;

import org.triplea.http.client.error.report.create.ErrorReport;

import lombok.Builder;

@Builder
class StackTraceReportModel {

  @Nonnull
  private final StackTraceReportView view;
  @Nonnull
  private final LogRecord stackTraceRecord;
  @Nonnull
  private final BiFunction<String, LogRecord, ErrorReport> formatter;
  @Nonnull
  private final Predicate<ErrorReport> uploader;

  void submitAction() {
    final ErrorReport data = formatter.apply(view.readUserDescription(), stackTraceRecord);
    if (uploader.test(data)) {
      view.close();
    }
  }

  void cancelAction() {
    view.close();
  }
}
