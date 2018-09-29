package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

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
    dialog.open().ifPresent(frame.getMapPanel()::centerOn);
  }
}
