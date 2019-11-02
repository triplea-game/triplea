package org.triplea.lobby.server.db;

import games.strategy.net.MacFinder;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.java.Interruptibles;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.test.common.Integration;

/** Superclass for fixtures that test a moderator service controller. */
@Integration
public abstract class AbstractModeratorServiceControllerTestCase {
  protected final User user = newUser();
  protected final User moderator = newUser();

  /** Creates a new unique user. */
  private static User newUser() {
    final User user =
        User.builder()
            .username(newUsername())
            .inetAddress(newInetAddress())
            .systemId(newHashedMacAddress())
            .build();
    new LobbyConfiguration()
        .getDatabaseDao()
        .getUserDao()
        .createUser(
            user.getUsername(),
            "email@email.com",
            new HashedPassword(BCrypt.hashpw("pass", BCrypt.gensalt())));
    return user;
  }

  private static String newUsername() {
    return "user_" + newUniqueTimestamp();
  }

  private static String newUniqueTimestamp() {
    final long time = System.currentTimeMillis();
    while (time == System.currentTimeMillis()) {
      Interruptibles.sleep(1);
    }
    return "" + System.currentTimeMillis();
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
