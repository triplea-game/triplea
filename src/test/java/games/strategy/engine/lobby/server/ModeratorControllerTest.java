package games.strategy.engine.lobby.server;

import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ModeratorControllerTest {
  private final IServerMessenger m_messenger = mock(IServerMessenger.class);
  private ModeratorController m_controller;
  private ConnectionChangeListener m_listener;
  private INode m_adminNode;

  @Before
  public void setUp() throws UnknownHostException {
    m_controller = new ModeratorController(m_messenger, null);
    final String adminName = Util.createUniqueTimeStamp();
    new DBUserController().createUser(adminName, "n@n.n", MD5Crypt.crypt(adminName), true);
    m_adminNode = new Node(adminName, InetAddress.getLocalHost(), 0);
  }

  @Test
  public void testBoot() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(m_adminNode);
    m_listener = new ConnectionChangeListener();
    final INode booted = new Node("foo", InetAddress.getByAddress(new byte[] {1, 2, 3, 4}), 0);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        m_listener.connectionRemoved(invocation.getArgument(0));
        return null;
      }
    }).when(m_messenger).removeConnection(booted);

    final INode dummyNode = new Node("dummy", InetAddress.getLocalHost(), 0);
    when(m_messenger.getServerNode()).thenReturn(dummyNode);
    m_controller.boot(booted);
    assertTrue(m_listener.getRemoved().contains(booted));
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
