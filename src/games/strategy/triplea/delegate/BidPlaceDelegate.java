package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import java.util.*;
import games.strategy.util.IntegerMap;
import games.strategy.engine.message.*;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.delegate.message.StringMessage;
import games.strategy.util.Match;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class BidPlaceDelegate extends AbstractPlaceDelegate
{

  public BidPlaceDelegate()
  {
  }


  protected StringMessage canProduce(PlaceMessage placeMessage, PlayerID player)
  {
    Territory to = placeMessage.getTo();
    Collection units = placeMessage.getUnits();

    //we can place if no enemy units and its water
    if(to.isWater())
    {
      if(Match.someMatch(units,Matches.UnitIsLand))
        return new StringMessage("Cant place land units at sea", true);
      else if(to.getUnits().allMatch(Matches.alliedUnit(player, getData())))
         return null;
      else
         return new StringMessage("Cant place in sea zone containing enemy units", true);
    }
    //we can place on territories we own
    else
    {
      if(Match.someMatch(units,Matches.UnitIsSea))
        return new StringMessage("Cant place sea units on land", true);
      else if(to.getOwner().equals(player))
        return null;
      else
        return new StringMessage("You dont own " + to.getName(), true);
    }

  }

  protected int getProduction(Territory t)
  {
    throw new UnsupportedOperationException("Not implemented");
  }


}