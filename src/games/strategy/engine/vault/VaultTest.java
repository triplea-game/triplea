/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package games.strategy.engine.vault;

import java.io.IOException;

import games.strategy.engine.message.ChannelMessenger;
import games.strategy.net.ClientMessenger;
import games.strategy.net.DummyMessenger;
import games.strategy.net.IMessenger;
import games.strategy.net.IServerMessenger;
import games.strategy.net.ServerMessenger;
import junit.framework.TestCase;

/**
 * @author Sean Bridges
 */
public class VaultTest extends TestCase
{

	private static int SERVER_PORT = 12022;

	private IServerMessenger m_server;
	private IMessenger m_client1;
	
	private Vault m_clientVault;
	private Vault m_serverVault;

	public void setUp() throws IOException
	{
        SERVER_PORT++;
		m_server = new ServerMessenger("Server", SERVER_PORT);
		m_server.setAcceptNewConnections(true);
		m_client1 = new ClientMessenger("localhost", SERVER_PORT, "client1");
		
		
		m_serverVault = new Vault(new ChannelMessenger(m_server));
		m_clientVault = new Vault(new ChannelMessenger(m_client1));
		
	}
 
	public void tearDown()
	{
		try
		{
			if(m_server != null)
				m_server.shutDown();
		} catch(Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			if(m_client1 != null)
				m_client1.shutDown();
		} catch(Exception e)
		{
			e.printStackTrace();
		}

		
	}
	
	
    public VaultTest(String arg0)
    {
        super(arg0);
    }

    public void assertEquals(byte[] b1, byte[] b2)
    {
        assertEquals(b1.length, b2.length);
        for(int i = 0 ; i < b1.length; i++)
        {
            assertEquals(b1[i], b2[i]);
        }
    }
    
    public void testLocal() throws NotUnlockedException
    {
        DummyMessenger messenger = new DummyMessenger();
        ChannelMessenger channelMessenger = new ChannelMessenger(messenger);
        
        Vault vault = new Vault(channelMessenger);
        
        byte[] data = new byte[] {0,1,2,3,4,5};
        VaultID id = vault.lock(data);
        vault.unlock(id);
        assertEquals(data, vault.get(id));
        vault.release(id);
    }
    
    public void testRemote() throws NotUnlockedException
    {
        byte[] data = new byte[] {0,1,2,3,4,5};
        VaultID id = m_serverVault.lock(data);
        
        long waitCount = 0;
        while(waitCount < 5 && !m_clientVault.knowsAbout(id))
        {
            waitCount++;
            try
            {
                Thread.sleep(100);
            } catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        assertTrue(m_clientVault.knowsAbout(id));        
        
        m_serverVault.unlock(id);
        
        waitCount = 0;
        while(waitCount < 5 && !m_clientVault.isUnlocked(id))
        {
            waitCount++;
            try
            {
                Thread.sleep(50);
            } catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        assertTrue(m_clientVault.isUnlocked(id));
        
        assertEquals(data, m_clientVault.get(id));
        
    }
    
    public void testJoin()
    {
        byte[] data = new byte[] {0,1,2,3,4,5};
        byte[] joined = Vault.joinDataAndKnown(data);
        assertEquals(new byte[] {0xC, 0xA, 0xF, 0xE, 0xB, 0xA, 0xB, 0xE, 0,1,2,3,4,5}, joined);
    }
    
}
