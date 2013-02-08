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
package games.strategy.grid.ui;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author Lane Schwartz
 * 
 */
public interface IGridPlayData extends Serializable
{
	public Territory getStart();
	
	public List<Territory> getMiddleSteps();
	
	public Territory getEnd();
	
	public List<Territory> getAllSteps();
	
	public List<Territory> getAllStepsExceptStart();
	
	public PlayerID getPlayerID();
	
	public boolean isBiggerThanAndContains(IGridPlayData otherPlay);
}
