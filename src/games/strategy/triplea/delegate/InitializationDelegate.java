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

/**
 * InitializationDelegate.java
 *
 * Created on January 4, 2002, 3:53 PM
 *
 * Subclasses can override init(), which will be called exactly once.
 */

package games.strategy.triplea.delegate;

import java.util.*;
import java.io.Serializable;

import games.strategy.engine.delegate.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.triplea.*;
import games.strategy.triplea.attatchments.*;


/**
 *
 * @author  Sean Bridges
 */
public class InitializationDelegate implements Delegate
{
  private String m_name;
  private String m_displayName;


  /** Creates a new instance of InitializationDelegate */
  public InitializationDelegate()
  {
  }


  public void initialize(String name, String displayName)
  {
    m_name = name;
    m_displayName = displayName;
  }

  /**
   * Called before the delegate will run.
   */
  public void start(DelegateBridge aBridge, GameData gameData)
  {
    init(gameData, aBridge);
  }

  protected void init(GameData data, DelegateBridge aBridge)
  {
    boolean userEnabled = games.strategy.triplea.Properties.getTwoHitBattleships(data);
    UnitAttatchment battleShipAttatchment = UnitAttatchment.get(data.getUnitTypeList().getUnitType(Constants.BATTLESHIP_TYPE));
    boolean defaultEnabled = battleShipAttatchment.isTwoHit();

    if(userEnabled != defaultEnabled)
    {
      aBridge.getHistoryWriter().startEvent("TwoHitBattleships:" + userEnabled);
      aBridge.addChange(ChangeFactory.attatchmentPropertyChange(battleShipAttatchment, "" + userEnabled, Constants.TWO_HIT));
    }

    OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();
    Iterator territories = data.getMap().iterator();
    while(territories.hasNext())
    {
      Territory current = (Territory) territories.next();
      if(!current.isWater() && !current.getOwner().isNull())
      {
        origOwnerTracker.addOriginalOwner(current, current.getOwner());
        Collection aaAndFactory = current.getUnits().getMatches(Matches.UnitIsAAOrFactory);
        origOwnerTracker.addOriginalOwner(aaAndFactory, current.getOwner());
      }
    }
  }

  public String getName()
  {
    return m_name;
  }

  public String getDisplayName()
  {
    return m_displayName;
  }


  public Message sendMessage(Message message)
  {
    throw new UnsupportedOperationException("Cant send messages to init delegate");
  }

  /**
   * Called before the delegate will stop running.
   */
  public void end()
  {
  }

  /**
   * Can the delegate be saved at the current time.
   * @arg message, a String[] of size 1, hack to pass an error message back.
   */
  public boolean canSave(String[] message)
  {
    return true;
  }


}
