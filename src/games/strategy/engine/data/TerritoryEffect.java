package games.strategy.engine.data;

import java.io.Serializable;

public class TerritoryEffect extends NamedAttachable implements Serializable
{
	private static final long serialVersionUID = 7574312162462968921L;
	
	public TerritoryEffect(final String name, final GameData data)
	{
		super(name, data);
	}
}
