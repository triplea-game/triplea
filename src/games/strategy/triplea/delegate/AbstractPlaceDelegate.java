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
 * PlaceDelegate.java
 *
 * Created on November 2, 2001, 12:29 PM
 */

package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;

/**
 *
 * Logic for placing units.  <p>
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * Known limitations.
 *
 * Doesnt take into account limits on number of factories that can be produced.
 *
 * The situation where one has two non original factories a,b each with production 2.
 * If sea zone e neighbors a,b and sea zone f neighbors b.  Then producing 2 in e
 * could make it such that you cannot produce in f.
 * The reason is that the production in e could be assigned to the
 * factory in b, leaving no capacity to produce in f.
 * If anyone ever accidently runs into this situation then they can
 * undo the production, produce in f first, and then produce in e.
 */
public abstract class AbstractPlaceDelegate implements SaveableDelegate
{
	private String m_name;
	private String m_displayName;
	private DelegateBridge m_bridge;
	private GameData m_data;
	private IntegerMap m_alreadyProduced = new IntegerMap(); //maps Territory-> alreadyProduced this turn
	private Set m_producedFactory = new HashSet();
	private PlayerID m_player;

	public void initialize(String name)
	{
		initialize(name, name);
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
		m_bridge = aBridge;
		m_data = gameData;
		m_player = aBridge.getPlayerID();
	}

	public String getName()
	{
		return m_name;
	}

	public String getDisplayName()
	{
		return m_displayName;
	}


	/**
	 * A message from the given player.
	 */
	public Message sendMessage(Message aMessage)
	{
		if(! (aMessage instanceof PlaceMessage))
			throw new IllegalArgumentException("Place delegate received message of wrong type:" + aMessage);

		PlaceMessage placeMessage = (PlaceMessage) aMessage;

		Message error = isValidPlacement(placeMessage, m_player);
		if(error!= null)
			return error;

		return placeUnits(placeMessage, m_player);
	}

	Message isValidPlacement(PlaceMessage placeMessage, PlayerID player)
	{
		//do we hold enough units
		Message error = playerHasEnoughUnits(placeMessage, player);
		if(error!= null)
			return error;

		//can we produce that much
		error = canProduce(placeMessage, player, m_alreadyProduced);
		if(error!= null)
			return error;

		//can we place it
		error = canUnitsBePlaced(placeMessage, player);
		if(error!= null)
			return error;

		return null;
	}

	/**
	 * Make sure the player has enough in hand to place the units.
	 */
	Message playerHasEnoughUnits(PlaceMessage placeMessage, PlayerID player)
	{
		//make sure the player has enough units in hand to place
		if(!player.getUnits().getUnits().containsAll(placeMessage.getUnits()))
			return new StringMessage("Not enough units", true);
		return null;
	}

	/**
	 * Make sure the units can be placed.
	 */
	Message canUnitsBePlaced(PlaceMessage placeMessage, PlayerID player)
	{
		Territory to = placeMessage.getTo();
		Collection units = placeMessage.getUnits();

		//check we havent just put a factory there
		if(m_producedFactory.contains(to))
			return new StringMessage("Factories cant produce until 1 turn after they are created", true);

		if(to.isWater())
		{
			//make sure all units are water
			if(! Match.allMatch(units, Matches.UnitIsSea))
				return new StringMessage("Cant place non naval units in sea zone", true);

			//make sure all units not the players are allied
			Set playersWithUnits = to.getUnits().getPlayersWithUnits();
			playersWithUnits.remove(player);
			Iterator iter = playersWithUnits.iterator();
			while(iter.hasNext())
			{
				PlayerID other = (PlayerID) iter.next();
				if(! m_data.getAllianceTracker().isAllied(player, other))
					return new StringMessage("Cant place units in water when the enemy has units in the same Sea Zone :" + other.getName(), true);
			}
		} else  //if land
		{
			//make sure we own the territory
			if(!to.getOwner().equals(player) )
				return new StringMessage("You don't own " +  placeMessage.getTo().getName(), true);
			//make sure all units are land
			if(!Match.allMatch(units, Matches.UnitIsNotSea))
				return new StringMessage("Cant place sea units on land", true);
		}

		//make sure only 1 AA
		if(Match.countMatches(units, Matches.UnitIsAA) >= 1)
		{
			//if trying to place 2
			if( Match.countMatches(units, Matches.UnitIsAA) > 1)
				return new StringMessage("Can only have 1 AA per territory", true);

			//if one already exists
			if(to.getUnits().someMatch(Matches.UnitIsAA))
				return new StringMessage("Can only have 1 AA per territory", true);
		}

		//make sure only max Factories
		if(Match.countMatches(units, Matches.UnitIsFactory) >= 1)
		{
            //after placement this is how many factories there will be
            int factoryCount = Match.countMatches(units, Matches.UnitIsFactory) +
                               to.getUnits().getMatches(Matches.UnitIsFactory ).size();

            //max factories allowed
            int maxFactory = games.strategy.triplea.Properties.getFactoriesPerCountry(m_data);

            if(factoryCount > maxFactory)
            {
            	return new StringMessage("Can only have " + maxFactory + " " + (maxFactory > 1 ? "factories" : "factory") +  " per territory", true);
            }
		}
		return null;
	}

