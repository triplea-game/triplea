package games.strategy.engine;


/**
 * 
 * Thrown when the game is over.<p>
 * 
 * Normally delegates should not catch this, but let it propogate.<p>
 * 
 * Displays and players should handle this as it may be thrown by delegates remotes
 * if a method is executed after the game is finished.
 * 
 * @author sgb
 */
public class GameOverException extends RuntimeException
{

    public GameOverException(String string)
    {
        super(string);
    }

}
