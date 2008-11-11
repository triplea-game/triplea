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
 * <p>Title: TripleA</p>
 * @author Sean Bridges
 *
 */

public class Properties implements Constants
{

  public static int getNeutralCharge(GameData data)
  {
    try
    {
      return Integer.parseInt( (String) data.getProperties().get(NEUTRAL_CHARGE_PROPERTY));
    }
    catch (Exception e)
    {
      return 3;
    }
  }

  public static int getFactoriesPerCountry(GameData data)
  {
    try
    {
      return Integer.parseInt( (String) data.getProperties().get(FACTORIES_PER_COUNTRY_PROPERTY));
    }
    catch (Exception e)
    {
      return 1;
    }

  }

  public static boolean getTwoHitBattleships(GameData data)
  {
    try
    {
      return ( (Boolean) data.getProperties().get(TWO_HIT_BATTLESHIP_PROPERTY) ).booleanValue();
    }
    catch(Exception e)
    {
      return false;
    }
  }

  public static boolean getFourthEdition(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(FOURTH_EDITION) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  //Comco
  /*
   * Pacific Edition
   */
  public static boolean getPacificEdition(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(FOURTH_EDITION) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  /*
   * Restrict the number of units that can be placed at a factory.
   */
  public static boolean getRestrictedPurchase(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(RESTRICTED_PURCHASE) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  
  /*
   * Can the player select the type of technology they are rolling for
   */
  public static boolean getSelectableTechRoll(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(SELECTABLE_TECH_ROLL) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  
  /*
   * Are transports restricted from unloading in multiple territories in a turn
   */
  public static boolean getTransportRestrictedUnload(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(TRANSPORT_RESTRICTED_UNLOAD) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  
  /*
   * Are AA casualties chosen randomly
   */
  public static boolean getRandomAACasualties(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(RANDOM_AA_CASUALTIES) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }

  /*
   * Limit the damage caused by each bomber on Strategic Bomb Raids to production of territory
   */
  public static boolean getLimitSBRDamageToProduction(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(LIMIT_SBR_DAMAGE_TO_PRODUCTION) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }

  /*
   * Limit the damage caused on Rocket attacks to production of territory
   */
  public static boolean getLimitRocketDamageToProduction(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(LIMIT_ROCKET_DAMAGE_TO_PRODUCTION) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }

  /*
   * Limit the TOTAL damage caused by Bombers in a turn to territory's production
   */
  public static boolean getLimitSBRDamagePerTurn(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(LIMIT_SBR_DAMAGE_PER_TURN) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }

  /*
   * Limit the TOTAL damage caused by Rockets in a turn to territory's production
   */
  public static boolean getLimitRocketDamagePerTurn(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(LIMIT_ROCKET_DAMAGE_PER_TURN) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }

  /*
   * Limit the TOTAL ipcs lost to Bombers/Rockets in a turn to territory's production
   */
  public static boolean getIPCCap(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(IPC_CAP) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  
  /*
   * Reduce Victory Points by Strategic Bombing
   */
  public static boolean getSBRVictoryPoint(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(SBR_VICTORY_POINTS) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  
  /*
   * Allow one rocket attack per defending factory
   */
  public static boolean getOneRocketAttackPerFactory(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(ONE_ROCKET_ATTACK_PER_FACTORY) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  
  /*
   * Are allied aircraft dependents of CVs
   */
  public static boolean getAlliedAirDependents(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(ALLIED_AIR_DEPENDENTS) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }

  /*
   * Defending subs sneak attack
   */
  public static boolean getDefendingSubsSneakAttack(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(DEFENDING_SUBS_SNEAK_ATTACK) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  

  /*
   * Attacker retreat planes from Amphib assault
   */
  public static boolean getAttackerRetreatPlanes(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(ATTACKER_RETREAT_PLANES) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }

  /*
   * Can surviving air at sea move to land on friendly land/carriers
   */
  public static boolean getSurvivingAirMoveToLand(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(SURVIVING_AIR_MOVE_TO_LAND) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  
  /*
   * Naval Bombard casualties return fire
   */
  public static boolean getNavalBombardCasualtiesReturnFire(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(NAVAL_BOMBARD_CASUALTIES_RETURN_FIRE) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }

  /*
   * Naval Bombard casualties return fire
   */
  public static boolean getBlitzThroughFactoriesAndAA(GameData data)
  {
	  try
	    {
	      return ( (Boolean) data.getProperties().get(BLITZ_THROUGH_FACTORIES_AND_AA) ).booleanValue();
	    }
	    catch(Exception e)
	    {
	      return false;
	    }
  }
  
  private Properties()
  {
  }
}
