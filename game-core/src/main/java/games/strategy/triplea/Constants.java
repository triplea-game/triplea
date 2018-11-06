package games.strategy.triplea;

import games.strategy.engine.data.PlayerID;

/**
 * Constants used throughout the game.
 */
public interface Constants {
  // Player names
  String PLAYER_NAME_AMERICANS = "Americans";
  String PLAYER_NAME_AUSTRALIANS = "Australians";
  String PLAYER_NAME_BRITISH = "British";
  String PLAYER_NAME_CANADIANS = "Canadians";
  String PLAYER_NAME_CHINESE = "Chinese";
  String PLAYER_NAME_FRENCH = "French";
  String PLAYER_NAME_GERMANS = "Germans";
  String PLAYER_NAME_IMPASSABLE = "Impassable";
  String PLAYER_NAME_ITALIANS = "Italians";
  String PLAYER_NAME_JAPANESE = "Japanese";
  String PLAYER_NAME_NEUTRAL = "Neutral";
  String PLAYER_NAME_PUPPET_STATES = "Puppet_States";
  String PLAYER_NAME_RUSSIANS = "Russians";
  // Attachment names
  String UNIT_ATTACHMENT_NAME = "unitAttachment";
  String TECH_ATTACHMENT_NAME = "techAttachment";
  String TECH_ABILITY_ATTACHMENT_NAME = "techAbilityAttachment";
  String RESOURCE_ATTACHMENT_NAME = "resourceAttachment";
  String TERRITORY_ATTACHMENT_NAME = "territoryAttachment";
  String RULES_ATTACHMENT_NAME = "rulesAttachment";
  String RULES_OBJECTIVE_PREFIX = "objectiveAttachment";
  String RULES_CONDITION_PREFIX = "conditionAttachment";
  String TRIGGER_ATTACHMENT_PREFIX = "triggerAttachment";
  String POLITICALACTION_ATTACHMENT_PREFIX = "politicalActionAttachment";
  String USERACTION_ATTACHMENT_PREFIX = "userActionAttachment";
  String PLAYER_ATTACHMENT_NAME = "playerAttachment";
  String RELATIONSHIPTYPE_ATTACHMENT_NAME = "relationshipTypeAttachment";
  String CANAL_ATTACHMENT_PREFIX = "canalAttachment";
  String SUPPORT_ATTACHMENT_PREFIX = "supportAttachment";
  String TERRITORYEFFECT_ATTACHMENT_NAME = "territoryEffectAttachment";
  String PUS = "PUs";
  String TECH_TOKENS = "techTokens";
  String VPS = "VPs";
  String NEUTRAL_CHARGE_PROPERTY = "neutralCharge";
  String FACTORIES_PER_COUNTRY_PROPERTY = "maxFactoriesPerTerritory";
  String TWO_HIT_BATTLESHIP_PROPERTY = "Two hit battleship";
  String ALWAYS_ON_AA_PROPERTY = "Always on AA";
  // allows lhtr carrier/fighter production
  String LHTR_CARRIER_PRODUCTION_RULES = "LHTR Carrier production rules";
  // Break up fighter/carrier production into atomic units
  // allow fighters to be placed on newly produced carriers
  String CAN_PRODUCE_FIGHTERS_ON_CARRIERS = "Produce fighters on carriers";
  String PRODUCE_NEW_FIGHTERS_ON_OLD_CARRIERS = "Produce new fighters on old carriers";
  String MOVE_EXISTING_FIGHTERS_TO_NEW_CARRIERS = "Move existing fighters to new carriers";
  String LAND_EXISTING_FIGHTERS_ON_NEW_CARRIERS = "Land existing fighters on new carriers";
  String HEAVY_BOMBER_DICE_ROLLS = "Heavy Bomber Dice Rolls";
  String TWO_HIT_BATTLESHIPS_REPAIR_END_OF_TURN = "Units Repair Hits End Turn";
  String WW2V2 = "WW2V2";
  String TOTAL_VICTORY = "Total Victory";
  String HONORABLE_SURRENDER = "Honorable Surrender";
  String PROJECTION_OF_POWER = "Projection of Power";
  String ALL_ROCKETS_ATTACK = "All Rockets Attack";
  String ROCKETS_CAN_FLY_OVER_IMPASSABLES = "Rockets Can Fly Over Impassables";
  String NEUTRALS_ARE_IMPASSABLE = "Neutrals Are Impassable";
  String NEUTRALS_ARE_BLITZABLE = "Neutrals Are Blitzable";
  String PARTIAL_AMPHIBIOUS_RETREAT = "Partial Amphibious Retreat";
  // public static final String PREVIOUS_UNITS_FIGHT = "Previous Units Fight";
  /*
   * These are the individual rules from a game (All default to FALSE)
   */
  String PLACEMENT_RESTRICTED_BY_FACTORY = "Placement Restricted By Factory";
  String SELECTABLE_TECH_ROLL = "Selectable Tech Roll";
  String WW2V3_TECH_MODEL = "WW2V3 Tech Model";
  String TECH_DEVELOPMENT = "Tech Development";
  String TRANSPORT_UNLOAD_RESTRICTED = "Transport Restricted Unload";
  String RANDOM_AA_CASUALTIES = "Random AA Casualties";
  String ROLL_AA_INDIVIDUALLY = "Roll AA Individually";
  String LIMIT_ROCKET_AND_SBR_DAMAGE_TO_PRODUCTION = "Limit SBR Damage To Factory Production";
  String SBR_VICTORY_POINTS = "SBR Victory Points";
  String LIMIT_SBR_DAMAGE_PER_TURN = "Limit SBR Damage Per Turn";
  String LIMIT_ROCKET_DAMAGE_PER_TURN = "Limit Rocket Damage Per Turn";
  String ALLIED_AIR_INDEPENDENT = "Allied Air Independent";
  String DEFENDING_SUBS_SNEAK_ATTACK = "Defending Subs Sneak Attack";
  String ATTACKER_RETREAT_PLANES = "Attacker Retreat Planes";
  String NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE_RESTRICTED =
      "Naval Bombard Casualties Return Fire";
  String SURVIVING_AIR_MOVE_TO_LAND = "Surviving Air Move To Land";
  String BLITZ_THROUGH_FACTORIES_AND_AA_RESTRICTED = "Blitz Through Factories And AA Restricted";
  String AIR_ATTACK_SUB_RESTRICTED = "Air Attack Sub Restricted";
  /*
   * End individual rules (All default to FALSE)
   */
  /*
   * These are the individual rules for TripleA WW2V3 (All default to FALSE)
   */
  String NATIONAL_OBJECTIVES = "National Objectives";
  String SUB_CONTROL_SEA_ZONE_RESTRICTED = "Sub Control Sea Zone Restricted";
  String UNIT_PLACEMENT_IN_ENEMY_SEAS = "Unit Placement In Enemy Seas";
  String TRANSPORT_CONTROL_SEA_ZONE = "Transport Control Sea Zone";
  String PRODUCTION_PER_X_TERRITORIES_RESTRICTED = "Production Per X Territories Restricted";
  String PLACE_IN_ANY_TERRITORY = "Place in Any Territory";
  String UNIT_PLACEMENT_PER_TERRITORY_RESTRICTED = "Unit Placement Per Territory Restricted";
  String MOVEMENT_BY_TERRITORY_RESTRICTED = "Movement By Territory Restricted";
  String TRANSPORT_CASUALTIES_RESTRICTED = "Transport Casualties Restricted";
  // may be SUBMERSIBLE_SUBS below
  String SUB_RETREAT_BEFORE_BATTLE = "Sub Retreat Before Battle";
  String SHORE_BOMBARD_PER_GROUND_UNIT_RESTRICTED = "Shore Bombard Per Ground Unit Restricted";
  String SBR_AFFECTS_UNIT_PRODUCTION = "Damage From Bombing Done To Units Instead Of Territories";
  String AA_TERRITORY_RESTRICTED = "AA Territory Restricted";
  String MULTIPLE_AA_PER_TERRITORY = "Multiple AA Per Territory";
  String IGNORE_TRANSPORT_IN_MOVEMENT = "Ignore Transport In Movement";
  String IGNORE_SUB_IN_MOVEMENT = "Ignore Sub In Movement";
  String UNPLACED_UNITS_LIVE = "Unplaced units live when not placed";
  /*
   * End individual rules for TripleA WW2V3 (All default to FALSE)
   */
  String PRODUCTION_PER_VALUED_TERRITORY_RESTRICTED = "Production Per Valued Territory Restricted";
  String CHOOSE_AA = "Choose AA Casualties";
  String PACIFIC_THEATER = "Pacific Theater";
  String WW2V3 = "WW2V3";
  String ECONOMIC_VICTORY = "Economic Victory";
  String SUBMERSIBLE_SUBS = "Submersible Subs";
  String ORIGINAL_OWNER = "originalOwner";
  String USE_DESTROYERS_AND_ARTILLERY = "Use Destroyers and Artillery";
  String USE_SHIPYARDS = "Use Shipyards";
  String LOW_LUCK = "Low Luck";
  String PU_CAP = "Territory Turn Limit";
  String KAMIKAZE = "Kamikaze Airplanes";
  String LHTR_HEAVY_BOMBERS = "LHTR Heavy Bombers";
  String EDIT_MODE = "EditMode";
  // by default this is 0, but for lhtr, it is 1
  String SUPER_SUB_DEFENSE_BONUS = "Super Sub Defence Bonus";
  // unit types
  String UNIT_TYPE_INFANTRY = "infantry";
  String UNIT_TYPE_INF = "inf";
  String UNIT_TYPE_MOTORIZED = "motorized";
  String UNIT_TYPE_ARMOUR = "armour";
  String UNIT_TYPE_TRANSPORT = "transport";
  String UNIT_TYPE_SUBMARINE = "submarine";
  String UNIT_TYPE_BATTLESHIP = "battleship";
  String UNIT_TYPE_MARINE = "marine";
  String UNIT_TYPE_CARRIER = "carrier";
  String UNIT_TYPE_FIGHTER = "fighter";
  String UNIT_TYPE_BOMBER = "bomber";
  String UNIT_TYPE_FACTORY = "factory";
  String UNIT_TYPE_AAGUN = "aaGun";
  String UNIT_TYPE_ARTILLERY = "artillery";
  String UNIT_TYPE_DESTROYER = "destroyer";
  String SMALL_MAP_FILENAME = "smallMap";
  String[] SMALL_MAP_EXTENSIONS = {"jpeg", "jpg", "png"};
  String MAP_NAME = "mapName";
  // new scramble property names
  String SCRAMBLE_RULES_IN_EFFECT = "Scramble Rules In Effect";
  String SCRAMBLED_UNITS_RETURN_TO_BASE = "Scrambled Units Return To Base";
  String SCRAMBLE_TO_SEA_ONLY = "Scramble To Sea Only";
  String SCRAMBLE_FROM_ISLAND_ONLY = "Scramble From Island Only";
  String SCRAMBLE_TO_ANY_AMPHIBIOUS_ASSAULT = "Scramble To Any Amphibious Assault";

