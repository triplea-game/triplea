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

/**
 * Used to store information about a dice roll.
 *
 * # of rolls at 5, at 4, etc.
 */

public class DiceRoll implements java.io.Serializable
{

  private int[][] m_rolls;
  private int m_hits;

  public static DiceRoll rollAA(int numberOfAirUnits, DelegateBridge bridge)
  {
    int[] random = bridge.getRandom(Constants.MAX_DICE, numberOfAirUnits);
    int hits = 0;
    for(int i = 0; i < random.length; i++)
    {
      if(random[i] == 0)
        hits++;
    }

    int[][] dice = new int[Constants.MAX_DICE][];
    dice[0] = random;
    for(int i = 1; i < dice[0].length; i++)
    {
      dice[i] = new int[0];
    }
    return new DiceRoll(dice, hits);
  }

  public static DiceRoll rollDice(List units, boolean defending, PlayerID player, DelegateBridge bridge)
  {
    int rollCount = BattleCalculator.getRolls(units, player, defending);
    int[] dice = bridge.getRandom(Constants.MAX_DICE, rollCount);

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
      for(int i = 0; i < rolls; i++)
      {
        int strength;
        if(defending)
          strength = ua.getDefense(current.getOwner());
        else
          strength = ua.getAttack(current.getOwner());

        sortedDice[strength - 1].add(new Integer(dice[diceIndex]));

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

    return new DiceRoll(sortedDiceInt, hitCount);
  }

  private DiceRoll(int[][] dice, int hits)
  {
    m_rolls = dice;
    m_hits = hits;
  }

  public int getHits()
  {
    return m_hits;
  }

  /**
   * @param rollAt the strength of the roll, eg infantry roll at 2, tanks at 3
   * @return in int[] which shouldnt be modifed, the int[] is 0 based, ie 0..MAX_DICE
   */
  public int[] getRolls(int rollAt)
  {
    return m_rolls[rollAt -1];
  }


}