	/**
	 *
	 * @return gets the production of the territory, ignores wether the territory was an original factory
	 */
	protected abstract int getProduction(Territory territory);

	/**
	 * Test whether or not the territory has the factory resources to
	 * support the placement.
	 * AlreadyProduced maps territory->units already produced this turn by that territory.
	 */
	protected StringMessage canProduce(PlaceMessage placeMessage, PlayerID player, IntegerMap alreadyProduced)
	{
		Territory producer = getProducer(placeMessage.getTo(), alreadyProduced);
		//the only reason to could be null is if its water and no
		//territories adjacent have factories
		if(producer == null)
			return new StringMessage("No factory adjacent to " + placeMessage.getTo().getName(), true);



		//make sure the territory wasnt conquered this turn
		if(wasConquered(producer))
			return new StringMessage(producer.getName() + " was conqured this turn and cannot produce till next turn", true);

		//make sure there is a factory
		if(! hasFactory(producer))
		{
			//check to see if we are producing a factory
			boolean onlyOne = placeMessage.getUnits().size() == 1;
			boolean allFactory = Match.allMatch(placeMessage.getUnits(), Matches.UnitIsFactory);
			if( onlyOne &&  allFactory)
				return null;
			else
				return new StringMessage("No Factory in " + producer.getName(), true);
		}

		//check production

		//if its an original factory then unlimited production
		TerritoryAttatchment ta = TerritoryAttatchment.get(producer);
		boolean originalFactory = ta.isOriginalFactory();
		boolean playerIsOriginalOwner = m_player.equals(getOriginalFactoryOwner(producer));

		if( originalFactory && playerIsOriginalOwner )
			return null;

		//a factory can produce the same number of units as the number of ipcs the
		//territroy generates each turn
		int unitCount = placeMessage.getUnits().size() + alreadyProduced.getInt(producer);
        int production = getProduction(producer);
        if(production == 0)
          production = 1; //if it has a factory then it can produce at least 1
		if(unitCount > production)
			return new StringMessage("Can only produce " + production + " in " +  producer.getName(), true);
		return null;
	}

	private boolean wasConquered(Territory t)
	{
		BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();
		return tracker.wasConquered(t);
	}

	/**
	 * Returns the better producer of the two territories, either of which can be null.
	 */
	private Territory getBetterProducer(Territory t1, Territory t2, IntegerMap alreadyProduced)
	{
		//anything is better than nothing
		if(t1 == null)
			return t2;
		if(t2 == null)
			return t1;

		//conquered cant produce
		if(wasConquered(t1))
			return t2;
		if(wasConquered(t2))
			return t1;

		//original factories are good
		TerritoryAttatchment t1a = TerritoryAttatchment.get(t1);
		if(t1a.isOriginalFactory())
			return t1;
		TerritoryAttatchment t2a = TerritoryAttatchment.get(t2);
		if(t2a.isOriginalFactory())
			return t2;

		//which can produce the most
		if(getProduction(t1)  - alreadyProduced.getInt(t1) >getProduction(t2) - alreadyProduced.getInt(t2))
			return t1;
		return t2;
	}

