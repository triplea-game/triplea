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

/*
 * Constants.java
 *
 * Created on November 8, 2001, 3:28 PM
 */

package games.strategy.triplea;

/**
 *
 * Constants used througout the game.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public interface Constants
{

  //Player names
  public static final String AMERICANS = "Americans";
  public static final String BRITISH = "British";
  public static final String GERMANS = "Germans";
  public static final String JAPANESE = "Japanese";
  public static final String RUSSIANS = "Russians";

  public static final String UNIT_ATTATCHMENT_NAME = "unitAttatchment";
  public static final String TECH_ATTATCHMENT_NAME = "techAttatchment";
  public static final String TERRITORY_ATTATCHMENT_NAME = "territoryAttatchment";
  public static final String IPCS = "IPCs";
  public static final int MAX_DICE = 6;
  public static final String NEUTRAL_CHARGE_PROPERTY = "neutralCharge";
  public static final String FACTORIES_PER_COUNTRY_PROPERTY ="maxFactoriesPerTerritory";
  public static final String TWO_HIT_BATTLESHIP_PROPERTY = "Two hit battleship";
  public static final String ALWAYS_ON_AA_PROPERTY = "Always on AA";
  //allow fighters to be placed on newly produced carriers
  public static final String CAN_PRODUCE_FIGHTERS_ON_CARRIERS = "Produce fighters on carriers";  
  public static final String HEAVY_BOMBER_DICE_ROLLS = "Heavy Bomber Dice Rolls";
  public static final String TWO_HIT_BATTLESHIPS_REPAIR_EACH_TURN = "Battleships repair at end of round";

  public static final String TWO_HIT = "isTwoHit";

  public static final int TECH_ROLL_COST = 5;

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

}
