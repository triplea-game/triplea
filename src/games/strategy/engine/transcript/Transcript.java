/*
 * Transcript.java
 *
 * Created on January 14, 2002, 3:12 PM
 */

package games.strategy.engine.transcript;

import java.util.*;
import java.io.Serializable;

import games.strategy.net.*;
import games.strategy.util.ListenerList;

/**
 *
 * The transcript can be written to and listened to.  
 * A simple way to broadcast messages throughout the game.
 *
 * @author  Sean Bridges
 */
public class Transcript 
{
	private IMessenger m_messenger;
	private ListenerList m_listeners = new ListenerList();
	
	/** Creates a new instance of Transcript */
    public Transcript(IMessenger messenger) 
	{
		m_messenger = messenger;
		m_messenger.addMessageListener(m_messageListener);
    }
	
	public void write(String message)
	{
		TranscriptMessage msg = new TranscriptMessage(message);
		write(msg);
	}
	
	public void write(String message, int channel)
	{
		TranscriptMessage msg = new TranscriptMessage(message, channel);
		write(msg);
	}
	
	public void write(TranscriptMessage msg)
	{
		m_messenger.broadcast(msg);
		messageRecieved(msg);
	}
	
	public void addTranscriptListener(ITranscriptListener listener)
	{
		m_listeners.add(listener);
	}
	
	public void removeTranscriptListener(ITranscriptListener listener)
	{
		m_listeners.remove(listener);
	}
	
	private void messageRecieved(TranscriptMessage msg)
	{
		Iterator iter = m_listeners.iterator();
		while(iter.hasNext())
		{
			ITranscriptListener listener = (ITranscriptListener) iter.next();
			listener.messageRecieved(msg);
		}
	}
	
	private IMessageListener m_messageListener = new IMessageListener()
	{
		public void messageReceived(Serializable msg, INode from)
		{
			if(msg instanceof TranscriptMessage)
				messageRecieved( (TranscriptMessage) msg);
		}
	};
}
