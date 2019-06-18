package org.triplea.http.client.moderator.toolbox;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.register.key.RegisterApiKeyResult;

class RegisterApiKeyResultTest {

  private static final String API_KEY = "Love me dubloon, ye wet girl!";
  private static final String ERROR_MESSAGE = "Avast, yer not leading me without a punishment!";

  @Test
  void newApiKeyResult() {
    final RegisterApiKeyResult result = RegisterApiKeyResult.newApiKeyResult(API_KEY);

    assertThat(result.getNewApiKey(), is(API_KEY));
    assertThat(result.getErrorMessage(), nullValue());
  }

  @Test
  void newErrorResult() {
    final RegisterApiKeyResult result = RegisterApiKeyResult.newErrorResult(ERROR_MESSAGE);

    assertThat(result.getNewApiKey(), nullValue());
    assertThat(result.getErrorMessage(), is(ERROR_MESSAGE));
  }
}
