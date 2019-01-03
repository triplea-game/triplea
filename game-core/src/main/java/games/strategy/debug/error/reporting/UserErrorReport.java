package games.strategy.debug.error.reporting;

import java.util.logging.LogRecord;

import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportDetails;

import games.strategy.engine.ClientContext;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Represents the data a user has entered into a swing GUI. */
@Builder
@EqualsAndHashCode
@ToString
@Getter(AccessLevel.PACKAGE)
class UserErrorReport {
  private final String title;
  private final String description;
  private final LogRecord logRecord;

  ErrorReport toErrorReport() {
    return new ErrorReport(
        ErrorReportDetails.builder()
            .gameVersion(ClientContext.engineVersion().getExactVersion())
            .logRecord(logRecord)
            .title(title)
            .description(description)
            .build());
  }
}
