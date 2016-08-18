package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
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

  public static PlayerAttachment get(final PlayerID p, final String nameOfAttachment) {
    final PlayerAttachment rVal = p.getPlayerAttachment(); //(PlayerAttachment) p.getAttachment(nameOfAttachment);
    if (rVal == null) {
      throw new IllegalStateException("No player attachment for:" + p.getName() + " with name:" + nameOfAttachment);
    }
    return rVal;
  }

  private int m_vps = 0;
  // need to store some data during a turn
  private int m_captureVps = 0;
  // number of capitals needed before we lose all our money
  private int m_retainCapitalNumber = 1;
  // number of capitals needed before we lose ability to gain money and produce units
  private int m_retainCapitalProduceNumber = 1;
  private ArrayList<PlayerID> m_giveUnitControl = new ArrayList<>();
  private ArrayList<PlayerID> m_captureUnitOnEnteringBy = new ArrayList<>();
  // gives any technology researched to this player automatically
  private ArrayList<PlayerID> m_shareTechnology = new ArrayList<>();
  // allows these players to help pay for technology
  private ArrayList<PlayerID> m_helpPayTechCost = new ArrayList<>();
  // do we lose our money and have it disappear or is that money captured?
  private boolean m_destroysPUs = false;
  // are we immune to being blockaded?
  private boolean m_immuneToBlockade = false;
  // what resources can be used for suicide attacks, and
  private IntegerMap<Resource> m_suicideAttackResources = new IntegerMap<>();
  // at what attack power
  // what can be hit by suicide attacks
  private HashSet<UnitType> m_suicideAttackTargets = null;
  // placement limits on a flexible per player basis
  private HashSet<Triple<Integer, String, HashSet<UnitType>>> m_placementLimit =
      new HashSet<>();

  // movement limits on a flexible per player basis
  private HashSet<Triple<Integer, String, HashSet<UnitType>>> m_movementLimit =
      new HashSet<>();

  // attacking limits on a flexible per player basis
  private HashSet<Triple<Integer, String, HashSet<UnitType>>> m_attackingLimit =
      new HashSet<>();

  /** Creates new PlayerAttachment */
  public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setPlacementLimit(final String value) throws GameParseException {
    final String[] s = value.split(":");
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
        } else {
          types.add(ut);
        }
      }
    }
    m_placementLimit.add(Triple.of(max, s[1], types));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setPlacementLimit(final HashSet<Triple<Integer, String, HashSet<UnitType>>> value) {
    m_placementLimit = value;
  }

  public HashSet<Triple<Integer, String, HashSet<UnitType>>> getPlacementLimit() {
    return m_placementLimit;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setMovementLimit(final String value) throws GameParseException {
    final String[] s = value.split(":");
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
        } else {
          types.add(ut);
        }
      }
    }
    m_movementLimit.add(Triple.of(max, s[1], types));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMovementLimit(final HashSet<Triple<Integer, String, HashSet<UnitType>>> value) {
    m_movementLimit = value;
  }

  public HashSet<Triple<Integer, String, HashSet<UnitType>>> getMovementLimit() {
    return m_movementLimit;
  }


  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAttackingLimit(final String value) throws GameParseException {
    final String[] s = value.split(":");
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
        } else {
          types.add(ut);
        }
      }
    }
    m_attackingLimit.add(Triple.of(max, s[1], types));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackingLimit(final HashSet<Triple<Integer, String, HashSet<UnitType>>> value) {
    m_attackingLimit = value;
  }

  public HashSet<Triple<Integer, String, HashSet<UnitType>>> getAttackingLimit() {
    return m_attackingLimit;
  }


  public static boolean getCanTheseUnitsMoveWithoutViolatingStackingLimit(final String limitType,
      final Collection<Unit> unitsMoving, final Territory toMoveInto, final PlayerID owner, final GameData data) {
    final PlayerAttachment pa = PlayerAttachment.get(owner);
    if (pa == null) {
      return true;
    }
    final HashSet<Triple<Integer, String, HashSet<UnitType>>> stackingLimits;
    if (limitType.equals("movementLimit")) {
      stackingLimits = pa.getMovementLimit();
    } else if (limitType.equals("attackingLimit")) {
      stackingLimits = pa.getAttackingLimit();
    } else if (limitType.equals("placementLimit")) {
      stackingLimits = pa.getPlacementLimit();
    } else {
      throw new IllegalStateException(
          "getCanTheseUnitsMoveWithoutViolatingStackingLimit does not allow limitType: " + limitType);
    }
    if (stackingLimits.isEmpty()) {
      return true;
    }
    for (final Triple<Integer, String, HashSet<UnitType>> currentLimit : stackingLimits) {
      // first make a copy of unitsMoving
      final Collection<Unit> copyUnitsMoving = new ArrayList<>(unitsMoving);
      final int max = currentLimit.getFirst();
      final String type = currentLimit.getSecond();
      final HashSet<UnitType> unitsToTest = currentLimit.getThird();
      final Collection<Unit> currentInTerritory = toMoveInto.getUnits().getUnits();
      // first remove units that do not apply to our current type
      if (type.equals("owned")) {
        currentInTerritory.removeAll(Match.getMatches(currentInTerritory, Matches.unitIsOwnedBy(owner).invert()));
        copyUnitsMoving.removeAll(Match.getMatches(copyUnitsMoving, Matches.unitIsOwnedBy(owner).invert()));
      } else if (type.equals("allied")) {
        currentInTerritory.removeAll(Match.getMatches(currentInTerritory, Matches.alliedUnit(owner, data).invert()));
        copyUnitsMoving.removeAll(Match.getMatches(copyUnitsMoving, Matches.alliedUnit(owner, data).invert()));
      }
      // else if (type.equals("total"))
      // now remove units that are not part of our list
      currentInTerritory.retainAll(Match.getMatches(currentInTerritory, Matches.unitIsOfTypes(unitsToTest)));
      copyUnitsMoving.retainAll(Match.getMatches(copyUnitsMoving, Matches.unitIsOfTypes(unitsToTest)));
      // now test
      if (max < (currentInTerritory.size() + copyUnitsMoving.size())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setSuicideAttackTargets(final String value) throws GameParseException {
    if (value == null) {
      m_suicideAttackTargets = null;
      return;
    }
    if (m_suicideAttackTargets == null) {
      m_suicideAttackTargets = new HashSet<>();
    }
    final String[] s = value.split(":");
    for (final String u : s) {
      final UnitType ut = getData().getUnitTypeList().getUnitType(u);
      if (ut == null) {
        throw new GameParseException("suicideAttackTargets: no such unit called " + u + thisErrorMsg());
      }
      m_suicideAttackTargets.add(ut);
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setSuicideAttackTargets(final HashSet<UnitType> value) {
    m_suicideAttackTargets = value;
  }

  public HashSet<UnitType> getSuicideAttackTargets() {
    return m_suicideAttackTargets;
  }

  public void clearSuicideAttackTargets() {
    m_suicideAttackTargets.clear();
  }

  public void resetSuicideAttackTargets() {
    m_suicideAttackTargets = null;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setSuicideAttackResources(final String value) throws GameParseException {
    final String[] s = value.split(":");
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

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setSuicideAttackResources(final IntegerMap<Resource> value) {
    m_suicideAttackResources = value;
  }

  public IntegerMap<Resource> getSuicideAttackResources() {
    return m_suicideAttackResources;
  }

  public void clearSuicideAttackResources() {
    m_suicideAttackResources.clear();
  }

  public void resetSuicideAttackResources() {
    m_suicideAttackResources = new IntegerMap<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setVps(final String value) {
    m_vps = getInt(value);
  }

  public int getVps() {
    return m_vps;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCaptureVps(final String value) {
    m_captureVps = getInt(value);
  }

  public int getCaptureVps() {
    return m_captureVps;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRetainCapitalNumber(final String value) {
    m_retainCapitalNumber = getInt(value);
  }

  public int getRetainCapitalNumber() {
    return m_retainCapitalNumber;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRetainCapitalProduceNumber(final String value) {
    m_retainCapitalProduceNumber = getInt(value);
  }

  public int getRetainCapitalProduceNumber() {
    return m_retainCapitalProduceNumber;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setGiveUnitControl(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
      if (tempPlayer != null) {
        m_giveUnitControl.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setGiveUnitControl(final ArrayList<PlayerID> value) {
    m_giveUnitControl = value;
  }

  public ArrayList<PlayerID> getGiveUnitControl() {
    return m_giveUnitControl;
  }


  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setCaptureUnitOnEnteringBy(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
      if (tempPlayer != null) {
        m_captureUnitOnEnteringBy.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setCaptureUnitOnEnteringBy(final ArrayList<PlayerID> value) {
    m_captureUnitOnEnteringBy = value;
  }

  public ArrayList<PlayerID> getCaptureUnitOnEnteringBy() {
    return m_captureUnitOnEnteringBy;
  }


  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setShareTechnology(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
      if (tempPlayer != null) {
        m_shareTechnology.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setShareTechnology(final ArrayList<PlayerID> value) {
    m_shareTechnology = value;
  }

  public ArrayList<PlayerID> getShareTechnology() {
    return m_shareTechnology;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   *
   * @param value
   * @throws GameParseException
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setHelpPayTechCost(final String value) throws GameParseException {
    final String[] temp = value.split(":");
    for (final String name : temp) {
      final PlayerID tempPlayer = getData().getPlayerList().getPlayerID(name);
      if (tempPlayer != null) {
        m_helpPayTechCost.add(tempPlayer);
      } else {
        throw new GameParseException("No player named: " + name + thisErrorMsg());
      }
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setHelpPayTechCost(final ArrayList<PlayerID> value) {
    m_helpPayTechCost = value;
  }

  public ArrayList<PlayerID> getHelpPayTechCost() {
    return m_helpPayTechCost;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDestroysPUs(final String value) {
    m_destroysPUs = getBool(value);
  }

  public boolean getDestroysPUs() {
    return m_destroysPUs;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setImmuneToBlockade(final String value) {
    m_immuneToBlockade = getBool(value);
  }

  public boolean getImmuneToBlockade() {
    return m_immuneToBlockade;
  }

  @Override
  public void validate(final GameData data) throws GameParseException {}
}
