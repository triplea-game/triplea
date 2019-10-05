package games.strategy.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * A UI action that shows the Find Territory dialog. This dialog allows the user to search for a
 * territory by name and highlights the selected territory on the map.
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
    new FindTerritoryDialog(frame).open();
  }
}
