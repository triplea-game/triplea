/*
 * TranscriptMessage.java
 *
 * Created on January 14, 2002, 3:06 PM
 */

package games.strategy.engine.transcript;

/**
 *
 *  Something to write to the transcript.
 *
 * @author  Sean Bridges
 */
public class TranscriptMessage implements java.io.Serializable
{
	
	/**
	 * If no channel is defined.
	 */
	public static final int DEFAULT_CHANNEL = 0;
	
	/**
	 * For prioirty messages.
	 */
	public static final int PRIORITY_CHANNEL = 1;
	
	public String m_message;
	public int m_channel = DEFAULT_CHANNEL;	

	public TranscriptMessage(String message) 
	{
		m_message = message;
    }

	
	/** Creates a new instance of NotificationMessage */
    public TranscriptMessage(String message, int channel) 
	{
		m_message = message;
		m_channel = channel;
    }
	
	public String getMessage()
	{
		return m_message;
	}
	
	public int getChannel()
	{
		return m_channel;
	}
}
