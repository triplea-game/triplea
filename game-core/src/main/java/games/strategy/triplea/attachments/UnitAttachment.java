package games.strategy.triplea.attachments;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.DefaultNamed;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
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
import java.util.stream.StreamSupport;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/** Despite the misleading name, this attaches not to individual Units but to UnitTypes. */
public class UnitAttachment extends DefaultAttachment {
  public static final String UNITSMAYNOTLANDONCARRIER = "unitsMayNotLandOnCarrier";
  public static final String UNITSMAYNOTLEAVEALLIEDCARRIER = "unitsMayNotLeaveAlliedCarrier";
  private static final long serialVersionUID = -2946748686268541820L;

  // movement related
  private boolean isAir = false;
  private boolean isSea = false;
  private int movement = 0;
  private boolean canBlitz = false;
  private boolean isKamikaze = false;
  // a colon delimited list of transports where this unit may invade from, it supports "none"
  // and if empty it allows you to invade from all
  private String[] canInvadeOnlyFrom = null;
  private IntegerMap<Resource> fuelCost = new IntegerMap<>();
  private IntegerMap<Resource> fuelFlatCost = new IntegerMap<>();
  private boolean canNotMoveDuringCombatMove = false;
  private Tuple<Integer, String> movementLimit = null;

  // combat related
  private int attack = 0;
  private int defense = 0;
  private boolean isInfrastructure = false;
  private boolean canBombard = false;
  private int bombard = -1;
  private boolean artillery = false;
  private boolean artillerySupportable = false;
  private int unitSupportCount = -1;
  private int isMarine = 0;
  private boolean isSuicideOnAttack = false;
  private boolean isSuicideOnDefense = false;
  private boolean isSuicideOnHit = false;
  private Tuple<Integer, String> attackingLimit = null;
  private int attackRolls = 1;
  private int defenseRolls = 1;
  private boolean chooseBestRoll = false;
  private Boolean canRetreatOnStalemate;

  // sub/destroyer related
  private boolean canEvade = false;
  private boolean isFirstStrike = false;
  private Set<UnitType> canNotTarget = new HashSet<>();
  private Set<UnitType> canNotBeTargetedBy = new HashSet<>();
  private boolean canMoveThroughEnemies = false;
  private boolean canBeMovedThroughByEnemies = false;
  private boolean isDestroyer = false;

  // transportation related
  private boolean isCombatTransport = false;
  // -1 if cant transport
  private int transportCapacity = -1;
  // -1 if cant be transported
  private int transportCost = -1;
  // -1 if cant act as a carrier
  private int carrierCapacity = -1;
  // -1 if cant land on a carrier
  private int carrierCost = -1;
  private boolean isAirTransport = false;
  private boolean isAirTransportable = false;
  private boolean isLandTransport = false;
  private boolean isLandTransportable = false;

  // aa related
  // "isAA" and "isAAmovement" are also valid setters, used as shortcuts for calling multiple aa
  // related setters. Must
  // keep.
  private boolean isAaForCombatOnly = false;
  private boolean isAaForBombingThisUnitOnly = false;
  private boolean isAaForFlyOverOnly = false;
  private boolean isRocket = false;
  private int attackAa = 1;
  private int offensiveAttackAa = 0;
  private int attackAaMaxDieSides = -1;
  private int offensiveAttackAaMaxDieSides = -1;
  // -1 means infinite
  private int maxAaAttacks = -1;
  // -1 means infinite
  private int maxRoundsAa = 1;
  // default value for when it is not set
  private String typeAa = "AA";
  // null means targeting air units only
  private Set<UnitType> targetsAa = null;
  // if false, we cannot shoot more times than there are number of planes
  private boolean mayOverStackAa = false;
  // if false, we instantly kill anything our AA shot hits
  private boolean damageableAa = false;
  // if these enemy units are present, the gun does not fire at all
  private Set<UnitType> willNotFireIfPresent = new HashSet<>();

  // strategic bombing related
  private boolean isStrategicBomber = false;
  private int bombingMaxDieSides = -1;
  private int bombingBonus = 0;
  private boolean canIntercept = false;
  private boolean requiresAirBaseToIntercept = false;
  private boolean canEscort = false;
  private boolean canAirBattle = false;
  private int airDefense = 0;
  private int airAttack = 0;
  // null means they can target any unit that can be damaged
  private Set<UnitType> bombingTargets = null;

  // production related
  // this has been split into canProduceUnits, isConstruction, canBeDamaged, and isInfrastructure
  private boolean canProduceUnits = false;
  // -1 means either it can't produce any, or it produces at the value of the territory it is
  // located in
  private int canProduceXUnits = -1;
  private IntegerMap<UnitType> createsUnitsList = new IntegerMap<>();
  private IntegerMap<Resource> createsResourcesList = new IntegerMap<>();

  // damage related
  private int hitPoints = 1;
  private boolean canBeDamaged = false;
  // this is bombing damage, not hitpoints. default of 2 means that factories will take 2x the
  // territory value
  // they are in, of damage.
  private int maxDamage = 2;
  // -1 if can't be disabled
  private int maxOperationalDamage = -1;
  private boolean canDieFromReachingMaxDamage = false;

  // placement related
  private boolean isConstruction = false;
  // can be any String except for "none" if isConstruction is true
  private String constructionType = "none";
  // -1 if not set, is meaningless
  private int constructionsPerTerrPerTypePerTurn = -1;
  // -1 if not set, is meaningless
  private int maxConstructionsPerTypePerTerr = -1;
  // -1 means anywhere
  private int canOnlyBePlacedInTerritoryValuedAtX = -1;
  // multiple colon delimited lists of the unit combos required for this unit to be built somewhere.
  // (units must be in
  // same territory, owned by player, not be disabled)
  private List<String[]> requiresUnits = new ArrayList<>();
  private IntegerMap<UnitType> consumesUnits = new IntegerMap<>();
  // multiple colon delimited lists of the unit combos required for
  // this unit to move into a territory. (units must be owned by player, not be disabled)
  private List<String[]> requiresUnitsToMove = new ArrayList<>();
  // a colon delimited list of territories where this unit may not be placed
  // also an allowed setter is "setUnitPlacementOnlyAllowedIn",
  // which just creates unitPlacementRestrictions with an inverted list of territories
  private String[] unitPlacementRestrictions = null;
  // -1 if infinite (infinite is default)
  private int maxBuiltPerPlayer = -1;
  private Tuple<Integer, String> placementLimit = null;

  // scrambling related
  private boolean canScramble = false;
  private boolean isAirBase = false;
  // -1 if can't scramble
  private int maxScrambleDistance = -1;
  // -1 for infinite
  private int maxScrambleCount = -1;
  // -1 for infinite
  private int maxInterceptCount = -1;

  // special abilities
  private int blockade = 0;
  // a colon delimited list of the units this unit can repair.
  // (units must be in same territory, unless this unit is land and the repaired unit is sea)
  private IntegerMap<UnitType> repairsUnits = new IntegerMap<>();
  private IntegerMap<UnitType> givesMovement = new IntegerMap<>();
  private List<Tuple<String, GamePlayer>> destroyedWhenCapturedBy = new ArrayList<>();
  // also an allowed setter is "setDestroyedWhenCapturedFrom" which will just create
  // destroyedWhenCapturedBy with a
  // specific list
  private Map<Integer, Tuple<Boolean, UnitType>> whenHitPointsDamagedChangesInto = new HashMap<>();
  private Map<Integer, Tuple<Boolean, UnitType>> whenHitPointsRepairedChangesInto = new HashMap<>();
  private Map<String, Tuple<String, IntegerMap<UnitType>>> whenCapturedChangesInto =
      new LinkedHashMap<>();
  private int whenCapturedSustainsDamage = 0;
  private List<GamePlayer> canBeCapturedOnEnteringBy = new ArrayList<>();
  private List<GamePlayer> canBeGivenByTerritoryTo = new ArrayList<>();
  // a set of information for dealing with special abilities or loss of abilities when a unit takes
  // x-y amount of damage
  private List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> whenCombatDamaged =
      new ArrayList<>();
  // a kind of support attachment for giving actual unit attachment abilities or other to a unit,
  // when in the presence or on the same route with another unit
  private List<String> receivesAbilityWhenWith = new ArrayList<>();
  // currently used for: placement in original territories only
  private Set<String> special = new HashSet<>();
  // Manually set TUV
  private int tuv = -1;

  public UnitAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static UnitAttachment get(final UnitType type) {
    return get(type, Constants.UNIT_ATTACHMENT_NAME);
  }

  static UnitAttachment get(final UnitType type, final String nameOfAttachment) {
    return getAttachment(type, nameOfAttachment, UnitAttachment.class);
  }

  private static Collection<UnitType> getUnitTypesFromUnitList(final Collection<Unit> units) {
    final Collection<UnitType> types = new ArrayList<>();
    for (final Unit u : units) {
      if (!types.contains(u.getType())) {
        types.add(u.getType());
      }
    }
    return types;
  }

  private void setCanIntercept(final String value) {
    canIntercept = getBool(value);
  }

  private void setCanIntercept(final Boolean value) {
    canIntercept = value;
  }

