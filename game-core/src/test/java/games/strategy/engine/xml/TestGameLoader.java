package games.strategy.engine.xml;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;

/**
 * Fake implementation of {@link IGameLoader} that does nothing.
 *
 * <p>
 * Note that instances of this class are typically created via reflection. See the various game XML test resources.
 * </p>
 */
public final class TestGameLoader implements IGameLoader {
  private static final long serialVersionUID = -8019996788216172034L;

  @Override
  public void startGame(final IGame game, final Set<IGamePlayer> players,
      final boolean headless, final @Nullable Chat chat) {}

  @Override
  public Set<IGamePlayer> createPlayers(final Map<String, PlayerType> players) {
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
    return new IUnitFactory() {
      private static final long serialVersionUID = 201371033476236028L;

      @Override
      public Unit createUnit(final UnitType type, final PlayerID owner, final GameData data) {
        return new Unit(type, owner, data);
      }
    };
  }
}
