package games.strategy.triplea.oddsCalculator.ta;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.ui.background.WaitDialog;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.IntTextField;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.ui.WidgetChangedListener;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.ListenerList;
import games.strategy.util.Match;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class OddsCalculatorPanel extends JPanel
{
	private static final long serialVersionUID = -3559687618320469183L;
	private static final String NO_EFFECTS = "*None*";
	private final Window m_parent;
	private final JLabel m_attackerWin = new JLabel();
	private final JLabel m_defenderWin = new JLabel();
	private final JLabel m_draw = new JLabel();
	private final JLabel m_defenderLeft = new JLabel();
	private final JLabel m_attackerLeft = new JLabel();
	private final JLabel m_defenderLeftWhenDefenderWon = new JLabel();
	private final JLabel m_attackerLeftWhenAttackerWon = new JLabel();
	private final JLabel m_averageChangeInTUV = new JLabel();
	private final JLabel m_roundsAverage = new JLabel();
	private final JLabel m_count = new JLabel();
	private final JLabel m_time = new JLabel();
	private final IntTextField m_numRuns = new games.strategy.ui.IntTextField();
	private final IntTextField m_retreatAfterXRounds = new games.strategy.ui.IntTextField();
	private final IntTextField m_retreatAfterXUnitsLeft = new games.strategy.ui.IntTextField();
	private final JPanel m_resultsPanel = new JPanel();
	private final JButton m_calculateButton = new JButton("Calculate Odds");
	private final JButton m_clearButton = new JButton("Clear");
	private final JButton m_closeButton = new JButton("Close");
	private final JButton m_SwapSidesButton = new JButton("Swap Sides");
	private final JButton m_orderOfLossesButton = new JButton("Order Of Losses");
	private final JCheckBox m_keepOneAttackingLandUnitCheckBox = new JCheckBox("One attacking land must live");
	private final JCheckBox m_amphibiousCheckBox = new JCheckBox("Battle is Amphibious");
	private final JCheckBox m_landBattleCheckBox = new JCheckBox("Land Battle");
	private final JCheckBox m_retreatWhenOnlyAirLeftCheckBox = new JCheckBox("Retreat when only air left");
	private final JCheckBox m_retreatWhenMetaPowerIsLower = new JCheckBox("Retreat when meta-power is lower");
	private final IUIContext m_context;
	private final GameData m_data;
	private PlayerUnitsPanel m_attackingUnitsPanel;
	private PlayerUnitsPanel m_defendingUnitsPanel;
	private JComboBox m_attackerCombo;
	private JComboBox m_defenderCombo;
	private JComboBox m_SwapSidesCombo;
	private final JLabel m_attackerUnitsTotalNumber = new JLabel();
	private final JLabel m_defenderUnitsTotalNumber = new JLabel();
	private final JLabel m_attackerUnitsTotalTUV = new JLabel();
	private final JLabel m_defenderUnitsTotalTUV = new JLabel();
	private final JLabel m_attackerUnitsTotalHitpoints = new JLabel();
	private final JLabel m_defenderUnitsTotalHitpoints = new JLabel();
	private final JLabel m_attackerUnitsTotalPower = new JLabel();
	private final JLabel m_defenderUnitsTotalPower = new JLabel();
	private String m_attackerOrderOfLosses = null;
	private String m_defenderOrderOfLosses = null;
	private Territory m_location = null;
	private JList m_territoryEffectsJList;
	private final WidgetChangedListener m_listenerPlayerUnitsPanel = new WidgetChangedListener()
	{
		public void widgetChanged()
		{
			setWidgetActivation();
		}
	};
	
	public OddsCalculatorPanel(final GameData data, final IUIContext context, final Territory location, final Window parent)
	{
		m_data = data;
		m_context = context;
		m_location = location;
		m_parent = parent;
		createComponents();
		layoutComponents();
		setupListeners();
		// use the one passed, not the one we found:
		if (location != null)
		{
			m_data.acquireReadLock();
			try
			{
				m_landBattleCheckBox.setSelected(!location.isWater());
				// default to the current player
				if (m_data.getSequence().getStep().getPlayerID() != null && !m_data.getSequence().getStep().getPlayerID().isNull())
				{
					m_attackerCombo.setSelectedItem(m_data.getSequence().getStep().getPlayerID());
				}
				if (!location.isWater())
				{
					m_defenderCombo.setSelectedItem(location.getOwner());
				}
				else
				{
					// we need to find out the defender for sea zones
					for (final PlayerID player : location.getUnits().getPlayersWithUnits())
					{
						if (player != getAttacker() && !m_data.getRelationshipTracker().isAllied(player, getAttacker()))
						{
							m_defenderCombo.setSelectedItem(player);
							break;
						}
					}
				}
				updateDefender(location.getUnits().getMatches(Matches.alliedUnit(getDefender(), data)));
				updateAttacker(location.getUnits().getMatches(Matches.alliedUnit(getAttacker(), data)));
			} finally
			{
				m_data.releaseReadLock();
			}
		}
		else
		{
			m_landBattleCheckBox.setSelected(true);
			m_defenderCombo.setSelectedItem(data.getPlayerList().getPlayers().iterator().next());
			updateDefender(null);
			updateAttacker(null);
		}
		setWidgetActivation();
		revalidate();
	}
	
	private PlayerID getDefender()
	{
		return (PlayerID) m_defenderCombo.getSelectedItem();
	}
	
	private PlayerID getAttacker()
	{
		return (PlayerID) m_attackerCombo.getSelectedItem();
	}
	
	private PlayerID getSwapSides()
	{
		return (PlayerID) m_SwapSidesCombo.getSelectedItem();
	}
	
	private void setupListeners()
	{
		m_defenderCombo.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_data.acquireReadLock();
				try
				{
					if (m_data.getRelationshipTracker().isAllied(getDefender(), getAttacker()))
					{
						m_attackerCombo.setSelectedItem(getEnemy(getDefender()));
					}
				} finally
				{
					m_data.releaseReadLock();
				}
				updateDefender(null);
				setWidgetActivation();
			}
		});
		m_attackerCombo.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_data.acquireReadLock();
				try
				{
					if (m_data.getRelationshipTracker().isAllied(getDefender(), getAttacker()))
					{
						m_defenderCombo.setSelectedItem(getEnemy(getAttacker()));
					}
				} finally
				{
					m_data.releaseReadLock();
				}
				updateAttacker(null);
				setWidgetActivation();
			}
		});
		m_amphibiousCheckBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				setWidgetActivation();
			}
		});
		m_landBattleCheckBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_attackerOrderOfLosses = null;
				m_defenderOrderOfLosses = null;
				updateDefender(null);
				updateAttacker(null);
				setWidgetActivation();
			}
		});
		m_calculateButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				updateStats();
			}
		});
		m_closeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_attackerOrderOfLosses = null;
				m_defenderOrderOfLosses = null;
				m_parent.setVisible(false);
			}
		});
		m_clearButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_defendingUnitsPanel.clear();
				m_attackingUnitsPanel.clear();
				setWidgetActivation();
			}
		});
		m_SwapSidesButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_attackerOrderOfLosses = null;
				m_defenderOrderOfLosses = null;
				List<Unit> getdefenders = new ArrayList<Unit>();
				List<Unit> getattackers = new ArrayList<Unit>();
				getdefenders = m_defendingUnitsPanel.getUnits();
				getattackers = m_attackingUnitsPanel.getUnits();
				m_SwapSidesCombo.setSelectedItem(getAttacker());
				m_attackerCombo.setSelectedItem(getDefender());
				m_defenderCombo.setSelectedItem(getSwapSides());
				m_attackingUnitsPanel.init(getAttacker(), getdefenders, isLand());
				m_defendingUnitsPanel.init(getDefender(), getattackers, isLand());
				setWidgetActivation();
			}
		});
		m_orderOfLossesButton.addActionListener(new ActionListener()
		{
			
			public void actionPerformed(final ActionEvent e)
			{
				final OrderOfLossesInputPanel oolPanel = new OrderOfLossesInputPanel(m_attackerOrderOfLosses, m_defenderOrderOfLosses, m_attackingUnitsPanel.getCategories(),
							m_defendingUnitsPanel.getCategories(), m_landBattleCheckBox.isSelected(), m_context, m_data);
				if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(OddsCalculatorPanel.this, oolPanel, "Create Order Of Losses for each side", JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE))
				{
					if (OddsCalculator.isValidOOL(oolPanel.getAttackerOrder(), m_data))
						m_attackerOrderOfLosses = oolPanel.getAttackerOrder();
					if (OddsCalculator.isValidOOL(oolPanel.getDefenderOrder(), m_data))
						m_defenderOrderOfLosses = oolPanel.getDefenderOrder();
				}
			}
		});
		if (m_territoryEffectsJList != null)
		{
			m_territoryEffectsJList.addListSelectionListener(new ListSelectionListener()
			{
				public void valueChanged(final ListSelectionEvent e)
				{
					setWidgetActivation();
				}
			});
		}
		m_attackingUnitsPanel.addChangeListener(m_listenerPlayerUnitsPanel);
		m_defendingUnitsPanel.addChangeListener(m_listenerPlayerUnitsPanel);
	}
	
	private boolean isAmphibiousBattle()
	{
		return (m_landBattleCheckBox.isSelected() && m_amphibiousCheckBox.isSelected());
	}
	
	private Collection<TerritoryEffect> getTerritoryEffects()
	{
		final Collection<TerritoryEffect> territoryEffects = new ArrayList<TerritoryEffect>();
		if (m_territoryEffectsJList != null)
		{
			final List<Object> selectedObjects = Arrays.asList(m_territoryEffectsJList.getSelectedValues());
			final List<String> selected = new ArrayList<String>();
			for (final Object obj : selectedObjects)
			{
				selected.add((String) obj);
			}
			m_data.acquireReadLock();
			try
			{
				final Hashtable<String, TerritoryEffect> allTerritoryEffects = m_data.getTerritoryEffectList();
				for (final String selection : selected)
				{
					if (selection.equals(NO_EFFECTS))
					{
						territoryEffects.clear();
						break;
					}
					territoryEffects.add(allTerritoryEffects.get(selection));
				}
			} finally
			{
				m_data.releaseReadLock();
			}
		}
		return territoryEffects;
	}
	
	private void updateStats()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			throw new IllegalStateException("Wrong thread");
		}
		final AtomicReference<AggregateResults> results = new AtomicReference<AggregateResults>();
		final OddsCalculator calculator = new OddsCalculator();
		final WaitDialog dialog = new WaitDialog(this, "Calculating Odds", new AbstractAction()
		{
			private static final long serialVersionUID = -2148507015083214974L;
			
			public void actionPerformed(final ActionEvent e)
			{
				calculator.cancel();
			}
		});
		final AtomicReference<Collection<Unit>> defenders = new AtomicReference<Collection<Unit>>();
		final AtomicReference<Collection<Unit>> attackers = new AtomicReference<Collection<Unit>>();
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		final Thread calcThread = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					// find a territory to fight in
					Territory location = null;
					if (m_location == null || m_location.isWater() == isLand())
					{
						for (final Territory t : m_data.getMap())
						{
							if (t.isWater() == !isLand())
							{
								location = t;
								break;
							}
						}
					}
					else
						location = m_location;
					if (location == null)
						throw new IllegalStateException("No territory found that is land:" + isLand());
					final List<Unit> defending = m_defendingUnitsPanel.getUnits();
					final List<Unit> attacking = m_attackingUnitsPanel.getUnits();
					List<Unit> bombarding = new ArrayList<Unit>();
					if (isLand())
					{
						bombarding = Match.getMatches(attacking, Matches.unitCanBombard(getAttacker()));
						attacking.removeAll(bombarding);
					}
					calculator.setRetreatAfterRound(m_retreatAfterXRounds.getValue());
					calculator.setRetreatAfterXUnitsLeft(m_retreatAfterXUnitsLeft.getValue());
					if (m_retreatWhenOnlyAirLeftCheckBox.isSelected())
						calculator.setRetreatWhenOnlyAirLeft(true);
					else
						calculator.setRetreatWhenOnlyAirLeft(false);
					if (m_landBattleCheckBox.isSelected() && m_keepOneAttackingLandUnitCheckBox.isSelected())
						calculator.setKeepOneAttackingLandUnit(true);
					else
						calculator.setKeepOneAttackingLandUnit(false);
					if (isAmphibiousBattle())
						calculator.setAmphibious(true);
					else
						calculator.setAmphibious(false);
					if (m_retreatWhenMetaPowerIsLower.isSelected())
						calculator.setRetreatWhenMetaPowerIsLower(true);
					else
						calculator.setRetreatWhenMetaPowerIsLower(false);
					calculator.setAttackerOrderOfLosses(m_attackerOrderOfLosses);
					calculator.setDefenderOrderOfLosses(m_defenderOrderOfLosses);
					final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
					defenders.set(defending);
					attackers.set(attacking);
					results.set(calculator.calculate(m_data, getAttacker(), getDefender(), location, attacking, defending, bombarding, territoryEffects, m_numRuns.getValue()));
				} finally
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							dialog.setVisible(false);
							dialog.dispose();
						}
					});
				}
			}
			// }, "Odds calc thread").start();
		}, "Odds calc thread");
		// Actually start thread.
		calcThread.start();
		// the runnable setting the dialog visible must
		// run after this code executes, since this
		// code is running on the swing event thread
		dialog.setVisible(true);
		// results.get() could be null if we cancelled to quickly or something weird like that.
		if (results == null || results.get() == null)
		{
			setResultsToBlank();
		}
		else
		{
			m_attackerWin.setText(formatPercentage(results.get().getAttackerWinPercent()));
			m_defenderWin.setText(formatPercentage(results.get().getDefenderWinPercent()));
			m_draw.setText(formatPercentage(results.get().getDrawPercent()));
			final boolean isLand = isLand();
			final List<Unit> mainCombatAttackers = Match.getMatches(attackers.get(), Matches.UnitCanBeInBattle(true, isLand, m_data, 1, false, true, true));
			final List<Unit> mainCombatDefenders = Match.getMatches(defenders.get(), Matches.UnitCanBeInBattle(false, isLand, m_data, 1, false, true, true));
			final int attackersTotal = mainCombatAttackers.size();
			final int defendersTotal = mainCombatDefenders.size();
			m_defenderLeft.setText(formatValue(results.get().getAverageDefendingUnitsLeft()) + " /" + defendersTotal);
			m_attackerLeft.setText(formatValue(results.get().getAverageAttackingUnitsLeft()) + " /" + attackersTotal);
			m_defenderLeftWhenDefenderWon.setText(formatValue(results.get().getAverageDefendingUnitsLeftWhenDefenderWon()) + " /" + defendersTotal);
			m_attackerLeftWhenAttackerWon.setText(formatValue(results.get().getAverageAttackingUnitsLeftWhenAttackerWon()) + " /" + attackersTotal);
			m_roundsAverage.setText("" + formatValue(results.get().getAverageBattleRoundsFought()));
			try
			{
				m_data.acquireReadLock();
				m_averageChangeInTUV.setText("" + formatValue(results.get().getAverageTUVswing(getAttacker(), mainCombatAttackers, getDefender(), mainCombatDefenders, m_data)));
			} finally
			{
				m_data.releaseReadLock();
			}
			m_count.setText(results.get().getRollCount() + "");
			m_time.setText(formatValue(results.get().getTime() / 1000.0) + "s");
		}
	}
	
	public String formatPercentage(final double percentage)
	{
		final NumberFormat format = new DecimalFormat("%");
		return format.format(percentage);
	}
	
	public String formatValue(final double value)
	{
		final NumberFormat format = new DecimalFormat("#0.##");
		return format.format(value);
	}
	
	private void updateDefender(List<Unit> units)
	{
		if (units == null)
			units = Collections.emptyList();
		final boolean isLand = isLand();
		units = Match.getMatches(units, Matches.UnitCanBeInBattle(false, isLand, m_data, 1, false, false, false));
		m_defendingUnitsPanel.init(getDefender(), units, isLand);
		/* setWidgetActivation now does all this
		final List<Unit> mainCombatUnits = Match.getMatches(units, Matches.UnitCanBeInBattle(false, isLand, m_data, false, true, true));
		m_defenderUnitsTotalNumber.setText("Units: " + mainCombatUnits.size());
		m_defenderUnitsTotalTUV.setText("TUV: " + BattleCalculator.getTUV(mainCombatUnits, getDefender(), BattleCalculator.getCostsForTUV(getDefender(), m_data), m_data));
		final int defenseHP = BattleCalculator.getTotalHitpoints(mainCombatUnits);
		m_defenderUnitsTotalHitpoints.setText("HP: " + defenseHP);
		final boolean isAmphibiousBattle = isAmphibiousBattle();
		final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
		final int defensePower = DiceRoll.getTotalPower(mainCombatUnits, true, getDefender(), m_location, territoryEffects, m_data, isAmphibiousBattle, new ArrayList<Unit>()); // defender is never amphibious
		m_defenderUnitsTotalPower.setText("Power: " + defensePower);
		m_defenderUnitsTotalPower.setToolTipText("<html>Meta Power: " + BattleCalculator.getNormalizedMetaPower(defensePower, defenseHP, m_data.getDiceSides())
					+ "<br /> (is equal to  Power  +  (2 * HitPoints * DiceSides / 6))</html>");
		*/
	}
	
	private void updateAttacker(List<Unit> units)
	{
		if (units == null)
			units = Collections.emptyList();
		final boolean isLand = isLand();
		units = Match.getMatches(units, Matches.UnitCanBeInBattle(true, isLand, m_data, 1, false, false, false));
		m_attackingUnitsPanel.init(getAttacker(), units, isLand);
		/* setWidgetActivation now does all this
		final List<Unit> mainCombatUnits = Match.getMatches(units, Matches.UnitCanBeInBattle(true, isLand, m_data, false, true, true));
		m_attackerUnitsTotalNumber.setText("Units: " + mainCombatUnits.size());
		m_attackerUnitsTotalTUV.setText("TUV: " + BattleCalculator.getTUV(mainCombatUnits, getAttacker(), BattleCalculator.getCostsForTUV(getAttacker(), m_data), m_data));
		final int attackHP = BattleCalculator.getTotalHitpoints(mainCombatUnits);
		m_attackerUnitsTotalHitpoints.setText("HP: " + attackHP);
		final boolean isAmphibiousBattle = isAmphibiousBattle();
		final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
		final int attackPower = DiceRoll.getTotalPower(mainCombatUnits, false, getAttacker(), m_location, territoryEffects, m_data, isAmphibiousBattle,
					(isAmphibiousBattle ? mainCombatUnits : new ArrayList<Unit>()));
		m_attackerUnitsTotalPower.setText("Power: " + attackPower);
		m_attackerUnitsTotalPower.setToolTipText("<html>Meta Power: " + BattleCalculator.getNormalizedMetaPower(attackPower, attackHP, m_data.getDiceSides())
					+ "<br /> (is equal to  Power  +  (2 * HitPoints * DiceSides / 6))</html>");
		*/
	}
	
	private boolean isLand()
	{
		return m_landBattleCheckBox.isSelected();
	}
	
	private PlayerID getEnemy(final PlayerID player)
	{
		for (final PlayerID id : m_data.getPlayerList())
		{
			if (m_data.getRelationshipTracker().isAtWar(player, id))
				return id;
		}
		for (final PlayerID id : m_data.getPlayerList())
		{
			if (!m_data.getRelationshipTracker().isAllied(player, id))
				return id;
		}
		// TODO: do we allow fighting allies in the battle calc?
		throw new IllegalStateException("No enemies or non-allies for :" + player);
	}
	
	private void layoutComponents()
	{
		setLayout(new BorderLayout());
		final JPanel main = new JPanel();
		main.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
		add(main, BorderLayout.CENTER);
		main.setLayout(new BorderLayout());
		final JPanel attackAndDefend = new JPanel();
		attackAndDefend.setLayout(new GridBagLayout());
		final int gap = 20;
		int row0 = 0;
		attackAndDefend.add(new JLabel("Attacker: "), new GridBagConstraints(0, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap, 0), 0, 0));
		attackAndDefend.add(m_attackerCombo, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, gap / 2, gap), 0, 0));
		attackAndDefend.add(new JLabel("Defender: "), new GridBagConstraints(2, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap, 0), 0, 0));
		attackAndDefend.add(m_defenderCombo, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, gap / 2, gap), 0, 0));
		row0++;
		attackAndDefend.add(m_attackerUnitsTotalNumber, new GridBagConstraints(0, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, 0, 0), 0, 0));
		attackAndDefend.add(m_attackerUnitsTotalTUV, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap / 2, 0, gap * 2), 0, 0));
		attackAndDefend.add(m_defenderUnitsTotalNumber, new GridBagConstraints(2, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, 0, 0), 0, 0));
		attackAndDefend.add(m_defenderUnitsTotalTUV, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap / 2, 0, gap * 2), 0, 0));
		row0++;
		attackAndDefend.add(m_attackerUnitsTotalHitpoints, new GridBagConstraints(0, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap / 2, 0), 0, 0));
		attackAndDefend.add(m_attackerUnitsTotalPower, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap / 2, gap / 2, gap * 2), 0, 0));
		attackAndDefend.add(m_defenderUnitsTotalHitpoints, new GridBagConstraints(2, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap / 2, 0), 0, 0));
		attackAndDefend.add(m_defenderUnitsTotalPower, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap / 2, gap / 2, gap * 2), 0, 0));
		row0++;
		final JScrollPane attackerScroll = new JScrollPane(m_attackingUnitsPanel);
		attackerScroll.setBorder(null);
		attackerScroll.getViewport().setBorder(null);
		final JScrollPane defenderScroll = new JScrollPane(m_defendingUnitsPanel);
		defenderScroll.setBorder(null);
		defenderScroll.getViewport().setBorder(null);
		attackAndDefend.add(attackerScroll, new GridBagConstraints(0, row0, 2, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, gap, gap, gap), 0, 0));
		attackAndDefend.add(defenderScroll, new GridBagConstraints(2, row0, 2, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(10, gap, gap, gap), 0, 0));
		main.add(attackAndDefend, BorderLayout.CENTER);
		final JPanel resultsText = new JPanel();
		resultsText.setLayout(new GridBagLayout());
		int row1 = 0;
		resultsText.add(new JLabel("Attacker Wins:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Draw:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Defender Wins:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Ave. Defender Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Units Left If Def Won:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Ave. Attacker Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Units Left If Att Won:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Average TUV Swing:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Average Rounds:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Simulation Count:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Time:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(m_calculateButton, new GridBagConstraints(0, row1++, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(20, 60, 0, 100), 0, 0));
		resultsText.add(m_clearButton, new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(6, 60, 0, 0), 0, 0));
		
		resultsText.add(new JLabel("Run Count:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(20, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Retreat After Round:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Retreat When X Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
		int row2 = 0;
		resultsText.add(m_attackerWin, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		resultsText.add(m_draw, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		resultsText.add(m_defenderWin, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		resultsText.add(m_defenderLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
		resultsText.add(m_defenderLeftWhenDefenderWon, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		resultsText.add(m_attackerLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
		resultsText.add(m_attackerLeftWhenAttackerWon, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		
		resultsText.add(m_averageChangeInTUV, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
		resultsText.add(m_roundsAverage, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		resultsText.add(m_count, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(15, 10, 0, 0), 0, 0));
		resultsText.add(m_time, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
		row2++;
		resultsText.add(m_SwapSidesButton, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(6, 10, 0, 100), 0, 0));
		
		resultsText.add(m_numRuns, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 10, 0, 0), 0, 0));
		resultsText.add(m_retreatAfterXRounds, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
		resultsText.add(m_retreatAfterXUnitsLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
		
		row1 = row2;
		resultsText.add(m_orderOfLossesButton, new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(10, 15, 0, 0), 0, 0));
		if (m_territoryEffectsJList != null)
		{
			resultsText.add(new JScrollPane(m_territoryEffectsJList), new GridBagConstraints(0, row1, 1, m_territoryEffectsJList.getVisibleRowCount(), 0, 0, GridBagConstraints.EAST,
						GridBagConstraints.BOTH, new Insets(10, 15, 0, 0), 0, 0));
			row1 += m_territoryEffectsJList.getVisibleRowCount();
		}
		
		resultsText.add(m_retreatWhenOnlyAirLeftCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 5), 0, 0));
		resultsText.add(m_retreatWhenMetaPowerIsLower, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
		resultsText.add(m_keepOneAttackingLandUnitCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
		resultsText.add(m_amphibiousCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
		resultsText.add(m_landBattleCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
		m_resultsPanel.add(resultsText);
		m_resultsPanel.setBorder(BorderFactory.createEmptyBorder());
		final JScrollPane resultsScroll = new JScrollPane(m_resultsPanel);
		resultsScroll.setBorder(BorderFactory.createEmptyBorder());
		final Dimension resultsScrollDimensions = resultsScroll.getPreferredSize();
		resultsScrollDimensions.width += 22; // add some so that we don't have double scroll bars appear when only one is needed
		resultsScroll.setPreferredSize(resultsScrollDimensions);
		main.add(resultsScroll, BorderLayout.EAST);
		final JPanel south = new JPanel();
		south.setLayout(new BorderLayout());
		final JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
		buttons.add(m_closeButton);
		south.add(buttons, BorderLayout.SOUTH);
		add(south, BorderLayout.SOUTH);
	}
	
	private void createComponents()
	{
		m_data.acquireReadLock();
		try
		{
			final Collection<PlayerID> playerList = new ArrayList<PlayerID>(m_data.getPlayerList().getPlayers());
			if (doesPlayerHaveUnitsOnMap(PlayerID.NULL_PLAYERID, m_data))
				playerList.add(PlayerID.NULL_PLAYERID);
			m_attackerCombo = new JComboBox(new Vector<PlayerID>(playerList));
			m_defenderCombo = new JComboBox(new Vector<PlayerID>(playerList));
			m_SwapSidesCombo = new JComboBox(new Vector<PlayerID>(playerList));
			final Hashtable<String, TerritoryEffect> allTerritoryEffects = m_data.getTerritoryEffectList();
			if (allTerritoryEffects == null || allTerritoryEffects.isEmpty())
				m_territoryEffectsJList = null;
			else
			{
				final Vector<String> effectNames = new Vector<String>();
				effectNames.add(NO_EFFECTS);
				effectNames.addAll(allTerritoryEffects.keySet());
				m_territoryEffectsJList = new JList(effectNames);
				m_territoryEffectsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				m_territoryEffectsJList.setLayoutOrientation(JList.VERTICAL);
				m_territoryEffectsJList.setVisibleRowCount(4); // equal to the amount of space left (number of remaining items on the right)
				if (m_location != null)
				{
					final Collection<TerritoryEffect> currentEffects = TerritoryEffectHelper.getEffects(m_location);
					if (!currentEffects.isEmpty())
					{
						final int[] selectedIndexes = new int[currentEffects.size()];
						int currentIndex = 0;
						for (final TerritoryEffect te : currentEffects)
						{
							selectedIndexes[currentIndex] = effectNames.indexOf(te.getName());
							currentIndex++;
						}
						m_territoryEffectsJList.setSelectedIndices(selectedIndexes);
					}
				}
			}
		} finally
		{
			m_data.releaseReadLock();
		}
		m_defenderCombo.setRenderer(new PlayerRenderer());
		m_attackerCombo.setRenderer(new PlayerRenderer());
		m_SwapSidesCombo.setRenderer(new PlayerRenderer());
		m_defendingUnitsPanel = new PlayerUnitsPanel(m_data, m_context, true);
		m_attackingUnitsPanel = new PlayerUnitsPanel(m_data, m_context, false);
		m_numRuns.setColumns(4);
		m_numRuns.setMin(1);
		m_numRuns.setMax(20000);
		m_numRuns.setValue((games.strategy.triplea.Properties.getLow_Luck(m_data) ? 500 : 2000));
		m_retreatAfterXRounds.setColumns(4);
		m_retreatAfterXRounds.setMin(-1);
		m_retreatAfterXRounds.setMax(1000);
		m_retreatAfterXRounds.setValue(-1);
		m_retreatAfterXRounds.setToolTipText("-1 means never.");
		m_retreatAfterXUnitsLeft.setColumns(4);
		m_retreatAfterXUnitsLeft.setMin(-1);
		m_retreatAfterXUnitsLeft.setMax(1000);
		m_retreatAfterXUnitsLeft.setValue(-1);
		m_retreatAfterXUnitsLeft.setToolTipText("-1 means never. If positive and 'retreat when only air left' is also selected, then we will retreat when X of non-air units is left.");
		setResultsToBlank();
		m_defenderLeft.setToolTipText("Units Left does not include AA guns and other infrastructure, and does not include Bombarding sea units for land battles.");
		m_attackerLeft.setToolTipText("Units Left does not include AA guns and other infrastructure, and does not include Bombarding sea units for land battles.");
		m_defenderLeftWhenDefenderWon.setToolTipText("Units Left does not include AA guns and other infrastructure, and does not include Bombarding sea units for land battles.");
		m_attackerLeftWhenAttackerWon.setToolTipText("Units Left does not include AA guns and other infrastructure, and does not include Bombarding sea units for land battles.");
		m_averageChangeInTUV.setToolTipText("TUV Swing does not include captured AA guns and other infrastructure, and does not include Bombarding sea units for land battles.");
		m_retreatWhenOnlyAirLeftCheckBox.setToolTipText("We retreat if only air is left, and if 'retreat when x units left' is positive we will retreat when x of non-air is left too.");
		m_retreatWhenMetaPowerIsLower.setToolTipText("We retreat if our 'meta power' is lower than the opponent. Meta Power is equal to:  Power  +  (2 * HitPoints * DiceSides / 6)");
		m_attackerUnitsTotalNumber.setToolTipText("Totals do not include AA guns and other infrastructure, and does not include Bombarding sea units for land battles.");
		m_defenderUnitsTotalNumber.setToolTipText("Totals do not include AA guns and other infrastructure, and does not include Bombarding sea units for land battles.");
	}
	
	private void setResultsToBlank()
	{
		final String blank = "------";
		m_attackerWin.setText(blank);
		m_defenderWin.setText(blank);
		m_draw.setText(blank);
		m_defenderLeft.setText(blank);
		m_attackerLeft.setText(blank);
		m_defenderLeftWhenDefenderWon.setText(blank);
		m_attackerLeftWhenAttackerWon.setText(blank);
		m_roundsAverage.setText(blank);
		m_averageChangeInTUV.setText(blank);
		m_count.setText(blank);
		m_time.setText(blank);
	}
	
	public void setWidgetActivation()
	{
		m_keepOneAttackingLandUnitCheckBox.setEnabled(m_landBattleCheckBox.isSelected());
		m_amphibiousCheckBox.setEnabled(m_landBattleCheckBox.isSelected());
		final boolean isLand = isLand();
		try
		{
			m_data.acquireReadLock();
			// do not include bombardment and aa guns in our "total" labels
			final List<Unit> attackers = Match.getMatches(m_attackingUnitsPanel.getUnits(), Matches.UnitCanBeInBattle(true, isLand, m_data, 1, false, true, true));
			final List<Unit> defenders = Match.getMatches(m_defendingUnitsPanel.getUnits(), Matches.UnitCanBeInBattle(false, isLand, m_data, 1, false, true, true));
			m_attackerUnitsTotalNumber.setText("Units: " + attackers.size());
			m_defenderUnitsTotalNumber.setText("Units: " + defenders.size());
			m_attackerUnitsTotalTUV.setText("TUV: " + BattleCalculator.getTUV(attackers, getAttacker(), BattleCalculator.getCostsForTUV(getAttacker(), m_data), m_data));
			m_defenderUnitsTotalTUV.setText("TUV: " + BattleCalculator.getTUV(defenders, getDefender(), BattleCalculator.getCostsForTUV(getDefender(), m_data), m_data));
			final int attackHP = BattleCalculator.getTotalHitpoints(attackers);
			final int defenseHP = BattleCalculator.getTotalHitpoints(defenders);
			m_attackerUnitsTotalHitpoints.setText("HP: " + attackHP);
			m_defenderUnitsTotalHitpoints.setText("HP: " + defenseHP);
			final boolean isAmphibiousBattle = isAmphibiousBattle();
			final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
			final int attackPower = DiceRoll.getTotalPowerAndRolls(
						DiceRoll.getUnitPowerAndRollsForNormalBattles(attackers, attackers, defenders, false, false, getAttacker(), m_data, m_location, territoryEffects, isAmphibiousBattle,
									(isAmphibiousBattle ? attackers : new ArrayList<Unit>())), m_data).getFirst();
			final int defensePower = DiceRoll.getTotalPowerAndRolls(
						DiceRoll.getUnitPowerAndRollsForNormalBattles(defenders, defenders, attackers, true, false, getDefender(), m_data, m_location, territoryEffects,
									isAmphibiousBattle, new ArrayList<Unit>()), m_data).getFirst(); // defender is never amphibious
			m_attackerUnitsTotalPower.setText("Power: " + attackPower);
			m_defenderUnitsTotalPower.setText("Power: " + defensePower);
			m_attackerUnitsTotalPower.setToolTipText("<html>Meta Power: " + BattleCalculator.getNormalizedMetaPower(attackPower, attackHP, m_data.getDiceSides())
						+ "<br /> (is equal to  Power  +  (2 * HitPoints * DiceSides / 6))</html>");
			m_defenderUnitsTotalPower.setToolTipText("<html>Meta Power: " + BattleCalculator.getNormalizedMetaPower(defensePower, defenseHP, m_data.getDiceSides())
						+ "<br /> (is equal to  Power  +  (2 * HitPoints * DiceSides / 6))</html>");
		} finally
		{
			m_data.releaseReadLock();
		}
	}
	
	
	class PlayerRenderer extends DefaultListCellRenderer
	{
		private static final long serialVersionUID = -7639128794342607309L;
		
		@Override
		public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
		{
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			final PlayerID id = (PlayerID) value;
			setText(id.getName());
			setIcon(new ImageIcon(m_context.getFlagImageFactory().getSmallFlag(id)));
			return this;
		}
	}
	
	public void selectCalculateButton()
	{
		m_calculateButton.requestFocus();
	}
	
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


class PlayerUnitsPanel extends JPanel
{
	private static final long serialVersionUID = -1206338960403314681L;
	private final GameData m_data;
	private final IUIContext m_context;
	private final boolean m_defender;
	private boolean m_isLand = true;
	private List<UnitCategory> m_categories = null;
	private final ListenerList<WidgetChangedListener> m_listeners = new ListenerList<WidgetChangedListener>();
	private final WidgetChangedListener m_listenerUnitPanel = new WidgetChangedListener()
	{
		public void widgetChanged()
		{
			notifyListeners();
		}
	};
	
	PlayerUnitsPanel(final GameData data, final IUIContext context, final boolean defender)
	{
		m_data = data;
		m_context = context;
		m_defender = defender;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	}
	
	public void clear()
	{
		for (final Component c : getComponents())
		{
			final UnitPanel panel = (UnitPanel) c;
			panel.setCount(0);
		}
	}
	
	public List<Unit> getUnits()
	{
		final List<Unit> allUnits = new ArrayList<Unit>();
		for (final Component c : getComponents())
		{
			final UnitPanel panel = (UnitPanel) c;
			allUnits.addAll(panel.getUnits());
		}
		return allUnits;
	}
	
	public List<UnitCategory> getCategories()
	{
		return m_categories;
	}
	
	public void init(final PlayerID id, final List<Unit> units, final boolean land)
	{
		m_isLand = land;
		m_categories = new ArrayList<UnitCategory>(categorize(id, units));
		Collections.sort(m_categories, new Comparator<UnitCategory>()
		{
			public int compare(final UnitCategory o1, final UnitCategory o2)
			{
				final UnitType ut1 = o1.getType();
				final UnitType ut2 = o2.getType();
				final UnitAttachment u1 = UnitAttachment.get(ut1);
				final UnitAttachment u2 = UnitAttachment.get(ut2);
				// for land, we want land, air, aa gun, then bombarding
				if (land)
				{
					if (u1.getIsSea() != u2.getIsSea())
					{
						return u1.getIsSea() ? 1 : -1;
					}
					if (Matches.UnitTypeIsAAforAnything.match(ut1) != Matches.UnitTypeIsAAforAnything.match(ut2))
					{
						return Matches.UnitTypeIsAAforAnything.match(ut1) ? 1 : -1;
					}
					if (u1.getIsAir() != u2.getIsAir())
					{
						return u1.getIsAir() ? 1 : -1;
					}
				}
				else
				{
					if (u1.getIsSea() != u2.getIsSea())
					{
						return u1.getIsSea() ? -1 : 1;
					}
				}
				return u1.getName().compareTo(u2.getName());
			}
		});
		removeAll();
		Match<UnitType> predicate;
		if (land)
		{
			if (m_defender)
				predicate = Matches.UnitTypeIsNotSea;
			else
				predicate = new CompositeMatchOr<UnitType>(Matches.UnitTypeIsNotSea, Matches.unitTypeCanBombard(id));
		}
		else
			predicate = Matches.UnitTypeIsSeaOrAir;
		final IntegerMap<UnitType> costs;
		try
		{
			m_data.acquireReadLock();
			costs = BattleCalculator.getCostsForTUV(id, m_data);
		} finally
		{
			m_data.releaseReadLock();
		}
		for (final UnitCategory category : m_categories)
		{
			if (predicate.match(category.getType()))
			{
				final UnitPanel upanel = new UnitPanel(m_data, m_context, category, costs);
				upanel.addChangeListener(m_listenerUnitPanel);
				add(upanel);
			}
		}
		invalidate();
		validate();
		revalidate();
		getParent().invalidate();
	}
	
	private Set<UnitCategory> categorize(final PlayerID id, final List<Unit> units)
	{
		// these are the units that exist
		final Set<UnitCategory> categories = UnitSeperator.categorize(units);
		// the units that can be produced or moved in
		for (final UnitType t : getUnitTypes(id))
		{
			final UnitCategory category = new UnitCategory(t, id);
			categories.add(category);
		}
		return categories;
	}
	
	/**
	 * return all the unit types available for the given player. a unit type is
	 * available if the unit is producable, or if a player has one
	 */
	private Collection<UnitType> getUnitTypes(final PlayerID player)
	{
		Collection<UnitType> rVal = new HashSet<UnitType>();
		final ProductionFrontier frontier = player.getProductionFrontier();
		if (frontier != null)
		{
			for (final ProductionRule rule : frontier)
			{
				for (final NamedAttachable type : rule.getResults().keySet())
				{
					if (type instanceof UnitType)
						rVal.add((UnitType) type);
				}
			}
		}
		for (final Territory t : m_data.getMap())
		{
			for (final Unit u : t.getUnits())
			{
				if (u.getOwner().equals(player))
					rVal.add(u.getType());
			}
		}
		// we want to filter out anything like factories, or units that have no combat ability AND can not be taken casualty.
		// in addition, as of right now AA guns can not fire on the offensive side, so we want to take them out too, unless they have other combat abilities.
		rVal = Match.getMatches(rVal, Matches.UnitTypeCanBeInBattle(!m_defender, m_isLand, player, m_data, 1, false, false, false));
		return rVal;
	}
	
	public void addChangeListener(final WidgetChangedListener listener)
	{
		m_listeners.add(listener);
	}
	
	public void removeChangeListener(final WidgetChangedListener listener)
	{
		m_listeners.remove(listener);
	}
	
	private void notifyListeners()
	{
		for (final WidgetChangedListener listener : m_listeners)
		{
			listener.widgetChanged();
		}
	}
}


class UnitPanel extends JPanel
{
	private static final long serialVersionUID = 1509643150038705671L;
	private final IUIContext m_context;
	private final UnitCategory m_category;
	private final ScrollableTextField m_textField;
	private final GameData m_data;
	private final ListenerList<WidgetChangedListener> m_listeners = new ListenerList<WidgetChangedListener>();
	private final ScrollableTextFieldListener m_listenerTextField = new ScrollableTextFieldListener()
	{
		public void changedValue(final ScrollableTextField field)
		{
			notifyListeners();
		}
	};
	
	public UnitPanel(final GameData data, final IUIContext context, final UnitCategory category, final IntegerMap<UnitType> costs)
	{
		m_category = category;
		m_context = context;
		m_data = data;
		m_textField = new ScrollableTextField(0, 512);
		m_textField.setShowMaxAndMin(false);
		m_textField.addChangeListener(m_listenerTextField);
		final Image img = m_context.getUnitImageFactory().getImage(m_category.getType(), m_category.getOwner(), m_data, m_category.hasDamageOrBombingUnitDamage(), m_category.getDisabled());
		final String toolTipText = "<html>" + m_category.getType().getName() + ":  " + costs.getInt(m_category.getType()) + " cost, <br /> &nbsp;&nbsp;&nbsp;&nbsp; "
					+ m_category.getType().getTooltip(m_category.getOwner(), true) + "</html>";
		setCount(m_category.getUnits().size());
		setLayout(new GridBagLayout());
		final JLabel label = new JLabel(new ImageIcon(img));
		label.setToolTipText(toolTipText);
		add(label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 10), 0, 0));
		add(m_textField, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
	}
	
	public List<Unit> getUnits()
	{
		final List<Unit> units = m_category.getType().create(m_textField.getValue(), m_category.getOwner(), true);
		if (!units.isEmpty())
		{
			// creating the unit just makes it, we want to make sure it is damaged if the category says it is damaged
			if (m_category.getHitPoints() > 1 && m_category.getDamaged() > 0)
			{
				// we do not need to use bridge and change factory here because this is not sent over the network. these are just some temporary units for the battle calc.
				for (final Unit u : units)
				{
					u.setHits(m_category.getDamaged());
				}
			}
			if (m_category.getDisabled() && Matches.UnitTypeCanBeDamaged.match(m_category.getType()))
			{
				final int uDamage = Math.max(0, 1 + UnitAttachment.get(m_category.getType()).getMaxOperationalDamage()); // add 1 because it is the max operational damage and we want to disable it
				for (final Unit u : units)
				{
					((TripleAUnit) u).setUnitDamage(uDamage);
				}
			}
		}
		return units;
	}
	
	public int getCount()
	{
		return m_textField.getValue();
	}
	
	public void setCount(final int value)
	{
		m_textField.setValue(value);
	}
	
	public UnitCategory getCategory()
	{
		return m_category;
	}
	
	public void addChangeListener(final WidgetChangedListener listener)
	{
		m_listeners.add(listener);
	}
	
	public void removeChangeListener(final WidgetChangedListener listener)
	{
		m_listeners.remove(listener);
	}
	
	private void notifyListeners()
	{
		for (final WidgetChangedListener listener : m_listeners)
		{
			listener.widgetChanged();
		}
	}
}


class OrderOfLossesInputPanel extends JPanel
{
	private static final long serialVersionUID = 8815617685388156219L;
	private final GameData m_data;
	private final IUIContext m_context;
	private final List<UnitCategory> m_attackerCategories;
	private final List<UnitCategory> m_defenderCategories;
	private final JTextField m_attackerTextField;
	private final JTextField m_defenderTextField;
	private final JLabel m_attackerLabel = new JLabel("Attacker Units:");
	private final JLabel m_defenderLabel = new JLabel("Defender Units:");
	private final JButton m_clear;
	private final boolean m_land;
	
	public OrderOfLossesInputPanel(final String attackerOrder, final String defenderOrder, final List<UnitCategory> attackerCategories, final List<UnitCategory> defenderCategories,
				final boolean land, final IUIContext context, final GameData data)
	{
		m_data = data;
		m_context = context;
		m_land = land;
		m_attackerCategories = attackerCategories;
		m_defenderCategories = defenderCategories;
		m_attackerTextField = new JTextField(attackerOrder == null ? "" : attackerOrder);
		m_attackerTextField.getDocument().addDocumentListener(new DocumentListener()
		{
			public void insertUpdate(final DocumentEvent e)
			{
				if (!OddsCalculator.isValidOOL(m_attackerTextField.getText(), m_data))
					m_attackerLabel.setForeground(Color.red);
				else
					m_attackerLabel.setForeground(null);
			}
			
			public void removeUpdate(final DocumentEvent e)
			{
				if (!OddsCalculator.isValidOOL(m_attackerTextField.getText(), m_data))
					m_attackerLabel.setForeground(Color.red);
				else
					m_attackerLabel.setForeground(null);
			}
			
			public void changedUpdate(final DocumentEvent e)
			{
				if (!OddsCalculator.isValidOOL(m_attackerTextField.getText(), m_data))
					m_attackerLabel.setForeground(Color.red);
				else
					m_attackerLabel.setForeground(null);
			}
		});
		m_defenderTextField = new JTextField(defenderOrder == null ? "" : defenderOrder);
		m_defenderTextField.getDocument().addDocumentListener(new DocumentListener()
		{
			public void insertUpdate(final DocumentEvent e)
			{
				if (!OddsCalculator.isValidOOL(m_defenderTextField.getText(), m_data))
					m_defenderLabel.setForeground(Color.red);
				else
					m_defenderLabel.setForeground(null);
			}
			
			public void removeUpdate(final DocumentEvent e)
			{
				if (!OddsCalculator.isValidOOL(m_defenderTextField.getText(), m_data))
					m_defenderLabel.setForeground(Color.red);
				else
					m_defenderLabel.setForeground(null);
			}
			
			public void changedUpdate(final DocumentEvent e)
			{
				if (!OddsCalculator.isValidOOL(m_defenderTextField.getText(), m_data))
					m_defenderLabel.setForeground(Color.red);
				else
					m_defenderLabel.setForeground(null);
			}
		});
		m_clear = new JButton("Clear");
		m_clear.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_attackerTextField.setText("");
				m_defenderTextField.setText("");
			}
		});
		layoutComponents();
	}
	
	private void layoutComponents()
	{
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		final JLabel instructions = new JLabel("<html>Here you can specify the 'Order of Losses' (OOL) for each side."
					+ "<br />Damageable units will be damanged first always. If the player label is red, your OOL is invalid."
					+ "<br />The engine will take your input and add all units to a list starting on the RIGHT side of your text line."
					+ "<br />Then, during combat, casualties will be chosen starting on the LEFT side of your OOL."
					+ "<br />" + OddsCalculator.OOL_SEPARATOR + " separates unit types."
					+ "<br />" + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + " is in front of the unit type and describes the number of units."
					+ "<br />" + OddsCalculator.OOL_ALL + " means all units of that type."
					+ "<br />Examples:"
					+ "<br />" + OddsCalculator.OOL_ALL + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "infantry" + OddsCalculator.OOL_SEPARATOR + OddsCalculator.OOL_ALL
					+ OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "artillery" + OddsCalculator.OOL_SEPARATOR + OddsCalculator.OOL_ALL + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "fighter"
					+ "<br />The above will take all infantry, then all artillery, then all fighters, then all other units as casualty."
					+ "<br /><br />1" + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "infantry" + OddsCalculator.OOL_SEPARATOR + "2" + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "artillery"
					+ OddsCalculator.OOL_SEPARATOR + "6" + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "fighter"
					+ "<br />The above will take 1 infantry, then 2 artillery, then 6 fighters, then all other units as casualty."
					+ "<br /><br />" + OddsCalculator.OOL_ALL + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "infantry" + OddsCalculator.OOL_SEPARATOR + OddsCalculator.OOL_ALL
					+ OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "fighter" + OddsCalculator.OOL_SEPARATOR + "1" + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "infantry"
					+ "<br />The above will take all except 1 infantry casualty, then all fighters, then the last infantry, then all other units casualty.</html>");
		instructions.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.add(instructions);
		this.add(Box.createVerticalStrut(30));
		m_attackerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.add(m_attackerLabel);
		final JPanel attackerUnits = getUnitButtonPanel(m_attackerCategories, m_attackerTextField);
		attackerUnits.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.add(attackerUnits);
		m_attackerTextField.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.add(m_attackerTextField);
		this.add(Box.createVerticalStrut(30));
		m_defenderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.add(m_defenderLabel);
		final JPanel defenderUnits = getUnitButtonPanel(m_defenderCategories, m_defenderTextField);
		defenderUnits.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.add(defenderUnits);
		m_defenderTextField.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.add(m_defenderTextField);
		this.add(Box.createVerticalStrut(10));
		m_clear.setAlignmentX(Component.CENTER_ALIGNMENT);
		this.add(m_clear);
	}
	
	private JPanel getUnitButtonPanel(final List<UnitCategory> categories, final JTextField textField)
	{
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		if (categories != null)
		{
			final Set<UnitType> typesUsed = new HashSet<UnitType>();
			for (final UnitCategory category : categories)
			{
				// no duplicates or infrastructure allowed. no sea if land, no land if sea.
				if (typesUsed.contains(category.getType()) || Matches.UnitTypeIsInfrastructure.match(category.getType()) || (m_land && Matches.UnitTypeIsSea.match(category.getType()))
							|| (!m_land && Matches.UnitTypeIsLand.match(category.getType())))
					continue;
				final Image img = m_context.getUnitImageFactory().getImage(category.getType(), category.getOwner(), m_data, category.hasDamageOrBombingUnitDamage(), category.getDisabled());
				final String unitName = OddsCalculator.OOL_ALL + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + category.getType().getName();
				final String toolTipText = "<html>" + category.getType().getName() + ":  " + category.getType().getTooltip(category.getOwner(), true) + "</html>";
				final JButton button = new JButton(new ImageIcon(img));
				button.setToolTipText(toolTipText);
				button.addActionListener(new ActionListener()
				{
					public void actionPerformed(final ActionEvent e)
					{
						textField.setText((textField.getText().length() > 0 ? (textField.getText() + OddsCalculator.OOL_SEPARATOR) : "") + unitName);
					}
				});
				panel.add(button);
				typesUsed.add(category.getType());
			}
		}
		return panel;
	}
	
	public String getAttackerOrder()
	{
		return m_attackerTextField.getText();
	}
	
	public String getDefenderOrder()
	{
		return m_defenderTextField.getText();
	}
}
