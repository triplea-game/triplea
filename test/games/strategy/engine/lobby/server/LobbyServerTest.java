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
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsNull;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.engine.framework.ui.NewGameChooser;
import games.strategy.engine.lobby.client.LobbyClient;
import games.strategy.engine.lobby.client.login.LobbyLogin;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.engine.lobby.client.login.LoginPanel;
import games.strategy.engine.lobby.client.ui.LobbyFrame;
import games.strategy.engine.lobby.client.ui.LobbyGamePanel;
import games.strategy.engine.lobby.server.login.LobbyLoginValidator;
import games.strategy.net.ClientMessenger;
import games.strategy.net.CouldNotLogInException;
import games.strategy.net.GUID;
import games.strategy.net.IConnectionLogin;
import games.strategy.net.MacFinder;
import games.strategy.util.MD5Crypt;

public class LobbyServerTest {

  @BeforeClass
  public static void testSetup() throws Exception {
    LobbyServer.main(new String[0]);
  }


  @AfterClass
  public static void testTeardown() {
    LobbyServer.stopServer();
  }


  @Test
  public void connectToLobby() {
    LobbyServerProperties props = new LobbyServerProperties("127.0.0.1", LobbyServer.DEFAULT_LOBBY_PORT, "", "the server says");

    final LobbyClient client = login( props );
    assertThat(client, notNullValue());

    Map<GUID, GameDescription> games = ((ILobbyGameController) client.getMessengers().getRemoteMessenger().getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE)).listGames();
    assertThat( games.keySet(), hasSize(0));

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



}
