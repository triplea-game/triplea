package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Attaches to technologies. Also contains static methods of interpreting data from all technology
 * attachments that a player has.
 */
public class TechAbilityAttachment extends DefaultAttachment {
  // unitAbilitiesGained Static Strings
  public static final String ABILITY_CAN_BLITZ = "canBlitz";
  public static final String ABILITY_CAN_BOMBARD = "canBombard";

  private static final long serialVersionUID = 1866305599625384294L;

  // attachment fields
  private IntegerMap<UnitType> attackBonus = new IntegerMap<>();
  private IntegerMap<UnitType> defenseBonus = new IntegerMap<>();
  private IntegerMap<UnitType> movementBonus = new IntegerMap<>();
  private IntegerMap<UnitType> radarBonus = new IntegerMap<>();
  private IntegerMap<UnitType> airAttackBonus = new IntegerMap<>();
  private IntegerMap<UnitType> airDefenseBonus = new IntegerMap<>();
  private IntegerMap<UnitType> productionBonus = new IntegerMap<>();
  // -1 means not set
  private int minimumTerritoryValueForProductionBonus = -1;
  // -1 means not set
  private int repairDiscount = -1;
  // -1 means not set
  private int warBondDiceSides = -1;
  private int warBondDiceNumber = 0;
  private IntegerMap<UnitType> rocketDiceNumber = new IntegerMap<>();
  private int rocketDistance = 0;
  private int rocketNumberPerTerritory = 1;
  private Map<UnitType, Set<String>> unitAbilitiesGained = new HashMap<>();
  private boolean airborneForces = false;
  private IntegerMap<UnitType> airborneCapacity = new IntegerMap<>();
  private Set<UnitType> airborneTypes = new HashSet<>();
  private int airborneDistance = 0;
  private Set<UnitType> airborneBases = new HashSet<>();
  private Map<String, Set<UnitType>> airborneTargetedByAa = new HashMap<>();
  private IntegerMap<UnitType> attackRollsBonus = new IntegerMap<>();
  private IntegerMap<UnitType> defenseRollsBonus = new IntegerMap<>();
  private IntegerMap<UnitType> bombingBonus = new IntegerMap<>();

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
  UnitType getUnitType(final String value) throws GameParseException {
    return Optional.ofNullable(getData().getUnitTypeList().getUnitType(value))
        .orElseThrow(() -> new GameParseException("No unit called:" + value + thisErrorMsg()));
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
    putter.accept(getUnitType(s[1]), getInt(s[0]));
  }

  @VisibleForTesting
  static int sumIntegerMap(
      final Function<TechAbilityAttachment, IntegerMap<UnitType>> mapper,
      final UnitType ut,
      final GamePlayer player,
      final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(mapper)
        .mapToInt(m -> m.getInt(ut))
        .sum();
  }

  @VisibleForTesting
  static int sumNumbers(
      final ToIntFunction<TechAbilityAttachment> mapper,
      final String attachmentType,
      final GamePlayer player,
      final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .filter(i -> i.getAttachedTo().toString().equals(attachmentType))
        .mapToInt(mapper)
        .filter(i -> i > 0)
        .sum();
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
    applyCheckedValue("attackBonus", value, attackBonus::put);
  }

  private void setAttackBonus(final IntegerMap<UnitType> value) {
    attackBonus = value;
  }

  private IntegerMap<UnitType> getAttackBonus() {
    return attackBonus;
  }

