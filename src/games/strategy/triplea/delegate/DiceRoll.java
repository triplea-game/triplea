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


package games.strategy.triplea.delegate;

import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.engine.delegate.DelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.formatter.*;
import games.strategy.util.Match;

/**
 * Used to store information about a dice roll.
 *
 * # of rolls at 5, at 4, etc.
 */

public class DiceRoll implements java.io.Serializable
{

  private final int[][] m_rolls;
  private final int m_hits;
  private final boolean m_hitOnlyIfEquals;

  public static DiceRoll rollAA(int numberOfAirUnits, DelegateBridge bridge, Territory location, GameData data)
  {
    int hits = 0;
    int[] dice = new int[0];
    int[][] sortedDiceInt = new int[Constants.MAX_DICE][0];

    if (data.getProperties().get(Constants.LOW_LUCK) != null
	&& ((Boolean)data.getProperties().get(Constants.LOW_LUCK)).booleanValue()) {
      // Low luck rolling
      hits = numberOfAirUnits / Constants.MAX_DICE;
      int hitsFractional = numberOfAirUnits % Constants.MAX_DICE;
      
      if (hitsFractional > 0) {
	dice = bridge.getRandom(Constants.MAX_DICE, 1, "Roll aa guns in " + location.getName());
	sortedDiceInt[hitsFractional - 1] = dice;
	if (hitsFractional > dice[0]) {
	  hits++;
	}
      }

    } else {
      // Normal rolling
      dice = bridge.getRandom(Constants.MAX_DICE, numberOfAirUnits, "Roll aa guns in " + location.getName());
      for(int i = 0; i < dice.length; i++)
      {
	if(dice[i] == 0)
	  hits++;
      }
      sortedDiceInt[0] = dice;
    }


    DiceRoll roll = new DiceRoll(sortedDiceInt, hits);
    bridge.getHistoryWriter().addChildToEvent("AA guns fire in" + location + " :" + Formatter.asDice(dice), roll);
    return roll;
  }


  /**
   * Roll dice for units.
   */
  public static DiceRoll rollDice(List units, boolean defending,
				  PlayerID player,
				  DelegateBridge bridge,
				  GameData data)
  {
    // Decide whether to use low luck rules or normal rules.
    if (data.getProperties().get(Constants.LOW_LUCK) != null
	&& ((Boolean)data.getProperties().get(Constants.LOW_LUCK)).booleanValue()) {
      return rollDiceLowLuck(units, defending, player, bridge, data);
    } else {
      return rollDiceNormal(units, defending, player, bridge, data);
    }
  }

  /**
   * Roll dice for units using low luck rules. Low luck rules based
   * on rules in DAAK.
   */
  private static DiceRoll rollDiceLowLuck(List units, boolean defending,
					  PlayerID player,
					  DelegateBridge bridge,
					  GameData data)
  {
    String annotation = player.getName() +  " roll dice for " + Formatter.unitsToTextNoOwner(units);

    int rollCount = BattleCalculator.getRolls(units, player, defending);
    if(rollCount == 0)
    {
      return new DiceRoll(new int[Constants.MAX_DICE][0], 0 );
    }

    int artillerySupportAvailable = 0;
    if(!defending)
      artillerySupportAvailable = Match.countMatches(units, Matches.UnitIsArtillery);

    Collection leftOverUnits = new ArrayList();
    Iterator iter = units.iterator();

    int power = 0;
    int hitCount = 0;

    // We can through the units to find the total strength of the units
    while(iter.hasNext())
    {
      Unit current = (Unit) iter.next();
      UnitAttatchment ua = UnitAttatchment.get(current.getType());
      int rolls = defending ? 1 : ua.getAttackRolls(player);
      for(int i = 0; i < rolls; i++)
      {
        int strength;
        if(defending)
          strength = ua.getDefense(current.getOwner());
        else
        {
          strength = ua.getAttack(current.getOwner());
          if(ua.isArtillerySupportable() && artillerySupportAvailable > 0)
          {
            strength++;
            artillerySupportAvailable--;
          }
        }

        if(!defending&&ua.isStrategicBomber()&& TechTracker.hasHeavyBomber(player) &&
           data.getProperties().get(Constants.HEAVY_BOMBER_DOWNGRADE)!=null &&
           ((Boolean)data.getProperties().get(Constants.HEAVY_BOMBER_DOWNGRADE)).booleanValue())
        {
	  // We don't support heavy rolls with Low Luck yet.
	  throw new IllegalStateException("Cannot use heavy bomber downgrade option with low luck");
        }

	power += strength;
      }
    }

    // Get number of hits
    hitCount = power / Constants.MAX_DICE;

    // Create sorted dice array with zero length arrays
    int[][] sortedDiceInt = new int[Constants.MAX_DICE][0];
    int[] dice = new int[0];

    // We need to roll dice for the fractional part of the dice.
    power = power % Constants.MAX_DICE;
    if (power != 0) {
      dice = bridge.getRandom(Constants.MAX_DICE, 1, annotation);
      sortedDiceInt[power - 1] = new int[1];
      sortedDiceInt[power - 1][0] = dice[0];
      if (power > dice[0]) {
	hitCount++;
      }
    }
    
    // Create DiceRoll object
    DiceRoll rVal = new  DiceRoll(sortedDiceInt, hitCount);
    bridge.getHistoryWriter().addChildToEvent(annotation + " : " + Formatter.asDice(dice), rVal);
    return rVal;
  }

