package org.triplea.http.client.web.socket.messages.envelopes.chat;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@EqualsAndHashCode
public class PlayerJoinedMessage implements WebSocketMessage {

  public static final MessageType<PlayerJoinedMessage> TYPE =
      MessageType.of(PlayerJoinedMessage.class);

  @NonNull private final String userName;
  private final String playerChatId;
  private final boolean isModerator;

  public PlayerJoinedMessage(final ChatParticipant chatParticipant) {
    this.userName = chatParticipant.getUserName().getValue();
    this.playerChatId = chatParticipant.getPlayerChatId().getValue();
    this.isModerator = chatParticipant.isModerator();
  }

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
