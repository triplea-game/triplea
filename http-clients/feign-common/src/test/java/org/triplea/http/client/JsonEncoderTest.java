package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import feign.RequestTemplate;
import feign.gson.GsonEncoder;
import java.nio.charset.StandardCharsets;
import lombok.Getter;
import org.junit.jupiter.api.Test;

class JsonEncoderTest {

  private static final JsonEncoder jsonEncoder = new JsonEncoder();

  private final RequestTemplate template = new RequestTemplate();

  /** We verify here strings are encoded without any modification. */
  @Test
  void encodeString() {
    final String value = "value";

    jsonEncoder.encode(value, String.class, template);

    assertThat(new String(template.body(), StandardCharsets.UTF_8), is(value));
  }

  /**
   * Verify that a simple example object is converted to a JSON string. We rely on GSON to create a
   * JSON form of the sample object and verify our custom encoder produces the same result.
   */
  @Test
  void encodeObject() {
    final SampleObject sampleObject = new SampleObject();

    jsonEncoder.encode(sampleObject, SampleObject.class, template);

    assertThat(
        new String(template.body(), StandardCharsets.UTF_8),
        is(new String(encodeWithGson(sampleObject).body(), StandardCharsets.UTF_8)));
  }

  private static RequestTemplate encodeWithGson(final SampleObject sampleObject) {
    final GsonEncoder gsonEncoder = new GsonEncoder();
    final RequestTemplate templateForGsonEncoding = new RequestTemplate();
    gsonEncoder.encode(sampleObject, SampleObject.class, templateForGsonEncoding);
    return templateForGsonEncoding;
  }

  @Getter
  private static final class SampleObject {
    final String key = "key-value";
    final int intValue = 1;
  }
}
