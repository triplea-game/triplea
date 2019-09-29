package org.triplea.lobby.server;

import games.strategy.engine.lobby.PlayerName;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.Messengers;
import java.time.Instant;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.lobby.common.IModeratorController;
import org.triplea.lobby.common.IRemoteHostUtils;
import org.triplea.lobby.server.db.DatabaseDao;
import org.triplea.lobby.server.db.dao.ModeratorAuditHistoryDao;

@Log
@AllArgsConstructor
final class ModeratorController implements IModeratorController {
  /**
   * The hashed MAC address used when the MAC address of a node is unknown. It corresponds to the
   * MAC address {@code 00:00:00:00:00:00}.
   */
  private static final String UNKNOWN_HASHED_MAC_ADDRESS =
      MacFinder.getHashedMacAddress(new byte[6]);

  private final IServerMessenger serverMessenger;
  private final Messengers messengers;
  private final DatabaseDao database;

  @Override
  public boolean isAdmin() {
    return isPlayerAdmin(MessageContext.getSender());
  }

  @Override
  public boolean isPlayerAdmin(final INode node) {
    final User user = getUserForNode(node);
    return database.getUserDao().isAdmin(user.getUsername());
  }

  private User getUserForNode(final INode node) {
    return User.builder()
        .username(IServerMessenger.getRealName(node.getName()))
        .inetAddress(node.getAddress())
        .hashedMacAddress(getNodeMacAddress(node.getPlayerName()))
        .build();
  }

  /**
   * Gets the hashed MAC address of the specified node.
   *
   * @param playerName The playerName whose hashed MAC address is desired.
   * @return The hashed MAC address of the specified node. If the MAC address of the node cannot be
   *     determined, this method returns {@link #UNKNOWN_HASHED_MAC_ADDRESS}.
   */
  private String getNodeMacAddress(final PlayerName playerName) {
    final @Nullable String hashedMacAddress = serverMessenger.getPlayerMac(playerName);
    return hashedMacAddress != null ? hashedMacAddress : UNKNOWN_HASHED_MAC_ADDRESS;
  }

  @Override
  public void banUser(final PlayerName playerName, final @Nullable Instant banExpires) {
    assertUserIsAdmin();

    final INode node =
        findNodeByPlayerName(playerName)
            .orElseThrow(() -> new IllegalStateException("Could not find player: " + playerName));

    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }
    final String hashedMac = getNodeMacAddress(playerName);

