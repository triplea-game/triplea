package games.strategy.grid.chess;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;

import java.io.Serializable;

public class ChessUnit extends Unit implements Serializable
{
	private static final long serialVersionUID = -1633476469497003117L;
	public static final String HAS_MOVED = "hasMoved";
	private int m_hasMoved = 0;
	
	protected ChessUnit(final UnitType type, final PlayerID owner, final GameData data)
	{
		super(type, owner, data);
	}
	
	public int getHasMoved()
	{
		return m_hasMoved;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setHasMoved(final int value)
	{
		m_hasMoved = value;
	}
	
	@GameProperty(xmlProperty = false, gameProperty = true, adds = false)
	public void setHasMoved(final Integer value)
	{
		m_hasMoved = value;
	}
}
