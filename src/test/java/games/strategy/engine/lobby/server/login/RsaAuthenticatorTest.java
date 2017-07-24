package games.strategy.engine.lobby.server.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import games.strategy.util.Util;

public class RsaAuthenticatorTest {

  @Test
  public void testCanProcess() {
    final Map<String, String> map = new HashMap<>();
    map.put(RsaAuthenticator.RSA_PUBLIC_KEY, "");
    assertTrue(RsaAuthenticator.canProcessChallenge(map));
    map.clear();
    map.put(RsaAuthenticator.ENCRYPTED_PASSWORD_KEY, "");
    assertTrue(RsaAuthenticator.canProcessResponse(map));
    map.clear();

    // Adding a completely unrelated key shouldn't change the outcome
    map.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, "");
    assertFalse(RsaAuthenticator.canProcessResponse(map));
    map.clear();
    map.put(LobbyLoginValidator.SALT_KEY, "");
    assertFalse(RsaAuthenticator.canProcessChallenge(map));
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
  public void testKeyStorage() {
    final Map<String, String> challenge = new HashMap<>();
    challenge.putAll(RsaAuthenticator.generatePublicKey());
    final Map<String, String> response = new HashMap<>();
    final String password = "something";
    response.putAll(RsaAuthenticator.getEncryptedPassword(challenge, password));

    assertNull(RsaAuthenticator.decryptPasswordForAction(challenge, response, pass -> {
      assertEquals(Util.sha512(password), pass);
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
