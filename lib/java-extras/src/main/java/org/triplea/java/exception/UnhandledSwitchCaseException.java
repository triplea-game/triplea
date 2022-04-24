package org.triplea.java.exception;

/**
 * Exception to throw when a default case block on a switch should never be executed.
 *
 * <p>EG: <code>
 *   <pre>
 *
 *  switch(someValue) {
 *    case ...:
 *       break;
 *    default:
 *       throw new UnhandledSwitchCaseException(someValue);
 *  }
 *   </pre>
 * </code>
 */
public class UnhandledSwitchCaseException extends RuntimeException {
  private static final long serialVersionUID = -2858696409322458671L;

  public <T> UnhandledSwitchCaseException(final T value) {
    super("Unhandled case value: " + value);
  }
}
