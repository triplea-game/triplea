package games.strategy.engine.lobby.server.login;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.Map;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.db.BadWordDao;
import games.strategy.engine.lobby.server.db.BannedMacDao;
import games.strategy.engine.lobby.server.db.BannedUsernameDao;
import games.strategy.engine.lobby.server.db.HashedPassword;
import games.strategy.engine.lobby.server.db.UserDao;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.util.MD5Crypt;
import games.strategy.util.Tuple;

@RunWith(Enclosed.class)
public final class LobbyLoginValidatorTests {
  private abstract static class AbstractTestCase {
    static final String EMAIL = "n@n.com";
    static final String PASSWORD = "password";
    private static final SocketAddress REMOTE_ADDRESS = new InetSocketAddress(9999);
    private static final String USERNAME = "username";

    @Mock
    private BadWordDao badWordDao;

    @Mock
    private BannedMacDao bannedMacDao;

    @Mock
    private BannedUsernameDao bannedUsernameDao;

    @Mock
    private UserDao userDao;

    private LobbyLoginValidator lobbyLoginValidator;

    private String authenticationErrorMessage;

    private final String bcryptSalt = BCrypt.gensalt();

    private final DBUser dbUser = new DBUser(new DBUser.UserName(USERNAME), new DBUser.UserEmail(EMAIL));

    private final String md5CryptSalt = MD5Crypt.newSalt();

    @Before
    public void setUp() {
      lobbyLoginValidator = new LobbyLoginValidator(
          badWordDao,
          bannedMacDao,
          bannedUsernameDao,
          userDao,
          () -> bcryptSalt);

      givenNoMacIsBanned();
      givenNoUsernameIsBanned();
    }

    final String bcrypt(final String password) {
      return BCrypt.hashpw(obfuscate(password), bcryptSalt);
    }

    private static String obfuscate(final String password) {
      return RsaAuthenticator.hashPasswordWithSalt(password);
    }

    final String md5Crypt(final String password) {
      return MD5Crypt.crypt(password, md5CryptSalt);
    }

    final void givenAuthenticationWillUseMd5CryptedPasswordAndSucceed() {
      when(userDao.login(USERNAME, new HashedPassword(md5Crypt(PASSWORD)))).thenReturn(true);
    }

    final void givenAuthenticationWillUseMd5CryptedPasswordAndFail() {
      when(userDao.login(USERNAME, new HashedPassword(md5Crypt(PASSWORD)))).thenReturn(false);
    }

    final void givenAuthenticationWillUseObfuscatedPasswordAndSucceed() {
      when(userDao.login(USERNAME, new HashedPassword(obfuscate(PASSWORD)))).thenReturn(true);
    }

    private void givenNoMacIsBanned() {
      when(bannedMacDao.isMacBanned(anyString())).thenReturn(Tuple.of(false, new Timestamp(0L)));
    }

    private void givenNoUsernameIsBanned() {
      when(bannedUsernameDao.isUsernameBanned(anyString())).thenReturn(Tuple.of(false, new Timestamp(0L)));
    }

    final void givenUserDoesNotExist() {
      when(userDao.doesUserExist(USERNAME)).thenReturn(false);
    }

