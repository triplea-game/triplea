package games.strategy.triplea.player;

import games.strategy.triplea.ui.MainGameFrame;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate
 * using a change).
 *
 * @param <T> The type of the frame window associated with the player.
 */
public abstract class AbstractHumanPlayer<T extends MainGameFrame> extends AbstractBasePlayer {
  protected T ui;

  /**
   * @param name
   *        - the name of the player.
   */
  public AbstractHumanPlayer(final String name, final String type) {
    super(name, type);
  }

  public final void setFrame(final T frame) {
    ui = frame;
  }
}
