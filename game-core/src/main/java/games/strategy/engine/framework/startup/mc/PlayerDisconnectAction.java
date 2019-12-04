package games.strategy.engine.framework.startup.mc;

import com.google.common.base.Preconditions;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import java.net.InetAddress;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.triplea.http.client.IpAddressParser;

/** Disconnects a player identified by name and/or IP from an {@code IServerMessenger}. */
@AllArgsConstructor
public class PlayerDisconnectAction implements Consumer<InetAddress> {
  private final IServerMessenger messenger;
  private final Runnable shutdownCallback;

  @Override
  public void accept(final InetAddress bannedIp) {
    Preconditions.checkNotNull(bannedIp);
    final Set<INode> nodes = messenger.getNodes();

    if (IpAddressParser.fromString(messenger.getLocalNode().getIpAddress()).equals(bannedIp)) {
      shutdownCallback.run();
    } else {
      for (final INode node : nodes) {
        final InetAddress ipAddress = IpAddressParser.fromString(node.getIpAddress());

        if (ipAddress.equals(bannedIp)) {
          messenger.removeConnection(node);
        }
      }
    }
  }
}
