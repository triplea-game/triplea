package games.strategy.engine.lobby.server;

import java.util.Date;

import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.BannedMacController;
import games.strategy.engine.lobby.server.userDB.BannedUsernameController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.lobby.server.userDB.MutedIpController;
import games.strategy.engine.lobby.server.userDB.MutedMacController;
import games.strategy.engine.lobby.server.userDB.MutedUsernameController;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Messengers;
import games.strategy.util.MD5Crypt;

public class ModeratorController extends AbstractModeratorController {
  public ModeratorController(final IServerMessenger serverMessenger, final Messengers messengers) {
    super(serverMessenger, messengers);
  }

  @Override
  public void banUsername(final INode node, final Date banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    new BannedUsernameController().addBannedUsername(getRealName(node), banExpires);
    final String banUntil = (banExpires == null ? "forever" : banExpires.toString());
    s_logger.info(String.format(
        "User was banned from the lobby(Username ban). Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s Expires: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banUntil));
  }

  @Override
  public void banIp(final INode node, final Date banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    new BannedIpController().addBannedIp(node.getAddress().getHostAddress(), banExpires);
    final String banUntil = (banExpires == null ? "forever" : banExpires.toString());
    s_logger.info(String.format(
        "User was banned from the lobby(IP ban). Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s Expires: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banUntil));
  }

  @Override
  public void banMac(final INode node, final Date banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    new BannedMacController().addBannedMac(mac, banExpires);
    final String banUntil = (banExpires == null ? "forever" : banExpires.toString());
    s_logger.info(String.format(
        "User was banned from the lobby(Mac ban). Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s Expires: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banUntil));
  }

  @Override
  public void banMac(final INode node, final String hashedMac, final Date banExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't ban an admin");
    }
    final INode modNode = MessageContext.getSender();
    new BannedMacController().addBannedMac(hashedMac, banExpires);
    final String banUntil = (banExpires == null ? "forever" : banExpires.toString());
    s_logger.info(String.format(
        "User was banned from the lobby(Mac ban). Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s Expires: %s",
        node.getName(), node.getAddress().getHostAddress(), hashedMac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), banUntil));
  }

  @Override
  public void muteUsername(final INode node, final Date muteExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't mute an admin");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final String realName = getRealName(node);
    new MutedUsernameController().addMutedUsername(realName, muteExpires);
    m_serverMessenger.NotifyUsernameMutingOfPlayer(realName, muteExpires);
    final String muteUntil = (muteExpires == null ? "forever" : muteExpires.toString());
    s_logger.info(String.format(
        "User was muted on the lobby(Username mute). Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s Expires: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteUntil));
  }

  @Override
  public void muteIp(final INode node, final Date muteExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't mute an admin");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final String ip = node.getAddress().getHostAddress();
    new MutedIpController().addMutedIp(ip, muteExpires);
    m_serverMessenger.NotifyIPMutingOfPlayer(ip, muteExpires);
    final String muteUntil = (muteExpires == null ? "forever" : muteExpires.toString());
    s_logger.info(String.format(
        "User was muted on the lobby(IP mute). Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s Expires: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteUntil));
  }

  @Override
  public void muteMac(final INode node, final Date muteExpires) {
    assertUserIsAdmin();
    if (isPlayerAdmin(node)) {
      throw new IllegalStateException("Can't mute an admin");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    new MutedMacController().addMutedMac(mac, muteExpires);
    m_serverMessenger.NotifyMacMutingOfPlayer(mac, muteExpires);
    final String muteUntil = (muteExpires == null ? "forever" : muteExpires.toString());
    s_logger.info(String.format(
        "User was muted on the lobby(Mac mute). Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s Expires: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode), muteUntil));
  }

  @Override
  public void boot(final INode node) {
    assertUserIsAdmin();
    /*
     * if (!MessageContext.getSender().getName().equals("Admin") && isPlayerAdmin(node)) // Let the master lobby
     * administrator boot admins
     * throw new IllegalStateException("Can't boot an admin");
     */
    // You can't boot the server node
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot boot server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    m_serverMessenger.removeConnection(node);
    s_logger.info(String.format(
        "User was booted from the lobby. Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
  }

  @Override
  public String getHeadlessHostBotSalt(final INode node) {
    assertUserIsAdmin();
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    s_logger.info(String.format(
        "Getting salt for Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
    return remoteHostUtils.getSalt();
  }

  @Override
  public String getChatLogHeadlessHostBot(final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.getChatLogHeadlessHostBot(hashedPassword, salt);
    s_logger.info(String.format(
        ((response == null || response.equals("Invalid password!")) ? "Failed" : "Successful")
            + " Remote get Chat Log of Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String mutePlayerHeadlessHostBot(final INode node, final String playerNameToBeMuted, final int minutes,
      final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response =
        remoteHostUtils.mutePlayerHeadlessHostBot(playerNameToBeMuted, minutes, hashedPassword, salt);
    s_logger.info(String.format(
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
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.bootPlayerHeadlessHostBot(playerNameToBeBooted, hashedPassword, salt);
    s_logger.info(String.format(
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
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.banPlayerHeadlessHostBot(playerNameToBeBanned, hours, hashedPassword, salt);
    s_logger.info(String.format(
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
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.stopGameHeadlessHostBot(hashedPassword, salt);
    s_logger.info(String.format(
        (response == null ? "Successful" : "Failed (" + response + ")")
            + " Remote Stopgame of Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    return response;
  }

  @Override
  public String shutDownHeadlessHostBot(final INode node, final String hashedPassword, final String salt) {
    assertUserIsAdmin();
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot shutdown server node");
    }
    final INode modNode = MessageContext.getSender();
    final String mac = getNodeMacAddress(node);
    s_logger.info(String.format(
        "Started Remote Shutdown of Headless HostBot. Host: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.shutDownHeadlessHostBot(hashedPassword, salt);
    s_logger.info(String.format(
        (response == null ? "Successful" : "Failed (" + response + ")")
            + " Remote Shutdown of Headless HostBot. Username: %s IP: %s Mac: %s Mod Username: %s Mod IP: %s Mod Mac: %s",
        node.getName(), node.getAddress().getHostAddress(), mac, modNode.getName(),
        modNode.getAddress().getHostAddress(), getNodeMacAddress(modNode)));
    return response;
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
    final String name = getRealName(node);
    final DBUserController controller = new DBUserController();
    final DBUser user = controller.getUser(name);
    if (user == null) {
      return false;
    }
    return user.isAdmin();
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
    builder.append("\r\nHashed Mac: ")
        .append((mac != null && mac.startsWith(MD5Crypt.MAGIC + "MH$") ? mac.substring(6) : mac + " (Invalid)"));
    builder.append("\r\nAliases: ").append(getAliasesFor(node));
    return builder.toString();
  }

  @Override
  public String getHostConnections(final INode node) {
    assertUserIsAdmin();
    if (m_serverMessenger.getServerNode().equals(node)) {
      throw new IllegalStateException("Cannot do this for server node");
    }
    final RemoteName remoteName = RemoteHostUtils.getRemoteHostUtilsName(node);
    final IRemoteHostUtils remoteHostUtils =
        (IRemoteHostUtils) m_allMessengers.getRemoteMessenger().getRemote(remoteName);
    final String response = remoteHostUtils.getConnections();
    return response;
  }

  @Override
  public String setPassword(final INode node, final String hashedPassword) {
    assertUserIsAdmin();
    final DBUserController controller = new DBUserController();
    final DBUser user = controller.getUser(getRealName(node));
    if (user == null) {
      return "Can't set the password of an anonymous player";
    }
    // Don't allow changing an admin password
    if (user.isAdmin()) {
      return "Can't set the password of an admin";
    }
    controller.updateUser(user.getName(), user.getEmail(), hashedPassword, user.isAdmin());
    return null;
  }
}
