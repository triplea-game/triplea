/*
 * TranscriptListener.java
 *
 * Created on January 14, 2002, 3:07 PM
 */

package games.strategy.engine.transcript;

/**
 *
 * Something has been written to the transcript.
 *
 * @author  Sean Bridges
 */
public interface ITranscriptListener 
{
	public void messageRecieved(TranscriptMessage msg);
}
