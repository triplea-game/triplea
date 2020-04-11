package org.triplea.modules.user.account.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.SystemId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.login.LoginRequest;

@ExtendWith(MockitoExtension.class)
class LoginModuleTest {

  private static final SystemId SYSTEM_ID = SystemId.of("system-id");
  private static final String IP = "ip";

  private static final LoginRequest LOGIN_REQUEST =
      LoginRequest.builder().name("name").password("password").build();

  private static final LoginRequest ANONYMOUS_LOGIN_REQUEST =
      LoginRequest.builder().name("name").build();

  private static final ApiKey API_KEY = ApiKey.of("api-key");

  @Mock private Predicate<LoginRequest> registeredLogin;
  @Mock private Predicate<LoginRequest> tempPasswordLogin;
  @Mock private Function<UserName, Optional<String>> anonymousLogin;
  @Mock private Function<LoginRecord, ApiKey> apiKeyGenerator;
  @Mock private Consumer<LoginRecord> accessLogUpdater;
  @Mock private UserJdbiDao userJdbiDao;
  @Mock private Function<String, Optional<String>> nameValidation;

  private LoginModule loginModule;

  @BeforeEach
  void setup() {
    loginModule =
        LoginModule.builder()
            .registeredLogin(registeredLogin)
            .tempPasswordLogin(tempPasswordLogin)
            .anonymousLogin(anonymousLogin)
            .apiKeyGenerator(apiKeyGenerator)
            .accessLogUpdater(accessLogUpdater)
            .userJdbiDao(userJdbiDao)
            .nameValidation(nameValidation)
            .build();
  }

  @SuppressWarnings("unused")
  static List<Arguments> rejectLoginOnBadArgs() {
    return List.of(
        Arguments.of(LoginRequest.builder().password("no-name").build(), "system-id-string", IP),
        Arguments.of(LOGIN_REQUEST, null, IP));
  }

  @ParameterizedTest
  @MethodSource
  void rejectLoginOnBadArgs(
      final LoginRequest loginRequest, final String systemIdString, final String ip) {
    givenNameValidationIsOkay();

    final LobbyLoginResponse result = loginModule.doLogin(loginRequest, systemIdString, ip);

    assertFailedLogin(result);
  }

  private void assertFailedLogin(final LobbyLoginResponse result) {
    assertThat(result.getFailReason(), notNullValue());
    assertThat(result.getApiKey(), nullValue());
    verify(userJdbiDao, never()).lookupUserRoleByUserName(any());
    verify(apiKeyGenerator, never()).apply(any());
    verify(accessLogUpdater, never()).accept(any());
  }

  private static void assertSuccessLogin(final LobbyLoginResponse result) {
    assertThat(result.getFailReason(), nullValue());
    assertThat(result.getApiKey(), is(API_KEY.getValue()));
  }

  @Nested
  class NameValidation {
    @Test
    void nameIsValidatedOnLogin() {
      when(nameValidation.apply(ANONYMOUS_LOGIN_REQUEST.getName()))
          .thenReturn(Optional.of("bad-name!"));

      final LobbyLoginResponse result =
          loginModule.doLogin(ANONYMOUS_LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertFailedLogin(result);
      assertThat(result.getFailReason(), is("bad-name!"));
    }
  }

