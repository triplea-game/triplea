package games.strategy.util;

/**
 * An exception to throw when the default case of a switch statment is hit, indicating we missed a case statement.
 * Example usage:
 * <code>
 * <pre>
 *   switch(value) {
 *     case "a":
 *       // do a
 *       break;
 *     case "b:
 *       // do b
 *       break;
 *     default:
 *       throw new UnhandledSwitchCaseException(value);
 *   }
 * </pre>
 * </code>
 */
public class UnhandledSwitchCaseException extends RuntimeException {
  private static final long serialVersionUID = 3537849919628576439L;

  /**
   * @param unhandledValue The switch value that fell thru to a 'default' clause of a switch statement.
   */
  public UnhandledSwitchCaseException(final Object unhandledValue) {
    super(unhandledValue.toString());
  }
}
