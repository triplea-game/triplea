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

/*
 * GameRunner.java
 *
 * Created on December 14, 2001, 12:05 PM
 */

package games.strategy.engine.framework;

import java.util.*;
import java.net.*;
import java.io.*;
import org.xml.sax.SAXException;
import java.awt.*;
import javax.swing.*;

import games.strategy.util.Util;
import games.strategy.net.*;
import games.strategy.ui.*;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.gamePlayer.GamePlayer;
import games.strategy.engine.data.GameData;

import games.strategy.engine.chat.*;
import games.strategy.engine.transcript.*;

import games.strategy.debug.Console;

//TODO remote this, it is evil
import games.strategy.triplea.TripleA;

/**
 *
 * @author  Sean Bridges
 *
 * This class starts and runs the game.
 */
public class GameRunner 
{
	
	private GameData m_data;
	private final static int PORT = 932;
	
	public static void main(String[] args)
	{
		//Console c = Console.getConsole();
		//c.displayStandardError();
		//c.displayStandardOutput();
		//c.show();
		
		GameRunner runner = new GameRunner();
		runner.loadData();
		if(args.length == 0)
			runner.start();
		else if(args[0].equalsIgnoreCase("server"))
			runner.startServer();
		else if(args[0].equalsIgnoreCase("client"))
			runner.startClient();
		else if(args[0].equalsIgnoreCase("local"))
			runner.startLocalGame();
	}
	
	private void start()
	{
		
		selectClientServer();		
	}	
	
	private void loadData()
	{
		
		//TODO, select from all possible games
		URL xmlFile = GameRunner.class.getResource("..\\..\\triplea\\xml\\Game.xml");	
		InputStream xmlStream = null;
		try
		{
			xmlStream = xmlFile.openStream();
			xmlStream = new BufferedInputStream(xmlStream);
		} catch(IOException e)
		{
			
			System.err.println("Cannot open xml file:" + xmlFile.getPath());
			System.exit(0);
		}
		
		
		
		m_data = null;
		
		try
		{
			System.out.print("Parsing XML game data");
			long now = System.currentTimeMillis();
			GameParser parser = new GameParser();
			m_data = parser.parse(xmlStream);
			System.out.println(" done:" + (((double) System.currentTimeMillis() - now) / 1000.0) + "s");
		} catch(GameParseException gpe)
		{
			gpe.printStackTrace();
			System.err.println("Error parsing xml:" + gpe.getMessage());
			System.exit(0);
		} catch(SAXException spe)
		{
			System.err.println("Error in xml file:" + spe.getMessage());
			System.exit(0);
		} 
	}
	
	private void selectClientServer()
	{
		String server = "Server";
		String client = "Client";
		String local = "Local Game";
		String[] options = {local, server, client};
		JComboBox combo = new JComboBox(options);
		combo.setSelectedIndex(0);
		combo.setEditable(false);

		Object[] choices = {"OK"};
		int option = JOptionPane.showOptionDialog(null, combo, "Select game type", JOptionPane.OK_OPTION, JOptionPane.QUESTION_MESSAGE, null, choices, null);
		if(option != 0)
			System.exit(0);
		String type = options[combo.getSelectedIndex()];
		
		if(type == server)
			startServer();
		else if(type == client)
			startClient();
		else
			startLocalGame();
	}
	
	public void startServer()
	{
		System.out.println("Server");
		ServerPlayerSelector selector = new ServerPlayerSelector(m_data.getPlayerList().getNames());
		Collection remotePlayers = selector.getRemotePlayers();
		
		if(remotePlayers.isEmpty())
			startLocalGame();
		
		String name = selector.getName();
		if(name.trim().length() ==0)
			name = "Server";
		
		
		ServerMessenger messenger = null;
		try
		{
			 messenger= new ServerMessenger(name, PORT, new GameObjectStreamFactory(m_data));	
		} catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(0);
		}
		
		ChatFrame chatFrame = new ChatFrame(messenger);
		chatFrame.show();
	
