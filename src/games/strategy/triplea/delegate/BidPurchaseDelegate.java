package games.strategy.triplea.delegate;


import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

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
    private IDelegateBridge m_bridge;


  /**
   * subclasses can over ride this method to use different restrictions as to what a player can buy
   */
  protected boolean canAfford(IntegerMap<Resource> costs, PlayerID player)
  {
      Resource ipcs = getData().getResourceList().getResource(Constants.IPCS);
      return costs.getInt(ipcs) <= m_bid;
  }

  public void start(IDelegateBridge bridge, GameData data)
  {
      super.start(bridge, data);
      m_bridge = bridge;
      String propertyName = bridge.getPlayerID().getName() + " bid";
      m_bid = Integer.parseInt(getData().getProperties().get(propertyName).toString());
      m_spent = 0;
  }

  protected void removeFromPlayer(PlayerID player, IntegerMap<Resource> resources)
  {
      m_spent = resources.getInt(super.getData().getResourceList().getResource(Constants.IPCS));
  }

  /**
    * Called before the delegate will stop running.
    */
   public void end()
   {
       super.end();
       int unspent =  m_bid - m_spent;
       if(unspent == 0)
           return;
       m_bridge.getHistoryWriter().startEvent(m_bridge.getPlayerID().getName() + " retains " + unspent + " IPCS not spent in bid phase");
       Change unspentChange = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), super.getData().getResourceList().getResource(Constants.IPCS), unspent);
       m_bridge.addChange(unspentChange);

   }


  /**
   * Can the delegate be saved at the current time.
   * @arg message, a String[] of size 1, hack to pass an error message back.
   */
  public boolean canSave(String[] message)
  {
    //if you want to change this, change
    //the start() method so that it wont reset when loading
    message[0] = "Cant save during bid purchase";
    return false;
  }

}
