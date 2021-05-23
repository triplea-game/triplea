package org.triplea.spitfire.server.controllers;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.forgot.password.ForgotPasswordClient;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.spitfire.server.BasicEndpointTest;

@SuppressWarnings("UnmatchedTest")
class ForgotPasswordControllerIntegrationTest extends BasicEndpointTest<ForgotPasswordClient> {

  ForgotPasswordControllerIntegrationTest(final URI localhost) {
    super(localhost, ForgotPasswordClient::newClient);
  }

  @Test
  void forgotPassword() {
    verifyEndpointReturningObject(
        client ->
            client.sendForgotPasswordRequest(
                SystemIdHeader.headers(),
                ForgotPasswordRequest.builder().username("user").email("email").build()));
  }
}
