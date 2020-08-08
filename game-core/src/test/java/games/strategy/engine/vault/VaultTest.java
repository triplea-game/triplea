package games.strategy.engine.vault;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Disabled;
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

  /**
   * Passes when run individually. Fails when run as part of a suite that consists of multiple
   * server/vault tests.
   */
  @Disabled
  @Test
  void temporarilyDisabledSoPleaseRunManuallytestServerLock() throws NotUnlockedException {
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final VaultId id = serverVault.lock(data);
    clientVault.waitForId(id, 1000);
    assertTrue(clientVault.knowsAbout(id));
    serverVault.unlock(id);
    clientVault.waitForIdToUnlock(id, 1000);
    assertTrue(clientVault.isUnlocked(id));
    assertEquals(data, clientVault.get(id));
    assertEquals(serverVault.get(id), clientVault.get(id));
    clientVault.release(id);
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

  /**
   * Passes when run individually. Fails when run as part of a suite that consists of multiple
   * server/vault tests.
   */
  @Disabled
  @Test
  void temporarilyDisabledSoPleaseRunManuallytestMultiple() throws NotUnlockedException {
    final byte[] data1 = new byte[] {0, 1, 2, 3, 4, 5};
    final byte[] data2 = new byte[] {0xE, 0xF, 2, 1, 3, 1, 2, 12, 3, 31, 124, 12, 1};
    final VaultId id1 = serverVault.lock(data1);
    final VaultId id2 = serverVault.lock(data2);
    clientVault.waitForId(id1, 2000);
    clientVault.waitForId(id2, 2000);
    assertTrue(clientVault.knowsAbout(id1));
    assertTrue(clientVault.knowsAbout(id2));
    serverVault.unlock(id1);
    serverVault.unlock(id2);
    clientVault.waitForIdToUnlock(id1, 1000);
    clientVault.waitForIdToUnlock(id2, 1000);
    assertTrue(clientVault.isUnlocked(id1));
    assertTrue(clientVault.isUnlocked(id2));
    assertEquals(data1, clientVault.get(id1));
    assertEquals(data2, clientVault.get(id2));
    clientVault.release(id1);
    clientVault.release(id2);
  }

  @Test
  void testJoin() {
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final byte[] joined = Vault.joinDataAndKnown(data);
    assertArrayEquals(
        new byte[] {0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE, 0, 1, 2, 3, 4, 5}, joined);
  }
}
