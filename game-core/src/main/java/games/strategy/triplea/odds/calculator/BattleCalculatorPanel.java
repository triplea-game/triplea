package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.ui.background.WaitDialog;
import games.strategy.engine.history.History;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.UnitBattleComparator;
import games.strategy.triplea.delegate.battle.casualty.CasualtyUtil;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.BorderFactory;
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
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import lombok.extern.java.Log;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.IntTextField;
import org.triplea.swing.SwingComponents;

@Log
class BattleCalculatorPanel extends JPanel {
  private static final long serialVersionUID = -3559687618320469183L;
  private static final String NO_EFFECTS = "*None*";
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
  private final JButton calculateButton = new JButton("Pls Wait, Copying Data...");
  private final JCheckBox keepOneAttackingLandUnitCheckBox =
      new JCheckBox("One attacking land must live");
  private final JCheckBox amphibiousCheckBox = new JCheckBox("Add amphibious attack modifiers");
  private final JCheckBox landBattleCheckBox = new JCheckBox("Land battle");
  private final JCheckBox retreatWhenOnlyAirLeftCheckBox =
      new JCheckBox("Retreat when only air left");
  private final UiContext uiContext;
  private final GameData data;
  private final ConcurrentBattleCalculator calculator;
  private final PlayerUnitsPanel attackingUnitsPanel;
  private final PlayerUnitsPanel defendingUnitsPanel;
  private final JComboBox<GamePlayer> attackerCombo;
  private final JComboBox<GamePlayer> defenderCombo;
  private final JComboBox<GamePlayer> swapSidesCombo;
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
  private final Territory location;
  private final JList<String> territoryEffectsJList;

