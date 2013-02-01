package games.strategy.chess.ui;

import games.strategy.common.ui.IPlayData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;

public class PlayData implements IPlayData
{
	private final Territory m_start;
	private final Territory m_end;
	private final Unit m_unit;
	
	/**
	 * Construct a new play, with the given start location and end location.
	 * 
	 * @param start
	 *            <code>Territory</code> where the play should start
	 * @param end
	 *            <code>Territory</code> where the play should end
	 */
	public PlayData(final Territory start, final Territory end, final Unit unit)
	{
		m_start = start;
		m_end = end;
		m_unit = unit;
	}
	
	/**
	 * Returns the start location for this play.
	 * 
	 * @return <code>Territory</code> where this play starts.
	 */
	public Territory getStart()
	{
		return m_start;
	}
	
	/**
	 * Returns the end location for this play.
	 * 
	 * @return <code>Territory</code> where this play ends.
	 */
	public Territory getEnd()
	{
		return m_end;
	}
	
	/**
	 * Returns the unit moving for this play.
	 * 
	 * @return <code>Unit</code> of this movement.
	 */
	public Unit getUnit()
	{
		return m_unit;
	}
}
