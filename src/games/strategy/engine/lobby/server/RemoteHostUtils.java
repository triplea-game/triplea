package games.strategy.engine.lobby.server;

import java.util.Set;

import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

public class RemoteHostUtils implements IRemoteHostUtils {
  private final INode m_serverNode;
  private final IServerMessenger m_serverMessenger;

  public static final RemoteName getRemoteHostUtilsName(final INode node) {
    return new RemoteName(IRemoteHostUtils.class,
        "games.strategy.engine.lobby.server.RemoteHostUtils:" + node.toString());
  }

  public RemoteHostUtils(final INode serverNode, final IServerMessenger gameServerMessenger) {
    m_serverNode = serverNode;
    m_serverMessenger = gameServerMessenger;
  }

  @Override
  public String getConnections() {
    if (!MessageContext.getSender().equals(m_serverNode)) {
      return "Not accepted!";
    }
    if (m_serverMessenger != null) {
      final StringBuilder sb = new StringBuilder("Connected: " + m_serverMessenger.isConnected() + "\n" + "Nodes: \n");
      final Set<INode> nodes = m_serverMessenger.getNodes();
      if (nodes == null) {
        sb.append("  null\n");
      } else {
        for (final INode node : nodes) {
          sb.append("  ").append(node).append("\n");
        }
      }
      return sb.toString();
    }
    return "Not a server.";
  }

  @Override
  public String getChatLogHeadlessHostBot(final String hashedPassword, final String salt) {
    if (!MessageContext.getSender().equals(m_serverNode)) {
      return "Not accepted!";
    }
    final HeadlessGameServer instance = HeadlessGameServer.getInstance();
    if (instance == null) {
      return "Not a headless host bot!";
    }
    return instance.remoteGetChatLog(hashedPassword, salt);
  }

  @Override
  public String mutePlayerHeadlessHostBot(final String playerNameToBeMuted, final int minutes,
      final String hashedPassword, final String salt) {
    if (!MessageContext.getSender().equals(m_serverNode)) {
      return "Not accepted!";
    }
    final HeadlessGameServer instance = HeadlessGameServer.getInstance();
    if (instance == null) {
      return "Not a headless host bot!";
    }
    return instance.remoteMutePlayer(playerNameToBeMuted, minutes, hashedPassword, salt);
  }

  @Override
  public String bootPlayerHeadlessHostBot(final String playerNameToBeBooted, final String hashedPassword,
      final String salt) {
    if (!MessageContext.getSender().equals(m_serverNode)) {
      return "Not accepted!";
    }
    final HeadlessGameServer instance = HeadlessGameServer.getInstance();
    if (instance == null) {
      return "Not a headless host bot!";
    }
    return instance.remoteBootPlayer(playerNameToBeBooted, hashedPassword, salt);
  }

  @Override
  public String banPlayerHeadlessHostBot(final String playerNameToBeBanned, final int hours,
      final String hashedPassword, final String salt) {
    if (!MessageContext.getSender().equals(m_serverNode)) {
      return "Not accepted!";
    }
    final HeadlessGameServer instance = HeadlessGameServer.getInstance();
    if (instance == null) {
      return "Not a headless host bot!";
    }
    return instance.remoteBanPlayer(playerNameToBeBanned, hours, hashedPassword, salt);
  }

  @Override
  public String stopGameHeadlessHostBot(final String hashedPassword, final String salt) {
    if (!MessageContext.getSender().equals(m_serverNode)) {
      return "Not accepted!";
    }
    final HeadlessGameServer instance = HeadlessGameServer.getInstance();
    if (instance == null) {
      return "Not a headless host bot!";
    }
    return instance.remoteStopGame(hashedPassword, salt);
  }

  @Override
  public String shutDownHeadlessHostBot(final String hashedPassword, final String salt) {
    if (!MessageContext.getSender().equals(m_serverNode)) {
      return "Not accepted!";
    }
    final HeadlessGameServer instance = HeadlessGameServer.getInstance();
    if (instance == null) {
      return "Not a headless host bot!";
    }
    return instance.remoteShutdown(hashedPassword, salt);
  }

  @Override
  public String getSalt() {
    if (!MessageContext.getSender().equals(m_serverNode)) {
      return "Not accepted!";
    }
    final HeadlessGameServer instance = HeadlessGameServer.getInstance();
    if (instance == null) {
      return "Not a headless host bot!";
    }
    return instance.getSalt();
  }
}
