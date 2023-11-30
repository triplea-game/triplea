package games.strategy.triplea.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.Getter;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Triple;

/**
 * An attachment for instances of {@link GamePlayer} that defines properties unrelated to rules (see
 * the class description of {@link AbstractPlayerRulesAttachment}). Note: Empty collection fields
 * default to null to minimize memory use and serialization size.
 */
public class PlayerAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 1880755875866426270L;

  @Getter private int vps = 0;
  // need to store some data during a turn
  @Getter private int captureVps = 0;
  // number of capitals needed before we lose all our money
  @Getter private int retainCapitalNumber = 1;
  // number of capitals needed before we lose ability to gain money and produce units
  private int retainCapitalProduceNumber = 1;
  private @Nullable List<GamePlayer> giveUnitControl = null;
  private boolean giveUnitControlInAllTerritories = false;
  private @Nullable List<GamePlayer> captureUnitOnEnteringBy = null;
  // gives any technology researched to this player automatically
  private @Nullable List<GamePlayer> shareTechnology = null;
  // allows these players to help pay for technology
  private @Nullable List<GamePlayer> helpPayTechCost = null;
  // do we lose our money and have it disappear or is that money captured?
  private boolean destroysPus = false;
  // are we immune to being blockaded?
  private boolean immuneToBlockade = false;
  // what resources can be used for suicide attacks, and at what attack power
  private @Nullable IntegerMap<Resource> suicideAttackResources = null;
  // what can be hit by suicide attacks
  private @Nullable Set<UnitType> suicideAttackTargets = null;

  // placement limits on a flexible per player basis
  private @Nullable Set<Triple<Integer, String, Set<UnitType>>> placementLimit = null;

  // movement limits on a flexible per player basis
  private @Nullable Set<Triple<Integer, String, Set<UnitType>>> movementLimit = null;

  // attacking limits on a flexible per player basis
  private @Nullable Set<Triple<Integer, String, Set<UnitType>>> attackingLimit = null;

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
    if (placementLimit == null) {
      placementLimit = new HashSet<>();
    }
    placementLimit.add(parseUnitLimit("placementLimit", value));
  }

  private void setPlacementLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    placementLimit = value;
  }

  public Set<Triple<Integer, String, Set<UnitType>>> getPlacementLimit() {
    return getSetProperty(placementLimit);
  }

  private void resetPlacementLimit() {
    placementLimit = null;
  }

  private void setMovementLimit(final String value) throws GameParseException {
    if (movementLimit == null) {
      movementLimit = new HashSet<>();
    }
    movementLimit.add(parseUnitLimit("movementLimit", value));
  }

  private void setMovementLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    movementLimit = value;
  }

  public Set<Triple<Integer, String, Set<UnitType>>> getMovementLimit() {
    return getSetProperty(movementLimit);
  }

  private void resetMovementLimit() {
    movementLimit = null;
  }

  private void setAttackingLimit(final String value) throws GameParseException {
    if (attackingLimit == null) {
      attackingLimit = new HashSet<>();
    }
    attackingLimit.add(parseUnitLimit("attackingLimit", value));
  }

  private void setAttackingLimit(final Set<Triple<Integer, String, Set<UnitType>>> value) {
    attackingLimit = value;
  }

  public Set<Triple<Integer, String, Set<UnitType>>> getAttackingLimit() {
    return getSetProperty(attackingLimit);
  }

  private void resetAttackingLimit() {
    attackingLimit = null;
  }

  private Triple<Integer, String, Set<UnitType>> parseUnitLimit(
      final String type, final String value) throws GameParseException {
    final String[] s = splitOnColon(value);
    if (s.length < 3) {
      throw new GameParseException(
          type + " must have 3 parts: count, type, unit list" + thisErrorMsg());
    }
    final int max = getInt(s[0]);
    if (max < 0) {
      throw new GameParseException(type + " count must have a positive number" + thisErrorMsg());
    }
    if (!(s[1].equals("owned") || s[1].equals("allied") || s[1].equals("total"))) {
      throw new GameParseException(
          type + " type must be: owned, allied, or total" + thisErrorMsg());
    }
    final Set<UnitType> types = new HashSet<>();
    if (s[2].equalsIgnoreCase("all")) {
      types.addAll(getData().getUnitTypeList().getAllUnitTypes());
    } else {
      for (int i = 2; i < s.length; i++) {
        types.add(getUnitTypeOrThrow(s[i]));
      }
    }
    return Triple.of(max, s[1].intern(), types);
  }

  private void setSuicideAttackTargets(final String value) throws GameParseException {
    suicideAttackTargets = parseUnitTypes("suicideAttackTargets", value, suicideAttackTargets);
  }

  private void setSuicideAttackTargets(final Set<UnitType> value) {
    suicideAttackTargets = value;
  }

  public Set<UnitType> getSuicideAttackTargets() {
    return getSetProperty(suicideAttackTargets);
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
    if (suicideAttackResources == null) {
      suicideAttackResources = new IntegerMap<>();
    }
    suicideAttackResources.put(r, attackValue);
  }

  private void setSuicideAttackResources(final IntegerMap<Resource> value) {
    suicideAttackResources = value;
  }

  public IntegerMap<Resource> getSuicideAttackResources() {
    return getIntegerMapProperty(suicideAttackResources);
  }

  private void resetSuicideAttackResources() {
    suicideAttackResources = null;
  }

  private void setVps(final int value) {
    vps = value;
  }

  private void setCaptureVps(final String value) {
    captureVps = getInt(value);
  }

  private void setCaptureVps(final Integer value) {
    captureVps = value;
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
    giveUnitControl = parsePlayerList(value, giveUnitControl);
  }

  private void setGiveUnitControl(final List<GamePlayer> value) {
    giveUnitControl = value;
  }

  public List<GamePlayer> getGiveUnitControl() {
    return getListProperty(giveUnitControl);
  }

  private void resetGiveUnitControl() {
    giveUnitControl = null;
  }

  private void setGiveUnitControlInAllTerritories(final boolean value) {
    giveUnitControlInAllTerritories = value;
  }

  public boolean getGiveUnitControlInAllTerritories() {
    return giveUnitControlInAllTerritories;
  }

  private void setCaptureUnitOnEnteringBy(final String value) throws GameParseException {
    captureUnitOnEnteringBy = parsePlayerList(value, captureUnitOnEnteringBy);
  }

  private void setCaptureUnitOnEnteringBy(final List<GamePlayer> value) {
    captureUnitOnEnteringBy = value;
  }

  public List<GamePlayer> getCaptureUnitOnEnteringBy() {
    return getListProperty(captureUnitOnEnteringBy);
  }

  private void resetCaptureUnitOnEnteringBy() {
    captureUnitOnEnteringBy = null;
  }

  private void setShareTechnology(final String value) throws GameParseException {
    shareTechnology = parsePlayerList(value, shareTechnology);
  }

  private void setShareTechnology(final List<GamePlayer> value) {
    shareTechnology = value;
  }

  public List<GamePlayer> getShareTechnology() {
    return getListProperty(shareTechnology);
  }

  private void resetShareTechnology() {
    shareTechnology = null;
  }

  private void setHelpPayTechCost(final String value) throws GameParseException {
    helpPayTechCost = parsePlayerList(value, helpPayTechCost);
  }

  private void setHelpPayTechCost(final List<GamePlayer> value) {
    helpPayTechCost = value;
  }

  public List<GamePlayer> getHelpPayTechCost() {
    return getListProperty(helpPayTechCost);
  }

  private void resetHelpPayTechCost() {
    helpPayTechCost = null;
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
  public void validate(final GameState data) {}

  @Override
  public MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "vps":
        return MutableProperty.ofMapper(
            DefaultAttachment::getInt, this::setVps, this::getVps, () -> 0);
      case "captureVps":
        return MutableProperty.of(
            this::setCaptureVps, this::setCaptureVps, this::getCaptureVps, this::resetCaptureVps);
      case "retainCapitalNumber":
        return MutableProperty.of(
            this::setRetainCapitalNumber,
            this::setRetainCapitalNumber,
            this::getRetainCapitalNumber,
            this::resetRetainCapitalNumber);
      case "retainCapitalProduceNumber":
        return MutableProperty.of(
            this::setRetainCapitalProduceNumber,
            this::setRetainCapitalProduceNumber,
            this::getRetainCapitalProduceNumber,
            this::resetRetainCapitalProduceNumber);
      case "giveUnitControl":
        return MutableProperty.of(
            this::setGiveUnitControl,
            this::setGiveUnitControl,
            this::getGiveUnitControl,
            this::resetGiveUnitControl);
      case "giveUnitControlInAllTerritories":
        return MutableProperty.ofMapper(
            DefaultAttachment::getBool,
            this::setGiveUnitControlInAllTerritories,
            this::getGiveUnitControlInAllTerritories,
            () -> false);
      case "captureUnitOnEnteringBy":
        return MutableProperty.of(
            this::setCaptureUnitOnEnteringBy,
            this::setCaptureUnitOnEnteringBy,
            this::getCaptureUnitOnEnteringBy,
            this::resetCaptureUnitOnEnteringBy);
      case "shareTechnology":
        return MutableProperty.of(
            this::setShareTechnology,
            this::setShareTechnology,
            this::getShareTechnology,
            this::resetShareTechnology);
      case "helpPayTechCost":
        return MutableProperty.of(
            this::setHelpPayTechCost,
            this::setHelpPayTechCost,
            this::getHelpPayTechCost,
            this::resetHelpPayTechCost);
      case "destroysPUs":
        return MutableProperty.of(
            this::setDestroysPUs,
            this::setDestroysPUs,
            this::getDestroysPUs,
            this::resetDestroysPUs);
      case "immuneToBlockade":
        return MutableProperty.of(
            this::setImmuneToBlockade,
            this::setImmuneToBlockade,
            this::getImmuneToBlockade,
            this::resetImmuneToBlockade);
      case "suicideAttackResources":
        return MutableProperty.of(
            this::setSuicideAttackResources,
            this::setSuicideAttackResources,
            this::getSuicideAttackResources,
            this::resetSuicideAttackResources);
      case "suicideAttackTargets":
        return MutableProperty.of(
            this::setSuicideAttackTargets,
            this::setSuicideAttackTargets,
            this::getSuicideAttackTargets,
            this::resetSuicideAttackTargets);
      case "placementLimit":
        return MutableProperty.of(
            this::setPlacementLimit,
            this::setPlacementLimit,
            this::getPlacementLimit,
            this::resetPlacementLimit);
      case "movementLimit":
        return MutableProperty.of(
            this::setMovementLimit,
            this::setMovementLimit,
            this::getMovementLimit,
            this::resetMovementLimit);
      case "attackingLimit":
        return MutableProperty.of(
            this::setAttackingLimit,
            this::setAttackingLimit,
            this::getAttackingLimit,
            this::resetAttackingLimit);
      default:
        return null;
    }
  }
}
