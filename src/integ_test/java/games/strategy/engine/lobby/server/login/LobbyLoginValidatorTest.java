package games.strategy.engine.lobby.server.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Test;
import org.mindrot.jbcrypt.BCrypt;

import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.db.BadWordController;
import games.strategy.engine.lobby.server.db.DbUserController;
import games.strategy.engine.lobby.server.db.HashedPassword;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.net.MacFinder;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

public class LobbyLoginValidatorTest {

  @Test
  public void testLegacyCreateNewUser() {
    final Map<String, String> properties = new HashMap<>();
    final Function<Map<String, String>, String> challengeFunction =
        generateChallenge(null,
            challengeProperties -> {
              properties.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
              properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt("123", "foo"));
            });
    assertNull(challengeFunction.apply(properties));
    // try to create a duplicate user, should not work
    assertNotNull(challengeFunction.apply(properties));
  }

  @Test
  public void testCreateNewUser() {
    final String name = Util.createUniqueTimeStamp();
    final String password = "password";
    final Map<String, String> properties = new HashMap<>();
    final Consumer<Map<String, String>> challengeHandler = challengeProperties -> {
      properties.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
      RsaAuthenticator.appendEncryptedPassword(properties, challengeProperties, password);
    };
    assertNull(generateChallenge(name, null, challengeHandler).apply(properties));

    final Function<Map<String, String>, String> challengeFunction =
        generateChallenge(name, null, challengeProperties -> {
          challengeHandler.accept(challengeProperties);
          RsaAuthenticator.appendEncryptedPassword(properties, challengeProperties, "wrong");
        });
    // try to create a duplicate user, should not work
    assertNotNull(challengeFunction.apply(properties));
    assertTrue(BCrypt.checkpw(Util.sha512(password), new DbUserController().getPassword(name).value));
  }

  @Test
  public void testWrongVersion() {
    final Map<String, String> properties = new HashMap<>();
    assertNotNull(generateChallenge(null, challengeProperties -> {
      properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
      properties.put(LobbyLoginValidator.LOBBY_VERSION, "0.1");
    }).apply(properties));
  }

  @Test
  public void testAnonymousLogin() {
    final Map<String, String> properties = new HashMap<>();
    assertNull(generateChallenge(null, challengeProperties -> {
      properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
    }).apply(properties));

    // create a user, verify we can't login with a username that already exists
    // we should not be able to login now
    final String name = Util.createUniqueTimeStamp();
    assertNotNull(generateChallenge(name, new HashedPassword(MD5Crypt.crypt("foo")), challengeProperties -> {
      properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
    }).apply(properties));
  }

  @Test
  public void testAnonymousLoginBadName() {
    final String name = "bitCh" + Util.createUniqueTimeStamp();
    new BadWordController().addBadWord("bitCh");
    final Map<String, String> properties = new HashMap<>();
    assertEquals(LobbyLoginValidator.THATS_NOT_A_NICE_NAME,
        generateChallenge(name, new HashedPassword(MD5Crypt.crypt("foo")), challengeProperties -> {
          properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
        }).apply(properties));
  }

  @Test
  public void testLegacyLogin() {
    final String hashedPassword = MD5Crypt.crypt("foo");
    final Map<String, String> properties = new HashMap<>();
    final Map<String, String> persistentChallenge = new HashMap<>();
    final Function<Map<String, String>, String> challengeFunction =
        generateChallenge(new HashedPassword(hashedPassword), challengeProperties -> {
          properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, hashedPassword);
          assertEquals(challengeProperties.get(LobbyLoginValidator.SALT_KEY),
              MD5Crypt.getSalt(MD5Crypt.MAGIC, hashedPassword));
          persistentChallenge.putAll(challengeProperties);
        });
    assertNull(challengeFunction.apply(properties));
    // with a bad password
    properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt("wrong"));
    assertNotNull(challengeFunction.apply(properties));
    // with a non existent user
    assertNotNull(generateChallenge(null, challengeProperties -> challengeProperties.putAll(persistentChallenge))
        .apply(properties));
  }

  @Test
  public void testLogin() {
    final String password = "foo";
    final Map<String, String> properties = new HashMap<>();
    final Map<String, String> persistentChallenge = new HashMap<>();
    final Function<Map<String, String>, String> challengeFunction =
        generateChallenge(new HashedPassword(BCrypt.hashpw(Util.sha512(password), BCrypt.gensalt())),
            challengeProperties -> {
              RsaAuthenticator.appendEncryptedPassword(properties, challengeProperties, password);
              persistentChallenge.putAll(challengeProperties);
            });
    assertNull(challengeFunction.apply(properties));
    final Map<String, String> badPassMap = new HashMap<>(persistentChallenge);
    // with a bad password
    RsaAuthenticator.appendEncryptedPassword(badPassMap, persistentChallenge, "wrong");
    assertNotNull(challengeFunction.apply(badPassMap));
    // with a non existent user
    assertNotNull(generateChallenge(null, challengeProperties -> {
      challengeProperties.putAll(persistentChallenge);
    }).apply(properties));
  }

  @Test
  public void testLegacyLoginCombined() {
    final String name = Util.createUniqueTimeStamp();
    final String password = "foo";
    final String hashedPassword = MD5Crypt.crypt(password);
    final Map<String, String> properties = new HashMap<>();
    final Function<Map<String, String>, String> challengeFunction =
        generateChallenge(name, new HashedPassword(hashedPassword),
            challengeProperties -> {
              properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, hashedPassword);
              RsaAuthenticator.appendEncryptedPassword(properties, challengeProperties, password);
              assertEquals(challengeProperties.get(LobbyLoginValidator.SALT_KEY),
                  MD5Crypt.getSalt(MD5Crypt.MAGIC, hashedPassword));
            });
    assertNull(challengeFunction.apply(properties));
    assertTrue(BCrypt.checkpw(Util.sha512(password), new DbUserController().getPassword(name).value));
    properties.remove(RsaAuthenticator.ENCRYPTED_PASSWORD_KEY);
    assertNull(challengeFunction.apply(properties));
    assertEquals(hashedPassword, new DbUserController().getLegacyPassword(name).value);
  }

  @Test
  public void testTimeout() {
    final String password = "foo";
    final Map<String, String> properties = new HashMap<>();
    final Function<Map<String, String>, String> challengeFunction =
        generateChallenge(new HashedPassword(BCrypt.hashpw(Util.sha512(password), BCrypt.gensalt())),
            challengeProperties -> {
              RsaAuthenticator.appendEncryptedPassword(properties, challengeProperties, password);
              RsaAuthenticator.invalidateAll();
            });

    final String errorMessage = challengeFunction.apply(properties);
    assertNotNull(errorMessage);
    assertTrue(errorMessage.toLowerCase().contains("timeout"));
  }

  @Test
  public void testChallengeExpires() {
    final String password = "password";
    final Map<String, String> properties = new HashMap<>();
    final Function<Map<String, String>, String> challengeFunction =
        generateChallenge(new HashedPassword(BCrypt.hashpw(Util.sha512(password), BCrypt.gensalt())),
            challengeProperties -> RsaAuthenticator.appendEncryptedPassword(properties, challengeProperties, password));
    assertNull(challengeFunction.apply(properties));
    assertNotNull(challengeFunction.apply(properties));
  }

  private static Function<Map<String, String>, String> generateChallenge(final HashedPassword password,
      Consumer<Map<String, String>> action) {
    return generateChallenge(Util.createUniqueTimeStamp(), password, action);
  }

  private static void createUser(final String name, final String email, final HashedPassword password) {
    new DbUserController().createUser(new DBUser(new DBUser.UserName(name), new DBUser.UserEmail(email)), password);
  }

  private static Function<Map<String, String>, String> generateChallenge(final String name,
      final HashedPassword password,
      Consumer<Map<String, String>> action) {
    final LobbyLoginValidator validator = new LobbyLoginValidator();
    final SocketAddress address = new InetSocketAddress(5000);
    final String mac = MacFinder.getHashedMacAddress();
    final String email = "none@none.none";
    if (password != null) {
      createUser(name, email, password);
    }
    final Map<String, String> challenge = validator.getChallengeProperties(name, address);
    action.accept(challenge);
    return response -> {
      response.putIfAbsent(LobbyLoginValidator.EMAIL_KEY, email);
      response.putIfAbsent(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
      return new LobbyLoginValidator().verifyConnection(challenge, response, name, mac, address);
    };
  }
}
