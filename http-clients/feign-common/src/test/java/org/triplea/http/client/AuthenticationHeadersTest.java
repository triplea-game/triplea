package org.triplea.http.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;

class AuthenticationHeadersTest {
  final ApiKey apiKey = ApiKey.newKey();
  final AuthenticationHeaders authenticationHeaders = new AuthenticationHeaders(apiKey);

  @Test
  void createHeaders() {
    assertTrue(authenticationHeaders.createHeaders() instanceof HashMap);
    assertTrue(authenticationHeaders.createHeaders().containsKey("Authorization"));
    assertTrue(authenticationHeaders.createHeaders().containsKey("System-Id-Header"));
    assertEquals(
        authenticationHeaders.createHeaders().get("Authorization").toString(),
        "Bearer" + " " + apiKey);
  }

  @Test
  void systemIdHeaders() {
    assertTrue(AuthenticationHeaders.systemIdHeaders().containsKey("System-Id-Header"));
  }
}
