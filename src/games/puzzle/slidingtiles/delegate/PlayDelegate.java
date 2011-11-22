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
package games.puzzle.slidingtiles.delegate;

import games.puzzle.slidingtiles.attachments.Tile;
import games.puzzle.slidingtiles.delegate.remote.IPlayDelegate;
import games.puzzle.slidingtiles.ui.display.INPuzzleDisplay;
import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;

import java.io.Serializable;

/**
 * Responsible for performing a move in a game of n-puzzle.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class PlayDelegate extends BaseDelegate implements IPlayDelegate
{
	private GameMap map;
	
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start(final IDelegateBridge bridge)
	{
		super.start(bridge);
		map = getData().getMap();
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final SlidingTilesPlayExtendedDelegateState state = new SlidingTilesPlayExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final SlidingTilesPlayExtendedDelegateState s = (SlidingTilesPlayExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	/**
	 * Attempt to play.
	 * 
	 * @param play
	 *            <code>Territory</code> where the play should occur
	 */
	public String play(final Territory from, Territory to)
	{
		if (from.equals(to))
		{
			final Tile fromTile = (Tile) from.getAttachment("tile");
			if (fromTile != null && fromTile.getValue() != 0)
			{
				final Territory blank = getBlankNeighbor(map, from);
				if (blank == null)
					return "Invalid move";
				else
					to = blank;
			}
		}
		else
		{
			final String error = isValidPlay(from, to);
			if (error != null)
				return error;
		}
		performPlay(from, to, m_player);
		return null;
	}
	
	public void signalStatus(final String status)
	{
		final INPuzzleDisplay display = (INPuzzleDisplay) m_bridge.getDisplayChannelBroadcaster();
		display.setStatus(status);
	}
	
	public static Territory getBlankNeighbor(final GameMap map, final Territory t)
	{
		for (final Territory neighbor : map.getNeighbors(t))
		{
			final Tile neighborTile = (Tile) neighbor.getAttachment("tile");
			if (neighborTile != null && neighborTile.getValue() == 0)
			{
				return neighbor;
			}
		}
		return null;
	}
	
	/**
	 * Check to see if a play is valid.
	 * 
	 * @param play
	 *            <code>Territory</code> where the play should occur
	 */
	private String isValidPlay(final Territory from, final Territory to)
	{
		final int startValue = ((Tile) from.getAttachment("tile")).getValue();
		final int destValue = ((Tile) to.getAttachment("tile")).getValue();
		if (startValue != 0 && destValue == 0)
			return null;
		else
			return "Move does not swap a tile with the blank square";
		/*
		if (territory.getOwner().equals(PlayerID.NULL_PLAYERID))
		    return null;
		else
		    return "Square is not empty";
		    */
		// return "Playing not yet implemented.";
	}
	
	/**
	 * Perform a play.
	 * 
	 * @param play
	 *            <code>Territory</code> where the play should occur
	 */
	private void performPlay(final Territory from, final Territory to, final PlayerID player)
	{
		final String transcriptText = player.getName() + " moved tile from " + from.getName() + " to " + to.getName();
		m_bridge.getHistoryWriter().startEvent(transcriptText);
		swap(m_bridge, from, to);
	}
	
	static void swap(final IDelegateBridge bridge, final Territory from, final Territory to)
	{
		final Tile fromAttachment = (Tile) from.getAttachment("tile");
		final Tile toAttachment = (Tile) to.getAttachment("tile");
		final int fromValue = fromAttachment.getValue();
		final int toValue = toAttachment.getValue();
		final Change fromChange = ChangeFactory.attachmentPropertyChange(fromAttachment, Integer.toString(toValue), "value");
		final Change toChange = ChangeFactory.attachmentPropertyChange(toAttachment, Integer.toString(fromValue), "value");
		final CompositeChange change = new CompositeChange();
		change.add(fromChange);
		change.add(toChange);
		bridge.addChange(change);
		final INPuzzleDisplay display = (INPuzzleDisplay) bridge.getDisplayChannelBroadcaster();
		display.performPlay();
		// return change;
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class implements IPlayDelegate, which inherits from IRemote.
		return IPlayDelegate.class;
	}
}


@SuppressWarnings("serial")
class SlidingTilesPlayExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
}
