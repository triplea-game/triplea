package games.strategy.engine.vault;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MessengerTestUtils;
import games.strategy.net.Node;
import games.strategy.net.TestServerMessenger;
import java.io.IOException;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.SystemId;

/**
 * Comment(KG): This test is broken, If you run each test individually they all work, but when
 * running all test in the class some will fail. This is because the lifecycle of the
 * UnifiedMessenger (and internal classes such as NioReader/Writer) are broken. The UnifiedMessenger
 * will create a new ThreadPool with each instantiation, and this pool is never shutdown.
 */
class VaultTest {
  private IServerMessenger serverMessenger;
  private IMessenger clientMessenger;
  private Vault clientVault;
  private Vault serverVault;

  @BeforeEach
  void setUp() throws IOException {
    serverMessenger = new TestServerMessenger();
    serverMessenger.setAcceptNewConnections(true);
    final int serverPort = serverMessenger.getLocalNode().getSocketAddress().getPort();
    clientMessenger =
        new ClientMessenger("localhost", serverPort, "client1", SystemId.of("system-id"));
    final UnifiedMessenger serverUnifiedMessenger = new UnifiedMessenger(serverMessenger);
    final UnifiedMessenger clientUnifiedMessenger = new UnifiedMessenger(clientMessenger);
    serverVault = new Vault(new ChannelMessenger(serverUnifiedMessenger));
    clientVault = new Vault(new ChannelMessenger(clientUnifiedMessenger));
  }

  @AfterEach
  void tearDown() {
    MessengerTestUtils.shutDownQuietly(serverMessenger);
    MessengerTestUtils.shutDownQuietly(clientMessenger);
  }

  @Test
  void testLocal() throws Exception {
    final IServerMessenger messenger = mock(IServerMessenger.class);
    when(messenger.getLocalNode()).thenReturn(new Node("dummy", InetAddress.getLocalHost(), 0));
    final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
    final ChannelMessenger channelMessenger = new ChannelMessenger(unifiedMessenger);
    // RemoteMessenger remoteMessenger = new RemoteMessenger(unifiedMessenger);
    final Vault vault = new Vault(channelMessenger);
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final VaultId id = vault.lock(data);
    vault.unlock(id);
    assertArrayEquals(data, vault.get(id));
    vault.release(id);
  }

  @Test
  void testClientLock() throws NotUnlockedException {
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final VaultId id = clientVault.lock(data);
    serverVault.waitForId(id, 1000);
    assertTrue(serverVault.knowsAbout(id));
    clientVault.unlock(id);
    serverVault.waitForIdToUnlock(id, 1000);
    assertTrue(serverVault.isUnlocked(id));
    assertArrayEquals(data, serverVault.get(id));
    assertArrayEquals(clientVault.get(id), serverVault.get(id));
    clientVault.release(id);
  }

  @Test
  void testJoin() {
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final byte[] joined = Vault.joinDataAndKnown(data);
    assertArrayEquals(
        new byte[] {0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE, 0, 1, 2, 3, 4, 5}, joined);
  }
}
