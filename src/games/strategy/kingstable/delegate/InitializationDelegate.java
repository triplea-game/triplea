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
package games.strategy.kingstable.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.message.IRemote;
import games.strategy.kingstable.attachments.PlayerAttachment;

import java.io.Serializable;

/**
 * Responsible for initializing a game of King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class InitializationDelegate extends BaseDelegate
{
	/**
	 * Called before the delegate will run.
	 */
	@Override
	public void start()
	{
		super.start();
		PlayerID attacker = null;
		PlayerID defender = null;
		final GameData data = getData();
		for (final PlayerID player : data.getPlayerList().getPlayers())
		{
			final PlayerAttachment pa = (PlayerAttachment) player.getAttachment("playerAttachment");
			if (pa == null)
				attacker = player;
			else if (pa.getNeedsKing())
				defender = player;
			else
				attacker = player;
		}
		if (attacker == null)
			throw new RuntimeException("Invalid game setup - no attacker is specified. Reconfigure the game xml file so that one player has a playerAttachment with needsKing set to false.");
		if (defender == null)
			throw new RuntimeException("Invalid game setup - no defender is specified. Reconfigure the game xml file so that one player has a playerAttachment with needsKing set to true.");
		for (final Territory t : data.getMap().getTerritories())
		{
			if (!t.getUnits().isEmpty())
			{
				if (t.getUnits().size() <= 1)
				{
					// This loop should be run exactly zero times or one time
					for (final PlayerID owner : t.getUnits().getPlayersWithUnits())
					{
						t.setOwner(owner);
					}
				}
				else
				{
					throw new RuntimeException("Territory " + t + " contains more than one unit.");
				}
			}
		}
	}
	
	@Override
	public void end()
	{
		super.end();
	}
	
	@Override
	public Serializable saveState()
	{
		final KingsTableInitializationExtendedDelegateState state = new KingsTableInitializationExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final KingsTableInitializationExtendedDelegateState s = (KingsTableInitializationExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
	}
	
	public boolean stuffToDoInThisDelegate()
	{
		return false;
	}
	
	/**
	 * If this class implements an interface which inherits from IRemote, returns the class of that interface.
	 * Otherwise, returns null.
	 */
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		// This class does not implement the IRemote interface, so return null.
		return null;
	}
}


class KingsTableInitializationExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = -3197858177470882249L;
	Serializable superState;
	// add other variables here:
}
