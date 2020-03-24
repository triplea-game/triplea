package org.triplea.modules.user.account.login.authorizer.registered;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.function.BiPredicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.domain.data.UserName;

@Builder
public class PasswordCheck implements BiPredicate<UserName, String> {

  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final BiPredicate<String, String> passwordVerifier;

  @Override
  public boolean test(final UserName userName, final String password) {
    Preconditions.checkNotNull(Strings.emptyToNull(password));
    return userJdbiDao
        .getPassword(userName.getValue())
        .map(dbPassword -> passwordVerifier.test(password, dbPassword))
        .orElse(false);
  }
}
