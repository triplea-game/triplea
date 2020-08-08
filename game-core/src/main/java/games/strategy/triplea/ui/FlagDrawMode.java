package games.strategy.triplea.ui;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class to change how unit flags are drawn on the map. Unit flags are flags drawn underneath or
 * below units to make it easier to identify which nation they belong to. By default flag drawing is
 * turned off, toggling once turns small flags on, toggle again to turn on large flags and again to
 * turn off.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FlagDrawMode {

  /**
   * Updates the current flag draw mode and refreshes the map so that the current flag draw mode is
   * rendered on map.
   */
  public static void toggleDrawMode(
      final UnitsDrawer.UnitFlagDrawMode drawMode, final MapPanel mapPanel) {
    ClientSetting.unitFlagDrawMode.setValueAndFlush(drawMode);
    mapPanel.resetMap();
  }

  /** Toggles to the next flag draw mode. */
  static void toggleNextDrawMode(final MapPanel mapPanel) {
    final var currentDrawMode = ClientSetting.unitFlagDrawMode.getValueOrThrow();
    toggleDrawMode(currentDrawMode.nextDrawMode(), mapPanel);
  }
}
