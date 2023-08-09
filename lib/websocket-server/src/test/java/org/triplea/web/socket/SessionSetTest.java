package org.triplea.web.socket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.Collection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.IpAddressParser;

@ExtendWith(MockitoExtension.class)
class SessionSetTest {
  private static final InetAddress IP_0 = IpAddressParser.fromString("99.99.99.0");
  private static final InetAddress IP_1 = IpAddressParser.fromString("99.99.99.11");

  private final SessionSet sessionSet = new SessionSet();

  @Mock private WebSocketSession session;
  @Mock private WebSocketSession session0;
  @Mock private WebSocketSession session1;
  @Mock private WebSocketSession session2;
  @Mock private WebSocketSession session3;

  @Nested
  class Put {
    @Test
    @DisplayName("Simple add of a single session")
    void addSession() {
      sessionSet.put(session);

      assertThat(sessionSet.getSessions(), hasSize(1));
      assertThat(sessionSet.getSessions(), hasItem(session));
    }

    @Test
    @DisplayName("Add multiple sessions")
    void addMultipleSessions() {
      sessionSet.put(session);
      sessionSet.put(session0);

      assertThat(sessionSet.getSessions(), hasSize(2));
      assertThat(sessionSet.getSessions(), hasItems(session, session0));
    }
  }

  @Nested
  class Values {
    @Test
    @DisplayName("Verify session count starts empty at zero")
    void initiallyEmpty() {
      assertThat(sessionSet.values(), is(empty()));
    }

    @Test
    @DisplayName("Simply add a session, verify session count is one")
    void addASession() {
      when(session.isOpen()).thenReturn(true);
      sessionSet.put(session);
      assertThat(sessionSet.values(), hasSize(1));
    }

    @Test
    @DisplayName("Add a session, then remove, should be no sessions present")
    void removeSession() {
      sessionSet.put(session);
      sessionSet.remove(session);

      assertThat(sessionSet.values(), is(empty()));
    }

    @Test
    void removeSessionThatDoesNotExistIsNoOp() {
      sessionSet.remove(session);

      assertThat(sessionSet.values(), is(empty()));
    }

    @Test
    @DisplayName("Verify a closed session is not returned")
    void closedSessionsArePruned() {
      when(session.isOpen()).thenReturn(false);
      sessionSet.put(session);
      assertThat(sessionSet.values(), is(empty()));
    }

    @Test
    @DisplayName("Add multiple sessions, verify the non-closed sessions are returned")
    void verifyWithMultipleSessions() {
      when(session.isOpen()).thenReturn(false);
      sessionSet.put(session);
      when(session0.isOpen()).thenReturn(true);
      sessionSet.put(session0);
      when(session1.isOpen()).thenReturn(true);
      sessionSet.put(session1);

      assertThat("Added two open sessions", sessionSet.values(), hasSize(2));
    }
  }

  @Nested
  class GetSessionsByIp {
    @Test
    @DisplayName("No sessions to match, expect no sessions to be returned")
    void noSessions() {
      final Collection<WebSocketSession> matches = sessionSet.getSessionsByIp(IP_0);

      assertThat("session set contains no sessions to match", matches, is(empty()));
    }

    @Test
    @DisplayName("No sessions with a matching IP, expect none to be returned")
    void noMatchingSessions() {
      givenSessionIsOpenAndHasIp(session, IP_1);

      final Collection<WebSocketSession> matches = sessionSet.getSessionsByIp(IP_0);

      assertThat("IP_1 is in the set, but IP_0 is not", matches, is(empty()));
    }

    @Test
    @DisplayName("One matching sessions is closed, expect no sessions to be returned")
    void matchingSessionIsClosed() {
      when(session.isOpen()).thenReturn(false);
      sessionSet.put(session);

      final Collection<WebSocketSession> matches = sessionSet.getSessionsByIp(IP_0);

      assertThat("IP_0 matches, but the session is closed", matches, is(empty()));
    }

    @Test
    @DisplayName("Verify sessions filtering, multiple IPs, some open, some closed")
    void matchingSessionsOneClosedAndMultipleMatching() {
      givenSessionIsOpenAndHasIp(session, IP_0);
      givenSessionIsOpenAndHasIp(session0, IP_0);
      givenSessionIsClosedAndHasIp(session1, IP_0);

      givenSessionIsOpenAndHasIp(session2, IP_1);
      givenSessionIsClosedAndHasIp(session3, IP_1);

      final var matches = sessionSet.getSessionsByIp(IP_0);

      assertThat("Two open sessions match IP_0", matches, hasSize(2));
      assertThat(matches, hasItems(session, session0));
    }
  }

  @Nested
  class CloseSessionsByIp {
    @Test
    @DisplayName("No sessions present to close")
    void noSessions() {
      sessionSet.closeSessionsByIp(IP_0);
    }

    @Test
    @DisplayName("Session is already closed")
    void sessionAlreadyClosed() {
      givenSessionIsClosedAndHasIp(session, IP_0);
      sessionSet.closeSessionsByIp(IP_0);
      verify(session, never()).close();
    }

    @Test
    @DisplayName("Multiple matching sessions, expect open sessions to be closed")
    void multipleSessionsToClose() {
      givenSessionIsClosedAndHasIp(session, IP_0);
      givenSessionIsOpenAndHasIp(session0, IP_0);
      givenSessionIsOpenAndHasIp(session1, IP_0);
      givenSessionIsOpenAndHasIp(session2, IP_1);

      sessionSet.closeSessionsByIp(IP_0);

      // Session is closed (even though IP matches).
      verify(session, never()).close();

      // open sessions with matching IPs
      verify(session0).close();
      verify(session1).close();

      // Session is open, but IP does not match
      verify(session2, never()).close();
    }
  }

  private void givenSessionIsOpenAndHasIp(final WebSocketSession session, final InetAddress ip) {
    givenSessionWithIp(session, ip, true);
  }

  private void givenSessionIsClosedAndHasIp(final WebSocketSession session, final InetAddress ip) {
    givenSessionWithIp(session, ip, false);
  }

  private void givenSessionWithIp(
      final WebSocketSession session, final InetAddress ip, final boolean isOpen) {
    when(session.isOpen()).thenReturn(isOpen);

    if (isOpen) {
      when(session.getRemoteAddress()).thenReturn(ip);
    }

    sessionSet.put(session);
  }
}
