package games.strategy.triplea.oddsCalculator.ta;

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
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

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

import games.strategy.debug.ClientLogger;
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
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.IntTextField;
import games.strategy.ui.ScrollableTextField;
import games.strategy.ui.ScrollableTextFieldListener;
import games.strategy.ui.WidgetChangedListener;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

class OddsCalculatorPanel extends JPanel {
  private static final long serialVersionUID = -3559687618320469183L;
  private static final String NO_EFFECTS = "*None*";
  private final Window parent;
  private final JLabel attackerWin = new JLabel();
  private final JLabel defenderWin = new JLabel();
  private final JLabel draw = new JLabel();
  private final JLabel defenderLeft = new JLabel();
  private final JLabel attackerLeft = new JLabel();
  private final JLabel defenderLeftWhenDefenderWon = new JLabel();
  private final JLabel attackerLeftWhenAttackerWon = new JLabel();
  private final JLabel averageChangeInTuv = new JLabel();
  private final JLabel roundsAverage = new JLabel();
  private final JLabel count = new JLabel();
  private final JLabel time = new JLabel();
  private final IntTextField numRuns = new IntTextField();
  private final IntTextField retreatAfterXRounds = new IntTextField();
  private final IntTextField retreatAfterXUnitsLeft = new IntTextField();
  private final JPanel resultsPanel = new JPanel();
  private final JButton calculateButton = new JButton("Calculate Odds");
  private final JButton clearButton = new JButton("Clear");
  private final JButton closeButton = new JButton("Close");
  private final JButton swapSidesButton = new JButton("Swap Sides");
  private final JButton orderOfLossesButton = new JButton("Order Of Losses");
  private final JCheckBox keepOneAttackingLandUnitCheckBox = new JCheckBox("One attacking land must live");
  private final JCheckBox amphibiousCheckBox = new JCheckBox("Battle is Amphibious");
  private final JCheckBox landBattleCheckBox = new JCheckBox("Land Battle");
  private final JCheckBox retreatWhenOnlyAirLeftCheckBox = new JCheckBox("Retreat when only air left");
  private final UiContext uiContext;
  private final GameData data;
  private final IOddsCalculator calculator;
  private PlayerUnitsPanel attackingUnitsPanel;
  private PlayerUnitsPanel defendingUnitsPanel;
  private JComboBox<PlayerID> attackerCombo;
  private JComboBox<PlayerID> defenderCombo;
  private JComboBox<PlayerID> swapSidesCombo;
  private final JLabel attackerUnitsTotalNumber = new JLabel();
  private final JLabel defenderUnitsTotalNumber = new JLabel();
  private final JLabel attackerUnitsTotalTuv = new JLabel();
  private final JLabel defenderUnitsTotalTuv = new JLabel();
  private final JLabel attackerUnitsTotalHitpoints = new JLabel();
  private final JLabel defenderUnitsTotalHitpoints = new JLabel();
  private final JLabel attackerUnitsTotalPower = new JLabel();
  private final JLabel defenderUnitsTotalPower = new JLabel();
  private String attackerOrderOfLosses = null;
  private String defenderOrderOfLosses = null;
  private Territory location = null;
  private JList<String> territoryEffectsJList;
  private final WidgetChangedListener listenerPlayerUnitsPanel = () -> setWidgetActivation();

  OddsCalculatorPanel(final GameData data, final UiContext uiContext, final Territory location,
      final Window parent) {
    this.data = data;
    this.uiContext = uiContext;
    this.location = location;
    this.parent = parent;
    createComponents();
    layoutComponents();
    setupListeners();
    // use the one passed, not the one we found:
    if (location != null) {
      data.acquireReadLock();
      try {
        landBattleCheckBox.setSelected(!location.isWater());
        // default to the current player
        if (data.getSequence().getStep().getPlayerId() != null
            && !data.getSequence().getStep().getPlayerId().isNull()) {
          attackerCombo.setSelectedItem(data.getSequence().getStep().getPlayerId());
        }
        if (!location.isWater()) {
          defenderCombo.setSelectedItem(location.getOwner());
        } else {
          // we need to find out the defender for sea zones
          for (final PlayerID player : location.getUnits().getPlayersWithUnits()) {
            if (player != getAttacker() && !data.getRelationshipTracker().isAllied(player, getAttacker())) {
              defenderCombo.setSelectedItem(player);
              break;
            }
          }
        }
        updateDefender(location.getUnits().getMatches(Matches.alliedUnit(getDefender(), data)));
        updateAttacker(location.getUnits().getMatches(Matches.alliedUnit(getAttacker(), data)));
      } finally {
        data.releaseReadLock();
      }
    } else {
      landBattleCheckBox.setSelected(true);
      defenderCombo.setSelectedItem(data.getPlayerList().getPlayers().iterator().next());
      updateDefender(null);
      updateAttacker(null);
    }
    calculator = new OddsCalculator(data);
    setWidgetActivation();
    revalidate();
  }

  void shutdown() {
    try {
      // use this if not using a static calc, so that we gc the calc and shutdown all threads.
      // must be shutdown, as it has a thread pool per each instance.
      calculator.shutdown();
    } catch (final Exception e) {
      ClientLogger.logQuietly("Failed to shut down odds calculator", e);
    }
  }

  private PlayerID getDefender() {
    return (PlayerID) defenderCombo.getSelectedItem();
  }

  private PlayerID getAttacker() {
    return (PlayerID) attackerCombo.getSelectedItem();
  }

  private PlayerID getSwapSides() {
    return (PlayerID) swapSidesCombo.getSelectedItem();
  }

