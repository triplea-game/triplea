package games.strategy.triplea;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.properties.GameProperties;

/** Provides typed access to the properties of GameData. */
public final class Properties implements Constants {
  private Properties() {}

  // These should always default to false, if boolean, and if not should default to whatever is the
  // "default" behavior  of TripleA.  If you want something to default to "true", when change the
  // wording of the constant to make it a negative of itself, then default to false.
  // (ex: "Do not do something", false; instead of "Do something", true;)

  public static int getNeutralCharge(final GameProperties properties) {
    return properties.get(NEUTRAL_CHARGE_PROPERTY, 0);
  }

  public static int getFactoriesPerCountry(final GameProperties properties) {
    return properties.get(FACTORIES_PER_COUNTRY_PROPERTY, 1);
  }

  public static boolean getTwoHitBattleships(final GameProperties properties) {
    return properties.get(TWO_HIT_BATTLESHIP_PROPERTY, false);
  }

  public static boolean getWW2V2(final GameProperties properties) {
    return properties.get(WW2V2, false);
  }

  /** World War 2 Version 3. */
  public static boolean getWW2V3(final GameProperties properties) {
    return properties.get(WW2V3, false);
  }

  /** Can the player select the type of technology they are rolling for. */
  public static boolean getWW2V3TechModel(final GameProperties properties) {
    return properties.get(WW2V3_TECH_MODEL, false);
  }

  public static boolean getPartialAmphibiousRetreat(final GameProperties properties) {
    return properties.get(PARTIAL_AMPHIBIOUS_RETREAT, false);
  }

  public static boolean getTotalVictory(final GameProperties properties) {
    return properties.get(TOTAL_VICTORY, false);
  }

  public static boolean getHonorableSurrender(final GameProperties properties) {
    return properties.get(HONORABLE_SURRENDER, false);
  }

  public static boolean getProjectionOfPower(final GameProperties properties) {
    return properties.get(PROJECTION_OF_POWER, false);
  }

  public static boolean getAllRocketsAttack(final GameProperties properties) {
    return properties.get(ALL_ROCKETS_ATTACK, false);
  }

  public static boolean getNeutralsImpassable(final GameProperties properties) {
    return properties.get(NEUTRALS_ARE_IMPASSABLE, false);
  }

  public static boolean getNeutralsBlitzable(final GameProperties properties) {
    return properties.get(NEUTRALS_ARE_BLITZABLE, false);
  }

  public static boolean getRocketsCanFlyOverImpassables(final GameProperties properties) {
    return properties.get(ROCKETS_CAN_FLY_OVER_IMPASSABLES, false);
  }

  public static boolean getSequentiallyTargetedRockets(final GameProperties properties) {
    return properties.get(TARGET_ROCKETS_SEQUENTIALLY_AND_AFTER_SBR, false);
  }

  /** Pacific Theater. */
  public static boolean getPacificTheater(final GameProperties properties) {
    return properties.get(PACIFIC_THEATER, false);
  }

  /** Economic Victory Condition. */
  public static boolean getEconomicVictory(final GameProperties properties) {
    return properties.get(ECONOMIC_VICTORY, false);
  }

  /** Triggered Victory Condition. */
  public static boolean getTriggeredVictory(final GameProperties properties) {
    return properties.get(TRIGGERED_VICTORY, false);
  }

  /** Indicates the number of units that can be placed at a factory is restricted. */
  public static boolean getPlacementRestrictedByFactory(final GameProperties properties) {
    return properties.get(PLACEMENT_RESTRICTED_BY_FACTORY, false);
  }

  /** Can the player select the type of technology they are rolling for. */
  public static boolean getSelectableTechRoll(final GameProperties properties) {
    return properties.get(SELECTABLE_TECH_ROLL, false);
  }

  /** Use Advanced Technology. */
  public static boolean getTechDevelopment(final GameProperties properties) {
    return properties.get(TECH_DEVELOPMENT, false);
  }

