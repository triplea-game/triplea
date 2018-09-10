package org.triplea.lobby.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import games.strategy.net.MacFinder;
import games.strategy.util.Util;

/**
 * A collection of methods for creating instances of {@link User} for testing purposes.
 */
public final class TestUserUtils {
  private TestUserUtils() {}

  /**
   * Creates a new unique user.
   */
  public static User newUser() {
    return User.builder()
        .username(newUsername())
        .inetAddress(newInetAddress())
        .hashedMacAddress(newHashedMacAddress())
        .build();
  }

  private static String newUsername() {
    return "user_" + Util.createUniqueTimeStamp();
  }

  private static InetAddress newInetAddress() {
    final byte[] addr = new byte[4];
    new Random().nextBytes(addr);
    try {
      return InetAddress.getByAddress(addr);
    } catch (final UnknownHostException e) {
      throw new AssertionError("should never happen", e);
    }
  }

  private static String newHashedMacAddress() {
    final byte[] bytes = new byte[6];
    new Random().nextBytes(bytes);
    return MacFinder.getHashedMacAddress(bytes);
  }
}
