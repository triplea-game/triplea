package games.strategy.debug.error.reporting;

import java.util.Optional;

import javax.annotation.Nullable;

import org.triplea.http.client.error.report.create.ErrorReport;

import games.strategy.engine.ClientContext;
import lombok.Builder;

/**
 * The user error report window can be seeded with information from the console or an error that
 * impacted the user. The user is given a chance to enter description information of their own. This
 * data object represents the bundle of that data after a user has entered data.
 * .
 * Or, a user can open a new bug report and simply enter a description and submit it.
 */
@Builder
class UserErrorReport {

  @Nullable
  private final String description;
  @Nullable
  private final String errorData;

  ErrorReport toErrorReport() {
    return ErrorReport.builder()
        .operatingSystem(System.getProperty("os.name"))
        .javaVersion(System.getProperty("java.version"))
        .gameVersion(ClientContext.engineVersion().toStringFull())
        .reportMessage(
            Optional.ofNullable(description).map(d -> "### Problem Description\n" + d).orElse("")
                + Optional.ofNullable(errorData).map(e -> "\n### Error data\n" + e))
        .build();
  }
}
