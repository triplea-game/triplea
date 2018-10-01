package games.strategy.triplea.ui;

import static games.strategy.triplea.ui.SelectTerritoryDialog.getSelectedTerritory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.SelectTerritoryDialog.Result;

final class SelectTerritoryDialogTest {
  @Nested
  final class GetSelectedTerritoryTest {
    private final Territory territory = new Territory("territory", new GameData());

    @Test
    void shouldReturnTerritoryWhenResultIsOk() {
      assertThat(getSelectedTerritory(Result.OK, () -> territory), is(Optional.of(territory)));
    }

    @Test
    void shouldReturnEmptyWhenResultIsCancel() {
      assertThat(getSelectedTerritory(Result.CANCEL, () -> territory), is(Optional.empty()));
    }
  }
}
