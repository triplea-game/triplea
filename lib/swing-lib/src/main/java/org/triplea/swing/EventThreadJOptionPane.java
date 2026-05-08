package org.triplea.swing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import lombok.AllArgsConstructor;
import org.triplea.java.concurrency.CountDownLatchHandler;

/** Blocking JOptionPane calls that do their work in the swing event thread (to be thread safe). */
public final class EventThreadJOptionPane {
  private static final int KEY_GUARD_WINDOW_MILLIS = 1000;

  private EventThreadJOptionPane() {}

  /**
   * Shows a message dialog using a {@code CountDownLatchHandler} that will release its associated
   * latches upon interruption.
   *
   * @see JOptionPane#showMessageDialog(Component, Object)
   */
  public static void showMessageDialog(
      final @Nullable Component parentComponent, final @Nullable Object message) {
    invokeAndWait(
        new CountDownLatchHandler(), () -> JOptionPane.showMessageDialog(parentComponent, message));
  }

  /**
   * Shows a message dialog using a {@code CountDownLatchHandler} that will release its associated
   * latches upon interruption.
   *
   * @see JOptionPane#showMessageDialog(Component, Object, String, int)
   */
  public static void showMessageDialog(
      final @Nullable Component parentComponent,
      final @Nullable Object message,
      final @Nullable String title,
      final int messageType) {
    showMessageDialog(parentComponent, message, title, messageType, new CountDownLatchHandler());
  }

  /**
   * Shows a message dialog using the specified {@code CountDownLatchHandler}.
   *
   * @param latchHandler The handler with which to associate the latch used to await the dialog.
   * @see JOptionPane#showMessageDialog(Component, Object, String, int)
   */
  public static void showMessageDialog(
      final @Nullable Component parentComponent,
      final @Nullable Object message,
      final @Nullable String title,
      final int messageType,
      final CountDownLatchHandler latchHandler) {
    checkNotNull(latchHandler);

    invokeAndWait(
        latchHandler,
        () -> JOptionPane.showMessageDialog(parentComponent, message, title, messageType));
  }

  /**
   * Shows a message dialog using the specified {@code CountDownLatchHandler} and using a scroll
   * pane to display the message.
   *
   * @param latchHandler The handler with which to associate the latch used to await the dialog.
   * @see JOptionPane#showMessageDialog(Component, Object, String, int)
   */
  public static void showMessageDialogWithScrollPane(
      final @Nullable Component parentComponent,
      final @Nullable String message,
      final @Nullable String title,
      final int messageType,
      final CountDownLatchHandler latchHandler) {
    checkNotNull(latchHandler);

    invokeAndWait(
        latchHandler,
        () ->
            JOptionPane.showMessageDialog(
                parentComponent, newScrollPane(message), title, messageType));
  }

  private static void invokeAndWait(
      final CountDownLatchHandler latchHandler, final Runnable runnable) {
    invokeAndWait(
        latchHandler,
        () -> {
          runnable.run();
          return Optional.empty();
        });
  }

  @VisibleForTesting
  static <T> Optional<T> invokeAndWait(
      final CountDownLatchHandler latchHandler, final Supplier<Optional<T>> supplier) {
    if (SwingUtilities.isEventDispatchThread()) {
      return supplier.get();
    }

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Optional<T>> result = new AtomicReference<>();
    SwingUtilities.invokeLater(
        () -> {
          result.set(supplier.get());
          latch.countDown();
        });
    latchHandler.addShutdownLatch(latch);
    awaitLatch(latchHandler, latch);
    return result.get();
  }

  @VisibleForTesting
  static int invokeAndWait(final CountDownLatchHandler latchHandler, final IntSupplier supplier) {
    return invokeAndWait(latchHandler, () -> Optional.of(supplier.getAsInt())).get();
  }

  private static JScrollPane newScrollPane(final @Nullable String message) {
    final JLabel label = new JLabel(message);
    final JScrollPane scroll = new JScrollPane(label);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.addAncestorListener(
        new AncestorListener() {
          private int getScrollWidth() {
            Object scrollWidth = UIManager.get("ScrollBar.width");
            if (scrollWidth instanceof Integer scrollWidthInteger) {
              return scrollWidthInteger;
            }
            return 25;
          }

          @Override
          public void ancestorAdded(final AncestorEvent event) {
            Rectangle maxBounds =
                GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            Rectangle r = event.getAncestor().getBounds();
            boolean vertScroll = (r.width > maxBounds.width);
            boolean horizScroll = (r.height > maxBounds.height);
            r.width = Math.min(r.width + (horizScroll ? getScrollWidth() : 0), maxBounds.width);
            r.height = Math.min(r.height + (vertScroll ? getScrollWidth() : 0), maxBounds.height);
            event.getAncestor().setBounds(r);
          }

          @Override
          public void ancestorRemoved(final AncestorEvent event) {}

          @Override
          public void ancestorMoved(final AncestorEvent event) {}
        });
    return scroll;
  }

