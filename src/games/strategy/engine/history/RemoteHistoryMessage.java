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

import javax.swing.*;
import java.awt.event.*;

/**
 * These events are written by the delegate, and need to be serialized and sent
 *  to all games.
 */
public class RemoteHistoryMessage implements java.io.Serializable
{
    private static long s_currentMessageIndex = 1;  
    
    
    private final long m_messageIndex;
    
    //this is a little curious
    //the final variables referenced by the anonymous
    //inner class are serialized when the object is sent over the network
    private Action m_action;
    
    //this is set before the action is performed
    //its only reason for existing is so I didnt have to create a new interface
    //to pass it to the actions.
    private transient HistoryWriter m_historyWriter;
    
    public RemoteHistoryMessage(final String event)
    {
        synchronized(RemoteHistoryMessage.class)
        {
            m_messageIndex = s_currentMessageIndex++;
        }
        
        
        m_action = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                m_historyWriter.startEvent(event);
            }
        };
    }
    
    public RemoteHistoryMessage(final String text, final Object renderingData)
    {

        synchronized(RemoteHistoryMessage.class)
        {
            m_messageIndex = s_currentMessageIndex++;
        }

        
        m_action = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                m_historyWriter.addChildToEvent(new EventChild(text, renderingData));
            }
        };
        
    }
    
    public RemoteHistoryMessage(final Object renderingData)
    {
        synchronized(RemoteHistoryMessage.class)
        {
            m_messageIndex = s_currentMessageIndex++;
        }

        
        m_action = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                m_historyWriter.setRenderingData(renderingData);
            }
        };
        
    }
    
    public void perform(HistoryWriter writer)
    {
        long waitCount = 0;
        Object lock = new Object();
        //ensure messages get processed in the order they are generated
        while(writer.getLastMessageReceived() +1 != m_messageIndex && writer.getLastMessageReceived() > 0)
        {
            waitCount++;
            
            if(waitCount > 20)
            {
                new IllegalStateException("Could not add history, m_index:" + m_messageIndex + " Last index:" + writer.getLastMessageReceived() + " this:" + this  ).printStackTrace();
                break;
            }
            
            synchronized(lock)
            {
                try
                {
                    lock.wait(50);
                } catch (InterruptedException e)
                {
                    //ignore
                }
            }
            
        }
        synchronized(writer)
        {
            writer.setLastMessageReceived(m_messageIndex);
            m_historyWriter = writer;
            m_action.actionPerformed(null);
            m_historyWriter = null;
        }
        
    }
}
