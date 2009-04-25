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

    public static boolean getFourthEdition(GameData data) {

        return data.getProperties().get(FOURTH_EDITION, false);

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

    public static boolean getRocketsCanViolateNeutrality(GameData data) {

        return data.getProperties().get(ROCKETS_CAN_VIOLATE_NEUTRALITY, false);

    }

    public static boolean getRocketsCanFlyOverImpassables(GameData data) {

        return data.getProperties().get(ROCKETS_CAN_FLY_OVER_IMPASSABLES, false);

    }

    /*
     * Pacific Edition
     */
    public static boolean getPacificEdition(GameData data) {

        return data.getProperties().get(PACIFIC_EDITION, false);

    }

    /*
     * Anniversary Edition
     */
    public static boolean getAnniversaryEdition(GameData data) {

        return data.getProperties().get(ANNIVERSARY_EDITION, false);

    }

    /*
     * No Economic Victory Edition
     */
    public static boolean getNoEconomicVictory(GameData data) {

        return data.getProperties().get(NO_ECONOMIC_VICTORY, false);

    }

    /*
     * Anniversary Edition Land & Production
     */
    public static boolean getAnniversaryEditionLandProduction(GameData data) {

        return data.getProperties().get(ANNIVERSARY_EDITION_LAND_PRODUCTION, false);

    }

    /*
     * Anniversary Edition Air & Naval
     */
    public static boolean getAnniversaryEditionAirNaval(GameData data) {

        return data.getProperties().get(ANNIVERSARY_EDITION_AIR_NAVAL, false);

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
    public static boolean getAA50TechModel(GameData data) {

        return data.getProperties().get(AA50_Tech_Model, false);

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
     * Limit the TOTAL ipcs lost to Bombers/Rockets in a turn to territory's
     * production
     */
    public static boolean getIPCCap(GameData data) {

        return data.getProperties().get(IPC_CAP, false);

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
     * Subs restricted from controling sea zones
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
     * Production restricted to 1 unit per owned territory with an IPC value
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

    /*
     * Are any territories originally occupied by enemies
     */
    public static boolean getOccupiedTerritories(GameData data) {

        return data.getProperties().get(OCCUPIED_TERRITORIES, false);

    }

    /*
     * Air restricted from attacking subs unless DD present
     */
    public static boolean getAirAttackSubRestricted(GameData data) {

        return data.getProperties().get(AIR_ATTACK_SUB_RESTRICTED, false);

    }

    /*
     * Sub retreat before battle
     */
    public static boolean getSubRetreatBeforeBattle(GameData data) {

        return data.getProperties().get(SUB_RETREAT_BEFORE_BATTLE, false);

    }

    /*
     * Sub retreat restricted by DD
     */
    public static boolean getSubRetreatDDRestricted(GameData data) {

        return data.getProperties().get(SUB_RETREAT_DD_RESTRICTED, false);

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
     * NATIONAL OBJECTIVES USED
     */
    public static boolean getNationalObjectives(GameData data) {

        return data.getProperties().get(NATIONAL_OBJECTIVES, false);

    }

    /*
     * Continuous Research
     */
    public static boolean getContinuousResearch(GameData data) {

        return data.getProperties().get(CONTINUOUS_RESEARCH, false);

    }

    private Properties() {
    }

}
