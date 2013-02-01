package games.strategy.chess.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;

public class PlayerAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = 8757958985142045603L;
	
	/** Creates new PlayerAttachment */
	public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
	}
}
