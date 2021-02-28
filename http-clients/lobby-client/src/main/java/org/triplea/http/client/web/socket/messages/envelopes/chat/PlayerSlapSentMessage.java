package org.triplea.http.client.web.socket.messages.envelopes.chat;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@Getter
@EqualsAndHashCode
public class PlayerSlapSentMessage implements WebSocketMessage {
  public static final MessageType<PlayerSlapSentMessage> TYPE =
      MessageType.of(PlayerSlapSentMessage.class);

  private final String slappedPlayer;

  public PlayerSlapSentMessage(final UserName userName) {
    this.slappedPlayer = userName.getValue();
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