  String OLD_ART_RULE_NAME = "ArtyOld";
  String SUPPORT_RULE_NAME_OLD = SUPPORT_ATTACHMENT_PREFIX + OLD_ART_RULE_NAME;
  String SUPPORT_RULE_NAME_OLD_TEMP_FIRST =
      SUPPORT_ATTACHMENT_PREFIX + OLD_ART_RULE_NAME + "TempFirst";
  String TRIGGERS = "Use Triggers";
  String PU_MULTIPLIER = "Multiply PUs";

  String LL_AA_ONLY = "Low Luck for AntiAircraft";
  String SELECTABLE_ZERO_MOVEMENT_UNITS = "Selectable Zero Movement Units";
  String PARATROOPERS_CAN_MOVE_DURING_NON_COMBAT = "Paratroopers Can Move During Non Combat";
  String UNLIMITED_CONSTRUCTIONS = "Unlimited Constructions";
  String MORE_CONSTRUCTIONS_WITHOUT_FACTORY = "More Constructions without Factory";
  String MORE_CONSTRUCTIONS_WITH_FACTORY = "More Constructions with Factory";
  String UNIT_PLACEMENT_RESTRICTIONS = "Unit Placement Restrictions";
  String TWO_HIT_BATTLESHIPS_REPAIR_BEGINNING_OF_TURN = "Units Repair Hits Start Turn";
  String TWO_HITPOINT_UNITS_REQUIRE_REPAIR_FACILITIES =
      "Two HitPoint Units Require Repair Facilities";
  String LL_TECH_ONLY = "Low Luck for Technology";
  String TRIGGERED_VICTORY = "Triggered Victory";
  String GIVE_UNITS_BY_TERRITORY = "Give Units By Territory";
  String UNITS_CAN_BE_DESTROYED_INSTEAD_OF_CAPTURED = "Units Can Be Destroyed Instead Of Captured";
  String SUICIDE_AND_MUNITION_CASUALTIES_RESTRICTED = "Suicide and Munition Casualties Restricted";
  String DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE =
      "Defending Suicide and Munition Units Do Not Fire";
  String NAVAL_UNITS_MAY_NOT_NONCOMBAT_MOVE_INTO_CONTROLLED_SEA_ZONES =
      "Naval Units May Not NonCombat Move Into Controlled Sea Zones";
  String UNITS_MAY_GIVE_BONUS_MOVEMENT = "Units May Give Bonus Movement";
  String LL_DAMAGE_ONLY = "Low Luck for Bombing and Territory Damage";
  String CAPTURE_UNITS_ON_ENTERING_TERRITORY = "Capture Units On Entering Territory";
  String DESTROY_UNITS_ON_ENTERING_TERRITORY = "On Entering Units Destroyed Instead Of Captured";
  String DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES =
      "Damage From Bombing Done To Units Instead Of Territories";
  String NEUTRAL_FLYOVER_ALLOWED = "Neutral Flyover Allowed";
  String UNITS_CAN_BE_CHANGED_ON_CAPTURE = "Units Can Be Changed On Capture";
  String RELATIONSHIPS_LAST_EXTRA_ROUNDS = "Relationships Last Extra Rounds";
  String ALLIANCES_CAN_CHAIN_TOGETHER = "Alliances Can Chain Together";
  String RAIDS_MAY_BE_PRECEEDED_BY_AIR_BATTLES = "Raids May Be Preceeded By Air Battles";
  String BATTLES_MAY_BE_PRECEEDED_BY_AIR_BATTLES = "Battles May Be Preceeded By Air Battles";
  String USE_KAMIKAZE_SUICIDE_ATTACKS = "Use Kamikaze Suicide Attacks";
  String KAMIKAZE_SUICIDE_ATTACKS_DONE_BY_CURRENT_TERRITORY_OWNER =
      "Kamikaze Suicide Attacks Done By Current Territory Owner";
  String FORCE_AA_ATTACKS_FOR_LAST_STEP_OF_FLY_OVER = "Force AA Attacks For Last Step Of Fly Over";
  String PARATROOPERS_CAN_ATTACK_DEEP_INTO_ENEMY_TERRITORY =
      "Paratroopers Can Attack Deep Into Enemy Territory";
  String USE_BOMBING_MAX_DICE_SIDES_AND_BONUS = "Use Bombing Max Dice Sides And Bonus";
  String CONVOY_BLOCKADES_ROLL_DICE_FOR_COST = "Convoy Blockades Roll Dice For Cost";
  String AIRBORNE_ATTACKS_ONLY_IN_EXISTING_BATTLES = "Airborne Attacks Only In Existing Battles";
  String AIRBORNE_ATTACKS_ONLY_IN_ENEMY_TERRITORIES = "Airborne Attacks Only In Enemy Territories";
  String SUBS_CAN_END_NONCOMBAT_MOVE_WITH_ENEMIES = "Subs Can End NonCombat Move With Enemies";
  String REMOVE_ALL_TECH_TOKENS_AT_END_OF_TURN = "Remove All Tech Tokens At End Of Turn";
  String KAMIKAZE_SUICIDE_ATTACKS_ONLY_WHERE_BATTLES_ARE =
      "Kamikaze Suicide Attacks Only Where Battles Are";
  String SUBMARINES_PREVENT_UNESCORTED_AMPHIBIOUS_ASSAULTS =
      "Submarines Prevent Unescorted Amphibious Assaults";
  String SUBMARINES_DEFENDING_MAY_SUBMERGE_OR_RETREAT =
      "Submarines Defending May Submerge Or Retreat";
  String AIR_BATTLE_ROUNDS = "Air Battle Rounds";
  String SEA_BATTLE_ROUNDS = "Sea Battle Rounds";
  String LAND_BATTLE_ROUNDS = "Land Battle Rounds";
  String AIR_BATTLE_ATTACKERS_CAN_RETREAT = "Air Battle Attackers Can Retreat";
  String AIR_BATTLE_DEFENDERS_CAN_RETREAT = "Air Battle Defenders Can Retreat";
  String CAN_SCRAMBLE_INTO_AIR_BATTLES = "Can Scramble Into Air Battles";
  String TERRITORIES_ARE_ASSIGNED_RANDOMLY = "Territories Are Assigned Randomly";
  String USE_FUEL_COST = "Use Fuel Cost";
  String RETREATING_UNITS_REMAIN_IN_PLACE = "Retreating Units Remain In Place";
  String CONTESTED_TERRITORIES_PRODUCE_NO_INCOME = "Contested Territories Produce No Income";
  String SEA_BATTLES_MAY_BE_IGNORED = "Sea Battles May Be Ignored";
  String ABANDONED_TERRITORIES_MAY_BE_TAKEN_OVER_IMMEDIATELY =
      "Abandoned Territories May Be Taken Over Immediately";
  String DISABLED_PLAYERS_ASSETS_DELETED = "Disabled Players Assets Deleted";
  String CONTROL_ALL_CANALS_BETWEEN_TERRITORIES_TO_PASS =
      "Control All Canals Between Territories To Pass";
  String UNITS_CAN_LOAD_IN_HOSTILE_SEA_ZONES = "Units Can Load In Hostile Sea Zones";