  private static void awaitLatch(
      final CountDownLatchHandler latchHandler, final CountDownLatch latch) {
    boolean done = false;
    while (!done) {
      try {
        latch.await();
        done = true;
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        latchHandler.interruptLatch(latch);
      }
    }

    latchHandler.removeShutdownLatch(latch);
  }

  /**
   * Shows an option dialog using the specified {@code CountDownLatchHandler}.
   *
   * @param latchHandler The handler with which to associate the latch used to await the dialog.
   * @see JOptionPane#showOptionDialog(Component, Object, String, int, int, Icon, Object[], Object)
   */
  public static int showOptionDialog(
      final @Nullable Component parentComponent,
      final @Nullable Object message,
      final @Nullable String title,
      final int optionType,
      final int messageType,
      final @Nullable Icon icon,
      final @Nullable Object[] options,
      final @Nullable Object initialValue,
      final CountDownLatchHandler latchHandler) {
    checkNotNull(latchHandler);

    return invokeAndWait(
        latchHandler,
        () ->
            JOptionPane.showOptionDialog(
                parentComponent,
                message,
                title,
                optionType,
                messageType,
                icon,
                options,
                initialValue));
  }

  @AllArgsConstructor
  public enum ConfirmDialogType {
    YES_NO(JOptionPane.YES_NO_OPTION),
    OK_CANCEL(JOptionPane.OK_CANCEL_OPTION);

    final int optionTypeMagicNumber;
  }

  /**
   * Shows a blocking, non-modal confirmation dialog.
   *
   * @return True if user confirms, false if user closes the confirmation dialog or selects no.
   */
  public static boolean showConfirmDialog(
      final @Nullable Component parentComponent,
      final @Nullable Object message,
      final @Nullable String title,
      final ConfirmDialogType confirmDialogType) {

    // We want to construct a 'JDialog' the "hard" way through a JOptionPane so
    // that we can set modal to be false.
    // Only modal dialogs are blocking. To mimic this, we use a latch to block once
    // the dialog is set to visible. We use a property listener to capture the users
    // confirmation choice and to unblock.
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Boolean> confirmation = new AtomicReference<>();

    // Swing components must be created in Swing event threads. If we are already
    // in one, dialog creation is fairly easy.
    if (SwingUtilities.isEventDispatchThread()) {
      final JOptionPane optionPane =
          new JOptionPane(
              message, JOptionPane.QUESTION_MESSAGE, confirmDialogType.optionTypeMagicNumber);
      final JDialog dialog = optionPane.createDialog(parentComponent, title);
      dialog.setAlwaysOnTop(true);
      final Runnable cancelKeyGuard = installKeyActivationGuard(optionPane, dialog);

      optionPane.addPropertyChangeListener(
          JOptionPane.VALUE_PROPERTY,
          ignored -> {
            cancelKeyGuard.run();
            final Object selectedValue = optionPane.getValue();
            confirmation.set(selectedValue != null && JOptionPane.OK_OPTION == (int) selectedValue);
            latch.countDown();
            dialog.dispose();
          });

      // modal dialog being set to visible is blocking
      dialog.setVisible(true);

      // For non-Swing event threads, we must request our code be invoked and block manually
    } else {
      SwingUtilities.invokeLater(
          () -> {
            final JOptionPane optionPane =
                new JOptionPane(
                    message, JOptionPane.QUESTION_MESSAGE, confirmDialogType.optionTypeMagicNumber);
            final JDialog dialog = optionPane.createDialog(parentComponent, title);
            dialog.setAlwaysOnTop(true);
            dialog.setModal(false);
            final Runnable cancelKeyGuard = installKeyActivationGuard(optionPane, dialog);

            optionPane.addPropertyChangeListener(
                JOptionPane.VALUE_PROPERTY,
                ignored -> {
                  cancelKeyGuard.run();
                  final Object selectedValue = optionPane.getValue();
                  confirmation.set(
                      selectedValue != null && JOptionPane.OK_OPTION == (int) selectedValue);
                  latch.countDown();
                  dialog.dispose();
                });

            // non modal dialog set to visible is not blocking and must be done on EDT
            dialog.setVisible(true);
          });

      try {
        // start blocking, wait for the dialog property event to fire to clear this latch
        latch.await();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        latch.countDown();
      }
    }

    return Optional.ofNullable(confirmation.get()).orElse(false);
  }

