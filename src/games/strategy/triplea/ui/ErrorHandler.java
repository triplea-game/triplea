package games.strategy.triplea.ui;

import games.strategy.engine.GameOverException;

/**
 * NOt entirly safe or elegant.
 * 
 * We want to ignore game over exceptions when the game is actually over.
 * 
 * This assumes only 1 game in a vm at a time.
 * 
 * @author sgb
 */
public class ErrorHandler
{
    
    private static volatile boolean m_isGameOver;
    
    public static void setGameOver(boolean aBool)
    {
        m_isGameOver = aBool;
    }
    
   
    public ErrorHandler()
    {
        
    }
    
    public void handle(Throwable t)
    {
        if(t instanceof GameOverException && m_isGameOver)
        {
            //ignore
            return;
        }
        t.printStackTrace();
    }
}
