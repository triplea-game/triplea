package org.triplea.server.forgot.password;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.forgot.password.ForgotPasswordClient;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.http.client.forgot.password.ForgotPasswordResponse;
import org.triplea.server.http.AbstractDropwizardTest;

class ForgotPasswordControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ForgotPasswordClient client =
      AbstractDropwizardTest.newClient(ForgotPasswordClient::newClient);

  @Test
  void uploadErrorReport() {
    final ForgotPasswordResponse response =
        client.sendForgotPasswordRequest(
            ForgotPasswordRequest.builder().username("user").email("email").build());

    assertThat(response.getResponseMessage(), notNullValue());
  }
}
