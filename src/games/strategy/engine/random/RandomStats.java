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
package games.strategy.engine.random;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.util.IntegerMap;

import java.util.HashMap;
import java.util.Map;

public class RandomStats implements IRandomStats
{
	private final IRemoteMessenger m_remoteMessenger;
	private final Map<PlayerID, IntegerMap<Integer>> m_randomStats = new HashMap<PlayerID, IntegerMap<Integer>>();
	
	public RandomStats(final IRemoteMessenger remoteMessenger)
	{
		m_remoteMessenger = remoteMessenger;
		remoteMessenger.registerRemote(this, RANDOM_STATS_REMOTE_NAME);
	}
	
	public void shutDown()
	{
		m_remoteMessenger.unregisterRemote(RANDOM_STATS_REMOTE_NAME);
	}
	
	public synchronized void addRandom(final int[] random, final PlayerID player, final DiceType diceType)
	{
		IntegerMap<Integer> map = m_randomStats.get(player);
		if (map == null)
			map = new IntegerMap<Integer>();
		for (int i = 0; i < random.length; i++)
		{
			map.add(Integer.valueOf(random[i] + 1), 1);
		}
		// for now, only record if it is combat, otherwise if not combat, throw it in the null pile
		m_randomStats.put((diceType == DiceType.COMBAT ? player : null), map);
	}
	
	public synchronized void addRandom(final int random, final PlayerID player, final DiceType diceType)
	{
		IntegerMap<Integer> map = m_randomStats.get(player);
		if (map == null)
			map = new IntegerMap<Integer>();
		map.add(Integer.valueOf(random + 1), 1);
		m_randomStats.put((diceType == DiceType.COMBAT ? player : null), map);
	}
	
	public synchronized RandomStatsDetails getRandomStats(final int diceSides)
	{
		return new RandomStatsDetails(m_randomStats, diceSides);
	}
}
