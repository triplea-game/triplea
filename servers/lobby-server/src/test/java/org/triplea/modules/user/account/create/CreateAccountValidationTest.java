package org.triplea.modules.user.account.create;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.login.CreateAccountRequest;

@ExtendWith(MockitoExtension.class)
class CreateAccountValidationTest {
  private static final String ERROR_MESSAGE = "error-message";
  private static final CreateAccountRequest CREATE_ACCOUNT_REQUEST =
      CreateAccountRequest.builder()
          .username("user")
          .email("email@email.com")
          .password("password")
          .build();

  @Mock private Function<String, Optional<String>> nameIsAvailableValidator;
  @Mock private Function<String, Optional<String>> nameValidator;
  @Mock private Function<String, Optional<String>> emailValidator;
  @Mock private Function<String, Optional<String>> passwordValidator;

  private CreateAccountValidation createAccountValidation;

  @BeforeEach
  void setUp() {
    createAccountValidation =
        new CreateAccountValidation(
            nameValidator, nameIsAvailableValidator, emailValidator, passwordValidator);
  }

  @Test
  void allValid() {
    when(nameValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername())).thenReturn(Optional.empty());
    when(nameIsAvailableValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername()))
        .thenReturn(Optional.empty());
    when(emailValidator.apply(CREATE_ACCOUNT_REQUEST.getEmail())).thenReturn(Optional.empty());
    when(passwordValidator.apply(CREATE_ACCOUNT_REQUEST.getPassword()))
        .thenReturn(Optional.empty());

    final Optional<String> result = createAccountValidation.apply(CREATE_ACCOUNT_REQUEST);

    assertThat(result, isEmpty());
  }

  @Test
  void invalidName() {
    when(nameValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername()))
        .thenReturn(Optional.of(ERROR_MESSAGE));

    final Optional<String> result = createAccountValidation.apply(CREATE_ACCOUNT_REQUEST);

    assertThat(result, isPresentAndIs(ERROR_MESSAGE));
  }

  @Test
  void nameIsTaken() {
    when(nameValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername())).thenReturn(Optional.empty());

    when(nameIsAvailableValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername()))
        .thenReturn(Optional.of(ERROR_MESSAGE));

    final Optional<String> result = createAccountValidation.apply(CREATE_ACCOUNT_REQUEST);

    assertThat(result, isPresentAndIs(ERROR_MESSAGE));
  }

  @Test
  void invalidEmail() {
    when(nameValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername())).thenReturn(Optional.empty());
    when(nameIsAvailableValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername()))
        .thenReturn(Optional.empty());
    when(emailValidator.apply(CREATE_ACCOUNT_REQUEST.getEmail()))
        .thenReturn(Optional.of(ERROR_MESSAGE));

    final Optional<String> result = createAccountValidation.apply(CREATE_ACCOUNT_REQUEST);

    assertThat(result, isPresentAndIs(ERROR_MESSAGE));
  }

  @Test
  void invalidPassword() {
    when(nameValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername())).thenReturn(Optional.empty());
    when(nameIsAvailableValidator.apply(CREATE_ACCOUNT_REQUEST.getUsername()))
        .thenReturn(Optional.empty());
    when(emailValidator.apply(CREATE_ACCOUNT_REQUEST.getEmail())).thenReturn(Optional.empty());
    when(passwordValidator.apply(CREATE_ACCOUNT_REQUEST.getPassword()))
        .thenReturn(Optional.of(ERROR_MESSAGE));

    final Optional<String> result = createAccountValidation.apply(CREATE_ACCOUNT_REQUEST);

    assertThat(result, isPresentAndIs(ERROR_MESSAGE));
  }
}
