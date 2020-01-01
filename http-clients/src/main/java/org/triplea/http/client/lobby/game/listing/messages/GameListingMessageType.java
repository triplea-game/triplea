package org.triplea.http.client.lobby.game.listing.messages;

import static org.triplea.http.client.web.socket.messages.MessageTypeListenerBinding.newBinding;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.web.socket.messages.MessageTypeListenerBinding;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

@AllArgsConstructor
@Getter(onMethod_ = @Override)
@SuppressWarnings("ImmutableEnumChecker")
public enum GameListingMessageType implements WebsocketMessageType<GameListingListeners> {
  GAME_UPDATED(newBinding(LobbyGameListing.class, GameListingListeners::getGameUpdated)),
  GAME_REMOVED(newBinding(String.class, GameListingListeners::getGameRemoved)),
  ;

  private final MessageTypeListenerBinding<GameListingListeners, ?> messageTypeListenerBinding;
}
