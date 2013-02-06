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
package games.strategy.grid.chess.attachments;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.annotations.GameProperty;

import java.util.ArrayList;

/**
 * 
 * @author veqryn
 * 
 */
public class PlayerAttachment extends DefaultAttachment
{
	private static final long serialVersionUID = -3602673484709292709L;
	public static final String ATTACHMENT_NAME = "playerAttachment";
	public static final String LAST_PIECES_MOVED = "lastPiecesMoved";
	private ArrayList<Unit> m_lastPiecesMoved = new ArrayList<Unit>();
	
	/**
	 * Convenience method. can return null.
	 */
	public static PlayerAttachment get(final PlayerID p)
	{
		final PlayerAttachment rVal = (PlayerAttachment) p.getAttachment(ATTACHMENT_NAME);
		return rVal;
	}
	
	/** Creates new PlayerAttachment */
	public PlayerAttachment(final String name, final Attachable attachable, final GameData gameData)
	{
		super(name, attachable, gameData);
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setLastPiecesMoved(final String value)
	{
	}
	
	@GameProperty(xmlProperty = true, gameProperty = true, adds = false)
	public void setLastPiecesMoved(final ArrayList<Unit> value)
	{
		m_lastPiecesMoved = value;
	}
	
	public ArrayList<Unit> getLastPiecesMoved()
	{
		return m_lastPiecesMoved;
	}
	
	public void resetLastPiecesMoved()
	{
		m_lastPiecesMoved = new ArrayList<Unit>();
	}
	
	@Override
	public void validate(final GameData data) throws GameParseException
	{
	}
}
