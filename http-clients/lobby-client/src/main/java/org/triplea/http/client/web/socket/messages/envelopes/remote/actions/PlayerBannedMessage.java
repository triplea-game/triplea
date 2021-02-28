package org.triplea.http.client.web.socket.messages.envelopes.remote.actions;

import java.net.InetAddress;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;
import org.triplea.java.IpAddressParser;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class PlayerBannedMessage implements WebSocketMessage {
  public static final MessageType<PlayerBannedMessage> TYPE =
      MessageType.of(PlayerBannedMessage.class);

  private final String ipAddress;

  public InetAddress getIpAddress() {
    return IpAddressParser.fromString(ipAddress);
  }

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
