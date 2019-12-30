package org.triplea.server.remote.actions;

import lombok.experimental.UtilityClass;
import org.triplea.server.access.BannedPlayerEventHandler;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.http.web.socket.MessageSender;
import org.triplea.server.http.web.socket.SessionSet;

@UtilityClass
public class RemoteActionsEventQueueFactory {

  public RemoteActionsEventQueue newRemoteActionsEventQueue(
      final SessionSet sessionSet, final BannedPlayerEventHandler bannedPlayerEventHandler) {
    final var messageSender = new MessageSender();

    return RemoteActionsEventQueue.builder()
        .bannedPlayerEventHandler(bannedPlayerEventHandler)
        .sessionSet(sessionSet)
        .messageSender(messageSender)
        .messageBroadcaster(new MessageBroadcaster(messageSender))
        .build();
  }
}
