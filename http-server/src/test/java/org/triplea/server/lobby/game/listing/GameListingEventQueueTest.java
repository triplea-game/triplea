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
import lombok.Builder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.lobby.game.listing.messages.GameListingMessageType;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.server.TestData;

@ExtendWith(MockitoExtension.class)
class GameListingEventQueueTest {

  @Mock private BiConsumer<Collection<Session>, ServerMessageEnvelope> broadcaster;

  private GameListingEventQueue gameListingEventQueue;

  @Mock private Session openSession;
  @Mock private Session closedSession;

  @BeforeEach
  void setup() {
    gameListingEventQueue = GameListingEventQueue.builder().broadcaster(broadcaster).build();
  }

  @Test
  void hasNoListenersInitially() {
    assertThat(gameListingEventQueue.getSessions().values(), hasSize(0));
  }

  @Test
  void addListener() {
    when(openSession.getId()).thenReturn("id0");
    gameListingEventQueue.addListener(openSession);

    assertThat(gameListingEventQueue.getSessions().values(), hasSize(1));
  }

  @Test
  void removeListener() {
    when(openSession.getId()).thenReturn("id0");
    gameListingEventQueue.addListener(openSession);

    gameListingEventQueue.removeListener("id0");

    assertThat(gameListingEventQueue.getSessions().values(), hasSize(0));
  }

  @Test
  void removeListenerIsNoOpWhenGameIdDoesNotMatch() {
    when(openSession.getId()).thenReturn("id0");
    gameListingEventQueue.addListener(openSession);

    gameListingEventQueue.removeListener("ID_DNE");

    assertThat(gameListingEventQueue.getSessions().values(), hasSize(1));
  }

  @Test
  void gameRemoved() {
    givenListener(openSession, SessionParameters.builder().open(true).sessionId("id0").build());
    givenListener(closedSession, SessionParameters.builder().open(false).sessionId("id1").build());

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
        gameListingEventQueue.getSessions().values(),
        hasSize(1));
  }

  @Builder
  private static class SessionParameters {
    private boolean open;
    private String sessionId;
  }

  private void givenListener(final Session session, final SessionParameters sessionParameters) {
    when(session.isOpen()).thenReturn(sessionParameters.open);
    when(session.getId()).thenReturn(sessionParameters.sessionId);

    gameListingEventQueue.addListener(session);
  }

  private void verifyBroadcastToSession(
      final Collection<Session> broadcastedSessions, final Session session) {
    assertThat("Expect broadcast to just the open session", broadcastedSessions, hasSize(1));
    assertThat(
        "Expect the open session to be the one we broadcasted to",
        broadcastedSessions,
        hasItem(openSession));
  }

  @Test
  void gameUpdated() {
    givenListener(openSession, SessionParameters.builder().open(true).sessionId("id0").build());
    givenListener(closedSession, SessionParameters.builder().open(false).sessionId("id1").build());

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
        gameListingEventQueue.getSessions().values(),
        hasSize(1));
  }
}
