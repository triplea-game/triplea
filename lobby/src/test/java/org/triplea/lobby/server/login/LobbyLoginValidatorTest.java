package org.triplea.lobby.server.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.triplea.lobby.common.LobbyConstants;
import org.triplea.lobby.common.login.LobbyLoginResponseKeys;
import org.triplea.lobby.common.login.RsaAuthenticator;
import org.triplea.lobby.server.TestUserUtils;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.db.BadWordDao;
import org.triplea.lobby.server.db.BannedMacDao;
import org.triplea.lobby.server.db.BannedUsernameDao;
import org.triplea.lobby.server.db.HashedPassword;
import org.triplea.lobby.server.db.UserDao;
import org.triplea.test.common.security.TestSecurityUtils;
import org.triplea.util.Tuple;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.lobby.server.userDB.DBUser;

final class LobbyLoginValidatorTest {

  interface ResponseGenerator extends Function<Map<String, String>, Map<String, String>> {
  }

  abstract class AbstractTestCase {
    static final String EMAIL = "n@n.com";
    static final String PASSWORD = "password";

    @Mock
    BannedMacDao bannedMacDao;

    @Mock
    BannedUsernameDao bannedUsernameDao;

    @Mock
    private AccessLog accessLog;

    @Mock
    private BadWordDao badWordDao;

    @Mock
    private UserDao userDao;

    private LobbyLoginValidator lobbyLoginValidator;

    private String authenticationErrorMessage;

    private final String bcryptSalt = BCrypt.gensalt();

    private final User user = TestUserUtils.newUser();

    @BeforeEach
    void createLobbyLoginValidator() throws Exception {
      lobbyLoginValidator = new LobbyLoginValidator(
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

    final void givenAnonymousAuthenticationWillFail() {
      when(userDao.doesUserExist(user.getUsername())).thenReturn(true);
    }

    final void givenAnonymousAuthenticationWillSucceed() {
      when(userDao.doesUserExist(user.getUsername())).thenReturn(false);
    }

    final void givenAuthenticationWillUseObfuscatedPasswordAndSucceed() {
      when(userDao.login(user.getUsername(), new HashedPassword(obfuscate(PASSWORD)))).thenReturn(true);
    }

    final void givenAuthenticationWillUseObfuscatedPasswordAndFail() {
      when(userDao.login(user.getUsername(), new HashedPassword(obfuscate(PASSWORD)))).thenReturn(false);
    }

    final void givenUserDoesNotExist() {
      when(userDao.doesUserExist(user.getUsername())).thenReturn(false);
    }

    final void givenUserHasBcryptedPassword() {
      when(userDao.getPassword(user.getUsername())).thenReturn(new HashedPassword(bcrypt(PASSWORD)));
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

    final void thenUserShouldNotBeCreated() {
      verify(userDao, never()).createUser(any(DBUser.class), any(HashedPassword.class));
    }

    final void thenUserShouldNotBeUpdated() {
      verify(userDao, never()).updateUser(any(DBUser.class), any(HashedPassword.class));
    }
  }

  abstract class AbstractNoBansTestCase extends AbstractTestCase {
    @BeforeEach
    void givenNoBans() {
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
  final class DatabaseInteractionTest {
    @ExtendWith(MockitoExtension.class)
    @Nested
    final class WhenUserIsAnonymousTest extends AbstractNoBansTestCase {
      @Test
      void shouldNotCreateOrUpdateUserWhenAuthenticationSucceeds() {
        givenAnonymousAuthenticationWillSucceed();

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
      final class WhenUsingCurrentClientTest extends AbstractNoBansTestCase {
        @Test
        void shouldCreateNewUser() {
          givenUserDoesNotExist();

          whenAuthenticating(givenAuthenticationResponse());

          thenAuthenticationShouldSucceed();
        }

        private ResponseGenerator givenAuthenticationResponse() {
          return challenge -> ImmutableMap.<String, String>builder()
              .put(LobbyLoginResponseKeys.EMAIL, EMAIL)
              .put(LobbyLoginResponseKeys.LOBBY_VERSION, LobbyConstants.LOBBY_VERSION.toString())
              .put(LobbyLoginResponseKeys.REGISTER_NEW_USER, Boolean.TRUE.toString())
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
      void shouldLogSuccessfulAuthenticationWhenAuthenticationSucceeds() {
        givenAnonymousAuthenticationWillSucceed();

        whenAuthenticating(givenAuthenticationResponse());

        thenAuthenticationShouldSucceed();
        thenAccessLogShouldReceiveSuccessfulAuthentication(UserType.ANONYMOUS);
      }

      @Test
      void shouldLogFailedAuthenticationWhenAuthenticationFails() {
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
    final class WhenUserIsRegistered extends AbstractNoBansTestCase {
      @Test
      void shouldLogSuccessfulAuthenticationWhenAuthenticationSucceeds() {
        givenUserHasBcryptedPassword();
        givenAuthenticationWillUseObfuscatedPasswordAndSucceed();

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
