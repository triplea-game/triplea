package org.triplea.server.error.report.upload;

import java.util.function.Consumer;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;


/**
 * A service class that can process an incoming ErrorReport and persist
 * the information on our server.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Log
public class ErrorReportIngestion {

  private final Predicate<ErrorReport> errorReportThrottling;
  private final Consumer<ErrorReport> ingestionStrategy;

  public ErrorReportIngestion() {
    this(
        new ErrorReportThrottling(),
        report -> log.info("Received error report: " + report));
  }

  public void reportError(final ErrorReport report) {
    if (errorReportThrottling.test(report)) {
      ingestionStrategy.accept(report);
    } else {
      // TODO: this should wind up being a JMX counter so we can get a line chart and not add to the log
      log.info("Throttle applied, dropping error report: " + report);
    }
  }
}
