package games.strategy.engine.lobby.server;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetAddress;

/**
 * Information about the moderator that performed a lobby maintenance operation.
 */
public final class Moderator {
  private final String hashedMacAddress;
  private final InetAddress inetAddress;
  private final String username;

  public Moderator(final String username, final InetAddress inetAddress, final String hashedMacAddress) {
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
}
