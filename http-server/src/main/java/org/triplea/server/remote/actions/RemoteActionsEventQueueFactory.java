package org.triplea.server.remote.actions;

import lombok.experimental.UtilityClass;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.http.web.socket.MessageSender;
import org.triplea.server.lobby.chat.InetExtractor;

@UtilityClass
public class RemoteActionsEventQueueFactory {

  public RemoteActionsEventQueue newRemoteActionsEventQueue() {
    final var messageSender = new MessageSender();

    return RemoteActionsEventQueue.builder()
        .sessionTracker(
            SessionTracker.builder()
                .ipAddressExtractor(session -> InetExtractor.extract(session.getUserProperties()))
                .build())
        .messageSender(messageSender)
        .messageBroadcaster(new MessageBroadcaster(messageSender))
        .build();
  }
}
