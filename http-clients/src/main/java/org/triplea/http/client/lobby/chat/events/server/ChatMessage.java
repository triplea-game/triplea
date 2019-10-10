package org.triplea.http.client.lobby.chat.events.server;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.domain.data.PlayerName;

@AllArgsConstructor
@Getter
public class ChatMessage {
  @Nonnull private final PlayerName from;
  @Nonnull private final String message;
}
