package games.strategy.triplea.ui;

/**
 * Captures the interface needed to be able to handle unhandled AWT exceptions thrown on other threads.
 * The intent of this class is to allow child class to mark the required method as "@Override" so that
 * static checkers will not think it is unused. The AWT system auto-magically calls that method.
 *
 * <p>
 * Otherwise, to implement this interface successfully, a class would also need to:
 * </p>
 *
 * <ul>
 * <li>have a zero arg constructor</li>
 * <li>register the class name with a system property, like this:
 * <code>System.setProperty("sun.awt.exception.handler", &lt;your_implementing_instance>.class.getName());</code></li>
 * </ul>
 *
 * @see ErrorHandler
 */
public interface ErrorHandlerAwtEvents {
  void handle(Throwable throwable);
}
