package org.triplea.debug.error.reporting;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.LogRecord;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.error.report.ErrorReportRequest;

@Builder
class StackTraceReportModel {

  @Nonnull private final StackTraceReportView view;
  @Nonnull private final LogRecord stackTraceRecord;
  @Nonnull private final BiFunction<String, LogRecord, ErrorReportRequest> formatter;
  @Nonnull private final Predicate<ErrorReportRequest> uploader;
  @Nonnull private final Consumer<ErrorReportRequest> preview;

  void submitAction() {
    if (uploader.test(readErrorReportFromUi())) {
      view.close();
    }
  }

  private ErrorReportRequest readErrorReportFromUi() {
    return formatter.apply(view.readUserDescription(), stackTraceRecord);
  }

  void previewAction() {
    preview.accept(readErrorReportFromUi());
  }

  void cancelAction() {
    view.close();
  }
}
