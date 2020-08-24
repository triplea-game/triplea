package org.triplea.modules.user.account.login.authorizer.anonymous;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.NonNull;
import org.jdbi.v3.core.Jdbi;
import org.triplea.domain.data.UserName;
import org.triplea.modules.chat.Chatters;
import org.triplea.modules.user.account.NameIsAvailableValidation;

@Builder
public class AnonymousLogin implements Function<UserName, Optional<String>> {
  @NonNull private final Chatters chatters;
  @Nonnull private final Function<String, Optional<String>> nameIsAvailableValidation;

  public static Function<UserName, Optional<String>> build(
      final Jdbi jdbi, final Chatters chatters) {
    return AnonymousLogin.builder()
        .chatters(chatters)
        .nameIsAvailableValidation(NameIsAvailableValidation.build(jdbi))
        .build();
  }

  @Override
  public Optional<String> apply(final UserName userName) {
    Preconditions.checkNotNull(userName);
    return (!chatters.isPlayerConnected(userName)
            && nameIsAvailableValidation.apply(userName.getValue()).isEmpty())
        ? Optional.empty()
        : Optional.of("Name is already in use, please choose another");
  }
}
