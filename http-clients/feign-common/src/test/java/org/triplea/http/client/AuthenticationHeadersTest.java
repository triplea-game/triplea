package org.triplea.http.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

import org.hamcrest.collection.IsMapWithSize;
import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;

class AuthenticationHeadersTest {
  final ApiKey apiKey = ApiKey.newKey();
  final AuthenticationHeaders authenticationHeaders = new AuthenticationHeaders(apiKey);

  @Test
  void createHeaders() {
    assertThat(authenticationHeaders.createHeaders(), IsMapWithSize.aMapWithSize(2));
    assertThat(authenticationHeaders.createHeaders(), hasKey("Authorization"));
    assertThat(authenticationHeaders.createHeaders(), hasKey("System-Id-Header"));
    assertThat(
        authenticationHeaders.createHeaders().get("Authorization").toString(),
        equalTo("Bearer" + " " + apiKey));
  }

  @Test
  void systemIdHeaders() {
    assertThat(AuthenticationHeaders.systemIdHeaders(), hasKey("System-Id-Header"));
  }
}
