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
 * TripleAPlayer.java
 *
 * Created on November 2, 2001, 8:45 PM
 */

package games.strategy.triplea;

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.*;
import games.strategy.triplea.delegate.dataObjects.*;
import games.strategy.triplea.delegate.remote.*;
import games.strategy.triplea.formatter.Formatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.*;
import games.strategy.util.*;

import java.util.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TripleAPlayer implements IGamePlayer, ITripleaPlayer
{
    private final String m_name;

    private PlayerID m_id;

    private TripleAFrame m_ui;

    private IPlayerBridge m_bridge;

    /** Creates new TripleAPlayer */
    public TripleAPlayer(String name)
    {
        m_name = name;
    }

    public void setFrame(TripleAFrame frame)
    {
        m_ui = frame;
    }

    public String getName()
    {
        return m_name;
    }

    public void reportError(String error)
    {
        m_ui.notifyError(error);
    }

    
    public void reportMessage(String message)
    {
        m_ui.notifyMessage(message);
    }
    
    public PlayerID getID()
    {
        return m_id;
    }

    public void initialize(IPlayerBridge bridge, PlayerID id)
    {
        m_bridge = bridge;
        m_id = id;
    }

    public void start(String name)
    {
        if (name.endsWith("Bid"))
            purchase(true);
        else if (name.endsWith("Tech"))
            tech();
        else if (name.endsWith("TechActivation"))
            ; // the delegate handles everything
        else if (name.endsWith("Purchase"))
            purchase(false);
        else if (name.endsWith("Move"))
            move(name.endsWith("NonCombatMove"));
        else if (name.endsWith("Battle"))
            battle();
        else if (name.endsWith("Place"))
            place(name.indexOf("Bid") != -1);
        else if (name.endsWith("EndTurn"))
            ;//intentionally blank
        else
            throw new IllegalArgumentException("Unrecognized step name:" + name);

    }

    private void tech()
    {
        //can we tech?
        if (m_id.getResources().getQuantity(Constants.IPCS) == 0)
            return;

        TechRoll techRoll = m_ui.getTechRolls(m_id);
        if (techRoll != null)
        {
            ITechDelegate techDelegate = (ITechDelegate) m_bridge.getRemote();
            TechResults techResults = techDelegate.rollTech(
                    techRoll.getRolls(), techRoll.getTech());

            if (techResults.isError())
            {
                m_ui.notifyError(techResults.getErrorString());
                tech();
            } else
                m_ui.notifyTechResults(techResults);
        }
    }

    private void move(boolean nonCombat)
    {
        if (!hasUnitsThatCanMove(nonCombat))
            return;

        MoveDescription moveDescription = m_ui.getMove(m_id, m_bridge, nonCombat);
        if (moveDescription == null)
        {
            if (nonCombat)
                ensureAirCanLand();
            return;
        }

        IMoveDelegate moveDel = (IMoveDelegate) m_bridge.getRemote();
        String error = moveDel.move(moveDescription.getUnits(), moveDescription.getRoute(), moveDescription.getTransportsThatCanBeLoaded());
        
        if (error != null )
            m_ui.notifyError(error);
        move(nonCombat);	
    }

    private void ensureAirCanLand()
    {
        Collection airCantLand = ((IMoveDelegate) m_bridge.getRemote()).getTerritoriesWhereAirCantLand();
        
        if (airCantLand.isEmpty())
            return;
        else
        {
            StringBuffer buf = new StringBuffer(
                    "Air in following territories cant land:");
            Iterator iter = airCantLand.iterator();
            while (iter.hasNext())
            {
                buf.append(((Territory) iter.next()).getName());
                buf.append(" ");
            }
            if (!m_ui.getOKToLetAirDie(buf.toString()))
                move(true);
        }
    }

    private boolean hasUnitsThatCanMove(boolean nonCom)
    {
        CompositeMatchAnd moveableUnitOwnedByMe = new CompositeMatchAnd();
        moveableUnitOwnedByMe.add(Matches.unitIsOwnedBy(m_id));
        //non com, can move aa units
        if(nonCom)
            moveableUnitOwnedByMe.add(new InverseMatch(Matches.UnitIsFactory));
        else //combat move, cant move aa units
            moveableUnitOwnedByMe.add(new InverseMatch(Matches.UnitIsAAOrFactory));
        
        Iterator territoryIter = m_bridge.getGameData().getMap()
                .getTerritories().iterator();
        while (territoryIter.hasNext())
        {
            Territory item = (Territory) territoryIter.next();
            if (item.getUnits().someMatch(moveableUnitOwnedByMe))
            {
                return true;
            }
        }
        return false;

    }

    private void purchase(boolean bid)
    {
        if (bid)
        {
            String propertyName = m_id.getName() + " bid";
            if (Integer.parseInt(m_bridge.getGameData().getProperties().get(
                    propertyName).toString()) == 0)
                return;
        } else
        {
            int minIPCsNeededToBuild = Integer.MAX_VALUE;
            Iterator prodRules = m_id.getProductionFrontier().getRules().iterator();
            while(prodRules.hasNext())
            {
                ProductionRule rule = (ProductionRule) prodRules.next();
                minIPCsNeededToBuild = Math.min(rule.getCosts().getInt(m_bridge.getGameData().getResourceList().getResource(Constants.IPCS)), minIPCsNeededToBuild);
            }
            
            //can we buy anything
            if (m_id.getResources().getQuantity(Constants.IPCS) < minIPCsNeededToBuild)
                return;
        }

        IntegerMap prod = m_ui.getProduction(m_id, bid);
        if (prod == null)
            return;
        IPurchaseDelegate purchaseDel = (IPurchaseDelegate) m_bridge.getRemote();
        String error = purchaseDel.purchase(prod);
        
        if (error != null)
        {
            m_ui.notifyError(error);
            //dont give up, keep going
            purchase(bid);

        }
        return;
    }

    private void battle()
    {
        while (true)
        {
            IBattleDelegate battleDel = (IBattleDelegate) m_bridge.getRemote();
            BattleListing battles = battleDel.getBattles();
           
                
                if (battles.isEmpty())
                {
                    return;
                }
                
                FightBattleDetails details = m_ui.getBattle(m_id, battles
                        .getBattles(), battles.getStrategicRaids());
                String error = battleDel.fightBattle(details.getWhere(), details.isBombingRaid());

                if(error != null)
                        m_ui.notifyError(error);
        }
    }

    private void place(boolean bid)
    {
        //nothing to place
        if (m_id.getUnits().size() == 0)
            return;
        
        while(true)
        {
            PlaceData data = m_ui.waitForPlace(m_id ,bid, m_bridge);
            if(data == null)
                return;
            IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) m_bridge.getRemote();
            String error = placeDel.placeUnits(data.getUnits(), data.getAt());
            if(error != null)
                m_ui.notifyError(error);
        }
    }
    

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectCasualties(java.lang.String, java.util.Collection, java.util.Map, int, java.lang.String, games.strategy.triplea.delegate.DiceRoll, games.strategy.engine.data.PlayerID, java.util.List)
     */
    public CasualtyDetails selectCasualties(String step, Collection selectFrom, Map dependents, int count, String message, DiceRoll dice, PlayerID hit, List defaultCasualties)
    {
        return m_ui.getBattlePanel().getCasualties(step, selectFrom, dependents, count, message, dice,hit, defaultCasualties);
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectBombardingTerritory(games.strategy.engine.data.Unit, games.strategy.engine.data.Territory, java.util.Collection, boolean)
     */
    public Territory selectBombardingTerritory(Unit unit, Territory unitTerritory, Collection territories, boolean noneAvailable)
    {
        return m_ui.getBattlePanel().getBombardment(unit, unitTerritory,  territories, noneAvailable);
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
     */
    public boolean shouldBomberBomb(Territory territory)
    {
        return m_ui.getStrategicBombingRaid(territory);
       
    } 
    
    public Territory whereShouldRocketsAttach(Collection candidates, Territory from)
    {
        return m_ui.getRocketAttack(candidates, from);
     }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
     */
    public Collection getNumberOfFightersToMoveToNewCarrier(Collection fightersThatCanBeMoved, Territory from)
    {
        return m_ui.moveFightersToCarrier(fightersThatCanBeMoved, from);
    }

    /*
     * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
     */
    public Territory selectTerritoryForAirToLand(Collection candidates)
    {
        return m_ui.selectTerritoryForAirToLand(candidates);
    }


    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmMoveInFaceOfAA(java.util.Collection)
     */
    public boolean confirmMoveInFaceOfAA(Collection aaFiringTerritories)
    {
        String question = "AA guns will fire in " + Formatter.territoriesToText(aaFiringTerritories, "and") + ", do you still want to move?";
        return m_ui.getOK(question);
        
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#retreatQuery(games.strategy.net.GUID, boolean, java.util.Collection, java.lang.String, java.lang.String)
     */
    public Territory retreatQuery(GUID battleID, boolean submerge, Collection possibleTerritories, String message, String step)
    {
        return m_ui.getBattlePanel().getRetreat(battleID, step, message, possibleTerritories,submerge);
    }
    
    public void confirmEnemyCasualties(GUID battleId, String message, String step, PlayerID hitPlayer)
    {
        //no need, we have already confirmed since we are firing player
        if(m_ui.playing(hitPlayer))
            return;
        m_ui.getBattlePanel().confirmCasualties(battleId, message);
    }

    
    
}








