package games.strategy.engine.framework.startup.ui;

import feign.FeignException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import lombok.Builder;

/**
 * Logic of this class:<br>
 * - send a keep alive, if we get a 'false', it means the server is alive but does not have our
 * game, we need to re-post.<br>
 * - After re-post, attempt a keep-alive again, if it succeeds, we are reconnected.<br>
 * - If we get an exception after sending keep alive, then report an error to user (just once) until
 * we later re-establish a connection.<br>
 */
@Builder
class LobbyWatcherKeepAliveTask implements Runnable {
  /** The current gameId, updated if we re-post. */
  @Nonnull private String gameId;
  /** Call this after re-posting so we can update with a new game id. */
  @Nonnull private final Consumer<String> gameIdSetter;
  /** Call this to report an error message when we have lost connection. */
  @Nonnull private final Consumer<String> connectionLostReporter;
  /** Call this to report a reconnection back to server. */
  @Nonnull private final Consumer<String> connectionReEstablishedReporter;
  /** Call this to send a keep-alive request to server. */
  @Nonnull private final Predicate<String> keepAliveSender;
  /** Call this to re-post the current game, obtains a new game id. */
  @Nonnull private final Supplier<String> gamePoster;

  @Builder.Default private volatile boolean lastAttemptWasSuccess = true;

  @Override
  public void run() {
    try {
      if (keepAliveSender.test(gameId)) {
        if (!lastAttemptWasSuccess) {
          reportReconnected();
        }
      } else {
        gameId = gamePoster.get();
        if (keepAliveSender.test(gameId)) {
          gameIdSetter.accept(gameId);
          reportReconnected();
        }
      }
    } catch (final FeignException e) {
      // Only report connection lost once.
      if (lastAttemptWasSuccess) {
        connectionLostReporter.accept(
            "Connection to lobby lost.\nWill automatically re-connect when it is available.");
        lastAttemptWasSuccess = false;
      }
    }
  }

  private void reportReconnected() {
    connectionReEstablishedReporter.accept("Re-Connected to Lobby");
    lastAttemptWasSuccess = true;
  }
}
