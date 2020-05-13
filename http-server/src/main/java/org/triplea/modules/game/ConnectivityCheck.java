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

  ConnectivityCheck(final GameListing gameListing) {
    this(Socket::new, gameListing);
  }

  /** Verifies if a game exists with a given API key and gameId. */
  boolean gameExists(final ApiKey apiKey, final String gameId) {
    return gameListing.getHostForGame(apiKey, gameId).isPresent();
  }

  /** Checks if the lobby can create a connection to a given game. */
  boolean canDoReverseConnect(final ApiKey apiKey, final String gameId) {
    return gameListing
        .getHostForGame(apiKey, gameId)
        .map(this::testConnectivityToAddress)
        .orElse(false);
  }

  private boolean testConnectivityToAddress(final InetSocketAddress address) {
    try (Socket s = socketSupplier.get()) {
      s.connect(address, (int) TimeUnit.SECONDS.toMillis(10));
      return true;
    } catch (final IOException e) {
      return false;
    }
  }
}
