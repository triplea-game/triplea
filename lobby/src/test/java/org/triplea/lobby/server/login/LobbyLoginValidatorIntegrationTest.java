package org.triplea.lobby.server.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.triplea.lobby.common.login.RsaAuthenticator.hashPasswordWithSalt;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.lobby.server.db.BadWordController;
import org.triplea.lobby.server.db.Database;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.UserController;
import org.triplea.test.common.Integration;

import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.net.ILoginValidator;
import games.strategy.net.MacFinder;
import games.strategy.util.Md5Crypt;
import games.strategy.util.Util;

@Integration
public class LobbyLoginValidatorIntegrationTest {
  private final Database database = new Database(TestLobbyConfigurations.INTEGRATION_TEST);
  private final ILoginValidator loginValidator = new LobbyLoginValidator(TestLobbyConfigurations.INTEGRATION_TEST);

  @Test
  public void testLegacyCreateNewUser() {
    final ChallengeResultFunction challengeFunction = generateChallenge(null);
    final Map<String, String> response = new HashMap<>();
    response.put(LobbyLoginResponseKeys.REGISTER_NEW_USER, Boolean.TRUE.toString());
    response.put(LobbyLoginResponseKeys.HASHED_PASSWORD, md5Crypt("123"));
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
    final Map<String, String> challenge = loginValidator.getChallengeProperties(name);
    return responseGetter -> {
      final Map<String, String> response = responseGetter.apply(challenge);
      response.putIfAbsent(LobbyLoginResponseKeys.EMAIL, email);
      response.putIfAbsent(LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
      return loginValidator.verifyConnection(challenge, response, name, mac, address);
    };
  }

  private void createUser(final String name, final String email, final HashedPassword password) {
    new UserController(database).createUser(
        new DBUser(new DBUser.UserName(name), new DBUser.UserEmail(email)),
        password);
  }

  private static String md5Crypt(final String value) {
    return Md5Crypt.hashPassword(value, Md5Crypt.newSalt());
  }

  @Test
  public void testCreateNewUser() {
    final String name = Util.createUniqueTimeStamp();
    final String password = "password";
    final Map<String, String> response = new HashMap<>();
    response.put(LobbyLoginResponseKeys.REGISTER_NEW_USER, Boolean.TRUE.toString());
    assertNull(generateChallenge(name, null).apply(challenge -> {
      response.putAll(RsaAuthenticator.newResponse(challenge, password));
      return response;
    }));

    // try to create a duplicate user, should not work
    assertNotNull(generateChallenge(name, null).apply(challenge -> {
      response.putAll(RsaAuthenticator.newResponse(challenge, "wrong"));
      return response;
    }));
    assertTrue(BCrypt.checkpw(hashPasswordWithSalt(password), new UserController(database).getPassword(name).value));
  }

  @Test
  public void testWrongVersion() {
    assertNotNull(generateChallenge(null).apply(challenge -> {
      final Map<String, String> response = new HashMap<>();
      response.put(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
      response.put(LobbyLoginResponseKeys.LOBBY_VERSION, "0.1");
      return response;
    }));
  }

  @Test
  public void testAnonymousLogin() {
    final Map<String, String> response = new HashMap<>();
    response.put(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
    assertNull(generateChallenge(null).apply(challenge -> response));

    // create a user, verify we can't login with a username that already exists
    // we should not be able to login now
    assertNotNull(generateChallenge(new HashedPassword(md5Crypt("foo"))).apply(challenge -> response));
  }

  @Test
  public void testAnonymousLoginBadName() {
    final String name = "bitCh" + Util.createUniqueTimeStamp();
    try {
      new BadWordController(database).addBadWord("bitCh");
    } catch (final Exception ignore) {
      // this is probably a duplicate insertion error, we can ignore that as it only means we already added the bad
      // word previously
    }
    assertEquals(LobbyLoginValidator.ErrorMessages.THATS_NOT_A_NICE_NAME,
        generateChallenge(name, new HashedPassword(md5Crypt("foo"))).apply(challenge -> new HashMap<>(
            Collections.singletonMap(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString()))));
  }

  @Test
  public void testLogin() {
    final String user = Util.createUniqueTimeStamp();
    final String password = "foo";
    final Map<String, String> response = new HashMap<>();
    assertNull(
        generateChallenge(user, new HashedPassword(BCrypt.hashpw(hashPasswordWithSalt(password), BCrypt.gensalt())))
            .apply(challenge -> {
              response.putAll(RsaAuthenticator.newResponse(challenge, password));
              return response;
            }));
    // with a bad password
    assertError(generateChallenge(user, null)
        .apply(challenge -> new HashMap<>(RsaAuthenticator.newResponse(challenge, "wrong"))), "password");
    // with a non existent user
    assertError(generateChallenge(null).apply(challenge -> response), "user");
  }

  private static void assertError(final @Nullable String errorMessage, final String... strings) {
    Arrays.stream(strings).forEach(string -> assertThat(errorMessage, containsStringIgnoringCase(string)));
  }

  private interface ChallengeResultFunction
      extends Function<Function<Map<String, String>, Map<String, String>>, String> {
  }
}
