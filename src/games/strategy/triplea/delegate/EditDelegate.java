/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Step;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.remote.IEditDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;

import java.io.Serializable;
import java.util.Collection;

/**
 * 
 * Edit game state
 * 
 * @author Tony Clayton
 */
public class EditDelegate implements IDelegate, IEditDelegate
{
    private String m_name;
    private String m_displayName;
    private TripleADelegateBridge m_bridge;
    private GameData m_data;
    
    public static boolean getEditMode(GameData data)
    {
        Object editMode = data.getProperties().get(Constants.EDIT_MODE);
        if (editMode == null)
            return false;
        if (! (editMode instanceof Boolean) )
            return false;
        return ((Boolean)editMode).booleanValue();
    }

    private String checkPlayerID()
    {
        ITripleaPlayer remotePlayer = (ITripleaPlayer)m_bridge.getRemote();
        if (!m_bridge.getPlayerID().equals(remotePlayer.getID()))
            return "Edit actions can only be performed during players turn";
        return null;
    }

    private String checkEditMode()
    {
        String result = checkPlayerID();
        if (null != result)
            return result;
        if (!getEditMode(m_data))
            return "Edit mode is not enabled";
        return null;
    }

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
    public void start(IDelegateBridge aBridge, GameData gameData)
    {

        m_bridge = new TripleADelegateBridge(aBridge, gameData);
        m_data = gameData;
    }

    /**
     * This will never be called since this is an IPersistentDelegate
     */
    public void end()
    {
    }

    public String getName()
    {

        return m_name;
    }

    public String getDisplayName()
    {

        return m_displayName;
    }

    public String setEditMode(boolean editMode)
    {
        ITripleaPlayer remotePlayer = (ITripleaPlayer)m_bridge.getRemote();
        if (!m_bridge.getPlayerID().equals(remotePlayer.getID()))
            return "Edit Mode can only be toggled during players turn";

        logEvent("Turning " + (editMode ? "on" : "off") + " Edit Mode", null);
        m_bridge.addChange(ChangeFactory.setProperty(Constants.EDIT_MODE, new Boolean(editMode), m_data));
        return null;
    }

    public boolean getEditMode()
    {
        return EditDelegate.getEditMode(m_data);
    }

    public String removeUnits(Territory territory, Collection<Unit> units)
    {
        String result = null;
        if (null != (result = checkEditMode())) 
            return result;

        if (null != (result = EditValidator.validateRemoveUnits(m_data, territory, units)))
            return result;

        logEvent("Removing units owned by "+m_bridge.getPlayerID().getName()+" from "+territory.getName()+": "+MyFormatter.unitsToTextNoOwner(units), units);
        m_bridge.addChange(ChangeFactory.removeUnits(territory, units));
        return null;
    }

    public String addUnits(Territory territory, Collection<Unit> units)
    {
        String result = null;
        if (null != (result = checkEditMode())) 
            return result;

        if (null != (result = EditValidator.validateAddUnits(m_data, territory, units)))
            return result;

        logEvent("Adding units owned by "+m_bridge.getPlayerID().getName()+" to "+territory.getName()+": "+MyFormatter.unitsToTextNoOwner(units), units);
        m_bridge.addChange(ChangeFactory.addUnits(territory, units));
        return null;
    }

    public String changeTerritoryOwner(Territory territory, PlayerID player)
    {
        String result = null;
        if (null != (result = checkEditMode())) 
            return result;

        // validate this edit
        if (null != (result = EditValidator.validateChangeTerritoryOwner(m_data, territory, player)))
            return result;
    
        logEvent("Changing ownership of "+territory.getName()+" from "+territory.getOwner().getName()+" to "+player.getName(), territory);

        if (m_data.getAllianceTracker().isAllied(territory.getOwner(), player))
        {
            // change ownership of friendly factories
            Collection<Unit> units = territory.getUnits().getMatches(Matches.UnitIsFactory);
            for (Unit unit : units)
                m_bridge.addChange(ChangeFactory.changeOwner(unit, player, territory));
        }
        else
        {
            CompositeMatch<Unit> enemyNonCom = new CompositeMatchAnd<Unit>();
            enemyNonCom.add(Matches.UnitIsAAOrFactory);
            enemyNonCom.add(Matches.enemyUnit(player, m_data));
            Collection<Unit> units = territory.getUnits().getMatches(enemyNonCom);
            // mark no movement for enemy units
            m_bridge.addChange(DelegateFinder.moveDelegate(m_data).markNoMovementChange(units));
            // change ownership of enemy AA and factories
            for (Unit unit : units)
                m_bridge.addChange(ChangeFactory.changeOwner(unit, player, territory));
        }
            
        // change ownership of territory
        m_bridge.addChange(ChangeFactory.changeOwner(territory, player));

        return null;
    }

    public String changeIPCs(PlayerID player, int newTotal)
    {
        String result = null;
        if (null != (result = checkEditMode())) 
            return result;

        Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
        int oldTotal = player.getResources().getQuantity(ipcs);

        if (oldTotal == newTotal)
            return "New ipcs total is unchanged";
        if (newTotal < 0)
            return "New ipcs total is invalid";

        logEvent("Changing ipcs for "+player.getName()+" from "+oldTotal+" to "+newTotal, null);
        m_bridge.addChange(ChangeFactory.changeResourcesChange(player, ipcs, (newTotal - oldTotal)));
       
        return null;
    }
//TODO COMCO use this to change player tech tokens
    public String changeTechTokens(PlayerID player, int newTotal)
    {
        String result = null;
        if (null != (result = checkEditMode())) 
            return result;

        Resource ipcs = m_data.getResourceList().getResource(Constants.IPCS);
        int oldTotal = player.getResources().getQuantity(ipcs);

        if (oldTotal == newTotal)
            return "New token total is unchanged";
        if (newTotal < 0)
            return "New token total is invalid";

        logEvent("Changing tech tokens for "+player.getName()+" from "+oldTotal+" to "+newTotal, null);
        m_bridge.addChange(ChangeFactory.changeResourcesChange(player, ipcs, (newTotal - oldTotal)));
       
        return null;
    }
    
    public String addComment(String message)
    {

        String result = null;
        if (null != (result = checkPlayerID())) 
            return result;

        logEvent("COMMENT: " + message, null);
        return null;
    }

    // We don't know the current context, so we need to figure 
    // out whether it makes more sense to log a new event or a child.
    // If any child events came before us, then we'll log a child event.
    // Otherwise, we'll log a new event.
    private void logEvent(String message, Object data)
    {
        // find last event node

        boolean foundChild = false;
        m_data.acquireReadLock();
        try 
        {
            HistoryNode curNode = m_data.getHistory().getLastNode();
            while (! (curNode instanceof Step) && ! (curNode instanceof Event))
            {
                if (curNode instanceof EventChild)
                {
                    foundChild = true;
                    break;
                }
                curNode = (HistoryNode)curNode.getPreviousNode();
            }
        }
        finally
        {
            m_data.releaseReadLock();
        }

        if (foundChild)
            m_bridge.getHistoryWriter().addChildToEvent(message, data);
        else
        {
            m_bridge.getHistoryWriter().startEvent(message);
            m_bridge.getHistoryWriter().setRenderingData(data);
        }

    }

    /*
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<? extends IRemote> getRemoteType()
    {
        return IEditDelegate.class;
    }

    public void loadState(Serializable state)
    {
    }

    public Serializable saveState()
    {
        return null;
    }
}
