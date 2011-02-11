/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.triplea;

import games.strategy.engine.data.*;

/**
 * <p>
 * Title: TripleA
 * </p>
 * 
 * @author Sean Bridges
 * 
 */

public class Properties implements Constants {

    public static int getNeutralCharge(GameData data) {
        try {
            return Integer.parseInt((String) data.getProperties().get(NEUTRAL_CHARGE_PROPERTY));
        } catch (NumberFormatException e) {
            return 3;
        }

    }

    public static int getFactoriesPerCountry(GameData data) {
        try {
            return Integer.parseInt((String) data.getProperties().get(
                FACTORIES_PER_COUNTRY_PROPERTY));
        } catch (NumberFormatException e) {
            return 1;
        }

    }

    public static boolean getTwoHitBattleships(GameData data) {

        return data.getProperties().get(TWO_HIT_BATTLESHIP_PROPERTY, false);

    }

    public static boolean getWW2V2(GameData data) {

        return data.getProperties().get(WW2V2, false);

    }

    public static boolean getPreviousUnitsFight(GameData data) {
        return data.getProperties().get(PREVIOUS_UNITS_FIGHT, false);

    }

    public static boolean getPartialAmphibiousRetreat(GameData data) {
        return data.getProperties().get(PARTIAL_AMPHIBIOUS_RETREAT, false);

    }

    public static boolean getTotalVictory(GameData data) {

        return data.getProperties().get(TOTAL_VICTORY, false);

    }

    public static boolean getHonorableSurrender(GameData data) {

        return data.getProperties().get(HONORABLE_SURRENDER, false);

    }

    public static boolean getProjectionOfPower(GameData data) {

        return data.getProperties().get(PROJECTION_OF_POWER, false);

    }

    public static boolean getAllRocketsAttack(GameData data) {

        return data.getProperties().get(ALL_ROCKETS_ATTACK, false);

    }

    public static boolean getNeutralsImpassable(GameData data) {

        return data.getProperties().get(NEUTRALS_ARE_IMPASSABLE, false);

    }

    public static boolean getNeutralsBlitzable(GameData data) {

        return data.getProperties().get(NEUTRALS_ARE_BLITZABLE, false);

    }

    public static boolean getRocketsCanFlyOverImpassables(GameData data) {

        return data.getProperties().get(ROCKETS_CAN_FLY_OVER_IMPASSABLES, false);

    }

    /*
     * Pacific Theater
     */
    public static boolean getPacificTheater(GameData data) {

        return data.getProperties().get(PACIFIC_THEATER, false);

    }

    /*
     * World War 2 Version 3
     */
    public static boolean getWW2V3(GameData data) {

        return data.getProperties().get(WW2V3, false);

    }

    /*
     * Economic Victory Condition
     */
    public static boolean getEconomicVictory(GameData data) {

        return data.getProperties().get(ECONOMIC_VICTORY, false);
    }

    /*
     * Restrict the number of units that can be placed at a factory.
     */
    public static boolean getPlacementRestrictedByFactory(GameData data) {

        return data.getProperties().get(PLACEMENT_RESTRICTED_BY_FACTORY, false);

    }

    /*
     * Can the player select the type of technology they are rolling for
     */
    public static boolean getSelectableTechRoll(GameData data) {

        return data.getProperties().get(SELECTABLE_TECH_ROLL, false);

    }

    /*
     * Can the player select the type of technology they are rolling for
     */
    public static boolean getWW2V3TechModel(GameData data) {

        return data.getProperties().get(WW2V3_Tech_Model, false);

    }

    /*
     * Use Advanced Technology
     */
    public static boolean getTechDevelopment(GameData data) {

        return data.getProperties().get(TECH_DEVELOPMENT, false);

    }

    /*
     * Are transports restricted from unloading in multiple territories in a
     * turn
     */
    public static boolean getTransportUnloadRestricted(GameData data) {

        return data.getProperties().get(TRANSPORT_UNLOAD_RESTRICTED, false);

    }

    /*
     * Are AA casualties chosen randomly
     */
    public static boolean getRandomAACasualties(GameData data) {

        return data.getProperties().get(RANDOM_AA_CASUALTIES, false);

    }

    /*
     * Are AA casualties chosen randomly
     */
    public static boolean getRollAAIndividually(GameData data) {

        return data.getProperties().get(ROLL_AA_INDIVIDUALLY, false);

    }

