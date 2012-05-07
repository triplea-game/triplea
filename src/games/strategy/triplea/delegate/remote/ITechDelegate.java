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
package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.dataObjects.TechResults;
import games.strategy.util.IntegerMap;

/**
 * @author Sean Bridges
 */
public interface ITechDelegate extends IRemote
{
	/**
	 * 
	 * @param rollCount
	 *            the number of tech rolls
	 * @param techToRollFor
	 *            the tech category to roll for, should be null if the game does not support
	 *            rolling for certain techs
	 * @param newTokens
	 *            if WW2V3TechModel is used it set rollCount
	 * @return TechResults. If the tech could not be rolled, then a message saying why.
	 */
	public TechResults rollTech(int rollCount, TechnologyFrontier techToRollFor, int newTokens, IntegerMap<PlayerID> whoPaysHowMuch);
}
