package org.triplea.debug.error.reporting;

import games.strategy.engine.data.GameData;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Setter;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.debug.LoggerRecord;
import org.triplea.debug.error.reporting.formatting.ErrorReportBodyFormatter;
import org.triplea.debug.error.reporting.formatting.ErrorReportTitleFormatter;
import org.triplea.http.client.error.report.ErrorReportRequest;
import org.triplea.util.Version;

@Builder
public class StackTraceReportModel {

  /**
   * A dirty hack to keep track of the current map for error reporting. Ideally this should be
   * solved differently.
   */
  @Setter @Nullable private static String currentMapName;

  @Nonnull private final StackTraceReportView view;
  @Nonnull private final LoggerRecord stackTraceRecord;
  @Nonnull private final Predicate<ErrorReportRequest> uploader;
  @Nonnull private final Consumer<ErrorReportRequest> preview;
  @Nonnull private final Version engineVersion;

  void submitAction() {
    if (uploader.test(readErrorReportFromUi())) {
      view.close();
    }
  }

  public static void setCurrentMapNameFromGameData(GameData gameData) {
    setCurrentMapName(gameData.getMapName() + " / " + gameData.getGameName());
  }

  private ErrorReportRequest readErrorReportFromUi() {
    return ErrorReportRequest.builder()
        .title(ErrorReportTitleFormatter.createTitle(stackTraceRecord))
        .body(
            ErrorReportBodyFormatter.buildBody(
                view.readUserDescription(), currentMapName, stackTraceRecord, engineVersion))
        .gameVersion(ProductVersionReader.getCurrentVersion().toString())
        .build();
  }

  void previewAction() {
    preview.accept(readErrorReportFromUi());
  }

  void cancelAction() {
    view.close();
  }
}
