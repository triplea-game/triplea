package games.strategy.engine.lobby.server;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import games.strategy.engine.lobby.server.userDB.DbUserController;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.Node;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

public class ModeratorControllerTest {
  private final IServerMessenger serverMessenger = mock(IServerMessenger.class);
  private ModeratorController moderatorController;
  private ConnectionChangeListener connectionChangeListener;
  private INode adminNode;

  @Before
  public void setUp() throws UnknownHostException {
    moderatorController = new ModeratorController(serverMessenger, null);
    final String adminName = Util.createUniqueTimeStamp();
    new DbUserController().createUser(adminName, "n@n.n", MD5Crypt.crypt(adminName), true);
    adminNode = new Node(adminName, InetAddress.getLocalHost(), 0);
  }

  @Test
  public void testBoot() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(adminNode);
    connectionChangeListener = new ConnectionChangeListener();
    final INode booted = new Node("foo", InetAddress.getByAddress(new byte[] {1, 2, 3, 4}), 0);

    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(final InvocationOnMock invocation) throws Throwable {
        connectionChangeListener.connectionRemoved(invocation.getArgument(0));
        return null;
      }
    }).when(serverMessenger).removeConnection(booted);

    final INode dummyNode = new Node("dummy", InetAddress.getLocalHost(), 0);
    when(serverMessenger.getServerNode()).thenReturn(dummyNode);
    moderatorController.boot(booted);
    assertTrue(connectionChangeListener.getRemoved().contains(booted));
  }

  @Test
  public void testCantResetAdminPassword() {
    MessageContext.setSenderNodeForThread(adminNode);
    final String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
    assertNotNull(moderatorController.setPassword(adminNode, newPassword));
  }

  @Test
  public void testResetUserPasswordUnknownUser() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(adminNode);
    final String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
    final INode node = new Node(Util.createUniqueTimeStamp(), InetAddress.getLocalHost(), 0);
    assertNotNull(moderatorController.setPassword(node, newPassword));
  }

  @Test
  public void testAssertAdmin() {
    MessageContext.setSenderNodeForThread(adminNode);
    assertTrue(moderatorController.isAdmin());
  }

  private static class ConnectionChangeListener implements IConnectionChangeListener {
    final List<INode> removed = new ArrayList<>();

    @Override
    public void connectionAdded(final INode to) {}

    @Override
    public void connectionRemoved(final INode to) {
      removed.add(to);
    }

    public List<INode> getRemoved() {
      return removed;
    }
  }
}
