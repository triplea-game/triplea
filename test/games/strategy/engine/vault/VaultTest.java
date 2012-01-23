/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.vault;

import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.message.UnifiedMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.ServerMessenger;
import games.strategy.test.TestUtil;

import java.io.IOException;

import junit.framework.TestCase;

/**
 * Comment(KG): This test is broken, If you run each test individually they all work, but when running all test in the class some will fail.
 * This is because the lifecycle of the UnifiedMessenger (and internal classes such as NIOReader/Writer) are broken.
 * The UnifiedMessenger will create a new ThreadPool with each instantiation, and this pool is never shutdown.
 * 
 * @author Sean Bridges
 */
public class VaultTest extends TestCase
{
	private static int SERVER_PORT = -1;
	private IServerMessenger m_server;
	private IMessenger m_client1;
	private Vault m_clientVault;
	private Vault m_serverVault;
	
	@Override
	public void setUp() throws IOException
	{
		SERVER_PORT = TestUtil.getUniquePort();
		m_server = new ServerMessenger("Server", SERVER_PORT);
		m_server.setAcceptNewConnections(true);
		final String mac = MacFinder.GetHashedMacAddress();
		m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1", mac);
		final UnifiedMessenger serverUM = new UnifiedMessenger(m_server);
		final UnifiedMessenger clientUM = new UnifiedMessenger(m_client1);
		m_serverVault = new Vault(new ChannelMessenger(serverUM));
		m_clientVault = new Vault(new ChannelMessenger(clientUM));
		Thread.yield();
	}
	
	@Override
	public void tearDown()
	{
		try
		{
			if (m_server != null)
				m_server.shutDown();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
		try
		{
			if (m_client1 != null)
				m_client1.shutDown();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public VaultTest(final String arg0)
	{
		super(arg0);
	}
	
	public void assertEquals(final byte[] b1, final byte[] b2)
	{
		assertEquals(b1.length, b2.length);
		for (int i = 0; i < b1.length; i++)
		{
			assertEquals(b1[i], b2[i]);
		}
	}
	
	public void testLocal() throws NotUnlockedException
	{
		final DummyMessenger messenger = new DummyMessenger();
		final UnifiedMessenger unifiedMessenger = new UnifiedMessenger(messenger);
		final ChannelMessenger channelMessenger = new ChannelMessenger(unifiedMessenger);
		// RemoteMessenger remoteMessenger = new RemoteMessenger(unifiedMessenger);
		final Vault vault = new Vault(channelMessenger);
		final byte[] data = new byte[] { 0, 1, 2, 3, 4, 5 };
		final VaultID id = vault.lock(data);
		vault.unlock(id);
		assertEquals(data, vault.get(id));
		vault.release(id);
	}
	
	/**
	 * Passes when run individually.
	 * Fails when run as part of a suite that consists of multiple server/vault tests.
	 * 
	 * @throws NotUnlockedException
	 */
	public void testServerLock() throws NotUnlockedException
	{
		final byte[] data = new byte[] { 0, 1, 2, 3, 4, 5 };
		final VaultID id = m_serverVault.lock(data);
		m_clientVault.waitForID(id, 1000);
		assertTrue(m_clientVault.knowsAbout(id));
		m_serverVault.unlock(id);
		m_clientVault.waitForIdToUnlock(id, 1000);
		assertTrue(m_clientVault.isUnlocked(id));
		assertEquals(data, m_clientVault.get(id));
		assertEquals(m_serverVault.get(id), m_clientVault.get(id));
		m_clientVault.release(id);
	}
	
	public void testClientLock() throws NotUnlockedException
	{
		final byte[] data = new byte[] { 0, 1, 2, 3, 4, 5 };
		final VaultID id = m_clientVault.lock(data);
		m_serverVault.waitForID(id, 1000);
		assertTrue(m_serverVault.knowsAbout(id));
		m_clientVault.unlock(id);
		m_serverVault.waitForIdToUnlock(id, 1000);
		assertTrue(m_serverVault.isUnlocked(id));
		assertEquals(data, m_serverVault.get(id));
		assertEquals(m_clientVault.get(id), m_serverVault.get(id));
		m_clientVault.release(id);
	}
	
	/**
	 * Passes when run individually.
	 * Fails when run as part of a suite that consists of multiple server/vault tests.
	 * 
	 * @throws NotUnlockedException
	 */
	public void testMultiple() throws NotUnlockedException
	{
		final byte[] data1 = new byte[] { 0, 1, 2, 3, 4, 5 };
		final byte[] data2 = new byte[] { 0xE, 0xF, 2, 1, 3, 1, 2, 12, 3, 31, 124, 12, 1 };
		final VaultID id1 = m_serverVault.lock(data1);
		final VaultID id2 = m_serverVault.lock(data2);
		m_clientVault.waitForID(id1, 2000);
		m_clientVault.waitForID(id2, 2000);
		assertTrue(m_clientVault.knowsAbout(id1));
		assertTrue(m_clientVault.knowsAbout(id2));
		m_serverVault.unlock(id1);
		m_serverVault.unlock(id2);
		m_clientVault.waitForIdToUnlock(id1, 1000);
		m_clientVault.waitForIdToUnlock(id2, 1000);
		assertTrue(m_clientVault.isUnlocked(id1));
		assertTrue(m_clientVault.isUnlocked(id2));
		assertEquals(data1, m_clientVault.get(id1));
		assertEquals(data2, m_clientVault.get(id2));
		m_clientVault.release(id1);
		m_clientVault.release(id2);
	}
	
	public void testJoin()
	{
		final byte[] data = new byte[] { 0, 1, 2, 3, 4, 5 };
		final byte[] joined = Vault.joinDataAndKnown(data);
		assertEquals(new byte[] { 0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE, 0, 1, 2, 3, 4, 5 }, joined);
	}
}