  public boolean getCanIntercept() {
    return canIntercept;
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

  public boolean getCanEscort() {
    return canEscort;
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

  public boolean getCanAirBattle() {
    return canAirBattle;
  }

  private void resetCanAirBattle() {
    canAirBattle = false;
  }

  private void setAirDefense(final String value) {
    airDefense = getInt(value);
  }

  private void setAirDefense(final Integer value) {
    airDefense = value;
  }

  private int getAirDefense() {
    return airDefense;
  }

  public int getAirDefense(final GamePlayer player) {
    return (Math.min(
        getData().getDiceSides(),
        Math.max(
            0,
            airDefense
                + TechAbilityAttachment.getAirDefenseBonus(
                    (UnitType) this.getAttachedTo(), player, getData()))));
  }

  private void resetAirDefense() {
    airDefense = 0;
  }

  private void setAirAttack(final String value) {
    airAttack = getInt(value);
  }

  private void setAirAttack(final Integer value) {
    airAttack = value;
  }

  private int getAirAttack() {
    return airAttack;
  }

  public int getAirAttack(final GamePlayer player) {
    return (Math.min(
        getData().getDiceSides(),
        Math.max(
            0,
            airAttack
                + TechAbilityAttachment.getAirAttackBonus(
                    (UnitType) this.getAttachedTo(), player, getData()))));
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

  public boolean getIsAirTransport() {
    return isAirTransport;
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

  public boolean getIsAirTransportable() {
    return isAirTransportable;
  }

  private void resetIsAirTransportable() {
    isAirTransportable = false;
  }

  private void setCanBeGivenByTerritoryTo(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final GamePlayer tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        canBeGivenByTerritoryTo.add(tempPlayer);
      } else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
        canBeGivenByTerritoryTo.clear();
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setCanBeGivenByTerritoryTo(final List<GamePlayer> value) {
    canBeGivenByTerritoryTo = value;
  }

  public List<GamePlayer> getCanBeGivenByTerritoryTo() {
    return canBeGivenByTerritoryTo;
  }

  private void resetCanBeGivenByTerritoryTo() {
    canBeGivenByTerritoryTo = new ArrayList<>();
  }

  private void setCanBeCapturedOnEnteringBy(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final GamePlayer tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        canBeCapturedOnEnteringBy.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setCanBeCapturedOnEnteringBy(final List<GamePlayer> value) {
    canBeCapturedOnEnteringBy = value;
  }

  public List<GamePlayer> getCanBeCapturedOnEnteringBy() {
    return canBeCapturedOnEnteringBy;
  }

  private void resetCanBeCapturedOnEnteringBy() {
    canBeCapturedOnEnteringBy = new ArrayList<>();
  }

  private void setWhenHitPointsDamagedChangesInto(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 3) {
      throw new GameParseException(
          "setWhenHitPointsDamagedChangesInto must have damage:translateAttributes:unitType "
              + thisErrorMsg());
    }
    final UnitType unitType = getData().getUnitTypeList().getUnitType(s[2]);
    if (unitType == null) {
      throw new GameParseException(
          "setWhenHitPointsDamagedChangesInto: No unit type: " + s[2] + thisErrorMsg());
    }
    whenHitPointsDamagedChangesInto.put(getInt(s[0]), Tuple.of(getBool(s[1]), unitType));
  }

  private void setWhenHitPointsDamagedChangesInto(
      final Map<Integer, Tuple<Boolean, UnitType>> value) {
    whenHitPointsDamagedChangesInto = value;
  }

  public Map<Integer, Tuple<Boolean, UnitType>> getWhenHitPointsDamagedChangesInto() {
    return whenHitPointsDamagedChangesInto;
  }

  private void resetWhenHitPointsDamagedChangesInto() {
    whenHitPointsDamagedChangesInto = new HashMap<>();
  }

  private void setWhenHitPointsRepairedChangesInto(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 3) {
      throw new GameParseException(
          "setWhenHitPointsRepairedChangesInto must have damage:translateAttributes:unitType "
              + thisErrorMsg());
    }
    final UnitType unitType = getData().getUnitTypeList().getUnitType(s[2]);
    if (unitType == null) {
      throw new GameParseException(
          "setWhenHitPointsRepairedChangesInto: No unit type: " + s[2] + thisErrorMsg());
    }
    whenHitPointsRepairedChangesInto.put(getInt(s[0]), Tuple.of(getBool(s[1]), unitType));
  }

  private void setWhenHitPointsRepairedChangesInto(
      final Map<Integer, Tuple<Boolean, UnitType>> value) {
    whenHitPointsRepairedChangesInto = value;
  }

  public Map<Integer, Tuple<Boolean, UnitType>> getWhenHitPointsRepairedChangesInto() {
    return whenHitPointsRepairedChangesInto;
  }

  private void resetWhenHitPointsRepairedChangesInto() {
    whenHitPointsRepairedChangesInto = new HashMap<>();
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
      final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
      if (ut == null) {
        throw new GameParseException(
            "whenCapturedChangesInto: No unit named: " + s[i] + thisErrorMsg());
      }
      unitsToMake.put(ut, getInt(s[i + 1]));
    }
    whenCapturedChangesInto.put(s[0] + ":" + s[1], Tuple.of(s[2], unitsToMake));
  }

  private void setWhenCapturedChangesInto(
      final Map<String, Tuple<String, IntegerMap<UnitType>>> value) {
    whenCapturedChangesInto = value;
  }

  public Map<String, Tuple<String, IntegerMap<UnitType>>> getWhenCapturedChangesInto() {
    return whenCapturedChangesInto;
  }

  private void resetWhenCapturedChangesInto() {
    whenCapturedChangesInto = new LinkedHashMap<>();
  }

  private void setWhenCapturedSustainsDamage(final int s) {
    whenCapturedSustainsDamage = s;
  }

  public int getWhenCapturedSustainsDamage() {
    return whenCapturedSustainsDamage;
  }

  private void setDestroyedWhenCapturedBy(final String initialValue) throws GameParseException {
    // We can prefix this value with "BY" or "FROM" to change the setting. If no setting, default to
    // "BY" since this
    // this is called by destroyedWhenCapturedBy
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
      final GamePlayer tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        destroyedWhenCapturedBy.add(Tuple.of(byOrFrom, tempPlayer));
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
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
    return destroyedWhenCapturedBy;
  }

  private void resetDestroyedWhenCapturedBy() {
    destroyedWhenCapturedBy = new ArrayList<>();
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
    return canBlitz
        || TechAbilityAttachment.getUnitAbilitiesGained(
            TechAbilityAttachment.ABILITY_CAN_BLITZ,
            (UnitType) this.getAttachedTo(),
            player,
            getData());
  }

  private void resetCanBlitz() {
    canBlitz = false;
  }

  private void setIsSub(final String s) {
    setIsSub(getBool(s));
  }

  private void setIsSub(final Boolean s) {
    setCanEvade(s);
    setIsFirstStrike(s);
    setCanMoveThroughEnemies(s && Properties.getSubmersibleSubs(getData()));
    setCanBeMovedThroughByEnemies(s && Properties.getIgnoreSubInMovement(getData()));
    if (s) {
      canNotTarget = null;
      canNotBeTargetedBy = Properties.getAirAttackSubRestricted(getData()) ? null : new HashSet<>();
    } else {
      resetCanNotTarget();
      resetCanNotBeTargetedBy();
    }
  }

  private void setCanEvade(final Boolean s) {
    canEvade = s;
  }

  public boolean getCanEvade() {
    return canEvade;
  }

  private void setIsFirstStrike(final Boolean s) {
    isFirstStrike = s;
  }

  public boolean getIsFirstStrike() {
    return isFirstStrike;
  }

  private void setCanMoveThroughEnemies(final Boolean s) {
    canMoveThroughEnemies = s;
  }

  public boolean getCanMoveThroughEnemies() {
    return canMoveThroughEnemies;
  }

  private void setCanBeMovedThroughByEnemies(final Boolean s) {
    canBeMovedThroughByEnemies = s;
  }

  public boolean getCanBeMovedThroughByEnemies() {
    return canBeMovedThroughByEnemies;
  }

  private void setCanNotTarget(final String value) throws GameParseException {
    if (canNotTarget == null) {
      throw new GameParseException(
          "Can't use canNotTarget with isSub/isSuicide, replace isSub with individual sub "
              + "properties or isSuicide with isSuicideOnAttack/isSuicideOnDefense: "
              + thisErrorMsg());
    }
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("canNotTarget: no such unit type: " + u + thisErrorMsg());
      }
      canNotTarget.add(ut);
    }
  }

  private void setCanNotTarget(final Set<UnitType> value) {
    canNotTarget = value;
  }

  public Set<UnitType> getCanNotTarget() {
    if (canNotTarget == null) {
      final Predicate<UnitType> unitTypeMatch =
          (isSuicideOnAttack && isFirstStrike)
              ? Matches.unitTypeIsSuicideOnAttack().or(Matches.unitTypeIsSuicideOnDefense())
              : Matches.unitTypeIsAir();
      canNotTarget =
          new HashSet<>(
              CollectionUtils.getMatches(
                  getData().getUnitTypeList().getAllUnitTypes(), unitTypeMatch));
    }
    return canNotTarget;
  }

  private void resetCanNotTarget() {
    canNotTarget = new HashSet<>();
  }

