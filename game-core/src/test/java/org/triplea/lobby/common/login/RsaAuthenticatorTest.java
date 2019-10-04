package org.triplea.lobby.common.login;

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
  final class ChallengeResponseTest {
    @Test
    void shouldBeAbleToDecryptHashedAndSaltedPassword() throws Exception {
      final RsaAuthenticator rsaAuthenticator =
          new RsaAuthenticator(TestSecurityUtils.loadRsaKeyPair());
      final String password = "password";
      @SuppressWarnings("unchecked")
      final Function<String, String> action = mock(Function.class);

      final Map<String, String> challenge = new HashMap<>(rsaAuthenticator.newChallenge());
      final Map<String, String> response = new HashMap<>();
      response.put(
          LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD,
          RsaAuthenticator.encrpytPassword(
              challenge.get(LobbyLoginChallengeKeys.RSA_PUBLIC_KEY), password));
      rsaAuthenticator.decryptPasswordForAction(response, action);

      verify(action).apply(RsaAuthenticator.hashPasswordWithSalt(password));
    }
  }
}
