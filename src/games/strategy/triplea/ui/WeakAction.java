package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;

import javax.swing.AbstractAction;
import javax.swing.Action;

/**
 * Avoid holding a strong reference to the action
 * fixes a memory leak in swing.
 */
public class WeakAction extends AbstractAction {
  private static final long serialVersionUID = 8931357243476123862L;
  private final WeakReference<Action> delegate;

  WeakAction(final String name, final Action delegate) {
    super(name);
    this.delegate = new WeakReference<>(delegate);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final Action a = delegate.get();
    if (a != null) {
      a.actionPerformed(e);
    }
  }
}