  /**
   * Installs a short-lived guard that prevents stray keystrokes from accidentally confirming or
   * dismissing a confirmation dialog the moment it appears.
   *
   * <p><b>Problem.</b> A confirmation dialog can pop up while the user is typing somewhere else
   * (most commonly the in-game chat). A space, Enter, or Escape that was already on the way to the
   * previously-focused window gets routed to the new dialog instead and immediately picks an answer
   * the user never intended to give.
   *
   * <p><b>Why the obvious fixes are not enough.</b> Swing wires button activation through several
   * independent paths, and disabling any one of them still leaves the others live:
   *
   * <ul>
   *   <li>Even with no default button on the root pane, a {@link JButton} that holds focus
   *       activates on Space and Enter through its own {@code WHEN_FOCUSED} bindings.
   *   <li>{@link JOptionPane}'s L&F installs a {@code WHEN_IN_FOCUSED_WINDOW} binding for Escape
   *       (the "close" action) that fires regardless of which component has focus.
   *   <li>Overriding {@code selectInitialValue} only stops the initial focus; it doesn't stop any
   *       of the bindings above.
   * </ul>
   *
   * <p><b>The guard.</b> For {@value #KEY_GUARD_WINDOW_MILLIS}ms after the dialog is shown, three
   * layers run together so every activation path is closed:
   *
   * <ol>
   *   <li>The root pane's default button is cleared, so Enter has no root-pane action target.
   *   <li>Every {@link JButton} in the option pane is made non-focusable, so focus traversal skips
   *       them and Space cannot fire a button (focus has no button to land on).
   *   <li>A {@link KeyEventDispatcher} on the focus manager swallows Enter and Escape whose source
   *       belongs to this dialog. This catches the JOptionPane's window-level Escape binding and
   *       any Enter that would reach a focused button — the two paths layers (1) and (2) don't
   *       cover.
   * </ol>
   *
   * <p><b>After the window.</b> Buttons become focusable again and the dispatcher is removed, so
   * Enter/Escape resume their normal L&F behavior. Crucially, no button is auto-focused; a keyboard
   * user must explicitly Tab to a button before Space/Enter will activate it, so a still-buffered
   * keystroke from before the window expired cannot land on a button. Mouse clicks work the entire
   * time.
   *
   * <p><b>Cleanup.</b> The returned {@link Runnable} cancels the guard early and must be invoked
   * when the dialog closes, in case the dialog is dismissed (e.g. by clicking) before the timer
   * fires — otherwise the dispatcher leaks on the focus manager.
   */
  private static Runnable installKeyActivationGuard(
      final JOptionPane optionPane, final JDialog dialog) {
    // Layer 1: no default button -> Enter has no root-pane target.
    dialog.getRootPane().setDefaultButton(null);

    // Layer 2: non-focusable buttons -> focus traversal skips them, Space has nothing to fire.
    final List<JButton> buttons = findButtons(optionPane);
    buttons.forEach(button -> button.setFocusable(false));

    // Layer 3: swallow Enter/Escape at the focus-manager level for events sourced inside this
    // dialog. Catches the JOptionPane's window-level Escape binding and any focused-button Enter
    // that layers 1 and 2 don't cover.
    final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    final KeyEventDispatcher swallower =
        event -> {
          if (event.getID() != KeyEvent.KEY_PRESSED && event.getID() != KeyEvent.KEY_RELEASED) {
            return false;
          }
          final int code = event.getKeyCode();
          if (code != KeyEvent.VK_ENTER && code != KeyEvent.VK_ESCAPE) {
            return false;
          }
          if (!(event.getSource() instanceof Component component)) {
            return false;
          }
          final Window srcWindow =
              (component instanceof Window window)
                  ? window
                  : SwingUtilities.getWindowAncestor(component);
          if (srcWindow != dialog) {
            return false;
          }
          event.consume();
          return true;
        };
    focusManager.addKeyEventDispatcher(swallower);

    // After the guard window: drop all three layers. No button is auto-focused, so a keyboard
    // user must Tab in deliberately before any keystroke can activate a button.
    final Timer timer =
        new Timer(
            KEY_GUARD_WINDOW_MILLIS,
            e -> {
              buttons.forEach(button -> button.setFocusable(true));
              focusManager.removeKeyEventDispatcher(swallower);
            });
    timer.setRepeats(false);
    timer.start();

    return () -> {
      timer.stop();
      focusManager.removeKeyEventDispatcher(swallower);
    };
  }

  private static List<JButton> findButtons(final Container container) {
    final List<JButton> buttons = new ArrayList<>();
    for (final Component child : container.getComponents()) {
      if (child instanceof JButton button) {
        buttons.add(button);
      } else if (child instanceof Container nested) {
        buttons.addAll(findButtons(nested));
      }
    }
    return buttons;
  }
}
