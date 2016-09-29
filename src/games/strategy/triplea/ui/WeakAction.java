package games.strategy.triplea.ui;

import java.lang.ref.WeakReference;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Avoid holding a strong reference to the action
 * fixes a memory leak in swing.
 * @deprecated remove! No longer using swing
 */
@Deprecated
public class WeakAction implements EventHandler<ActionEvent> {
  private final WeakReference<EventHandler<ActionEvent>> delegate;

  WeakAction(final String name, final EventHandler<ActionEvent> delegate) {
    this.delegate = new WeakReference<>(delegate);
  }

  @Override
  public void handle(ActionEvent event) {
    final EventHandler<ActionEvent> a = delegate.get();
    if (a != null) {
      a.handle(event);
    }
  }
}
