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
package games.strategy.triplea.ui.history;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.PlacementDescription;
import games.strategy.triplea.ui.DicePanel;
import games.strategy.triplea.ui.MapPanel;
import games.strategy.triplea.ui.SimpleUnitPanel;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class HistoryDetailsPanel extends JPanel
{
	private static final long serialVersionUID = 5092004144144006960L;
	private final GameData m_data;
	private final JTextArea m_title = new JTextArea();
	private final JScrollPane m_scroll = new JScrollPane(m_title);
	private final MapPanel m_mapPanel;
	
	public HistoryDetailsPanel(final GameData data, final MapPanel mapPanel)
	{
		m_data = data;
		setLayout(new GridBagLayout());
		m_title.setWrapStyleWord(true);
		m_title.setBackground(this.getBackground());
		m_title.setLineWrap(true);
		m_title.setBorder(null);
		m_title.setEditable(false);
		m_scroll.setBorder(null);
		m_mapPanel = mapPanel;
	}
	
	@SuppressWarnings("unchecked")
	public void render(final HistoryNode node)
	{
		removeAll();
		m_mapPanel.setRoute(null);
		final Insets insets = new Insets(5, 0, 0, 0);
		m_title.setText(node.getTitle());
		add(m_scroll, new GridBagConstraints(0, 0, 1, 1, 1, 0.1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0));
		final GridBagConstraints mainConstraints = new GridBagConstraints(0, 1, 1, 1, 1, 0.9, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0);
		if (node instanceof Renderable)
		{
			final Object details = ((Renderable) node).getRenderingData();
			if (details instanceof DiceRoll)
			{
				final DicePanel dicePanel = new DicePanel(m_mapPanel.getUIContext(), m_data);
				dicePanel.setDiceRoll((DiceRoll) details);
				add(dicePanel, mainConstraints);
			}
			else if (details instanceof MoveDescription)
			{
				final MoveDescription moveMessage = (MoveDescription) details;
				renderUnits(mainConstraints, moveMessage.getUnits());
				m_mapPanel.setRoute(moveMessage.getRoute());
				if (!m_mapPanel.isShowing(moveMessage.getRoute().getEnd()))
					m_mapPanel.centerOn(moveMessage.getRoute().getEnd());
			}
			else if (details instanceof PlacementDescription)
			{
				final PlacementDescription placeMessage = (PlacementDescription) details;
				renderUnits(mainConstraints, placeMessage.getUnits());
				if (!m_mapPanel.isShowing(placeMessage.getTerritory()))
					m_mapPanel.centerOn(placeMessage.getTerritory());
			}
			else if (details instanceof Collection)
			{
				final Collection<Object> objects = (Collection<Object>) details;
				final Iterator<Object> objIter = objects.iterator();
				if (objIter.hasNext())
				{
					final Object obj = objIter.next();
					if (obj instanceof Unit)
					{
						final Collection<Unit> units = (Collection<Unit>) details;
						renderUnits(mainConstraints, units);
					}
				}
			}
			else if (details instanceof Territory)
			{
				final Territory t = (Territory) details;
				if (!m_mapPanel.isShowing(t))
					m_mapPanel.centerOn(t);
			}
		}
		add(Box.createGlue());
		validate();
		repaint();
	}
	
	private void renderUnits(final GridBagConstraints mainConstraints, final Collection<Unit> units)
	{
		final Collection<UnitCategory> unitsCategories = UnitSeperator.categorize(units);
		final SimpleUnitPanel unitsPanel = new SimpleUnitPanel(m_mapPanel.getUIContext());
		unitsPanel.setUnitsFromCategories(unitsCategories, m_data);
		add(unitsPanel, mainConstraints);
	}
}
