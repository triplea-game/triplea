package games.strategy.engine.framework.startup.ui;

import feign.FeignException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.extern.java.Log;

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
@Log
class LobbyWatcherKeepAliveTask implements Runnable {
  /** The current gameId, updated if we re-post. */
  @Nonnull private String gameId;
  /** Call this after re-posting so we can update with a new game id. */
  @Nonnull private final Consumer<String> gameIdSetter;
  /** Call this to send a keep-alive request to server. */
  @Nonnull private final Predicate<String> keepAliveSender;
  /** Call this to re-post the current game, obtains a new game id. */
  @Nonnull private final Supplier<String> gamePoster;

  @Override
  public void run() {
    try {
      if (!keepAliveSender.test(gameId)) {
        gameId = gamePoster.get();
        if (keepAliveSender.test(gameId)) {
          gameIdSetter.accept(gameId);
        }
      }
    } catch (final FeignException e) {
      log.log(Level.INFO, "Unable to connect to lobby (lobby is shut down?)", e);
    }
  }
}
