package org.triplea.http.client.lobby.chat.events.server;

import javax.annotation.Nonnull;
import lombok.Value;

@Value
public class ChatEvent {
  @Nonnull private final String message;
}
