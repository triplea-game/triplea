package org.triplea.modules.user.account.update;

import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.db.dao.UserJdbiDao;

@Builder
class UpdateAccountService {

  @Nonnull private final UserJdbiDao userJdbiDao;

  @Nonnull private final Function<String, String> passwordEncrpter;

  void changePassword(final int userId, final String newPassword) {
    final int updateCount = userJdbiDao.updatePassword(userId, passwordEncrpter.apply(newPassword));
    assert updateCount == 1;
  }

  String fetchEmail(final int userId) {
    final String email = userJdbiDao.fetchEmail(userId);
    assert email != null;
    return email;
  }

  void changeEmail(final int userId, final String newEmail) {
    final int updateCount = userJdbiDao.updateEmail(userId, newEmail);
    assert updateCount == 1;
  }
}
