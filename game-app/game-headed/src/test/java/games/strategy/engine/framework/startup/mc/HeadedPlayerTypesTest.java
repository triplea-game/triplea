package games.strategy.engine.framework.startup.mc;

import static org.hamcrest.MatcherAssert.assertThat;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

public class HeadedPlayerTypesTest {

  @BeforeEach
  public final void initializeClientSettingPreferences() {
    ClientSetting.setPreferences(new MemoryPreferences());
  }

  @AfterEach
  @SuppressWarnings("static-method")
  public final void uninitializeClientSettingPreferences() {
    ClientSetting.resetPreferences();
  }

  @Test
  void playerTypes() {
    final PlayerTypes playerTypes = new PlayerTypes(HeadedPlayerTypes.getPlayerTypes());
    assertThat(
        "Ensure we do not have an example invisible player type in the selection list",
        List.of(playerTypes.getAvailablePlayerLabels()),
        Matchers.not(IsCollectionContaining.hasItem(HeadedPlayerTypes.CLIENT_PLAYER.getLabel())));

    assertThat(
        "Ensure we have a visible player type in the selection list",
        List.of(playerTypes.getAvailablePlayerLabels()),
        IsCollectionContaining.hasItem(PlayerTypes.WEAK_AI.getLabel()));
  }
}
