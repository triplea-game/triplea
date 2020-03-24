package org.triplea.modules.user.account.create;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.http.client.lobby.login.CreateAccountRequest;
import org.triplea.http.client.lobby.login.CreateAccountResponse;

/**
 * Imperative shell for creating a user account. Validates a request, if valid, creates a new user
 * account in database.
 */
@Builder
class CreateAccountModule implements Function<CreateAccountRequest, CreateAccountResponse> {

  @Nonnull private final Function<CreateAccountRequest, Optional<String>> createAccountValidation;
  @Nonnull private final Function<CreateAccountRequest, CreateAccountResponse> accountCreator;

  @Override
  public CreateAccountResponse apply(final CreateAccountRequest createAccountRequest) {
    // create account if request is valid
    return createAccountValidation
        .apply(createAccountRequest)
        .map(CreateAccountModule::createError)
        .orElseGet(() -> accountCreator.apply(createAccountRequest));
  }

  private static CreateAccountResponse createError(final String errorMessage) {
    return CreateAccountResponse.builder().errorMessage(errorMessage).build();
  }
}
