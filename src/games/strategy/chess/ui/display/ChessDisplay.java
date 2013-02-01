package games.strategy.chess.ui.display;

import games.strategy.chess.ui.ChessFrame;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplayBridge;

import java.util.Map;

public class ChessDisplay implements IChessDisplay
{
	@SuppressWarnings("unused")
	private IDisplayBridge m_displayBridge;
	private final ChessFrame m_ui;
	
	public ChessDisplay(final ChessFrame ui)
	{
		m_ui = ui;
	}
	
	public void initialize(final IDisplayBridge bridge)
	{
		m_displayBridge = bridge;
	}
	
	public void shutDown()
	{
		m_ui.stopGame();
	}
	
	public void setStatus(final String status)
	{
		m_ui.setStatus(status);
	}
	
	public void setGameOver()
	{
		m_ui.setGameOver();
	}
	
	public void performPlay(final Territory start, final Territory end, final Unit unit, final Map<Territory, Unit> captured)
	{
		m_ui.performPlay(start, end, unit, captured);
	}
}
