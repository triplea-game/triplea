package org.triplea.lobby.server;

import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.Messengers;
import java.time.Instant;
import java.util.Date;
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
  public void addUsernameToBlacklist(final String name) {
    assertUserIsAdmin();

    // TODO: The User object here probably is not needed, can be simplified as we just need
    // moderator name.
    final User moderator = getUserForNode(MessageContext.getSender());
    database.getUsernameBlacklistDao().addName(name, moderator.getUsername());
    log.info(
        String.format("User name was blacklisted: %s, by: %sj", name, moderator.getUsername()));
  }

  private void assertUserIsAdmin() {
    if (!isAdmin()) {
      throw new IllegalStateException("Not an admin");
    }
  }

  @Override
  public boolean isAdmin() {
    final INode node = MessageContext.getSender();
    return isPlayerAdmin(node);
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
        .hashedMacAddress(getNodeMacAddress(node))
        .build();
  }

  /**
   * Gets the hashed MAC address of the specified node.
   *
   * @param node The node whose hashed MAC address is desired.
   * @return The hashed MAC address of the specified node. If the MAC address of the node cannot be
   *     determined, this method returns {@link #UNKNOWN_HASHED_MAC_ADDRESS}.
   */
  private String getNodeMacAddress(final INode node) {
    final @Nullable String hashedMacAddress = serverMessenger.getPlayerMac(node.getName());
    return hashedMacAddress != null ? hashedMacAddress : UNKNOWN_HASHED_MAC_ADDRESS;
  }

  @Override
  public void banMac(final INode node, final @Nullable Date banExpires) {
    banMac(node, getNodeMacAddress(node), banExpires);
  }

  @Override
  public void banMac(final INode node, final String hashedMac, final @Nullable Date banExpires) {
    banMac(node, hashedMac, banExpires != null ? banExpires.toInstant() : null);
  }

  private void banMac(
      final INode node, final String hashedMac, final @Nullable Instant banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }

    final User bannedUser = getUserForNode(node).withHashedMacAddress(hashedMac);
    final User moderator = getUserForNode(MessageContext.getSender());
    database.getBannedMacDao().addBannedMac(bannedUser, banExpires, moderator);
    log.info(
        String.format(
            "User was banned from the lobby (by MAC); "
                + "Username: %s, IP: %s, "
                + "MAC: %s, "
                + "Mod Username: %s, "
                + "Mod IP: %s, "
                + "Mod MAC: %s, "
                + "Expires: %s",
            bannedUser.getUsername(),
            bannedUser.getInetAddress().getHostAddress(),
            bannedUser.getHashedMacAddress(),
            moderator.getUsername(),
            moderator.getInetAddress().getHostAddress(),
            moderator.getHashedMacAddress(),
            banExpires == null ? "forever" : banExpires.toString()));
  }

  @Override
  public void boot(final INode node) {
    assertUserIsAdmin();
    // You can't boot the server node
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot boot server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    serverMessenger.removeConnection(node);
    log.info(
        String.format(
            "User was booted from the lobby. "
                + "Username: %s, "
                + "IP: %s, "
                + "Mac: %s "
                + "Mod Username: %s. "
                + "Mod IP: %s, "
                + "Mod Mac: %s",
            node.getName(),
            node.getAddress().getHostAddress(),
            mac,
            modNode.getName(),
            modNode.getAddress().getHostAddress(),
            getNodeMacAddress(modNode)));
    database
        .getModeratorAuditHistoryDao()
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(
                    database
                        .getUserLookupDao()
                        .lookupUserIdByName(modNode.getName())
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Failed to find user: " + modNode.getName())))
                .actionName(ModeratorAuditHistoryDao.AuditAction.BOOT_USER_FROM_LOBBY)
                .actionTarget(node.getName())
                .build());
  }

  @Override
  public String getHeadlessHostBotSalt(final INode node) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    log.info(
        String.format(
            "Getting salt for Headless HostBot. "
                + "Host: %s, "
                + "IP: %s, "
                + "Mac: %s, "
                + "Mod Username: %s, "
                + "Mod IP: %s, "
                + "Mod Mac: %s",
            node.getName(),
            node.getAddress().getHostAddress(),
            mac,
            modNode.getName(),
            modNode.getAddress().getHostAddress(),
            getNodeMacAddress(modNode)));
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    return remoteHostUtils.getSalt();
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
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    final String response = remoteHostUtils.getChatLogHeadlessHostBot(hashedPassword, salt);
    log.info(
        String.format(
            ((response == null || response.equals("Invalid password!")) ? "Failed" : "Successful")
                + " Remote get Chat Log of Headless HostBot. "
                + "Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
            node.getName(),
            node.getAddress().getHostAddress(),
            mac,
            modNode.getName(),
            modNode.getAddress().getHostAddress(),
            getNodeMacAddress(modNode)));
    return response;
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
    final String mac = getNodeMacAddress(node);
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    final String response =
        remoteHostUtils.bootPlayerHeadlessHostBot(playerNameToBeBooted, hashedPassword, salt);
    log.info(
        String.format(
            (response == null ? "Successful" : "Failed (" + response + ")")
                + " Remote Boot of "
                + playerNameToBeBooted
                + " In Headless HostBot. "
                + "Host: %s, "
                + "IP: %s, "
                + "Mac: %s, "
                + "Mod Username: %s, "
                + "Mod IP: %s, "
                + "Mod Mac: %s",
            node.getName(),
            node.getAddress().getHostAddress(),
            mac,
            modNode.getName(),
            modNode.getAddress().getHostAddress(),
            getNodeMacAddress(modNode)));

    database
        .getModeratorAuditHistoryDao()
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(
                    database
                        .getUserLookupDao()
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
    final String mac = getNodeMacAddress(node);
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    final String response =
        remoteHostUtils.banPlayerHeadlessHostBot(playerNameToBeBanned, hashedPassword, salt);
    log.info(
        String.format(
            (response == null ? "Successful" : "Failed (" + response + ")")
                + " Remote Ban of "
                + playerNameToBeBanned
                + "' Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
            node.getName(),
            node.getAddress().getHostAddress(),
            mac,
            modNode.getName(),
            modNode.getAddress().getHostAddress(),
            getNodeMacAddress(modNode)));

    database
        .getModeratorAuditHistoryDao()
        .addAuditRecord(
            ModeratorAuditHistoryDao.AuditArgs.builder()
                .moderatorUserId(
                    database
                        .getUserLookupDao()
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
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    final String response = remoteHostUtils.stopGameHeadlessHostBot(hashedPassword, salt);
    log.info(
        String.format(
            (response == null ? "Successful" : "Failed (" + response + ")")
                + " Remote Stop game of Headless HostBot. "
                + "Host: %s, "
                + "IP: %s, "
                + "Mac: %s, "
                + "Mod Username: %s, "
                + "Mod IP: %s, "
                + "Mod Mac: %s",
            node.getName(),
            node.getAddress().getHostAddress(),
            mac,
            modNode.getName(),
            modNode.getAddress().getHostAddress(),
            getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String shutDownHeadlessHostBot(
      final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot shutdown server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    log.info(
        String.format(
            "Started Remote Shutdown of Headless HostBot. "
                + "Host: %s, "
                + "IP: %s, "
                + "Mac: %s, "
                + "Mod Username: %s, "
                + "Mod IP: %s, "
                + "Mod Mac: %s",
            node.getName(),
            node.getAddress().getHostAddress(),
            mac,
            modNode.getName(),
            modNode.getAddress().getHostAddress(),
            getNodeMacAddress(modNode)));
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    final String response = remoteHostUtils.shutDownHeadlessHostBot(hashedPassword, salt);
    log.info(
        String.format(
            (response == null ? "Successful" : "Failed (" + response + ")")
                + " Remote Shutdown of Headless HostBot. "
                + "Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
            node.getName(),
            node.getAddress().getHostAddress(),
            mac,
            modNode.getName(),
            modNode.getAddress().getHostAddress(),
            getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String getInformationOn(final INode node) {
    assertUserIsAdmin();
    final String mac = getNodeMacAddress(node);
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
    final String nodeMac = getNodeMacAddress(node);
    for (final INode cur : serverMessenger.getNodes()) {
      if (cur.equals(node) || cur.getName().equals("Admin")) {
        continue;
      }
      if (cur.getAddress().equals(node.getAddress())
          || (!UNKNOWN_HASHED_MAC_ADDRESS.equals(nodeMac)
              && getNodeMacAddress(cur).equals(nodeMac))) {
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

  @Override
  public String getHostConnections(final INode node) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final IRemoteHostUtils remoteHostUtils = getRemoteHostUtilsForNode(node);
    return remoteHostUtils.getConnections();
  }

  void register(final IRemoteMessenger messenger) {
    messenger.registerRemote(this, REMOTE_NAME);
  }
}
