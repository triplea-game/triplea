package org.triplea.http.client.lobby.chat.messages.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.web.socket.messages.ClientMessageEnvelope;

@ExtendWith(MockitoExtension.class)
class ChatClientEnvelopeFactoryTest {
  private static final ApiKey API_KEY = ApiKey.of("api-key");
  private static final String STATUS = "status";
  private static final String MESSAGE = "message";

  private final ChatClientEnvelopeFactory clientEventFactory =
      new ChatClientEnvelopeFactory(API_KEY);

  @Test
  void connectToChat() {
    final ClientMessageEnvelope result = clientEventFactory.connectToChat();

    assertThat(
        result,
        is(
            ClientMessageEnvelope.builder()
                .apiKey(API_KEY.getValue())
                .messageType(ChatClientEnvelopeType.CONNECT.name())
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
                .messageType(ChatClientEnvelopeType.UPDATE_MY_STATUS.name())
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
                .messageType(ChatClientEnvelopeType.MESSAGE.name())
                .payload(MESSAGE)
                .build()));
  }
}
