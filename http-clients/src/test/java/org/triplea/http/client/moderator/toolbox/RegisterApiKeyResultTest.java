package org.triplea.http.client.moderator.toolbox;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class RegisterApiKeyResultTest {

  private static final String API_KEY = "Love me dubloon, ye wet girl!";
  private static final String ERROR_MESSAGE = "Avast, yer not leading me without a punishment!";

  @Test
  void newApiKeyResult() {
    final RegisterApiKeyResult result = RegisterApiKeyResult.newApiKeyResult(API_KEY);

    assertThat(result.getNewApiKey(), isPresentAndIs(API_KEY));
    assertThat(result.getErrorMessage(), isEmpty());
  }

  @Test
  void newErrorResult() {
    final RegisterApiKeyResult result = RegisterApiKeyResult.newErrorResult(ERROR_MESSAGE);

    assertThat(result.getNewApiKey(), isEmpty());
    assertThat(result.getErrorMessage(), isPresentAndIs(ERROR_MESSAGE));
  }
}
