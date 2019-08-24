package games.strategy.triplea.ui;

import games.strategy.triplea.ui.screen.UnitsDrawer;
import java.util.prefs.Preferences;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class to change how unit flags are drawn on the map. Unit flags are flags drawn underneath or
 * below units to make it easier to identify which nation they belong to. By default flag drawing is
 * turned off, toggling once turns small flags on, toggle again to turn on large flags and again to
 * turn off.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class FlagDrawMode {

  static void toggleNextDrawMode(final MapPanel mapPanel) {

    final Preferences prefs = Preferences.userNodeForPackage(FlagDrawMode.class);
    final UnitsDrawer.UnitFlagDrawMode setting =
        Enum.valueOf(
            UnitsDrawer.UnitFlagDrawMode.class,
            prefs.get(
                UnitsDrawer.PreferenceKeys.DRAW_MODE.name(),
                UnitsDrawer.UnitFlagDrawMode.SMALL_FLAG.toString()));
    UnitsDrawer.setUnitFlagDrawMode(setting, prefs);
    UnitsDrawer.enabledFlags =
        prefs.getBoolean(
            UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), UnitsDrawer.enabledFlags);

    // draw mode is off, toggle small flag
    if (!prefs.getBoolean(UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), false)) {
      UnitsDrawer.enabledFlags = true;
      prefs.putBoolean(UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), true);
      prefs.put(
          UnitsDrawer.PreferenceKeys.DRAW_MODE.name(),
          UnitsDrawer.UnitFlagDrawMode.SMALL_FLAG.toString());
      UnitsDrawer.setUnitFlagDrawMode(UnitsDrawer.UnitFlagDrawMode.SMALL_FLAG, prefs);
      // check if small flags, if so then draw large flags
    } else if (UnitsDrawer.UnitFlagDrawMode.SMALL_FLAG
        .toString()
        .equals(
            prefs.get(
                UnitsDrawer.PreferenceKeys.DRAW_MODE.name(),
                UnitsDrawer.UnitFlagDrawMode.SMALL_FLAG.toString()))) {
      UnitsDrawer.enabledFlags = true;
      prefs.putBoolean(UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), true);
      prefs.put(
          UnitsDrawer.PreferenceKeys.DRAW_MODE.name(),
          UnitsDrawer.UnitFlagDrawMode.LARGE_FLAG.toString());
      UnitsDrawer.setUnitFlagDrawMode(UnitsDrawer.UnitFlagDrawMode.LARGE_FLAG, prefs);
      // otherwise we had large flags, turn drawing flags off
    } else {
      UnitsDrawer.enabledFlags = false;
      prefs.putBoolean(UnitsDrawer.PreferenceKeys.DRAWING_ENABLED.name(), false);
      prefs.put(
          UnitsDrawer.PreferenceKeys.DRAW_MODE.name(),
          UnitsDrawer.UnitFlagDrawMode.LARGE_FLAG.toString());
      UnitsDrawer.setUnitFlagDrawMode(UnitsDrawer.UnitFlagDrawMode.SMALL_FLAG, prefs);
    }
    mapPanel.resetMap();
  }
}
