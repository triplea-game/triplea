package org.triplea.server.reporting.error;

import java.util.function.Predicate;

/**
 * An object to check if we should accept an error report request from our users and use that
 * request to create a bug tracking ticket. A reason to deny is if rate limit has been exceeded.
 */
class ErrorReportGateKeeper implements Predicate<ErrorReportRequest> {

  /** True if we should allow the error request. */
  @Override
  public boolean test(final ErrorReportRequest errorReportRequest) {
    // TODO: implement
    // TODO: log a warning when we do a request reject and return false
    return true;
  }
}
