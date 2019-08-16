package org.triplea.lobby.server.login.forgot.password.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.triplea.test.common.Integration;

@ExtendWith(DBUnitExtension.class)
@Integration
@DataSet("forgot_password/user_data.yml")
class ForgotPasswordModuleIntegrationTest {
  private final Predicate<String> module =
      ForgotPasswordModuleFactory.buildForgotPasswordModule((name, password) -> {});

  @Test
  void userDoesNotExist() {
    assertThat(module.test("user_DNE"), is(false));
  }

  @Test
  void userExists() {
    assertThat(module.test("user"), is(true));
  }
}
