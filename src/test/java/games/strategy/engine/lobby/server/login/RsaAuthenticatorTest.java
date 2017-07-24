package games.strategy.engine.lobby.server.login;

import static games.strategy.engine.lobby.server.login.RsaAuthenticator.hashPasswordWithSalt;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class RsaAuthenticatorTest {

  @Test
  public void testCanProcess() {
    assertTrue(RsaAuthenticator.canProcessChallenge(singletonMap(RsaAuthenticator.RSA_PUBLIC_KEY, "")));
    assertTrue(RsaAuthenticator.canProcessResponse(singletonMap(RsaAuthenticator.ENCRYPTED_PASSWORD_KEY, "")));

    // Adding a completely unrelated key shouldn't change the outcome
    assertFalse(RsaAuthenticator.canProcessResponse(singletonMap(LobbyLoginValidator.HASHED_PASSWORD_KEY, "")));
    assertFalse(RsaAuthenticator.canProcessChallenge(singletonMap(LobbyLoginValidator.SALT_KEY, "")));
  }

  @Test
  public void testExpire() {
    final Map<String, String> challenge = new HashMap<>();
    challenge.putAll(RsaAuthenticator.generatePublicKey());
    RsaAuthenticator.invalidateAll();
    final Map<String, String> response = new HashMap<>();
    response.putAll(RsaAuthenticator.getEncryptedPassword(challenge, "something"));
    final String errorMessage = RsaAuthenticator.decryptPasswordForAction(challenge, response, pass -> {
      fail("The password should never be successfully encrypted");
      return null;
    });
    assertNotNull(errorMessage);
    assertTrue(errorMessage.toLowerCase().contains("timeout"));
  }

  @Test
  public void testKeyExpirationAfterAuthentication() {
    final Map<String, String> challenge = new HashMap<>();
    challenge.putAll(RsaAuthenticator.generatePublicKey());
    final Map<String, String> response = new HashMap<>();
    final String password = "something";
    response.putAll(RsaAuthenticator.getEncryptedPassword(challenge, password));

    assertNull(RsaAuthenticator.decryptPasswordForAction(challenge, response, pass -> {
      assertEquals(hashPasswordWithSalt(password), pass);
      return null;
    }));
    final String errorMessage = RsaAuthenticator.decryptPasswordForAction(challenge, response, pass -> {
      fail("The password should never be successfully encrypted");
      return null;
    });
    assertNotNull(errorMessage);
    assertTrue(errorMessage.toLowerCase().contains("timeout"));
  }
}
