package games.strategy.triplea.settings;

import games.strategy.triplea.settings.models.AiSettings;
import games.strategy.triplea.settings.models.BattleCalcSettings;
import games.strategy.triplea.settings.models.BattleOptionsSettings;
import games.strategy.triplea.settings.models.FolderSettings;
import games.strategy.triplea.settings.models.ScrollSettings;

/**
 * A collection-like class containing model objects for the game settings that can be adjusted by players.
 */
public class ClientSettings {
  private final ScrollSettings scrollSettings = new ScrollSettings();
  private final FolderSettings folderSettings = new FolderSettings();
  private final AiSettings aiSettings = new AiSettings();
  private final BattleCalcSettings battleCalcSettings = new BattleCalcSettings();
  private final BattleOptionsSettings battleOptionsSettings = new BattleOptionsSettings();


  public ScrollSettings getScrollSettings() {
    return scrollSettings;
  }

  public FolderSettings getFolderSettings() {
     return folderSettings;
  }

  public AiSettings getAiSettings() {
    return aiSettings;
  }

  public BattleCalcSettings getBattleCalcSettings() {
    return battleCalcSettings;
  }

  public BattleOptionsSettings getBattleOptionsSettings() {
    return battleOptionsSettings;
  }

}
