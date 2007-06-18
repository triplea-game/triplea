package games.strategy.kingstable.delegate.remote;

import games.strategy.engine.data.Territory;
import games.strategy.engine.message.IRemote;

/**
 * Implementing class is responsible for performing a turn in a Kings Table game.
 *
 * @author Lane Schwartz
 */
public interface IPlayDelegate extends IRemote
{
    public String play(Territory start, Territory end);
}
