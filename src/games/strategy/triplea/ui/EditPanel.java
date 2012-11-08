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
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.TechnologyDelegate;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Triple;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class EditPanel extends ActionPanel
{
	private static final long serialVersionUID = 5043639777373556106L;
	private TripleAFrame m_frame;
	private Action m_addUnitsAction;
	private Action m_delUnitsAction;
	private Action m_changePUsAction;
	private Action m_addTechAction;
	private Action m_removeTechAction;
	private Action m_changeUnitHitDamageAction;
	private Action m_changeUnitBombingDamageAction;
	private Action m_changeTerritoryOwnerAction;
	private Action m_currentAction = null;
	private JLabel m_actionLabel;
	private boolean m_active = false;
	private Point m_mouseSelectedPoint;
	private Point m_mouseCurrentPoint;
	// use a LinkedHashSet because we want to know the order
	private final Set<Unit> m_selectedUnits = new LinkedHashSet<Unit>();
	private Territory m_selectedTerritory = null;
	private Territory m_currentTerritory = null;
	
	public EditPanel(final GameData data, final MapPanel map, final TripleAFrame frame)
	{
		super(data, map);
		m_frame = frame;
		m_actionLabel = new JLabel();
		m_addUnitsAction = new AbstractAction("Add Units")
		{
			private static final long serialVersionUID = 2205085537962024476L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_currentAction = this;
				setWidgetActivation();
				// TODO: change cursor to select territory
				// continued in territorySelected() handler below
			}
		};
		m_delUnitsAction = new AbstractAction("Remove Selected Units")
		{
			private static final long serialVersionUID = 5127470604727907906L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_currentAction = this;
				setWidgetActivation();
				final List<Unit> allUnits = new ArrayList<Unit>(m_selectedTerritory.getUnits().getUnits());
				sortUnitsToRemove(allUnits, m_selectedTerritory);
				final MustMoveWithDetails mustMoveWithDetails = MoveValidator.getMustMoveWith(m_selectedTerritory, allUnits, new HashMap<Unit, Collection<Unit>>(), getData(), getCurrentPlayer());
				boolean mustChoose = false;
				if (m_selectedUnits.containsAll(allUnits))
				{
					mustChoose = false;
				}
				else
				{
					// if the unit choice is ambiguous then ask the user to clarify which units to remove
					// an ambiguous selection would be if the user selects 1 of 2 tanks, but
					// the tanks have different movement.
					final Set<UnitType> selectedUnitTypes = new HashSet<UnitType>();
					for (final Unit u : m_selectedUnits)
					{
						selectedUnitTypes.add(u.getType());
					}
					final List<Unit> allOfCorrectType = Match.getMatches(allUnits, new Match<Unit>()
					{
						@Override
						public boolean match(final Unit o)
						{
							return selectedUnitTypes.contains(o.getType());
						}
					});
					final int allCategories = UnitSeperator.categorize(allOfCorrectType, mustMoveWithDetails.getMustMoveWith(), true, true).size();
					final int selectedCategories = UnitSeperator.categorize(m_selectedUnits, mustMoveWithDetails.getMustMoveWith(), true, true).size();
					mustChoose = (allCategories != selectedCategories);
				}
				Collection<Unit> bestUnits;
				if (mustChoose)
				{
					final String chooserText = "Remove units from " + m_selectedTerritory + ":";
					final UnitChooser chooser = new UnitChooser(allUnits, m_selectedUnits, mustMoveWithDetails.getMustMoveWith(), true, false, getData(),
								/*allowTwoHit=*/false, getMap().getUIContext());
					final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, chooserText, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
					if (option != JOptionPane.OK_OPTION)
					{
						CANCEL_EDIT_ACTION.actionPerformed(null);
						return;
					}
					bestUnits = chooser.getSelected(true);
				}
				else
				{
					bestUnits = new ArrayList<Unit>(m_selectedUnits);
				}
				final String result = m_frame.getEditDelegate().removeUnits(m_selectedTerritory, bestUnits);
				if (result != null)
					JOptionPane.showMessageDialog(getTopLevelAncestor(), result, MyFormatter.pluralize("Could not remove unit", m_selectedUnits.size()), JOptionPane.ERROR_MESSAGE);
				CANCEL_EDIT_ACTION.actionPerformed(null);
			}
		};
		m_changeTerritoryOwnerAction = new AbstractAction("Change Territory Owner")
		{
			private static final long serialVersionUID = 8547635747553626362L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_currentAction = this;
				setWidgetActivation();
				// TODO: change cursor to select territory
				// continued in territorySelected() handler below
			}
		};
		m_changePUsAction = new AbstractAction("Change PUs")
		{
			private static final long serialVersionUID = -2751668909341983795L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_currentAction = this;
				setWidgetActivation();
				final PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), getMap().getUIContext(), false);
				final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select owner PUs to change");
				dialog.setVisible(true);
				
				final PlayerID player = playerChooser.getSelected();
				if (player == null)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				Resource PUs = null;
				getData().acquireReadLock();
				try
				{
					PUs = getData().getResourceList().getResource(Constants.PUS);
				} finally
				{
					getData().releaseReadLock();
				}
				if (PUs == null)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				final int oldTotal = player.getResources().getQuantity(PUs);
				int newTotal = oldTotal;
				final JTextField PUsField = new JTextField(String.valueOf(oldTotal), 4);
				PUsField.setMaximumSize(PUsField.getPreferredSize());
				final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), new JScrollPane(PUsField), "Select new number of PUs", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
							null,
								null, null);
				if (option != JOptionPane.OK_OPTION)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				try
				{
					newTotal = Integer.parseInt(PUsField.getText());
				} catch (final Exception e)
				{
				}
				final String result = m_frame.getEditDelegate().changePUs(player, newTotal);
				if (result != null)
					JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
				CANCEL_EDIT_ACTION.actionPerformed(null);
			}
		};
		m_addTechAction = new AbstractAction("Add Technology")
		{
			private static final long serialVersionUID = -5536151512828077755L;
			
			@SuppressWarnings("null")
			public void actionPerformed(final ActionEvent event)
			{
				m_currentAction = this;
				setWidgetActivation();
				final PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), getMap().getUIContext(), false);
				final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select player to get technology");
				dialog.setVisible(true);
				
				final PlayerID player = playerChooser.getSelected();
				if (player == null)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				Vector<TechAdvance> techs = null;
				getData().acquireReadLock();
				try
				{
					techs = new Vector<TechAdvance>(TechnologyDelegate.getAvailableTechs(player, data));
				} finally
				{
					getData().releaseReadLock();
				}
				if (techs == null || techs.isEmpty())
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				final JList techList = new JList(techs);
				techList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				techList.setLayoutOrientation(JList.VERTICAL);
				techList.setVisibleRowCount(10);
				final JScrollPane scroll = new JScrollPane(techList);
				final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Select tech to add", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
				if (option != JOptionPane.OK_OPTION)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				TechAdvance advance = null;
				try
				{
					advance = (TechAdvance) techList.getSelectedValue();
				} catch (final Exception e)
				{
				}
				final String result = m_frame.getEditDelegate().addTechAdvance(player, advance);
				if (result != null)
					JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
				CANCEL_EDIT_ACTION.actionPerformed(null);
			}
		};
		m_removeTechAction = new AbstractAction("Remove Technology")
		{
			private static final long serialVersionUID = -2456111915025687825L;
			
			@SuppressWarnings("null")
			public void actionPerformed(final ActionEvent event)
			{
				m_currentAction = this;
				setWidgetActivation();
				final PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), getMap().getUIContext(), false);
				final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select player to remove technology");
				dialog.setVisible(true);
				
				final PlayerID player = playerChooser.getSelected();
				if (player == null)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				Vector<TechAdvance> techs = null;
				getData().acquireReadLock();
				try
				{
					techs = new Vector<TechAdvance>(TechTracker.getCurrentTechAdvances(player, data));
					// there is no way to "undo" these two techs, so do not allow them to be removed
					final Iterator<TechAdvance> iter = techs.iterator();
					while (iter.hasNext())
					{
						final TechAdvance ta = iter.next();
						if (ta.getProperty().equals(TechAdvance.TECH_PROPERTY_IMPROVED_SHIPYARDS) || ta.getProperty().equals(TechAdvance.TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY))
							iter.remove();
					}
				} finally
				{
					getData().releaseReadLock();
				}
				if (techs == null || techs.isEmpty())
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				final JList techList = new JList(techs);
				techList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				techList.setLayoutOrientation(JList.VERTICAL);
				techList.setVisibleRowCount(10);
				final JScrollPane scroll = new JScrollPane(techList);
				final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Select tech to remove", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
				if (option != JOptionPane.OK_OPTION)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				TechAdvance advance = null;
				try
				{
					advance = (TechAdvance) techList.getSelectedValue();
				} catch (final Exception e)
				{
				}
				final String result = m_frame.getEditDelegate().removeTechAdvance(player, advance);
				if (result != null)
					JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
				CANCEL_EDIT_ACTION.actionPerformed(null);
			}
		};
		m_changeUnitHitDamageAction = new AbstractAction("Change Unit Hit Damage")
		{
			private static final long serialVersionUID = 1835547345902760810L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_currentAction = this;
				setWidgetActivation();
				final List<Unit> units = Match.getMatches(m_selectedUnits, Matches.UnitIsTwoHit);
				if (units == null || units.isEmpty() || !m_selectedTerritory.getUnits().getUnits().containsAll(units))
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				// all owned by one player
				units.retainAll(Match.getMatches(units, Matches.unitIsOwnedBy(units.iterator().next().getOwner())));
				if (units.isEmpty())
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				sortUnitsToRemove(units, m_selectedTerritory);
				Collections.sort(units, new UnitBattleComparator(false, BattleCalculator.getCostsForTuvForAllPlayersMergedAndAveraged(getData()), getData(), false));
				Collections.reverse(units);
				// unit mapped to <max, min, current>
				final HashMap<Unit, Triple<Integer, Integer, Integer>> currentDamageMap = new HashMap<Unit, Triple<Integer, Integer, Integer>>();
				for (final Unit u : units)
				{
					currentDamageMap.put(u, new Triple<Integer, Integer, Integer>(1, 0, u.getHits()));
				}
				final IndividualUnitPanel unitPanel = new IndividualUnitPanel(currentDamageMap, "Change Unit Hit Damage", getData(), getMap().getUIContext(), -1, true, true, null);
				final JScrollPane scroll = new JScrollPane(unitPanel);
				final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Change Unit Hit Damage", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
				if (option != JOptionPane.OK_OPTION)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				final IntegerMap<Unit> newDamageMap = unitPanel.getSelected();
				final String result = m_frame.getEditDelegate().changeUnitHitDamage(newDamageMap, m_selectedTerritory);
				if (result != null)
					JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
				CANCEL_EDIT_ACTION.actionPerformed(null);
			}
		};
		m_changeUnitBombingDamageAction = new AbstractAction("Change Unit Bombing Damage")
		{
			private static final long serialVersionUID = 6975869192911780860L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_currentAction = this;
				setWidgetActivation();
				final List<Unit> units = Match.getMatches(m_selectedUnits, Matches.UnitCanBeDamaged);
				if (units == null || units.isEmpty() || !m_selectedTerritory.getUnits().getUnits().containsAll(units))
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				// all owned by one player
				units.retainAll(Match.getMatches(units, Matches.unitIsOwnedBy(units.iterator().next().getOwner())));
				if (units.isEmpty())
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				sortUnitsToRemove(units, m_selectedTerritory);
				Collections.sort(units, new UnitBattleComparator(false, BattleCalculator.getCostsForTuvForAllPlayersMergedAndAveraged(getData()), getData(), false));
				Collections.reverse(units);
				final boolean damageToTerritories = games.strategy.triplea.Properties.getSBRAffectsUnitProduction(getData());
				final TerritoryAttachment ta;
				final int currentDamage;
				if (damageToTerritories && m_selectedTerritory != null)
				{
					ta = TerritoryAttachment.get(m_selectedTerritory);
					currentDamage = ta == null ? 0 : ta.getProduction() - ta.getUnitProduction();
				}
				else
				{
					ta = null;
					currentDamage = 0;
				}
				// unit mapped to <max, min, current>
				final HashMap<Unit, Triple<Integer, Integer, Integer>> currentDamageMap = new HashMap<Unit, Triple<Integer, Integer, Integer>>();
				for (final Unit u : units)
				{
					currentDamageMap.put(u,
								new Triple<Integer, Integer, Integer>(
											((TripleAUnit) u).getHowMuchDamageCanThisUnitTakeTotal(u, m_selectedTerritory),
											0, (damageToTerritories ? currentDamage : ((TripleAUnit) u).getUnitDamage())));
				}
				final IndividualUnitPanel unitPanel = new IndividualUnitPanel(currentDamageMap, "Change Unit Bombing Damage", getData(), getMap().getUIContext(), -1, true, true, null);
				final JScrollPane scroll = new JScrollPane(unitPanel);
				final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), scroll, "Change Unit Bombing Damage", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
				if (option != JOptionPane.OK_OPTION)
				{
					CANCEL_EDIT_ACTION.actionPerformed(null);
					return;
				}
				final IntegerMap<Unit> newDamageMap = unitPanel.getSelected();
				final String result = m_frame.getEditDelegate().changeUnitBombingDamage(newDamageMap, m_selectedTerritory);
				if (result != null)
					JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
				CANCEL_EDIT_ACTION.actionPerformed(null);
			}
		};
		m_actionLabel.setText("Edit Mode Actions");
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(new EmptyBorder(5, 5, 0, 0));
		add(m_actionLabel);
		add(new JButton(m_addUnitsAction));
		add(new JButton(m_delUnitsAction));
		add(new JButton(m_changeTerritoryOwnerAction));
		add(new JButton(m_changePUsAction));
		if (games.strategy.triplea.Properties.getTechDevelopment(getData()))
		{
			add(new JButton(m_addTechAction));
			add(new JButton(m_removeTechAction));
		}
		data.acquireReadLock();
		try
		{
			final Set<UnitType> allUnitTypes = data.getUnitTypeList().getAllUnitTypes();
			if (Match.someMatch(allUnitTypes, Matches.UnitTypeIsTwoHit))
				add(new JButton(m_changeUnitHitDamageAction));
			if ((games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data) || games.strategy.triplea.Properties.getSBRAffectsUnitProduction(data))
						&& Match.someMatch(allUnitTypes, Matches.UnitTypeCanBeDamaged))
				add(new JButton(m_changeUnitBombingDamageAction));
		} finally
		{
			data.releaseReadLock();
		}
		add(Box.createVerticalStrut(15));
		setWidgetActivation();
	}
	
	private void sortUnitsToRemove(final List<Unit> units, /*final MustMoveWithDetails mustMoveWith,*/final Territory territory)
	{
		if (units.isEmpty())
			return;
		// sort units based on which transports are allowed to unload
		Collections.sort(units, getRemovableUnitsOrder(units, /*mustMoveWith,*/territory, true));
	}
	
	public static Comparator<Unit> getRemovableUnitsOrder(final List<Unit> units, final Territory territory, final boolean noTies)
	{
		final Comparator<Unit> removableUnitsOrder = new Comparator<Unit>()
		{
			public int compare(final Unit unit1, final Unit unit2)
			{
				final TripleAUnit u1 = TripleAUnit.get(unit1);
				final TripleAUnit u2 = TripleAUnit.get(unit2);
				if (UnitAttachment.get(u1.getType()).getTransportCapacity() != -1)
				{
					// sort transports
					Collection<Unit> transporting1 = u1.getTransporting();
					Collection<Unit> transporting2 = u2.getTransporting();
					if (transporting1 == null)
						transporting1 = Collections.emptyList();
					if (transporting2 == null)
						transporting2 = Collections.emptyList();
					// sort by decreasing transport capacity
					final int cost1 = MoveValidator.getTransportCost(transporting1);
					final int cost2 = MoveValidator.getTransportCost(transporting2);
					if (cost1 != cost2)
						return cost2 - cost1;
				}
				// sort by increasing movement left
				final int left1 = u1.getMovementLeft();
				final int left2 = u2.getMovementLeft();
				if (left1 != left2)
					return left1 - left2;
				// if noTies is set, sort by hashcode so that result is deterministic
				if (noTies)
					return u1.hashCode() - u2.hashCode();
				else
					return 0;
			}
		};
		return removableUnitsOrder;
	}
	
	private void setWidgetActivation()
	{
		if (m_frame.getEditDelegate() == null)
		{
			// current turn belongs to remote player or AI player
			m_addUnitsAction.setEnabled(false);
			m_delUnitsAction.setEnabled(false);
			m_changeTerritoryOwnerAction.setEnabled(false);
			m_changePUsAction.setEnabled(false);
			m_addTechAction.setEnabled(false);
			m_removeTechAction.setEnabled(false);
			m_changeUnitHitDamageAction.setEnabled(false);
			m_changeUnitBombingDamageAction.setEnabled(false);
		}
		else
		{
			m_addUnitsAction.setEnabled(m_currentAction == null && m_selectedUnits.isEmpty());
			m_delUnitsAction.setEnabled(!m_selectedUnits.isEmpty());
			m_changeTerritoryOwnerAction.setEnabled(m_currentAction == null && m_selectedUnits.isEmpty());
			m_changePUsAction.setEnabled(m_currentAction == null && m_selectedUnits.isEmpty());
			m_addTechAction.setEnabled(m_currentAction == null && m_selectedUnits.isEmpty());
			m_removeTechAction.setEnabled(m_currentAction == null && m_selectedUnits.isEmpty());
			m_changeUnitHitDamageAction.setEnabled(!m_selectedUnits.isEmpty());
			m_changeUnitBombingDamageAction.setEnabled(!m_selectedUnits.isEmpty());
		}
	}
	
	@Override
	public String toString()
	{
		return "EditPanel";
	}
	
	@Override
	public void setActive(final boolean active)
	{
		if (m_frame.getEditDelegate() == null)
		{
			// current turn belongs to remote player or AI player
			getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
			getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
			getMap().removeMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
			setWidgetActivation();
		}
		else if (!m_active && active)
		{
			getMap().addMapSelectionListener(MAP_SELECTION_LISTENER);
			getMap().addUnitSelectionListener(UNIT_SELECTION_LISTENER);
			getMap().addMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
			setWidgetActivation();
		}
		else if (!active && m_active)
		{
			getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
			getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
			getMap().removeMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
			CANCEL_EDIT_ACTION.actionPerformed(null);
		}
		m_active = active;
	}
	
	@Override
	public boolean getActive()
	{
		return m_active;
	}
	
	private final UnitSelectionListener UNIT_SELECTION_LISTENER = new UnitSelectionListener()
	{
		public void unitsSelected(final List<Unit> units, final Territory t, final MouseDetails md)
		{
			// check if we can handle this event, are we active?
			if (!getActive())
				return;
			if (t == null)
				return;
			if (m_currentAction != null)
				return;
			final boolean rightMouse = md.isRightButton();
			if (!m_selectedUnits.isEmpty() && !(m_selectedTerritory == t))
			{
				deselectUnits(new ArrayList<Unit>(m_selectedUnits), t, md);
				m_selectedTerritory = null;
			}
			if (rightMouse && (m_selectedTerritory == t))
			{
				deselectUnits(units, t, md);
			}
			if (!rightMouse && (m_currentAction == m_addUnitsAction))
			{
				// clicking on unit or territory selects territory
				m_selectedTerritory = t;
				MAP_SELECTION_LISTENER.territorySelected(t, md);
			}
			else if (!rightMouse)
			{
				// delete units
				selectUnitsToRemove(units, t, md);
			}
			setWidgetActivation();
		}
		
		private void deselectUnits(final List<Unit> units, final Territory t, final MouseDetails md)
		{
			// no unit selected, deselect the most recent
			if (units.isEmpty())
			{
				if (md.isControlDown() || t != m_selectedTerritory || m_selectedUnits.isEmpty())
					m_selectedUnits.clear();
				else
					// remove the last element
					m_selectedUnits.remove(new ArrayList<Unit>(m_selectedUnits).get(m_selectedUnits.size() - 1));
			}
			// user has clicked on a specific unit
			else
			{
				// deselect all if control is down
				if (md.isControlDown() || t != m_selectedTerritory)
				{
					m_selectedUnits.removeAll(units);
				}
				// deselect one
				else
				{
					// remove those with the least movement first
					for (final Unit unit : units)
					{
						if (m_selectedUnits.contains(unit))
						{
							m_selectedUnits.remove(unit);
							break;
						}
					}
				}
			}
			// nothing left, cancel edit
			if (m_selectedUnits.isEmpty())
				CANCEL_EDIT_ACTION.actionPerformed(null);
			else
				getMap().setMouseShadowUnits(m_selectedUnits);
		}
		
		private void selectUnitsToRemove(final List<Unit> units, final Territory t, final MouseDetails md)
		{
			if (units.isEmpty() && m_selectedUnits.isEmpty())
			{
				if (!md.isShiftDown())
				{
					final Collection<Unit> unitsToMove = t.getUnits().getUnits();
					if (unitsToMove.isEmpty())
						return;
					final String text = "Remove from " + t.getName();
					final UnitChooser chooser = new UnitChooser(unitsToMove, m_selectedUnits, null, false, false, getData(), false, getMap().getUIContext());
					final int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, text, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
					if (option != JOptionPane.OK_OPTION)
						return;
					if (chooser.getSelected(false).isEmpty())
						return;
					m_selectedUnits.addAll(chooser.getSelected(false));
				}
			}
			if (m_selectedTerritory == null)
			{
				m_selectedTerritory = t;
				m_mouseSelectedPoint = md.getMapPoint();
				m_mouseCurrentPoint = md.getMapPoint();
				CANCEL_EDIT_ACTION.setEnabled(true);
			}
			// select all
			if (md.isShiftDown())
			{
				m_selectedUnits.addAll(t.getUnits().getUnits());
			}
			else if (md.isControlDown())
			{
				m_selectedUnits.addAll(units);
			}
			// select one
			else
			{
				for (final Unit unit : units)
				{
					if (!m_selectedUnits.contains(unit))
					{
						m_selectedUnits.add(unit);
						break;
					}
				}
			}
			final Route defaultRoute = getData().getMap().getRoute(m_selectedTerritory, m_selectedTerritory);
			getMap().setRoute(defaultRoute, m_mouseSelectedPoint, m_mouseCurrentPoint, null);
			getMap().setMouseShadowUnits(m_selectedUnits);
		}
	};
	private final MouseOverUnitListener MOUSE_OVER_UNIT_LISTENER = new MouseOverUnitListener()
	{
		public void mouseEnter(final List<Unit> units, final Territory territory, final MouseDetails md)
		{
			if (!getActive())
				return;
			if (m_currentAction != null)
				return;
			if (!units.isEmpty())
				getMap().setUnitHighlight(units, territory);
			else
				getMap().setUnitHighlight(null, null);
		}
	};
	private final MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
	{
		@Override
		public void territorySelected(final Territory territory, final MouseDetails md)
		{
			if (territory == null)
				return;
			if (m_currentAction == m_changeTerritoryOwnerAction)
			{
				final TerritoryAttachment ta = TerritoryAttachment.get(territory);
				if (ta == null)
				{
					JOptionPane.showMessageDialog(getTopLevelAncestor(), "No TerritoryAttachment for " + territory + ".", "Could not perform edit", JOptionPane.ERROR_MESSAGE);
					return;
				}
				// PlayerID defaultPlayer = TerritoryAttachment.get(territory).getOriginalOwner();
				final PlayerID defaultPlayer = ta.getOriginalOwner();
				final PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), defaultPlayer, getMap().getUIContext(), true);
				final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select new owner for territory");
				dialog.setVisible(true);
				
				final PlayerID player = playerChooser.getSelected();
				if (player != null)
				{
					final String result = m_frame.getEditDelegate().changeTerritoryOwner(territory, player);
					if (result != null)
						JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
				}
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						CANCEL_EDIT_ACTION.actionPerformed(null);
					}
				});
			}
			else if (m_currentAction == m_addUnitsAction)
			{
				final boolean allowNeutral = doesPlayerHaveUnitsOnMap(PlayerID.NULL_PLAYERID, getData());
				final PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), territory.getOwner(), getMap().getUIContext(), allowNeutral);
				final JDialog dialog = playerChooser.createDialog(getTopLevelAncestor(), "Select owner for new units");
				dialog.setVisible(true);
				
				final PlayerID player = playerChooser.getSelected();
				if (player != null)
				{
					// open production panel for adding new units
					final IntegerMap<ProductionRule> production = EditProductionPanel.getProduction(player, m_frame, getData(), getMap().getUIContext());
					final Collection<Unit> units = new ArrayList<Unit>();
					for (final ProductionRule productionRule : production.keySet())
					{
						final int quantity = production.getInt(productionRule);
						final UnitType type = (UnitType) productionRule.getResults().keySet().iterator().next();
						units.addAll(type.create(quantity, player));
					}
					final String result = m_frame.getEditDelegate().addUnits(territory, units);
					if (result != null)
						JOptionPane.showMessageDialog(getTopLevelAncestor(), result, "Could not perform edit", JOptionPane.ERROR_MESSAGE);
				}
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						CANCEL_EDIT_ACTION.actionPerformed(null);
					}
				});
			}
		}
		
		@Override
		public void mouseMoved(final Territory territory, final MouseDetails md)
		{
			if (!getActive())
				return;
			if (territory != null)
			{
				if (m_currentAction == null && m_selectedTerritory != null)
				{
					m_mouseCurrentPoint = md.getMapPoint();
					getMap().setMouseShadowUnits(m_selectedUnits);
				}
				// highlight territory
				if (m_currentAction == m_changeTerritoryOwnerAction || m_currentAction == m_addUnitsAction)
				{
					if (m_currentTerritory != territory)
					{
						if (m_currentTerritory != null)
							getMap().clearTerritoryOverlay(m_currentTerritory);
						m_currentTerritory = territory;
						getMap().setTerritoryOverlay(m_currentTerritory, Color.WHITE, 200);
						getMap().repaint();
					}
				}
			}
		}
	};
	private final AbstractAction CANCEL_EDIT_ACTION = new AbstractAction("Cancel")
	{
		private static final long serialVersionUID = 6394987295241603443L;
		
		public void actionPerformed(final ActionEvent e)
		{
			m_selectedTerritory = null;
			m_selectedUnits.clear();
			this.setEnabled(false);
			getMap().setRoute(null, m_mouseSelectedPoint, m_mouseCurrentPoint, null);
			getMap().setMouseShadowUnits(null);
			if (m_currentTerritory != null)
				getMap().clearTerritoryOverlay(m_currentTerritory);
			m_currentTerritory = null;
			m_currentAction = null;
			setWidgetActivation();
		}
	};
	
	private static boolean doesPlayerHaveUnitsOnMap(final PlayerID player, final GameData data)
	{
		for (final Territory t : data.getMap())
		{
			for (final Unit u : t.getUnits())
			{
				if (u.getOwner().equals(player))
					return true;
			}
		}
		return false;
	}
}
