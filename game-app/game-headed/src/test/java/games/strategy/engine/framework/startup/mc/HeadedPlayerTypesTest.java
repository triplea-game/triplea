package games.strategy.engine.framework.startup.mc;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
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
    assertThat(List.of(playerTypes.getAvailablePlayerLabels()))
        .as("Ensure we do not have an example invisible player type in the selection list")
        .doesNotContain(HeadedPlayerTypes.CLIENT_PLAYER.getLabel());

    assertThat(List.of(playerTypes.getAvailablePlayerLabels()))
        .as("Ensure we have a visible player type in the selection list")
        .contains(PlayerTypes.WEAK_AI.getLabel());
  }
}