  static int getAttackBonus(final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getAttackBonus, ut, player, data);
  }

  private void resetAttackBonus() {
    attackBonus = new IntegerMap<>();
  }

  private void setDefenseBonus(final String value) throws GameParseException {
    applyCheckedValue("defenseBonus", value, defenseBonus::put);
  }

  private void setDefenseBonus(final IntegerMap<UnitType> value) {
    defenseBonus = value;
  }

  private IntegerMap<UnitType> getDefenseBonus() {
    return defenseBonus;
  }

  static int getDefenseBonus(final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getDefenseBonus, ut, player, data);
  }

  private void resetDefenseBonus() {
    defenseBonus = new IntegerMap<>();
  }

  private void setMovementBonus(final String value) throws GameParseException {
    applyCheckedValue("movementBonus", value, movementBonus::put);
  }

  private void setMovementBonus(final IntegerMap<UnitType> value) {
    movementBonus = value;
  }

  private IntegerMap<UnitType> getMovementBonus() {
    return movementBonus;
  }

  static int getMovementBonus(final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getMovementBonus, ut, player, data);
  }

  private void resetMovementBonus() {
    movementBonus = new IntegerMap<>();
  }

  private void setRadarBonus(final String value) throws GameParseException {
    applyCheckedValue("radarBonus", value, radarBonus::put);
  }

  private void setRadarBonus(final IntegerMap<UnitType> value) {
    radarBonus = value;
  }

  private IntegerMap<UnitType> getRadarBonus() {
    return radarBonus;
  }

  static int getRadarBonus(final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getRadarBonus, ut, player, data);
  }

  private void resetRadarBonus() {
    radarBonus = new IntegerMap<>();
  }

  private void setAirAttackBonus(final String value) throws GameParseException {
    applyCheckedValue("airAttackBonus", value, airAttackBonus::put);
  }

  private void setAirAttackBonus(final IntegerMap<UnitType> value) {
    airAttackBonus = value;
  }

  private IntegerMap<UnitType> getAirAttackBonus() {
    return airAttackBonus;
  }

  static int getAirAttackBonus(final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getAirAttackBonus, ut, player, data);
  }

  private void resetAirAttackBonus() {
    airAttackBonus = new IntegerMap<>();
  }

  private void setAirDefenseBonus(final String value) throws GameParseException {
    applyCheckedValue("airDefenseBonus", value, airDefenseBonus::put);
  }

  private void setAirDefenseBonus(final IntegerMap<UnitType> value) {
    airDefenseBonus = value;
  }

  private IntegerMap<UnitType> getAirDefenseBonus() {
    return airDefenseBonus;
  }

  static int getAirDefenseBonus(final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getAirDefenseBonus, ut, player, data);
  }

  private void resetAirDefenseBonus() {
    airDefenseBonus = new IntegerMap<>();
  }

  private void setProductionBonus(final String value) throws GameParseException {
    applyCheckedValue("productionBonus", value, productionBonus::put);
  }

  private void setProductionBonus(final IntegerMap<UnitType> value) {
    productionBonus = value;
  }

  private IntegerMap<UnitType> getProductionBonus() {
    return productionBonus;
  }

  public static int getProductionBonus(
      final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getProductionBonus, ut, player, data);
  }

  private void resetProductionBonus() {
    productionBonus = new IntegerMap<>();
  }

  private void setMinimumTerritoryValueForProductionBonus(final String value)
      throws GameParseException {
    minimumTerritoryValueForProductionBonus =
        getIntInRange("minimumTerritoryValueForProductionBonus", value, 10000, true);
  }

  private void setMinimumTerritoryValueForProductionBonus(final Integer value) {
    minimumTerritoryValueForProductionBonus = value;
  }

  private int getMinimumTerritoryValueForProductionBonus() {
    return minimumTerritoryValueForProductionBonus;
  }

  public static int getMinimumTerritoryValueForProductionBonus(
      final GamePlayer player, final GameData data) {
    return Math.max(
        0,
        TechTracker.getCurrentTechAdvances(player, data).stream()
            .map(TechAbilityAttachment::get)
            .filter(Objects::nonNull)
            .mapToInt(TechAbilityAttachment::getMinimumTerritoryValueForProductionBonus)
            .filter(i -> i != -1)
            .min()
            .orElse(-1));
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

  public static double getRepairDiscount(final GamePlayer player, final GameData data) {
    return Math.max(
        0,
        1.0
            - TechTracker.getCurrentTechAdvances(player, data).stream()
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

  public static int getWarBondDiceSides(final GamePlayer player, final GameData data) {
    return sumNumbers(
        TechAbilityAttachment::getWarBondDiceSides, TechAdvance.TECH_NAME_WAR_BONDS, player, data);
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

  public static int getWarBondDiceNumber(final GamePlayer player, final GameData data) {
    return sumNumbers(
        TechAbilityAttachment::getWarBondDiceNumber, TechAdvance.TECH_NAME_WAR_BONDS, player, data);
  }

  private void resetWarBondDiceNumber() {
    warBondDiceNumber = 0;
  }

  private void setRocketDiceNumber(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("rocketDiceNumber must have two fields" + thisErrorMsg());
    }
    rocketDiceNumber.put(getUnitType(s[1]), getInt(s[0]));
  }

  private void setRocketDiceNumber(final IntegerMap<UnitType> value) {
    rocketDiceNumber = value;
  }

  private IntegerMap<UnitType> getRocketDiceNumber() {
    return rocketDiceNumber;
  }

  private static int getRocketDiceNumber(
      final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getRocketDiceNumber, ut, player, data);
  }

  public static int getRocketDiceNumber(final Collection<Unit> rockets, final GameData data) {
    int rocketDiceNumber = 0;
    for (final Unit u : rockets) {
      rocketDiceNumber += getRocketDiceNumber(u.getType(), u.getOwner(), data);
    }
    return rocketDiceNumber;
  }

  private void resetRocketDiceNumber() {
    rocketDiceNumber = new IntegerMap<>();
  }

  private void setRocketDistance(final String value) throws GameParseException {
    rocketDistance = getIntInRange("rocketDistance", value, 100, false);
  }

  private void setRocketDistance(final Integer value) {
    rocketDistance = value;
  }

  private int getRocketDistance() {
    return rocketDistance;
  }

  public static int getRocketDistance(final GamePlayer player, final GameData data) {
    return sumNumbers(
        TechAbilityAttachment::getRocketDistance, TechAdvance.TECH_NAME_ROCKETS, player, data);
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

  private int getRocketNumberPerTerritory() {
    return rocketNumberPerTerritory;
  }

  public static int getRocketNumberPerTerritory(final GamePlayer player, final GameData data) {
    return sumNumbers(
        TechAbilityAttachment::getRocketNumberPerTerritory,
        TechAdvance.TECH_NAME_ROCKETS,
        player,
        data);
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
    final UnitType ut = getUnitType(unitType);
    final Set<String> abilities = unitAbilitiesGained.getOrDefault(ut, new HashSet<>());
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
      abilities.add(ability);
    }
    unitAbilitiesGained.put(ut, abilities);
  }

  private void setUnitAbilitiesGained(final Map<UnitType, Set<String>> value) {
    unitAbilitiesGained = value;
  }

  private Map<UnitType, Set<String>> getUnitAbilitiesGained() {
    return unitAbilitiesGained;
  }

  public static boolean getUnitAbilitiesGained(
      final String filterForAbility,
      final UnitType ut,
      final GamePlayer player,
      final GameData data) {
    Preconditions.checkNotNull(filterForAbility);
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getUnitAbilitiesGained)
        .map(m -> m.get(ut))
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .anyMatch(filterForAbility::equals);
  }

  private void resetUnitAbilitiesGained() {
    unitAbilitiesGained = new HashMap<>();
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
    applyCheckedValue("airborneCapacity", value, airborneCapacity::put);
  }

  private void setAirborneCapacity(final IntegerMap<UnitType> value) {
    airborneCapacity = value;
  }

  private IntegerMap<UnitType> getAirborneCapacity() {
    return airborneCapacity;
  }

  public static IntegerMap<UnitType> getAirborneCapacity(
      final GamePlayer player, final GameData data) {
    final IntegerMap<UnitType> capacityMap = new IntegerMap<>();
    for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data)) {
      final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
      if (taa != null) {
        capacityMap.add(taa.getAirborneCapacity());
      }
    }
    return capacityMap;
  }

  public static int getAirborneCapacity(
      final Collection<Unit> units, final GamePlayer player, final GameData data) {
    final IntegerMap<UnitType> capacityMap = getAirborneCapacity(player, data);
    int airborneCapacity = 0;
    for (final Unit u : units) {
      airborneCapacity += Math.max(0, (capacityMap.getInt(u.getType()) - u.getLaunched()));
    }
    return airborneCapacity;
  }

  private void resetAirborneCapacity() {
    airborneCapacity = new IntegerMap<>();
  }

  private void setAirborneTypes(final String value) throws GameParseException {
    for (final String unit : splitOnColon(value)) {
      airborneTypes.add(getUnitType(unit));
    }
  }

  private void setAirborneTypes(final Set<UnitType> value) {
    airborneTypes = value;
  }

  private Set<UnitType> getAirborneTypes() {
    return airborneTypes;
  }

  public static Set<UnitType> getAirborneTypes(final GamePlayer player, final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getAirborneTypes)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private void resetAirborneTypes() {
    airborneTypes = new HashSet<>();
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

  public static int getAirborneDistance(final GamePlayer player, final GameData data) {
    return Math.max(
        0,
        TechTracker.getCurrentTechAdvances(player, data).stream()
            .map(TechAbilityAttachment::get)
            .filter(Objects::nonNull)
            .mapToInt(TechAbilityAttachment::getAirborneDistance)
            .sum());
  }

  private void resetAirborneDistance() {
    airborneDistance = 0;
  }

  private void setAirborneBases(final String value) throws GameParseException {
    for (final String u : splitOnColon(value)) {
      airborneBases.add(getUnitType(u));
    }
  }

  private void setAirborneBases(final Set<UnitType> value) {
    airborneBases = value;
  }

  private Set<UnitType> getAirborneBases() {
    return airborneBases;
  }

  public static Set<UnitType> getAirborneBases(final GamePlayer player, final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getAirborneBases)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  private void resetAirborneBases() {
    airborneBases = new HashSet<>();
  }

  private void setAirborneTargettedByAa(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 2) {
      throw new GameParseException(
          "airborneTargettedByAA must have at least two fields" + thisErrorMsg());
    }
    final Set<UnitType> unitTypes = new HashSet<>();
    for (int i = 1; i < s.length; i++) {
      unitTypes.add(getUnitType(s[i]));
    }
    airborneTargetedByAa.put(s[0], unitTypes);
  }

  private void setAirborneTargettedByAa(final Map<String, Set<UnitType>> value) {
    airborneTargetedByAa = value;
  }

  private Map<String, Set<UnitType>> getAirborneTargettedByAa() {
    return airborneTargetedByAa;
  }

  /**
   * Returns a map of the air unit types that can be targeted by each type of AA fire. The key is
   * the type of AA fire. The value is the collection of air unit types that can be targeted by the
   * key.
   */
  public static Map<String, Set<UnitType>> getAirborneTargettedByAa(
      final GamePlayer player, final GameData data) {
    final Map<String, Set<UnitType>> airborneTargettedByAa = new HashMap<>();
    for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data)) {
      final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
      if (taa != null) {
        final Map<String, Set<UnitType>> mapAa = taa.getAirborneTargettedByAa();
        if (mapAa != null && !mapAa.isEmpty()) {
          for (final Entry<String, Set<UnitType>> entry : mapAa.entrySet()) {
            final Set<UnitType> current =
                airborneTargettedByAa.getOrDefault(entry.getKey(), new HashSet<>());
            current.addAll(entry.getValue());
            airborneTargettedByAa.put(entry.getKey(), current);
          }
        }
      }
    }
    return airborneTargettedByAa;
  }

  private void resetAirborneTargettedByAa() {
    airborneTargetedByAa = new HashMap<>();
  }

  private void setAttackRollsBonus(final String value) throws GameParseException {
    applyCheckedValue("attackRollsBonus", value, attackRollsBonus::put);
  }

  private void setAttackRollsBonus(final IntegerMap<UnitType> value) {
    attackRollsBonus = value;
  }

  private IntegerMap<UnitType> getAttackRollsBonus() {
    return attackRollsBonus;
  }

  static int getAttackRollsBonus(final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getAttackRollsBonus, ut, player, data);
  }

  private void resetAttackRollsBonus() {
    attackRollsBonus = new IntegerMap<>();
  }

  private void setDefenseRollsBonus(final String value) throws GameParseException {
    applyCheckedValue("defenseRollsBonus", value, defenseRollsBonus::put);
  }

  private void setDefenseRollsBonus(final IntegerMap<UnitType> value) {
    defenseRollsBonus = value;
  }

  private IntegerMap<UnitType> getDefenseRollsBonus() {
    return defenseRollsBonus;
  }

  static int getDefenseRollsBonus(final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getDefenseRollsBonus, ut, player, data);
  }

  private void setBombingBonus(final String value) throws GameParseException {
    applyCheckedValue("bombingBonus", value, bombingBonus::put);
  }

  private void setBombingBonus(final IntegerMap<UnitType> value) {
    bombingBonus = value;
  }

  private IntegerMap<UnitType> getBombingBonus() {
    return bombingBonus;
  }

  public static int getBombingBonus(
      final UnitType ut, final GamePlayer player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getBombingBonus, ut, player, data);
  }

  private void resetDefenseRollsBonus() {
    defenseRollsBonus = new IntegerMap<>();
  }

  private void resetBombingBonus() {
    bombingBonus = new IntegerMap<>();
  }

  public static boolean getAllowAirborneForces(final GamePlayer player, final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
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
    for (final TechAdvance techAdvance : TechAdvance.getTechAdvances(data)) {
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
                      - UnitAttachment.get(bomber).getAttackRolls(GamePlayer.NULL_PLAYERID);
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
        // through support
        // attachments
        // paratroopers = because it is already completely atomized and controlled through unit
        // attachments + game
        // options
        // mechanizedInfantry = because it is already completely atomized and controlled through
        // unit attachments
        // IF one of the above named techs changes what it does in a future version of a&a, and the
        // change is large
        // enough or different enough that it cannot be done easily with a new game option,
        // then it is better to create a new tech rather than change the old one, and give the new
        // one a new name, like
        // paratroopers2 or paratroopersAttack or Airborne_Forces, or some crap.
      }
    }
  }

  // validator
  @Override
  public void validate(final GameData data) throws GameParseException {
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
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put(
            "attackBonus",
            MutableProperty.of(
                this::setAttackBonus,
                this::setAttackBonus,
                this::getAttackBonus,
                this::resetAttackBonus))
        .put(
            "defenseBonus",
            MutableProperty.of(
                this::setDefenseBonus,
                this::setDefenseBonus,
                this::getDefenseBonus,
                this::resetDefenseBonus))
        .put(
            "movementBonus",
            MutableProperty.of(
                this::setMovementBonus,
                this::setMovementBonus,
                this::getMovementBonus,
                this::resetMovementBonus))
        .put(
            "radarBonus",
            MutableProperty.of(
                this::setRadarBonus,
                this::setRadarBonus,
                this::getRadarBonus,
                this::resetRadarBonus))
        .put(
            "airAttackBonus",
            MutableProperty.of(
                this::setAirAttackBonus,
                this::setAirAttackBonus,
                this::getAirAttackBonus,
                this::resetAirAttackBonus))
        .put(
            "airDefenseBonus",
            MutableProperty.of(
                this::setAirDefenseBonus,
                this::setAirDefenseBonus,
                this::getAirDefenseBonus,
                this::resetAirDefenseBonus))
        .put(
            "productionBonus",
            MutableProperty.of(
                this::setProductionBonus,
                this::setProductionBonus,
                this::getProductionBonus,
                this::resetProductionBonus))
        .put(
            "minimumTerritoryValueForProductionBonus",
            MutableProperty.of(
                this::setMinimumTerritoryValueForProductionBonus,
                this::setMinimumTerritoryValueForProductionBonus,
                this::getMinimumTerritoryValueForProductionBonus,
                this::resetMinimumTerritoryValueForProductionBonus))
        .put(
            "repairDiscount",
            MutableProperty.of(
                this::setRepairDiscount,
                this::setRepairDiscount,
                this::getRepairDiscount,
                this::resetRepairDiscount))
        .put(
            "warBondDiceSides",
            MutableProperty.of(
                this::setWarBondDiceSides,
                this::setWarBondDiceSides,
                this::getWarBondDiceSides,
                this::resetWarBondDiceSides))
        .put(
            "warBondDiceNumber",
            MutableProperty.of(
                this::setWarBondDiceNumber,
                this::setWarBondDiceNumber,
                this::getWarBondDiceNumber,
                this::resetWarBondDiceNumber))
        .put(
            "rocketDiceNumber",
            MutableProperty.of(
                this::setRocketDiceNumber,
                this::setRocketDiceNumber,
                this::getRocketDiceNumber,
                this::resetRocketDiceNumber))
        .put(
            "rocketDistance",
            MutableProperty.of(
                this::setRocketDistance,
                this::setRocketDistance,
                this::getRocketDistance,
                this::resetRocketDistance))
        .put(
            "rocketNumberPerTerritory",
            MutableProperty.of(
                this::setRocketNumberPerTerritory,
                this::setRocketNumberPerTerritory,
                this::getRocketNumberPerTerritory,
                this::resetRocketNumberPerTerritory))
        .put(
            "unitAbilitiesGained",
            MutableProperty.of(
                this::setUnitAbilitiesGained,
                this::setUnitAbilitiesGained,
                this::getUnitAbilitiesGained,
                this::resetUnitAbilitiesGained))
        .put(
            "airborneForces",
            MutableProperty.of(
                this::setAirborneForces,
                this::setAirborneForces,
                this::getAirborneForces,
                this::resetAirborneForces))
        .put(
            "airborneCapacity",
            MutableProperty.of(
                this::setAirborneCapacity,
                this::setAirborneCapacity,
                this::getAirborneCapacity,
                this::resetAirborneCapacity))
        .put(
            "airborneTypes",
            MutableProperty.of(
                this::setAirborneTypes,
                this::setAirborneTypes,
                this::getAirborneTypes,
                this::resetAirborneTypes))
        .put(
            "airborneDistance",
            MutableProperty.of(
                this::setAirborneDistance,
                this::setAirborneDistance,
                this::getAirborneDistance,
                this::resetAirborneDistance))
        .put(
            "airborneBases",
            MutableProperty.of(
                this::setAirborneBases,
                this::setAirborneBases,
                this::getAirborneBases,
                this::resetAirborneBases))
        .put(
            "airborneTargettedByAA",
            MutableProperty.of(
                this::setAirborneTargettedByAa,
                this::setAirborneTargettedByAa,
                this::getAirborneTargettedByAa,
                this::resetAirborneTargettedByAa))
        .put(
            "attackRollsBonus",
            MutableProperty.of(
                this::setAttackRollsBonus,
                this::setAttackRollsBonus,
                this::getAttackRollsBonus,
                this::resetAttackRollsBonus))
        .put(
            "defenseRollsBonus",
            MutableProperty.of(
                this::setDefenseRollsBonus,
                this::setDefenseRollsBonus,
                this::getDefenseRollsBonus,
                this::resetDefenseRollsBonus))
        .put(
            "bombingBonus",
            MutableProperty.of(
                this::setBombingBonus,
                this::setBombingBonus,
                this::getBombingBonus,
                this::resetBombingBonus))
        .build();
  }
}
