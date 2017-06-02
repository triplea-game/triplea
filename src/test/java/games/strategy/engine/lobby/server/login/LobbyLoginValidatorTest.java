package games.strategy.engine.lobby.server.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.userDB.BadWordController;
import games.strategy.engine.lobby.server.userDB.DbUser;
import games.strategy.engine.lobby.server.userDB.HashedPassword;
import games.strategy.engine.lobby.server.userDB.DbUserController;
import games.strategy.net.MacFinder;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Util;

public class LobbyLoginValidatorTest {

  @Test
  public void testCreateNewUser() {
    final LobbyLoginValidator validator = new LobbyLoginValidator();
    final SocketAddress address = new InetSocketAddress(5000);
    final String name = Util.createUniqueTimeStamp();
    final String mac = MacFinder.getHashedMacAddress();
    final Map<String, String> properties = new HashMap<>();
    properties.put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
    properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt("123", "foo"));
    properties.put(LobbyLoginValidator.EMAIL_KEY, "none@none.none");
    properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
    assertNull(new LobbyLoginValidator().verifyConnection(validator.getChallengeProperties(name, address), properties,
        name, mac, address));
    // try to create a duplicate user, should not work
    assertNotNull(new LobbyLoginValidator().verifyConnection(validator.getChallengeProperties(name, address),
        properties, name, mac, address));
  }

  @Test
  public void testWrongVersion() {
    final LobbyLoginValidator validator = new LobbyLoginValidator();
    final SocketAddress address = new InetSocketAddress(5000);
    final String name = Util.createUniqueTimeStamp();
    final String mac = MacFinder.getHashedMacAddress();
    final Map<String, String> properties = new HashMap<>();
    properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
    properties.put(LobbyLoginValidator.LOBBY_VERSION, "0.1");
    assertNotNull(new LobbyLoginValidator().verifyConnection(validator.getChallengeProperties(name, address),
        properties, name, mac, address));
  }

  @Test
  public void testAnonymousLogin() {
    final LobbyLoginValidator validator = new LobbyLoginValidator();
    final SocketAddress address = new InetSocketAddress(5000);
    final String name = Util.createUniqueTimeStamp();
    final String mac = MacFinder.getHashedMacAddress();
    final Map<String, String> properties = new HashMap<>();
    properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
    properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
    assertNull(new LobbyLoginValidator().verifyConnection(validator.getChallengeProperties(name, address), properties,
        name, mac, address));
    // create a user, verify we can't login with a username that already exists
    new DbUserController().createUser(
        new DbUser(
            new DbUser.UserName(name),
            new DbUser.UserEmail("none@none.none")),
          new HashedPassword(MD5Crypt.crypt("foo")));
    // we should not be able to login now
    assertNotNull(new LobbyLoginValidator().verifyConnection(validator.getChallengeProperties(name, address),
        properties, name, mac, address));
  }

  @Test
  public void testAnonymousLoginBadName() {
    final LobbyLoginValidator validator = new LobbyLoginValidator();
    final SocketAddress address = new InetSocketAddress(5000);
    final String name = "bitCh" + Util.createUniqueTimeStamp();
    final String mac = MacFinder.getHashedMacAddress();
    new BadWordController().addBadWord("bitCh");
    final Map<String, String> properties = new HashMap<>();
    properties.put(LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString());
    properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
    assertEquals(LobbyLoginValidator.THATS_NOT_A_NICE_NAME, new LobbyLoginValidator()
        .verifyConnection(validator.getChallengeProperties(name, address), properties, name, mac, address));
  }

  @Test
  public void testLogin() {
    final LobbyLoginValidator validator = new LobbyLoginValidator();
    final SocketAddress address = new InetSocketAddress(5000);
    final String name = Util.createUniqueTimeStamp();
    final String mac = MacFinder.getHashedMacAddress();
    final String email = "none@none.none";
    final String password = "foo";
    final String hashedPassword = MD5Crypt.crypt(password);
    new DbUserController().createUser(
        new DbUser(
            new DbUser.UserName(name),
            new DbUser.UserEmail(email)),
        new HashedPassword(hashedPassword));
    final Map<String, String> properties = new HashMap<>();
    properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, hashedPassword);
    properties.put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
    final Map<String, String> challengeProperties = validator.getChallengeProperties(name, address);
    assertEquals(challengeProperties.get(LobbyLoginValidator.SALT_KEY),
        MD5Crypt.getSalt(MD5Crypt.MAGIC, hashedPassword));
    assertNull(new LobbyLoginValidator().verifyConnection(challengeProperties, properties, name, mac, address));
    // with a bad password
    properties.put(LobbyLoginValidator.HASHED_PASSWORD_KEY, MD5Crypt.crypt("wrong"));
    assertNotNull(new LobbyLoginValidator().verifyConnection(challengeProperties, properties, name, mac, address));
    // with a non existent user
    assertNotNull(new LobbyLoginValidator().verifyConnection(challengeProperties, properties,
        Util.createUniqueTimeStamp(), mac, address));
  }
}
