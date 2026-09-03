package org.triplea.game.server;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;

/**
 * Standalone entrypoint so the relay can be run as its own process for a lobby-managed fleet. The
 * in-process launch path ({@code new GameRelayServer(port)}) is unaffected.
 *
 * <p>Configuration precedence (first present wins), for both port and optional bind address:
 *
 * <ol>
 *   <li>command-line arg ({@code args[0]} = port, {@code args[1]} = bind address)
 *   <li>system property ({@code -Dtriplea.relay.port}, {@code -Dtriplea.relay.bindAddress})
 *   <li>environment variable ({@code TRIPLEA_RELAY_PORT}, {@code TRIPLEA_RELAY_BIND_ADDRESS})
 *   <li>default: port {@value #DEFAULT_PORT}, bind to all interfaces
 * </ol>
 */
@Slf4j
public final class RelayServerMain {
  static final int DEFAULT_PORT = 6000;

  private static final String PORT_PROPERTY = "triplea.relay.port";
  private static final String PORT_ENV = "TRIPLEA_RELAY_PORT";
  private static final String BIND_PROPERTY = "triplea.relay.bindAddress";
  private static final String BIND_ENV = "TRIPLEA_RELAY_BIND_ADDRESS";

  private RelayServerMain() {}

  public static void main(final String[] args) throws InterruptedException {
    final int port = resolvePort(args);
    final InetSocketAddress bindAddress = resolveBindAddress(args, port);

    final GameRelayServer relayServer = new GameRelayServer(bindAddress);
    Runtime.getRuntime().addShutdownHook(new Thread(relayServer::stop));
    relayServer.start();
    log.info("Relay server running on {}. Press Ctrl-C to stop.", bindAddress);

    // Keep the process alive until it is killed (the shutdown hook stops the server cleanly).
    new CountDownLatch(1).await();
  }

  private static int resolvePort(final String[] args) {
    return firstPresent(args, 0, PORT_PROPERTY, PORT_ENV)
        .map(Integer::parseInt)
        .orElse(DEFAULT_PORT);
  }

  private static InetSocketAddress resolveBindAddress(final String[] args, final int port) {
    return firstPresent(args, 1, BIND_PROPERTY, BIND_ENV)
        .map(host -> new InetSocketAddress(host, port))
        .orElseGet(() -> new InetSocketAddress(port));
  }

  private static Optional<String> firstPresent(
      final String[] args, final int argIndex, final String property, final String env) {
    if (args.length > argIndex && !args[argIndex].isBlank()) {
      return Optional.of(args[argIndex].trim());
    }
    return Optional.ofNullable(System.getProperty(property))
        .filter(value -> !value.isBlank())
        .or(() -> Optional.ofNullable(System.getenv(env)).filter(value -> !value.isBlank()))
        .map(String::trim);
  }
}
