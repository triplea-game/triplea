package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.DefaultNamed;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.UnitUtils;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.ChangeOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Despite the misleading name, this attaches not to individual Units but to UnitTypes. Note: Empty
 * collection fields default to null to minimize memory use and serialization size.
 */
public class UnitAttachment extends DefaultAttachment {
  @NonNls public static final String UNITS_MAY_NOT_LAND_ON_CARRIER = "unitsMayNotLandOnCarrier";

  @NonNls
  public static final String UNITS_MAY_NOT_LEAVE_ALLIED_CARRIER = "unitsMayNotLeaveAlliedCarrier";

  @NonNls public static final String IS_SEA = "isSea";
  @NonNls public static final String DEFENSE_STRENGTH = "defense";
  @NonNls public static final String ATTACK_STRENGTH = "attack";
  @NonNls public static final String ATTACK_ROLL = "attackRolls";
  @NonNls public static final String DEFENSE_ROLL = "defenseRolls";
  @NonNls public static final String ATTACK_AA = "attackAA";
  @NonNls public static final String OFFENSIVE_ATTACK_AA = "offensiveAttackAA";
  @NonNls public static final String MAX_AA_ATTACKS = "maxAAattacks";
  @NonNls public static final String ATTACK_AA_MAX_DIE_SIDES = "attackAAmaxDieSides";

  @NonNls
  public static final String OFFENSIVE_ATTACK_AA_MAX_DIE_SIDES = "offensiveAttackAAmaxDieSides";

  @NonNls public static final String MAY_OVER_STACK_AA = "mayOverStackAA";
  @NonNls public static final String IS_MARINE = "isMarine";
  @NonNls public static final String BOMBARD = "bombard";
  @NonNls public static final String CHOOSE_BEST_ROLL = "chooseBestRoll";

  private static final long serialVersionUID = -2946748686268541820L;

  // movement related
  @Accessors(fluent = true)
  @Getter
  private boolean isAir = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isSea = false;

  private int movement = 0;
  private boolean canBlitz = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isKamikaze = false;

  // a colon delimited list of transports where this unit may invade from, it supports "none"
  // and if empty it allows you to invade from all
  private @Nullable String[] canInvadeOnlyFrom = null;
  private @Nullable IntegerMap<Resource> fuelCost = null;
  private @Nullable IntegerMap<Resource> fuelFlatCost = null;

  @Accessors(fluent = true)
  @Getter
  private boolean canNotMoveDuringCombatMove = false;

  private @Nullable Tuple<Integer, String> movementLimit = null;

  // combat related
  private int attack = 0;
  private int defense = 0;

  @Accessors(fluent = true)
  @Getter
  private boolean isInfrastructure = false;

  @Accessors(fluent = true)
  @Getter
  private boolean canBombard = false;

  private int bombard = -1;
  private boolean artillery = false;
  private boolean artillerySupportable = false;
  private int unitSupportCount = -1;
  @Getter private int isMarine = 0;
  private boolean isSuicideOnAttack = false;
  private boolean isSuicideOnDefense = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isSuicideOnHit = false;

  private @Nullable Tuple<Integer, String> attackingLimit = null;
  private int attackRolls = 1;
  private int defenseRolls = 1;
  private boolean chooseBestRoll = false;
  private @Nullable Boolean canRetreatOnStalemate;

  // sub/destroyer related
  @Accessors(fluent = true)
  @Getter
  private boolean canEvade = false;

  private boolean isFirstStrike = false;
  private @Nullable Set<UnitType> canNotTarget = null;
  private @Nullable Set<UnitType> canNotBeTargetedBy = null;
  private boolean canMoveThroughEnemies = false;

  @Accessors(fluent = true)
  @Getter
  private boolean canBeMovedThroughByEnemies = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isDestroyer = false;

  // transportation related
  @Accessors(fluent = true)
  @Getter
  private boolean isCombatTransport = false;

  // -1 if cant transport
  @Getter private int transportCapacity = -1;
  // -1 if cant be transported
  @Getter private int transportCost = -1;
  // -1 if cant act as a carrier
  @Getter private int carrierCapacity = -1;
  // -1 if cant land on a carrier
  @Getter private int carrierCost = -1;

  @Accessors(fluent = true)
  @Getter
  private boolean isAirTransport = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isAirTransportable = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isLandTransport = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isLandTransportable = false;

  // aa related
  // "isAA" and "isAAmovement" are also valid setters, used as shortcuts for calling multiple aa
  // related setters. Must keep.
  @Accessors(fluent = true)
  @Getter
  private boolean isAaForCombatOnly = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isAaForBombingThisUnitOnly = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isAaForFlyOverOnly = false;

  @Accessors(fluent = true)
  @Getter
  private boolean isRocket = false;

  private int attackAa = 1;
  private int offensiveAttackAa = 0;
  private int attackAaMaxDieSides = -1;
  private int offensiveAttackAaMaxDieSides = -1;
  // -1 means infinite
  @Getter private int maxAaAttacks = -1;
  // -1 means infinite
  @Getter private int maxRoundsAa = 1;
  // default value for when it is not set
  @Getter private String typeAa = "AA";
  // null means targeting air units only
  private @Nullable Set<UnitType> targetsAa = null;
  // if false, we cannot shoot more times than there are number of planes
  private boolean mayOverStackAa = false;
  // if false, we instantly kill anything our AA shot hits
  private boolean damageableAa = false;
  // if these enemy units are present, the gun does not fire at all
  private @Nullable Set<UnitType> willNotFireIfPresent = null;

  // strategic bombing related
  @Accessors(fluent = true)
  @Getter
  private boolean isStrategicBomber = false;

  @Getter private int bombingMaxDieSides = -1;
  @Getter private int bombingBonus = 0;

  @Accessors(fluent = true)
  @Getter
  private boolean canIntercept = false;

  private boolean requiresAirBaseToIntercept = false;

  @Accessors(fluent = true)
  @Getter
  private boolean canEscort = false;

  @Accessors(fluent = true)
  @Getter
  private boolean canAirBattle = false;

  private int airDefense = 0;
  private int airAttack = 0;
  // null means they can target any unit that can be damaged
  private @Nullable Set<UnitType> bombingTargets = null;

  // production related
  // this has been split into canProduceUnits, isConstruction, canBeDamaged, and isInfrastructure
  @Accessors(fluent = true)
  @Getter
  private boolean canProduceUnits = false;

  // -1 means either it can't produce any, or it produces at the value of the territory it is
  // located in
  @Getter private int canProduceXUnits = -1;
  private @Nullable IntegerMap<UnitType> createsUnitsList = null;
  private @Nullable IntegerMap<Resource> createsResourcesList = null;

  // damage related
  @Getter private int hitPoints = 1;

  @Accessors(fluent = true)
  @Getter
  private boolean canBeDamaged = false;

  // this is bombing damage, not hit points. default of 2 means that factories will take 2x the
  // territory value they are in, of damage.
  @Getter private int maxDamage = 2;
  // -1 for can't be disabled
  @Getter private int maxOperationalDamage = -1;

  @Accessors(fluent = true)
  @Getter
  private boolean canDieFromReachingMaxDamage = false;

  // placement related
  @Getter private boolean isConstruction = false;
  // can be any String except for "none" if isConstruction is true
  @Getter private String constructionType = "none";
  // -1 if not set, is meaningless
  @Getter private int constructionsPerTerrPerTypePerTurn = -1;
  // -1 if not set, is meaningless
  @Getter private int maxConstructionsPerTypePerTerr = -1;
  // -1 means anywhere
  @Getter private int canOnlyBePlacedInTerritoryValuedAtX = -1;
  // multiple colon delimited lists of the unit combos required for this unit to be built somewhere.
  // (units must be in the same territory, owned by player, not be disabled)
  private @Nullable List<String[]> requiresUnits = null;
  private @Nullable IntegerMap<UnitType> consumesUnits = null;
  // multiple colon delimited lists of the unit combos required for
  // this unit to move into a territory. (units must be owned by player, not be disabled)
  private @Nullable List<String[]> requiresUnitsToMove = null;
  // a colon delimited list of territories where this unit may not be placed
  // also an allowed setter is "setUnitPlacementOnlyAllowedIn",
  // which just creates unitPlacementRestrictions with an inverted list of territories
  @Getter private @Nullable String[] unitPlacementRestrictions = null;
  // -1 if infinite (infinite is default)
  @Getter private int maxBuiltPerPlayer = -1;
  private @Nullable Tuple<Integer, String> placementLimit = null;

  // scrambling related
  @Accessors(fluent = true)
  @Getter
  private boolean canScramble = false;

  @Getter private boolean isAirBase = false;
  // -1 for can't scramble
  @Getter private int maxScrambleDistance = -1;
  // -1 for infinite
  @Getter private int maxScrambleCount = -1;
  // -1 for infinite
  @Getter private int maxInterceptCount = -1;

  // special abilities
  @Getter private int blockade = 0;
  // a colon delimited list of the units this unit can repair.
  // (units must be in same territory, unless this unit is land and the repaired unit is sea)
  private @Nullable IntegerMap<UnitType> repairsUnits = null;
  private @Nullable IntegerMap<UnitType> givesMovement = null;
  private @Nullable List<Tuple<String, GamePlayer>> destroyedWhenCapturedBy = null;
  // also an allowed setter is "setDestroyedWhenCapturedFrom" which will just create
  // destroyedWhenCapturedBy with a specific list
  private @Nullable Map<Integer, Tuple<Boolean, UnitType>> whenHitPointsDamagedChangesInto = null;
  private @Nullable Map<Integer, Tuple<Boolean, UnitType>> whenHitPointsRepairedChangesInto = null;
  private @Nullable Map<String, Tuple<String, IntegerMap<UnitType>>> whenCapturedChangesInto = null;
  @Getter private int whenCapturedSustainsDamage = 0;
  private @Nullable List<GamePlayer> canBeCapturedOnEnteringBy = null;
  private @Nullable List<GamePlayer> canBeGivenByTerritoryTo = null;

  // a set of information for dealing with special abilities or loss of abilities when a unit takes
  // x-y amount of damage
  @ChangeOnNextMajorRelease("This should be a list of WhenCombatDamaged objects instead of Tuples")
  private @Nullable List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> whenCombatDamaged =
      null;

  // a kind of support attachment for giving actual unit attachment abilities or other to a unit,
  // when in the presence or on the same route with another unit
  private @Nullable List<String> receivesAbilityWhenWith = null;
  // currently used for: placement in original territories only
  private @Nullable Set<String> special = null;
  // Manually set TUV
  @Getter private int tuv = -1;

  // combo properties
  private boolean isSub = false;
  private boolean isSuicide = false;

  public UnitAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static UnitAttachment get(final UnitType type, final String nameOfAttachment) {
    return getAttachment(type, nameOfAttachment, UnitAttachment.class);
  }

  private TechTracker getTechTracker() {
    return getData().getTechTracker();
  }

  private UnitType getUnitType() {
    return (UnitType) getAttachedTo();
  }

  private void setCanIntercept(final String value) {
    canIntercept = getBool(value);
  }

  private void setCanIntercept(final Boolean value) {
    canIntercept = value;
  }

  private void resetCanIntercept() {
    canIntercept = false;
  }

  private void setRequiresAirBaseToIntercept(final String value) {
    requiresAirBaseToIntercept = getBool(value);
  }

  private void setRequiresAirBaseToIntercept(final Boolean value) {
    requiresAirBaseToIntercept = value;
  }

  public boolean getRequiresAirBaseToIntercept() {
    return requiresAirBaseToIntercept;
  }

  private void resetRequiresAirBaseToIntercept() {
    requiresAirBaseToIntercept = false;
  }

  private void setCanEscort(final String value) {
    canEscort = getBool(value);
  }

  private void setCanEscort(final Boolean value) {
    canEscort = value;
  }

  private void resetCanEscort() {
    canEscort = false;
  }

  private void setCanAirBattle(final String value) {
    canAirBattle = getBool(value);
  }

  private void setCanAirBattle(final Boolean value) {
    canAirBattle = value;
  }

  private void resetCanAirBattle() {
    canAirBattle = false;
  }

  private void setAirDefense(final String value) {
    airDefense = getInt(value);
  }

  @VisibleForTesting
  public void setAirDefense(final Integer value) {
    airDefense = value;
  }

  private int getAirDefense() {
    return airDefense;
  }

  public int getAirDefense(final GamePlayer player) {
    final int bonus = getTechTracker().getAirDefenseBonus(player, getUnitType());
    return Math.min(getData().getDiceSides(), Math.max(0, airDefense + bonus));
  }

  private void resetAirDefense() {
    airDefense = 0;
  }

  private void setAirAttack(final String value) {
    airAttack = getInt(value);
  }

  @VisibleForTesting
  public void setAirAttack(final Integer value) {
    airAttack = value;
  }

  private int getAirAttack() {
    return airAttack;
  }

  public int getAirAttack(final GamePlayer player) {
    final int bonus = getTechTracker().getAirAttackBonus(player, getUnitType());
    return Math.min(getData().getDiceSides(), Math.max(0, airAttack + bonus));
  }

  private void resetAirAttack() {
    airAttack = 0;
  }

  private void setIsAirTransport(final String s) {
    isAirTransport = getBool(s);
  }

  private void setIsAirTransport(final Boolean s) {
    isAirTransport = s;
  }

  private void resetIsAirTransport() {
    isAirTransport = false;
  }

  private void setIsAirTransportable(final String s) {
    isAirTransportable = getBool(s);
  }

  private void setIsAirTransportable(final Boolean s) {
    isAirTransportable = s;
  }

  private void resetIsAirTransportable() {
    isAirTransportable = false;
  }

