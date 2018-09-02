package org.triplea.server.error.report.upload;

import java.util.function.Predicate;

/**
 * A strategy object that will give a yes/no answer if a message should
 * be process (true), or if it should be throttled (false). When throttling
 * is applied we should attempt to then use a minimum of resources.
 */
public class ErrorReportThrottling implements Predicate<ErrorReport> {

  @Override
  public boolean test(final ErrorReport report) {
    // TODO develop a throttling strategy
    return true;
  }
}
