package org.triplea.server.access;

import java.net.InetAddress;
import java.util.Collection;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.server.http.web.socket.SessionSet;

/**
 * Provides a callback mechanism to close all sessions of a banned player (banned by IP). Each
 * websocket will be provided a {@code SessionSet} where any new sessions will be registered, each
 * such {@code SessionSet} is also registered here and then receives a callback to close any
 * sessions for a banned IP.
 */
@Builder
public class BannedPlayerEventHandler {

  @NonNull private final Collection<SessionSet> sessionSets;

  public void fireBannedEvent(final InetAddress bannedIp) {
    sessionSets.forEach(sessionSet -> sessionSet.closeSessionsByIp(bannedIp));
  }
}
