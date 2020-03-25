package org.triplea.modules.forgot.password;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.TempPasswordHistoryDao;
import org.triplea.http.AppConfig;
import org.triplea.http.client.forgot.password.ForgotPasswordRequest;

/** Constructs a {@code ForgotPasswordModule} with dependencies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ForgotPasswordModuleFactory {

  public static BiFunction<String, ForgotPasswordRequest, String> buildForgotPasswordModule(
      final AppConfig appConfig, final Jdbi jdbi) {
    return buildForgotPasswordModule(jdbi, new PasswordEmailSender(appConfig));
  }

  @VisibleForTesting
  static BiFunction<String, ForgotPasswordRequest, String> buildForgotPasswordModule(
      final Jdbi jdbi, final BiConsumer<String, String> passwordEmailSender) {
    return ForgotPasswordModule.builder()
        .passwordEmailSender(passwordEmailSender)
        .passwordGenerator(new PasswordGenerator())
        .tempPasswordPersistence(TempPasswordPersistence.newInstance(jdbi))
        .tempPasswordHistory(new TempPasswordHistory(jdbi.onDemand(TempPasswordHistoryDao.class)))
        .build();
  }
}
