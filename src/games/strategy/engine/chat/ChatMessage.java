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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package games.strategy.engine.chat;


import java.io.Serializable;

/**
 *
 * @author Stephen Wicklund
 */
public class ChatMessage implements Serializable
{
    private String m_message = "";
    private ChatBroadcastType m_broadcastType = ChatBroadcastType.Public_Chat;
    private Boolean m_hideFromServer = false;
    private String m_from = "";
    private Boolean m_isMeMessage = false;
    public ChatMessage(String message, ChatBroadcastType broadcastType, Boolean hideFromServer, String from, Boolean isMeMessage)
    {
        m_message = message;
        m_broadcastType = broadcastType;
        m_hideFromServer = hideFromServer;
        m_from = from;
        m_isMeMessage = isMeMessage;
    }
    public String GetMessage()
    {
        return m_message;
    }
    public ChatBroadcastType GetBroadcastType()
    {
        return m_broadcastType;
    }
    public Boolean GetHiddenFromServer()
    {
        return m_hideFromServer;
    }
    public String GetSender()
    {
        return m_from;
    }
    public Boolean IsMeMessage()
    {
        return m_isMeMessage;
    }
    public void SetHiddenFromServer(Boolean hide)
    {
        m_hideFromServer = hide;
    }
    public void SetSender(String name)
    {
        m_from = name;
    }
}

