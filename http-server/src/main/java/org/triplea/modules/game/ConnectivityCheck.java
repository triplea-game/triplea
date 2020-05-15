package org.triplea.modules.game;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.modules.game.listing.GameListing;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
class ConnectivityCheck {

  private final Supplier<Socket> socketSupplier;
  private final GameListing gameListing;

  public enum ReverseConnectionResult {
    SUCCESS,
    FAILED,
    GAME_ID_NOT_FOUND
  }

  ConnectivityCheck(final GameListing gameListing) {
    this(Socket::new, gameListing);
  }

  /** Checks if the lobby can create a connection to a given game. */
  ReverseConnectionResult canDoReverseConnect(final ApiKey apiKey, final String gameId) {
    return gameListing
        .getHostForGame(apiKey, gameId)
        .map(this::testConnectivityToAddress)
        .map(result -> result ? ReverseConnectionResult.SUCCESS : ReverseConnectionResult.FAILED)
        .orElse(ReverseConnectionResult.GAME_ID_NOT_FOUND);
  }

  private boolean testConnectivityToAddress(final InetSocketAddress address) {
    try (Socket s = socketSupplier.get()) {
      s.connect(address, (int) TimeUnit.SECONDS.toMillis(10));
      return s.isConnected();
    } catch (final IOException e) {
      return false;
    }
  }
}
