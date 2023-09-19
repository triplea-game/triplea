package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Attaches to technologies. Also contains static methods of interpreting data from all technology
 * attachments that a player has. Note: Empty collection fields default to null to minimize memory
 * use and serialization size.
 */
public class TechAbilityAttachment extends DefaultAttachment {
  // unitAbilitiesGained Static Strings
  public static final String ABILITY_CAN_BLITZ = "canBlitz";
  public static final String ABILITY_CAN_BOMBARD = "canBombard";

  private static final long serialVersionUID = 1866305599625384294L;

  // attachment fields
  private @Nullable IntegerMap<UnitType> attackBonus = null;
  private @Nullable IntegerMap<UnitType> defenseBonus = null;
  private @Nullable IntegerMap<UnitType> movementBonus = null;
  private @Nullable IntegerMap<UnitType> radarBonus = null;
  private @Nullable IntegerMap<UnitType> airAttackBonus = null;
  private @Nullable IntegerMap<UnitType> airDefenseBonus = null;
  private @Nullable IntegerMap<UnitType> productionBonus = null;
  // -1 means not set
  private int minimumTerritoryValueForProductionBonus = -1;
  // -1 means not set
  private int repairDiscount = -1;
  // -1 means not set
  private int warBondDiceSides = -1;
  private int warBondDiceNumber = 0;
  private @Nullable IntegerMap<UnitType> rocketDiceNumber = null;
  private int rocketDistance = 0;
  private int rocketNumberPerTerritory = 1;
  private @Nullable Map<UnitType, Set<String>> unitAbilitiesGained = null;
  private boolean airborneForces = false;
  private @Nullable IntegerMap<UnitType> airborneCapacity = null;
  private @Nullable Set<UnitType> airborneTypes = null;
  private int airborneDistance = 0;
  private @Nullable Set<UnitType> airborneBases = null;
  private @Nullable Map<String, Set<UnitType>> airborneTargetedByAa = null;
  private @Nullable IntegerMap<UnitType> attackRollsBonus = null;
  private @Nullable IntegerMap<UnitType> defenseRollsBonus = null;
  private @Nullable IntegerMap<UnitType> bombingBonus = null;
  private @Nullable IntegerMap<UnitType> tuvBonus = null;

