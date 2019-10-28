package org.triplea.server.user.account.login.authorizer.registered;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.login.LoginRequest;

@ExtendWith(MockitoExtension.class)
class RegisteredLoginTest {

  private static final LoginRequest LOGIN_REQUEST =
      LoginRequest.builder().name("name").password("password").build();

  @Mock private BiPredicate<PlayerName, String> passwordCheck;
  @Mock private BiPredicate<PlayerName, String> legacyPasswordCheck;
  @Mock private BiConsumer<PlayerName, String> legacyPasswordUpdater;

  private RegisteredLogin registeredLogin;

  @BeforeEach
  void setup() {
    registeredLogin =
        RegisteredLogin.builder()
            .passwordCheck(passwordCheck)
            .legacyPasswordCheck(legacyPasswordCheck)
            .legacyPasswordUpdater(legacyPasswordUpdater)
            .build();
  }

  @Test
  void noPasswordMatch() {
    final boolean result = registeredLogin.test(LOGIN_REQUEST);

    assertThat(result, is(false));
    verify(legacyPasswordUpdater, never()).accept(any(), any());
  }

  @Test
  void legacyPasswordMatches() {
    when(legacyPasswordCheck.test(
            PlayerName.of(LOGIN_REQUEST.getName()), LOGIN_REQUEST.getPassword()))
        .thenReturn(true);

    final boolean result = registeredLogin.test(LOGIN_REQUEST);

    assertThat(result, is(true));
    verify(legacyPasswordUpdater)
        .accept(PlayerName.of(LOGIN_REQUEST.getName()), LOGIN_REQUEST.getPassword());
  }

  @Test
  void passwordMatches() {
    when(passwordCheck.test(PlayerName.of(LOGIN_REQUEST.getName()), LOGIN_REQUEST.getPassword()))
        .thenReturn(true);

    final boolean result = registeredLogin.test(LOGIN_REQUEST);

    assertThat(result, is(true));
    verify(legacyPasswordUpdater, never()).accept(any(), any());
  }
}
