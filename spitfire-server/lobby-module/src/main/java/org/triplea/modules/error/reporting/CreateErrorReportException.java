package org.triplea.modules.error.reporting;

/**
 * An exception type thrown when http server is unable to create an error report, generally this
 * will be an error interacting with github API.
 */
public class CreateErrorReportException extends RuntimeException {

  private static final long serialVersionUID = -3383780224745718882L;

  public CreateErrorReportException(final String msg) {
    super("Error creating error report: " + msg);
  }
}
