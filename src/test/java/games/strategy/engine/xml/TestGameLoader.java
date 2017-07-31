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

/**
 * Fake implementation of {@link IGameLoader} that does nothing.
 */
public final class TestGameLoader implements IGameLoader {
  private static final long serialVersionUID = -8019996788216172034L;

  @Override
  public String[] getServerPlayerTypes() {
    return null;
  }

  @Override
  public void startGame(final IGame game, final Set<IGamePlayer> players, final boolean headless) {}

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
