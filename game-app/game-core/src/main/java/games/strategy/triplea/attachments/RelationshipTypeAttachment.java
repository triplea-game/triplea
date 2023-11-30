package games.strategy.triplea.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import lombok.Getter;

/** An attachment for instances of {@link RelationshipType}. */
public class RelationshipTypeAttachment extends DefaultAttachment {
  public static final String ARCHETYPE_NEUTRAL = Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL;
  public static final String ARCHETYPE_WAR = Constants.RELATIONSHIP_ARCHETYPE_WAR;
  public static final String ARCHETYPE_ALLIED = Constants.RELATIONSHIP_ARCHETYPE_ALLIED;
  public static final String UPKEEP_FLAT = "flat";
  public static final String UPKEEP_PERCENTAGE = "percentage";
  public static final String PROPERTY_DEFAULT = Constants.RELATIONSHIP_PROPERTY_DEFAULT;
  public static final String PROPERTY_TRUE = Constants.RELATIONSHIP_PROPERTY_TRUE;
  public static final String PROPERTY_FALSE = Constants.RELATIONSHIP_PROPERTY_FALSE;
  private static final long serialVersionUID = -4367286684249791984L;

  /**
   * -- GETTER -- Returns the ArcheType of this relationshipType, this really shouldn't be called,
   * typically you should call isNeutral, isAllied or isWar().
   */
  @Getter private String archeType = ARCHETYPE_WAR;

  private String canMoveLandUnitsOverOwnedLand = PROPERTY_DEFAULT;
  private String canMoveAirUnitsOverOwnedLand = PROPERTY_DEFAULT;
  private String alliancesCanChainTogether = PROPERTY_DEFAULT;
  private String isDefaultWarPosition = PROPERTY_DEFAULT;
  private String upkeepCost = PROPERTY_DEFAULT;
  private String canLandAirUnitsOnOwnedLand = PROPERTY_DEFAULT;
  private String canTakeOverOwnedTerritory = PROPERTY_DEFAULT;
  private String givesBackOriginalTerritories = PROPERTY_DEFAULT;
  private String canMoveIntoDuringCombatMove = PROPERTY_DEFAULT;
  private String canMoveThroughCanals = PROPERTY_DEFAULT;
  private String rocketsCanFlyOver = PROPERTY_DEFAULT;

