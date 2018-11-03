package games.strategy.triplea;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;

/**
 * Provides typed access to the properties of GameData.
 */
public final class Properties implements Constants {
  private Properties() {}

  // These should always default to false, if boolean, and if not should default to whatever is the "default" behavior
  // of TripleA.
  // If you want something to default to "true", when change the wording of the constant to make it a negative of
  // itself, then default to
  // false. (ex: "Do not do something", false; instead of "Do something", true;)

  public static int getNeutralCharge(final GameData data) {
    return data.getProperties().get(NEUTRAL_CHARGE_PROPERTY, 0);
  }

  public static int getFactoriesPerCountry(final GameData data) {
    return data.getProperties().get(FACTORIES_PER_COUNTRY_PROPERTY, 1);
  }

  public static boolean getTwoHitBattleships(final GameData data) {
    return data.getProperties().get(TWO_HIT_BATTLESHIP_PROPERTY, false);
  }

  public static boolean getWW2V2(final GameData data) {
    return data.getProperties().get(WW2V2, false);
  }

  public static boolean getPartialAmphibiousRetreat(final GameData data) {
    return data.getProperties().get(PARTIAL_AMPHIBIOUS_RETREAT, false);
  }

  public static boolean getTotalVictory(final GameData data) {
    return data.getProperties().get(TOTAL_VICTORY, false);
  }

  public static boolean getHonorableSurrender(final GameData data) {
    return data.getProperties().get(HONORABLE_SURRENDER, false);
  }

  public static boolean getProjectionOfPower(final GameData data) {
    return data.getProperties().get(PROJECTION_OF_POWER, false);
  }

  public static boolean getAllRocketsAttack(final GameData data) {
    return data.getProperties().get(ALL_ROCKETS_ATTACK, false);
  }

  public static boolean getNeutralsImpassable(final GameData data) {
    return data.getProperties().get(NEUTRALS_ARE_IMPASSABLE, false);
  }

  public static boolean getNeutralsBlitzable(final GameData data) {
    return data.getProperties().get(NEUTRALS_ARE_BLITZABLE, false);
  }

  public static boolean getRocketsCanFlyOverImpassables(final GameData data) {
    return data.getProperties().get(ROCKETS_CAN_FLY_OVER_IMPASSABLES, false);
  }

  public static boolean getStrictRockets(final GameData data) {
    return data.getProperties().get("Strictly rule compliant rockets", false);
  }

  /*
   * Pacific Theater
   */
  public static boolean getPacificTheater(final GameData data) {
    return data.getProperties().get(PACIFIC_THEATER, false);
  }

  /*
   * World War 2 Version 3
   */
  public static boolean getWW2V3(final GameData data) {
    return data.getProperties().get(WW2V3, false);
  }

  /*
   * Economic Victory Condition
   */
  public static boolean getEconomicVictory(final GameData data) {
    return data.getProperties().get(ECONOMIC_VICTORY, false);
  }

  /*
   * Triggered Victory Condition
   */
  public static boolean getTriggeredVictory(final GameData data) {
    return data.getProperties().get(TRIGGERED_VICTORY, false);
  }

  /*
   * Restrict the number of units that can be placed at a factory.
   */
  public static boolean getPlacementRestrictedByFactory(final GameData data) {
    return data.getProperties().get(PLACEMENT_RESTRICTED_BY_FACTORY, false);
  }

  /*
   * Can the player select the type of technology they are rolling for
   */
  public static boolean getSelectableTechRoll(final GameData data) {
    return data.getProperties().get(SELECTABLE_TECH_ROLL, false);
  }

  /*
   * Can the player select the type of technology they are rolling for
   */
  public static boolean getWW2V3TechModel(final GameData data) {
    return data.getProperties().get(WW2V3_TECH_MODEL, false);
  }

  /*
   * Use Advanced Technology
   */
  public static boolean getTechDevelopment(final GameData data) {
    return data.getProperties().get(TECH_DEVELOPMENT, false);
  }

  /*
   * Are transports restricted from unloading in multiple territories in a
   * turn
   */
  public static boolean getTransportUnloadRestricted(final GameData data) {
    return data.getProperties().get(TRANSPORT_UNLOAD_RESTRICTED, false);
  }

  /*
   * Are AA casualties chosen randomly
   */
  public static boolean getRandomAaCasualties(final GameData data) {
    return data.getProperties().get(RANDOM_AA_CASUALTIES, false);
  }

