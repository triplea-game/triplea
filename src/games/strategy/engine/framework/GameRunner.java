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
import games.strategy.engine.framework.ui.*;

import games.strategy.engine.chat.*;
import games.strategy.engine.transcript.*;

import games.strategy.debug.Console;

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
		ServerMessenger messenger = null;
		
		ServerOptions options = new ServerOptions("Server", PORT);
		options.show();
		options.waitForOptions();
		options.dispose();
		
		String name = options.getName();
		int port = options.getPort();
		
		try
		{
			 messenger= new ServerMessenger(name, port, new GameObjectStreamFactory(m_data));	
		} catch(IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(0);
		}
		
		ChatFrame chatFrame = new ChatFrame(messenger);
		chatFrame.show();
	
		IGameLoader loader = m_data.getGameLoader();
		
		System.out.println("starting");
		ServerStartup startup = new ServerStartup(loader, m_data, messenger);
		startup.setVisible(true);
		
		startup.waitForPlayers();
		startup.dispose();
		
		ServerWaitForClientMessageListener listener = new ServerWaitForClientMessageListener();
		messenger.addMessageListener(listener);
		
		Map playerMapping = startup.getLocalPlayerMapping();
		Set playerSet = loader.createPlayers(playerMapping);
		Map remotePlayers = startup.getRemotePlayerMapping();
			
		ServerGame serverGame = new ServerGame(m_data, playerSet, messenger, remotePlayers);
		
		chatFrame.getChat().showTranscript(serverGame.getTranscript());
		
		loader.startGame(serverGame, playerSet);
		
		listener.waitFor( new HashSet(playerMapping.values()).size());
		messenger.removeMessageListener(listener);
		
		serverGame.startGame();
	}
	
	public void startClient()
	{
		System.out.println("Client");
		
		ClientOptions options = new ClientOptions("Client", PORT, "127.0.0.1");
		options.show();
		options.waitForOptions();
		options.dispose();
		
		String name = options.getName();
		int port = options.getPort();
		String address = options.getAddress();
		
		ClientMessenger messenger = null;
		try
		{
			messenger = new ClientMessenger(address, port, name, new GameObjectStreamFactory(m_data));
		} catch(Exception e)
		{
			JOptionPane.showMessageDialog(null, e.getMessage());
			startClient();
			return;
		}
		
		ChatFrame chatFrame = new ChatFrame(messenger);
		
		chatFrame.show();
		
		IGameLoader loader = m_data.getGameLoader();
		
		ClientStartup startup = new ClientStartup(loader, m_data, messenger);
		startup.setVisible(true);
		
		startup.waitForPlayers();
		startup.dispose();
		
		Map playerMapping = startup.getLocalPlayerMapping();
		Set playerSet = loader.createPlayers(playerMapping);
		
		
		
		ClientGame clientGame = new ClientGame(m_data, playerSet, messenger, messenger.getServerNode());
		
		chatFrame.getChat().showTranscript(clientGame.getTranscript());
		
		m_data.getGameLoader().startGame(clientGame, playerSet);
		
		messenger.send(new ClientReady(), messenger.getServerNode());
	}
	
	public void startLocalGame()
	{
		IServerMessenger messenger = new DummyMessenger();

		java.util.List players = games.strategy.util.Util.toList(m_data.getPlayerList().getNames());
		
		Map localPlayerMap = new HashMap();
		Iterator playerIter = players.iterator();
		while(playerIter.hasNext())
		{
			localPlayerMap.put(playerIter.next(), "local");
		}
		
		Set gamePlayers = m_data.getGameLoader().createPlayers(localPlayerMap);		
		ServerGame game = new ServerGame(m_data, gamePlayers, messenger, new HashMap());
		m_data.getGameLoader().startGame(game, gamePlayers);
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