  /** Are transports restricted from unloading in multiple territories in a turn. */
  public static boolean getTransportUnloadRestricted(final GameProperties properties) {
    return properties.get(TRANSPORT_UNLOAD_RESTRICTED, false);
  }

  /** Are AA casualties chosen randomly. */
  public static boolean getRandomAaCasualties(final GameProperties properties) {
    return properties.get(RANDOM_AA_CASUALTIES, false);
  }

  /** Indicates AA dice for each type of aircraft are rolled separately. */
  public static boolean getRollAaIndividually(final GameProperties properties) {
    return properties.get(ROLL_AA_INDIVIDUALLY, false);
  }

  /**
   * Limit the damage caused by each bomber on rockets and Strategic Bomb Raids to production of
   * territory.
   */
  public static boolean getLimitRocketAndSbrDamageToProduction(final GameProperties properties) {
    return properties.get(LIMIT_ROCKET_AND_SBR_DAMAGE_TO_PRODUCTION, false);
  }

  /** Limit the TOTAL damage caused by Bombers in a turn to territory's production. */
  public static boolean getLimitSbrDamagePerTurn(final GameProperties properties) {
    return properties.get(LIMIT_SBR_DAMAGE_PER_TURN, false);
  }

  /** Limit the TOTAL damage caused by Rockets in a turn to territory's production. */
  public static boolean getLimitRocketDamagePerTurn(final GameProperties properties) {
    return properties.get(LIMIT_ROCKET_DAMAGE_PER_TURN, false);
  }

  /** Limit the TOTAL PUs lost to Bombers/Rockets in a turn to territory's production. */
  public static boolean getPuCap(final GameProperties properties) {
    return properties.get(PU_CAP, false);
  }

  /** Reduce Victory Points by Strategic Bombing. */
  public static boolean getSbrVictoryPoints(final GameProperties properties) {
    return properties.get(SBR_VICTORY_POINTS, false);
  }

  /** Are allied aircraft dependents of CVs. */
  public static boolean getAlliedAirIndependent(final GameProperties properties) {
    return properties.get(ALLIED_AIR_INDEPENDENT, false);
  }

  /** Defending subs sneak attack. */
  public static boolean getDefendingSubsSneakAttack(final GameProperties properties) {
    return properties.get(DEFENDING_SUBS_SNEAK_ATTACK, false);
  }

  /** Attacker retreat planes from Amphib assault. */
  public static boolean getAttackerRetreatPlanes(final GameProperties properties) {
    return properties.get(ATTACKER_RETREAT_PLANES, false);
  }

  /** Can surviving air at sea move to land on friendly land/carriers. */
  public static boolean getSurvivingAirMoveToLand(final GameProperties properties) {
    return properties.get(SURVIVING_AIR_MOVE_TO_LAND, false);
  }

  /** Naval Bombard casualties restricted from return fire. */
  public static boolean getNavalBombardCasualtiesReturnFire(final GameProperties properties) {
    return properties.get(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE, false);
  }

  /** Restricted from blitz through territories with factories/AA. */
  public static boolean getBlitzThroughFactoriesAndAaRestricted(final GameProperties properties) {
    return properties.get(BLITZ_THROUGH_FACTORIES_AND_AA_RESTRICTED, false);
  }

  /** Can place new units in occupied sea zones. */
  public static boolean getUnitPlacementInEnemySeas(final GameProperties properties) {
    return properties.get(UNIT_PLACEMENT_IN_ENEMY_SEAS, false);
  }

  /** Subs restricted from controlling sea zones. */
  public static boolean getSubControlSeaZoneRestricted(final GameProperties properties) {
    return properties.get(SUB_CONTROL_SEA_ZONE_RESTRICTED, false);
  }

  /** Can Transports control sea zones. */
  public static boolean getTransportControlSeaZone(final GameProperties properties) {
    return properties.get(TRANSPORT_CONTROL_SEA_ZONE, false);
  }

