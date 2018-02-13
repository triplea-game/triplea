package games.strategy.engine.lobby.server.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;

import com.example.mockito.MockitoExtension;
import com.google.common.collect.ImmutableMap;

import games.strategy.engine.config.PropertyReader;
import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.engine.lobby.server.TestUserUtils;
import games.strategy.engine.lobby.server.User;
import games.strategy.engine.lobby.server.db.BadWordDao;
import games.strategy.engine.lobby.server.db.BannedMacDao;
import games.strategy.engine.lobby.server.db.BannedUsernameDao;
import games.strategy.engine.lobby.server.db.HashedPassword;
import games.strategy.engine.lobby.server.db.UserDao;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.security.TestSecurityUtils;
import games.strategy.util.Tuple;

@ExtendWith(MockitoExtension.class)
public final class LobbyLoginValidatorTest {

  interface ResponseGenerator extends Function<Map<String, String>, Map<String, String>> {
  }

  abstract class AbstractTestCase {
    static final String EMAIL = "n@n.com";
    static final String PASSWORD = "password";

    @Mock
    private AccessLog accessLog;

    @Mock
    private BadWordDao badWordDao;

    @Mock
    private BannedMacDao bannedMacDao;

    @Mock
    private BannedUsernameDao bannedUsernameDao;

    @Mock
    private PropertyReader propertyReader;

    @Mock
    private UserDao userDao;

    private LobbyLoginValidator lobbyLoginValidator;

    private String authenticationErrorMessage;

    private final String bcryptSalt = BCrypt.gensalt();

    private final User user = TestUserUtils.newUser();

    private final DBUser dbUser = new DBUser(new DBUser.UserName(user.getUsername()), new DBUser.UserEmail(EMAIL));

    private final String md5CryptSalt = games.strategy.util.Md5Crypt.newSalt();

    @BeforeEach
    public void setUp() throws IOException, GeneralSecurityException {
      lobbyLoginValidator = new LobbyLoginValidator(
          new LobbyPropertyReader(propertyReader),
          badWordDao,
          bannedMacDao,
          bannedUsernameDao,
          userDao,
          accessLog,
          new RsaAuthenticator(TestSecurityUtils.loadRsaKeyPair()),
          () -> bcryptSalt);

      givenNoMacIsBanned();
      givenNoUsernameIsBanned();
    }

    final String bcrypt(final String password) {
      return BCrypt.hashpw(obfuscate(password), bcryptSalt);
    }

    private String obfuscate(final String password) {
      return RsaAuthenticator.hashPasswordWithSalt(password);
    }

    final String md5Crypt(final String password) {
      return games.strategy.util.Md5Crypt.crypt(password, md5CryptSalt);
    }

    final void givenAnonymousAuthenticationWillFail() {
      when(userDao.doesUserExist(user.getUsername())).thenReturn(true);
    }

    final void givenAnonymousAuthenticationWillSucceed() {
      when(userDao.doesUserExist(user.getUsername())).thenReturn(false);
    }

    final void givenAuthenticationWillUseMd5CryptedPasswordAndSucceed() {
      when(userDao.login(user.getUsername(), new HashedPassword(md5Crypt(PASSWORD)))).thenReturn(true);
    }

    final void givenAuthenticationWillUseMd5CryptedPasswordAndFail() {
      when(userDao.login(user.getUsername(), new HashedPassword(md5Crypt(PASSWORD)))).thenReturn(false);
    }

    final void givenAuthenticationWillUseObfuscatedPasswordAndSucceed() {
      when(userDao.login(user.getUsername(), new HashedPassword(obfuscate(PASSWORD)))).thenReturn(true);
    }

    final void givenAuthenticationWillUseObfuscatedPasswordAndFail() {
      when(userDao.login(user.getUsername(), new HashedPassword(obfuscate(PASSWORD)))).thenReturn(false);
    }

