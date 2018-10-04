package games.strategy.triplea.attachments;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.RelationshipType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;

@MapSupport
public class RelationshipTypeAttachment extends DefaultAttachment {
  private static final long serialVersionUID = -4367286684249791984L;

  public static final String ARCHETYPE_NEUTRAL = Constants.RELATIONSHIP_ARCHETYPE_NEUTRAL;
  public static final String ARCHETYPE_WAR = Constants.RELATIONSHIP_ARCHETYPE_WAR;
  public static final String ARCHETYPE_ALLIED = Constants.RELATIONSHIP_ARCHETYPE_ALLIED;
  public static final String UPKEEP_FLAT = "flat";
  public static final String UPKEEP_PERCENTAGE = "percentage";
  public static final String PROPERTY_DEFAULT = Constants.RELATIONSHIP_PROPERTY_DEFAULT;
  public static final String PROPERTY_TRUE = Constants.RELATIONSHIP_PROPERTY_TRUE;
  public static final String PROPERTY_FALSE = Constants.RELATIONSHIP_PROPERTY_FALSE;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_archeType = ARCHETYPE_WAR;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_canMoveLandUnitsOverOwnedLand = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_canMoveAirUnitsOverOwnedLand = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_alliancesCanChainTogether = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_isDefaultWarPosition = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_upkeepCost = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_canLandAirUnitsOnOwnedLand = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_canTakeOverOwnedTerritory = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_givesBackOriginalTerritories = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_canMoveIntoDuringCombatMove = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_canMoveThroughCanals = PROPERTY_DEFAULT;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private String m_rocketsCanFlyOver = PROPERTY_DEFAULT;

