package org.triplea.http.client.lobby.chat.events.server;

import javax.annotation.Nonnull;
import lombok.Value;
import org.triplea.domain.data.PlayerName;

@Value
public class ChatMessage {
  @Nonnull private final PlayerName from;
  @Nonnull private final String message;
}
