package games.strategy.debug.error.reporting;

import java.util.logging.LogRecord;

import org.triplea.http.data.error.report.ErrorReport;
import org.triplea.http.data.error.report.ErrorReportDetails;

import com.google.common.base.Strings;

import games.strategy.engine.ClientContext;
import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * Represents the data a user has entered into a swing GUI.
 */
@Builder
@EqualsAndHashCode
class UserErrorReport {
  private final String description;
  private final String additionalInfo;
  private final LogRecord logRecord;

  public ErrorReport toErrorReport() {
    return new ErrorReport(ErrorReportDetails.builder()
        .gameVersion(ClientContext.engineVersion().getExactVersion())
        .logRecord(logRecord)
        .messageFromUser(formatUserMessage())
        .build());
  }

  private String formatUserMessage() {
    if ((Strings.emptyToNull(description) == null) && (Strings.emptyToNull(additionalInfo) == null)) {
      return "";
    }

    if (description == null) {
      return additionalInfo;
    } else if (additionalInfo == null) {
      return description;
    }

    return description + " : " + additionalInfo;
  }
}
