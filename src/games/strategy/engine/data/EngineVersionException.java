package games.strategy.engine.data;

public class EngineVersionException extends Exception
{
	private static final long serialVersionUID = 8800415601463715772L;
	
	public EngineVersionException(final String error)
	{
		super(error);
	}
}
