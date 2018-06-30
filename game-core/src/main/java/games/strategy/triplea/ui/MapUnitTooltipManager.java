package games.strategy.triplea.ui;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;


/**
 * Responsible for showing tool tips when hovering over units on the main map.
 */
public class MapUnitTooltipManager implements ActionListener {
  private final JComponent parent;
  private final WindowDeactivationObserver deactivationObserver;
  private final Timer timer;
  private String text;
  private Popup popup;

  public MapUnitTooltipManager(final JComponent parent) {
    this.parent = parent;
    this.timer = new Timer(1000, this);
    this.timer.setRepeats(false);
    // Note: Timer not started yet.

    // Close tooltips when the window becomes inactive - so they don't overlap opened dialogs.
    deactivationObserver = new WindowDeactivationObserver();
  }

  private class WindowDeactivationObserver extends WindowAdapter implements AncestorListener {
    private Window window;

    public WindowDeactivationObserver() {
      // Listen to ancestor events as the component may not have a Window yet.
      parent.addAncestorListener(this);
      updateWindowObserver();
    }

    private void updateWindowObserver() {
      if (window != null) {
        window.removeWindowListener(this);
      }
      window = SwingUtilities.getWindowAncestor(parent);
      if (window != null) {
        window.addWindowListener(this);
      }
    }

    @Override
    public void windowClosed(final WindowEvent e) {
      window.removeWindowListener(this);
    }

    @Override
    public void windowDeactivated(final WindowEvent e) {
      updateTooltip("");
    }

    @Override
    public void ancestorAdded(final AncestorEvent event) {
      updateWindowObserver();
    }

    @Override
    public void ancestorRemoved(final AncestorEvent event) {
      updateWindowObserver();
    }

    @Override
    public void ancestorMoved(final AncestorEvent event) {
      updateWindowObserver();
    }
  }

  /**
   * Updates the tooltip. The tooltip will show after 1s without another update if the text is not empty.
   *
   * @param tipText The tooltip text to set or the empty string if it should be hidden.
   */
  public void updateTooltip(final String tipText) {
    if (tipText.equals(text)) {
      return;
    }

    if (popup != null) {
      popup.hide();
      popup = null;
    }
    text = tipText;

    timer.stop();
    if (text.length() > 0) {
      timer.restart();
    }
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (text.length() > 0) {
      final Point currentPoint = MouseInfo.getPointerInfo().getLocation();
      final PopupFactory popupFactory = PopupFactory.getSharedInstance();
      final JToolTip info = new JToolTip();
      info.setTipText("<html>" + text + "</html>");
      popup = popupFactory.getPopup(parent, info, currentPoint.x + 5, currentPoint.y + 5);
      popup.show();
    }
  }
}