    /*
     * Limit the damage caused by each bomber on Strategic Bomb Raids to
     * production of territory
     */
    public static boolean getLimitSBRDamageToProduction(GameData data) {

        return data.getProperties().get(LIMIT_SBR_DAMAGE_TO_PRODUCTION, false);

    }

    /*
     * Limit the damage caused on Rocket attacks to production of territory
     */
    public static boolean getLimitRocketDamageToProduction(GameData data) {

        return data.getProperties().get(LIMIT_ROCKET_DAMAGE_TO_PRODUCTION, false);

    }

    /*
     * Limit the TOTAL damage caused by Bombers in a turn to territory's
     * production
     */
    public static boolean getLimitSBRDamagePerTurn(GameData data) {

        return data.getProperties().get(LIMIT_SBR_DAMAGE_PER_TURN, false);

    }

    /*
     * Limit the TOTAL damage caused by Rockets in a turn to territory's
     * production
     */
    public static boolean getLimitRocketDamagePerTurn(GameData data) {

        return data.getProperties().get(LIMIT_ROCKET_DAMAGE_PER_TURN, false);

    }

    /*
     * Limit the TOTAL PUs lost to Bombers/Rockets in a turn to territory's
     * production
     */
    public static boolean getPUCap(GameData data) {

        return data.getProperties().get(PU_CAP, false);

    }

    /*
     * Reduce Victory Points by Strategic Bombing
     */
    public static boolean getSBRVictoryPoint(GameData data) {

        return data.getProperties().get(SBR_VICTORY_POINTS, false);

    }

    /*
     * Allow x rocket attack(s) per defending factory
     */
    public static boolean getRocketAttackPerFactoryRestricted(GameData data) {

        return data.getProperties().get(ROCKET_ATTACK_PER_FACTORY_RESTRICTED, false);

    }

    /*
     * Are allied aircraft dependents of CVs
     */
    public static boolean getAlliedAirDependents(GameData data) {

        return data.getProperties().get(ALLIED_AIR_DEPENDENTS, false);

    }

    /*
     * Defending subs sneak attack
     */
    public static boolean getDefendingSubsSneakAttack(GameData data) {

        return data.getProperties().get(DEFENDING_SUBS_SNEAK_ATTACK, false);

    }

    /*
     * Attacker retreat planes from Amphib assault
     */
    public static boolean getAttackerRetreatPlanes(GameData data) {

        return data.getProperties().get(ATTACKER_RETREAT_PLANES, false);

    }

    /*
     * Can surviving air at sea move to land on friendly land/carriers
     */
    public static boolean getSurvivingAirMoveToLand(GameData data) {

        return data.getProperties().get(SURVIVING_AIR_MOVE_TO_LAND, false);

    }

    /*
     * Naval Bombard casualties restricted from return fire
     */
    public static boolean getNavalBombardCasualtiesReturnFireRestricted(GameData data) {

        return data.getProperties().get(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE_RESTRICTED, false);

    }

    /*
     * Restricted from blitz through territories with factories/AA
     */
    public static boolean getBlitzThroughFactoriesAndAARestricted(GameData data) {

        return data.getProperties().get(BLITZ_THROUGH_FACTORIES_AND_AA_RESTRICTED, false);

    }

    /*
     * Can place new units in occupied sea zones
     */
    public static boolean getUnitPlacementInEnemySeas(GameData data) {

        return data.getProperties().get(UNIT_PLACEMENT_IN_ENEMY_SEAS, false);

    }

    /*
     * Subs restricted from controlling sea zones
     */
    public static boolean getSubControlSeaZoneRestricted(GameData data) {

        return data.getProperties().get(SUB_CONTROL_SEA_ZONE_RESTRICTED, false);

    }

    /*
     * Can Transports control sea zones
     */
    public static boolean getTransportControlSeaZone(GameData data) {

        return data.getProperties().get(TRANSPORT_CONTROL_SEA_ZONE, false);

    }

    /*
     * Production restricted to 1 unit per X owned territories
     */
    public static boolean getProductionPerXTerritoriesRestricted(GameData data) {

        return data.getProperties().get(PRODUCTION_PER_X_TERRITORIES_RESTRICTED, false);

    }

    /*
     * Production restricted to 1 unit per owned territory with an PU value
     */
    public static boolean getProductionPerValuedTerritoryRestricted(GameData data) {

        return data.getProperties().get(PRODUCTION_PER_VALUED_TERRITORY_RESTRICTED, false);

    }

