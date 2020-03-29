package org.triplea.modules.user.account.login.authorizer.anonymous;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.db.dao.UserJdbiDao;
import org.triplea.domain.data.UserName;
import org.triplea.modules.chat.Chatters;

@Builder
class AnonymousLogin implements Function<UserName, Optional<String>> {
  @NonNull private final Chatters chatters;
  @Nonnull private final UserJdbiDao userJdbiDao;

  // TODO: Project#12 bad-words check
  // TODO: Project#12 banned username check

  @Override
  public Optional<String> apply(final UserName userName) {
    Preconditions.checkNotNull(userName);
    return (!chatters.hasPlayer(userName)
            && userJdbiDao.lookupUserIdByName(userName.getValue()).isEmpty())
        ? Optional.empty()
        : Optional.of("Name is already in use, please choose another");
  }
}
