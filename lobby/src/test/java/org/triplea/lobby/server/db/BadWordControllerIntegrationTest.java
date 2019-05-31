package org.triplea.lobby.server.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.test.common.Integration;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;

@ExtendWith(DBUnitExtension.class)
@Integration
final class BadWordControllerIntegrationTest {
  private final BadWordDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getBadWordDao();

  @Nested
  final class CaseInsensitiveContainsTest {
    @ParameterizedTest
    @ValueSource(strings = {"bad", "BAD", "one bad", "Badword"})
    @DataSet("badwords/bad.yml")
    void containsCase(final String testValue) {
      assertThat(controller.containsBadWord(testValue), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ok", "B A D", ""})
    @DataSet("badwords/bad.yml")
    void doesNotContainCase(final String doesNotContain) {
      assertThat(controller.containsBadWord(doesNotContain), is(false));
    }
  }
}
