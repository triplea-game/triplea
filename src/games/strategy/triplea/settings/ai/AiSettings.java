package games.strategy.triplea.settings.ai;

import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.PreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;

public class AiSettings implements HasDefaults {


  private static final int DEFAULT_AI_PAUSE_DURACTION = 400;

  @Override
  public void setToDefault() {
    setAiPauseDuration(String.valueOf(DEFAULT_AI_PAUSE_DURACTION));
  }

  public int getAiPauseDuration() {
    return SystemPreferences.get(PreferenceKey.AI_PAUSE_DURACTION, DEFAULT_AI_PAUSE_DURACTION);
  }
  public void setAiPauseDuration(String value) {
    SystemPreferences.put(PreferenceKey.AI_PAUSE_DURACTION, value);
  }
}
