package org.triplea.lobby.server.login.forgot.password.create;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.triplea.lobby.server.db.JdbiDatabase;
import org.triplea.lobby.server.db.dao.TempPasswordDao;

/** Constructs a {@code ForgotPasswordModule} with dependencies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ForgotPasswordModuleFactory {

  public static Predicate<String> buildForgotPasswordModule() {
    return buildForgotPasswordModule(new PasswordEmailSender());
  }

  @VisibleForTesting
  static Predicate<String> buildForgotPasswordModule(
      final BiConsumer<String, String> passwordEmailSender) {
    return ForgotPasswordModule.builder()
        .emailLookup(JdbiDatabase.newConnection().onDemand(TempPasswordDao.class)::lookupUserEmail)
        .passwordEmailSender(passwordEmailSender)
        .passwordGenerator(new PasswordGenerator())
        .tempPasswordPersistence(TempPasswordPersistence.newInstance())
        .build();
  }
}
