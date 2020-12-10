package org.triplea.http.client.forgot.password;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Data object that corresponds to the JSON response from lobby-server for forgot password request.
 */
@ToString
@Builder
@Getter
@EqualsAndHashCode
public class ForgotPasswordResponse {
  /**
   * Contains response from server, whether success (email sent), or not: bad email, bad username,
   * or too many requests sent.
   */
  @Nonnull private final String responseMessage;
}
