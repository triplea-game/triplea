package games.strategy.triplea.delegate;


import java.io.Serializable;
import java.util.Collection;

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
    
    private boolean m_hasBid = false;

    
    private static int getBidAmount(GameData data, PlayerID currentPlayer)
    {
      String propertyName = currentPlayer.getName() + " bid";
      int bid = Integer.parseInt(data.getProperties().get(propertyName, "0").toString());
      return bid;
    }
    
    public static boolean doesPlayerHaveBid(GameData data, PlayerID player)
    {
        return getBidAmount(data, player) != 0;
    }

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
      
      if(m_hasBid)
          return;
      
      m_bridge = bridge;  
      m_bid = getBidAmount(data, bridge.getPlayerID());
      m_spent = 0;
      
      
      
  }

  @Override
  protected String removeFromPlayer(PlayerID player, IntegerMap<Resource> resources, CompositeChange change, Collection<Unit> units)
  {
      m_spent = resources.getInt(super.getData().getResourceList().getResource(Constants.IPCS));
      return (m_bid - m_spent) + " IPC unused";
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
       
       m_hasBid = false;

   }
  
    /**
     * Returns the state of the Delegate.
     */
    public Serializable saveState()
    {
        BidPurchaseState s = new BidPurchaseState();
        s.superState = super.saveState();
        s.m_bid = m_bid;
        s.m_hasBid = m_hasBid;
        s.m_spent = this.m_spent;
        return s;
    }
    
   
    /**
     * Loads the delegates state
     */
    public void loadState(Serializable state)
    { 
        BidPurchaseState s  = (BidPurchaseState)  state;
        m_bid = s.m_bid;
        m_spent = s.m_spent;
        m_hasBid = s.m_hasBid;
        super.loadState(s.superState);
    }
  
  
  
 
}


class BidPurchaseState implements Serializable
{
    Serializable superState;
    int m_bid;
    int m_spent;
    boolean m_hasBid;
}
