package games.strategy.triplea.delegate;


import java.util.*;
import java.io.Serializable;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;

import games.strategy.triplea.Constants;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class BidPurchaseDelegate extends PurchaseDelegate
{
    private int m_bid;
    private int m_spent;
    private DelegateBridge m_bridge;


  /**
   * subclasses can over ride this method to use different restrictions as to what a player can buy
   */
  protected boolean canAfford(IntegerMap costs, PlayerID player)
  {
    int ipcCost = costs.getInt(Constants.IPCS);



    return m_bid >= ipcCost;
  }

  public void start(DelegateBridge bridge, GameData data)
  {
      super.start(bridge, data);
      m_bridge = bridge;
      String propertyName = bridge.getPlayerID().getName() + " bid";
      m_bid = Integer.parseInt(getData().getProperties().get(propertyName).toString());
      m_spent = 0;
  }

  protected void removeFromPlayer(PlayerID player, IntegerMap resources)
  {
      m_spent = resources.getInt(super.getData().getResourceList().getResource(Constants.IPCS));
  }

  /**
    * Called before the delegate will stop running.
    */
   public void end()
   {
       super.end();
       Change unspent = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), super.getData().getResourceList().getResource(Constants.IPCS), m_bid - m_spent);
       m_bridge.addChange(unspent);

   }



}
