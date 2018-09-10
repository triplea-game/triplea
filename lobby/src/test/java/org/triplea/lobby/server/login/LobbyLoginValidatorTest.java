package org.triplea.lobby.server.login;

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

import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.TestUserUtils;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.LobbyPropertyReader;
import org.triplea.lobby.server.db.BadWordDao;
import org.triplea.lobby.server.db.BannedMacDao;
import org.triplea.lobby.server.db.BannedUsernameDao;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.UserDao;
import org.triplea.test.common.security.TestSecurityUtils;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.config.MemoryPropertyReader;
import games.strategy.engine.lobby.common.LobbyConstants;
import games.strategy.engine.lobby.common.login.LobbyLoginChallengeKeys;
import games.strategy.engine.lobby.common.login.LobbyLoginResponseKeys;
import games.strategy.engine.lobby.common.login.RsaAuthenticator;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.util.Md5Crypt;
import games.strategy.util.Tuple;

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
    BannedMacDao bannedMacDao;

    @Mock
    BannedUsernameDao bannedUsernameDao;

    private final MemoryPropertyReader memoryPropertyReader = new MemoryPropertyReader();

    @Mock
    private UserDao userDao;

    private LobbyLoginValidator lobbyLoginValidator;

    private String authenticationErrorMessage;

    private final String bcryptSalt = BCrypt.gensalt();

    private final User user = TestUserUtils.newUser();

    private final DBUser dbUser = new DBUser(new DBUser.UserName(user.getUsername()), new DBUser.UserEmail(EMAIL));

    private final String md5CryptSalt = Md5Crypt.newSalt();

    @BeforeEach
    public void createLobbyLoginValidator() throws Exception {
      lobbyLoginValidator = new LobbyLoginValidator(
          new LobbyPropertyReader(memoryPropertyReader),
          badWordDao,
          bannedMacDao,
          bannedUsernameDao,
          userDao,
          accessLog,
          new RsaAuthenticator(TestSecurityUtils.loadRsaKeyPair()),
          () -> bcryptSalt);
    }

    final String bcrypt(final String password) {
      return BCrypt.hashpw(obfuscate(password), bcryptSalt);
    }

    private String obfuscate(final String password) {
      return RsaAuthenticator.hashPasswordWithSalt(password);
    }

    final String md5Crypt(final String password) {
      return Md5Crypt.hashPassword(password, md5CryptSalt);
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
      memoryPropertyReader.setProperty(LobbyPropertyReader.PropertyKeys.MAINTENANCE_MODE, String.valueOf(true));
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
          lobbyLoginValidator.getChallengeProperties(user.getUsername());
      authenticationErrorMessage = lobbyLoginValidator.verifyConnection(
          challenge,
          responseGenerator.apply(challenge),
          user.getUsername(),
          user.getHashedMacAddress(),
          remoteAddress);
    }

    final void thenAccessLogShouldReceiveFailedAuthentication(final UserType userType) {
      verify(accessLog).logFailedAuthentication(eq(user), eq(userType), anyString());
    }

    final void thenAccessLogShouldReceiveSuccessfulAuthentication(final UserType userType) {
      verify(accessLog).logSuccessfulAuthentication(eq(user), eq(userType));
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

  abstract class AbstractNoBansTestCase extends AbstractTestCase {
    @BeforeEach
    public void givenNoBans() {
      givenNoMacIsBanned();
      givenNoUsernameIsBanned();
    }

    private void givenNoMacIsBanned() {
      when(bannedMacDao.isMacBanned(anyString())).thenReturn(Tuple.of(false, new Timestamp(0L)));
    }

    private void givenNoUsernameIsBanned() {
      when(bannedUsernameDao.isUsernameBanned(anyString())).thenReturn(Tuple.of(false, new Timestamp(0L)));
    }
  }

  @Nested
  public final class DatabaseInteractionTest {
    @ExtendWith(MockitoExtension.class)
    @Nested
    public final class WhenUserIsAnonymousTest extends AbstractNoBansTestCase {
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
            LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString(),
            LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
      }
    }

    @Nested
    public final class WhenUserDoesNotExistTest {
      @ExtendWith(MockitoExtension.class)
      @Nested
      public final class WhenUsingLegacyClientTest extends AbstractNoBansTestCase {
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
              LobbyLoginResponseKeys.EMAIL, EMAIL,
              LobbyLoginResponseKeys.HASHED_PASSWORD, md5Crypt(PASSWORD),
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString(),
              LobbyLoginResponseKeys.REGISTER_NEW_USER, Boolean.TRUE.toString());
        }
      }

      @ExtendWith(MockitoExtension.class)
      @Nested
      public final class WhenUsingCurrentClientTest extends AbstractNoBansTestCase {
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
              .put(LobbyLoginResponseKeys.EMAIL, EMAIL)
              .put(LobbyLoginResponseKeys.HASHED_PASSWORD, md5Crypt(PASSWORD))
              .put(LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString())
              .put(LobbyLoginResponseKeys.REGISTER_NEW_USER, Boolean.TRUE.toString())
              .putAll(RsaAuthenticator.newResponse(challenge, PASSWORD))
              .build();
        }
      }
    }

    @Nested
    public final class WhenUserExistsTest {
      @ExtendWith(MockitoExtension.class)
      @Nested
      public final class WhenUsingLegacyClientTest extends AbstractNoBansTestCase {
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
              LobbyLoginResponseKeys.HASHED_PASSWORD, md5Crypt(PASSWORD),
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
        }
      }

      @ExtendWith(MockitoExtension.class)
      @Nested
      public final class WhenUsingCurrentClientTest extends AbstractNoBansTestCase {
        @Test
        public void shouldNotUpdatePasswordsWhenUserHasBothPasswords() {
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
              .put(LobbyLoginResponseKeys.HASHED_PASSWORD, md5Crypt(PASSWORD))
              .put(LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString())
              .putAll(RsaAuthenticator.newResponse(challenge, PASSWORD))
              .build();
        }
      }
    }
  }

  @Nested
  public final class MaintenanceModeTest {
    @ExtendWith(MockitoExtension.class)
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
              LobbyLoginResponseKeys.HASHED_PASSWORD, md5Crypt(PASSWORD),
              LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
        };
      }

      private void thenChallengeShouldBeProcessableByMd5CryptAuthenticator(final Map<String, String> challenge) {
        assertThat(challenge.containsKey(LobbyLoginChallengeKeys.SALT), is(true));
      }
    }

    @ExtendWith(MockitoExtension.class)
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
              .put(LobbyLoginResponseKeys.HASHED_PASSWORD, md5Crypt(PASSWORD))
              .put(LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString())
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
    @ExtendWith(MockitoExtension.class)
    @Nested
    public final class WhenUserIsAnonymous extends AbstractNoBansTestCase {
      @Test
      public void shouldLogSuccessfulAuthenticationWhenAuthenticationSucceeds() {
        givenAnonymousAuthenticationWillSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenAccessLogShouldReceiveSuccessfulAuthentication(UserType.ANONYMOUS);
      }

      @Test
      public void shouldLogFailedAuthenticationWhenAuthenticationFails() {
        givenAnonymousAuthenticationWillFail();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFail();
        thenAccessLogShouldReceiveFailedAuthentication(UserType.ANONYMOUS);
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.of(
            LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString(),
            LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
      }
    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    public final class WhenUserIsRegistered extends AbstractNoBansTestCase {
      @Test
      public void shouldLogSuccessfulAuthenticationWhenAuthenticationSucceeds() {
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenAccessLogShouldReceiveSuccessfulAuthentication(UserType.REGISTERED);
      }

      @Test
      public void shouldLogFailedAuthenticationWhenAuthenticationFails() {
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndFail();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFail();
        thenAccessLogShouldReceiveFailedAuthentication(UserType.REGISTERED);
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.<String, String>builder()
            .put(LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString())
            .putAll(RsaAuthenticator.newResponse(challenge, PASSWORD))
            .build();
      }
    }
  }
}
