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
 * Chat.java
 *
 * Created on January 14, 2002, 11:10 AM
 */

package games.strategy.engine.chat;

import java.util.*;
import java.io.Serializable;
import games.strategy.net.*;

import games.strategy.engine.transcript.*;

/**
 *
 * chat logic.
 *
 * @author  Sean Bridges
 */
public class Chat 
{
	private ChatFrame m_frame;
	private IMessenger m_messenger;
	private Transcript m_transcript;
	
	/** Creates a new instance of Chat */
    public Chat(IMessenger messenger, ChatFrame frame) 
	{
		m_frame = frame;
		m_messenger = messenger;
		m_messenger.addMessageListener(m_messageListener);
		m_messenger.addConnectionChangeListener(m_connectionChangeListener);
		updateConnections();
		
    }
	
	/**
	 * Stop receiving events from the messenger.
	 */
	public void shutdown()
	{
		m_messenger.removeMessageListener(m_messageListener);
		m_messenger.removeConnectionChangeListener(m_connectionChangeListener);	
		if(m_transcript != null)
		{
			m_transcript.removeTranscriptListener(m_transcriptListener);
			m_transcript = null;
		}
	}

	public synchronized void showTranscript(Transcript t)
	{
		if(m_transcript != null)
			throw new IllegalStateException("Already displaying a transcript");
		
		m_transcript =t;
		m_transcript.addTranscriptListener(m_transcriptListener);
	}
	
	void sendMessage(ChatMessage msg)
	{
		m_messenger.broadcast(msg);
		m_frame.addMessage( msg, m_messenger.getLocalNode().getName());
	}

	private synchronized void updateConnections()
	{
		Set players = m_messenger.getNodes();
		List playerNames = new ArrayList(players.size());

		Iterator iter = players.iterator();
		while(iter.hasNext())
		{
			INode node = (INode) iter.next();
			String name = node.getName();
			playerNames.add(name);
		}

		Collections.sort(playerNames);
		m_frame.updatePlayerList(playerNames);
	}
	
	private IConnectionChangeListener m_connectionChangeListener = new IConnectionChangeListener()
	{
		public void connectionsChanged()
		{
			updateConnections();
		}
	};

	private ITranscriptListener m_transcriptListener = new ITranscriptListener()
	{
		public void messageRecieved(TranscriptMessage msg)
		{
			m_frame.addMessage(msg.getMessage(), "Game");
		}
	};
	
	private IMessageListener m_messageListener = new IMessageListener()
	{
		public void messageReceived(Serializable msg, INode from)
		{
			if(msg instanceof ChatMessage)
			{
				m_frame.addMessage( (ChatMessage) msg, from.getName());
			}
		}
	};
}