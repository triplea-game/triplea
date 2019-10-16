package org.triplea.http.client.lobby.chat.events.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.http.client.lobby.chat.events.client.ClientEventEnvelope.ClientMessageType;

class ClientEventEnvelopeTest {

  @SuppressWarnings({"PMD.UnusedPrivateMethod", "UnusedMethod"})
  private static ClientMessageType[] knownClientMessageTypes() {
    return Arrays.stream(ClientMessageType.values())
        .filter(t -> t != ClientMessageType.UNKNOWN)
        .toArray(ClientMessageType[]::new);
  }

  @ParameterizedTest
  @MethodSource("knownClientMessageTypes")
  void getMessageTypeValidCases(final ClientMessageType messageType) {
    final ClientEventEnvelope clientEventEnvelope =
        ClientEventEnvelope.builder()
            .apiKey("api-key")
            .messageType(messageType.toString())
            .payload("")
            .build();

    final ClientMessageType result = clientEventEnvelope.getMessageType();

    assertThat(result, is(messageType));
  }

  @Test
  void getMessageTypeUnknownCase() {
    final ClientEventEnvelope clientEventEnvelope =
        ClientEventEnvelope.builder().apiKey("api-key").messageType("").payload("").build();

    final ClientMessageType result = clientEventEnvelope.getMessageType();

    assertThat(result, is(ClientMessageType.UNKNOWN));
  }
}