  /** Production restricted to 1 unit per X owned territories. */
  public static boolean getProductionPerXTerritoriesRestricted(final GameProperties properties) {
    return properties.get(PRODUCTION_PER_X_TERRITORIES_RESTRICTED, false);
  }

  /** Production restricted to 1 unit per owned territory with an PU value. */
  public static boolean getProductionPerValuedTerritoryRestricted(final GameProperties properties) {
    return properties.get(PRODUCTION_PER_VALUED_TERRITORY_RESTRICTED, false);
  }

  /** Can units be placed in any owned territory. */
  public static boolean getPlaceInAnyTerritory(final GameProperties properties) {
    return properties.get(PLACE_IN_ANY_TERRITORY, false);
  }

  /** Limit the number of units that can be in a territory. */
  public static boolean getUnitPlacementPerTerritoryRestricted(final GameProperties properties) {
    return properties.get(UNIT_PLACEMENT_PER_TERRITORY_RESTRICTED, false);
  }

  /** Movement restricted for territories. */
  public static boolean getMovementByTerritoryRestricted(final GameProperties properties) {
    return properties.get(MOVEMENT_BY_TERRITORY_RESTRICTED, false);
  }

  /** Transports restricted from being taken as casualties. */
  public static boolean getTransportCasualtiesRestricted(final GameProperties properties) {
    return properties.get(TRANSPORT_CASUALTIES_RESTRICTED, false);
  }

  /** Transports do not restrict movement of other units. */
  public static boolean getIgnoreTransportInMovement(final GameProperties properties) {
    return properties.get(IGNORE_TRANSPORT_IN_MOVEMENT, false);
  }

  /**
   * Subs do not restrict movement of other units. When 'isSub' unit option is used this sets
   * 'canBeMovedThroughByEnemies' unit option to true.
   */
  public static boolean getIgnoreSubInMovement(final GameProperties properties) {
    return properties.get(IGNORE_SUB_IN_MOVEMENT, false);
  }

  public static boolean getUnplacedUnitsLive(final GameProperties properties) {
    return properties.get(UNPLACED_UNITS_LIVE, false);
  }

  /**
   * Air restricted from attacking subs unless DD present. When 'isSub' unit option is used this
   * sets 'canNotBeTargetedBy' unit option to all air units.
   */
  public static boolean getAirAttackSubRestricted(final GameProperties properties) {
    return properties.get(AIR_ATTACK_SUB_RESTRICTED, false);
  }

  /** Allows units with zero movement to be selected to be moved. */
  public static boolean getSelectableZeroMovementUnits(final GameProperties properties) {
    return properties.get(SELECTABLE_ZERO_MOVEMENT_UNITS, false);
  }

  /**
   * Allows paratroopers to move ground units to friendly territories during non-combat move phase.
   */
  public static boolean getParatroopersCanMoveDuringNonCombat(final GameProperties properties) {
    return properties.get(PARATROOPERS_CAN_MOVE_DURING_NON_COMBAT, false);
  }

  public static boolean getSubRetreatBeforeBattle(final GameProperties properties) {
    return properties.get(SUB_RETREAT_BEFORE_BATTLE, false);
  }

  /** Shore Bombard per Ground Unit Restricted. */
  public static boolean getShoreBombardPerGroundUnitRestricted(final GameProperties properties) {
    return properties.get(SHORE_BOMBARD_PER_GROUND_UNIT_RESTRICTED, false);
  }

  /** AA restricted to Attacked Territory Only. */
  public static boolean getAaTerritoryRestricted(final GameProperties properties) {
    return properties.get(AA_TERRITORY_RESTRICTED, false);
  }

  public static boolean getMultipleAaPerTerritory(final GameProperties properties) {
    return properties.get(MULTIPLE_AA_PER_TERRITORY, false);
  }

  public static boolean getNationalObjectives(final GameProperties properties) {
    return properties.get(NATIONAL_OBJECTIVES, false);
  }

