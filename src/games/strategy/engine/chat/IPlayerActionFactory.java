/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.engine.chat;

import games.strategy.net.INode;

import java.util.Collections;
import java.util.List;

import javax.swing.Action;


public interface IPlayerActionFactory
{

    public static final IPlayerActionFactory NULL_FACTORY = new IPlayerActionFactory()
    {
    
        public List<Action> mouseOnPlayer(INode clickedOn)
        {
            return Collections.emptyList();
        }
    };
    
    /**
     *  The mouse has been clicked on a player, create a list of actions to be displayed
     */
    public List<Action> mouseOnPlayer(INode clickedOn);
}
