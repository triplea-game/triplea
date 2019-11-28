package org.triplea.game.server.ban.check;

import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.PlayerName;

/** Disconnects a player identified by name and/or IP from an {@code IServerMessenger}. */
@AllArgsConstructor
public class PlayerDisconnectAction implements BiConsumer<PlayerName, String> {
  private final IServerMessenger messenger;

  @Override
  public void accept(final PlayerName playerName, final String ip) {
    final Set<INode> nodes = messenger.getNodes();

    for (final INode node : nodes) {
      final PlayerName realName = PlayerName.of(IServerMessenger.getRealName(node.getName()));
      final String ipAddress = node.getIpAddress();

      if (realName.equals(playerName) || ipAddress.equals(ip)) {
        messenger.removeConnection(node);
      }
    }
  }
}
