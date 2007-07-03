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

package games.strategy.kingstable.ui.display;

import games.strategy.engine.data.Territory;
import games.strategy.engine.display.IDisplayBridge;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

/**
 * Dummy display for a King's Table game, for use in testing.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class DummyDisplay implements IKingsTableDisplay {

	/** 
     * @see games.strategy.engine.display.IKingsTableDisplay#performPlay(Territory,Territory,Collection<Territory>)
     */
	public void performPlay(Territory start, Territory end, Collection<Territory> captured) {

	}

	/** 
     * @see games.strategy.engine.display.IKingsTableDisplay#setGameOver()
     */
	public void setGameOver()//CountDownLatch waiting) {
    {

	}

	/** 
     * @see games.strategy.engine.display.IKingsTableDisplay#setStatus(String)
     */
	public void setStatus(String status) {

	}

	/** 
     * @see games.strategy.engine.display.IKingsTableDisplay#initialize(IDisplayBridge)
     */
	public void initialize(IDisplayBridge bridge) {

	}

	/** 
     * @see games.strategy.engine.display.IKingsTableDisplay#shutDown()
     */
	public void shutDown() {

	}

}