    final void givenUserDoesNotHaveBcryptedPassword() {
      when(userDao.getPassword(USERNAME)).thenReturn(new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void givenUserDoesNotHaveMd5CryptedPassword() {
      when(userDao.getLegacyPassword(USERNAME)).thenReturn(new HashedPassword(""));
    }

    final void givenUserExists() {
      when(userDao.getUserByName(USERNAME)).thenReturn(dbUser);
    }

    final void givenUserHasBcryptedPassword() {
      when(userDao.getPassword(USERNAME)).thenReturn(new HashedPassword(bcrypt(PASSWORD)));
    }

    final void givenUserHasMd5CryptedPassword() {
      when(userDao.getLegacyPassword(USERNAME)).thenReturn(new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void whenAuthenticating(final ResponseGenerator responseGenerator) {
      final String hashedMac = "$1$MH$lW2b9Tx3VIpD4llOnivrP1";
      final Map<String, String> challenge = lobbyLoginValidator.getChallengeProperties(USERNAME, REMOTE_ADDRESS);
      authenticationErrorMessage = lobbyLoginValidator.verifyConnection(
          challenge,
          responseGenerator.apply(challenge),
          USERNAME,
          hashedMac,
          REMOTE_ADDRESS);
    }

    final void thenAuthenticationShouldFailWithMessage(final String errorMessage) {
      assertThat(authenticationErrorMessage, is(errorMessage));
    }

    final void thenAuthenticationShouldSucceed() {
      assertThat(authenticationErrorMessage, is(nullValue()));
    }

    final void thenUserShouldBeCreatedWithMd5CryptedPassword() {
      verify(userDao).createUser(dbUser, new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void thenUserShouldBeUpdatedWithBcryptedPassword() {
      verify(userDao).updateUser(dbUser, new HashedPassword(bcrypt(PASSWORD)));
    }

    final void thenUserShouldBeUpdatedWithMd5CryptedPassword() {
      verify(userDao).updateUser(dbUser, new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void thenUserShouldNotBeCreated() {
      verify(userDao, never()).createUser(any(DBUser.class), any(HashedPassword.class));
    }

    final void thenUserShouldNotBeUpdatedWithBcryptedPassword() {
      verify(userDao, never()).updateUser(eq(dbUser), argThat(HashedPassword::isBcrypted));
    }

    final void thenUserShouldNotBeUpdatedWithMd5CryptedPassword() {
      verify(userDao, never()).updateUser(eq(dbUser), argThat(HashedPassword::isMd5Crypted));
    }

    final void thenUserShouldNotBeUpdated() {
      verify(userDao, never()).updateUser(any(DBUser.class), any(HashedPassword.class));
    }

    interface ResponseGenerator extends Function<Map<String, String>, Map<String, String>> {
    }
  }

  @RunWith(MockitoJUnitRunner.StrictStubs.class)
  public static final class WhenUserIsAnonymousTest extends AbstractTestCase {
    @Test
    public void shouldNotCreateOrUpdateUser() {
      givenUserDoesNotExist();

      whenAuthenticating(givenAuthenticationResponse());

      thenAuthenticationShouldSucceed();
      thenUserShouldNotBeCreated();
      thenUserShouldNotBeUpdated();
    }

    private static ResponseGenerator givenAuthenticationResponse() {
      return challenge -> ImmutableMap.of(
          LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString(),
          LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
    }
  }

  @RunWith(Enclosed.class)
  public static final class WhenUserDoesNotExistTests {
    @RunWith(MockitoJUnitRunner.StrictStubs.class)
    public static final class WhenUsingLegacyClientTest extends AbstractTestCase {
      @Test
      public void shouldCreateNewUserWithOnlyMd5CryptedPassword() {
        givenUserDoesNotExist();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldBeCreatedWithMd5CryptedPassword();
        thenUserShouldNotBeUpdatedWithBcryptedPassword();
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.of(
            LobbyLoginValidator.EMAIL_KEY, EMAIL,
            LobbyLoginValidator.HASHED_PASSWORD_KEY, md5Crypt(PASSWORD),
            LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString(),
            LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString());
      }
    }

    @RunWith(MockitoJUnitRunner.StrictStubs.class)
    public static final class WhenUsingCurrentClientTest extends AbstractTestCase {
      @Test
      public void shouldCreateNewUserWithBothPasswords() {
        givenUserDoesNotExist();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldBeCreatedWithMd5CryptedPassword();
        thenUserShouldBeUpdatedWithBcryptedPassword();
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.<String, String>builder()
            .put(LobbyLoginValidator.EMAIL_KEY, EMAIL)
            .put(LobbyLoginValidator.HASHED_PASSWORD_KEY, md5Crypt(PASSWORD))
            .put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString())
            .put(LobbyLoginValidator.REGISTER_NEW_USER_KEY, Boolean.TRUE.toString())
            .putAll(RsaAuthenticator.getEncryptedPassword(challenge, PASSWORD))
            .build();
      }
    }
  }

  @RunWith(Enclosed.class)
  public static final class WhenUserExistsTests {
    @RunWith(MockitoJUnitRunner.StrictStubs.class)
    public static final class WhenUsingLegacyClientTest extends AbstractTestCase {
      @Test
      public void shouldNotUpdatePasswordsWhenUserHasOnlyMd5CryptedPassword() {
        givenUserDoesNotHaveBcryptedPassword();
        givenAuthenticationWillUseMd5CryptedPasswordAndSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldNotBeCreated();
        thenUserShouldNotBeUpdated();
      }

      @Test
      public void shouldNotUpdatePasswordsWhenUserHasBothPasswords() {
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseMd5CryptedPasswordAndSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldNotBeCreated();
        thenUserShouldNotBeUpdated();
      }

      @Test
      public void shouldNotUpdatePasswordsWhenUserHasOnlyBcryptedPassword() {
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseMd5CryptedPasswordAndFail();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFailWithMessage(LobbyLoginValidator.AUTHENTICATION_FAILED);
        thenUserShouldNotBeCreated();
        thenUserShouldNotBeUpdated();
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.of(
            LobbyLoginValidator.HASHED_PASSWORD_KEY, md5Crypt(PASSWORD),
            LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
      }
    }

    @RunWith(MockitoJUnitRunner.StrictStubs.class)
    public static final class WhenUsingCurrentClientTest extends AbstractTestCase {
      @Test
      public void shouldNotUpdatePasswordsWhenUserHasBothPasswords() {
        givenUserExists();
        givenUserHasMd5CryptedPassword();
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldNotBeCreated();
        thenUserShouldNotBeUpdated();
      }

      @Test
      public void shouldUpdateBcryptedPasswordWhenUserHasOnlyMd5CryptedPassword() {
        givenUserExists();
        givenUserHasMd5CryptedPassword();
        givenUserDoesNotHaveBcryptedPassword();
        givenAuthenticationWillUseMd5CryptedPasswordAndSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldNotBeCreated();
        thenUserShouldNotBeUpdatedWithMd5CryptedPassword();
        thenUserShouldBeUpdatedWithBcryptedPassword();
      }

      @Test
      public void shouldUpdateBothPasswordsWhenUserHasOnlyBcryptedPassword() {
        givenUserExists();
        givenUserDoesNotHaveMd5CryptedPassword();
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldNotBeCreated();
        thenUserShouldBeUpdatedWithMd5CryptedPassword();
        thenUserShouldBeUpdatedWithBcryptedPassword();
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.<String, String>builder()
            .put(LobbyLoginValidator.HASHED_PASSWORD_KEY, md5Crypt(PASSWORD))
            .put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString())
            .putAll(RsaAuthenticator.getEncryptedPassword(challenge, PASSWORD))
            .build();
      }
    }
  }
}