  /*
   * Are AA casualties chosen randomly
   */
  public static boolean getRollAaIndividually(final GameData data) {
    return data.getProperties().get(ROLL_AA_INDIVIDUALLY, false);
  }

  /*
   * Limit the damage caused by each bomber on rockets and Strategic Bomb Raids to
   * production of territory
   */
  public static boolean getLimitRocketAndSbrDamageToProduction(final GameData data) {
    return data.getProperties().get(LIMIT_ROCKET_AND_SBR_DAMAGE_TO_PRODUCTION, false);
  }

  /*
   * Limit the TOTAL damage caused by Bombers in a turn to territory's
   * production
   */
  public static boolean getLimitSbrDamagePerTurn(final GameData data) {
    return data.getProperties().get(LIMIT_SBR_DAMAGE_PER_TURN, false);
  }

  /**
   * Limit the TOTAL damage caused by Rockets in a turn to territory's
   * production.
   */
  public static boolean getLimitRocketDamagePerTurn(final GameData data) {
    return data.getProperties().get(LIMIT_ROCKET_DAMAGE_PER_TURN, false);
  }

  /**
   * Limit the TOTAL PUs lost to Bombers/Rockets in a turn to territory's
   * production.
   */
  public static boolean getPuCap(final GameData data) {
    return data.getProperties().get(PU_CAP, false);
  }

  /**
   * Reduce Victory Points by Strategic Bombing.
   */
  public static boolean getSbrVictoryPoints(final GameData data) {
    return data.getProperties().get(SBR_VICTORY_POINTS, false);
  }

  /**
   * Are allied aircraft dependents of CVs.
   */
  public static boolean getAlliedAirIndependent(final GameData data) {
    return data.getProperties().get(ALLIED_AIR_INDEPENDENT, false);
  }

  /**
   * Defending subs sneak attack.
   */
  public static boolean getDefendingSubsSneakAttack(final GameData data) {
    return data.getProperties().get(DEFENDING_SUBS_SNEAK_ATTACK, false);
  }

  /**
   * Attacker retreat planes from Amphib assault.
   */
  public static boolean getAttackerRetreatPlanes(final GameData data) {
    return data.getProperties().get(ATTACKER_RETREAT_PLANES, false);
  }

  /**
   * Can surviving air at sea move to land on friendly land/carriers.
   */
  public static boolean getSurvivingAirMoveToLand(final GameData data) {
    return data.getProperties().get(SURVIVING_AIR_MOVE_TO_LAND, false);
  }

  /**
   * Naval Bombard casualties restricted from return fire.
   */
  public static boolean getNavalBombardCasualtiesReturnFireRestricted(final GameData data) {
    return data.getProperties().get(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE_RESTRICTED, false);
  }

  /**
   * Restricted from blitz through territories with factories/AA.
   */
  public static boolean getBlitzThroughFactoriesAndAaRestricted(final GameData data) {
    return data.getProperties().get(BLITZ_THROUGH_FACTORIES_AND_AA_RESTRICTED, false);
  }

  /**
   * Can place new units in occupied sea zones.
   */
  public static boolean getUnitPlacementInEnemySeas(final GameData data) {
    return data.getProperties().get(UNIT_PLACEMENT_IN_ENEMY_SEAS, false);
  }

  /**
   * Subs restricted from controlling sea zones.
   */
  public static boolean getSubControlSeaZoneRestricted(final GameData data) {
    return data.getProperties().get(SUB_CONTROL_SEA_ZONE_RESTRICTED, false);
  }

  /*
   * Can Transports control sea zones
   */
  public static boolean getTransportControlSeaZone(final GameData data) {
    return data.getProperties().get(TRANSPORT_CONTROL_SEA_ZONE, false);
  }

  /**
   * Production restricted to 1 unit per X owned territories.
   */
  public static boolean getProductionPerXTerritoriesRestricted(final GameData data) {
    return data.getProperties().get(PRODUCTION_PER_X_TERRITORIES_RESTRICTED, false);
  }

  /**
   * Production restricted to 1 unit per owned territory with an PU value.
   */
  public static boolean getProductionPerValuedTerritoryRestricted(final GameData data) {
    return data.getProperties().get(PRODUCTION_PER_VALUED_TERRITORY_RESTRICTED, false);
  }

  /**
   * Can units be placed in any owned territory.
   */
  public static boolean getPlaceInAnyTerritory(final GameData data) {
    return data.getProperties().get(PLACE_IN_ANY_TERRITORY, false);
  }