  /**
   * Roll dice for units per normal rules.
   */
  private static DiceRoll rollDiceNormal(List units, boolean defending,
					 PlayerID player,
					 DelegateBridge bridge,
					 GameData data)
  {
    String annotation = player.getName() +  " roll dice for " + Formatter.unitsToTextNoOwner(units);

    int rollCount = BattleCalculator.getRolls(units, player, defending);
    if(rollCount == 0)
    {
      return new DiceRoll(new int[Constants.MAX_DICE][0], 0 );
    }


    int artillerySupportAvailable = 0;
    if(!defending)
      artillerySupportAvailable = Match.countMatches(units, Matches.UnitIsArtillery);

    int[] dice = bridge.getRandom(Constants.MAX_DICE, rollCount, annotation);

    List[] sortedDice = new List[Constants.MAX_DICE];
    for(int i = 0; i < sortedDice.length; i++)
    {
      sortedDice[i] = new ArrayList();
    }

    Iterator iter = units.iterator();

    int hitCount = 0;
    int diceIndex = 0;
    while(iter.hasNext())
    {
      Unit current = (Unit) iter.next();
      UnitAttatchment ua = UnitAttatchment.get(current.getType());
      int rolls = defending ? 1 : ua.getAttackRolls(player);
      int lowerRollForBomber=Constants.MAX_DICE;
      String bomberRollFeedback="Bomber rolled:";
      for(int i = 0; i < rolls; i++)
      {
        int strength;
        if(defending)
          strength = ua.getDefense(current.getOwner());
        else
        {
          strength = ua.getAttack(current.getOwner());
          if(ua.isArtillerySupportable() && artillerySupportAvailable > 0)
          {
            strength++;
            artillerySupportAvailable--;
          }
        }

        sortedDice[strength - 1].add(new Integer(dice[diceIndex]));
        if(!defending&&ua.isStrategicBomber()&& TechTracker.hasHeavyBomber(player) &&
           data.getProperties().get(Constants.HEAVY_BOMBER_DOWNGRADE)!=null &&
           data.getProperties().get(Constants.HEAVY_BOMBER_DOWNGRADE)==Boolean.TRUE)
        {
          bomberRollFeedback+=" "+dice[diceIndex];
          if(lowerRollForBomber>dice[diceIndex])
            lowerRollForBomber=dice[diceIndex];
          if(i+1<rolls)
            continue;
          bridge.getHistoryWriter().addChildToEvent(bomberRollFeedback+", picked "+lowerRollForBomber);
          strength=lowerRollForBomber;
        }

        //dice is [0-MAX_DICE)
        if( strength > dice[diceIndex])
          hitCount++;
        diceIndex++;
      }
    }

    int[][] sortedDiceInt = new int[Constants.MAX_DICE][];
    for(int i = 0; i <  sortedDice.length; i++)
    {
      int[] values = new int[sortedDice[i].size()];
      for(int j = 0; j < sortedDice[i].size(); j++)
      {
        values[j] = ((Integer) sortedDice[i].get(j)).intValue();
      }
      sortedDiceInt[i] = values;
    }

    DiceRoll rVal = new  DiceRoll(sortedDiceInt, hitCount);
    bridge.getHistoryWriter().addChildToEvent(annotation + " : " + Formatter.asDice(dice), rVal);
    return rVal;
  }

  /**
   *
   * @param dice int[] the dice, 0 based
   * @param hits int - the number of hits
   * @param rollAt int - what we roll at, [0,Constants.MAX_DICE]
   * @param hitOnlyIfEquals boolean - do we get a hit only if we are equals, or do we hit when we are equal or less than
   * for example a 5 is a hit when rolling at 6 for equal and less than, but is not for equals
   */
  public DiceRoll(int[] dice, int hits, int rollAt, boolean hitOnlyIfEquals)
  {
    m_hitOnlyIfEquals = hitOnlyIfEquals;
    m_rolls = new int[Constants.MAX_DICE][];
    for(int i = 0; i < m_rolls.length; i++)
    {
      m_rolls[i] = new int[0];
    }
    m_rolls[rollAt] = dice;
    m_hits = hits;
  }

  public DiceRoll(int[][] dice, int hits)
  {
    m_hitOnlyIfEquals = false;
    m_rolls = dice;
    m_hits = hits;
  }

  public int getHits()
  {
    return m_hits;
  }

  /**
   * @param rollAt the strength of the roll, eg infantry roll at 2, expecting a number in [1,6]
   * @return in int[] which shouldnt be modifed, the int[] is 0 based, ie 0..MAX_DICE
   */
  public int[] getRolls(int rollAt)
  {
    return m_rolls[rollAt -1];
  }



  public boolean getHitOnlyIfEquals()
  {
    return m_hitOnlyIfEquals;
  }

}
