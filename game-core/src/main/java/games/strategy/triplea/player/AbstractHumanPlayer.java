package games.strategy.triplea.player;

import games.strategy.triplea.ui.TripleAFrame;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done
 * through an IDelegate using a change).
 */
public abstract class AbstractHumanPlayer extends AbstractBasePlayer {
  protected TripleAFrame ui;

  public AbstractHumanPlayer(final String name) {
    super(name);
  }

  public final void setFrame(final TripleAFrame frame) {
    ui = frame;
  }
}
