package games.strategy.grid.chess;

import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

public class ChessUnitFactory extends DefaultUnitFactory implements IUnitFactory
{
	private static final long serialVersionUID = 6864854940846487873L;
	
	@Override
	public Unit createUnit(final UnitType type, final PlayerID owner, final GameData data)
	{
		return new ChessUnit(type, owner, data);
	}
}