  public static boolean getTriggers(final GameProperties properties) {
    return properties.get(USE_TRIGGERS, false);
  }

  public static boolean getAlwaysOnAa(final GameProperties properties) {
    return properties.get(ALWAYS_ON_AA_PROPERTY, false);
  }

  public static boolean getLhtrCarrierProductionRules(final GameProperties properties) {
    return properties.get(LHTR_CARRIER_PRODUCTION_RULES, false);
  }

  /** Atomic units of the fighter/carrier production rules. */
  public static boolean getProduceFightersOnCarriers(final GameProperties properties) {
    return properties.get(CAN_PRODUCE_FIGHTERS_ON_CARRIERS, false);
  }

  public static boolean getProduceNewFightersOnOldCarriers(final GameProperties properties) {
    return properties.get(PRODUCE_NEW_FIGHTERS_ON_OLD_CARRIERS, false);
  }

  public static boolean getMoveExistingFightersToNewCarriers(final GameProperties properties) {
    return properties.get(MOVE_EXISTING_FIGHTERS_TO_NEW_CARRIERS, false);
  }

  public static boolean getLandExistingFightersOnNewCarriers(final GameProperties properties) {
    return properties.get(LAND_EXISTING_FIGHTERS_ON_NEW_CARRIERS, false);
  }

  public static int getHeavyBomberDiceRolls(final GameProperties properties) {
    return properties.get(HEAVY_BOMBER_DICE_ROLLS, 2);
  }

  public static boolean getBattleshipsRepairAtEndOfRound(final GameProperties properties) {
    return properties.get(TWO_HIT_BATTLESHIPS_REPAIR_END_OF_TURN, false);
  }

  public static boolean getBattleshipsRepairAtBeginningOfRound(final GameProperties properties) {
    return properties.get(TWO_HIT_BATTLESHIPS_REPAIR_BEGINNING_OF_TURN, false);
  }

  public static boolean getTwoHitPointUnitsRequireRepairFacilities(
      final GameProperties properties) {
    return properties.get(TWO_HITPOINT_UNITS_REQUIRE_REPAIR_FACILITIES, false);
  }

  public static boolean getChooseAaCasualties(final GameProperties properties) {
    return properties.get(CHOOSE_AA, false);
  }

  public static boolean getSubmersibleSubs(final GameProperties properties) {
    return properties.get(SUBMERSIBLE_SUBS, false);
  }

  public static boolean getUseDestroyersAndArtillery(final GameProperties properties) {
    return properties.get(USE_DESTROYERS_AND_ARTILLERY, false);
  }

  public static boolean getUseShipyards(final GameProperties properties) {
    return properties.get(USE_SHIPYARDS, false);
  }

  public static boolean getLowLuck(final GameProperties properties) {
    return properties.get(LOW_LUCK, false);
  }

  public static boolean getLowLuckAaOnly(final GameProperties properties) {
    return properties.get(LL_AA_ONLY, false);
  }

  public static boolean getLowLuckTechOnly(final GameProperties properties) {
    return properties.get(LL_TECH_ONLY, false);
  }

  public static boolean getLowLuckDamageOnly(final GameProperties properties) {
    return properties.get(LL_DAMAGE_ONLY, false);
  }

  public static boolean getKamikazeAirplanes(final GameProperties properties) {
    return properties.get(KAMIKAZE, false);
  }

  public static boolean getLhtrHeavyBombers(final GameProperties properties) {
    return properties.get(LHTR_HEAVY_BOMBERS, false);
  }

  public static int getSuperSubDefenseBonus(final GameProperties properties) {
    return properties.get(SUPER_SUB_DEFENSE_BONUS, 0);
  }

  public static boolean getScrambleRulesInEffect(final GameProperties properties) {
    return properties.get(SCRAMBLE_RULES_IN_EFFECT, false);
  }

