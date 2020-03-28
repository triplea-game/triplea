package org.triplea.modules.user.account.login.authorizer.temp.password;

import com.google.common.base.Preconditions;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.TempPasswordDao;
import org.triplea.http.client.lobby.login.LoginRequest;
import org.triplea.modules.user.account.login.authorizer.BCryptHashVerifier;

@Builder
public class TempPasswordLogin implements Predicate<LoginRequest> {

  @Nonnull private final TempPasswordDao tempPasswordDao;
  @Nonnull private final BiPredicate<String, String> passwordChecker;

  public static TempPasswordLogin build(final Jdbi jdbi) {
    return TempPasswordLogin.builder()
        .tempPasswordDao(jdbi.onDemand(TempPasswordDao.class))
        .passwordChecker(new BCryptHashVerifier())
        .build();
  }

  @Override
  public boolean test(final LoginRequest loginRequest) {
    Preconditions.checkNotNull(loginRequest);
    Preconditions.checkNotNull(loginRequest.getName());
    Preconditions.checkNotNull(loginRequest.getPassword());
    return tempPasswordDao
        .fetchTempPassword(loginRequest.getName())
        .map(
            tempPassword -> {
              if (passwordChecker.test(loginRequest.getPassword(), tempPassword)) {
                tempPasswordDao.invalidateTempPasswords(loginRequest.getName());
                return true;
              } else {
                return false;
              }
            })
        .orElse(false);
  }
}
