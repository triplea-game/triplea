package org.triplea.http.client;

import org.junit.jupiter.api.Test;
import org.triplea.domain.data.ApiKey;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationHeadersTest {
    final ApiKey apiKey = ApiKey.newKey();
    final AuthenticationHeaders authenticationHeaders = new AuthenticationHeaders(apiKey);

    @Test
    void createHeaders() {
        assertTrue(authenticationHeaders.createHeaders() instanceof HashMap);
        assertTrue(authenticationHeaders.createHeaders().containsKey("Authorization"));
        assertTrue(authenticationHeaders.createHeaders().containsKey("System-Id-Header"));
        assertEquals(authenticationHeaders.createHeaders()
                .get("Authorization").toString(), "Bearer" + " " + apiKey);
    }

    @Test
    void systemIdHeaders() {
        assertTrue(AuthenticationHeaders.systemIdHeaders().containsKey("System-Id-Header"));
    }
}