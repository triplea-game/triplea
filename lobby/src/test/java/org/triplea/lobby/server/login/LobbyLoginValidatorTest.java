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
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.TestUserUtils;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.AccessLogDao;
import org.triplea.lobby.server.db.BadWordDao;
import org.triplea.lobby.server.db.BannedMacDao;
import org.triplea.lobby.server.db.DatabaseDao;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.UserDao;
import org.triplea.lobby.server.db.UsernameBlacklistDao;
import org.triplea.test.common.security.TestSecurityUtils;
import org.triplea.util.Md5Crypt;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.lobby.server.userDB.DBUser;

final class LobbyLoginValidatorTest {

  private interface ResponseGenerator extends Function<Map<String, String>, Map<String, String>> {
  }

  abstract class AbstractTestCase {
    static final String EMAIL = "n@n.com";
    static final String PASSWORD = "password";

    @Mock
    BannedMacDao bannedMacDao;

    @Mock
    UsernameBlacklistDao bannedUsernameDao;

    @Mock
    AccessLogDao accessLog;

    @Mock
    BadWordDao badWordDao;

    @Mock
    UserDao userDao;

    @Mock
    DatabaseDao databaseDao;

    private LobbyLoginValidator lobbyLoginValidator;

    private String authenticationErrorMessage;

    private final String bcryptSalt = BCrypt.gensalt();

    private final User user = TestUserUtils.newUser();

    private final DBUser dbUser = new DBUser(new DBUser.UserName(user.getUsername()), new DBUser.UserEmail(EMAIL));

    private final String md5CryptSalt = Md5Crypt.newSalt();

    @BeforeEach
    public void createLobbyLoginValidator() throws Exception {
      lobbyLoginValidator = new LobbyLoginValidator(
          databaseDao,
          new RsaAuthenticator(TestSecurityUtils.loadRsaKeyPair()),
          () -> bcryptSalt);
    }

    final String bcrypt(final String password) {
      return BCrypt.hashpw(obfuscate(password), bcryptSalt);
    }

    private String obfuscate(final String password) {
      return RsaAuthenticator.hashPasswordWithSalt(password);
    }

    @SuppressWarnings("deprecation") // required for testing; remove upon next lobby-incompatible release
    final String md5Crypt(final String password) {
      return Md5Crypt.hashPassword(password, md5CryptSalt);
    }