    /*
     * Can units be placed in any owned territory
     */
    public static boolean getPlaceInAnyTerritory(GameData data) {

        return data.getProperties().get(PLACE_IN_ANY_TERRITORY, false);

    }

    /*
     * Limit the number of units that can be in a territory
     */
    public static boolean getUnitPlacementPerTerritoryRestricted(GameData data) {

        return data.getProperties().get(UNIT_PLACEMENT_PER_TERRITORY_RESTRICTED, false);

    }

    /*
     * Movement restricted for territories
     */
    public static boolean getMovementByTerritoryRestricted(GameData data) {

        return data.getProperties().get(MOVEMENT_BY_TERRITORY_RESTRICTED, false);

    }

    /*
     * Transports restricted from being taken as casualties
     */
    public static boolean getTransportCasualtiesRestricted(GameData data) {

        return data.getProperties().get(TRANSPORT_CASUALTIES_RESTRICTED, false);

    }

    /*
     * Transports do not restrict movement of other units
     */
    public static boolean getIgnoreTransportInMovement(GameData data) {

        return data.getProperties().get(IGNORE_TRANSPORT_IN_MOVEMENT, false);

    }

    /*
     * Subs do not restrict movement of other units
     */
    public static boolean getIgnoreSubInMovement(GameData data) {

        return data.getProperties().get(IGNORE_SUB_IN_MOVEMENT, false);

    }

    /*
     * May units with 0 attack power enter combat
     */
    public static boolean getHariKariUnits(GameData data) {

        return data.getProperties().get(HARI_KARI_UNITS, false);

    }

    public static boolean getUnplacedUnitsLive(GameData data) {

        return data.getProperties().get(UNPLACED_UNITS_LIVE, false);
    }
    

    /*
     * Air restricted from attacking subs unless DD present
     */
    public static boolean getAirAttackSubRestricted(GameData data) {

        return data.getProperties().get(AIR_ATTACK_SUB_RESTRICTED, false);

    }
    

    /*
     * Allows units with zero movement to be selected to be moved
     */
    public static boolean getSelectableZeroMovementUnits(GameData data) {

        return data.getProperties().get(SELECTABLE_ZERO_MOVEMENT_UNITS, false);

    }
    

    /*
     * Allows paratroopers to move ground units to friendly territories during non-combat move phase
     */
    public static boolean getParatroopersCanMoveDuringNonCombat(GameData data) {

        return data.getProperties().get(PARATROOPERS_CAN_MOVE_DURING_NON_COMBAT, false);

    }
    
