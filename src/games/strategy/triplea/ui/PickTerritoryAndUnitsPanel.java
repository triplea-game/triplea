package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * For choosing territories and units for them, during RandomStartDelegate.
 * 
 * @author veqryn
 * 
 */
public class PickTerritoryAndUnitsPanel extends ActionPanel
{
	private static final long serialVersionUID = -2672163347536778594L;
	private final TripleAFrame m_parent;
	private final JLabel m_actionLabel = new JLabel();
	private JButton m_doneButton = null;
	private JButton m_selectTerritoryButton = null;
	private JButton m_selectUnitsButton = null;
	private Territory m_pickedTerritory = null;
	private Set<Unit> m_pickedUnits = new HashSet<Unit>();
	private List<Territory> m_territoryChoices = null;
	private List<Unit> m_unitChoices = null;
	private int m_unitsPerPick = 1;
	private Action m_currentAction = null;
	private Territory m_currentHighlightedTerritory = null;
	
	public PickTerritoryAndUnitsPanel(final GameData data, final MapPanel map, final TripleAFrame parent)
	{
		super(data, map);
		m_parent = parent;
	}
	
	@Override
	public String toString()
	{
		return "Pick Territory and Units";
	}
	
	@Override
	public void display(final PlayerID id)
	{
		super.display(id);
		m_pickedTerritory = null;
		m_pickedUnits = new HashSet<Unit>();
		m_currentAction = null;
		m_currentHighlightedTerritory = null;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				removeAll();
				m_actionLabel.setText(id.getName() + " Pick Territory and Units");
				add(m_actionLabel);
				m_selectTerritoryButton = new JButton(SelectTerritoryAction);
				add(m_selectTerritoryButton);
				m_selectUnitsButton = new JButton(SelectUnitsAction);
				add(m_selectUnitsButton);
				m_doneButton = new JButton(DoneAction);
				add(m_doneButton);
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						m_selectTerritoryButton.requestFocusInWindow();
					}
				});
			}
		});
	}
	
	public Tuple<Territory, Set<Unit>> waitForPickTerritoryAndUnits(final List<Territory> territoryChoices, final List<Unit> unitChoices, final int unitsPerPick)
	{
		m_territoryChoices = territoryChoices;
		m_unitChoices = unitChoices;
		m_unitsPerPick = unitsPerPick;
		if (m_currentHighlightedTerritory != null)
		{
			getMap().clearTerritoryOverlay(m_currentHighlightedTerritory);
			m_currentHighlightedTerritory = null;
		}
		if (territoryChoices.size() == 1)
		{
			m_pickedTerritory = territoryChoices.get(0);
			m_currentHighlightedTerritory = m_pickedTerritory;
			getMap().setTerritoryOverlay(m_currentHighlightedTerritory, Color.WHITE, 200);
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (territoryChoices.size() > 1)
					SelectTerritoryAction.actionPerformed(null);
				else if (unitChoices.size() > 1)
					SelectUnitsAction.actionPerformed(null);
			}
		});
		waitForRelease();
		return new Tuple<Territory, Set<Unit>>(this.m_pickedTerritory, this.m_pickedUnits);
	}
	
	private void setWidgetActivation()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				if (!getActive())
				{
					// current turn belongs to remote player or AI player
					DoneAction.setEnabled(false);
					SelectUnitsAction.setEnabled(false);
					SelectTerritoryAction.setEnabled(false);
				}
				else
				{
					DoneAction.setEnabled(m_currentAction == null);
					SelectUnitsAction.setEnabled(m_currentAction == null);
					SelectTerritoryAction.setEnabled(m_currentAction == null);
				}
			}
		});
	}
	
	private final Action DoneAction = new AbstractAction("Done")
	{
		private static final long serialVersionUID = -2376988913511268803L;
		
		public void actionPerformed(final ActionEvent event)
		{
			m_currentAction = DoneAction;
			setWidgetActivation();
			if (m_pickedTerritory == null || !m_territoryChoices.contains(m_pickedTerritory))
			{
				EventThreadJOptionPane.showMessageDialog(m_parent, "Must Pick An Unowned Territory", "Must Pick An Unowned Territory", JOptionPane.WARNING_MESSAGE, new CountDownLatchHandler(true));
				m_currentAction = null;
				if (m_currentHighlightedTerritory != null)
					getMap().clearTerritoryOverlay(m_currentHighlightedTerritory);
				m_currentHighlightedTerritory = null;
				m_pickedTerritory = null;
				setWidgetActivation();
				return;
			}
			if (!m_pickedUnits.isEmpty() && !m_unitChoices.containsAll(m_pickedUnits))
			{
				EventThreadJOptionPane.showMessageDialog(m_parent, "Invalid Units?!?", "Invalid Units?!?", JOptionPane.WARNING_MESSAGE, new CountDownLatchHandler(true));
				m_currentAction = null;
				m_pickedUnits.clear();
				setWidgetActivation();
				return;
			}
			if (m_pickedUnits.size() > Math.max(0, m_unitsPerPick))
			{
				EventThreadJOptionPane.showMessageDialog(m_parent, "Too Many Units?!?", "Too Many Units?!?", JOptionPane.WARNING_MESSAGE, new CountDownLatchHandler(true));
				m_currentAction = null;
				m_pickedUnits.clear();
				setWidgetActivation();
				return;
			}
			if (m_pickedUnits.size() < m_unitsPerPick)
			{
				if (m_unitChoices.size() < m_unitsPerPick)
				{
					// if we have fewer units than the number we are supposed to pick, set it to all
					m_pickedUnits.addAll(m_unitChoices);
				}
				else if (Match.allMatch(m_unitChoices, Matches.unitIsOfType(m_unitChoices.get(0).getType())))
				{
					// if we have only 1 unit type, set it to that
					m_pickedUnits.clear();
					m_pickedUnits.addAll(Match.getNMatches(m_unitChoices, m_unitsPerPick, Match.<Unit> getAlwaysMatch()));
				}
				else
				{
					EventThreadJOptionPane.showMessageDialog(m_parent, "Must Choose Units For This Territory", "Must Choose Units For This Territory", JOptionPane.WARNING_MESSAGE,
								new CountDownLatchHandler(true));
					m_currentAction = null;
					setWidgetActivation();
					return;
				}
			}
			m_currentAction = null;
			if (m_currentHighlightedTerritory != null)
				getMap().clearTerritoryOverlay(m_currentHighlightedTerritory);
			m_currentHighlightedTerritory = null;
			setWidgetActivation();
			release();
		}
	};
	
	private final Action SelectUnitsAction = new AbstractAction("Select Units")
	{
		private static final long serialVersionUID = 4745335350716395600L;
		
		public void actionPerformed(final ActionEvent event)
		{
			m_currentAction = SelectUnitsAction;
			setWidgetActivation();
			final UnitChooser unitChooser = new UnitChooser(m_unitChoices, Collections.<Unit, Collection<Unit>> emptyMap(), getData(), false, getMap().getUIContext());
			unitChooser.setMaxAndShowMaxButton(m_unitsPerPick);
			if (JOptionPane.OK_OPTION == EventThreadJOptionPane.showConfirmDialog(m_parent, unitChooser, "Select Units", JOptionPane.OK_CANCEL_OPTION, new CountDownLatchHandler(true)))
			{
				m_pickedUnits.clear();
				m_pickedUnits.addAll(unitChooser.getSelected());
			}
			m_currentAction = null;
			setWidgetActivation();
		}
	};
	
	private final Action SelectTerritoryAction = new AbstractAction("Select Territory")
	{
		private static final long serialVersionUID = -8003634505955439651L;
		
		public void actionPerformed(final ActionEvent event)
		{
			m_currentAction = SelectTerritoryAction;
			setWidgetActivation();
			getMap().addMapSelectionListener(MAP_SELECTION_LISTENER);
		}
	};
	private final MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
	{
		@Override
		public void territorySelected(final Territory territory, final MouseDetails md)
		{
			if (territory == null)
				return;
			if (m_currentAction == SelectTerritoryAction)
			{
				if (territory == null || !m_territoryChoices.contains(territory))
				{
					EventThreadJOptionPane.showMessageDialog(m_parent, "Must Pick An Unowned Territory (will have a white highlight)", "Must Pick An Unowned Territory", JOptionPane.WARNING_MESSAGE,
								new CountDownLatchHandler(true));
					return;
				}
				m_pickedTerritory = territory;
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
						m_currentAction = null;
						setWidgetActivation();
					}
				});
			}
			else
			{
				System.err.println("Should not be able to select a territory outside of the SelectTerritoryAction.");
			}
		}
		
		@Override
		public void mouseMoved(final Territory territory, final MouseDetails md)
		{
			if (!getActive())
			{
				System.err.println("Should not be able to select a territory when inactive.");
				return;
			}
			if (territory != null)
			{
				// highlight territory
				if (m_currentAction == SelectTerritoryAction)
				{
					if (m_currentHighlightedTerritory != territory)
					{
						if (m_currentHighlightedTerritory != null)
							getMap().clearTerritoryOverlay(m_currentHighlightedTerritory);
						m_currentHighlightedTerritory = territory;
						if (m_territoryChoices.contains(m_currentHighlightedTerritory))
							getMap().setTerritoryOverlay(m_currentHighlightedTerritory, Color.WHITE, 200);
						else
							getMap().setTerritoryOverlay(m_currentHighlightedTerritory, Color.RED, 200);
						getMap().repaint();
					}
				}
			}
		}
	};
}
