package games.strategy.engine.lobby.server;

import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.login.LoginPanel;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.lobby.client.ui.LobbyGamePanel;
import games.strategy.engine.lobby.server.GameDescription.GameStatus;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.INode;
import games.strategy.net.MacFinder;
import games.strategy.net.Node;
import games.strategy.util.MD5Crypt;

public class LobbyServerTest {


  private ILobbyGameController lobbyController;


  @Before
  public void testSetup() throws Exception {

    LobbyServer.main(new String[0]);

    LobbyServerProperties props = new LobbyServerProperties("127.0.100.1", LobbyServer.DEFAULT_LOBBY_PORT, "", "the server says");

    final LobbyClient client = login( props );
    assertThat(client, notNullValue());
    lobbyController = (ILobbyGameController) client.getMessengers().getRemoteMessenger().getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE);
//    client.getMessengers().get
  }

  private static LobbyClient login(LobbyServerProperties props) {
    try {
      final String mac = MacFinder.GetHashedMacAddress();
      String userName = "testUser";
      final ClientMessenger messenger = new ClientMessenger(props.getHost(), props.getPort(),
          userName, mac, new IConnectionLogin() {
            @Override
            public void notifyFailedLogin(String message) {
              throw new IllegalStateException("failed to login: " + message);
            }

            @Override
            public Map<String, String> getProperties(final Map<String, String> challengProperties) {
              final Map<String, String> props = new HashMap<String, String>();
              props.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
              props.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
              return props;
            }
          });
      boolean isAnonymous = true;
      return new LobbyClient(messenger, isAnonymous);
    } catch (final CouldNotLogInException e) {
      throw new IllegalStateException(e);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }


  @After
  public void testTeardown() {
    LobbyServer.stopServer();
  }


  @Test
  public void listGamesIsInitiallyZero() {
    assertGameCountIs(0);
  }

  private void assertGameCountIs( int expectedValue ) {
    Map<GUID, GameDescription> games = lobbyController.listGames();
    assertThat( games.keySet(), hasSize(expectedValue));
  }

  @Test
  public void postGame() {
    lobbyController.postGame(new GUID(), createGame());
    assertGameCountIs(1);
    lobbyController.postGame(new GUID(), createGame());
    assertGameCountIs(2);
  }


  private static GameDescription createGame() {
    GameDescription game = new GameDescription();
    game.setHostedBy( createNode() );
    game.setPort( 15000 );
    game.setStartDateTime(new Date());
    game.setPlayerCount(1);
    game.setRound("1");
    game.setStatus( GameDescription.GameStatus.WAITING_FOR_PLAYERS );
    game.setGameVersion("1.0game_version");
    game.setEngineVersion("engine version");
    game.setHostName("I am host");
    game.setComment("commentary text");
    game.setGameName("i am game name");
    game.setPassworded(false);
    return game;
  }

  private static INode createNode() {
    try {
      return new Node( "nodeName", InetAddress.getByName("127.0.0.1"), 15000 );
    } catch (UnknownHostException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void postAndRemoveGame() {


//    lobbyController.updateGame(gameID, description);

  }
}
