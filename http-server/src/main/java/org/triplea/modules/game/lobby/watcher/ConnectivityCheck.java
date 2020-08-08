package org.triplea.modules.game.lobby.watcher;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * Performs a 'reverse' connection back to a game host to ensure that they can be connected to from
 * the public internet (required if others are to join their game).
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
class ConnectivityCheck {

  private final Supplier<Socket> socketSupplier;

  ConnectivityCheck() {
    this(Socket::new);
  }

  /** Checks if the lobby can create a connection to a given game. */
  boolean canDoReverseConnect(final String gameHostAddress, final int port) {
    final InetSocketAddress address = new InetSocketAddress(gameHostAddress, port);
    try (Socket s = socketSupplier.get()) {
      s.connect(address, (int) TimeUnit.SECONDS.toMillis(10));
      return s.isConnected();
    } catch (final IOException e) {
      return false;
    }
  }
}