  @Nested
  class AnonymousLogin {
    @Test
    void loginRejected() {
      givenNameValidationIsOkay();
      when(anonymousLogin.apply(UserName.of(ANONYMOUS_LOGIN_REQUEST.getName())))
          .thenReturn(Optional.of("error"));

      final LobbyLoginResponse result =
          loginModule.doLogin(ANONYMOUS_LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertFailedLogin(result);
      verify(registeredLogin, never()).test(any());
      verify(tempPasswordLogin, never()).test(any());
      verify(accessLogUpdater, never()).accept(any());
    }

    @Test
    void loginSuccess() {
      givenNameValidationIsOkay();
      when(anonymousLogin.apply(UserName.of(ANONYMOUS_LOGIN_REQUEST.getName())))
          .thenReturn(Optional.empty());
      when(apiKeyGenerator.apply(any())).thenReturn(API_KEY);

      final LobbyLoginResponse result =
          loginModule.doLogin(ANONYMOUS_LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertSuccessLogin(result);
      assertThat(result.isPasswordChangeRequired(), is(false));
      verify(registeredLogin, never()).test(any());
      verify(tempPasswordLogin, never()).test(any());
      verify(userJdbiDao, never()).lookupUserRoleByUserName(any());
      final ArgumentCaptor<LoginRecord> loginRecordArgumentCaptor =
          ArgumentCaptor.forClass(LoginRecord.class);
      verify(accessLogUpdater).accept(loginRecordArgumentCaptor.capture());
      assertThat(loginRecordArgumentCaptor.getValue().isRegistered(), is(false));
    }
  }

  private void givenNameValidationIsOkay() {
    when(nameValidation.apply(any())).thenReturn(Optional.empty());
  }

  @Nested
  class RegisteredLogin {
    @Test
    void loginRejected() {
      givenNameValidationIsOkay();
      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertFailedLogin(result);

      verify(anonymousLogin, never()).apply(any());
      verify(accessLogUpdater, never()).accept(any());
    }

    @Test
    void loginSuccess() {
      givenNameValidationIsOkay();
      when(registeredLogin.test(LOGIN_REQUEST)).thenReturn(true);
      when(userJdbiDao.lookupUserRoleByUserName(LOGIN_REQUEST.getName()))
          .thenReturn(Optional.of(UserRole.PLAYER));
      when(apiKeyGenerator.apply(any())).thenReturn(API_KEY);

      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertSuccessLogin(result);
      assertThat(result.isPasswordChangeRequired(), is(false));
      verify(anonymousLogin, never()).apply(any());
      final ArgumentCaptor<LoginRecord> loginRecordArgumentCaptor =
          ArgumentCaptor.forClass(LoginRecord.class);
      verify(accessLogUpdater).accept(loginRecordArgumentCaptor.capture());
      assertThat(loginRecordArgumentCaptor.getValue().isRegistered(), is(true));
    }
  }

  @Nested
  class TempPasswordLogin {
    @Test
    void loginSuccess() {
      givenNameValidationIsOkay();
      when(tempPasswordLogin.test(LOGIN_REQUEST)).thenReturn(true);
      when(userJdbiDao.lookupUserRoleByUserName(LOGIN_REQUEST.getName()))
          .thenReturn(Optional.of(UserRole.PLAYER));
      when(apiKeyGenerator.apply(any())).thenReturn(API_KEY);

      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertSuccessLogin(result);
      assertThat(result.isPasswordChangeRequired(), is(true));
      verify(anonymousLogin, never()).apply(any());
      verify(accessLogUpdater).accept(any());
    }
  }

  /** Verify lobby login result 'isModerator' flag has expected values. */
  @Nested
  class ModeratorUser {
    @ParameterizedTest
    @ValueSource(strings = {UserRole.MODERATOR, UserRole.ADMIN})
    void loginSuccessWithModerator(final String moderatorUserRole) {
      givenNameValidationIsOkay();
      givenLoginWithUserRole(moderatorUserRole);

      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertThat(result.isModerator(), is(true));
    }

    private void givenLoginWithUserRole(final String userRole) {
      when(registeredLogin.test(LOGIN_REQUEST)).thenReturn(true);
      when(userJdbiDao.lookupUserRoleByUserName(LOGIN_REQUEST.getName()))
          .thenReturn(Optional.of(userRole));
      when(apiKeyGenerator.apply(any())).thenReturn(API_KEY);
    }

    @ParameterizedTest
    @ValueSource(strings = {UserRole.ANONYMOUS, UserRole.PLAYER})
    void loginSuccessWithNonModerator(final String nonModeratorUserRole) {
      givenNameValidationIsOkay();
      givenLoginWithUserRole(nonModeratorUserRole);

      final LobbyLoginResponse result =
          loginModule.doLogin(LOGIN_REQUEST, SYSTEM_ID.getValue(), IP);

      assertThat(result.isModerator(), is(false));
    }
  }
}