	private boolean hasFactory(Territory to)
	{
		return to.getUnits().someMatch(Matches.UnitIsFactory);
	}

	/**
	 * Returns the territory that would do the producing if
	 * units are to be placed in a given territory.
	 * Returns null if no suitable territory could be found.
	 */
	private Territory getProducer(Territory to, IntegerMap alreadyProduced)
	{
		//if not water then must produce in that territory
		if(!to.isWater() )
			return to;

		Territory neighborFactory = null;
		Iterator iter = m_data.getMap().getNeighbors(to).iterator();
		while(iter.hasNext() )
		{
			Territory current = (Territory)iter.next();
			if( hasFactory( current) && ! m_producedFactory.contains(current))
			{
				neighborFactory = getBetterProducer(current, neighborFactory, alreadyProduced);
			}
		}
		return neighborFactory;
	}

	/**
	 * There must be a factory in the territotory or an illegal state exception will be thrown.
	 * return value may be null.
	 */
	private PlayerID getOriginalFactoryOwner(Territory territory)
	{
		Collection factoryUnits = territory.getUnits().getMatches(Matches.UnitIsFactory);
		if(factoryUnits.size() != 1)
			throw new IllegalStateException("No factory in territroy:" + territory);

		Unit factory = (Unit) factoryUnits.iterator().next();
		return DelegateFinder.battleDelegate(m_data).getOriginalOwnerTracker().getOriginalOwner(factory);
	}

	private Message placeUnits(PlaceMessage placeMessage, PlayerID player)
	{
		Collection units = placeMessage.getUnits();

		Territory producer = getProducer(placeMessage.getTo(), m_alreadyProduced);
		m_alreadyProduced.add(producer, units.size() );

		if(Match.someMatch(units, Matches.UnitIsFactory))
			m_producedFactory.add(producer);


		Collection factoryAndAA = Match.getMatches(units, Matches.UnitIsAAOrFactory);
		DelegateFinder.battleDelegate(m_data).getOriginalOwnerTracker().addOriginalOwner(factoryAndAA, m_player);

		Change remove= ChangeFactory.removeUnits(player, units);
		Change place = ChangeFactory.addUnits(placeMessage.getTo(), units);
		m_bridge.addChange(remove);
		m_bridge.addChange(place);

		String transcriptText = Formatter.unitsToText(units) + " placed in " +placeMessage.getTo().getName();
		m_bridge.getTranscript().write(transcriptText);

		return new StringMessage("done");
	}

	/**
	 * Called before the delegate will stop running.
	 */
	public void end()
	{
		PlayerID player = m_bridge.getPlayerID();
		//clear all units not placed
		Collection units = player.getUnits().getUnits();
		if(!units.isEmpty())
		{
			Change change = ChangeFactory.removeUnits( player, units);
			m_bridge.addChange(change);
		}

		//reset ourseleves for next turn
		m_alreadyProduced = new IntegerMap();
		m_producedFactory.clear();

	}

	/**
	 * Can the delegate be saved at the current time.
	 * @arg message, a String[] of size 1, hack to pass an error message back.
	 */
	public boolean canSave(String[] message)
	{
		return true;
	}

	/**
	 * Returns the state of the Delegate.
	 */
	public Serializable saveState()
	{
		PlaceState state = new PlaceState();
		state.m_alreadyProduced = m_alreadyProduced;
		state.m_producedFactory = m_producedFactory;
		return state;
	}

	/**
	 * Loads the delegates state
	 */
	public void loadState(Serializable aState)
	{
		PlaceState state = (PlaceState) aState;
		m_alreadyProduced = state.m_alreadyProduced;
		m_producedFactory = state.m_producedFactory;
	}

	protected GameData getData()
	{
		return m_data;
	}
}


class PlaceState implements Serializable
{
	public IntegerMap m_alreadyProduced;
	public Set m_producedFactory;
}