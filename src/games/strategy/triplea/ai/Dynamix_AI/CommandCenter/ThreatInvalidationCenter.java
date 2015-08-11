package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.Dynamix_AI.DSettings;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.ai.Dynamix_AI.Others.ThreatInvalidationType;

public class ThreatInvalidationCenter {
  private static HashMap<PlayerID, ThreatInvalidationCenter> s_TICInstances =
      new HashMap<PlayerID, ThreatInvalidationCenter>();

  public static ThreatInvalidationCenter get(final GameData data, final PlayerID player) {
    if (!s_TICInstances.containsKey(player)) {
      s_TICInstances.put(player, create(data, player));
    }
    return s_TICInstances.get(player);
  }

  private static ThreatInvalidationCenter create(final GameData data, final PlayerID player) {
    return new ThreatInvalidationCenter(data, player);
  }

  public static void ClearStaticInstances() {
    s_TICInstances.clear();
  }

  public static void NotifyStartOfRound() {
    s_TICInstances.clear();
  }

  private GameData m_data = null;
  @SuppressWarnings("unused")
  private PlayerID m_player = null;

  public ThreatInvalidationCenter(final GameData data, final PlayerID player) {
    m_data = data;
    m_player = player;
  }

  private boolean ThreatInvalidationSuspended = false;

  public void SuspendThreatInvalidation() {
    ThreatInvalidationSuspended = true;
  }

  public void ResumeThreatInvalidation() {
    ThreatInvalidationSuspended = false;
  }

  private final HashMap<Territory, List<Unit>> InvalidatedEnemyUnits = new HashMap<Territory, List<Unit>>();

  @SuppressWarnings("unchecked")
  public void InvalidateThreats(List<Unit> threats, final Territory hotspot) {
    if (DSettings.LoadSettings().AA_threatInvalidationType.equals(ThreatInvalidationType.None)) {
      return;
    }
    if (DSettings.LoadSettings().AA_percentageOfResistedThreatThatTasksInvalidate != 100) {
      DUtils.Log(Level.FINER, "            Threats we would invalidate if we invalidated all: {0}",
          DUtils.UnitList_ToString(threats));
      threats = DUtils.GetXPercentOfTheItemsInList(threats,
          DUtils.ToFloat(DSettings.LoadSettings().AA_percentageOfResistedThreatThatTasksInvalidate));
    }
    List<Territory> tersWereInvalidatingThreatsFor = new ArrayList<Territory>();
    if (DSettings.LoadSettings().AA_threatInvalidationType.equals(ThreatInvalidationType.Global)) {
      tersWereInvalidatingThreatsFor = DUtils.ToList(m_data.getMap().getTerritories());
    } else {
      // We're invalidating threats around the territory, the extent of the ring of ters we invalidate the threats for
      // is user-set
      tersWereInvalidatingThreatsFor = DUtils.GetTerritoriesWithinXDistanceOfY(m_data, hotspot,
          DSettings.LoadSettings().AA_threatInvalidationAroundHotspotRadius);
    }
    // Don't invalidate threats for the hotspot itself
    tersWereInvalidatingThreatsFor.remove(hotspot);
    for (final Territory ter : tersWereInvalidatingThreatsFor) {
      DUtils.AddObjectsToListValueForKeyInMap(InvalidatedEnemyUnits, ter, threats);
    }
    DUtils.Log(Level.FINER, "          Invalidating threats. Units: {0} Hotspot: {1} Ters: {2}",
        DUtils.UnitList_ToString(threats), hotspot.getName(), tersWereInvalidatingThreatsFor);
  }

  public boolean IsUnitInvalidatedForTer(final Unit unit, final Territory ter) {
    if (ThreatInvalidationSuspended) {
      return false;
    }
    if (!InvalidatedEnemyUnits.containsKey(ter)) {
      return false;
    }
    return InvalidatedEnemyUnits.get(ter).contains(unit);
  }

  public void ClearInvalidatedThreats() {
    DUtils.Log(Level.FINE, "    Clearing invalidated threats.");
    InvalidatedEnemyUnits.clear();
  }
}