  public TechAbilityAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /** Returns the corresponding tech ability attachment for a given tech advancement type. */
  public static TechAbilityAttachment get(final TechAdvance type) {
    if (type instanceof GenericTechAdvance) {
      // generic techs can name a hardcoded tech, therefore if it exists we should use the hard
      // coded tech's attachment.
      // (if the map maker doesn't want to use the hardcoded tech's attachment, they should not name
      // a hardcoded tech)
      final TechAdvance hardCodedAdvance = ((GenericTechAdvance) type).getAdvance();
      if (hardCodedAdvance != null) {
        return (TechAbilityAttachment)
            hardCodedAdvance.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME);
      }
    }
    return (TechAbilityAttachment) type.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME);
  }

  @VisibleForTesting
  String[] splitAndValidate(final String name, final String value) throws GameParseException {
    final String[] stringArray = splitOnColon(value);
    if (value.isEmpty() || stringArray.length > 2) {
      throw new GameParseException(
          String.format(
              "%s cannot be empty or have more than two fields %s", name, thisErrorMsg()));
    }
    return stringArray;
  }

  @VisibleForTesting
  void applyCheckedValue(
      final String name, final String value, final BiConsumer<UnitType, Integer> putter)
      throws GameParseException {
    final String[] s = splitAndValidate(name, value);
    putter.accept(getUnitTypeOrThrow(s[1]), getInt(s[0]));
  }

  @VisibleForTesting
  int getIntInRange(
      final String name, final String value, final int max, final boolean allowUndefined)
      throws GameParseException {
    final int intValue = getInt(value);
    if (intValue < (allowUndefined ? -1 : 0) || intValue > max) {
      throw new GameParseException(
          String.format(
              "%s must be%s between 0 and %s, was %s %s",
              name, allowUndefined ? " -1 (no effect), or be" : "", max, value, thisErrorMsg()));
    }
    return intValue;
  }

  private void setAttackBonus(final String value) throws GameParseException {
    if (attackBonus == null) {
      attackBonus = new IntegerMap<>();
    }
    applyCheckedValue("attackBonus", value, attackBonus::put);
  }

  private void setAttackBonus(final IntegerMap<UnitType> value) {
    attackBonus = value;
  }

  public IntegerMap<UnitType> getAttackBonus() {
    return getIntegerMapProperty(attackBonus);
  }

  private void resetAttackBonus() {
    attackBonus = null;
  }

  private void setDefenseBonus(final String value) throws GameParseException {
    if (defenseBonus == null) {
      defenseBonus = new IntegerMap<>();
    }
    applyCheckedValue("defenseBonus", value, defenseBonus::put);
  }

  private void setDefenseBonus(final IntegerMap<UnitType> value) {
    defenseBonus = value;
  }

  public IntegerMap<UnitType> getDefenseBonus() {
    return getIntegerMapProperty(defenseBonus);
  }

  private void resetDefenseBonus() {
    defenseBonus = null;
  }

  private void setMovementBonus(final String value) throws GameParseException {
    if (movementBonus == null) {
      movementBonus = new IntegerMap<>();
    }
    applyCheckedValue("movementBonus", value, movementBonus::put);
  }

  private void setMovementBonus(final IntegerMap<UnitType> value) {
    movementBonus = value;
  }

  public IntegerMap<UnitType> getMovementBonus() {
    return getIntegerMapProperty(movementBonus);
  }

  private void resetMovementBonus() {
    movementBonus = null;
  }

  private void setRadarBonus(final String value) throws GameParseException {
    if (radarBonus == null) {
      radarBonus = new IntegerMap<>();
    }
    applyCheckedValue("radarBonus", value, radarBonus::put);
  }

  private void setRadarBonus(final IntegerMap<UnitType> value) {
    radarBonus = value;
  }

  public IntegerMap<UnitType> getRadarBonus() {
    return getIntegerMapProperty(radarBonus);
  }

  private void resetRadarBonus() {
    radarBonus = null;
  }

  private void setAirAttackBonus(final String value) throws GameParseException {
    if (airAttackBonus == null) {
      airAttackBonus = new IntegerMap<>();
    }
    applyCheckedValue("airAttackBonus", value, airAttackBonus::put);
  }

  private void setAirAttackBonus(final IntegerMap<UnitType> value) {
    airAttackBonus = value;
  }

  public IntegerMap<UnitType> getAirAttackBonus() {
    return getIntegerMapProperty(airAttackBonus);
  }

  private void resetAirAttackBonus() {
    airAttackBonus = null;
  }

  private void setAirDefenseBonus(final String value) throws GameParseException {
    if (airDefenseBonus == null) {
      airDefenseBonus = new IntegerMap<>();
    }
    applyCheckedValue("airDefenseBonus", value, airDefenseBonus::put);
  }

  private void setAirDefenseBonus(final IntegerMap<UnitType> value) {
    airDefenseBonus = value;
  }

  public IntegerMap<UnitType> getAirDefenseBonus() {
    return getIntegerMapProperty(airDefenseBonus);
  }

  private void resetAirDefenseBonus() {
    airDefenseBonus = null;
  }

  private void setProductionBonus(final String value) throws GameParseException {
    if (productionBonus == null) {
      productionBonus = new IntegerMap<>();
    }
    applyCheckedValue("productionBonus", value, productionBonus::put);
  }

  private void setProductionBonus(final IntegerMap<UnitType> value) {
    productionBonus = value;
  }

  public IntegerMap<UnitType> getProductionBonus() {
    return getIntegerMapProperty(productionBonus);
  }

  private void resetProductionBonus() {
    productionBonus = null;
  }

  private void setMinimumTerritoryValueForProductionBonus(final String value)
      throws GameParseException {
    minimumTerritoryValueForProductionBonus =
        getIntInRange("minimumTerritoryValueForProductionBonus", value, 10000, true);
  }

  private void setMinimumTerritoryValueForProductionBonus(final Integer value) {
    minimumTerritoryValueForProductionBonus = value;
  }

  public int getMinimumTerritoryValueForProductionBonus() {
    return minimumTerritoryValueForProductionBonus;
  }

  private void resetMinimumTerritoryValueForProductionBonus() {
    minimumTerritoryValueForProductionBonus = -1;
  }

  private void setRepairDiscount(final String value) throws GameParseException {
    repairDiscount = getIntInRange("repairDiscount", value, 100, true);
  }

  private void setRepairDiscount(final Integer value) {
    repairDiscount = value;
  }

  private int getRepairDiscount() {
    return repairDiscount;
  }

  public static double getRepairDiscount(final Collection<TechAdvance> techAdvances) {
    return Math.max(
        0,
        1.0
            - techAdvances.stream()
                .map(TechAbilityAttachment::get)
                .filter(Objects::nonNull)
                .mapToInt(TechAbilityAttachment::getRepairDiscount)
                .filter(i -> i != -1)
                .mapToDouble(d -> d / 100.0)
                .sum());
  }

  private void resetRepairDiscount() {
    repairDiscount = -1;
  }

  private void setWarBondDiceSides(final String value) throws GameParseException {
    warBondDiceSides = getIntInRange("warBondDiceSides", value, 200, true);
  }

  private void setWarBondDiceSides(final Integer value) {
    warBondDiceSides = value;
  }

  private int getWarBondDiceSides() {
    return warBondDiceSides;
  }

  public static int getWarBondDiceSides(final Collection<TechAdvance> techAdvances) {
    return techAdvances.stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .mapToInt(t -> t.getWarBondDiceSides())
        .filter(t -> t > 0)
        .findAny()
        .orElse(0);
  }

  private void resetWarBondDiceSides() {
    warBondDiceSides = -1;
  }

  private void setWarBondDiceNumber(final String value) throws GameParseException {
    warBondDiceNumber = getIntInRange("warBondDiceNumber", value, 100, false);
  }

  private void setWarBondDiceNumber(final Integer value) {
    warBondDiceNumber = value;
  }

  private int getWarBondDiceNumber() {
    return warBondDiceNumber;
  }

  public static int getWarBondDiceNumber(final Collection<TechAdvance> techAdvances) {
    return techAdvances.stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .mapToInt(t -> t.getWarBondDiceNumber())
        .filter(t -> t > 0)
        .sum();
  }

  private void resetWarBondDiceNumber() {
    warBondDiceNumber = 0;
  }

  private void setRocketDiceNumber(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("rocketDiceNumber must have two fields" + thisErrorMsg());
    }
    if (rocketDiceNumber == null) {
      rocketDiceNumber = new IntegerMap<>();
    }
    rocketDiceNumber.put(getUnitTypeOrThrow(s[1]), getInt(s[0]));
  }

  private void setRocketDiceNumber(final IntegerMap<UnitType> value) {
    rocketDiceNumber = value;
  }

  public IntegerMap<UnitType> getRocketDiceNumber() {
    return getIntegerMapProperty(rocketDiceNumber);
  }

  private void resetRocketDiceNumber() {
    rocketDiceNumber = null;
  }

  private void setRocketDistance(final String value) throws GameParseException {
    rocketDistance = getIntInRange("rocketDistance", value, 100, false);
  }

  private void setRocketDistance(final Integer value) {
    rocketDistance = value;
  }

  public int getRocketDistance() {
    return rocketDistance;
  }

  private void resetRocketDistance() {
    rocketDistance = 0;
  }

  private void setRocketNumberPerTerritory(final String value) throws GameParseException {
    rocketNumberPerTerritory = getIntInRange("rocketNumberPerTerritory", value, 200, true);
  }

  private void setRocketNumberPerTerritory(final Integer value) {
    rocketNumberPerTerritory = value;
  }

  public int getRocketNumberPerTerritory() {
    return rocketNumberPerTerritory;
  }

  private void resetRocketNumberPerTerritory() {
    rocketNumberPerTerritory = 1;
  }

  private void setUnitAbilitiesGained(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 2) {
      throw new GameParseException(
          "unitAbilitiesGained must list the unit type, then all abilities gained"
              + thisErrorMsg());
    }
    final String unitType = s[0];
    // validate that this unit exists in the xml
    final UnitType ut = getUnitTypeOrThrow(unitType);
    if (unitAbilitiesGained == null) {
      unitAbilitiesGained = new HashMap<>();
    }
    final Set<String> abilities = unitAbilitiesGained.computeIfAbsent(ut, key -> new HashSet<>());
    // start at 1
    for (int i = 1; i < s.length; i++) {
      final String ability = s[i];
      if (!(ability.equals(ABILITY_CAN_BLITZ) || ability.equals(ABILITY_CAN_BOMBARD))) {
        throw new GameParseException(
            "unitAbilitiesGained so far only supports: "
                + ABILITY_CAN_BLITZ
                + " and "
                + ABILITY_CAN_BOMBARD
                + thisErrorMsg());
      }
      abilities.add(ability.intern());
    }
  }

  private void setUnitAbilitiesGained(final Map<UnitType, Set<String>> value) {
    unitAbilitiesGained = value;
  }

  public Map<UnitType, Set<String>> getUnitAbilitiesGained() {
    return getMapProperty(unitAbilitiesGained);
  }

  private void resetUnitAbilitiesGained() {
    unitAbilitiesGained = null;
  }

  private void setAirborneForces(final String value) {
    airborneForces = getBool(value);
  }

  private void setAirborneForces(final Boolean value) {
    airborneForces = value;
  }

  private boolean getAirborneForces() {
    return airborneForces;
  }

  private void resetAirborneForces() {
    airborneForces = false;
  }

  private void setAirborneCapacity(final String value) throws GameParseException {
    if (airborneCapacity == null) {
      airborneCapacity = new IntegerMap<>();
    }
    applyCheckedValue("airborneCapacity", value, airborneCapacity::put);
  }

  private void setAirborneCapacity(final IntegerMap<UnitType> value) {
    airborneCapacity = value;
  }

  private IntegerMap<UnitType> getAirborneCapacity() {
    return getIntegerMapProperty(airborneCapacity);
  }

  public static IntegerMap<UnitType> getAirborneCapacity(
      final Collection<TechAdvance> techAdvances) {
    final IntegerMap<UnitType> capacityMap = new IntegerMap<>();
    for (final TechAdvance ta : techAdvances) {
      final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
      if (taa != null) {
        capacityMap.add(taa.getAirborneCapacity());
      }
    }
    return capacityMap;
  }

  public static int getAirborneCapacity(
      final Collection<Unit> units, final Collection<TechAdvance> techAdvances) {
    final IntegerMap<UnitType> capacityMap = getAirborneCapacity(techAdvances);
    int airborneCapacity = 0;
    for (final Unit u : units) {
      airborneCapacity += Math.max(0, (capacityMap.getInt(u.getType()) - u.getLaunched()));
    }
    return airborneCapacity;
  }

  private void resetAirborneCapacity() {
    airborneCapacity = null;
  }

  private void setAirborneTypes(final String value) throws GameParseException {
    airborneTypes = parseUnitTypes("airborneTypes", value, airborneTypes);
  }

  private void setAirborneTypes(final Set<UnitType> value) {
    airborneTypes = value;
  }

  private Set<UnitType> getAirborneTypes() {
    return getSetProperty(airborneTypes);
  }

  public static Set<UnitType> getAirborneTypes(final Collection<TechAdvance> techAdvances) {
    return techAdvances.stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getAirborneTypes)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private void resetAirborneTypes() {
    airborneTypes = null;
  }

  private void setAirborneDistance(final String value) throws GameParseException {
    airborneDistance = getIntInRange("airborneDistance", value, 100, false);
  }

  private void setAirborneDistance(final Integer value) {
    airborneDistance = value;
  }

  private int getAirborneDistance() {
    return airborneDistance;
  }

  public static int getAirborneDistance(final Collection<TechAdvance> techAdvances) {
    return Math.max(
        0,
        techAdvances.stream()
            .map(TechAbilityAttachment::get)
            .filter(Objects::nonNull)
            .mapToInt(TechAbilityAttachment::getAirborneDistance)
            .sum());
  }

  private void resetAirborneDistance() {
    airborneDistance = 0;
  }

  private void setAirborneBases(final String value) throws GameParseException {
    airborneBases = parseUnitTypes("airborneBases", value, airborneBases);
  }

  private void setAirborneBases(final Set<UnitType> value) {
    airborneBases = value;
  }

  private Set<UnitType> getAirborneBases() {
    return getSetProperty(airborneBases);
  }

  public static Set<UnitType> getAirborneBases(final Collection<TechAdvance> techAdvances) {
    return techAdvances.stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getAirborneBases)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private void resetAirborneBases() {
    airborneBases = null;
  }

  private void setAirborneTargettedByAa(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 2) {
      throw new GameParseException(
          "airborneTargettedByAA must have at least two fields" + thisErrorMsg());
    }
    final Set<UnitType> unitTypes = new HashSet<>();
    for (int i = 1; i < s.length; i++) {
      unitTypes.add(getUnitTypeOrThrow(s[i]));
    }
    if (airborneTargetedByAa == null) {
      airborneTargetedByAa = new HashMap<>();
    }
    airborneTargetedByAa.put(s[0].intern(), unitTypes);
  }

  private void setAirborneTargettedByAa(final Map<String, Set<UnitType>> value) {
    airborneTargetedByAa = value;
  }

  private Map<String, Set<UnitType>> getAirborneTargettedByAa() {
    return getMapProperty(airborneTargetedByAa);
  }

  /**
   * Returns a map of the air unit types that can be targeted by each type of AA fire. The key is
   * the type of AA fire. The value is the collection of air unit types that can be targeted by the
   * key.
   */
  public static Map<String, Set<UnitType>> getAirborneTargettedByAa(
      final Collection<TechAdvance> techAdvances) {
    final Map<String, Set<UnitType>> airborneTargettedByAa = new HashMap<>();
    for (final TechAdvance ta : techAdvances) {
      final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
      if (taa != null) {
        final Map<String, Set<UnitType>> mapAa = taa.getAirborneTargettedByAa();
        if (mapAa != null && !mapAa.isEmpty()) {
          for (final Entry<String, Set<UnitType>> entry : mapAa.entrySet()) {
            airborneTargettedByAa
                .computeIfAbsent(entry.getKey(), key -> new HashSet<>())
                .addAll(entry.getValue());
          }
        }
      }
    }
    return airborneTargettedByAa;
  }

  private void resetAirborneTargettedByAa() {
    airborneTargetedByAa = null;
  }

  private void setAttackRollsBonus(final String value) throws GameParseException {
    if (attackRollsBonus == null) {
      attackRollsBonus = new IntegerMap<>();
    }
    applyCheckedValue("attackRollsBonus", value, attackRollsBonus::put);
  }

  private void setAttackRollsBonus(final IntegerMap<UnitType> value) {
    attackRollsBonus = value;
  }

  public IntegerMap<UnitType> getAttackRollsBonus() {
    return getIntegerMapProperty(attackRollsBonus);
  }

  private void resetAttackRollsBonus() {
    attackRollsBonus = null;
  }

  private void setDefenseRollsBonus(final String value) throws GameParseException {
    if (defenseRollsBonus == null) {
      defenseRollsBonus = new IntegerMap<>();
    }
    applyCheckedValue("defenseRollsBonus", value, defenseRollsBonus::put);
  }

  private void setDefenseRollsBonus(final IntegerMap<UnitType> value) {
    defenseRollsBonus = value;
  }

  public IntegerMap<UnitType> getDefenseRollsBonus() {
    return getIntegerMapProperty(defenseRollsBonus);
  }

  private void resetDefenseRollsBonus() {
    defenseRollsBonus = null;
  }

  private void setBombingBonus(final String value) throws GameParseException {
    if (bombingBonus == null) {
      bombingBonus = new IntegerMap<>();
    }
    applyCheckedValue("bombingBonus", value, bombingBonus::put);
  }

  private void setBombingBonus(final IntegerMap<UnitType> value) {
    bombingBonus = value;
  }

  public IntegerMap<UnitType> getBombingBonus() {
    return getIntegerMapProperty(bombingBonus);
  }

  private void resetBombingBonus() {
    bombingBonus = null;
  }

  private void setTUVBonus(final String value) throws GameParseException {
    if (tuvBonus == null) {
      tuvBonus = new IntegerMap<>();
    }
    applyCheckedValue("tuvBonus", value, tuvBonus::put);
  }

  private void setTUVBonus(final IntegerMap<UnitType> value) {
    tuvBonus = value;
  }

  public IntegerMap<UnitType> getTUVBonus() {
    return getIntegerMapProperty(tuvBonus);
  }

  private void resetTUVBonus() {
    tuvBonus = null;
  }
  public static boolean getAllowAirborneForces(final Collection<TechAdvance> techAdvances) {
    return techAdvances.stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .anyMatch(TechAbilityAttachment::getAirborneForces);
  }

  /**
   * Must be done only in GameParser, and only after we have already parsed ALL technologies,
   * attachments, and game options/properties.
   */
  public static void setDefaultTechnologyAttachments(final GameData data)
      throws GameParseException {
    // loop through all technologies. any "default/hard-coded" tech that doesn't have an attachment,
    // will get its
    // "default" attachment. any non-default tech are ignored.
    for (final TechAdvance techAdvance :
        TechAdvance.getTechAdvances(data.getTechnologyFrontier())) {
      final TechAdvance ta;
      if (techAdvance instanceof GenericTechAdvance) {
        final TechAdvance adv = ((GenericTechAdvance) techAdvance).getAdvance();
        if (adv != null) {
          ta = adv;
        } else {
          continue;
        }
      } else {
        ta = techAdvance;
      }
      final String propertyString = ta.getProperty();
      TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
      if (taa == null) {
        // debating if we should have flags for things like "air", "land", "sea", "aaGun",
        // "factory", "strategic
        // bomber", etc.
        // perhaps just the easy ones, of air, land, and sea?
        switch (propertyString) {
          case TechAdvance.TECH_PROPERTY_LONG_RANGE_AIRCRAFT:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            final List<UnitType> allAir =
                CollectionUtils.getMatches(
                    data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAir());
            for (final UnitType air : allAir) {
              taa.setMovementBonus("2:" + air.getName());
            }
            break;
          case TechAdvance.TECH_PROPERTY_AA_RADAR:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            final List<UnitType> allAa =
                CollectionUtils.getMatches(
                    data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAaForAnything());
            for (final UnitType aa : allAa) {
              taa.setRadarBonus("1:" + aa.getName());
            }
            break;
          case TechAdvance.TECH_PROPERTY_SUPER_SUBS:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            final List<UnitType> allSubs =
                CollectionUtils.getMatches(
                    data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsFirstStrike());
            for (final UnitType sub : allSubs) {
              taa.setAttackBonus("1:" + sub.getName());
            }
            break;
          case TechAdvance.TECH_PROPERTY_JET_POWER:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            final List<UnitType> allJets =
                CollectionUtils.getMatches(
                    data.getUnitTypeList().getAllUnitTypes(),
                    Matches.unitTypeIsAir().and(Matches.unitTypeIsStrategicBomber().negate()));
            final boolean ww2v3TechModel = Properties.getWW2V3TechModel(data.getProperties());
            for (final UnitType jet : allJets) {
              if (ww2v3TechModel) {
                taa.setAttackBonus("1:" + jet.getName());
                taa.setAirAttackBonus("1:" + jet.getName());
              } else {
                taa.setDefenseBonus("1:" + jet.getName());
                taa.setAirDefenseBonus("1:" + jet.getName());
              }
            }
            break;
          case TechAdvance.TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            final List<UnitType> allFactories =
                CollectionUtils.getMatches(
                    data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeCanProduceUnits());
            for (final UnitType factory : allFactories) {
              taa.setProductionBonus("2:" + factory.getName());
              taa.setMinimumTerritoryValueForProductionBonus("3");
              // means a 50% discount, which is half price
              taa.setRepairDiscount("50");
            }
            break;
          case TechAdvance.TECH_PROPERTY_WAR_BONDS:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            taa.setWarBondDiceSides(Integer.toString(data.getDiceSides()));
            taa.setWarBondDiceNumber("1");
            break;
          case TechAdvance.TECH_PROPERTY_ROCKETS:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            final List<UnitType> allRockets =
                CollectionUtils.getMatches(
                    data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsRocket());
            for (final UnitType rocket : allRockets) {
              taa.setRocketDiceNumber("1:" + rocket.getName());
            }
            taa.setRocketDistance("3");
            taa.setRocketNumberPerTerritory("1");
            break;
          case TechAdvance.TECH_PROPERTY_DESTROYER_BOMBARD:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            final List<UnitType> allDestroyers =
                CollectionUtils.getMatches(
                    data.getUnitTypeList().getAllUnitTypes(),
                    Matches.unitTypeIsDestroyer().and(Matches.unitTypeIsSea()));
            for (final UnitType destroyer : allDestroyers) {
              taa.setUnitAbilitiesGained(destroyer.getName() + ":" + ABILITY_CAN_BOMBARD);
            }
            break;
          case TechAdvance.TECH_PROPERTY_HEAVY_BOMBER:
            taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
            ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
            final List<UnitType> allBombers =
                CollectionUtils.getMatches(
                    data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsStrategicBomber());
            final int heavyBomberDiceRollsTotal =
                Properties.getHeavyBomberDiceRolls(data.getProperties());
            final boolean heavyBombersLhtr = Properties.getLhtrHeavyBombers(data.getProperties());
            for (final UnitType bomber : allBombers) {
              // TODO: The bomber dice rolls get set when the xml is parsed.
              // we subtract the base rolls to get the bonus
              final int heavyBomberDiceRollsBonus =
                  heavyBomberDiceRollsTotal
                      - bomber
                          .getUnitAttachment()
                          .getAttackRolls(data.getPlayerList().getNullPlayer());
              taa.setAttackRollsBonus(heavyBomberDiceRollsBonus + ":" + bomber.getName());
              if (heavyBombersLhtr) {
                // TODO: this all happens WHEN the xml is parsed. Which means if the user changes
                // the game options, this
                // does not get changed. (meaning, turning on LHTR bombers will not result in this
                // bonus damage,
                // etc. It would have to start on, in the xml.)
                taa.setDefenseRollsBonus(heavyBomberDiceRollsBonus + ":" + bomber.getName());
                // LHTR adds 1 to base roll
                taa.setBombingBonus("1:" + bomber.getName());
              }
            }
            break;
          default:
            break;
        }
        // The following technologies should NOT have ability attachments for them:
        // shipyards and industrialTechnology = because it is better to use a Trigger to change
        // player's production
        // improvedArtillerySupport = because it is already completely atomized and controlled
        // through support attachments
        // paratroopers = because it is already completely atomized and controlled through unit
        // attachments + game options
        // mechanizedInfantry = because it is already completely atomized and controlled through
        // unit attachments
        // IF one of the above named techs changes what it does in a future version of a&a, and the
        // change is large
        // enough or different enough that it cannot be done easily with a new game option,
        // then it is better to create a new tech rather than change the old one, and give the new
        // one a new name, like paratroopers2 or paratroopersAttack or Airborne_Forces.
      }
    }
  }

  // validator
  @Override
  public void validate(final GameState data) throws GameParseException {
    final TechAdvance ta = (TechAdvance) this.getAttachedTo();
    if (ta instanceof GenericTechAdvance) {
      final TechAdvance hardCodedAdvance = ((GenericTechAdvance) ta).getAdvance();
      if (hardCodedAdvance != null) {
        throw new GameParseException(
            "A custom Generic Tech Advance naming a hardcoded tech, may "
                + "not have a Tech Ability Attachment!"
                + this.thisErrorMsg());
      }
    }
  }

  @Override
  public MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "attackBonus":
        return MutableProperty.of(
            this::setAttackBonus,
            this::setAttackBonus,
            this::getAttackBonus,
            this::resetAttackBonus);
      case "defenseBonus":
        return MutableProperty.of(
            this::setDefenseBonus,
            this::setDefenseBonus,
            this::getDefenseBonus,
            this::resetDefenseBonus);
      case "movementBonus":
        return MutableProperty.of(
            this::setMovementBonus,
            this::setMovementBonus,
            this::getMovementBonus,
            this::resetMovementBonus);
      case "radarBonus":
        return MutableProperty.of(
            this::setRadarBonus, this::setRadarBonus, this::getRadarBonus, this::resetRadarBonus);
      case "airAttackBonus":
        return MutableProperty.of(
            this::setAirAttackBonus,
            this::setAirAttackBonus,
            this::getAirAttackBonus,
            this::resetAirAttackBonus);
      case "airDefenseBonus":
        return MutableProperty.of(
            this::setAirDefenseBonus,
            this::setAirDefenseBonus,
            this::getAirDefenseBonus,
            this::resetAirDefenseBonus);
      case "productionBonus":
        return MutableProperty.of(
            this::setProductionBonus,
            this::setProductionBonus,
            this::getProductionBonus,
            this::resetProductionBonus);
      case "minimumTerritoryValueForProductionBonus":
        return MutableProperty.of(
            this::setMinimumTerritoryValueForProductionBonus,
            this::setMinimumTerritoryValueForProductionBonus,
            this::getMinimumTerritoryValueForProductionBonus,
            this::resetMinimumTerritoryValueForProductionBonus);
      case "repairDiscount":
        return MutableProperty.of(
            this::setRepairDiscount,
            this::setRepairDiscount,
            this::getRepairDiscount,
            this::resetRepairDiscount);
      case "warBondDiceSides":
        return MutableProperty.of(
            this::setWarBondDiceSides,
            this::setWarBondDiceSides,
            this::getWarBondDiceSides,
            this::resetWarBondDiceSides);
      case "warBondDiceNumber":
        return MutableProperty.of(
            this::setWarBondDiceNumber,
            this::setWarBondDiceNumber,
            this::getWarBondDiceNumber,
            this::resetWarBondDiceNumber);
      case "rocketDiceNumber":
        return MutableProperty.of(
            this::setRocketDiceNumber,
            this::setRocketDiceNumber,
            this::getRocketDiceNumber,
            this::resetRocketDiceNumber);
      case "rocketDistance":
        return MutableProperty.of(
            this::setRocketDistance,
            this::setRocketDistance,
            this::getRocketDistance,
            this::resetRocketDistance);
      case "rocketNumberPerTerritory":
        return MutableProperty.of(
            this::setRocketNumberPerTerritory,
            this::setRocketNumberPerTerritory,
            this::getRocketNumberPerTerritory,
            this::resetRocketNumberPerTerritory);
      case "unitAbilitiesGained":
        return MutableProperty.of(
            this::setUnitAbilitiesGained,
            this::setUnitAbilitiesGained,
            this::getUnitAbilitiesGained,
            this::resetUnitAbilitiesGained);
      case "airborneForces":
        return MutableProperty.of(
            this::setAirborneForces,
            this::setAirborneForces,
            this::getAirborneForces,
            this::resetAirborneForces);
      case "airborneCapacity":
        return MutableProperty.of(
            this::setAirborneCapacity,
            this::setAirborneCapacity,
            this::getAirborneCapacity,
            this::resetAirborneCapacity);
      case "airborneTypes":
        return MutableProperty.of(
            this::setAirborneTypes,
            this::setAirborneTypes,
            this::getAirborneTypes,
            this::resetAirborneTypes);
      case "airborneDistance":
        return MutableProperty.of(
            this::setAirborneDistance,
            this::setAirborneDistance,
            this::getAirborneDistance,
            this::resetAirborneDistance);
      case "airborneBases":
        return MutableProperty.of(
            this::setAirborneBases,
            this::setAirborneBases,
            this::getAirborneBases,
            this::resetAirborneBases);
      case "airborneTargettedByAA":
        return MutableProperty.of(
            this::setAirborneTargettedByAa,
            this::setAirborneTargettedByAa,
            this::getAirborneTargettedByAa,
            this::resetAirborneTargettedByAa);
      case "attackRollsBonus":
        return MutableProperty.of(
            this::setAttackRollsBonus,
            this::setAttackRollsBonus,
            this::getAttackRollsBonus,
            this::resetAttackRollsBonus);
      case "defenseRollsBonus":
        return MutableProperty.of(
            this::setDefenseRollsBonus,
            this::setDefenseRollsBonus,
            this::getDefenseRollsBonus,
            this::resetDefenseRollsBonus);
      case "bombingBonus":
        return MutableProperty.of(
            this::setBombingBonus,
            this::setBombingBonus,
            this::getBombingBonus,
            this::resetBombingBonus);
      case "tuvBonus":
        return MutableProperty.of(
            this::setTUVBonus,
            this::setTUVBonus,
            this::getTUVBonus,
            this::resetTUVBonus);
      default:
        return null;
    }
  }
}
