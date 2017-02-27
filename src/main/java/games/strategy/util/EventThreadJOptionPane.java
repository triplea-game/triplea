package games.strategy.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

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
  private EventThreadJOptionPane() {
    // do nothing
  }

  public static void showMessageDialog(final Component parentComponent, final Object message, final String title,
      final int messageType, final CountDownLatchHandler latchHandler) {
    showMessageDialog(parentComponent, message, title, messageType, false, latchHandler);
  }

  public static void showMessageDialog(final Component parentComponent, final Object message, final String title,
      final int messageType, final boolean useJLabel, final CountDownLatchHandler latchHandler) {
    invokeAndWait(
        latchHandler,
        () -> JOptionPane.showMessageDialog(parentComponent,
            useJLabel ? createJLabelInScrollPane((String) message) : message, title, messageType));
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
    if (latchHandler != null) {
      latchHandler.addShutdownLatch(latch);
    }
    awaitLatch(latchHandler, latch);
    return result.get();
  }

  private static JScrollPane createJLabelInScrollPane(final String message) {
    final JLabel label = new JLabel(message);
    final JScrollPane scroll = new JScrollPane(label);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
    final int availWidth = screenResolution.width - 40;
    final int availHeight = screenResolution.height - 140;
    // add 25 for scrollbars
    final int newWidth = (scroll.getPreferredSize().width > availWidth ? availWidth
        : (scroll.getPreferredSize().width + (scroll.getPreferredSize().height > availHeight ? 25 : 0)));
    final int newHeight = (scroll.getPreferredSize().height > availHeight ? availHeight
        : (scroll.getPreferredSize().height + (scroll.getPreferredSize().width > availWidth ? 25 : 0)));
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
        if (latchHandler != null) {
          latchHandler.interruptLatch(latch);
        }
      }
    }
    if (latchHandler != null) {
      latchHandler.removeShutdownLatch(latch);
    }
  }

  public static void showMessageDialog(final Component parentComponent, final Object message,
      final CountDownLatchHandler latchHandler) {
    invokeAndWait(
        latchHandler,
        () -> JOptionPane.showMessageDialog(parentComponent, message));
  }

  public static int showOptionDialog(final Component parentComponent, final Object message, final String title,
      final int optionType, final int messageType, final Icon icon, final Object[] options, final Object initialValue,
      final CountDownLatchHandler latchHandler) {
    return invokeAndWait(
        latchHandler,
        () -> JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options,
            initialValue));
  }

  private static int invokeAndWait(final CountDownLatchHandler latchHandler, final IntSupplier supplier) {
    return invokeAndWait(latchHandler, () -> Optional.of(supplier.getAsInt())).get();
  }

  public static int showConfirmDialog(final Component parentComponent, final Object message, final String title,
      final int optionType, final CountDownLatchHandler latchHandler) {
    return invokeAndWait(
        latchHandler,
        () -> JOptionPane.showConfirmDialog(parentComponent, message, title, optionType));
  }
}
