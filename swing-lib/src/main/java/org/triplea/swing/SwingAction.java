package org.triplea.swing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Throwables;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;

/**
 * Builder class for using Lambda expressions to create 'AbstractAction'.
 *
 * <p>For example, instead of:
 *
 * <pre>
 *   new AbstractionAction("text") {
 *         private final long serialVersion = ..
 *         &#64;Override public void actionPerformed(ActionEvent e ) { doSomething(); }
 *   }
 * </pre>
 *
 * <p>You can rewrite the above using 'SwingAction.of(..)':
 *
 * <pre>
 * SwingAction.of(e -> doSomething());
 * </pre>
 */
public final class SwingAction {
  private SwingAction() {}

  /**
   * Creates a Swing 'Action' object around a given name and action listener. Example:
   *
   * <pre>
   * <code>
   *   private final Action nullAction = SwingAction.of(" ", e -> {
   *   });
   * </code>
   * </pre>
   *
   * @param name Name for the abstract action, passed along to the AbstractAction constructor.
   * @param swingAction Lambda java.tools.function.Consumer object, accepts one arg and returns
   *     void.
   */
  public static Action of(final String name, final ActionListener swingAction) {
    return new AbstractAction(name) {
      private static final long serialVersionUID = 6751222534195121860L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        swingAction.actionPerformed(e);
      }
    };
  }

  /**
   * Creates a swing {@code Action} object that executes a simple {@code runnable} when executed.
   */
  public static Action of(final String name, final Runnable runner) {
    return of(name, e -> runner.run());
  }

  /**
   * Creates a swing abstract action.
   *
   * @param consumer The potentially nullable source of the event will be passed to the consumer
   */
  public static <T> AbstractAction of(final Consumer<T> consumer) {
    return new AbstractAction() {
      private static final long serialVersionUID = 611055501471099500L;

      @SuppressWarnings("unchecked")
      @Override
      public void actionPerformed(final ActionEvent e) {
        consumer.accept((T) e.getSource());
      }
    };
  }

  /** Creates a swing action that executes a given Runnable when fired. */
  public static AbstractAction of(final Runnable action) {
    return new AbstractAction() {
      private static final long serialVersionUID = 555055501471099555L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        action.run();
      }
    };
  }

  /**
   * Synchronously executes the specified action on the Swing event dispatch thread.
   *
   * <p>This method may safely be called from any thread, including the Swing event dispatch thread.
   *
   * @param action The action to execute.
   * @throws RuntimeException If the action throws an unchecked exception.
   * @throws InterruptedException If the current thread is interrupted while waiting for the action
   *     to complete.
   */
  public static void invokeAndWait(final Runnable action) throws InterruptedException {
    checkNotNull(action);

    invokeAndWaitResult(
        () -> {
          action.run();
          return null;
        });
  }

  /**
   * Synchronously executes the specified action on the Swing event dispatch thread and returns the
   * value it supplies.
   *
   * <p>This method may safely be called from any thread, including the Swing event dispatch thread.
   *
   * @param action The action to execute.
   * @return The value supplied by the action.
   * @throws RuntimeException If the action throws an unchecked exception.
   * @throws InterruptedException If the current thread is interrupted while waiting for the action
   *     to complete.
   */
  public static <T> T invokeAndWaitResult(final Supplier<T> action) throws InterruptedException {
    checkNotNull(action);

    if (SwingUtilities.isEventDispatchThread()) {
      return action.get();
    }

    try {
      final AtomicReference<T> result = new AtomicReference<>();
      SwingUtilities.invokeAndWait(() -> result.set(action.get()));
      return result.get();
    } catch (final InvocationTargetException e) {
      final Throwable cause = e.getCause();
      Throwables.throwIfUnchecked(cause);
      throw new AssertionError("unexpected checked exception", cause);
    }
  }

  /**
   * Synchronously executes the specified action if called from the Swing event dispatch thread.
   * Otherwise, asynchronously executes the specified action on the Swing event dispatch thread.
   *
   * <p>This method may safely be called from any thread, including the Swing event dispatch thread.
   *
   * @param action The action to execute.
   */
  public static void invokeNowOrLater(final Runnable action) {
    checkNotNull(action);

    if (SwingUtilities.isEventDispatchThread()) {
      action.run();
    } else {
      SwingUtilities.invokeLater(action);
    }
  }

  /**
   * Creates a new KeyListener that is executed on key release event.
   *
   * @param eventConsumer We will pass a key event to this consumer whenever there is a key released
   *     event.
   * @return A Swing KeyListener object, can be attached to swing component objects.
   */
  public static KeyListener keyReleaseListener(final Consumer<KeyEvent> eventConsumer) {
    return new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        eventConsumer.accept(e);
      }
    };
  }
}