  /**
   * Creates new RelationshipTypeAttachment.
   */
  public RelationshipTypeAttachment(final String name, final Attachable attachable, final GameData gameData) {
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
   * This sets a ArcheType for this relationshipType, there are 3 different archeTypes: War, Allied and Neutral
   * These archeTypes can be accessed by using the constants: WAR_ARCHETYPE, ALLIED_ARCHETYPE, NEUTRAL_ARCHETYPE
   * These archeTypes determine the behavior of isAllied, isWar and isNeutral
   * These archeTyps determine the default behavior of the engine unless you override some option in this attachment;
   * for example the RelationshipType ColdWar could be based on the WAR_ARCHETYPE but overrides options like "canInvade"
   * "canAttackHomeTerritory"
   * to not allow all-out invasion to mimic a not-all-out-war.
   * Or you could base it on NEUTRAL_ARCHETYPE but override the options like "canAttackAtSea" and "canFireAA" to mimic a
   * uneasy peace.
   *
   * @param archeType the template used to base this relationType on, can be war, allied or neutral, default archeType =
   *        WAR_ARCHETYPE
   * @throws GameParseException
   *         if archeType isn't set to war, allied or neutral
   */
  public void setArcheType(final String archeType) throws GameParseException {
    final String lowerArcheType = archeType.toLowerCase();
    switch (lowerArcheType) {
      case ARCHETYPE_WAR:
      case ARCHETYPE_ALLIED:
      case ARCHETYPE_NEUTRAL:
        m_archeType = lowerArcheType;
        break;
      default:
        throw new GameParseException("archeType must be " + ARCHETYPE_WAR + "," + ARCHETYPE_ALLIED + " or "
            + ARCHETYPE_NEUTRAL + " for " + thisErrorMsg());
    }
  }

  /**
   * Returns the ArcheType of this relationshipType, this really shouldn't be called, typically you should call
   * isNeutral, isAllied or isWar().
   */
  public String getArcheType() {
    return m_archeType;
  }

  private void resetArcheType() {
    m_archeType = ARCHETYPE_WAR;
  }

  /**
   * <strong> EXAMPLE</strong> method on how you could do finegrained authorizations instead of looking at isNeutral,
   * isAllied or isWar();
   * Just for future reference, doesn't do anything right now.
   *
   * @param canFlyOver should be "true", "false" or "default"
   */
  private void setCanMoveAirUnitsOverOwnedLand(final String canFlyOver) {
    m_canMoveAirUnitsOverOwnedLand = canFlyOver;
  }

  private String getCanMoveAirUnitsOverOwnedLand() {
    return m_canMoveAirUnitsOverOwnedLand;
  }

  /**
   * <strong> EXAMPLE</strong> method on how you could do finegrained authorizations instead of looking at isNeutral,
   * isAllied or isWar();
   * Just for future reference, doesn't do anything right now.
   *
   * @return whether in this relationshipType you can fly over other territories
   */
  public boolean canMoveAirUnitsOverOwnedLand() { // War: true, Allied: True, Neutral: false
    if (m_canMoveAirUnitsOverOwnedLand.equals(PROPERTY_DEFAULT)) {
      return isWar() || isAllied();
    }
    return m_canMoveAirUnitsOverOwnedLand.equals(PROPERTY_TRUE);
  }

  private void resetCanMoveAirUnitsOverOwnedLand() {
    m_canMoveAirUnitsOverOwnedLand = PROPERTY_DEFAULT;
  }

  private void setCanMoveLandUnitsOverOwnedLand(final String canFlyOver) {
    m_canMoveLandUnitsOverOwnedLand = canFlyOver;
  }

  private String getCanMoveLandUnitsOverOwnedLand() {
    return m_canMoveLandUnitsOverOwnedLand;
  }

  public boolean canMoveLandUnitsOverOwnedLand() { // War: true, Allied: True, Neutral: false
    if (m_canMoveLandUnitsOverOwnedLand.equals(PROPERTY_DEFAULT)) {
      return isWar() || isAllied();
    }
    return m_canMoveLandUnitsOverOwnedLand.equals(PROPERTY_TRUE);
  }

  private void resetCanMoveLandUnitsOverOwnedLand() {
    m_canMoveLandUnitsOverOwnedLand = PROPERTY_DEFAULT;
  }

  private void setCanLandAirUnitsOnOwnedLand(final String canLandAir) {
    m_canLandAirUnitsOnOwnedLand = canLandAir;
  }

  private String getCanLandAirUnitsOnOwnedLand() {
    return m_canLandAirUnitsOnOwnedLand;
  }

  public boolean canLandAirUnitsOnOwnedLand() {
    // War: false, Allied: true, Neutral: false
    if (m_canLandAirUnitsOnOwnedLand.equals(PROPERTY_DEFAULT)) {
      return isAllied();
    }
    return m_canLandAirUnitsOnOwnedLand.equals(PROPERTY_TRUE);
  }

  private void resetCanLandAirUnitsOnOwnedLand() {
    m_canLandAirUnitsOnOwnedLand = PROPERTY_DEFAULT;
  }

  private void setCanTakeOverOwnedTerritory(final String canTakeOver) {
    m_canTakeOverOwnedTerritory = canTakeOver;
  }

  private String getCanTakeOverOwnedTerritory() {
    return m_canTakeOverOwnedTerritory;
  }

  public boolean canTakeOverOwnedTerritory() {
    // War: true, Allied: false, Neutral: false
    if (m_canTakeOverOwnedTerritory.equals(PROPERTY_DEFAULT)) {
      return isWar();
    }
    return m_canTakeOverOwnedTerritory.equals(PROPERTY_TRUE);
  }

  private void resetCanTakeOverOwnedTerritory() {
    m_canTakeOverOwnedTerritory = PROPERTY_DEFAULT;
  }

  private void setUpkeepCost(final String integerCost) throws GameParseException {
    if (integerCost.equals(PROPERTY_DEFAULT)) {
      m_upkeepCost = PROPERTY_DEFAULT;
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
              throw new GameParseException("upkeepCost may not have a percentage greater than 100" + thisErrorMsg());
            }
            break;
          default:
            throw new GameParseException(
                "upkeepCost must have either: " + UPKEEP_FLAT + " or " + UPKEEP_PERCENTAGE + thisErrorMsg());
        }
      }
      m_upkeepCost = integerCost;
    }
  }

  public String getUpkeepCost() {
    if (m_upkeepCost.equals(PROPERTY_DEFAULT)) {
      return String.valueOf(0);
    }
    return m_upkeepCost;
  }

  private void resetUpkeepCost() {
    m_upkeepCost = PROPERTY_DEFAULT;
  }

  private void setAlliancesCanChainTogether(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException("alliancesCanChainTogether must be either " + PROPERTY_DEFAULT + " or "
          + PROPERTY_FALSE + " or " + PROPERTY_TRUE + thisErrorMsg());
    }
    m_alliancesCanChainTogether = value;
  }

  private String getAlliancesCanChainTogether() {
    return m_alliancesCanChainTogether;
  }

  public boolean canAlliancesChainTogether() {
    return !m_alliancesCanChainTogether.equals(PROPERTY_DEFAULT)
        && !isWar()
        && !isNeutral()
        && m_alliancesCanChainTogether.equals(PROPERTY_TRUE);
  }

  private void resetAlliancesCanChainTogether() {
    m_alliancesCanChainTogether = PROPERTY_DEFAULT;
  }

  private void setIsDefaultWarPosition(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException("isDefaultWarPosition must be either " + PROPERTY_DEFAULT + " or " + PROPERTY_FALSE
          + " or " + PROPERTY_TRUE + thisErrorMsg());
    }
    m_isDefaultWarPosition = value;
  }

  private String getIsDefaultWarPosition() {
    return m_isDefaultWarPosition;
  }

  public boolean isDefaultWarPosition() {
    return !m_isDefaultWarPosition.equals(PROPERTY_DEFAULT)
        && !isAllied()
        && !isNeutral()
        && m_isDefaultWarPosition.equals(PROPERTY_TRUE);
  }

  private void resetIsDefaultWarPosition() {
    m_isDefaultWarPosition = PROPERTY_DEFAULT;
  }

  private void setGivesBackOriginalTerritories(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException("givesBackOriginalTerritories must be either " + PROPERTY_DEFAULT + " or "
          + PROPERTY_FALSE + " or " + PROPERTY_TRUE + thisErrorMsg());
    }
    m_givesBackOriginalTerritories = value;
  }

  private String getGivesBackOriginalTerritories() {
    return m_givesBackOriginalTerritories;
  }

  public boolean givesBackOriginalTerritories() {
    return !m_givesBackOriginalTerritories.equals(PROPERTY_DEFAULT)
        && m_givesBackOriginalTerritories.equals(PROPERTY_TRUE);
  }

  private void resetGivesBackOriginalTerritories() {
    m_givesBackOriginalTerritories = PROPERTY_DEFAULT;
  }

  private void setCanMoveIntoDuringCombatMove(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException("canMoveIntoDuringCombatMove must be either " + PROPERTY_DEFAULT + " or "
          + PROPERTY_FALSE + " or " + PROPERTY_TRUE + thisErrorMsg());
    }
    m_canMoveIntoDuringCombatMove = value;
  }

  private String getCanMoveIntoDuringCombatMove() {
    return m_canMoveIntoDuringCombatMove;
  }

  public boolean canMoveIntoDuringCombatMove() {
    // this property is not affected by any archetype.
    return m_canMoveIntoDuringCombatMove.equals(PROPERTY_DEFAULT)
        || m_canMoveIntoDuringCombatMove.equals(PROPERTY_TRUE);
  }

  private void resetCanMoveIntoDuringCombatMove() {
    m_canMoveIntoDuringCombatMove = PROPERTY_DEFAULT;
  }

  private void setCanMoveThroughCanals(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException("canMoveIntoDuringCombatMove must be either " + PROPERTY_DEFAULT + " or "
          + PROPERTY_FALSE + " or " + PROPERTY_TRUE + thisErrorMsg());
    }
    m_canMoveThroughCanals = value;
  }

  private String getCanMoveThroughCanals() {
    return m_canMoveThroughCanals;
  }

  public boolean canMoveThroughCanals() {
    // only allied can move through canals normally
    if (m_canMoveThroughCanals.equals(PROPERTY_DEFAULT)) {
      return isAllied();
    }
    return m_canMoveThroughCanals.equals(PROPERTY_TRUE);
  }

  private void resetCanMoveThroughCanals() {
    m_canMoveThroughCanals = PROPERTY_DEFAULT;
  }

  private void setRocketsCanFlyOver(final String value) throws GameParseException {
    if (!(value.equals(PROPERTY_DEFAULT) || value.equals(PROPERTY_FALSE) || value.equals(PROPERTY_TRUE))) {
      throw new GameParseException("canMoveIntoDuringCombatMove must be either " + PROPERTY_DEFAULT + " or "
          + PROPERTY_FALSE + " or " + PROPERTY_TRUE + thisErrorMsg());
    }
    m_rocketsCanFlyOver = value;
  }

  private String getRocketsCanFlyOver() {
    return m_rocketsCanFlyOver;
  }

  public boolean canRocketsFlyOver() {
    // rockets can normally fly over everyone.
    return m_rocketsCanFlyOver.equals(PROPERTY_DEFAULT) || m_rocketsCanFlyOver.equals(PROPERTY_TRUE);
  }

  private void resetRocketsCanFlyOver() {
    m_rocketsCanFlyOver = PROPERTY_DEFAULT;
  }

  /**
   * Indicates whether this relationship is based on the WAR_ARCHETYPE.
   */
  public boolean isWar() {
    return m_archeType.equals(RelationshipTypeAttachment.ARCHETYPE_WAR);
  }

  /**
   * Indicates whether this relationship is based on the ALLIED_ARCHETYPE.
   */
  public boolean isAllied() {
    return m_archeType.equals(RelationshipTypeAttachment.ARCHETYPE_ALLIED);
  }

  /**
   * Indicates whether this relationship is based on the NEUTRAL_ARCHETYPE.
   */
  public boolean isNeutral() {
    return m_archeType.equals(RelationshipTypeAttachment.ARCHETYPE_NEUTRAL);
  }

  @Override
  public void validate(final GameData data) {}

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .put("archeType",
            MutableProperty.ofString(
                this::setArcheType,
                this::getArcheType,
                this::resetArcheType))
        .put("canMoveLandUnitsOverOwnedLand",
            MutableProperty.ofString(
                this::setCanMoveLandUnitsOverOwnedLand,
                this::getCanMoveLandUnitsOverOwnedLand,
                this::resetCanMoveLandUnitsOverOwnedLand))
        .put("canMoveAirUnitsOverOwnedLand",
            MutableProperty.ofString(
                this::setCanMoveAirUnitsOverOwnedLand,
                this::getCanMoveAirUnitsOverOwnedLand,
                this::resetCanMoveAirUnitsOverOwnedLand))
        .put("alliancesCanChainTogether",
            MutableProperty.ofString(
                this::setAlliancesCanChainTogether,
                this::getAlliancesCanChainTogether,
                this::resetAlliancesCanChainTogether))
        .put("isDefaultWarPosition",
            MutableProperty.ofString(
                this::setIsDefaultWarPosition,
                this::getIsDefaultWarPosition,
                this::resetIsDefaultWarPosition))
        .put("upkeepCost",
            MutableProperty.ofString(
                this::setUpkeepCost,
                this::getUpkeepCost,
                this::resetUpkeepCost))
        .put("canLandAirUnitsOnOwnedLand",
            MutableProperty.ofString(
                this::setCanLandAirUnitsOnOwnedLand,
                this::getCanLandAirUnitsOnOwnedLand,
                this::resetCanLandAirUnitsOnOwnedLand))
        .put("canTakeOverOwnedTerritory",
            MutableProperty.ofString(
                this::setCanTakeOverOwnedTerritory,
                this::getCanTakeOverOwnedTerritory,
                this::resetCanTakeOverOwnedTerritory))
        .put("givesBackOriginalTerritories",
            MutableProperty.ofString(
                this::setGivesBackOriginalTerritories,
                this::getGivesBackOriginalTerritories,
                this::resetGivesBackOriginalTerritories))
        .put("canMoveIntoDuringCombatMove",
            MutableProperty.ofString(
                this::setCanMoveIntoDuringCombatMove,
                this::getCanMoveIntoDuringCombatMove,
                this::resetCanMoveIntoDuringCombatMove))
        .put("canMoveThroughCanals",
            MutableProperty.ofString(
                this::setCanMoveThroughCanals,
                this::getCanMoveThroughCanals,
                this::resetCanMoveThroughCanals))
        .put("rocketsCanFlyOver",
            MutableProperty.ofString(
                this::setRocketsCanFlyOver,
                this::getRocketsCanFlyOver,
                this::resetRocketsCanFlyOver))
        .build();
  }
}
