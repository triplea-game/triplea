package games.strategy.triplea.ai.proAI.util;

/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.ui.AbstractUIContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Pro AI utilities (these are very general and maybe should be moved into delegate or engine).
 * 
 * @author Ron Murhammer
 * @since 2014
 */
public class ProUtils
{
	private final ProAI ai;
	
	public ProUtils(final ProAI ai)
	{
		this.ai = ai;
	}
	
	public List<PlayerID> getEnemyPlayers(final PlayerID player)
	{
		final GameData data = ai.getGameData();
		final List<PlayerID> enemyPlayers = new ArrayList<PlayerID>();
		for (final PlayerID players : data.getPlayerList().getPlayers())
		{
			if (!data.getRelationshipTracker().isAllied(player, players))
				enemyPlayers.add(players);
		}
		return enemyPlayers;
	}
	
	/**
	 * Pause the game to allow the human player to see what is going on.
	 */
	public void pause()
	{
		try
		{
			Thread.sleep(AbstractUIContext.getAIPauseDuration());
		} catch (final InterruptedException e)
		{
			e.printStackTrace();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
