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

  public static boolean aaHit(int die) {
    return die == 0;
  }

  public static DiceRoll rollAA(int numberOfAirUnits, DelegateBridge bridge, Territory location)
  {
    int[] random = bridge.getRandom(Constants.MAX_DICE, numberOfAirUnits, "Roll aa guns in " + location.getName());
    int hits = 0;
    for(int i = 0; i < random.length; i++)
    {
      if(aaHit(random[i]))
        hits++;
    }

    int[][] dice = new int[Constants.MAX_DICE][];
    dice[0] = random;
    for(int i = 1; i < Constants.MAX_DICE; i++)
    {
      dice[i] = new int[0];
    }

    DiceRoll roll = new DiceRoll(dice, hits);
    bridge.getHistoryWriter().addChildToEvent("AA guns fire in" + location + " :" + Formatter.asDice(random), roll);
    return roll;
  }



  public static DiceRoll rollDice(List units, boolean defending,
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
