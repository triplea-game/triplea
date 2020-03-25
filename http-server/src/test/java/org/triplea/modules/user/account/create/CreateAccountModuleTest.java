package org.triplea.modules.user.account.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.login.CreateAccountRequest;
import org.triplea.http.client.lobby.login.CreateAccountResponse;

@ExtendWith(MockitoExtension.class)
class CreateAccountModuleTest {
  private static final CreateAccountRequest CREATE_ACCOUNT_REQUEST =
      CreateAccountRequest.builder().username("username").email("email").password("pass").build();
  private static final String ERROR_MESSAGE = "example error message";

  @Mock private Function<CreateAccountRequest, Optional<String>> createAccountValidation;
  @Mock private Function<CreateAccountRequest, CreateAccountResponse> accountCreator;

  private CreateAccountModule createAccountModule;

  @BeforeEach
  void setup() {
    createAccountModule =
        CreateAccountModule.builder()
            .createAccountValidation(createAccountValidation)
            .accountCreator(accountCreator)
            .build();
  }

  @Test
  void invalidRequest() {
    when(createAccountValidation.apply(CREATE_ACCOUNT_REQUEST))
        .thenReturn(Optional.of(ERROR_MESSAGE));

    final CreateAccountResponse result = createAccountModule.apply(CREATE_ACCOUNT_REQUEST);

    assertThat(result.getErrorMessage(), notNullValue());
    assertThat(result.getErrorMessage(), not(is(CreateAccountResponse.SUCCESS_RESPONSE)));
    verify(accountCreator, never()).apply(any());
  }

  @Test
  void validRequest() {
    when(createAccountValidation.apply(CREATE_ACCOUNT_REQUEST)).thenReturn(Optional.empty());
    when(accountCreator.apply(CREATE_ACCOUNT_REQUEST))
        .thenReturn(CreateAccountResponse.SUCCESS_RESPONSE);

    final CreateAccountResponse result = createAccountModule.apply(CREATE_ACCOUNT_REQUEST);

    assertThat(result, is(CreateAccountResponse.SUCCESS_RESPONSE));
  }
}