	/*
	* Display units as counters: stacked instead of a number drawn under the unit
	* the number means the maximum stack size, 0 means default behavior, 
	* if a stack is above max stack size the number is drawn in the top right corner of the counter
	*/
	public static int getCountersDisplay(GameData data) {
		try {
			return Integer.parseInt((String) data.getProperties().get(COUNTERS_DISPLAY));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
    /*
     * Sub retreat before battle
     */
    public static boolean getSubRetreatBeforeBattle(GameData data) {

        return data.getProperties().get(SUB_RETREAT_BEFORE_BATTLE, false);

    }

    /*
     * Shore Bombard per Ground Unit Restricted
     */
    public static boolean getShoreBombardPerGroundUnitRestricted(GameData data) {

        return data.getProperties().get(SHORE_BOMBARD_PER_GROUND_UNIT_RESTRICTED, false);

    }

    /*
     * Shore Bombard decreases number of units that can be produced at a factory
     */
    public static boolean getSBRAffectsUnitProduction(GameData data) {

        return data.getProperties().get(SBR_AFFECTS_UNIT_PRODUCTION, false);

    }

    /*
     * AA restricted to Attacked Territory Only
     */
    public static boolean getAATerritoryRestricted(GameData data) {

        return data.getProperties().get(AA_TERRITORY_RESTRICTED, false);

    }
    
    /*
     * Multiple AA allowed per territory
     */
    public static boolean getMultipleAAPerTerritory(GameData data) {

        return data.getProperties().get(MULTIPLE_AA_PER_TERRITORY, false);

    }
    
    /*
     * NATIONAL OBJECTIVES USED
     */
    public static boolean getNationalObjectives(GameData data) {

        return data.getProperties().get(NATIONAL_OBJECTIVES, false);

    }

    /*
     * Triggers USED
     */
    public static boolean getTriggers(GameData data) {

        return data.getProperties().get(TRIGGERS, false);

    }

    /*
     * Display Sea Names
     */
    public static boolean getDisplaySeaNames(GameData data) {

        return data.getProperties().get(DISPLAY_SEA_NAMES, false);

    }

    /*
     * 
     */
    public static boolean getAlways_On_AA(GameData data) {

        return data.getProperties().get(ALWAYS_ON_AA_PROPERTY, false);
    }

    /*
     * 
     */
    public static boolean getLHTR_Carrier_Production_Rules(GameData data) {

        return data.getProperties().get(LHTR_CARRIER_PRODUCTION_RULES, false);
    }
/*
 * Atomic units of the fighter/carrier production rules
 */
    public static boolean getProduce_Fighters_On_Carriers(GameData data) {

        return data.getProperties().get(CAN_PRODUCE_FIGHTERS_ON_CARRIERS, false);
    }

    public static boolean getProduce_New_Fighters_On_Old_Carriers(GameData data) {

        return data.getProperties().get(PRODUCE_NEW_FIGHTERS_ON_OLD_CARRIERS, false);
    }

    public static boolean getMove_Existing_Fighters_To_New_Carriers(GameData data) {

        return data.getProperties().get(MOVE_EXISTING_FIGHTERS_TO_NEW_CARRIERS, false);
    }

    public static boolean getLand_Existing_Fighters_On_New_Carriers(GameData data) {

        return data.getProperties().get(LAND_EXISTING_FIGHTERS_ON_NEW_CARRIERS, false);
    }
    
    /*
     * 
     */
    public static Integer getHeavy_Bomber_Dice_Rolls(GameData data) {
    	try {
            return Integer.parseInt((String) data.getProperties().get(HEAVY_BOMBER_DICE_ROLLS));
        } catch (NumberFormatException e) {
            return 3;
        }
    }

    /*
     * 
     */
    public static boolean getBattleships_Repair_At_End_Of_Round(GameData data) {

        return data.getProperties().get(TWO_HIT_BATTLESHIPS_REPAIR_EACH_TURN, false);
    }

    /*
     * 
     */
    public static boolean getChoose_AA_Casualties(GameData data) {

        return data.getProperties().get(CHOOSE_AA, false);
    }
    
    /*
     * 
     */
    public static boolean getSubmersible_Subs(GameData data) {

        return data.getProperties().get(SUBMERSIBLE_SUBS, false);
    }
    
    /*
     * 
     */
    public static boolean getUse_Destroyers_And_Artillery(GameData data) {

        return data.getProperties().get(USE_DESTROYERS_AND_ARTILLERY, false);
    }
    
    /*
     * 
     */
    public static boolean getUse_Shipyards(GameData data) {

        return data.getProperties().get(USE_SHIPYARDS, false);
    }
    
    /*
     * 
     */
    public static boolean getLow_Luck(GameData data) {

        return data.getProperties().get(LOW_LUCK, false);
    }
    
    /*
     * 
     */
    public static boolean getKamikaze_Airplanes(GameData data) {

        return data.getProperties().get(KAMIKAZE, false);
    }
    
    /*
     * 
     */
    public static boolean getLHTR_Heavy_Bombers(GameData data) {

        return data.getProperties().get(LHTR_HEAVY_BOMBERS, false);
    }
    
    /*
     * 
     */ 
    public static Integer getSuper_Sub_Defense_Bonus(GameData data) {
    	try {
            return Integer.parseInt((String) data.getProperties().get(SUPER_SUB_DEFENSE_BONUS));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /*
     * 
     */
    public static boolean getScramble_Rules_In_Effect(GameData data) {

        return data.getProperties().get(SCRAMBLE_RULES_IN_EFFECT, false);
    }
    
    
    /*
     * 
     */
    public static boolean getScrambled_Units_Return_To_Base(GameData data) {

        return data.getProperties().get(SCRAMBLED_UNITS_RETURN_TO_BASE, false);
    }
    
    /*
     * 
     */
    public static boolean getScramble_To_Sea_Only(GameData data) {

        return data.getProperties().get(SCRAMBLE_TO_SEA_ONLY, false);
    }
    
    /*
     * 
     */
    public static boolean getScramble_From_Island_Only(GameData data) {

        return data.getProperties().get(SCRAMBLE_FROM_ISLAND_ONLY, false);
    }
    
    public static Integer getPU_Multiplier(GameData data) {
    	try {
    		return Integer.parseInt((String) data.getProperties().get(PU_MULTIPLIER));
    	} catch (NumberFormatException e) {
    		return 1;
    	}
    }
    
    private Properties() {
    }
}