  /**
   * Limit the number of units that can be in a territory.
   */
  public static boolean getUnitPlacementPerTerritoryRestricted(final GameData data) {
    return data.getProperties().get(UNIT_PLACEMENT_PER_TERRITORY_RESTRICTED, false);
  }

  /**
   * Movement restricted for territories.
   */
  public static boolean getMovementByTerritoryRestricted(final GameData data) {
    return data.getProperties().get(MOVEMENT_BY_TERRITORY_RESTRICTED, false);
  }

  /**
   * Transports restricted from being taken as casualties.
   */
  public static boolean getTransportCasualtiesRestricted(final GameData data) {
    return data.getProperties().get(TRANSPORT_CASUALTIES_RESTRICTED, false);
  }

  /**
   * Transports do not restrict movement of other units.
   */
  public static boolean getIgnoreTransportInMovement(final GameData data) {
    return data.getProperties().get(IGNORE_TRANSPORT_IN_MOVEMENT, false);
  }

  /**
   * Subs do not restrict movement of other units.
   */
  public static boolean getIgnoreSubInMovement(final GameData data) {
    return data.getProperties().get(IGNORE_SUB_IN_MOVEMENT, false);
  }

  public static boolean getUnplacedUnitsLive(final GameData data) {
    return data.getProperties().get(UNPLACED_UNITS_LIVE, false);
  }

  /**
   * Air restricted from attacking subs unless DD present.
   */
  public static boolean getAirAttackSubRestricted(final GameData data) {
    return data.getProperties().get(AIR_ATTACK_SUB_RESTRICTED, false);
  }

  /**
   * Allows units with zero movement to be selected to be moved.
   */
  public static boolean getSelectableZeroMovementUnits(final GameData data) {
    return data.getProperties().get(SELECTABLE_ZERO_MOVEMENT_UNITS, false);
  }

  /**
   * Allows paratroopers to move ground units to friendly territories during non-combat move phase.
   */
  public static boolean getParatroopersCanMoveDuringNonCombat(final GameData data) {
    return data.getProperties().get(PARATROOPERS_CAN_MOVE_DURING_NON_COMBAT, false);
  }

  public static boolean getSubRetreatBeforeBattle(final GameData data) {
    return data.getProperties().get(SUB_RETREAT_BEFORE_BATTLE, false);
  }

  /**
   * Shore Bombard per Ground Unit Restricted.
   */
  public static boolean getShoreBombardPerGroundUnitRestricted(final GameData data) {
    return data.getProperties().get(SHORE_BOMBARD_PER_GROUND_UNIT_RESTRICTED, false);
  }

  /**
   * AA restricted to Attacked Territory Only.
   */
  public static boolean getAaTerritoryRestricted(final GameData data) {
    return data.getProperties().get(AA_TERRITORY_RESTRICTED, false);
  }

  public static boolean getMultipleAaPerTerritory(final GameData data) {
    return data.getProperties().get(MULTIPLE_AA_PER_TERRITORY, false);
  }

  public static boolean getNationalObjectives(final GameData data) {
    return data.getProperties().get(NATIONAL_OBJECTIVES, false);
  }

  public static boolean getTriggers(final GameData data) {
    return data.getProperties().get(TRIGGERS, false);
  }

  public static boolean getAlwaysOnAa(final GameData data) {
    return data.getProperties().get(ALWAYS_ON_AA_PROPERTY, false);
  }

  public static boolean getLhtrCarrierProductionRules(final GameData data) {
    return data.getProperties().get(LHTR_CARRIER_PRODUCTION_RULES, false);
  }

  /**
   * Atomic units of the fighter/carrier production rules.
   */
  public static boolean getProduceFightersOnCarriers(final GameData data) {
    return data.getProperties().get(CAN_PRODUCE_FIGHTERS_ON_CARRIERS, false);
  }

  public static boolean getProduceNewFightersOnOldCarriers(final GameData data) {
    return data.getProperties().get(PRODUCE_NEW_FIGHTERS_ON_OLD_CARRIERS, false);
  }

  public static boolean getMoveExistingFightersToNewCarriers(final GameData data) {
    return data.getProperties().get(MOVE_EXISTING_FIGHTERS_TO_NEW_CARRIERS, false);
  }

  public static boolean getLandExistingFightersOnNewCarriers(final GameData data) {
    return data.getProperties().get(LAND_EXISTING_FIGHTERS_ON_NEW_CARRIERS, false);
  }

