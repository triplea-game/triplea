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

/*
 * ITestDelegateBridge.java
 *
 */

package games.strategy.engine.data;

import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.random.IRandomSource;

/**
 * 
 * @author Tony Clayton
 * 
 * Not for actual use, suitable for testing. Never returns messages, but can get
 * random and implements changes immediately.
 */
public interface ITestDelegateBridge extends IDelegateBridge
{
    /**
     * Changing the player has the effect of commiting the current transaction.
     * Player is initialized to the player specified in the xml data.
     */
    public void setPlayerID(PlayerID aPlayer);

    public boolean inTransaction();

    public void commit();

    public void startTransaction();

    public void rollback();

    public void setStepName(String name);

    public void setRandomSource(IRandomSource randomSource);
    
    public void setRemote(IRemote remote);

}
