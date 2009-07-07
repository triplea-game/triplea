/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public Licensec
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * EndTurnDelegate.java
 *
 * Created on November 2, 2001, 12:30 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.remote.IAbstractEndTurnDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * At the end of the turn collect income.
 */
public abstract class AbstractEndTurnDelegate
    implements IDelegate, IAbstractEndTurnDelegate
{
    private IDelegateBridge m_bridge = null;
    private String m_name;
    private String m_displayName;
    protected GameData m_data;

    //we only want to notify once that the game is over
    private boolean m_needToInitialize = true;
    private boolean m_hasPostedTurnSummary = false;
    protected boolean m_gameOver = false;

    public void initialize(String name, String displayName)
    {
        m_name = name;
        m_displayName = displayName;
    }

    private boolean doBattleShipsRepair()
    {
        return m_data.getProperties().get(Constants.TWO_HIT_BATTLESHIPS_REPAIR_EACH_TURN, false);
    }


    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {
        m_bridge = aBridge;
        m_data = gameData;
        if(!m_needToInitialize)
            return;
        m_hasPostedTurnSummary = false;
        PlayerID player = aBridge.getPlayerID();

        //cant collect unless you own your own capital
        Territory capital = TerritoryAttachment.getCapital(player, m_data);
        if(!capital.getOwner().equals(player))
            return;

        Resource ipcs = gameData.getResourceList().getResource(Constants.IPCS);
        //just collect resources
        Collection<Territory> territories = gameData.getMap().getTerritoriesOwnedBy(player);

        int toAdd = getProduction(territories);
        //Add the War Bond dialog
        if(isWarBonds(player))
        {
            int[] randomRoll;
            String annotation = player + " Roll to resolve War Bonds:";
            
            randomRoll = aBridge.getRandom(Constants.MAX_DICE, 1, annotation);

            m_bridge.getHistoryWriter().startEvent("Roll to resolve War Bonds:" + MyFormatter.asDice(randomRoll));
            
            toAdd += randomRoll[0]+1;
        }
        
        int total = player.getResources().getQuantity(ipcs) + toAdd;
        String transcriptText = player.getName() + " collect " + toAdd + MyFormatter.pluralize(" ipc", toAdd)+"; end with " + total+ MyFormatter.pluralize(" ipc", total) + " total";
        aBridge.getHistoryWriter().startEvent(transcriptText);

        Change change = ChangeFactory.changeResourcesChange(player, ipcs, toAdd);
        aBridge.addChange(change);


        PlayerAttachment pa = PlayerAttachment.get(player);
        if(m_data.getProperties().get(Constants.PACIFIC_EDITION, false) && pa != null)
        {      
            Change changeVP = (ChangeFactory.attachmentPropertyChange(pa, (new Integer(Integer.parseInt(pa.getVps()) + (toAdd / 10 + Integer.parseInt(pa.getCaptureVps()) / 10))).toString(), "vps"));
            Change changeCapVP = ChangeFactory.attachmentPropertyChange(pa, "0", "captureVps");
            CompositeChange ccVP = new CompositeChange(changeVP, changeCapVP);
            aBridge.addChange(ccVP);
        }

        checkForWinner(aBridge);

        if(doBattleShipsRepair())
        {
            repairBattleShips(aBridge);
        }
        m_needToInitialize = false;

        if(pa != null && pa.getGiveUnitControl())
        {
        	changeUnitOwnership(aBridge, gameData);
        }
    }

    private void repairBattleShips(IDelegateBridge aBridge)
    {
       Match<Unit> damagedBattleship = new CompositeMatchAnd<Unit>(Matches.UnitIsTwoHit, Matches.UnitIsDamaged);
        
       Collection<Unit> damaged = new ArrayList<Unit>();
       Iterator iter = m_data.getMap().getTerritories().iterator();
       while(iter.hasNext())
       {
           Territory current = (Territory) iter.next();
           damaged.addAll(current.getUnits().getMatches(damagedBattleship));
       }

       if(damaged.size() == 0)
           return;
       
       IntegerMap<Unit> hits = new IntegerMap<Unit>();
       iter = damaged.iterator();
       while(iter.hasNext())
       {
           Unit unit = (Unit) iter.next();
           hits.put(unit,0);
       }
       aBridge.addChange(ChangeFactory.unitsHit(hits));
       aBridge.getHistoryWriter().startEvent(damaged.size() + " " +  MyFormatter.pluralize("unit", damaged.size()) + " repaired.");

    }


	private void changeUnitOwnership(IDelegateBridge aBridge, GameData gameData)
	{
		PlayerID Player = aBridge.getPlayerID();
		PlayerID newOwner = null;
		
		//get the list of players
		Collection<PlayerID> players = gameData.getPlayerList().getPlayers();
		Iterator<PlayerID> playerIter = players.iterator();
		
		//Find the player who will take control of the units
		while (playerIter.hasNext())
		{
			PlayerID currPlayer = (PlayerID) playerIter.next();
			 PlayerAttachment pa = PlayerAttachment.get(currPlayer);
			if (pa != null && pa.getTakeUnitControl())
			{
				newOwner = currPlayer;
				break;
			}
		}
		
		Collection<Territory> territories = gameData.getMap().getTerritories();
		Iterator<Territory> terrIter = territories.iterator();
		while (terrIter.hasNext())
		{
			Territory currTerritory = (Territory) terrIter.next();
            TerritoryAttachment ta = (TerritoryAttachment) currTerritory.getAttachment(Constants.TERRITORY_ATTATCHMENT_NAME);
            //if ownership should change in this territory
            if(ta != null && ta.getChangeUnitOwners())
            {
            	//PlayerOwnerChange
            	Collection<Unit> units = currTerritory.getUnits().getMatches(Matches.unitOwnedBy(Player));
            	Change changeOwner = ChangeFactory.changeOwner(units, newOwner, currTerritory);
            	aBridge.getHistoryWriter().addChildToEvent(changeOwner.toString());
            	aBridge.addChange(changeOwner);
            }
		}
	}

    protected abstract void checkForWinner(IDelegateBridge bridge);


    protected int getProduction(Collection<Territory> territories)
    {
        int value = 0;
        Iterator<Territory> iter = territories.iterator();
        while(iter.hasNext() )
        {
            Territory current = (Territory) iter.next();
            TerritoryAttachment attatchment = (TerritoryAttachment) current.getAttachment(Constants.TERRITORY_ATTATCHMENT_NAME);

            if(attatchment == null)
                throw new IllegalStateException("No attachment for owned territory:" + current.getName());
            // Check if territory is convoy	
            if(current.isWater() && DelegateFinder.battleDelegate(m_data).getOriginalOwnerTracker().getOriginalOwner(current).equals(current.getOwner()) || !current.isWater())
                value += attatchment.getProduction();
        }
        return value;
    }

    private boolean isWarBonds(PlayerID player)
    {
        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
        if(ta != null)
            return ta.hasWarBonds();
        
        return false;
    }

    public void setHasPostedTurnSummary(boolean hasPostedTurnSummary)
    {
        m_hasPostedTurnSummary = hasPostedTurnSummary;
    }

    public boolean getHasPostedTurnSummary()
    {
        return m_hasPostedTurnSummary;
    }

    public boolean postTurnSummary(PBEMMessagePoster poster)
    {
        m_hasPostedTurnSummary = poster.post(m_bridge.getHistoryWriter());
        return m_hasPostedTurnSummary;
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
     * Called before the delegate will stop running.
     */
    public void end()
    {
        m_needToInitialize = true;
        DelegateFinder.battleDelegate(m_data).getBattleTracker().clear();
    }

    /* 
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<? extends IRemote> getRemoteType()
    {
        return IAbstractEndTurnDelegate.class;
    }
    
    /**
     * Returns the state of the Delegate.
     */
    public Serializable saveState()
    {
        EndTurnState state = new EndTurnState();
        state.m_needToInitialize = m_needToInitialize;
        state.m_hasPostedTurnSummary = m_hasPostedTurnSummary;
        return state;
    }
    
    /**
     * Loads the delegates state
     */
    public void loadState(Serializable aState)
    {
        if(aState != null)
        {
            EndTurnState state = (EndTurnState)aState;
            m_needToInitialize = state.m_needToInitialize;
            m_hasPostedTurnSummary = state.m_hasPostedTurnSummary;
        }
    }
}

class EndTurnState
    implements Serializable
{

    EndTurnState()
    {
    }

    public boolean m_needToInitialize;
    public boolean m_hasPostedTurnSummary;
}
