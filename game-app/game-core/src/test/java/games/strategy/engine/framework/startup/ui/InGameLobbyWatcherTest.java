package games.strategy.engine.framework.startup.ui;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.startup.ui.InGameLobbyWatcher.getLobbySystemProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.framework.startup.WatcherThreadMessaging;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import java.net.InetAddress;
import java.util.Optional;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingResponse;
import org.triplea.http.client.web.socket.client.connections.GameToLobbyConnection;

@ExtendWith(MockitoExtension.class)
final class InGameLobbyWatcherTest {

  @Mock private IServerMessenger mockIServerMessenger;

  @Mock private GameToLobbyConnection mockGameToLobbyConnection;

  @Mock private WatcherThreadMessaging mockWatcherThreadMessaging;

  @Mock private InGameLobbyWatcher mockInGameLobbyWatcher;

  @Mock private INode mockINode;

  @Mock private GamePostingResponse mockGamePostingResponse;

  @Test
  public void testNewInGameLobbyWatcher_hostNotReachable() throws Exception {

    // need to set LOBBY_GAME_COMMENTS system property to avoid NPE in
    // SystemPropertyReader.gameComments()
    System.setProperty(LOBBY_GAME_COMMENTS, "testLobbyGameComments");

    when(mockIServerMessenger.getLocalNode()).thenReturn(mockINode);
    when(mockGameToLobbyConnection.getPublicVisibleIp()).thenReturn(InetAddress.getLocalHost());
    when(mockGameToLobbyConnection.postGame(any(GamePostingRequest.class)))
        .thenReturn(mockGamePostingResponse);

    // simulate "Your computer is not reachable from the internet"
    when(mockGamePostingResponse.isConnectivityCheckSucceeded()).thenReturn(false);

    /* Issue 10251: (https://github.com/triplea-game/triplea/issues/10251)
     An extra error message is shown when a player attempts to host a game on the lobby and the call to 'reverse connect'
     back to the game host fails. This is caused by the assignment to keepAliveTimer in the private InGameLobbyWatcher
     Constructor which takes 6 arguments. As part of the assignment of keepAliveTimer in that Constructor, a
     LobbyWatcherKeepAliveTask instance will try to be created via a builder with a null gameId, which will throw a NPE
     (thrown by javax.annotation.nonnull annotation on gameId). This assignment is ultimately unneeded in this case since
     the connection failed. To remediate this issue, when the 'reverse connect' fails, both the keepAliveTimer and
     connectionChangeListener can be set to null (since those fields are final and must be initialized) and we can break
     out of the constructor method via "return;". This will avoid the assignment of keepAliveTimer
     (and the NPE causing the additional error window) later in the private constructor code. Additionally,
     InGameLobbyWatcher.shutDown was updated to only call GameToLobbyConnection.disconnect if the gameId is not null
     to avoid an IllegalArgumentException that will be thrown by LobbyWatcherClient.removeGame.
    */
    Optional<InGameLobbyWatcher> createdWatcherOptional =
        InGameLobbyWatcher.newInGameLobbyWatcher(
            mockIServerMessenger,
            mockGameToLobbyConnection,
            mockWatcherThreadMessaging,
            mockInGameLobbyWatcher,
            true);

    assertTrue(createdWatcherOptional.isPresent());
    assertNull(createdWatcherOptional.get().getGameId());

    verify(mockIServerMessenger, times(2)).getLocalNode();
    verify(mockGameToLobbyConnection).getPublicVisibleIp();
    verify(mockGameToLobbyConnection).postGame(any(GamePostingRequest.class));
    verify(mockGamePostingResponse).isConnectivityCheckSucceeded();

    // verify GameToLobbyConnection.disconnect is not called since gameId is null
    verify(mockGameToLobbyConnection, never()).disconnect(null);
  }

  @Test
  public void testShutDown_gameIdNotNull() throws Exception {
    final String testGameId = "testGameId";

    // need to set LOBBY_GAME_COMMENTS system property to avoid NPE in
    // SystemPropertyReader.gameComments()
    System.setProperty(LOBBY_GAME_COMMENTS, "testLobbyGameComments");

    when(mockIServerMessenger.getLocalNode()).thenReturn(mockINode);
    when(mockGameToLobbyConnection.getPublicVisibleIp()).thenReturn(InetAddress.getLocalHost());
    when(mockGameToLobbyConnection.postGame(any(GamePostingRequest.class)))
        .thenReturn(mockGamePostingResponse);
    when(mockGamePostingResponse.isConnectivityCheckSucceeded()).thenReturn(true);
    when(mockGamePostingResponse.getGameId()).thenReturn(testGameId);

    // Create an InGameLobbyWatcher with a non-null gameId and call shutDown to verify
    // GameToLobbyConnection.disconnect(gameId) is still called
    Optional<InGameLobbyWatcher> createdWatcherOptional =
        InGameLobbyWatcher.newInGameLobbyWatcher(
            mockIServerMessenger,
            mockGameToLobbyConnection,
            mockWatcherThreadMessaging,
            mockInGameLobbyWatcher,
            true);

    assertTrue(createdWatcherOptional.isPresent());
    Assertions.assertNotNull(createdWatcherOptional.get().getGameId());

    createdWatcherOptional.get().shutDown();

    verify(mockIServerMessenger, atLeastOnce()).getLocalNode();
    verify(mockGameToLobbyConnection).getPublicVisibleIp();
    verify(mockGameToLobbyConnection, atLeastOnce()).postGame(any(GamePostingRequest.class));
    verify(mockGamePostingResponse, atLeastOnce()).isConnectivityCheckSucceeded();
    verify(mockGamePostingResponse, atLeastOnce()).getGameId();
    verify(mockGameToLobbyConnection).disconnect(testGameId);
  }

  @Nested
  final class GetLobbySystemPropertyTest {
    private static final String KEY = "__GetLobbySystemPropertyTest__key";
    @NonNls private static final String BACKUP_KEY = KEY + ".backup";
    private static final String VALUE = "primaryValue";
    private static final String BACKUP_VALUE = "backupValue";

    @AfterEach
    void clearSystemProperties() {
      givenPrimaryValueNotSet();
      givenBackupValueNotSet();
    }

    private void givenPrimaryValueSet() {
      System.setProperty(KEY, VALUE);
    }

    private void givenPrimaryValueNotSet() {
      System.clearProperty(KEY);
    }

    private void givenBackupValueSet() {
      System.setProperty(BACKUP_KEY, BACKUP_VALUE);
    }

    private void givenBackupValueNotSet() {
      System.clearProperty(BACKUP_KEY);
    }

    @Test
    void shouldReturnPrimaryValueWhenPrimaryValueSet() {
      givenPrimaryValueSet();

      assertThat(getLobbySystemProperty(KEY), is(VALUE));
    }

    @Test
    void shouldCopyPrimaryValueToBackupValueWhenPrimaryValueSet() {
      givenPrimaryValueSet();

      getLobbySystemProperty(KEY);

      assertThat(System.getProperty(BACKUP_KEY), is(VALUE));
    }

    @Test
    void shouldReturnBackupValueWhenPrimaryValueNotSet() {
      givenPrimaryValueNotSet();
      givenBackupValueSet();

      assertThat(getLobbySystemProperty(KEY), is(BACKUP_VALUE));
    }

    @Test
    void shouldReturnNullWhenPrimaryValueNotSetAndBackupValueNotSet() {
      givenPrimaryValueNotSet();
      givenBackupValueNotSet();

      assertThat(getLobbySystemProperty(KEY), is(nullValue()));
    }
  }
}
