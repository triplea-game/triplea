package org.triplea.http.client.lobby.game.hosting.request;

import java.net.InetAddress;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.triplea.http.client.IpAddressParser;

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
    return IpAddressParser.fromString(publicVisibleIp);
  }
}
