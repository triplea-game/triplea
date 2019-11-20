package org.triplea.http.client.lobby.chat.events.server;

import com.google.common.base.Ascii;
import com.google.common.base.Preconditions;
import lombok.Value;
import org.triplea.domain.data.PlayerName;

@Value
public class ChatMessage {
  private final PlayerName from;
  private final String message;

  public ChatMessage(final PlayerName from, final String message) {
    this.from = Preconditions.checkNotNull(from);
    this.message = Ascii.truncate(Preconditions.checkNotNull(message), 200, "..");
  }
}
