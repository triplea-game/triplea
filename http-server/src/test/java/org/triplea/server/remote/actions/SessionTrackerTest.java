package org.triplea.server.remote.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Collection;
import java.util.function.Function;
import javax.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.IpAddressParser;

@SuppressWarnings("SameParameterValue")
@ExtendWith(MockitoExtension.class)
class SessionTrackerTest {

  private static final String ID_1 = "id1";
  private static final String ID_2 = "id2";
  private static final String ID_3 = "id3";

  private static final InetAddress IP_1 = IpAddressParser.fromString("33.33.33.33");
  private static final InetAddress IP_2 = IpAddressParser.fromString("55.55.55.55");
  private static final InetAddress IP_3 = IpAddressParser.fromString("88.88.88.88");

  @Mock private Function<Session, InetAddress> ipExtractor;

  private SessionTracker sessionTracker;

  @Mock private Session session1;
  @Mock private Session session2;
  @Mock private Session session3;

  @BeforeEach
  void setup() {
    sessionTracker = SessionTracker.builder().ipAddressExtractor(ipExtractor).build();
  }

  private void givenSessionIdAndIp(final Session session, final String id, final InetAddress ip) {
    when(session.getId()).thenReturn(id);
    when(ipExtractor.apply(session)).thenReturn(ip);
  }

  private void givenClosedSessionIdAndIp(
      final Session session, final String id, final InetAddress ip) {
    when(session.getId()).thenReturn(id);
    when(ipExtractor.apply(session)).thenReturn(ip);
  }

  @Nested
  class AddAndGetSessions {
    @Test
    @DisplayName("Can add sessions")
    void addSession() {
      givenSessionIdAndIp(session1, ID_1, IP_1);
      when(session1.isOpen()).thenReturn(true);
      givenSessionIdAndIp(session2, ID_2, IP_1);
      when(session2.isOpen()).thenReturn(true);
      givenSessionIdAndIp(session3, ID_3, IP_2);
      when(session3.isOpen()).thenReturn(true);

      sessionTracker.addSession(session1);
      assertThat(sessionTracker.getSessions(), hasSize(1));

      sessionTracker.addSession(session2);
      assertThat(sessionTracker.getSessions(), hasSize(2));

      sessionTracker.addSession(session3);
      assertThat(sessionTracker.getSessions(), hasSize(3));
    }
  }

  @Nested
  class GettingSessions {
    @Test
    @DisplayName(
        "When we add sessions then get them, we expect to be returned the sessions we added")
    void simpleTestToGetAddedSessions() {
      givenSessionIdAndIp(session1, ID_1, IP_1);
      givenSessionIdAndIp(session2, ID_2, IP_1);
      sessionTracker.addSession(session1);
      sessionTracker.addSession(session2);
      when(session1.isOpen()).thenReturn(true);
      when(session2.isOpen()).thenReturn(true);

      final Collection<Session> sessions = sessionTracker.getSessions();
      assertThat(sessions, hasItems(session1, session2));
    }

    @Test
    @DisplayName("Verify we get only open sessions")
    void getSessionsReturnsOnlyOpenSessions() {
      givenSessionIdAndIp(session1, ID_1, IP_1);
      givenClosedSessionIdAndIp(session2, ID_2, IP_1);
      sessionTracker.addSession(session1);
      when(session1.isOpen()).thenReturn(true);
      sessionTracker.addSession(session2);
      when(session2.isOpen()).thenReturn(false);

      final Collection<Session> sessions = sessionTracker.getSessions();
      assertThat(sessions, hasItems(session1));
    }

    @Test
    @DisplayName("Verify we get only open sessions")
    void getSessionsRemovesClosedSessions() {
      givenClosedSessionIdAndIp(session1, ID_2, IP_1);
      sessionTracker.addSession(session1);

      sessionTracker.getSessions();
      sessionTracker.getSessions();

      // Expect explicitly and exactly one call to check is open, since we called
      // get sessions twice, the lack of a second call means the session was removed.
      verify(session1, times(1)).isOpen();
    }
  }

  @Nested
  class RemoveSession {
    @Test
    @DisplayName("Add 2 sessions then remove both of them, should result in no session")
    void removeSession() {
      givenSessionIdAndIp(session1, ID_1, IP_1);
      givenSessionIdAndIp(session2, ID_2, IP_1);

      sessionTracker.addSession(session1);
      sessionTracker.addSession(session2);
      when(session2.isOpen()).thenReturn(true);

      sessionTracker.removeSession(session1);
      assertThat(
          "Remove first session, expect one to remain", sessionTracker.getSessions(), hasSize(1));

      sessionTracker.removeSession(session2);
      assertThat("Remove second and last session", sessionTracker.getSessions(), is(empty()));
    }

    @Test
    @DisplayName("If a session DNE, removing it is a no-op")
    void removeNonExistentSessionIsNoOp() {
      givenSessionIdAndIp(session1, ID_1, IP_1);

      assertDoesNotThrow(() -> sessionTracker.removeSession(session1));
    }
  }

  @Nested
  class GetSessionByIP {

    @Test
    @DisplayName("With no sessions, getting session by IP returns empty")
    void emptyCase() {
      final Collection<Session> sessions = sessionTracker.getSessionsByIp(IP_1);

      assertThat(sessions, is(empty()));
    }

    @Test
    @DisplayName("Getting session by IP that is not added returns empty")
    void notTheRightIp() {
      givenSessionIdAndIp(session1, ID_1, IP_1);
      givenSessionIdAndIp(session2, ID_2, IP_2);

      sessionTracker.addSession(session1);
      sessionTracker.addSession(session2);

      final Collection<Session> sessions = sessionTracker.getSessionsByIp(IP_3);

      assertThat(sessions, is(empty()));
    }

    @Test
    @DisplayName("Getting session by IP that matches the one session returns that session")
    void singletonCase() {
      givenSessionIdAndIp(session1, ID_1, IP_1);
      sessionTracker.addSession(session1);

      final Collection<Session> sessions = sessionTracker.getSessionsByIp(IP_1);

      assertThat(sessions, hasItems(session1));
    }

    @Test
    @DisplayName("Getting session by IP where we have multiple return values")
    void matchMultipleSessionsByIp() {
      givenSessionIdAndIp(session1, ID_1, IP_1);
      givenSessionIdAndIp(session2, ID_2, IP_1);
      givenSessionIdAndIp(session3, ID_3, IP_2);

      sessionTracker.addSession(session1);
      sessionTracker.addSession(session2);
      sessionTracker.addSession(session3);

      final Collection<Session> sessions = sessionTracker.getSessionsByIp(IP_1);

      assertThat(sessions, hasItems(session1, session2));
    }
  }
}
