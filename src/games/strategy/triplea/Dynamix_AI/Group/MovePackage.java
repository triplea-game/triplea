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

package games.strategy.triplea.Dynamix_AI.Group;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.delegate.remote.IMoveDelegate;

/**
 * 
 * @author Stephen
 */
public class MovePackage
{
	public Dynamix_AI AI = null;
	public GameData Data = null;
	public IMoveDelegate Mover = null;
	public PlayerID Player = null;
	public Object Obj1 = null;
	public Object Obj2 = null;
	public Object Obj3 = null;
	
	public MovePackage(Dynamix_AI ai, GameData data, IMoveDelegate mover, PlayerID player, Object obj1, Object obj2, Object obj3)
	{
		AI = ai;
		Data = data;
		Mover = mover;
		Player = player;
		Obj1 = obj1;
		Obj2 = obj2;
		Obj3 = obj3;
	}
	
	public MovePackage SetObj1To(Object obj1)
	{
		Obj1 = obj1;
		return this;
	}
	
	public MovePackage SetObjectsTo(Object obj1, Object obj2, Object obj3)
	{
		Obj1 = obj1;
		Obj2 = obj2;
		Obj3 = obj3;
		return this;
	}
}