  // relationships stuff
  String RELATIONSHIP_TYPE_SELF = "self_relation";
  String RELATIONSHIP_TYPE_NULL = "null_relation";
  String RELATIONSHIP_TYPE_DEFAULT_ALLIED = "default_allied_relation";
  String RELATIONSHIP_TYPE_DEFAULT_WAR = "default_war_relation";
  String RELATIONSHIP_CONDITION_ANY_NEUTRAL = "anyNeutral";
  String RELATIONSHIP_CONDITION_ANY = "any";
  String RELATIONSHIP_CONDITION_ANY_ALLIED = "anyAllied";
  String RELATIONSHIP_CONDITION_ANY_WAR = "anyWar";
  String RELATIONSHIP_ARCHETYPE_NEUTRAL = "neutral";
  String RELATIONSHIP_ARCHETYPE_WAR = "war";
  String RELATIONSHIP_ARCHETYPE_ALLIED = "allied";
  String RELATIONSHIP_PROPERTY_DEFAULT = "default";
  String RELATIONSHIP_PROPERTY_TRUE = "true";
  String RELATIONSHIP_PROPERTY_FALSE = "false";
  String USE_POLITICS = "Use Politics";
  String PROPERTY_TRUE = "true";
  String PROPERTY_FALSE = "false";

  String CONSTRUCTION_TYPE_FACTORY = "factory";

  static String getIncomePercentageFor(final PlayerID playerId) {
    return playerId.getName() + " Income Percentage";
  }

  static String getPuIncomeBonus(final PlayerID playerId) {
    return playerId.getName() + "PU Income Bonus";
  }

}
