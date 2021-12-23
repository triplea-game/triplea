package org.triplea.http.client.web.socket.messages.envelopes.chat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.triplea.domain.data.ChatParticipant;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@EqualsAndHashCode
public class ChatterListingMessage implements WebSocketMessage {
  public static final MessageType<ChatterListingMessage> TYPE =
      MessageType.of(ChatterListingMessage.class);

  private final List<Chatter> chatters;

  @Builder
  private static class Chatter {
    private final String userName;
    private final String playerChatId;
    private final boolean isModerator;
    private final String status;
  }

  public ChatterListingMessage(final Collection<ChatParticipant> chatParticipants) {
    chatters =
        chatParticipants.stream() //
            .map(this::mapToChatter)
            .collect(Collectors.toList());
  }

  private Chatter mapToChatter(final ChatParticipant chatParticipant) {
    return Chatter.builder()
        .isModerator(chatParticipant.isModerator())
        .playerChatId(chatParticipant.getPlayerChatId().getValue())
        .status(chatParticipant.getStatus())
        .userName(chatParticipant.getUserName().getValue())
        .build();
  }

  public List<ChatParticipant> getChatters() {
    return chatters.stream()
        .map(
            chatter ->
                ChatParticipant.builder()
                    .userName(chatter.userName)
                    .playerChatId(chatter.playerChatId)
                    .isModerator(chatter.isModerator)
                    .status(chatter.status)
                    .build())
        .collect(Collectors.toList());
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
