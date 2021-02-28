package games.strategy.engine.framework.startup.mc;

import com.google.common.base.Preconditions;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import java.net.InetAddress;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.java.IpAddressParser;

/** Disconnects a player identified by name and/or IP from an {@code IServerMessenger}. */
@AllArgsConstructor
public class PlayerDisconnectAction implements Consumer<InetAddress> {
  private final IServerMessenger messenger;
  private final Runnable shutdownCallback;

  @Override
  public void accept(final InetAddress bannedIp) {
    Preconditions.checkNotNull(bannedIp);
    if (isGameHostBeingBanned(bannedIp)) {
      shutdownCallback.run();
    } else {
      findNodesWithIp(bannedIp).forEach(messenger::removeConnection);
    }
  }

  private boolean isGameHostBeingBanned(final InetAddress bannedIp) {
    return IpAddressParser.fromString(messenger.getLocalNode().getIpAddress()).equals(bannedIp);
  }

  private Collection<INode> findNodesWithIp(final InetAddress bannedIp) {
    return messenger.getNodes().stream()
        .filter(node -> IpAddressParser.fromString(node.getIpAddress()).equals(bannedIp))
        .collect(Collectors.toSet());
  }
}
