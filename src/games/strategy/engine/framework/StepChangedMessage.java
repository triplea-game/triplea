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
 * StepChangedMessage.java
 *
 * Created on January 1, 2002, 12:30 PM
 */

package games.strategy.engine.framework;

import java.io.*;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.GameObjectInputStream;
import games.strategy.engine.message.Message;
import games.strategy.net.OrderedMessage;

/**
 *
 * @author  Sean Bridges
 */
class StepChangedMessage implements Message, OrderedMessage
{
    
    private static final long serialVersionUID = 3330970682208872242L;
    
    private String m_stepName;
    private String m_delegateName;
    private PlayerID m_player;
    private int m_round;
    private String m_displayName;
    
    /** Creates a new instance of StepChangedMessage */
    StepChangedMessage(String stepName, String delegateName, PlayerID player, int round, String displayName)
    {
        m_delegateName = delegateName;
        m_player = player;
        m_stepName = stepName;
        m_round = round;
        m_displayName = displayName;
    }
    
    public String getStepName()
    {
        return m_stepName;
    }
    
    public String getDelegateName()
    {
        return m_delegateName;
    }
    
    public PlayerID getPlayer()
    {
        return m_player;
    }
    
    public int getRound()
    {
        return m_round;
    }
    
    public String getDisplayName()
    {
        return m_displayName;
    }
}
