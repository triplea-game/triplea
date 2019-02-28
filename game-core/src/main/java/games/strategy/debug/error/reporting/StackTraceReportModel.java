package games.strategy.debug.error.reporting;

import java.util.function.BiFunction;
import java.util.function.Consumer;
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
  @Nonnull
  private final Consumer<ErrorReport> preview;

  void submitAction() {
    if (uploader.test(readErrorReportFromUi())) {
      view.close();
    }
  }

  private ErrorReport readErrorReportFromUi() {
    return formatter.apply(view.readUserDescription(), stackTraceRecord);
  }

  void previewAction() {
    preview.accept(readErrorReportFromUi());
  }


  void cancelAction() {
    view.close();
  }
}
