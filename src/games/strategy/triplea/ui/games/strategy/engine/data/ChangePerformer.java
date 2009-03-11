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
 * ChangePerformer.java
 *
 * Created on January 1, 2002, 1:18 PM
 */

package games.strategy.engine.data;

import javax.swing.SwingUtilities;

/**
 *
 * @author  Sean Bridges
 *
 * Allows changes to be performed outside of the data package.
 * Should not be created by non engine code.
 * Made this since I didnt want to unprotect the Change.perform method,
 * but didnt want to put everything that needed to
 * perform a change in the data package.
 */
public class ChangePerformer
{
	private final GameData m_data;

	/** Creates a new instance of ChangePerformer */
    public ChangePerformer(GameData data)
	{
        if(data == null)
            throw new IllegalArgumentException("Data cannot be null");
		m_data = data;
    }

	public void perform(Change aChange)
	{
        
        if(m_data.areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
        
		try
		{
		    m_data.acquireWriteLock();
			aChange.perform(m_data);
		} finally
		{
		    m_data.releaseWriteLock();
		}
        m_data.notifyGameDataChanged(aChange);
        
	}
}
