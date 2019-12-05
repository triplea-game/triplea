package org.triplea.server.remote.actions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.remote.actions.messages.server.RemoteActionsEnvelopeFactory;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

@ExtendWith(MockitoExtension.class)
class RemoteActionsEventQueueTest {
  private static final InetAddress IP = IpAddressParser.fromString("33.33.33.33");

  @Mock private BiConsumer<Collection<Session>, ServerMessageEnvelope> messageBroadcaster;
  @Mock private BiConsumer<Session, ServerMessageEnvelope> messageSender;
  @Mock private SessionTracker sessionTracker;

  private RemoteActionsEventQueue remoteActionsEventQueue;

  @Mock private Session session1;
  @Mock private Session session2;

  @BeforeEach
  void setup() {
    remoteActionsEventQueue =
        RemoteActionsEventQueue.builder()
            .messageBroadcaster(messageBroadcaster)
            .messageSender(messageSender)
            .sessionTracker(sessionTracker)
            .build();
  }

  @Nested
  class AddingAndRemovingSessions {
    @Test
    @DisplayName("Adding a session adds to the session tracker")
    void addSession() {
      remoteActionsEventQueue.addSession(session1);
      verify(sessionTracker).addSession(session1);
    }

    @Test
    @DisplayName("Removing a session removes the session from the session tracker")
    void removeListener() {
      remoteActionsEventQueue.removeSession(session1);
      verify(sessionTracker).removeSession(session1);
    }
  }

  @Nested
  class PlayerBannedEvent {
    @Test
    @DisplayName("Player bans are broadcasted to all sessions")
    void addPlayerBannedEvent() {
      when(sessionTracker.getSessions()).thenReturn(Set.of(session1, session2));

      remoteActionsEventQueue.addPlayerBannedEvent(IP);

      verify(messageBroadcaster)
          .accept(Set.of(session1, session2), RemoteActionsEnvelopeFactory.newBannedPlayer(IP));
    }
  }

  @Nested
  class ShutdownRequest {
    @Test
    @DisplayName("Shutdown requests are sent only to the target IP")
    void shutdownRequest() {
      when(sessionTracker.getSessionsByIp(IP)).thenReturn(Set.of(session1));

      remoteActionsEventQueue.addShutdownRequestEvent(IP);

      verifyShutdownMessageIsSentToSession(session1);
    }

    private void verifyShutdownMessageIsSentToSession(final Session session) {
      verify(messageSender, atLeastOnce())
          .accept(session, RemoteActionsEnvelopeFactory.newShutdownMessage());
    }

    @Test
    @DisplayName("Shutdown requests are sent to each session identified by an IP")
    void shutdownRequestWithMultipleSessionsOnAnIp() {
      when(sessionTracker.getSessionsByIp(IP)).thenReturn(Set.of(session1, session2));

      remoteActionsEventQueue.addShutdownRequestEvent(IP);

      verifyShutdownMessageIsSentToSession(session1);
      verifyShutdownMessageIsSentToSession(session2);
    }

    @Test
    @DisplayName("Shutdown requests for non-existent IPs is a no-op")
    void verifyNoOpForNonExistentSession() {
      when(sessionTracker.getSessionsByIp(IP)).thenReturn(Set.of());

      remoteActionsEventQueue.addShutdownRequestEvent(IP);

      verify(messageSender, never()).accept(any(), any());
    }
  }
}
