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
package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IChannelSubscribor;

/**
 * All changes to game data (Changes and History events) can be tracked through this channel.
 * 
 * @author Sean Bridges
 */
public interface IGameModifiedChannel extends IChannelSubscribor
{
	public void gameDataChanged(final Change aChange);
	
	public void startHistoryEvent(final String event, final Object renderingData);
	
	public void startHistoryEvent(final String event);
	
	// public void setRenderingData(final Object renderingData);
	
	public void addChildToEvent(final String text, final Object renderingData);
	
	/**
	 * 
	 * @param stepName
	 * @param delegateName
	 * @param player
	 * @param round
	 * @param displayName
	 * @param loadedFromSavedGame
	 *            - true if the game step has changed because we were loaded from a saved game
	 */
	public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName, final boolean loadedFromSavedGame);
	
	public void shutDown();
}
