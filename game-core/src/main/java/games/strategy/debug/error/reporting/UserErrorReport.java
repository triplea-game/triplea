package games.strategy.debug.error.reporting;

import java.util.logging.LogRecord;

import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportDetails;

import games.strategy.engine.ClientContext;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * Represents the data a user has entered into a swing GUI.
 */
@Builder
@EqualsAndHashCode
class UserErrorReport {
  private final String title;
  private final String description;
  private final LogRecord logRecord;

  public ErrorReport toErrorReport() {
    return new ErrorReport(ErrorReportDetails.builder()
        .gameVersion(ClientContext.engineVersion().getExactVersion())
        .logRecord(logRecord)
        .title(title)
        .problemDescription(description)
        .build());
  }
}
