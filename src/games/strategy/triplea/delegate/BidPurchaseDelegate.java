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
  /**
   * subclasses can over ride this method to use different restrictions as to what a player can buy
   */
  protected boolean canAfford(IntegerMap costs, PlayerID player)
  {
    int ipcCost = costs.getInt(Constants.IPCS);

    String propertyName = player.getName() + " bid";

    int ipcAvailable =  Integer.parseInt(getData().getProperties().get(propertyName).toString());
    return ipcAvailable >= ipcCost;
  }


  protected void removeFromPlayer(PlayerID player, IntegerMap resources)
  {
    //we dont take away since the limit is a (now) non editable property
  }


}