  private void setCanNotBeTargetedBy(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException(
            "canNotBeTargetedBy: no such unit type: " + u + thisErrorMsg());
      }
      canNotBeTargetedBy.add(ut);
    }
  }

  private void setCanNotBeTargetedBy(final Set<UnitType> value) {
    canNotBeTargetedBy = value;
  }

  public Set<UnitType> getCanNotBeTargetedBy() {
    if (canNotBeTargetedBy == null) {
      canNotBeTargetedBy =
          new HashSet<>(
              CollectionUtils.getMatches(
                  getData().getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAir()));
    }
    return canNotBeTargetedBy;
  }

  private void resetCanNotBeTargetedBy() {
    canNotBeTargetedBy = new HashSet<>();
  }

  private void setIsCombatTransport(final String s) {
    isCombatTransport = getBool(s);
  }

  private void setIsCombatTransport(final Boolean s) {
    isCombatTransport = s;
  }

  public boolean getIsCombatTransport() {
    return isCombatTransport;
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

  public boolean getIsStrategicBomber() {
    return isStrategicBomber;
  }

  private void resetIsStrategicBomber() {
    isStrategicBomber = false;
  }

  private void setIsDestroyer(final String s) {
    isDestroyer = getBool(s);
  }

  private void setIsDestroyer(final Boolean s) {
    isDestroyer = s;
  }

  public boolean getIsDestroyer() {
    return isDestroyer;
  }

  private void resetIsDestroyer() {
    isDestroyer = false;
  }

  public void setCanBombard(final String s) {
    canBombard = getBool(s);
  }

  private void setCanBombard(final Boolean s) {
    canBombard = s;
  }

  private boolean getCanBombard() {
    return canBombard;
  }

  public boolean getCanBombard(final GamePlayer player) {
    return canBombard
        || TechAbilityAttachment.getUnitAbilitiesGained(
            TechAbilityAttachment.ABILITY_CAN_BOMBARD,
            (UnitType) this.getAttachedTo(),
            player,
            getData());
  }

  private void resetCanBombard() {
    canBombard = false;
  }

  private void setIsAir(final String s) {
    isAir = getBool(s);
  }

  private void setIsAir(final Boolean s) {
    isAir = s;
  }

  public boolean getIsAir() {
    return isAir;
  }

  private void resetIsAir() {
    isAir = false;
  }

  private void setIsSea(final String s) {
    isSea = getBool(s);
  }

  private void setIsSea(final Boolean s) {
    isSea = s;
  }

  public boolean getIsSea() {
    return isSea;
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
    if (s) {
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

  public boolean getCanProduceUnits() {
    return canProduceUnits;
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

  public int getCanProduceXUnits() {
    return canProduceXUnits;
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

  public int getCanOnlyBePlacedInTerritoryValuedAtX() {
    return canOnlyBePlacedInTerritoryValuedAtX;
  }

  private void resetCanOnlyBePlacedInTerritoryValuedAtX() {
    canOnlyBePlacedInTerritoryValuedAtX = -1;
  }

  private void setUnitPlacementRestrictions(final String value) throws GameParseException {
    if (value == null) {
      unitPlacementRestrictions = null;
      return;
    }
    final Collection<Territory> restrictedTerritories = getListedTerritories(splitOnColon(value));
    unitPlacementRestrictions =
        restrictedTerritories.stream().map(Territory::getName).toArray(String[]::new);
  }

  private void setUnitPlacementRestrictions(final String[] value) {
    unitPlacementRestrictions = value;
  }

  public String[] getUnitPlacementRestrictions() {
    return unitPlacementRestrictions;
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
    if (s.length <= 0) {
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
      final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
      if (ut == null) {
        throw new GameParseException("No unit called:" + s[i] + thisErrorMsg());
      }
      repairsUnits.put(ut, amount);
    }
  }

  private void setRepairsUnits(final IntegerMap<UnitType> value) {
    repairsUnits = value;
  }

  public IntegerMap<UnitType> getRepairsUnits() {
    return repairsUnits;
  }

  private void resetRepairsUnits() {
    repairsUnits = new IntegerMap<>();
  }

  private void setSpecial(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    for (final String option : s) {
      if (!(option.equals("none") || option.equals("canOnlyPlaceInOriginalTerritories"))) {
        throw new GameParseException("special does not allow: " + option + thisErrorMsg());
      }
      special.add(option);
    }
  }

  private void setSpecial(final Set<String> value) {
    special = value;
  }

  public Set<String> getSpecial() {
    return special;
  }

  private void resetSpecial() {
    special = new HashSet<>();
  }

  private void setCanInvadeOnlyFrom(final String value) {
    if (value == null) {
      canInvadeOnlyFrom = null;
      return;
    }
    final String[] canOnlyInvadeFrom = splitOnColon(value);
    if (canOnlyInvadeFrom[0].toLowerCase().equals("none")) {
      canInvadeOnlyFrom = new String[] {"none"};
      return;
    }
    if (canOnlyInvadeFrom[0].toLowerCase().equals("all")) {
      canInvadeOnlyFrom = new String[] {"all"};
      return;
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
    requiresUnits.add(splitOnColon(value));
  }

  private void setRequiresUnits(final List<String[]> value) {
    requiresUnits = value;
  }

  public List<String[]> getRequiresUnits() {
    return requiresUnits;
  }

  private void resetRequiresUnits() {
    requiresUnits = new ArrayList<>();
  }

  private void setRequiresUnitsToMove(final String value) throws GameParseException {
    final String[] array = splitOnColon(value);
    if (array.length == 0) {
      throw new GameParseException(
          "requiresUnitsToMove must have at least 1 unit type" + thisErrorMsg());
    }
    for (final String s : array) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(s);
      if (ut == null) {
        throw new GameParseException("No unit called:" + s + thisErrorMsg());
      }
    }
    requiresUnitsToMove.add(array);
  }

  private void setRequiresUnitsToMove(final List<String[]> value) {
    requiresUnitsToMove = value;
  }

  public List<String[]> getRequiresUnitsToMove() {
    return requiresUnitsToMove;
  }

  private void resetRequiresUnitsToMove() {
    requiresUnitsToMove = new ArrayList<>();
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
      effectNum = Tuple.of(s[2], null);
    } else {
      effectNum = Tuple.of(s[2], s[3]);
    }
    whenCombatDamaged.add(Tuple.of(fromTo, effectNum));
  }

  private void setWhenCombatDamaged(
      final List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> value) {
    whenCombatDamaged = value;
  }

  public List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> getWhenCombatDamaged() {
    return whenCombatDamaged;
  }

  private void resetWhenCombatDamaged() {
    whenCombatDamaged = new ArrayList<>();
  }

  private void setReceivesAbilityWhenWith(final String value) {
    receivesAbilityWhenWith.add(value);
  }

  private void setReceivesAbilityWhenWith(final List<String> value) {
    receivesAbilityWhenWith = value;
  }

  public List<String> getReceivesAbilityWhenWith() {
    return receivesAbilityWhenWith;
  }

  private void resetReceivesAbilityWhenWith() {
    receivesAbilityWhenWith = new ArrayList<>();
  }

  private static IntegerMap<Tuple<String, String>> getReceivesAbilityWhenWithMap(
      final Collection<Unit> units, final String filterForAbility, final GameData data) {
    final IntegerMap<Tuple<String, String>> map = new IntegerMap<>();
    final Collection<UnitType> canReceive =
        getUnitTypesFromUnitList(
            CollectionUtils.getMatches(units, Matches.unitCanReceiveAbilityWhenWith()));
    for (final UnitType ut : canReceive) {
      final Collection<String> receives = UnitAttachment.get(ut).getReceivesAbilityWhenWith();
      for (final String receive : receives) {
        final String[] s = splitOnColon(receive);
        if (filterForAbility != null && !filterForAbility.equals(s[0])) {
          continue;
        }
        map.put(
            Tuple.of(s[0], s[1]),
            CollectionUtils.countMatches(
                units, Matches.unitIsOfType(data.getUnitTypeList().getUnitType(s[1]))));
      }
    }
    return map;
  }

  /**
   * Returns the subset of {@code units} that will receive the ability {@code filterForAbility} when
   * they are with, or on the same route as, another unit.
   */
  public static Collection<Unit> getUnitsWhichReceivesAbilityWhenWith(
      final Collection<Unit> units, final String filterForAbility, final GameData data) {
    if (units.stream().noneMatch(Matches.unitCanReceiveAbilityWhenWith())) {
      return new ArrayList<>();
    }
    final Collection<Unit> unitsCopy = new ArrayList<>(units);
    final Set<Unit> whichReceiveNoDuplicates = new HashSet<>();
    final IntegerMap<Tuple<String, String>> whichGive =
        getReceivesAbilityWhenWithMap(unitsCopy, filterForAbility, data);
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

  public boolean getIsConstruction() {
    return isConstruction;
  }

  private void resetIsConstruction() {
    isConstruction = false;
  }

  private void setConstructionType(final String s) {
    constructionType = s;
  }

  public String getConstructionType() {
    return constructionType;
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

  public int getConstructionsPerTerrPerTypePerTurn() {
    return constructionsPerTerrPerTypePerTurn;
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

  public int getMaxConstructionsPerTypePerTerr() {
    return maxConstructionsPerTypePerTerr;
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

  private void setIsMarine(final Integer s) {
    isMarine = s;
  }

  public int getIsMarine() {
    return isMarine;
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

  public boolean getIsLandTransportable() {
    return isLandTransportable;
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

  public boolean isLandTransport() {
    return isLandTransport;
  }

  public boolean getIsLandTransport() {
    return isLandTransport;
  }

  private void resetIsLandTransport() {
    isLandTransport = false;
  }

  private void setTransportCapacity(final int s) {
    transportCapacity = s;
  }

  public int getTransportCapacity() {
    return transportCapacity;
  }

  private void setIsTwoHit(final String s) {
    setIsTwoHit(getBool(s));
  }

  private void setIsTwoHit(final boolean s) {
    hitPoints = s ? 2 : 1;
  }

  private void setHitPoints(final int value) {
    hitPoints = value;
  }

  public int getHitPoints() {
    return hitPoints;
  }

  private void setTransportCost(final Integer s) {
    transportCost = s;
  }

  public int getTransportCost() {
    return transportCost;
  }

  private void setMaxBuiltPerPlayer(final String s) {
    maxBuiltPerPlayer = getInt(s);
  }

  private void setMaxBuiltPerPlayer(final Integer s) {
    maxBuiltPerPlayer = s;
  }

  public int getMaxBuiltPerPlayer() {
    return maxBuiltPerPlayer;
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

  public int getCarrierCapacity() {
    return carrierCapacity;
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

  public int getCarrierCost() {
    return carrierCost;
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
    UnitSupportAttachment.setOldSupportCount((UnitType) getAttachedTo(), getData(), s);
  }

  private void setUnitSupportCount(final Integer s) {
    unitSupportCount = s;
    UnitSupportAttachment.setOldSupportCount((UnitType) getAttachedTo(), getData(), s.toString());
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
  public void setBombard(final int s) {
    bombard = s;
  }

  public int getBombard() {
    return bombard > 0 ? bombard : attack;
  }

  private void setMovement(final String s) {
    movement = getInt(s);
  }

  private void setMovement(final Integer s) {
    movement = s;
  }

  private int getMovement() {
    return movement;
  }

  public int getMovement(final GamePlayer player) {
    return Math.max(
        0,
        movement
            + TechAbilityAttachment.getMovementBonus(
                (UnitType) this.getAttachedTo(), player, getData()));
  }

  private void resetMovement() {
    movement = 0;
  }

  private void setAttack(final String s) {
    attack = getInt(s);
  }

  private void setAttack(final Integer s) {
    attack = s;
  }

  int getAttack() {
    return attack;
  }

  public int getAttack(final GamePlayer player) {
    final int attackValue =
        attack
            + TechAbilityAttachment.getAttackBonus(
                (UnitType) this.getAttachedTo(), player, getData());
    return Math.min(getData().getDiceSides(), Math.max(0, attackValue));
  }

  private void resetAttack() {
    attack = 0;
  }

  private void setAttackRolls(final String s) {
    attackRolls = getInt(s);
  }

  private void setAttackRolls(final Integer s) {
    attackRolls = s;
  }

  private int getAttackRolls() {
    return attackRolls;
  }

  public int getAttackRolls(final GamePlayer player) {
    return Math.max(
        0,
        attackRolls
            + TechAbilityAttachment.getAttackRollsBonus(
                (UnitType) this.getAttachedTo(), player, getData()));
  }

  private void resetAttackRolls() {
    attackRolls = 1;
  }

  private void setDefense(final String s) {
    defense = getInt(s);
  }

  private void setDefense(final Integer s) {
    defense = s;
  }

  private int getDefense() {
    return defense;
  }

  public int getDefense(final GamePlayer player) {
    int defenseValue =
        defense
            + TechAbilityAttachment.getDefenseBonus(
                (UnitType) this.getAttachedTo(), player, getData());
    if (defenseValue > 0 && isFirstStrike && TechTracker.hasSuperSubs(player)) {
      final int bonus = Properties.getSuperSubDefenseBonus(getData());
      defenseValue += bonus;
    }
    return Math.min(getData().getDiceSides(), Math.max(0, defenseValue));
  }

  private void resetDefense() {
    defense = 0;
  }

  private void setDefenseRolls(final String s) {
    defenseRolls = getInt(s);
  }

  private void setDefenseRolls(final Integer s) {
    defenseRolls = s;
  }

  private int getDefenseRolls() {
    return defenseRolls;
  }

  public int getDefenseRolls(final GamePlayer player) {
    return Math.max(
        0,
        defenseRolls
            + TechAbilityAttachment.getDefenseRollsBonus(
                (UnitType) this.getAttachedTo(), player, getData()));
  }

  private void resetDefenseRolls() {
    defenseRolls = 1;
  }

  private void setChooseBestRoll(final String s) {
    chooseBestRoll = getBool(s);
  }

  private void setChooseBestRoll(final Boolean s) {
    chooseBestRoll = s;
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

  public boolean getCanScramble() {
    return canScramble;
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

  public int getMaxScrambleCount() {
    return maxScrambleCount;
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

  public int getMaxScrambleDistance() {
    return maxScrambleDistance;
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

  public int getMaxInterceptCount() {
    return maxInterceptCount;
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

  public int getMaxOperationalDamage() {
    return maxOperationalDamage;
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

  public int getMaxDamage() {
    return maxDamage;
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

  public boolean getIsAirBase() {
    return isAirBase;
  }

  private void resetIsAirBase() {
    isAirBase = false;
  }

  private void setIsInfrastructure(final String s) {
    isInfrastructure = getBool(s);
  }

  private void setIsInfrastructure(final Boolean s) {
    isInfrastructure = s;
  }

  public boolean getIsInfrastructure() {
    return isInfrastructure;
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

  public boolean getCanBeDamaged() {
    return canBeDamaged;
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

  public boolean getCanDieFromReachingMaxDamage() {
    return canDieFromReachingMaxDamage;
  }

  private void resetCanDieFromReachingMaxDamage() {
    canDieFromReachingMaxDamage = false;
  }

  @Deprecated
  private void setIsSuicide(final String s) {
    setIsSuicide(getBool(s));
  }

  @Deprecated
  private void setIsSuicide(final Boolean s) {
    setIsSuicideOnAttack(s);
    // Global property controlled whether isSuicide units would suicide on defense
    setIsSuicideOnDefense(s && !Properties.getDefendingSuicideAndMunitionUnitsDoNotFire(getData()));
    setIsFirstStrike(s);
    if (s) {
      canNotTarget = null;
    } else {
      resetCanNotTarget();
    }
  }

  private void setIsSuicideOnAttack(final Boolean s) {
    isSuicideOnAttack = s;
  }

  public boolean getIsSuicideOnAttack() {
    return isSuicideOnAttack;
  }

  private void setIsSuicideOnDefense(final Boolean s) {
    isSuicideOnDefense = s;
  }

  public boolean getIsSuicideOnDefense() {
    return isSuicideOnDefense;
  }

  private void setIsSuicideOnHit(final String s) {
    isSuicideOnHit = getBool(s);
  }

  private void setIsSuicideOnHit(final Boolean s) {
    isSuicideOnHit = s;
  }

  public boolean getIsSuicideOnHit() {
    return isSuicideOnHit;
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

  public boolean getIsKamikaze() {
    return isKamikaze;
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

  public int getBlockade() {
    return blockade;
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
      final String unitTypeName = s[i];
      // validate that this unit exists in the xml
      final UnitType type = getData().getUnitTypeList().getUnitType(unitTypeName);
      if (type == null) {
        throw new GameParseException("No unit called: " + unitTypeName + thisErrorMsg());
      }
      // we should allow positive and negative numbers, since you can give bonuses to units or take
      // away a unit's movement
      givesMovement.put(type, movement);
    }
  }

  private void setGivesMovement(final IntegerMap<UnitType> value) {
    givesMovement = value;
  }

  public IntegerMap<UnitType> getGivesMovement() {
    return givesMovement;
  }

  private void resetGivesMovement() {
    givesMovement = new IntegerMap<>();
  }

  private void setConsumesUnits(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("consumesUnits must have two fields" + thisErrorMsg());
    }
    final String unitTypeToProduce = s[1];
    // validate that this unit exists in the xml
    final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
    if (ut == null) {
      throw new GameParseException("No unit called:" + unitTypeToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 1) {
      throw new GameParseException("consumesUnits must have positive values" + thisErrorMsg());
    }
    consumesUnits.put(ut, n);
  }

  private void setConsumesUnits(final IntegerMap<UnitType> value) {
    consumesUnits = value;
  }

  public IntegerMap<UnitType> getConsumesUnits() {
    return consumesUnits;
  }

  private void resetConsumesUnits() {
    consumesUnits = new IntegerMap<>();
  }

  private void setCreatesUnitsList(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length <= 0 || s.length > 2) {
      throw new GameParseException(
          "createsUnitsList cannot be empty or have more than two fields" + thisErrorMsg());
    }
    final String unitTypeToProduce = s[1];
    // validate that this unit exists in the xml
    final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
    if (ut == null) {
      throw new GameParseException(
          "createsUnitsList: No unit called:" + unitTypeToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 0) {
      throw new GameParseException("createsUnitsList cannot have negative values" + thisErrorMsg());
    }
    createsUnitsList.put(ut, n);
  }

  private void setCreatesUnitsList(final IntegerMap<UnitType> value) {
    createsUnitsList = value;
  }

  public IntegerMap<UnitType> getCreatesUnitsList() {
    return createsUnitsList;
  }

  private void resetCreatesUnitsList() {
    createsUnitsList = new IntegerMap<>();
  }

  private void setCreatesResourcesList(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length <= 0 || s.length > 2) {
      throw new GameParseException(
          "createsResourcesList cannot be empty or have more than two fields" + thisErrorMsg());
    }
    final String resourceToProduce = s[1];
    // validate that this resource exists in the xml
    final Resource r = getData().getResourceList().getResource(resourceToProduce);
    if (r == null) {
      throw new GameParseException(
          "createsResourcesList: No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    createsResourcesList.put(r, n);
  }

  private void setCreatesResourcesList(final IntegerMap<Resource> value) {
    createsResourcesList = value;
  }

  public IntegerMap<Resource> getCreatesResourcesList() {
    return createsResourcesList;
  }

  private void resetCreatesResourcesList() {
    createsResourcesList = new IntegerMap<>();
  }

  private void setFuelCost(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("fuelCost must have two fields" + thisErrorMsg());
    }
    final String resourceToProduce = s[1];
    // validate that this resource exists in the xml
    final Resource r = getData().getResourceList().getResource(resourceToProduce);
    if (r == null) {
      throw new GameParseException(
          "fuelCost: No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 0) {
      throw new GameParseException("fuelCost must have positive values" + thisErrorMsg());
    }
    fuelCost.put(r, n);
  }

  private void setFuelCost(final IntegerMap<Resource> value) {
    fuelCost = value;
  }

  public IntegerMap<Resource> getFuelCost() {
    return fuelCost;
  }

  private void resetFuelCost() {
    fuelCost = new IntegerMap<>();
  }

  private void setFuelFlatCost(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("fuelFlatCost must have two fields" + thisErrorMsg());
    }
    final String resourceToProduce = s[1];
    // validate that this resource exists in the xml
    final Resource r = getData().getResourceList().getResource(resourceToProduce);
    if (r == null) {
      throw new GameParseException(
          "fuelFlatCost: No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 0) {
      throw new GameParseException("fuelFlatCost must have positive values" + thisErrorMsg());
    }
    fuelFlatCost.put(r, n);
  }

  private void setFuelFlatCost(final IntegerMap<Resource> value) {
    fuelFlatCost = value;
  }

  public IntegerMap<Resource> getFuelFlatCost() {
    return fuelFlatCost;
  }

  private void resetFuelFlatCost() {
    fuelFlatCost = new IntegerMap<>();
  }

  private void setBombingBonus(final String s) {
    bombingBonus = getInt(s);
  }

  private void setBombingBonus(final Integer s) {
    bombingBonus = s;
  }

  public int getBombingBonus() {
    return bombingBonus;
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

  public int getBombingMaxDieSides() {
    return bombingMaxDieSides;
  }

  private void resetBombingMaxDieSides() {
    bombingMaxDieSides = -1;
  }

  private void setBombingTargets(final String value) throws GameParseException {
    if (value == null) {
      bombingTargets = null;
      return;
    }
    if (bombingTargets == null) {
      bombingTargets = new HashSet<>();
    }
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("bombingTargets: no such unit type: " + u + thisErrorMsg());
      }
      bombingTargets.add(ut);
    }
  }

  private void setBombingTargets(final Set<UnitType> value) {
    bombingTargets = value;
  }

  private Set<UnitType> getBombingTargets() {
    return bombingTargets;
  }

  public Set<UnitType> getBombingTargets(final GameData data) {
    if (bombingTargets != null) {
      return bombingTargets;
    }
    return new HashSet<>(data.getUnitTypeList().getAllUnitTypes());
  }

  private void resetBombingTargets() {
    bombingTargets = null;
  }

  /** Finds potential unit types which all passed in bombers and rockets can target. */
  public static Set<UnitType> getAllowedBombingTargetsIntersection(
      final Collection<Unit> bombersOrRockets, final GameData data) {
    if (bombersOrRockets.isEmpty()) {
      return new HashSet<>();
    }
    Collection<UnitType> allowedTargets = data.getUnitTypeList().getAllUnitTypes();
    for (final Unit u : bombersOrRockets) {
      final UnitAttachment ua = UnitAttachment.get(u.getType());
      final Set<UnitType> bombingTargets = ua.getBombingTargets(data);
      if (bombingTargets != null) {
        allowedTargets = CollectionUtils.intersection(allowedTargets, bombingTargets);
      }
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

  private void setAttackAa(final Integer s) {
    attackAa = s;
  }

  private int getAttackAa() {
    return attackAa;
  }

  public int getAttackAa(final GamePlayer player) {
    // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something
    // other than 6, or it
    // does not divide perfectly into attackAAmaxDieSides
    return Math.max(
        0,
        Math.min(
            getAttackAaMaxDieSides(),
            attackAa
                + TechAbilityAttachment.getRadarBonus(
                    (UnitType) this.getAttachedTo(), player, getData())));
  }

  private void resetAttackAa() {
    attackAa = 1;
  }

  private void setOffensiveAttackAa(final String s) {
    offensiveAttackAa = getInt(s);
  }

  private void setOffensiveAttackAa(final Integer s) {
    offensiveAttackAa = s;
  }

  private int getOffensiveAttackAa() {
    return offensiveAttackAa;
  }

  public int getOffensiveAttackAa(final GamePlayer player) {
    // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something
    // other than 6, or it
    // does not divide perfectly into attackAAmaxDieSides
    return Math.max(
        0,
        Math.min(
            getOffensiveAttackAaMaxDieSides(),
            offensiveAttackAa
                + TechAbilityAttachment.getRadarBonus(
                    (UnitType) this.getAttachedTo(), player, getData())));
  }

  private void resetOffensiveAttackAa() {
    offensiveAttackAa = 1;
  }

  private void setAttackAaMaxDieSides(final String s) {
    attackAaMaxDieSides = getInt(s);
  }

  private void setAttackAaMaxDieSides(final Integer s) {
    attackAaMaxDieSides = s;
  }

  public int getAttackAaMaxDieSides() {
    if (attackAaMaxDieSides < 0) {
      return getData().getDiceSides();
    }
    return attackAaMaxDieSides;
  }

  private void resetAttackAaMaxDieSides() {
    attackAaMaxDieSides = -1;
  }

  private void setOffensiveAttackAaMaxDieSides(final String s) {
    offensiveAttackAaMaxDieSides = getInt(s);
  }

  private void setOffensiveAttackAaMaxDieSides(final Integer s) {
    offensiveAttackAaMaxDieSides = s;
  }

  public int getOffensiveAttackAaMaxDieSides() {
    if (offensiveAttackAaMaxDieSides < 0) {
      return getData().getDiceSides();
    }
    return offensiveAttackAaMaxDieSides;
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

  private void setMaxAaAttacks(final Integer s) {
    maxAaAttacks = s;
  }

  public int getMaxAaAttacks() {
    return maxAaAttacks;
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

  private void setMaxRoundsAa(final Integer s) {
    maxRoundsAa = s;
  }

  public int getMaxRoundsAa() {
    return maxRoundsAa;
  }

  private void resetMaxRoundsAa() {
    maxRoundsAa = 1;
  }

  private void setMayOverStackAa(final String s) {
    mayOverStackAa = getBool(s);
  }

  private void setMayOverStackAa(final Boolean s) {
    mayOverStackAa = s;
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

  private void setDamageableAa(final Boolean s) {
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

  private void setIsAaForCombatOnly(final Boolean s) {
    isAaForCombatOnly = s;
  }

  public boolean getIsAaForCombatOnly() {
    return isAaForCombatOnly;
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

  public boolean getIsAaForBombingThisUnitOnly() {
    return isAaForBombingThisUnitOnly;
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

  public boolean getIsAaForFlyOverOnly() {
    return isAaForFlyOverOnly;
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

  public boolean getIsRocket() {
    return isRocket;
  }

  private void resetIsRocket() {
    isRocket = false;
  }

  private void setTypeAa(final String s) {
    typeAa = s;
  }

  public String getTypeAa() {
    return typeAa;
  }

  private void resetTypeAa() {
    typeAa = "AA";
  }

  public static List<String> getAllOfTypeAas(final Collection<Unit> aaUnitsAlreadyVerified) {
    final Set<String> aaSet = new HashSet<>();
    for (final Unit u : aaUnitsAlreadyVerified) {
      aaSet.add(UnitAttachment.get(u.getType()).getTypeAa());
    }
    final List<String> aaTypes = new ArrayList<>(aaSet);
    Collections.sort(aaTypes);
    return aaTypes;
  }

  private void setTargetsAa(final String value) throws GameParseException {
    if (value == null) {
      targetsAa = null;
      return;
    }
    if (targetsAa == null) {
      targetsAa = new HashSet<>();
    }
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("AAtargets: no such unit type: " + u + thisErrorMsg());
      }
      targetsAa.add(ut);
    }
  }

  private void setTargetsAa(final Set<UnitType> value) {
    targetsAa = value;
  }

  private Set<UnitType> getTargetsAa() {
    return targetsAa;
  }

  public Set<UnitType> getTargetsAa(final GameData data) {
    if (targetsAa != null) {
      return targetsAa;
    }
    return StreamSupport.stream(data.getUnitTypeList().spliterator(), false)
        .filter(ut -> UnitAttachment.get(ut).getIsAir())
        .collect(Collectors.toSet());
  }

  private void resetTargetsAa() {
    targetsAa = null;
  }

  private void setWillNotFireIfPresent(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException(
            "willNotFireIfPresent: no such unit type: " + u + thisErrorMsg());
      }
      willNotFireIfPresent.add(ut);
    }
  }

  private void setWillNotFireIfPresent(final Set<UnitType> value) {
    willNotFireIfPresent = value;
  }

  public Set<UnitType> getWillNotFireIfPresent() {
    return willNotFireIfPresent;
  }

  private void resetWillNotFireIfPresent() {
    willNotFireIfPresent = new HashSet<>();
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

  public boolean getCanNotMoveDuringCombatMove() {
    return canNotMoveDuringCombatMove;
  }

  private void resetCanNotMoveDuringCombatMove() {
    canNotMoveDuringCombatMove = false;
  }

  private void setMovementLimit(final String value) throws GameParseException {
    if (value == null) {
      movementLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException(
          "movementLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException(
          "movementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException(
          "movementLimit value must owned, allied, or total" + thisErrorMsg());
    }
    movementLimit = Tuple.of(max, s[1]);
  }

  private void setMovementLimit(final Tuple<Integer, String> value) {
    movementLimit = value;
  }

  public Tuple<Integer, String> getMovementLimit() {
    return movementLimit;
  }

  private void resetMovementLimit() {
    movementLimit = null;
  }

  private void setAttackingLimit(final String value) throws GameParseException {
    if (value == null) {
      attackingLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException(
          "attackingLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException(
          "attackingLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException(
          "attackingLimit value must owned, allied, or total" + thisErrorMsg());
    }
    attackingLimit = Tuple.of(max, s[1]);
  }

  private void setAttackingLimit(final Tuple<Integer, String> value) {
    attackingLimit = value;
  }

  public Tuple<Integer, String> getAttackingLimit() {
    return attackingLimit;
  }

  private void resetAttackingLimit() {
    attackingLimit = null;
  }

  private void setPlacementLimit(final String value) throws GameParseException {
    if (value == null) {
      placementLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException(
          "placementLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException(
          "placementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException(
          "placementLimit value must owned, allied, or total" + thisErrorMsg());
    }
    placementLimit = Tuple.of(max, s[1]);
  }

  private void setPlacementLimit(final Tuple<Integer, String> value) {
    placementLimit = value;
  }

  private Tuple<Integer, String> getPlacementLimit() {
    return placementLimit;
  }

  private void resetPlacementLimit() {
    placementLimit = null;
  }

  private void setTuv(final String s) {
    tuv = getInt(s);
  }

  private void setTuv(final Integer s) {
    tuv = s;
  }

  public int getTuv() {
    return tuv;
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

  public Boolean getCanRetreatOnStalemate() {
    return canRetreatOnStalemate;
  }

  public void resetCanRetreatOnStalemate() {
    canRetreatOnStalemate = null;
  }

  /**
   * Returns the maximum number of units of the specified type that can be placed in the specified
   * territory according to the specified stacking limit (movement, attack, or placement).
   *
   * @return {@link Integer#MAX_VALUE} if there is no stacking limit for the specified conditions.
   */
  public static int getMaximumNumberOfThisUnitTypeToReachStackingLimit(
      final String limitType,
      final UnitType ut,
      final Territory t,
      final GamePlayer owner,
      final GameData data) {
    final UnitAttachment ua = UnitAttachment.get(ut);
    final Tuple<Integer, String> stackingLimit;
    switch (limitType) {
      case "movementLimit":
        stackingLimit = ua.getMovementLimit();
        break;
      case "attackingLimit":
        stackingLimit = ua.getAttackingLimit();
        break;
      case "placementLimit":
        stackingLimit = ua.getPlacementLimit();
        break;
      default:
        throw new IllegalStateException(
            "getMaximumNumberOfThisUnitTypeToReachStackingLimit does not allow limitType: "
                + limitType);
    }
    if (stackingLimit == null) {
      return Integer.MAX_VALUE;
    }
    int max = stackingLimit.getFirst();
    if (max == Integer.MAX_VALUE
        && (ua.getIsAaForBombingThisUnitOnly() || ua.getIsAaForCombatOnly())) {
      // under certain rules (classic rules) there can only be 1 aa gun in a territory.
      if (!(Properties.getWW2V2(data)
          || Properties.getWW2V3(data)
          || Properties.getMultipleAaPerTerritory(data))) {
        max = 1;
      }
    }
    final Predicate<Unit> stackingMatch;
    final String stackingType = stackingLimit.getSecond();
    switch (stackingType) {
      case "owned":
        stackingMatch = Matches.unitIsOfType(ut).and(Matches.unitIsOwnedBy(owner));
        break;
      case "allied":
        stackingMatch = Matches.unitIsOfType(ut).and(Matches.isUnitAllied(owner, data));
        break;
      default:
        stackingMatch = Matches.unitIsOfType(ut);
        break;
    }
    // else if (stackingType.equals("total"))
    final int totalInTerritory = CollectionUtils.countMatches(t.getUnits(), stackingMatch);
    return Math.max(0, max - totalInTerritory);
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
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
        && transportCapacity != -1
        && Properties.getTransportCasualtiesRestricted(data)
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
        final UnitType ut = getData().getUnitTypeList().getUnitType(transport);
        if (ut == null) {
          throw new GameParseException("No unit called:" + transport + thisErrorMsg());
        }
        if (ut.getAttachments() == null || ut.getAttachments().isEmpty()) {
          throw new GameParseException(
              transport
                  + " has no attachments, please declare "
                  + transport
                  + " in the xml before using it as a transport"
                  + thisErrorMsg());
          // Units may be considered transported if they are on a carrier, or if they are
          // paratroopers, or if they are
          // mech infantry. The "transporter" may not be an actual transport, so we should not check
          // for that here.
        }
      }
    }
    if (!receivesAbilityWhenWith.isEmpty()) {
      for (final String value : receivesAbilityWhenWith) {
        // first is ability, second is unit that we get it from
        final String[] s = splitOnColon(value);
        if (s.length != 2) {
          throw new GameParseException(
              "receivesAbilityWhenWith must have 2 parts, 'ability:unit'" + thisErrorMsg());
        }
        if (getData().getUnitTypeList().getUnitType(s[1]) == null) {
          throw new GameParseException(
              "receivesAbilityWhenWith, unit does not exist, name:" + s[1] + thisErrorMsg());
        }
        // currently only supports canBlitz (canBlitz)
        if (!s[0].equals("canBlitz")) {
          throw new GameParseException(
              "receivesAbilityWhenWith so far only supports: canBlitz" + thisErrorMsg());
        }
      }
    }
    if (!whenCombatDamaged.isEmpty()) {
      for (final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key : whenCombatDamaged) {
        final String obj = key.getSecond().getFirst();
        if (obj.equals(UNITSMAYNOTLANDONCARRIER)) {
          continue;
        }
        if (obj.equals(UNITSMAYNOTLEAVEALLIEDCARRIER)) {
          continue;
        }
        throw new GameParseException(
            "whenCombatDamaged so far only supports: "
                + UNITSMAYNOTLANDONCARRIER
                + ", "
                + UNITSMAYNOTLEAVEALLIEDCARRIER
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
        // Check if its a territory effect and get all territories
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
        + (canNotTarget == null || canNotTarget.isEmpty() ? "empty" : canNotTarget.toString())
        + "  canNotBeTargetedBy:"
        + (canNotBeTargetedBy.isEmpty() ? "empty" : canNotBeTargetedBy.toString())
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
        + "  isAAforCombatOnly:"
        + isAaForCombatOnly
        + "  isAAforBombingThisUnitOnly:"
        + isAaForBombingThisUnitOnly
        + "  isAAforFlyOverOnly:"
        + isAaForFlyOverOnly
        + "  attackAA:"
        + attackAa
        + "  offensiveAttackAA:"
        + offensiveAttackAa
        + "  attackAAmaxDieSides:"
        + attackAaMaxDieSides
        + "  offensiveAttackAAmaxDieSides:"
        + offensiveAttackAaMaxDieSides
        + "  maxAAattacks:"
        + maxAaAttacks
        + "  maxRoundsAA:"
        + maxRoundsAa
        + "  mayOverStackAA:"
        + mayOverStackAa
        + "  damageableAA:"
        + damageableAa
        + "  typeAA:"
        + typeAa
        + "  targetsAA:"
        + (targetsAa != null
            ? (targetsAa.isEmpty() ? "empty" : targetsAa.toString())
            : "all air units")
        + "  willNotFireIfPresent:"
        + (willNotFireIfPresent != null
            ? (willNotFireIfPresent.isEmpty() ? "empty" : willNotFireIfPresent.toString())
            : "null")
        + "  isRocket:"
        + isRocket
        + "  canProduceUnits:"
        + canProduceUnits
        + "  canProduceXUnits:"
        + canProduceXUnits
        + "  createsUnitsList:"
        + (createsUnitsList != null
            ? (createsUnitsList.isEmpty() ? "empty" : createsUnitsList.toString())
            : "null")
        + "  createsResourcesList:"
        + (createsResourcesList != null
            ? (createsResourcesList.isEmpty() ? "empty" : createsResourcesList.toString())
            : "null")
        + "  fuelCost:"
        + (fuelCost != null ? (fuelCost.isEmpty() ? "empty" : fuelCost.toString()) : "null")
        + "  fuelFlatCost:"
        + (fuelFlatCost != null
            ? (fuelFlatCost.isEmpty() ? "empty" : fuelFlatCost.toString())
            : "null")
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
        + (destroyedWhenCapturedBy != null
            ? (destroyedWhenCapturedBy.isEmpty() ? "empty" : destroyedWhenCapturedBy.toString())
            : "null")
        + "  canBeCapturedOnEnteringBy:"
        + (canBeCapturedOnEnteringBy != null
            ? (canBeCapturedOnEnteringBy.isEmpty() ? "empty" : canBeCapturedOnEnteringBy.toString())
            : "null")
        + "  canBeDamaged:"
        + canBeDamaged
        + "  canDieFromReachingMaxDamage:"
        + canDieFromReachingMaxDamage
        + "  maxOperationalDamage:"
        + maxOperationalDamage
        + "  maxDamage:"
        + maxDamage
        + "  unitPlacementRestrictions:"
        + (unitPlacementRestrictions != null
            ? (unitPlacementRestrictions.length == 0
                ? "empty"
                : Arrays.toString(unitPlacementRestrictions))
            : "null")
        + "  requiresUnits:"
        + (requiresUnits != null
            ? (requiresUnits.isEmpty() ? "empty" : MyFormatter.listOfArraysToString(requiresUnits))
            : "null")
        + "  consumesUnits:"
        + (consumesUnits != null
            ? (consumesUnits.isEmpty() ? "empty" : consumesUnits.toString())
            : "null")
        + "  requiresUnitsToMove:"
        + (requiresUnitsToMove != null
            ? (requiresUnitsToMove.isEmpty()
                ? "empty"
                : MyFormatter.listOfArraysToString(requiresUnitsToMove))
            : "null")
        + "  canOnlyBePlacedInTerritoryValuedAtX:"
        + canOnlyBePlacedInTerritoryValuedAtX
        + "  maxBuiltPerPlayer:"
        + maxBuiltPerPlayer
        + "  special:"
        + (special != null ? (special.isEmpty() ? "empty" : special.toString()) : "null")
        + "  isSuicideOnAttack:"
        + isSuicideOnAttack
        + "  isSuicideOnDefense:"
        + isSuicideOnDefense
        + "  isSuicideOnHit:"
        + isSuicideOnHit
        + "  isCombatTransport:"
        + isCombatTransport
        + "  canInvadeOnlyFrom:"
        + (canInvadeOnlyFrom != null
            ? (canInvadeOnlyFrom.length == 0 ? "empty" : Arrays.toString(canInvadeOnlyFrom))
            : "null")
        + "  canBeGivenByTerritoryTo:"
        + (canBeGivenByTerritoryTo != null
            ? (canBeGivenByTerritoryTo.isEmpty() ? "empty" : canBeGivenByTerritoryTo.toString())
            : "null")
        + "  receivesAbilityWhenWith:"
        + (receivesAbilityWhenWith != null
            ? (receivesAbilityWhenWith.isEmpty() ? "empty" : receivesAbilityWhenWith.toString())
            : "null")
        + "  whenCombatDamaged:"
        + (whenCombatDamaged != null
            ? (whenCombatDamaged.isEmpty() ? "empty" : whenCombatDamaged.toString())
            : "null")
        + "  blockade:"
        + blockade
        + "  bombingMaxDieSides:"
        + bombingMaxDieSides
        + "  bombingBonus:"
        + bombingBonus
        + "  bombingTargets:"
        + bombingTargets
        + "  givesMovement:"
        + (givesMovement != null
            ? (givesMovement.isEmpty() ? "empty" : givesMovement.toString())
            : "null")
        + "  repairsUnits:"
        + (repairsUnits != null
            ? (repairsUnits.isEmpty() ? "empty" : repairsUnits.toString())
            : "null")
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
        + (whenCapturedChangesInto != null
            ? (whenCapturedChangesInto.isEmpty() ? "empty" : whenCapturedChangesInto.toString())
            : "null")
        + " whenCapturedSustainsDamage:"
        + whenCapturedSustainsDamage
        + "  whenHitPointsDamagedChangesInto:"
        + (whenHitPointsDamagedChangesInto != null
            ? (whenHitPointsDamagedChangesInto.isEmpty()
                ? "empty"
                : whenHitPointsDamagedChangesInto.toString())
            : "null")
        + "  whenHitPointsRepairedChangesInto:"
        + (whenHitPointsRepairedChangesInto != null
            ? (whenHitPointsRepairedChangesInto.isEmpty()
                ? "empty"
                : whenHitPointsRepairedChangesInto.toString())
            : "null")
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
   * Displays all unit options in a short description form that's user friendly rather than as XML.
   * Shows all except for: constructionType, constructionsPerTerrPerTypePerTurn,
   * maxConstructionsPerTypePerTerr, canBeGivenByTerritoryTo, destroyedWhenCapturedBy,
   * canBeCapturedOnEnteringBy.
   */
  public String toStringShortAndOnlyImportantDifferences(final GamePlayer player) {
    final List<Tuple<String, String>> tuples = new ArrayList<>();
    final UnitType unitType = (UnitType) this.getAttachedTo();

    if (getIsAir()) {
      tuples.add(Tuple.of("Type", "Air"));
    } else if (getIsSea()) {
      tuples.add(Tuple.of("Type", "Sea"));
    } else {
      tuples.add(Tuple.of("Type", "Land"));
    }
    final int attackRolls = getAttackRolls(player);
    final int defenseRolls = getDefenseRolls(player);
    final String attack = (attackRolls > 1 ? (attackRolls + "x") : "") + getAttack(player);
    final String defense = (defenseRolls > 1 ? (defenseRolls + "x") : "") + getDefense(player);
    final String movement = String.valueOf(getMovement(player));
    tuples.add(Tuple.of("Att | Def | Mov", attack + " | " + defense + " | " + movement));
    if (getHitPoints() > 1) {
      tuples.add(Tuple.of("HP", String.valueOf(getHitPoints())));
    }

    if (getCanProduceUnits() && getCanProduceXUnits() < 0) {
      tuples.add(Tuple.of("Can Produce Units up to Territory Value", ""));
    } else if (getCanProduceUnits() && getCanProduceXUnits() > 0) {
      tuples.add(Tuple.of("Can Produce Units", String.valueOf(getCanProduceXUnits())));
    }
    addIntegerMapDescription("Creates Units each Turn", getCreatesUnitsList(), tuples);
    addIntegerMapDescription("Produces Resources each Turn", getCreatesResourcesList(), tuples);

    addIntegerMapDescription("Fuel Cost per Movement", getFuelCost(), tuples);
    addIntegerMapDescription("Fuel Cost each Turn if Moved", getFuelFlatCost(), tuples);

    addAaDescription(
        "Targeted Attack", getOffensiveAttackAa(player), getOffensiveAttackAaMaxDieSides(), tuples);
    addAaDescription("Targeted Defense", getAttackAa(player), getAttackAaMaxDieSides(), tuples);

    // TODO: Rework rocket description
    if (getIsRocket() && playerHasRockets(player)) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Can Rocket Attack, ");
      final int bombingBonus = getBombingBonus();
      if ((getBombingMaxDieSides() != -1 || bombingBonus != 0)
          && Properties.getUseBombingMaxDiceSidesAndBonus(getData())) {
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
      tuples.add(Tuple.of(sb.toString(), ""));
    }

    if (getIsInfrastructure()) {
      tuples.add(Tuple.of("Can be Captured", ""));
    }
    if (getIsConstruction()) {
      tuples.add(Tuple.of("Can be Placed Without Factory", ""));
    }

    // TODO: Rework damaged description
    if (getCanBeDamaged()
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData())) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Can be Damaged by Raids, ");
      if (getMaxOperationalDamage() > -1) {
        sb.append(getMaxOperationalDamage()).append(" Max Operational Damage, ");
      }
      if (getCanProduceUnits() && getCanProduceXUnits() < 0) {
        sb.append("Total Damage up to ")
            .append(getMaxDamage() > -1 ? getMaxDamage() : 2)
            .append("x Territory Value, ");
      } else if (getMaxDamage() > -1) {
        sb.append(getMaxDamage()).append(" Max Total Damage, ");
      }
      if (getCanDieFromReachingMaxDamage()) {
        sb.append("Dies if Max Damage Reached, ");
      }
      tuples.add(Tuple.of(sb.toString(), ""));
    } else if (getCanBeDamaged()) {
      tuples.add(Tuple.of("Can be Damaged by Raids", ""));
    }

    if (getIsAirBase() && Properties.getScrambleRulesInEffect(getData())) {
      tuples.add(Tuple.of("Allows Scrambling", ""));
    }
    if (getCanScramble() && Properties.getScrambleRulesInEffect(getData())) {
      tuples.add(
          Tuple.of(
              "Scramble Range",
              String.valueOf(getMaxScrambleDistance() > 0 ? getMaxScrambleDistance() : 1)));
    }

    final List<UnitSupportAttachment> supports =
        CollectionUtils.getMatches(
            UnitSupportAttachment.get(unitType),
            Matches.unitSupportAttachmentCanBeUsedByPlayer(player));
    if (supports.size() > 3) {
      tuples.add(Tuple.of("Can Provide Support to Units", ""));
    } else if (!supports.isEmpty()) {
      final boolean moreThanOneSupportType = UnitSupportAttachment.get(getData()).size() > 1;
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
        tuples.add(Tuple.of(key, text));
      }
    }

    if (getIsMarine() != 0) {
      tuples.add(Tuple.of("Amphibious Attack Modifier", String.valueOf(getIsMarine())));
    }
    if (getCanBlitz(player)) {
      tuples.add(Tuple.of("Can Blitz", ""));
    }

    if (!getReceivesAbilityWhenWith().isEmpty()) {
      if (getReceivesAbilityWhenWith().size() <= 2) {
        for (final String ability : getReceivesAbilityWhenWith()) {
          final String[] s = splitOnColon(ability);
          tuples.add(Tuple.of("Receives Ability", s[0] + " when Paired with " + s[1]));
        }
      } else {
        tuples.add(Tuple.of("Receives Abilities when Paired with Other Units", ""));
      }
    }

    if (getIsStrategicBomber()) {
      final StringBuilder sb = new StringBuilder();
      final int bombingBonus = getBombingBonus();
      if ((getBombingMaxDieSides() != -1 || bombingBonus != 0)
          && Properties.getUseBombingMaxDiceSidesAndBonus(getData())) {
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
      tuples.add(Tuple.of("Can Perform Raids", sb.toString()));
    }

    if (getAirAttack(player) > 0
        && (getIsStrategicBomber() || getCanEscort() || getCanAirBattle())) {
      tuples.add(
          Tuple.of(
              "Air Attack", (attackRolls > 1 ? (attackRolls + "x") : "") + getAirAttack(player)));
    }
    if (getAirDefense(player) > 0 && (getCanIntercept() || getCanAirBattle())) {
      tuples.add(
          Tuple.of(
              "Air Defense",
              (defenseRolls > 1 ? (defenseRolls + "x") : "") + getAirDefense(player)));
    }

    if (getCanEvade()) {
      tuples.add(Tuple.of("Can Evade", ""));
    }
    if (getIsFirstStrike()) {
      tuples.add(Tuple.of("Is First Strike", ""));
    }
    if (getCanMoveThroughEnemies()) {
      tuples.add(Tuple.of("Can Move Through Enemies", ""));
    }
    if (getCanBeMovedThroughByEnemies()) {
      tuples.add(Tuple.of("Can Be Moved Through By Enemies", ""));
    }
    addLabeledUnitTypes("Can't Target", getCanNotTarget(), tuples);
    addLabeledUnitTypes("Can't Be Targeted By", getCanNotBeTargetedBy(), tuples);
    if (getIsDestroyer()) {
      tuples.add(Tuple.of("Is Anti-Stealth", ""));
    }

    if (getCanBombard(player) && getBombard() > 0) {
      tuples.add(Tuple.of("Bombard", (attackRolls > 1 ? (attackRolls + "x") : "") + getBombard()));
    }

    if (getBlockade() > 0) {
      tuples.add(Tuple.of("Blockade Loss", String.valueOf(getBlockade())));
    }

    if (getIsSuicideOnAttack()) {
      tuples.add(Tuple.of("Suicide on Attack Unit", ""));
    }
    if (getIsSuicideOnDefense()) {
      tuples.add(Tuple.of("Suicide on Defense Unit", ""));
    }
    if (getIsSuicideOnHit()) {
      tuples.add(Tuple.of("Suicide on Hit Unit", ""));
    }
    if (getIsAir() && (getIsKamikaze() || Properties.getKamikazeAirplanes(getData()))) {
      tuples.add(Tuple.of("Is Kamikaze", "Can use all Movement to Attack Target"));
    }

    if (getIsLandTransportable() && playerHasMechInf(player)) {
      tuples.add(Tuple.of("Can be Land Transported", ""));
    }
    if (getIsLandTransport() && playerHasMechInf(player)) {
      tuples.add(Tuple.of("Is a Land Transport", ""));
    }
    if (getIsAirTransportable() && playerHasParatroopers(player)) {
      tuples.add(Tuple.of("Can be Air Transported", ""));
    }
    if (getIsAirTransport() && playerHasParatroopers(player)) {
      tuples.add(Tuple.of("Is an Air Transport", ""));
    }
    if (getIsCombatTransport() && getTransportCapacity() > 0) {
      tuples.add(Tuple.of("Is a Combat Transport", ""));
    } else if (getTransportCapacity() > 0 && getIsSea()) {
      tuples.add(Tuple.of("Is a Sea Transport", ""));
    }
    if (getTransportCost() > -1) {
      tuples.add(Tuple.of("Transporting Cost", String.valueOf(getTransportCost())));
    }
    if (getTransportCapacity() > 0
        && (getIsSea()
            || (getIsAir() && playerHasParatroopers(player))
            || (playerHasMechInf(player) && !getIsSea() && !getIsAir()))) {
      tuples.add(Tuple.of("Transporting Capacity", String.valueOf(getTransportCapacity())));
    }

    if (getCarrierCost() > -1) {
      tuples.add(Tuple.of("Carrier Cost", String.valueOf(getCarrierCost())));
    }
    if (getCarrierCapacity() > 0) {
      tuples.add(Tuple.of("Carrier Capacity", String.valueOf(getCarrierCapacity())));
    }

    if (!getWhenCombatDamaged().isEmpty()) {
      tuples.add(Tuple.of("When Hit Loses Certain Abilities", ""));
    }

    if (getMaxBuiltPerPlayer() > -1) {
      tuples.add(Tuple.of("Max Built Allowed", String.valueOf(getMaxBuiltPerPlayer())));
    }

    if (getRepairsUnits() != null
        && !getRepairsUnits().isEmpty()
        && Properties.getTwoHitPointUnitsRequireRepairFacilities(getData())
        && (Properties.getBattleshipsRepairAtBeginningOfRound(getData())
            || Properties.getBattleshipsRepairAtEndOfRound(getData()))) {
      if (getRepairsUnits().size() <= 4) {
        tuples.add(
            Tuple.of(
                "Can Repair",
                MyFormatter.integerDefaultNamedMapToString(getRepairsUnits(), " ", "=", false)));
      } else {
        tuples.add(Tuple.of("Can Repair some Units", ""));
      }
    }

    if (getGivesMovement() != null
        && getGivesMovement().totalValues() > 0
        && Properties.getUnitsMayGiveBonusMovement(getData())) {
      if (getGivesMovement().size() <= 4) {
        tuples.add(
            Tuple.of(
                "Can Modify Unit Movement",
                MyFormatter.integerDefaultNamedMapToString(getGivesMovement(), " ", "=", false)));
      } else {
        tuples.add(Tuple.of("Can Modify Unit Movement", ""));
      }
    }

    if (getConsumesUnits() != null && getConsumesUnits().totalValues() == 1) {
      tuples.add(
          Tuple.of(
              "Unit is an Upgrade Of", getConsumesUnits().keySet().iterator().next().getName()));
    } else if (getConsumesUnits() != null && getConsumesUnits().totalValues() > 0) {
      if (getConsumesUnits().size() <= 4) {
        tuples.add(
            Tuple.of(
                "Unit Consumes on Placement",
                MyFormatter.integerDefaultNamedMapToString(getConsumesUnits(), " ", "x", true)));
      } else {
        tuples.add(Tuple.of("Unit Consumes Other Units on Placement", ""));
      }
    }

    if (getRequiresUnits() != null
        && !getRequiresUnits().isEmpty()
        && Properties.getUnitPlacementRestrictions(getData())) {
      final List<String> totalUnitsListed = new ArrayList<>();
      for (final String[] list : getRequiresUnits()) {
        totalUnitsListed.addAll(List.of(list));
      }
      if (totalUnitsListed.size() > 4) {
        tuples.add(Tuple.of("Has Placement Requirements", ""));
      } else {
        tuples.add(Tuple.of("Placement Requirements", joinRequiredUnits(getRequiresUnits())));
      }
    }

    if (getRequiresUnitsToMove() != null && !getRequiresUnitsToMove().isEmpty()) {
      final List<String> totalUnitsListed = new ArrayList<>();
      for (final String[] list : getRequiresUnitsToMove()) {
        totalUnitsListed.addAll(List.of(list));
      }
      if (totalUnitsListed.size() > 4) {
        tuples.add(Tuple.of("Has Movement Requirements", ""));
      } else {
        tuples.add(Tuple.of("Movement Requirements", joinRequiredUnits(getRequiresUnitsToMove())));
      }
    }

    if (getUnitPlacementRestrictions() != null
        && Properties.getUnitPlacementRestrictions(getData())) {
      if (getUnitPlacementRestrictions().length > 4) {
        tuples.add(Tuple.of("Has Placement Restrictions", ""));
      } else {
        tuples.add(
            Tuple.of("Placement Restrictions", Arrays.toString(getUnitPlacementRestrictions())));
      }
    }
    if (getCanOnlyBePlacedInTerritoryValuedAtX() > 0
        && Properties.getUnitPlacementRestrictions(getData())) {
      tuples.add(
          Tuple.of(
              "Must be Placed in Territory with Value of at Least",
              String.valueOf(getCanOnlyBePlacedInTerritoryValuedAtX())));
    }

    if (getCanNotMoveDuringCombatMove()) {
      tuples.add(Tuple.of("Cannot Combat Move", ""));
    }

    if (getMovementLimit() != null) {
      if (getMovementLimit().getFirst() == Integer.MAX_VALUE
          && (getIsAaForBombingThisUnitOnly() || getIsAaForCombatOnly())
          && !(Properties.getWW2V2(getData())
              || Properties.getWW2V3(getData())
              || Properties.getMultipleAaPerTerritory(getData()))) {
        tuples.add(
            Tuple.of("Max " + getMovementLimit().getSecond() + " Units Moving per Territory", "1"));
      } else if (getMovementLimit().getFirst() < 10000) {
        tuples.add(
            Tuple.of(
                "Max " + getMovementLimit().getSecond() + " Units Moving per Territory",
                String.valueOf(getMovementLimit().getFirst())));
      }
    }

    if (getAttackingLimit() != null) {
      if (getAttackingLimit().getFirst() == Integer.MAX_VALUE
          && (getIsAaForBombingThisUnitOnly() || getIsAaForCombatOnly())
          && !(Properties.getWW2V2(getData())
              || Properties.getWW2V3(getData())
              || Properties.getMultipleAaPerTerritory(getData()))) {
        tuples.add(
            Tuple.of(
                "Max " + getAttackingLimit().getSecond() + " Units Attacking per Territory", "1"));
      } else if (getAttackingLimit().getFirst() < 10000) {
        tuples.add(
            Tuple.of(
                "Max " + getAttackingLimit().getSecond() + " Units Attacking per Territory",
                String.valueOf(getAttackingLimit().getFirst())));
      }
    }

    if (getPlacementLimit() != null) {
      if (getPlacementLimit().getFirst() == Integer.MAX_VALUE
          && (getIsAaForBombingThisUnitOnly() || getIsAaForCombatOnly())
          && !(Properties.getWW2V2(getData())
              || Properties.getWW2V3(getData())
              || Properties.getMultipleAaPerTerritory(getData()))) {
        tuples.add(
            Tuple.of(
                "Max " + getPlacementLimit().getSecond() + " Units Placed per Territory", "1"));
      } else if (getPlacementLimit().getFirst() < 10000) {
        tuples.add(
            Tuple.of(
                "Max " + getPlacementLimit().getSecond() + " Units Placed per Territory",
                String.valueOf(getPlacementLimit().getFirst())));
      }
    }

    final StringBuilder result = new StringBuilder();
    for (final Tuple<String, String> tuple : tuples) {
      result.append(tuple.getFirst());
      if (!tuple.getSecond().isEmpty()) {
        result
            .append(": <b>")
            .append(
                MyFormatter.addHtmlBreaksAndIndents(
                    tuple.getSecond(), 100 - tuple.getFirst().length(), 100))
            .append("</b>");
      }
      result.append("<br />");
    }
    return result.toString();
  }

  private void addLabeledUnitTypes(
      final String label,
      final Collection<UnitType> unitTypes,
      final List<Tuple<String, String>> tuples) {
    if (!unitTypes.isEmpty()) {
      if (unitTypes.size() <= 4) {
        tuples.add(Tuple.of(label, MyFormatter.defaultNamedToTextList(unitTypes)));
      } else {
        tuples.add(Tuple.of(label + " Some Units", ""));
      }
    }
  }

  private static <T extends DefaultNamed> void addIntegerMapDescription(
      final String key, final IntegerMap<T> integerMap, final List<Tuple<String, String>> tuples) {
    if (integerMap != null && !integerMap.isEmpty()) {
      final StringBuilder sb = new StringBuilder();
      if (integerMap.size() > 4) {
        sb.append(integerMap.totalValues());
      } else {
        for (final Entry<T, Integer> entry : integerMap.entrySet()) {
          sb.append(entry.getValue()).append("x").append(entry.getKey().getName()).append(" ");
        }
      }
      tuples.add(Tuple.of(key, sb.toString()));
    }
  }

  private void addAaDescription(
      final String startOfKey,
      final int aa,
      final int aaMaxDieSides,
      final List<Tuple<String, String>> tuples) {
    if ((getIsAaForCombatOnly() || getIsAaForBombingThisUnitOnly() || getIsAaForFlyOverOnly())
        && (aa > 0)) {
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
      tuples.add(Tuple.of(startOfKey + getAaKey(), string));
    }
  }

  private String getAaKey() {
    if (getIsAaForCombatOnly()
        && getIsAaForFlyOverOnly()
        && !Properties.getAaTerritoryRestricted(getData())) {
      return " for Combat & Move Through";
    } else if (getIsAaForBombingThisUnitOnly()
        && getIsAaForFlyOverOnly()
        && !Properties.getAaTerritoryRestricted(getData())) {
      return " for Raids & Move Through";
    } else if (getIsAaForCombatOnly()) {
      return " for Combat";
    } else if (getIsAaForBombingThisUnitOnly()) {
      return " for Raids";
    } else if (getIsAaForFlyOverOnly()) {
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
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put(
            "isAir",
            MutableProperty.of(this::setIsAir, this::setIsAir, this::getIsAir, this::resetIsAir))
        .put(
            "isMechanized", // kept for map compatibility; remove upon next map-incompatible release
            MutableProperty.ofWriteOnlyString(s -> {}))
        .put(
            "isParatroop", // kept for map compatibility; remove upon next map-incompatible release
            MutableProperty.ofWriteOnlyString(s -> {}))
        .put(
            "isSea",
            MutableProperty.of(this::setIsSea, this::setIsSea, this::getIsSea, this::resetIsSea))
        .put(
            "movement",
            MutableProperty.of(
                this::setMovement, this::setMovement, this::getMovement, this::resetMovement))
        .put(
            "canBlitz",
            MutableProperty.of(
                this::setCanBlitz, this::setCanBlitz, this::getCanBlitz, this::resetCanBlitz))
        .put(
            "isKamikaze",
            MutableProperty.of(
                this::setIsKamikaze,
                this::setIsKamikaze,
                this::getIsKamikaze,
                this::resetIsKamikaze))
        .put(
            "canInvadeOnlyFrom",
            MutableProperty.of(
                this::setCanInvadeOnlyFrom,
                this::setCanInvadeOnlyFrom,
                this::getCanInvadeOnlyFrom,
                this::resetCanInvadeOnlyFrom))
        .put(
            "fuelCost",
            MutableProperty.of(
                this::setFuelCost, this::setFuelCost, this::getFuelCost, this::resetFuelCost))
        .put(
            "fuelFlatCost",
            MutableProperty.of(
                this::setFuelFlatCost,
                this::setFuelFlatCost,
                this::getFuelFlatCost,
                this::resetFuelFlatCost))
        .put(
            "canNotMoveDuringCombatMove",
            MutableProperty.of(
                this::setCanNotMoveDuringCombatMove,
                this::setCanNotMoveDuringCombatMove,
                this::getCanNotMoveDuringCombatMove,
                this::resetCanNotMoveDuringCombatMove))
        .put(
            "movementLimit",
            MutableProperty.of(
                this::setMovementLimit,
                this::setMovementLimit,
                this::getMovementLimit,
                this::resetMovementLimit))
        .put(
            "attack",
            MutableProperty.of(
                this::setAttack, this::setAttack, this::getAttack, this::resetAttack))
        .put(
            "defense",
            MutableProperty.of(
                this::setDefense, this::setDefense, this::getDefense, this::resetDefense))
        .put(
            "isInfrastructure",
            MutableProperty.of(
                this::setIsInfrastructure,
                this::setIsInfrastructure,
                this::getIsInfrastructure,
                this::resetIsInfrastructure))
        .put(
            "canBombard",
            MutableProperty.of(
                this::setCanBombard,
                this::setCanBombard,
                this::getCanBombard,
                this::resetCanBombard))
        .put(
            "bombard",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt, this::setBombard, this::getBombard, () -> -1))
        .put("isSub", MutableProperty.<Boolean>ofWriteOnly(this::setIsSub, this::setIsSub))
        .put(
            "canEvade",
            MutableProperty.ofMapper(
                DefaultAttachment::getBool, this::setCanEvade, this::getCanEvade, () -> false))
        .put(
            "isFirstStrike",
            MutableProperty.ofMapper(
                DefaultAttachment::getBool,
                this::setIsFirstStrike,
                this::getIsFirstStrike,
                () -> false))
        .put(
            "canNotTarget",
            MutableProperty.of(
                this::setCanNotTarget,
                this::setCanNotTarget,
                this::getCanNotTarget,
                this::resetCanNotTarget))
        .put(
            "canNotBeTargetedBy",
            MutableProperty.of(
                this::setCanNotBeTargetedBy,
                this::setCanNotBeTargetedBy,
                this::getCanNotBeTargetedBy,
                this::resetCanNotBeTargetedBy))
        .put(
            "canMoveThroughEnemies",
            MutableProperty.ofMapper(
                DefaultAttachment::getBool,
                this::setCanMoveThroughEnemies,
                this::getCanMoveThroughEnemies,
                () -> false))
        .put(
            "canBeMovedThroughByEnemies",
            MutableProperty.ofMapper(
                DefaultAttachment::getBool,
                this::setCanBeMovedThroughByEnemies,
                this::getCanBeMovedThroughByEnemies,
                () -> false))
        .put(
            "isDestroyer",
            MutableProperty.of(
                this::setIsDestroyer,
                this::setIsDestroyer,
                this::getIsDestroyer,
                this::resetIsDestroyer))
        .put(
            "artillery",
            MutableProperty.of(
                this::setArtillery, this::setArtillery, this::getArtillery, this::resetArtillery))
        .put(
            "artillerySupportable",
            MutableProperty.of(
                this::setArtillerySupportable,
                this::setArtillerySupportable,
                this::getArtillerySupportable,
                this::resetArtillerySupportable))
        .put(
            "unitSupportCount",
            MutableProperty.of(
                this::setUnitSupportCount,
                this::setUnitSupportCount,
                this::getUnitSupportCount,
                this::resetUnitSupportCount))
        .put(
            "isMarine",
            MutableProperty.of(
                this::setIsMarine, this::setIsMarine, this::getIsMarine, this::resetIsMarine))
        .put(
            "isSuicide",
            MutableProperty.<Boolean>ofWriteOnly(this::setIsSuicide, this::setIsSuicide))
        .put(
            "isSuicideOnAttack",
            MutableProperty.ofMapper(
                DefaultAttachment::getBool,
                this::setIsSuicideOnAttack,
                this::getIsSuicideOnAttack,
                () -> false))
        .put(
            "isSuicideOnDefense",
            MutableProperty.ofMapper(
                DefaultAttachment::getBool,
                this::setIsSuicideOnDefense,
                this::getIsSuicideOnDefense,
                () -> false))
        .put(
            "isSuicideOnHit",
            MutableProperty.of(
                this::setIsSuicideOnHit,
                this::setIsSuicideOnHit,
                this::getIsSuicideOnHit,
                this::resetIsSuicideOnHit))
        .put(
            "attackingLimit",
            MutableProperty.of(
                this::setAttackingLimit,
                this::setAttackingLimit,
                this::getAttackingLimit,
                this::resetAttackingLimit))
        .put(
            "attackRolls",
            MutableProperty.of(
                this::setAttackRolls,
                this::setAttackRolls,
                this::getAttackRolls,
                this::resetAttackRolls))
        .put(
            "defenseRolls",
            MutableProperty.of(
                this::setDefenseRolls,
                this::setDefenseRolls,
                this::getDefenseRolls,
                this::resetDefenseRolls))
        .put(
            "chooseBestRoll",
            MutableProperty.of(
                this::setChooseBestRoll,
                this::setChooseBestRoll,
                this::getChooseBestRoll,
                this::resetChooseBestRoll))
        .put(
            "isCombatTransport",
            MutableProperty.of(
                this::setIsCombatTransport,
                this::setIsCombatTransport,
                this::getIsCombatTransport,
                this::resetIsCombatTransport))
        .put(
            "transportCapacity",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setTransportCapacity,
                this::getTransportCapacity,
                () -> -1))
        .put(
            "transportCost",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setTransportCost,
                this::getTransportCost,
                () -> -1))
        .put(
            "carrierCapacity",
            MutableProperty.of(
                this::setCarrierCapacity,
                this::setCarrierCapacity,
                this::getCarrierCapacity,
                this::resetCarrierCapacity))
        .put(
            "carrierCost",
            MutableProperty.of(
                this::setCarrierCost,
                this::setCarrierCost,
                this::getCarrierCost,
                this::resetCarrierCost))
        .put(
            "isAirTransport",
            MutableProperty.of(
                this::setIsAirTransport,
                this::setIsAirTransport,
                this::getIsAirTransport,
                this::resetIsAirTransport))
        .put(
            "isAirTransportable",
            MutableProperty.of(
                this::setIsAirTransportable,
                this::setIsAirTransportable,
                this::getIsAirTransportable,
                this::resetIsAirTransportable))
        .put(
            "isInfantry", // kept for map compatibility; remove upon next map-incompatible release
            MutableProperty.of(
                this::setIsLandTransportable,
                this::setIsLandTransportable,
                this::getIsLandTransportable,
                this::resetIsLandTransportable))
        .put(
            "isLandTransport",
            MutableProperty.of(
                this::setIsLandTransport,
                this::setIsLandTransport,
                this::getIsLandTransport,
                this::resetIsLandTransport))
        .put(
            "isLandTransportable",
            MutableProperty.of(
                this::setIsLandTransportable,
                this::setIsLandTransportable,
                this::getIsLandTransportable,
                this::resetIsLandTransportable))
        .put(
            "isAAforCombatOnly",
            MutableProperty.of(
                this::setIsAaForCombatOnly,
                this::setIsAaForCombatOnly,
                this::getIsAaForCombatOnly,
                this::resetIsAaForCombatOnly))
        .put(
            "isAAforBombingThisUnitOnly",
            MutableProperty.of(
                this::setIsAaForBombingThisUnitOnly,
                this::setIsAaForBombingThisUnitOnly,
                this::getIsAaForBombingThisUnitOnly,
                this::resetIsAaForBombingThisUnitOnly))
        .put(
            "isAAforFlyOverOnly",
            MutableProperty.of(
                this::setIsAaForFlyOverOnly,
                this::setIsAaForFlyOverOnly,
                this::getIsAaForFlyOverOnly,
                this::resetIsAaForFlyOverOnly))
        .put(
            "isRocket",
            MutableProperty.of(
                this::setIsRocket, this::setIsRocket, this::getIsRocket, this::resetIsRocket))
        .put(
            "attackAA",
            MutableProperty.of(
                this::setAttackAa, this::setAttackAa, this::getAttackAa, this::resetAttackAa))
        .put(
            "offensiveAttackAA",
            MutableProperty.of(
                this::setOffensiveAttackAa,
                this::setOffensiveAttackAa,
                this::getOffensiveAttackAa,
                this::resetOffensiveAttackAa))
        .put(
            "attackAAmaxDieSides",
            MutableProperty.of(
                this::setAttackAaMaxDieSides,
                this::setAttackAaMaxDieSides,
                this::getAttackAaMaxDieSides,
                this::resetAttackAaMaxDieSides))
        .put(
            "offensiveAttackAAmaxDieSides",
            MutableProperty.of(
                this::setOffensiveAttackAaMaxDieSides,
                this::setOffensiveAttackAaMaxDieSides,
                this::getOffensiveAttackAaMaxDieSides,
                this::resetOffensiveAttackAaMaxDieSides))
        .put(
            "maxAAattacks",
            MutableProperty.of(
                this::setMaxAaAttacks,
                this::setMaxAaAttacks,
                this::getMaxAaAttacks,
                this::resetMaxAaAttacks))
        .put(
            "maxRoundsAA",
            MutableProperty.of(
                this::setMaxRoundsAa,
                this::setMaxRoundsAa,
                this::getMaxRoundsAa,
                this::resetMaxRoundsAa))
        .put(
            "typeAA", MutableProperty.ofString(this::setTypeAa, this::getTypeAa, this::resetTypeAa))
        .put(
            "targetsAA",
            MutableProperty.of(
                this::setTargetsAa, this::setTargetsAa, this::getTargetsAa, this::resetTargetsAa))
        .put(
            "mayOverStackAA",
            MutableProperty.of(
                this::setMayOverStackAa,
                this::setMayOverStackAa,
                this::getMayOverStackAa,
                this::resetMayOverStackAa))
        .put(
            "damageableAA",
            MutableProperty.of(
                this::setDamageableAa,
                this::setDamageableAa,
                this::getDamageableAa,
                this::resetDamageableAa))
        .put(
            "willNotFireIfPresent",
            MutableProperty.of(
                this::setWillNotFireIfPresent,
                this::setWillNotFireIfPresent,
                this::getWillNotFireIfPresent,
                this::resetWillNotFireIfPresent))
        .put(
            "isStrategicBomber",
            MutableProperty.of(
                this::setIsStrategicBomber,
                this::setIsStrategicBomber,
                this::getIsStrategicBomber,
                this::resetIsStrategicBomber))
        .put(
            "bombingMaxDieSides",
            MutableProperty.of(
                this::setBombingMaxDieSides,
                this::setBombingMaxDieSides,
                this::getBombingMaxDieSides,
                this::resetBombingMaxDieSides))
        .put(
            "bombingBonus",
            MutableProperty.of(
                this::setBombingBonus,
                this::setBombingBonus,
                this::getBombingBonus,
                this::resetBombingBonus))
        .put(
            "canIntercept",
            MutableProperty.of(
                this::setCanIntercept,
                this::setCanIntercept,
                this::getCanIntercept,
                this::resetCanIntercept))
        .put(
            "requiresAirbaseToIntercept",
            MutableProperty.of(
                this::setRequiresAirBaseToIntercept,
                this::setRequiresAirBaseToIntercept,
                this::getRequiresAirBaseToIntercept,
                this::resetRequiresAirBaseToIntercept))
        .put(
            "canEscort",
            MutableProperty.of(
                this::setCanEscort, this::setCanEscort, this::getCanEscort, this::resetCanEscort))
        .put(
            "canAirBattle",
            MutableProperty.of(
                this::setCanAirBattle,
                this::setCanAirBattle,
                this::getCanAirBattle,
                this::resetCanAirBattle))
        .put(
            "airDefense",
            MutableProperty.of(
                this::setAirDefense,
                this::setAirDefense,
                this::getAirDefense,
                this::resetAirDefense))
        .put(
            "airAttack",
            MutableProperty.of(
                this::setAirAttack, this::setAirAttack, this::getAirAttack, this::resetAirAttack))
        .put(
            "bombingTargets",
            MutableProperty.of(
                this::setBombingTargets,
                this::setBombingTargets,
                this::getBombingTargets,
                this::resetBombingTargets))
        .put(
            "canProduceUnits",
            MutableProperty.of(
                this::setCanProduceUnits,
                this::setCanProduceUnits,
                this::getCanProduceUnits,
                this::resetCanProduceUnits))
        .put(
            "canProduceXUnits",
            MutableProperty.of(
                this::setCanProduceXUnits,
                this::setCanProduceXUnits,
                this::getCanProduceXUnits,
                this::resetCanProduceXUnits))
        .put(
            "createsUnitsList",
            MutableProperty.of(
                this::setCreatesUnitsList,
                this::setCreatesUnitsList,
                this::getCreatesUnitsList,
                this::resetCreatesUnitsList))
        .put(
            "createsResourcesList",
            MutableProperty.of(
                this::setCreatesResourcesList,
                this::setCreatesResourcesList,
                this::getCreatesResourcesList,
                this::resetCreatesResourcesList))
        .put(
            "hitPoints",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt, this::setHitPoints, this::getHitPoints, () -> 1))
        .put(
            "canBeDamaged",
            MutableProperty.of(
                this::setCanBeDamaged,
                this::setCanBeDamaged,
                this::getCanBeDamaged,
                this::resetCanBeDamaged))
        .put(
            "maxDamage",
            MutableProperty.of(
                this::setMaxDamage, this::setMaxDamage, this::getMaxDamage, this::resetMaxDamage))
        .put(
            "maxOperationalDamage",
            MutableProperty.of(
                this::setMaxOperationalDamage,
                this::setMaxOperationalDamage,
                this::getMaxOperationalDamage,
                this::resetMaxOperationalDamage))
        .put(
            "canDieFromReachingMaxDamage",
            MutableProperty.of(
                this::setCanDieFromReachingMaxDamage,
                this::setCanDieFromReachingMaxDamage,
                this::getCanDieFromReachingMaxDamage,
                this::resetCanDieFromReachingMaxDamage))
        .put(
            "isConstruction",
            MutableProperty.of(
                this::setIsConstruction,
                this::setIsConstruction,
                this::getIsConstruction,
                this::resetIsConstruction))
        .put(
            "constructionType",
            MutableProperty.ofString(
                this::setConstructionType, this::getConstructionType, this::resetConstructionType))
        .put(
            "constructionsPerTerrPerTypePerTurn",
            MutableProperty.of(
                this::setConstructionsPerTerrPerTypePerTurn,
                this::setConstructionsPerTerrPerTypePerTurn,
                this::getConstructionsPerTerrPerTypePerTurn,
                this::resetConstructionsPerTerrPerTypePerTurn))
        .put(
            "maxConstructionsPerTypePerTerr",
            MutableProperty.of(
                this::setMaxConstructionsPerTypePerTerr,
                this::setMaxConstructionsPerTypePerTerr,
                this::getMaxConstructionsPerTypePerTerr,
                this::resetMaxConstructionsPerTypePerTerr))
        .put(
            "canOnlyBePlacedInTerritoryValuedAtX",
            MutableProperty.of(
                this::setCanOnlyBePlacedInTerritoryValuedAtX,
                this::setCanOnlyBePlacedInTerritoryValuedAtX,
                this::getCanOnlyBePlacedInTerritoryValuedAtX,
                this::resetCanOnlyBePlacedInTerritoryValuedAtX))
        .put(
            "requiresUnits",
            MutableProperty.of(
                this::setRequiresUnits,
                this::setRequiresUnits,
                this::getRequiresUnits,
                this::resetRequiresUnits))
        .put(
            "consumesUnits",
            MutableProperty.of(
                this::setConsumesUnits,
                this::setConsumesUnits,
                this::getConsumesUnits,
                this::resetConsumesUnits))
        .put(
            "requiresUnitsToMove",
            MutableProperty.of(
                this::setRequiresUnitsToMove,
                this::setRequiresUnitsToMove,
                this::getRequiresUnitsToMove,
                this::resetRequiresUnitsToMove))
        .put(
            "unitPlacementRestrictions",
            MutableProperty.of(
                this::setUnitPlacementRestrictions,
                this::setUnitPlacementRestrictions,
                this::getUnitPlacementRestrictions,
                this::resetUnitPlacementRestrictions))
        .put(
            "maxBuiltPerPlayer",
            MutableProperty.of(
                this::setMaxBuiltPerPlayer,
                this::setMaxBuiltPerPlayer,
                this::getMaxBuiltPerPlayer,
                this::resetMaxBuiltPerPlayer))
        .put(
            "placementLimit",
            MutableProperty.of(
                this::setPlacementLimit,
                this::setPlacementLimit,
                this::getPlacementLimit,
                this::resetPlacementLimit))
        .put(
            "canScramble",
            MutableProperty.of(
                this::setCanScramble,
                this::setCanScramble,
                this::getCanScramble,
                this::resetCanScramble))
        .put(
            "isAirBase",
            MutableProperty.of(
                this::setIsAirBase, this::setIsAirBase, this::getIsAirBase, this::resetIsAirBase))
        .put(
            "maxScrambleDistance",
            MutableProperty.of(
                this::setMaxScrambleDistance,
                this::setMaxScrambleDistance,
                this::getMaxScrambleDistance,
                this::resetMaxScrambleDistance))
        .put(
            "maxScrambleCount",
            MutableProperty.of(
                this::setMaxScrambleCount,
                this::setMaxScrambleCount,
                this::getMaxScrambleCount,
                this::resetMaxScrambleCount))
        .put(
            "maxInterceptCount",
            MutableProperty.of(
                this::setMaxInterceptCount,
                this::setMaxInterceptCount,
                this::getMaxInterceptCount,
                this::resetMaxInterceptCount))
        .put(
            "blockade",
            MutableProperty.of(
                this::setBlockade, this::setBlockade, this::getBlockade, this::resetBlockade))
        .put(
            "repairsUnits",
            MutableProperty.of(
                this::setRepairsUnits,
                this::setRepairsUnits,
                this::getRepairsUnits,
                this::resetRepairsUnits))
        .put(
            "givesMovement",
            MutableProperty.of(
                this::setGivesMovement,
                this::setGivesMovement,
                this::getGivesMovement,
                this::resetGivesMovement))
        .put(
            "destroyedWhenCapturedBy",
            MutableProperty.of(
                this::setDestroyedWhenCapturedBy,
                this::setDestroyedWhenCapturedBy,
                this::getDestroyedWhenCapturedBy,
                this::resetDestroyedWhenCapturedBy))
        .put(
            "whenHitPointsDamagedChangesInto",
            MutableProperty.of(
                this::setWhenHitPointsDamagedChangesInto,
                this::setWhenHitPointsDamagedChangesInto,
                this::getWhenHitPointsDamagedChangesInto,
                this::resetWhenHitPointsDamagedChangesInto))
        .put(
            "whenHitPointsRepairedChangesInto",
            MutableProperty.of(
                this::setWhenHitPointsRepairedChangesInto,
                this::setWhenHitPointsRepairedChangesInto,
                this::getWhenHitPointsRepairedChangesInto,
                this::resetWhenHitPointsRepairedChangesInto))
        .put(
            "whenCapturedChangesInto",
            MutableProperty.of(
                this::setWhenCapturedChangesInto,
                this::setWhenCapturedChangesInto,
                this::getWhenCapturedChangesInto,
                this::resetWhenCapturedChangesInto))
        .put(
            "whenCapturedSustainsDamage",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setWhenCapturedSustainsDamage,
                this::getWhenCapturedSustainsDamage,
                () -> 0))
        .put(
            "canBeCapturedOnEnteringBy",
            MutableProperty.of(
                this::setCanBeCapturedOnEnteringBy,
                this::setCanBeCapturedOnEnteringBy,
                this::getCanBeCapturedOnEnteringBy,
                this::resetCanBeCapturedOnEnteringBy))
        .put(
            "canBeGivenByTerritoryTo",
            MutableProperty.of(
                this::setCanBeGivenByTerritoryTo,
                this::setCanBeGivenByTerritoryTo,
                this::getCanBeGivenByTerritoryTo,
                this::resetCanBeGivenByTerritoryTo))
        .put(
            "whenCombatDamaged",
            MutableProperty.of(
                this::setWhenCombatDamaged,
                this::setWhenCombatDamaged,
                this::getWhenCombatDamaged,
                this::resetWhenCombatDamaged))
        .put(
            "receivesAbilityWhenWith",
            MutableProperty.of(
                this::setReceivesAbilityWhenWith,
                this::setReceivesAbilityWhenWith,
                this::getReceivesAbilityWhenWith,
                this::resetReceivesAbilityWhenWith))
        .put(
            "special",
            MutableProperty.of(
                this::setSpecial, this::setSpecial, this::getSpecial, this::resetSpecial))
        .put("tuv", MutableProperty.of(this::setTuv, this::setTuv, this::getTuv, this::resetTuv))
        .put(
            "isFactory",
            MutableProperty.<Boolean>ofWriteOnly(this::setIsFactory, this::setIsFactory))
        .put("isAA", MutableProperty.<Boolean>ofWriteOnly(this::setIsAa, this::setIsAa))
        .put(
            "destroyedWhenCapturedFrom",
            MutableProperty.ofWriteOnlyString(this::setDestroyedWhenCapturedFrom))
        .put(
            "unitPlacementOnlyAllowedIn",
            MutableProperty.ofWriteOnlyString(this::setUnitPlacementOnlyAllowedIn))
        .put(
            "isAAmovement",
            MutableProperty.<Boolean>ofWriteOnly(this::setIsAaMovement, this::setIsAaMovement))
        .put("isTwoHit", MutableProperty.<Boolean>ofWriteOnly(this::setIsTwoHit, this::setIsTwoHit))
        .put(
            "canRetreatOnStalemate",
            MutableProperty.of(
                this::setCanRetreatOnStalemate, this::setCanRetreatOnStalemate,
                this::getCanRetreatOnStalemate, this::resetCanRetreatOnStalemate))
        .build();
  }
}
