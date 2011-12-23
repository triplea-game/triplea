package games.strategy.engine.random;

import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface IRemoteDiceServer extends IBean
{
	/**
	 * Post a request to the dice server, and return the resulting html page as a string
	 */
	public String postRequest(int max, int numDice, String subjectMessage, String gameID, String gameUUID) throws IOException;
	
	/**
	 * Given the html page returned from postRequest, return the dice []
	 * 
	 * throw an InvocationTargetException to indicate an error message to be returned
	 */
	public int[] getDice(String string, int count) throws IOException, InvocationTargetException;
	
	/**
	 * Get the to address
	 * @return returns the to address or null if not configured
	 */
	String getToAddress();

	/**
	 * Set the to address
	 * @param toAddress the new to address
	 */
	void setToAddress(String toAddress);

	/**
	 * get the CC address
	 * @return the address or null if not configured
	 */
	String getCcAddress();

	/**
	 * Set the cc address
	 * @param ccAddress the address or null if not configured
	 */
	void setCcAddress(String ccAddress);

	/**
	 * Get the info text displayed for this Dice server
	 * @return the info text
	 */
	String getInfoText();

	/**
	 * True if this dice server sends email, and therefore requires email addresses
	 * @return true if email addresses are required
	 */
	boolean sendsEmail();

	/**
	 * True if this dice server requires a game id
	 * @return true if a game id is required
	 */
	boolean supportsGameId();

	/**
	 * Set the game id used for this instance.
	 * If you don't provide a game id, the TripleA GUID will be used
	 * @param gameId the new game id
	 */
	public void setGameId(String gameId);

	/**
	 * Get the configured game id
	 * @return the game id or null if not configured
	 */
	public String getGameId();

}
