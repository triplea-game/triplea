package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Triple;

@MapSupport
public class PlayerAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 1880755875866426270L;

  /**
   * Convenience method. can be null
   */
  public static PlayerAttachment get(final PlayerID p) {
    // allow null
    return p.getPlayerAttachment();
  }

  static PlayerAttachment get(final PlayerID p, final String nameOfAttachment) {
    final PlayerAttachment playerAttachment = p.getPlayerAttachment();
    if (playerAttachment == null) {
      throw new IllegalStateException("No player attachment for:" + p.getName() + " with name:" + nameOfAttachment);
    }
    return playerAttachment;
  }

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private int m_vps = 0;
  // need to store some data during a turn
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private int m_captureVps = 0;
  // number of capitals needed before we lose all our money
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private int m_retainCapitalNumber = 1;
  // number of capitals needed before we lose ability to gain money and produce units
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private int m_retainCapitalProduceNumber = 1;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private List<PlayerID> m_giveUnitControl = new ArrayList<>();
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private List<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<>();
  // gives any technology researched to this player automatically
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private List<PlayerID> m_shareTechnology = new ArrayList<>();
  // allows these players to help pay for technology
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private List<PlayerID> m_helpPayTechCost = new ArrayList<>();
  // do we lose our money and have it disappear or is that money captured?
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_destroysPUs = false;
  // are we immune to being blockaded?
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private boolean m_immuneToBlockade = false;
  // what resources can be used for suicide attacks, and
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private IntegerMap<Resource> m_suicideAttackResources = new IntegerMap<>();
  // at what attack power
  // what can be hit by suicide attacks
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private Set<UnitType> m_suicideAttackTargets = null;
  // placement limits on a flexible per player basis
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private Set<Triple<Integer, String, Set<UnitType>>> m_placementLimit = new HashSet<>();

  // movement limits on a flexible per player basis
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private Set<Triple<Integer, String, Set<UnitType>>> m_movementLimit = new HashSet<>();

  // attacking limits on a flexible per player basis
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private Set<Triple<Integer, String, Set<UnitType>>> m_attackingLimit = new HashSet<>();

  /** Creates new PlayerAttachment. */
  public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  private void setPlacementLimit(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 3) {
      throw new GameParseException("placementLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("placementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("placementLimit type must be: owned, allied, or total" + thisErrorMsg());
    }
    final HashSet<UnitType> types = new HashSet<>();
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
    m_placementLimit.add(Triple.of(max, s[1], types));
  }

  private void setPlacementLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    m_placementLimit = value;
  }

  private Set<Triple<Integer, String, Set<UnitType>>> getPlacementLimit() {
    return m_placementLimit;
  }

  private void resetPlacementLimit() {
    m_placementLimit = new HashSet<>();
  }

  private void setMovementLimit(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 3) {
      throw new GameParseException("movementLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("movementLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("movementLimit type must be: owned, allied, or total" + thisErrorMsg());
    }
    final HashSet<UnitType> types = new HashSet<>();
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
    m_movementLimit.add(Triple.of(max, s[1], types));
  }

  private void setMovementLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    m_movementLimit = value;
  }

  private Set<Triple<Integer, String, Set<UnitType>>> getMovementLimit() {
    return m_movementLimit;
  }

  private void resetMovementLimit() {
    m_movementLimit = new HashSet<>();
  }

  private void setAttackingLimit(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 3) {
      throw new GameParseException("attackingLimit must have 3 parts: count, type, unit list" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException("attackingLimit count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException("attackingLimit type must be: owned, allied, or total" + thisErrorMsg());
    }
    final HashSet<UnitType> types = new HashSet<>();
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
    m_attackingLimit.add(Triple.of(max, s[1], types));
  }

  private void setAttackingLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    m_attackingLimit = value;
  }

  private Set<Triple<Integer, String, Set<UnitType>>> getAttackingLimit() {
    return m_attackingLimit;
  }

  private void resetAttackingLimit() {
    m_attackingLimit = new HashSet<>();
  }

  public static boolean getCanTheseUnitsMoveWithoutViolatingStackingLimit(final String limitType,
      final Collection<Unit> unitsMoving, final Territory toMoveInto, final PlayerID owner, final GameData data) {
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
            "getCanTheseUnitsMoveWithoutViolatingStackingLimit does not allow limitType: " + limitType);
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
      final Collection<Unit> currentInTerritory = toMoveInto.getUnits().getUnits();
      // first remove units that do not apply to our current type
      if (type.equals("owned")) {
        currentInTerritory
            .removeAll(CollectionUtils.getMatches(currentInTerritory, Matches.unitIsOwnedBy(owner).negate()));
        copyUnitsMoving.removeAll(CollectionUtils.getMatches(copyUnitsMoving, Matches.unitIsOwnedBy(owner).negate()));
      } else if (type.equals("allied")) {
        currentInTerritory
            .removeAll(CollectionUtils.getMatches(currentInTerritory, Matches.alliedUnit(owner, data).negate()));
        copyUnitsMoving
            .removeAll(CollectionUtils.getMatches(copyUnitsMoving, Matches.alliedUnit(owner, data).negate()));
      }
      // else if (type.equals("total"))
      // now remove units that are not part of our list
      currentInTerritory.retainAll(CollectionUtils.getMatches(currentInTerritory, Matches.unitIsOfTypes(unitsToTest)));
      copyUnitsMoving.retainAll(CollectionUtils.getMatches(copyUnitsMoving, Matches.unitIsOfTypes(unitsToTest)));
      // now test
      if (max < (currentInTerritory.size() + copyUnitsMoving.size())) {
        return false;
      }
    }
    return true;
  }

  private void setSuicideAttackTargets(final String value) throws GameParseException {
    if (value == null) {
      m_suicideAttackTargets = null;
      return;
    }
    if (m_suicideAttackTargets == null) {
      m_suicideAttackTargets = new HashSet<>();
    }
    final String[] s = splitOnColon(value);
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("suicideAttackTargets: no such unit called " + u + thisErrorMsg());
      }
      m_suicideAttackTargets.add(ut);
    }
  }

  private void setSuicideAttackTargets(final Set<UnitType> value) {
    m_suicideAttackTargets = value;
  }

  public Set<UnitType> getSuicideAttackTargets() {
    return m_suicideAttackTargets;
  }

  private void resetSuicideAttackTargets() {
    m_suicideAttackTargets = null;
  }

  private void setSuicideAttackResources(final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length != 2) {
      throw new GameParseException("suicideAttackResources must have exactly 2 fields" + thisErrorMsg());
    }
    final int attackValue = getInt(s[0]);
    if (attackValue < 0) {
      throw new GameParseException("suicideAttackResources attack value must be positive" + thisErrorMsg());
    }
    final Resource r = getData().getResourceList().getResource(s[1]);
    if (r == null) {
      throw new GameParseException("no such resource: " + s[1] + thisErrorMsg());
    }
    m_suicideAttackResources.put(r, attackValue);
  }

  private void setSuicideAttackResources(final IntegerMap<Resource> value) {
    m_suicideAttackResources = value;
  }

  public IntegerMap<Resource> getSuicideAttackResources() {
    return m_suicideAttackResources;
  }

  private void resetSuicideAttackResources() {
    m_suicideAttackResources = new IntegerMap<>();
  }

  private void setVps(final int value) {
    m_vps = value;
  }

  public int getVps() {
    return m_vps;
  }

  private void setCaptureVps(final String value) {
    m_captureVps = getInt(value);
  }

  private void setCaptureVps(final Integer value) {
    m_captureVps = value;
  }

  public int getCaptureVps() {
    return m_captureVps;
  }

  private void resetCaptureVps() {
    m_captureVps = 0;
  }

  private void setRetainCapitalNumber(final String value) {
    m_retainCapitalNumber = getInt(value);
  }

  private void setRetainCapitalNumber(final Integer value) {
    m_retainCapitalNumber = value;
  }

  public int getRetainCapitalNumber() {
    return m_retainCapitalNumber;
  }

  private void resetRetainCapitalNumber() {
    m_retainCapitalNumber = 1;
  }

  private void setRetainCapitalProduceNumber(final String value) {
    m_retainCapitalProduceNumber = getInt(value);
  }

  private void setRetainCapitalProduceNumber(final Integer value) {
    m_retainCapitalProduceNumber = value;
  }

  int getRetainCapitalProduceNumber() {
    return m_retainCapitalProduceNumber;
  }

  private void resetRetainCapitalProduceNumber() {
    m_retainCapitalProduceNumber = 1;
  }

  private void setGiveUnitControl(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_giveUnitControl.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setGiveUnitControl(final List<PlayerID> value) {
    m_giveUnitControl = value;
  }

  public List<PlayerID> getGiveUnitControl() {
    return m_giveUnitControl;
  }

  private void resetGiveUnitControl() {
    m_giveUnitControl = new ArrayList<>();
  }

  private void setCaptureUnitOnEnteringBy(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_captureUnitOnEnteringBy.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setCaptureUnitOnEnteringBy(final List<PlayerID> value) {
    m_captureUnitOnEnteringBy = value;
  }

  public List<PlayerID> getCaptureUnitOnEnteringBy() {
    return m_captureUnitOnEnteringBy;
  }

  private void resetCaptureUnitOnEnteringBy() {
    m_captureUnitOnEnteringBy = new ArrayList<>();
  }

  private void setShareTechnology(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_shareTechnology.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setShareTechnology(final List<PlayerID> value) {
    m_shareTechnology = value;
  }

  public List<PlayerID> getShareTechnology() {
    return m_shareTechnology;
  }

  private void resetShareTechnology() {
    m_shareTechnology = new ArrayList<>();
  }

  private void setHelpPayTechCost(final String value) throws GameParseException {
    final String[] temp = splitOnColon(value);
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerId(name);
      if (tempPlayer != null) {
        m_helpPayTechCost.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  private void setHelpPayTechCost(final List<PlayerID> value) {
    m_helpPayTechCost = value;
  }

  public List<PlayerID> getHelpPayTechCost() {
    return m_helpPayTechCost;
  }

  private void resetHelpPayTechCost() {
    m_helpPayTechCost = new ArrayList<>();
  }

  private void setDestroysPUs(final String value) {
    m_destroysPUs = getBool(value);
  }

  private void setDestroysPUs(final Boolean value) {
    m_destroysPUs = value;
  }

  public boolean getDestroysPUs() {
    return m_destroysPUs;
  }

  private void resetDestroysPUs() {
    m_destroysPUs = false;
  }

  private void setImmuneToBlockade(final String value) {
    m_immuneToBlockade = getBool(value);
  }

  private void setImmuneToBlockade(final Boolean value) {
    m_immuneToBlockade = value;
  }

  public boolean getImmuneToBlockade() {
    return m_immuneToBlockade;
  }

  private void resetImmuneToBlockade() {
    m_immuneToBlockade = false;
  }

  @Override
  public void validate(final GameData data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("vps",
            MutableProperty.ofMapper(
                DefaultAttachment::getInt,
                this::setVps,
                this::getVps,
                () -> 0))
        .put("captureVps",
            MutableProperty.of(
                this::setCaptureVps,
                this::setCaptureVps,
                this::getCaptureVps,
                this::resetCaptureVps))
        .put("retainCapitalNumber",
            MutableProperty.of(
                this::setRetainCapitalNumber,
                this::setRetainCapitalNumber,
                this::getRetainCapitalNumber,
                this::resetRetainCapitalNumber))
        .put("retainCapitalProduceNumber",
            MutableProperty.of(
                this::setRetainCapitalProduceNumber,
                this::setRetainCapitalProduceNumber,
                this::getRetainCapitalProduceNumber,
                this::resetRetainCapitalProduceNumber))
        .put("giveUnitControl",
            MutableProperty.of(
                this::setGiveUnitControl,
                this::setGiveUnitControl,
                this::getGiveUnitControl,
                this::resetGiveUnitControl))
        .put("captureUnitOnEnteringBy",
            MutableProperty.of(
                this::setCaptureUnitOnEnteringBy,
                this::setCaptureUnitOnEnteringBy,
                this::getCaptureUnitOnEnteringBy,
                this::resetCaptureUnitOnEnteringBy))
        .put("shareTechnology",
            MutableProperty.of(
                this::setShareTechnology,
                this::setShareTechnology,
                this::getShareTechnology,
                this::resetShareTechnology))
        .put("helpPayTechCost",
            MutableProperty.of(
                this::setHelpPayTechCost,
                this::setHelpPayTechCost,
                this::getHelpPayTechCost,
                this::resetHelpPayTechCost))
        .put("destroysPUs",
            MutableProperty.of(
                this::setDestroysPUs,
                this::setDestroysPUs,
                this::getDestroysPUs,
                this::resetDestroysPUs))
        .put("immuneToBlockade",
            MutableProperty.of(
                this::setImmuneToBlockade,
                this::setImmuneToBlockade,
                this::getImmuneToBlockade,
                this::resetImmuneToBlockade))
        .put("suicideAttackResources",
            MutableProperty.of(
                this::setSuicideAttackResources,
                this::setSuicideAttackResources,
                this::getSuicideAttackResources,
                this::resetSuicideAttackResources))
        .put("suicideAttackTargets",
            MutableProperty.of(
                this::setSuicideAttackTargets,
                this::setSuicideAttackTargets,
                this::getSuicideAttackTargets,
                this::resetSuicideAttackTargets))
        .put("placementLimit",
            MutableProperty.of(
                this::setPlacementLimit,
                this::setPlacementLimit,
                this::getPlacementLimit,
                this::resetPlacementLimit))
        .put("movementLimit",
            MutableProperty.of(
                this::setMovementLimit,
                this::setMovementLimit,
                this::getMovementLimit,
                this::resetMovementLimit))
        .put("attackingLimit",
            MutableProperty.of(
                this::setAttackingLimit,
                this::setAttackingLimit,
                this::getAttackingLimit,
                this::resetAttackingLimit))
        .build();
  }
}
