package games.strategy.engine.lobby.server;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.logging.Logger;

import javax.annotation.Nullable;

import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.Messengers;

public abstract class AbstractModeratorController implements IModeratorController {
  protected static final Logger logger = Logger.getLogger(ModeratorController.class.getName());

  /**
   * The hashed MAC address used when the MAC address of a node is unknown. It corresponds to the MAC address
   * {@code 00:00:00:00:00:00}.
   */
  protected static final String UNKNOWN_HASHED_MAC_ADDRESS = MacFinder.getHashedMacAddress(new byte[6]);

  protected final IServerMessenger serverMessenger;
  protected final Messengers allMessengers;

  public AbstractModeratorController(final IServerMessenger serverMessenger, final Messengers messengers) {
    this.serverMessenger = serverMessenger;
    allMessengers = messengers;
  }

  public static RemoteName getModeratorControllerName() {
    return new RemoteName(IModeratorController.class, "games.strategy.engine.lobby.server.ModeratorController:Global");
  }

  public void register(final IRemoteMessenger messenger) {
    messenger.registerRemote(this, getModeratorControllerName());
  }

  /**
   * Gets the hashed MAC address of the specified node.
   *
   * @param node The node whose hashed MAC address is desired.
   *
   * @return The hashed MAC address of the specified node. If the MAC address of the node cannot be determined, this
   *         method returns {@link #UNKNOWN_HASHED_MAC_ADDRESS}.
   */
  protected final String getNodeMacAddress(final INode node) {
    checkNotNull(node);

    final @Nullable String hashedMacAddress = serverMessenger.getPlayerMac(node.getName());
    return (hashedMacAddress != null) ? hashedMacAddress : UNKNOWN_HASHED_MAC_ADDRESS;
  }

  protected String getAliasesFor(final INode node) {
    final StringBuilder builder = new StringBuilder();
    final String nodeMac = getNodeMacAddress(node);
    for (final INode cur : serverMessenger.getNodes()) {
      if (cur.equals(node) || cur.getName().equals("Admin")) {
        continue;
      }
      if (cur.getAddress().equals(node.getAddress())
          || (!UNKNOWN_HASHED_MAC_ADDRESS.equals(nodeMac) && getNodeMacAddress(cur).equals(nodeMac))) {
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
