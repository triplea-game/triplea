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

package games.puzzle.tictactoe.delegate;

import games.strategy.common.delegate.BaseDelegate;
import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;

/**
 * Responsible for initializing a game of King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate: 2007-06-19 13:39:15 -0500 (Tue, 19 Jun 2007) $
 */
public class InitializationDelegate extends BaseDelegate
{
    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge bridge, GameData gameData)
    {   
        super.start(bridge, gameData);        
    }
    
    
    /**
     * If this class implements an interface which inherits from IRemote, returns the class of that interface.
     * Otherwise, returns null.
     */
    public Class<? extends IRemote> getRemoteType()
    {
        // This class does not implement the IRemote interface, so return null.
        return null;
    }
}
