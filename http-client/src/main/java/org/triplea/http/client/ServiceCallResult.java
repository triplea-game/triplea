package org.triplea.http.client;

import java.util.Optional;

import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Class that represents the result of an http service call. The 'payload' is the body portion of
 * the response, typically will be a POJO and the type of this generic class would be the corresponding java type.
 * 
 * @param <T> If response is JSON then the generic type will be the corresponding Java class, otherwise if
 *        the response from server is plain text the generic type would be String.
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceCallResult<T> {
  /**
   * Response payload from server, null if we never sent a message or did not receive a response.
   */
  @Nullable
  private final T payload;

  /**
   * Any exceptions we caught while sending/receiving.
   */
  @Nullable
  private final Throwable thrown;

  /**
   * Convenience method to get the error message frome exception message (if present).
   * 
   * @return An empty string if no exception is present, otherwise the exception message is returned.
   */
  public String getErrorDetails() {
    return Optional.ofNullable(thrown)
        .map(Throwable::getMessage)
        .orElse("");
  }

  /**
   * Any exceptions sending or receiving the service call result will be returned, if no errors then
   * will return empty.
   */
  public Optional<Throwable> getThrown() {
    return Optional.ofNullable(thrown);
  }

  /**
   * Returns the payload object if the service call was successful, otherwise an empty object if there was
   * a problem in the service call send or receive.
   */
  Optional<T> getPayload() {
    return Optional.ofNullable(payload);
  }

}
