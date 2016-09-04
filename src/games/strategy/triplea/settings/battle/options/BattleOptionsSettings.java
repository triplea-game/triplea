package games.strategy.triplea.settings.battle.options;

import games.strategy.triplea.settings.HasDefaults;
import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;
import games.strategy.triplea.ui.BattleDisplay;

public class BattleOptionsSettings implements HasDefaults {

  private static final boolean DEFAULT_CONFIRM_ENEMY_CASUALTIES = false;
  private static final boolean DEFAULT_CONFIRM_DEFENSIVE_ROLLS = false;
  private static final boolean DEFAULT_FOCUS_ON_OWN_CASUALTIES = true;

  public boolean confirmEnemyCasualties() {
    return SystemPreferences.get(SystemPreferenceKey.CONFIRM_ENEMY_CASUALTIES, DEFAULT_CONFIRM_ENEMY_CASUALTIES);
  }

  public void setConfirmEnemyCasualties(final boolean value) {
    BattleDisplay.setFocusOnOwnCasualtiesNotification(value);
    SystemPreferences.put(SystemPreferenceKey.CONFIRM_ENEMY_CASUALTIES, value);
  }

  public boolean confirmDefensiveRolls() {
    return SystemPreferences.get(SystemPreferenceKey.CONFIRM_DEFENSIVE_ROLLS, DEFAULT_CONFIRM_DEFENSIVE_ROLLS);
  }

  public void setConfirmDefensiveRolls(final boolean value) {
    BattleDisplay.setConfirmDefensiveRolls(value);
    SystemPreferences.put(SystemPreferenceKey.CONFIRM_DEFENSIVE_ROLLS, value);
  }

  public boolean focusOnOwnCasualties() {
    return SystemPreferences.get(SystemPreferenceKey.FOCUS_ON_OWN_CASUALTIES, DEFAULT_FOCUS_ON_OWN_CASUALTIES);
  }

  public void setFocusOnOwnCasualties(final boolean value) {
    BattleDisplay.setFocusOnOwnCasualtiesNotification(value);
    SystemPreferences.put(SystemPreferenceKey.FOCUS_ON_OWN_CASUALTIES, value);
  }


  @Override
  public void setToDefault() {
    setConfirmEnemyCasualties(DEFAULT_CONFIRM_ENEMY_CASUALTIES);
  }
}