  public static boolean getScrambledUnitsReturnToBase(final GameProperties properties) {
    return properties.get(SCRAMBLED_UNITS_RETURN_TO_BASE, false);
  }

  public static boolean getScrambleToSeaOnly(final GameProperties properties) {
    return properties.get(SCRAMBLE_TO_SEA_ONLY, false);
  }

  public static boolean getScrambleFromIslandOnly(final GameProperties properties) {
    return properties.get(SCRAMBLE_FROM_ISLAND_ONLY, false);
  }

  public static boolean getScrambleToAnyAmphibiousAssault(final GameProperties properties) {
    return properties.get(SCRAMBLE_TO_ANY_AMPHIBIOUS_ASSAULT, false);
  }

  public static int getPuMultiplier(final GameProperties properties) {
    return properties.get(PU_MULTIPLIER, 1);
  }

  public static boolean getUnlimitedConstructions(final GameProperties properties) {
    return properties.get(UNLIMITED_CONSTRUCTIONS, false);
  }

  public static boolean getMoreConstructionsWithoutFactory(final GameProperties properties) {
    return properties.get(MORE_CONSTRUCTIONS_WITHOUT_FACTORY, false);
  }

  public static boolean getMoreConstructionsWithFactory(final GameProperties properties) {
    return properties.get(MORE_CONSTRUCTIONS_WITH_FACTORY, false);
  }

  public static boolean getUnitPlacementRestrictions(final GameProperties properties) {
    return properties.get(UNIT_PLACEMENT_RESTRICTIONS, false);
  }

  public static boolean getGiveUnitsByTerritory(final GameProperties properties) {
    return properties.get(GIVE_UNITS_BY_TERRITORY, false);
  }

  public static boolean getUnitsCanBeDestroyedInsteadOfCaptured(final GameProperties properties) {
    return properties.get(UNITS_CAN_BE_DESTROYED_INSTEAD_OF_CAPTURED, false);
  }

  public static boolean getSuicideAndMunitionCasualtiesRestricted(final GameProperties properties) {
    return properties.get(SUICIDE_AND_MUNITION_CASUALTIES_RESTRICTED, false);
  }

  public static boolean getDefendingSuicideAndMunitionUnitsDoNotFire(
      final GameProperties properties) {
    return properties.get(DEFENDING_SUICIDE_AND_MUNITION_UNITS_DO_NOT_FIRE, false);
  }

  public static boolean getNavalUnitsMayNotNonCombatMoveIntoControlledSeaZones(
      final GameProperties properties) {
    return properties.get(NAVAL_UNITS_MAY_NOT_NONCOMBAT_MOVE_INTO_CONTROLLED_SEA_ZONES, false);
  }

  public static boolean getUnitsMayGiveBonusMovement(final GameProperties properties) {
    return properties.get(UNITS_MAY_GIVE_BONUS_MOVEMENT, false);
  }

  public static boolean getCaptureUnitsOnEnteringTerritory(final GameProperties properties) {
    return properties.get(CAPTURE_UNITS_ON_ENTERING_TERRITORY, false);
  }

  public static boolean getOnEnteringUnitsDestroyedInsteadOfCaptured(
      final GameProperties properties) {
    return properties.get(DESTROY_UNITS_ON_ENTERING_TERRITORY, false);
  }

  public static boolean getDamageFromBombingDoneToUnitsInsteadOfTerritories(
      final GameProperties properties) {
    return properties.get(
        DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES,
        properties.get(SBR_AFFECTS_UNIT_PRODUCTION, false));
  }

  public static boolean getNeutralFlyoverAllowed(final GameProperties properties) {
    return properties.get(NEUTRAL_FLYOVER_ALLOWED, false);
  }

  public static boolean getUnitsCanBeChangedOnCapture(final GameProperties properties) {
    return properties.get(UNITS_CAN_BE_CHANGED_ON_CAPTURE, false);
  }

  public static boolean getUsePolitics(final GameProperties properties) {
    return properties.get(USE_POLITICS, false);
  }

