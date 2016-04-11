package games.strategy.common.player;

import games.strategy.common.ui.MainGameFrame;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate
 * using a change).
 */
public abstract class AbstractHumanPlayer<CustomGameFrame extends MainGameFrame> extends AbstractBasePlayer {
  protected CustomGameFrame ui;

  /**
   * @param name
   *        - the name of the player.
   */
  public AbstractHumanPlayer(final String name, final String type) {
    super(name, type);
  }

  public final void setFrame(final CustomGameFrame frame) {
    ui = frame;
  }
}
