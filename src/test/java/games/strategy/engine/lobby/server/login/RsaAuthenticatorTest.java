package games.strategy.engine.lobby.server.login;

import static java.util.Collections.singletonMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RsaAuthenticatorTest {

  @Mock
  private PrivateKey mockPrivateKey;

  @Test
  public void testCanProcess() {
    assertTrue(RsaAuthenticator.canProcessChallenge(singletonMap(RsaAuthenticator.RSA_PUBLIC_KEY, "")));
    assertTrue(RsaAuthenticator.canProcessResponse(singletonMap(RsaAuthenticator.ENCRYPTED_PASSWORD_KEY, "")));

    // Adding a completely unrelated key shouldn't change the outcome
    assertFalse(RsaAuthenticator.canProcessResponse(singletonMap(LobbyLoginValidator.HASHED_PASSWORD_KEY, "")));
    assertFalse(RsaAuthenticator.canProcessChallenge(singletonMap(LobbyLoginValidator.SALT_KEY, "")));
  }

  @Test
  public void testPublicKeysAreExpungedAfterLookup() {
    final String publicKey = "something";
    final Map<String, String> challenge = new HashMap<>();
    challenge.put(RsaAuthenticator.RSA_PUBLIC_KEY, publicKey);

    RsaAuthenticator.invalidateAll();

    assertTrue("There are no public keys after an invalidateAll, so we expect key lookup to fail",
        !RsaAuthenticator.getPrivateKey(challenge).isPresent());

    RsaAuthenticator.putKey(publicKey, mockPrivateKey);

    assertTrue("We just added a matching public key, we expect to find it",
        RsaAuthenticator.getPrivateKey(challenge).isPresent());

    assertTrue("A second lookup should now fail, the public key should be "
        + "purged after the previous successful lookup.",
        !RsaAuthenticator.getPrivateKey(challenge).isPresent());
  }
}
