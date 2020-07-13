package org.triplea.debug.error.reporting;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.UiContext;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.LogRecord;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.error.report.ErrorReportRequest;

@Builder
class StackTraceReportModel {

  @Nonnull private final StackTraceReportView view;
  @Nonnull private final LogRecord stackTraceRecord;
  @Nonnull private final Function<ErrorReportRequestParams, ErrorReportRequest> formatter;
  @Nonnull private final Predicate<ErrorReportRequest> uploader;
  @Nonnull private final Consumer<ErrorReportRequest> preview;

  void submitAction() {
    if (uploader.test(readErrorReportFromUi())) {
      view.close();
    }
  }

  private ErrorReportRequest readErrorReportFromUi() {
    return formatter.apply(
        ErrorReportRequestParams.builder()
            .userDescription(view.readUserDescription())
            .mapName(
                Optional.ofNullable(UiContext.getResourceLoader())
                    .map(ResourceLoader::getMapName)
                    .orElse(null))
            .logRecord(stackTraceRecord)
            .build());
  }

  void previewAction() {
    preview.accept(readErrorReportFromUi());
  }

  void cancelAction() {
    view.close();
  }
}
