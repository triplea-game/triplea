package org.triplea.http.client.moderator.toolbox;

import java.util.Optional;

import javax.annotation.Nullable;

import lombok.Builder;
import lombok.EqualsAndHashCode;

/**
 * JSON data class that is returned as the result of 'registering' a new API.
 * That is the process of using up a single-use key to obtain a new API key.
 * This class contains the new API key or alternatively will contain an error message
 * if something was invalid or did not work.
 */
@Builder
@EqualsAndHashCode
public class RegisterApiKeyResult {
  @Nullable
  private final String newApiKey;
  @Nullable
  private final String errorMessage;

  /**
   * Creates a new result object, successful case, with the given api key.
   * Returned value will have a null error message.
   */
  public static RegisterApiKeyResult newApiKeyResult(final String newApiKey) {
    return RegisterApiKeyResult.builder()
        .newApiKey(newApiKey)
        .build();
  }


  /**
   * Creates a new result object, error case, with the given error message.
   * Returned value will have a null API key.
   */
  public static RegisterApiKeyResult newErrorResult(final String errorMessage) {
    return RegisterApiKeyResult.builder()
        .errorMessage(errorMessage)
        .build();
  }

  public Optional<String> getNewApiKey() {
    return Optional.ofNullable(newApiKey);
  }

  public Optional<String> getErrorMessage() {
    return Optional.ofNullable(errorMessage);
  }
}
