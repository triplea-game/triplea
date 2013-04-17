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
 * Constants.java
 * 
 * Created on November 8, 2001, 3:28 PM
 */
package games.strategy.triplea;

/**
 * 
 * Constants used throughout the game.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public interface Constants
{
	// Player names
	public static final String AMERICANS = "Americans";
	public static final String BRITISH = "British";
	public static final String GERMANS = "Germans";
	public static final String JAPANESE = "Japanese";
	public static final String RUSSIANS = "Russians";
	public static final String ITALIANS = "Italians";
	public static final String CHINESE = "Chinese";
	public static final String UNIT_ATTACHMENT_NAME = "unitAttatchment";
	public static final String INF_ATTACHMENT_NAME = "infAttatchment";
	public static final String TECH_ATTACHMENT_NAME = "techAttatchment";
	public static final String TECH_ABILITY_ATTACHMENT_NAME = "techAbilityAttachment";
	public static final String RESOURCE_ATTACHMENT_NAME = "resourceAttatchment";
	public static final String TERRITORY_ATTACHMENT_NAME = "territoryAttatchment";
	public static final String RULES_ATTACHMENT_NAME = "rulesAttatchment";
	public static final String RULES_OBJECTIVE_PREFIX = "objectiveAttachment";
	public static final String RULES_CONDITION_PREFIX = "conditionAttachment";
	public static final String TRIGGER_ATTACHMENT_PREFIX = "triggerAttachment";
	public static final String POLITICALACTION_ATTACHMENT_PREFIX = "politicalActionAttachment";
	public static final String PLAYER_ATTACHMENT_NAME = "playerAttatchment";
	public static final String RELATIONSHIPTYPE_ATTACHMENT_NAME = "relationshipTypeAttachment";
	public static final String CANAL_ATTACHMENT_PREFIX = "canalAttatchment";
	public static final String SUPPORT_ATTACHMENT_PREFIX = "supportAttachment";
	public static final String TERRITORYEFFECT_ATTACHMENT_NAME = "territoryEffectAttachment";
	public static final String PUS = "PUs";
	public static final String TECH_TOKENS = "techTokens";
	public static final String VPS = "VPs";
	// public static final String SUICIDE_ATTACK_TOKENS = "SuicideAttackTokens";
	// public static final int MAX_DICE = 6; now please use data.getDiceSides()
	public static final String NEUTRAL_CHARGE_PROPERTY = "neutralCharge";
	public static final String FACTORIES_PER_COUNTRY_PROPERTY = "maxFactoriesPerTerritory";
	public static final String TWO_HIT_BATTLESHIP_PROPERTY = "Two hit battleship";
	public static final String ALWAYS_ON_AA_PROPERTY = "Always on AA";
	// allows lhtr carrier/fighter production
	public static final String LHTR_CARRIER_PRODUCTION_RULES = "LHTR Carrier production rules";
	// Break up fighter/carrier production into atomic units
	// allow fighters to be placed on newly produced carriers
	public static final String CAN_PRODUCE_FIGHTERS_ON_CARRIERS = "Produce fighters on carriers";
	public static final String PRODUCE_NEW_FIGHTERS_ON_OLD_CARRIERS = "Produce new fighters on old carriers";
	public static final String MOVE_EXISTING_FIGHTERS_TO_NEW_CARRIERS = "Move existing fighters to new carriers";
	public static final String LAND_EXISTING_FIGHTERS_ON_NEW_CARRIERS = "Land existing fighters on new carriers";
	public static final String HEAVY_BOMBER_DICE_ROLLS = "Heavy Bomber Dice Rolls";
	public static final String TWO_HIT_BATTLESHIPS_REPAIR_END_OF_TURN = "Battleships repair at end of round";
	public static final String WW2V2 = "WW2V2";
	public static final String TOTAL_VICTORY = "Total Victory";
	public static final String HONORABLE_SURRENDER = "Honorable Surrender";
	public static final String PROJECTION_OF_POWER = "Projection of Power";
	public static final String ALL_ROCKETS_ATTACK = "All Rockets Attack";
	public static final String ROCKETS_CAN_FLY_OVER_IMPASSABLES = "Rockets Can Fly Over Impassables";
	public static final String NEUTRALS_ARE_IMPASSABLE = "Neutrals Are Impassable";
	public static final String NEUTRALS_ARE_BLITZABLE = "Neutrals Are Blitzable";
	public static final String PARTIAL_AMPHIBIOUS_RETREAT = "Partial Amphibious Retreat";
	// public static final String PREVIOUS_UNITS_FIGHT = "Previous Units Fight";
	/**
	 * These are the individual rules from a game (All default to FALSE)
	 */
	public static final String PLACEMENT_RESTRICTED_BY_FACTORY = "Placement Restricted By Factory";
	public static final String SELECTABLE_TECH_ROLL = "Selectable Tech Roll";
	public static final String WW2V3_TECH_MODEL = "WW2V3 Tech Model";
	public static final String TECH_DEVELOPMENT = "Tech Development";
	public static final String TRANSPORT_UNLOAD_RESTRICTED = "Transport Restricted Unload";
	public static final String RANDOM_AA_CASUALTIES = "Random AA Casualties";
	public static final String ROLL_AA_INDIVIDUALLY = "Roll AA Individually";
	public static final String LIMIT_ROCKET_AND_SBR_DAMAGE_TO_PRODUCTION = "Limit SBR Damage To Factory Production";
	public static final String SBR_VICTORY_POINTS = "SBR Victory Points";
	public static final String ROCKET_ATTACKS_PER_FACTORY_INFINITE = "Rocket Attacks Per Factory Infinite";
	public static final String LIMIT_SBR_DAMAGE_PER_TURN = "Limit SBR Damage Per Turn";
	public static final String LIMIT_ROCKET_DAMAGE_PER_TURN = "Limit Rocket Damage Per Turn";
	public static final String ALLIED_AIR_DEPENDENTS = "Allied Air Dependents";
	public static final String DEFENDING_SUBS_SNEAK_ATTACK = "Defending Subs Sneak Attack";
	public static final String ATTACKER_RETREAT_PLANES = "Attacker Retreat Planes";
	public static final String NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE_RESTRICTED = "Naval Bombard Casualties Return Fire Restricted";
	public static final String SURVIVING_AIR_MOVE_TO_LAND = "Surviving Air Move To Land";
	public static final String BLITZ_THROUGH_FACTORIES_AND_AA_RESTRICTED = "Blitz Through Factories And AA Restricted";
	public static final String AIR_ATTACK_SUB_RESTRICTED = "Air Attack Sub Restricted";
	/**
	 * End individual rules (All default to FALSE)
	 */
	/**
	 * These are the individual rules for TripleA WW2V3 (All default to FALSE)
	 */
	public static final String NATIONAL_OBJECTIVES = "National Objectives";
	public static final String SUB_CONTROL_SEA_ZONE_RESTRICTED = "Sub Control Sea Zone Restricted";
	public static final String UNIT_PLACEMENT_IN_ENEMY_SEAS = "Unit Placement In Enemy Seas";
	public static final String TRANSPORT_CONTROL_SEA_ZONE = "Transport Control Sea Zone";
	public static final String PRODUCTION_PER_X_TERRITORIES_RESTRICTED = "Production Per X Territories Restricted";
	public static final String PLACE_IN_ANY_TERRITORY = "Place in Any Territory";
	public static final String UNIT_PLACEMENT_PER_TERRITORY_RESTRICTED = "Unit Placement Per Territory Restricted";
	public static final String MOVEMENT_BY_TERRITORY_RESTRICTED = "Movement By Territory Restricted";
	public static final String TRANSPORT_CASUALTIES_RESTRICTED = "Transport Casualties Restricted";
	public static final String SUB_RETREAT_BEFORE_BATTLE = "Sub Retreat Before Battle"; // may be SUBMERSIBLE_SUBS below
	public static final String SHORE_BOMBARD_PER_GROUND_UNIT_RESTRICTED = "Shore Bombard Per Ground Unit Restricted";
	public static final String SBR_AFFECTS_UNIT_PRODUCTION = "SBR Affects Unit Production";
	public static final String AA_TERRITORY_RESTRICTED = "AA Territory Restricted";
	public static final String MULTIPLE_AA_PER_TERRITORY = "Multiple AA Per Territory";
	public static final String IGNORE_TRANSPORT_IN_MOVEMENT = "Ignore Transport In Movement";
	public static final String IGNORE_SUB_IN_MOVEMENT = "Ignore Sub In Movement";
	// public static final String HARI_KARI_UNITS = "Hari-Kari Units";
	public static final String UNPLACED_UNITS_LIVE = "Unplaced units live when not placed";
	/**
	 * End individual rules for TripleA WW2V3 (All default to FALSE)
	 */
	public static final String PRODUCTION_PER_VALUED_TERRITORY_RESTRICTED = "Production Per Valued Territory Restricted";
	public static final String CHOOSE_AA = "Choose AA Casualties";
	public static final String PACIFIC_THEATER = "Pacific Theater";
	public static final String WW2V3 = "WW2V3";
	public static final String ECONOMIC_VICTORY = "Economic Victory";
	public static final String SUBMERSIBLE_SUBS = "Submersible Subs";
	public static final String TWO_HIT = "isTwoHit";
	public static final String ORIGINAL_OWNER = "originalOwner";
	public static final String USE_DESTROYERS_AND_ARTILLERY = "Use Destroyers and Artillery";
	public static final String USE_SHIPYARDS = "Use Shipyards";
	public static final String LOW_LUCK = "Low Luck";
	public static final String PU_CAP = "Territory Turn Limit";
	public static final String KAMIKAZE = "Kamikaze Airplanes";
	public static final String LHTR_HEAVY_BOMBERS = "LHTR Heavy Bombers";
	public static final String EDIT_MODE = "EditMode";
	// by defaul this is 0, but for lhtr, it is 1
	public static final String SUPER_SUB_DEFENSE_BONUS = "Super Sub Defence Bonus";
	public static final String INFANTRY_TYPE = "infantry";
	public static final String ARMOUR_TYPE = "armour";
	public static final String TRANSPORT_TYPE = "transport";
	public static final String SUBMARINE_TYPE = "submarine";
	public static final String BATTLESHIP_TYPE = "battleship";
	public static final String CARRIER_TYPE = "carrier";
	public static final String FIGHTER_TYPE = "fighter";
	public static final String BOMBER_TYPE = "bomber";
	public static final String FACTORY_TYPE = "factory";
	public static final String AAGUN_TYPE = "aaGun";
	public static final String ARTILLERY = "artillery";
	public static final String DESTROYER = "destroyer";
	public static final String LARGE_MAP_FILENAME = "largeMap.gif";
	public static final String SMALL_MAP_FILENAME = "smallMap.jpeg";
	public static final String MAP_NAME = "mapName";
	public static final String SHOW_ENEMY_CASUALTIES_USER_PREF = "ShowEnemyCasualties";
	public static final String FOCUS_ON_OWN_CASUALTIES_USER_PREF = "FocusOnOwnCasualties";
	
	// new scramble property names
	public static final String SCRAMBLE_RULES_IN_EFFECT = "Scramble Rules In Effect";
	public static final String SCRAMBLED_UNITS_RETURN_TO_BASE = "Scrambled Units Return To Base";
	public static final String SCRAMBLE_TO_SEA_ONLY = "Scramble To Sea Only";
	public static final String SCRAMBLE_FROM_ISLAND_ONLY = "Scramble From Island Only";
	public static final String SCRAMBLE_TO_ANY_AMPHIBIOUS_ASSAULT = "Scramble To Any Amphibious Assault";
	
	// squid
	public static final String OLD_ART_RULE_NAME = "ArtyOld";
	public static final String SUPPORT_RULE_NAME_OLD = SUPPORT_ATTACHMENT_PREFIX + OLD_ART_RULE_NAME;
	public static final String SUPPORT_RULE_NAME_OLD_TEMP_FIRST = SUPPORT_ATTACHMENT_PREFIX + OLD_ART_RULE_NAME + "TempFirst";
	public static final String TRIGGERS = "Use Triggers";
	public static final String PU_MULTIPLIER = "Multiply PUs";
	
	// veqryn (Mark Christopher Duncan)
	public static final String LL_AA_ONLY = "Low Luck for AntiAircraft";
	public static final String SELECTABLE_ZERO_MOVEMENT_UNITS = "Selectable Zero Movement Units";
	public static final String PARATROOPERS_CAN_MOVE_DURING_NON_COMBAT = "Paratroopers Can Move During Non Combat";
	public static final String UNLIMITED_CONSTRUCTIONS = "Unlimited Constructions";
	public static final String MORE_CONSTRUCTIONS_WITHOUT_FACTORY = "More Constructions without Factory";
	public static final String MORE_CONSTRUCTIONS_WITH_FACTORY = "More Constructions with Factory";
	public static final String UNIT_PLACEMENT_RESTRICTIONS = "Unit Placement Restrictions";
	public static final String TWO_HIT_BATTLESHIPS_REPAIR_BEGINNING_OF_TURN = "Battleships repair at beginning of round";
	public static final String TWO_HITPOINT_UNITS_REQUIRE_REPAIR_FACILITIES = "Two HitPoint Units Require Repair Facilities";
	public static final String LL_TECH_ONLY = "Low Luck for Technology";
	public static final String TRIGGERED_VICTORY = "Triggered Victory";
	public static final String GIVE_UNITS_BY_TERRITORY = "Give Units By Territory";
	public static final String UNITS_CAN_BE_DESTROYED_INSTEAD_OF_CAPTURED = "Units Can Be Destroyed Instead Of Captured";
	public static final String SUICIDE_AND_MUNITION_CASUALTIES_RESTRICTED = "Suicide and Munition Casualties Restricted";
	public static final String DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE = "Defending Suicide and Munition Units Do Not Fire";
	public static final String NAVAL_UNITS_MAY_NOT_NONCOMBAT_MOVE_INTO_CONTROLLED_SEA_ZONES = "Naval Units May Not NonCombat Move Into Controlled Sea Zones";
	public static final String UNITS_MAY_GIVE_BONUS_MOVEMENT = "Units May Give Bonus Movement";
	public static final String LL_DAMAGE_ONLY = "Low Luck for Bombing and Territory Damage";
	public static final String CAPTURE_UNITS_ON_ENTERING_TERRITORY = "Capture Units On Entering Territory";
	public static final String DESTROY_UNITS_ON_ENTERING_TERRITORY = "On Entering Units Destroyed Instead Of Captured";
	public static final String DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES = "Damage From Bombing Done To Units Instead Of Territories";
	public static final String NEUTRAL_FLYOVER_ALLOWED = "Neutral Flyover Allowed";
	public static final String UNITS_CAN_BE_CHANGED_ON_CAPTURE = "Units Can Be Changed On Capture";
	public static final String AI_BONUS_INCOME_PERCENTAGE = "AI Bonus Income Percentage";
	public static final String AI_BONUS_INCOME_FLAT_RATE = "AI Bonus Income Flat Rate";
	public static final String AI_BONUS_ATTACK = "AI Bonus Attack";
	public static final String AI_BONUS_DEFENSE = "AI Bonus Defense";
	public static final String RELATIONSHIPS_LAST_EXTRA_ROUNDS = "Relationships Last Extra Rounds";
	public static final String ALLIANCES_CAN_CHAIN_TOGETHER = "Alliances Can Chain Together";
	public static final String RAIDS_MAY_BE_PRECEEDED_BY_AIR_BATTLES = "Raids May Be Preceeded By Air Battles";
	public static final String BATTLES_MAY_BE_PRECEEDED_BY_AIR_BATTLES = "Battles May Be Preceeded By Air Battles";
	public static final String USE_KAMIKAZE_SUICIDE_ATTACKS = "Use Kamikaze Suicide Attacks";
	public static final String KAMIKAZE_SUICIDE_ATTACKS_DONE_BY_CURRENT_TERRITORY_OWNER = "Kamikaze Suicide Attacks Done By Current Territory Owner";
	public static final String FORCE_AA_ATTACKS_FOR_LAST_STEP_OF_FLY_OVER = "Force AA Attacks For Last Step Of Fly Over";
	public static final String PARATROOPERS_CAN_ATTACK_DEEP_INTO_ENEMY_TERRITORY = "Paratroopers Can Attack Deep Into Enemy Territory";
	public static final String USE_BOMBING_MAX_DICE_SIDES_AND_BONUS = "Use Bombing Max Dice Sides And Bonus";
	public static final String CONVOY_BLOCKADES_ROLL_DICE_FOR_COST = "Convoy Blockades Roll Dice For Cost";
	public static final String AIRBORNE_ATTACKS_ONLY_IN_EXISTING_BATTLES = "Airborne Attacks Only In Existing Battles";
	public static final String AIRBORNE_ATTACKS_ONLY_IN_ENEMY_TERRITORIES = "Airborne Attacks Only In Enemy Territories";
	public static final String SUBS_CAN_END_NONCOMBAT_MOVE_WITH_ENEMIES = "Subs Can End NonCombat Move With Enemies";
	public static final String REMOVE_ALL_TECH_TOKENS_AT_END_OF_TURN = "Remove All Tech Tokens At End Of Turn";
	public static final String KAMIKAZE_SUICIDE_ATTACKS_ONLY_WHERE_BATTLES_ARE = "Kamikaze Suicide Attacks Only Where Battles Are";
	public static final String SUBMARINES_PREVENT_UNESCORTED_AMPHIBIOUS_ASSAULTS = "Submarines Prevent Unescorted Amphibious Assaults";
	public static final String SUBMARINES_DEFENDING_MAY_SUBMERGE_OR_RETREAT = "Submarines Defending May Submerge Or Retreat";
	public static final String AIR_BATTLE_ROUNDS = "Air Battle Rounds";
	public static final String BATTLE_ROUNDS = "Battle Rounds";
	public static final String AIR_BATTLE_ATTACKERS_CAN_RETREAT = "Air Battle Attackers Can Retreat";
	public static final String AIR_BATTLE_DEFENDERS_CAN_RETREAT = "Air Battle Defenders Can Retreat";
	public static final String CAN_SCRAMBLE_INTO_AIR_BATTLES = "Can Scramble Into Air Battles";
	
	// relationships stuff (Edwin, with help from Veqryn and Frig)
	public static final String RELATIONSHIP_TYPE_SELF = "self_relation";
	public static final String RELATIONSHIP_TYPE_NULL = "null_relation";
	public static final String RELATIONSHIP_TYPE_DEFAULT_ALLIED = "default_allied_relation";
	public static final String RELATIONSHIP_TYPE_DEFAULT_WAR = "default_war_relation";
	public static final String RELATIONSHIP_CONDITION_ANY_NEUTRAL = "anyNeutral";
	public static final String RELATIONSHIP_CONDITION_ANY = "any";
	public static final String RELATIONSHIP_CONDITION_ANY_ALLIED = "anyAllied";
	public static final String RELATIONSHIP_CONDITION_ANY_WAR = "anyWar";
	public static final String RELATIONSHIP_ARCHETYPE_NEUTRAL = "neutral";
	public static final String RELATIONSHIP_ARCHETYPE_WAR = "war";
	public static final String RELATIONSHIP_ARCHETYPE_ALLIED = "allied";
	public static final String RELATIONSHIP_PROPERTY_DEFAULT = "default";
	public static final String RELATIONSHIP_PROPERTY_TRUE = "true";
	public static final String RELATIONSHIP_PROPERTY_FALSE = "false";
	public static final String USE_POLITICS = "Use Politics";
	
	public static final String PROPERTY_TRUE = "true";
	public static final String PROPERTY_FALSE = "false";
	public static final String PROPERTY_DEFAULT = "default";
	
	/*public static final char VALUE_TRUE = 't';
	public static final char VALUE_FALSE = 'f';
	public static final char VALUE_DEFAULT = 'd';*/

}
