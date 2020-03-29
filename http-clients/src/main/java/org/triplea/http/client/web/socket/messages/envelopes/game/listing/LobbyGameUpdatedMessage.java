package org.triplea.http.client.web.socket.messages.envelopes.game.listing;

import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * An upsert message indicating a new lobby game or a lobby game has changed state. State changes
 * include things like player count has changed, game status changed (eg: waiting, started), map
 * changed, etc.. Any such change should be updated in the players view of the lobby games.
 */
@Getter
@AllArgsConstructor
public class LobbyGameUpdatedMessage implements WebSocketMessage {
  public static final MessageType<LobbyGameUpdatedMessage> TYPE =
      MessageType.of(LobbyGameUpdatedMessage.class);

  @Nonnull private final LobbyGameListing lobbyGameListing;

  @Override
  public MessageEnvelope toEnvelope() {
    return MessageEnvelope.packageMessage(TYPE, this);
  }
}
