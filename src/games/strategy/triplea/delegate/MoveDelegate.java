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
 * MoveDelegate.java
 *
 * Created on November 2, 2001, 12:24 PM
 */

package games.strategy.triplea.delegate;

import java.util.*;
import java.io.Serializable;

import games.strategy.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.*;
import games.strategy.engine.transcript.*;

import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.formatter.Formatter;
import java.io.*;
import games.strategy.engine.framework.*;



/**
 *
 * Responsible for moving units on the board. <p>
 * Responible for checking the validity of a move, and for moving
 * the units.  <br>
 *
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public class MoveDelegate implements SaveableDelegate
{
  private String m_name;
  private String m_displayName;
  private DelegateBridge m_bridge;
  private GameData m_data;
  private PlayerID m_player;
  private boolean m_firstRun = true;
  private boolean m_nonCombat;
  private TransportTracker m_transportTracker = new TransportTracker();
  private IntegerMap m_alreadyMoved = new IntegerMap();



  // A collection of UndoableMoves
  private List m_movesToUndo = new ArrayList();

  //The current move
  private UndoableMove m_currentMove;

  /** Creates new MoveDelegate */
  public MoveDelegate()
  {
  }

  public void initialize(String name, String displayName)
  {
    m_name = name;
    m_displayName = displayName;
  }


  /**
   * Want to make sure that all units in the sea that can be transported are
   * marked as being transported by something.
   *
   * We assume that all transportable units in the sea are
   * in a transport, no exceptions.
   *
   */
  private void firstRun()
  {
    m_firstRun = false;
    //check every territory
    Iterator allTerritories = m_data.getMap().getTerritories().iterator();
    while(allTerritories.hasNext())
    {
      Territory current =  (Territory) allTerritories.next();
      //only care about water
      if(!current.isWater())
        continue;

      Collection units = current.getUnits().getUnits();
      if(units.size() == 0 || !Match.someMatch(units, Matches.UnitIsLand))
        continue;

      //map transports, try to fill
      Collection transports = Match.getMatches(units, Matches.UnitIsTransport);
      Collection land = Match.getMatches(units, Matches.UnitIsLand);
      Iterator landIter = land.iterator();
      while(landIter.hasNext())
      {
        Unit toLoad = (Unit) landIter.next();
        UnitAttatchment ua = UnitAttatchment.get(toLoad.getType());
        int cost = ua.getTransportCost();
        if(cost == -1)
          throw new IllegalStateException("Non transportable unit in sea");

        //find the next transport that can hold it
        Iterator transportIter = transports.iterator();
        boolean found = false;
        while(transportIter.hasNext())
        {
          Unit transport = (Unit) transportIter.next();
          int capacity = m_transportTracker.getAvailableCapacity(transport);
          if(capacity >= cost)
          {
            m_transportTracker.load(toLoad, transport);
            found = true;
            break;
          }
        }
        if(!found)
          throw new IllegalStateException("Cannot load all units");
      }
    }
  }

  /**
   * Called before the delegate will run.
   */
  public void start(DelegateBridge aBridge, GameData gameData)
  {
    if(aBridge.getStepName().endsWith("NonCombatMove"))
      m_nonCombat = true;
    else if(aBridge.getStepName().endsWith("CombatMove"))
      m_nonCombat = false;
    else
      throw new IllegalStateException("Cannot determine combat or not");

    m_bridge = aBridge;
    PlayerID player = aBridge.getPlayerID();

    //reset movement, dont do at end of turn since
    //we move a couple times.
    if(!m_nonCombat)
    {
      m_alreadyMoved.clear();
      m_transportTracker.clearUnloadedCapacity();
    }
    m_data = gameData;
    m_player = player;

    if(m_firstRun)
      firstRun();
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
    if(aMessage instanceof MoveMessage)
      return move((MoveMessage) aMessage, m_player);
    else if(aMessage instanceof MustMoveAirQueryMessage)
      return new TerritoryCollectionMessage( getTerritoriesWhereAirCantLand());
    else if(aMessage instanceof MustMoveWithQuery)
      return mustMoveWith((MustMoveWithQuery) aMessage);
    else if(aMessage instanceof UndoMoveMessage)
      return undoMove();
    else if (aMessage instanceof MoveCountRequestMessage)
        return getMoveCount();
    else
      throw new IllegalArgumentException("Move delegate received message of wrong type:" + aMessage);
  }

  private MoveCountReplyMessage getMoveCount()
  {
    String[] moves = new String[m_movesToUndo.size()];
    int index = 0;

    Iterator iter = m_movesToUndo.iterator();
    while (iter.hasNext()) {
      UndoableMove item = (UndoableMove)iter.next();
      moves[index] = item.getDescription();
      index++;
    }
    return new MoveCountReplyMessage(moves);

  }

  private StringMessage undoMove()
  {
    if(m_movesToUndo.isEmpty())
        return new StringMessage("No moves to undo", true);
    UndoableMove lastMove = (UndoableMove) m_movesToUndo.get(m_movesToUndo.size() -1);
    if( !lastMove.getcanUndo())
        return new StringMessage("Move cannot be undone:" + lastMove.getReasonCantUndo(), true);

    lastMove.undo(m_data, m_bridge);
    m_movesToUndo.remove(m_movesToUndo.size() -1);

    return null;
  }

  private MustMoveWithReply mustMoveWith(MustMoveWithQuery query)
  {
    return new MustMoveWithReply( mustMoveWith(query.getUnits(), query.getStart()), movementLeft(query.getUnits()));
  }

  private IntegerMap movementLeft(Collection units)
  {
    IntegerMap movement = new IntegerMap();

    Iterator iter = units.iterator();
    while(iter.hasNext())
    {
      Unit current = (Unit) iter.next();
      movement.put(current, MoveValidator.movementLeft(current, m_alreadyMoved));
    }

    return movement;
  }

  private Map mustMoveWith(Collection units, Territory start)
  {
    List sortedUnits = new ArrayList(units);



    Collections.sort(sortedUnits, increasingMovement);

    Map mapping = new HashMap();
    mapping.putAll(transportsMustMoveWith(sortedUnits));
    mapping.putAll(carrierMustMoveWith(sortedUnits, start));
    return mapping;
  }

  private Map transportsMustMoveWith(Collection units)
  {
    Map mustMoveWith = new HashMap();
    //map transports
    Collection transports = Match.getMatches(units, Matches.UnitIsTransport);
    Iterator iter = transports.iterator();
    while(iter.hasNext())
    {
      Unit transport = (Unit) iter.next();
      Collection transporting = m_transportTracker.transporting(transport);
      mustMoveWith.put(transport, transporting);
    }
    return mustMoveWith;
  }

  private Map carrierMustMoveWith(Collection units, Territory start)
  {
    //we want to get all air units that are owned by our allies
    //but not us that can land on a carrier
    CompositeMatch friendlyNotOwnedAir = new CompositeMatchAnd();
    friendlyNotOwnedAir.add(Matches.alliedUnit(m_player, m_data));
    friendlyNotOwnedAir.addInverse(Matches.unitIsOwnedBy(m_player));
    friendlyNotOwnedAir.add(Matches.UnitCanLandOnCarrier);

    Collection alliedAir = start.getUnits().getMatches(friendlyNotOwnedAir);


    if(alliedAir.isEmpty())
      return Collections.EMPTY_MAP;

    //remove air that can be carried by allied
    CompositeMatch friendlyNotOwnedCarrier = new CompositeMatchAnd();
    friendlyNotOwnedCarrier.add(Matches.UnitIsCarrier);
    friendlyNotOwnedCarrier.add(Matches.alliedUnit(m_player, m_data));
    friendlyNotOwnedCarrier.addInverse(Matches.unitIsOwnedBy(m_player));

    Collection alliedCarrier = start.getUnits().getMatches(friendlyNotOwnedCarrier);


    Iterator alliedCarrierIter = alliedCarrier.iterator();
    while(alliedCarrierIter.hasNext())
    {
      Unit carrier = (Unit) alliedCarrierIter.next();
      Collection carrying = getCanCarry(carrier, alliedAir);
      alliedAir.removeAll(carrying);
    }

    if(alliedAir.isEmpty())
      return Collections.EMPTY_MAP;

    Map mapping = new HashMap();
    //get air that must be carried by our carriers
    Collection ownedCarrier = Match.getMatches(units, Matches.UnitIsCarrier);


    Iterator ownedCarrierIter = ownedCarrier.iterator();
    while(ownedCarrierIter.hasNext())
    {
      Unit carrier = (Unit) ownedCarrierIter.next();
      Collection carrying = getCanCarry(carrier, alliedAir);
      alliedAir.removeAll(carrying);

      mapping.put(carrier, carrying);
    }

    return mapping;
  }

  private Collection getCanCarry(Unit carrier, Collection selectFrom)
  {
    UnitAttatchment ua = UnitAttatchment.get(carrier.getUnitType());
    Collection canCarry = new ArrayList();

    int available = ua.getCarrierCapacity();
    Iterator iter = selectFrom.iterator();
    while(iter.hasNext())
    {
      Unit plane = (Unit) iter.next();
      UnitAttatchment planeAttatchment = UnitAttatchment.get(plane.getUnitType());
      int cost = planeAttatchment.getCarrierCost();
      if(available >= cost)
      {
        available -= cost;
        canCarry.add(plane);
      }
      if(available == 0)
        break;
    }
    return canCarry;
  }




  private StringMessage move(MoveMessage message, PlayerID id)
  {
    Route route = message.getRoute();
    Collection units = message.getUnits();

    String error = validateMove(units, route, id);
    if(error != null)
      return new StringMessage(error, true);
    //do the move
    m_currentMove = new UndoableMove(m_data);
    StringMessage rVal = moveUnits(units, route, id);
    if(!rVal.isError())
    {
      m_movesToUndo.add(m_currentMove);
    }
    m_currentMove = null;
    return rVal;
  }


  private String validateMove(Collection units, Route route, PlayerID player)
  {
    String error;

    if(m_nonCombat)
    {
      error = validateNonCombat(units, route, player);
      if(error != null)
        return error;
    }

    if(!m_nonCombat)
    {
      error = validateCombat(units, route, player);
      if(error != null)
        return error;
    }

    error = validateBasic(units, route, player);
    if(error != null)
      return error;

    error = validateAirCanLand(units, route, player);
    if(error != null)
      return error;

    error = validateTransport(units, route, player);
    if(error != null)
      return error;

    error = validateCanal(units, route, player);
    if(error != null)
      return error;


    //dont let the user move out of a battle zone
    //note that this restricts planes who may be able to fly away
    if(DelegateFinder.battleDelegate(m_data).getBattleTracker().hasPendingBattle(route.getStart(), false))
    {
      boolean unload = MoveValidator.isUnload(route);
      PlayerID endOwner = route.getEnd().getOwner();
      boolean attack = !m_data.getAllianceTracker().isAllied(endOwner, m_player);
      //unless they are unloading into another battle
      if(! (unload  && attack ))
        return "Cant move units out of battle zone";
    }

    //make sure we can afford to pay neutral fees
    int cost = getNeutralCharge(route);
    int resources = player.getResources().getQuantity(Constants.IPCS);
    if(resources - cost < 0)
      return "Not enough money to pay for violating neutrality";

    //TODO
    //special cases, suez and panama canal, can only move if both are owned
    //make sure aircraft carriers dont move away
    //if we are going through nuetral make sure we can pay
    //make sure if an aircraft must retreat through a nuetral
    //it can pay as well
    return null;
  }


  private String validateCanal(Collection units, Route route, PlayerID player)
  {
    Collection territories = route.getTerritories();

    //check suez canal
    Territory eastMed = m_data.getMap().getTerritory("East Mediteranean Sea Zone");
    Territory redSea = m_data.getMap().getTerritory("Red Sea Zone");
    if(territories.contains(eastMed) && territories.contains(redSea))
    {
      Territory egypt = m_data.getMap().getTerritory("Anglo Sudan Egypt");
      Territory iraq = m_data.getMap().getTerritory("Syria Jordan");

      if(! m_data.getAllianceTracker().isAllied(player, egypt.getOwner()) ||
         ! m_data.getAllianceTracker().isAllied(player, iraq.getOwner()))
        return "Must own Egypt and Syria/Jordan  to go through Suez Canal";

      BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();
      if(tracker.wasConquered(egypt) || tracker.wasConquered(iraq))
        return "Cannot move through canal without owning Egypt and Syria/Jordan for an entire turn.";

    }

    //check panama canal
    Territory carib = m_data.getMap().getTerritory("Carribean Sea Zone");
    Territory westPan = m_data.getMap().getTerritory("West Panama Sea Zone");
    if(territories.contains(carib) && territories.contains(westPan))
    {
      Territory panama = m_data.getMap().getTerritory("Panama");

      if(! m_data.getAllianceTracker().isAllied(player, panama.getOwner()))

        return "Must own panama to go through Panama Canal";

      BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();
      if(tracker.wasConquered(panama))
        return "Cannot move through canal without owning panama an entire turn.";
    }

    return null;

  }

  private String validateCombat(Collection units, Route route, PlayerID player)
  {
    if(Match.someMatch(units, Matches.UnitIsAA))
      return "Cant move aa guns in combat movement phase";
    return null;
  }

  private String validateNonCombat(Collection units, Route route, PlayerID player)
  {
    CompositeMatch battle = new CompositeMatchOr();
    battle.add(Matches.territoryHasEnemyUnits(player, m_data));
    battle.add(Matches.isTerritoryEnemy(player, m_data));
    battle.add(Matches.TerritoryIsNuetral);

    if(Match.allMatch(units, Matches.UnitIsAir))
    {
      if(route.someMatch(Matches.TerritoryIsNuetral))
        return "Air units cannot fly over neutral territories in non combat";

      if( battle.match(route.getEnd()))
        return "Cant advance air units to battle in non combat";
    }
    else
    {
      if(route.someMatch(battle))
        return "Cant advance units to battle in non combat";
    }
    return null;
  }


  private String validateBasic(Collection units, Route route, PlayerID player)
  {
    //make sure all units are actually in the start territory
    if(!route.getStart().getUnits().containsAll(units))
    {
      return "Not enough units in starting territory";
    }

    //make sure all units are at least friendly
    if(!Match.allMatch(units, Matches.alliedUnit(m_player, m_data)))
      return "Can only move friendly units";

    //check we have enough movement
    //exclude transported units
    Collection moveTest;
    if(route.getStart().isWater())
    {
      moveTest = MoveValidator.getNonLand(units);
    } else
    {
      moveTest = units;
    }
    if(!MoveValidator.hasEnoughMovement(moveTest, m_alreadyMoved, route.getLength()))
      return "Units do not enough movement";

    //TODO not for aircarft
    //check to see no enemy units on path
    if(!MoveValidator.onlyAlliedUnitsOnPath(route, player, m_data))
      if(! MoveValidator.isAir(units))
        return "Non allied units on path";

    //if there is a nuetral in the middle must stop unless all are air
    if(MoveValidator.hasNuetralBeforeEnd(route))
    {
      if(! MoveValidator.isAir(units))
        return "Must stop land units when passing through nuetral territories";
    }

    //check if we are blitzing
    //if we are blitzing check that all units are capable
    if(MoveValidator.isBlitz(route, player, m_data))
    {
      if(!MoveValidator.canBlitz(Match.getMatches(units, Matches.UnitIsNotAir)))
        return "Cannot blitz with non blitzing units";
    }

    //make sure no conquered territories on route
    if(MoveValidator.hasConqueredNonBlitzedOnRoute(route, m_data))
    {
      //unless we are all air
      if( !Match.allMatch(units, Matches.UnitIsAir))
        return "Cannot move through newly captured territories";
    }

    //make sure that no non sea non transportable no carriable units
    //end at sea
    if(route.getEnd().isWater())
    {
      if( MoveValidator.hasUnitsThatCantGoOnWater(units))
        return "Those units cannot end at water";
    }

    //if we are water make sure no land
    if(MoveValidator.hasSea(units))
    {
      if(MoveValidator.hasLand(route))
        return "Sea units cant go on land";
    }

    //make sure that we dont send aa guns to attack
    if(Match.someMatch(units, Matches.UnitIsAA))
    {
      //TODO
      //dont move if some were conquered
      BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();

      for(int i = 0; i < route.getLength(); i++)
      {
        Territory current = route.at(i);
        if(! (current.isWater() || current.getOwner().equals(m_player) ||
           m_data.getAllianceTracker().isAllied(m_player, current.getOwner())))

           return "AA units cant advance to battle";

      }
    }

    //only allow aa into a terland territory if one already present.
    if(Match.someMatch(units, Matches.UnitIsAA) &&
       route.getEnd().getUnits().someMatch(Matches.UnitIsAA) &&
       !route.getEnd().isWater())
    {
      return "Only one AA gun allowed in a territroy";
    }

    //only allow 1 aa to unload
    if(route.getStart().isWater() &&
       !route.getEnd().isWater() &&
       Match.countMatches(units, Matches.UnitIsAA) > 1 )
    {
      return "Only 1 AA gun allowed in a territory";
    }
    return null;
  }

  private String validateAirCanLand(Collection units, Route route, PlayerID player)
  {
    //TODO,
    //if they cant all land in one territory then
    //they an error will be returned, even
    //if they could land in multiple territories

    //make sure air units have enough movement to land

    if(!MoveValidator.hasAir(units))
      return null;

    //could be a war zone, make sure we only look at
    //friendly units that will be in the territory after
    //the move
    Collection friendly = MoveValidator.getFriendly(route.getEnd(), player, m_data);
    friendly.addAll(units);
    Collection friendlyAir = Match.getMatches(friendly, Matches.UnitIsAir);

    if( !MoveValidator.canLand(friendlyAir, route.getEnd(), player, m_data))
    {
      //get movement left of units that are moving
      //not enough carrier capacity at end
      Collection air = Match.getMatches(units, Matches.UnitIsAir);
      int distance = MoveValidator.getLeastMovement(air, m_alreadyMoved);
      distance = distance - route.getLength();

      Set neighbors = m_data.getMap().getNeighbors(route.getEnd(), distance);

      boolean canLand = false;
      Iterator iter = neighbors.iterator();

      //neutrals we will overfly in the first place
      Collection neutrals = getEmptyNeutral(route);
      int ipcs = player.getResources().getQuantity(Constants.IPCS);

      while(iter.hasNext() )
      {
        Territory possible = (Territory) iter.next();
        if(MoveValidator.canLand(air,possible, player, m_data))
        {
          //make sure we can pay for the neutrals we will
          //overfly when we go to land
          Set overflownNeutrals = new HashSet();
          overflownNeutrals.addAll(neutrals);
          overflownNeutrals.addAll(getBestNeutralEmptyCollection(route.getEnd(), possible, distance));
          if(ipcs >=  getNeutralCharge(overflownNeutrals.size()))
            canLand = true;
        }
      }

      //TODO - may be able to split air units up and land in different groups
      //eg a bomber and a fighter move to a carrier. Bomber can land on
      //carrier but has enough movment to make it to land,
      //fighter can land, but does not have enough movement to
      //make it to land.
      //Both can land somewhere, but there is no place where all can land
      if(!canLand)
      {
        if(air.size() == 1)
          return "No place for the air unit to land.";
        else
          return "There is no place where all the air units can land. You may be able to move these units in smaller groups.";
      }
    }


    return null;
  }

  private String validateTransport(Collection units, Route route, PlayerID player)
  {
    if(Match.allMatch(units, Matches.UnitIsAir))
      return null;

    if(!MoveValidator.hasWater(route))
      return null;

    //if unloading make sure length of route is only 1
    if(MoveValidator.isUnload(route))
    {
      if(route.getLength() > 1)
        return "Unloading units must stop where they are unloaded";
    }

    //if we are land make sure no water in route except for transport situations
    Collection land = Match.getMatches(units, Matches.UnitIsLand);

    //make sure we can be transported
    if(! Match.allMatch(land, Matches.UnitCanBeTransported))
      return "Unit cannot be transported";

    //make sure that the only the first or last territory is land
    //dont want situation where they go sea land sea
    if(MoveValidator.hasLand(route) && !(route.getStart().isWater() || route.getEnd().isWater()))
      return "Invalid move, only start or end can be land when route has water.";

    //simply because I dont want to handle it yet
    //checks are done at the start and end, dont want to worry about just
    //using a transport as a bridge yet
    //TODO handle this
    if( !route.getEnd().isWater()  && !route.getStart().isWater())
      return "Must stop units at a transport on route";

    if(route.getEnd().isWater() && route.getStart().isWater())
    {
      //make sure units and transports stick together
      Iterator iter = units.iterator();
      while(iter.hasNext())
      {
        Unit unit = (Unit) iter.next();
        UnitAttatchment ua = UnitAttatchment.get(unit.getType());
        //make sure transports dont leave their units behind
        if(ua.getTransportCapacity() != -1)
        {
          Collection holding = m_transportTracker.transporting(unit);
          if( holding != null && !units.containsAll(holding))
          {
            return "Transport cannot leave their units";
          }
        }
        //make sure units dont leave their transports behind
        if(ua.getTransportCost() != -1)
        {
          Unit transport = m_transportTracker.transportedBy(unit);
          if(transport != null && !units.contains(transport))
          {
            return "Unit must stay with its transport while moving";
          }
        }
      }
    }//end if end is water

    if(MoveValidator.isLoad(route))
    {
      if(mapTransports(route, land) == null)
        return "Not enough transports";

      if(route.getLength() != 1)
        return "Units cannot move before loading onto transports";

      Iterator iter = units.iterator();
      while(iter.hasNext())
      {
        Unit unit= (Unit) iter.next();
        if(m_alreadyMoved.getInt(unit) != 0)
          return "Units cannot move before loading onto transports";
      }
    }

    return null;
  }


  /**
   * We assume that the move is valid
   */
  private StringMessage moveUnits(Collection units, Route route, PlayerID id)
  {
    //mark movement
    markMovement(units, route);

    Collection arrivingUnits = units;
    if(!m_nonCombat)
    {
      Collection aaCasualties = fireAA(route, units);
      arrivingUnits = Util.difference(units, aaCasualties);
    }

    //if any non enemy territories on route
    //or if any enemy units on route the
    //battles on (note water could have enemy but its
    //not owned)
    CompositeMatch mustFightThrough = new CompositeMatchOr();
    mustFightThrough.add(Matches.isTerritoryEnemy(id, m_data));
    mustFightThrough.add(Matches.territoryHasEnemyUnits(id, m_data));

    if(route.someMatch(mustFightThrough ) && arrivingUnits.size() != 0)
    {
      boolean bombing = false;
      //could it be a bombuing raid
      boolean allCanBomb = Match.allMatch(units, Matches.UnitIsStrategicBomber);

      CompositeMatch enemyFactory = new CompositeMatchAnd();
      enemyFactory.add(Matches.UnitIsFactory);
      enemyFactory.add(Matches.enemyUnit(id, m_data));
      boolean targetToBomb = route.getEnd().getUnits().someMatch(enemyFactory);

      if(allCanBomb && targetToBomb)
      {
        StrategicBombQuery query = new StrategicBombQuery(route.getEnd());
        Message response = m_bridge.sendMessage(query);
        if(!(response instanceof BooleanMessage))
        {
          throw new IllegalStateException("Received message of wrong type. Message:" + response);
        }
        bombing = ((BooleanMessage) response).getBoolean();
      }

      DelegateFinder.battleDelegate(m_data).getBattleTracker().addBattle(route, arrivingUnits, m_transportTracker, bombing, id, m_data, m_bridge, m_currentMove);
    }

    //note that this must be done after adding battles since the battles
    //must be able to determine who transported who

    //TODO, put units in owned transports first
    Map transporting = mapTransports(route, units);
    markTransportsMovement(transporting, route);

    //actually move the units
    Change remove = ChangeFactory.removeUnits(route.getStart(), units);
    Change add = ChangeFactory.addUnits(route.getEnd(), arrivingUnits);
    m_bridge.addChange(remove);
    m_bridge.addChange(add);

    m_currentMove.addChange(remove);
    m_currentMove.addChange(add);

    Collection moved = Util.intersection(units, arrivingUnits);
    String transcriptText = Formatter.unitsToText(moved) + " moved from " + route.getStart().getName() + " to " +  route.getEnd().getName();

    m_currentMove.setDescription(Formatter.unitsToTextNoOwner(moved) + " moved from " + route.getStart().getName() + " to " +  route.getEnd().getName());

    m_bridge.getTranscript().write(transcriptText);

    return new StringMessage("done");
  }

  private Collection getBestNeutralEmptyCollection(Territory start, Territory end, int maxDistance)
  {
    //TODO fix this.  If there are two neutral territories
    //on the route, we may be able to find
    //a route with only one, currently its either
    //take the obvious unless a perfect route
    //with no neutrals can be found

    //get the obvious route
    Route route = m_data.getMap().getRoute(start, end);
    if(route.getLength() > maxDistance)
      throw new IllegalStateException("No route short enough." + "route:" + route + " maxDistance:" + maxDistance);

    Collection neutral = getEmptyNeutral(route);
    if(neutral.size() == 0)
    {
      return neutral;
    }


    //see if we can do better
    Match emptyNeutral = new CompositeMatchAnd(Matches.TerritoryIsNuetral, Matches.TerritoryIsEmpty);

    Route alternate = m_data.getMap().getRoute(start, end, emptyNeutral);
    if(alternate == null)
      return neutral;
    if(alternate.getLength() > maxDistance)
      return neutral;
    //route has no empty neutral states in path, no charge
    return new ArrayList();
  }

  private int getNeutralCharge(Route route)
  {
    return getNeutralCharge(getEmptyNeutral(route).size());
  }

  private int getNeutralCharge(int numberOfTerritories)
  {
    return numberOfTerritories *  games.strategy.triplea.Properties.getNeutralCharge(m_data);
  }

  private boolean hasConqueredNonBlitzed(Route route)
  {
    BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();

    for(int i = 0; i < route.getLength(); i++)
    {
      Territory current = route.at(i);
      if(tracker.wasConquered(current) && !tracker.wasBlitzed(current))
        return true;
    }
    return false;
  }


  private Collection getEmptyNeutral(Route route)
  {
    Match emptyNeutral = new CompositeMatchAnd(Matches.TerritoryIsEmpty, Matches.TerritoryIsNuetral);
    Collection neutral = route.getMatches(emptyNeutral);
    return neutral;
  }

  private void markMovement(Collection units, Route route)
  {
    int moved = route.getLength();
    Iterator iter = units.iterator();
    while(iter.hasNext())
    {
      Unit unit = (Unit) iter.next();
      m_alreadyMoved.add(unit, moved);
    }

    //if neutrals were taken over mark land units with 0 movement
    //if weve entered a non blitzed conquered territory, mark with 0 movement
    if(getEmptyNeutral(route).size() != 0 || hasConqueredNonBlitzed(route))
    {
      Collection land = Match.getMatches(units, Matches.UnitIsLand);
      iter = land.iterator();
      while(iter.hasNext())
      {
        Unit unit = (Unit) iter.next();
        markNoMovement(unit);
      }
    }
  }

  /**
   * Marks transports and units involved in unloading with no movement left.
   */
  private void markTransportsMovement(Map transporting, Route route)
  {

    if(transporting == null)
      return;

    if(MoveValidator.isUnload(route))
    {

      Collection units = new ArrayList();
      units.addAll(transporting.values());
      units.addAll(transporting.keySet());
      Iterator iter = units.iterator();
      while(iter.hasNext())
      {
        Unit unit = (Unit) iter.next();
        markNoMovement(unit);
      }

      //unload the transports
      Iterator unitIter = transporting.keySet().iterator();
      while(unitIter.hasNext())
      {
        Unit load = (Unit) unitIter.next();
        m_transportTracker.unload(load);
      }
    }

    //load the transports
    if(MoveValidator.isLoad(route))
    {
      //mark transports as having transported
      Iterator units = transporting.keySet().iterator();
      while(units.hasNext())
      {

        Unit load = (Unit) units.next();
        Unit transport = (Unit) transporting.get(load);
        m_transportTracker.load(load, transport);
      }
    }
  }

  public void markNoMovement(Collection units)
  {
    Iterator iter = units.iterator();
    while(iter.hasNext())
    {
      Unit unit = (Unit) iter.next();
      markNoMovement(unit);
    }
  }

  private void markNoMovement(Unit unit)
  {
    UnitAttatchment ua = UnitAttatchment.get(unit.getType());
    m_alreadyMoved.put(unit, ua.getMovement(unit.getOwner()));
  }

  /**
   * Called before the delegate will stop running.
   */
  public void end()
  {
    if(m_nonCombat)
      removeAirThatCantLand();
    m_movesToUndo.clear();
  }


  /**
   * returns a map of unit -> transport.
   * returns null if no mapping can be done
   * either because there is not sufficient transport capacity
   * or because a unit is not with its transport
   */
  private Map mapTransports(Route route, Collection units)
  {
    if(MoveValidator.isLoad(route))
      return mapTransportsToLoad(units, route.getEnd().getUnits().getUnits());
    if(MoveValidator.isUnload(route))
      return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
    return mapTransportsAlreadyLoaded(units, units);
  }

  /**
   * Returns a map of unit -> transport.
   * Unit must already be loaded in the transport,
   * if the unit is in a transport not in transports then
   * null will be returned.
   */
  private Map mapTransportsAlreadyLoaded(Collection units, Collection transports)
  {


    Collection canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
    Collection canTransport = Match.getMatches(transports, Matches.UnitCanTransport);

    Map mapping = new HashMap();
    Iterator land = canBeTransported.iterator();
    while(land.hasNext())
    {
      Unit currentTransported = (Unit) land.next();
      Unit transport = m_transportTracker.transportedBy(currentTransported);
      //already being transported, make sure it is in transports
      if(transport == null)
        return null;

      if(! canTransport.contains(transport))
          return null;
      mapping.put(currentTransported, transport);
    }
    return mapping;
  }

  /**
   * Returns a map of unit -> transport.
   * Tries to find transports to load all units.
   * If it cant suceed returns null.
   *
   */
  private Map mapTransportsToLoad(Collection units, Collection transports)
  {
    Collection canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
    Collection canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
    Collection ownedTransport = Match.getMatches(transports, Matches.unitIsOwnedBy(m_player));
    canTransport = Util.difference(canTransport, ownedTransport);

    Map mapping = new HashMap();
    IntegerMap addedLoad = new IntegerMap();

    Iterator landIter = canBeTransported.iterator();
    while(landIter.hasNext())
    {
      Unit land = (Unit) landIter.next();
      UnitAttatchment landUA = UnitAttatchment.get(land.getType());
      int cost = landUA.getTransportCost();
      boolean loaded = false;

      //check owned first
      Iterator transportIter = ownedTransport.iterator();
      while(transportIter.hasNext() && !loaded)
      {
        Unit transport = (Unit) transportIter.next();
        int capacity = m_transportTracker.getAvailableCapacity(transport);
        capacity -= addedLoad.getInt(transport);
        if(capacity >= cost)
        {
          addedLoad.add(transport, cost);
          mapping.put(land, transport);
          loaded = true;
        }
      }
      //check allied
      transportIter = canTransport.iterator();
      while(transportIter.hasNext() && !loaded)
      {
        Unit transport = (Unit) transportIter.next();
        int capacity = m_transportTracker.getAvailableCapacity(transport);
        capacity -= addedLoad.getInt(transport);
        if(capacity >= cost)
        {
          addedLoad.add(transport, cost);
          mapping.put(land, transport);
          loaded = true;
        }
      }
      if(!loaded)
        return null;
    }
    return mapping;
  }



  public void sortAccordingToMovementLeft(List units, boolean ascending)
  {
    Collections.sort(units, ascending ? increasingMovement : decreasingMovement);
  }

  private Comparator decreasingMovement = new Comparator()
  {
    public int compare(Object o1, Object o2)
    {
      Unit u1 = (Unit) o1;
      Unit u2 = (Unit) o2;

      int left1 = MoveValidator.movementLeft(u1, m_alreadyMoved);
      int left2 = MoveValidator.movementLeft(u2, m_alreadyMoved);

      if(left1 == left2)
        return 0;
      if(left1 > left2)
        return 1;
      return -1;
    }
  };

  private Comparator increasingMovement = new Comparator()
  {
    public int compare(Object o1, Object o2)
    {
      //reverse the order, clever huh
      return decreasingMovement.compare(o2,o1);
    }
  };

  private Collection getTerritoriesWhereAirCantLand()
  {
    Collection cantLand = new ArrayList();
    Iterator territories = m_data.getMap().getTerritories().iterator();
    while(territories.hasNext())
    {
      Territory current = (Territory) territories.next();
      CompositeMatch ownedAir = new CompositeMatchAnd();
      ownedAir.add(Matches.UnitIsAir);
      ownedAir.add(Matches.alliedUnit(m_player, m_data));
      Collection air = current.getUnits().getMatches(ownedAir);
      if(air.size() != 0 && !MoveValidator.canLand(air, current, m_player, m_data))
      {
        cantLand.add(current);
      }
    }
    return cantLand;
  }

  private void removeAirThatCantLand()
  {
    Iterator territories = getTerritoriesWhereAirCantLand().iterator();
    while(territories.hasNext())
    {
      Territory current = (Territory) territories.next();
      CompositeMatch ownedAir = new CompositeMatchAnd();
      ownedAir.add(Matches.UnitIsAir);
      ownedAir.add(Matches.alliedUnit(m_player, m_data));
      Collection air = current.getUnits().getMatches(ownedAir);

      removeAirThatCantLand(current, air);
    }
  }

  private void removeAirThatCantLand(Territory territory, Collection airUnits)
  {
    Collection toRemove = new ArrayList(airUnits.size());
    //if we cant land on land then none can
    if(!territory.isWater())
    {
      toRemove.addAll(airUnits);
    }
    else //on water we may just no have enough carriers
    {
      //find the carrier capacity
      Collection carriers = territory.getUnits().getMatches(Matches.alliedUnit(m_player, m_data));
      int capacity = MoveValidator.carrierCapacity(carriers);

      Iterator iter = airUnits.iterator();
      while(iter.hasNext())
      {
        Unit unit = (Unit) iter.next();
        UnitAttatchment ua = UnitAttatchment.get(unit.getType());
        int cost = ua.getCarrierCost();
        if(cost == -1 || cost > capacity)
          toRemove.add(unit);
        else
          capacity -= cost;
      }
    }

    Change remove = ChangeFactory.removeUnits(territory, toRemove);

    m_bridge.addChange(remove);

    String transcriptText = Formatter.unitsToText(toRemove) + " could not land in " + territory.getName() + " and " + (toRemove.size() > 1 ? "were" : "was") +  " removed";
    m_bridge.getTranscript().write(transcriptText);
  }

  private IntegerMap getUnitTypeMap(Collection units)
  {
    IntegerMap map = new IntegerMap();
    Iterator iter = units.iterator();
    while(iter.hasNext())
    {
      Unit unit = (Unit) iter.next();
      map.add(unit.getType(), 1);
    }
    return map;
  }


  /**
   * Fire aa guns.  Returns units to remove.
   */
  private Collection fireAA(Route route, Collection units)
  {
    if(Match.noneMatch(units, Matches.UnitIsAir))
      return Collections.EMPTY_LIST;

    List targets = Match.getMatches(units, Matches.UnitIsAir);

    //select units with lowest movement first
    Collections.sort(targets, decreasingMovement);
    Collection originalTargets = new ArrayList(targets);

    //dont iteratate over the end
    //that will be a battle
    //and handled else where in this tangled mess
    CompositeMatch hasAA = new CompositeMatchAnd();
    hasAA.add(Matches.UnitIsAA);
    hasAA.add(Matches.enemyUnit(m_player, m_data));

    for(int i = 0; i  < route.getLength() -1 ; i++)
    {
      Territory current = route.at(i);

      //aa guns in transports shouldnt be able to fire
      if(current.getUnits().someMatch(hasAA) && !current.isWater())
      {
        fireAA(current, targets);
      }
    }

    //check start as well, prevent user from moving to and from aa sites one
    //at a time
    if( route.getStart().getUnits().someMatch(hasAA))
      fireAA(route.getStart(), targets);

    return Util.difference(originalTargets, targets);
  }

  /**
   * Fire the aa units in the given territory,
   * hits are removed from units
   */
  private void fireAA(Territory territory, Collection units)
  {
    //once we fire the aa guns, we cant undo
    //otherwise you could keep undoing and redoing
    //until you got the roll you wanted
    m_currentMove.setCantUndo("AA has fired.");
    DiceRoll dice = DiceRoll.rollAA(units.size(), m_bridge,
                                    m_player, territory.getOwner());
    int hitCount = dice.getHits();

    if(hitCount == 0)
    {
      m_bridge.sendMessage( new StringMessage("No aa hits in " + territory.getName()));
    }
    else
      selectCasualties(dice, units, territory);
  }

  /**
   * hits are removed from units.  Note that units are removed in the
   * order that the iterator will move through them.
   */
  private void selectCasualties(DiceRoll dice, Collection units, Territory territory)
  {
    String text = "Select " + dice.getHits() + " casualties from aa fire in " + territory.getName();

    Collection casualties = BattleCalculator.selectCasualties(m_player, units, m_bridge, text, m_data, dice);
    units.removeAll(casualties);
  }

  public TransportTracker getTransportTracker()
  {
    return m_transportTracker;
  }

  /**
   * Can the delegate be saved at the current time.
   * @arg message, a String[] of size 1, hack to pass an error message back.
   */
  public boolean canSave(String[] message)
  {
    return true;
  }

  public Serializable saveState()
  {
    return saveState(true);
  }

  /**
   * Returns the state of the Delegate.
   * We dont want to save the undoState if we are saving the state for an undo move
   * (we dont need it, it will just take up extra space).
   */
  Serializable saveState(boolean saveUndo)
  {
    MoveState state = new MoveState();
    state.m_firstRun= m_firstRun;
    state.m_nonCombat = m_nonCombat;
    state.m_transportTracker = m_transportTracker;
    state.m_alreadyMoved = m_alreadyMoved;
    if(saveUndo)
      state.m_movesToUndo = m_movesToUndo;
    return state;
  }

  /**
   * Loads the delegates state
   *
   */
  public void loadState(Serializable aState)
  {
    MoveState state = (MoveState) aState;
    m_firstRun= state.m_firstRun;
    m_nonCombat = state.m_nonCombat;
    m_transportTracker = state.m_transportTracker;
    m_alreadyMoved = state.m_alreadyMoved;
    //if the undo state wasnt saved, then dont load it
    //prevents overwriting undo state when we restore from an undo move
    if(state.m_movesToUndo != null)
      m_movesToUndo = state.m_movesToUndo;
  }
}

