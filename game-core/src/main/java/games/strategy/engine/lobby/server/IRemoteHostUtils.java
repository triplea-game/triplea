package games.strategy.engine.lobby.server;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;

public interface IRemoteHostUtils extends IRemote {
  String getConnections();

  String getChatLogHeadlessHostBot(String hashedPassword, String salt);

  String mutePlayerHeadlessHostBot(String playerNameToBeMuted, int minutes, String hashedPassword, String salt);

  String bootPlayerHeadlessHostBot(String playerNameToBeBooted, String hashedPassword, String salt);

  String banPlayerHeadlessHostBot(String playerNameToBeBanned, int hours, String hashedPassword, String salt);

  String stopGameHeadlessHostBot(String hashedPassword, String salt);

  String shutDownHeadlessHostBot(String hashedPassword, String salt);

  String getSalt();

  static RemoteName newRemoteNameForNode(final INode node) {
    checkNotNull(node);

    return new RemoteName(
        "games.strategy.engine.lobby.server.RemoteHostUtils:" + node.toString(),
        IRemoteHostUtils.class);
  }
}
