package games.strategy.engine.framework.startup.ui;

import feign.FeignException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingResponse;

/**
 * Logic of this class:<br>
 * - send a keep alive:<br>
 * -- if we get a 'true', server has our game id, our game is still posted, all is good.<br>
 * -- if we get a 'false', it means the server is alive but does not have our game, we need to
 * re-post.<br>
 * - After re-post, attempt a keep-alive again, if it succeeds, we are reconnected.<br>
 * - If we get an exception after sending keep alive, report a lobby disconnect to user (just once)
 * and keep attempting 'keep-alive' requests until the server responds.
 *
 * <p>In other words, sending a keep alive (which takes a game ID parameter) is similar to asking
 * the server "do you have this game?"
 */
@Builder
@Slf4j
class LobbyWatcherKeepAliveTask implements Runnable {
  /** The current gameId, updated if we re-post. */
  @Nonnull private String gameId;
  /** Call this after re-posting so we can update with a new game id. */
  @Nonnull private final Consumer<String> gameIdSetter;
  /** Call this to send a keep-alive request to server. */
  @Nonnull private final Predicate<String> keepAliveSender;
  /** Call this to re-post the current game, obtains a new game id. */
  @Nonnull private final Supplier<GamePostingResponse> gamePoster;

  @Override
  public void run() {
    try {
      if (!keepAliveSender.test(gameId)) {
        repostGame();
      }
    } catch (final FeignException e) {
      log.info("Unable to connect to lobby (lobby is shut down?)", e);
    }
  }

  private void repostGame() {
    final GamePostingResponse gamePostingResponse = gamePoster.get();
    if (gamePostingResponse.isConnectivityCheckSucceeded()) {
      gameId = gamePostingResponse.getGameId();

      // check if our new game is showing as alive
      if (keepAliveSender.test(gameId)) {
        gameIdSetter.accept(gameId);
      }
    } else {
      messageConnectivityCheckFails();
    }
  }

  private void messageConnectivityCheckFails() {
    log.error(
        "Failed to re-post game back to the lobby, connectivity check to your host "
            + "failed. This is unexpected and means your host is no longer reachable from "
            + "the public internet, your game is no longer listed on the lobby.");
  }
}
