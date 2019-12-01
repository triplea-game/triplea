package org.triplea.http.client.lobby.game.remote.actions;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.triplea.http.client.IpAddressParser;

@ToString
@Builder
@EqualsAndHashCode
public class RemoteActions {
  private final Collection<BannedPlayer> bannedPlayers;
  private final Collection<String> serversToShutdown;

  public boolean serverIsRequestedToShutdown(final InetAddress myPublicIpAddress) {
    return serversToShutdown != null
        && serversToShutdown.stream()
            .map(IpAddressParser::fromString)
            .anyMatch(myPublicIpAddress::equals);
  }

  public Collection<BannedPlayer> getBannedPlayers() {
    return Optional.ofNullable(bannedPlayers).orElseGet(Set::of);
  }
}
