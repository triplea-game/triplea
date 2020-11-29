package games.strategy.triplea;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.player.Player;
import games.strategy.triplea.delegate.EditDelegate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Default implementation of {@link IGameLoader}.
 *
 * <p>TODO: As there are no longer different game loader specializations, this class should be
 * renamed to {@code DefaultGameLoader} and moved to the {@code g.s.engine.data} package.
 */
public class TripleA implements IGameLoader {
  private static final long serialVersionUID = -8374315848374732436L;

  protected transient IGame game;

  @Override
  public Set<Player> newPlayers(final Map<String, PlayerType> playerNames) {
    return playerNames.entrySet().stream().map(TripleA::toGamePlayer).collect(Collectors.toSet());
  }

  private static Player toGamePlayer(final Map.Entry<String, PlayerType> namePlayerType) {
    return namePlayerType.getValue().newPlayerWithName(namePlayerType.getKey());
  }

  @Override
  public void shutDown() {
    if (game != null) {
      game.setSoundChannel(null);
      game.setDisplay(null);
    }
  }

  @Override
  public void startGame(
      final IGame game,
      final Set<Player> players,
      final LaunchAction launchAction,
      @Nullable final Chat chat) {
    this.game = game;
    if (game.getData().getDelegate("edit") == null) {
      // An evil hack: instead of modifying the XML, force an EditDelegate by adding one here
      final EditDelegate delegate = new EditDelegate();
      delegate.initialize("edit", "edit");
      game.getData().addDelegate(delegate);
      if (game instanceof ServerGame) {
        ((ServerGame) game).addDelegateMessenger(delegate);
      }
    }

    final LocalPlayers localPlayers = new LocalPlayers(players);
    game.setDisplay(launchAction.startGame(localPlayers, game, players, chat));
    game.setSoundChannel(launchAction.getSoundChannel(localPlayers));
  }
}
