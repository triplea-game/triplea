package org.triplea.http.client.web.socket.messages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServerMessageEnvelopeTest {
  private static final Gson gson = new Gson();

  @ToString
  @EqualsAndHashCode
  @AllArgsConstructor
  private static class NestedData {
    private final String nestedString;
  }

  @ToString
  @EqualsAndHashCode
  @Builder
  private static class SampleData {
    private final boolean booleanValue;
    private final String stringValue;
    private final float floatValue;
    private final double doubleValue;
    private final NestedData nestedData;
  }

  @Test
  @DisplayName("Assert we can serialize a ServerMessgeEnvelope to JSON and then back")
  void toPlayerStatusChange() {
    final SampleData sampleData = givenSampleData();

    final ServerMessageEnvelope serverEventEnvelope =
        ServerMessageEnvelope.packageMessage("message-type", sampleData);

    final SampleData result = toAndFromJson(serverEventEnvelope);

    assertThat(result, is(sampleData));
  }

  private static SampleData givenSampleData() {
    return SampleData.builder()
        .nestedData(new NestedData("some data"))
        .stringValue("string value")
        .floatValue(1.0f)
        .doubleValue(1.0)
        .booleanValue(true)
        .build();
  }

  private static SampleData toAndFromJson(final ServerMessageEnvelope messageEnvelope) {
    final String jsonString = gson.toJson(messageEnvelope);
    return gson.fromJson(jsonString, ServerMessageEnvelope.class).getPayload(SampleData.class);
  }
}
