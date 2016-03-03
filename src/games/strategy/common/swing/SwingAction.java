package games.strategy.common.swing;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;


/**
 * Builder class for using Lambda expressions to create 'AbstractAction'
 *
 * For example, instead of:
 * <code>
 *   new AbstractionAction("text") {
 *         private final long serialVersion = ..
 *         &#64;Override public void actionPerformed(ActionEvent e ) { doSomething(); }
 *   }
 * </code>
 *
 * You can rewrite the above using 'SwingAction.of(..)':
 *
 * <code>
 *    SwingAction.of( e -> doSomething() );
 * </code>
 */
public class SwingAction {

  /**
   * @param name Name for the abstract action, passed along to the AbstractAction constructor.
   * @param swingAction Lambda java.util.function.Consumer object, accepts one arg and returns void.
   */
  public static AbstractAction of(final String name, final Consumer<ActionEvent> swingAction) {
    return new AbstractAction(name) {
      private static final long serialVersionUID = 6751222534195121860L;

      @Override
      public void actionPerformed(ActionEvent e) {
        swingAction.accept(e);
      }
    };
  }

  public static AbstractAction of(final Consumer<ActionEvent> swingAction) {
    return new AbstractAction() {
      private static final long serialVersionUID = 12331L;

      @Override
      public void actionPerformed(ActionEvent e) {
        swingAction.accept(e);
      }
    };
  }

  public static void invokeAndWait(Runnable action) {
    try {
      SwingUtilities.invokeAndWait(() -> action.run());
    } catch (InvocationTargetException e) {
      ClientLogger.logError(e);
    } catch (InterruptedException e) {
      ClientLogger.logQuietly(e);
    }
  }
}
