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
 * TechnolgoyDelegate.java
 *
 *
 * Created on November 25, 2001, 4:16 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.TechPanel;
import games.strategy.util.Util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.Serializable;
import java.util.*;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Logic for dealing with player tech rolls. This class requires the
 * TechActivationDelegate which actually activates the tech.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TechnologyDelegate implements IDelegate, ITechDelegate
{

    private String m_name;

    private String m_displayName;
    
    private int m_techCost;
    
    private GameData m_data;

    private IDelegateBridge m_bridge;

    private PlayerID m_player;

    private HashMap<PlayerID, Collection> m_techs;

    private TechnologyFrontier m_techCategory;
    
    /** Creates new TechnolgoyDelegate */
    public TechnologyDelegate()
    {
    }

    public void initialize(String name, String displayName)
    {
        m_name = name;
        m_displayName = displayName;
        m_techs = new HashMap<PlayerID, Collection>();
        m_techCost = -1;
    }

    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {
    	m_bridge = new TripleADelegateBridge(aBridge, gameData);
    	m_data = gameData;
    	m_player = aBridge.getPlayerID();
    	if(games.strategy.triplea.Properties.getTriggers(m_data)){
    		TriggerAttachment.triggerAvailableTechChange(m_player, m_bridge, m_data);
    	}
    }

    public Map<PlayerID, Collection> getAdvances()
    {
        return m_techs;
    }

    private boolean isWW2V2()
    {
        return games.strategy.triplea.Properties.getWW2V2(m_data);
    }

    private boolean isWW2V3TechModel()
    {
        return games.strategy.triplea.Properties.getWW2V3TechModel(m_data);
    }

    private boolean isSelectableTechRoll()
    {
        return games.strategy.triplea.Properties.getSelectableTechRoll(m_data);
    }

    private boolean isLL_TECH_ONLY()
    {
        return games.strategy.triplea.Properties.getLL_TECH_ONLY(m_data);
    }
 
    public TechResults rollTech(int techRolls, TechnologyFrontier techToRollFor, int newTokens)
    {
        int rollCount = techRolls;
        
        if(isWW2V3TechModel())
            rollCount = newTokens;
        
        boolean canPay = checkEnoughMoney(rollCount);
        if (!canPay)
            return new TechResults("Not enough money to pay for that many tech rolls.");

        chargeForTechRolls(rollCount);        
        int m_currTokens = 0;
        
        if(isWW2V3TechModel())
            m_currTokens = m_player.getResources().getQuantity(Constants.TECH_TOKENS);
        
        if (getAvailableTechs().isEmpty())
        {
            if(isWW2V3TechModel())
            {
                Resource techTokens = m_data.getResourceList().getResource(Constants.TECH_TOKENS);
                String transcriptText = m_player.getName() + " No more available tech advances.";

                m_bridge.getHistoryWriter().startEvent(transcriptText);

                Change removeTokens = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), techTokens, -m_currTokens);
                m_bridge.addChange(removeTokens);
            }
            return new TechResults("No more available tech advances.");
        }
        
        
        
        String annotation = m_player.getName() + " rolling for tech.";
        int[] random;
        int techHits = 0;
        int remainder = 0;
        if (EditDelegate.getEditMode(m_data))
        {
            ITripleaPlayer tripleaPlayer = (ITripleaPlayer) m_bridge.getRemote();
            random = tripleaPlayer.selectFixedDice(techRolls, m_data.getDiceSides(), true, annotation, m_data.getDiceSides());
            techHits = getTechHits(random);
        }
        else if (isLL_TECH_ONLY())
        {
        	techHits = techRolls / m_data.getDiceSides();
        	remainder = techRolls % m_data.getDiceSides();
        	if (remainder > 0)
        	{
        		random = m_bridge.getRandom(m_data.getDiceSides(), 1, annotation);
        		if (random[0] + 1 <= remainder)
            		techHits++;
        	}
        	else
        	{
        		random = m_bridge.getRandom(m_data.getDiceSides(), 1, annotation);
        		remainder = m_data.getDiceSides();
        	}
        }
        else
        {
        	random = m_bridge.getRandom(m_data.getDiceSides(), techRolls, annotation);
        	techHits = getTechHits(random);
        }

        boolean selectableTech = isSelectableTechRoll() || isWW2V2();
        String directedTechInfo = selectableTech ? " for "
                + techToRollFor.getTechs().get(0) : "";
        m_bridge.getHistoryWriter().startEvent(
                m_player.getName()
                        + (random.hashCode() > 0 ? " roll " : " rolls : ")
                        + MyFormatter.asDice(random) + directedTechInfo
                        + " and gets " + techHits + " "
                        + MyFormatter.pluralize("hit", techHits));
        
        if(techHits > 0 && isWW2V3TechModel())
        {
            m_techCategory = techToRollFor;
            //remove all the tokens            
            Resource techTokens = m_data.getResourceList().getResource(Constants.TECH_TOKENS);
            String transcriptText = m_player.getName() + " removing all Technology Tokens after successful research.";

            m_bridge.getHistoryWriter().startEvent(transcriptText);

            Change removeTokens = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), techTokens, -m_currTokens);
            m_bridge.addChange(removeTokens);            
        }

        if (isLL_TECH_ONLY())
        	m_bridge.getHistoryWriter().setRenderingData(
                        new DiceRoll(random, techHits, remainder, false));
        else
        	m_bridge.getHistoryWriter().setRenderingData(
                new DiceRoll(random, techHits, m_data.getDiceSides() - 1, true));

        Collection<TechAdvance> advances;
        if (selectableTech)
        {
            if (techHits > 0)
                advances = Collections.singletonList(techToRollFor.getTechs().get(0));
            else
                advances = Collections.emptyList();
        } else
        {
            advances = getTechAdvances(techHits);
        }

        // Put in techs so they can be activated later.
        m_techs.put(m_player, advances);

        List<String> advancesAsString = new ArrayList<String>();

        Iterator<TechAdvance> iter = advances.iterator();
        int count = advances.size();

        StringBuilder text = new StringBuilder();
        while (iter.hasNext())
        {
            TechAdvance advance = iter.next();
            text.append(advance.getName());
            count--;

            advancesAsString.add(advance.getName());

            if (count > 1)
                text.append(", ");
            if (count == 1)
                text.append(" and ");
        }

        String transcriptText = m_player.getName() + " discover "
                + text.toString();
        if (advances.size() > 0)
            m_bridge.getHistoryWriter().startEvent(transcriptText);

        return new TechResults(random, remainder, techHits, advancesAsString,
                m_player);

    }
    
    private List<TechAdvance> getAvailableTechs()
    {
        m_data.acquireReadLock();
        try
        {
            Collection<TechAdvance> currentAdvances = TechTracker.getTechAdvances(m_player,m_data);
            Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(m_data,m_player);
            return Util.difference(allAdvances, currentAdvances);
        }
        finally 
        {
            m_data.releaseReadLock();
        }
    }

    boolean checkEnoughMoney(int rolls)
    {
        Resource PUs = m_data.getResourceList().getResource(Constants.PUS);
        int cost = rolls * getTechCost();
        int has = m_bridge.getPlayerID().getResources().getQuantity(PUs);
        return has >= cost;
    }
    

    private void chargeForTechRolls(int rolls)
    {
        Resource PUs = m_data.getResourceList().getResource(Constants.PUS);
        //TODO techCost
        int cost = rolls * getTechCost();

        String transcriptText = m_bridge.getPlayerID().getName() + " spend "
                + cost + " on tech rolls";
        m_bridge.getHistoryWriter().startEvent(transcriptText);

        Change charge = ChangeFactory.changeResourcesChange(m_bridge
                .getPlayerID(), PUs, -cost);
        m_bridge.addChange(charge);
        
        if(isWW2V3TechModel())
        {
            Resource tokens = m_data.getResourceList().getResource(Constants.TECH_TOKENS);
            Change newTokens = ChangeFactory.changeResourcesChange(m_bridge
                .getPlayerID(), tokens, rolls);
            m_bridge.addChange(newTokens);
        }
    }

    private int getTechHits(int[] random)
    {
        int count = 0;
        for (int i = 0; i < random.length; i++)
        {
            if (random[i] == m_data.getDiceSides() - 1)
                count++;
        }
        return count;
    }

    private Collection<TechAdvance> getTechAdvances(int hits)
    {
        List<TechAdvance> available = new ArrayList<TechAdvance>();
        if(hits > 0 && isWW2V3TechModel())
        {
            available = getAvailableAdvancesForCategory(m_techCategory);
            hits=1;
        } 
        else
        {
            available = getAvailableAdvances();
        }
        if (available.isEmpty())
            return Collections.emptyList();
        if (hits >= available.size())
            return available;
        if (hits == 0)
            return Collections.emptyList();

        Collection<TechAdvance> newAdvances = new ArrayList<TechAdvance>(hits);

        String annotation = m_player.getName() + " rolling to see what tech advances are aquired";
        int[] random;
        if (EditDelegate.getEditMode(m_data))
        {
            ITripleaPlayer tripleaPlayer = (ITripleaPlayer) m_bridge.getRemote();
            random = tripleaPlayer.selectFixedDice(hits, 0, true, annotation, m_data.getDiceSides());

        }
        else {
        	random = new int[hits];
        	List<Integer> rolled = new ArrayList<Integer>();
        	// generating discrete rolls. messy, can't think of a more elegant way
        	// hits guaranteed to be less than available at this point.
        	for(int i = 0; i<hits;i++){
        		int roll = m_bridge.getRandom(available.size()-i, annotation);
        		for(int r:rolled){
        			if( roll>= r)
        				roll++;
        		}
        		random[i] = roll;
        		rolled.add(roll);
        	}
        }
    	List<Integer> rolled = new ArrayList<Integer>();
        for (int i = 0; i < random.length; i++)
        {
            int index = random[i];
            // check in case of dice chooser.
            if( !rolled.contains(index) && index < available.size()) {
            	newAdvances.add(available.get(index));
            	rolled.add(index);
            }
        }
        m_bridge.getHistoryWriter().startEvent(
                "Rolls to resolve tech hits:" + MyFormatter.asDice(random));
        return newAdvances;
    }

    private List<TechAdvance> getAvailableAdvances()
    {
        //too many
        Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(m_data,m_bridge
                .getPlayerID());
        Collection<TechAdvance> playersAdvances = TechTracker.getTechAdvances(m_bridge
                .getPlayerID(),m_data);

        List<TechAdvance> available = Util.difference(allAdvances, playersAdvances);
        return available;
    }

    private List<TechAdvance> getAvailableAdvancesForCategory(TechnologyFrontier techCategory)
    {
        //Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(m_data, techCategory);
        Collection<TechAdvance> playersAdvances = TechTracker.getTechAdvances(m_bridge
                .getPlayerID(),m_data);

        List<TechAdvance> available = Util.difference(techCategory.getTechs(), playersAdvances);
        return available;
    }
    
    public String getName()
    {
        return m_name;
    }

    public String getDisplayName()
    {
        return m_displayName;
    }

    public int getTechCost()
    {
    	m_techCost = TechTracker.getTechCost(m_player);
        return m_techCost >0 ? m_techCost : Constants.TECH_ROLL_COST;        
    }
    
    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {
    }

    /**
     * Returns the state of the Delegate.
     */
    public Serializable saveState()
    {
        return m_techs;
    }

    /**
     * Loads the delegates state
     */
    @SuppressWarnings("unchecked")
    public void loadState(Serializable state)
    {
        m_techs = (HashMap<PlayerID, Collection>) state;
    }

    /*
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<ITechDelegate> getRemoteType()
    {
        return ITechDelegate.class;
    }

}
