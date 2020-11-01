package org.triplea.debug.error.reporting;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.UiContext;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.debug.LoggerRecord;
import org.triplea.debug.console.window.DebugUtils;
import org.triplea.debug.error.reporting.formatting.ErrorReportBodyFormatter;
import org.triplea.debug.error.reporting.formatting.ErrorReportTitleFormatter;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.injection.ClientContext;

@Builder
class StackTraceReportModel {

  @Nonnull private final StackTraceReportView view;
  @Nonnull private final LoggerRecord stackTraceRecord;
  @Nonnull private final Predicate<ErrorReportRequest> uploader;
  @Nonnull private final Consumer<ErrorReportRequest> preview;

  void submitAction() {
    if (uploader.test(readErrorReportFromUi())) {
      view.close();
    }
  }

  private ErrorReportRequest readErrorReportFromUi() {
    return ErrorReportRequest.builder()
        .title(ErrorReportTitleFormatter.createTitle(stackTraceRecord))
        .body(
            ErrorReportBodyFormatter.buildBody(
                view.readUserDescription(),
                Optional.ofNullable(UiContext.getResourceLoader())
                    .map(ResourceLoader::getMapName)
                    .orElse(null),
                DebugUtils.getMemory(),
                stackTraceRecord))
        .gameVersion(ClientContext.engineVersion().toString())
        .build();
  }

  void previewAction() {
    preview.accept(readErrorReportFromUi());
  }

  void cancelAction() {
    view.close();
  }
}
