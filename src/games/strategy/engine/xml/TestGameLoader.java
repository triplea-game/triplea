package games.strategy.engine.xml;

import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

public class TestGameLoader implements IGameLoader {
  private static final long serialVersionUID = -8019996788216172034L;

  /**
   * Return an array of player types that can play on the server.
   * This array must not contain any entries that could play on the client.
   */
  @Override
  public String[] getServerPlayerTypes() {
    return null;
  }

  /**
   * Return an array of player types that can play on the client.
   * This array must not contain any entries that could play on the server.
   */
  public String[] getClientPlayerTypes() {
    return null;
  }

  /**
   * The game is about to start.
   */
  @Override
  public void startGame(final IGame game, final Set<IGamePlayer> players, final boolean headless, Runnable exceptionAction) {}

  /**
   * Create the players. Given a map of playerName -> type,
   * where type is one of the Strings returned by a get*PlayerType() method.
   */
  @Override
  public Set<IGamePlayer> createPlayers(final Map<String, String> players) {
    return null;
  }

  @Override
  public Class<? extends IChannelSubscribor> getDisplayType() {
    return IChannelSubscribor.class;
  }

  @Override
  public Class<? extends IChannelSubscribor> getSoundType() {
    return IChannelSubscribor.class;
  }

  @Override
  public Class<? extends IRemote> getRemotePlayerType() {
    return IRemote.class;
  }

  @Override
  public void shutDown() {}

  @Override
  public IUnitFactory getUnitFactory() {
    return new DefaultUnitFactory();
  }
}
