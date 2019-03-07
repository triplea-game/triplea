package org.triplea.lobby.common;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

/**
 * A service that provides operations to manage a headless game server (bot) connected to the lobby. Each headless game
 * server provides this service. Only the lobby server is permitted to invoke the operations of this service.
 */
public interface IRemoteHostUtils extends IRemote {
  String getConnections();

  String getChatLogHeadlessHostBot(String hashedPassword, String salt);

  String mutePlayerHeadlessHostBot(String playerNameToBeMuted, int minutes, String hashedPassword, String salt);

  String bootPlayerHeadlessHostBot(String playerNameToBeBooted, String hashedPassword, String salt);

  String banPlayerHeadlessHostBot(String playerNameToBeBanned, String hashedPassword, String salt);

  String stopGameHeadlessHostBot(String hashedPassword, String salt);

  String shutDownHeadlessHostBot(String hashedPassword, String salt);

  String getSalt();

  /**
   * Companion object for {@link IRemoteHostUtils} that provides various utility methods.
   *
   * <p>
   * <strong>NOTE:</strong> These methods cannot be members of {@link IRemoteHostUtils} directly (even if they are
   * static) because their presence may affect the RMI method ordinal calculation.
   * </p>
   */
  final class Companion {
    private Companion() {}

    public static RemoteName newRemoteNameForNode(final INode node) {
      checkNotNull(node);

      return new RemoteName(
          "games.strategy.engine.lobby.server.RemoteHostUtils:" + node.toString(),
          IRemoteHostUtils.class);
    }
  }
}
