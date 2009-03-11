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
 * DefaultMapSelectionListener.java
 *
 * Created on November 7, 2001, 8:36 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class DefaultMapSelectionListener implements MapSelectionListener 
{

	public void territorySelected(Territory territory, MouseDetails me) {}
	
	public void mouseEntered(Territory territory) {}
	
	public void mouseMoved(Territory territory, MouseDetails me) {}
	
}
