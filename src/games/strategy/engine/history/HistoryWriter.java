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

package games.strategy.engine.history;

import java.util.logging.*;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;

/**
 * Used to write to a history object. Delegates should use a
 * DelegateHistoryWriter
 */
public class HistoryWriter implements java.io.Serializable
{
    private final History m_history;
    private HistoryNode m_current;
    private static final Logger s_logger = Logger.getLogger(HistoryWriter.class.getName());

    public HistoryWriter(History history)
    {
        m_history = history;
    }
    
    private void assertCorrectThread()
    {
        if(m_history.getGameData().areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
    }

    /**
     * Can only be called if we are currently in a round or a step
     */
    public void startNextStep(String stepName, String delegateName, PlayerID player, String stepDisplayName)
    {
        assertCorrectThread();
        
        s_logger.log(Level.FINE, "start step, stepName:" + stepName + " delegateName:" + delegateName + " player:" + player + " displayName:"
                + stepDisplayName);

        //we are being called for the first time
        if (m_current == null)
            startNextRound(1);

        if (isCurrentEvent())
            closeCurrent();

        //stop the current step
        if (isCurrentStep())
            closeCurrent();

        if (!isCurrentRound())
        {
            throw new IllegalStateException("Not in a round");
        }

        
        Step currentStep = new Step(stepName, delegateName, player, m_history.getChanges().size(), stepDisplayName);
        HistoryNode old = m_current;
        
        m_history.getGameData().acquireWriteLock();
        try
        {
            m_current.add(currentStep);
            m_current = currentStep;
        }
        finally
        {
            m_history.getGameData().releaseWriteLock();
        }
        
        
        m_history.nodeStructureChanged(old);
    }

    public void startNextRound(int round)
    {
        assertCorrectThread();
        
        s_logger.log(Level.FINE, "Starting round:" + round);

        if (isCurrentEvent())
            closeCurrent();
        if (isCurrentStep())
            closeCurrent();
        if (isCurrentRound())
            closeCurrent();

        Round currentRound = new Round(round, m_history.getChanges().size());

        m_history.getGameData().acquireWriteLock();
        try
        {
            ((HistoryNode) m_history.getRoot()).add(currentRound);
        
            m_current = currentRound;
        }
        finally
        {
            m_history.getGameData().releaseWriteLock();
        }            
        
        
        m_history.reload();
    }

    private void closeCurrent()
    {
        assertCorrectThread();
        
        HistoryNode old = m_current;
        m_history.getGameData().acquireWriteLock();
        try
        {        
            
            //remove steps where nothing happened
            if (isCurrentStep())
            {
                HistoryNode parent = (HistoryNode) m_current.getParent();
                if (m_current.getChildCount() == 0)
                {
                    int index = parent.getChildCount() - 1;
                    parent.remove(m_current);
                    m_history.nodesWereRemoved(parent, new int[]
                    { index }, new Object[]
                    { m_current });
                }
    
                m_current = parent;
                return;
            }
            m_current = (HistoryNode) m_current.getParent();
            ((IndexedHistoryNode) old).setChangeEndIndex(m_history.getChanges().size());
        }
        finally
        {
            m_history.getGameData().releaseWriteLock();
        }
            
        
       
        
    }

    public void startEvent(String eventName)
    {
        assertCorrectThread();
        
        s_logger.log(Level.FINE, "Starting event:" + eventName);

        //close the current event
        if (isCurrentEvent())
            closeCurrent();

        if (!isCurrentStep())
            throw new IllegalStateException("Cant add an event, not a step");

        Event event = new Event(eventName, m_history.getChanges().size());

        HistoryNode oldCurrent = m_current;
        
        m_history.getGameData().acquireWriteLock();
        try
        {
            m_current.add(event);
            m_current = event;
            
        }
        finally
        {
            m_history.getGameData().releaseWriteLock();
        }

        m_history.reload(oldCurrent);
            
        

    }

    private boolean isCurrentEvent()
    {
        return m_current instanceof Event;
    }

    private boolean isCurrentRound()
    {
        return m_current instanceof Round;
    }

    private boolean isCurrentStep()
    {
        return m_current instanceof Step;
    }

    /**
     * Add a child to the current event.
     */
    public void addChildToEvent(EventChild node)
    {
        assertCorrectThread();
        
        s_logger.log(Level.FINE, "Adding child:" + node);

        
        m_history.getGameData().acquireWriteLock();
        try
        {
            if (!isCurrentEvent())
            {
                new IllegalStateException("Not in an event, but trying to add child:" + node + " current is:" + m_current).printStackTrace(System.out);
                startEvent("???");
            }

            m_current.add(node);
        }
        finally
        {
            m_history.getGameData().releaseWriteLock();
        }            
        
        
        
        m_history.nodesWereInserted(m_current, new int[]
        { m_current.getChildCount() - 1 });
    }

    /**
     * Add a change to the current event.
     */
    public void addChange(Change change)
    {
        assertCorrectThread();
        
        s_logger.log(Level.FINE, "Adding change:" + change);

        if (!isCurrentEvent() && !isCurrentStep())
        {
            new IllegalStateException("Not in an event, but trying to add change:" + change + " current is:" + m_current).printStackTrace(System.out);
            startEvent("????");
        }
        m_history.changeAdded(change);
    }

    public void setRenderingData(Object details)
    {
        assertCorrectThread();
        
        s_logger.log(Level.FINE, "Setting rendering data:" + details);

        if (!isCurrentEvent())
        {
            new IllegalStateException("Not in an event, but trying to set details:" + details + " current is:" + m_current)
                    .printStackTrace(System.out);
            startEvent("???");
        }
        
        m_history.getGameData().acquireWriteLock();
        try
        {
            ((Event) m_current).setRenderingData(details);
        }
        finally
        {
            m_history.getGameData().releaseWriteLock();
        }
        
        m_history.reload(m_current);
    }

}