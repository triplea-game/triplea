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


package games.strategy.engine.framework.ui;

import games.strategy.engine.chat.ChatFrame;
import games.strategy.engine.data.*;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.*;
import games.strategy.engine.message.*;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.random.*;
import games.strategy.net.*;
import games.strategy.triplea.Constants;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;

import javax.swing.*;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class LauncherFrame extends JFrame
{
    
  public static final String CLIENT_READY_CHANNEL = "games.strategy.engine.framework.ui.LauncherFrame.CLIENT_READY_CHANNEL";  
    
  private JTabbedPane m_mainTabPanel = new JTabbedPane();
  private JPanel m_buttonPanel = new JPanel();
  private JButton m_playButton = new JButton();
  private JButton m_cancelButton = new JButton();
  private GameTypePanel m_gameTypePanel = new GameTypePanel();
  private GameData m_gameData;
  private PropertiesUI m_propertuesUI;
  private IMessenger m_messenger;
  private IChannelMessenger m_channelMessenger;
  private IRemoteMessenger m_remoteMessenger;
  private IMessageManager m_messageManager;
  private ChatFrame m_chat;
  private ServerStartup m_serverStartup;
  private ClientStartup m_clientStartup;
  private GameObjectStreamFactory m_objectStreamFactory = new GameObjectStreamFactory(null);
  private PBEMStartup m_pbemStartup;
  private LocalPlayerSelectionPanel m_localPlayerTypes = new LocalPlayerSelectionPanel();


  private final String PLAYERNAME = "PlayerName";

  public LauncherFrame()
  {
    super("Triplea");
    try
    {
      jbInit();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    setWidgetActivation();
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    m_gameTypePanel.initializeWithDefaultFile();
    setWidgetActivation();
  }

  private void jbInit() throws Exception
  {
    m_playButton.setToolTipText("");
    m_playButton.setText("Play");
    m_playButton.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        m_PlayButton_actionPerformed(e);
      }
    });
    m_cancelButton.setText("Exit");
    m_cancelButton.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        m_cancelButton_actionPerformed(e);
      }
    });
    this.getContentPane().add(m_mainTabPanel,  BorderLayout.CENTER);
    m_mainTabPanel.add(m_gameTypePanel,  "Game");
    this.getContentPane().add(m_buttonPanel,  BorderLayout.SOUTH);
    m_buttonPanel.add(m_playButton, null);
    m_buttonPanel.add(m_cancelButton, null);
    m_gameTypePanel.setLauncherFrame(this);
  }

  public void setGameData(GameData data)
  {
    m_gameData = data;
     m_localPlayerTypes.setGameData(m_gameData);
    updatePropertiesPanel();
    setWidgetActivation();

    if(m_serverStartup != null)
      m_serverStartup.setGameData(data);

    m_objectStreamFactory.setData(data);


  }

  private void updateLocalPlayerTypes()
  {
    m_mainTabPanel.remove(m_localPlayerTypes);

    //we dont do this on client play
    if(!m_gameTypePanel.isLocal())
      return;

    if(m_gameData == null)
      return;

    if (m_gameData.getGameLoader().getServerPlayerTypes().length != 1 )
    {
      int tabIndex = m_propertuesUI == null ? 1 :2;
      m_mainTabPanel.add(m_localPlayerTypes, "Player Types", tabIndex);

    }
  }


  private void updatePropertiesPanel()
  {
    //clear the old properties
    m_mainTabPanel.remove(m_propertuesUI);
    m_propertuesUI = null;

    if(m_gameData == null)
      return;

    if(m_gameData.getProperties().getEditableProperties().isEmpty())
      return;

    m_propertuesUI = new PropertiesUI(m_gameData.getProperties(), true);
    m_mainTabPanel.add(m_propertuesUI, "Properties", 1);



    m_mainTabPanel.setTabPlacement(1);
  }

  public GameData getGameData()
  {
    return m_gameData;
  }

  void m_PlayButton_actionPerformed(ActionEvent e)
  {
    if(m_gameTypePanel.isLocal() || m_gameTypePanel.isPBEM())
    {
      startLocal();
    }
    if(m_gameTypePanel.isServer())
    {
      startServer();
    }

  }

  public static byte[] gameDataToBytes(GameData data) throws IOException
  {
      ByteArrayOutputStream sink = new ByteArrayOutputStream(25000);

      new GameDataManager().saveGame(sink, data);
      sink.flush();
      sink.close();
      return sink.toByteArray();
  }

  
  private void startServer()
  {
    m_serverStartup.cleanUpWaitForPlayers();
    m_serverStartup.cleanUp();

    ServerReady serverReady = new ServerReady();
    m_remoteMessenger.registerRemote(IServerReady.class, serverReady, CLIENT_READY_CHANNEL);
    
    byte[] gameDataAsBytes;
    try
    {
        gameDataAsBytes = gameDataToBytes(m_gameData);
    } catch (IOException e)
    {
        e.printStackTrace();
        throw new IllegalStateException(e.getMessage());
    }
    
    ((IClientChannel) m_channelMessenger.getChannelBroadcastor(IClientChannel.CHANNEL_NAME)).doneSelectingPlayers(gameDataAsBytes);

    Map localPlayerMapping = m_serverStartup.getLocalPlayerMapping();
    Set localPlayerSet = m_gameData.getGameLoader().createPlayers(localPlayerMapping);
    Map remotePlayers = m_serverStartup.getRemotePlayerMapping();

    final ServerGame serverGame = new ServerGame(m_gameData, localPlayerSet,(IServerMessenger) m_messenger, remotePlayers, m_channelMessenger, m_remoteMessenger, m_messageManager);
    boolean useSecureRandomSource = !remotePlayers.isEmpty() && !localPlayerMapping.isEmpty();
    if(useSecureRandomSource)
    {
      //server game.
      //if we have two players, use a crypto random source.
       PlayerID remotePlayer = m_gameData.getPlayerList().getPlayerID(( String) remotePlayers.keySet().iterator().next());
       
       CryptoRandomSource randomSource = new CryptoRandomSource(remotePlayer, serverGame);
       serverGame.setRandomSource(randomSource);
       System.out.println("Using secure random with remote player " + remotePlayer.getName());
    }



    m_gameData.getGameLoader().startGame(serverGame, localPlayerSet);

    
    serverReady.waitFor( m_messenger.getNodes().size() - 1);
    m_remoteMessenger.unregisterRemote(CLIENT_READY_CHANNEL);
    
    if (useSecureRandomSource)
    {
        //the first roll takes a while, initialize
        //here in the background so that the user doesnt notice
        Thread t = new Thread()
        {
            public void run()
            {
                serverGame.getRandomSource().getRandom(Constants.MAX_DICE, 2, "Warming up crpyto random source");
            }
        };
        t.start();

    }
    
    //make sure all the communicationis done
    m_messenger.waitForAllMessagsToBeProcessed();
    

    Thread t = new Thread()
    {
      public void run()
      {
        serverGame.startGame();
      }
    };
    t.start();

    setVisible(false);




  }

  void startClient()
  {
    Map playerMapping = m_clientStartup.getLocalPlayerMapping();
    Set playerSet =  m_gameData.getGameLoader().createPlayers(playerMapping);

    ClientGame clientGame = new ClientGame(m_gameData, playerSet, m_messenger, ((ClientMessenger) m_messenger).getServerNode(), m_channelMessenger, m_remoteMessenger, m_messageManager);


    m_gameData.getGameLoader().startGame(clientGame, playerSet);

    ((IServerReady) m_remoteMessenger.getRemote(LauncherFrame.CLIENT_READY_CHANNEL)).clientReady();
 
    setVisible(false);
  }

  public void setGameInfo(String gameName, String gameVersion)
  {
    m_gameTypePanel.setGameName(gameName);
    m_gameTypePanel.setGameVersion(gameVersion);
  }

  private void startLocal()
  {
    this.setVisible(false);

    Runnable runner = new Runnable()
    {
      public void run()
      {
        IServerMessenger messenger = new DummyMessenger();
        m_messenger = messenger;
        m_channelMessenger = new ChannelMessenger(m_messenger);
        m_messageManager = new MessageManager(m_messenger);
        m_remoteMessenger = new RemoteMessenger(m_messageManager, m_messenger);

        java.util.List players = games.strategy.util.Util.toList(m_gameData.getPlayerList().getNames());

        Map localPlayerMap = new HashMap();
        Iterator playerIter = players.iterator();
        while(playerIter.hasNext())
        {
          String playerName = (String) playerIter.next();
          localPlayerMap.put(playerName, m_localPlayerTypes.getPlayerType(playerName));
        }

        Set gamePlayers = m_gameData.getGameLoader().createPlayers(localPlayerMap);
        ServerGame game = new ServerGame(m_gameData, gamePlayers, messenger, new HashMap(), m_channelMessenger, m_remoteMessenger, m_messageManager);
        if(m_gameTypePanel.isPBEM())
        {
          IronyGamesDiceRollerRandomSource randomSource = new IronyGamesDiceRollerRandomSource(m_pbemStartup.getEmail1(), m_pbemStartup.getEmail2());
          game.setRandomSource(randomSource);
        }

        m_gameData.getGameLoader().startGame(game, gamePlayers);

        game.startGame();

      }
    };
    Thread thread = new Thread(runner);
    thread.start();
    setVisible(false);
  }


  public void clearGameType()
  {
    if(m_messenger != null)
    {
      m_messenger.shutDown();
      m_messenger = null;
    }

    if(m_serverStartup != null)
    {
      m_mainTabPanel.remove(m_serverStartup);
      m_serverStartup = null;
    }

    if(m_clientStartup != null)
    {
      m_mainTabPanel.remove(m_clientStartup);
      m_clientStartup = null;
    }

    if(m_pbemStartup != null)
    {
      m_mainTabPanel.remove(m_pbemStartup);
      m_pbemStartup = null;

    }

    if(m_chat != null)
    {
      m_chat.setVisible(false);
      m_chat = null;
    }


  }

  public void chooseClientServerOptions()
  {

    if(m_gameTypePanel.isServer())
    {
      chooseServerOptions();
    }
    else if(m_gameTypePanel.isClient())
    {
      chooseClientOptions();
    }
    else if(m_gameTypePanel.isPBEM())
    {
      choosePBEMOptions();
    }

    setWidgetActivation();
  }

  private void chooseServerOptions()
  {
  	//load in the saved name! -- lnxduk
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    String playername = prefs.get(PLAYERNAME, "Server");

    ServerOptions options = new ServerOptions(this,playername, GameRunner.PORT);
    options.setLocationRelativeTo(this);

    options.show();
    options.dispose();

    if(!options.getOKPressed())
    {
      m_gameTypePanel.setLocal();
      return;
    }


    String name = options.getName();
  	//save the name! -- lnxduk
    prefs.put(PLAYERNAME, name);

    int port = options.getPort();
    if(port>=65536||port==0){
        JOptionPane.showMessageDialog(this,"Invalid Port: "+port, "Error", JOptionPane.ERROR_MESSAGE);
    	return;
  	}
    
    ServerMessenger messenger;
    try
    {
      messenger= new ServerMessenger(name, port, m_objectStreamFactory);
    } catch(IOException ioe)
    {
      ioe.printStackTrace();
      JOptionPane.showMessageDialog(this, "Unable to create server socket:" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    m_messenger = messenger;
    m_channelMessenger = new ChannelMessenger(m_messenger);
    m_messageManager = new MessageManager(m_messenger);
    m_remoteMessenger = new RemoteMessenger(m_messageManager, m_messenger);
    
    m_chat = new ChatFrame(messenger, m_channelMessenger);
    m_chat.show();

    m_serverStartup = new ServerStartup( messenger, m_remoteMessenger, m_channelMessenger);
    if(m_gameData != null)
      m_serverStartup.setGameData(m_gameData);

    m_mainTabPanel.add(m_serverStartup, "Server");
    m_serverStartup.setLauncerFrame(this);
    m_serverStartup.waitForPlayers();
  }

  private void choosePBEMOptions()
  {
    m_pbemStartup = new PBEMStartup();
    m_mainTabPanel.add(m_pbemStartup, "PBEM");
  }

  private void chooseClientOptions()
  {
  	//load in the saved name!
    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
    String playername = prefs.get(PLAYERNAME, "Client");

    ClientOptions options = new ClientOptions(this,playername, GameRunner.PORT, "127.0.0.1");
    options.setLocationRelativeTo(this);
    options.show();
    options.dispose();

    if(!options.getOKPressed())
    {
      m_gameTypePanel.setLocal();
      return;
    }

    String name = options.getName();
  	//save the name! -- lnxduk
    prefs.put(PLAYERNAME, name);

    int port = options.getPort();
    if(port>=65536||port==0){
        JOptionPane.showMessageDialog(this,"Invalid Port: "+port, "Error", JOptionPane.ERROR_MESSAGE);
    	return;
  	}

    
    String address = options.getAddress();

    try
    {
      m_messenger = new ClientMessenger(address, port, name, m_objectStreamFactory);
    } catch(Exception ioe)
    {
      ioe.printStackTrace();
      JOptionPane.showMessageDialog(this, "Unable to create server socket:" + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    m_channelMessenger = new ChannelMessenger(m_messenger);
    m_messageManager = new MessageManager(m_messenger);
    m_remoteMessenger = new RemoteMessenger(m_messageManager, m_messenger);
    	
    m_chat= new ChatFrame(m_messenger, m_channelMessenger);
    m_chat.show();

    m_clientStartup = new ClientStartup(m_channelMessenger, m_remoteMessenger);
    m_clientStartup.setLauncherFrame(this);


    m_mainTabPanel.add(m_clientStartup, "Client");
    m_clientStartup.waitForPlayers();

    setGameData(null);


  }

  void setWidgetActivation()
  {
    setPlayActivation();
    updateLocalPlayerTypes();
  }

  private void setPlayActivation()
  {
    //play
    boolean playEnabled = true;
    //disable play if we are client
    if(m_clientStartup != null)
      playEnabled = false;
    //disable if no game loaded and not client
    if(m_clientStartup == null && m_gameData == null)
      playEnabled = false;
    //disable if server and not all slots filled
    if(m_serverStartup != null && !m_serverStartup.allPlayersFilled())
      playEnabled = false;
    m_playButton.setEnabled(playEnabled);
  }

  void m_cancelButton_actionPerformed(ActionEvent e)
  {
    try
    {
      if(m_messenger != null)
        m_messenger.shutDown();
    }
    finally
    {
      System.exit(0);
    }
  }



}

/**
 * 
 * Allows for the server to wait for all clients to finish initialization before starting the game
 *
 * @author Sean Bridges
 */
interface IServerReady extends IRemote
{
    public void clientReady();
}

class ServerReady implements IServerReady
{
  int m_count = 0;

  public void clientReady()
  {
      synchronized(this)
      {
        m_count++;
        this.notifyAll();
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

