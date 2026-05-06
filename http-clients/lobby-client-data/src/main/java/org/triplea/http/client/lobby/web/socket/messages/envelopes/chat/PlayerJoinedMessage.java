package org.triplea.http.client.lobby.web.socket.messages.envelopes.chat;

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
public class PlayerJoinedMessage implements WebSocketMessage {

  public static final MessageType<PlayerJoinedMessage> TYPE =
      MessageType.of(PlayerJoinedMessage.class);

  @Nonnull private final String userName;
  private final String playerChatId;
  private final boolean isModerator;

  public ChatParticipant getChatParticipant() {
    return ChatParticipant.builder()
        .userName(userName)
        .playerChatId(playerChatId)
        .isModerator(isModerator)
        .build();
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