class MoveState implements Serializable
{
  public boolean m_firstRun = true;
  public boolean m_nonCombat;
  public TransportTracker m_transportTracker;
  public IntegerMap m_alreadyMoved;
  public List m_movesToUndo;
}


/**
 * Stores data about a move so that it can be undone.
 * Stores the serialized state of the move and battle delegates (just
 * as if they were saved), and a CompositeChange that represents all the changes that
 * were made during the move.
 *
 * Some moves (such as those following an aa fire) can't be undone.
 */

class UndoableMove implements Serializable
{
  byte[] m_data;  //the serialized state of the battle and move delegates
  private CompositeChange m_undoChange = new CompositeChange();
  private String m_reasonCantUndo;
  private String m_description;

  public boolean getcanUndo()
  {
      return m_reasonCantUndo == null;
  }

  public String getReasonCantUndo()
  {
    return m_reasonCantUndo;
  }

  public void setCantUndo(String reason)
  {
    m_reasonCantUndo = reason;
  }

  public void addChange(Change aChange)
  {
    m_undoChange.add(aChange);
  }

  public String getDescription()
  {
    return m_description;
  }

  public void setDescription(String description)
  {
    m_description = description;
  }

  public UndoableMove(GameData data)
  {
    try
    {
        //capture the save state of the move and save delegates
        GameObjectStreamFactory factory = new GameObjectStreamFactory(data);

        ByteArrayOutputStream sink = new ByteArrayOutputStream(2000);
        ObjectOutputStream out = factory.create(sink);

        out.writeObject(DelegateFinder.moveDelegate(data).saveState(false));
        out.writeObject(DelegateFinder.battleDelegate(data).saveState());
        out.flush();
        out.close();

        m_data = sink.toByteArray();
    }
    catch (IOException ex)
    {
      ex.printStackTrace();
      throw new IllegalStateException(ex.getMessage());
    }
  }

  public void undo(GameData data, DelegateBridge bridge)
  {

      try
      {
          GameObjectStreamFactory factory = new GameObjectStreamFactory(data);
          ObjectInputStream in = factory.create(new ByteArrayInputStream(
              m_data));
          MoveState moveState = (MoveState) in.readObject();
          BattleState battleState = (BattleState) in.readObject();

          DelegateFinder.moveDelegate(data).loadState(moveState);
          DelegateFinder.battleDelegate(data).loadState(battleState);

          //undo any changes to the game data
          bridge.addChange(m_undoChange.invert());

          bridge.getTranscript().write(bridge.getPlayerID().getName() +  " undoes his last move.");

      }
      catch (ClassNotFoundException ex)
      {
        ex.printStackTrace();
      }
      catch (IOException ex)
      {
        ex.printStackTrace();
      }

  }

}