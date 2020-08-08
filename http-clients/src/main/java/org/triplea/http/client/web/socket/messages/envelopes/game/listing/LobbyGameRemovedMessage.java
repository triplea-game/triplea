package org.triplea.http.client.web.socket.messages.envelopes.game.listing;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Indicates a lobby game has been closed (for any reason) and is no longer available to players to
 * join.
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class LobbyGameRemovedMessage implements WebSocketMessage {
  public static final MessageType<LobbyGameRemovedMessage> TYPE =
      MessageType.of(LobbyGameRemovedMessage.class);

  @Nonnull private final String gameId;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