  private void setupListeners() {
    defenderCombo.addActionListener(e -> {
      data.acquireReadLock();
      try {
        if (data.getRelationshipTracker().isAllied(getDefender(), getAttacker())) {
          attackerCombo.setSelectedItem(getEnemy(getDefender()));
        }
      } finally {
        data.releaseReadLock();
      }
      updateDefender(null);
      setWidgetActivation();
    });
    attackerCombo.addActionListener(e -> {
      data.acquireReadLock();
      try {
        if (data.getRelationshipTracker().isAllied(getDefender(), getAttacker())) {
          defenderCombo.setSelectedItem(getEnemy(getAttacker()));
        }
      } finally {
        data.releaseReadLock();
      }
      updateAttacker(null);
      setWidgetActivation();
    });
    amphibiousCheckBox.addActionListener(e -> setWidgetActivation());
    landBattleCheckBox.addActionListener(e -> {
      attackerOrderOfLosses = null;
      defenderOrderOfLosses = null;
      updateDefender(null);
      updateAttacker(null);
      setWidgetActivation();
    });
    calculateButton.addActionListener(e -> updateStats());
    closeButton.addActionListener(e -> {
      attackerOrderOfLosses = null;
      defenderOrderOfLosses = null;
      parent.setVisible(false);
      shutdown();
      parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
    });
    clearButton.addActionListener(e -> {
      defendingUnitsPanel.clear();
      attackingUnitsPanel.clear();
      setWidgetActivation();
    });
    swapSidesButton.addActionListener(e -> {
      attackerOrderOfLosses = null;
      defenderOrderOfLosses = null;
      final List<Unit> getDefenders = defendingUnitsPanel.getUnits();
      final List<Unit> getAttackers = attackingUnitsPanel.getUnits();
      swapSidesCombo.setSelectedItem(getAttacker());
      attackerCombo.setSelectedItem(getDefender());
      defenderCombo.setSelectedItem(getSwapSides());
      attackingUnitsPanel.init(getAttacker(), getDefenders, isLand());
      defendingUnitsPanel.init(getDefender(), getAttackers, isLand());
      setWidgetActivation();
    });
    orderOfLossesButton.addActionListener(e -> {
      final OrderOfLossesInputPanel oolPanel = new OrderOfLossesInputPanel(attackerOrderOfLosses,
          defenderOrderOfLosses, attackingUnitsPanel.getCategories(), defendingUnitsPanel.getCategories(),
          landBattleCheckBox.isSelected(), uiContext, data);
      if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(OddsCalculatorPanel.this, oolPanel,
          "Create Order Of Losses for each side", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
        if (OddsCalculator.isValidOrderOfLoss(oolPanel.getAttackerOrder(), data)) {
          attackerOrderOfLosses = oolPanel.getAttackerOrder();
        }
        if (OddsCalculator.isValidOrderOfLoss(oolPanel.getDefenderOrder(), data)) {
          defenderOrderOfLosses = oolPanel.getDefenderOrder();
        }
      }
    });
    if (territoryEffectsJList != null) {
      territoryEffectsJList.addListSelectionListener(e -> setWidgetActivation());
    }
    attackingUnitsPanel.addChangeListener(listenerPlayerUnitsPanel);
    defendingUnitsPanel.addChangeListener(listenerPlayerUnitsPanel);
  }

  private boolean isAmphibiousBattle() {
    return (landBattleCheckBox.isSelected() && amphibiousCheckBox.isSelected());
  }

  private Collection<TerritoryEffect> getTerritoryEffects() {
    final Collection<TerritoryEffect> territoryEffects = new ArrayList<>();
    if (territoryEffectsJList != null) {
      final List<String> selected = territoryEffectsJList.getSelectedValuesList();
      data.acquireReadLock();
      try {
        final Hashtable<String, TerritoryEffect> allTerritoryEffects = data.getTerritoryEffectList();
        for (final String selection : selected) {
          if (selection.equals(NO_EFFECTS)) {
            territoryEffects.clear();
            break;
          }
          territoryEffects.add(allTerritoryEffects.get(selection));
        }
      } finally {
        data.releaseReadLock();
      }
    }
    return territoryEffects;
  }