  BattleCalculatorPanel(
      final GameData data,
      final History history,
      final UiContext uiContext,
      final Territory location) {
    this.data = data;
    this.uiContext = uiContext;
    this.location = location;
    calculateButton.setEnabled(false);
    data.acquireReadLock();
    try {
      final Collection<GamePlayer> playerList = new ArrayList<>(data.getPlayerList().getPlayers());
      if (doesPlayerHaveUnitsOnMap(GamePlayer.NULL_PLAYERID, data)) {
        playerList.add(GamePlayer.NULL_PLAYERID);
      }
      attackerCombo = new JComboBox<>(SwingComponents.newComboBoxModel(playerList));
      defenderCombo = new JComboBox<>(SwingComponents.newComboBoxModel(playerList));
      swapSidesCombo = new JComboBox<>(SwingComponents.newComboBoxModel(playerList));
      final Map<String, TerritoryEffect> allTerritoryEffects = data.getTerritoryEffectList();
      if (allTerritoryEffects == null || allTerritoryEffects.isEmpty()) {
        territoryEffectsJList = null;
      } else {
        final List<String> effectNames = new ArrayList<>();
        effectNames.add(NO_EFFECTS);
        effectNames.addAll(allTerritoryEffects.keySet());
        territoryEffectsJList = new JList<>(SwingComponents.newListModel(effectNames));
        territoryEffectsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        territoryEffectsJList.setLayoutOrientation(JList.VERTICAL);
        // equal to the amount of space left (number of remaining items on the right)
        territoryEffectsJList.setVisibleRowCount(4);
        if (location != null) {
          final Collection<TerritoryEffect> currentEffects =
              TerritoryEffectHelper.getEffects(location);
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

    final int simulationCount =
        Properties.getLowLuck(data)
            ? ClientSetting.battleCalcSimulationCountLowLuck.getValueOrThrow()
            : ClientSetting.battleCalcSimulationCountDice.getValueOrThrow();
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
    retreatAfterXUnitsLeft.setToolTipText(
        "-1 means never. If positive and 'retreat when only air left' is also "
            + "selected, then we will retreat when X of non-air units is left.");
    setResultsToBlank();
    defenderLeft.setToolTipText(
        "Units Left does not include AA guns and other infrastructure, and does not include "
            + "Bombarding sea units for land battles.");
    attackerLeft.setToolTipText(
        "Units Left does not include AA guns and other infrastructure, and does not include "
            + "Bombarding sea units for land battles.");
    defenderLeftWhenDefenderWon.setToolTipText(
        "Units Left does not include AA guns and other infrastructure, and "
            + "does not include Bombarding sea units for land battles.");
    attackerLeftWhenAttackerWon.setToolTipText(
        "Units Left does not include AA guns and other infrastructure, and "
            + "does not include Bombarding sea units for land battles.");
    averageChangeInTuv.setToolTipText(
        "TUV Swing does not include captured AA guns and other infrastructure, and "
            + "does not include Bombarding sea units for land battles.");
    retreatWhenOnlyAirLeftCheckBox.setToolTipText(
        "We retreat if only air is left, and if 'retreat when x units "
            + "left' is positive we will retreat when x of non-air is left too.");
    amphibiousCheckBox.setToolTipText(
        "Applies amphibious attack modifiers to all attacking land units");
    attackerUnitsTotalNumber.setToolTipText(
        "Totals do not include AA guns and other infrastructure, and does not "
            + "include Bombarding sea units for land battles.");
    defenderUnitsTotalNumber.setToolTipText(
        "Totals do not include AA guns and other infrastructure, and does not "
            + "include Bombarding sea units for land battles.");

    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    final JPanel main = new JPanel();
    main.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    main.setLayout(new BorderLayout());
    add(main);

    final JPanel attackAndDefend = new JPanel();
    attackAndDefend.setLayout(new GridBagLayout());
    final int gap = 20;
    int row0 = 0;
    attackAndDefend.add(
        new JLabel("Attacker: "),
        new GridBagConstraints(
            0,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap, gap, 0),
            0,
            0));
    attackAndDefend.add(
        attackerCombo,
        new GridBagConstraints(
            1,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, gap / 2, gap),
            0,
            0));
    attackAndDefend.add(
        new JLabel("Defender: "),
        new GridBagConstraints(
            2,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap, gap, 0),
            0,
            0));
    attackAndDefend.add(
        defenderCombo,
        new GridBagConstraints(
            3,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, gap / 2, gap),
            0,
            0));
    row0++;
    attackAndDefend.add(
        attackerUnitsTotalNumber,
        new GridBagConstraints(
            0,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap, 0, 0),
            0,
            0));
    attackAndDefend.add(
        attackerUnitsTotalTuv,
        new GridBagConstraints(
            1,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap / 2, 0, gap * 2),
            0,
            0));
    attackAndDefend.add(
        defenderUnitsTotalNumber,
        new GridBagConstraints(
            2,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap, 0, 0),
            0,
            0));
    attackAndDefend.add(
        defenderUnitsTotalTuv,
        new GridBagConstraints(
            3,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap / 2, 0, gap * 2),
            0,
            0));
    row0++;
    attackAndDefend.add(
        attackerUnitsTotalHitpoints,
        new GridBagConstraints(
            0,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap, gap / 2, 0),
            0,
            0));
    attackAndDefend.add(
        attackerUnitsTotalPower,
        new GridBagConstraints(
            1,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap / 2, gap / 2, gap * 2),
            0,
            0));
    attackAndDefend.add(
        defenderUnitsTotalHitpoints,
        new GridBagConstraints(
            2,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap, gap / 2, 0),
            0,
            0));
    attackAndDefend.add(
        defenderUnitsTotalPower,
        new GridBagConstraints(
            3,
            row0,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, gap / 2, gap / 2, gap * 2),
            0,
            0));
    final JPanel attackAndDefendAlignLeft = new JPanel();
    attackAndDefendAlignLeft.setLayout(new BorderLayout());
    attackAndDefendAlignLeft.add(attackAndDefend, BorderLayout.WEST);

    final JPanel unitPanels = new JPanel();
    unitPanels.setLayout(new BoxLayout(unitPanels, BoxLayout.X_AXIS));
    final JScrollPane attackerScroll = new JScrollPane(attackingUnitsPanel);
    attackerScroll.setBorder(null);
    attackerScroll.getViewport().setBorder(null);
    attackerScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    final JScrollPane defenderScroll = new JScrollPane(defendingUnitsPanel);
    defenderScroll.setBorder(null);
    defenderScroll.getViewport().setBorder(null);
    defenderScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    unitPanels.add(attackerScroll);
    unitPanels.add(defenderScroll);

    final JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BorderLayout());
    leftPanel.add(attackAndDefendAlignLeft, BorderLayout.NORTH);
    leftPanel.add(unitPanels, BorderLayout.CENTER);
    main.add(leftPanel, BorderLayout.CENTER);

    final JPanel resultsText = new JPanel();
    resultsText.setLayout(new GridBagLayout());
    int row1 = 0;
    resultsText.add(
        new JLabel("Attacker Wins:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Draw:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Defender Wins:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Ave. Defender Units Left:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(6, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Units Left If Def Won:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Ave. Attacker Units Left:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(6, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Units Left If Att Won:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Average TUV Swing:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(6, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Average Rounds:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Simulation Count:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(15, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Time:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(0, 0, 0, 0),
            0,
            0));
    resultsText.add(
        calculateButton,
        new GridBagConstraints(
            0,
            row1++,
            2,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.BOTH,
            new Insets(20, 60, 0, 100),
            0,
            0));
    final JButton clearButton = new JButton("Clear");
    resultsText.add(
        clearButton,
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.BOTH,
            new Insets(6, 60, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Run Count:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(20, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Retreat After Round:"),
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));
    resultsText.add(
        new JLabel("Retreat When X Units Left:"),
        new GridBagConstraints(
            0,
            row1,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.NONE,
            new Insets(10, 0, 0, 0),
            0,
            0));
    int row2 = 0;
    resultsText.add(
        attackerWin,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 10, 0, 0),
            0,
            0));
    resultsText.add(
        draw,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 10, 0, 0),
            0,
            0));
    resultsText.add(
        defenderWin,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 10, 0, 0),
            0,
            0));
    resultsText.add(
        defenderLeft,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(6, 10, 0, 0),
            0,
            0));
    resultsText.add(
        defenderLeftWhenDefenderWon,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 10, 0, 0),
            0,
            0));
    resultsText.add(
        attackerLeft,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(6, 10, 0, 0),
            0,
            0));
    resultsText.add(
        attackerLeftWhenAttackerWon,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 10, 0, 0),
            0,
            0));
    resultsText.add(
        averageChangeInTuv,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(6, 10, 0, 0),
            0,
            0));
    resultsText.add(
        roundsAverage,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 10, 0, 0),
            0,
            0));
    resultsText.add(
        count,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(15, 10, 0, 0),
            0,
            0));
    resultsText.add(
        time,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(0, 10, 0, 0),
            0,
            0));
    row2++;
    final JButton swapSidesButton = new JButton("Swap Sides");
    resultsText.add(
        swapSidesButton,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.BOTH,
            new Insets(6, 10, 0, 100),
            0,
            0));
    resultsText.add(
        numRuns,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(20, 10, 0, 0),
            0,
            0));
    resultsText.add(
        retreatAfterXRounds,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(10, 10, 0, 0),
            0,
            0));
    resultsText.add(
        retreatAfterXUnitsLeft,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(10, 10, 0, 0),
            0,
            0));
    row1 = row2;

    final JButton orderOfLossesButton = new JButton("Order Of Losses");
    resultsText.add(
        orderOfLossesButton,
        new GridBagConstraints(
            0,
            row1++,
            1,
            1,
            0,
            0,
            GridBagConstraints.EAST,
            GridBagConstraints.BOTH,
            new Insets(10, 15, 0, 0),
            0,
            0));
    if (territoryEffectsJList != null) {
      resultsText.add(
          new JScrollPane(territoryEffectsJList),
          new GridBagConstraints(
              0,
              row1,
              1,
              territoryEffectsJList.getVisibleRowCount(),
              0,
              0,
              GridBagConstraints.EAST,
              GridBagConstraints.BOTH,
              new Insets(10, 15, 0, 0),
              0,
              0));
    }
    resultsText.add(
        retreatWhenOnlyAirLeftCheckBox,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(10, 10, 0, 5),
            0,
            0));
    resultsText.add(
        keepOneAttackingLandUnitCheckBox,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(2, 10, 0, 5),
            0,
            0));
    resultsText.add(
        amphibiousCheckBox,
        new GridBagConstraints(
            1,
            row2++,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(2, 10, 0, 5),
            0,
            0));
    resultsText.add(
        landBattleCheckBox,
        new GridBagConstraints(
            1,
            row2,
            1,
            1,
            0,
            0,
            GridBagConstraints.WEST,
            GridBagConstraints.NONE,
            new Insets(2, 10, 0, 5),
            0,
            0));

    final JPanel resultsPanel = new JPanel();
    resultsPanel.add(resultsText);
    resultsPanel.setBorder(BorderFactory.createEmptyBorder());
    final JScrollPane resultsScroll = new JScrollPane(resultsPanel);
    resultsScroll.setBorder(BorderFactory.createEmptyBorder());
    final Dimension resultsScrollDimensions = resultsScroll.getPreferredSize();
    // add some so that we don't have double scroll bars appear when only one is needed
    resultsScrollDimensions.width += 22;
    resultsScroll.setPreferredSize(resultsScrollDimensions);
    main.add(resultsScroll, BorderLayout.EAST);

    final JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
    final JButton closeButton = new JButton("Close");
    buttons.add(closeButton);
    add(buttons);

    defenderCombo.addActionListener(
        e -> {
          data.acquireReadLock();
          try {
            if (data.getRelationshipTracker().isAllied(getDefender(), getAttacker())) {
              attackerCombo.setSelectedItem(getEnemy(getDefender()));
            }
          } finally {
            data.releaseReadLock();
          }
          setDefendingUnits(
              defendingUnitsPanel.getUnits().stream().anyMatch(Matches.unitIsOwnedBy(getDefender()))
                  ? defendingUnitsPanel.getUnits()
                  : null);
          setWidgetActivation();
        });
    attackerCombo.addActionListener(
        e -> {
          data.acquireReadLock();
          try {
            if (data.getRelationshipTracker().isAllied(getDefender(), getAttacker())) {
              defenderCombo.setSelectedItem(getEnemy(getAttacker()));
            }
          } finally {
            data.releaseReadLock();
          }
          setAttackingUnits(null);
          setWidgetActivation();
        });
    amphibiousCheckBox.addActionListener(e -> setWidgetActivation());
    landBattleCheckBox.addActionListener(
        e -> {
          attackerOrderOfLosses = null;
          defenderOrderOfLosses = null;
          setDefendingUnits(null);
          setAttackingUnits(null);
          setWidgetActivation();
        });
    calculateButton.addActionListener(e -> updateStats());
    closeButton.addActionListener(
        e -> {
          attackerOrderOfLosses = null;
          defenderOrderOfLosses = null;
          final Window parent = SwingUtilities.getWindowAncestor(BattleCalculatorPanel.this);
          if (parent != null) {
            parent.setVisible(false);
          }
          if (parent != null) {
            parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
          }
        });
    clearButton.addActionListener(
        e -> {
          defendingUnitsPanel.clear();
          attackingUnitsPanel.clear();
          setWidgetActivation();
        });
    swapSidesButton.addActionListener(
        e -> {
          attackerOrderOfLosses = null;
          defenderOrderOfLosses = null;
          final List<Unit> newAttackers =
              CollectionUtils.getMatches(
                  defendingUnitsPanel.getUnits(),
                  Matches.unitIsOwnedBy(getDefender())
                      .and(
                          Matches.unitCanBeInBattle(
                              true, isLand(), 1, hasMaxRounds(isLand(), data), true, List.of())));
          final List<Unit> newDefenders =
              CollectionUtils.getMatches(
                  attackingUnitsPanel.getUnits(),
                  Matches.unitCanBeInBattle(true, isLand(), 1, true));
          swapSidesCombo.setSelectedItem(getAttacker());
          attackerCombo.setSelectedItem(getDefender());
          defenderCombo.setSelectedItem(getSwapSides());
          setAttackingUnits(newAttackers);
          setDefendingUnits(newDefenders);
          setWidgetActivation();
        });
    orderOfLossesButton.addActionListener(
        e -> {
          final OrderOfLossesInputPanel oolPanel =
              new OrderOfLossesInputPanel(
                  attackerOrderOfLosses,
                  defenderOrderOfLosses,
                  attackingUnitsPanel.getCategories(),
                  defendingUnitsPanel.getCategories(),
                  landBattleCheckBox.isSelected(),
                  uiContext,
                  data);
          if (JOptionPane.OK_OPTION
              == JOptionPane.showConfirmDialog(
                  BattleCalculatorPanel.this,
                  oolPanel,
                  "Create Order Of Losses for each side",
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.PLAIN_MESSAGE)) {
            if (OrderOfLossesInputPanel.isValidOrderOfLoss(oolPanel.getAttackerOrder(), data)) {
              attackerOrderOfLosses = oolPanel.getAttackerOrder();
            }
            if (OrderOfLossesInputPanel.isValidOrderOfLoss(oolPanel.getDefenderOrder(), data)) {
              defenderOrderOfLosses = oolPanel.getDefenderOrder();
            }
          }
        });
    if (territoryEffectsJList != null) {
      territoryEffectsJList.addListSelectionListener(e -> setWidgetActivation());
    }
    attackingUnitsPanel.addChangeListener(this::setWidgetActivation);
    defendingUnitsPanel.addChangeListener(this::setWidgetActivation);

    // use the one passed, not the one we found:
    if (location != null) {
      data.acquireReadLock();
      try {
        landBattleCheckBox.setSelected(!location.isWater());

        // Default attacker to current player
        final Optional<GamePlayer> currentPlayer = getCurrentPlayer(history);
        currentPlayer.ifPresent(attackerCombo::setSelectedItem);

        // Get players with units sorted
        final List<GamePlayer> players = location.getUnitCollection().getPlayersByUnitCount();
        if (currentPlayer.isPresent() && players.contains(currentPlayer.get())) {
          players.remove(currentPlayer.get());
          players.add(0, currentPlayer.get());
        }

        // Check location to determine optimal attacker and defender
        if (!location.isWater()) {
          defenderCombo.setSelectedItem(location.getOwner());
          for (final GamePlayer player : players) {
            if (Matches.isAtWar(getDefender(), data).test(player)) {
              attackerCombo.setSelectedItem(player);
              break;
            }
          }
        } else {
          if (players.size() == 1) {
            defenderCombo.setSelectedItem(players.get(0));
          } else if (players.size() > 1) {
            if (!data.getRelationshipTracker()
                .isAtWarWithAnyOfThesePlayers(players.get(0), players)) {
              defenderCombo.setSelectedItem(players.get(0));
            } else {
              attackerCombo.setSelectedItem(players.get(0));
              for (final GamePlayer player : players) {
                if (Matches.isAtWar(getAttacker(), data).test(player)) {
                  defenderCombo.setSelectedItem(player);
                  break;
                }
              }
            }
          }
        }

        setAttackingUnits(
            location.getUnitCollection().getMatches(Matches.unitIsOwnedBy(getAttacker())));
        setDefendingUnits(
            location.getUnitCollection().getMatches(Matches.alliedUnit(getDefender(), data)));
      } finally {
        data.releaseReadLock();
      }
    } else {
      landBattleCheckBox.setSelected(true);
      defenderCombo.setSelectedItem(data.getPlayerList().getPlayers().iterator().next());
      setDefendingUnits(null);
      setAttackingUnits(null);
    }
    calculator =
        new ConcurrentBattleCalculator(
            () ->
                SwingUtilities.invokeLater(
                    () -> {
                      calculateButton.setText("Calculate Odds");
                      calculateButton.setEnabled(true);
                    }));

    calculator.setGameData(data);
    setWidgetActivation();
    revalidate();
  }

  private Optional<GamePlayer> getCurrentPlayer(final History history) {
    final Optional<GamePlayer> player = history.getActivePlayer();
    if (player.isPresent()) {
      return player;
    }
    return GamePlayer.asOptional(data.getSequence().getStep().getPlayerId());
  }

  GamePlayer getAttacker() {
    return (GamePlayer) attackerCombo.getSelectedItem();
  }

  GamePlayer getDefender() {
    return (GamePlayer) defenderCombo.getSelectedItem();
  }

  private GamePlayer getSwapSides() {
    return (GamePlayer) swapSidesCombo.getSelectedItem();
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
        final Map<String, TerritoryEffect> allTerritoryEffects = data.getTerritoryEffectList();
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
    final WaitDialog dialog =
        new WaitDialog(this, "Calculating Odds... (this may take a while)", calculator::cancel);
    final AtomicReference<Collection<Unit>> defenders = new AtomicReference<>();
    final AtomicReference<Collection<Unit>> attackers = new AtomicReference<>();
    new Thread(
            () -> {
              try {
                final Territory location = findPotentialBattleSite();
                if (location == null) {
                  throw new IllegalStateException("No territory found that is land:" + isLand());
                }
                final List<Unit> defending = defendingUnitsPanel.getUnits();
                final List<Unit> attacking = attackingUnitsPanel.getUnits();
                List<Unit> bombarding = new ArrayList<>();
                if (isLand()) {
                  bombarding =
                      CollectionUtils.getMatches(attacking, Matches.unitCanBombard(getAttacker()));
                  attacking.removeAll(bombarding);
                  final int numLandUnits =
                      CollectionUtils.countMatches(attacking, Matches.unitIsLand());
                  if (Properties.getShoreBombardPerGroundUnitRestricted(data)
                      && numLandUnits < bombarding.size()) {
                    BattleDelegate.sortUnitsToBombard(bombarding);
                    // Create new list as needs to be serializable which subList isn't
                    bombarding = new ArrayList<>(bombarding.subList(0, numLandUnits));
                  }
                }
                calculator.setRetreatAfterRound(retreatAfterXRounds.getValue());
                calculator.setRetreatAfterXUnitsLeft(retreatAfterXUnitsLeft.getValue());
                calculator.setKeepOneAttackingLandUnit(
                    landBattleCheckBox.isSelected()
                        && keepOneAttackingLandUnitCheckBox.isSelected());
                calculator.setAmphibious(isAmphibiousBattle());
                calculator.setAttackerOrderOfLosses(attackerOrderOfLosses);
                calculator.setDefenderOrderOfLosses(defenderOrderOfLosses);
                final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
                defenders.set(defending);
                attackers.set(attacking);
                results.set(
                    calculator.calculate(
                        getAttacker(),
                        getDefender(),
                        location,
                        attacking,
                        defending,
                        bombarding,
                        territoryEffects,
                        retreatWhenOnlyAirLeftCheckBox.isSelected(),
                        numRuns.getValue()));
              } finally {
                SwingUtilities.invokeLater(
                    () -> {
                      dialog.setVisible(false);
                      dialog.dispose();
                    });
              }
            },
            "Battle calculator thread")
        .start();
    // the runnable setting the dialog visible must run after this code executes, since this code is
    // running on the
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
          CollectionUtils.getMatches(
              attackers.get(), Matches.unitCanBeInBattle(true, isLand, 1, true));
      final List<Unit> mainCombatDefenders =
          CollectionUtils.getMatches(
              defenders.get(), Matches.unitCanBeInBattle(false, isLand, 1, true));
      final int attackersTotal = mainCombatAttackers.size();
      final int defendersTotal = mainCombatDefenders.size();
      defenderLeft.setText(
          formatValue(results.get().getAverageDefendingUnitsLeft()) + " / " + defendersTotal);
      attackerLeft.setText(
          formatValue(results.get().getAverageAttackingUnitsLeft()) + " / " + attackersTotal);
      defenderLeftWhenDefenderWon.setText(
          formatValue(results.get().getAverageDefendingUnitsLeftWhenDefenderWon())
              + " / "
              + defendersTotal);
      attackerLeftWhenAttackerWon.setText(
          formatValue(results.get().getAverageAttackingUnitsLeftWhenAttackerWon())
              + " / "
              + attackersTotal);
      roundsAverage.setText("" + formatValue(results.get().getAverageBattleRoundsFought()));
      try {
        data.acquireReadLock();
        averageChangeInTuv.setText(
            ""
                + formatValue(
                    results
                        .get()
                        .getAverageTuvSwing(
                            getAttacker(),
                            mainCombatAttackers,
                            getDefender(),
                            mainCombatDefenders,
                            data)));
      } finally {
        data.releaseReadLock();
      }
      count.setText(results.get().getRollCount() + "");
      time.setText(formatValue(results.get().getTime() / 1000.0) + " s");
    }
  }

  private Territory findPotentialBattleSite() {
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
    return location;
  }

  private static String formatPercentage(final double percentage) {
    return new DecimalFormat("#%").format(percentage);
  }

  private static String formatValue(final double value) {
    return new DecimalFormat("#0.##").format(value);
  }

  void addAttackingUnits(final List<Unit> unitsToAdd) {
    final List<Unit> units = attackingUnitsPanel.getUnits();
    units.addAll(unitsToAdd);
    setAttackingUnits(units);
    setWidgetActivation();
  }

  private void setAttackingUnits(final List<Unit> initialUnits) {
    final List<Unit> units = Optional.ofNullable(initialUnits).orElseGet(List::of);
    attackingUnitsPanel.init(
        getAttacker(),
        CollectionUtils.getMatches(
            units,
            Matches.unitCanBeInBattle(
                true, isLand(), 1, hasMaxRounds(isLand(), data), false, List.of())),
        isLand());
  }

  void addDefendingUnits(final List<Unit> unitsToAdd) {
    final List<Unit> units = defendingUnitsPanel.getUnits();
    units.addAll(unitsToAdd);
    setDefendingUnits(units);
    setWidgetActivation();
  }

  private void setDefendingUnits(final List<Unit> initialUnits) {
    final List<Unit> units = Optional.ofNullable(initialUnits).orElseGet(List::of);
    defendingUnitsPanel.init(
        getDefender(),
        CollectionUtils.getMatches(units, Matches.unitCanBeInBattle(false, isLand(), 1, false)),
        isLand());
  }

  private boolean isLand() {
    return landBattleCheckBox.isSelected();
  }

  private GamePlayer getEnemy(final GamePlayer player) {
    for (final GamePlayer gamePlayer : data.getPlayerList()) {
      if (data.getRelationshipTracker().isAtWar(player, gamePlayer)) {
        return gamePlayer;
      }
    }
    for (final GamePlayer gamePlayer : data.getPlayerList()) {
      if (!data.getRelationshipTracker().isAllied(player, gamePlayer)) {
        return gamePlayer;
      }
    }
    // TODO: do we allow fighting allies in the battle calc?
    throw new IllegalStateException("No enemies or non-allies for :" + player);
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

  private void setWidgetActivation() {
    keepOneAttackingLandUnitCheckBox.setEnabled(landBattleCheckBox.isSelected());
    amphibiousCheckBox.setEnabled(landBattleCheckBox.isSelected());
    final boolean isLand = isLand();
    try {
      data.acquireReadLock();
      final List<Unit> attackers =
          CollectionUtils.getMatches(
              attackingUnitsPanel.getUnits(), Matches.unitCanBeInBattle(true, isLand, 1, true));
      final List<Unit> defenders =
          CollectionUtils.getMatches(
              defendingUnitsPanel.getUnits(), Matches.unitCanBeInBattle(false, isLand, 1, true));
      attackerUnitsTotalNumber.setText("Units: " + attackers.size());
      defenderUnitsTotalNumber.setText("Units: " + defenders.size());
      attackerUnitsTotalTuv.setText(
          "TUV: "
              + TuvUtils.getTuv(
                  attackers, getAttacker(), TuvUtils.getCostsForTuv(getAttacker(), data), data));
      defenderUnitsTotalTuv.setText(
          "TUV: "
              + TuvUtils.getTuv(
                  defenders, getDefender(), TuvUtils.getCostsForTuv(getDefender(), data), data));
      final int attackHitPoints = CasualtyUtil.getTotalHitpointsLeft(attackers);
      final int defenseHitPoints = CasualtyUtil.getTotalHitpointsLeft(defenders);
      attackerUnitsTotalHitpoints.setText("HP: " + attackHitPoints);
      defenderUnitsTotalHitpoints.setText("HP: " + defenseHitPoints);
      final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
      final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(getAttacker(), data);
      attackers.sort(
          new UnitBattleComparator(
                  costs,
                  data,
                  PowerStrengthAndRolls.build(
                      attackers,
                      CombatValue.buildNoSupportCombatValue(false, data, territoryEffects)))
              .reversed());
      final Territory location = findPotentialBattleSite();
      if (isAmphibiousBattle()) {
        attackers.stream()
            .filter(Matches.unitIsLand())
            .forEach(
                unit -> {
                  final Optional<MutableProperty<?>> property =
                      unit.getProperty(Unit.UNLOADED_AMPHIBIOUS);
                  if (property.isPresent()) {
                    try {
                      property.get().setValue(true);
                    } catch (final MutableProperty.InvalidValueException e) {
                      // ignore units that can't be unloaded
                    }
                  }
                });
      }
      final int attackPower =
          PowerStrengthAndRolls.build(
                  attackers,
                  CombatValue.buildMainCombatValue(
                      defenders, attackers, false, data, location, territoryEffects))
              .calculateTotalPower();
      // defender is never amphibious
      final int defensePower =
          PowerStrengthAndRolls.build(
                  defenders,
                  CombatValue.buildMainCombatValue(
                      attackers, defenders, true, data, location, territoryEffects))
              .calculateTotalPower();
      attackerUnitsTotalPower.setText("Power: " + attackPower);
      defenderUnitsTotalPower.setText("Power: " + defensePower);
    } finally {
      data.releaseReadLock();
    }
  }

  class PlayerRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -7639128794342607309L;

    @Override
    public Component getListCellRendererComponent(
        final JList<?> list,
        final Object value,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      final GamePlayer gamePlayer = (GamePlayer) value;
      setText(gamePlayer.getName());
      setIcon(new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(gamePlayer)));
      return this;
    }
  }

  void selectCalculateButton() {
    calculateButton.requestFocus();
  }

  private static boolean doesPlayerHaveUnitsOnMap(final GamePlayer player, final GameData data) {
    for (final Territory t : data.getMap()) {
      for (final Unit u : t.getUnitCollection()) {
        if (u.getOwner().equals(player)) {
          return true;
        }
      }
    }
    return false;
  }

  static boolean hasMaxRounds(final boolean isLand, final GameData data) {
    data.acquireReadLock();
    try {
      return isLand
          ? Properties.getLandBattleRounds(data) > 0
          : Properties.getSeaBattleRounds(data) > 0;
    } finally {
      data.releaseReadLock();
    }
  }
}
