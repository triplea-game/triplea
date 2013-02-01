package games.strategy.chess.delegate.remote;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;

public interface IPlayDelegate extends IRemote, IDelegate
{
	public String play(Territory start, Territory end, Unit unit);
}