  public static int getIncomePercentage(
      final GamePlayer gamePlayer, final GameProperties properties) {
    return properties.get(Constants.getIncomePercentageFor(gamePlayer), 100);
  }

  public static int getPuIncomeBonus(final GamePlayer gamePlayer, final GameProperties properties) {
    return properties.get(Constants.getPuIncomeBonus(gamePlayer), 0);
  }

  public static int getRelationshipsLastExtraRounds(final GameProperties properties) {
    return properties.get(RELATIONSHIPS_LAST_EXTRA_ROUNDS, 0);
  }

  public static boolean getAlliancesCanChainTogether(final GameProperties properties) {
    return properties.get(ALLIANCES_CAN_CHAIN_TOGETHER, false);
  }

  public static boolean getUseNonAirUnitsInNormalBattle(final GameProperties properties) {
    return properties.get(NON_AIR_SBR_UNITS_IN_NORMAL_BATTLE,false);
  }
  public static boolean getRaidsMayBePreceededByAirBattles(final GameProperties properties) {
    return properties.get(RAIDS_MAY_BE_PRECEEDED_BY_AIR_BATTLES, false);
  }

  public static boolean getBattlesMayBePreceededByAirBattles(final GameProperties properties) {
    return properties.get(BATTLES_MAY_BE_PRECEEDED_BY_AIR_BATTLES, false);
  }

  public static boolean getUseKamikazeSuicideAttacks(final GameProperties properties) {
    return properties.get(USE_KAMIKAZE_SUICIDE_ATTACKS, false);
  }

  public static boolean getKamikazeSuicideAttacksDoneByCurrentTerritoryOwner(
      final GameProperties properties) {
    return properties.get(KAMIKAZE_SUICIDE_ATTACKS_DONE_BY_CURRENT_TERRITORY_OWNER, false);
  }

  public static boolean getForceAaAttacksForLastStepOfFlyOver(final GameProperties properties) {
    return properties.get(FORCE_AA_ATTACKS_FOR_LAST_STEP_OF_FLY_OVER, false);
  }

  public static boolean getParatroopersCanAttackDeepIntoEnemyTerritory(
      final GameProperties properties) {
    return properties.get(PARATROOPERS_CAN_ATTACK_DEEP_INTO_ENEMY_TERRITORY, false);
  }

  public static boolean getUseBombingMaxDiceSidesAndBonus(final GameProperties properties) {
    return properties.get(USE_BOMBING_MAX_DICE_SIDES_AND_BONUS, false);
  }

  public static boolean getConvoyBlockadesRollDiceForCost(final GameProperties properties) {
    return properties.get(CONVOY_BLOCKADES_ROLL_DICE_FOR_COST, false);
  }

  public static boolean getAirborneAttacksOnlyInExistingBattles(final GameProperties properties) {
    return properties.get(AIRBORNE_ATTACKS_ONLY_IN_EXISTING_BATTLES, false);
  }

  public static boolean getAirborneAttacksOnlyInEnemyTerritories(final GameProperties properties) {
    return properties.get(AIRBORNE_ATTACKS_ONLY_IN_ENEMY_TERRITORIES, false);
  }

  public static boolean getSubsCanEndNonCombatMoveWithEnemies(final GameProperties properties) {
    return properties.get(SUBS_CAN_END_NONCOMBAT_MOVE_WITH_ENEMIES, false);
  }

  public static boolean getRemoveAllTechTokensAtEndOfTurn(final GameProperties properties) {
    return properties.get(REMOVE_ALL_TECH_TOKENS_AT_END_OF_TURN, false);
  }

  public static boolean getKamikazeSuicideAttacksOnlyWhereBattlesAre(
      final GameProperties properties) {
    return properties.get(KAMIKAZE_SUICIDE_ATTACKS_ONLY_WHERE_BATTLES_ARE, false);
  }

