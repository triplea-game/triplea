package org.triplea.http.client.lobby.chat.events.client;

import lombok.AllArgsConstructor;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.client.ClientEventEnvelope.ClientMessageType;

/**
 * Class to handle details of creating a {@code ClientEventEnvelope}, using this class such objects
 * can be created by using the right factory method and providing the contents of the message
 * envelope. This class will take care of the details for creating the message envelope around a
 * given message.
 */
@AllArgsConstructor
public class ClientEventFactory {
  private final ApiKey apiKey;

  public ClientEventEnvelope slapMessage(final PlayerName playerToSlap) {
    return packageMessage(ClientMessageType.SLAP, playerToSlap.getValue());
  }

  private ClientEventEnvelope packageMessage(
      final ClientMessageType messageType, final String payload) {
    return ClientEventEnvelope.builder()
        .apiKey(apiKey.getValue())
        .messageType(messageType.name())
        .payload(payload)
        .build();
  }

  public ClientEventEnvelope connectToChat() {
    return packageMessage(ClientMessageType.CONNECT, "");
  }

  public ClientEventEnvelope updateMyPlayerStatus(final String status) {
    return packageMessage(ClientMessageType.UPDATE_MY_STATUS, status);
  }

  public ClientEventEnvelope sendMessage(final String message) {
    return packageMessage(ClientMessageType.MESSAGE, message);
  }
}
