package org.triplea.modules.user.account.login.authorizer.anonymous;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.UserName;
import org.triplea.modules.chat.Chatters;

@ExtendWith(MockitoExtension.class)
class AnonymousLoginTest {
  private static final UserName PLAYER_NAME = UserName.of("Player");

  @Mock private Function<String, Optional<String>> nameIsAvailableValidator;
  @Mock private Chatters chatters;

  private AnonymousLogin anonymousLogin;

  @BeforeEach
  void setUp() {
    anonymousLogin =
        AnonymousLogin.builder()
            .nameIsAvailableValidation(nameIsAvailableValidator)
            .chatters(chatters)
            .build();
  }

  @Test
  void nameIsInUse() {
    when(chatters.isPlayerConnected(PLAYER_NAME)).thenReturn(true);

    final Optional<String> result = anonymousLogin.apply(PLAYER_NAME);

    assertThat(result, isPresent());
  }

  @Test
  void nameIsRegistered() {
    when(nameIsAvailableValidator.apply(PLAYER_NAME.getValue())).thenReturn(Optional.of("present"));

    final Optional<String> result = anonymousLogin.apply(PLAYER_NAME);

    assertThat(result, isPresent());
  }

  @Test
  void allowLogin() {
    when(chatters.isPlayerConnected(PLAYER_NAME)).thenReturn(false);
    when(nameIsAvailableValidator.apply(PLAYER_NAME.getValue())).thenReturn(Optional.empty());

    final Optional<String> result = anonymousLogin.apply(PLAYER_NAME);

    assertThat(result, isEmpty());
  }
}