  private void updateStats() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    final AtomicReference<AggregateResults> results = new AtomicReference<>();
    final WaitDialog dialog = new WaitDialog(
        this,
        "Calculating Odds",
        calculator::cancel);
    final AtomicReference<Collection<Unit>> defenders = new AtomicReference<>();
    final AtomicReference<Collection<Unit>> attackers = new AtomicReference<>();
    new Thread(() -> {
      try {
        // find a territory to fight in
        Territory location = null;
        if (this.location == null || this.location.isWater() == isLand()) {
          for (final Territory t : data.getMap()) {
            if (t.isWater() == !isLand()) {
              location = t;
              break;
            }
          }
        } else {
          location = this.location;
        }
        if (location == null) {
          throw new IllegalStateException("No territory found that is land:" + isLand());
        }
        final List<Unit> defending = defendingUnitsPanel.getUnits();
        final List<Unit> attacking = attackingUnitsPanel.getUnits();
        List<Unit> bombarding = new ArrayList<>();
        if (isLand()) {
          bombarding = CollectionUtils.getMatches(attacking, Matches.unitCanBombard(getAttacker()));
          attacking.removeAll(bombarding);
        }
        calculator.setRetreatAfterRound(retreatAfterXRounds.getValue());
        calculator.setRetreatAfterXUnitsLeft(retreatAfterXUnitsLeft.getValue());
        if (retreatWhenOnlyAirLeftCheckBox.isSelected()) {
          calculator.setRetreatWhenOnlyAirLeft(true);
        } else {
          calculator.setRetreatWhenOnlyAirLeft(false);
        }
        if (landBattleCheckBox.isSelected() && keepOneAttackingLandUnitCheckBox.isSelected()) {
          calculator.setKeepOneAttackingLandUnit(true);
        } else {
          calculator.setKeepOneAttackingLandUnit(false);
        }
        if (isAmphibiousBattle()) {
          calculator.setAmphibious(true);
        } else {
          calculator.setAmphibious(false);
        }
        calculator.setAttackerOrderOfLosses(attackerOrderOfLosses);
        calculator.setDefenderOrderOfLosses(defenderOrderOfLosses);
        final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
        defenders.set(defending);
        attackers.set(attacking);
        results.set(calculator.setCalculateDataAndCalculate(getAttacker(), getDefender(), location, attacking,
            defending, bombarding, territoryEffects, numRuns.getValue()));
      } finally {
        SwingUtilities.invokeLater(() -> {
          dialog.setVisible(false);
          dialog.dispose();
        });
      }
    }, "Odds calc thread").start();
    // the runnable setting the dialog visible must run after this code executes, since this code is running on the
    // swing event thread
    dialog.setVisible(true);
    // results.get() could be null if we cancelled to quickly or something weird like that.
    if (results.get() == null) {
      setResultsToBlank();
    } else {
      attackerWin.setText(formatPercentage(results.get().getAttackerWinPercent()));
      defenderWin.setText(formatPercentage(results.get().getDefenderWinPercent()));
      draw.setText(formatPercentage(results.get().getDrawPercent()));
      final boolean isLand = isLand();
      final List<Unit> mainCombatAttackers =
          CollectionUtils.getMatches(attackers.get(), Matches.unitCanBeInBattle(true, isLand, 1, false, true, true));
      final List<Unit> mainCombatDefenders =
          CollectionUtils.getMatches(defenders.get(), Matches.unitCanBeInBattle(false, isLand, 1, false, true, true));
      final int attackersTotal = mainCombatAttackers.size();
      final int defendersTotal = mainCombatDefenders.size();
      defenderLeft.setText(formatValue(results.get().getAverageDefendingUnitsLeft()) + " /" + defendersTotal);
      attackerLeft.setText(formatValue(results.get().getAverageAttackingUnitsLeft()) + " /" + attackersTotal);
      defenderLeftWhenDefenderWon
          .setText(formatValue(results.get().getAverageDefendingUnitsLeftWhenDefenderWon()) + " /" + defendersTotal);
      attackerLeftWhenAttackerWon
          .setText(formatValue(results.get().getAverageAttackingUnitsLeftWhenAttackerWon()) + " /" + attackersTotal);
      roundsAverage.setText("" + formatValue(results.get().getAverageBattleRoundsFought()));
      try {
        data.acquireReadLock();
        averageChangeInTuv.setText("" + formatValue(results.get().getAverageTuvSwing(getAttacker(),
            mainCombatAttackers, getDefender(), mainCombatDefenders, data)));
      } finally {
        data.releaseReadLock();
      }
      count.setText(results.get().getRollCount() + "");
      time.setText(formatValue(results.get().getTime() / 1000.0) + "s");
    }
  }

  String formatPercentage(final double percentage) {
    final NumberFormat format = new DecimalFormat("#%");
    return format.format(percentage);
  }

  String formatValue(final double value) {
    final NumberFormat format = new DecimalFormat("#0.##");
    return format.format(value);
  }

  private void updateDefender(List<Unit> units) {
    if (units == null) {
      units = Collections.emptyList();
    }
    final boolean isLand = isLand();
    units = CollectionUtils.getMatches(units, Matches.unitCanBeInBattle(false, isLand, 1, false, false, false));
    defendingUnitsPanel.init(getDefender(), units, isLand);
  }

  private void updateAttacker(List<Unit> units) {
    if (units == null) {
      units = Collections.emptyList();
    }
    final boolean isLand = isLand();
    units = CollectionUtils.getMatches(units, Matches.unitCanBeInBattle(true, isLand, 1, false, false, false));
    attackingUnitsPanel.init(getAttacker(), units, isLand);
  }

  private boolean isLand() {
    return landBattleCheckBox.isSelected();
  }

  private PlayerID getEnemy(final PlayerID player) {
    for (final PlayerID id : data.getPlayerList()) {
      if (data.getRelationshipTracker().isAtWar(player, id)) {
        return id;
      }
    }
    for (final PlayerID id : data.getPlayerList()) {
      if (!data.getRelationshipTracker().isAllied(player, id)) {
        return id;
      }
    }
    // TODO: do we allow fighting allies in the battle calc?
    throw new IllegalStateException("No enemies or non-allies for :" + player);
  }

  private void layoutComponents() {
    setLayout(new BorderLayout());
    final JPanel main = new JPanel();
    main.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    add(main, BorderLayout.CENTER);
    main.setLayout(new BorderLayout());
    final JPanel attackAndDefend = new JPanel();
    attackAndDefend.setLayout(new GridBagLayout());
    final int gap = 20;
    int row0 = 0;
    attackAndDefend.add(new JLabel("Attacker: "), new GridBagConstraints(0, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap, gap, 0), 0, 0));
    attackAndDefend.add(attackerCombo, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, gap / 2, gap), 0, 0));
    attackAndDefend.add(new JLabel("Defender: "), new GridBagConstraints(2, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap, gap, 0), 0, 0));
    attackAndDefend.add(defenderCombo, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, gap / 2, gap), 0, 0));
    row0++;
    attackAndDefend.add(attackerUnitsTotalNumber, new GridBagConstraints(0, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap, 0, 0), 0, 0));
    attackAndDefend.add(attackerUnitsTotalTuv, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap / 2, 0, gap * 2), 0, 0));
    attackAndDefend.add(defenderUnitsTotalNumber, new GridBagConstraints(2, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap, 0, 0), 0, 0));
    attackAndDefend.add(defenderUnitsTotalTuv, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap / 2, 0, gap * 2), 0, 0));
    row0++;
    attackAndDefend.add(attackerUnitsTotalHitpoints, new GridBagConstraints(0, row0, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap / 2, 0), 0, 0));
    attackAndDefend.add(attackerUnitsTotalPower, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap / 2, gap / 2, gap * 2), 0, 0));
    attackAndDefend.add(defenderUnitsTotalHitpoints, new GridBagConstraints(2, row0, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap / 2, 0), 0, 0));
    attackAndDefend.add(defenderUnitsTotalPower, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap / 2, gap / 2, gap * 2), 0, 0));
    row0++;
    final JScrollPane attackerScroll = new JScrollPane(attackingUnitsPanel);
    attackerScroll.setBorder(null);
    attackerScroll.getViewport().setBorder(null);
    final JScrollPane defenderScroll = new JScrollPane(defendingUnitsPanel);
    defenderScroll.setBorder(null);
    defenderScroll.getViewport().setBorder(null);
    attackAndDefend.add(attackerScroll, new GridBagConstraints(0, row0, 2, 1, 1, 1, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets(10, gap, gap, gap), 0, 0));
    attackAndDefend.add(defenderScroll, new GridBagConstraints(2, row0, 2, 1, 1, 1, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets(10, gap, gap, gap), 0, 0));
    main.add(attackAndDefend, BorderLayout.CENTER);
    final JPanel resultsText = new JPanel();
    resultsText.setLayout(new GridBagLayout());
    int row1 = 0;
    resultsText.add(new JLabel("Attacker Wins:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Draw:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Defender Wins:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Ave. Defender Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Units Left If Def Won:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Ave. Attacker Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Units Left If Att Won:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Average TUV Swing:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Average Rounds:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Simulation Count:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Time:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(calculateButton, new GridBagConstraints(0, row1++, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(20, 60, 0, 100), 0, 0));
    resultsText.add(clearButton, new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(6, 60, 0, 0), 0, 0));
    resultsText.add(new JLabel("Run Count:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(20, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Retreat After Round:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Retreat When X Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    int row2 = 0;
    resultsText.add(attackerWin, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(draw, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(defenderWin, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(defenderLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
    resultsText.add(defenderLeftWhenDefenderWon, new GridBagConstraints(1, row2++, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(attackerLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
    resultsText.add(attackerLeftWhenAttackerWon, new GridBagConstraints(1, row2++, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(averageChangeInTuv, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
    resultsText.add(roundsAverage, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(count, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(15, 10, 0, 0), 0, 0));
    resultsText.add(time, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    row2++;
    resultsText.add(swapSidesButton, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(6, 10, 0, 100), 0, 0));
    resultsText.add(numRuns, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(20, 10, 0, 0), 0, 0));
    resultsText.add(retreatAfterXRounds, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
    resultsText.add(retreatAfterXUnitsLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
    row1 = row2;
    resultsText.add(orderOfLossesButton, new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.BOTH, new Insets(10, 15, 0, 0), 0, 0));
    if (territoryEffectsJList != null) {
      resultsText.add(new JScrollPane(territoryEffectsJList),
          new GridBagConstraints(0, row1, 1, territoryEffectsJList.getVisibleRowCount(), 0, 0,
              GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(10, 15, 0, 0), 0, 0));
    }
    resultsText.add(retreatWhenOnlyAirLeftCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 5), 0, 0));
    resultsText.add(keepOneAttackingLandUnitCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
    resultsText.add(amphibiousCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
    resultsText.add(landBattleCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
    resultsPanel.add(resultsText);
    resultsPanel.setBorder(BorderFactory.createEmptyBorder());
    final JScrollPane resultsScroll = new JScrollPane(resultsPanel);
    resultsScroll.setBorder(BorderFactory.createEmptyBorder());
    final Dimension resultsScrollDimensions = resultsScroll.getPreferredSize();
    // add some so that we don't have double scroll bars appear when only one is needed
    resultsScrollDimensions.width += 22;
    resultsScroll.setPreferredSize(resultsScrollDimensions);
    main.add(resultsScroll, BorderLayout.EAST);
    final JPanel south = new JPanel();
    south.setLayout(new BorderLayout());
    final JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
    buttons.add(closeButton);
    south.add(buttons, BorderLayout.SOUTH);
    add(south, BorderLayout.SOUTH);
  }

  private void createComponents() {
    data.acquireReadLock();
    try {
      final Collection<PlayerID> playerList = new ArrayList<>(data.getPlayerList().getPlayers());
      if (doesPlayerHaveUnitsOnMap(PlayerID.NULL_PLAYERID, data)) {
        playerList.add(PlayerID.NULL_PLAYERID);
      }
      attackerCombo = new JComboBox<>(new Vector<>(playerList));
      defenderCombo = new JComboBox<>(new Vector<>(playerList));
      swapSidesCombo = new JComboBox<>(new Vector<>(playerList));
      final Hashtable<String, TerritoryEffect> allTerritoryEffects = data.getTerritoryEffectList();
      if (allTerritoryEffects == null || allTerritoryEffects.isEmpty()) {
        territoryEffectsJList = null;
      } else {
        final Vector<String> effectNames = new Vector<>();
        effectNames.add(NO_EFFECTS);
        effectNames.addAll(allTerritoryEffects.keySet());
        territoryEffectsJList = new JList<>(effectNames);
        territoryEffectsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        territoryEffectsJList.setLayoutOrientation(JList.VERTICAL);
        // equal to the amount of space left (number of remaining items on the right)
        territoryEffectsJList.setVisibleRowCount(4);
        if (location != null) {
          final Collection<TerritoryEffect> currentEffects = TerritoryEffectHelper.getEffects(location);
          if (!currentEffects.isEmpty()) {
            final int[] selectedIndexes = new int[currentEffects.size()];
            int currentIndex = 0;
            for (final TerritoryEffect te : currentEffects) {
              selectedIndexes[currentIndex] = effectNames.indexOf(te.getName());
              currentIndex++;
            }
            territoryEffectsJList.setSelectedIndices(selectedIndexes);
          }
        }
      }
    } finally {
      data.releaseReadLock();
    }
    defenderCombo.setRenderer(new PlayerRenderer());
    attackerCombo.setRenderer(new PlayerRenderer());
    swapSidesCombo.setRenderer(new PlayerRenderer());
    defendingUnitsPanel = new PlayerUnitsPanel(data, uiContext, true);
    attackingUnitsPanel = new PlayerUnitsPanel(data, uiContext, false);
    numRuns.setColumns(4);
    numRuns.setMin(1);
    numRuns.setMax(20000);

    final int simulationCount = Properties.getLowLuck(data)
        ? ClientSetting.BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK.intValue()
        : ClientSetting.BATTLE_CALC_SIMULATION_COUNT_DICE.intValue();
    numRuns.setValue(simulationCount);
    retreatAfterXRounds.setColumns(4);
    retreatAfterXRounds.setMin(-1);
    retreatAfterXRounds.setMax(1000);
    retreatAfterXRounds.setValue(-1);
    retreatAfterXRounds.setToolTipText("-1 means never.");
    retreatAfterXUnitsLeft.setColumns(4);
    retreatAfterXUnitsLeft.setMin(-1);
    retreatAfterXUnitsLeft.setMax(1000);
    retreatAfterXUnitsLeft.setValue(-1);
    retreatAfterXUnitsLeft.setToolTipText("-1 means never. If positive and 'retreat when only air left' is also "
        + "selected, then we will retreat when X of non-air units is left.");
    setResultsToBlank();
    defenderLeft.setToolTipText("Units Left does not include AA guns and other infrastructure, and does not include "
        + "Bombarding sea units for land battles.");
    attackerLeft.setToolTipText("Units Left does not include AA guns and other infrastructure, and does not include "
        + "Bombarding sea units for land battles.");
    defenderLeftWhenDefenderWon.setToolTipText("Units Left does not include AA guns and other infrastructure, and "
        + "does not include Bombarding sea units for land battles.");
    attackerLeftWhenAttackerWon.setToolTipText("Units Left does not include AA guns and other infrastructure, and "
        + "does not include Bombarding sea units for land battles.");
    averageChangeInTuv.setToolTipText("TUV Swing does not include captured AA guns and other infrastructure, and "
        + "does not include Bombarding sea units for land battles.");
    retreatWhenOnlyAirLeftCheckBox.setToolTipText("We retreat if only air is left, and if 'retreat when x units "
        + "left' is positive we will retreat when x of non-air is left too.");
    attackerUnitsTotalNumber.setToolTipText("Totals do not include AA guns and other infrastructure, and does not "
        + "include Bombarding sea units for land battles.");
    defenderUnitsTotalNumber.setToolTipText("Totals do not include AA guns and other infrastructure, and does not "
        + "include Bombarding sea units for land battles.");
  }

  private void setResultsToBlank() {
    final String blank = "------";
    attackerWin.setText(blank);
    defenderWin.setText(blank);
    draw.setText(blank);
    defenderLeft.setText(blank);
    attackerLeft.setText(blank);
    defenderLeftWhenDefenderWon.setText(blank);
    attackerLeftWhenAttackerWon.setText(blank);
    roundsAverage.setText(blank);
    averageChangeInTuv.setText(blank);
    count.setText(blank);
    time.setText(blank);
  }

  void setWidgetActivation() {
    keepOneAttackingLandUnitCheckBox.setEnabled(landBattleCheckBox.isSelected());
    amphibiousCheckBox.setEnabled(landBattleCheckBox.isSelected());
    final boolean isLand = isLand();
    try {
      data.acquireReadLock();
      // do not include bombardment and aa guns in our "total" labels
      final List<Unit> attackers = CollectionUtils.getMatches(attackingUnitsPanel.getUnits(),
          Matches.unitCanBeInBattle(true, isLand, 1, false, true, true));
      final List<Unit> defenders = CollectionUtils.getMatches(defendingUnitsPanel.getUnits(),
          Matches.unitCanBeInBattle(false, isLand, 1, false, true, true));
      attackerUnitsTotalNumber.setText("Units: " + attackers.size());
      defenderUnitsTotalNumber.setText("Units: " + defenders.size());
      attackerUnitsTotalTuv.setText("TUV: " + TuvUtils.getTuv(attackers, getAttacker(),
          TuvUtils.getCostsForTuv(getAttacker(), data), data));
      defenderUnitsTotalTuv.setText("TUV: " + TuvUtils.getTuv(defenders, getDefender(),
          TuvUtils.getCostsForTuv(getDefender(), data), data));
      final int attackHitPoints = BattleCalculator.getTotalHitpointsLeft(attackers);
      final int defenseHitPoints = BattleCalculator.getTotalHitpointsLeft(defenders);
      attackerUnitsTotalHitpoints.setText("HP: " + attackHitPoints);
      defenderUnitsTotalHitpoints.setText("HP: " + defenseHitPoints);
      final boolean isAmphibiousBattle = isAmphibiousBattle();
      final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
      final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(getAttacker(), data);
      Collections.sort(attackers, new UnitBattleComparator(false, costs, territoryEffects, data, false, false));
      Collections.reverse(attackers);
      final int attackPower = DiceRoll.getTotalPower(DiceRoll.getUnitPowerAndRollsForNormalBattles(attackers, defenders,
          false, false, data, location, territoryEffects, isAmphibiousBattle,
          (isAmphibiousBattle ? attackers : new ArrayList<>())), data);
      // defender is never amphibious
      final int defensePower =
          DiceRoll
              .getTotalPower(
                  DiceRoll.getUnitPowerAndRollsForNormalBattles(defenders, attackers, true, false,
                      data, location, territoryEffects, isAmphibiousBattle, new ArrayList<>()),
                  data);
      attackerUnitsTotalPower.setText("Power: " + attackPower);
      defenderUnitsTotalPower.setText("Power: " + defensePower);
    } finally {
      data.releaseReadLock();
    }
  }

  class PlayerRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -7639128794342607309L;

    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
        final boolean isSelected, final boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      final PlayerID id = (PlayerID) value;
      setText(id.getName());
      setIcon(new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(id)));
      return this;
    }
  }

  void selectCalculateButton() {
    calculateButton.requestFocus();
  }

  private static boolean doesPlayerHaveUnitsOnMap(final PlayerID player, final GameData data) {
    for (final Territory t : data.getMap()) {
      for (final Unit u : t.getUnits()) {
        if (u.getOwner().equals(player)) {
          return true;
        }
      }
    }
    return false;
  }

  private static final class PlayerUnitsPanel extends JPanel {
    private static final long serialVersionUID = -1206338960403314681L;
    private final GameData data;
    private final UiContext uiContext;
    private final boolean defender;
    private boolean isLand = true;
    private List<UnitCategory> categories = null;
    private final List<WidgetChangedListener> listeners = new ArrayList<>();
    private final WidgetChangedListener listenerUnitPanel = () -> notifyListeners();

    PlayerUnitsPanel(final GameData data, final UiContext uiContext, final boolean defender) {
      this.data = data;
      this.uiContext = uiContext;
      this.defender = defender;
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    void clear() {
      for (final Component c : getComponents()) {
        final UnitPanel panel = (UnitPanel) c;
        panel.setCount(0);
      }
    }

    List<Unit> getUnits() {
      final List<Unit> allUnits = new ArrayList<>();
      for (final Component c : getComponents()) {
        final UnitPanel panel = (UnitPanel) c;
        allUnits.addAll(panel.getUnits());
      }
      return allUnits;
    }

    List<UnitCategory> getCategories() {
      return categories;
    }

    void init(final PlayerID id, final List<Unit> units, final boolean land) {
      isLand = land;
      categories = new ArrayList<>(categorize(id, units));
      Collections.sort(categories, Comparator.comparing(UnitCategory::getType, (ut1, ut2) -> {
        final UnitAttachment u1 = UnitAttachment.get(ut1);
        final UnitAttachment u2 = UnitAttachment.get(ut2);
        // For land battles, sort by land, air, can't combat move (AA), bombarding
        if (land) {
          if (u1.getIsSea() != u2.getIsSea()) {
            return u1.getIsSea() ? 1 : -1;
          }
          final boolean u1CanNotCombatMove =
              Matches.unitTypeCanNotMoveDuringCombatMove().test(ut1) || !Matches.unitTypeCanMove(id).test(ut1);
          final boolean u2CanNotCombatMove =
              Matches.unitTypeCanNotMoveDuringCombatMove().test(ut2) || !Matches.unitTypeCanMove(id).test(ut2);
          if (u1CanNotCombatMove != u2CanNotCombatMove) {
            return u1CanNotCombatMove ? 1 : -1;
          }
          if (u1.getIsAir() != u2.getIsAir()) {
            return u1.getIsAir() ? 1 : -1;
          }
        } else {
          if (u1.getIsSea() != u2.getIsSea()) {
            return u1.getIsSea() ? -1 : 1;
          }
        }
        return u1.getName().compareTo(u2.getName());
      }));
      removeAll();
      final Predicate<UnitType> predicate;
      if (land) {
        if (defender) {
          predicate = Matches.unitTypeIsNotSea();
        } else {
          predicate = Matches.unitTypeIsNotSea().or(Matches.unitTypeCanBombard(id));
        }
      } else {
        predicate = Matches.unitTypeIsSeaOrAir();
      }
      final IntegerMap<UnitType> costs;
      try {
        data.acquireReadLock();
        costs = TuvUtils.getCostsForTuv(id, data);
      } finally {
        data.releaseReadLock();
      }
      for (final UnitCategory category : categories) {
        if (predicate.test(category.getType())) {
          final UnitPanel upanel = new UnitPanel(uiContext, category, costs);
          upanel.addChangeListener(listenerUnitPanel);
          add(upanel);
        }
      }
      invalidate();
      validate();
      revalidate();
      getParent().invalidate();
    }

    /**
     * Get all unit type categories that can be in combat first in the order of the player's
     * production frontier and then any unit types the player owns on the map. Then populate the list
     * of units into the categories.
     */
    private Set<UnitCategory> categorize(final PlayerID id, final List<Unit> units) {

      // Get all unit types from production frontier and player unit types on the map
      final Set<UnitCategory> categories = new LinkedHashSet<>();
      for (final UnitType t : getUnitTypes(id)) {
        final UnitCategory category = new UnitCategory(t, id);
        categories.add(category);
      }

      // Populate units into each category then add any remaining categories (damaged units, etc)
      final Set<UnitCategory> unitCategories = UnitSeperator.categorize(units);
      for (final UnitCategory category : categories) {
        for (final UnitCategory unitCategory : unitCategories) {
          if (category.equals(unitCategory)) {
            category.getUnits().addAll(unitCategory.getUnits());
          }
        }
      }
      categories.addAll(unitCategories);

      return categories;
    }

    /**
     * Return all the unit types available for the given player. A unit type is
     * available if the unit can be purchased or if a player has one on the map.
     */
    private Collection<UnitType> getUnitTypes(final PlayerID player) {
      Collection<UnitType> unitTypes = new LinkedHashSet<>();
      final ProductionFrontier frontier = player.getProductionFrontier();
      if (frontier != null) {
        for (final ProductionRule rule : frontier) {
          for (final NamedAttachable type : rule.getResults().keySet()) {
            if (type instanceof UnitType) {
              unitTypes.add((UnitType) type);
            }
          }
        }
      }
      for (final Territory t : data.getMap()) {
        for (final Unit u : t.getUnits()) {
          if (u.getOwner().equals(player)) {
            unitTypes.add(u.getType());
          }
        }
      }

      // Filter out anything like factories, or units that have no combat ability AND cannot be taken casualty
      unitTypes = CollectionUtils.getMatches(unitTypes,
          Matches.unitTypeCanBeInBattle(!defender, isLand, player, 1, false, false, false));

      return unitTypes;
    }

    void addChangeListener(final WidgetChangedListener listener) {
      listeners.add(listener);
    }

    private void notifyListeners() {
      for (final WidgetChangedListener listener : listeners) {
        listener.widgetChanged();
      }
    }
  }

  private static final class UnitPanel extends JPanel {
    private static final long serialVersionUID = 1509643150038705671L;
    private final UiContext uiContext;
    private final UnitCategory category;
    private final ScrollableTextField textField;
    private final List<WidgetChangedListener> listeners = new CopyOnWriteArrayList<>();
    private final ScrollableTextFieldListener listenerTextField = field -> notifyListeners();

    UnitPanel(final UiContext uiContext, final UnitCategory category, final IntegerMap<UnitType> costs) {
      this.category = category;
      this.uiContext = uiContext;
      textField = new ScrollableTextField(0, 512);
      textField.setShowMaxAndMin(false);
      textField.addChangeListener(listenerTextField);

      final String toolTipText = "<html>" + category.getType().getName() + ":  " + costs.getInt(category.getType())
          + " cost, <br /> &nbsp;&nbsp;&nbsp;&nbsp; " + category.getType().getTooltip(category.getOwner())
          + "</html>";
      setCount(category.getUnits().size());
      setLayout(new GridBagLayout());


      final Optional<Image> img =
          this.uiContext.getUnitImageFactory().getImage(category.getType(), category.getOwner(),
              category.hasDamageOrBombingUnitDamage(), category.getDisabled());

      final JLabel label = img.isPresent() ? new JLabel(new ImageIcon(img.get())) : new JLabel();
      label.setToolTipText(toolTipText);
      add(label, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 10), 0, 0));
      add(textField, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE,
          new Insets(0, 0, 0, 0), 0, 0));
    }

    List<Unit> getUnits() {
      final List<Unit> units = category.getType().create(textField.getValue(), category.getOwner(), true);
      if (!units.isEmpty()) {
        // creating the unit just makes it, we want to make sure it is damaged if the category says it is damaged
        if (category.getHitPoints() > 1 && category.getDamaged() > 0) {
          // we do not need to use bridge and change factory here because this is not sent over the network. these are
          // just some temporary
          // units for the battle calc.
          for (final Unit u : units) {
            u.setHits(category.getDamaged());
          }
        }
        if (category.getDisabled() && Matches.unitTypeCanBeDamaged().test(category.getType())) {
          // add 1 because it is the max operational damage and we want to disable it
          final int unitDamage = Math.max(0, 1 + UnitAttachment.get(category.getType()).getMaxOperationalDamage());
          for (final Unit u : units) {
            ((TripleAUnit) u).setUnitDamage(unitDamage);
          }
        }
      }
      return units;
    }

    void setCount(final int value) {
      textField.setValue(value);
    }

    void addChangeListener(final WidgetChangedListener listener) {
      listeners.add(listener);
    }

    private void notifyListeners() {
      for (final WidgetChangedListener listener : listeners) {
        listener.widgetChanged();
      }
    }
  }

  private static final class OrderOfLossesInputPanel extends JPanel {
    private static final long serialVersionUID = 8815617685388156219L;
    private final GameData data;
    private final UiContext uiContext;
    private final List<UnitCategory> attackerCategories;
    private final List<UnitCategory> defenderCategories;
    private final JTextField attackerTextField;
    private final JTextField defenderTextField;
    private final JLabel attackerLabel = new JLabel("Attacker Units:");
    private final JLabel defenderLabel = new JLabel("Defender Units:");
    private final JButton clear;
    private final boolean land;

    OrderOfLossesInputPanel(final String attackerOrder, final String defenderOrder,
        final List<UnitCategory> attackerCategories, final List<UnitCategory> defenderCategories, final boolean land,
        final UiContext uiContext, final GameData data) {
      this.data = data;
      this.uiContext = uiContext;
      this.land = land;
      this.attackerCategories = attackerCategories;
      this.defenderCategories = defenderCategories;
      attackerTextField = new JTextField(attackerOrder == null ? "" : attackerOrder);
      attackerTextField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(final DocumentEvent e) {
          if (!OddsCalculator.isValidOrderOfLoss(attackerTextField.getText(), OrderOfLossesInputPanel.this.data)) {
            attackerLabel.setForeground(Color.red);
          } else {
            attackerLabel.setForeground(null);
          }
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
          if (!OddsCalculator.isValidOrderOfLoss(attackerTextField.getText(), OrderOfLossesInputPanel.this.data)) {
            attackerLabel.setForeground(Color.red);
          } else {
            attackerLabel.setForeground(null);
          }
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
          if (!OddsCalculator.isValidOrderOfLoss(attackerTextField.getText(), OrderOfLossesInputPanel.this.data)) {
            attackerLabel.setForeground(Color.red);
          } else {
            attackerLabel.setForeground(null);
          }
        }
      });
      defenderTextField = new JTextField(defenderOrder == null ? "" : defenderOrder);
      defenderTextField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(final DocumentEvent e) {
          if (!OddsCalculator.isValidOrderOfLoss(defenderTextField.getText(), OrderOfLossesInputPanel.this.data)) {
            defenderLabel.setForeground(Color.red);
          } else {
            defenderLabel.setForeground(null);
          }
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
          if (!OddsCalculator.isValidOrderOfLoss(defenderTextField.getText(), OrderOfLossesInputPanel.this.data)) {
            defenderLabel.setForeground(Color.red);
          } else {
            defenderLabel.setForeground(null);
          }
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
          if (!OddsCalculator.isValidOrderOfLoss(defenderTextField.getText(), OrderOfLossesInputPanel.this.data)) {
            defenderLabel.setForeground(Color.red);
          } else {
            defenderLabel.setForeground(null);
          }
        }
      });
      clear = new JButton("Clear");
      clear.addActionListener(e -> {
        attackerTextField.setText("");
        defenderTextField.setText("");
      });
      layoutComponents();
    }

    private void layoutComponents() {
      this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      final JLabel instructions = new JLabel("<html>Here you can specify the 'Order of Losses' (OOL) for each side."
          + "<br />Damageable units will be damanged first always. If the player label is red, your OOL is invalid."
          + "<br />The engine will take your input and add all units to a list starting on the RIGHT side of your text "
          + "line."
          + "<br />Then, during combat, casualties will be chosen starting on the LEFT side of your OOL." + "<br />"
          + OddsCalculator.OOL_SEPARATOR + " separates unit types." + "<br />" + OddsCalculator.OOL_AMOUNT_DESCRIPTOR
          + " is in front of the unit type and describes the number of units." + "<br />" + OddsCalculator.OOL_ALL
          + " means all units of that type." + "<br />Examples:" + "<br />" + OddsCalculator.OOL_ALL
          + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "infantry" + OddsCalculator.OOL_SEPARATOR + OddsCalculator.OOL_ALL
          + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "artillery" + OddsCalculator.OOL_SEPARATOR + OddsCalculator.OOL_ALL
          + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "fighter"
          + "<br />The above will take all infantry, then all artillery, then all fighters, then all other units as "
          + "casualty."
          + "<br /><br />1" + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "infantry" + OddsCalculator.OOL_SEPARATOR + "2"
          + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "artillery" + OddsCalculator.OOL_SEPARATOR + "6"
          + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "fighter"
          + "<br />The above will take 1 infantry, then 2 artillery, then 6 fighters, then all other units as casualty."
          + "<br /><br />" + OddsCalculator.OOL_ALL + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "infantry"
          + OddsCalculator.OOL_SEPARATOR + OddsCalculator.OOL_ALL + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "fighter"
          + OddsCalculator.OOL_SEPARATOR + "1" + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + "infantry"
          + "<br />The above will take all except 1 infantry casualty, then all fighters, then the last infantry, then "
          + "all other units casualty.</html>");
      instructions.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.add(instructions);
      this.add(Box.createVerticalStrut(30));
      attackerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.add(attackerLabel);
      final JPanel attackerUnits = getUnitButtonPanel(attackerCategories, attackerTextField);
      attackerUnits.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.add(attackerUnits);
      attackerTextField.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.add(attackerTextField);
      this.add(Box.createVerticalStrut(30));
      defenderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.add(defenderLabel);
      final JPanel defenderUnits = getUnitButtonPanel(defenderCategories, defenderTextField);
      defenderUnits.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.add(defenderUnits);
      defenderTextField.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.add(defenderTextField);
      this.add(Box.createVerticalStrut(10));
      clear.setAlignmentX(Component.CENTER_ALIGNMENT);
      this.add(clear);
    }

    private JPanel getUnitButtonPanel(final List<UnitCategory> categories, final JTextField textField) {
      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
      if (categories != null) {
        final Set<UnitType> typesUsed = new HashSet<>();
        for (final UnitCategory category : categories) {
          // no duplicates or infrastructure allowed. no sea if land, no land if sea.
          if (typesUsed.contains(category.getType()) || Matches.unitTypeIsInfrastructure().test(category.getType())
              || (land && Matches.unitTypeIsSea().test(category.getType()))
              || (!land && Matches.unitTypeIsLand().test(category.getType()))) {
            continue;
          }
          final String unitName =
              OddsCalculator.OOL_ALL + OddsCalculator.OOL_AMOUNT_DESCRIPTOR + category.getType().getName();
          final String toolTipText = "<html>" + category.getType().getName() + ":  "
              + category.getType().getTooltip(category.getOwner()) + "</html>";
          final Optional<Image> img =
              uiContext.getUnitImageFactory().getImage(category.getType(), category.getOwner(),
                  category.hasDamageOrBombingUnitDamage(), category.getDisabled());
          if (img.isPresent()) {
            final JButton button = new JButton(new ImageIcon(img.get()));
            button.setToolTipText(toolTipText);
            button.addActionListener(e -> textField
                .setText((textField.getText().length() > 0 ? (textField.getText() + OddsCalculator.OOL_SEPARATOR) : "")
                    + unitName));
            panel.add(button);
          }
          typesUsed.add(category.getType());
        }
      }
      return panel;
    }

    String getAttackerOrder() {
      return attackerTextField.getText();
    }

    String getDefenderOrder() {
      return defenderTextField.getText();
    }
  }
}
