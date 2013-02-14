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
package games.strategy.grid.player;

import games.strategy.common.player.AbstractBaseAI;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;

import java.util.Collection;

/**
 * Abstract class for a Grid AI agent.
 * 
 * @author Lane Schwartz (original) and Veqryn (abstraction)
 * @version $LastChangedDate: 2011-11-22 18:21:37 +0800 (Tue, 22 Nov 2011) $
 */
public abstract class GridAbstractAI extends AbstractBaseAI implements IGridGamePlayer
{
	public GridAbstractAI(final String name, final String type)
	{
		super(name, type);
	}
	
	protected abstract void play();
	
	protected void endTurn()
	{
	}
	
	/**
	 * The given phase has started. Parse the phase name and call the appropiate method.
	 */
	@Override
	public void start(final String stepName)
	{
		if (stepName.endsWith("Play"))
			play();
		else if (stepName.endsWith("EndTurn"))
			endTurn();
		else
			throw new IllegalArgumentException("Unrecognized step stepName:" + stepName);
	}
	
	public final Class<IGridGamePlayer> getRemotePlayerType()
	{
		return IGridGamePlayer.class;
	}
	
	public UnitType selectUnit(final Unit startUnit, final Collection<UnitType> options, final Territory territory, final PlayerID player, final GameData data, final String message)
	{
		return null;
	}
}
