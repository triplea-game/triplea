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
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.Util;

import java.io.Serializable;
import java.util.*;

/**
 * Logic for dealing with player tech rolls. This class requires the
 * TechActivationDelegate which actually activates the tech.
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class TechnologyDelegate implements ISaveableDelegate, ITechDelegate
{

    private String m_name;

    private String m_displayName;

    private GameData m_data;

    private IDelegateBridge m_bridge;

    private PlayerID m_player;

    private HashMap<PlayerID, Collection> m_techs;

    /** Creates new TechnolgoyDelegate */
    public TechnologyDelegate()
    {
    }

    public void initialize(String name, String displayName)
    {
        m_name = name;
        m_displayName = displayName;
        m_techs = new HashMap<PlayerID, Collection>();
    }

    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {
        m_bridge = aBridge;
        m_data = gameData;
        m_player = aBridge.getPlayerID();
    }

    public Map<PlayerID, Collection> getAdvances()
    {
        return m_techs;
    }

    private boolean isFourthEdition()
    {
        return m_data.getProperties().get(Constants.FOURTH_EDITION, false);
    }

    
    public TechResults rollTech(int techRolls, TechAdvance techToRollFor)
    {
        boolean canPay = checkEnoughMoney(techRolls);
        if (!canPay)
            return new TechResults("Not enough money to pay for that many tech rolls");

        chargeForTechRolls(techRolls);
        int[] random = m_bridge.getRandom(Constants.MAX_DICE, techRolls,
                m_player.getName() + " rolling for tech.");
        int techHits = getTechHits(random);

        String directedTechInfo = isFourthEdition() ? " for "
                + techToRollFor : "";
        m_bridge.getHistoryWriter().startEvent(
                m_player.getName()
                        + (random.hashCode() > 0 ? " roll " : " rolls : ")
                        + MyFormatter.asDice(random) + directedTechInfo
                        + " and gets " + techHits + " "
                        + MyFormatter.pluralize("hit", techHits));
        m_bridge.getHistoryWriter().setRenderingData(
                new DiceRoll(random, techHits, 5, true));

        Collection<TechAdvance> advances;
        if (isFourthEdition())
        {
            if (techHits > 0)
                advances = Collections.singletonList(techToRollFor);
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

        return new TechResults(random, techHits, advancesAsString,
                m_player);

    }

    boolean checkEnoughMoney(int rolls)
    {
        Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
        int cost = rolls * Constants.TECH_ROLL_COST;
        int has = m_bridge.getPlayerID().getResources().getQuantity(ipcs);
        return has >= cost;
    }

    private void chargeForTechRolls(int rolls)
    {
        Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
        int cost = rolls * Constants.TECH_ROLL_COST;

        String transcriptText = m_bridge.getPlayerID().getName() + " spends "
                + cost + " on tech rolls";
        m_bridge.getHistoryWriter().startEvent(transcriptText);

        Change charge = ChangeFactory.changeResourcesChange(m_bridge
                .getPlayerID(), ipcs, -cost);
        m_bridge.addChange(charge);
    }

    private int getTechHits(int[] random)
    {
        int count = 0;
        for (int i = 0; i < random.length; i++)
        {
            if (random[i] == Constants.MAX_DICE - 1)
                count++;
        }
        return count;
    }

    private Collection<TechAdvance> getTechAdvances(int hits)
    {
        List<TechAdvance> available = getAvailableAdvances();
        if (available.isEmpty())
            return Collections.emptyList();
        if (hits >= available.size())
            return available;
        if (hits == 0)
            return Collections.emptyList();

        Collection<TechAdvance> newAdvances = new ArrayList<TechAdvance>(hits);

        int random[] = m_bridge.getRandom(Constants.MAX_DICE, hits, m_player
                .getName()
                + " rolling to see what tech advances are aquired");
        m_bridge.getHistoryWriter().startEvent(
                "Rolls to resolve tech hits:" + MyFormatter.asDice(random));
        for (int i = 0; i < random.length; i++)
        {
            int index = random[i] % available.size();
            newAdvances.add(available.get(index));
            available.remove(index);
        }
        return newAdvances;
    }

    private List<TechAdvance> getAvailableAdvances()
    {
        //too many
        Collection<TechAdvance> allAdvances = TechAdvance.getTechAdvances(m_data);
        Collection<TechAdvance> playersAdvances = TechTracker.getTechAdvances(m_bridge
                .getPlayerID());

        List<TechAdvance> available = Util.difference(allAdvances, playersAdvances);
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
    
    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {
    }

    /**
     * Can the delegate be saved at the current time.
     * 
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
        return m_techs;
    }

    /**
     * Loads the delegates state
     */
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
