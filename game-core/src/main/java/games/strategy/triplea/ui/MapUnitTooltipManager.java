package games.strategy.triplea.ui;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;


/**
 * Responsible for showing tool tips when hovering over units on the main map.
 */
public class MapUnitTooltipManager implements ActionListener {
  private final MapPanel mapPanel;
  private Timer timer;
  private String text;
  private Popup popup;

  public MapUnitTooltipManager(MapPanel mapPanel) {
    this.mapPanel = mapPanel;
    this.timer = new Timer(1000, this);
    this.timer.setRepeats(false);
    // Note: Timer not started yet.
  }

  /** Updates the tooltip. */
  public void updateTooltip(String tipText) {
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
  public void actionPerformed(ActionEvent e) {
    if (text.length() > 0) {
      final Point currentPoint = MouseInfo.getPointerInfo().getLocation();
      final PopupFactory popupFactory = PopupFactory.getSharedInstance();
      final JToolTip info = new JToolTip();
      info.setTipText("<html>" + text + "</html>");
      popup = popupFactory.getPopup(mapPanel, info, currentPoint.x + 5, currentPoint.y + 5);
      popup.show();
    }
  }
}
