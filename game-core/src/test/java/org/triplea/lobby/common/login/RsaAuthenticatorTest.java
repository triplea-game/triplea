package org.triplea.lobby.common.login;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.test.common.security.TestSecurityUtils;

final class RsaAuthenticatorTest {
  @Nested
  final class CanProcessResponseTest {
    @Test
    void shouldReturnTrueWhenResponseContainsAllRequiredProperties() {
      assertTrue(
          RsaAuthenticator.canProcessResponse(
              singletonMap(LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD, "")));
    }

    @Test
    void shouldReturnFalseWhenResponseDoesNotContainAllRequiredProperties() {
      assertFalse(RsaAuthenticator.canProcessResponse(singletonMap("someOtherResponseKey", "")));
    }
  }

  @Nested
  final class ChallengeResponseTest {
    @Test
    void shouldBeAbleToDecryptHashedAndSaltedPassword() throws Exception {
      final RsaAuthenticator rsaAuthenticator =
          new RsaAuthenticator(TestSecurityUtils.loadRsaKeyPair());
      final String password = "password";
      final Map<String, String> challenge = new HashMap<>();
      final Map<String, String> response = new HashMap<>();
      @SuppressWarnings("unchecked")
      final Function<String, String> action = mock(Function.class);

      challenge.putAll(rsaAuthenticator.newChallenge());
      response.putAll(RsaAuthenticator.newResponse(challenge, password));
      rsaAuthenticator.decryptPasswordForAction(response, action);

      verify(action).apply(RsaAuthenticator.hashPasswordWithSalt(password));
    }
  }
}
