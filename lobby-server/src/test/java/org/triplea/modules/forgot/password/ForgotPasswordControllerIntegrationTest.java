package org.triplea.modules.forgot.password;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.SystemIdHeader;
import org.triplea.http.client.forgot.password.ForgotPasswordClient;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;
import org.triplea.modules.http.BasicEndpointTest;

class ForgotPasswordControllerIntegrationTest extends BasicEndpointTest<ForgotPasswordClient> {

  ForgotPasswordControllerIntegrationTest() {
    super(ForgotPasswordClient::newClient);
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
