package games.strategy.triplea.delegate;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.*;
import games.strategy.engine.delegate.*;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.delegate.message.*;

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

  // Allow player to place as many units as they want in bid phase
  protected int getMaxUnitsToBePlaced(Territory to, PlayerID player)
  {
    return -1;
  }

  // Allow production of any number of units
  protected StringMessage checkProduction(Territory to, Collection units, PlayerID player)
  {
    return null;
  }

  // Return whether we can place bid in a certain territory
  protected StringMessage canProduce(PlaceMessage placeMessage, PlayerID player)
  {
    Territory to = placeMessage.getTo();
    Collection units = placeMessage.getUnits();
    return canProduce(to, units, player);
  }

  // Return whether we can place bid in a certain territory
  protected StringMessage canProduce(Territory to, Collection units, PlayerID player)
  {

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

  // Return collection of bid units which can placed in a land territory
  protected Collection getUnitsToBePlacedLand(Territory to, Collection units, PlayerID player)
  {
    Collection placeableUnits = new ArrayList();

    //make sure only 1 AA in territory for classic
    if (isFourthEdition())
    {
      placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsAA));
    }
    else
    {
      //allow 1 AA to be placed if none already exists
      if (!to.getUnits().someMatch(Matches.UnitIsAA))
	placeableUnits.addAll(Match.getNMatches(units, 1, Matches.UnitIsAA));
    }
    
    CompositeMatch groundUnits = new CompositeMatchAnd();
    groundUnits.add(Matches.UnitIsLand);		
    groundUnits.add(new InverseMatch(Matches.UnitIsAAOrFactory));
    placeableUnits.addAll(Match.getMatches(units, groundUnits));
    placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsAir));
            
    //make sure only max Factories
    if (Match.countMatches(units, Matches.UnitIsFactory) >= 1)
    {
      //if its an original factory then unlimited production
      TerritoryAttatchment ta = TerritoryAttatchment.get(to);

      //4th edition, you cant place factories in territories with no production
      if(!(isFourthEdition() && ta.getProduction() == 0))
      {
	//this is how many factories exist now
	int factoryCount = to.getUnits().getMatches(Matches.UnitIsFactory).size();
                    
	//max factories allowed
	int maxFactory = games.strategy.triplea.Properties.getFactoriesPerCountry(m_data);

	placeableUnits.addAll(Match.getNMatches(units, maxFactory - factoryCount, Matches.UnitIsFactory));
      }
    }

    return placeableUnits;
  }

  protected int getProduction(Territory t)
  {
    throw new UnsupportedOperationException("Not implemented");
  }


}
