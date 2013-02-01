package games.strategy.chess.ui.display;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplay;

import java.util.Map;

public interface IChessDisplay extends IDisplay
{
	/**
	 * Graphically notify the user of the current game status.
	 * 
	 * @param error
	 *            the status message to display
	 */
	public void setStatus(String status);
	
	/**
	 * Set the game over status for this display to <code>true</code>.
	 */
	public void setGameOver();
	
	/**
	 * Ask the user interface for this display to process a play and zero or more captures.
	 * 
	 * @param start
	 *            <code>Territory</code> where the play began
	 * @param end
	 *            <code>Territory</code> where the play ended
	 * @param captured
	 *            <code>Collection</code> of <code>Territory</code>s whose pieces were captured during the play
	 */
	public void performPlay(Territory start, Territory end, Unit unit, Map<Territory, Unit> captured);
}
