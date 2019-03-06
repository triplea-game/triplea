package org.triplea.lobby.server;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.stubbing.Answer;
import org.triplea.java.Util;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.UserDao;
import org.triplea.test.common.Integration;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.Node;

@Integration
class ModeratorControllerIntegrationTest {
  private final IServerMessenger serverMessenger = mock(IServerMessenger.class);
  private ModeratorController moderatorController;
  private ConnectionChangeListener connectionChangeListener;
  private INode adminNode;

  private static String newHashedMacAddress() {
    final byte[] bytes = new byte[6];
    new Random().nextBytes(bytes);
    return MacFinder.getHashedMacAddress(bytes);
  }

  @BeforeEach
  void setUp() throws UnknownHostException {
    moderatorController = new ModeratorController(
        serverMessenger, null, TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao());
    final String adminName = Util.newUniqueTimestamp();

    final DBUser dbUser = new DBUser(new DBUser.UserName(adminName), new DBUser.UserEmail("n@n.n"), DBUser.Role.ADMIN);


    final UserDao userController = TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getUserDao();
    userController.createUser(dbUser, new HashedPassword(BCrypt.hashpw(adminName, BCrypt.gensalt())));
    userController.makeAdmin(dbUser);

    adminNode = new Node(adminName, InetAddress.getLocalHost(), 0);
    when(serverMessenger.getPlayerMac(adminName)).thenReturn(newHashedMacAddress());
  }

  @Test
  void testBoot() throws UnknownHostException {
    MessageContext.setSenderNodeForThread(adminNode);
    connectionChangeListener = new ConnectionChangeListener();
    final INode booted = new Node("foo", InetAddress.getByAddress(new byte[] {1, 2, 3, 4}), 0);

    doAnswer((Answer<Void>) invocation -> {
      connectionChangeListener.connectionRemoved(invocation.getArgument(0));
      return null;
    }).when(serverMessenger).removeConnection(booted);

    final INode dummyNode = new Node("dummy", InetAddress.getLocalHost(), 0);
    when(serverMessenger.getServerNode()).thenReturn(dummyNode);
    moderatorController.boot(booted);
    assertTrue(connectionChangeListener.removed.contains(booted));
  }

  @Test
  void testAssertAdmin() {
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
  }
}
