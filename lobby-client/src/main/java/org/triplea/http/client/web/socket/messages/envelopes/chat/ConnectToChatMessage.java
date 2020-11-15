package org.triplea.http.client.web.socket.messages.envelopes.chat;

import lombok.EqualsAndHashCode;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * A message sent from a player to lobby requesting that they connect to lobby chat. In response,
 * server should respond back with a chatter list to this player and broadcast to all players a
 * player joined message.
 */
@EqualsAndHashCode
public class ConnectToChatMessage implements WebSocketMessage {
  public static final MessageType<ConnectToChatMessage> TYPE =
      MessageType.of(ConnectToChatMessage.class);

  private final String apiKey;

  public ConnectToChatMessage(final ApiKey apiKey) {
    this.apiKey = apiKey.getValue();
  }

  public ApiKey getApiKey() {
    return ApiKey.of(apiKey);
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
