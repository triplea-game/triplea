package org.triplea.http.client.lobby.web.socket.messages.envelopes.chat;

import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Builder
public class PlayerListingMessage implements WebSocketMessage {
  public static final MessageType<PlayerListingMessage> TYPE =
      MessageType.of(PlayerListingMessage.class);

  @Nonnull private final Set<ChatParticipant> chatters;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