  public RelationshipTypeAttachment(
      final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  /**
   * Convenience method.
   *
   * @return RelationshipTypeAttachment belonging to the RelationshipType pr
   */
  public static RelationshipTypeAttachment get(final RelationshipType pr) {
    return get(pr, Constants.RELATIONSHIPTYPE_ATTACHMENT_NAME);
  }

  static RelationshipTypeAttachment get(final RelationshipType pr, final String nameOfAttachment) {
    return getAttachment(pr, nameOfAttachment, RelationshipTypeAttachment.class);
  }

  /**
   * This sets a ArcheType for this relationshipType, there are 3 different archeTypes: War, Allied
   * and Neutral These archeTypes can be accessed by using the constants: WAR_ARCHETYPE,
   * ALLIED_ARCHETYPE, NEUTRAL_ARCHETYPE These archeTypes determine the behavior of isAllied, isWar
   * and isNeutral These archeTypes determine the default behavior of the engine unless you override
   * some option in this attachment; for example the RelationshipType ColdWar could be based on the
   * WAR_ARCHETYPE but overrides options like "canInvade" "canAttackHomeTerritory" to not allow
   * all-out invasion to mimic a not-all-out-war. Or you could base it on NEUTRAL_ARCHETYPE but
   * override the options like "canAttackAtSea" and "canFireAA" to mimic a uneasy peace.
   *
   * @param archeType the template used to base this relationType on, can be war, allied or neutral,
   *     default archeType = WAR_ARCHETYPE
   * @throws GameParseException if archeType isn't set to war, allied or neutral
   */
  public void setArcheType(final String archeType) throws GameParseException {
    final String lowerArcheType = archeType.toLowerCase();
    switch (lowerArcheType) {
      case ARCHETYPE_WAR:
      case ARCHETYPE_ALLIED:
      case ARCHETYPE_NEUTRAL:
        this.archeType = lowerArcheType.intern();
        break;
      default:
        throw new GameParseException(
            "archeType must be "
                + ARCHETYPE_WAR
                + ","
                + ARCHETYPE_ALLIED
                + " or "
                + ARCHETYPE_NEUTRAL
                + " for "
                + thisErrorMsg());
    }
  }

  private void resetArcheType() {
    archeType = ARCHETYPE_WAR;
  }

  /**
   * <strong> EXAMPLE</strong> method on how you could do finegrained authorizations instead of
   * looking at isNeutral, isAllied or isWar(); Just for future reference, doesn't do anything right
   * now.
   *
   * @param canFlyOver should be "true", "false" or "default"
   */
  private void setCanMoveAirUnitsOverOwnedLand(final String canFlyOver) {
    canMoveAirUnitsOverOwnedLand = canFlyOver.intern();
  }

  private String getCanMoveAirUnitsOverOwnedLand() {
    return canMoveAirUnitsOverOwnedLand;
  }

  /**
   * <strong> EXAMPLE</strong> method on how you could do finegrained authorizations instead of
   * looking at isNeutral, isAllied or isWar(); Just for future reference, doesn't do anything right
   * now.
   *
   * @return whether in this relationshipType you can fly over other territories
   */
  public boolean canMoveAirUnitsOverOwnedLand() { // War: true, Allied: True, Neutral: false
    if (canMoveAirUnitsOverOwnedLand.equals(PROPERTY_DEFAULT)) {
      return isWar() || isAllied();
    }
    return canMoveAirUnitsOverOwnedLand.equals(PROPERTY_TRUE);
  }

  private void resetCanMoveAirUnitsOverOwnedLand() {
    canMoveAirUnitsOverOwnedLand = PROPERTY_DEFAULT;
  }

  private void setCanMoveLandUnitsOverOwnedLand(final String canFlyOver) {
    canMoveLandUnitsOverOwnedLand = canFlyOver.intern();
  }

  private String getCanMoveLandUnitsOverOwnedLand() {
    return canMoveLandUnitsOverOwnedLand;
  }

  public boolean canMoveLandUnitsOverOwnedLand() { // War: true, Allied: True, Neutral: false
    if (canMoveLandUnitsOverOwnedLand.equals(PROPERTY_DEFAULT)) {
      return isWar() || isAllied();
    }
    return canMoveLandUnitsOverOwnedLand.equals(PROPERTY_TRUE);
  }

  private void resetCanMoveLandUnitsOverOwnedLand() {
    canMoveLandUnitsOverOwnedLand = PROPERTY_DEFAULT;
  }

  private void setCanLandAirUnitsOnOwnedLand(final String canLandAir) {
    canLandAirUnitsOnOwnedLand = canLandAir.intern();
  }

  private String getCanLandAirUnitsOnOwnedLand() {
    return canLandAirUnitsOnOwnedLand;
  }

  public boolean canLandAirUnitsOnOwnedLand() {
    // War: false, Allied: true, Neutral: false
    if (canLandAirUnitsOnOwnedLand.equals(PROPERTY_DEFAULT)) {
      return isAllied();
    }
    return canLandAirUnitsOnOwnedLand.equals(PROPERTY_TRUE);
  }

  private void resetCanLandAirUnitsOnOwnedLand() {
    canLandAirUnitsOnOwnedLand = PROPERTY_DEFAULT;
  }

  private void setCanTakeOverOwnedTerritory(final String canTakeOver) {
    canTakeOverOwnedTerritory = canTakeOver.intern();
  }

  private String getCanTakeOverOwnedTerritory() {
    return canTakeOverOwnedTerritory;
  }

  public boolean canTakeOverOwnedTerritory() {
    // War: true, Allied: false, Neutral: false
    if (canTakeOverOwnedTerritory.equals(PROPERTY_DEFAULT)) {
      return isWar();
    }
    return canTakeOverOwnedTerritory.equals(PROPERTY_TRUE);
  }

  private void resetCanTakeOverOwnedTerritory() {
    canTakeOverOwnedTerritory = PROPERTY_DEFAULT;
  }

  private void setUpkeepCost(final String integerCost) throws GameParseException {
    if (integerCost.equals(PROPERTY_DEFAULT)) {
      upkeepCost = PROPERTY_DEFAULT;
    } else {
      final String[] s = splitOnColon(integerCost);
      if (s.length < 1 || s.length > 2) {
        throw new GameParseException("upkeepCost must have either 1 or 2 fields" + thisErrorMsg());
      }
      final int cost = getInt(s[0]);
      if (s.length == 2) {
        switch (s[1]) {
          case UPKEEP_FLAT:
            // do nothing
            break;
          case UPKEEP_PERCENTAGE:
            if (cost > 100) {
              throw new GameParseException(
                  "upkeepCost may not have a percentage greater than 100" + thisErrorMsg());
            }
            break;
          default:
            throw new GameParseException(
                "upkeepCost must have either: "
                    + UPKEEP_FLAT
                    + " or "
                    + UPKEEP_PERCENTAGE
                    + thisErrorMsg());
        }
      }
      upkeepCost = integerCost.intern();
    }
  }

  public String getUpkeepCost() {
    if (upkeepCost.equals(PROPERTY_DEFAULT)) {
      return String.valueOf(0);
    }
    return upkeepCost;
  }

  private void resetUpkeepCost() {
    upkeepCost = PROPERTY_DEFAULT;
  }

  private void setAlliancesCanChainTogether(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT)
        || value.equals(PROPERTY_FALSE)
        || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException(
          "alliancesCanChainTogether must be either "
              + PROPERTY_DEFAULT
              + " or "
              + PROPERTY_FALSE
              + " or "
              + PROPERTY_TRUE
              + thisErrorMsg());
    }
    alliancesCanChainTogether = value.intern();
  }

  private String getAlliancesCanChainTogether() {
    return alliancesCanChainTogether;
  }

  public boolean canAlliancesChainTogether() {
    return !alliancesCanChainTogether.equals(PROPERTY_DEFAULT)
        && !isWar()
        && !isNeutral()
        && alliancesCanChainTogether.equals(PROPERTY_TRUE);
  }

  private void resetAlliancesCanChainTogether() {
    alliancesCanChainTogether = PROPERTY_DEFAULT;
  }

  private void setIsDefaultWarPosition(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT)
        || value.equals(PROPERTY_FALSE)
        || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException(
          "isDefaultWarPosition must be either "
              + PROPERTY_DEFAULT
              + " or "
              + PROPERTY_FALSE
              + " or "
              + PROPERTY_TRUE
              + thisErrorMsg());
    }
    isDefaultWarPosition = value.intern();
  }

  private String getIsDefaultWarPosition() {
    return isDefaultWarPosition;
  }

  public boolean isDefaultWarPosition() {
    return !isDefaultWarPosition.equals(PROPERTY_DEFAULT)
        && !isAllied()
        && !isNeutral()
        && isDefaultWarPosition.equals(PROPERTY_TRUE);
  }

  private void resetIsDefaultWarPosition() {
    isDefaultWarPosition = PROPERTY_DEFAULT;
  }

  private void setGivesBackOriginalTerritories(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT)
        || value.equals(PROPERTY_FALSE)
        || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException(
          "givesBackOriginalTerritories must be either "
              + PROPERTY_DEFAULT
              + " or "
              + PROPERTY_FALSE
              + " or "
              + PROPERTY_TRUE
              + thisErrorMsg());
    }
    givesBackOriginalTerritories = value.intern();
  }

  private String getGivesBackOriginalTerritories() {
    return givesBackOriginalTerritories;
  }

  public boolean givesBackOriginalTerritories() {
    return !givesBackOriginalTerritories.equals(PROPERTY_DEFAULT)
        && givesBackOriginalTerritories.equals(PROPERTY_TRUE);
  }

  private void resetGivesBackOriginalTerritories() {
    givesBackOriginalTerritories = PROPERTY_DEFAULT;
  }

  private void setCanMoveIntoDuringCombatMove(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT)
        || value.equals(PROPERTY_FALSE)
        || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException(
          "canMoveIntoDuringCombatMove must be either "
              + PROPERTY_DEFAULT
              + " or "
              + PROPERTY_FALSE
              + " or "
              + PROPERTY_TRUE
              + thisErrorMsg());
    }
    canMoveIntoDuringCombatMove = value.intern();
  }

  private String getCanMoveIntoDuringCombatMove() {
    return canMoveIntoDuringCombatMove;
  }

  public boolean canMoveIntoDuringCombatMove() {
    // this property is not affected by any archetype.
    return canMoveIntoDuringCombatMove.equals(PROPERTY_DEFAULT)
        || canMoveIntoDuringCombatMove.equals(PROPERTY_TRUE);
  }

  private void resetCanMoveIntoDuringCombatMove() {
    canMoveIntoDuringCombatMove = PROPERTY_DEFAULT;
  }

  private void setCanMoveThroughCanals(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT)
        || value.equals(PROPERTY_FALSE)
        || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException(
          "canMoveIntoDuringCombatMove must be either "
              + PROPERTY_DEFAULT
              + " or "
              + PROPERTY_FALSE
              + " or "
              + PROPERTY_TRUE
              + thisErrorMsg());
    }
    canMoveThroughCanals = value.intern();
  }

  private String getCanMoveThroughCanals() {
    return canMoveThroughCanals;
  }

  public boolean canMoveThroughCanals() {
    // only allied can move through canals normally
    if (canMoveThroughCanals.equals(PROPERTY_DEFAULT)) {
      return isAllied();
    }
    return canMoveThroughCanals.equals(PROPERTY_TRUE);
  }

  private void resetCanMoveThroughCanals() {
    canMoveThroughCanals = PROPERTY_DEFAULT;
  }

  private void setRocketsCanFlyOver(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT)
        || value.equals(PROPERTY_FALSE)
        || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException(
          "canMoveIntoDuringCombatMove must be either "
              + PROPERTY_DEFAULT
              + " or "
              + PROPERTY_FALSE
              + " or "
              + PROPERTY_TRUE
              + thisErrorMsg());
    }
    rocketsCanFlyOver = value.intern();
  }

  private String getRocketsCanFlyOver() {
    return rocketsCanFlyOver;
  }

  public boolean canRocketsFlyOver() {
    // rockets can normally fly over everyone.
    return rocketsCanFlyOver.equals(PROPERTY_DEFAULT) || rocketsCanFlyOver.equals(PROPERTY_TRUE);
  }

  private void resetRocketsCanFlyOver() {
    rocketsCanFlyOver = PROPERTY_DEFAULT;
  }

  /** Indicates whether this relationship is based on the WAR_ARCHETYPE. */
  public boolean isWar() {
    return archeType.equals(RelationshipTypeAttachment.ARCHETYPE_WAR);
  }

  /** Indicates whether this relationship is based on the ALLIED_ARCHETYPE. */
  public boolean isAllied() {
    return archeType.equals(RelationshipTypeAttachment.ARCHETYPE_ALLIED);
  }

  /** Indicates whether this relationship is based on the NEUTRAL_ARCHETYPE. */
  public boolean isNeutral() {
    return archeType.equals(RelationshipTypeAttachment.ARCHETYPE_NEUTRAL);
  }

  @Override
  public void validate(final GameState data) {}

  @Override
  public MutableProperty<?> getPropertyOrNull(String propertyName) {
    switch (propertyName) {
      case "archeType":
        return MutableProperty.ofString(
            this::setArcheType, this::getArcheType, this::resetArcheType);
      case "canMoveLandUnitsOverOwnedLand":
        return MutableProperty.ofString(
            this::setCanMoveLandUnitsOverOwnedLand,
            this::getCanMoveLandUnitsOverOwnedLand,
            this::resetCanMoveLandUnitsOverOwnedLand);
      case "canMoveAirUnitsOverOwnedLand":
        return MutableProperty.ofString(
            this::setCanMoveAirUnitsOverOwnedLand,
            this::getCanMoveAirUnitsOverOwnedLand,
            this::resetCanMoveAirUnitsOverOwnedLand);
      case "alliancesCanChainTogether":
        return MutableProperty.ofString(
            this::setAlliancesCanChainTogether,
            this::getAlliancesCanChainTogether,
            this::resetAlliancesCanChainTogether);
      case "isDefaultWarPosition":
        return MutableProperty.ofString(
            this::setIsDefaultWarPosition,
            this::getIsDefaultWarPosition,
            this::resetIsDefaultWarPosition);
      case "upkeepCost":
        return MutableProperty.ofString(
            this::setUpkeepCost, this::getUpkeepCost, this::resetUpkeepCost);
      case "canLandAirUnitsOnOwnedLand":
        return MutableProperty.ofString(
            this::setCanLandAirUnitsOnOwnedLand,
            this::getCanLandAirUnitsOnOwnedLand,
            this::resetCanLandAirUnitsOnOwnedLand);
      case "canTakeOverOwnedTerritory":
        return MutableProperty.ofString(
            this::setCanTakeOverOwnedTerritory,
            this::getCanTakeOverOwnedTerritory,
            this::resetCanTakeOverOwnedTerritory);
      case "givesBackOriginalTerritories":
        return MutableProperty.ofString(
            this::setGivesBackOriginalTerritories,
            this::getGivesBackOriginalTerritories,
            this::resetGivesBackOriginalTerritories);
      case "canMoveIntoDuringCombatMove":
        return MutableProperty.ofString(
            this::setCanMoveIntoDuringCombatMove,
            this::getCanMoveIntoDuringCombatMove,
            this::resetCanMoveIntoDuringCombatMove);
      case "canMoveThroughCanals":
        return MutableProperty.ofString(
            this::setCanMoveThroughCanals,
            this::getCanMoveThroughCanals,
            this::resetCanMoveThroughCanals);
      case "rocketsCanFlyOver":
        return MutableProperty.ofString(
            this::setRocketsCanFlyOver, this::getRocketsCanFlyOver, this::resetRocketsCanFlyOver);
      default:
        return null;
    }
  }
}
