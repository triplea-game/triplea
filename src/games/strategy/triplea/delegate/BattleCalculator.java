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
 * BattleCalculator.java
 *
 * Created on November 29, 2001, 2:27 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.triplea.Constants;
import games.strategy.engine.message.Message;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.util.*;
import games.strategy.util.*;
import games.strategy.engine.delegate.DelegateBridge;
import games.strategy.engine.data.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Utiltity class for determing casualties and
 * selecting casualties.  The code was being dduplicated all over the place.
 */
public class BattleCalculator
{

  public static int getAAHits(Collection units, DelegateBridge bridge, int[] dice)
  {
    int attackingAirCount = Match.countMatches(units, Matches.UnitIsAir);

    int hitCount = 0;
    for(int i = 0; i < attackingAirCount; i++)
    {
      if(1 > dice[i])
        hitCount++;
    }
    return hitCount;
  }

  public static SelectCasualtyMessage selectCasualties(PlayerID player, Collection targets, DelegateBridge bridge, String text, GameData data, DiceRoll dice)
  {
    return selectCasualties(null, player, targets, bridge, text, data, dice);
  }


  public static SelectCasualtyMessage selectCasualties(String step, PlayerID player, Collection targets, DelegateBridge bridge, String text, GameData data, DiceRoll dice)
  {
    int hits = dice.getHits();
    if(hits == 0)
      return new SelectCasualtyMessage(Collections.EMPTY_LIST, Collections.EMPTY_LIST, false);

    Map dependents = getDependents(targets,data);

    // If all targets are one type and not two hit then
    // just remove the appropriate amount of units of that type.
    // Sets the appropriate flag in the select casualty message
    // such that user is prompted to continue since they did not
    // select the units themselves.
    if (allTargetsOneTypeNotTwoHit(targets, dependents)) {
      List killed = new ArrayList();
      Iterator iter = targets.iterator();
      for (int i = 0; i < hits; i++) {
	killed.add(iter.next());
      }
      return new SelectCasualtyMessage(killed, Collections.EMPTY_LIST, true);
    }


    Message msg = new SelectCasualtyQueryMessage(step, targets, dependents, dice.getHits(), text, dice, player);
    Message response = bridge.sendMessage(msg, player);
    if(!(response instanceof SelectCasualtyMessage))
      throw new IllegalStateException("Message of wrong type:" + response);

    SelectCasualtyMessage casualtySelection = (SelectCasualtyMessage) response;
    List killed = casualtySelection.getKilled();
    List damaged = casualtySelection.getDamaged();

    //check right number
    if ( ! (killed.size()  + damaged.size() == dice.getHits()) )
    {
      bridge.sendMessage( new StringMessage("Wrong number of casualties selected", true), player);
      return selectCasualties(player, targets, bridge,text, data, dice);
    }
    //check we have enough of each type
    if(!targets.containsAll(killed) || ! targets.containsAll(damaged))
    {
      bridge.sendMessage( new StringMessage("Cannot remove enough units of those types", true), player);
      return selectCasualties(player, targets, bridge,text, data, dice);
    }
    return casualtySelection;
  }

  private static Map getDependents(Collection targets, GameData data)
  {
    //jsut worry about transports
    TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();

    Map dependents = new HashMap();
    Iterator iter = targets.iterator();
    while(iter.hasNext())
    {
      Unit target = (Unit) iter.next();
      dependents.put( target, tracker.transportingAndUnloaded(target));
    }
    return dependents;
  }

  /**
   * Checks if the given collections target are all of one category as
   * defined by UnitSeperator.categorize and they are not two hit units.
   * @param targets a collection of target units
   * @param dependents map of depend units for target units
   */
  private static boolean allTargetsOneTypeNotTwoHit(Collection targets, Map dependents)
  {
    Set categorized = UnitSeperator.categorize(targets, dependents, null);
    if (categorized.size() == 1) {
      UnitCategory unitCategory = (UnitCategory)categorized.iterator().next();
      if (!unitCategory.isTwoHit() || unitCategory.getDamaged()) {
	return true;
      }
    }

    return false;
  }


  public static int getRolls(Collection units, PlayerID id, boolean defend)
  {
    int count = 0;
    Iterator iter = units.iterator();
    while(iter.hasNext())
    {
      Unit unit = (Unit) iter.next();    
      count+=getRolls(unit,id, defend);
    }
    return count;
  }
  
  public static int getRolls(Unit unit, PlayerID id, boolean defend)
  {
      if(defend)
          return 1;
      return UnitAttatchment.get(unit.getType()).getAttackRolls(id);
      
  }

  //nothing but static
  private BattleCalculator()
  {}
}