  public static int getHeavyBomberDiceRolls(final GameData data) {
    return data.getProperties().get(HEAVY_BOMBER_DICE_ROLLS, 2);
  }

  public static boolean getBattleshipsRepairAtEndOfRound(final GameData data) {
    return data.getProperties().get(TWO_HIT_BATTLESHIPS_REPAIR_END_OF_TURN, false);
  }

  public static boolean getBattleshipsRepairAtBeginningOfRound(final GameData data) {
    return data.getProperties().get(TWO_HIT_BATTLESHIPS_REPAIR_BEGINNING_OF_TURN, false);
  }

  public static boolean getTwoHitPointUnitsRequireRepairFacilities(final GameData data) {
    return data.getProperties().get(TWO_HITPOINT_UNITS_REQUIRE_REPAIR_FACILITIES, false);
  }

  public static boolean getChooseAaCasualties(final GameData data) {
    return data.getProperties().get(CHOOSE_AA, false);
  }

  public static boolean getSubmersibleSubs(final GameData data) {
    return data.getProperties().get(SUBMERSIBLE_SUBS, false);
  }

  public static boolean getUseDestroyersAndArtillery(final GameData data) {
    return data.getProperties().get(USE_DESTROYERS_AND_ARTILLERY, false);
  }

  public static boolean getUseShipyards(final GameData data) {
    return data.getProperties().get(USE_SHIPYARDS, false);
  }

  public static boolean getLowLuck(final GameData data) {
    return data.getProperties().get(LOW_LUCK, false);
  }

  public static boolean getLowLuckAaOnly(final GameData data) {
    return data.getProperties().get(LL_AA_ONLY, false);
  }

  public static boolean getLowLuckTechOnly(final GameData data) {
    return data.getProperties().get(LL_TECH_ONLY, false);
  }

  public static boolean getLowLuckDamageOnly(final GameData data) {
    return data.getProperties().get(LL_DAMAGE_ONLY, false);
  }

  public static boolean getKamikazeAirplanes(final GameData data) {
    return data.getProperties().get(KAMIKAZE, false);
  }

  public static boolean getLhtrHeavyBombers(final GameData data) {
    return data.getProperties().get(LHTR_HEAVY_BOMBERS, false);
  }

  public static int getSuperSubDefenseBonus(final GameData data) {
    return data.getProperties().get(SUPER_SUB_DEFENSE_BONUS, 0);
  }

  public static boolean getScrambleRulesInEffect(final GameData data) {
    return data.getProperties().get(SCRAMBLE_RULES_IN_EFFECT, false);
  }

  public static boolean getScrambledUnitsReturnToBase(final GameData data) {
    return data.getProperties().get(SCRAMBLED_UNITS_RETURN_TO_BASE, false);
  }

  public static boolean getScrambleToSeaOnly(final GameData data) {
    return data.getProperties().get(SCRAMBLE_TO_SEA_ONLY, false);
  }

  public static boolean getScrambleFromIslandOnly(final GameData data) {
    return data.getProperties().get(SCRAMBLE_FROM_ISLAND_ONLY, false);
  }

  public static boolean getScrambleToAnyAmphibiousAssault(final GameData data) {
    return data.getProperties().get(SCRAMBLE_TO_ANY_AMPHIBIOUS_ASSAULT, false);
  }

  public static int getPuMultiplier(final GameData data) {
    return data.getProperties().get(PU_MULTIPLIER, 1);
  }

  public static boolean getUnlimitedConstructions(final GameData data) {
    return data.getProperties().get(UNLIMITED_CONSTRUCTIONS, false);
  }

  public static boolean getMoreConstructionsWithoutFactory(final GameData data) {
    return data.getProperties().get(MORE_CONSTRUCTIONS_WITHOUT_FACTORY, false);
  }

  public static boolean getMoreConstructionsWithFactory(final GameData data) {
    return data.getProperties().get(MORE_CONSTRUCTIONS_WITH_FACTORY, false);
  }

  public static boolean getUnitPlacementRestrictions(final GameData data) {
    return data.getProperties().get(UNIT_PLACEMENT_RESTRICTIONS, false);
  }

  public static boolean getGiveUnitsByTerritory(final GameData data) {
    return data.getProperties().get(GIVE_UNITS_BY_TERRITORY, false);
  }

