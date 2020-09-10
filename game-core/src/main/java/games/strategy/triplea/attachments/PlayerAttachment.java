package games.strategy.triplea.attachments;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

/**
 * An attachment for instances of {@link GamePlayer} that defines properties unrelated to rules (see
 * the class description of {@link AbstractPlayerRulesAttachment}).
 */
public class PlayerAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 1880755875866426270L;

  private int vps = 0;
  // need to store some data during a turn
  private int captureVps = 0;
  // number of capitals needed before we lose all our money
  private int retainCapitalNumber = 1;
  // number of capitals needed before we lose ability to gain money and produce units
  private int retainCapitalProduceNumber = 1;
  private List<GamePlayer> giveUnitControl = new ArrayList<>();
  private boolean giveUnitControlInAllTerritories = false;
  private List<GamePlayer> captureUnitOnEnteringBy = new ArrayList<>();
  // gives any technology researched to this player automatically
  private List<GamePlayer> shareTechnology = new ArrayList<>();
  // allows these players to help pay for technology
  private List<GamePlayer> helpPayTechCost = new ArrayList<>();
  // do we lose our money and have it disappear or is that money captured?
  private boolean destroysPus = false;
  // are we immune to being blockaded?
  private boolean immuneToBlockade = false;
  // what resources can be used for suicide attacks, and at what attack power
  private IntegerMap<Resource> suicideAttackResources = new IntegerMap<>();
  // what can be hit by suicide attacks
  private Set<UnitType> suicideAttackTargets = null;
  // placement limits on a flexible per player basis
  private Set<Triple<Integer, String, Set<UnitType>>> placementLimit = new HashSet<>();

  // movement limits on a flexible per player basis
  private Set<Triple<Integer, String, Set<UnitType>>> movementLimit = new HashSet<>();

  // attacking limits on a flexible per player basis
  private Set<Triple<Integer, String, Set<UnitType>>> attackingLimit = new HashSet<>();

  public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /** Convenience method. can be null */
  public static PlayerAttachment get(final GamePlayer p) {
    // allow null
    return p.getPlayerAttachment();
  }

  static PlayerAttachment get(final GamePlayer p, final String nameOfAttachment) {
    final PlayerAttachment playerAttachment = p.getPlayerAttachment();
    if (playerAttachment == null) {
      throw new IllegalStateException(
          "No player attachment for:" + p.getName() + " with name:" + nameOfAttachment);
    }
    return playerAttachment;
  }

  private void setPlacementLimit(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 3) {
      throw new GameParseException(
          "placementLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException(
          "placementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException(
          "placementLimit type must be: owned, allied, or total" + thisErrorMsg());
    }
    final Set<UnitType> types = new HashSet<>();
    if (s[2].equalsIgnoreCase("all")) {
      types.addAll(getData().getUnitTypeList().getAllUnitTypes());
    } else {
      for (int i = 2; i < s.length; i++) {
        final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
        if (ut == null) {
          throw new GameParseException("No unit called: " + s[i] + thisErrorMsg());
        }
        types.add(ut);
      }
    }
    placementLimit.add(Triple.of(max, s[1], types));
  }

  private void setPlacementLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    placementLimit = value;
  }

  private Set<Triple<Integer, String, Set<UnitType>>> getPlacementLimit() {
    return placementLimit;
  }

  private void resetPlacementLimit() {
    placementLimit = new HashSet<>();
  }

  private void setMovementLimit(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 3) {
      throw new GameParseException(
          "movementLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException(
          "movementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException(
          "movementLimit type must be: owned, allied, or total" + thisErrorMsg());
    }
    final Set<UnitType> types = new HashSet<>();
    if (s[2].equalsIgnoreCase("all")) {
      types.addAll(getData().getUnitTypeList().getAllUnitTypes());
    } else {
      for (int i = 2; i < s.length; i++) {
        final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
        if (ut == null) {
          throw new GameParseException("No unit called: " + s[i] + thisErrorMsg());
        }
        types.add(ut);
      }
    }
    movementLimit.add(Triple.of(max, s[1], types));
  }

  private void setMovementLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    movementLimit = value;
  }

  private Set<Triple<Integer, String, Set<UnitType>>> getMovementLimit() {
    return movementLimit;
  }

  private void resetMovementLimit() {
    movementLimit = new HashSet<>();
  }

  private void setAttackingLimit(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 3) {
      throw new GameParseException(
          "attackingLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException(
          "attackingLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException(
          "attackingLimit type must be: owned, allied, or total" + thisErrorMsg());
    }
    final Set<UnitType> types = new HashSet<>();
    if (s[2].equalsIgnoreCase("all")) {
      types.addAll(getData().getUnitTypeList().getAllUnitTypes());
    } else {
      for (int i = 2; i < s.length; i++) {
        final UnitType ut = getData().getUnitTypeList().getUnitType(s[i]);
        if (ut == null) {
          throw new GameParseException("No unit called: " + s[i] + thisErrorMsg());
        }
        types.add(ut);
      }
    }
    attackingLimit.add(Triple.of(max, s[1], types));
  }

  private void setAttackingLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    attackingLimit = value;
  }

  private Set<Triple<Integer, String, Set<UnitType>>> getAttackingLimit() {
    return attackingLimit;
  }

  private void resetAttackingLimit() {
    attackingLimit = new HashSet<>();
  }

  /**
   * Returns {@code true} if the specified units can move into the specified territory without
   * violating the specified stacking limit (movement, attack, or placement).
   */
  public static boolean getCanTheseUnitsMoveWithoutViolatingStackingLimit(
      final String limitType,
      final Collection<Unit> unitsMoving,
      final Territory toMoveInto,
      final GamePlayer owner,
      final GameData data) {
    final PlayerAttachment pa = PlayerAttachment.get(owner);
    if (pa == null) {
      return true;
    }
    final Set<Triple<Integer, String, Set<UnitType>>> stackingLimits;
    switch (limitType) {
      case "movementLimit":
        stackingLimits = pa.getMovementLimit();
        break;
      case "attackingLimit":
        stackingLimits = pa.getAttackingLimit();
        break;
      case "placementLimit":
        stackingLimits = pa.getPlacementLimit();
        break;
      default:
        throw new IllegalStateException(
            "getCanTheseUnitsMoveWithoutViolatingStackingLimit does not allow limitType: "
                + limitType);
    }
    if (stackingLimits.isEmpty()) {
      return true;
    }
    for (final Triple<Integer, String, Set<UnitType>> currentLimit : stackingLimits) {
      // first make a copy of unitsMoving
      final Collection<Unit> copyUnitsMoving = new ArrayList<>(unitsMoving);
      final int max = currentLimit.getFirst();
      final String type = currentLimit.getSecond();
      final Set<UnitType> unitsToTest = currentLimit.getThird();
      final Collection<Unit> currentInTerritory = new ArrayList<>(toMoveInto.getUnits());
      // first remove units that do not apply to our current type
      if (type.equals("owned")) {
        currentInTerritory.removeAll(
            CollectionUtils.getMatches(currentInTerritory, Matches.unitIsOwnedBy(owner).negate()));
        copyUnitsMoving.removeAll(
            CollectionUtils.getMatches(copyUnitsMoving, Matches.unitIsOwnedBy(owner).negate()));
      } else if (type.equals("allied")) {
        currentInTerritory.removeAll(
            CollectionUtils.getMatches(
                currentInTerritory, Matches.alliedUnit(owner, data).negate()));
        copyUnitsMoving.removeAll(
            CollectionUtils.getMatches(copyUnitsMoving, Matches.alliedUnit(owner, data).negate()));
      }
      // now remove units that are not part of our list
      currentInTerritory.retainAll(
          CollectionUtils.getMatches(currentInTerritory, Matches.unitIsOfTypes(unitsToTest)));
      copyUnitsMoving.retainAll(
          CollectionUtils.getMatches(copyUnitsMoving, Matches.unitIsOfTypes(unitsToTest)));
      // now test
      if (max < (currentInTerritory.size() + copyUnitsMoving.size())) {
        return false;
      }
    }
    return true;
  }

  private void setSuicideAttackTargets(final String value) throws GameParseException {
    if (value == null) {
      suicideAttackTargets = null;
      return;
    }
    if (suicideAttackTargets == null) {
      suicideAttackTargets = new HashSet<>();
    }
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException(
            "suicideAttackTargets: no such unit called " + u + thisErrorMsg());
      }
      suicideAttackTargets.add(ut);
    }
  }

  private void setSuicideAttackTargets(final Set<UnitType> value) {
    suicideAttackTargets = value;
  }

  public Set<UnitType> getSuicideAttackTargets() {
    return suicideAttackTargets;
  }

  private void resetSuicideAttackTargets() {
    suicideAttackTargets = null;
  }

  private void setSuicideAttackResources(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException(
          "suicideAttackResources must have exactly 2 fields" + thisErrorMsg());
    }
    final int attackValue = getInt(s[0]);
    if (attackValue < 0) {
      throw new GameParseException(
          "suicideAttackResources attack value must be positive" + thisErrorMsg());
    }
    final Resource r = getData().getResourceList().getResource(s[1]);
    if (r == null) {
      throw new GameParseException("no such resource: " + s[1] + thisErrorMsg());
    }
    suicideAttackResources.put(r, attackValue);
  }

  private void setSuicideAttackResources(final IntegerMap<Resource> value) {
    suicideAttackResources = value;
  }

  public IntegerMap<Resource> getSuicideAttackResources() {
    return suicideAttackResources;
  }

  private void resetSuicideAttackResources() {
    suicideAttackResources = new IntegerMap<>();
  }

  private void setVps(final int value) {
    vps = value;
  }

  public int getVps() {
    return vps;
  }

  private void setCaptureVps(final String value) {
    captureVps = getInt(value);
  }

  private void setCaptureVps(final Integer value) {
    captureVps = value;
  }

  public int getCaptureVps() {
    return captureVps;
  }

  private void resetCaptureVps() {
    captureVps = 0;
  }

  private void setRetainCapitalNumber(final String value) {
    retainCapitalNumber = getInt(value);
  }

  private void setRetainCapitalNumber(final Integer value) {
    retainCapitalNumber = value;
  }

  public int getRetainCapitalNumber() {
    return retainCapitalNumber;
  }

  private void resetRetainCapitalNumber() {
    retainCapitalNumber = 1;
  }

  private void setRetainCapitalProduceNumber(final String value) {
    retainCapitalProduceNumber = getInt(value);
  }

  private void setRetainCapitalProduceNumber(final Integer value) {
    retainCapitalProduceNumber = value;
  }

  int getRetainCapitalProduceNumber() {
    return retainCapitalProduceNumber;
  }

  private void resetRetainCapitalProduceNumber() {
    retainCapitalProduceNumber = 1;
  }

  private void setGiveUnitControl(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final GamePlayer tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        giveUnitControl.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setGiveUnitControl(final List<GamePlayer> value) {
    giveUnitControl = value;
  }

  public List<GamePlayer> getGiveUnitControl() {
    return giveUnitControl;
  }

  private void resetGiveUnitControl() {
    giveUnitControl = new ArrayList<>();
  }

  private void setGiveUnitControlInAllTerritories(final boolean value) {
    giveUnitControlInAllTerritories = value;
  }

  public boolean getGiveUnitControlInAllTerritories() {
    return giveUnitControlInAllTerritories;
  }

  private void setCaptureUnitOnEnteringBy(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final GamePlayer tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        captureUnitOnEnteringBy.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setCaptureUnitOnEnteringBy(final List<GamePlayer> value) {
    captureUnitOnEnteringBy = value;
  }

  public List<GamePlayer> getCaptureUnitOnEnteringBy() {
    return captureUnitOnEnteringBy;
  }

  private void resetCaptureUnitOnEnteringBy() {
    captureUnitOnEnteringBy = new ArrayList<>();
  }

  private void setShareTechnology(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final GamePlayer tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        shareTechnology.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setShareTechnology(final List<GamePlayer> value) {
    shareTechnology = value;
  }

  public List<GamePlayer> getShareTechnology() {
    return shareTechnology;
  }

  private void resetShareTechnology() {
    shareTechnology = new ArrayList<>();
  }

  private void setHelpPayTechCost(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final GamePlayer tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        helpPayTechCost.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setHelpPayTechCost(final List<GamePlayer> value) {
    helpPayTechCost = value;
  }

  public List<GamePlayer> getHelpPayTechCost() {
    return helpPayTechCost;
  }

  private void resetHelpPayTechCost() {
    helpPayTechCost = new ArrayList<>();
  }

  private void setDestroysPUs(final String value) {
    destroysPus = getBool(value);
  }

  private void setDestroysPUs(final Boolean value) {
    destroysPus = value;
  }

  public boolean getDestroysPUs() {
    return destroysPus;
  }

  private void resetDestroysPUs() {
    destroysPus = false;
  }

  private void setImmuneToBlockade(final String value) {
    immuneToBlockade = getBool(value);
  }

  private void setImmuneToBlockade(final Boolean value) {
    immuneToBlockade = value;
  }

  public boolean getImmuneToBlockade() {
    return immuneToBlockade;
  }

  private void resetImmuneToBlockade() {
    immuneToBlockade = false;
  }

  @Override
  public void validate(final GameData data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put(
            "vps",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt, this::setVps, this::getVps, () -> 0))
        .put(
            "captureVps",
            MutableProperty.of(
                this::setCaptureVps,
                this::setCaptureVps,
                this::getCaptureVps,
                this::resetCaptureVps))
        .put(
            "retainCapitalNumber",
            MutableProperty.of(
                this::setRetainCapitalNumber,
                this::setRetainCapitalNumber,
                this::getRetainCapitalNumber,
                this::resetRetainCapitalNumber))
        .put(
            "retainCapitalProduceNumber",
            MutableProperty.of(
                this::setRetainCapitalProduceNumber,
                this::setRetainCapitalProduceNumber,
                this::getRetainCapitalProduceNumber,
                this::resetRetainCapitalProduceNumber))
        .put(
            "giveUnitControl",
            MutableProperty.of(
                this::setGiveUnitControl,
                this::setGiveUnitControl,
                this::getGiveUnitControl,
                this::resetGiveUnitControl))
        .put(
            "giveUnitControlInAllTerritories",
            MutableProperty.ofMapper(
                DefaultAttachment::getBool,
                this::setGiveUnitControlInAllTerritories,
                this::getGiveUnitControlInAllTerritories,
                () -> false))
        .put(
            "captureUnitOnEnteringBy",
            MutableProperty.of(
                this::setCaptureUnitOnEnteringBy,
                this::setCaptureUnitOnEnteringBy,
                this::getCaptureUnitOnEnteringBy,
                this::resetCaptureUnitOnEnteringBy))
        .put(
            "shareTechnology",
            MutableProperty.of(
                this::setShareTechnology,
                this::setShareTechnology,
                this::getShareTechnology,
                this::resetShareTechnology))
        .put(
            "helpPayTechCost",
            MutableProperty.of(
                this::setHelpPayTechCost,
                this::setHelpPayTechCost,
                this::getHelpPayTechCost,
                this::resetHelpPayTechCost))
        .put(
            "destroysPUs",
            MutableProperty.of(
                this::setDestroysPUs,
                this::setDestroysPUs,
                this::getDestroysPUs,
                this::resetDestroysPUs))
        .put(
            "immuneToBlockade",
            MutableProperty.of(
                this::setImmuneToBlockade,
                this::setImmuneToBlockade,
                this::getImmuneToBlockade,
                this::resetImmuneToBlockade))
        .put(
            "suicideAttackResources",
            MutableProperty.of(
                this::setSuicideAttackResources,
                this::setSuicideAttackResources,
                this::getSuicideAttackResources,
                this::resetSuicideAttackResources))
        .put(
            "suicideAttackTargets",
            MutableProperty.of(
                this::setSuicideAttackTargets,
                this::setSuicideAttackTargets,
                this::getSuicideAttackTargets,
                this::resetSuicideAttackTargets))
        .put(
            "placementLimit",
            MutableProperty.of(
                this::setPlacementLimit,
                this::setPlacementLimit,
                this::getPlacementLimit,
                this::resetPlacementLimit))
        .put(
            "movementLimit",
            MutableProperty.of(
                this::setMovementLimit,
                this::setMovementLimit,
                this::getMovementLimit,
                this::resetMovementLimit))
        .put(
            "attackingLimit",
            MutableProperty.of(
                this::setAttackingLimit,
                this::setAttackingLimit,
                this::getAttackingLimit,
                this::resetAttackingLimit))
        .build();
  }
}
