package org.triplea.modules.user.account.login.authorizer.legacy;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.domain.data.UserName;
import org.triplea.java.Postconditions;
import org.triplea.modules.user.account.PasswordBCrypter;

// TODO: Md5-Deprecation - This class can be removed when MD5 is removed
@Builder
public class LegacyPasswordUpdate implements BiConsumer<UserName, String> {

  @Nonnull private final UserJdbiDao userJdbiDao;
  @Nonnull private final Function<String, String> passwordBcrypter;

  public static LegacyPasswordUpdate build(final Jdbi jdbi) {
    return LegacyPasswordUpdate.builder()
        .passwordBcrypter(new PasswordBCrypter())
        .userJdbiDao(jdbi.onDemand(UserJdbiDao.class))
        .build();
  }

  @Override
  public void accept(final UserName userName, final String password) {
    Preconditions.checkNotNull(Strings.emptyToNull(password));
    Preconditions.checkNotNull(userName);

    final int id =
        userJdbiDao
            .lookupUserIdByName(userName.getValue())
            .orElseThrow(() -> new IllegalArgumentException("No user id found for: " + userName));
    Postconditions.assertState(id > 0);

    final int updateCount = userJdbiDao.updatePassword(id, passwordBcrypter.apply(password));
    Postconditions.assertState(updateCount == 1, "Password update failed: " + userName);
  }
}
