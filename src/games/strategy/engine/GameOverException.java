package games.strategy.engine;

/**
 * 
 * Thrown when the game is over.
 * <p>
 * 
 * Normally delegates should not catch this, but let it propogate.
 * <p>
 * 
 * Displays and players should handle this as it may be thrown by delegates remotes if a method is executed after the game is finished.
 * 
 * @author sgb
 */
public class GameOverException extends RuntimeException
{
	private static final long serialVersionUID = -167666722695780120L;
	
	public GameOverException(final String string)
	{
		super(string);
	}
}
