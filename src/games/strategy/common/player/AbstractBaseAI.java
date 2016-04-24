package games.strategy.common.player;

import games.strategy.debug.ClientLogger;
import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.util.ThreadUtil;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate
 * using a change).
 */
public abstract class AbstractBaseAI extends AbstractBasePlayer {
  /**
   * @param name
   *        - the name of the player.
   */
  public AbstractBaseAI(final String name, final String type) {
    super(name, type);
  }

  /**
   * Pause the game to allow the human player to see what is going on.
   */
  protected void pause() {
    try {
      ThreadUtil.sleep(AbstractUIContext.getAIPauseDuration());
    } catch (final Exception e) {
      ClientLogger.logError("There was an Error trying to Pause the Game", e);
    }
  }
}