  public static boolean getUnitsCanBeDestroyedInsteadOfCaptured(final GameData data) {
    return data.getProperties().get(UNITS_CAN_BE_DESTROYED_INSTEAD_OF_CAPTURED, false);
  }

  public static boolean getSuicideAndMunitionCasualtiesRestricted(final GameData data) {
    return data.getProperties().get(SUICIDE_AND_MUNITION_CASUALTIES_RESTRICTED, false);
  }

  public static boolean getDefendingSuicideAndMunitionUnitsDoNotFire(final GameData data) {
    return data.getProperties().get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false);
  }

  public static boolean getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(final GameData data) {
    return data.getProperties().get(NAVAL_UNITS_MAY_NOT_NONCOMBAT_MOVE_INTO_CONTROLLED_SEA_ZONES, false);
  }

  public static boolean getUnitsMayGiveBonusMovement(final GameData data) {
    return data.getProperties().get(UNITS_MAY_GIVE_BONUS_MOVEMENT, false);
  }

  public static boolean getCaptureUnitsOnEnteringTerritory(final GameData data) {
    return data.getProperties().get(CAPTURE_UNITS_ON_ENTERING_TERRITORY, false);
  }

  public static boolean getOnEnteringUnitsDestroyedInsteadOfCaptured(final GameData data) {
    return data.getProperties().get(DESTROY_UNITS_ON_ENTERING_TERRITORY, false);
  }

  public static boolean getDamageFromBombingDoneToUnitsInsteadOfTerritories(final GameData data) {
    return data.getProperties().get(DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES,
        data.getProperties().get(SBR_AFFECTS_UNIT_PRODUCTION, false));
  }

  public static boolean getNeutralFlyoverAllowed(final GameData data) {
    return data.getProperties().get(NEUTRAL_FLYOVER_ALLOWED, false);
  }

  public static boolean getUnitsCanBeChangedOnCapture(final GameData data) {
    return data.getProperties().get(UNITS_CAN_BE_CHANGED_ON_CAPTURE, false);
  }

  public static boolean getUsePolitics(final GameData data) {
    return data.getProperties().get(USE_POLITICS, false);
  }

  public static int getIncomePercentage(final PlayerID playerId, final GameData data) {
    return data.getProperties().get(Constants.getIncomePercentageFor(playerId), 100);
  }

  public static int getPuIncomeBonus(final PlayerID playerId, final GameData data) {
    return data.getProperties().get(Constants.getPuIncomeBonus(playerId), 0);
  }

  public static int getRelationshipsLastExtraRounds(final GameData data) {
    return data.getProperties().get(RELATIONSHIPS_LAST_EXTRA_ROUNDS, 0);
  }

  public static boolean getAlliancesCanChainTogether(final GameData data) {
    return data.getProperties().get(ALLIANCES_CAN_CHAIN_TOGETHER, false);
  }

  public static boolean getRaidsMayBePreceededByAirBattles(final GameData data) {
    return data.getProperties().get(RAIDS_MAY_BE_PRECEEDED_BY_AIR_BATTLES, false);
  }

  public static boolean getBattlesMayBePreceededByAirBattles(final GameData data) {
    return data.getProperties().get(BATTLES_MAY_BE_PRECEEDED_BY_AIR_BATTLES, false);
  }

  public static boolean getUseKamikazeSuicideAttacks(final GameData data) {
    return data.getProperties().get(USE_KAMIKAZE_SUICIDE_ATTACKS, false);
  }

  public static boolean getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(final GameData data) {
    return data.getProperties().get(KAMIKAZE_SUICIDE_ATTACKS_DONE_BY_CURRENT_TERRITORY_OWNER, false);
  }

  public static boolean getForceAaAttacksForLastStepOfFlyOver(final GameData data) {
    return data.getProperties().get(FORCE_AA_ATTACKS_FOR_LAST_STEP_OF_FLY_OVER, false);
  }

  public static boolean getParatroopersCanAttackDeepIntoEnemyTerritory(final GameData data) {
    return data.getProperties().get(PARATROOPERS_CAN_ATTACK_DEEP_INTO_ENEMY_TERRITORY, false);
  }

  public static boolean getUseBombingMaxDiceSidesAndBonus(final GameData data) {
    return data.getProperties().get(USE_BOMBING_MAX_DICE_SIDES_AND_BONUS, false);
  }

  public static boolean getConvoyBlockadesRollDiceForCost(final GameData data) {
    return data.getProperties().get(CONVOY_BLOCKADES_ROLL_DICE_FOR_COST, false);
  }

  public static boolean getAirborneAttacksOnlyInExistingBattles(final GameData data) {
    return data.getProperties().get(AIRBORNE_ATTACKS_ONLY_IN_EXISTING_BATTLES, false);
  }

  public static boolean getAirborneAttacksOnlyInEnemyTerritories(final GameData data) {
    return data.getProperties().get(AIRBORNE_ATTACKS_ONLY_IN_ENEMY_TERRITORIES, false);
  }

  public static boolean getSubsCanEndNonCombatMoveWithEnemies(final GameData data) {
    return data.getProperties().get(SUBS_CAN_END_NONCOMBAT_MOVE_WITH_ENEMIES, false);
  }

  public static boolean getRemoveAllTechTokensAtEndOfTurn(final GameData data) {
    return data.getProperties().get(REMOVE_ALL_TECH_TOKENS_AT_END_OF_TURN, false);
  }

  public static boolean getKamikazeSuicideAttacksOnlyWhereBattlesAre(final GameData data) {
    return data.getProperties().get(KAMIKAZE_SUICIDE_ATTACKS_ONLY_WHERE_BATTLES_ARE, false);
  }

  public static boolean getSubmarinesPreventUnescortedAmphibiousAssaults(final GameData data) {
    return data.getProperties().get(SUBMARINES_PREVENT_UNESCORTED_AMPHIBIOUS_ASSAULTS, false);
  }

  public static boolean getSubmarinesDefendingMaySubmergeOrRetreat(final GameData data) {
    return data.getProperties().get(SUBMARINES_DEFENDING_MAY_SUBMERGE_OR_RETREAT, false);
  }

  public static int getAirBattleRounds(final GameData data) {
    return data.getProperties().get(AIR_BATTLE_ROUNDS, 1);
  }

  public static int getSeaBattleRounds(final GameData data) {
    // negative = infinite
    return data.getProperties().get(SEA_BATTLE_ROUNDS, -1);
  }

  public static int getLandBattleRounds(final GameData data) {
    // negative = infinite
    return data.getProperties().get(LAND_BATTLE_ROUNDS, -1);
  }

  public static boolean getAirBattleAttackersCanRetreat(final GameData data) {
    return data.getProperties().get(AIR_BATTLE_ATTACKERS_CAN_RETREAT, false);
  }

  public static boolean getAirBattleDefendersCanRetreat(final GameData data) {
    return data.getProperties().get(AIR_BATTLE_DEFENDERS_CAN_RETREAT, false);
  }

  public static boolean getCanScrambleIntoAirBattles(final GameData data) {
    return data.getProperties().get(CAN_SCRAMBLE_INTO_AIR_BATTLES, false);
  }

  public static boolean getTerritoriesAreAssignedRandomly(final GameData data) {
    return data.getProperties().get(TERRITORIES_ARE_ASSIGNED_RANDOMLY, false);
  }

  public static boolean getUseFuelCost(final GameData data) {
    return data.getProperties().get(USE_FUEL_COST, false);
  }

  public static boolean getRetreatingUnitsRemainInPlace(final GameData data) {
    return data.getProperties().get(RETREATING_UNITS_REMAIN_IN_PLACE, false);
  }

  public static boolean getContestedTerritoriesProduceNoIncome(final GameData data) {
    return data.getProperties().get(CONTESTED_TERRITORIES_PRODUCE_NO_INCOME, false);
  }

  public static boolean getSeaBattlesMayBeIgnored(final GameData data) {
    return data.getProperties().get(SEA_BATTLES_MAY_BE_IGNORED, false);
  }

  public static boolean getAbandonedTerritoriesMayBeTakenOverImmediately(final GameData data) {
    return data.getProperties().get(ABANDONED_TERRITORIES_MAY_BE_TAKEN_OVER_IMMEDIATELY, false);
  }

  public static boolean getDisabledPlayersAssetsDeleted(final GameData data) {
    return data.getProperties().get(DISABLED_PLAYERS_ASSETS_DELETED, false);
  }

  public static boolean getControlAllCanalsBetweenTerritoriesToPass(final GameData data) {
    return data.getProperties().get(CONTROL_ALL_CANALS_BETWEEN_TERRITORIES_TO_PASS, false);
  }

  public static boolean getUnitsCanLoadInHostileSeaZones(final GameData data) {
    return data.getProperties().get(UNITS_CAN_LOAD_IN_HOSTILE_SEA_ZONES, false);
  }
}
