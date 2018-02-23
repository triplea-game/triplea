package games.strategy.engine.lobby.server;

import java.time.Instant;
import java.util.Date;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.lobby.server.db.BannedMacController;
import games.strategy.engine.lobby.server.db.BannedUsernameController;
import games.strategy.engine.lobby.server.db.MutedMacController;
import games.strategy.engine.lobby.server.db.MutedUsernameController;
import games.strategy.engine.lobby.server.db.UserController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.Messengers;

public class ModeratorController extends AbstractModeratorController {
  public ModeratorController(final IServerMessenger serverMessenger, final Messengers messengers) {
    super(serverMessenger, messengers);
  }

  @Override
  public void banUsername(final INode node, final @Nullable Date banExpires) {
    banUsername(node, (banExpires != null) ? banExpires.toInstant() : null);
  }

  private void banUsername(final INode node, final @Nullable Instant banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }

    final User bannedUser = getUserForNode(node);
    final User moderator = getUserForNode(MessageContext.getSender());
    new BannedUsernameController().addBannedUsername(bannedUser, banExpires, moderator);
    logger.info(String.format(
        "User was banned from the lobby (by username); "
            + "Username: %s, IP: %s, MAC: %s, Mod Username: %s, Mod IP: %s, Mod MAC: %s, Expires: %s",
        bannedUser.getUsername(), bannedUser.getInetAddress().getHostAddress(), bannedUser.getHashedMacAddress(),
        moderator.getUsername(), moderator.getInetAddress().getHostAddress(), moderator.getHashedMacAddress(),
        (banExpires == null) ? "forever" : banExpires.toString()));
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
    final DBUser dbUser = new UserController().getUserByName(user.getUsername());
    return (dbUser != null) && dbUser.isAdmin();
  }

  private User getUserForNode(final INode node) {
    return new User(getUsernameForNode(node), node.getAddress(), getNodeMacAddress(node));
  }

  @VisibleForTesting
  static String getUsernameForNode(final INode node) {
    // usernames may contain a " (n)" suffix when the same user is logged in multiple times
    return node.getName().split(" ")[0];
  }

  @Override
  public void banMac(final INode node, final @Nullable Date banExpires) {
    banMac(node, getNodeMacAddress(node), banExpires);
  }

  @Override
  public void banMac(final INode node, final String hashedMac, final @Nullable Date banExpires) {
    banMac(node, hashedMac, (banExpires != null) ? banExpires.toInstant() : null);
  }

  private void banMac(final INode node, final String hashedMac, final @Nullable Instant banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }

    final User bannedUser = getUserForNode(node).withHashedMacAddress(hashedMac);
    final User moderator = getUserForNode(MessageContext.getSender());
    new BannedMacController().addBannedMac(bannedUser, banExpires, moderator);
    logger.info(String.format(
        "User was banned from the lobby (by MAC); "
            + "Username: %s, IP: %s, MAC: %s, Mod Username: %s, Mod IP: %s, Mod MAC: %s, Expires: %s",
        bannedUser.getUsername(), bannedUser.getInetAddress().getHostAddress(), bannedUser.getHashedMacAddress(),
        moderator.getUsername(), moderator.getInetAddress().getHostAddress(), moderator.getHashedMacAddress(),
        (banExpires == null) ? "forever" : banExpires.toString()));
  }

  @Override
  public void muteUsername(final INode node, final @Nullable Date muteExpires) {
    muteUsername(node, (muteExpires != null) ? muteExpires.toInstant() : null);
  }

  private void muteUsername(final INode node, final @Nullable Instant muteExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't mute an admin");
    }

    final User mutedUser = getUserForNode(node);
    final User moderator = getUserForNode(MessageContext.getSender());
    new MutedUsernameController().addMutedUsername(mutedUser, muteExpires, moderator);
    serverMessenger.notifyUsernameMutingOfPlayer(mutedUser.getUsername(), muteExpires);
    logger.info(String.format(
        "User was muted in the lobby (by username); "
            + "Username: %s, IP: %s, MAC: %s, Mod Username: %s, Mod IP: %s, Mod MAC: %s, Expires: %s",
        mutedUser.getUsername(), mutedUser.getInetAddress().getHostAddress(), mutedUser.getHashedMacAddress(),
        moderator.getUsername(), moderator.getInetAddress().getHostAddress(), moderator.getHashedMacAddress(),
        (muteExpires == null) ? "forever" : muteExpires.toString()));
  }

  @Override
  public void muteMac(final INode node, final @Nullable Date muteExpires) {
    muteMac(node, (muteExpires != null) ? muteExpires.toInstant() : null);
  }

  private void muteMac(final INode node, final @Nullable Instant muteExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't mute an admin");
    }

    final User mutedUser = getUserForNode(node);
    final User moderator = getUserForNode(MessageContext.getSender());
    new MutedMacController().addMutedMac(mutedUser, muteExpires, moderator);
    serverMessenger.notifyMacMutingOfPlayer(mutedUser.getHashedMacAddress(), muteExpires);
    logger.info(String.format(
        "User was muted in the lobby (by MAC); "
            + "Username: %s, IP: %s, MAC: %s, Mod Username: %s, Mod IP: %s, Mod MAC: %s, Expires: %s",
        mutedUser.getUsername(), mutedUser.getInetAddress().getHostAddress(), mutedUser.getHashedMacAddress(),
        moderator.getUsername(), moderator.getInetAddress().getHostAddress(), moderator.getHashedMacAddress(),
        (muteExpires == null) ? "forever" : muteExpires.toString()));
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
    logger.info(String.format(
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
    logger.info(String.format(
        "Getting salt for Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
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
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.getChatLogHeadlessHostBot(hashedPassword, salt);
    logger.info(String.format(
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
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response =
        remoteHostUtils.mutePlayerHeadlessHostBot(playerNameToBeMuted, minutes, hashedPassword, salt);
    logger.info(String.format(
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
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.bootPlayerHeadlessHostBot(playerNameToBeBooted, hashedPassword, salt);
    logger.info(String.format(
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
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.banPlayerHeadlessHostBot(playerNameToBeBanned, hours, hashedPassword, salt);
    logger.info(String.format(
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
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.stopGameHeadlessHostBot(hashedPassword, salt);
    logger.info(String.format(
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
    logger.info(String.format(
        "Started Remote Shutdown of Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.shutDownHeadlessHostBot(hashedPassword, salt);
    logger.info(String.format(
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

  @Override
  public String getHostConnections(final INode node) {
    assertUserIsAdmin();
    if (serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) allMessengers.getRemoteMessenger().getRemote(remoteName);
    return remoteHostUtils.getConnections();
  }

  @Override
  public String setPassword(final INode node, final String hashedPassword) {
    // TODO: remove once we confirm no backwards compat issues
    return "";
  }
}
