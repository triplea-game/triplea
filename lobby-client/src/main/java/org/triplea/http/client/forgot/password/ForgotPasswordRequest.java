package org.triplea.http.client.forgot.password;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Request data sent to server for a forgot password. We need username and email as username is
 * unique, but email is not unique.
 */
@Builder
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ForgotPasswordRequest {
  private String username;
  private String email;
}
