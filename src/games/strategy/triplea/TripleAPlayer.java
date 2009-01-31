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

import games.strategy.common.player.AbstractHumanPlayer;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.BidPurchaseDelegate;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.BattleDisplay;
import games.strategy.triplea.ui.PlaceData;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import games.strategy.engine.data.GameData;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ButtonModel;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TripleAPlayer extends AbstractHumanPlayer<TripleAFrame> implements IGamePlayer, ITripleaPlayer
{
    /** Creates new TripleAPlayer */
    public TripleAPlayer(String name)
    {
        super(name);
    }

    public void reportError(String error)
    {
        m_ui.notifyError(error);
    }
    
    public void reportMessage(String message)
    {
        m_ui.notifyMessage(message);
    }
 
    public void start(String name)
    {
        boolean badStep = false;

        try 
        {
            m_ui.setEditDelegate((IEditDelegate)m_bridge.getRemote("edit"));
        }
        catch (Exception e)
        {
        }
        m_ui.getEditModeButtonModel().addActionListener(m_editModeAction);
        m_ui.getEditModeButtonModel().setEnabled(true);

        if (name.endsWith("Bid"))
            purchase(true);
        else if (name.endsWith("Tech"))
            tech();
        else if (name.endsWith("TechActivation"))
            {} // the delegate handles everything
        else if (name.endsWith("Purchase"))
            purchase(false);
        else if (name.endsWith("Move"))
            move(name.endsWith("NonCombatMove"));
        else if (name.endsWith("Battle"))
            battle();
        else if (name.endsWith("Place"))
            place(name.indexOf("Bid") != -1);
        else if (name.endsWith("EndTurn"))
            endTurn();
        else
            badStep = true;

        m_ui.getEditModeButtonModel().setEnabled(false);
        m_ui.getEditModeButtonModel().removeActionListener(m_editModeAction);
        m_ui.setEditDelegate(null);
        
        if (badStep)
            throw new IllegalArgumentException("Unrecognized step name:" + name);
    }
    
    private AbstractAction m_editModeAction = new AbstractAction()
    {
        public void actionPerformed(ActionEvent ae)
        {
            boolean editMode = ((ButtonModel)ae.getSource()).isSelected();
            try 
            {
                // Set edit mode
                // All GameDataChangeListeners will be notified upon success
                IEditDelegate editDelegate = (IEditDelegate) m_bridge.getRemote("edit");
                editDelegate.setEditMode(editMode);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                // toggle back to previous state since setEditMode failed
                m_ui.getEditModeButtonModel().setSelected(!m_ui.getEditModeButtonModel().isSelected());
            }
        }
    };

    private void tech()
    {
        //can we tech?
        m_bridge.getGameData().acquireReadLock();
        try
        {
            if (m_id.getResources().getQuantity(Constants.IPCS) == 0)
                return;
        }
        finally 
        {
            m_bridge.getGameData().releaseReadLock();
        }

        TechRoll techRoll = m_ui.getTechRolls(m_id);
        if (techRoll != null)
        {
            ITechDelegate techDelegate = (ITechDelegate) m_bridge.getRemote();
            TechResults techResults = techDelegate.rollTech(
                    techRoll.getRolls(), techRoll.getTech(), techRoll.getNewTokens());

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
            {
                if(!canAirLand(true))
                    move(nonCombat);
            }
            else
            {       	
            	if(canUnitsFight())
            		move(nonCombat);
            }
            return;
        }

        IMoveDelegate moveDel = (IMoveDelegate) m_bridge.getRemote();
        String error = moveDel.move(moveDescription.getUnits(), moveDescription.getRoute(), moveDescription.getTransportsThatCanBeLoaded());
        
        if (error != null )
            m_ui.notifyError(error);
        move(nonCombat);	
    }

    private boolean canAirLand(boolean movePhase)
    {
        
        Collection<Territory> airCantLand;
        if(movePhase)
            airCantLand = ((IMoveDelegate) m_bridge.getRemote()).getTerritoriesWhereAirCantLand();
        else
            airCantLand = ((IAbstractPlaceDelegate) m_bridge.getRemote()).getTerritoriesWhereAirCantLand();
        
        if (airCantLand.isEmpty())
            return true;
        else
        {
            StringBuilder buf = new StringBuilder(
                    "Air in following territories cant land:");
            Iterator<Territory> iter = airCantLand.iterator();
            while (iter.hasNext())
            {
                buf.append(((Territory) iter.next()).getName());
                buf.append(" ");
            }
            if (!m_ui.getOKToLetAirDie(m_id, buf.toString(), movePhase))
                return false;
            return true;
        }
    }
    
    private boolean canUnitsFight()
    {        
        Collection<Territory> unitsCantFight;
        
        unitsCantFight = ((IMoveDelegate) m_bridge.getRemote()).getTerritoriesWhereUnitsCantFight();
        
        
        if (unitsCantFight.isEmpty())
            return false;
        else
        {
            StringBuilder buf = new StringBuilder(
                    "Units in the following territories will die:");
            Iterator<Territory> iter = unitsCantFight.iterator();
            while (iter.hasNext())
            {
                buf.append(((Territory) iter.next()).getName());
                buf.append(" ");
            }
            if (m_ui.getOKToLetUnitsDie(m_id, buf.toString(), true))
                return false;
            return true;
        }
    }
    private boolean hasUnitsThatCanMove(boolean nonCom)
    {
        CompositeMatchAnd<Unit> moveableUnitOwnedByMe = new CompositeMatchAnd<Unit>();
        moveableUnitOwnedByMe.add(Matches.unitIsOwnedBy(m_id));
        //non com, can move aa units
        if(nonCom)
            moveableUnitOwnedByMe.add(new InverseMatch<Unit>(Matches.UnitIsFactory));
        else //combat move, cant move aa units
            moveableUnitOwnedByMe.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));

        m_bridge.getGameData().acquireReadLock();
        try
        {
            Iterator<Territory> territoryIter = m_bridge.getGameData().getMap()
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
        finally 
        {
            m_bridge.getGameData().releaseReadLock();
        }

    }

    private void purchase(boolean bid)
    {
        if (bid)
        {
            if(!BidPurchaseDelegate.doesPlayerHaveBid(m_bridge.getGameData(), m_id))
                return;
        }
        //we have no production frontier
        else if(m_id.getProductionFrontier() == null || m_id.getProductionFrontier().getRules().isEmpty())
        {
            return;
        }         
        else
        {
            //if my capital is captured, 
            //I can't produce
            //i may have ipcs if i capture someone else's
            //capital
            Territory capital = TerritoryAttachment.getCapital(m_id, m_bridge.getGameData());
            if(capital != null && !capital.getOwner().equals(m_id))
                return;
            
            int minIPCsNeededToBuild = Integer.MAX_VALUE;
            m_bridge.getGameData().acquireReadLock();
            try
            {
                Iterator<ProductionRule> prodRules = m_id.getProductionFrontier().getRules().iterator();
                while(prodRules.hasNext())
                {
                    ProductionRule rule = (ProductionRule) prodRules.next();
                    minIPCsNeededToBuild = Math.min(rule.getCosts().getInt(m_bridge.getGameData().getResourceList().getResource(Constants.IPCS)), minIPCsNeededToBuild);
                }
                //TODO COMCO added this       
                if(m_id.getRepairFrontier() != null)
                {
                    Iterator<RepairRule> repairRules = m_id.getRepairFrontier().getRules().iterator();
                    while(repairRules.hasNext())
                    {
                        RepairRule rule = (RepairRule) repairRules.next();
                        minIPCsNeededToBuild = Math.min(rule.getCosts().getInt(m_bridge.getGameData().getResourceList().getResource(Constants.IPCS)), minIPCsNeededToBuild);
                    }
                }
                
                //can we buy anything
                if (m_id.getResources().getQuantity(Constants.IPCS) < minIPCsNeededToBuild)
                    return;
            }
            finally
            {
                m_bridge.getGameData().releaseReadLock();
            }
        }
        //TODO COMCO add determination of damaged factories here
      //Check if any factories need to be repaired
        String error = null;
		IPurchaseDelegate purchaseDel = (IPurchaseDelegate) m_bridge.getRemote();
		
        if(isSBRAffectsUnitProduction(m_bridge.getGameData()))
        {
        	GameData data = m_bridge.getGameData();
        	Collection<Territory> bombedTerrs = new ArrayList<Territory>();
        	for(Territory t : Match.getMatches(data.getMap().getTerritories(), Matches.territoryHasOwnedFactory(data, m_id))) 
        	{
        		TerritoryAttachment ta = TerritoryAttachment.get(t);
        		if(ta.getProduction() != ta.getUnitProduction())
        		{
        			bombedTerrs.add(t);
        		}
        	}
        	
        	if(bombedTerrs.size() > 0)
        	{
        		IntegerMap<RepairRule> repair = m_ui.getRepair(m_id, bid);
        		if (repair != null)
        		{
        			purchaseDel = (IPurchaseDelegate) m_bridge.getRemote();
        			error = purchaseDel.purchaseRepair(repair);
        			if (error != null)
        			{
        				m_ui.notifyError(error);
        				//dont give up, keep going
        				purchase(bid);

        			}
        		}
        	}
    	}
        
        IntegerMap<ProductionRule> prod = m_ui.getProduction(m_id, bid);
        if (prod == null)
            return;
        purchaseDel = (IPurchaseDelegate) m_bridge.getRemote();
        error = purchaseDel.purchase(prod);
        
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
                
                if(m_bridge.isGameOver())
                    return;
                
                String error = battleDel.fightBattle(details.getWhere(), details.isBombingRaid());

                if(error != null)
                        m_ui.notifyError(error);
        }
    }

    private void place(boolean bid)
    {
        IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) m_bridge.getRemote();
        //nothing to place
        //nothing placed
        if (m_id.getUnits().size() == 0 && placeDel.getPlacementsMade() == 0)
            return;
        
        while(true)
        {
            PlaceData data = m_ui.waitForPlace(m_id ,bid, m_bridge);
            if(data == null)
            {
                //this only happens in lhtr rules
                if(canAirLand(false))
                    return;
                else
                    continue;
            }
            
            String error = placeDel.placeUnits(data.getUnits(), data.getAt());
            if(error != null)
                m_ui.notifyError(error);
        }
    }

    private void endTurn()
    {
        m_ui.waitForEndTurn(m_id, m_bridge);
    }
    

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectCasualties(java.lang.String, java.util.Collection, java.util.Map, int, java.lang.String, games.strategy.triplea.delegate.DiceRoll, games.strategy.engine.data.PlayerID, java.util.List)
     */

    public CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID)
    {
        return m_ui.getBattlePanel().getCasualties(selectFrom, dependents, count, message, dice,hit, defaultCasualties, battleID);
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectFixedDice(int, int, boolean, java.lang.String)
     */
    public int[] selectFixedDice(int numDice, int hitAt, boolean hitOnlyIfEquals, String title)
    {
        return m_ui.selectFixedDice(numDice, hitAt, hitOnlyIfEquals, title);
    }

    /* 
     * @see games.strategy.triplea.player.ITripleaPlayer#selectBombardingTerritory(games.strategy.engine.data.Unit, games.strategy.engine.data.Territory, java.util.Collection, boolean)
     */
    public Territory selectBombardingTerritory(Unit unit, Territory unitTerritory, Collection<Territory>  territories, boolean noneAvailable)
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
    
    public Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from)
    {
        return m_ui.getRocketAttack(candidates, from);
     }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
     */
    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(Collection<Unit> fightersThatCanBeMoved, Territory from)
    {
        return m_ui.moveFightersToCarrier(fightersThatCanBeMoved, from);
    }

    /*
     * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
     */
    public Territory selectTerritoryForAirToLand(Collection<Territory> candidates)
    {
        return m_ui.selectTerritoryForAirToLand(candidates);
    }


    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#confirmMoveInFaceOfAA(java.util.Collection)
     */
    public boolean confirmMoveInFaceOfAA(Collection<Territory> aaFiringTerritories)
    {
        String question = "AA guns will fire in " + MyFormatter.territoriesToText(aaFiringTerritories, "and") + ", do you still want to move?";
        return m_ui.getOK(question);
        
    }

    public boolean confirmMoveKamikaze()
    {
        String question = "Not all air units in destination territory can land, do you still want to move?";
        return m_ui.getOK(question);
    }

    public boolean confirmMoveHariKari()
    {
        String question = "All units in destination territory will automatically die, do you still want to move?";
        return m_ui.getOK(question);
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#retreatQuery(games.strategy.net.GUID, boolean, java.util.Collection, java.lang.String, java.lang.String)
     */
    public Territory retreatQuery(GUID battleID, boolean submerge, Collection<Territory> possibleTerritories, String message)
    {
        return m_ui.getBattlePanel().getRetreat(battleID, message, possibleTerritories,submerge);
    }
    
    public void confirmEnemyCasualties(GUID battleId, String message, PlayerID hitPlayer)
    {
        //no need, we have already confirmed since we are firing player
        if(m_ui.playing(hitPlayer))
            return;
        //we dont want to confirm enemy casualties
        if(!BattleDisplay.getShowEnemyCasualtyNotification())
            return;
        
        m_ui.getBattlePanel().confirmCasualties(battleId, message);
    }

    public void confirmOwnCasualties(GUID battleId, String message)
    {
        m_ui.getBattlePanel().confirmCasualties(battleId, message);
    }   

    public final boolean isSBRAffectsUnitProduction(GameData data)
    {
        return games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data);
    }
}








