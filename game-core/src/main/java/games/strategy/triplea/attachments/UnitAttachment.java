package games.strategy.triplea.attachments;

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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.DefaultNamed;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Tuple;

/**
 * Despite the misleading name, this attaches not to individual Units but to UnitTypes.
 */
@MapSupport
public class UnitAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -2946748686268541820L;

  /**
   * Convenience method.
   */
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

  public static final String UNITSMAYNOTLANDONCARRIER = "unitsMayNotLandOnCarrier";
  public static final String UNITSMAYNOTLEAVEALLIEDCARRIER = "unitsMayNotLeaveAlliedCarrier";
  // movement related
  private boolean m_isAir = false;
  private boolean m_isSea = false;
  private int m_movement = 0;
  private boolean m_canBlitz = false;
  private boolean m_isKamikaze = false;
  // a colon delimited list of transports where this unit may invade from, it supports "none"
  // and if empty it allows you to invade from all
  private String[] m_canInvadeOnlyFrom = null;
  private IntegerMap<Resource> m_fuelCost = new IntegerMap<>();
  private IntegerMap<Resource> m_fuelFlatCost = new IntegerMap<>();
  private boolean m_canNotMoveDuringCombatMove = false;
  private Tuple<Integer, String> m_movementLimit = null;
  // combat related
  private int m_attack = 0;
  private int m_defense = 0;
  private boolean m_isInfrastructure = false;
  private boolean m_canBombard = false;
  private int m_bombard = -1;
  private boolean m_isSub = false;
  private boolean m_isDestroyer = false;
  private boolean m_artillery = false;
  private boolean m_artillerySupportable = false;
  private int m_unitSupportCount = -1;
  private int m_isMarine = 0;
  private boolean m_isSuicide = false;
  private boolean m_isSuicideOnHit = false;
  private Tuple<Integer, String> m_attackingLimit = null;
  private int m_attackRolls = 1;
  private int m_defenseRolls = 1;
  private boolean m_chooseBestRoll = false;
  // transportation related
  private boolean m_isCombatTransport = false;
  // -1 if cant transport
  private int m_transportCapacity = -1;
  // -1 if cant be transported
  private int m_transportCost = -1;
  // -1 if cant act as a carrier
  private int m_carrierCapacity = -1;
  // -1 if cant land on a carrier
  private int m_carrierCost = -1;
  private boolean m_isAirTransport = false;
  private boolean m_isAirTransportable = false;
  // isInfantry is DEPRECATED, use isLandTransportable
  private boolean m_isInfantry = false;
  private boolean m_isLandTransport = false;
  private boolean m_isLandTransportable = false;
  // aa related
  // "isAA" and "isAAmovement" are also valid setters, used as shortcuts for calling multiple aa related setters. Must
  // keep.
  private boolean m_isAAforCombatOnly = false;
  private boolean m_isAAforBombingThisUnitOnly = false;
  private boolean m_isAAforFlyOverOnly = false;
  private boolean m_isRocket = false;
  private int m_attackAA = 1;
  private int m_offensiveAttackAA = 0;
  private int m_attackAAmaxDieSides = -1;
  private int m_offensiveAttackAAmaxDieSides = -1;
  // -1 means infinite
  private int m_maxAAattacks = -1;
  // -1 means infinite
  private int m_maxRoundsAA = 1;
  // default value for when it is not set
  private String m_typeAA = "AA";
  // null means targeting air units only
  private Set<UnitType> m_targetsAA = null;
  // if false, we cannot shoot more times than there are number of planes
  private boolean m_mayOverStackAA = false;
  // if false, we instantly kill anything our AA shot hits
  private boolean m_damageableAA = false;
  // if these enemy units are present, the gun does not fire at all
  private Set<UnitType> m_willNotFireIfPresent = new HashSet<>();
  // strategic bombing related
  private boolean m_isStrategicBomber = false;
  private int m_bombingMaxDieSides = -1;
  private int m_bombingBonus = 0;
  private boolean m_canIntercept = false;
  private boolean m_canEscort = false;
  private boolean m_canAirBattle = false;
  private int m_airDefense = 0;
  private int m_airAttack = 0;
  // null means they can target any unit that can be damaged
  private Set<UnitType> m_bombingTargets = null;
  // production related
  // this has been split into canProduceUnits, isConstruction, canBeDamaged, and isInfrastructure
  // private boolean m_isFactory = false;
  private boolean m_canProduceUnits = false;
  // -1 means either it can't produce any, or it produces at the value of the territory it is located in
  private int m_canProduceXUnits = -1;
  private IntegerMap<UnitType> m_createsUnitsList = new IntegerMap<>();
  private IntegerMap<Resource> m_createsResourcesList = new IntegerMap<>();
  // damage related
  private int m_hitPoints = 1;
  private boolean m_canBeDamaged = false;
  // this is bombing damage, not hitpoints. default of 2 means that factories will take 2x the territory value
  // they are in, of damage.
  private int m_maxDamage = 2;
  // -1 if can't be disabled
  private int m_maxOperationalDamage = -1;
  private boolean m_canDieFromReachingMaxDamage = false;
  // placement related
  private boolean m_isConstruction = false;
  // can be any String except for "none" if isConstruction is true
  private String m_constructionType = "none";
  // -1 if not set, is meaningless
  private int m_constructionsPerTerrPerTypePerTurn = -1;
  // -1 if not set, is meaningless
  private int m_maxConstructionsPerTypePerTerr = -1;
  // -1 means anywhere
  private int m_canOnlyBePlacedInTerritoryValuedAtX = -1;
  // multiple colon delimited lists of the unit combos required for
  // this unit to be built somewhere. (units must be in same
  // territory, owned by player, not be disabled)
  private List<String[]> m_requiresUnits = new ArrayList<>();
  private IntegerMap<UnitType> m_consumesUnits = new IntegerMap<>();
  // multiple colon delimited lists of the unit combos required for
  // this unit to move into a territory. (units must be owned by player, not be disabled)
  private List<String[]> m_requiresUnitsToMove = new ArrayList<>();
  // a colon delimited list of territories where this unit may not be placed
  // also an allowed setter is "setUnitPlacementOnlyAllowedIn",
  // which just creates m_unitPlacementRestrictions with an inverted list of territories
  private String[] m_unitPlacementRestrictions = null;
  // -1 if infinite (infinite is default)
  private int m_maxBuiltPerPlayer = -1;
  private Tuple<Integer, String> m_placementLimit = null;
  // scrambling related
  private boolean m_canScramble = false;
  private boolean m_isAirBase = false;
  // -1 if can't scramble
  private int m_maxScrambleDistance = -1;
  // -1 for infinite
  private int m_maxScrambleCount = -1;
  // special abilities
  private int m_blockade = 0;
  // a colon delimited list of the units this unit can repair.
  // (units must be in same territory, unless this unit is land
  // and the repaired unit is sea)
  private IntegerMap<UnitType> m_repairsUnits = new IntegerMap<>();
  private IntegerMap<UnitType> m_givesMovement = new IntegerMap<>();
  private List<Tuple<String, PlayerID>> m_destroyedWhenCapturedBy = new ArrayList<>();
  // also an allowed setter is "setDestroyedWhenCapturedFrom" which will just create m_destroyedWhenCapturedBy with a
  // specific list
  private Map<Integer, Tuple<Boolean, UnitType>> m_whenHitPointsDamagedChangesInto = new HashMap<>();
  private Map<Integer, Tuple<Boolean, UnitType>> m_whenHitPointsRepairedChangesInto = new HashMap<>();
  private Map<String, Tuple<String, IntegerMap<UnitType>>> m_whenCapturedChangesInto = new LinkedHashMap<>();
  private int m_whenCapturedSustainsDamage = 0;
  private List<PlayerID> m_canBeCapturedOnEnteringBy = new ArrayList<>();
  private List<PlayerID> m_canBeGivenByTerritoryTo = new ArrayList<>();
  // a set of information for dealing with special abilities or
  // loss of abilities when a unit takes x-y amount of damage
  private List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> m_whenCombatDamaged = new ArrayList<>();
  // a kind of support attachment for giving actual unit
  // attachment abilities or other to a unit, when in the
  // precense or on the same route with another unit
  private List<String> m_receivesAbilityWhenWith = new ArrayList<>();
  // currently used for: placement in original territories only
  private Set<String> m_special = new HashSet<>();
  // Manually set TUV
  private int m_tuv = -1;

  /** Creates new UnitAttachment. */
  public UnitAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  private void setCanIntercept(final String value) {
    m_canIntercept = getBool(value);
  }

  private void setCanIntercept(final Boolean value) {
    m_canIntercept = value;
  }

  public boolean getCanIntercept() {
    return m_canIntercept;
  }

  private void resetCanIntercept() {
    m_canIntercept = false;
  }

  private void setCanEscort(final String value) {
    m_canEscort = getBool(value);
  }

  private void setCanEscort(final Boolean value) {
    m_canEscort = value;
  }

  public boolean getCanEscort() {
    return m_canEscort;
  }

  private void resetCanEscort() {
    m_canEscort = false;
  }

  private void setCanAirBattle(final String value) {
    m_canAirBattle = getBool(value);
  }

  private void setCanAirBattle(final Boolean value) {
    m_canAirBattle = value;
  }

  public boolean getCanAirBattle() {
    return m_canAirBattle;
  }

  private void resetCanAirBattle() {
    m_canAirBattle = false;
  }

  private void setAirDefense(final String value) {
    m_airDefense = getInt(value);
  }

  private void setAirDefense(final Integer value) {
    m_airDefense = value;
  }

  private int getAirDefense() {
    return m_airDefense;
  }

  public int getAirDefense(final PlayerID player) {
    return (Math.min(getData().getDiceSides(), Math.max(0,
        m_airDefense + TechAbilityAttachment.getAirDefenseBonus((UnitType) this.getAttachedTo(), player, getData()))));
  }

  private void resetAirDefense() {
    m_airDefense = 0;
  }

  private void setAirAttack(final String value) {
    m_airAttack = getInt(value);
  }

  private void setAirAttack(final Integer value) {
    m_airAttack = value;
  }

  private int getAirAttack() {
    return m_airAttack;
  }

  public int getAirAttack(final PlayerID player) {
    return (Math.min(getData().getDiceSides(), Math.max(0,
        m_airAttack + TechAbilityAttachment.getAirAttackBonus((UnitType) this.getAttachedTo(), player, getData()))));
  }

  private void resetAirAttack() {
    m_airAttack = 0;
  }

  private void setIsAirTransport(final String s) {
    m_isAirTransport = getBool(s);
  }

  private void setIsAirTransport(final Boolean s) {
    m_isAirTransport = s;
  }

  public boolean getIsAirTransport() {
    return m_isAirTransport;
  }

  private void resetIsAirTransport() {
    m_isAirTransport = false;
  }

  private void setIsAirTransportable(final String s) {
    m_isAirTransportable = getBool(s);
  }

  private void setIsAirTransportable(final Boolean s) {
    m_isAirTransportable = s;
  }

  public boolean getIsAirTransportable() {
    return m_isAirTransportable;
  }

  private void resetIsAirTransportable() {
    m_isAirTransportable = false;
  }

  private void setCanBeGivenByTerritoryTo(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_canBeGivenByTerritoryTo.add(tempPlayer);
      } else if (name.equalsIgnoreCase("true") || name.equalsIgnoreCase("false")) {
        m_canBeGivenByTerritoryTo.clear();
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setCanBeGivenByTerritoryTo(final List<PlayerID> value) {
    m_canBeGivenByTerritoryTo = value;
  }

  public List<PlayerID> getCanBeGivenByTerritoryTo() {
    return m_canBeGivenByTerritoryTo;
  }

  private void resetCanBeGivenByTerritoryTo() {
    m_canBeGivenByTerritoryTo = new ArrayList<>();
  }

  private void setCanBeCapturedOnEnteringBy(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_canBeCapturedOnEnteringBy.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setCanBeCapturedOnEnteringBy(final List<PlayerID> value) {
    m_canBeCapturedOnEnteringBy = value;
  }

  public List<PlayerID> getCanBeCapturedOnEnteringBy() {
    return m_canBeCapturedOnEnteringBy;
  }

  private void resetCanBeCapturedOnEnteringBy() {
    m_canBeCapturedOnEnteringBy = new ArrayList<>();
  }

  private void setWhenHitPointsDamagedChangesInto(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 3) {
      throw new GameParseException(
          "setWhenHitPointsDamagedChangesInto must have damage:translateAttributes:unitType " + thisErrorMsg());
    }
    final UnitType unitType = getData().getUnitTypeList().getUnitType(s[2]);
    if (unitType == null) {
      throw new GameParseException("setWhenHitPointsDamagedChangesInto: No unit type: " + s[2] + thisErrorMsg());
    }
    m_whenHitPointsDamagedChangesInto.put(getInt(s[0]), Tuple.of(getBool(s[1]), unitType));
  }

  private void setWhenHitPointsDamagedChangesInto(final Map<Integer, Tuple<Boolean, UnitType>> value) {
    m_whenHitPointsDamagedChangesInto = value;
  }

  /**
   * Can remove null check and this comment for next incompatible release.
   */
  public Map<Integer, Tuple<Boolean, UnitType>> getWhenHitPointsDamagedChangesInto() {
    if (m_whenHitPointsDamagedChangesInto == null) {
      resetWhenHitPointsDamagedChangesInto(); // TODO: Can remove for incompatible release
    }
    return m_whenHitPointsDamagedChangesInto;
  }

  private void resetWhenHitPointsDamagedChangesInto() {
    m_whenHitPointsDamagedChangesInto = new HashMap<>();
  }

  private void setWhenHitPointsRepairedChangesInto(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 3) {
      throw new GameParseException(
          "setWhenHitPointsRepairedChangesInto must have damage:translateAttributes:unitType " + thisErrorMsg());
    }
    final UnitType unitType = getData().getUnitTypeList().getUnitType(s[2]);
    if (unitType == null) {
      throw new GameParseException("setWhenHitPointsRepairedChangesInto: No unit type: " + s[2] + thisErrorMsg());
    }
    m_whenHitPointsRepairedChangesInto.put(getInt(s[0]), Tuple.of(getBool(s[1]), unitType));
  }

  private void setWhenHitPointsRepairedChangesInto(final Map<Integer, Tuple<Boolean, UnitType>> value) {
    m_whenHitPointsRepairedChangesInto = value;
  }

  /**
   * Can remove null check and this comment for next incompatible release.
   */
  public Map<Integer, Tuple<Boolean, UnitType>> getWhenHitPointsRepairedChangesInto() {
    if (m_whenHitPointsRepairedChangesInto == null) {
      resetWhenHitPointsRepairedChangesInto(); // TODO: Can remove for incompatible release
    }
    return m_whenHitPointsRepairedChangesInto;
  }

  private void resetWhenHitPointsRepairedChangesInto() {
    m_whenHitPointsRepairedChangesInto = new HashMap<>();
  }

  @VisibleForTesting
  void setWhenCapturedChangesInto(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 5 || s.length % 2 == 0) {
      throw new GameParseException("whenCapturedChangesInto must have 5 or more values, "
          + "playerFrom:playerTo:keepAttributes:unitType:howMany "
          + "(you may have additional unitType:howMany:unitType:howMany, etc" + thisErrorMsg());
    }
    final PlayerID pfrom = getData().getPlayerList().getPlayerId(s[0]);
    if (pfrom == null && !s[0].equals("any")) {
      throw new GameParseException("whenCapturedChangesInto: No player named: " + s[0] + thisErrorMsg());
    }
    final PlayerID pto = getData().getPlayerList().getPlayerId(s[1]);
    if (pto == null && !s[1].equals("any")) {
      throw new GameParseException("whenCapturedChangesInto: No player named: " + s[1] + thisErrorMsg());
    }
    getBool(s[2]);
    final IntegerMap<UnitType> unitsToMake = new IntegerMap<>();
    for (int i = 3; i < s.length; i += 2) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
      if (ut == null) {
        throw new GameParseException("whenCapturedChangesInto: No unit named: " + s[i] + thisErrorMsg());
      }
      unitsToMake.put(ut, getInt(s[i + 1]));
    }
    m_whenCapturedChangesInto.put(s[0] + ":" + s[1], Tuple.of(s[2], unitsToMake));
  }

  private void setWhenCapturedChangesInto(final Map<String, Tuple<String, IntegerMap<UnitType>>> value) {
    m_whenCapturedChangesInto = value;
  }

  public Map<String, Tuple<String, IntegerMap<UnitType>>> getWhenCapturedChangesInto() {
    return m_whenCapturedChangesInto;
  }

  private void resetWhenCapturedChangesInto() {
    m_whenCapturedChangesInto = new LinkedHashMap<>();
  }

  private void setWhenCapturedSustainsDamage(final int s) {
    m_whenCapturedSustainsDamage = s;
  }

  public int getWhenCapturedSustainsDamage() {
    return m_whenCapturedSustainsDamage;
  }

  private void setDestroyedWhenCapturedBy(final String initialValue) throws GameParseException {
    // We can prefix this value with "BY" or "FROM" to change the setting. If no setting, default to "BY" since this
    // this is called by destroyedWhenCapturedBy
    String value = initialValue;
    String byOrFrom = "BY";
    if (value.startsWith("BY:") && getData().getPlayerList().getPlayerId("BY") == null) {
      byOrFrom = "BY";
      value = value.replaceFirst("BY:", "");
    } else if (value.startsWith("FROM:") && getData().getPlayerList().getPlayerId("FROM") == null) {
      byOrFrom = "FROM";
      value = value.replaceFirst("FROM:", "");
    }
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_destroyedWhenCapturedBy.add(Tuple.of(byOrFrom, tempPlayer));
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setDestroyedWhenCapturedBy(final List<Tuple<String, PlayerID>> value) {
    m_destroyedWhenCapturedBy = value;
  }

  private void setDestroyedWhenCapturedFrom(final String initialValue) throws GameParseException {
    String value = initialValue;
    if (!(value.startsWith("BY:") || value.startsWith("FROM:"))) {
      value = "FROM:" + value;
    }
    setDestroyedWhenCapturedBy(value);
  }

  public List<Tuple<String, PlayerID>> getDestroyedWhenCapturedBy() {
    return m_destroyedWhenCapturedBy;
  }

  private void resetDestroyedWhenCapturedBy() {
    m_destroyedWhenCapturedBy = new ArrayList<>();
  }

  private void setCanBlitz(final String s) {
    m_canBlitz = getBool(s);
  }

  private void setCanBlitz(final Boolean s) {
    m_canBlitz = s;
  }

  private boolean getCanBlitz() {
    return m_canBlitz;
  }

  public boolean getCanBlitz(final PlayerID player) {
    return m_canBlitz
        || TechAbilityAttachment.getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BLITZ,
            (UnitType) this.getAttachedTo(), player, getData());
  }

  private void resetCanBlitz() {
    m_canBlitz = false;
  }

  private void setIsSub(final String s) {
    m_isSub = getBool(s);
  }

  private void setIsSub(final Boolean s) {
    m_isSub = s;
  }

  public boolean getIsSub() {
    return m_isSub;
  }

  private void resetIsSub() {
    m_isSub = false;
  }

  private void setIsCombatTransport(final String s) {
    m_isCombatTransport = getBool(s);
  }

  private void setIsCombatTransport(final Boolean s) {
    m_isCombatTransport = s;
  }

  public boolean getIsCombatTransport() {
    return m_isCombatTransport;
  }

  private void resetIsCombatTransport() {
    m_isCombatTransport = false;
  }

  private void setIsStrategicBomber(final String s) {
    m_isStrategicBomber = getBool(s);
  }

  private void setIsStrategicBomber(final Boolean s) {
    m_isStrategicBomber = s;
  }

  public boolean getIsStrategicBomber() {
    return m_isStrategicBomber;
  }

  private void resetIsStrategicBomber() {
    m_isStrategicBomber = false;
  }

  private void setIsDestroyer(final String s) {
    m_isDestroyer = getBool(s);
  }

  private void setIsDestroyer(final Boolean s) {
    m_isDestroyer = s;
  }

  public boolean getIsDestroyer() {
    return m_isDestroyer;
  }

  private void resetIsDestroyer() {
    m_isDestroyer = false;
  }

  public void setCanBombard(final String s) {
    m_canBombard = getBool(s);
  }

  private void setCanBombard(final Boolean s) {
    m_canBombard = s;
  }

  private boolean getCanBombard() {
    return m_canBombard;
  }

  public boolean getCanBombard(final PlayerID player) {
    return m_canBombard
        || TechAbilityAttachment.getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BOMBARD,
            (UnitType) this.getAttachedTo(), player, getData());
  }

  private void resetCanBombard() {
    m_canBombard = false;
  }

  private void setIsAir(final String s) {
    m_isAir = getBool(s);
  }

  private void setIsAir(final Boolean s) {
    m_isAir = s;
  }

  public boolean getIsAir() {
    return m_isAir;
  }

  private void resetIsAir() {
    m_isAir = false;
  }

  private void setIsSea(final String s) {
    m_isSea = getBool(s);
  }

  private void setIsSea(final Boolean s) {
    m_isSea = s;
  }

  public boolean getIsSea() {
    return m_isSea;
  }

  private void resetIsSea() {
    m_isSea = false;
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
    m_canProduceUnits = getBool(s);
  }

  private void setCanProduceUnits(final Boolean s) {
    m_canProduceUnits = s;
  }

  public boolean getCanProduceUnits() {
    return m_canProduceUnits;
  }

  private void resetCanProduceUnits() {
    m_canProduceUnits = false;
  }

  private void setCanProduceXUnits(final String s) {
    m_canProduceXUnits = getInt(s);
  }

  private void setCanProduceXUnits(final Integer s) {
    m_canProduceXUnits = s;
  }

  public int getCanProduceXUnits() {
    return m_canProduceXUnits;
  }

  private void resetCanProduceXUnits() {
    m_canProduceXUnits = -1;
  }

  private void setCanOnlyBePlacedInTerritoryValuedAtX(final String s) {
    m_canOnlyBePlacedInTerritoryValuedAtX = getInt(s);
  }

  private void setCanOnlyBePlacedInTerritoryValuedAtX(final Integer s) {
    m_canOnlyBePlacedInTerritoryValuedAtX = s;
  }

  public int getCanOnlyBePlacedInTerritoryValuedAtX() {
    return m_canOnlyBePlacedInTerritoryValuedAtX;
  }

  private void resetCanOnlyBePlacedInTerritoryValuedAtX() {
    m_canOnlyBePlacedInTerritoryValuedAtX = -1;
  }

  private void setUnitPlacementRestrictions(final String value) {
    if (value == null) {
      m_unitPlacementRestrictions = null;
      return;
    }
    m_unitPlacementRestrictions = splitOnColon(value);
  }

  private void setUnitPlacementRestrictions(final String[] value) {
    m_unitPlacementRestrictions = value;
  }

  public String[] getUnitPlacementRestrictions() {
    return m_unitPlacementRestrictions;
  }

  private void resetUnitPlacementRestrictions() {
    m_unitPlacementRestrictions = null;
  }

  // no m_ variable for this, since it is the inverse of m_unitPlacementRestrictions
  // we might as well just use m_unitPlacementRestrictions
  private void setUnitPlacementOnlyAllowedIn(final String value) throws GameParseException {
    final Collection<Territory> allowedTerritories = getListedTerritories(splitOnColon(value));
    final Collection<Territory> restrictedTerritories = new HashSet<>(getData().getMap().getTerritories());
    restrictedTerritories.removeAll(allowedTerritories);
    m_unitPlacementRestrictions = restrictedTerritories.stream()
        .map(Territory::getName)
        .toArray(String[]::new);
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
      m_repairsUnits.put(ut, amount);
    }
  }

  private void setRepairsUnits(final IntegerMap<UnitType> value) {
    m_repairsUnits = value;
  }

  public IntegerMap<UnitType> getRepairsUnits() {
    return m_repairsUnits;
  }

  private void resetRepairsUnits() {
    m_repairsUnits = new IntegerMap<>();
  }

  private void setSpecial(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    for (final String option : s) {
      if (!(option.equals("none") || option.equals("canOnlyPlaceInOriginalTerritories"))) {
        throw new GameParseException("special does not allow: " + option + thisErrorMsg());
      }
      m_special.add(option);
    }
  }

  private void setSpecial(final Set<String> value) {
    m_special = value;
  }

  public Set<String> getSpecial() {
    return m_special;
  }

  private void resetSpecial() {
    m_special = new HashSet<>();
  }

  private void setCanInvadeOnlyFrom(final String value) {
    if (value == null) {
      m_canInvadeOnlyFrom = null;
      return;
    }
    final String[] canOnlyInvadeFrom = splitOnColon(value);
    if (canOnlyInvadeFrom[0].toLowerCase().equals("none")) {
      m_canInvadeOnlyFrom = new String[] {"none"};
      return;
    }
    if (canOnlyInvadeFrom[0].toLowerCase().equals("all")) {
      m_canInvadeOnlyFrom = new String[] {"all"};
      return;
    }
    m_canInvadeOnlyFrom = canOnlyInvadeFrom;
  }

  private void setCanInvadeOnlyFrom(final String[] value) {
    m_canInvadeOnlyFrom = value;
  }

  private String[] getCanInvadeOnlyFrom() {
    return m_canInvadeOnlyFrom;
  }

  public boolean canInvadeFrom(final Unit transport) {
    return m_canInvadeOnlyFrom == null
        || Arrays.asList(m_canInvadeOnlyFrom).isEmpty()
        || m_canInvadeOnlyFrom[0].isEmpty()
        || m_canInvadeOnlyFrom[0].equals("all")
        || Arrays.asList(m_canInvadeOnlyFrom).contains(transport.getType().getName());
  }

  private void resetCanInvadeOnlyFrom() {
    m_canInvadeOnlyFrom = null;
  }

  private void setRequiresUnits(final String value) {
    m_requiresUnits.add(splitOnColon(value));
  }

  private void setRequiresUnits(final List<String[]> value) {
    m_requiresUnits = value;
  }

  public List<String[]> getRequiresUnits() {
    return m_requiresUnits;
  }

  private void resetRequiresUnits() {
    m_requiresUnits = new ArrayList<>();
  }

  private void setRequiresUnitsToMove(final String value) throws GameParseException {
    final String[] array = splitOnColon(value);
    if (array.length == 0) {
      throw new GameParseException("requiresUnitsToMove must have at least 1 unit type" + thisErrorMsg());
    }
    for (final String s : array) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(s);
      if (ut == null) {
        throw new GameParseException("No unit called:" + s + thisErrorMsg());
      }
    }
    m_requiresUnitsToMove.add(array);
  }

  private void setRequiresUnitsToMove(final List<String[]> value) {
    m_requiresUnitsToMove = value;
  }

  public List<String[]> getRequiresUnitsToMove() {
    return m_requiresUnitsToMove;
  }

  private void resetRequiresUnitsToMove() {
    m_requiresUnitsToMove = new ArrayList<>();
  }

  private void setWhenCombatDamaged(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (!(s.length == 3 || s.length == 4)) {
      throw new GameParseException(
          "whenCombatDamaged must have 3 or 4 parts: value=effect:optionalNumber, count=integer:integer"
              + thisErrorMsg());
    }
    final int from = getInt(s[0]);
    final int to = getInt(s[1]);
    if (from < 0 || to < 0 || to < from) {
      throw new GameParseException("whenCombatDamaged damaged integers must be positive, and the second integer must "
          + "be equal to or greater than the first" + thisErrorMsg());
    }
    final Tuple<Integer, Integer> fromTo = Tuple.of(from, to);
    final Tuple<String, String> effectNum;
    if (s.length == 3) {
      effectNum = Tuple.of(s[2], null);
    } else {
      effectNum = Tuple.of(s[2], s[3]);
    }
    m_whenCombatDamaged.add(Tuple.of(fromTo, effectNum));
  }

  private void setWhenCombatDamaged(final List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> value) {
    m_whenCombatDamaged = value;
  }

  public List<Tuple<Tuple<Integer, Integer>, Tuple<String, String>>> getWhenCombatDamaged() {
    return m_whenCombatDamaged;
  }

  private void resetWhenCombatDamaged() {
    m_whenCombatDamaged = new ArrayList<>();
  }

  private void setReceivesAbilityWhenWith(final String value) {
    m_receivesAbilityWhenWith.add(value);
  }

  private void setReceivesAbilityWhenWith(final List<String> value) {
    m_receivesAbilityWhenWith = value;
  }

  public List<String> getReceivesAbilityWhenWith() {
    return m_receivesAbilityWhenWith;
  }

  private void resetReceivesAbilityWhenWith() {
    m_receivesAbilityWhenWith = new ArrayList<>();
  }

  private static IntegerMap<Tuple<String, String>> getReceivesAbilityWhenWithMap(final Collection<Unit> units,
      final String filterForAbility, final GameData data) {
    final IntegerMap<Tuple<String, String>> map = new IntegerMap<>();
    final Collection<UnitType> canReceive =
        getUnitTypesFromUnitList(CollectionUtils.getMatches(units, Matches.unitCanReceiveAbilityWhenWith()));
    for (final UnitType ut : canReceive) {
      final Collection<String> receives = UnitAttachment.get(ut).getReceivesAbilityWhenWith();
      for (final String receive : receives) {
        final String[] s = splitOnColon(receive);
        if (filterForAbility != null && !filterForAbility.equals(s[0])) {
          continue;
        }
        map.put(Tuple.of(s[0], s[1]),
            CollectionUtils.countMatches(units, Matches.unitIsOfType(data.getUnitTypeList().getUnitType(s[1]))));
      }
    }
    return map;
  }

  public static Collection<Unit> getUnitsWhichReceivesAbilityWhenWith(final Collection<Unit> units,
      final String filterForAbility, final GameData data) {
    if (units.stream().noneMatch(Matches.unitCanReceiveAbilityWhenWith())) {
      return new ArrayList<>();
    }
    final Collection<Unit> unitsCopy = new ArrayList<>(units);
    final HashSet<Unit> whichReceiveNoDuplicates = new HashSet<>();
    final IntegerMap<Tuple<String, String>> whichGive =
        getReceivesAbilityWhenWithMap(unitsCopy, filterForAbility, data);
    for (final Tuple<String, String> abilityUnitType : whichGive.keySet()) {
      final Collection<Unit> receives = CollectionUtils.getNMatches(unitsCopy, whichGive.getInt(abilityUnitType),
          Matches.unitCanReceiveAbilityWhenWith(filterForAbility, abilityUnitType.getSecond()));
      whichReceiveNoDuplicates.addAll(receives);
      unitsCopy.removeAll(receives);
    }
    return whichReceiveNoDuplicates;
  }

  private void setIsConstruction(final String s) {
    m_isConstruction = getBool(s);
  }

  private void setIsConstruction(final Boolean s) {
    m_isConstruction = s;
  }

  public boolean getIsConstruction() {
    return m_isConstruction;
  }

  private void resetIsConstruction() {
    m_isConstruction = false;
  }

  private void setConstructionType(final String s) {
    m_constructionType = s;
  }

  public String getConstructionType() {
    return m_constructionType;
  }

  private void resetConstructionType() {
    m_constructionType = "none";
  }

  private void setConstructionsPerTerrPerTypePerTurn(final String s) {
    m_constructionsPerTerrPerTypePerTurn = getInt(s);
  }

  private void setConstructionsPerTerrPerTypePerTurn(final Integer s) {
    m_constructionsPerTerrPerTypePerTurn = s;
  }

  public int getConstructionsPerTerrPerTypePerTurn() {
    return m_constructionsPerTerrPerTypePerTurn;
  }

  private void resetConstructionsPerTerrPerTypePerTurn() {
    m_constructionsPerTerrPerTypePerTurn = -1;
  }

  private void setMaxConstructionsPerTypePerTerr(final String s) {
    m_maxConstructionsPerTypePerTerr = getInt(s);
  }

  private void setMaxConstructionsPerTypePerTerr(final Integer s) {
    m_maxConstructionsPerTypePerTerr = s;
  }

  public int getMaxConstructionsPerTypePerTerr() {
    return m_maxConstructionsPerTypePerTerr;
  }

  private void resetMaxConstructionsPerTypePerTerr() {
    m_maxConstructionsPerTypePerTerr = -1;
  }

  private void setIsMarine(final String s) {
    if (s.equalsIgnoreCase(Constants.PROPERTY_TRUE)) {
      m_isMarine = 1;
    } else if (s.equalsIgnoreCase(Constants.PROPERTY_FALSE)) {
      m_isMarine = 0;
    } else {
      m_isMarine = getInt(s);
    }
  }

  private void setIsMarine(final Integer s) {
    m_isMarine = s;
  }

  public int getIsMarine() {
    return m_isMarine;
  }

  private void resetIsMarine() {
    m_isMarine = 0;
  }

  @Deprecated
  private void setIsInfantry(final String s) {
    m_isInfantry = getBool(s);
  }

  @Deprecated
  private void setIsInfantry(final Boolean s) {
    m_isInfantry = s;
  }

  @Deprecated
  public boolean getIsInfantry() {
    return m_isInfantry;
  }

  @Deprecated
  private void resetIsInfantry() {
    m_isInfantry = false;
  }

  private void setIsLandTransportable(final String s) {
    m_isLandTransportable = getBool(s);
  }

  private void setIsLandTransportable(final Boolean s) {
    m_isLandTransportable = s;
  }

  public boolean getIsLandTransportable() {
    return m_isLandTransportable;
  }

  private void resetIsLandTransportable() {
    m_isLandTransportable = false;
  }

  private void setIsLandTransport(final String s) {
    m_isLandTransport = getBool(s);
  }

  private void setIsLandTransport(final Boolean s) {
    m_isLandTransport = s;
  }

  public boolean isLandTransport() {
    return m_isLandTransport;
  }

  public boolean getIsLandTransport() {
    return m_isLandTransport;
  }

  private void resetIsLandTransport() {
    m_isLandTransport = false;
  }

  private void setTransportCapacity(final int s) {
    m_transportCapacity = s;
  }

  public int getTransportCapacity() {
    return m_transportCapacity;
  }

  private void setIsTwoHit(final String s) {
    setIsTwoHit(getBool(s));
  }

  private void setIsTwoHit(final boolean s) {
    m_hitPoints = s ? 2 : 1;
  }

  private void setHitPoints(final int value) {
    m_hitPoints = value;
  }

  public int getHitPoints() {
    return m_hitPoints;
  }

  private void setTransportCost(final Integer s) {
    m_transportCost = s;
  }

  public int getTransportCost() {
    return m_transportCost;
  }

  private void setMaxBuiltPerPlayer(final String s) {
    m_maxBuiltPerPlayer = getInt(s);
  }

  private void setMaxBuiltPerPlayer(final Integer s) {
    m_maxBuiltPerPlayer = s;
  }

  public int getMaxBuiltPerPlayer() {
    return m_maxBuiltPerPlayer;
  }

  private void resetMaxBuiltPerPlayer() {
    m_maxBuiltPerPlayer = -1;
  }

  private void setCarrierCapacity(final String s) {
    m_carrierCapacity = getInt(s);
  }

  private void setCarrierCapacity(final Integer s) {
    m_carrierCapacity = s;
  }

  public int getCarrierCapacity() {
    return m_carrierCapacity;
  }

  private void resetCarrierCapacity() {
    m_carrierCapacity = -1;
  }

  private void setCarrierCost(final String s) {
    m_carrierCost = getInt(s);
  }

  private void setCarrierCost(final Integer s) {
    m_carrierCost = s;
  }

  public int getCarrierCost() {
    return m_carrierCost;
  }

  private void resetCarrierCost() {
    m_carrierCost = -1;
  }

  private void setArtillery(final String s) throws GameParseException {
    m_artillery = getBool(s);
    if (m_artillery) {
      UnitSupportAttachment.addRule((UnitType) getAttachedTo(), getData(), false);
    }
  }

  private void setArtillery(final Boolean s) throws GameParseException {
    m_artillery = s;
    if (m_artillery) {
      UnitSupportAttachment.addRule((UnitType) getAttachedTo(), getData(), false);
    }
  }

  public boolean getArtillery() {
    return m_artillery;
  }

  private void resetArtillery() {
    throw new IllegalStateException(
        "Resetting Artillery (UnitAttachment) is not allowed, please use Support Attachments instead.");
  }

  private void setArtillerySupportable(final String s) throws GameParseException {
    m_artillerySupportable = getBool(s);
    if (m_artillerySupportable) {
      UnitSupportAttachment.addTarget((UnitType) getAttachedTo(), getData());
    }
  }

  private void setArtillerySupportable(final Boolean s) throws GameParseException {
    m_artillerySupportable = s;
    if (m_artillerySupportable) {
      UnitSupportAttachment.addTarget((UnitType) getAttachedTo(), getData());
    }
  }

  public boolean getArtillerySupportable() {
    return m_artillerySupportable;
  }

  private void resetArtillerySupportable() {
    throw new IllegalStateException(
        "Resetting Artillery Supportable (UnitAttachment) is not allowed, please use Support Attachments instead.");
  }

  public void setUnitSupportCount(final String s) {
    m_unitSupportCount = getInt(s);
    UnitSupportAttachment.setOldSupportCount((UnitType) getAttachedTo(), getData(), s);
  }

  private void setUnitSupportCount(final Integer s) {
    m_unitSupportCount = s;
    UnitSupportAttachment.setOldSupportCount((UnitType) getAttachedTo(), getData(), s.toString());
  }

  private int getUnitSupportCount() {
    return m_unitSupportCount > 0 ? m_unitSupportCount : 1;
  }

  private void resetUnitSupportCount() {
    throw new IllegalStateException(
        "Resetting Artillery Support Count (UnitAttachment) is not allowed, please use Support Attachments instead.");
  }

  @VisibleForTesting
  public void setBombard(final int s) {
    m_bombard = s;
  }

  public int getBombard() {
    return m_bombard > 0 ? m_bombard : m_attack;
  }

  private void setMovement(final String s) {
    m_movement = getInt(s);
  }

  private void setMovement(final Integer s) {
    m_movement = s;
  }

  private int getMovement() {
    return m_movement;
  }

  public int getMovement(final PlayerID player) {
    return Math.max(0,
        m_movement + TechAbilityAttachment.getMovementBonus((UnitType) this.getAttachedTo(), player, getData()));
  }

  private void resetMovement() {
    m_movement = 0;
  }

  private void setAttack(final String s) {
    m_attack = getInt(s);
  }

  private void setAttack(final Integer s) {
    m_attack = s;
  }

  int getAttack() {
    return m_attack;
  }

  public int getAttack(final PlayerID player) {
    final int attackValue =
        m_attack + TechAbilityAttachment.getAttackBonus((UnitType) this.getAttachedTo(), player, getData());
    return Math.min(getData().getDiceSides(), Math.max(0, attackValue));
  }

  private void resetAttack() {
    m_attack = 0;
  }

  private void setAttackRolls(final String s) {
    m_attackRolls = getInt(s);
  }

  private void setAttackRolls(final Integer s) {
    m_attackRolls = s;
  }

  private int getAttackRolls() {
    return m_attackRolls;
  }

  public int getAttackRolls(final PlayerID player) {
    return Math.max(0,
        m_attackRolls + TechAbilityAttachment.getAttackRollsBonus((UnitType) this.getAttachedTo(), player, getData()));
  }

  private void resetAttackRolls() {
    m_attackRolls = 1;
  }

  private void setDefense(final String s) {
    m_defense = getInt(s);
  }

  private void setDefense(final Integer s) {
    m_defense = s;
  }

  private int getDefense() {
    return m_defense;
  }

  public int getDefense(final PlayerID player) {
    int defenseValue =
        m_defense + TechAbilityAttachment.getDefenseBonus((UnitType) this.getAttachedTo(), player, getData());
    if (defenseValue > 0 && m_isSub && TechTracker.hasSuperSubs(player)) {
      final int bonus = Properties.getSuperSubDefenseBonus(getData());
      defenseValue += bonus;
    }
    return Math.min(getData().getDiceSides(), Math.max(0, defenseValue));
  }

  private void resetDefense() {
    m_defense = 0;
  }

  private void setDefenseRolls(final String s) {
    m_defenseRolls = getInt(s);
  }

  private void setDefenseRolls(final Integer s) {
    m_defenseRolls = s;
  }

  private int getDefenseRolls() {
    return m_defenseRolls;
  }

  public int getDefenseRolls(final PlayerID player) {
    return Math.max(0, m_defenseRolls
        + TechAbilityAttachment.getDefenseRollsBonus((UnitType) this.getAttachedTo(), player, getData()));
  }

  private void resetDefenseRolls() {
    m_defenseRolls = 1;
  }

  private void setChooseBestRoll(final String s) {
    m_chooseBestRoll = getBool(s);
  }

  private void setChooseBestRoll(final Boolean s) {
    m_chooseBestRoll = s;
  }

  public boolean getChooseBestRoll() {
    return m_chooseBestRoll;
  }

  private void resetChooseBestRoll() {
    m_chooseBestRoll = false;
  }

  private void setCanScramble(final String s) {
    m_canScramble = getBool(s);
  }

  private void setCanScramble(final Boolean s) {
    m_canScramble = s;
  }

  public boolean getCanScramble() {
    return m_canScramble;
  }

  private void resetCanScramble() {
    m_canScramble = false;
  }

  private void setMaxScrambleCount(final String s) {
    m_maxScrambleCount = getInt(s);
  }

  private void setMaxScrambleCount(final Integer s) {
    m_maxScrambleCount = s;
  }

  public int getMaxScrambleCount() {
    return m_maxScrambleCount;
  }

  private void resetMaxScrambleCount() {
    m_maxScrambleCount = -1;
  }

  private void setMaxScrambleDistance(final String s) {
    m_maxScrambleDistance = getInt(s);
  }

  private void setMaxScrambleDistance(final Integer s) {
    m_maxScrambleDistance = s;
  }

  public int getMaxScrambleDistance() {
    return m_maxScrambleDistance;
  }

  private void resetMaxScrambleDistance() {
    m_maxScrambleDistance = -1;
  }

  private void setMaxOperationalDamage(final String s) {
    m_maxOperationalDamage = getInt(s);
  }

  private void setMaxOperationalDamage(final Integer s) {
    m_maxOperationalDamage = s;
  }

  public int getMaxOperationalDamage() {
    return m_maxOperationalDamage;
  }

  private void resetMaxOperationalDamage() {
    m_maxOperationalDamage = -1;
  }

  private void setMaxDamage(final String s) {
    m_maxDamage = getInt(s);
  }

  private void setMaxDamage(final Integer s) {
    m_maxDamage = s;
  }

  public int getMaxDamage() {
    return m_maxDamage;
  }

  private void resetMaxDamage() {
    m_maxDamage = 2;
  }

  private void setIsAirBase(final String s) {
    m_isAirBase = getBool(s);
  }

  private void setIsAirBase(final Boolean s) {
    m_isAirBase = s;
  }

  public boolean getIsAirBase() {
    return m_isAirBase;
  }

  private void resetIsAirBase() {
    m_isAirBase = false;
  }

  private void setIsInfrastructure(final String s) {
    m_isInfrastructure = getBool(s);
  }

  private void setIsInfrastructure(final Boolean s) {
    m_isInfrastructure = s;
  }

  public boolean getIsInfrastructure() {
    return m_isInfrastructure;
  }

  private void resetIsInfrastructure() {
    m_isInfrastructure = false;
  }

  private void setCanBeDamaged(final String s) {
    m_canBeDamaged = getBool(s);
  }

  private void setCanBeDamaged(final Boolean s) {
    m_canBeDamaged = s;
  }

  public boolean getCanBeDamaged() {
    return m_canBeDamaged;
  }

  private void resetCanBeDamaged() {
    m_canBeDamaged = false;
  }

  private void setCanDieFromReachingMaxDamage(final String s) {
    m_canDieFromReachingMaxDamage = getBool(s);
  }

  private void setCanDieFromReachingMaxDamage(final Boolean s) {
    m_canDieFromReachingMaxDamage = s;
  }

  public boolean getCanDieFromReachingMaxDamage() {
    return m_canDieFromReachingMaxDamage;
  }

  private void resetCanDieFromReachingMaxDamage() {
    m_canDieFromReachingMaxDamage = false;
  }

  private void setIsSuicide(final String s) {
    m_isSuicide = getBool(s);
  }

  private void setIsSuicide(final Boolean s) {
    m_isSuicide = s;
  }

  public boolean getIsSuicide() {
    return m_isSuicide;
  }

  private void resetIsSuicide() {
    m_isSuicide = false;
  }

  private void setIsSuicideOnHit(final String s) {
    m_isSuicideOnHit = getBool(s);
  }

  private void setIsSuicideOnHit(final Boolean s) {
    m_isSuicideOnHit = s;
  }

  public boolean getIsSuicideOnHit() {
    return m_isSuicideOnHit;
  }

  private void resetIsSuicideOnHit() {
    m_isSuicideOnHit = false;
  }

  private void setIsKamikaze(final String s) {
    m_isKamikaze = getBool(s);
  }

  private void setIsKamikaze(final Boolean s) {
    m_isKamikaze = s;
  }

  public boolean getIsKamikaze() {
    return m_isKamikaze;
  }

  private void resetIsKamikaze() {
    m_isKamikaze = false;
  }

  private void setBlockade(final String s) {
    m_blockade = getInt(s);
  }

  private void setBlockade(final Integer s) {
    m_blockade = s;
  }

  public int getBlockade() {
    return m_blockade;
  }

  private void resetBlockade() {
    m_blockade = 0;
  }

  private void setGivesMovement(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length <= 0 || s.length > 2) {
      throw new GameParseException("givesMovement cannot be empty or have more than two fields" + thisErrorMsg());
    }
    final String unitTypeToProduce = s[1];
    // validate that this unit exists in the xml
    final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
    if (ut == null) {
      throw new GameParseException("No unit called:" + unitTypeToProduce + thisErrorMsg());
    }
    // we should allow positive and negative numbers, since you can give bonuses to units or take away a unit's movement
    final int n = getInt(s[0]);
    m_givesMovement.put(ut, n);
  }

  private void setGivesMovement(final IntegerMap<UnitType> value) {
    m_givesMovement = value;
  }

  public IntegerMap<UnitType> getGivesMovement() {
    return m_givesMovement;
  }

  private void resetGivesMovement() {
    m_givesMovement = new IntegerMap<>();
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
    m_consumesUnits.put(ut, n);
  }

  private void setConsumesUnits(final IntegerMap<UnitType> value) {
    m_consumesUnits = value;
  }

  public IntegerMap<UnitType> getConsumesUnits() {
    return m_consumesUnits;
  }

  private void resetConsumesUnits() {
    m_consumesUnits = new IntegerMap<>();
  }

  private void setCreatesUnitsList(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length <= 0 || s.length > 2) {
      throw new GameParseException("createsUnitsList cannot be empty or have more than two fields" + thisErrorMsg());
    }
    final String unitTypeToProduce = s[1];
    // validate that this unit exists in the xml
    final UnitType ut = getData().getUnitTypeList().getUnitType(unitTypeToProduce);
    if (ut == null) {
      throw new GameParseException("createsUnitsList: No unit called:" + unitTypeToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 1) {
      throw new GameParseException("createsUnitsList must have positive values" + thisErrorMsg());
    }
    m_createsUnitsList.put(ut, n);
  }

  private void setCreatesUnitsList(final IntegerMap<UnitType> value) {
    m_createsUnitsList = value;
  }

  public IntegerMap<UnitType> getCreatesUnitsList() {
    return m_createsUnitsList;
  }

  private void resetCreatesUnitsList() {
    m_createsUnitsList = new IntegerMap<>();
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
      throw new GameParseException("createsResourcesList: No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    m_createsResourcesList.put(r, n);
  }

  private void setCreatesResourcesList(final IntegerMap<Resource> value) {
    m_createsResourcesList = value;
  }

  public IntegerMap<Resource> getCreatesResourcesList() {
    return m_createsResourcesList;
  }

  private void resetCreatesResourcesList() {
    m_createsResourcesList = new IntegerMap<>();
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
      throw new GameParseException("fuelCost: No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 0) {
      throw new GameParseException("fuelCost must have positive values" + thisErrorMsg());
    }
    m_fuelCost.put(r, n);
  }

  private void setFuelCost(final IntegerMap<Resource> value) {
    m_fuelCost = value;
  }

  public IntegerMap<Resource> getFuelCost() {
    return m_fuelCost;
  }

  private void resetFuelCost() {
    m_fuelCost = new IntegerMap<>();
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
      throw new GameParseException("fuelFlatCost: No resource called:" + resourceToProduce + thisErrorMsg());
    }
    final int n = getInt(s[0]);
    if (n < 0) {
      throw new GameParseException("fuelFlatCost must have positive values" + thisErrorMsg());
    }
    m_fuelFlatCost.put(r, n);
  }

  private void setFuelFlatCost(final IntegerMap<Resource> value) {
    m_fuelFlatCost = value;
  }

  public IntegerMap<Resource> getFuelFlatCost() {
    // TODO: remove for incompatible release
    if (m_fuelFlatCost == null) {
      m_fuelFlatCost = new IntegerMap<>();
    }
    return m_fuelFlatCost;
  }

  private void resetFuelFlatCost() {
    m_fuelFlatCost = new IntegerMap<>();
  }

  private void setBombingBonus(final String s) {
    m_bombingBonus = getInt(s);
  }

  private void setBombingBonus(final Integer s) {
    m_bombingBonus = s;
  }

  public int getBombingBonus() {
    return m_bombingBonus;
  }

  private void resetBombingBonus() {
    m_bombingBonus = -1;
  }

  private void setBombingMaxDieSides(final String s) {
    m_bombingMaxDieSides = getInt(s);
  }

  private void setBombingMaxDieSides(final Integer s) {
    m_bombingMaxDieSides = s;
  }

  public int getBombingMaxDieSides() {
    return m_bombingMaxDieSides;
  }

  private void resetBombingMaxDieSides() {
    m_bombingMaxDieSides = -1;
  }

  private void setBombingTargets(final String value) throws GameParseException {
    if (value == null) {
      m_bombingTargets = null;
      return;
    }
    if (m_bombingTargets == null) {
      m_bombingTargets = new HashSet<>();
    }
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("bombingTargets: no such unit type: " + u + thisErrorMsg());
      }
      m_bombingTargets.add(ut);
    }
  }

  private void setBombingTargets(final Set<UnitType> value) {
    m_bombingTargets = value;
  }

  private Set<UnitType> getBombingTargets() {
    return m_bombingTargets;
  }

  public Set<UnitType> getBombingTargets(final GameData data) {
    if (m_bombingTargets != null) {
      return m_bombingTargets;
    }
    return new HashSet<>(data.getUnitTypeList().getAllUnitTypes());
  }

  private void resetBombingTargets() {
    m_bombingTargets = null;
  }

  public static Set<UnitType> getAllowedBombingTargetsIntersection(final Collection<Unit> bombersOrRockets,
      final GameData data) {
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
    m_attackAA = getInt(s);
  }

  private void setAttackAa(final Integer s) {
    m_attackAA = s;
  }

  private int getAttackAa() {
    return m_attackAA;
  }

  public int getAttackAa(final PlayerID player) {
    // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something other than 6, or it
    // does not divide
    // perfectly into attackAAmaxDieSides
    return Math.max(0, Math.min(getAttackAaMaxDieSides(),
        m_attackAA + TechAbilityAttachment.getRadarBonus((UnitType) this.getAttachedTo(), player, getData())));
  }

  private void resetAttackAa() {
    m_attackAA = 1;
  }

  private void setOffensiveAttackAa(final String s) {
    m_offensiveAttackAA = getInt(s);
  }

  private void setOffensiveAttackAa(final Integer s) {
    m_offensiveAttackAA = s;
  }

  private int getOffensiveAttackAa() {
    return m_offensiveAttackAA;
  }

  public int getOffensiveAttackAa(final PlayerID player) {
    // TODO: this may cause major problems with Low Luck, if they have diceSides equal to something other than 6, or it
    // does not divide
    // perfectly into attackAAmaxDieSides
    return Math.max(0, Math.min(getOffensiveAttackAaMaxDieSides(),
        m_offensiveAttackAA + TechAbilityAttachment.getRadarBonus((UnitType) this.getAttachedTo(), player, getData())));
  }

  private void resetOffensiveAttackAa() {
    m_offensiveAttackAA = 1;
  }

  private void setAttackAaMaxDieSides(final String s) {
    m_attackAAmaxDieSides = getInt(s);
  }

  private void setAttackAaMaxDieSides(final Integer s) {
    m_attackAAmaxDieSides = s;
  }

  public int getAttackAaMaxDieSides() {
    if (m_attackAAmaxDieSides < 0) {
      return getData().getDiceSides();
    }
    return m_attackAAmaxDieSides;
  }

  private void resetAttackAaMaxDieSides() {
    m_attackAAmaxDieSides = -1;
  }

  private void setOffensiveAttackAaMaxDieSides(final String s) {
    m_offensiveAttackAAmaxDieSides = getInt(s);
  }

  private void setOffensiveAttackAaMaxDieSides(final Integer s) {
    m_offensiveAttackAAmaxDieSides = s;
  }

  public int getOffensiveAttackAaMaxDieSides() {
    if (m_offensiveAttackAAmaxDieSides < 0) {
      return getData().getDiceSides();
    }
    return m_offensiveAttackAAmaxDieSides;
  }

  private void resetOffensiveAttackAaMaxDieSides() {
    m_offensiveAttackAAmaxDieSides = -1;
  }

  private void setMaxAaAttacks(final String s) throws GameParseException {
    final int attacks = getInt(s);
    if (attacks < -1) {
      throw new GameParseException("maxAAattacks must be positive (or -1 for attacking all) " + thisErrorMsg());
    }
    m_maxAAattacks = getInt(s);
  }

  private void setMaxAaAttacks(final Integer s) {
    m_maxAAattacks = s;
  }

  public int getMaxAaAttacks() {
    return m_maxAAattacks;
  }

  private void resetMaxAaAttacks() {
    m_maxAAattacks = -1;
  }

  private void setMaxRoundsAa(final String s) throws GameParseException {
    final int attacks = getInt(s);
    if (attacks < -1) {
      throw new GameParseException("maxRoundsAA must be positive (or -1 for infinite) " + thisErrorMsg());
    }
    m_maxRoundsAA = getInt(s);
  }

  private void setMaxRoundsAa(final Integer s) {
    m_maxRoundsAA = s;
  }

  public int getMaxRoundsAa() {
    return m_maxRoundsAA;
  }

  private void resetMaxRoundsAa() {
    m_maxRoundsAA = 1;
  }

  private void setMayOverStackAa(final String s) {
    m_mayOverStackAA = getBool(s);
  }

  private void setMayOverStackAa(final Boolean s) {
    m_mayOverStackAA = s;
  }

  public boolean getMayOverStackAa() {
    return m_mayOverStackAA;
  }

  private void resetMayOverStackAa() {
    m_mayOverStackAA = false;
  }

  private void setDamageableAa(final String s) {
    m_damageableAA = getBool(s);
  }

  private void setDamageableAa(final Boolean s) {
    m_damageableAA = s;
  }

  public boolean getDamageableAa() {
    return m_damageableAA;
  }

  private void resetDamageableAa() {
    m_damageableAA = false;
  }

  private void setIsAaForCombatOnly(final String s) {
    m_isAAforCombatOnly = getBool(s);
  }

  private void setIsAaForCombatOnly(final Boolean s) {
    m_isAAforCombatOnly = s;
  }

  public boolean getIsAaForCombatOnly() {
    return m_isAAforCombatOnly;
  }

  private void resetIsAaForCombatOnly() {
    m_isAAforCombatOnly = false;
  }

  private void setIsAaForBombingThisUnitOnly(final String s) {
    m_isAAforBombingThisUnitOnly = getBool(s);
  }

  private void setIsAaForBombingThisUnitOnly(final Boolean s) {
    m_isAAforBombingThisUnitOnly = s;
  }

  public boolean getIsAaForBombingThisUnitOnly() {
    return m_isAAforBombingThisUnitOnly;
  }

  private void resetIsAaForBombingThisUnitOnly() {
    m_isAAforBombingThisUnitOnly = false;
  }

  private void setIsAaForFlyOverOnly(final String s) {
    m_isAAforFlyOverOnly = getBool(s);
  }

  private void setIsAaForFlyOverOnly(final Boolean s) {
    m_isAAforFlyOverOnly = s;
  }

  public boolean getIsAaForFlyOverOnly() {
    return m_isAAforFlyOverOnly;
  }

  private void resetIsAaForFlyOverOnly() {
    m_isAAforFlyOverOnly = false;
  }

  private void setIsRocket(final String s) {
    m_isRocket = getBool(s);
  }

  private void setIsRocket(final Boolean s) {
    m_isRocket = s;
  }

  public boolean getIsRocket() {
    return m_isRocket;
  }

  private void resetIsRocket() {
    m_isRocket = false;
  }

  private void setTypeAa(final String s) {
    m_typeAA = s;
  }

  public String getTypeAa() {
    return m_typeAA;
  }

  private void resetTypeAa() {
    m_typeAA = "AA";
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
      m_targetsAA = null;
      return;
    }
    if (m_targetsAA == null) {
      m_targetsAA = new HashSet<>();
    }
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("AAtargets: no such unit type: " + u + thisErrorMsg());
      }
      m_targetsAA.add(ut);
    }
  }

  private void setTargetsAa(final Set<UnitType> value) {
    m_targetsAA = value;
  }

  private Set<UnitType> getTargetsAa() {
    return m_targetsAA;
  }

  public Set<UnitType> getTargetsAa(final GameData data) {
    if (m_targetsAA != null) {
      return m_targetsAA;
    }
    return StreamSupport.stream(data.getUnitTypeList().spliterator(), false)
        .filter(ut -> UnitAttachment.get(ut).getIsAir())
        .collect(Collectors.toSet());
  }

  private void resetTargetsAa() {
    m_targetsAA = null;
  }

  private void setWillNotFireIfPresent(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("willNotFireIfPresent: no such unit type: " + u + thisErrorMsg());
      }
      m_willNotFireIfPresent.add(ut);
    }
  }

  private void setWillNotFireIfPresent(final Set<UnitType> value) {
    m_willNotFireIfPresent = value;
  }

  public Set<UnitType> getWillNotFireIfPresent() {
    return m_willNotFireIfPresent;
  }

  private void resetWillNotFireIfPresent() {
    m_willNotFireIfPresent = new HashSet<>();
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
      m_movementLimit = null;
      m_attackingLimit = null;
      m_placementLimit = null;
    }
  }

  private void setCanNotMoveDuringCombatMove(final String s) {
    m_canNotMoveDuringCombatMove = getBool(s);
  }

  private void setCanNotMoveDuringCombatMove(final Boolean s) {
    m_canNotMoveDuringCombatMove = s;
  }

  public boolean getCanNotMoveDuringCombatMove() {
    return m_canNotMoveDuringCombatMove;
  }

  private void resetCanNotMoveDuringCombatMove() {
    m_canNotMoveDuringCombatMove = false;
  }

  private void setMovementLimit(final String value) throws GameParseException {
    if (value == null) {
      m_movementLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("movementLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("movementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("movementLimit value must owned, allied, or total" + thisErrorMsg());
    }
    m_movementLimit = Tuple.of(max, s[1]);
  }

  private void setMovementLimit(final Tuple<Integer, String> value) {
    m_movementLimit = value;
  }

  public Tuple<Integer, String> getMovementLimit() {
    return m_movementLimit;
  }

  private void resetMovementLimit() {
    m_movementLimit = null;
  }

  private void setAttackingLimit(final String value) throws GameParseException {
    if (value == null) {
      m_attackingLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("attackingLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("attackingLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("attackingLimit value must owned, allied, or total" + thisErrorMsg());
    }
    m_attackingLimit = Tuple.of(max, s[1]);
  }

  private void setAttackingLimit(final Tuple<Integer, String> value) {
    m_attackingLimit = value;
  }

  public Tuple<Integer, String> getAttackingLimit() {
    return m_attackingLimit;
  }

  private void resetAttackingLimit() {
    m_attackingLimit = null;
  }

  private void setPlacementLimit(final String value) throws GameParseException {
    if (value == null) {
      m_placementLimit = null;
      return;
    }
    final UnitType ut = (UnitType) this.getAttachedTo();
    if (ut == null) {
      throw new GameParseException("getAttachedTo returned null" + thisErrorMsg());
    }
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("placementLimit must have 2 fields, value and count" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("placementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("placementLimit value must owned, allied, or total" + thisErrorMsg());
    }
    m_placementLimit = Tuple.of(max, s[1]);
  }

  private void setPlacementLimit(final Tuple<Integer, String> value) {
    m_placementLimit = value;
  }

  private Tuple<Integer, String> getPlacementLimit() {
    return m_placementLimit;
  }

  private void resetPlacementLimit() {
    m_placementLimit = null;
  }

  private void setTuv(final String s) {
    m_tuv = getInt(s);
  }

  private void setTuv(final Integer s) {
    m_tuv = s;
  }

  public int getTuv() {
    return m_tuv;
  }

  private void resetTuv() {
    m_tuv = -1;
  }

  public static int getMaximumNumberOfThisUnitTypeToReachStackingLimit(final String limitType, final UnitType ut,
      final Territory t, final PlayerID owner, final GameData data) {
    final UnitAttachment ua = UnitAttachment.get(ut);
    final Tuple<Integer, String> stackingLimit;
    if (limitType.equals("movementLimit")) {
      stackingLimit = ua.getMovementLimit();
    } else if (limitType.equals("attackingLimit")) {
      stackingLimit = ua.getAttackingLimit();
    } else if (limitType.equals("placementLimit")) {
      stackingLimit = ua.getPlacementLimit();
    } else {
      throw new IllegalStateException(
          "getMaximumNumberOfThisUnitTypeToReachStackingLimit does not allow limitType: " + limitType);
    }
    if (stackingLimit == null) {
      return Integer.MAX_VALUE;
    }
    int max = stackingLimit.getFirst();
    if (max == Integer.MAX_VALUE && (ua.getIsAaForBombingThisUnitOnly() || ua.getIsAaForCombatOnly())) {
      // under certain rules (classic rules) there can only be 1 aa gun in a territory.
      if (!(Properties.getWW2V2(data) || Properties.getWW2V3(data)
          || Properties.getMultipleAaPerTerritory(data))) {
        max = 1;
      }
    }
    final Predicate<Unit> stackingMatch;
    final String stackingType = stackingLimit.getSecond();
    if (stackingType.equals("owned")) {
      stackingMatch = Matches.unitIsOfType(ut).and(Matches.unitIsOwnedBy(owner));
    } else if (stackingType.equals("allied")) {
      stackingMatch = Matches.unitIsOfType(ut).and(Matches.isUnitAllied(owner, data));
    } else {
      stackingMatch = Matches.unitIsOfType(ut);
    }
    // else if (stackingType.equals("total"))
    final int totalInTerritory = CollectionUtils.countMatches(t.getUnits().getUnits(), stackingMatch);
    return Math.max(0, max - totalInTerritory);
  }

  @Override
  public void validate(final GameData data) throws GameParseException {
    if (m_isAir) {
      if (m_isSea /* || m_isFactory */ || m_isSub || m_transportCost != -1 || m_carrierCapacity != -1 || m_canBlitz
          || m_canBombard || m_isMarine != 0 || m_isInfantry || m_isLandTransportable || m_isLandTransport
          || m_isAirTransportable || m_isCombatTransport) {
        throw new GameParseException("air units cannot have certain properties, " + thisErrorMsg());
      }
    } else if (m_isSea) {
      if (m_canBlitz || m_isAir /* || m_isFactory */ || m_isStrategicBomber || m_carrierCost != -1
          || m_transportCost != -1 || m_isMarine != 0 || m_isInfantry || m_isLandTransportable || m_isLandTransport
          || m_isAirTransportable || m_isAirTransport || m_isKamikaze) {
        throw new GameParseException("sea units cannot have certain properties, " + thisErrorMsg());
      }
    } else { // if land
      if (m_canBombard || m_isStrategicBomber || m_isSub || m_carrierCapacity != -1 || m_bombard != -1
          || m_isAirTransport || m_isCombatTransport || m_isKamikaze) {
        throw new GameParseException("land units cannot have certain properties, " + thisErrorMsg());
      }
    }
    if (m_hitPoints < 1) {
      throw new GameParseException("hitPoints cannot be zero or negative, " + thisErrorMsg());
    }
    if (m_attackAA < 0 || m_attackAAmaxDieSides < -1 || m_attackAAmaxDieSides > 200 || m_offensiveAttackAA < 0
        || m_offensiveAttackAAmaxDieSides < -1 || m_offensiveAttackAAmaxDieSides > 200) {
      throw new GameParseException(
          "attackAA or attackAAmaxDieSides or offensiveAttackAA or offensiveAttackAAmaxDieSides is wrong, "
              + thisErrorMsg());
    }
    if (m_carrierCapacity != -1 && m_carrierCost != -1) {
      throw new GameParseException("carrierCost and carrierCapacity cannot be set at same time, " + thisErrorMsg());
    }
    if (((m_bombingBonus != 0 || m_bombingMaxDieSides >= 0) && !(m_isStrategicBomber || m_isRocket))
        || (m_bombingMaxDieSides < -1)) {
      throw new GameParseException("something wrong with bombingBonus or bombingMaxDieSides, " + thisErrorMsg());
    }
    if (m_maxBuiltPerPlayer < -1) {
      throw new GameParseException("maxBuiltPerPlayer cannot be negative, " + thisErrorMsg());
    }
    if (m_isCombatTransport && m_transportCapacity < 1) {
      throw new GameParseException(
          "cannot have isCombatTransport on unit without transportCapacity, " + thisErrorMsg());
    }
    if (m_isSea && m_transportCapacity != -1 && Properties.getTransportCasualtiesRestricted(data)
        && (m_attack > 0 || m_defense > 0) && !m_isCombatTransport) {
      throw new GameParseException("Restricted transports cannot have attack or defense, " + thisErrorMsg());
    }
    if (m_isConstruction
        && (m_constructionType == null || m_constructionType.equals("none") || m_constructionType.isEmpty()
            || m_constructionsPerTerrPerTypePerTurn < 0 || m_maxConstructionsPerTypePerTerr < 0)) {
      throw new GameParseException("Constructions must have constructionType and positive constructionsPerTerrPerType "
          + "and maxConstructionsPerType, " + thisErrorMsg());
    }
    if (!m_isConstruction
        && (!(m_constructionType == null || m_constructionType.equals("none") || m_constructionType.isEmpty())
            || m_constructionsPerTerrPerTypePerTurn >= 0 || m_maxConstructionsPerTypePerTerr >= 0)) {
      throw new GameParseException("Constructions must have isConstruction true, " + thisErrorMsg());
    }
    if (m_constructionsPerTerrPerTypePerTurn > m_maxConstructionsPerTypePerTerr) {
      throw new GameParseException(
          "Constructions must have constructionsPerTerrPerTypePerTurn Less than maxConstructionsPerTypePerTerr, "
              + thisErrorMsg());
    }
    if (m_unitPlacementRestrictions != null) {
      getListedTerritories(m_unitPlacementRestrictions);
    }
    if (m_requiresUnits != null) {
      for (final String[] combo : m_requiresUnits) {
        getListedUnits(combo);
      }
    }
    if ((m_canBeDamaged && m_maxDamage < 1) || (m_canDieFromReachingMaxDamage && m_maxDamage < 1)
        || (!m_canBeDamaged && m_canDieFromReachingMaxDamage)) {
      throw new GameParseException(
          "something wrong with canBeDamaged or maxDamage or canDieFromReachingMaxDamage or isFactory, "
              + thisErrorMsg());
    }
    if (m_canInvadeOnlyFrom != null && !m_canInvadeOnlyFrom[0].equals("all")
        && !m_canInvadeOnlyFrom[0].equals("none")) {
      for (final String transport : m_canInvadeOnlyFrom) {
        final UnitType ut = getData().getUnitTypeList().getUnitType(transport);
        if (ut == null) {
          throw new GameParseException("No unit called:" + transport + thisErrorMsg());
        }
        if (ut.getAttachments() == null || ut.getAttachments().isEmpty()) {
          throw new GameParseException(transport + " has no attachments, please declare " + transport
              + " in the xml before using it as a transport" + thisErrorMsg());
          // Units may be considered transported if they are on a carrier, or if they are paratroopers, or if they are
          // mech infantry. The
          // "transporter" may not be an actual transport, so we should not check for that here.
        }
      }
    }
    if (!m_receivesAbilityWhenWith.isEmpty()) {
      for (final String value : m_receivesAbilityWhenWith) {
        // first is ability, second is unit that we get it from
        final String[] s = splitOnColon(value);
        if (s.length != 2) {
          throw new GameParseException("receivesAbilityWhenWith must have 2 parts, 'ability:unit'" + thisErrorMsg());
        }
        if (getData().getUnitTypeList().getUnitType(s[1]) == null) {
          throw new GameParseException("receivesAbilityWhenWith, unit does not exist, name:" + s[1] + thisErrorMsg());
        }
        // currently only supports canBlitz (m_canBlitz)
        if (!s[0].equals("canBlitz")) {
          throw new GameParseException("receivesAbilityWhenWith so far only supports: canBlitz" + thisErrorMsg());
        }
      }
    }
    if (!m_whenCombatDamaged.isEmpty()) {
      for (final Tuple<Tuple<Integer, Integer>, Tuple<String, String>> key : m_whenCombatDamaged) {
        final String obj = key.getSecond().getFirst();
        if (obj.equals(UNITSMAYNOTLANDONCARRIER)) {
          continue;
        }
        if (obj.equals(UNITSMAYNOTLEAVEALLIEDCARRIER)) {
          continue;
        }
        throw new GameParseException("m_whenCombatDamaged so far only supports: " + UNITSMAYNOTLANDONCARRIER + ", "
            + UNITSMAYNOTLEAVEALLIEDCARRIER + thisErrorMsg());
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

  private Collection<Territory> getListedTerritories(final String[] list) throws GameParseException {
    final List<Territory> territories = new ArrayList<>();
    for (final String name : list) {
      // Validate all territories exist
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new GameParseException("No territory called: " + name + thisErrorMsg());
      }
      territories.add(territory);
    }
    return territories;
  }

  private static boolean playerHasRockets(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    return ta != null && ta.getRocket();
  }

  private static boolean playerHasMechInf(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    return ta != null && ta.getMechanizedInfantry();
  }

  private static boolean playerHasParatroopers(final PlayerID player) {
    final TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
    return ta != null && ta.getParatroopers();
  }

  /**
   * Returns a list of all unit properties. Should cover ALL fields stored in UnitAttachment
   * Remember to test for null and fix arrays. The stats exporter relies on this toString having
   * two spaces after each entry, so do not change this please, except to add new abilities onto the end.
   */
  public String allUnitStatsForExporter() {
    return this.getAttachedTo().toString().replaceFirst("games.strategy.engine.data.", "") + " with:"
        + "  isAir:" + m_isAir
        + "  isSea:" + m_isSea
        + "  movement:" + m_movement
        + "  attack:" + m_attack
        + "  defense:" + m_defense
        + "  hitPoints:" + m_hitPoints
        + "  canBlitz:" + m_canBlitz
        + "  artillerySupportable:" + m_artillerySupportable
        + "  artillery:" + m_artillery
        + "  unitSupportCount:" + m_unitSupportCount
        + "  attackRolls:" + m_attackRolls
        + "  defenseRolls:" + m_defenseRolls
        + "  chooseBestRoll:" + m_chooseBestRoll
        + "  isMarine:" + m_isMarine
        + "  isInfantry:" + m_isInfantry
        + "  isLandTransportable:" + m_isLandTransportable
        + "  isLandTransport:" + m_isLandTransport
        + "  isAirTransportable:" + m_isAirTransportable
        + "  isAirTransport:" + m_isAirTransport
        + "  isStrategicBomber:" + m_isStrategicBomber
        + "  transportCapacity:" + m_transportCapacity
        + "  transportCost:" + m_transportCost
        + "  carrierCapacity:" + m_carrierCapacity
        + "  carrierCost:" + m_carrierCost
        + "  isSub:" + m_isSub
        + "  isDestroyer:" + m_isDestroyer
        + "  canBombard:" + m_canBombard
        + "  bombard:" + m_bombard
        + "  isAAforCombatOnly:" + m_isAAforCombatOnly
        + "  isAAforBombingThisUnitOnly:" + m_isAAforBombingThisUnitOnly
        + "  isAAforFlyOverOnly:" + m_isAAforFlyOverOnly
        + "  attackAA:" + m_attackAA
        + "  offensiveAttackAA:" + m_offensiveAttackAA
        + "  attackAAmaxDieSides:" + m_attackAAmaxDieSides
        + "  offensiveAttackAAmaxDieSides:" + m_offensiveAttackAAmaxDieSides
        + "  maxAAattacks:" + m_maxAAattacks
        + "  maxRoundsAA:" + m_maxRoundsAA
        + "  mayOverStackAA:" + m_mayOverStackAA
        + "  damageableAA:" + m_damageableAA
        + "  typeAA:" + m_typeAA
        + "  targetsAA:"
        + (m_targetsAA != null ? (m_targetsAA.size() == 0 ? "empty" : m_targetsAA.toString()) : "all air units")
        + "  willNotFireIfPresent:"
        + (m_willNotFireIfPresent != null
            ? (m_willNotFireIfPresent.size() == 0 ? "empty" : m_willNotFireIfPresent.toString())
            : "null")
        + "  isRocket:" + m_isRocket + "  canProduceUnits:" + m_canProduceUnits + "  canProduceXUnits:"
        + m_canProduceXUnits + "  createsUnitsList:"
        + (m_createsUnitsList != null ? (m_createsUnitsList.size() == 0 ? "empty" : m_createsUnitsList.toString())
            : "null")
        + "  createsResourcesList:"
        + (m_createsResourcesList != null
            ? (m_createsResourcesList.size() == 0 ? "empty" : m_createsResourcesList.toString())
            : "null")
        + "  fuelCost:" + (m_fuelCost != null ? (m_fuelCost.size() == 0 ? "empty" : m_fuelCost.toString()) : "null")
        + "  fuelFlatCost:"
        + (m_fuelFlatCost != null ? (m_fuelFlatCost.size() == 0 ? "empty" : m_fuelFlatCost.toString()) : "null")
        + "  isInfrastructure:" + m_isInfrastructure + "  isConstruction:" + m_isConstruction + "  constructionType:"
        + m_constructionType + "  constructionsPerTerrPerTypePerTurn:" + m_constructionsPerTerrPerTypePerTurn
        + "  maxConstructionsPerTypePerTerr:" + m_maxConstructionsPerTypePerTerr + "  destroyedWhenCapturedBy:"
        + (m_destroyedWhenCapturedBy != null
            ? (m_destroyedWhenCapturedBy.size() == 0 ? "empty" : m_destroyedWhenCapturedBy.toString())
            : "null")
        + "  canBeCapturedOnEnteringBy:"
        + (m_canBeCapturedOnEnteringBy != null
            ? (m_canBeCapturedOnEnteringBy.size() == 0 ? "empty" : m_canBeCapturedOnEnteringBy.toString())
            : "null")
        + "  canBeDamaged:" + m_canBeDamaged + "  canDieFromReachingMaxDamage:" + m_canDieFromReachingMaxDamage
        + "  maxOperationalDamage:" + m_maxOperationalDamage + "  maxDamage:" + m_maxDamage
        + "  unitPlacementRestrictions:"
        + (m_unitPlacementRestrictions != null
            ? (m_unitPlacementRestrictions.length == 0 ? "empty" : Arrays.toString(m_unitPlacementRestrictions))
            : "null")
        + "  requiresUnits:"
        + (m_requiresUnits != null
            ? (m_requiresUnits.size() == 0 ? "empty" : MyFormatter.listOfArraysToString(m_requiresUnits))
            : "null")
        + "  consumesUnits:"
        + (m_consumesUnits != null ? (m_consumesUnits.size() == 0 ? "empty" : m_consumesUnits.toString()) : "null")
        + "  requiresUnitsToMove:"
        + (m_requiresUnitsToMove != null
            ? (m_requiresUnitsToMove.size() == 0 ? "empty" : MyFormatter.listOfArraysToString(m_requiresUnitsToMove))
            : "null")
        + "  canOnlyBePlacedInTerritoryValuedAtX:" + m_canOnlyBePlacedInTerritoryValuedAtX + "  maxBuiltPerPlayer:"
        + m_maxBuiltPerPlayer + "  special:"
        + (m_special != null ? (m_special.size() == 0 ? "empty" : m_special.toString()) : "null")
        + "  isSuicide:" + m_isSuicide
        + "  isSuicideOnHit:" + m_isSuicideOnHit
        + "  isCombatTransport:" + m_isCombatTransport
        + "  canInvadeOnlyFrom:"
        + (m_canInvadeOnlyFrom != null
            ? (m_canInvadeOnlyFrom.length == 0 ? "empty" : Arrays.toString(m_canInvadeOnlyFrom))
            : "null")
        + "  canBeGivenByTerritoryTo:"
        + (m_canBeGivenByTerritoryTo != null
            ? (m_canBeGivenByTerritoryTo.size() == 0 ? "empty" : m_canBeGivenByTerritoryTo.toString())
            : "null")
        + "  receivesAbilityWhenWith:"
        + (m_receivesAbilityWhenWith != null
            ? (m_receivesAbilityWhenWith.size() == 0 ? "empty" : m_receivesAbilityWhenWith.toString())
            : "null")
        + "  whenCombatDamaged:"
        + (m_whenCombatDamaged != null ? (m_whenCombatDamaged.size() == 0 ? "empty" : m_whenCombatDamaged.toString())
            : "null")
        + "  blockade:" + m_blockade + "  bombingMaxDieSides:" + m_bombingMaxDieSides + "  bombingBonus:"
        + m_bombingBonus + "  bombingTargets:" + m_bombingTargets + "  givesMovement:"
        + (m_givesMovement != null ? (m_givesMovement.size() == 0 ? "empty" : m_givesMovement.toString()) : "null")
        + "  repairsUnits:"
        + (m_repairsUnits != null ? (m_repairsUnits.isEmpty() ? "empty" : m_repairsUnits.toString()) : "null")
        + "  canScramble:" + m_canScramble + "  maxScrambleDistance:" + m_maxScrambleDistance + "  isAirBase:"
        + m_isAirBase + "  maxScrambleCount:" + m_maxScrambleCount
        + "  whenCapturedChangesInto:"
        + (m_whenCapturedChangesInto != null
            ? (m_whenCapturedChangesInto.size() == 0 ? "empty" : m_whenCapturedChangesInto.toString())
            : "null")
        + " whenCapturedSustainsDamage:" + m_whenCapturedSustainsDamage
        + "  whenHitPointsDamagedChangesInto:"
        + (m_whenHitPointsDamagedChangesInto != null
            ? (m_whenHitPointsDamagedChangesInto.size() == 0 ? "empty" : m_whenHitPointsDamagedChangesInto.toString())
            : "null")
        + "  whenHitPointsRepairedChangesInto:"
        + (m_whenHitPointsRepairedChangesInto != null
            ? (m_whenHitPointsRepairedChangesInto.size() == 0 ? "empty" : m_whenHitPointsRepairedChangesInto.toString())
            : "null")
        + "  canIntercept:" + m_canIntercept + "  canEscort:" + m_canEscort + "  canAirBattle:" + m_canAirBattle
        + "  airDefense:" + m_airDefense + "  airAttack:" + m_airAttack + "  canNotMoveDuringCombatMove:"
        + m_canNotMoveDuringCombatMove + "  movementLimit:"
        + (m_movementLimit != null ? m_movementLimit.toString() : "null") + "  attackingLimit:"
        + (m_attackingLimit != null ? m_attackingLimit.toString() : "null") + "  placementLimit:"
        + (m_placementLimit != null ? m_placementLimit.toString() : "null")
        + "  tuv:" + m_tuv;
  }

  /**
   * Displays all unit options in a short description form that's user friendly rather than as XML.
   * Shows all except for: m_constructionType, m_constructionsPerTerrPerTypePerTurn, m_maxConstructionsPerTypePerTerr,
   * m_canBeGivenByTerritoryTo, m_destroyedWhenCapturedBy, m_canBeCapturedOnEnteringBy.
   */
  public String toStringShortAndOnlyImportantDifferences(final PlayerID player) {
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

    addAaDescription("Targeted Attack", getOffensiveAttackAa(player), getOffensiveAttackAaMaxDieSides(), tuples);
    addAaDescription("Targeted Defense", getAttackAa(player), getAttackAaMaxDieSides(), tuples);

    // TODO: Rework rocket description
    if (getIsRocket() && playerHasRockets(player)) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Can Rocket Attack, ");
      final int bombingBonus = getBombingBonus();
      if ((getBombingMaxDieSides() != -1 || bombingBonus != 0)
          && Properties.getUseBombingMaxDiceSidesAndBonus(getData())) {
        sb.append(bombingBonus != 0 ? bombingBonus + 1 : 1).append("-")
            .append(getBombingMaxDieSides() != -1 ? getBombingMaxDieSides() + bombingBonus
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
    if ((getCanBeDamaged())
        && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(getData())) {
      final StringBuilder sb = new StringBuilder();
      sb.append("Can be Damaged by Raids, ");
      if (getMaxOperationalDamage() > -1) {
        sb.append(getMaxOperationalDamage()).append(" Max Operational Damage, ");
      }
      if ((getCanProduceUnits()) && getCanProduceXUnits() < 0) {
        sb.append("Total Damage up to ").append(getMaxDamage() > -1 ? getMaxDamage() : 2)
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
      tuples
          .add(Tuple.of("Scramble Range", String.valueOf(getMaxScrambleDistance() > 0 ? getMaxScrambleDistance() : 1)));
    }

    final List<UnitSupportAttachment> supports = CollectionUtils.getMatches(UnitSupportAttachment.get(unitType),
        Matches.unitSupportAttachmentCanBeUsedByPlayer(player));
    if (supports.size() > 3) {
      tuples.add(Tuple.of("Can Provide Support to Units", ""));
    } else if (supports.size() > 0) {
      final boolean moreThanOneSupportType = UnitSupportAttachment.get(getData()).size() > 1;
      for (final UnitSupportAttachment support : supports) {
        if (support.getUnitType() == null || support.getUnitType().isEmpty()) {
          continue;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(support.getBonus()).append(moreThanOneSupportType ? " " + support.getBonusType() : "")
            .append(support.getStrength() && support.getRoll() ? " Power & Rolls"
                : (support.getStrength() ? " Power" : " Rolls"))
            .append(" to ").append(support.getNumber())
            .append(support.getAllied() && support.getEnemy() ? " Allied & Enemy "
                : (support.getAllied() ? " Allied " : " Enemy "))
            .append(support.getUnitType().size() > 4 ? "Units"
                : MyFormatter.defaultNamedToTextList(support.getUnitType(), "/", false));
        final String key = "Support on " + (support.getOffence() && support.getDefence() ? "Attack & Defense"
            : (support.getOffence() ? "Attack" : "Defense"));
        tuples.add(Tuple.of(key, sb.toString()));
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
        sb.append(bombingBonus != 0 ? bombingBonus + 1 : 1).append("-")
            .append(getBombingMaxDieSides() != -1 ? getBombingMaxDieSides() + bombingBonus
                : getData().getDiceSides() + bombingBonus);
      } else {
        sb.append("1-").append(getData().getDiceSides());
      }
      sb.append(" Damage");
      tuples.add(Tuple.of("Can Perform Raids", sb.toString()));
    }

    if (getAirAttack(player) > 0 && (getIsStrategicBomber() || getCanEscort() || getCanAirBattle())) {
      tuples.add(Tuple.of("Air Attack", attackRolls > 1 ? (attackRolls + "x") : "" + getAirAttack(player)));
    }
    if (getAirDefense(player) > 0 && (getCanIntercept() || getCanAirBattle())) {
      tuples.add(Tuple.of("Air Defense", defenseRolls > 1 ? (defenseRolls + "x") : "" + getAirDefense(player)));
    }

    if (getIsSub()) {
      tuples.add(Tuple.of("Is Stealth", ""));
    }
    if (getIsDestroyer()) {
      tuples.add(Tuple.of("Is Anti-Stealth", ""));
    }

    if (getCanBombard(player) && getBombard() > 0) {
      tuples.add(Tuple.of("Bombard", (attackRolls > 1 ? (attackRolls + "x") : "") + getBombard()));
    }

    if (getBlockade() > 0) {
      tuples.add(Tuple.of("Blockade Loss", String.valueOf(getBlockade())));
    }

    if (getIsSuicide()) {
      tuples.add(Tuple.of("Suicide/Munition Unit", ""));
    }
    if (getIsSuicideOnHit()) {
      tuples.add(Tuple.of("Suicide on Hit Unit", ""));
    }
    if (getIsAir() && (getIsKamikaze() || Properties.getKamikazeAirplanes(getData()))) {
      tuples.add(Tuple.of("Is Kamikaze", "Can use all Movement to Attack Target"));
    }

    if ((getIsInfantry() || getIsLandTransportable()) && playerHasMechInf(player)) {
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
    if (getTransportCapacity() > 0 && (getIsSea() || (getIsAir() && playerHasParatroopers(player))
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

    if (getRepairsUnits() != null && !getRepairsUnits().isEmpty()
        && Properties.getTwoHitPointUnitsRequireRepairFacilities(getData())
        && (Properties.getBattleshipsRepairAtBeginningOfRound(getData())
            || Properties.getBattleshipsRepairAtEndOfRound(getData()))) {
      if (getRepairsUnits().size() <= 4) {
        tuples.add(
            Tuple.of("Can Repair", MyFormatter.integerDefaultNamedMapToString(getRepairsUnits(), " ", "=", false)));
      } else {
        tuples.add(Tuple.of("Can Repair some Units", ""));
      }
    }

    if (getGivesMovement() != null && getGivesMovement().totalValues() > 0
        && Properties.getUnitsMayGiveBonusMovement(getData())) {
      if (getGivesMovement().size() <= 4) {
        tuples.add(Tuple.of("Can Modify Unit Movement",
            MyFormatter.integerDefaultNamedMapToString(getGivesMovement(), " ", "=", false)));
      } else {
        tuples.add(Tuple.of("Can Modify Unit Movement", ""));
      }
    }

    if (getConsumesUnits() != null && getConsumesUnits().totalValues() == 1) {
      tuples.add(Tuple.of("Unit is an Upgrade Of", getConsumesUnits().keySet().iterator().next().getName()));
    } else if (getConsumesUnits() != null && getConsumesUnits().totalValues() > 0) {
      if (getConsumesUnits().size() <= 4) {
        tuples.add(Tuple.of("Unit Consumes on Placement",
            MyFormatter.integerDefaultNamedMapToString(getConsumesUnits(), " ", "x", true)));
      } else {
        tuples.add(Tuple.of("Unit Consumes Other Units on Placement", ""));
      }
    }

    if (getRequiresUnits() != null && getRequiresUnits().size() > 0
        && Properties.getUnitPlacementRestrictions(getData())) {
      final List<String> totalUnitsListed = new ArrayList<>();
      for (final String[] list : getRequiresUnits()) {
        totalUnitsListed.addAll(Arrays.asList(list));
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
        totalUnitsListed.addAll(Arrays.asList(list));
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
        tuples.add(Tuple.of("Placement Restrictions", Arrays.toString(getUnitPlacementRestrictions())));
      }
    }
    if (getCanOnlyBePlacedInTerritoryValuedAtX() > 0
        && Properties.getUnitPlacementRestrictions(getData())) {
      tuples.add(Tuple.of("Must be Placed in Territory with Value of at Least",
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
        tuples.add(Tuple.of("Max " + getMovementLimit().getSecond() + " Units Moving per Territory", "1"));
      } else if (getMovementLimit().getFirst() < 10000) {
        tuples.add(Tuple.of("Max " + getMovementLimit().getSecond() + " Units Moving per Territory",
            String.valueOf(getMovementLimit().getFirst())));
      }
    }

    if (getAttackingLimit() != null) {
      if (getAttackingLimit().getFirst() == Integer.MAX_VALUE
          && (getIsAaForBombingThisUnitOnly() || getIsAaForCombatOnly())
          && !(Properties.getWW2V2(getData())
              || Properties.getWW2V3(getData())
              || Properties.getMultipleAaPerTerritory(getData()))) {
        tuples.add(Tuple.of("Max " + getAttackingLimit().getSecond() + " Units Attacking per Territory", "1"));
      } else if (getAttackingLimit().getFirst() < 10000) {
        tuples.add(Tuple.of("Max " + getAttackingLimit().getSecond() + " Units Attacking per Territory",
            String.valueOf(getAttackingLimit().getFirst())));
      }
    }

    if (getPlacementLimit() != null) {
      if (getPlacementLimit().getFirst() == Integer.MAX_VALUE
          && (getIsAaForBombingThisUnitOnly() || getIsAaForCombatOnly())
          && !(Properties.getWW2V2(getData())
              || Properties.getWW2V3(getData())
              || Properties.getMultipleAaPerTerritory(getData()))) {
        tuples.add(Tuple.of("Max " + getPlacementLimit().getSecond() + " Units Placed per Territory", "1"));
      } else if (getPlacementLimit().getFirst() < 10000) {
        tuples.add(Tuple.of("Max " + getPlacementLimit().getSecond() + " Units Placed per Territory",
            String.valueOf(getPlacementLimit().getFirst())));
      }
    }

    final StringBuilder result = new StringBuilder();
    for (final Tuple<String, String> tuple : tuples) {
      result.append(tuple.getFirst());
      if (!tuple.getSecond().isEmpty()) {
        result.append(": <b>").append(tuple.getSecond()).append("</b>");
      }
      result.append("<br />");
    }
    return result.toString();
  }

  private static <T extends DefaultNamed> void addIntegerMapDescription(final String key,
      final IntegerMap<T> integerMap, final List<Tuple<String, String>> tuples) {
    if (integerMap != null && integerMap.size() > 0) {
      final StringBuilder sb = new StringBuilder();
      if (integerMap.size() > 4) {
        sb.append(String.valueOf(integerMap.totalValues()));
      } else {
        for (final Entry<T, Integer> entry : integerMap.entrySet()) {
          sb.append(entry.getValue()).append("x").append(entry.getKey().getName()).append(" ");
        }
      }
      tuples.add(Tuple.of(key, sb.toString()));
    }
  }

  private void addAaDescription(final String startOfKey, final int aa, final int aaMaxDieSides,
      final List<Tuple<String, String>> tuples) {
    if ((getIsAaForCombatOnly() || getIsAaForBombingThisUnitOnly() || getIsAaForFlyOverOnly()) && (aa > 0)) {
      final String string = String.valueOf(aa) + "/"
          + (aaMaxDieSides != -1 ? aaMaxDieSides : getData().getDiceSides())
          + " " + getTypeAa()
          + " with " + (getMaxAaAttacks() > -1 ? getMaxAaAttacks() : "Unlimited")
          + " Attacks for " + (getMaxRoundsAa() > -1 ? getMaxRoundsAa() : "Unlimited") + " Rounds";
      tuples.add(Tuple.of(startOfKey + getAaKey(), string));
    }
  }

  private String getAaKey() {
    if (getIsAaForCombatOnly() && getIsAaForFlyOverOnly()
        && !Properties.getAaTerritoryRestricted(getData())) {
      return " for Combat & Move Through";
    } else if (getIsAaForBombingThisUnitOnly() && getIsAaForFlyOverOnly()
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

  /**
   * Parses the specified value and sets whether or not the unit is a paratroop.
   *
   * @deprecated does nothing, kept to avoid breaking maps, do not remove.
   */
  @Deprecated
  private void setIsParatroop(@SuppressWarnings("unused") final String s) {}

  /**
   * Parses the specified value and sets whether or not the unit is mechanized.
   *
   * @deprecated does nothing, used to keep compatibility with older xml files, do not remove.
   */
  @Deprecated
  private void setIsMechanized(@SuppressWarnings("unused") final String s) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("isAir",
            MutableProperty.of(
                this::setIsAir,
                this::setIsAir,
                this::getIsAir,
                this::resetIsAir))
        .put("isMechanized",
            MutableProperty.ofWriteOnlyString(
                this::setIsMechanized))
        .put("isParatroop",
            MutableProperty.ofWriteOnlyString(
                this::setIsParatroop))
        .put("isSea",
            MutableProperty.of(
                this::setIsSea,
                this::setIsSea,
                this::getIsSea,
                this::resetIsSea))
        .put("movement",
            MutableProperty.of(
                this::setMovement,
                this::setMovement,
                this::getMovement,
                this::resetMovement))
        .put("canBlitz",
            MutableProperty.of(
                this::setCanBlitz,
                this::setCanBlitz,
                this::getCanBlitz,
                this::resetCanBlitz))
        .put("isKamikaze",
            MutableProperty.of(
                this::setIsKamikaze,
                this::setIsKamikaze,
                this::getIsKamikaze,
                this::resetIsKamikaze))
        .put("canInvadeOnlyFrom",
            MutableProperty.of(
                this::setCanInvadeOnlyFrom,
                this::setCanInvadeOnlyFrom,
                this::getCanInvadeOnlyFrom,
                this::resetCanInvadeOnlyFrom))
        .put("fuelCost",
            MutableProperty.of(
                this::setFuelCost,
                this::setFuelCost,
                this::getFuelCost,
                this::resetFuelCost))
        .put("fuelFlatCost",
            MutableProperty.of(
                this::setFuelFlatCost,
                this::setFuelFlatCost,
                this::getFuelFlatCost,
                this::resetFuelFlatCost))
        .put("canNotMoveDuringCombatMove",
            MutableProperty.of(
                this::setCanNotMoveDuringCombatMove,
                this::setCanNotMoveDuringCombatMove,
                this::getCanNotMoveDuringCombatMove,
                this::resetCanNotMoveDuringCombatMove))
        .put("movementLimit",
            MutableProperty.of(
                this::setMovementLimit,
                this::setMovementLimit,
                this::getMovementLimit,
                this::resetMovementLimit))
        .put("attack",
            MutableProperty.of(
                this::setAttack,
                this::setAttack,
                this::getAttack,
                this::resetAttack))
        .put("defense",
            MutableProperty.of(
                this::setDefense,
                this::setDefense,
                this::getDefense,
                this::resetDefense))
        .put("isInfrastructure",
            MutableProperty.of(
                this::setIsInfrastructure,
                this::setIsInfrastructure,
                this::getIsInfrastructure,
                this::resetIsInfrastructure))
        .put("canBombard",
            MutableProperty.of(
                this::setCanBombard,
                this::setCanBombard,
                this::getCanBombard,
                this::resetCanBombard))
        .put("bombard",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setBombard,
                this::getBombard,
                () -> -1))
        .put("isSub",
            MutableProperty.of(
                this::setIsSub,
                this::setIsSub,
                this::getIsSub,
                this::resetIsSub))
        .put("isDestroyer",
            MutableProperty.of(
                this::setIsDestroyer,
                this::setIsDestroyer,
                this::getIsDestroyer,
                this::resetIsDestroyer))
        .put("artillery",
            MutableProperty.of(
                this::setArtillery,
                this::setArtillery,
                this::getArtillery,
                this::resetArtillery))
        .put("artillerySupportable",
            MutableProperty.of(
                this::setArtillerySupportable,
                this::setArtillerySupportable,
                this::getArtillerySupportable,
                this::resetArtillerySupportable))
        .put("unitSupportCount",
            MutableProperty.of(
                this::setUnitSupportCount,
                this::setUnitSupportCount,
                this::getUnitSupportCount,
                this::resetUnitSupportCount))
        .put("isMarine",
            MutableProperty.of(
                this::setIsMarine,
                this::setIsMarine,
                this::getIsMarine,
                this::resetIsMarine))
        .put("isSuicide",
            MutableProperty.of(
                this::setIsSuicide,
                this::setIsSuicide,
                this::getIsSuicide,
                this::resetIsSuicide))
        .put("isSuicideOnHit",
            MutableProperty.of(
                this::setIsSuicideOnHit,
                this::setIsSuicideOnHit,
                this::getIsSuicideOnHit,
                this::resetIsSuicideOnHit))
        .put("attackingLimit",
            MutableProperty.of(
                this::setAttackingLimit,
                this::setAttackingLimit,
                this::getAttackingLimit,
                this::resetAttackingLimit))
        .put("attackRolls",
            MutableProperty.of(
                this::setAttackRolls,
                this::setAttackRolls,
                this::getAttackRolls,
                this::resetAttackRolls))
        .put("defenseRolls",
            MutableProperty.of(
                this::setDefenseRolls,
                this::setDefenseRolls,
                this::getDefenseRolls,
                this::resetDefenseRolls))
        .put("chooseBestRoll",
            MutableProperty.of(
                this::setChooseBestRoll,
                this::setChooseBestRoll,
                this::getChooseBestRoll,
                this::resetChooseBestRoll))
        .put("isCombatTransport",
            MutableProperty.of(
                this::setIsCombatTransport,
                this::setIsCombatTransport,
                this::getIsCombatTransport,
                this::resetIsCombatTransport))
        .put("transportCapacity",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setTransportCapacity,
                this::getTransportCapacity,
                () -> -1))
        .put("transportCost",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setTransportCost,
                this::getTransportCost,
                () -> -1))
        .put("carrierCapacity",
            MutableProperty.of(
                this::setCarrierCapacity,
                this::setCarrierCapacity,
                this::getCarrierCapacity,
                this::resetCarrierCapacity))
        .put("carrierCost",
            MutableProperty.of(
                this::setCarrierCost,
                this::setCarrierCost,
                this::getCarrierCost,
                this::resetCarrierCost))
        .put("isAirTransport",
            MutableProperty.of(
                this::setIsAirTransport,
                this::setIsAirTransport,
                this::getIsAirTransport,
                this::resetIsAirTransport))
        .put("isAirTransportable",
            MutableProperty.of(
                this::setIsAirTransportable,
                this::setIsAirTransportable,
                this::getIsAirTransportable,
                this::resetIsAirTransportable))
        .put("isInfantry",
            MutableProperty.of(
                this::setIsInfantry,
                this::setIsInfantry,
                this::getIsInfantry,
                this::resetIsInfantry))
        .put("isLandTransport",
            MutableProperty.of(
                this::setIsLandTransport,
                this::setIsLandTransport,
                this::getIsLandTransport,
                this::resetIsLandTransport))
        .put("isLandTransportable",
            MutableProperty.of(
                this::setIsLandTransportable,
                this::setIsLandTransportable,
                this::getIsLandTransportable,
                this::resetIsLandTransportable))
        .put("isAAforCombatOnly",
            MutableProperty.of(
                this::setIsAaForCombatOnly,
                this::setIsAaForCombatOnly,
                this::getIsAaForCombatOnly,
                this::resetIsAaForCombatOnly))
        .put("isAAforBombingThisUnitOnly",
            MutableProperty.of(
                this::setIsAaForBombingThisUnitOnly,
                this::setIsAaForBombingThisUnitOnly,
                this::getIsAaForBombingThisUnitOnly,
                this::resetIsAaForBombingThisUnitOnly))
        .put("isAAforFlyOverOnly",
            MutableProperty.of(
                this::setIsAaForFlyOverOnly,
                this::setIsAaForFlyOverOnly,
                this::getIsAaForFlyOverOnly,
                this::resetIsAaForFlyOverOnly))
        .put("isRocket",
            MutableProperty.of(
                this::setIsRocket,
                this::setIsRocket,
                this::getIsRocket,
                this::resetIsRocket))
        .put("attackAA",
            MutableProperty.of(
                this::setAttackAa,
                this::setAttackAa,
                this::getAttackAa,
                this::resetAttackAa))
        .put("offensiveAttackAA",
            MutableProperty.of(
                this::setOffensiveAttackAa,
                this::setOffensiveAttackAa,
                this::getOffensiveAttackAa,
                this::resetOffensiveAttackAa))
        .put("attackAAmaxDieSides",
            MutableProperty.of(
                this::setAttackAaMaxDieSides,
                this::setAttackAaMaxDieSides,
                this::getAttackAaMaxDieSides,
                this::resetAttackAaMaxDieSides))
        .put("offensiveAttackAAmaxDieSides",
            MutableProperty.of(
                this::setOffensiveAttackAaMaxDieSides,
                this::setOffensiveAttackAaMaxDieSides,
                this::getOffensiveAttackAaMaxDieSides,
                this::resetOffensiveAttackAaMaxDieSides))
        .put("maxAAattacks",
            MutableProperty.of(
                this::setMaxAaAttacks,
                this::setMaxAaAttacks,
                this::getMaxAaAttacks,
                this::resetMaxAaAttacks))
        .put("maxRoundsAA",
            MutableProperty.of(
                this::setMaxRoundsAa,
                this::setMaxRoundsAa,
                this::getMaxRoundsAa,
                this::resetMaxRoundsAa))
        .put("typeAA",
            MutableProperty.ofString(
                this::setTypeAa,
                this::getTypeAa,
                this::resetTypeAa))
        .put("targetsAA",
            MutableProperty.of(
                this::setTargetsAa,
                this::setTargetsAa,
                this::getTargetsAa,
                this::resetTargetsAa))
        .put("mayOverStackAA",
            MutableProperty.of(
                this::setMayOverStackAa,
                this::setMayOverStackAa,
                this::getMayOverStackAa,
                this::resetMayOverStackAa))
        .put("damageableAA",
            MutableProperty.of(
                this::setDamageableAa,
                this::setDamageableAa,
                this::getDamageableAa,
                this::resetDamageableAa))
        .put("willNotFireIfPresent",
            MutableProperty.of(
                this::setWillNotFireIfPresent,
                this::setWillNotFireIfPresent,
                this::getWillNotFireIfPresent,
                this::resetWillNotFireIfPresent))
        .put("isStrategicBomber",
            MutableProperty.of(
                this::setIsStrategicBomber,
                this::setIsStrategicBomber,
                this::getIsStrategicBomber,
                this::resetIsStrategicBomber))
        .put("bombingMaxDieSides",
            MutableProperty.of(
                this::setBombingMaxDieSides,
                this::setBombingMaxDieSides,
                this::getBombingMaxDieSides,
                this::resetBombingMaxDieSides))
        .put("bombingBonus",
            MutableProperty.of(
                this::setBombingBonus,
                this::setBombingBonus,
                this::getBombingBonus,
                this::resetBombingBonus))
        .put("canIntercept",
            MutableProperty.of(
                this::setCanIntercept,
                this::setCanIntercept,
                this::getCanIntercept,
                this::resetCanIntercept))
        .put("canEscort",
            MutableProperty.of(
                this::setCanEscort,
                this::setCanEscort,
                this::getCanEscort,
                this::resetCanEscort))
        .put("canAirBattle",
            MutableProperty.of(
                this::setCanAirBattle,
                this::setCanAirBattle,
                this::getCanAirBattle,
                this::resetCanAirBattle))
        .put("airDefense",
            MutableProperty.of(
                this::setAirDefense,
                this::setAirDefense,
                this::getAirDefense,
                this::resetAirDefense))
        .put("airAttack",
            MutableProperty.of(
                this::setAirAttack,
                this::setAirAttack,
                this::getAirAttack,
                this::resetAirAttack))
        .put("bombingTargets",
            MutableProperty.of(
                this::setBombingTargets,
                this::setBombingTargets,
                this::getBombingTargets,
                this::resetBombingTargets))
        .put("canProduceUnits",
            MutableProperty.of(
                this::setCanProduceUnits,
                this::setCanProduceUnits,
                this::getCanProduceUnits,
                this::resetCanProduceUnits))
        .put("canProduceXUnits",
            MutableProperty.of(
                this::setCanProduceXUnits,
                this::setCanProduceXUnits,
                this::getCanProduceXUnits,
                this::resetCanProduceXUnits))
        .put("createsUnitsList",
            MutableProperty.of(
                this::setCreatesUnitsList,
                this::setCreatesUnitsList,
                this::getCreatesUnitsList,
                this::resetCreatesUnitsList))
        .put("createsResourcesList",
            MutableProperty.of(
                this::setCreatesResourcesList,
                this::setCreatesResourcesList,
                this::getCreatesResourcesList,
                this::resetCreatesResourcesList))
        .put("hitPoints",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setHitPoints,
                this::getHitPoints,
                () -> 1))
        .put("canBeDamaged",
            MutableProperty.of(
                this::setCanBeDamaged,
                this::setCanBeDamaged,
                this::getCanBeDamaged,
                this::resetCanBeDamaged))
        .put("maxDamage",
            MutableProperty.of(
                this::setMaxDamage,
                this::setMaxDamage,
                this::getMaxDamage,
                this::resetMaxDamage))
        .put("maxOperationalDamage",
            MutableProperty.of(
                this::setMaxOperationalDamage,
                this::setMaxOperationalDamage,
                this::getMaxOperationalDamage,
                this::resetMaxOperationalDamage))
        .put("canDieFromReachingMaxDamage",
            MutableProperty.of(
                this::setCanDieFromReachingMaxDamage,
                this::setCanDieFromReachingMaxDamage,
                this::getCanDieFromReachingMaxDamage,
                this::resetCanDieFromReachingMaxDamage))
        .put("isConstruction",
            MutableProperty.of(
                this::setIsConstruction,
                this::setIsConstruction,
                this::getIsConstruction,
                this::resetIsConstruction))
        .put("constructionType",
            MutableProperty.ofString(
                this::setConstructionType,
                this::getConstructionType,
                this::resetConstructionType))
        .put("constructionsPerTerrPerTypePerTurn",
            MutableProperty.of(
                this::setConstructionsPerTerrPerTypePerTurn,
                this::setConstructionsPerTerrPerTypePerTurn,
                this::getConstructionsPerTerrPerTypePerTurn,
                this::resetConstructionsPerTerrPerTypePerTurn))
        .put("maxConstructionsPerTypePerTerr",
            MutableProperty.of(
                this::setMaxConstructionsPerTypePerTerr,
                this::setMaxConstructionsPerTypePerTerr,
                this::getMaxConstructionsPerTypePerTerr,
                this::resetMaxConstructionsPerTypePerTerr))
        .put("canOnlyBePlacedInTerritoryValuedAtX",
            MutableProperty.of(
                this::setCanOnlyBePlacedInTerritoryValuedAtX,
                this::setCanOnlyBePlacedInTerritoryValuedAtX,
                this::getCanOnlyBePlacedInTerritoryValuedAtX,
                this::resetCanOnlyBePlacedInTerritoryValuedAtX))
        .put("requiresUnits",
            MutableProperty.of(
                this::setRequiresUnits,
                this::setRequiresUnits,
                this::getRequiresUnits,
                this::resetRequiresUnits))
        .put("consumesUnits",
            MutableProperty.of(
                this::setConsumesUnits,
                this::setConsumesUnits,
                this::getConsumesUnits,
                this::resetConsumesUnits))
        .put("requiresUnitsToMove",
            MutableProperty.of(
                this::setRequiresUnitsToMove,
                this::setRequiresUnitsToMove,
                this::getRequiresUnitsToMove,
                this::resetRequiresUnitsToMove))
        .put("unitPlacementRestrictions",
            MutableProperty.of(
                this::setUnitPlacementRestrictions,
                this::setUnitPlacementRestrictions,
                this::getUnitPlacementRestrictions,
                this::resetUnitPlacementRestrictions))
        .put("maxBuiltPerPlayer",
            MutableProperty.of(
                this::setMaxBuiltPerPlayer,
                this::setMaxBuiltPerPlayer,
                this::getMaxBuiltPerPlayer,
                this::resetMaxBuiltPerPlayer))
        .put("placementLimit",
            MutableProperty.of(
                this::setPlacementLimit,
                this::setPlacementLimit,
                this::getPlacementLimit,
                this::resetPlacementLimit))
        .put("canScramble",
            MutableProperty.of(
                this::setCanScramble,
                this::setCanScramble,
                this::getCanScramble,
                this::resetCanScramble))
        .put("isAirBase",
            MutableProperty.of(
                this::setIsAirBase,
                this::setIsAirBase,
                this::getIsAirBase,
                this::resetIsAirBase))
        .put("maxScrambleDistance",
            MutableProperty.of(
                this::setMaxScrambleDistance,
                this::setMaxScrambleDistance,
                this::getMaxScrambleDistance,
                this::resetMaxScrambleDistance))
        .put("maxScrambleCount",
            MutableProperty.of(
                this::setMaxScrambleCount,
                this::setMaxScrambleCount,
                this::getMaxScrambleCount,
                this::resetMaxScrambleCount))
        .put("blockade",
            MutableProperty.of(
                this::setBlockade,
                this::setBlockade,
                this::getBlockade,
                this::resetBlockade))
        .put("repairsUnits",
            MutableProperty.of(
                this::setRepairsUnits,
                this::setRepairsUnits,
                this::getRepairsUnits,
                this::resetRepairsUnits))
        .put("givesMovement",
            MutableProperty.of(
                this::setGivesMovement,
                this::setGivesMovement,
                this::getGivesMovement,
                this::resetGivesMovement))
        .put("destroyedWhenCapturedBy",
            MutableProperty.of(
                this::setDestroyedWhenCapturedBy,
                this::setDestroyedWhenCapturedBy,
                this::getDestroyedWhenCapturedBy,
                this::resetDestroyedWhenCapturedBy))
        .put("whenHitPointsDamagedChangesInto",
            MutableProperty.of(
                this::setWhenHitPointsDamagedChangesInto,
                this::setWhenHitPointsDamagedChangesInto,
                this::getWhenHitPointsDamagedChangesInto,
                this::resetWhenHitPointsDamagedChangesInto))
        .put("whenHitPointsRepairedChangesInto",
            MutableProperty.of(
                this::setWhenHitPointsRepairedChangesInto,
                this::setWhenHitPointsRepairedChangesInto,
                this::getWhenHitPointsRepairedChangesInto,
                this::resetWhenHitPointsRepairedChangesInto))
        .put("whenCapturedChangesInto",
            MutableProperty.of(
                this::setWhenCapturedChangesInto,
                this::setWhenCapturedChangesInto,
                this::getWhenCapturedChangesInto,
                this::resetWhenCapturedChangesInto))
        .put("whenCapturedSustainsDamage",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setWhenCapturedSustainsDamage,
                this::getWhenCapturedSustainsDamage,
                () -> 0))
        .put("canBeCapturedOnEnteringBy",
            MutableProperty.of(
                this::setCanBeCapturedOnEnteringBy,
                this::setCanBeCapturedOnEnteringBy,
                this::getCanBeCapturedOnEnteringBy,
                this::resetCanBeCapturedOnEnteringBy))
        .put("canBeGivenByTerritoryTo",
            MutableProperty.of(
                this::setCanBeGivenByTerritoryTo,
                this::setCanBeGivenByTerritoryTo,
                this::getCanBeGivenByTerritoryTo,
                this::resetCanBeGivenByTerritoryTo))
        .put("whenCombatDamaged",
            MutableProperty.of(
                this::setWhenCombatDamaged,
                this::setWhenCombatDamaged,
                this::getWhenCombatDamaged,
                this::resetWhenCombatDamaged))
        .put("receivesAbilityWhenWith",
            MutableProperty.of(
                this::setReceivesAbilityWhenWith,
                this::setReceivesAbilityWhenWith,
                this::getReceivesAbilityWhenWith,
                this::resetReceivesAbilityWhenWith))
        .put("special",
            MutableProperty.of(
                this::setSpecial,
                this::setSpecial,
                this::getSpecial,
                this::resetSpecial))
        .put("tuv",
            MutableProperty.of(
                this::setTuv,
                this::setTuv,
                this::getTuv,
                this::resetTuv))
        .put("isFactory",
            MutableProperty.<Boolean>ofWriteOnly(
                this::setIsFactory,
                this::setIsFactory))
        .put("isAA",
            MutableProperty.<Boolean>ofWriteOnly(
                this::setIsAa,
                this::setIsAa))
        .put("destroyedWhenCapturedFrom",
            MutableProperty.ofWriteOnlyString(
                this::setDestroyedWhenCapturedFrom))
        .put("unitPlacementOnlyAllowedIn",
            MutableProperty.ofWriteOnlyString(
                this::setUnitPlacementOnlyAllowedIn))
        .put("isAAmovement",
            MutableProperty.<Boolean>ofWriteOnly(
                this::setIsAaMovement,
                this::setIsAaMovement))
        .put("isTwoHit",
            MutableProperty.<Boolean>ofWriteOnly(
                this::setIsTwoHit,
                this::setIsTwoHit))
        .build();
  }
}