  public static boolean getSubmarinesPreventUnescortedAmphibiousAssaults(
      final GameProperties properties) {
    return properties.get(SUBMARINES_PREVENT_UNESCORTED_AMPHIBIOUS_ASSAULTS, false);
  }

  public static boolean getSubmarinesDefendingMaySubmergeOrRetreat(
      final GameProperties properties) {
    return properties.get(SUBMARINES_DEFENDING_MAY_SUBMERGE_OR_RETREAT, false);
  }

  public static int getAirBattleRounds(final GameProperties properties) {
    return properties.get(AIR_BATTLE_ROUNDS, 1);
  }

  public static int getSeaBattleRounds(final GameProperties properties) {
    // negative = infinite
    return properties.get(SEA_BATTLE_ROUNDS, -1);
  }

  public static int getLandBattleRounds(final GameProperties properties) {
    // negative = infinite
    return properties.get(LAND_BATTLE_ROUNDS, -1);
  }

  public static boolean getSeaBattlesMayBeIgnored(final GameProperties properties) {
    return properties.get(SEA_BATTLES_MAY_BE_IGNORED, false);
  }

  public static boolean getLandBattlesMayBeIgnored(final GameProperties properties) {
    return properties.get(LAND_BATTLES_MAY_BE_IGNORED, false);
  }

  public static boolean getAirBattleAttackersCanRetreat(final GameProperties properties) {
    return properties.get(AIR_BATTLE_ATTACKERS_CAN_RETREAT, false);
  }

  public static boolean getAirBattleDefendersCanRetreat(final GameProperties properties) {
    return properties.get(AIR_BATTLE_DEFENDERS_CAN_RETREAT, false);
  }

  public static boolean getCanScrambleIntoAirBattles(final GameProperties properties) {
    return properties.get(CAN_SCRAMBLE_INTO_AIR_BATTLES, false);
  }

  public static boolean getTerritoriesAreAssignedRandomly(final GameProperties properties) {
    return properties.get(TERRITORIES_ARE_ASSIGNED_RANDOMLY, false);
  }

  public static boolean getUseFuelCost(final GameProperties properties) {
    return properties.get(USE_FUEL_COST, false);
  }

  public static boolean getRetreatingUnitsRemainInPlace(final GameProperties properties) {
    return properties.get(RETREATING_UNITS_REMAIN_IN_PLACE, false);
  }

  public static boolean getContestedTerritoriesProduceNoIncome(final GameProperties properties) {
    return properties.get(CONTESTED_TERRITORIES_PRODUCE_NO_INCOME, false);
  }

  public static boolean getAllUnitsCanAttackFromContestedTerritories(GameProperties properties) {
    return properties.get(ALL_UNITS_CAN_ATTACK_FROM_CONTESTED_TERRITORIES, false);
  }

  public static boolean getAbandonedTerritoriesMayBeTakenOverImmediately(
      final GameProperties properties) {
    return properties.get(ABANDONED_TERRITORIES_MAY_BE_TAKEN_OVER_IMMEDIATELY, false);
  }

  public static boolean getDisabledPlayersAssetsDeleted(final GameProperties properties) {
    return properties.get(DISABLED_PLAYERS_ASSETS_DELETED, false);
  }

  public static boolean getControlAllCanalsBetweenTerritoriesToPass(
      final GameProperties properties) {
    return properties.get(CONTROL_ALL_CANALS_BETWEEN_TERRITORIES_TO_PASS, false);
  }

  public static boolean getEnterTerritoriesWithHigherMovementCostsThenRemainingMovement(
      final GameProperties properties) {
    return properties.get(
        ENTER_TERRITORIES_WITH_HIGHER_MOVEMENT_COSTS_THEN_REMAINING_MOVEMENT, false);
  }

  public static boolean getUnitsCanLoadInHostileSeaZones(final GameProperties properties) {
    return properties.get(UNITS_CAN_LOAD_IN_HOSTILE_SEA_ZONES, false);
  }
}
