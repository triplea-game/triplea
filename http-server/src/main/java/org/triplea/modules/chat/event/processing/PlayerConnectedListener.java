package org.triplea.modules.chat.event.processing;

import java.util.function.Consumer;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.api.key.PlayerApiKeyDaoWrapper;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatterListingMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ConnectToChatMessage;
import org.triplea.http.client.web.socket.messages.envelopes.chat.PlayerJoinedMessage;
import org.triplea.modules.chat.ChatterSession;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessageContext;

@Builder
public class PlayerConnectedListener
    implements Consumer<WebSocketMessageContext<ConnectToChatMessage>> {

  private final PlayerApiKeyDaoWrapper apiKeyDaoWrapper;
  private final ChatParticipantAdapter chatParticipantAdapter;
  private final Chatters chatters;

  public static PlayerConnectedListener build(final Chatters chatters, final Jdbi jdbi) {
    return PlayerConnectedListener.builder()
        .apiKeyDaoWrapper(PlayerApiKeyDaoWrapper.build(jdbi))
        .chatParticipantAdapter(new ChatParticipantAdapter())
        .chatters(chatters)
        .build();
  }

  @Override
  public void accept(final WebSocketMessageContext<ConnectToChatMessage> context) {
    // Make sure chatter has logged in (has a valid API key)
    // Based on the API key we'll know if the player is a moderator.
    apiKeyDaoWrapper
        .lookupByApiKey(context.getMessage().getApiKey())
        .ifPresent(
            keyLookup -> {
              final ChatterSession chatterSession =
                  chatParticipantAdapter.apply(context.getSenderSession(), keyLookup);
              final boolean alreadyConnected =
                  chatters.isPlayerConnected(chatterSession.getChatParticipant().getUserName());

              // connect the current chatter session if they are connected or not
              chatters.connectPlayer(chatterSession);

              context.sendResponse(new ChatterListingMessage(chatters.getChatters()));

              // if the player was already connected, do not send a seemingly
              // duplicate player joined message. Registered users can have multiple
              // chat sessions and will appear under a single name.
              if (!alreadyConnected) {
                context.broadcastMessage(
                    new PlayerJoinedMessage(chatterSession.getChatParticipant()));
              }
            });
  }
}
