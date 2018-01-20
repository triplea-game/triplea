package games.strategy.engine.lobby.server;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetAddress;

import javax.annotation.concurrent.Immutable;

/**
 * Information about a lobby user.
 */
@Immutable
public final class User {
  private final String hashedMacAddress;
  private final InetAddress inetAddress;
  private final String username;

  public User(final String username, final InetAddress inetAddress, final String hashedMacAddress) {
    checkNotNull(username);
    checkNotNull(inetAddress);
    checkNotNull(hashedMacAddress);

    this.hashedMacAddress = hashedMacAddress;
    this.inetAddress = inetAddress;
    this.username = username;
  }

  public String getHashedMacAddress() {
    return hashedMacAddress;
  }

  public InetAddress getInetAddress() {
    return inetAddress;
  }

  public String getUsername() {
    return username;
  }

  /**
   * Creates a copy of this user but with the specified hashed MAC address.
   */
  public User withHashedMacAddress(final String hashedMacAddress) {
    checkNotNull(hashedMacAddress);

    return new User(username, inetAddress, hashedMacAddress);
  }

  /**
   * Creates a copy of this user but with the specified username.
   */
  public User withUsername(final String username) {
    checkNotNull(username);

    return new User(username, inetAddress, hashedMacAddress);
  }
}
