package org.triplea.http.client;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

class ServiceResponseTest {
  private static final String SAMPLE_MESSGE = "message from exception";

  @Test
  void getPayload() {
    assertThat(
        "We do not set a payload on the service call result, then getting it returns an empty optional",
        ServiceResponse.<String>builder()
            .sendResult(SendResult.SERVER_ERROR)
            .build()
            .getPayload()
            .isPresent(),
        is(false));

    assertThat(
        "When we set the payload value, getting the payload returns a non-empty optional",
        ServiceResponse.<String>builder()
            .payload(SAMPLE_MESSGE)
            .sendResult(SendResult.SENT)
            .build()
            .getPayload()
            .orElseThrow(() -> new IllegalStateException("expecting value to be present")),
        is(SAMPLE_MESSGE));
  }

  @Test
  void getErrorDetails() {
    assertThat(
        ServiceResponse.<String>builder()
            .sendResult(SendResult.SERVER_ERROR)
            .build()
            .getExceptionMessage(),
        isEmpty());

    assertThat(
        ServiceResponse.<String>builder()
            .sendResult(SendResult.SERVER_ERROR)
            .thrown(new IllegalStateException(SAMPLE_MESSGE))
            .build()
            .getExceptionMessage(),
        isPresentAndIs(SAMPLE_MESSGE));
  }
}
