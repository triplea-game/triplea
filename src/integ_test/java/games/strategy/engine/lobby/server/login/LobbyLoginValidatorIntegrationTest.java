package games.strategy.engine.lobby.server.login;

import static games.strategy.engine.lobby.server.login.RsaAuthenticator.hashPasswordWithSalt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import org.mindrot.jbcrypt.BCrypt;

import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.db.BadWordController;
import games.strategy.engine.lobby.server.db.HashedPassword;
import games.strategy.engine.lobby.server.db.UserController;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.net.ILoginValidator;
import games.strategy.net.MacFinder;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

public class LobbyLoginValidatorIntegrationTest {
  private final ILoginValidator loginValidator = new LobbyLoginValidator();

  @Test
  public void testLegacyCreateNewUser() {
    final ChallengeResultFunction challengeFunction = generateChallenge(null);
    final Map<String, String> response = new HashMap<>();
    response.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
    response.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt("123"));
    assertNull(challengeFunction.apply(challenge -> response));
    // try to create a duplicate user, should not work
    assertNotNull(challengeFunction.apply(challenge -> response));
  }

  private ChallengeResultFunction generateChallenge(final HashedPassword password) {
    return generateChallenge(Util.createUniqueTimeStamp(), password);
  }

  private ChallengeResultFunction generateChallenge(final String name, final HashedPassword password) {
    final SocketAddress address = new InetSocketAddress(5000);
    final String mac = MacFinder.getHashedMacAddress();
    final String email = "none@none.none";
    if (password != null) {
      createUser(name, email, password);
    }
    final Map<String, String> challenge = loginValidator.getChallengeProperties(name, address);
    return responseGetter -> {
      final Map<String, String> response = responseGetter.apply(challenge);
      response.putIfAbsent(LobbyLoginValidator.EMAIL_KEY, email);
      response.putIfAbsent(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
      return loginValidator.verifyConnection(challenge, response, name, mac, address);
    };
  }

  private static void createUser(final String name, final String email, final HashedPassword password) {
    new UserController().createUser(new DBUser(new DBUser.UserName(name), new DBUser.UserEmail(email)), password);
  }

  @Test
  public void testCreateNewUser() {
    final String name = Util.createUniqueTimeStamp();
    final String password = "password";
    final Map<String, String> response = new HashMap<>();
    response.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
    assertNull(generateChallenge(name, null).apply(challenge -> {
      response.putAll(RsaAuthenticator.getEncryptedPassword(challenge, password));
      return response;
    }));

    // try to create a duplicate user, should not work
    assertNotNull(generateChallenge(name, null).apply(challenge -> {
      response.putAll(RsaAuthenticator.getEncryptedPassword(challenge, "wrong"));
      return response;
    }));
    assertTrue(BCrypt.checkpw(hashPasswordWithSalt(password), new UserController().getPassword(name).value));
  }

  @Test
  public void testWrongVersion() {
    assertNotNull(generateChallenge(null).apply(challenge -> {
      final Map<String, String> response = new HashMap<>();
      response.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
      response.put(LobbyLoginValidator.LOBBY_VERSION, "0.1");
      return response;
    }));
  }

  @Test
  public void testAnonymousLogin() {
    final Map<String, String> response = new HashMap<>();
    response.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
    assertNull(generateChallenge(null).apply(challenge -> response));

    // create a user, verify we can't login with a username that already exists
    // we should not be able to login now
    assertNotNull(generateChallenge(new HashedPassword(MD5Crypt.crypt("foo"))).apply(challenge -> response));
  }

  @Test
  public void testAnonymousLoginBadName() {
    final String name = "bitCh" + Util.createUniqueTimeStamp();
    try {
      new BadWordController().addBadWord("bitCh");
    } catch (final Exception ignore) {
      // this is probably a duplicate insertion error, we can ignore that as it only means we already added the bad
      // word previously
    }
    assertEquals(LobbyLoginValidator.THATS_NOT_A_NICE_NAME,
        generateChallenge(name, new HashedPassword(MD5Crypt.crypt("foo"))).apply(challenge -> new HashMap<>(
            Collections.singletonMap(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString()))));
  }

  @Test
  public void testLogin() {
    final String user = Util.createUniqueTimeStamp();
    final String password = "foo";
    final Map<String, String> response = new HashMap<>();
    assertNull(
        generateChallenge(user, new HashedPassword(BCrypt.hashpw(hashPasswordWithSalt(password), BCrypt.gensalt())))
            .apply(challenge -> {
              response.putAll(RsaAuthenticator.getEncryptedPassword(challenge, password));
              return response;
            }));
    // with a bad password
    assertError(generateChallenge(user, null)
        .apply(challenge -> new HashMap<>(RsaAuthenticator.getEncryptedPassword(challenge, "wrong"))), "password");
    // with a non existent user
    assertError(generateChallenge(null).apply(challenge -> response), "user");
  }

  private static void assertError(final String errorMessage, final String... strings) {
    assertNotNull(errorMessage);
    final String simpleError = errorMessage.trim().toLowerCase();
    try {
      assertTrue(Arrays.stream(strings).map(String::toLowerCase).allMatch(simpleError::contains));
    } catch (final AssertionError e) {
      throw new AssertionError(String.format("Error message '%s' did not contain all of those keywords: %s",
          errorMessage, Arrays.toString(strings)), e);
    }
  }

  private interface ChallengeResultFunction
      extends Function<Function<Map<String, String>, Map<String, String>>, String> {
  }
}
