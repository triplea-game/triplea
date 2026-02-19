package org.triplea.http.client.forgot.password;

import feign.FeignException;
import feign.RequestLine;
import java.net.URI;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client to send a password forgot request, triggers a temp password to be emailed to the
 * user.
 */
@SuppressWarnings("InterfaceNeverImplemented")
public interface ForgotPasswordClient {

  String FORGOT_PASSWORD_PATH = "/lobby/forgot-password";

  /**
   * Sends request for a temporary password to be emailed to user.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("POST " + FORGOT_PASSWORD_PATH)
  ForgotPasswordResponse sendForgotPasswordRequest(ForgotPasswordRequest request);

  /** Creates an error report uploader clients, sends error reports and gets a response back. */
  static ForgotPasswordClient newClient(final URI uri) {
    return HttpClient.newClient(
        ForgotPasswordClient.class, uri, AuthenticationHeaders.systemIdHeaders());
  }
}
