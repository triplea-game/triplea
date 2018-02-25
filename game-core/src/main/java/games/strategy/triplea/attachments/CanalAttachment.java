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
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
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

  public static Set<Territory> getAllCanalSeaZones(final String canalName, final GameData data) {
    final Set<Territory> territories = new HashSet<>();
    for (final Territory t : data.getMap()) {
      final Set<CanalAttachment> canalAttachments = get(t);
      if (canalAttachments.isEmpty()) {
        continue;
      }
      for (final CanalAttachment canalAttachment : canalAttachments) {
        if (canalAttachment.getCanalName().equals(canalName)) {
          territories.add(t);
        }
      }
    }
    if (territories.size() != 2) {
      throw new IllegalStateException(
          "Wrong number of sea zones for canal (exactly 2 sea zones may have the same canalName):" + territories);
    }
    return territories;
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

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCanalName(final String name) {
    if (name == null) {
      m_canalName = null;
      return;
    }
    m_canalName = name;
  }

  public String getCanalName() {
    return m_canalName;
  }

  public void resetCanalName() {
    m_canalName = null;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setLandTerritories(final String landTerritories) {
    if (landTerritories == null) {
      m_landTerritories = null;
      return;
    }
    final HashSet<Territory> terrs = new HashSet<>();
    for (final String name : landTerritories.split(":")) {
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new IllegalStateException("Canals: No territory called: " + name + thisErrorMsg());
      }
      terrs.add(territory);
    }
    m_landTerritories = terrs;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setLandTerritories(final Set<Territory> value) {
    m_landTerritories = value;
  }

  public Set<Territory> getLandTerritories() {
    return m_landTerritories;
  }

  public void resetLandTerritories() {
    m_landTerritories = null;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setExcludedUnits(final String value) {
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
    for (final String name : value.split(":")) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(name);
      if (ut == null) {
        throw new IllegalStateException("Canals: No UnitType called: " + name + thisErrorMsg());
      }
      m_excludedUnits.add(ut);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setExcludedUnits(final Set<UnitType> value) {
    m_excludedUnits = value;
  }

  public Set<UnitType> getExcludedUnits() {
    if (m_excludedUnits == null) {
      return new HashSet<>(
          CollectionUtils.getMatches(getData().getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAir()));
    }
    return m_excludedUnits;
  }

  public void clearExcludedUnits() {
    m_excludedUnits.clear();
  }

  public void resetExcludedUnits() {
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
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("canalName", MutableProperty.ofString(this::setCanalName, this::getCanalName, this::resetCanalName))
        .put("landTerritories",
            MutableProperty.of(
                Set.class,
                this::setLandTerritories,
                this::setLandTerritories,
                this::getLandTerritories,
                this::resetLandTerritories))
        .put("excludedUnits",
            MutableProperty.of(
                Set.class,
                this::setExcludedUnits,
                this::setExcludedUnits,
                this::getExcludedUnits,
                this::resetExcludedUnits))
        .build();
  }
}
