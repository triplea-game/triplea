package games.strategy.grid.ui;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.formatter.MyFormatter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class GridEndTurnData implements IGridEndTurnData
{
	private static final long serialVersionUID = -4940904520179372089L;
	protected final PlayerID m_player;
	protected final Set<Territory> m_territoryUnitsRemovalAdjustment;
	protected final boolean m_wantToContinuePlaying;
	
	public GridEndTurnData(final Collection<Territory> territoryUnitsRemovalAdjustment, final boolean wantToContinuePlaying, final PlayerID player)
	{
		m_player = player;
		m_territoryUnitsRemovalAdjustment = (territoryUnitsRemovalAdjustment == null ? null : new HashSet<Territory>(territoryUnitsRemovalAdjustment));
		m_wantToContinuePlaying = wantToContinuePlaying;
	}
	
	public GridEndTurnData(final IGridEndTurnData groupsThatShouldDie)
	{
		this(groupsThatShouldDie.getTerritoryUnitsRemovalAdjustment(), groupsThatShouldDie.getWantToContinuePlaying(), groupsThatShouldDie.getPlayer());
	}
	
	public Set<Territory> getTerritoryUnitsRemovalAdjustment()
	{
		return (m_territoryUnitsRemovalAdjustment == null ? null : new HashSet<Territory>(m_territoryUnitsRemovalAdjustment));
	}
	
	public boolean getWantToContinuePlaying()
	{
		return m_wantToContinuePlaying;
	}
	
	public PlayerID getPlayer()
	{
		return m_player;
	}
	
	@Override
	public String toString()
	{
		return (m_player == null ? "" : m_player.getName() + " ")
					+ (m_wantToContinuePlaying ? "continuing play" :
								(m_territoryUnitsRemovalAdjustment == null || m_territoryUnitsRemovalAdjustment.isEmpty() ? "making no territory adjustments" :
											"changing the following territories: " + MyFormatter.defaultNamedToTextList(m_territoryUnitsRemovalAdjustment)));
	}
}
