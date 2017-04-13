package games.strategy.ui;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;


/**
 * Builder class for using Lambda expressions to create 'AbstractAction'
 *
 * <p>
 * For example, instead of:
 * </p>
 *
 * <pre>
 *   new AbstractionAction("text") {
 *         private final long serialVersion = ..
 *         &#64;Override public void actionPerformed(ActionEvent e ) { doSomething(); }
 *   }
 * </pre>
 *
 * <p>
 * You can rewrite the above using 'SwingAction.of(..)':
 * </p>
 *
 * <pre>
 * SwingAction.of(e -> doSomething());
 * </pre>
 */
public class SwingAction {

  /**
   * @param name Name for the abstract action, passed along to the AbstractAction constructor.
   * @param swingAction Lambda java.tools.function.Consumer object, accepts one arg and returns void.
   */
  public static AbstractAction of(final String name, final Consumer<ActionEvent> swingAction) {
    return new AbstractAction(name) {
      private static final long serialVersionUID = 6751222534195121860L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        swingAction.accept(e);
      }
    };
  }

  public static AbstractAction of(final Consumer<ActionEvent> swingAction) {
    return new AbstractAction() {
      private static final long serialVersionUID = 12331L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        swingAction.accept(e);
      }
    };
  }

  public static void invokeAndWait(final Runnable action) {
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        action.run();
      } else {
        SwingUtilities.invokeAndWait(() -> action.run());
      }
    } catch (final InvocationTargetException e) {
      ClientLogger.logError(e);
    } catch (final InterruptedException e) {
      ClientLogger.logQuietly(e);
    }
  }
}
