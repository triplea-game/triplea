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

  public static Collection selectCasualties(PlayerID player, Collection targets, DelegateBridge bridge, String text, GameData data, DiceRoll dice)
  {
    return selectCasualties(null, player, targets, bridge, text, data, dice);
  }


  public static Collection selectCasualties(String step, PlayerID player, Collection targets, DelegateBridge bridge, String text, GameData data, DiceRoll dice)
  {
    if(dice.getHits() == 0)
      return Collections.EMPTY_LIST;

    Map dependents = getDependents(targets,data);

    Message msg = new SelectCasualtyQueryMessage(step, targets, dependents, dice.getHits(), text, dice, player);
    Message response = bridge.sendMessage(msg, player);
    if(!(response instanceof SelectCasualtyMessage))
      throw new IllegalStateException("Message of wrong type:" + response);

    SelectCasualtyMessage casualtySelection = (SelectCasualtyMessage) response;
    Collection casualties = casualtySelection.getSelection();
    //check right number
    if ( ! (casualties.size() == dice.getHits()) )
    {
      bridge.sendMessage( new StringMessage("Wrong number of casualties selected", true), player);
      selectCasualties(player, targets, bridge,text, data, dice);
    }
    //check we have enough of each type
    if(!targets.containsAll(casualties))
    {
      bridge.sendMessage( new StringMessage("Cannot remove enough units of those types", true), player);
      selectCasualties(player, targets, bridge,text, data, dice);
    }
    return casualties;
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



  public static int getRolls(Collection units, PlayerID id, boolean defend)
  {
    if(defend)
      return units.size();

    int count = 0;
    Iterator iter = units.iterator();
    while(iter.hasNext())
    {
      Unit unit = (Unit) iter.next();
      UnitAttatchment ua = UnitAttatchment.get(unit.getType());
      count+=ua.getAttackRolls(id);
    }
    return count;
  }

  //nothing but static
  private BattleCalculator()
  {}
}
