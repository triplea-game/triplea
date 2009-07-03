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

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.util.*;

import java.util.*;



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
    protected String checkProduction(Territory to, Collection<Unit> units,
            PlayerID player)
    {
        return null;
    }

    // Return whether we can place bid in a certain territory
    @Override
    protected String canProduce(Territory to, Collection<Unit> units, PlayerID player)
    {
        //we can place if no enemy units and its water
        if (to.isWater())
        {
            if (Match.someMatch(units, Matches.UnitIsLand))
                return "Cant place land units at sea";
            else if (to.getUnits().allMatch(
                    Matches.alliedUnit(player, getData())))
                return null;
            else
                return "Cant place in sea zone containing enemy units";
        }
        //we can place on territories we own
        else
        {
            if (Match.someMatch(units, Matches.UnitIsSea))
                return "Cant place sea units on land";
            else if (to.getOwner().equals(player))
                return null;
            else
                return "You dont own " + to.getName();
        }

    }

    // Return collection of bid units which can placed in a land territory
    protected Collection<Unit> getUnitsToBePlacedLand(Territory to, Collection<Unit> units,
            PlayerID player)
    {
        Collection<Unit> placeableUnits = new ArrayList<Unit>();

        //make sure only 1 AA in territory for classic
        if (isFourthEdition())
        {
            placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsAA));
        } else
        {
            //allow 1 AA to be placed if none already exists
            if (!to.getUnits().someMatch(Matches.UnitIsAA))
                placeableUnits.addAll(Match.getNMatches(units, 1,
                        Matches.UnitIsAA));
        }

        CompositeMatch<Unit> groundUnits = new CompositeMatchAnd<Unit>();
        groundUnits.add(Matches.UnitIsLand);
        groundUnits.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));
        placeableUnits.addAll(Match.getMatches(units, groundUnits));
        placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsAir));

        //make sure only max Factories
        if (Match.countMatches(units, Matches.UnitIsFactory) >= 1)
        {
            //if its an original factory then unlimited production
            TerritoryAttachment ta = TerritoryAttachment.get(to);

            //4th edition, you cant place factories in territories with no
            // production
            if (!(isFourthEdition() && ta.getProduction() == 0))
            {
                //this is how many factories exist now
                int factoryCount = to.getUnits().getMatches(
                        Matches.UnitIsFactory).size();

                //max factories allowed
                int maxFactory = games.strategy.triplea.Properties
                        .getFactoriesPerCountry(m_data);

                placeableUnits.addAll(Match.getNMatches(units, maxFactory
                        - factoryCount, Matches.UnitIsFactory));
            }
        }

        return placeableUnits;
    }
}
