package org.triplea.modules.game;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
class ConnectivityCheck implements Predicate<InetSocketAddress> {
  private final Supplier<Socket> socketSupplier;

  ConnectivityCheck() {
    this(Socket::new);
  }

  @Override
  public boolean test(final InetSocketAddress address) {
    try (Socket s = socketSupplier.get()) {
      s.connect(address, (int) TimeUnit.SECONDS.toMillis(10));
      return true;
    } catch (final IOException e) {
      return false;
    }
  }
}
