package org.triplea.lobby.common.login;

import static java.util.Collections.singletonMap;
import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
      @SuppressWarnings("unchecked")
      final Function<String, String> action = mock(Function.class);

      final Map<String, String> challenge = new HashMap<>(rsaAuthenticator.newChallenge());
      final Map<String, String> response =
          new HashMap<>(RsaAuthenticator.newResponse(challenge, password));
      rsaAuthenticator.decryptPasswordForAction(response, action);

      verify(action).apply(RsaAuthenticator.hashPasswordWithSalt(password));
    }
  }

  @Nested
  final class Sha512 {

    @Test
    void testSha512() {
      assertEquals(
          "317167dd20761e90ab62f85af48985716bd2df129a0c36e5403841a861c01e51d8786c33e3"
              + "4dedeba6cb969aa9a957ba1079b9a48a66ceec668af39b91446ec5",
          RsaAuthenticator.sha512("TripleA"));
      assertEquals(
          "1fdeebdbd3363f2d3f14f10e4cc85bc8115f564ba85a179f19b2d5b3da7ec3f7"
              + "9484cd4e59c6103ff4c8dd1cf37a82da13ed185f2e64725e113b0fb448c87fcb",
          RsaAuthenticator.sha512("triplea"));
      assertEquals(
          "8d708d18b54df3962d696f069ad42dad7762b5d4d3c97ee5fa2dae0673ed4654"
              + "5164c078b8db3d59c4b96020e4316f17bb3d91bf1f6bc0896bbe75416eb8c385",
          RsaAuthenticator.sha512("AAA"));
      assertEquals(
          "6bed2b94f7204211ba1ce66869096a59898688088d482e12c95a9778d2adf2ab"
              + "aee05890f97f73e4274742c69adf51406c0535452c9ec2e2adbf98048526b30c",
          RsaAuthenticator.sha512("WWII"));

      assertThrows(NullPointerException.class, () -> RsaAuthenticator.hashPasswordWithSalt(null));
    }
  }
}
