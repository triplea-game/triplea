package games.strategy.engine.lobby.server;

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.InetAddress;

import javax.annotation.concurrent.Immutable;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * Information about a lobby user.
 */
@Immutable
@Builder
@Value
public final class User {
  @NonNull
  private final String hashedMacAddress;
  @NonNull
  private final InetAddress inetAddress;
  @NonNull
  private final String username;

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
