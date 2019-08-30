package org.triplea.lobby.server.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.triplea.lobby.common.login.RsaAuthenticator.hashPasswordWithSalt;

import games.strategy.net.ILoginValidator;
import games.strategy.net.MacFinder;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginChallengeKeys;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.TestUserUtils;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.dao.TempPasswordDao;
import org.triplea.lobby.server.login.forgot.password.verify.TempPasswordVerification;
import org.triplea.test.common.Integration;

@Integration
class LobbyLoginValidatorIntegrationTest {
  private static final String EMAIL = "Chremisa@mori.com";
  private final ILoginValidator loginValidator =
      new LobbyLoginValidator(
          TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao(),
          new RsaAuthenticator(),
          BCrypt::gensalt,
          new FailedLoginThrottle(),
          new TempPasswordVerification(
              JdbiDatabase.newConnection().onDemand(TempPasswordDao.class)));

  private ChallengeResultFunction generateChallenge(final HashedPassword password) {
    return generateChallenge("a" + TestUserUtils.newUniqueTimestamp(), password);
  }

  private ChallengeResultFunction generateChallenge(
      final String name, final HashedPassword password) {
    final SocketAddress address = new InetSocketAddress(5000);
    final String mac = MacFinder.getHashedMacAddress();
    if (password != null) {
      createUser(name, password);
    }
    final Map<String, String> challenge = loginValidator.getChallengeProperties(name);
    return responseGetter -> {
      final Map<String, String> response = responseGetter.apply(challenge);
      response.putIfAbsent(LobbyLoginResponseKeys.EMAIL, EMAIL);
      response.putIfAbsent(
          LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
      return loginValidator.verifyConnection(challenge, response, name, mac, address);
    };
  }

  private void createUser(final String name, final HashedPassword password) {
    TestLobbyConfigurations.INTEGRATION_TEST
        .getDatabaseDao()
        .getUserDao()
        .createUser(name, EMAIL, password);
  }

  @Test
  void testCreateNewUser() {
    final String name = "a" + TestUserUtils.newUniqueTimestamp();
    final String password = "password";
    final Map<String, String> response = new HashMap<>();
    response.put(LobbyLoginResponseKeys.REGISTER_NEW_USER, Boolean.TRUE.toString());
    assertNull(
        generateChallenge(name, null)
            .apply(
                challenge -> {
                  response.put(
                      LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD,
                      RsaAuthenticator.encrpytPassword(
                          challenge.get(LobbyLoginChallengeKeys.RSA_PUBLIC_KEY), password));
                  return response;
                }));

    // try to create a duplicate user, should not work
    assertNotNull(
        generateChallenge(name, null)
            .apply(
                challenge -> {
                  response.put(
                      LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD,
                      RsaAuthenticator.encrpytPassword(
                          challenge.get(LobbyLoginChallengeKeys.RSA_PUBLIC_KEY), "wrong"));
                  return response;
                }));
    assertTrue(
        BCrypt.checkpw(
            hashPasswordWithSalt(password),
            TestLobbyConfigurations.INTEGRATION_TEST
                .getDatabaseDao()
                .getUserDao()
                .getPassword(name)
                .value));
  }

  @Test
  void testWrongVersion() {
    assertNotNull(
        generateChallenge(null)
            .apply(
                challenge -> {
                  final Map<String, String> response = new HashMap<>();
                  response.put(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
                  response.put(LobbyLoginResponseKeys.LOBBY_VERSION, "0.1");
                  return response;
                }));
  }

  @Test
  void testAnonymousLogin() {
    final Map<String, String> response = new HashMap<>();
    response.put(LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
    assertNull(generateChallenge(null).apply(challenge -> response));

    // create a user, verify we can't login with a username that already exists
    // we should not be able to login now
    assertNotNull(
        generateChallenge(
                new HashedPassword(BCrypt.hashpw(hashPasswordWithSalt("foo"), BCrypt.gensalt())))
            .apply(challenge -> response));
  }

  @Test
  void testLogin() {
    final String user = TestUserUtils.newUniqueTimestamp();
    final String password = "foo";
    final Map<String, String> response = new HashMap<>();
    assertNull(
        generateChallenge(
                user,
                new HashedPassword(BCrypt.hashpw(hashPasswordWithSalt(password), BCrypt.gensalt())))
            .apply(
                challenge -> {
                  response.put(
                      LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD,
                      RsaAuthenticator.encrpytPassword(
                          challenge.get(LobbyLoginChallengeKeys.RSA_PUBLIC_KEY), password));
                  return response;
                }));
    // with a bad password
    assertError(
        generateChallenge(user, null)
            .apply(
                challenge -> {
                  final Map<String, String> map = new HashMap<>();
                  map.put(
                      LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD,
                      RsaAuthenticator.encrpytPassword(
                          challenge.get(LobbyLoginChallengeKeys.RSA_PUBLIC_KEY), "wrong"));
                  return map;
                }),
        "password");
    // with a non existent user
    assertError(generateChallenge(null).apply(challenge -> response), "user");
  }

  private static void assertError(final @Nullable String errorMessage, final String... strings) {
    Arrays.stream(strings)
        .forEach(string -> assertThat(errorMessage, StringContains.containsString(string)));
  }

  private interface ChallengeResultFunction
      extends Function<Function<Map<String, String>, Map<String, String>>, String> {}
}
