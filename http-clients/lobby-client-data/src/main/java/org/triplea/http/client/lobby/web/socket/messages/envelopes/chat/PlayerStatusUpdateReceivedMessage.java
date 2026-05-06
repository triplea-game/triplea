package org.triplea.http.client.lobby.web.socket.messages.envelopes.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Message indicating that "my" status has changed. This message is sent from players to the server
 * to indicate they have updated their status. The server should then broadcast a {@code
 * PlayerStatusChangedMessage} to all players to notify them of the player status update.
 *
 * <p>A player status is a side-bar text shown next to a players name in chat.
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Builder
public class PlayerStatusUpdateReceivedMessage implements WebSocketMessage {
  public static final MessageType<PlayerStatusUpdateReceivedMessage> TYPE =
      MessageType.of(PlayerStatusUpdateReceivedMessage.class);

  private final String userName;
  private final String status;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