    final void givenMaintenanceModeIsEnabled() {
      when(propertyReader.readProperty(LobbyPropertyReader.PropertyKeys.MAINTENANCE_MODE))
          .thenReturn(Boolean.TRUE.toString());
    }

    private void givenNoMacIsBanned() {
      when(bannedMacDao.isMacBanned(anyString())).thenReturn(Tuple.of(false, new Timestamp(0L)));
    }

    private void givenNoUsernameIsBanned() {
      when(bannedUsernameDao.isUsernameBanned(anyString())).thenReturn(Tuple.of(false, new Timestamp(0L)));
    }

    final void givenUserDoesNotExist() {
      when(userDao.doesUserExist(user.getUsername())).thenReturn(false);
    }

    final void givenUserDoesNotHaveBcryptedPassword() {
      when(userDao.getPassword(user.getUsername())).thenReturn(new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void givenUserDoesNotHaveMd5CryptedPassword() {
      when(userDao.getLegacyPassword(user.getUsername())).thenReturn(new HashedPassword(""));
    }

    final void givenUserExists() {
      when(userDao.getUserByName(user.getUsername())).thenReturn(dbUser);
    }

    final void givenUserHasBcryptedPassword() {
      when(userDao.getPassword(user.getUsername())).thenReturn(new HashedPassword(bcrypt(PASSWORD)));
    }

    final void givenUserHasMd5CryptedPassword() {
      when(userDao.getLegacyPassword(user.getUsername())).thenReturn(new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void whenAuthenticating(final ResponseGenerator responseGenerator) {
      final InetSocketAddress remoteAddress = new InetSocketAddress(user.getInetAddress(), 9999);
      final Map<String, String> challenge =
          lobbyLoginValidator.getChallengeProperties(user.getUsername(), remoteAddress);
      authenticationErrorMessage = lobbyLoginValidator.verifyConnection(
          challenge,
          responseGenerator.apply(challenge),
          user.getUsername(),
          user.getHashedMacAddress(),
          remoteAddress);
    }

    final void thenAccessLogShouldReceiveFailedAccessOf(final AuthenticationType authenticationType) {
      verify(accessLog).logFailedAccess(any(Instant.class), eq(user), eq(authenticationType), anyString());
    }

    final void thenAccessLogShouldReceiveSuccessfulAccessOf(final AuthenticationType authenticationType) {
      verify(accessLog).logSuccessfulAccess(any(Instant.class), eq(user), eq(authenticationType));
    }

    final void thenAuthenticationShouldFail() {
      assertThat(authenticationErrorMessage, is(not(nullValue())));
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
  }

  @Nested
  public final class DatabaseInteractionTest {
    @Nested
    public final class WhenUserIsAnonymousTest extends AbstractTestCase {
      @Test
      public void shouldNotCreateOrUpdateUserWhenAuthenticationSucceeds() {
        givenAnonymousAuthenticationWillSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldNotBeCreated();
        thenUserShouldNotBeUpdated();
      }

      @Test
      public void shouldNotCreateOrUpdateUserWhenAuthenticationFails() {
        givenAnonymousAuthenticationWillFail();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFailWithMessage(LobbyLoginValidator.ErrorMessages.ANONYMOUS_AUTHENTICATION_FAILED);
        thenUserShouldNotBeCreated();
        thenUserShouldNotBeUpdated();
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.of(
            LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString(),
            LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
      }
    }

    @Nested
    public final class WhenUserDoesNotExistTest {
      @Nested
      public final class WhenUsingLegacyClientTest extends AbstractTestCase {
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

      @Nested
      public final class WhenUsingCurrentClientTest extends AbstractTestCase {
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
              .putAll(RsaAuthenticator.newResponse(challenge, PASSWORD))
              .build();
        }
      }
    }

    @Nested
    public final class WhenUserExistsTest {
      @Nested
      public final class WhenUsingLegacyClientTest extends AbstractTestCase {
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

          thenAuthenticationShouldFailWithMessage(LobbyLoginValidator.ErrorMessages.AUTHENTICATION_FAILED);
          thenUserShouldNotBeCreated();
          thenUserShouldNotBeUpdated();
        }

        private ResponseGenerator givenAuthenticationResponse() {
          return challenge -> ImmutableMap.of(
              LobbyLoginValidator.HASHED_PASSWORD_KEY, md5Crypt(PASSWORD),
              LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
        }
      }

      @Nested
      public final class WhenUsingCurrentClientTest extends AbstractTestCase {
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
              .putAll(RsaAuthenticator.newResponse(challenge, PASSWORD))
              .build();
        }
      }
    }
  }

  @Nested
  public final class MaintenanceModeTest {
    @Nested
    public final class WhenUsingLegacyClientTest extends AbstractTestCase {
      @Test
      public void shouldFailAuthentication() {
        givenMaintenanceModeIsEnabled();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFailWithMessage(LobbyLoginValidator.ErrorMessages.MAINTENANCE_MODE_ENABLED);
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> {
          thenChallengeShouldBeProcessableByMd5CryptAuthenticator(challenge);
          return ImmutableMap.of(
              LobbyLoginValidator.HASHED_PASSWORD_KEY, md5Crypt(PASSWORD),
              LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
        };
      }

      private void thenChallengeShouldBeProcessableByMd5CryptAuthenticator(final Map<String, String> challenge) {
        assertThat(challenge.containsKey(LobbyLoginValidator.SALT_KEY), is(true));
      }
    }

    @Nested
    public final class WhenUsingCurrentClientTest extends AbstractTestCase {
      @Test
      public void shouldFailAuthentication() {
        givenMaintenanceModeIsEnabled();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFailWithMessage(LobbyLoginValidator.ErrorMessages.MAINTENANCE_MODE_ENABLED);
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> {
          thenChallengeShouldBeProcessableByRsaAuthenticator(challenge);
          return ImmutableMap.<String, String>builder()
              .put(LobbyLoginValidator.HASHED_PASSWORD_KEY, md5Crypt(PASSWORD))
              .put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString())
              .putAll(RsaAuthenticator.newResponse(challenge, PASSWORD))
              .build();
        };
      }

      private void thenChallengeShouldBeProcessableByRsaAuthenticator(final Map<String, String> challenge) {
        assertThat(RsaAuthenticator.canProcessChallenge(challenge), is(true));
      }
    }
  }

  @Nested
  public final class AccessLogTest {
    @Nested
    public final class WhenUserIsAnonymous extends AbstractTestCase {
      @Test
      public void shouldLogSuccessfulLoginWhenAuthenticationSucceeds() {
        givenAnonymousAuthenticationWillSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenAccessLogShouldReceiveSuccessfulAccessOf(AuthenticationType.ANONYMOUS);
      }

      @Test
      public void shouldLogFailedLoginWhenAuthenticationFails() {
        givenAnonymousAuthenticationWillFail();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFail();
        thenAccessLogShouldReceiveFailedAccessOf(AuthenticationType.ANONYMOUS);
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.of(
            LobbyLoginValidator.ANONYMOUS_LOGIN, Boolean.TRUE.toString(),
            LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString());
      }
    }

    @Nested
    public final class WhenUserIsRegistered extends AbstractTestCase {
      @Test
      public void shouldLogSuccessfulLoginWhenAuthenticationSucceeds() {
        givenUserExists();
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenAccessLogShouldReceiveSuccessfulAccessOf(AuthenticationType.REGISTERED);
      }

      @Test
      public void shouldLogFailedLoginWhenAuthenticationFails() {
        givenUserExists();
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndFail();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFail();
        thenAccessLogShouldReceiveFailedAccessOf(AuthenticationType.REGISTERED);
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.<String, String>builder()
            .put(LobbyLoginValidator.LOBBY_VERSION, LobbyServer.LOBBY_VERSION.toString())
            .putAll(RsaAuthenticator.newResponse(challenge, PASSWORD))
            .build();
      }
    }
  }
}
