package games.strategy.engine.framework;

import games.strategy.sound.ISound;

abstract public class AbstractGameLoader implements IGameLoader {
  private static final long serialVersionUID = 5698425046660960332L;
  protected transient ISound m_soundChannel;
  protected transient IGame m_game;

  /**
   * Return an array of player types that can play on the server.
   */
  @Override
  abstract public String[] getServerPlayerTypes();

  @Override
  public void shutDown() {
    if (m_game != null && m_soundChannel != null) {
      m_game.removeSoundChannel(m_soundChannel);
      // set sound channel to null to handle the case of shutdown being called multiple times.
      // If/when shutdown is called exactly once, then the null assignment should be unnecessary.
      m_soundChannel = null;
    }
  }
}
