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
package games.strategy.kingstable.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.annotations.GameProperty;

/**
 * Territory attachment for King's Table.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
@SuppressWarnings("serial")
public class TerritoryAttachment extends DefaultAttachment
{
	private boolean m_kingsSquare = false;
	private boolean m_kingsExit = false;
	
	/** Creates new TerritoryAttachment */
	public TerritoryAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setKingsSquare(final String value)
	{
		m_kingsSquare = getBool(value);
	}
	
	public boolean getKingsSquare()
	{
		return m_kingsSquare;
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setKingsExit(final String value)
	{
		m_kingsExit = getBool(value);
	}
	
	public boolean getKingsExit()
	{
		return m_kingsExit;
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
		// TODO Auto-generated method stub
	}
}
