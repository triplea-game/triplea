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

package games.strategy.engine.lobby.server;

import games.strategy.engine.lobby.server.userDB.BannedIpController;
import games.strategy.engine.lobby.server.userDB.DBUserController;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.Node;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class ModeratorControllerTest extends TestCase
{
    
    private DummyMessenger m_messenger;
    private ModeratorController m_controller;
    private ConnectionChangeListener m_listener;
    private INode m_adminNode;
    
    public void setUp() throws UnknownHostException 
    {
        m_messenger = new DummyMessenger();
        m_controller = new ModeratorController(m_messenger);
        m_listener = new ConnectionChangeListener();
        m_messenger.addConnectionChangeListener(m_listener);
        
        String adminName = Util.createUniqueTimeStamp();
        new DBUserController().createUser(adminName, "n@n.n", MD5Crypt.crypt(adminName), true);
        m_adminNode = new Node(adminName, InetAddress.getLocalHost(), 0);
    }
        

    public void testBoot() throws UnknownHostException
    {
        MessageContext.setSenderNodeForThread(m_adminNode);
        INode booted = new Node("foo", InetAddress.getLocalHost(), 0);
        m_controller.boot(booted);
        assertTrue(m_listener.getRemoved().contains(booted));
    }
    
    public void testBan() throws UnknownHostException 
    {
        InetAddress bannedAddress = Inet4Address.getByAddress(new byte[] {(byte)10,(byte)10,(byte)10,(byte)10});
        new BannedIpController().removeBannedIp(bannedAddress.getHostAddress()); 
        
        MessageContext.setSenderNodeForThread(m_adminNode);
        INode booted = new Node("foo", bannedAddress, 0);
        m_controller.banIp(booted, null);
        assertTrue(m_listener.getRemoved().contains(booted));
        
        assertTrue(new BannedIpController().isIpBanned(bannedAddress.getHostAddress()));
    }
    
    public void testResetUserPassword() 
    {
        String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
        assertTrue(m_controller.setPassword(m_adminNode, newPassword));
        
        assertTrue(new DBUserController().login(m_adminNode.getName(), newPassword));
    }

    public void testResetUserPassworUnknownUserd() throws UnknownHostException 
    {
        String newPassword = MD5Crypt.crypt("" + System.currentTimeMillis());
        INode node = new Node(Util.createUniqueTimeStamp(), InetAddress.getLocalHost(), 0);
        assertFalse(m_controller.setPassword(node, newPassword));
    }
    
    
    public void testAssertAdmin() throws UnknownHostException 
    {
        MessageContext.setSenderNodeForThread(m_adminNode);
        assertTrue(m_controller.isAdmin());
        m_controller.assertUserIsAdmin();
    }
    
}

class ConnectionChangeListener implements IConnectionChangeListener
{
    final List<INode> m_removed = new ArrayList<INode>();
    public void connectionAdded(INode to)
    {}

    public void connectionRemoved(INode to)
    {
        m_removed.add(to);
    }

    public List<INode> getRemoved()
    {
        return m_removed;
    }    
    
    
}

