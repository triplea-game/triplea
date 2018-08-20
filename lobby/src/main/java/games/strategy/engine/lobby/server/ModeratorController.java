package games.strategy.engine.lobby.server;

import java.time.Instant;
import java.util.Date;

import javax.annotation.Nullable;

import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.engine.lobby.common.IModeratorController;
import games.strategy.engine.lobby.common.LobbyConstants;
import games.strategy.engine.lobby.server.db.BannedMacController;
import games.strategy.engine.lobby.server.db.BannedUsernameController;
import games.strategy.engine.lobby.server.db.Database;
import games.strategy.engine.lobby.server.db.MutedMacController;
import games.strategy.engine.lobby.server.db.MutedUsernameController;
import games.strategy.engine.lobby.server.db.UserController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.Messengers;
import lombok.extern.java.Log;

@Log
final class ModeratorController implements IModeratorController {
  /**
   * The hashed MAC address used when the MAC address of a node is unknown. It corresponds to the MAC address
   * {@code 00:00:00:00:00:00}.
   */
  private static final String UNKNOWN_HASHED_MAC_ADDRESS = MacFinder.getHashedMacAddress(new byte[6]);

  private final IServerMessenger serverMessenger;
  private final Messengers allMessengers;
  private final Database database;

  ModeratorController(
      final IServerMessenger serverMessenger,
      final Messengers messengers,
      final LobbyPropertyReader lobbyPropertyReader) {
    this.serverMessenger = serverMessenger;
    allMessengers = messengers;
    database = new Database(lobbyPropertyReader);
  }

  @Override
  public void banUsername(final INode node, final @Nullable Date banExpires) {
    banUsername(node, banExpires != null ? banExpires.toInstant() : null);
  }

  private void banUsername(final INode node, final @Nullable Instant banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }

    final User bannedUser = getUserForNode(node);
    final User moderator = getUserForNode(MessageContext.getSender());
    new BannedUsernameController(database).addBannedUsername(bannedUser, banExpires, moderator);
    log.info(String.format(
        "User was banned from the lobby (by username); "
            + "Username: %s, IP: %s, MAC: %s, Mod Username: %s, Mod IP: %s, Mod MAC: %s, Expires: %s",
        bannedUser.getUsername(), bannedUser.getInetAddress().getHostAddress(), bannedUser.getHashedMacAddress(),
        moderator.getUsername(), moderator.getInetAddress().getHostAddress(), moderator.getHashedMacAddress(),
        banExpires == null ? "forever" : banExpires.toString()));
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
    final DBUser dbUser = new UserController(database).getUserByName(user.getUsername());
    return dbUser != null && dbUser.isAdmin();
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
   *
   * @return The hashed MAC address of the specified node. If the MAC address of the node cannot be determined, this
   *         method returns {@link #UNKNOWN_HASHED_MAC_ADDRESS}.
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

  private void banMac(final INode node, final String hashedMac, final @Nullable Instant banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }

    final User bannedUser = getUserForNode(node).withHashedMacAddress(hashedMac);
    final User moderator = getUserForNode(MessageContext.getSender());
    new BannedMacController(database).addBannedMac(bannedUser, banExpires, moderator);
    log.info(String.format(
        "User was banned from the lobby (by MAC); "
            + "Username: %s, IP: %s, MAC: %s, Mod Username: %s, Mod IP: %s, Mod MAC: %s, Expires: %s",
        bannedUser.getUsername(), bannedUser.getInetAddress().getHostAddress(), bannedUser.getHashedMacAddress(),
        moderator.getUsername(), moderator.getInetAddress().getHostAddress(), moderator.getHashedMacAddress(),
        banExpires == null ? "forever" : banExpires.toString()));
  }

  @Override
  public void muteUsername(final INode node, final @Nullable Date muteExpires) {
    muteUsername(node, muteExpires != null ? muteExpires.toInstant() : null);
  }

  private void muteUsername(final INode node, final @Nullable Instant muteExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't mute an admin");
    }

