package games.strategy.engine.lobby.server;

import java.util.logging.Logger;

import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;

public abstract class AbstractModeratorController implements IModeratorController {
  protected static final Logger s_logger = Logger.getLogger(ModeratorController.class.getName());
  protected final IServerMessenger m_serverMessenger;
  protected final Messengers m_allMessengers;

  public AbstractModeratorController(final IServerMessenger serverMessenger, final Messengers messengers) {
    m_serverMessenger = serverMessenger;
    m_allMessengers = messengers;
  }

  public static RemoteName getModeratorControllerName() {
    return new RemoteName(IModeratorController.class, "games.strategy.engine.lobby.server.ModeratorController:Global");
  }

  public void register(final IRemoteMessenger messenger) {
    messenger.registerRemote(this, getModeratorControllerName());
  }

  protected String getNodeMacAddress(final INode node) {
    return m_serverMessenger.getPlayerMac(node.getName());
  }

  protected String getRealName(final INode node) {
    // Remove any (n) that is added to distinguish duplicate names
    final String name = node.getName().split(" ")[0];
    return name;
  }

  protected String getAliasesFor(final INode node) {
    final StringBuilder builder = new StringBuilder();
    final String nodeMac = getNodeMacAddress(node);
    for (final INode cur : m_serverMessenger.getNodes()) {
      if (cur.equals(node) || cur.getName().equals("Admin")) {
        continue;
      }
      if (cur.getAddress().equals(node.getAddress()) || getNodeMacAddress(cur).equals(nodeMac)) {
        if (builder.length() > 0) {
          builder.append(", ");
        }
        builder.append(cur.getName());
      }
    }
    if (builder.length() > 100) {
      // So replace comma's to keep names within screen
      return builder.toString().replace(", ", "\r\n");
    }
    return builder.toString();
  }
}
