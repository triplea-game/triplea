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
 * TechActivationDelegate.java
 *
 * Created on December 7, 2004, 9:55 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;
import java.util.*;

/**
 * Logic for activating tech rolls. This delegate requires the
 * TechnologyDelegate to run correctly.
 * 
 * @author Ali Ibrahim
 * @version 1.0
 */
public class TechActivationDelegate implements IDelegate
{

    private String m_name;
    private String m_displayName;
    private GameData m_data;
    private IDelegateBridge m_bridge;
    private PlayerID m_player;

    /** Creates new TechActivationDelegate */
    public TechActivationDelegate()
    {
    }

    public void initialize(String name, String displayName)
    {
        m_name = name;
        m_displayName = displayName;
    }

    /**
     * Called before the delegate will run. In this class, this does all the
     * work.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {
        m_bridge = aBridge;
        m_data = gameData;
        m_player = aBridge.getPlayerID();

        // Activate techs
        Map<PlayerID, Collection> techMap = DelegateFinder.techDelegate(m_data).getAdvances();
        Collection advances = techMap.get(m_player);
        if ((advances != null) && (advances.size() > 0))
        {
            // Start event
            m_bridge.getHistoryWriter().startEvent(m_player.getName() + " activating " + advancesAsString(advances));

            Iterator techsIter = advances.iterator();
            while (techsIter.hasNext())
            {
                TechAdvance advance = (TechAdvance) techsIter.next();
                advance.perform(m_bridge.getPlayerID(), m_bridge, m_data);
                TechTracker.addAdvance(m_player, m_data, m_bridge, advance);
            }
        }
        //empty
        techMap.put(m_player, null);
        
    }

    // Return string representing all advances in collection
    private String advancesAsString(Collection advances)
    {
        Iterator iter = advances.iterator();
        int count = advances.size();
        StringBuilder text = new StringBuilder();

        while (iter.hasNext())
        {
            TechAdvance advance = (TechAdvance) iter.next();
            text.append(advance.getName());
            count--;
            if (count > 1)
                text.append(", ");
            if (count == 1)
                text.append(" and ");
        }
        return text.toString();
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
     * Returns the state of the Delegate.
     */
    public Serializable saveState()
    {
        return null;
    }

    /**
     * Loads the delegates state
     */
    public void loadState(Serializable state)
    {

    }

    /*
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<? extends IRemote> getRemoteType()
    {
        return null;
    }

}