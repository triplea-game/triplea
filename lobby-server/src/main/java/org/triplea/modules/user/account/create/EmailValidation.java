package org.triplea.modules.user.account.create;

import com.google.common.base.Strings;
import java.util.Optional;
import java.util.function.Function;
import org.triplea.domain.data.PlayerEmailValidation;

public class EmailValidation implements Function<String, Optional<String>> {
  @Override
  public Optional<String> apply(final String email) {
    return !Strings.nullToEmpty(email).isEmpty() && PlayerEmailValidation.isValid(email)
        ? Optional.empty()
        : Optional.of("Invalid email address");
  }
}
