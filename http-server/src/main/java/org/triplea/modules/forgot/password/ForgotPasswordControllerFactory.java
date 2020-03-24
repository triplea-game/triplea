package org.triplea.modules.forgot.password;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.AppConfig;

/** Creates a {@code ForgotPasswordController} with dependencies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ForgotPasswordControllerFactory {
  /** Creates a {@code ForgotPasswordController} with dependencies. */
  public static ForgotPasswordController buildController(
      final AppConfig configuration, final Jdbi jdbi) {

    return ForgotPasswordController.builder()
        .forgotPasswordModule(
            ForgotPasswordModuleFactory.buildForgotPasswordModule(configuration, jdbi))
        .build();
  }
}
