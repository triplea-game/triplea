package games.strategy.net.websocket.transport;

import games.strategy.net.ILoginValidator;
import games.strategy.net.INode;
import games.strategy.net.IObjectStreamFactory;
import games.strategy.net.IServerMessenger;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;

/**
 * Websocket {@link IServerMessenger} for the game host. Like a client it dials OUT to the relay (it
 * is a transparent relay client of its own game); {@link #isServer()} is {@code true} and it
 * exposes the host-only admin API. Being the first joiner of the game-id, the relay designates it
 * host, giving it boot/ban authority. See {@link RelayMessenger} for the shared transport
 * behaviour.
 *
 * <h2>Admin API (round 1)</h2>
 *
 * Membership-derived operations are real: {@link #getNodes()} reflects relay membership and {@link
 * #removeConnection(INode)}/{@link #banPlayer} issue relay {@code BOOT}s. Auth is DEFERRED to round
 * 2: the login validator and accept-new-connections flag are stored but not yet enforced (the relay
 * admits everyone), MACs are stable synthetics derived from the node id, and the ban list is a
 * local IP set with no cross-session persistence.
 */
@Slf4j
public class WebsocketServerMessenger extends RelayMessenger implements IServerMessenger {

  @Nullable private volatile ILoginValidator loginValidator;
  private volatile boolean acceptNewConnections = true;
  private final Set<String> bannedIpAddresses = ConcurrentHashMap.newKeySet();

  public WebsocketServerMessenger(
      final URI relayUri,
      final String gameId,
      final String playerName,
      final IObjectStreamFactory objectStreamFactory)
      throws IOException {
    super(relayUri, gameId, playerName, objectStreamFactory);
  }

  @Override
  public boolean isServer() {
    return true;
  }

  @Override
  public Set<INode> getNodes() {
    return currentNodes();
  }

  @Override
  public void removeConnection(final INode node) {
    if (node.equals(getLocalNode())) {
      throw new IllegalArgumentException("Can't remove yourself!");
    }
    sendBoot(node, false);
  }

  @Override
  public void banPlayer(final String ip, final String mac) {
    bannedIpAddresses.add(ip);
    getNodes().stream()
        .filter(node -> node.getIpAddress().equals(ip))
        .findAny()
        .ifPresent(node -> sendBoot(node, true));
  }

  @Override
  public boolean isPlayerBanned(final String ip, final String mac) {
    return bannedIpAddresses.contains(ip);
  }

  @Override
  public void setAcceptNewConnections(final boolean accept) {
    // Stored only; relay-side admission control is deferred to round 2.
    acceptNewConnections = accept;
  }

  @Override
  public void setLoginValidator(final ILoginValidator loginValidator) {
    // Stored only; the relay handshake does not yet run credential validation (round 2).
    this.loginValidator = loginValidator;
  }

  @Override
  @Nullable
  public ILoginValidator getLoginValidator() {
    return loginValidator;
  }

  @Override
  @Nullable
  public String getPlayerMac(final UserName name) {
    // Stable synthetic MAC derived from the node id; no real MAC is exchanged over the relay yet.
    return getNodes().stream()
        .filter(node -> node.getName().equals(name.getValue()))
        .findAny()
        .map(this::syntheticMac)
        .orElse(null);
  }

  private String syntheticMac(final INode node) {
    final String nodeId = nodeIdOf(node);
    return nodeId == null ? null : "$relay$" + nodeId;
  }
}
