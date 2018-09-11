package games.strategy.triplea.attachments;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;

@MapSupport
public class CanalAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -1991066817386812634L;

  private String m_canalName = null;
  private Set<Territory> m_landTerritories = null;
  private Set<UnitType> m_excludedUnits = null;

  public CanalAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Checks if the route contains both territories to pass through the given canal. If route is
   * null returns true.
   */
  public static boolean isCanalOnRoute(final String canalName, final Route route) {
    if (route == null) {
      return true;
    }
    boolean previousTerritoryHasCanal = false;
    for (final Territory t : route) {
      boolean currentTerritoryHasCanal = false;
      for (final CanalAttachment canalAttachment : get(t)) {
        if (canalAttachment.getCanalName().equals(canalName)) {
          currentTerritoryHasCanal = true;
          break;
        }
      }
      if (previousTerritoryHasCanal && currentTerritoryHasCanal) {
        return true;
      }
      previousTerritoryHasCanal = currentTerritoryHasCanal;
    }
    return false;
  }

  public static Set<CanalAttachment> get(final Territory t) {
    return t.getAttachments().values().stream()
        .filter(attachment -> attachment.getName().startsWith(Constants.CANAL_ATTACHMENT_PREFIX))
        .map(CanalAttachment.class::cast)
        .collect(Collectors.toSet());
  }

  static CanalAttachment get(final Territory t, final String nameOfAttachment) {
    return getAttachment(t, nameOfAttachment, CanalAttachment.class);
  }

  private void setCanalName(final String name) {
    if (name == null) {
      m_canalName = null;
      return;
    }
    m_canalName = name;
  }

  public String getCanalName() {
    return m_canalName;
  }

  private void resetCanalName() {
    m_canalName = null;
  }

  private void setLandTerritories(final String landTerritories) {
    if (landTerritories == null) {
      m_landTerritories = null;
      return;
    }
    final HashSet<Territory> terrs = new HashSet<>();
    for (final String name : splitOnColon(landTerritories)) {
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new IllegalStateException("Canals: No territory called: " + name + thisErrorMsg());
      }
      terrs.add(territory);
    }
    m_landTerritories = terrs;
  }

  private void setLandTerritories(final Set<Territory> value) {
    m_landTerritories = value;
  }

  public Set<Territory> getLandTerritories() {
    return m_landTerritories;
  }

  private void resetLandTerritories() {
    m_landTerritories = null;
  }

  private void setExcludedUnits(final String value) {
    if (value == null) {
      m_excludedUnits = null;
      return;
    }
    if (m_excludedUnits == null) {
      m_excludedUnits = new HashSet<>();
    }
    if (value.equalsIgnoreCase("NONE")) {
      return;
    }
    if (value.equalsIgnoreCase("ALL")) {
      m_excludedUnits.addAll(getData().getUnitTypeList().getAllUnitTypes());
      return;
    }
    for (final String name : splitOnColon(value)) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(name);
      if (ut == null) {
        throw new IllegalStateException("Canals: No UnitType called: " + name + thisErrorMsg());
      }
      m_excludedUnits.add(ut);
    }
  }

  private void setExcludedUnits(final Set<UnitType> value) {
    m_excludedUnits = value;
  }

  public Set<UnitType> getExcludedUnits() {
    if (m_excludedUnits == null) {
      return new HashSet<>(
          CollectionUtils.getMatches(getData().getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAir()));
    }
    return m_excludedUnits;
  }

  private void resetExcludedUnits() {
    m_excludedUnits = null;
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    if (m_canalName == null) {
      throw new GameParseException("Canals must have a canalName set!" + thisErrorMsg());
    }
    if (m_landTerritories == null || m_landTerritories.size() == 0) {
      throw new GameParseException("Canal named " + m_canalName + " must have landTerritories set!" + thisErrorMsg());
    }
    final Set<Territory> territories = new HashSet<>();
    for (final Territory t : data.getMap()) {
      final Set<CanalAttachment> canalAttachments = get(t);
      for (final CanalAttachment canalAttachment : canalAttachments) {
        if (canalAttachment.getCanalName().equals(m_canalName)) {
          territories.add(t);
        }
      }
    }
    if (territories.size() != 2) {
      throw new GameParseException(
          "Wrong number of sea zones for canal (exactly 2 sea zones may have the same canalName):" + territories);
    }
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("canalName", MutableProperty.ofString(this::setCanalName, this::getCanalName, this::resetCanalName))
        .put("landTerritories",
            MutableProperty.of(
                this::setLandTerritories,
                this::setLandTerritories,
                this::getLandTerritories,
                this::resetLandTerritories))
        .put("excludedUnits",
            MutableProperty.of(
                this::setExcludedUnits,
                this::setExcludedUnits,
                this::getExcludedUnits,
                this::resetExcludedUnits))
        .build();
  }
}