    final User mutedUser = getUserForNode(node);
    final User moderator = getUserForNode(MessageContext.getSender());
    new MutedUsernameController(database).addMutedUsername(mutedUser, muteExpires, moderator);
    serverMessenger.notifyUsernameMutingOfPlayer(mutedUser.getUsername(), muteExpires);
    log.info(String.format(
        "User was muted in the lobby (by username); "
            + "Username: %s, IP: %s, MAC: %s, Mod Username: %s, Mod IP: %s, Mod MAC: %s, Expires: %s",
        mutedUser.getUsername(), mutedUser.getInetAddress().getHostAddress(), mutedUser.getHashedMacAddress(),
        moderator.getUsername(), moderator.getInetAddress().getHostAddress(), moderator.getHashedMacAddress(),
        muteExpires == null ? "forever" : muteExpires.toString()));
  }

  @Override
  public void muteMac(final INode node, final @Nullable Date muteExpires) {
    muteMac(node, muteExpires != null ? muteExpires.toInstant() : null);
  }

  private void muteMac(final INode node, final @Nullable Instant muteExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't mute an admin");
    }

    final User mutedUser = getUserForNode(node);
    final User moderator = getUserForNode(MessageContext.getSender());
    new MutedMacController(database).addMutedMac(mutedUser, muteExpires, moderator);
    serverMessenger.notifyMacMutingOfPlayer(mutedUser.getHashedMacAddress(), muteExpires);
    log.info(String.format(
        "User was muted in the lobby (by MAC); "
            + "Username: %s, IP: %s, MAC: %s, Mod Username: %s, Mod IP: %s, Mod MAC: %s, Expires: %s",
        mutedUser.getUsername(), mutedUser.getInetAddress().getHostAddress(), mutedUser.getHashedMacAddress(),
        moderator.getUsername(), moderator.getInetAddress().getHostAddress(), moderator.getHashedMacAddress(),
        muteExpires == null ? "forever" : muteExpires.toString()));
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
    log.info(String.format(
        "User was booted from the lobby. Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
  }

  @Override
  public String getHeadlessHostBotSalt(final INode node) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    log.info(String.format(
        "Getting salt for Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    final RemoteName remoteName = IRemoteHostUtils.newRemoteNameForNode(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    return remoteHostUtils.getSalt();
  }

  @Override
  public String getChatLogHeadlessHostBot(final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = IRemoteHostUtils.newRemoteNameForNode(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.getChatLogHeadlessHostBot(hashedPassword, salt);
    log.info(String.format(
        ((response == null || response.equals("Invalid password!")) ? "Failed" : "Successful")
            + " Remote get Chat Log of Headless HostBot. "
            + "Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String mutePlayerHeadlessHostBot(final INode node, final String playerNameToBeMuted, final int minutes,
      final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = IRemoteHostUtils.newRemoteNameForNode(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response =
        remoteHostUtils.mutePlayerHeadlessHostBot(playerNameToBeMuted, minutes, hashedPassword, salt);
    log.info(String.format(
        (response == null ? "Successful" : "Failed (" + response + ")") + " Remote Mute of " + playerNameToBeMuted
            + " for " + minutes
            + " minutes In Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String bootPlayerHeadlessHostBot(final INode node, final String playerNameToBeBooted,
      final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = IRemoteHostUtils.newRemoteNameForNode(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.bootPlayerHeadlessHostBot(playerNameToBeBooted, hashedPassword, salt);
    log.info(String.format(
        (response == null ? "Successful" : "Failed (" + response + ")") + " Remote Boot of " + playerNameToBeBooted
            + " In Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String banPlayerHeadlessHostBot(final INode node, final String playerNameToBeBanned, final int hours,
      final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = IRemoteHostUtils.newRemoteNameForNode(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.banPlayerHeadlessHostBot(playerNameToBeBanned, hours, hashedPassword, salt);
    log.info(String.format(
        (response == null ? "Successful" : "Failed (" + response + ")") + " Remote Ban of " + playerNameToBeBanned
            + " for " + hours
            + "hours  In Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String stopGameHeadlessHostBot(final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = IRemoteHostUtils.newRemoteNameForNode(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.stopGameHeadlessHostBot(hashedPassword, salt);
    log.info(String.format(
        (response == null ? "Successful" : "Failed (" + response + ")")
            + " Remote Stopgame of Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String shutDownHeadlessHostBot(final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot shutdown server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    log.info(String.format(
        "Started Remote Shutdown of Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    final RemoteName remoteName = IRemoteHostUtils.newRemoteNameForNode(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.shutDownHeadlessHostBot(hashedPassword, salt);
    log.info(String.format(
        (response == null ? "Successful" : "Failed (" + response + ")")
            + " Remote Shutdown of Headless HostBot. "
            + "Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
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
    } else if (MacFinder.isValidHashedMacAddress(mac)) {
      builder.append(MacFinder.trimHashedMacAddressPrefix(mac));
    } else {
      builder.append(mac).append(" (Invalid)");
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

  @Override
  public String getHostConnections(final INode node) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final RemoteName remoteName = IRemoteHostUtils.newRemoteNameForNode(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    return remoteHostUtils.getConnections();
  }

  void register(final IRemoteMessenger messenger) {
    messenger.registerRemote(this, LobbyConstants.MODERATOR_CONTROLLER_REMOTE_NAME);
  }
}
