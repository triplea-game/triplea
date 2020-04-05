package org.triplea.http.client.web.socket.messages.envelopes.chat;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@Builder
@EqualsAndHashCode
public class PlayerSlapReceivedMessage implements WebSocketMessage {
  public static final MessageType<PlayerSlapReceivedMessage> TYPE =
      MessageType.of(PlayerSlapReceivedMessage.class);

  @Nonnull private final String slappedPlayer;
  @Nonnull private final String slappingPlayer;

  public UserName getSlappedPlayer() {
    return UserName.of(slappedPlayer);
  }

  public UserName getSlappingPlayer() {
    return UserName.of(slappingPlayer);
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
