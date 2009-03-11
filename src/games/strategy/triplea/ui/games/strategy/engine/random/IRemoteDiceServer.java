package games.strategy.engine.random;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public interface IRemoteDiceServer
{
    
    /**
     * Post a request to the dice server, and return the resulting html page as a string 
     */
    public String postRequest(String player1, String player2, int max, int numDice, String text, String gameID, String gameUUID) throws IOException;
    
    
    /**
     * Given the html page returned from postRequest, return the dice []
     * 
     *  throw an InvocationTargetException to indicate an error message to be returned
     */
    public int[] getDice(String string, int count) throws IOException, InvocationTargetException;
    
    
    /**
     * Get the display name for this dice server
     */
    public String getName();

}