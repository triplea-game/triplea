package games.strategy.engine.random;

import java.io.IOException;

public interface IRemoteDiceServer
{
    
    /**
     * Post a request to the dice server, and return the resulting html page as a string 
     */
    public String postRequest(String player1, String player2, int max, int numDice, String text, String gameID) throws IOException;
    
    
    /**
     * Given the html page returned from postRequest, return the dice [] 
     */
    public int[] getDice(String string, int count) throws IOException;
    
    
    /**
     * Get the display name for this dice server
     */
    public String getName();

}