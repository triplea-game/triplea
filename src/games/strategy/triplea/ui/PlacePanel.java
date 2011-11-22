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
/*
 * PlacePanel.java
 * 
 * Created on December 4, 2001, 7:45 PM
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.Match;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Sean Bridges, edited by Erik von der Osten
 * @version 1.1
 */
public class PlacePanel extends AbstractMovePanel
{
	private final JLabel actionLabel = new JLabel();
	private PlaceData m_placeData;
	private final SimpleUnitPanel m_unitsToPlace;
	
	/** Creates new PlacePanel */
	public PlacePanel(final GameData data, final MapPanel map, final TripleAFrame frame)
	{
		super(data, map, frame);
		m_undoableMovesPanel = new UndoablePlacementsPanel(data, this);
		m_unitsToPlace = new SimpleUnitPanel(map.getUIContext());
	}
	
	@Override
	public void display(final PlayerID id)
	{
		super.display(id, " place");
	}
	
	private void refreshActionLabelText(final boolean bid)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				actionLabel.setText(getCurrentPlayer().getName() + " place" + (bid ? " for bid" : ""));
			}
		});
	}
	
	public PlaceData waitForPlace(final boolean bid, final IPlayerBridge playerBridge)
	{
		setUp(playerBridge);
		refreshActionLabelText(bid); // workaround: meant to be in setUpSpecific, but it requires a variable
		waitForRelease();
		cleanUp();
		return m_placeData;
	}
	
	private boolean canProduceFightersOnCarriers()
	{
		return games.strategy.triplea.Properties.getProduce_Fighters_On_Carriers(getData());
	}
	
	private boolean canProduceNewFightersOnOldCarriers()
	{
		return games.strategy.triplea.Properties.getProduce_New_Fighters_On_Old_Carriers(getData());
	}
	
	private boolean isLHTR_Carrier_Production_Rules()
	{
		return games.strategy.triplea.Properties.getLHTR_Carrier_Production_Rules(getData());
	}
	
	private final MapSelectionListener PLACE_MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
	{
		@Override
		public void territorySelected(final Territory territory, final MouseDetails e)
		{
			if (!getActive() || (e.getButton() != MouseEvent.BUTTON1))
				return;
			final int maxUnits[] = new int[1];
			final Collection<Unit> units = getUnitsToPlace(territory, maxUnits);
			if (units.isEmpty())
				return;
			final UnitChooser chooser = new UnitChooser(units, Collections.<Unit, Collection<Unit>> emptyMap(), getData(), false, getMap().getUIContext());
			final String messageText = "Place units in " + territory.getName();
			if (maxUnits[0] > 0)
				chooser.setMaxAndShowMaxButton(maxUnits[0]);
			final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, messageText, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
			if (option == JOptionPane.OK_OPTION)
			{
				final Collection<Unit> choosen = chooser.getSelected();
				m_placeData = new PlaceData(choosen, territory);
				updateUnits();
				release();
			}
		}
	};
	
	private Collection<Unit> getUnitsToPlace(final Territory territory, final int maxUnits[])
	{
		getData().acquireReadLock();
		try
		{
			// not our territory
			if (!territory.isWater() && !territory.getOwner().equals(getCurrentPlayer()))
				return Collections.emptyList();
			// get the units that can be placed on this territory.
			Collection<Unit> units = getCurrentPlayer().getUnits().getUnits();
			if (territory.isWater())
			{
				if (!(canProduceFightersOnCarriers() || canProduceNewFightersOnOldCarriers() || isLHTR_Carrier_Production_Rules()))
					units = Match.getMatches(units, Matches.UnitIsSea);
				else
				{
					final CompositeMatch<Unit> unitIsSeaOrCanLandOnCarrier = new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.UnitCanLandOnCarrier);
					units = Match.getMatches(units, unitIsSeaOrCanLandOnCarrier);
				}
			}
			else
				units = Match.getMatches(units, Matches.UnitIsNotSea);
			if (units.isEmpty())
				return Collections.emptyList();
			final IAbstractPlaceDelegate placeDel = (IAbstractPlaceDelegate) getPlayerBridge().getRemote();
			final PlaceableUnits production = placeDel.getPlaceableUnits(units, territory);
			if (production.isError())
			{
				JOptionPane.showMessageDialog(getTopLevelAncestor(), production.getErrorMessage(), "No units", JOptionPane.INFORMATION_MESSAGE);
				return Collections.emptyList();
			}
			maxUnits[0] = production.getMaxUnits();
			return production.getUnits();
		} finally
		{
			getData().releaseReadLock();
		}
	}
	
	private void updateUnits()
	{
		final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(getCurrentPlayer().getUnits().getUnits());
		m_unitsToPlace.setUnitsFromCategories(unitCategories, getData());
	}
	
	@Override
	public String toString()
	{
		return "PlacePanel";
	}
	
	@Override
	protected final void cancelMoveAction()
	{
		// TODO Auto-generated method stub
		getMap().showMouseCursor();
		getMap().setMouseShadowUnits(null);
	}
	
	@Override
	protected final void undoMoveSpecific()
	{
		// TODO Auto-generated method stub
		updateUnits();
	}
	
	@Override
	protected final void cleanUpSpecific()
	{
		// TODO Auto-generated method stub
		getMap().removeMapSelectionListener(PLACE_MAP_SELECTION_LISTENER);
	}
	
	@Override
	protected final void setUpSpecific()
	{
		// TODO Auto-generated method stub
		getMap().addMapSelectionListener(PLACE_MAP_SELECTION_LISTENER);
	}
	
	@Override
	protected boolean doneMoveAction()
	{
		// TODO Auto-generated method stub
		if (getCurrentPlayer().getUnits().size() > 0)
		{
			final int option = JOptionPane.showConfirmDialog(getTopLevelAncestor(), "You have not placed all your units yet.  Are you sure you want to end your turn?", "TripleA",
						JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
			// TODO COMCO add code here to store the units until next time
			if (option != JOptionPane.YES_OPTION)
				return false;
		}
		m_placeData = null;
		return true;
	}
	
	@Override
	protected boolean setCancelButton()
	{
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	final protected void addAdditionalButtons()
	{
		add(leftBox(new JLabel("Units left to place:")));
		add(m_unitsToPlace);
		updateUnits();
	}
}