  private void setCanBeGivenByTerritoryTo(final String value) throws GameParseException {
    if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
      canBeGivenByTerritoryTo = null;
    } else {
      canBeGivenByTerritoryTo = parsePlayerList(value, canBeGivenByTerritoryTo);
    }
  }

  private void setCanBeGivenByTerritoryTo(final List<GamePlayer> value) {
    canBeGivenByTerritoryTo = value;
  }

  public List<GamePlayer> getCanBeGivenByTerritoryTo() {
    return getListProperty(canBeGivenByTerritoryTo);
  }

  private void resetCanBeGivenByTerritoryTo() {
    canBeGivenByTerritoryTo = null;
  }

  private void setCanBeCapturedOnEnteringBy(final String value) throws GameParseException {
    canBeCapturedOnEnteringBy = parsePlayerList(value, canBeCapturedOnEnteringBy);
  }

  private void setCanBeCapturedOnEnteringBy(final List<GamePlayer> value) {
    canBeCapturedOnEnteringBy = value;
  }

  public List<GamePlayer> getCanBeCapturedOnEnteringBy() {
    return getListProperty(canBeCapturedOnEnteringBy);
  }

  private void resetCanBeCapturedOnEnteringBy() {
    canBeCapturedOnEnteringBy = null;
  }

  private void setWhenHitPointsDamagedChangesInto(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 3) {
      throw new GameParseException(
          "setWhenHitPointsDamagedChangesInto must have damage:translateAttributes:unitType "
              + thisErrorMsg());
    }
    final UnitType unitType = getUnitTypeOrThrow(s[2]);
    if (whenHitPointsDamagedChangesInto == null) {
      whenHitPointsDamagedChangesInto = new HashMap<>();
    }
    whenHitPointsDamagedChangesInto.put(getInt(s[0]), Tuple.of(getBool(s[1]), unitType));
  }

  private void setWhenHitPointsDamagedChangesInto(
      final Map<Integer, Tuple<Boolean, UnitType>> value) {
    whenHitPointsDamagedChangesInto = value;
  }

  public Map<Integer, Tuple<Boolean, UnitType>> getWhenHitPointsDamagedChangesInto() {
    return getMapProperty(whenHitPointsDamagedChangesInto);
  }

  private void resetWhenHitPointsDamagedChangesInto() {
    whenHitPointsDamagedChangesInto = null;
  }

  private void setWhenHitPointsRepairedChangesInto(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 3) {
      throw new GameParseException(
          "setWhenHitPointsRepairedChangesInto must have damage:translateAttributes:unitType "
              + thisErrorMsg());
    }
    final UnitType unitType = getUnitTypeOrThrow(s[2]);
    if (whenHitPointsRepairedChangesInto == null) {
      whenHitPointsRepairedChangesInto = new HashMap<>();
    }
    whenHitPointsRepairedChangesInto.put(getInt(s[0]), Tuple.of(getBool(s[1]), unitType));
  }

  private void setWhenHitPointsRepairedChangesInto(
      final Map<Integer, Tuple<Boolean, UnitType>> value) {
    whenHitPointsRepairedChangesInto = value;
  }

  public Map<Integer, Tuple<Boolean, UnitType>> getWhenHitPointsRepairedChangesInto() {
    return getMapProperty(whenHitPointsRepairedChangesInto);
  }

  private void resetWhenHitPointsRepairedChangesInto() {
    whenHitPointsRepairedChangesInto = null;
  }

  @VisibleForTesting
  void setWhenCapturedChangesInto(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 5 || s.length % 2 == 0) {
      throw new GameParseException(
          "whenCapturedChangesInto must have 5 or more values, "
              + "playerFrom:playerTo:keepAttributes:unitType:howMany "
              + "(you may have additional unitType:howMany:unitType:howMany, etc"
              + thisErrorMsg());
    }
    final GamePlayer pfrom = getData().getPlayerList().getPlayerId(s[0]);
    if (pfrom == null && !s[0].equals("any")) {
      throw new GameParseException(
          "whenCapturedChangesInto: No player named: " + s[0] + thisErrorMsg());
    }
    final GamePlayer pto = getData().getPlayerList().getPlayerId(s[1]);
    if (pto == null && !s[1].equals("any")) {
      throw new GameParseException(
          "whenCapturedChangesInto: No player named: " + s[1] + thisErrorMsg());
    }
    getBool(s[2]);
    final IntegerMap<UnitType> unitsToMake = new IntegerMap<>();
    for (int i = 3; i < s.length; i += 2) {
      unitsToMake.put(getUnitTypeOrThrow(s[i]), getInt(s[i + 1]));
    }
    if (whenCapturedChangesInto == null) {
      whenCapturedChangesInto = new LinkedHashMap<>();
    }
    whenCapturedChangesInto.put((s[0] + ":" + s[1]).intern(), Tuple.of(s[2].intern(), unitsToMake));
  }

  private void setWhenCapturedChangesInto(
      final Map<String, Tuple<String, IntegerMap<UnitType>>> value) {
    whenCapturedChangesInto = value;
  }

  public Map<String, Tuple<String, IntegerMap<UnitType>>> getWhenCapturedChangesInto() {
    return getMapProperty(whenCapturedChangesInto);
  }

  private void resetWhenCapturedChangesInto() {
    whenCapturedChangesInto = null;
  }

  private void setWhenCapturedSustainsDamage(final int s) {
    whenCapturedSustainsDamage = s;
  }

  private void setDestroyedWhenCapturedBy(final String initialValue) throws GameParseException {
    // We can prefix this value with "BY" or "FROM" to change the setting. If no setting, default to
    // "BY" since this is called by destroyedWhenCapturedBy
    String value = initialValue;
    String byOrFrom = "BY";
    if (value.startsWith("BY:") && getData().getPlayerList().getPlayerId("BY") == null) {
      value = value.replaceFirst("BY:", "");
    } else if (value.startsWith("FROM:") && getData().getPlayerList().getPlayerId("FROM") == null) {
      byOrFrom = "FROM";
      value = value.replaceFirst("FROM:", "");
    }
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      if (destroyedWhenCapturedBy == null) {
        destroyedWhenCapturedBy = new ArrayList<>();
      }
      destroyedWhenCapturedBy.add(Tuple.of(byOrFrom.intern(), getPlayerOrThrow(name)));
    }
  }

  private void setDestroyedWhenCapturedBy(final List<Tuple<String, GamePlayer>> value) {
    destroyedWhenCapturedBy = value;
  }

  private void setDestroyedWhenCapturedFrom(final String initialValue) throws GameParseException {
    String value = initialValue;
    if (!(value.startsWith("BY:") || value.startsWith("FROM:"))) {
      value = "FROM:" + value;
    }
    setDestroyedWhenCapturedBy(value);
  }

  public List<Tuple<String, GamePlayer>> getDestroyedWhenCapturedBy() {
    return getListProperty(destroyedWhenCapturedBy);
  }

  private void resetDestroyedWhenCapturedBy() {
    destroyedWhenCapturedBy = null;
  }

  private void setCanBlitz(final String s) {
    canBlitz = getBool(s);
  }

  private void setCanBlitz(final Boolean s) {
    canBlitz = s;
  }

  private boolean getCanBlitz() {
    return canBlitz;
  }

  public boolean getCanBlitz(final GamePlayer player) {
    return canBlitz || getTechTracker().canBlitz(player, getUnitType());
  }

  private void resetCanBlitz() {
    canBlitz = false;
  }

  private void setIsSub(final String s) {
    setIsSub(getBool(s));
  }

  @VisibleForTesting
  public void setIsSub(final Boolean s) {
    isSub = s;
    resetCanNotTarget();
    resetCanNotBeTargetedBy();
  }

  private void setCanEvade(final Boolean s) {
    canEvade = s;
  }

  public boolean getCanEvade() {
    return canEvade || isSub;
  }

  @VisibleForTesting
  public void setIsFirstStrike(final Boolean s) {
    isFirstStrike = s;
  }

  public boolean getIsFirstStrike() {
    return isFirstStrike || isSub || isSuicide;
  }

  private void setCanMoveThroughEnemies(final Boolean s) {
    canMoveThroughEnemies = s;
  }

  public boolean getCanMoveThroughEnemies() {
    return canMoveThroughEnemies
        || (isSub && Properties.getSubmersibleSubs(getData().getProperties()));
  }

  private void setCanBeMovedThroughByEnemies(final Boolean s) {
    canBeMovedThroughByEnemies = s;
  }

  public boolean getCanBeMovedThroughByEnemies() {
    return canBeMovedThroughByEnemies
        || (isSub && Properties.getIgnoreSubInMovement(getData().getProperties()));
  }

  private void setCanNotTarget(final String value) throws GameParseException {
    if (isSub || isSuicide) {
      throw new GameParseException(
          "Can't use canNotTarget with isSub/isSuicide, replace isSub with individual sub "
              + "properties or isSuicide with isSuicideOnAttack/isSuicideOnDefense: "
              + thisErrorMsg());
    }
    canNotTarget = parseUnitTypes("canNotTarget", value, canNotTarget);
  }

  @VisibleForTesting
  public UnitAttachment setCanNotTarget(final Set<UnitType> value) {
    canNotTarget = value;
    return this;
  }

  public Set<UnitType> getCanNotTarget() {
    if (canNotTarget == null && (isSub || isSuicide)) {
      final Predicate<UnitType> unitTypeMatch =
          (getIsSuicideOnAttack() && getIsFirstStrike())
              ? Matches.unitTypeIsSuicideOnAttack().or(Matches.unitTypeIsSuicideOnDefense())
              : Matches.unitTypeIsAir();
      canNotTarget =
          new HashSet<>(
              CollectionUtils.getMatches(
                  getData().getUnitTypeList().getAllUnitTypes(), unitTypeMatch));
    }
    return getSetProperty(canNotTarget);
  }

  private void resetCanNotTarget() {
    canNotTarget = null;
  }

  private void setCanNotBeTargetedBy(final String value) throws GameParseException {
    canNotBeTargetedBy = parseUnitTypes("canNotBeTargetedBy", value, canNotBeTargetedBy);
  }

  @VisibleForTesting
  public void setCanNotBeTargetedBy(final Set<UnitType> value) {
    canNotBeTargetedBy = value;
  }

  public Set<UnitType> getCanNotBeTargetedBy() {
    if (canNotBeTargetedBy == null && isSub) {
      canNotBeTargetedBy =
          Properties.getAirAttackSubRestricted(getData().getProperties())
              ? new HashSet<>(
                  CollectionUtils.getMatches(
                      getData().getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAir()))
              : new HashSet<>();
    }
    return getSetProperty(canNotBeTargetedBy);
  }

  private void resetCanNotBeTargetedBy() {
    canNotBeTargetedBy = null;
  }

  private void setIsCombatTransport(final String s) {
    isCombatTransport = getBool(s);
  }

  private void setIsCombatTransport(final Boolean s) {
    isCombatTransport = s;
  }

  private void resetIsCombatTransport() {
    isCombatTransport = false;
  }

  private void setIsStrategicBomber(final String s) {
    isStrategicBomber = getBool(s);
  }

  private void setIsStrategicBomber(final Boolean s) {
    isStrategicBomber = s;
  }

  private void resetIsStrategicBomber() {
    isStrategicBomber = false;
  }

  private void setIsDestroyer(final String s) {
    isDestroyer = getBool(s);
  }

  @VisibleForTesting
  public void setIsDestroyer(final Boolean s) {
    isDestroyer = s;
  }

  private void resetIsDestroyer() {
    isDestroyer = false;
  }

  public void setCanBombard(final String s) {
    canBombard = getBool(s);
  }

  @VisibleForTesting
  public void setCanBombard(final Boolean s) {
    canBombard = s;
  }

  private boolean getCanBombard() {
    return canBombard;
  }

  public boolean getCanBombard(final GamePlayer player) {
    return canBombard || getTechTracker().canBombard(player, getUnitType());
  }

  private void resetCanBombard() {
    canBombard = false;
  }

  private void setIsAir(final String s) {
    isAir = getBool(s);
  }

  @VisibleForTesting
  public void setIsAir(final Boolean s) {
    isAir = s;
  }

  private void resetIsAir() {
    isAir = false;
  }

  private void setIsSea(final String s) {
    isSea = getBool(s);
  }

  @VisibleForTesting
  public UnitAttachment setIsSea(final Boolean s) {
    isSea = s;
    return this;
  }

  private void resetIsSea() {
    isSea = false;
  }

  private void setIsFactory(final String s) {
    setIsFactory(getBool(s));
  }

  private void setIsFactory(final Boolean s) {
    setCanBeDamaged(s);
    setIsInfrastructure(s);
    setCanProduceUnits(s);
    setIsConstruction(s);
    if (Boolean.TRUE.equals(s)) {
      setConstructionType(Constants.CONSTRUCTION_TYPE_FACTORY);
      setMaxConstructionsPerTypePerTerr("1");
      setConstructionsPerTerrPerTypePerTurn("1");
    } else {
      // return to defaults
      setConstructionType("none");
      setMaxConstructionsPerTypePerTerr("-1");
      setConstructionsPerTerrPerTypePerTurn("-1");
    }
  }

  private void setCanProduceUnits(final String s) {
    canProduceUnits = getBool(s);
  }

  private void setCanProduceUnits(final Boolean s) {
    canProduceUnits = s;
  }

  private void resetCanProduceUnits() {
    canProduceUnits = false;
  }

  private void setCanProduceXUnits(final String s) {
    canProduceXUnits = getInt(s);
  }

  private void setCanProduceXUnits(final Integer s) {
    canProduceXUnits = s;
  }

  private void resetCanProduceXUnits() {
    canProduceXUnits = -1;
  }

  private void setCanOnlyBePlacedInTerritoryValuedAtX(final String s) {
    canOnlyBePlacedInTerritoryValuedAtX = getInt(s);
  }

  private void setCanOnlyBePlacedInTerritoryValuedAtX(final Integer s) {
    canOnlyBePlacedInTerritoryValuedAtX = s;
  }

  private void resetCanOnlyBePlacedInTerritoryValuedAtX() {
    canOnlyBePlacedInTerritoryValuedAtX = -1;
  }

  private void setUnitPlacementRestrictions(final String value) throws GameParseException {
    final Collection<Territory> restrictedTerritories = getListedTerritories(splitOnColon(value));
    unitPlacementRestrictions =
        restrictedTerritories.stream().map(Territory::getName).toArray(String[]::new);
  }

  private void setUnitPlacementRestrictions(final String[] value) {
    unitPlacementRestrictions = value;
  }

  public boolean unitPlacementRestrictionsContain(Territory territory) {
    if (unitPlacementRestrictions == null) {
      return false;
    }
    return Arrays.asList(unitPlacementRestrictions).contains(territory.getName());
  }

  private void resetUnitPlacementRestrictions() {
    unitPlacementRestrictions = null;
  }

  // no field for this, since it is the inverse of unitPlacementRestrictions
  // we might as well just use unitPlacementRestrictions
  private void setUnitPlacementOnlyAllowedIn(final String value) throws GameParseException {
    final Collection<Territory> allowedTerritories = getListedTerritories(splitOnColon(value));
    final Collection<Territory> restrictedTerritories =
        new HashSet<>(getData().getMap().getTerritories());
    restrictedTerritories.removeAll(allowedTerritories);
    unitPlacementRestrictions =
        restrictedTerritories.stream().map(Territory::getName).toArray(String[]::new);
  }

  private void setRepairsUnits(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length == 0) {
      throw new GameParseException("repairsUnits cannot be empty" + thisErrorMsg());
    }
    int i = 0;
    int amount;
    try {
      amount = Integer.parseInt(s[0]);
      i++;
    } catch (final NumberFormatException nfe) {
      amount = 1;
    }
    for (; i < s.length; i++) {
      if (repairsUnits == null) {
        repairsUnits = new IntegerMap<>();
      }
      repairsUnits.put(getUnitTypeOrThrow(s[i]), amount);
    }
  }

  private void setRepairsUnits(final IntegerMap<UnitType> value) {
    repairsUnits = value;
  }

  public IntegerMap<UnitType> getRepairsUnits() {
    return getIntegerMapProperty(repairsUnits);
  }

  private void resetRepairsUnits() {
    repairsUnits = null;
  }

  private void setSpecial(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    for (final String option : s) {
      if (!(option.equals("none") || option.equals("canOnlyPlaceInOriginalTerritories"))) {
        throw new GameParseException("special does not allow: " + option + thisErrorMsg());
      }
      if (special == null) {
        special = new HashSet<>();
      }
      special.add(option.intern());
    }
  }

  private void setSpecial(final Set<String> value) {
    special = value;
  }

  public Set<String> getSpecial() {
    return getSetProperty(special);
  }

  private void resetSpecial() {
    special = null;
  }

  private void setCanInvadeOnlyFrom(final String value) {
    final String[] canOnlyInvadeFrom = splitOnColon(value);
    if (canOnlyInvadeFrom[0].equalsIgnoreCase("none")) {
      canInvadeOnlyFrom = new String[] {"none"};
      return;
    }
    if (canOnlyInvadeFrom[0].equalsIgnoreCase("all")) {
      canInvadeOnlyFrom = new String[] {"all"};
      return;
    }
    for (int i = 0; i < canOnlyInvadeFrom.length; i++) {
      canOnlyInvadeFrom[i] = canOnlyInvadeFrom[i].intern();
    }
    canInvadeOnlyFrom = canOnlyInvadeFrom;
  }

  private void setCanInvadeOnlyFrom(final String[] value) {
    canInvadeOnlyFrom = value;
  }

  private String[] getCanInvadeOnlyFrom() {
    return canInvadeOnlyFrom;
  }

  public boolean canInvadeFrom(final Unit transport) {
    return canInvadeOnlyFrom == null
        || List.of(canInvadeOnlyFrom).isEmpty()
        || canInvadeOnlyFrom[0].isEmpty()
        || canInvadeOnlyFrom[0].equals("all")
        || List.of(canInvadeOnlyFrom).contains(transport.getType().getName());
  }

  private void resetCanInvadeOnlyFrom() {
    canInvadeOnlyFrom = null;
  }

  private void setRequiresUnits(final String value) {
    if (requiresUnits == null) {
      requiresUnits = new ArrayList<>();
    }
    final String[] s = splitOnColon(value);
    for (int i = 0; i < s.length; i++) {
      s[i] = s[i].intern();
    }
    requiresUnits.add(s);
  }

  private void setRequiresUnits(final List<String[]> value) {
    requiresUnits = value;
  }

  public List<String[]> getRequiresUnits() {
    return getListProperty(requiresUnits);
  }

  private void resetRequiresUnits() {
    requiresUnits = null;
  }

  private void setRequiresUnitsToMove(final String value) throws GameParseException {
    final String[] array = splitOnColon(value);
    if (array.length == 0) {
      throw new GameParseException(
          "requiresUnitsToMove must have at least 1 unit type" + thisErrorMsg());
    }
    for (int i = 0; i < array.length; i++) {
      getUnitTypeOrThrow(array[i]);
      array[i] = array[i].intern();
    }
    if (requiresUnitsToMove == null) {
      requiresUnitsToMove = new ArrayList<>();
    }
    requiresUnitsToMove.add(array);
  }

  private void setRequiresUnitsToMove(final List<String[]> value) {
    requiresUnitsToMove = value;
  }

  public List<String[]> getRequiresUnitsToMove() {
    return getListProperty(requiresUnitsToMove);
  }

  private void resetRequiresUnitsToMove() {
    requiresUnitsToMove = null;
  }

  private void setWhenCombatDamaged(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (!(s.length == 3 || s.length == 4)) {
      throw new GameParseException(
          "whenCombatDamaged must have 3 or 4 parts: "
              + "value=effect:optionalNumber, count=integer:integer"
              + thisErrorMsg());
    }
    final int from = getInt(s[0]);
    final int to = getInt(s[1]);
    if (from < 0 || to < 0 || to < from) {
      throw new GameParseException(
          "whenCombatDamaged damaged integers must be positive, and the second integer must "
              + "be equal to or greater than the first"
              + thisErrorMsg());
    }
    final Tuple<Integer, Integer> fromTo = Tuple.of(from, to);
    final Tuple<String, String> effectNum;
    if (s.length == 3) {
      effectNum = Tuple.of(s[2].intern(), null);
    } else {
      effectNum = Tuple.of(s[2].intern(), s[3].intern());
    }
    if (whenCombatDamaged == null) {
      whenCombatDamaged = new ArrayList<>();
    }
    whenCombatDamaged.add(Tuple.of(fromTo, effectNum));
  }

  private void setWhenCombatDamaged(final List<WhenCombatDamaged> value) {
    whenCombatDamaged = value.stream().map(WhenCombatDamaged::toTuple).collect(Collectors.toList());
  }

  public List<WhenCombatDamaged> getWhenCombatDamaged() {
    return getListProperty(whenCombatDamaged).stream()
        .map(WhenCombatDamaged::new)
        .collect(Collectors.toList());
  }

  @Value
  public static class WhenCombatDamaged {
    int damageMin;
    int damageMax;
    String effect;
    String unknown;

    WhenCombatDamaged(final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> tuple) {
      damageMin = tuple.getFirst().getFirst();
      damageMax = tuple.getFirst().getSecond();
      effect = tuple.getSecond().getFirst();
      unknown = tuple.getSecond().getSecond();
    }

    Tuple<Tuple<Integer, Integer>, Tuple<String, String>> toTuple() {
      return Tuple.of(Tuple.of(damageMin, damageMax), Tuple.of(effect, unknown));
    }
  }

  private void resetWhenCombatDamaged() {
    whenCombatDamaged = null;
  }

  private void setReceivesAbilityWhenWith(final String value) {
    if (receivesAbilityWhenWith == null) {
      receivesAbilityWhenWith = new ArrayList<>();
    }
    receivesAbilityWhenWith.add(value.intern());
  }

  private void setReceivesAbilityWhenWith(final List<String> value) {
    receivesAbilityWhenWith = value;
  }

  public List<String> getReceivesAbilityWhenWith() {
    return getListProperty(receivesAbilityWhenWith);
  }

  private void resetReceivesAbilityWhenWith() {
    receivesAbilityWhenWith = null;
  }

  private static IntegerMap<Tuple<String, String>> getReceivesAbilityWhenWithMap(
      final Collection<Unit> units,
      final String filterForAbility,
      final UnitTypeList unitTypeList) {
    final IntegerMap<Tuple<String, String>> map = new IntegerMap<>();
    final Collection<UnitType> canReceive =
        UnitUtils.getUnitTypesFromUnitList(
            CollectionUtils.getMatches(units, Matches.unitCanReceiveAbilityWhenWith()));
    for (final UnitType ut : canReceive) {
      final Collection<String> receives = ut.getUnitAttachment().getReceivesAbilityWhenWith();
      for (final String receive : receives) {
        final String[] s = splitOnColon(receive);
        if (filterForAbility != null && !filterForAbility.equals(s[0])) {
          continue;
        }
        map.put(
            Tuple.of(s[0], s[1]),
            CollectionUtils.countMatches(
                units, Matches.unitIsOfType(unitTypeList.getUnitType(s[1]))));
      }
    }
    return map;
  }

  /**
   * Returns the subset of {@code units} that will receive the ability {@code filterForAbility} when
   * they are with, or on the same route as, another unit.
   */
  public static Collection<Unit> getUnitsWhichReceivesAbilityWhenWith(
      final Collection<Unit> units,
      final String filterForAbility,
      final UnitTypeList unitTypeList) {
    if (units.stream().noneMatch(Matches.unitCanReceiveAbilityWhenWith())) {
      return new ArrayList<>();
    }
    final Collection<Unit> unitsCopy = new ArrayList<>(units);
    final Set<Unit> whichReceiveNoDuplicates = new HashSet<>();
    final IntegerMap<Tuple<String, String>> whichGive =
        getReceivesAbilityWhenWithMap(unitsCopy, filterForAbility, unitTypeList);
    for (final Tuple<String, String> abilityUnitType : whichGive.keySet()) {
      final Collection<Unit> receives =
          CollectionUtils.getNMatches(
              unitsCopy,
              whichGive.getInt(abilityUnitType),
              Matches.unitCanReceiveAbilityWhenWith(filterForAbility, abilityUnitType.getSecond()));
      whichReceiveNoDuplicates.addAll(receives);
      unitsCopy.removeAll(receives);
    }
    return whichReceiveNoDuplicates;
  }

  private void setIsConstruction(final String s) {
    isConstruction = getBool(s);
  }

  private void setIsConstruction(final Boolean s) {
    isConstruction = s;
  }

  private void resetIsConstruction() {
    isConstruction = false;
  }

  private void setConstructionType(final String s) {
    constructionType = s;
  }

  private void resetConstructionType() {
    constructionType = "none";
  }

  private void setConstructionsPerTerrPerTypePerTurn(final String s) {
    constructionsPerTerrPerTypePerTurn = getInt(s);
  }

  private void setConstructionsPerTerrPerTypePerTurn(final Integer s) {
    constructionsPerTerrPerTypePerTurn = s;
  }

  private void resetConstructionsPerTerrPerTypePerTurn() {
    constructionsPerTerrPerTypePerTurn = -1;
  }

  private void setMaxConstructionsPerTypePerTerr(final String s) {
    maxConstructionsPerTypePerTerr = getInt(s);
  }

  private void setMaxConstructionsPerTypePerTerr(final Integer s) {
    maxConstructionsPerTypePerTerr = s;
  }

  private void resetMaxConstructionsPerTypePerTerr() {
    maxConstructionsPerTypePerTerr = -1;
  }

  private void setIsMarine(final String s) {
    if (s.equalsIgnoreCase(Constants.PROPERTY_TRUE)) {
      isMarine = 1;
    } else if (s.equalsIgnoreCase(Constants.PROPERTY_FALSE)) {
      isMarine = 0;
    } else {
      isMarine = getInt(s);
    }
  }

  @VisibleForTesting
  public UnitAttachment setIsMarine(final Integer s) {
    isMarine = s;
    return this;
  }

  private void resetIsMarine() {
    isMarine = 0;
  }

  private void setIsLandTransportable(final String s) {
    isLandTransportable = getBool(s);
  }

  private void setIsLandTransportable(final Boolean s) {
    isLandTransportable = s;
  }

  private void resetIsLandTransportable() {
    isLandTransportable = false;
  }

  private void setIsLandTransport(final String s) {
    isLandTransport = getBool(s);
  }

  private void setIsLandTransport(final Boolean s) {
    isLandTransport = s;
  }

  private void resetIsLandTransport() {
    isLandTransport = false;
  }

  private void setTransportCapacity(final int s) {
    transportCapacity = s;
  }

  public boolean isTransportCapacity() {
    return (transportCapacity >= 0);
  }

  private void setIsTwoHit(final String s) {
    setIsTwoHit(getBool(s));
  }

  private void setIsTwoHit(final boolean s) {
    hitPoints = s ? 2 : 1;
  }

  @VisibleForTesting
  public void setHitPoints(final int value) {
    hitPoints = value;
  }

  private void setTransportCost(final Integer s) {
    transportCost = s;
  }

  private void setMaxBuiltPerPlayer(final String s) {
    maxBuiltPerPlayer = getInt(s);
  }

  private void setMaxBuiltPerPlayer(final Integer s) {
    maxBuiltPerPlayer = s;
  }

  private void resetMaxBuiltPerPlayer() {
    maxBuiltPerPlayer = -1;
  }

  private void setCarrierCapacity(final String s) {
    carrierCapacity = getInt(s);
  }

  private void setCarrierCapacity(final Integer s) {
    carrierCapacity = s;
  }

  private void resetCarrierCapacity() {
    carrierCapacity = -1;
  }

  private void setCarrierCost(final String s) {
    carrierCost = getInt(s);
  }

  private void setCarrierCost(final Integer s) {
    carrierCost = s;
  }

  private void resetCarrierCost() {
    carrierCost = -1;
  }

  private void setArtillery(final String s) throws GameParseException {
    artillery = getBool(s);
    if (artillery) {
      UnitSupportAttachment.addRule((UnitType) getAttachedTo(), getData(), false);
    }
  }

  private void setArtillery(final Boolean s) throws GameParseException {
    artillery = s;
    if (artillery) {
      UnitSupportAttachment.addRule((UnitType) getAttachedTo(), getData(), false);
    }
  }

  public boolean getArtillery() {
    return artillery;
  }

  private void resetArtillery() {
    throw new IllegalStateException(
        "Resetting Artillery (UnitAttachment) is not allowed, "
            + "please use Support Attachments instead.");
  }

  private void setArtillerySupportable(final String s) throws GameParseException {
    artillerySupportable = getBool(s);
    if (artillerySupportable) {
      UnitSupportAttachment.addTarget((UnitType) getAttachedTo(), getData());
    }
  }

  private void setArtillerySupportable(final Boolean s) throws GameParseException {
    artillerySupportable = s;
    if (artillerySupportable) {
      UnitSupportAttachment.addTarget((UnitType) getAttachedTo(), getData());
    }
  }

  public boolean getArtillerySupportable() {
    return artillerySupportable;
  }

  private void resetArtillerySupportable() {
    throw new IllegalStateException(
        "Resetting Artillery Supportable (UnitAttachment) is not allowed, "
            + "please use Support Attachments instead.");
  }

  public void setUnitSupportCount(final String s) {
    unitSupportCount = getInt(s);
    UnitSupportAttachment.setOldSupportCount(
        (UnitType) getAttachedTo(), getData().getUnitTypeList(), s);
  }

  private void setUnitSupportCount(final Integer s) {
    unitSupportCount = s;
    UnitSupportAttachment.setOldSupportCount(
        (UnitType) getAttachedTo(), getData().getUnitTypeList(), s.toString());
  }

  private int getUnitSupportCount() {
    return unitSupportCount > 0 ? unitSupportCount : 1;
  }

  private void resetUnitSupportCount() {
    throw new IllegalStateException(
        "Resetting Artillery Support Count (UnitAttachment) is not allowed, "
            + "please use Support Attachments instead.");
  }

  @VisibleForTesting
  public UnitAttachment setBombard(final int s) {
    bombard = s;
    return this;
  }

  public int getBombard() {
    return bombard > 0 ? bombard : attack;
  }

  private void setMovement(final String s) {
    movement = getInt(s);
  }

  @VisibleForTesting
  public void setMovement(final Integer s) {
    movement = s;
  }

  private int getMovement() {
    return movement;
  }

  public int getMovement(final GamePlayer player) {
    final int bonus = getTechTracker().getMovementBonus(player, getUnitType());
    return Math.max(0, movement + bonus);
  }

  private void resetMovement() {
    movement = 0;
  }

  private void setAttack(final String s) {
    attack = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setAttack(final Integer s) {
    attack = s;
    return this;
  }

  int getAttack() {
    return attack;
  }

  public int getAttack(final GamePlayer player) {
    final int bonus = getTechTracker().getAttackBonus(player, getUnitType());
    return Math.min(getData().getDiceSides(), Math.max(0, attack + bonus));
  }

  private void resetAttack() {
    attack = 0;
  }

  private void setAttackRolls(final String s) {
    attackRolls = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setAttackRolls(final Integer s) {
    attackRolls = s;
    return this;
  }

  private int getAttackRolls() {
    return attackRolls;
  }

  public int getAttackRolls(final GamePlayer player) {
    final int bonus = getTechTracker().getAttackRollsBonus(player, getUnitType());
    return Math.max(0, attackRolls + bonus);
  }

  private void resetAttackRolls() {
    attackRolls = 1;
  }

  private void setDefense(final String s) {
    defense = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setDefense(final Integer s) {
    defense = s;
    return this;
  }

  private int getDefense() {
    return defense;
  }

  public int getDefense(final GamePlayer player) {
    final int bonus = getTechTracker().getDefenseBonus(player, getUnitType());
    int defenseValue = defense + bonus;
    if (defenseValue > 0 && getIsFirstStrike() && TechTracker.hasSuperSubs(player)) {
      final int superSubBonus = Properties.getSuperSubDefenseBonus(getData().getProperties());
      defenseValue += superSubBonus;
    }
    return Math.min(getData().getDiceSides(), Math.max(0, defenseValue));
  }

  private void resetDefense() {
    defense = 0;
  }

  private void setDefenseRolls(final String s) {
    defenseRolls = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setDefenseRolls(final Integer s) {
    defenseRolls = s;
    return this;
  }

  private int getDefenseRolls() {
    return defenseRolls;
  }

  public int getDefenseRolls(final GamePlayer player) {
    final int bonus = getTechTracker().getDefenseRollsBonus(player, getUnitType());
    return Math.max(0, defenseRolls + bonus);
  }

  private void resetDefenseRolls() {
    defenseRolls = 1;
  }

  private void setChooseBestRoll(final String s) {
    chooseBestRoll = getBool(s);
  }

  @VisibleForTesting
  public UnitAttachment setChooseBestRoll(final Boolean s) {
    chooseBestRoll = s;
    return this;
  }

  public boolean getChooseBestRoll() {
    return chooseBestRoll;
  }

  private void resetChooseBestRoll() {
    chooseBestRoll = false;
  }

  private void setCanScramble(final String s) {
    canScramble = getBool(s);
  }

  private void setCanScramble(final Boolean s) {
    canScramble = s;
  }

  private void resetCanScramble() {
    canScramble = false;
  }

  private void setMaxScrambleCount(final String s) {
    maxScrambleCount = getInt(s);
  }

  private void setMaxScrambleCount(final Integer s) {
    maxScrambleCount = s;
  }

  private void resetMaxScrambleCount() {
    maxScrambleCount = -1;
  }

  private void setMaxScrambleDistance(final String s) {
    maxScrambleDistance = getInt(s);
  }

  private void setMaxScrambleDistance(final Integer s) {
    maxScrambleDistance = s;
  }

  private void resetMaxScrambleDistance() {
    maxScrambleDistance = -1;
  }

  private void setMaxInterceptCount(final String s) {
    maxInterceptCount = getInt(s);
  }

  private void setMaxInterceptCount(final Integer s) {
    maxInterceptCount = s;
  }

  private void resetMaxInterceptCount() {
    maxInterceptCount = -1;
  }

  private void setMaxOperationalDamage(final String s) {
    maxOperationalDamage = getInt(s);
  }

  private void setMaxOperationalDamage(final Integer s) {
    maxOperationalDamage = s;
  }

  private void resetMaxOperationalDamage() {
    maxOperationalDamage = -1;
  }

  private void setMaxDamage(final String s) {
    maxDamage = getInt(s);
  }

  private void setMaxDamage(final Integer s) {
    maxDamage = s;
  }

  private void resetMaxDamage() {
    maxDamage = 2;
  }

  private void setIsAirBase(final String s) {
    isAirBase = getBool(s);
  }

  private void setIsAirBase(final Boolean s) {
    isAirBase = s;
  }

  private void resetIsAirBase() {
    isAirBase = false;
  }

  private void setIsInfrastructure(final String s) {
    isInfrastructure = getBool(s);
  }

  @VisibleForTesting
  public UnitAttachment setIsInfrastructure(final Boolean s) {
    isInfrastructure = s;
    return this;
  }

  private void resetIsInfrastructure() {
    isInfrastructure = false;
  }

  private void setCanBeDamaged(final String s) {
    canBeDamaged = getBool(s);
  }

  private void setCanBeDamaged(final Boolean s) {
    canBeDamaged = s;
  }

  private void resetCanBeDamaged() {
    canBeDamaged = false;
  }

  private void setCanDieFromReachingMaxDamage(final String s) {
    canDieFromReachingMaxDamage = getBool(s);
  }

  private void setCanDieFromReachingMaxDamage(final Boolean s) {
    canDieFromReachingMaxDamage = s;
  }

  private void resetCanDieFromReachingMaxDamage() {
    canDieFromReachingMaxDamage = false;
  }

  @Deprecated
  @VisibleForTesting
  public void setIsSuicide(final Boolean s) {
    isSuicide = s;
    resetCanNotTarget();
  }

  @Deprecated
  public boolean getIsSuicide() {
    return isSuicide || isSuicideOnAttack || isSuicideOnDefense;
  }

  @VisibleForTesting
  public void setIsSuicideOnAttack(final Boolean s) {
    isSuicideOnAttack = s;
  }

  public boolean getIsSuicideOnAttack() {
    return isSuicideOnAttack || isSuicide;
  }

  @VisibleForTesting
  public void setIsSuicideOnDefense(final Boolean s) {
    isSuicideOnDefense = s;
  }

  public boolean getIsSuicideOnDefense() {
    return isSuicideOnDefense
        // Global property controlled whether isSuicide units would suicide on defense
        || (isSuicide
            && !Properties.getDefendingSuicideAndMunitionUnitsDoNotFire(getData().getProperties()));
  }

  private void setIsSuicideOnHit(final String s) {
    isSuicideOnHit = getBool(s);
  }

  @VisibleForTesting
  public UnitAttachment setIsSuicideOnHit(final Boolean s) {
    isSuicideOnHit = s;
    return this;
  }

  private void resetIsSuicideOnHit() {
    isSuicideOnHit = false;
  }

  private void setIsKamikaze(final String s) {
    isKamikaze = getBool(s);
  }

  private void setIsKamikaze(final Boolean s) {
    isKamikaze = s;
  }

  private void resetIsKamikaze() {
    isKamikaze = false;
  }

  private void setBlockade(final String s) {
    blockade = getInt(s);
  }

  private void setBlockade(final Integer s) {
    blockade = s;
  }

  private void resetBlockade() {
    blockade = 0;
  }

  private void setGivesMovement(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 2) {
      throw new GameParseException(
          "givesMovement must have an integer followed by 1 or more unit types" + thisErrorMsg());
    }
    final int movement = getInt(s[0]);
    for (int i = 1; i < s.length; i++) {
      // we should allow positive and negative numbers, since you can give bonuses to units or take
      // away a unit's movement
      if (givesMovement == null) {
        givesMovement = new IntegerMap<>();
      }
      givesMovement.put(getUnitTypeOrThrow(s[i]), movement);
    }
  }

  private void setGivesMovement(final IntegerMap<UnitType> value) {
    givesMovement = value;
  }

  public IntegerMap<UnitType> getGivesMovement() {
    return getIntegerMapProperty(givesMovement);
  }

  private void resetGivesMovement() {
    givesMovement = null;
  }

  private void setConsumesUnits(final String value) throws GameParseException {
    if (consumesUnits == null) {
      consumesUnits = new IntegerMap<>();
    }
    addToUnitTypeMap("consumesUnits", consumesUnits, value, 1);
  }

  private void setConsumesUnits(final IntegerMap<UnitType> value) {
    consumesUnits = value;
  }

  public IntegerMap<UnitType> getConsumesUnits() {
    return getIntegerMapProperty(consumesUnits);
  }

  private void resetConsumesUnits() {
    consumesUnits = null;
  }

  private void setCreatesUnitsList(final String value) throws GameParseException {
    if (createsUnitsList == null) {
      createsUnitsList = new IntegerMap<>();
    }
    addToUnitTypeMap("createsUnitsList", createsUnitsList, value, 0);
  }

  private void setCreatesUnitsList(final IntegerMap<UnitType> value) {
    createsUnitsList = value;
  }

  public IntegerMap<UnitType> getCreatesUnitsList() {
    return getIntegerMapProperty(createsUnitsList);
  }

  private void resetCreatesUnitsList() {
    createsUnitsList = null;
  }

  private void addToUnitTypeMap(
      String context, IntegerMap<UnitType> utMap, String value, int minValue)
      throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length == 0 || s.length > 2) {
      throw new GameParseException(
          context + " cannot be empty or have more than two fields" + thisErrorMsg());
    }
    final UnitType ut = getUnitTypeOrThrow(s[1]);
    final int n = getInt(s[0]);
    if (n < minValue) {
      throw new GameParseException(context + " value must be >= " + minValue + thisErrorMsg());
    }
    utMap.put(ut, n);
  }

  private void setCreatesResourcesList(final String value) throws GameParseException {
    if (createsResourcesList == null) {
      createsResourcesList = new IntegerMap<>();
    }
    addToResourceMap("createsResourcesList", createsResourcesList, value, true);
  }

  private void setCreatesResourcesList(final IntegerMap<Resource> value) {
    createsResourcesList = value;
  }

  public IntegerMap<Resource> getCreatesResourcesList() {
    return getIntegerMapProperty(createsResourcesList);
  }

  private void resetCreatesResourcesList() {
    createsResourcesList = null;
  }

  private void setFuelCost(final String value) throws GameParseException {
    if (fuelCost == null) {
      fuelCost = new IntegerMap<>();
    }
    addToResourceMap("fuelCost", fuelCost, value, false);
  }

  private void setFuelCost(final IntegerMap<Resource> value) {
    fuelCost = value;
  }

  public IntegerMap<Resource> getFuelCost() {
    return getIntegerMapProperty(fuelCost);
  }

  private void resetFuelCost() {
    fuelCost = null;
  }

  private void setFuelFlatCost(final String value) throws GameParseException {
    if (fuelFlatCost == null) {
      fuelFlatCost = new IntegerMap<>();
    }
    addToResourceMap("fuelFlatCost", fuelFlatCost, value, false);
  }

  private void setFuelFlatCost(final IntegerMap<Resource> value) {
    fuelFlatCost = value;
  }

  public IntegerMap<Resource> getFuelFlatCost() {
    return getIntegerMapProperty(fuelFlatCost);
  }

  private void resetFuelFlatCost() {
    fuelFlatCost = null;
  }

  private void addToResourceMap(
      String description, IntegerMap<Resource> resourceMap, String value, boolean allowNegative)
      throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException(description + " must have two fields" + thisErrorMsg());
    }
    final String resourceToProduce = s[1];
    // validate that this resource exists in the xml
    final Resource r = getData().getResourceList().getResource(resourceToProduce);
    if (r == null) {
      throw new GameParseException(
          description + ": No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (!allowNegative && n < 0) {
      throw new GameParseException(description + " must have positive values" + thisErrorMsg());
    }
    resourceMap.put(r, n);
  }

  private void setBombingBonus(final String s) {
    bombingBonus = getInt(s);
  }

  private void setBombingBonus(final Integer s) {
    bombingBonus = s;
  }

  private void resetBombingBonus() {
    bombingBonus = -1;
  }

  private void setBombingMaxDieSides(final String s) {
    bombingMaxDieSides = getInt(s);
  }

  private void setBombingMaxDieSides(final Integer s) {
    bombingMaxDieSides = s;
  }

  private void resetBombingMaxDieSides() {
    bombingMaxDieSides = -1;
  }

  private void setBombingTargets(final String value) throws GameParseException {
    bombingTargets = parseUnitTypes("bombingTargets", value, bombingTargets);
  }

  private void setBombingTargets(final Set<UnitType> value) {
    bombingTargets = value;
  }

  private Set<UnitType> getBombingTargets() {
    return getSetProperty(bombingTargets);
  }

  public Set<UnitType> getBombingTargets(final UnitTypeList unitTypeList) {
    if (bombingTargets != null) {
      return Collections.unmodifiableSet(bombingTargets);
    }
    return unitTypeList.getAllUnitTypes();
  }

  private void resetBombingTargets() {
    bombingTargets = null;
  }

  /** Finds potential unit types which all passed in bombers and rockets can target. */
  public static Set<UnitType> getAllowedBombingTargetsIntersection(
      final Collection<Unit> bombersOrRockets, final UnitTypeList unitTypeList) {
    if (bombersOrRockets.isEmpty()) {
      return new HashSet<>();
    }
    Collection<UnitType> allowedTargets = unitTypeList.getAllUnitTypes();
    for (final Unit u : bombersOrRockets) {
      final UnitAttachment ua = u.getUnitAttachment();
      final Set<UnitType> bombingTargets = ua.getBombingTargets(unitTypeList);
      allowedTargets = CollectionUtils.intersection(allowedTargets, bombingTargets);
    }
    return new HashSet<>(allowedTargets);
  }

  private void setIsAa(final String s) throws GameParseException {
    setIsAa(getBool(s));
  }

  private void setIsAa(final Boolean s) throws GameParseException {
    setIsAaForCombatOnly(s);
    setIsAaForBombingThisUnitOnly(s);
    setIsAaForFlyOverOnly(s);
    setIsAaMovement(s);
    setIsRocket(s);
    setIsInfrastructure(s);
  }

  private void setAttackAa(final String s) {
    attackAa = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setAttackAa(final Integer s) {
    attackAa = s;
    return this;
  }

  private int getAttackAa() {
    return attackAa;
  }

  public int getAttackAa(final GamePlayer player) {
    // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something
    // other than 6, or it does not divide perfectly into attackAAmaxDieSides
    final int bonus = getTechTracker().getRadarBonus(player, getUnitType());
    return Math.max(0, Math.min(getAttackAaMaxDieSides(), attackAa + bonus));
  }

  private void resetAttackAa() {
    attackAa = 1;
  }

  private void setOffensiveAttackAa(final String s) {
    offensiveAttackAa = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setOffensiveAttackAa(final Integer s) {
    offensiveAttackAa = s;
    return this;
  }

  private int getOffensiveAttackAa() {
    return offensiveAttackAa;
  }

  public int getOffensiveAttackAa(final GamePlayer player) {
    // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something
    // other than 6, or it does not divide perfectly into attackAAmaxDieSides
    final int bonus = getTechTracker().getRadarBonus(player, getUnitType());
    return Math.max(0, Math.min(getOffensiveAttackAaMaxDieSides(), offensiveAttackAa + bonus));
  }

  private void resetOffensiveAttackAa() {
    offensiveAttackAa = 1;
  }

  private void setAttackAaMaxDieSides(final String s) {
    attackAaMaxDieSides = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setAttackAaMaxDieSides(final Integer s) {
    attackAaMaxDieSides = s;
    return this;
  }

  public int getAttackAaMaxDieSides() {
    return attackAaMaxDieSides > 0 ? attackAaMaxDieSides : getData().getDiceSides();
  }

  private void resetAttackAaMaxDieSides() {
    attackAaMaxDieSides = -1;
  }

  private void setOffensiveAttackAaMaxDieSides(final String s) {
    offensiveAttackAaMaxDieSides = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setOffensiveAttackAaMaxDieSides(final Integer s) {
    offensiveAttackAaMaxDieSides = s;
    return this;
  }

  public int getOffensiveAttackAaMaxDieSides() {
    return offensiveAttackAaMaxDieSides > 0
        ? offensiveAttackAaMaxDieSides
        : getData().getDiceSides();
  }

  private void resetOffensiveAttackAaMaxDieSides() {
    offensiveAttackAaMaxDieSides = -1;
  }

  private void setMaxAaAttacks(final String s) throws GameParseException {
    final int attacks = getInt(s);
    if (attacks < -1) {
      throw new GameParseException(
          "maxAAattacks must be positive (or -1 for attacking all) " + thisErrorMsg());
    }
    maxAaAttacks = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setMaxAaAttacks(final Integer s) {
    maxAaAttacks = s;
    return this;
  }

  private void resetMaxAaAttacks() {
    maxAaAttacks = -1;
  }

  private void setMaxRoundsAa(final String s) throws GameParseException {
    final int attacks = getInt(s);
    if (attacks < -1) {
      throw new GameParseException(
          "maxRoundsAA must be positive (or -1 for infinite) " + thisErrorMsg());
    }
    maxRoundsAa = getInt(s);
  }

  @VisibleForTesting
  public UnitAttachment setMaxRoundsAa(final Integer s) {
    maxRoundsAa = s;
    return this;
  }

  private void resetMaxRoundsAa() {
    maxRoundsAa = 1;
  }

  private void setMayOverStackAa(final String s) {
    mayOverStackAa = getBool(s);
  }

  @VisibleForTesting
  public UnitAttachment setMayOverStackAa(final Boolean s) {
    mayOverStackAa = s;
    return this;
  }

  public boolean getMayOverStackAa() {
    return mayOverStackAa;
  }

  private void resetMayOverStackAa() {
    mayOverStackAa = false;
  }

  private void setDamageableAa(final String s) {
    damageableAa = getBool(s);
  }

  @VisibleForTesting
  public void setDamageableAa(final Boolean s) {
    damageableAa = s;
  }

  public boolean getDamageableAa() {
    return damageableAa;
  }

  private void resetDamageableAa() {
    damageableAa = false;
  }

  private void setIsAaForCombatOnly(final String s) {
    isAaForCombatOnly = getBool(s);
  }

  @VisibleForTesting
  public UnitAttachment setIsAaForCombatOnly(final Boolean s) {
    isAaForCombatOnly = s;
    return this;
  }

  private void resetIsAaForCombatOnly() {
    isAaForCombatOnly = false;
  }

  private void setIsAaForBombingThisUnitOnly(final String s) {
    isAaForBombingThisUnitOnly = getBool(s);
  }

  private void setIsAaForBombingThisUnitOnly(final Boolean s) {
    isAaForBombingThisUnitOnly = s;
  }

  private void resetIsAaForBombingThisUnitOnly() {
    isAaForBombingThisUnitOnly = false;
  }

  private void setIsAaForFlyOverOnly(final String s) {
    isAaForFlyOverOnly = getBool(s);
  }

  private void setIsAaForFlyOverOnly(final Boolean s) {
    isAaForFlyOverOnly = s;
  }

  private void resetIsAaForFlyOverOnly() {
    isAaForFlyOverOnly = false;
  }

  private void setIsRocket(final String s) {
    isRocket = getBool(s);
  }

  private void setIsRocket(final Boolean s) {
    isRocket = s;
  }

  private void resetIsRocket() {
    isRocket = false;
  }

  @VisibleForTesting
  public void setTypeAa(final String s) {
    typeAa = s.intern();
  }

  private void resetTypeAa() {
    typeAa = "AA";
  }

  public static List<String> getAllOfTypeAas(final Collection<Unit> aaUnitsAlreadyVerified) {
    final Set<String> aaSet = new HashSet<>();
    for (final Unit u : aaUnitsAlreadyVerified) {
      aaSet.add(u.getUnitAttachment().getTypeAa());
    }
    final List<String> aaTypes = new ArrayList<>(aaSet);
    Collections.sort(aaTypes);
    return aaTypes;
  }

  private void setTargetsAa(final String value) throws GameParseException {
    targetsAa = parseUnitTypes("AAtargets", value, targetsAa);
  }

  @VisibleForTesting
  public UnitAttachment setTargetsAa(final Set<UnitType> value) {
    targetsAa = value;
    return this;
  }

  private Set<UnitType> getTargetsAa() {
    return getSetProperty(targetsAa);
  }

  public Set<UnitType> getTargetsAa(final UnitTypeList unitTypeList) {
    if (targetsAa != null) {
      return Collections.unmodifiableSet(targetsAa);
    }
    return unitTypeList.stream()
        .filter(ut -> ut.getUnitAttachment().isAir())
        .collect(Collectors.toSet());
  }

  private void resetTargetsAa() {
    targetsAa = null;
  }

  private void setWillNotFireIfPresent(final String value) throws GameParseException {
    willNotFireIfPresent = parseUnitTypes("willNotFireIfPresent", value, willNotFireIfPresent);
  }

  @VisibleForTesting
  public void setWillNotFireIfPresent(final Set<UnitType> value) {
    willNotFireIfPresent = value;
  }

  public Set<UnitType> getWillNotFireIfPresent() {
    return getSetProperty(willNotFireIfPresent);
  }

  private void resetWillNotFireIfPresent() {
    willNotFireIfPresent = null;
  }

  private void setIsAaMovement(final String s) throws GameParseException {
    setIsAaMovement(getBool(s));
  }

  private void setIsAaMovement(final boolean s) throws GameParseException {
    setCanNotMoveDuringCombatMove(s);
    if (s) {
      setMovementLimit(Integer.MAX_VALUE + ":allied");
      setAttackingLimit(Integer.MAX_VALUE + ":allied");
      setPlacementLimit(Integer.MAX_VALUE + ":allied");
    } else {
      movementLimit = null;
      attackingLimit = null;
      placementLimit = null;
    }
  }

  private void setCanNotMoveDuringCombatMove(final String s) {
    canNotMoveDuringCombatMove = getBool(s);
  }

  private void setCanNotMoveDuringCombatMove(final Boolean s) {
    canNotMoveDuringCombatMove = s;
  }

  private void resetCanNotMoveDuringCombatMove() {
    canNotMoveDuringCombatMove = false;
  }

  private void setMovementLimit(final String value) throws GameParseException {
    placementLimit = parseStackingLimit("movementLimit", value);
  }

  private void setMovementLimit(final Tuple<Integer, String> value) {
    movementLimit = value;
  }

  public @Nullable Tuple<Integer, String> getMovementLimit() {
    return movementLimit;
  }

  private void resetMovementLimit() {
    movementLimit = null;
  }

  private void setAttackingLimit(final String value) throws GameParseException {
    attackingLimit = parseStackingLimit("attackingLimit", value);
  }

  private void setAttackingLimit(final Tuple<Integer, String> value) {
    attackingLimit = value;
  }

  public @Nullable Tuple<Integer, String> getAttackingLimit() {
    return attackingLimit;
  }

  private void resetAttackingLimit() {
    attackingLimit = null;
  }

  private void setPlacementLimit(final String value) throws GameParseException {
    placementLimit = parseStackingLimit("placementLimit", value);
  }

  private void setPlacementLimit(final Tuple<Integer, String> value) {
    placementLimit = value;
  }

  public @Nullable Tuple<Integer, String> getPlacementLimit() {
    return placementLimit;
  }

  private void resetPlacementLimit() {
    placementLimit = null;
  }

  private Tuple<Integer, String> parseStackingLimit(final String type, final String value)
      throws GameParseException {
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException(type + " must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException(type + " count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException(type + " value must owned, allied, or total" + thisErrorMsg());
    }
    return Tuple.of(max, s[1].intern());
  }

  private void setTuv(final String s) throws GameParseException {
    final int value = getInt(s);
    if (value < -1) {
      throw new GameParseException(
          "tuv must be 0 positive (or -1, default, to calculate) " + thisErrorMsg());
    }
    tuv = getInt(s);
  }

  private void setTuv(final Integer s) {
    tuv = s;
  }

  private void resetTuv() {
    tuv = -1;
  }

  public void setCanRetreatOnStalemate(final boolean value) {
    canRetreatOnStalemate = value;
  }

  public void setCanRetreatOnStalemate(final String value) {
    canRetreatOnStalemate = getBool(value);
  }

  public @Nullable Boolean getCanRetreatOnStalemate() {
    return canRetreatOnStalemate;
  }

  public void resetCanRetreatOnStalemate() {
    canRetreatOnStalemate = null;
  }

  @Override
  public void validate(final GameState data) throws GameParseException {
    if (isAir) {
      if (isSea
          || transportCost != -1
          || carrierCapacity != -1
          || canBlitz
          || canBombard
          || isMarine != 0
          || isLandTransportable
          || isLandTransport
          || isAirTransportable
          || isCombatTransport) {
        throw new GameParseException("air units cannot have certain properties, " + thisErrorMsg());
      }
    } else if (isSea) {
      if (canBlitz
          || isStrategicBomber
          || carrierCost != -1
          || transportCost != -1
          || isMarine != 0
          || isLandTransportable
          || isLandTransport
          || isAirTransportable
          || isAirTransport
          || isKamikaze) {
        throw new GameParseException("sea units cannot have certain properties, " + thisErrorMsg());
      }
    } else { // if land
      if (canBombard
          || carrierCapacity != -1
          || bombard != -1
          || isAirTransport
          || isCombatTransport
          || isKamikaze) {
        throw new GameParseException(
            "land units cannot have certain properties, " + thisErrorMsg());
      }
    }
    if (hitPoints < 1) {
      throw new GameParseException("hitPoints cannot be zero or negative, " + thisErrorMsg());
    }
    if (attackAa < 0
        || attackAaMaxDieSides < -1
        || attackAaMaxDieSides > 200
        || offensiveAttackAa < 0
        || offensiveAttackAaMaxDieSides < -1
        || offensiveAttackAaMaxDieSides > 200) {
      throw new GameParseException(
          "attackAA or attackAAmaxDieSides or offensiveAttackAA or "
              + "offensiveAttackAAmaxDieSides is wrong, "
              + thisErrorMsg());
    }
    if (maxBuiltPerPlayer < -1) {
      throw new GameParseException("maxBuiltPerPlayer cannot be negative, " + thisErrorMsg());
    }
    if (isCombatTransport && transportCapacity < 1) {
      throw new GameParseException(
          "cannot have isCombatTransport on unit without transportCapacity, " + thisErrorMsg());
    }
    if (isSea
        && isTransportCapacity()
        && Properties.getTransportCasualtiesRestricted(data.getProperties())
        && (attack > 0 || defense > 0)
        && !isCombatTransport) {
      throw new GameParseException(
          "Restricted transports cannot have attack or defense, " + thisErrorMsg());
    }
    if (isConstruction
        && (constructionType == null
            || constructionType.equals("none")
            || constructionType.isEmpty()
            || constructionsPerTerrPerTypePerTurn < 0
            || maxConstructionsPerTypePerTerr < 0)) {
      throw new GameParseException(
          "Constructions must have constructionType and positive constructionsPerTerrPerType "
              + "and maxConstructionsPerType, "
              + thisErrorMsg());
    }
    if (!isConstruction
        && (!(constructionType == null
                || constructionType.equals("none")
                || constructionType.isEmpty())
            || constructionsPerTerrPerTypePerTurn >= 0
            || maxConstructionsPerTypePerTerr >= 0)) {
      throw new GameParseException(
          "Constructions must have isConstruction true, " + thisErrorMsg());
    }
    if (constructionsPerTerrPerTypePerTurn > maxConstructionsPerTypePerTerr) {
      throw new GameParseException(
          "Constructions must have constructionsPerTerrPerTypePerTurn "
              + "Less than maxConstructionsPerTypePerTerr, "
              + thisErrorMsg());
    }
    if (requiresUnits != null) {
      for (final String[] combo : requiresUnits) {
        getListedUnits(combo);
      }
    }
    if ((canBeDamaged && maxDamage < 1)
        || (canDieFromReachingMaxDamage && maxDamage < 1)
        || (!canBeDamaged && canDieFromReachingMaxDamage)) {
      throw new GameParseException(
          "something wrong with canBeDamaged or maxDamage or "
              + "canDieFromReachingMaxDamage or isFactory, "
              + thisErrorMsg());
    }
    if (canInvadeOnlyFrom != null
        && !canInvadeOnlyFrom[0].equals("all")
        && !canInvadeOnlyFrom[0].equals("none")) {
      for (final String transport : canInvadeOnlyFrom) {
        final UnitType ut = getUnitTypeOrThrow(transport);
        if (ut.getAttachments() == null || ut.getAttachments().isEmpty()) {
          throw new GameParseException(
              transport
                  + " has no attachments, please declare "
                  + transport
                  + " in the xml before using it as a transport"
                  + thisErrorMsg());
          // Units may be considered transported if they are on a carrier, or if they are
          // paratroopers, or if they are mech infantry. The "transporter" may not be an actual
          // transport, so we should not check for that here.
        }
      }
    }
    for (final String value : getReceivesAbilityWhenWith()) {
      // first is ability, second is unit that we get it from
      final String[] s = splitOnColon(value);
      if (s.length != 2) {
        throw new GameParseException(
            "receivesAbilityWhenWith must have 2 parts, 'ability:unit'" + thisErrorMsg());
      }
      getUnitTypeOrThrow(s[1]);
      // currently only supports canBlitz (canBlitz)
      if (!s[0].equals("canBlitz")) {
        throw new GameParseException(
            "receivesAbilityWhenWith so far only supports: canBlitz" + thisErrorMsg());
      }
    }
    if (!getWhenCombatDamaged().isEmpty()) {
      for (final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key : whenCombatDamaged) {
        final String obj = key.getSecond().getFirst();
        if (obj.equals(UNITS_MAY_NOT_LAND_ON_CARRIER)) {
          continue;
        }
        if (obj.equals(UNITS_MAY_NOT_LEAVE_ALLIED_CARRIER)) {
          continue;
        }
        throw new GameParseException(
            "whenCombatDamaged so far only supports: "
                + UNITS_MAY_NOT_LAND_ON_CARRIER
                + ", "
                + UNITS_MAY_NOT_LEAVE_ALLIED_CARRIER
                + thisErrorMsg());
      }
    }
  }

  public Collection<UnitType> getListedUnits(final String[] list) {
    final List<UnitType> unitTypes = new ArrayList<>();
    for (final String name : list) {
      // Validate all units exist
      final UnitType ut = getData().getUnitTypeList().getUnitType(name);
      if (ut == null) {
        throw new IllegalStateException("No unit called: " + name + thisErrorMsg());
      }
      unitTypes.add(ut);
    }
    return unitTypes;
  }

  private Collection<Territory> getListedTerritories(final String[] list)
      throws GameParseException {
    final Set<Territory> territories = new HashSet<>();
    for (final String name : list) {
      // Validate all territories exist
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory != null) {
        territories.add(territory);
      } else {
        // Check if it's a territory effect and get all territories
        if (getData().getTerritoryEffectList().containsKey(name)) {
          for (final Territory t : getData().getMap().getTerritories()) {
            for (final TerritoryEffect te : TerritoryEffectHelper.getEffects(t)) {
              if (name.equals(te.getName())) {
                territories.add(t);
              }
            }
          }
        } else {
          throw new GameParseException(
              "No territory or territory effect called: " + name + thisErrorMsg());
        }
      }
    }
    return territories;
  }

  private static boolean playerHasRockets(final GamePlayer player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    return ta != null && ta.getRocket();
  }

  private static boolean playerHasMechInf(final GamePlayer player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    return ta != null && ta.getMechanizedInfantry();
  }

  private static boolean playerHasParatroopers(final GamePlayer player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    return ta != null && ta.getParatroopers();
  }

  private static <T> String toString(final Collection<T> collection, final String nullStr) {
    return collection == null ? nullStr : collection.isEmpty() ? "empty" : collection.toString();
  }

  private static <T> String toString(final Collection<T> collection) {
    return toString(collection, "null");
  }

  private static String toString(final String[] collection) {
    return collection == null
        ? "null"
        : collection.length == 0 ? "empty" : Arrays.toString(collection);
  }

  private static <T> String toString(final IntegerMap<T> collection) {
    return collection == null ? "null" : collection.isEmpty() ? "empty" : collection.toString();
  }

  private static <K, V> String toString(final Map<K, V> collection) {
    return collection == null ? "null" : collection.isEmpty() ? "empty" : collection.toString();
  }

  private static String toString(final List<String[]> collection) {
    return collection == null
        ? "null"
        : collection.isEmpty() ? "empty" : MyFormatter.listOfArraysToString(collection);
  }

  /**
   * Returns a list of all unit properties. Should cover ALL fields stored in UnitAttachment
   * Remember to test for null and fix arrays. The stats exporter relies on this toString having two
   * spaces after each entry, so do not change this please, except to add new abilities onto the
   * end.
   */
  public String allUnitStatsForExporter() {
    return this.getAttachedTo().toString().replaceFirst("games.strategy.engine.data.", "")
        + " with:"
        + "  isAir:"
        + isAir
        + "  isSea:"
        + isSea
        + "  movement:"
        + movement
        + "  attack:"
        + attack
        + "  defense:"
        + defense
        + "  hitPoints:"
        + hitPoints
        + "  canBlitz:"
        + canBlitz
        + "  artillerySupportable:"
        + artillerySupportable
        + "  artillery:"
        + artillery
        + "  unitSupportCount:"
        + unitSupportCount
        + "  attackRolls:"
        + attackRolls
        + "  defenseRolls:"
        + defenseRolls
        + "  chooseBestRoll:"
        + chooseBestRoll
        + "  isMarine:"
        + isMarine
        + "  isLandTransportable:"
        + isLandTransportable
        + "  isLandTransport:"
        + isLandTransport
        + "  isAirTransportable:"
        + isAirTransportable
        + "  isAirTransport:"
        + isAirTransport
        + "  isStrategicBomber:"
        + isStrategicBomber
        + "  transportCapacity:"
        + transportCapacity
        + "  transportCost:"
        + transportCost
        + "  carrierCapacity:"
        + carrierCapacity
        + "  carrierCost:"
        + carrierCost
        + "  canEvade:"
        + canEvade
        + "  isFirstStrike:"
        + isFirstStrike
        + "  canNotTarget:"
        + toString(canNotTarget)
        + "  canNotBeTargetedBy:"
        + toString(canNotBeTargetedBy)
        + "  canMoveThroughEnemies:"
        + canMoveThroughEnemies
        + "  canBeMovedThroughByEnemies:"
        + canBeMovedThroughByEnemies
        + "  isDestroyer:"
        + isDestroyer
        + "  canBombard:"
        + canBombard
        + "  bombard:"
        + bombard
        + "  isAaForCombatOnly:"
        + isAaForCombatOnly
        + "  isAaForBombingThisUnitOnly:"
        + isAaForBombingThisUnitOnly
        + "  isAaForFlyOverOnly:"
        + isAaForFlyOverOnly
        + "  attackAa:"
        + attackAa
        + "  offensiveAttackAa:"
        + offensiveAttackAa
        + "  attackAaMaxDieSides:"
        + attackAaMaxDieSides
        + "  offensiveAttackAaMaxDieSides:"
        + offensiveAttackAaMaxDieSides
        + "  maxAaAttacks:"
        + maxAaAttacks
        + "  maxRoundsAa:"
        + maxRoundsAa
        + "  mayOverStackAa:"
        + mayOverStackAa
        + "  damageableAa:"
        + damageableAa
        + "  typeAa:"
        + typeAa
        + "  targetsAa:"
        + toString(targetsAa, "all air units")
        + "  willNotFireIfPresent:"
        + toString(willNotFireIfPresent)
        + "  isRocket:"
        + isRocket
        + "  canProduceUnits:"
        + canProduceUnits
        + "  canProduceXUnits:"
        + canProduceXUnits
        + "  createsUnitsList:"
        + toString(createsUnitsList)
        + "  createsResourcesList:"
        + toString(createsResourcesList)
        + "  fuelCost:"
        + toString(fuelCost)
        + "  fuelFlatCost:"
        + toString(fuelFlatCost)
        + "  isInfrastructure:"
        + isInfrastructure
        + "  isConstruction:"
        + isConstruction
        + "  constructionType:"
        + constructionType
        + "  constructionsPerTerrPerTypePerTurn:"
        + constructionsPerTerrPerTypePerTurn
        + "  maxConstructionsPerTypePerTerr:"
        + maxConstructionsPerTypePerTerr
        + "  destroyedWhenCapturedBy:"
        + toString(destroyedWhenCapturedBy, "null")
        + "  canBeCapturedOnEnteringBy:"
        + toString(canBeCapturedOnEnteringBy, "null")
        + "  canBeDamaged:"
        + canBeDamaged
        + "  canDieFromReachingMaxDamage:"
        + canDieFromReachingMaxDamage
        + "  maxOperationalDamage:"
        + maxOperationalDamage
        + "  maxDamage:"
        + maxDamage
        + "  unitPlacementRestrictions:"
        + toString(unitPlacementRestrictions)
        + "  requiresUnits:"
        + toString(requiresUnits)
        + "  consumesUnits:"
        + toString(consumesUnits)
        + "  requiresUnitsToMove:"
        + toString(requiresUnitsToMove)
        + "  canOnlyBePlacedInTerritoryValuedAtX:"
        + canOnlyBePlacedInTerritoryValuedAtX
        + "  maxBuiltPerPlayer:"
        + maxBuiltPerPlayer
        + "  special:"
        + toString(special)
        + "  isSuicideOnAttack:"
        + isSuicideOnAttack
        + "  isSuicideOnDefense:"
        + isSuicideOnDefense
        + "  isSuicideOnHit:"
        + isSuicideOnHit
        + "  isCombatTransport:"
        + isCombatTransport
        + "  canInvadeOnlyFrom:"
        + toString(special)
        + "  canBeGivenByTerritoryTo:"
        + toString(canBeGivenByTerritoryTo)
        + "  receivesAbilityWhenWith:"
        + toString(receivesAbilityWhenWith)
        + "  whenCombatDamaged:"
        + toString(whenCombatDamaged, "null")
        + "  blockade:"
        + blockade
        + "  bombingMaxDieSides:"
        + bombingMaxDieSides
        + "  bombingBonus:"
        + bombingBonus
        + "  bombingTargets:"
        + bombingTargets
        + "  givesMovement:"
        + toString(givesMovement)
        + "  repairsUnits:"
        + toString(repairsUnits)
        + "  canScramble:"
        + canScramble
        + "  maxScrambleDistance:"
        + maxScrambleDistance
        + "  isAirBase:"
        + isAirBase
        + "  maxScrambleCount:"
        + maxScrambleCount
        + "  maxInterceptCount:"
        + maxInterceptCount
        + "  whenCapturedChangesInto:"
        + toString(whenCapturedChangesInto)
        + "  whenCapturedSustainsDamage:"
        + whenCapturedSustainsDamage
        + "  whenHitPointsDamagedChangesInto:"
        + toString(whenHitPointsDamagedChangesInto)
        + "  whenHitPointsRepairedChangesInto:"
        + toString(whenHitPointsRepairedChangesInto)
        + "  canIntercept:"
        + canIntercept
        + "  requiresAirBaseToIntercept:"
        + requiresAirBaseToIntercept
        + "  canEscort:"
        + canEscort
        + "  canAirBattle:"
        + canAirBattle
        + "  airDefense:"
        + airDefense
        + "  airAttack:"
        + airAttack
        + "  canNotMoveDuringCombatMove:"
        + canNotMoveDuringCombatMove
        + "  movementLimit:"
        + (movementLimit != null ? movementLimit.toString() : "null")
        + "  attackingLimit:"
        + (attackingLimit != null ? attackingLimit.toString() : "null")
        + "  placementLimit:"
        + (placementLimit != null ? placementLimit.toString() : "null")
        + "  tuv:"
        + tuv;
  }

  /**
   * Displays all unit options in a short description form that's user-friendly rather than as XML.
   * Shows all except for: constructionType, constructionsPerTerrPerTypePerTurn,
   * maxConstructionsPerTypePerTerr, canBeGivenByTerritoryTo, destroyedWhenCapturedBy,
   * canBeCapturedOnEnteringBy.
   */
  public String toStringShortAndOnlyImportantDifferences(final GamePlayer player) {
    final Formatter formatter = new Formatter();
    final UnitType unitType = (UnitType) this.getAttachedTo();

    if (isAir()) {
      formatter.append("Type", "Air");
    } else if (isSea()) {
      formatter.append("Type", "Sea");
    } else {
      formatter.append("Type", "Land");
    }
    final int attackRolls = getAttackRolls(player);
    final int defenseRolls = getDefenseRolls(player);
    final String attack = (attackRolls > 1 ? (attackRolls + "x") : "") + getAttack(player);
    final String defense = (defenseRolls > 1 ? (defenseRolls + "x") : "") + getDefense(player);
    final String movement = String.valueOf(getMovement(player));
    formatter.append("Att | Def | Mov", attack + " | " + defense + " | " + movement);
    if (getHitPoints() > 1) {
      formatter.append("HP", String.valueOf(getHitPoints()));
    }

    if (canProduceUnits() && getCanProduceXUnits() < 0) {
      formatter.append("Can Produce Units up to Territory Value", "");
    } else if (canProduceUnits() && getCanProduceXUnits() > 0) {
      formatter.append("Can Produce Units", String.valueOf(getCanProduceXUnits()));
    }
    addIntegerMapDescription("Creates Units each Turn", getCreatesUnitsList(), formatter);
    addIntegerMapDescription("Produces Resources each Turn", getCreatesResourcesList(), formatter);

    addIntegerMapDescription("Fuel Cost per Movement", getFuelCost(), formatter);
    addIntegerMapDescription("Fuel Cost each Turn if Moved", getFuelFlatCost(), formatter);

    addAaDescription(
        "Targeted Attack",
        getOffensiveAttackAa(player),
        getOffensiveAttackAaMaxDieSides(),
        formatter);
    addAaDescription("Targeted Defense", getAttackAa(player), getAttackAaMaxDieSides(), formatter);

    // TODO: Rework rocket description
    if (isRocket() && playerHasRockets(player)) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Can Rocket Attack, ");
      final int bombingBonus = getBombingBonus();
      if ((getBombingMaxDieSides() != -1 || bombingBonus != 0)
          && Properties.getUseBombingMaxDiceSidesAndBonus(getData().getProperties())) {
        sb.append(bombingBonus != 0 ? bombingBonus + 1 : 1)
            .append("-")
            .append(
                getBombingMaxDieSides() != -1
                    ? getBombingMaxDieSides() + bombingBonus
                    : getData().getDiceSides() + bombingBonus)
            .append(" Rocket Damage, ");
      } else {
        sb.append("1-").append(getData().getDiceSides()).append(" Rocket Damage, ");
      }
      formatter.append(sb.toString(), "");
    }

    if (isInfrastructure()) {
      formatter.append("Can be Captured", "");
    }
    if (isConstruction()) {
      formatter.append("Can be Placed Without Factory", "");
    }

    // TODO: Rework damaged description
    if (canBeDamaged()
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
            getData().getProperties())) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Can be Damaged by Raids, ");
      if (getMaxOperationalDamage() > -1) {
        sb.append(getMaxOperationalDamage()).append(" Max Operational Damage, ");
      }
      if (canProduceUnits() && getCanProduceXUnits() < 0) {
        sb.append("Total Damage up to ")
            .append(getMaxDamage() > -1 ? getMaxDamage() : 2)
            .append("x Territory Value, ");
      } else if (getMaxDamage() > -1) {
        sb.append(getMaxDamage()).append(" Max Total Damage, ");
      }
      if (canDieFromReachingMaxDamage()) {
        sb.append("Dies if Max Damage Reached, ");
      }
      formatter.append(sb.toString(), "");
    } else if (canBeDamaged()) {
      formatter.append("Can be Damaged by Raids", "");
    }

    if (isAirBase() && Properties.getScrambleRulesInEffect(getData().getProperties())) {
      formatter.append("Allows Scrambling", "");
    }
    if (canScramble() && Properties.getScrambleRulesInEffect(getData().getProperties())) {
      formatter.append(
          "Scramble Range",
          String.valueOf(getMaxScrambleDistance() > 0 ? getMaxScrambleDistance() : 1));
    }

    final List<UnitSupportAttachment> supports =
        CollectionUtils.getMatches(
            UnitSupportAttachment.get(unitType),
            Matches.unitSupportAttachmentCanBeUsedByPlayer(player));
    if (supports.size() > 3) {
      formatter.append("Can Provide Support to Units", "");
    } else if (!supports.isEmpty()) {
      final boolean moreThanOneSupportType =
          UnitSupportAttachment.get(getData().getUnitTypeList()).size() > 1;
      for (final UnitSupportAttachment support : supports) {
        if (support.getUnitType() == null || support.getUnitType().isEmpty()) {
          continue;
        }
        final String key =
            "Support on "
                + (support.getOffence() && support.getDefence()
                    ? "Attack & Defense"
                    : (support.getOffence() ? "Attack" : "Defense"));
        final String text =
            support.getBonus()
                + (moreThanOneSupportType ? " " + support.getBonusType().getName() : "")
                + " "
                + (support.getDice() == null
                    ? ""
                    : support
                        .getDice()
                        .replace(":", " & ")
                        .replace("AAroll", "Targeted Roll")
                        .replace("AAstrength", "Targeted Power")
                        .replace("roll", "Roll")
                        .replace("strength", "Power"))
                + " to "
                + support.getNumber()
                + (support.getAllied() && support.getEnemy()
                    ? " Allied & Enemy "
                    : (support.getAllied() ? " Allied " : " Enemy "))
                + (support.getUnitType().size() > 25
                    ? "Units"
                    : MyFormatter.defaultNamedToTextList(support.getUnitType()));
        formatter.append(key, text);
      }
    }

    if (getIsMarine() != 0) {
      formatter.append("Amphibious Attack Modifier", String.valueOf(getIsMarine()));
    }
    if (getCanBlitz(player)) {
      formatter.append("Can Blitz", "");
    }

    if (!getReceivesAbilityWhenWith().isEmpty()) {
      if (getReceivesAbilityWhenWith().size() <= 2) {
        for (final String ability : getReceivesAbilityWhenWith()) {
          final String[] s = splitOnColon(ability);
          formatter.append("Receives Ability", s[0] + " when Paired with " + s[1]);
        }
      } else {
        formatter.append("Receives Abilities when Paired with Other Units", "");
      }
    }

    if (isStrategicBomber()) {
      final StringBuilder sb = new StringBuilder();
      final int bombingBonus = getBombingBonus();
      if ((getBombingMaxDieSides() != -1 || bombingBonus != 0)
          && Properties.getUseBombingMaxDiceSidesAndBonus(getData().getProperties())) {
        sb.append(bombingBonus != 0 ? bombingBonus + 1 : 1)
            .append("-")
            .append(
                getBombingMaxDieSides() != -1
                    ? getBombingMaxDieSides() + bombingBonus
                    : getData().getDiceSides() + bombingBonus);
      } else {
        sb.append("1-").append(getData().getDiceSides());
      }
      sb.append(" Damage");
      formatter.append("Can Perform Raids", sb.toString());
    }

    if (getAirAttack(player) > 0 && (isStrategicBomber() || canEscort() || canAirBattle())) {
      formatter.append(
          "Air Attack", (attackRolls > 1 ? (attackRolls + "x") : "") + getAirAttack(player));
    }
    if (getAirDefense(player) > 0 && (canIntercept() || canAirBattle())) {
      formatter.append(
          "Air Defense", (defenseRolls > 1 ? (defenseRolls + "x") : "") + getAirDefense(player));
    }

    if (getCanEvade()) {
      formatter.append("Can Evade", "");
    }
    if (getIsFirstStrike()) {
      formatter.append("Is First Strike", "");
    }
    if (getCanMoveThroughEnemies()) {
      formatter.append("Can Move Through Enemies", "");
    }
    if (getCanBeMovedThroughByEnemies()) {
      formatter.append("Can Be Moved Through By Enemies", "");
    }
    addLabeledUnitTypes("Can't Target", getCanNotTarget(), formatter);
    addLabeledUnitTypes("Can't Be Targeted By", getCanNotBeTargetedBy(), formatter);
    if (isDestroyer()) {
      formatter.append("Is Anti-Stealth", "");
    }

    if (getCanBombard(player) && getBombard() > 0) {
      formatter.append("Bombard", (attackRolls > 1 ? (attackRolls + "x") : "") + getBombard());
    }

    if (getBlockade() > 0) {
      formatter.append("Blockade Loss", String.valueOf(getBlockade()));
    }

    if (getIsSuicideOnAttack()) {
      formatter.append("Suicide on Attack Unit", "");
    }
    if (getIsSuicideOnDefense()) {
      formatter.append("Suicide on Defense Unit", "");
    }
    if (isSuicideOnHit()) {
      formatter.append("Suicide on Hit Unit", "");
    }
    if (isAir() && (isKamikaze() || Properties.getKamikazeAirplanes(getData().getProperties()))) {
      formatter.append("Is Kamikaze", "Can use all Movement to Attack Target");
    }

    if (isLandTransportable() && playerHasMechInf(player)) {
      formatter.append("Can be Land Transported", "");
    }
    if (isLandTransport() && playerHasMechInf(player)) {
      formatter.append("Is a Land Transport", "");
    }
    if (isAirTransportable() && playerHasParatroopers(player)) {
      formatter.append("Can be Air Transported", "");
    }
    if (isAirTransport() && playerHasParatroopers(player)) {
      formatter.append("Is an Air Transport", "");
    }
    if (isCombatTransport() && getTransportCapacity() > 0) {
      formatter.append("Is a Combat Transport", "");
    } else if (getTransportCapacity() > 0 && isSea()) {
      formatter.append("Is a Sea Transport", "");
    }
    if (getTransportCost() > -1) {
      formatter.append("Transporting Cost", String.valueOf(getTransportCost()));
    }
    if (getTransportCapacity() > 0
        && (isSea()
            || (isAir() && playerHasParatroopers(player))
            || (playerHasMechInf(player) && !isSea() && !isAir()))) {
      formatter.append("Transporting Capacity", String.valueOf(getTransportCapacity()));
    }

    if (getCarrierCost() > -1) {
      formatter.append("Carrier Cost", String.valueOf(getCarrierCost()));
    }
    if (getCarrierCapacity() > 0) {
      formatter.append("Carrier Capacity", String.valueOf(getCarrierCapacity()));
    }

    if (!getWhenCombatDamaged().isEmpty()) {
      formatter.append("When Hit Loses Certain Abilities", "");
    }

    if (getMaxBuiltPerPlayer() > -1) {
      formatter.append("Max Built Allowed", String.valueOf(getMaxBuiltPerPlayer()));
    }

    if (!getRepairsUnits().isEmpty()
        && Properties.getTwoHitPointUnitsRequireRepairFacilities(getData().getProperties())
        && (Properties.getBattleshipsRepairAtBeginningOfRound(getData().getProperties())
            || Properties.getBattleshipsRepairAtEndOfRound(getData().getProperties()))) {
      if (getRepairsUnits().size() <= 4) {
        formatter.append(
            "Can Repair",
            MyFormatter.integerDefaultNamedMapToString(getRepairsUnits(), " ", "=", false));
      } else {
        formatter.append("Can Repair some Units", "");
      }
    }

    if (getGivesMovement().totalValues() > 0
        && Properties.getUnitsMayGiveBonusMovement(getData().getProperties())) {
      if (getGivesMovement().size() <= 4) {
        formatter.append(
            "Can Modify Unit Movement",
            MyFormatter.integerDefaultNamedMapToString(getGivesMovement(), " ", "=", false));
      } else {
        formatter.append("Can Modify Unit Movement", "");
      }
    }

    if (getConsumesUnits().totalValues() == 1) {
      formatter.append(
          "Unit is an Upgrade Of", CollectionUtils.getAny(getConsumesUnits().keySet()).getName());
    } else if (getConsumesUnits().totalValues() > 0) {
      if (getConsumesUnits().size() <= 4) {
        formatter.append(
            "Unit Consumes on Placement",
            MyFormatter.integerDefaultNamedMapToString(getConsumesUnits(), " ", "x", true));
      } else {
        formatter.append("Unit Consumes Other Units on Placement", "");
      }
    }

    if (getRequiresUnits() != null
        && !getRequiresUnits().isEmpty()
        && Properties.getUnitPlacementRestrictions(getData().getProperties())) {
      final List<String> totalUnitsListed = new ArrayList<>();
      for (final String[] list : getRequiresUnits()) {
        totalUnitsListed.addAll(List.of(list));
      }
      if (totalUnitsListed.size() > 4) {
        formatter.append("Has Placement Requirements", "");
      } else {
        formatter.append("Placement Requirements", joinRequiredUnits(getRequiresUnits()));
      }
    }

    if (!getRequiresUnitsToMove().isEmpty()) {
      final List<String> totalUnitsListed = new ArrayList<>();
      for (final String[] list : getRequiresUnitsToMove()) {
        totalUnitsListed.addAll(List.of(list));
      }
      if (totalUnitsListed.size() > 4) {
        formatter.append("Has Movement Requirements", "");
      } else {
        formatter.append("Movement Requirements", joinRequiredUnits(getRequiresUnitsToMove()));
      }
    }

    if (getUnitPlacementRestrictions() != null
        && Properties.getUnitPlacementRestrictions(getData().getProperties())) {
      if (getUnitPlacementRestrictions().length > 4) {
        formatter.append("Has Placement Restrictions", "");
      } else {
        formatter.append("Placement Restrictions", Arrays.toString(getUnitPlacementRestrictions()));
      }
    }
    if (getCanOnlyBePlacedInTerritoryValuedAtX() > 0
        && Properties.getUnitPlacementRestrictions(getData().getProperties())) {
      formatter.append(
          "Must be Placed in Territory with Value of at Least",
          String.valueOf(getCanOnlyBePlacedInTerritoryValuedAtX()));
    }

    if (canNotMoveDuringCombatMove()) {
      formatter.append("Cannot Combat Move", "");
    }

    addStackingLimitDescription(movementLimit, "Moving", formatter);
    addStackingLimitDescription(attackingLimit, "Attacking", formatter);
    addStackingLimitDescription(placementLimit, "Placed", formatter);

    return formatter.toString();
  }

  private static class Formatter {
    private final StringBuilder sb = new StringBuilder();

    void append(final String first, final String second) {
      sb.append(first);
      if (!second.isEmpty()) {
        sb.append(": <b>")
            .append(MyFormatter.addHtmlBreaksAndIndents(second, 100 - second.length(), 100))
            .append("</b>");
      }
      sb.append("<br />");
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }

  private void addLabeledUnitTypes(
      final String label, final Collection<UnitType> unitTypes, final Formatter formatter) {
    if (!unitTypes.isEmpty()) {
      if (unitTypes.size() <= 4) {
        formatter.append(label, MyFormatter.defaultNamedToTextList(unitTypes));
      } else {
        formatter.append(label + " Some Units", "");
      }
    }
  }

  private static <T extends DefaultNamed> void addIntegerMapDescription(
      final String key, final IntegerMap<T> integerMap, final Formatter formatter) {
    if (integerMap != null && !integerMap.isEmpty()) {
      final StringBuilder sb = new StringBuilder();
      if (integerMap.size() > 4) {
        sb.append(integerMap.totalValues());
      } else {
        for (final Entry<T, Integer> entry : integerMap.entrySet()) {
          if (entry.getValue() != 0) {
            sb.append(entry.getValue()).append("x").append(entry.getKey().getName()).append(" ");
          }
        }
      }
      if (sb.length() > 0) {
        formatter.append(key, sb.toString());
      }
    }
  }

  private void addAaDescription(
      final String startOfKey, final int aa, final int aaMaxDieSides, final Formatter formatter) {
    if ((isAaForCombatOnly() || isAaForBombingThisUnitOnly() || isAaForFlyOverOnly()) && (aa > 0)) {
      final String string =
          aa
              + "/"
              + (aaMaxDieSides != -1 ? aaMaxDieSides : getData().getDiceSides())
              + " "
              + getTypeAa()
              + " with "
              + (getMaxAaAttacks() > -1 ? getMaxAaAttacks() : "Unlimited")
              + " Attacks for "
              + (getMaxRoundsAa() > -1 ? getMaxRoundsAa() : "Unlimited")
              + " Rounds";
      formatter.append(startOfKey + getAaKey(), string);
    }
  }

  public int getStackingLimitMax(final Tuple<Integer, String> stackingLimit) {
    int max = stackingLimit.getFirst();
    if (max != Integer.MAX_VALUE) {
      return max;
    }
    // under certain rules (classic rules) there can only be 1 aa gun in a territory.
    final GameProperties properties = getData().getProperties();
    if ((isAaForBombingThisUnitOnly() || isAaForCombatOnly())
        && !(Properties.getWW2V2(properties)
            || Properties.getWW2V3(properties)
            || Properties.getMultipleAaPerTerritory(properties))) {
      max = 1;
    }
    return max;
  }

  private void addStackingLimitDescription(
      final Tuple<Integer, String> stackingLimit,
      final String description,
      final Formatter formatter) {
    if (stackingLimit != null) {
      int max = getStackingLimitMax(stackingLimit);
      if (max < 10000) {
        formatter.append(
            "Max " + stackingLimit.getSecond() + " Units " + description + " per Territory",
            String.valueOf(max));
      }
    }
  }

  private String getAaKey() {
    if (isAaForCombatOnly()
        && isAaForFlyOverOnly()
        && !Properties.getAaTerritoryRestricted(getData().getProperties())) {
      return " for Combat & Move Through";
    } else if (isAaForBombingThisUnitOnly()
        && isAaForFlyOverOnly()
        && !Properties.getAaTerritoryRestricted(getData().getProperties())) {
      return " for Raids & Move Through";
    } else if (isAaForCombatOnly()) {
      return " for Combat";
    } else if (isAaForBombingThisUnitOnly()) {
      return " for Raids";
    } else if (isAaForFlyOverOnly()) {
      return " for Move Through";
    }
    return "";
  }

  private static String joinRequiredUnits(final List<String[]> units) {
    return units.stream()
        .map(required -> required.length == 1 ? required[0] : Arrays.toString(required))
        .collect(Collectors.joining(" or "));
  }

  @Override
  public @Nullable MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "isAir":
        return MutableProperty.of(this::setIsAir, this::setIsAir, this::isAir, this::resetIsAir);
      case IS_SEA:
        return MutableProperty.of(this::setIsSea, this::setIsSea, this::isSea, this::resetIsSea);
      case "movement":
        return MutableProperty.of(
            this::setMovement, this::setMovement, this::getMovement, this::resetMovement);
      case "canBlitz":
        return MutableProperty.of(
            this::setCanBlitz, this::setCanBlitz, this::getCanBlitz, this::resetCanBlitz);
      case "isKamikaze":
        return MutableProperty.of(
            this::setIsKamikaze, this::setIsKamikaze, this::isKamikaze, this::resetIsKamikaze);
      case "canInvadeOnlyFrom":
        return MutableProperty.of(
            this::setCanInvadeOnlyFrom,
            this::setCanInvadeOnlyFrom,
            this::getCanInvadeOnlyFrom,
            this::resetCanInvadeOnlyFrom);
      case "fuelCost":
        return MutableProperty.of(
            this::setFuelCost, this::setFuelCost, this::getFuelCost, this::resetFuelCost);
      case "fuelFlatCost":
        return MutableProperty.of(
            this::setFuelFlatCost,
            this::setFuelFlatCost,
            this::getFuelFlatCost,
            this::resetFuelFlatCost);
      case "canNotMoveDuringCombatMove":
        return MutableProperty.of(
            this::setCanNotMoveDuringCombatMove,
            this::setCanNotMoveDuringCombatMove,
            this::canNotMoveDuringCombatMove,
            this::resetCanNotMoveDuringCombatMove);
      case "movementLimit":
        return MutableProperty.of(
            this::setMovementLimit,
            this::setMovementLimit,
            this::getMovementLimit,
            this::resetMovementLimit);
      case ATTACK_STRENGTH:
        return MutableProperty.of(
            this::setAttack, this::setAttack, this::getAttack, this::resetAttack);
      case DEFENSE_STRENGTH:
        return MutableProperty.of(
            this::setDefense, this::setDefense, this::getDefense, this::resetDefense);
      case "isInfrastructure":
        return MutableProperty.of(
            this::setIsInfrastructure,
            this::setIsInfrastructure,
            this::isInfrastructure,
            this::resetIsInfrastructure);
      case "canBombard":
        return MutableProperty.of(
            this::setCanBombard, this::setCanBombard, this::getCanBombard, this::resetCanBombard);
      case BOMBARD:
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt, this::setBombard, this::getBombard, () -> -1);
      case "isSub":
        return MutableProperty.<Boolean>ofWriteOnly(this::setIsSub, this::setIsSub);
      case "canEvade":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool, this::setCanEvade, this::getCanEvade, () -> false);
      case "isFirstStrike":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool,
            this::setIsFirstStrike,
            this::getIsFirstStrike,
            () -> false);
      case "canNotTarget":
        return MutableProperty.of(
            this::setCanNotTarget,
            this::setCanNotTarget,
            this::getCanNotTarget,
            this::resetCanNotTarget);
      case "canNotBeTargetedBy":
        return MutableProperty.of(
            this::setCanNotBeTargetedBy,
            this::setCanNotBeTargetedBy,
            this::getCanNotBeTargetedBy,
            this::resetCanNotBeTargetedBy);
      case "canMoveThroughEnemies":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool,
            this::setCanMoveThroughEnemies,
            this::getCanMoveThroughEnemies,
            () -> false);
      case "canBeMovedThroughByEnemies":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool,
            this::setCanBeMovedThroughByEnemies,
            this::getCanBeMovedThroughByEnemies,
            () -> false);
      case "isDestroyer":
        return MutableProperty.of(
            this::setIsDestroyer, this::setIsDestroyer, this::isDestroyer, this::resetIsDestroyer);
      case "artillery":
        return MutableProperty.of(
            this::setArtillery, this::setArtillery, this::getArtillery, this::resetArtillery);
      case "artillerySupportable":
        return MutableProperty.of(
            this::setArtillerySupportable,
            this::setArtillerySupportable,
            this::getArtillerySupportable,
            this::resetArtillerySupportable);
      case "unitSupportCount":
        return MutableProperty.of(
            this::setUnitSupportCount,
            this::setUnitSupportCount,
            this::getUnitSupportCount,
            this::resetUnitSupportCount);
      case IS_MARINE:
        return MutableProperty.of(
            this::setIsMarine, this::setIsMarine, this::getIsMarine, this::resetIsMarine);
      case "isSuicide":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool, this::setIsSuicide, this::getIsSuicide, () -> false);
      case "isSuicideOnAttack":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool,
            this::setIsSuicideOnAttack,
            this::getIsSuicideOnAttack,
            () -> false);
      case "isSuicideOnDefense":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool,
            this::setIsSuicideOnDefense,
            this::getIsSuicideOnDefense,
            () -> false);
      case "isSuicideOnHit":
        return MutableProperty.of(
            this::setIsSuicideOnHit,
            this::setIsSuicideOnHit,
            this::isSuicideOnHit,
            this::resetIsSuicideOnHit);
      case "attackingLimit":
        return MutableProperty.of(
            this::setAttackingLimit,
            this::setAttackingLimit,
            this::getAttackingLimit,
            this::resetAttackingLimit);
      case ATTACK_ROLL:
        return MutableProperty.of(
            this::setAttackRolls,
            this::setAttackRolls,
            this::getAttackRolls,
            this::resetAttackRolls);
      case DEFENSE_ROLL:
        return MutableProperty.of(
            this::setDefenseRolls,
            this::setDefenseRolls,
            this::getDefenseRolls,
            this::resetDefenseRolls);
      case CHOOSE_BEST_ROLL:
        return MutableProperty.of(
            this::setChooseBestRoll,
            this::setChooseBestRoll,
            this::getChooseBestRoll,
            this::resetChooseBestRoll);
      case "isCombatTransport":
        return MutableProperty.of(
            this::setIsCombatTransport,
            this::setIsCombatTransport,
            this::isCombatTransport,
            this::resetIsCombatTransport);
      case "transportCapacity":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt,
            this::setTransportCapacity,
            this::getTransportCapacity,
            () -> -1);
      case "transportCost":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt, this::setTransportCost, this::getTransportCost, () -> -1);
      case "carrierCapacity":
        return MutableProperty.of(
            this::setCarrierCapacity,
            this::setCarrierCapacity,
            this::getCarrierCapacity,
            this::resetCarrierCapacity);
      case "carrierCost":
        return MutableProperty.of(
            this::setCarrierCost,
            this::setCarrierCost,
            this::getCarrierCost,
            this::resetCarrierCost);
      case "isAirTransport":
        return MutableProperty.of(
            this::setIsAirTransport,
            this::setIsAirTransport,
            this::isAirTransport,
            this::resetIsAirTransport);
      case "isAirTransportable":
        return MutableProperty.of(
            this::setIsAirTransportable,
            this::setIsAirTransportable,
            this::isAirTransportable,
            this::resetIsAirTransportable);
      case "isLandTransport":
        return MutableProperty.of(
            this::setIsLandTransport,
            this::setIsLandTransport,
            this::isLandTransport,
            this::resetIsLandTransport);
      case "isLandTransportable":
        return MutableProperty.of(
            this::setIsLandTransportable,
            this::setIsLandTransportable,
            this::isLandTransportable,
            this::resetIsLandTransportable);
      case "isAAforCombatOnly":
        return MutableProperty.of(
            this::setIsAaForCombatOnly,
            this::setIsAaForCombatOnly,
            this::isAaForCombatOnly,
            this::resetIsAaForCombatOnly);
      case "isAAforBombingThisUnitOnly":
        return MutableProperty.of(
            this::setIsAaForBombingThisUnitOnly,
            this::setIsAaForBombingThisUnitOnly,
            this::isAaForBombingThisUnitOnly,
            this::resetIsAaForBombingThisUnitOnly);
      case "isAAforFlyOverOnly":
        return MutableProperty.of(
            this::setIsAaForFlyOverOnly,
            this::setIsAaForFlyOverOnly,
            this::isAaForFlyOverOnly,
            this::resetIsAaForFlyOverOnly);
      case "isRocket":
        return MutableProperty.of(
            this::setIsRocket, this::setIsRocket, this::isRocket, this::resetIsRocket);
      case ATTACK_AA:
        return MutableProperty.of(
            this::setAttackAa, this::setAttackAa, this::getAttackAa, this::resetAttackAa);
      case OFFENSIVE_ATTACK_AA:
        return MutableProperty.of(
            this::setOffensiveAttackAa,
            this::setOffensiveAttackAa,
            this::getOffensiveAttackAa,
            this::resetOffensiveAttackAa);
      case ATTACK_AA_MAX_DIE_SIDES:
        return MutableProperty.of(
            this::setAttackAaMaxDieSides,
            this::setAttackAaMaxDieSides,
            this::getAttackAaMaxDieSides,
            this::resetAttackAaMaxDieSides);
      case OFFENSIVE_ATTACK_AA_MAX_DIE_SIDES:
        return MutableProperty.of(
            this::setOffensiveAttackAaMaxDieSides,
            this::setOffensiveAttackAaMaxDieSides,
            this::getOffensiveAttackAaMaxDieSides,
            this::resetOffensiveAttackAaMaxDieSides);
      case MAX_AA_ATTACKS:
        return MutableProperty.of(
            this::setMaxAaAttacks,
            this::setMaxAaAttacks,
            this::getMaxAaAttacks,
            this::resetMaxAaAttacks);
      case "maxRoundsAA":
        return MutableProperty.of(
            this::setMaxRoundsAa,
            this::setMaxRoundsAa,
            this::getMaxRoundsAa,
            this::resetMaxRoundsAa);
      case "typeAA":
        return MutableProperty.ofString(this::setTypeAa, this::getTypeAa, this::resetTypeAa);
      case "targetsAA":
        return MutableProperty.of(
            this::setTargetsAa, this::setTargetsAa, this::getTargetsAa, this::resetTargetsAa);
      case MAY_OVER_STACK_AA:
        return MutableProperty.of(
            this::setMayOverStackAa,
            this::setMayOverStackAa,
            this::getMayOverStackAa,
            this::resetMayOverStackAa);
      case "damageableAA":
        return MutableProperty.of(
            this::setDamageableAa,
            this::setDamageableAa,
            this::getDamageableAa,
            this::resetDamageableAa);
      case "willNotFireIfPresent":
        return MutableProperty.of(
            this::setWillNotFireIfPresent,
            this::setWillNotFireIfPresent,
            this::getWillNotFireIfPresent,
            this::resetWillNotFireIfPresent);
      case "isStrategicBomber":
        return MutableProperty.of(
            this::setIsStrategicBomber,
            this::setIsStrategicBomber,
            this::isStrategicBomber,
            this::resetIsStrategicBomber);
      case "bombingMaxDieSides":
        return MutableProperty.of(
            this::setBombingMaxDieSides,
            this::setBombingMaxDieSides,
            this::getBombingMaxDieSides,
            this::resetBombingMaxDieSides);
      case "bombingBonus":
        return MutableProperty.of(
            this::setBombingBonus,
            this::setBombingBonus,
            this::getBombingBonus,
            this::resetBombingBonus);
      case "canIntercept":
        return MutableProperty.of(
            this::setCanIntercept,
            this::setCanIntercept,
            this::canIntercept,
            this::resetCanIntercept);
      case "requiresAirbaseToIntercept":
        return MutableProperty.of(
            this::setRequiresAirBaseToIntercept,
            this::setRequiresAirBaseToIntercept,
            this::getRequiresAirBaseToIntercept,
            this::resetRequiresAirBaseToIntercept);
      case "canEscort":
        return MutableProperty.of(
            this::setCanEscort, this::setCanEscort, this::canEscort, this::resetCanEscort);
      case "canAirBattle":
        return MutableProperty.of(
            this::setCanAirBattle,
            this::setCanAirBattle,
            this::canAirBattle,
            this::resetCanAirBattle);
      case "airDefense":
        return MutableProperty.of(
            this::setAirDefense, this::setAirDefense, this::getAirDefense, this::resetAirDefense);
      case "airAttack":
        return MutableProperty.of(
            this::setAirAttack, this::setAirAttack, this::getAirAttack, this::resetAirAttack);
      case "bombingTargets":
        return MutableProperty.of(
            this::setBombingTargets,
            this::setBombingTargets,
            this::getBombingTargets,
            this::resetBombingTargets);
      case "canProduceUnits":
        return MutableProperty.of(
            this::setCanProduceUnits,
            this::setCanProduceUnits,
            this::canProduceUnits,
            this::resetCanProduceUnits);
      case "canProduceXUnits":
        return MutableProperty.of(
            this::setCanProduceXUnits,
            this::setCanProduceXUnits,
            this::getCanProduceXUnits,
            this::resetCanProduceXUnits);
      case "createsUnitsList":
        return MutableProperty.of(
            this::setCreatesUnitsList,
            this::setCreatesUnitsList,
            this::getCreatesUnitsList,
            this::resetCreatesUnitsList);
      case "createsResourcesList":
        return MutableProperty.of(
            this::setCreatesResourcesList,
            this::setCreatesResourcesList,
            this::getCreatesResourcesList,
            this::resetCreatesResourcesList);
      case "hitPoints":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt, this::setHitPoints, this::getHitPoints, () -> 1);
      case "canBeDamaged":
        return MutableProperty.of(
            this::setCanBeDamaged,
            this::setCanBeDamaged,
            this::canBeDamaged,
            this::resetCanBeDamaged);
      case "maxDamage":
        return MutableProperty.of(
            this::setMaxDamage, this::setMaxDamage, this::getMaxDamage, this::resetMaxDamage);
      case "maxOperationalDamage":
        return MutableProperty.of(
            this::setMaxOperationalDamage,
            this::setMaxOperationalDamage,
            this::getMaxOperationalDamage,
            this::resetMaxOperationalDamage);
      case "canDieFromReachingMaxDamage":
        return MutableProperty.of(
            this::setCanDieFromReachingMaxDamage,
            this::setCanDieFromReachingMaxDamage,
            this::canDieFromReachingMaxDamage,
            this::resetCanDieFromReachingMaxDamage);
      case "isConstruction":
        return MutableProperty.of(
            this::setIsConstruction,
            this::setIsConstruction,
            this::isConstruction,
            this::resetIsConstruction);
      case "constructionType":
        return MutableProperty.ofString(
            this::setConstructionType, this::getConstructionType, this::resetConstructionType);
      case "constructionsPerTerrPerTypePerTurn":
        return MutableProperty.of(
            this::setConstructionsPerTerrPerTypePerTurn,
            this::setConstructionsPerTerrPerTypePerTurn,
            this::getConstructionsPerTerrPerTypePerTurn,
            this::resetConstructionsPerTerrPerTypePerTurn);
      case "maxConstructionsPerTypePerTerr":
        return MutableProperty.of(
            this::setMaxConstructionsPerTypePerTerr,
            this::setMaxConstructionsPerTypePerTerr,
            this::getMaxConstructionsPerTypePerTerr,
            this::resetMaxConstructionsPerTypePerTerr);
      case "canOnlyBePlacedInTerritoryValuedAtX":
        return MutableProperty.of(
            this::setCanOnlyBePlacedInTerritoryValuedAtX,
            this::setCanOnlyBePlacedInTerritoryValuedAtX,
            this::getCanOnlyBePlacedInTerritoryValuedAtX,
            this::resetCanOnlyBePlacedInTerritoryValuedAtX);
      case "requiresUnits":
        return MutableProperty.of(
            this::setRequiresUnits,
            this::setRequiresUnits,
            this::getRequiresUnits,
            this::resetRequiresUnits);
      case "consumesUnits":
        return MutableProperty.of(
            this::setConsumesUnits,
            this::setConsumesUnits,
            this::getConsumesUnits,
            this::resetConsumesUnits);
      case "requiresUnitsToMove":
        return MutableProperty.of(
            this::setRequiresUnitsToMove,
            this::setRequiresUnitsToMove,
            this::getRequiresUnitsToMove,
            this::resetRequiresUnitsToMove);
      case "unitPlacementRestrictions":
        return MutableProperty.of(
            this::setUnitPlacementRestrictions,
            this::setUnitPlacementRestrictions,
            this::getUnitPlacementRestrictions,
            this::resetUnitPlacementRestrictions);
      case "maxBuiltPerPlayer":
        return MutableProperty.of(
            this::setMaxBuiltPerPlayer,
            this::setMaxBuiltPerPlayer,
            this::getMaxBuiltPerPlayer,
            this::resetMaxBuiltPerPlayer);
      case "placementLimit":
        return MutableProperty.of(
            this::setPlacementLimit,
            this::setPlacementLimit,
            this::getPlacementLimit,
            this::resetPlacementLimit);
      case "canScramble":
        return MutableProperty.of(
            this::setCanScramble, this::setCanScramble, this::canScramble, this::resetCanScramble);
      case "isAirBase":
        return MutableProperty.of(
            this::setIsAirBase, this::setIsAirBase, this::isAirBase, this::resetIsAirBase);
      case "maxScrambleDistance":
        return MutableProperty.of(
            this::setMaxScrambleDistance,
            this::setMaxScrambleDistance,
            this::getMaxScrambleDistance,
            this::resetMaxScrambleDistance);
      case "maxScrambleCount":
        return MutableProperty.of(
            this::setMaxScrambleCount,
            this::setMaxScrambleCount,
            this::getMaxScrambleCount,
            this::resetMaxScrambleCount);
      case "maxInterceptCount":
        return MutableProperty.of(
            this::setMaxInterceptCount,
            this::setMaxInterceptCount,
            this::getMaxInterceptCount,
            this::resetMaxInterceptCount);
      case "blockade":
        return MutableProperty.of(
            this::setBlockade, this::setBlockade, this::getBlockade, this::resetBlockade);
      case "repairsUnits":
        return MutableProperty.of(
            this::setRepairsUnits,
            this::setRepairsUnits,
            this::getRepairsUnits,
            this::resetRepairsUnits);
      case "givesMovement":
        return MutableProperty.of(
            this::setGivesMovement,
            this::setGivesMovement,
            this::getGivesMovement,
            this::resetGivesMovement);
      case "destroyedWhenCapturedBy":
        return MutableProperty.of(
            this::setDestroyedWhenCapturedBy,
            this::setDestroyedWhenCapturedBy,
            this::getDestroyedWhenCapturedBy,
            this::resetDestroyedWhenCapturedBy);
      case "whenHitPointsDamagedChangesInto":
        return MutableProperty.of(
            this::setWhenHitPointsDamagedChangesInto,
            this::setWhenHitPointsDamagedChangesInto,
            this::getWhenHitPointsDamagedChangesInto,
            this::resetWhenHitPointsDamagedChangesInto);
      case "whenHitPointsRepairedChangesInto":
        return MutableProperty.of(
            this::setWhenHitPointsRepairedChangesInto,
            this::setWhenHitPointsRepairedChangesInto,
            this::getWhenHitPointsRepairedChangesInto,
            this::resetWhenHitPointsRepairedChangesInto);
      case "whenCapturedChangesInto":
        return MutableProperty.of(
            this::setWhenCapturedChangesInto,
            this::setWhenCapturedChangesInto,
            this::getWhenCapturedChangesInto,
            this::resetWhenCapturedChangesInto);
      case "whenCapturedSustainsDamage":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt,
            this::setWhenCapturedSustainsDamage,
            this::getWhenCapturedSustainsDamage,
            () -> 0);
      case "canBeCapturedOnEnteringBy":
        return MutableProperty.of(
            this::setCanBeCapturedOnEnteringBy,
            this::setCanBeCapturedOnEnteringBy,
            this::getCanBeCapturedOnEnteringBy,
            this::resetCanBeCapturedOnEnteringBy);
      case "canBeGivenByTerritoryTo":
        return MutableProperty.of(
            this::setCanBeGivenByTerritoryTo,
            this::setCanBeGivenByTerritoryTo,
            this::getCanBeGivenByTerritoryTo,
            this::resetCanBeGivenByTerritoryTo);
      case "whenCombatDamaged":
        return MutableProperty.of(
            this::setWhenCombatDamaged,
            this::setWhenCombatDamaged,
            this::getWhenCombatDamaged,
            this::resetWhenCombatDamaged);
      case "receivesAbilityWhenWith":
        return MutableProperty.of(
            this::setReceivesAbilityWhenWith,
            this::setReceivesAbilityWhenWith,
            this::getReceivesAbilityWhenWith,
            this::resetReceivesAbilityWhenWith);
      case "special":
        return MutableProperty.of(
            this::setSpecial, this::setSpecial, this::getSpecial, this::resetSpecial);
      case "tuv":
        return MutableProperty.of(this::setTuv, this::setTuv, this::getTuv, this::resetTuv);
      case "isFactory":
        return MutableProperty.<Boolean>ofWriteOnly(this::setIsFactory, this::setIsFactory);
      case "isAA":
        return MutableProperty.<Boolean>ofWriteOnly(this::setIsAa, this::setIsAa);
      case "destroyedWhenCapturedFrom":
        return MutableProperty.ofWriteOnlyString(this::setDestroyedWhenCapturedFrom);
      case "unitPlacementOnlyAllowedIn":
        return MutableProperty.ofWriteOnlyString(this::setUnitPlacementOnlyAllowedIn);
      case "isAAmovement":
        return MutableProperty.<Boolean>ofWriteOnly(this::setIsAaMovement, this::setIsAaMovement);
      case "isTwoHit":
        return MutableProperty.<Boolean>ofWriteOnly(this::setIsTwoHit, this::setIsTwoHit);
      case "canRetreatOnStalemate":
        return MutableProperty.of(
            this::setCanRetreatOnStalemate, this::setCanRetreatOnStalemate,
            this::getCanRetreatOnStalemate, this::resetCanRetreatOnStalemate);
      default:
        return null;
    }
  }
}
