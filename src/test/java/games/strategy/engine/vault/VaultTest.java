package games.strategy.engine.vault;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.unifiedmessenger.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.Node;
import games.strategy.net.ServerMessenger;
import games.strategy.test.TestUtil;

/**
 * Comment(KG): This test is broken, If you run each test individually they all work, but when running all test in the
 * class some will fail.
 * This is because the lifecycle of the UnifiedMessenger (and internal classes such as NIOReader/Writer) are broken.
 * The UnifiedMessenger will create a new ThreadPool with each instantiation, and this pool is never shutdown.
 */
public class VaultTest {
  private static int SERVER_PORT = -1;
  private IServerMessenger serverMessenger;
  private IMessenger clientMessenger;
  private Vault clientVault;
  private Vault serverVault;

  @Before
  public void setUp() throws IOException {
    SERVER_PORT = TestUtil.getUniquePort();
    serverMessenger = new ServerMessenger("Server", SERVER_PORT);
    serverMessenger.setAcceptNewConnections(true);
    final String mac = MacFinder.getHashedMacAddress();
    clientMessenger = new ClientMessenger("localhost", SERVER_PORT, "client1", mac);
    final UnifiedMessenger serverUM = new UnifiedMessenger(serverMessenger);
    final UnifiedMessenger clientUM = new UnifiedMessenger(clientMessenger);
    serverVault = new Vault(new ChannelMessenger(serverUM));
    clientVault = new Vault(new ChannelMessenger(clientUM));
    Thread.yield();
  }

  @After
  public void tearDown() {
    try {
      if (serverMessenger != null) {
        serverMessenger.shutDown();
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
    try {
      if (clientMessenger != null) {
        clientMessenger.shutDown();
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }

  @Test
  public void testLocal() throws NotUnlockedException {
    final IServerMessenger messenger = mock(IServerMessenger.class);
    try {
      when(messenger.getLocalNode()).thenReturn(new Node("dummy", InetAddress.getLocalHost(), 0));
    } catch (final UnknownHostException e) {
      ClientLogger.logQuietly(e);
    }
    final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
    final ChannelMessenger channelMessenger = new ChannelMessenger(unifiedMessenger);
    // RemoteMessenger remoteMessenger = new RemoteMessenger(unifiedMessenger);
    final Vault vault = new Vault(channelMessenger);
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final VaultID id = vault.lock(data);
    vault.unlock(id);
    assertArrayEquals(data, vault.get(id));
    vault.release(id);
  }

  /**
   * Passes when run individually.
   * Fails when run as part of a suite that consists of multiple server/vault tests.
   */
  public void temporarilyDisabledSoPleaseRunManuallytestServerLock() throws NotUnlockedException {
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final VaultID id = serverVault.lock(data);
    clientVault.waitForID(id, 1000);
    assertTrue(clientVault.knowsAbout(id));
    serverVault.unlock(id);
    clientVault.waitForIdToUnlock(id, 1000);
    assertTrue(clientVault.isUnlocked(id));
    assertEquals(data, clientVault.get(id));
    assertEquals(serverVault.get(id), clientVault.get(id));
    clientVault.release(id);
  }

  @Test
  public void testClientLock() throws NotUnlockedException {
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final VaultID id = clientVault.lock(data);
    serverVault.waitForID(id, 1000);
    assertTrue(serverVault.knowsAbout(id));
    clientVault.unlock(id);
    serverVault.waitForIdToUnlock(id, 1000);
    assertTrue(serverVault.isUnlocked(id));
    assertArrayEquals(data, serverVault.get(id));
    assertArrayEquals(clientVault.get(id), serverVault.get(id));
    clientVault.release(id);
  }

  /**
   * Passes when run individually.
   * Fails when run as part of a suite that consists of multiple server/vault tests.
   */
  public void temporarilyDisabledSoPleaseRunManuallytestMultiple() throws NotUnlockedException {
    final byte[] data1 = new byte[] {0, 1, 2, 3, 4, 5};
    final byte[] data2 = new byte[] {0xE, 0xF, 2, 1, 3, 1, 2, 12, 3, 31, 124, 12, 1};
    final VaultID id1 = serverVault.lock(data1);
    final VaultID id2 = serverVault.lock(data2);
    clientVault.waitForID(id1, 2000);
    clientVault.waitForID(id2, 2000);
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
  public void testJoin() {
    final byte[] data = new byte[] {0, 1, 2, 3, 4, 5};
    final byte[] joined = Vault.joinDataAndKnown(data);
    assertArrayEquals(new byte[] {0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE, 0, 1, 2, 3, 4, 5}, joined);
  }
}
