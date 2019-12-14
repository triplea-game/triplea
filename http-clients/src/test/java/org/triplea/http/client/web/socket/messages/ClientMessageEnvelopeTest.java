package org.triplea.http.client.web.socket.messages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.gson.Gson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClientMessageEnvelopeTest {
  private static final ClientMessageEnvelope CLIENT_ENVELOPE =
      ClientMessageEnvelope.builder()
          .apiKey("api-key")
          .messageType("message-type")
          .payload("")
          .build();

  @Test
  @DisplayName("Verify a json string can be converted back to a ClientMessageEnvelope")
  void toAndFromJson() {
    final String jsonString = new Gson().toJson(CLIENT_ENVELOPE);

    final ClientMessageEnvelope result = ClientMessageEnvelope.fromJson(jsonString);

    assertThat(result, is(CLIENT_ENVELOPE));
  }
}
