package org.triplea.http.client.lobby.chat.events.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;

@ExtendWith(MockitoExtension.class)
class ClientMessageFactoryTest {
  private static final ApiKey API_KEY = ApiKey.of("api-key");
  private static final String STATUS = "status";
  private static final String MESSAGE = "message";

  private final ClientMessageFactory clientEventFactory = new ClientMessageFactory(API_KEY);

  @Test
  void connectToChat() {
    final ClientMessageEnvelope result = clientEventFactory.connectToChat();

    assertThat(
        result,
        is(
            ClientMessageEnvelope.builder()
                .apiKey(API_KEY.getValue())
                .messageType(ClientMessageEnvelope.ClientMessageType.CONNECT.name())
                .payload("")
                .build()));
  }

  @Test
  void updatePlayerStatus() {
    final ClientMessageEnvelope result = clientEventFactory.updateMyPlayerStatus(STATUS);

    assertThat(
        result,
        is(
            ClientMessageEnvelope.builder()
                .apiKey(API_KEY.getValue())
                .messageType(ClientMessageEnvelope.ClientMessageType.UPDATE_MY_STATUS.name())
                .payload(STATUS)
                .build()));
  }

  @Test
  void sendMessage() {
    final ClientMessageEnvelope result = clientEventFactory.sendMessage(MESSAGE);

    assertThat(
        result,
        is(
            ClientMessageEnvelope.builder()
                .apiKey(API_KEY.getValue())
                .messageType(ClientMessageEnvelope.ClientMessageType.MESSAGE.name())
                .payload(MESSAGE)
                .build()));
  }
}
