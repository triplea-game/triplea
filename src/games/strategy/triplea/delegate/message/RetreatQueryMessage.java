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
 * RetreatQueryMessage.java
 *
 * Created on November 22, 2001, 9:58 AM
 */

package games.strategy.triplea.delegate.message;

import java.util.Collection;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class RetreatQueryMessage extends BattleMessage
{
    private boolean m_submerge;
	private Collection m_territories;
	private String m_message;

	/** Creates new RetreatQueryMessage */
    public RetreatQueryMessage(String step, Collection territories, String message) 
	{
        this(false, step, territories, message);
    }
	
	/** Creates new RetreatQueryMessage */
    public RetreatQueryMessage(boolean submerge, String step, Collection territories, String message) 
	{
		super(step);
		m_submerge = submerge;
		m_territories = territories;

		m_message = message;
    }
	
    public boolean getSubmerge()
    {
        return m_submerge;
    }
	
	public Collection getTerritories()
	{
		return m_territories;
	}
	
	public String getMessage()
	{
		return m_message;
	}
}
