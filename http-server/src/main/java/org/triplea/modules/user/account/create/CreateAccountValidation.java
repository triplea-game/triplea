package org.triplea.modules.user.account.create;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.lobby.login.CreateAccountRequest;
import org.triplea.modules.user.account.NameIsAvailableValidation;
import org.triplea.modules.user.account.NameValidation;

/**
 * Verifies that a new user account request is good-to-create. General validation rules are:
 *
 * <ul>
 *   <li>username is not already in database
 *   <li>username is not banned and does not contain a word from 'bad words list'
 *   <li>password is a min length
 *   <li>email looks valid, contains an '@' sign
 * </ul>
 */
@Builder
class CreateAccountValidation implements Function<CreateAccountRequest, Optional<String>> {

  @Nonnull private final Function<String, Optional<String>> nameValidator;
  @Nonnull private final Function<String, Optional<String>> nameIsAvailableValidator;
  @Nonnull private final Function<String, Optional<String>> emailValidator;
  @Nonnull private final Function<String, Optional<String>> passwordValidator;

  public static CreateAccountValidation build(final Jdbi jdbi) {
    return CreateAccountValidation.builder()
        .nameValidator(NameValidation.build(jdbi))
        .emailValidator(new EmailValidation())
        .passwordValidator(new PasswordValidation())
        .nameIsAvailableValidator(NameIsAvailableValidation.build(jdbi))
        .build();
  }

  @Override
  public Optional<String> apply(final CreateAccountRequest createAccountRequest) {
    return nameValidator
        .apply(createAccountRequest.getUsername())
        .or(() -> nameIsAvailableValidator.apply(createAccountRequest.getUsername()))
        .or(() -> emailValidator.apply(createAccountRequest.getEmail()))
        .or(() -> passwordValidator.apply(createAccountRequest.getPassword()));
  }
}
