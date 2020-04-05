package org.triplea.http.client.web.socket.messages.envelopes.chat;

import lombok.EqualsAndHashCode;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

@EqualsAndHashCode
public class PlayerLeftMessage implements WebSocketMessage {
  public static final MessageType<PlayerLeftMessage> TYPE = MessageType.of(PlayerLeftMessage.class);

  private final String userName;

  public PlayerLeftMessage(final UserName userName) {
    this.userName = userName.getValue();
  }

  public UserName getUserName() {
    return UserName.of(userName);
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
