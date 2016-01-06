package games.strategy.engine.framework;

import games.strategy.sound.ISound;

abstract public class AbstractGameLoader implements IGameLoader {
  private static final long serialVersionUID = 5698425046660960332L;
  protected transient ISound soundChannel;
  protected transient IGame game;

  /**
   * Return an array of player types that can play on the server.
   */
  @Override
  abstract public String[] getServerPlayerTypes();

  @Override
  public void shutDown() {
    if (game != null && soundChannel != null) {
      game.removeSoundChannel(soundChannel);
      // set sound channel to null to handle the case of shutdown being called multiple times.
      // If/when shutdown is called exactly once, then the null assignment should be unnecessary.
      soundChannel = null;
    }
  }
}
