package games.strategy.triplea.settings.models;

import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class AiSettings {


  static final int DEFAULT_AI_PAUSE_DURACTION = 400;
  private static final boolean DEFAULT_SHOW_BATTLE_BETWEEN_AI = true;

  public int getAiPauseDuration() {
    return SystemPreferences.get(SystemPreferenceKey.AI_PAUSE_DURACTION, DEFAULT_AI_PAUSE_DURACTION);
  }

  public void setAiPauseDuration(final String value) {
    SystemPreferences.put(SystemPreferenceKey.AI_PAUSE_DURACTION, value);
  }

  public boolean showBattlesBetweenAi() {
    return SystemPreferences.get(SystemPreferenceKey.SHOW_BATTLES_BETWEEN_AI, DEFAULT_SHOW_BATTLE_BETWEEN_AI);
  }

  public void setShowBattlesBetweenAi(final boolean value) {
    SystemPreferences.put(SystemPreferenceKey.SHOW_BATTLES_BETWEEN_AI, value);
  }
}
