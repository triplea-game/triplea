package org.triplea.http.client.lobby.web.socket.messages.envelopes.chat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * A message sent from a player to lobby requesting that they connect to lobby chat. In response,
 * server should respond back with a chatter list to this player and broadcast to all players a
 * player joined message.
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class ConnectToChatMessage implements WebSocketMessage {
  public static final MessageType<ConnectToChatMessage> TYPE =
      MessageType.of(ConnectToChatMessage.class);

  private final String apiKey;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