    final User bannedUser = getUserForNode(node).withHashedMacAddress(hashedMac);
    final User moderator = getUserForNode(MessageContext.getSender());
    database.getBannedMacDao().banUser(bannedUser, banExpires, moderator);
  }

  private Optional<INode> findNodeByPlayerName(final PlayerName playerName) {
    return serverMessenger.getNodes().stream()
        .filter(n -> n.getPlayerName().equals(playerName))
        .findAny();
  }

  private void assertUserIsAdmin() {
    if (!isAdmin()) {
      throw new IllegalStateException("Not an admin");
    }
  }

  @Override
  public void boot(final PlayerName playerName) {
    assertUserIsAdmin();
    // You can't boot the server node
    if (serverMessenger.getServerNode().getPlayerName().equals(playerName)) {
      throw new IllegalStateException("Cannot boot server node");
    }
    final INode modNode = MessageContext.getSender();

    findNodeByPlayerName(playerName)
        .ifPresent(
            node -> {
              serverMessenger.removeConnection(node);
              database
                  .getModeratorAuditHistoryDao()
                  .addAuditRecord(
                      ModeratorAuditHistoryDao.AuditArgs.builder()
                          .moderatorUserId(
                              database
                                  .getUserJdbiDao()
                                  .lookupUserIdByName(modNode.getName())
                                  .orElseThrow(
                                      () ->
                                          new IllegalStateException(
                                              "Failed to find user: " + modNode.getName())))
                          .actionName(ModeratorAuditHistoryDao.AuditAction.BOOT_USER_FROM_LOBBY)
                          .actionTarget(node.getPlayerName().getValue())
                          .build());
            });
  }

  @Override
  public String getHeadlessHostBotSalt(final INode node) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    return getRemoteHostUtilsForNode(node).getSalt();
  }

  private IRemoteHostUtils getRemoteHostUtilsForNode(final INode node) {
    final RemoteName remoteName = IRemoteHostUtils.Companion.newRemoteNameForNode(node);
    return (IRemoteHostUtils) messengers.getRemote(remoteName);
  }

  @Override
  public String getChatLogHeadlessHostBot(
      final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    return getRemoteHostUtilsForNode(node).getChatLogHeadlessHostBot(hashedPassword, salt);
  }

  @Override
  public String bootPlayerHeadlessHostBot(
      final INode node,
      final String playerNameToBeBooted,
      final String hashedPassword,
      final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    final String response =
        remoteHostUtils.bootPlayerHeadlessHostBot(playerNameToBeBooted, hashedPassword, salt);
    database
        .getModeratorAuditHistoryDao()
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(
                    database
                        .getUserJdbiDao()
                        .lookupUserIdByName(modNode.getName())
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Failed to find user: " + modNode.getName())))
                .actionName(ModeratorAuditHistoryDao.AuditAction.BOOT_USER_FROM_BOT)
                .actionTarget(node.getName())
                .build());

    return response;
  }

  @Override
  public String banPlayerHeadlessHostBot(
      final INode node,
      final String playerNameToBeBanned,
      final String hashedPassword,
      final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    final String response =
        remoteHostUtils.banPlayerHeadlessHostBot(playerNameToBeBanned, hashedPassword, salt);

    database
        .getModeratorAuditHistoryDao()
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(
                    database
                        .getUserJdbiDao()
                        .lookupUserIdByName(modNode.getName())
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Failed to find user: " + modNode.getName())))
                .actionName(ModeratorAuditHistoryDao.AuditAction.BAN_PLAYER_FROM_BOT)
                .actionTarget(node.getName())
                .build());

    return response;
  }

  @Override
  public String stopGameHeadlessHostBot(
      final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    return getRemoteHostUtilsForNode(node).stopGameHeadlessHostBot(hashedPassword, salt);
  }

  @Override
  public String shutDownHeadlessHostBot(
      final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot shutdown server node");
    }
    return getRemoteHostUtilsForNode(node).shutDownHeadlessHostBot(hashedPassword, salt);
  }

  @Override
  public String getInformationOn(final INode node) {
    assertUserIsAdmin();
    final String mac = getNodeMacAddress(node.getPlayerName());
    final StringBuilder builder = new StringBuilder();
    builder.append("Name: ").append(node.getName());
    builder.append("\r\nHost Name: ").append(node.getAddress().getHostName());
    builder.append("\r\nIP Address: ").append(node.getAddress().getHostAddress());
    builder.append("\r\nPort: ").append(node.getPort());
    builder.append("\r\nHashed Mac: ");
    if (UNKNOWN_HASHED_MAC_ADDRESS.equals(mac)) {
      builder.append("(Unknown)");
    } else {
      builder.append(mac);
      if (!MacFinder.isValidHashedMacAddress(mac)) {
        builder.append(" (Invalid)");
      }
    }
    builder.append("\r\nAliases: ").append(getAliasesFor(node));
    return builder.toString();
  }

  private String getAliasesFor(final INode node) {
    final StringBuilder builder = new StringBuilder();
    final String nodeMac = getNodeMacAddress(node.getPlayerName());
    for (final INode currentNode : serverMessenger.getNodes()) {
      if (currentNode.equals(node) || currentNode.getName().equals("Admin")) {
        continue;
      }
      if (currentNode.getAddress().equals(node.getAddress())
          || (!UNKNOWN_HASHED_MAC_ADDRESS.equals(nodeMac)
              && getNodeMacAddress(currentNode.getPlayerName()).equals(nodeMac))) {
        if (builder.length() > 0) {
          builder.append(", ");
        }
        builder.append(currentNode.getName());
      }
    }
    if (builder.length() > 100) {
      // So replace comma's to keep names within screen
      return builder.toString().replace(", ", "\r\n");
    }
    return builder.toString();
  }

  @Override
  public String getHostConnections(final INode node) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    return getRemoteHostUtilsForNode(node).getConnections();
  }

  void register(final IRemoteMessenger messenger) {
    messenger.registerRemote(this, REMOTE_NAME);
  }
}
