package org.triplea.server.remote.actions;

import java.net.InetAddress;
import java.util.Collection;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.websocket.Session;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.http.client.remote.actions.messages.server.RemoteActionsEnvelopeFactory;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.server.access.BannedPlayerEventHandler;
import org.triplea.server.http.web.socket.SessionSet;

/**
 * Event queue has a set of listeners for remote action events. We expect listeners to be added and
 * removed as websocket connections are made or closed to the remote actions websocket endpoint.
 * Remote action events such as shutdown request or a player is banned is published to the queue,
 * the queue then sends a websocket message to listeners notifying them of the event.
 *
 * <ul>
 *   Message Types:
 *   <li>PlayerBanned: This is broardcast to all listeners
 *   <li>Shutdown: This is done by IP, there may be multiple sessions for one IP, all sessions
 *       corresponding to a given IP are notified to shutdown.
 * </ul>
 */
@Builder
public class RemoteActionsEventQueue {

  @Nonnull private final BiConsumer<Collection<Session>, ServerMessageEnvelope> messageBroadcaster;
  @Nonnull private final BiConsumer<Session, ServerMessageEnvelope> messageSender;
  @Nonnull private final SessionSet sessionSet;
  @NonNull private final BannedPlayerEventHandler bannedPlayerEventHandler;

  void addSession(final Session session) {
    sessionSet.put(session);
  }

  void removeSession(final Session session) {
    sessionSet.remove(session);
  }

  public void addPlayerBannedEvent(final InetAddress bannedIP) {
    final ServerMessageEnvelope playerBanned =
        RemoteActionsEnvelopeFactory.newBannedPlayer(bannedIP);
    messageBroadcaster.accept(sessionSet.values(), playerBanned);
    bannedPlayerEventHandler.fireBannedEvent(bannedIP);
  }

  public void addShutdownRequestEvent(final InetAddress serverIp) {
    final ServerMessageEnvelope shutdownMessage = RemoteActionsEnvelopeFactory.newShutdownMessage();
    sessionSet
        .getSessionsByIp(serverIp)
        .forEach(session -> messageSender.accept(session, shutdownMessage));
  }
}
