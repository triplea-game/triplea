package org.triplea.modules.moderation.remote.actions;

import lombok.experimental.UtilityClass;
import org.triplea.modules.moderation.ban.user.BannedPlayerEventHandler;
import org.triplea.web.socket.MessageBroadcaster;
import org.triplea.web.socket.MessageSender;
import org.triplea.web.socket.SessionSet;

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
