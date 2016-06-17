package games.strategy.engine.lobby.server;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.Node;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

public class ModeratorControllerTest {
  private DummyMessenger m_messenger;
  private ModeratorController m_controller;
  private ConnectionChangeListener m_listener;
  private INode m_adminNode;

  @Before
  public void setUp() throws UnknownHostException {
    m_messenger = new DummyMessenger();
    m_controller = new ModeratorController(m_messenger, null);
    m_listener = new ConnectionChangeListener();
    m_messenger.addConnectionChangeListener(m_listener);
    final String adminName = Util.createUniqueTimeStamp();
    new DBUserController().createUser(adminName, "n@n.n", MD5Crypt.crypt(adminName), true);
    m_adminNode = new Node(adminName, InetAddress.getLocalHost(), 0);
  }

  @Test
  public void testBoot() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(m_adminNode);
    final INode booted = new Node("foo", InetAddress.getByAddress(new byte[] {1, 2, 3, 4}), 0);
    m_controller.boot(booted);
    assertTrue(m_listener.getRemoved().contains(booted));
  }

  @Test
  public void testBan() throws UnknownHostException {
    final InetAddress bannedAddress = InetAddress.getByAddress(new byte[] {(byte) 10, (byte) 10, (byte) 10, (byte) 10});
    new BannedIpController().removeBannedIp(bannedAddress.getHostAddress());
    MessageContext.setSenderNodeForThread(m_adminNode);
    final INode booted = new Node("foo", bannedAddress, 0);
    // this test is failing because any kind of ban requires a mac address for the logging information,
    // yet this node has no mac address. need to fix this somehow.
    m_controller.banIp(booted, null);
    assertTrue(new BannedIpController().isIpBanned(bannedAddress.getHostAddress()).getFirst());
  }

  @Test
  public void testResetUserPassword() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(m_adminNode);
    final String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
    final String userName = Util.createUniqueTimeStamp();
    new DBUserController().createUser(userName, "n@n.n", newPassword, false);
    final INode node = new Node(userName, InetAddress.getLocalHost(), 0);
    assertNull(m_controller.setPassword(node, newPassword));
    assertTrue(new DBUserController().login(node.getName(), newPassword));
  }

  @Test
  public void testCantResetAdminPassword() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(m_adminNode);
    final String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
    assertNotNull(m_controller.setPassword(m_adminNode, newPassword));
  }

  public void testResetUserPasswordUnknownUser() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(m_adminNode);
    final String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
    final INode node = new Node(Util.createUniqueTimeStamp(), InetAddress.getLocalHost(), 0);
    assertNotNull(m_controller.setPassword(node, newPassword));
  }

  public void testAssertAdmin() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(m_adminNode);
    assertTrue(m_controller.isAdmin());
  }
}


class ConnectionChangeListener implements IConnectionChangeListener {
  final List<INode> m_removed = new ArrayList<>();

  @Override
  public void connectionAdded(final INode to) {}

  @Override
  public void connectionRemoved(final INode to) {
    m_removed.add(to);
  }

  public List<INode> getRemoved() {
    return m_removed;
  }
}
