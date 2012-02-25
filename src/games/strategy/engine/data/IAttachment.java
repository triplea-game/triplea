/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * Attachment.java
 * 
 * Created on November 8, 2001, 3:09 PM
 */
package games.strategy.engine.data;

import java.io.Serializable;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public interface IAttachment extends Serializable
{
	/** each implementing class NEEDS to have such an constructor, otherwise the parsing in GameParser won't work */
	@SuppressWarnings("rawtypes")
	Class[] attachmentConstructorParameter = new Class[] { String.class, Attachable.class, GameData.class };
	
	public void setData(GameData m_data);
	
	/**
	 * Called after ALL attachments are created. IF an error occurs should throw an exception to halt the parsing.
	 * 
	 * @param data
	 *            game data
	 * @throws GameParseException
	 *             an error has occurred while validation
	 */
	public void validate(GameData data) throws GameParseException;
	
	public Attachable getAttachedTo();
	
	public void setAttachedTo(Attachable attachable);
	
	public String getName();
	
	public void setName(String aString);
	
	/* SLIDINGTILES
	// Tile attachment
	final static String PROPERTY_SLIDINGTILES_Tile_value = "value"; */

	/* KINGSTABLE
	// PlayerAttachment
	final static String PROPERTY_KINGSTABLE_NEEDS_KING = "needsKing";
	static final String PROPERTY_KINGSTABLE_ALPHA_BETA_SEARCH_DEPTH = "alphaBetaSearchDepth";
	// TerritoryAttachment
	static final String PROPERTY_KINGSTABLE_KINGS_EXIT = "kingsExit";
	static final String PROPERTY_KINGSTABLE_KINGS_SQUARE = "kingsSquare"; */

	/* TRIPLEA
	// PlayerAttachment
	static final String PROPERTY_PlayerAttachment_DESTROY_PUS = "destroysPUs";
	static final String PROPERTY_PlayerAttachment_CAPTURE_UNIT_ON_ENTERING_BY = "captureUnitOnEnteringBy";
	static final String PROPERTY_PlayerAttachment_CAPTURE_VPS = "captureVps";
	static final String PROPERTY_PlayerAttachment_GIVE_UNIT_CONTROL = "giveUnitControl";
	static final String PROPERTY_PlayerAttachment_RETAIN_CAPITAL_NUMBER = "retainCapitalNumber";
	static final String PROPERTY_PlayerAttachment_RETAIN_CAPITAL_PRODUCE_NUMBER = "retainCapitalProduceNumber";
	static final String PROPERTY_PlayerAttachment_TAKE_UNIT_CONTROL = "takeUnitControl"; // kept for backwards compatibility to avoid giving users java errors. does nothing.
	static final String PROPERTY_PlayerAttachment_SUICIDE_ATTACK_RESOURCES = "suicideAttackResources";
	static final String PROPERTY_PlayerAttachment_SUICIDE_ATTACK_TARGETS = "suicideAttackTargets"; // just added this
	static final String PROPERTY_PlayerAttachment_VPS = "vps";
	static final String PROPERTY_PlayerAttachment_STACKING_LIMIT = "stackingLimit"; // just added this 2
	
	// TechAttachment
	static final String PROPERTY_TechAttachment_AA_RADAR = "aARadar";
	static final String PROPERTY_TechAttachment_DESTROYER_BOMBARD = "destroyerBombard";
	static final String PROPERTY_TechAttachment_HEAVY_BOMBER = "heavyBomber";
	static final String PROPERTY_TechAttachment_IMPROVED_ARTILLERY_SUPPORT = "improvedArtillerySupport";
	static final String PROPERTY_TechAttachment_INCREASED_FACTORY_PRODUCTION = "increasedFactoryProduction";
	static final String PROPERTY_TechAttachment_INDUSTRIAL_TECHNOLOGY = "industrialTechnology";
	static final String PROPERTY_TechAttachment_JET_POWER = "jetPower";
	static final String PROPERTY_TechAttachment_LONG_RANGE_AIR = "longRangeAir";
	static final String PROPERTY_TechAttachment_MECHANIZED_INFANTRY = "mechanizedInfantry";
	static final String PROPERTY_TechAttachment_PARATROOPERS = "paratroopers";
	static final String PROPERTY_TechAttachment_ROCKET = "rocket";
	static final String PROPERTY_TechAttachment_SHIPYARDS = "shipyards";
	static final String PROPERTY_TechAttachment_SUPERSUB = "superSub";
	static final String PROPERTY_TechAttachment_TECHCOST = "techCost";
	static final String PROPERTY_TechAttachment_WARBONDS = "warBonds";
	
	// TerritoryAttachment
	// FYI: "originalOwner" does not belong in IAttachment as the user can not set it as a territory attachment property
	static final String PROPERTY_TerritoryAttachment_AIR_BASE = "airBase";
	static final String PROPERTY_TerritoryAttachment_BLOCKADE_ZONE = "blockadeZone";
	static final String PROPERTY_TerritoryAttachment_CAPITAL = "capital";
	static final String PROPERTY_TerritoryAttachment_CAPTURE_UNIT_ON_ENTERING_BY = "captureUnitOnEnteringBy";
	static final String PROPERTY_TerritoryAttachment_CHANGE_UNIT_OWNERS = "changeUnitOwners";
	static final String PROPERTY_TerritoryAttachment_CONVOY_ATTACHED = "convoyAttached";
	static final String PROPERTY_TerritoryAttachment_CONVOY_ROUTE = "convoyRoute";
	static final String PROPERTY_TerritoryAttachment_IS_IMPASSIBLE = "isImpassible";
	static final String PROPERTY_TerritoryAttachment_KAMIKAZE_ZONE = "kamikazeZone";
	static final String PROPERTY_TerritoryAttachment_NAVAL_BASE = "navalBase";
	static final String PROPERTY_TerritoryAttachment_OCCUPIED_TERR_OF = "occupiedTerrOf";
	static final String PROPERTY_TerritoryAttachment_ORIGINAL_FACTORY = "originalFactory";
	static final String PROPERTY_TerritoryAttachment_PRODUCTION = "production";
	static final String PROPERTY_TerritoryAttachment_PRODUCTION_ONLY = "productionOnly";
	static final String PROPERTY_TerritoryAttachment_RESOURCES = "resources";
	static final String PROPERTY_TerritoryAttachment_TERRITORY_EFFECT = "territoryEffect";
	static final String PROPERTY_TerritoryAttachment_UNIT_PRODUCTION = "unitProduction";
	static final String PROPERTY_TerritoryAttachment_VICTORY_CITY = "victoryCity";
	static final String PROPERTY_TerritoryAttachment_WHEN_CAPTURED_BY_GOES_TO = "whenCapturedByGoesTo";
	
	// TerritoryEffectAttachment
	static final String PROPERTY_TerritoryEffectAttachment_COMBAT_DEFENSE_EFFECT = "combatDefenseEffect";
	static final String PROPERTY_TerritoryEffectAttachment_COMBAT_OFFENSE_EFFECT = "combatOffenseEffect";
	static final String PROPERTY_TerritoryEffectAttachment_NO_BLITZ = "noBlitz";
	
	// UnitAttachment
	static final String PROPERTY_UnitAttachment_AIR_ATTACK = "airAttack";
	static final String PROPERTY_UnitAttachment_AIR_DEFENSE = "airDefense";
	static final String PROPERTY_UnitAttachment_ARTILLERY = "artillery";
	static final String PROPERTY_UnitAttachment_ARTILLERY_SUPPORTABLE = "artillerySupportable";
	static final String PROPERTY_UnitAttachment_ATTACK = "attack";
	static final String PROPERTY_UnitAttachment_ATTACK_AA = "attackAA";
	static final String PROPERTY_UnitAttachment_ATTACK_AA_MAX_DIE_SIDES = "attackAAmaxDieSides";
	static final String PROPERTY_UnitAttachment_BLOCKADE = "blockade";
	static final String PROPERTY_UnitAttachment_BOMBARD = "bombard";
	static final String PROPERTY_UnitAttachment_BOMBING_BONUS = "bombingBonus";
	static final String PROPERTY_UnitAttachment_BOMBING_MAX_DIE_SIDES = "bombingMaxDieSides";
	static final String PROPERTY_UnitAttachment_CAN_BE_CAPTURED_ON_ENTERING_BY = "canBeCapturedOnEnteringBy";
	static final String PROPERTY_UnitAttachment_CAN_BE_DAMAGED = "canBeDamaged";
	static final String PROPERTY_UnitAttachment_CAN_BE_GIVEN_BY_TERRITORY_TO = "canBeGivenByTerritoryTo";
	static final String PROPERTY_UnitAttachment_CAN_BLITZ = "canBlitz";
	static final String PROPERTY_UnitAttachment_CAN_BOMBARD = "canBombard";
	static final String PROPERTY_UnitAttachment_CAN_DIE_FROM_REACHING_MAX_DAMAGE = "canDieFromReachingMaxDamage";
	static final String PROPERTY_UnitAttachment_CAN_ESCORT = "canEscort";
	static final String PROPERTY_UnitAttachment_CAN_INTERCEPT = "canIntercept";
	static final String PROPERTY_UnitAttachment_CAN_INVADE_ONLY_FROM = "canInvadeOnlyFrom";
	static final String PROPERTY_UnitAttachment_CAN_NOT_MOVE_DURING_COMBAT_MOVE = "canNotMoveDuringCombatMove"; // just added this
	static final String PROPERTY_UnitAttachment_CAN_ONLY_BE_PLACED_IN_TERRITORY_VALUED_AT_X = "canOnlyBePlacedInTerritoryValuedAtX";
	static final String PROPERTY_UnitAttachment_CAN_PRODUCE_UNITS = "canProduceUnits";
	static final String PROPERTY_UnitAttachment_CAN_PRODUCE_X_UNITS = "canProduceXUnits";
	static final String PROPERTY_UnitAttachment_CAN_SCRAMBLE = "canScramble";
	static final String PROPERTY_UnitAttachment_CARRIER_CAPACITY = "carrierCapacity";
	static final String PROPERTY_UnitAttachment_CARRIER_COST = "carrierCost";
	static final String PROPERTY_UnitAttachment_CONSTRUCTIONS_PER_TERR_PER_TYPE_PER_TURN = "constructionsPerTerrPerTypePerTurn";
	static final String PROPERTY_UnitAttachment_CONSTRUCTION_TYPE = "constructionType";
	static final String PROPERTY_UnitAttachment_CONSUMES_UNITS = "consumesUnits";
	static final String PROPERTY_UnitAttachment_CREATES_RESOURCES_LIST = "createsResourcesList";
	static final String PROPERTY_UnitAttachment_CREATES_UNITS_LIST = "createsUnitsList";
	static final String PROPERTY_UnitAttachment_DEFENSE = "defense";
	static final String PROPERTY_UnitAttachment_DESTROYED_WHEN_CAPTURED_BY = "destroyedWhenCapturedBy";
	static final String PROPERTY_UnitAttachment_DESTROYED_WHEN_CAPTURED_FROM = "destroyedWhenCapturedFrom";
	static final String PROPERTY_UnitAttachment_FUEL_COST = "fuelCost";
	static final String PROPERTY_UnitAttachment_GIVES_MOVEMENT = "givesMovement";
	static final String PROPERTY_UnitAttachment_IS_AA = "isAA";
	static final String PROPERTY_UnitAttachment_IS_AA_FOR_BOMBING_THIS_UNIT_ONLY = "isAAforBombingThisUnitOnly";
	static final String PROPERTY_UnitAttachment_IS_AA_FOR_COMBAT_ONLY = "isAAforCombatOnly";
	static final String PROPERTY_UnitAttachment_IS_AA_FOR_FLY_OVER_ONLY = "isAAforFlyOverOnly"; // just added this
	static final String PROPERTY_UnitAttachment_IS_AA_MOVEMENT = "isAAmovement";
	static final String PROPERTY_UnitAttachment_IS_AIR = "isAir";
	static final String PROPERTY_UnitAttachment_IS_AIR_BASE = "isAirBase";
	static final String PROPERTY_UnitAttachment_IS_AIR_TRANSPORT = "isAirTransport";
	static final String PROPERTY_UnitAttachment_IS_AIR_TRANSPORTABLE = "isAirTransportable";
	static final String PROPERTY_UnitAttachment_IS_COMBAT_TRANSPORT = "isCombatTransport";
	static final String PROPERTY_UnitAttachment_IS_CONSTRUCTION = "isConstruction";
	static final String PROPERTY_UnitAttachment_IS_DESTROYER = "isDestroyer";
	static final String PROPERTY_UnitAttachment_IS_FACTORY = "isFactory";
	static final String PROPERTY_UnitAttachment_IS_INFANTRY = "isInfantry";
	static final String PROPERTY_UnitAttachment_IS_INFRASTRUCTURE = "isInfrastructure";
	static final String PROPERTY_UnitAttachment_IS_Kamikaze = "isKamikaze";
	static final String PROPERTY_UnitAttachment_IS_LAND_TRANSPORT = "isLandTransport";
	static final String PROPERTY_UnitAttachment_IS_MARINE = "isMarine";
	static final String PROPERTY_UnitAttachment_IS_MECHANIZED = "isMechanized"; // kept for backwards compatibility to avoid giving users java errors. does nothing.
	static final String PROPERTY_UnitAttachment_IS_PARATROOP = "isParatroop"; // kept for backwards compatibility to avoid giving users java errors. does nothing.
	static final String PROPERTY_UnitAttachment_IS_ROCKET = "isRocket";
	static final String PROPERTY_UnitAttachment_IS_SEA = "isSea";
	static final String PROPERTY_UnitAttachment_IS_STRATEGIC_BOMBER = "isStrategicBomber";
	static final String PROPERTY_UnitAttachment_IS_SUB = "isSub";
	static final String PROPERTY_UnitAttachment_IS_SUICIDE = "isSuicide";
	static final String PROPERTY_UnitAttachment_IS_TWO_HIT = "isTwoHit";
	static final String PROPERTY_UnitAttachment_MAX_AA_ATTACKS = "maxAAattacks"; // just added this
	static final String PROPERTY_UnitAttachment_MAX_BUILT_PER_PLAYER = "maxBuiltPerPlayer";
	static final String PROPERTY_UnitAttachment_MAX_CONSTRUCTIONS_PER_TYPE_PER_TERR = "maxConstructionsPerTypePerTerr";
	static final String PROPERTY_UnitAttachment_MAX_DAMAGE = "maxDamage";
	static final String PROPERTY_UnitAttachment_MAX_OPERATIONAL_DAMAGE = "maxOperationalDamage";
	static final String PROPERTY_UnitAttachment_MAX_SCRAMBLE_COUNT = "maxScrambleCount";
	static final String PROPERTY_UnitAttachment_MAX_SCRAMBLE_DISTANCE = "maxScrambleDistance";
	static final String PROPERTY_UnitAttachment_MAY_OVER_STACK_AA = "mayOverStackAA"; // just added this
	static final String PROPERTY_UnitAttachment_MOVEMENT = "movement";
	static final String PROPERTY_UnitAttachment_RECEIVES_ABILITY_WHEN_WITH = "receivesAbilityWhenWith";
	static final String PROPERTY_UnitAttachment_REPAIRS_UNITS = "repairsUnits";
	static final String PROPERTY_UnitAttachment_REQUIRES_UNITS = "requiresUnits";
	static final String PROPERTY_UnitAttachment_SPECIAL = "special";
	static final String PROPERTY_UnitAttachment_STACKING_LIMIT = "stackingLimit"; // just added this
	static final String PROPERTY_UnitAttachment_TARGETS_AA = "targetsAA"; // just added this
	static final String PROPERTY_UnitAttachment_TRANSPORT_CAPACITY = "transportCapacity";
	static final String PROPERTY_UnitAttachment_TRANSPORT_COST = "transportCost";
	static final String PROPERTY_UnitAttachment_TYPE_AA = "typeAA"; // just added this
	static final String PROPERTY_UnitAttachment_UNIT_PLACEMENT_ONLY_ALLOWED_IN = "unitPlacementOnlyAllowedIn";
	static final String PROPERTY_UnitAttachment_UNIT_PLACEMENT_RESTRICTIONS = "unitPlacementRestrictions";
	static final String PROPERTY_UnitAttachment_UNIT_SUPPORT_COUNT = "unitSupportCount";
	static final String PROPERTY_UnitAttachment_WHEN_CAPTURED_CHANGES_INTO = "whenCapturedChangesInto";
	static final String PROPERTY_UnitAttachment_WHEN_COMBAT_DAMAGED = "whenCombatDamaged";
	static final String PROPERTY_UnitAttachment_WILL_NOT_FIRE_IF_PRESENT = "willNotFireIfPresent"; // just added this
	
	// UnitSupportAttachment
	static final String PROPERTY_UnitSupportAttachment_BONUS = "bonus";
	static final String PROPERTY_UnitSupportAttachment_BONUS_TYPE = "bonusType";
	static final String PROPERTY_UnitSupportAttachment_DICE = "dice";
	static final String PROPERTY_UnitSupportAttachment_FACTION = "faction";
	static final String PROPERTY_UnitSupportAttachment_IMPARTTECH = "impArtTech";
	static final String PROPERTY_UnitSupportAttachment_NUMBER = "number";
	static final String PROPERTY_UnitSupportAttachment_PLAYERS = "players";
	static final String PROPERTY_UnitSupportAttachment_SIDE = "side";
	static final String PROPERTY_UnitSupportAttachment_UNIT_TYPE = "unitType";
	
	// CanalAttachment
	static final String PROPERTY_CanalAttachment_CANAL_NAME = "canalName";
	static final String PROPERTY_CanalAttachment_LAND_TERRITORIES = "landTerritories";
	
	// PoliticalActionAttachment
	// FYI: "attemptsLeftThisTurn" does not belong in IAttachment as the user can not set it as a political action attachment property
	static final String PROPERTY_PoliticalActionAttachment_ACTION_ACCEPT = "actionAccept";
	static final String PROPERTY_PoliticalActionAttachment_ATTEMPTS_PER_TURN = "attemptsPerTurn";
	static final String PROPERTY_PoliticalActionAttachment_COST_PU = "costPU";
	static final String PROPERTY_PoliticalActionAttachment_RELATIONSHIP_CHANGE = "relationshipChange";
	static final String PROPERTY_PoliticalActionAttachment_TEXT = "text";
	
	// RelationshipTypeAttachment
	static final String PROPERTY_RelationshipTypeAttachment_ALLIANCES_CAN_CHAIN_TOGETHER = "alliancesCanChainTogether";
	static final String PROPERTY_RelationshipTypeAttachment_ARCHE_TYPE = "archeType";
	static final String PROPERTY_RelationshipTypeAttachment_CAN_LAND_AIR_UNITS_ON_OWNED_LAND = "canLandAirUnitsOnOwnedLand";
	static final String PROPERTY_RelationshipTypeAttachment_CAN_MOVE_AIR_UNITS_OVER_OWNED_LAND = "canMoveAirUnitsOverOwnedLand";
	static final String PROPERTY_RelationshipTypeAttachment_CAN_MOVE_LAND_UNITS_OVER_OWNED_LAND = "canMoveLandUnitsOverOwnedLand";
	static final String PROPERTY_RelationshipTypeAttachment_CAN_TAKE_OVER_OWNED_TERRITORY = "canTakeOverOwnedTerritory";
	static final String PROPERTY_RelationshipTypeAttachment_GIVES_BACK_ORIGINAL_TERRITORIES = "givesBackOriginalTerritories";
	static final String PROPERTY_RelationshipTypeAttachment_HELPS_DEFEND_AT_SEA = "helpsDefendAtSea";
	static final String PROPERTY_RelationshipTypeAttachment_IS_DEFAULT_WAR_POSITION = "isDefaultWarPosition";
	static final String PROPERTY_RelationshipTypeAttachment_UP_KEEP_COST = "upkeepCost";
	static final String PROPERTY_RelationshipTypeAttachment_CAN_MOVE_INTO_DURING_COMBAT_MOVE = "canMoveIntoDuringCombatMove"; // just added this 2
	
	// AbstractPlayerRulesAttachment
	static final String PROPERTY_AbstractPlayerRulesAttachment_DOMINATING_FIRST_ROUND_ATTACK = "dominatingFirstRoundAttack";
	static final String PROPERTY_AbstractPlayerRulesAttachment_MAX_PLACE_PER_TERRITORY = "maxPlacePerTerritory";
	static final String PROPERTY_AbstractPlayerRulesAttachment_MOVEMENT_RESTRICTION_TERRITORIES = "movementRestrictionTerritories";
	static final String PROPERTY_AbstractPlayerRulesAttachment_MOVEMENT_RESTRICTION_TYPE = "movementRestrictionType";
	static final String PROPERTY_AbstractPlayerRulesAttachment_NEGATE_DOMINATING_FIRST_ROUND_ATTACK = "negateDominatingFirstRoundAttack";
	static final String PROPERTY_AbstractPlayerRulesAttachment_PLACEMENT_ANY_SEA_ZONE = "placementAnySeaZone";
	static final String PROPERTY_AbstractPlayerRulesAttachment_PLACEMENT_ANY_TERRITORY = "placementAnyTerritory";
	static final String PROPERTY_AbstractPlayerRulesAttachment_PLACEMENT_CAPTURED_TERRITORY = "placementCapturedTerritory";
	static final String PROPERTY_AbstractPlayerRulesAttachment_PLACEMENT_IN_CAPITAL_RESTRICTED = "placementInCapitalRestricted";
	static final String PROPERTY_AbstractPlayerRulesAttachment_PLACEMENT_PER_TERRITORY = "placementPerTerritory";
	static final String PROPERTY_AbstractPlayerRulesAttachment_PRODUCTION_PER_X_TERRITORIES = "productionPerXTerritories";
	static final String PROPERTY_AbstractPlayerRulesAttachment_UNLIMITED_PRODUCTION = "unlimitedProduction";
	
	// AbstractConditionsAttachment
	static final String PROPERTY_AbstractConditionsAttachment_CHANCE = "chance";
	static final String PROPERTY_AbstractConditionsAttachment_CONDITIONS = "conditions";
	static final String PROPERTY_AbstractConditionsAttachment_CONDITION_TYPE = "conditionType";
	static final String PROPERTY_AbstractConditionsAttachment_INVERT = "invert";
	
	// AbstractRulesAttachment
	// FYI: "countEach", "eachMultiple", and "territoryCount" do not belong in IAttachment as the user can not set it as a abstract rules attachment property
	static final String PROPERTY_AbstractRulesAttachment_OBJECTIVE_VALUE = "objectiveValue";
	static final String PROPERTY_AbstractRulesAttachment_TURNS = "turns";
	static final String PROPERTY_AbstractRulesAttachment_USES = "uses";
	static final String PROPERTY_AbstractRulesAttachment_PLAYERS = "players"; // just added this 2
	
	// RulesAttachment
	// FYI: "techCount" and "atWarCount" do not belong in IAttachment as the as the user can not set it as a rules attachment property
	static final String PROPERTY_RulesAttachment_ALLIED_EXCLUSION_TERRITORIES = "alliedExclusionTerritories";
	static final String PROPERTY_RulesAttachment_ALLIED_OWNERSHIP_TERRITORIES = "alliedOwnershipTerritories";
	static final String PROPERTY_RulesAttachment_ALLIED_PRESENCE_TERRITORIES = "alliedPresenceTerritories";
	static final String PROPERTY_RulesAttachment_AT_WAR_PLAYERS = "atWarPlayers";
	static final String PROPERTY_RulesAttachment_DESTROYED_TUV = "destroyedTUV";
	static final String PROPERTY_RulesAttachment_DIRECT_EXCLUSION_TERRITORIES = "directExclusionTerritories";
	static final String PROPERTY_RulesAttachment_DIRECT_OWNERSHIP_TERRITORIES = "directOwnershipTerritories";
	static final String PROPERTY_RulesAttachment_DIRECT_PRESENCE_TERRITORIES = "directPresenceTerritories";
	static final String PROPERTY_RulesAttachment_ENEMY_EXCLUSION_TERRITORIES = "enemyExclusionTerritories";
	static final String PROPERTY_RulesAttachment_ENEMY_PRESENCE_TERRITORIES = "enemyPresenceTerritories";
	static final String PROPERTY_RulesAttachment_ENEMY_SURFACE_EXCLUSION_TERRITORIES = "enemySurfaceExclusionTerritories";
	static final String PROPERTY_RulesAttachment_RELATIONSHIP = "relationship";
	static final String PROPERTY_RulesAttachment_TECHS = "techs";
	static final String PROPERTY_RulesAttachment_UNIT_PRESENCE = "unitPresence";
	
	// AbstractTriggerAttachment
	// FYI: "usedThisRound" does not belong in IAttachment as the as the user can not set it as a trigger attachment property
	static final String PROPERTY_AbstractTriggerAttachment_NOTIFICATION = "notification";
	static final String PROPERTY_AbstractTriggerAttachment_TRIGGER = "trigger";
	static final String PROPERTY_AbstractTriggerAttachment_USES = "uses"; // just added this
	static final String PROPERTY_AbstractTriggerAttachment_WHEN = "when";
	
	// TriggerAttachment
	static final String PROPERTY_TriggerAttachment_AVAILABLE_TECH = "availableTech";
	static final String PROPERTY_TriggerAttachment_FRONTIER = "frontier";
	static final String PROPERTY_TriggerAttachment_PLACEMENT = "placement";
	static final String PROPERTY_TriggerAttachment_PLAYER_ATTACHMENT_NAME = "playerAttachmentName";
	static final String PROPERTY_TriggerAttachment_PLAYER_PROPERTY = "playerProperty";
	static final String PROPERTY_TriggerAttachment_PLAYERS = "players"; // just added this
	static final String PROPERTY_TriggerAttachment_PRODUCTION_RULE = "productionRule";
	static final String PROPERTY_TriggerAttachment_PURCHASE = "purchase";
	static final String PROPERTY_TriggerAttachment_RELATIONSHIP_CHANGE = "relationshipChange"; // just added this
	static final String PROPERTY_TriggerAttachment_RELATIONSHIP_TYPE_ATTACHMENT_NAME = "relationshipTypeAttachmentName";
	static final String PROPERTY_TriggerAttachment_RELATIONSHIP_TYPE_PROPERTY = "relationshipTypeProperty";
	static final String PROPERTY_TriggerAttachment_RELATIONSHIP_TYPES = "relationshipTypes";
	static final String PROPERTY_TriggerAttachment_REMOVE_UNITS = "removeUnits";
	static final String PROPERTY_TriggerAttachment_RESOURCE = "resource";
	static final String PROPERTY_TriggerAttachment_RESOURCE_COUNT = "resourceCount";
	static final String PROPERTY_TriggerAttachment_SUPPORT = "support";
	static final String PROPERTY_TriggerAttachment_TECH = "tech";
	static final String PROPERTY_TriggerAttachment_TERRITORIES = "territories";
	static final String PROPERTY_TriggerAttachment_TERRITORY_ATTACHMENT_NAME = "territoryAttachmentName";
	static final String PROPERTY_TriggerAttachment_TERRITORY_EFFECT_ATTACHMENT_NAME = "territoryEffectAttachmentName";
	static final String PROPERTY_TriggerAttachment_TERRITORY_EFFECT_PROPERTY = "territoryEffectProperty";
	static final String PROPERTY_TriggerAttachment_TERRITORY_EFFECTS = "territoryEffects";
	static final String PROPERTY_TriggerAttachment_TERRITORY_PROPERTY = "territoryProperty";
	static final String PROPERTY_TriggerAttachment_UNIT_ATTACHMENT_NAME = "unitAttachmentName";
	static final String PROPERTY_TriggerAttachment_UNIT_PROPERTY = "unitProperty";
	static final String PROPERTY_TriggerAttachment_UNIT_TYPE = "unitType"; // just added this
	static final String PROPERTY_TriggerAttachment_VICTORY = "victory";
	static final String PROPERTY_TriggerAttachment_ACTIVATE_TRIGGER = "activateTrigger"; // just added this 2
	*/
}
