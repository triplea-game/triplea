package games.strategy.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

/**
 * Utility to check that code that should be running in the swing event thread runs in the swing event thread.
 */
public class SwingEventThreadGoon {
  private static boolean s_isInitialized;

  public static synchronized void initialize() {
    if (s_isInitialized) {
      return;
    }
    s_isInitialized = true;
    final RepaintManager manager = new RepaintManager() {
      /**
       * We are updating a portion of the screen, check to see if we are in the right thread.
       */
      @Override
      public void addDirtyRegion(final JComponent c, final int x, final int y, final int w, final int h) {
        // if a component is not displayable, then we can modify
        // if outside the swing event thread.
        if (isComponentDisplayable(c)) {
          if (!SwingUtilities.isEventDispatchThread()) {
            Thread.dumpStack();
          }
        }
        super.addDirtyRegion(c, x, y, w, h);
      }
    };
    // update the repaint manager
    RepaintManager.setCurrentManager(manager);
  }

  /**
   * Once a component is added to a displayed heirarchy, all changes to the component
   * should be done in the swing event thread.
   * Check the component (and its parent recursivly) till you get to the top
   * window (or null). If the window is displayable, then the component is displayable.
   */
  private static boolean isComponentDisplayable(final Component c) {
    // we are not visible
    if (!c.isVisible()) {
      return false;
    }
    // if we have no parent, we are not part of a display hierarchy, and
    // we are not displayable
    final Container parent = c.getParent();
    if (parent == null) {
      return false;
    }
    // is our window displayable?
    if (parent instanceof Window) {
      final Window w = (Window) parent;
      return w.isDisplayable();
    }
    // recursivly check our parent
    return isComponentDisplayable(parent);
  }
}
