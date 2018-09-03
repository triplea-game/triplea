package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

class ServiceCallResultTest {
  private static final String SAMPLE_MESSGE = "message from exception";

  @Test
  void getPayload() {
    assertThat(
        "We do not set a payload on the service call result, then getting it returns an empty optional",
        ServiceCallResult.<String>builder()
            .build()
            .getPayload()
            .isPresent(),
        is(false));

    assertThat(
        "When we set the payload value, getting the payload returns a non-empty optional",
        ServiceCallResult.<String>builder()
            .payload(SAMPLE_MESSGE)
            .build()
            .getPayload()
            .orElseThrow(() -> new IllegalStateException("expecting value to be present")),
        is(SAMPLE_MESSGE));
  }

  @Test
  void getErrorDetails() {
    assertThat(
        ServiceCallResult.<String>builder()
            .build()
            .getErrorDetails(),
        is(""));

    assertThat(
        ServiceCallResult.<String>builder()
            .thrown(new IllegalStateException(SAMPLE_MESSGE))
            .build()
            .getErrorDetails(),
        is(SAMPLE_MESSGE));
  }
}
