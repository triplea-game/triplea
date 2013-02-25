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
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.ui.UIContext;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.IntTextField;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.ui.WidgetChangedListener;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.ListenerList;
import games.strategy.util.Match;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

public class OddsCalculatorPanel extends JPanel
{
	private static final long serialVersionUID = -3559687618320469183L;
	private final Window m_parent;
	private JLabel m_attackerWin;
	private JLabel m_defenderWin;
	private JLabel m_draw;
	private JLabel m_defenderLeft;
	private JLabel m_attackerLeft;
	private JLabel m_defenderLeftWhenDefenderWon;
	private JLabel m_attackerLeftWhenAttackerWon;
	private JLabel m_roundsAverage;
	private int m_defendersTotal = 0;
	private int m_attackersTotal = 0;
	private JLabel m_count;
	private final UIContext m_context;
	private final GameData m_data;
	private IntTextField m_numRuns;
	private JPanel m_resultsPanel;
	private JButton m_calculateButton;
	private JButton m_closeButton;
	private JButton m_SwapSidesButton;
	private PlayerUnitsPanel m_attackingUnitsPanel;
	private PlayerUnitsPanel m_defendingUnitsPanel;
	private JComboBox m_attackerCombo;
	private JComboBox m_defenderCombo;
	private final JLabel m_attackerUnitsTotal = new JLabel();
	private final JLabel m_defenderUnitsTotal = new JLabel();
	private JComboBox m_SwapSidesCombo;
	private JCheckBox m_keepOneAttackingLandUnitCheckBox;
	private JCheckBox m_amphibiousCheckBox;
	private JCheckBox m_landBattleCheckBox;
	private IntTextField m_retreatAfterXRounds;
	private JButton m_clearButton;
	private JLabel m_time;
	private Territory m_location = null;
	private JList m_territoryEffectsJList;
	private static final String NO_EFFECTS = "*None*";
	private final WidgetChangedListener m_listenerPlayerUnitsPanel = new WidgetChangedListener()
	{
		public void widgetChanged()
		{
			setWidgetActivation();
		}
	};
	
