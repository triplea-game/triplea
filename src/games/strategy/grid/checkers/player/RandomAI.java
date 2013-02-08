package games.strategy.grid.checkers.player;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.player.GridAbstractAI;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;

import java.util.Collection;
import java.util.Random;

public class RandomAI extends GridAbstractAI
{
	public RandomAI(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void play()
	{
		// Unless the triplea.ai.pause system property is set to false,
		// pause for 0.8 seconds to give the impression of thinking
		pause();
		// Get the collection of territories from the map
		final Collection<Territory> territories = getGameData().getMap().getTerritories();
		final Territory[] territoryArray = territories.toArray(new Territory[territories.size()]);
		final Random generator = new Random();
		int trymeStart;
		int trymeEnd;
		String error;
		// Get the play delegate
		final IGridPlayDelegate playDel = (IGridPlayDelegate) this.getPlayerBridge().getRemote();
		final PlayerID me = getPlayerID();
		// Randomly select a territory and try playing there
		// If that play isn't legal, try again
		do
		{
			trymeStart = generator.nextInt(territoryArray.length);
			trymeEnd = generator.nextInt(territoryArray.length);
			final IGridPlayData play = new GridPlayData(territoryArray[trymeStart], territoryArray[trymeEnd], me);
			error = playDel.play(play);
		} while (error != null);
	}
}