		Collection allPlayers = Util.toList(m_data.getPlayerList().getNames());
		Collection localPlayers = games.strategy.util.Util.difference(allPlayers, remotePlayers);
		
		Set gamePlayers = TripleA.createPlayers(new HashSet(localPlayers));
	
		ServerWaitForClientMessageListener listener = new ServerWaitForClientMessageListener();
		messenger.addMessageListener(listener);
		
		ServerWaitForPlayers servertWait = new ServerWaitForPlayers(messenger, remotePlayers);
		Map playerMapping = servertWait.waitForPlayers();	
		
		System.out.println(playerMapping);
		
		ServerGame serverGame = new ServerGame(m_data,gamePlayers, messenger, playerMapping);
		
		chatFrame.getChat().showTranscript(serverGame.getTranscript());
		
		TripleA.startUI(serverGame, gamePlayers);
		listener.waitFor( new HashSet(playerMapping.values()).size());
		messenger.removeMessageListener(listener);
		serverGame.startGame();
		
		
	}
	
	public void startClient()
	{
		System.out.println("Client");
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JPanel p1 = new JPanel();
		p1.setLayout(new FlowLayout(FlowLayout.LEFT));
		p1.add(new JLabel("name:"));
		JTextField name = new JTextField("client");
		p1.add(name);
		name.setColumns(5);

		panel.add(p1);
		
		JPanel p2 = new JPanel();
		p2.setLayout(new FlowLayout(FlowLayout.LEFT));
		p2.add(new JLabel("port:"));
		IntTextField port = new IntTextField(0, Integer.MAX_VALUE);
		p2.add(port);
		port.setValue(PORT);
		port.setColumns(5);
		
		panel.add(p2);
		
		JPanel p3 = new JPanel();
		p3.setLayout(new FlowLayout(FlowLayout.LEFT));
		p3.add(new JLabel("ip:"));
		JTextField ip = new JTextField();
		ip.setColumns(15);
		ip.setText("localhost");
		p3.add(ip);
		
		panel.add(p3);
		
		JOptionPane.showMessageDialog(null, panel, "Enter server info", JOptionPane.YES_OPTION);
		
		ClientMessenger messenger = null;
		try
		{
			messenger = new ClientMessenger(ip.getText(), port.getValue(), name.getText(), new GameObjectStreamFactory(m_data));
		} catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage());
			startClient();
			return;
		}
		
		ChatFrame f = new ChatFrame(messenger);
		
		f.show();
		
		ClientPlayerSelector selector = new ClientPlayerSelector(messenger, messenger.getServerNode());
		Collection players = selector.waitToJoin();
		
		Set gamePlayers = TripleA.createPlayers( new HashSet(players));
		
		
		ClientGame game = new ClientGame(m_data, gamePlayers, messenger, messenger.getServerNode());
		
		f.getChat().showTranscript(game.getTranscript());
		
		TripleA.startUI(game, gamePlayers);
		messenger.send(new ClientReady(), messenger.getServerNode());
		
	}
	
	public void startLocalGame()
	{
		IServerMessenger messenger = new DummyMessenger();

		java.util.List players = games.strategy.util.Util.toList(m_data.getPlayerList().getNames());
		Set gamePlayers = TripleA.createPlayers( new HashSet(players));		
		ServerGame game = new ServerGame(m_data, gamePlayers, messenger, new HashMap());
		TripleA.startUI(game, gamePlayers);
		game.startGame();
	}		
}

class ClientReady implements Serializable
{

}

class ServerWaitForClientMessageListener implements IMessageListener
{
	int m_count = 0;
	
	public void messageReceived(Serializable msg, INode from)
	{
		if(msg instanceof ClientReady)
		{
			synchronized(this)
			{
				m_count++;
				this.notifyAll();
			}
		}
	}
	
	public void waitFor(int target)
	{
		synchronized(this)
		{
			while(m_count < target)
			{
				try
				{
					this.wait();
				} catch(InterruptedException ie) {}
			}
		}
	}
}