	public OddsCalculatorPanel(final GameData data, final UIContext context, final Territory location, final Window parent)
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
			}
		});
		m_landBattleCheckBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
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
				m_parent.setVisible(false);
			}
		});
		m_clearButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				m_defendingUnitsPanel.clear();
				m_attackingUnitsPanel.clear();
			}
		});
		m_SwapSidesButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(final ActionEvent e)
			{
				List<Unit> getdefenders = new ArrayList<Unit>();
				List<Unit> getattackers = new ArrayList<Unit>();
				getdefenders = m_defendingUnitsPanel.getUnits();
				getattackers = m_attackingUnitsPanel.getUnits();
				m_SwapSidesCombo.setSelectedItem(getAttacker());
				m_attackerCombo.setSelectedItem(getDefender());
				m_defenderCombo.setSelectedItem(getSwapSides());
				m_attackingUnitsPanel.init(getAttacker(), getdefenders, isLand());
				m_defendingUnitsPanel.init(getDefender(), getattackers, isLand());
			}
		});
		m_attackingUnitsPanel.addChangeListener(m_listenerPlayerUnitsPanel);
		m_defendingUnitsPanel.addChangeListener(m_listenerPlayerUnitsPanel);
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
		final AtomicInteger numberDefenders = new AtomicInteger();
		final AtomicInteger numberAttackers = new AtomicInteger();
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
					numberDefenders.set(defending.size());
					final List<Unit> attacking = m_attackingUnitsPanel.getUnits();
					numberAttackers.set(attacking.size());
					List<Unit> bombarding = new ArrayList<Unit>();
					if (isLand())
					{
						bombarding = Match.getMatches(attacking, Matches.unitCanBombard(getAttacker()));
						attacking.removeAll(bombarding);
					}
					calculator.setRetreatAfterRound(m_retreatAfterXRounds.getValue());
					if (m_landBattleCheckBox.isSelected() && m_keepOneAttackingLandUnitCheckBox.isSelected())
						calculator.setKeepOneAttackingLandUnit(true);
					else
						calculator.setKeepOneAttackingLandUnit(false);
					if (m_landBattleCheckBox.isSelected() && m_amphibiousCheckBox.isSelected())
						calculator.setAmphibious(true);
					else
						calculator.setAmphibious(false);
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
		m_attackerWin.setText(formatPercentage(results.get().getAttackerWinPercent()));
		m_defenderWin.setText(formatPercentage(results.get().getDefenderWinPercent()));
		m_draw.setText(formatPercentage(results.get().getDrawPercent()));
		m_defendersTotal = numberDefenders.get();
		m_attackersTotal = numberAttackers.get();
		m_defenderLeft.setText(formatValue(results.get().getAverageDefendingUnitsLeft()) + " /" + m_defendersTotal);
		m_attackerLeft.setText(formatValue(results.get().getAverageAttackingUnitsLeft()) + " /" + m_attackersTotal);
		m_defenderLeftWhenDefenderWon.setText(formatValue(results.get().getAverageDefendingUnitsLeftWhenDefenderWon()) + " /" + m_defendersTotal);
		m_attackerLeftWhenAttackerWon.setText(formatValue(results.get().getAverageAttackingUnitsLeftWhenAttackerWon()) + " /" + m_attackersTotal);
		m_roundsAverage.setText("" + formatValue(results.get().getAverageBattleRoundsFought()));
		m_count.setText(results.get().getRollCount() + "");
		m_time.setText(formatValue(results.get().getTime() / 1000.0) + "s");
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
		units = Match.getMatches(units, Matches.UnitCanBeInBattle(false, m_data, false, false));
		m_defendingUnitsPanel.init(getDefender(), units, isLand());
		m_defenderUnitsTotal.setText("Num Units: " + units.size());
	}
	
	private void updateAttacker(List<Unit> units)
	{
		if (units == null)
			units = Collections.emptyList();
		units = Match.getMatches(units, Matches.UnitCanBeInBattle(true, m_data, false, false));
		m_attackingUnitsPanel.init(getAttacker(), units, isLand());
		m_attackerUnitsTotal.setText("Num Units: " + units.size());
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
		attackAndDefend.add(m_attackerCombo, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, gap, gap), 0, 0));
		attackAndDefend.add(new JLabel("Defender: "), new GridBagConstraints(2, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap, 0), 0, 0));
		attackAndDefend.add(m_defenderCombo, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, gap, gap), 0, 0));
		row0++;
		attackAndDefend.add(m_attackerUnitsTotal, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, gap, gap), 0, 0));
		attackAndDefend.add(m_defenderUnitsTotal, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, gap, gap), 0, 0));
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
		resultsText.add(new JLabel("Ave. Defender Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Units Left If Def Won:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Ave. Attacker Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Units Left If Att Won:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Average Rounds:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Simulation Count:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Time:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Run Count:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(30, 0, 0, 0), 0, 0));
		resultsText.add(new JLabel("Retreat After Round:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 0, 0, 0), 0, 0));
		int row2 = 0;
		resultsText.add(m_attackerWin, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		resultsText.add(m_draw, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		resultsText.add(m_defenderWin, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		resultsText.add(m_defenderLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
		resultsText.add(m_defenderLeftWhenDefenderWon, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		resultsText.add(m_attackerLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
		resultsText.add(m_attackerLeftWhenAttackerWon, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		resultsText.add(m_roundsAverage, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(5, 5, 0, 0), 0, 0));
		resultsText.add(m_count, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 5, 0, 0), 0, 0));
		resultsText.add(m_time, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 5, 0, 0), 0, 0));
		resultsText.add(m_numRuns, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(30, 5, 0, 0), 0, 0));
		resultsText.add(m_retreatAfterXRounds, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 5, 0, 0), 0, 0));
		
		resultsText.add(m_keepOneAttackingLandUnitCheckBox, new GridBagConstraints(0, row2++, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 5, 0, 5), 0, 0));
		if (true)
			resultsText.add(m_amphibiousCheckBox, new GridBagConstraints(0, row2++, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 5, 0, 5), 0, 0));
		resultsText.add(m_landBattleCheckBox, new GridBagConstraints(0, row2++, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 5, 0, 5), 0, 0));
		if (m_territoryEffectsJList != null)
			resultsText.add(new JScrollPane(m_territoryEffectsJList), new GridBagConstraints(0, row2++, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 5, 0, 5), 0, 0));
		resultsText.add(m_clearButton, new GridBagConstraints(0, row2++, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(30, 5, 0, 5), 0, 0));
		resultsText.add(m_calculateButton, new GridBagConstraints(0, row2++, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(25, 5, 0, 5), 0, 0));
		resultsText.add(m_SwapSidesButton, new GridBagConstraints(0, row2++, 2, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(25, 5, 10, 5), 0, 0));
		m_resultsPanel.add(resultsText);
		main.add(m_resultsPanel, BorderLayout.EAST);
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
				m_territoryEffectsJList.setVisibleRowCount(3);
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
		m_landBattleCheckBox = new JCheckBox("Land Battle");
		m_numRuns = new games.strategy.ui.IntTextField();
		m_numRuns.setColumns(4);
		m_numRuns.setMin(1);
		m_numRuns.setMax(20000);
		m_numRuns.setValue((games.strategy.triplea.Properties.getLow_Luck(m_data) ? 500 : 2000));
		m_retreatAfterXRounds = new games.strategy.ui.IntTextField();
		m_retreatAfterXRounds.setColumns(4);
		m_retreatAfterXRounds.setMin(1);
		m_retreatAfterXRounds.setMax(1000);
		m_retreatAfterXRounds.setValue(1000);
		m_calculateButton = new JButton("Calculate Odds");
		m_resultsPanel = new JPanel();
		final String blank = "------";
		m_attackerWin = new JLabel(blank);
		m_defenderWin = new JLabel(blank);
		m_draw = new JLabel(blank);
		m_defenderLeft = new JLabel(blank);
		m_attackerLeft = new JLabel(blank);
		m_defenderLeftWhenDefenderWon = new JLabel(blank);
		m_attackerLeftWhenAttackerWon = new JLabel(blank);
		m_roundsAverage = new JLabel(blank);
		m_count = new JLabel(blank);
		m_time = new JLabel(blank);
		m_closeButton = new JButton("Close");
		m_clearButton = new JButton("Clear");
		m_SwapSidesButton = new JButton("Swap Sides");
		m_keepOneAttackingLandUnitCheckBox = new JCheckBox("One attacking land must live");
		m_amphibiousCheckBox = new JCheckBox("Battle is Amphibious");
	}
	
	public void setWidgetActivation()
	{
		m_keepOneAttackingLandUnitCheckBox.setEnabled(m_landBattleCheckBox.isSelected());
		m_amphibiousCheckBox.setEnabled(m_landBattleCheckBox.isSelected());
		m_attackerUnitsTotal.setText("Num Units: " + m_attackingUnitsPanel.getUnits().size());
		m_defenderUnitsTotal.setText("Num Units: " + m_defendingUnitsPanel.getUnits().size());
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
	private final UIContext m_context;
	private final boolean m_defender;
	private final ListenerList<WidgetChangedListener> m_listeners = new ListenerList<WidgetChangedListener>();
	private final WidgetChangedListener m_listenerUnitPanel = new WidgetChangedListener()
	{
		public void widgetChanged()
		{
			notifyListeners();
		}
	};
	
	PlayerUnitsPanel(final GameData data, final UIContext context, final boolean defender)
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
	
	public void init(final PlayerID id, final List<Unit> units, final boolean land)
	{
		final List<UnitCategory> categories = new ArrayList<UnitCategory>(categorize(id, units));
		Collections.sort(categories, new Comparator<UnitCategory>()
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
		for (final UnitCategory category : categories)
		{
			if (predicate.match(category.getType()))
			{
				final UnitPanel upanel = new UnitPanel(m_data, m_context, category);
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
		rVal = Match.getMatches(rVal, Matches.UnitTypeCanBeInBattle(!m_defender, player, m_data, false, false));
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
	private final UIContext m_context;
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
	
	public UnitPanel(final GameData data, final UIContext context, final UnitCategory category)
	{
		m_category = category;
		m_context = context;
		m_data = data;
		m_textField = new ScrollableTextField(0, 512);
		m_textField.setShowMaxAndMin(false);
		m_textField.addChangeListener(m_listenerTextField);
		final Image img = m_context.getUnitImageFactory().getImage(m_category.getType(), m_category.getOwner(), m_data, m_category.getDamaged(), m_category.getDisabled());
		final String toolTipText = "<html>" + m_category.getType().getName() + ": " + m_category.getType().getTooltip(m_category.getOwner(), true) + "</html>";
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
			if (m_category.isTwoHit() && m_category.getDamaged())
			{
				// we do not need to use bridge and change factory here because this is not sent over the network. these are just some temporary units for the battle calc.
				for (final Unit u : units)
				{
					u.setHits(1);
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
