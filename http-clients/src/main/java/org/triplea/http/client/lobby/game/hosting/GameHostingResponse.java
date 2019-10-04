package org.triplea.http.client.lobby.game.hosting;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
@Getter
@EqualsAndHashCode
public class GameHostingResponse {

  /** Server responds with IP address that it sees. */
  private final String publicVisibleIp;

  /**
   * Server grants the host an API key for future connections. Game hosts do not necessarily go
   * through the lobby login process and need an API key.
   */
  private final String apiKey;

  public InetAddress getPublicVisibleIp() {
    try {
      return InetAddress.getByName(publicVisibleIp);
    } catch (final UnknownHostException e) {
      throw new IllegalStateException(
          "Our IP address reported by lobby server is invalid: " + publicVisibleIp);
    }
  }
}