    final void givenAnonymousAuthenticationWillFail() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.doesUserExist(user.getUsername())).thenReturn(true);
    }

    final void givenAnonymousAuthenticationWillSucceed() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.doesUserExist(user.getUsername())).thenReturn(false);
    }

    final void givenAuthenticationWillUseMd5CryptedPasswordAndSucceed() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.login(user.getUsername(), new HashedPassword(md5Crypt(PASSWORD)))).thenReturn(true);
    }

    final void givenAuthenticationWillUseMd5CryptedPasswordAndFail() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.login(user.getUsername(), new HashedPassword(md5Crypt(PASSWORD)))).thenReturn(false);
    }

    final void givenAuthenticationWillUseObfuscatedPasswordAndSucceed() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.login(user.getUsername(), new HashedPassword(obfuscate(PASSWORD)))).thenReturn(true);
    }

    final void givenAuthenticationWillUseObfuscatedPasswordAndFail() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.login(user.getUsername(), new HashedPassword(obfuscate(PASSWORD)))).thenReturn(false);
    }

    final void givenUserDoesNotExist() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.doesUserExist(user.getUsername())).thenReturn(false);
    }

    final void givenUserDoesNotHaveBcryptedPassword() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.getPassword(user.getUsername())).thenReturn(new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void givenUserDoesNotHaveMd5CryptedPassword() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.getLegacyPassword(user.getUsername())).thenReturn(new HashedPassword(""));
    }

    final void givenUserExists() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.getUserByName(user.getUsername())).thenReturn(dbUser);
    }

    final void givenUserHasBcryptedPassword() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
      when(userDao.getPassword(user.getUsername())).thenReturn(new HashedPassword(bcrypt(PASSWORD)));
    }

    final void givenUserHasMd5CryptedPassword() {
      when(databaseDao.getUserDao()).thenReturn(userDao);
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

    final void thenAccessLogShouldReceiveSuccessfulAuthentication(final UserType userType) throws Exception {
      verify(accessLog).insert(eq(user), eq(userType));
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
      verify(userDao).createUser(dbUser.getName(), dbUser.getEmail(), new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void thenUserShouldBeUpdatedWithBcryptedPassword() {
      verify(userDao).updateUser(dbUser.getName(), dbUser.getEmail(), new HashedPassword(bcrypt(PASSWORD)));
    }

    final void thenUserShouldBeUpdatedWithMd5CryptedPassword() {
      verify(userDao).updateUser(dbUser.getName(), dbUser.getEmail(), new HashedPassword(md5Crypt(PASSWORD)));
    }

    final void thenUserShouldNotBeCreated() {
      verify(userDao, never()).createUser(any(), any(), any());
    }

    final void thenUserShouldNotBeUpdatedWithBcryptedPassword() {
      verify(userDao, never())
          .updateUser(eq(dbUser.getName()), eq(dbUser.getEmail()), argThat(HashedPassword::isBcrypted));
    }

    final void thenUserShouldNotBeUpdatedWithMd5CryptedPassword() {
      verify(userDao, never())
          .updateUser(eq(dbUser.getName()), eq(dbUser.getEmail()), argThat(HashedPassword::isMd5Crypted));
    }

    final void thenUserShouldNotBeUpdated() {
      verify(userDao, never()).updateUser(any(), any(), any());
    }
  }

  abstract class AbstractNoBansTestCase extends AbstractTestCase {
    @BeforeEach
    public void givenNoBans() {
      givenNoMacIsBanned();
      givenNoUsernameIsBanned();
      when(databaseDao.getBadWordDao()).thenReturn(badWordDao);
    }

    private void givenNoMacIsBanned() {
      when(databaseDao.getBannedMacDao()).thenReturn(bannedMacDao);
      when(bannedMacDao.isMacBanned(any(), anyString())).thenReturn(Optional.empty());
    }

    private void givenNoUsernameIsBanned() {
      when(databaseDao.getUsernameBlacklistDao()).thenReturn(bannedUsernameDao);
      when(bannedUsernameDao.isUsernameBanned(anyString())).thenReturn(false);
    }
  }

  @Nested
  final class DatabaseInteractionTest {
    @ExtendWith(MockitoExtension.class)
    @Nested
    final class WhenUserIsAnonymousTest extends AbstractNoBansTestCase {
      @Test
      void shouldNotCreateOrUpdateUserWhenAuthenticationSucceeds() {
        givenAnonymousAuthenticationWillSucceed();
        when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenUserShouldNotBeCreated();
        thenUserShouldNotBeUpdated();
      }

      @Test
      void shouldNotCreateOrUpdateUserWhenAuthenticationFails() {
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
    final class WhenUserDoesNotExistTest {
      @ExtendWith(MockitoExtension.class)
      @Nested
      final class WhenUsingLegacyClientTest extends AbstractNoBansTestCase {
        @Test
        void shouldCreateNewUserWithOnlyMd5CryptedPassword() {
          givenUserDoesNotExist();
          when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

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
      final class WhenUsingCurrentClientTest extends AbstractNoBansTestCase {
        @Test
        void shouldCreateNewUserWithBothPasswords() {
          givenUserDoesNotExist();
          when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

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
    final class WhenUserExistsTest {
      @ExtendWith(MockitoExtension.class)
      @Nested
      final class WhenUsingLegacyClientTest extends AbstractNoBansTestCase {
        @Test
        void shouldNotUpdatePasswordsWhenUserHasOnlyMd5CryptedPassword() {
          givenUserDoesNotHaveBcryptedPassword();
          givenAuthenticationWillUseMd5CryptedPasswordAndSucceed();
          when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

          whenAuthenticating(givenAuthenticationResponse());

          thenAuthenticationShouldSucceed();
          thenUserShouldNotBeCreated();
          thenUserShouldNotBeUpdated();
        }

        @Test
        void shouldNotUpdatePasswordsWhenUserHasBothPasswords() {
          givenUserHasBcryptedPassword();
          givenAuthenticationWillUseMd5CryptedPasswordAndSucceed();
          when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

          whenAuthenticating(givenAuthenticationResponse());

          thenAuthenticationShouldSucceed();
          thenUserShouldNotBeCreated();
          thenUserShouldNotBeUpdated();
        }

        @Test
        void shouldNotUpdatePasswordsWhenUserHasOnlyBcryptedPassword() {
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
      final class WhenUsingCurrentClientTest extends AbstractNoBansTestCase {
        @Test
        void shouldNotUpdatePasswordsWhenUserHasBothPasswords() {
          givenUserHasMd5CryptedPassword();
          givenUserHasBcryptedPassword();
          givenAuthenticationWillUseObfuscatedPasswordAndSucceed();
          when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

          whenAuthenticating(givenAuthenticationResponse());

          thenAuthenticationShouldSucceed();
          thenUserShouldNotBeCreated();
          thenUserShouldNotBeUpdated();
        }

        @Test
        void shouldUpdateBcryptedPasswordWhenUserHasOnlyMd5CryptedPassword() {
          givenUserExists();
          givenUserHasMd5CryptedPassword();
          givenUserDoesNotHaveBcryptedPassword();
          givenAuthenticationWillUseMd5CryptedPasswordAndSucceed();
          when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

          whenAuthenticating(givenAuthenticationResponse());

          thenAuthenticationShouldSucceed();
          thenUserShouldNotBeCreated();
          thenUserShouldNotBeUpdatedWithMd5CryptedPassword();
          thenUserShouldBeUpdatedWithBcryptedPassword();
        }

        @Test
        void shouldUpdateBothPasswordsWhenUserHasOnlyBcryptedPassword() {
          givenUserExists();
          givenUserDoesNotHaveMd5CryptedPassword();
          givenUserHasBcryptedPassword();
          givenAuthenticationWillUseObfuscatedPasswordAndSucceed();
          when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

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
  final class AccessLogTest {
    @ExtendWith(MockitoExtension.class)
    @Nested
    final class WhenUserIsAnonymous extends AbstractNoBansTestCase {
      @Test
      void shouldLogSuccessfulAuthenticationWhenAuthenticationSucceeds() throws Exception {
        givenAnonymousAuthenticationWillSucceed();
        when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenAccessLogShouldReceiveSuccessfulAuthentication(UserType.ANONYMOUS);
      }

      @Test
      void shouldLogFailedAuthenticationWhenAuthenticationFails() {
        givenAnonymousAuthenticationWillFail();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFail();
      }

      private ResponseGenerator givenAuthenticationResponse() {
        return challenge -> ImmutableMap.of(
            LobbyLoginResponseKeys.ANONYMOUS_LOGIN, Boolean.TRUE.toString(),
            LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString());
      }
    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    final class WhenUserIsRegistered extends AbstractNoBansTestCase {
      @Test
      void shouldLogSuccessfulAuthenticationWhenAuthenticationSucceeds() throws Exception {
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndSucceed();
        when(databaseDao.getAccessLogDao()).thenReturn(accessLog);

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenAccessLogShouldReceiveSuccessfulAuthentication(UserType.REGISTERED);
      }

      @Test
      void shouldLogFailedAuthenticationWhenAuthenticationFails() {
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndFail();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldFail();
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
