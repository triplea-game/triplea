package org.triplea.spitfire.server.controllers;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.forgot.password.ForgotPasswordClient;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class ForgotPasswordControllerIntegrationTest extends ControllerIntegrationTest {
  private final ForgotPasswordClient client;

  ForgotPasswordControllerIntegrationTest(final URI localhost) {
    this.client = ForgotPasswordClient.newClient(localhost);
  }

  @Test
  void badArgs() {
    assertBadRequest(
        () -> client.sendForgotPasswordRequest(ForgotPasswordRequest.builder().build()));
  }

  @Test
  void forgotPassword() {
    client.sendForgotPasswordRequest(
        ForgotPasswordRequest.builder().username("user").email("email").build());
  }
}
