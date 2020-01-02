package org.triplea.server.lobby.game.listing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.lobby.game.listing.messages.GameListingMessageType;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.server.TestData;
import org.triplea.server.http.web.socket.SessionSet;

@ExtendWith(MockitoExtension.class)
class GameListingEventQueueTest {

  @Mock private BiConsumer<Collection<Session>, ServerMessageEnvelope> broadcaster;

  private GameListingEventQueue gameListingEventQueue;

  @Mock private Session openSession;
  @Mock private Session closedSession;
  private SessionSet sessionSet = new SessionSet();

  @BeforeEach
  void setup() {
    gameListingEventQueue =
        GameListingEventQueue.builder().sessionSet(sessionSet).broadcaster(broadcaster).build();
  }

  @Test
  void hasNoListenersInitially() {
    assertThat(gameListingEventQueue.getSessionSet().values(), hasSize(0));
  }

  @Test
  void addListener() {
    when(openSession.isOpen()).thenReturn(true);
    gameListingEventQueue.addListener(openSession);

    assertThat(gameListingEventQueue.getSessionSet().values(), hasSize(1));
  }

  @Test
  void removeListener() {
    gameListingEventQueue.addListener(openSession);

    gameListingEventQueue.removeListener(openSession);

    assertThat(gameListingEventQueue.getSessionSet().values(), hasSize(0));
  }

  @Test
  void gameRemoved() {
    when(openSession.isOpen()).thenReturn(true);
    gameListingEventQueue.addListener(openSession);
    when(closedSession.isOpen()).thenReturn(false);
    gameListingEventQueue.addListener(closedSession);

    gameListingEventQueue.gameRemoved("gameId");

    final ArgumentCaptor<Collection<Session>> broadcasterArguments =
        ArgumentCaptor.forClass(Collection.class);
    final ArgumentCaptor<ServerMessageEnvelope> serverMessage =
        ArgumentCaptor.forClass(ServerMessageEnvelope.class);
    verify(broadcaster).accept(broadcasterArguments.capture(), serverMessage.capture());
    verifyBroadcastToSession(broadcasterArguments.getValue(), openSession);
    assertThat(
        "Game removed payload should be the removed game id",
        serverMessage.getValue(),
        is(
            ServerMessageEnvelope.packageMessage(
                GameListingMessageType.GAME_REMOVED.name(), "gameId")));
    assertThat(
        "Verify closed session is removed",
        gameListingEventQueue.getSessionSet().values(),
        hasSize(1));
  }

  private void verifyBroadcastToSession(
      final Collection<Session> broadcastedSessions, final Session session) {
    assertThat("Expect broadcast to just the open session", broadcastedSessions, hasSize(1));
    assertThat(
        "Expect the open session to be the one we broadcasted to",
        broadcastedSessions,
        hasItem(session));
  }

  @Test
  @DisplayName("Update game, verify update game message is sent to open sessions")
  void gameUpdated() {
    when(openSession.isOpen()).thenReturn(true);
    gameListingEventQueue.addListener(openSession);
    when(closedSession.isOpen()).thenReturn(false);
    gameListingEventQueue.addListener(closedSession);
    final LobbyGameListing lobbyGameListing =
        LobbyGameListing.builder().gameId("gameId").lobbyGame(TestData.LOBBY_GAME).build();

    gameListingEventQueue.gameUpdated(lobbyGameListing);

    final ArgumentCaptor<Collection<Session>> broadcasterArguments =
        ArgumentCaptor.forClass(Collection.class);
    final ArgumentCaptor<ServerMessageEnvelope> serverMessage =
        ArgumentCaptor.forClass(ServerMessageEnvelope.class);
    verify(broadcaster).accept(broadcasterArguments.capture(), serverMessage.capture());
    verifyBroadcastToSession(broadcasterArguments.getValue(), openSession);
    assertThat(
        serverMessage.getValue(),
        is(
            ServerMessageEnvelope.packageMessage(
                GameListingMessageType.GAME_UPDATED.name(), lobbyGameListing)));
    assertThat(
        "Verify closed session is removed",
        gameListingEventQueue.getSessionSet().values(),
        hasSize(1));
  }
}
