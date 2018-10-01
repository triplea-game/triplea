package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Timer;

import games.strategy.engine.data.Territory;

/**
 * A UI action that prompts the user to select a territory by name and then centers the map on the selected territory.
 * If no territory is selected, this action does nothing.
 */
public final class FindTerritoryAction extends AbstractAction {
  private static final long serialVersionUID = 5122180892200519366L;

  private final TripleAFrame frame;

  public FindTerritoryAction(final TripleAFrame frame) {
    super("Find Territory...");

    this.frame = checkNotNull(frame);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final SelectTerritoryDialog dialog = new SelectTerritoryDialog(
        frame,
        "Find Territory",
        frame.getGame().getData().getMap().getTerritories());
    dialog.open().ifPresent(this::showTerritory);
  }

  private void showTerritory(final Territory territory) {
    final MapPanel mapPanel = frame.getMapPanel();
    mapPanel.centerOn(territory);
    TerritoryBorderAnimator.run(mapPanel, territory);
  }

  private static final class TerritoryBorderAnimator implements ActionListener {
    private static final int FRAME_DELAY_IN_MS = 500;
    private static final int TOTAL_FRAMES = 10;

    private int frame;
    private final MapPanel mapPanel;
    private final Territory territory;

    private TerritoryBorderAnimator(final MapPanel mapPanel, final Territory territory) {
      this.mapPanel = mapPanel;
      this.territory = territory;
    }

    static void run(final MapPanel mapPanel, final Territory territory) {
      new Timer(FRAME_DELAY_IN_MS, new TerritoryBorderAnimator(mapPanel, territory)).start();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if ((frame % 2) == 0) {
        mapPanel.setTerritoryOverlayForBorder(territory, Color.WHITE);
      } else {
        mapPanel.clearTerritoryOverlay(territory);
      }
      mapPanel.paintImmediately(mapPanel.getBounds());

      if (++frame >= TOTAL_FRAMES) {
        ((Timer) e.getSource()).stop();
      }
    }
  }
}
