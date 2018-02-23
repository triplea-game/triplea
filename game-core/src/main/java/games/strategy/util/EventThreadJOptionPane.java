package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import com.google.common.annotations.VisibleForTesting;

/**
 * Blocking JOptionPane calls that do their work in the swing event thread (to be thread safe).
 */
public final class EventThreadJOptionPane {
  private EventThreadJOptionPane() {}

  /**
   * Shows a message dialog using a {@code CountDownLatchHandler} that will release its associated latches upon
   * interruption.
   *
   * @see JOptionPane#showMessageDialog(Component, Object)
   */
  public static void showMessageDialog(final @Nullable Component parentComponent, final @Nullable Object message) {
    invokeAndWait(new CountDownLatchHandler(), () -> JOptionPane.showMessageDialog(parentComponent, message));
  }

  /**
   * Shows a message dialog using a {@code CountDownLatchHandler} that will release its associated latches upon
   * interruption.
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
   *
   * @see JOptionPane#showMessageDialog(Component, Object, String, int)
   */
  public static void showMessageDialog(
      final @Nullable Component parentComponent,
      final @Nullable Object message,
      final @Nullable String title,
      final int messageType,
      final CountDownLatchHandler latchHandler) {
    checkNotNull(latchHandler);

    invokeAndWait(latchHandler, () -> JOptionPane.showMessageDialog(parentComponent, message, title, messageType));
  }

  /**
   * Shows a message dialog using the specified {@code CountDownLatchHandler} and using a scroll pane to display the
   * message.
   *
   * @param latchHandler The handler with which to associate the latch used to await the dialog.
   *
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
        () -> JOptionPane.showMessageDialog(parentComponent, createJLabelInScrollPane(message), title, messageType));
  }

  private static void invokeAndWait(final CountDownLatchHandler latchHandler, final Runnable runnable) {
    invokeAndWait(latchHandler, () -> {
      runnable.run();
      return Optional.empty();
    });
  }

  @VisibleForTesting
  static <T> Optional<T> invokeAndWait(final CountDownLatchHandler latchHandler, final Supplier<Optional<T>> supplier) {
    if (SwingUtilities.isEventDispatchThread()) {
      return supplier.get();
    }

    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Optional<T>> result = new AtomicReference<>();
    SwingUtilities.invokeLater(() -> {
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

  private static JScrollPane createJLabelInScrollPane(final @Nullable String message) {
    final JLabel label = new JLabel(message);
    final JScrollPane scroll = new JScrollPane(label);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    final int availWidth = screenResolution.width - 40;
    final int availHeight = screenResolution.height - 140;
    // add 25 for scrollbars
    final int newWidth = ((scroll.getPreferredSize().width > availWidth) ? availWidth
        : (scroll.getPreferredSize().width + ((scroll.getPreferredSize().height > availHeight) ? 25 : 0)));
    final int newHeight = ((scroll.getPreferredSize().height > availHeight) ? availHeight
        : (scroll.getPreferredSize().height + ((scroll.getPreferredSize().width > availWidth) ? 25 : 0)));
    scroll.setPreferredSize(new Dimension(newWidth, newHeight));
    return scroll;
  }

  private static void awaitLatch(final CountDownLatchHandler latchHandler, final CountDownLatch latch) {
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
   *
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
        () -> JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options,
            initialValue));
  }

  /**
   * Shows a confirmation dialog using a {@code CountDownLatchHandler} that will release its associated latches upon
   * interruption.
   *
   * @see JOptionPane#showConfirmDialog(Component, Object, String, int)
   */
  public static int showConfirmDialog(
      final @Nullable Component parentComponent,
      final @Nullable Object message,
      final @Nullable String title,
      final int optionType) {
    return showConfirmDialog(parentComponent, message, title, optionType, new CountDownLatchHandler());
  }

  /**
   * Shows a confirmation dialog using the specified {@code CountDownLatchHandler}.
   *
   * @param latchHandler The handler with which to associate the latch used to await the dialog.
   *
   * @see JOptionPane#showConfirmDialog(Component, Object, String, int)
   */
  public static int showConfirmDialog(
      final @Nullable Component parentComponent,
      final @Nullable Object message,
      final @Nullable String title,
      final int optionType,
      final CountDownLatchHandler latchHandler) {
    checkNotNull(latchHandler);

    return invokeAndWait(
        latchHandler,
        () -> JOptionPane.showConfirmDialog(parentComponent, message, title, optionType));
  }
}
