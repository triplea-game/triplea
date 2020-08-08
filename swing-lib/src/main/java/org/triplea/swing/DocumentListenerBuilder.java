package org.triplea.swing;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import lombok.RequiredArgsConstructor;

/**
 * Helper to create a 'DocumentListener' that will fire callback events on keypressed events.
 * Callbacks are not fired on copy/paste (insert) events.
 *
 * <p>This helper will buffer callbacks and waits a short duration after the last event before
 * firing a single callback.
 *
 * <p>Document action listeners typically only fire when a user has pressed enter, this document
 * listener should reliably fire whenever text is changed (apart from copy/paste events).
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * new DocumentListenerBuilder(() -> callbackRunnable)
 *    .attachTo(textField1, textField2)
 * }</pre>
 */
@RequiredArgsConstructor
public final class DocumentListenerBuilder {
  @VisibleForTesting public static final int CALLBACK_DELAY_MS = 100;
  private final AtomicReference<Timer> callListenerAction = new AtomicReference<>();
  private final Runnable listener;

  public void attachTo(final JTextComponent textComponent, final JTextComponent... components) {
    attachListener(textComponent);
    List.of(components).forEach(this::attachListener);
  }

  private void attachListener(final JTextComponent textComponent) {
    textComponent
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              @Override
              public void insertUpdate(final DocumentEvent e) {
                scheduleCallback();
              }

              @Override
              public void removeUpdate(final DocumentEvent e) {
                scheduleCallback();
              }

              @Override
              public void changedUpdate(final DocumentEvent e) {
                scheduleCallback();
              }
            });
  }

  private void scheduleCallback() {
    // set new timer and cancel old one if present
    final var newTimer = createNewTimer();
    final var oldTimer = callListenerAction.getAndSet(newTimer);
    Optional.ofNullable(oldTimer).ifPresent(Timer::cancel);
  }

  private Timer createNewTimer() {
    final Timer timer = new Timer();
    timer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            listener.run();
          }
        },
        CALLBACK_DELAY_MS);
    return timer;
  }
}
