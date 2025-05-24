package games.strategy.triplea.odds.calculator;

import static java.text.MessageFormat.format;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.ui.background.WaitDialog;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.casualty.CasualtyUtil;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvCostsCalculator;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.java.ThreadRunner;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.swing.IntTextField;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;

@Slf4j
class BattleCalculatorPanel extends JPanel {
  private static final long serialVersionUID = -3559687618320469183L;
  private static final @NonNls String NO_EFFECTS = "*None*";
  private final JLabel attackerWin = new JLabel();
  private final JLabel defenderWin = new JLabel();
  private final JLabel draw = new JLabel();
  private final @NonNls JLabel defenderLeft = new JLabel();
  private final @NonNls JLabel attackerLeft = new JLabel();
  private final JLabel defenderLeftWhenDefenderWon = new JLabel();
  private final JLabel attackerLeftWhenAttackerWon = new JLabel();
  private final JLabel averageChangeInTuv = new JLabel();
  private final JLabel roundsAverage = new JLabel();
  private final JLabel count = new JLabel();
  private final @NonNls JLabel time = new JLabel();
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
  private final JLabel attackerUnitsTotalNumber = new JLabel();
  private final JLabel defenderUnitsTotalNumber = new JLabel();
  private final JLabel attackerUnitsTotalTuv = new JLabel();
  private final JLabel defenderUnitsTotalTuv = new JLabel();
  private final JLabel attackerUnitsTotalHitPoints = new JLabel();
  private final JLabel defenderUnitsTotalHitPoints = new JLabel();
  private final JLabel attackerUnitsTotalPower = new JLabel();
  private final JLabel defenderUnitsTotalPower = new JLabel();
  private String attackerOrderOfLosses = null;
  private String defenderOrderOfLosses = null;
  @Nullable private final Territory battleSiteTerritory;
  private final JList<String> territoryEffectsJList;
  private final TuvCostsCalculator tuvCalculator = new TuvCostsCalculator();

  BattleCalculatorPanel(
      final GameData data,
      final UiContext uiContext,
      @Nullable final Territory battleSiteTerritory) {
    this.data = data;
    this.uiContext = uiContext;
    this.battleSiteTerritory = battleSiteTerritory;
    calculateButton.setEnabled(false);
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final Collection<GamePlayer> playerList = new ArrayList<>(data.getPlayerList().getPlayers());
      if (doesPlayerHaveUnitsOnMap(data.getPlayerList().getNullPlayer(), data)) {
        playerList.add(data.getPlayerList().getNullPlayer());
      }
      attackerCombo = new JComboBox<>(SwingComponents.newComboBoxModel(playerList));
      defenderCombo = new JComboBox<>(SwingComponents.newComboBoxModel(playerList));
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
        if (battleSiteTerritory != null) {
          final Collection<TerritoryEffect> currentEffects =
              TerritoryEffectHelper.getEffects(battleSiteTerritory);
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
    }
    defenderCombo.setRenderer(new PlayerRenderer());
    attackerCombo.setRenderer(new PlayerRenderer());
    defendingUnitsPanel = new PlayerUnitsPanel(data, uiContext, true);
    attackingUnitsPanel = new PlayerUnitsPanel(data, uiContext, false);
    numRuns.setColumns(4);
    numRuns.setMin(1);
    numRuns.setMax(20000);

    final int simulationCount =
        Properties.getLowLuck(data.getProperties())
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
    GridBagConstraintsBuilder builder0 =
        new GridBagConstraintsBuilder().gridY(row0++).anchor(GridBagConstraintsAnchor.EAST);
    attackAndDefend.add(new JLabel("Attacker: "), builder0.gridX(0).insets(0, gap, gap, 0).build());
    attackAndDefend.add(attackerCombo, builder0.gridX(1).insets(0, 0, gap / 2, gap).build());
    attackAndDefend.add(new JLabel("Defender: "), builder0.gridX(2).insets(0, gap, gap, 0).build());
    attackAndDefend.add(defenderCombo, builder0.gridX(3).insets(0, 0, gap / 2, gap).build());
    attackAndDefend.add(
        attackerUnitsTotalNumber, builder0.gridY(row0++).gridX(0).insets(0, gap, 0, 0).build());
    attackAndDefend.add(
        attackerUnitsTotalTuv, builder0.gridX(1).insets(0, gap / 2, 0, gap * 2).build());
    attackAndDefend.add(defenderUnitsTotalNumber, builder0.gridX(2).insets(0, gap, 0, 0).build());
    attackAndDefend.add(
        defenderUnitsTotalTuv, builder0.gridX(3).insets(0, gap / 2, 0, gap * 2).build());
    attackAndDefend.add(
        attackerUnitsTotalHitPoints,
        builder0.gridY(row0).gridX(0).insets(0, gap, gap / 2, 0).build());
    attackAndDefend.add(
        attackerUnitsTotalPower, builder0.gridX(1).insets(0, gap / 2, gap / 2, gap * 2).build());
    attackAndDefend.add(
        defenderUnitsTotalHitPoints, builder0.gridX(2).insets(0, gap, gap / 2, 0).build());
    attackAndDefend.add(
        defenderUnitsTotalPower, builder0.gridX(3).insets(0, gap / 2, gap / 2, gap * 2).build());
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
    GridBagConstraintsBuilder builder1 =
        new GridBagConstraintsBuilder().anchor(GridBagConstraintsAnchor.EAST);
    resultsText.add(new JLabel("Attacker Wins:"), builder1.gridY(row1++).build());
    resultsText.add(new JLabel("Draw:"), builder1.gridY(row1++).build());
    resultsText.add(new JLabel("Defender Wins:"), builder1.gridY(row1++).build());
    resultsText.add(
        new JLabel("Ave. Defender Units Left:"), builder1.gridY(row1++).insets(6, 0, 0, 0).build());
    resultsText.add(
        new JLabel("Units Left If Def Won:"), builder1.gridY(row1++).insets(0, 0, 0, 0).build());
    resultsText.add(
        new JLabel("Ave. Attacker Units Left:"), builder1.gridY(row1++).insets(6, 0, 0, 0).build());
    resultsText.add(
        new JLabel("Units Left If Att Won:"), builder1.gridY(row1++).insets(0, 0, 0, 0).build());
    resultsText.add(
        new JLabel("Average TUV Swing:"), builder1.gridY(row1++).insets(6, 0, 0, 0).build());
    resultsText.add(
        new JLabel("Average Rounds:"), builder1.gridY(row1++).insets(0, 0, 0, 0).build());
    resultsText.add(
        new JLabel("Simulation Count:"), builder1.gridY(row1++).insets(15, 0, 0, 0).build());
    resultsText.add(new JLabel("Time:"), builder1.gridY(row1++).insets(0, 0, 0, 0).build());
    GridBagConstraintsBuilder builder2 =
        new GridBagConstraintsBuilder()
            .gridWidth(2)
            .anchor(GridBagConstraintsAnchor.WEST)
            .fill(GridBagConstraintsFill.BOTH);
    resultsText.add(
        calculateButton, builder2.gridY(row1++).gridWidth(2).insets(20, 60, 0, 100).build());
    final JButton clearButton = new JButton("Clear");
    resultsText.add(clearButton, builder2.gridY(row1++).gridWidth(1).insets(6, 60, 0, 0).build());
    resultsText.add(new JLabel("Run Count:"), builder1.gridY(row1++).insets(20, 0, 0, 0).build());
    resultsText.add(
        new JLabel("Retreat After Round:"), builder1.gridY(row1++).insets(10, 0, 0, 0).build());
    resultsText.add(new JLabel("Retreat When X Units Left:"), builder1.gridY(row1).build());
    GridBagConstraintsBuilder builder3 =
        new GridBagConstraintsBuilder()
            .gridX(1)
            .anchor(GridBagConstraintsAnchor.WEST)
            .insets(0, 10, 0, 0);
    int row2 = 0;
    resultsText.add(attackerWin, builder3.gridY(row2++).build());
    resultsText.add(draw, builder3.gridY(row2++).build());
    resultsText.add(defenderWin, builder3.gridY(row2++).build());
    resultsText.add(defenderLeft, builder3.gridY(row2++).build());
    resultsText.add(defenderLeftWhenDefenderWon, builder3.gridY(row2++).build());
    resultsText.add(attackerLeft, builder3.gridY(row2++).insets(6, 10, 0, 0).build());
    resultsText.add(
        attackerLeftWhenAttackerWon, builder3.gridY(row2++).insets(0, 10, 0, 0).build());
    resultsText.add(averageChangeInTuv, builder3.gridY(row2++).insets(6, 10, 0, 0).build());
    resultsText.add(roundsAverage, builder3.gridY(row2++).insets(0, 10, 0, 0).build());
    resultsText.add(count, builder3.gridY(row2++).insets(15, 10, 0, 0).build());
    resultsText.add(time, builder3.gridY(row2++).insets(0, 10, 0, 0).build());

    row2++;
    final JButton swapSidesButton = new JButton("Swap Sides");
    resultsText.add(
        swapSidesButton,
        builder3.gridY(row2++).fill(GridBagConstraintsFill.BOTH).insets(6, 10, 0, 100).build());
    resultsText.add(
        numRuns,
        builder3.gridY(row2++).fill(GridBagConstraintsFill.NONE).insets(20, 10, 0, 0).build());
    resultsText.add(retreatAfterXRounds, builder3.gridY(row2++).insets(10, 10, 0, 0).build());
    resultsText.add(retreatAfterXUnitsLeft, builder3.gridY(row2++).build());

    row1 = row2;
    final JButton orderOfLossesButton = new JButton("Order Of Losses");
    resultsText.add(
        orderOfLossesButton, builder1.gridX(0).gridY(row1++).insets(10, 15, 0, 0).build());
    if (territoryEffectsJList != null) {
      resultsText.add(
          new JScrollPane(territoryEffectsJList),
          builder1
              .gridY(row1)
              .gridHeight(territoryEffectsJList.getVisibleRowCount())
              .fill(GridBagConstraintsFill.BOTH)
              .build());
    }
    resultsText.add(
        retreatWhenOnlyAirLeftCheckBox,
        builder1
            .gridX(1)
            .gridY(row2++)
            .gridHeight(1)
            .anchor(GridBagConstraintsAnchor.WEST)
            .fill(GridBagConstraintsFill.NONE)
            .insets(10, 10, 0, 5)
            .build());
    resultsText.add(
        keepOneAttackingLandUnitCheckBox, builder1.gridY(row2++).insets(2, 10, 0, 5).build());
    resultsText.add(amphibiousCheckBox, builder1.gridY(row2++).build());
    resultsText.add(landBattleCheckBox, builder1.gridY(row2).build());

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

    amphibiousCheckBox.addActionListener(e -> setWidgetActivation());
    landBattleCheckBox.addActionListener(
        e -> {
          attackerOrderOfLosses = null;
          defenderOrderOfLosses = null;
          setDefendingUnits(List.of());
          setAttackingUnits(List.of());
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
          final GamePlayer newAttacker = getDefender();
          final List<Unit> newAttackerUnits =
              CollectionUtils.getMatches(
                  defendingUnitsPanel.getUnits(),
                  Matches.unitIsOwnedBy(getDefender())
                      .and(
                          Matches.unitCanBeInBattle(
                              true,
                              isLandBattle(),
                              1,
                              hasMaxRounds(isLandBattle(), data),
                              true,
                              List.of())));
          final GamePlayer newDefender = getAttacker();
          final List<Unit> newDefenderUnits =
              CollectionUtils.getMatches(
                  attackingUnitsPanel.getUnits(),
                  Matches.unitCanBeInBattle(true, isLandBattle(), 1, true));
          setAttackerWithUnits(newAttacker, newAttackerUnits);
          setDefenderWithUnits(newDefender, newDefenderUnits);
          setWidgetActivation();
        });
    orderOfLossesButton.addActionListener(
        e -> {
          final OrderOfLossesInputPanel oolPanel =
              new OrderOfLossesInputPanel(
                  attackerOrderOfLosses,
                  defenderOrderOfLosses,
                  attackingUnitsPanel.getUnitCategories(),
                  defendingUnitsPanel.getUnitCategories(),
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

    // Note: Setting landBattleCheckBox resets the units. Thus, set the units after this.
    if (battleSiteTerritory == null) {
      landBattleCheckBox.setSelected(true);
    } else {
      landBattleCheckBox.setSelected(!battleSiteTerritory.isWater());
    }
    setupAttackerAndDefender();

    final Instant start = Instant.now();
    calculator = new ConcurrentBattleCalculator();

    calculator
        .setGameData(data)
        .thenAccept(
            bool -> {
              if (Boolean.TRUE.equals(bool)) {
                long millis = Duration.between(start, Instant.now()).toMillis();
                SwingUtilities.invokeLater(
                    () -> {
                      log.debug("Battle Calculator ready in {}ms", millis);
                      calculateButton.setText("Calculate Odds");
                      calculateButton.setEnabled(true);
                      calculateButton.requestFocusInWindow();
                    });
              }
            });
    setWidgetActivation();
    revalidate();
  }

  private void setupAttackerAndDefender() {
    final AttackerAndDefenderSelector.AttackerAndDefender attAndDef =
        AttackerAndDefenderSelector.builder()
            .players(data.getPlayerList().getPlayers())
            .currentPlayer(data.getHistory().getCurrentPlayer())
            .relationshipTracker(data.getRelationshipTracker())
            .territory(battleSiteTerritory)
            .build()
            .getAttackerAndDefender();

    attAndDef.getAttacker().ifPresent(this::setAttacker);
    attAndDef.getDefender().ifPresent(this::setDefender);
    setAttackingUnits(attAndDef.getAttackingUnits());
    setDefendingUnits(attAndDef.getDefendingUnits());
  }

  GamePlayer getAttacker() {
    return (GamePlayer) attackerCombo.getSelectedItem();
  }

  private void setAttacker(final GamePlayer gamePlayer) {
    attackerCombo.setSelectedItem(gamePlayer);
  }

  GamePlayer getDefender() {
    return (GamePlayer) defenderCombo.getSelectedItem();
  }

  private void setDefender(final GamePlayer gamePlayer) {
    defenderCombo.setSelectedItem(gamePlayer);
  }

  private boolean isAmphibiousBattle() {
    return (landBattleCheckBox.isSelected() && amphibiousCheckBox.isSelected());
  }

  private Collection<TerritoryEffect> getTerritoryEffects() {
    final Collection<TerritoryEffect> territoryEffects = new ArrayList<>();
    if (territoryEffectsJList != null) {
      final List<String> selected = territoryEffectsJList.getSelectedValuesList();
      try (GameData.Unlocker ignored = data.acquireReadLock()) {
        final Map<String, TerritoryEffect> allTerritoryEffects = data.getTerritoryEffectList();
        for (final String selection : selected) {
          if (selection.equals(NO_EFFECTS)) {
            territoryEffects.clear();
            break;
          }
          territoryEffects.add(allTerritoryEffects.get(selection));
        }
      }
    }
    return territoryEffects;
  }

  private void updateStats() {
    Util.ensureOnEventDispatchThread();
    final AtomicReference<AggregateResults> resultsRef = new AtomicReference<>();
    final WaitDialog dialog =
        new WaitDialog(this, "Calculating Odds... (this may take a while)", calculator::cancel);
    final AtomicReference<Collection<Unit>> defenders = new AtomicReference<>();
    final AtomicReference<Collection<Unit>> attackers = new AtomicReference<>();
    final GamePlayer attacker = getAttacker();
    final GamePlayer defender = getDefender();
    ThreadRunner.runInNewThread(
        () -> {
          try {
            final Territory newBattleSiteTerritory = findPotentialBattleSite();
            final List<Unit> defending = defendingUnitsPanel.getUnits();
            final List<Unit> attacking = attackingUnitsPanel.getUnits();
            List<Unit> bombarding = new ArrayList<>();
            if (isLandBattle()) {
              bombarding =
                  CollectionUtils.getMatches(attacking, Matches.unitCanBombard(getAttacker()));
              attacking.removeAll(bombarding);
              final int numLandUnits =
                  CollectionUtils.countMatches(attacking, Matches.unitIsLand());
              if (Properties.getShoreBombardPerGroundUnitRestricted(data.getProperties())
                  && numLandUnits < bombarding.size()) {
                BattleDelegate.sortUnitsToBombard(bombarding);
                // Create new list as needs to be serializable which subList isn't
                bombarding = new ArrayList<>(bombarding.subList(0, numLandUnits));
              }
            }
            calculator.setRetreatAfterRound(retreatAfterXRounds.getValue());
            calculator.setRetreatAfterXUnitsLeft(retreatAfterXUnitsLeft.getValue());
            calculator.setKeepOneAttackingLandUnit(
                landBattleCheckBox.isSelected() && keepOneAttackingLandUnitCheckBox.isSelected());
            calculator.setAmphibious(isAmphibiousBattle());
            calculator.setAttackerOrderOfLosses(attackerOrderOfLosses);
            calculator.setDefenderOrderOfLosses(defenderOrderOfLosses);
            final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
            defenders.set(defending);
            attackers.set(attacking);
            resultsRef.set(
                calculator.calculate(
                    attacker,
                    defender,
                    newBattleSiteTerritory,
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
        });
    // the runnable setting the dialog visible must run after this code executes, since this code is
    // running on the swing event thread
    dialog.setVisible(true);
    // results.get() could be null if we cancelled to quickly or something weird like that.
    final var results = resultsRef.get();
    if (results == null) {
      setResultsToBlank();
      return;
    }
    // All AggregateResults method return NaN if there are no battle results to aggregate over.
    // For "unrestricted" average methods, this cannot happen as we ensure that at least 1 round
    // is simulated. However, the ...IfAbcWon() methods restrict that set of results which might
    // become empty. In this case we display N/A (not applicable) instead of NaN (not a number).
    attackerWin.setText(formatPercentage(results.getAttackerWinPercent()));
    defenderWin.setText(formatPercentage(results.getDefenderWinPercent()));
    draw.setText(formatPercentage(results.getDrawPercent()));
    final boolean isLand = isLandBattle();
    final List<Unit> mainCombatAttackers =
        CollectionUtils.getMatches(
            attackers.get(), Matches.unitCanBeInBattle(true, isLand, 1, true));
    final List<Unit> mainCombatDefenders =
        CollectionUtils.getMatches(
            defenders.get(), Matches.unitCanBeInBattle(false, isLand, 1, true));
    final int attackersTotal = mainCombatAttackers.size();
    final int defendersTotal = mainCombatDefenders.size();
    defenderLeft.setText(
        getRelationNumberText(results.getAverageDefendingUnitsLeft(), defendersTotal));
    attackerLeft.setText(
        getRelationNumberText(results.getAverageAttackingUnitsLeft(), attackersTotal));
    final double avgDefIfDefWon = results.getAverageDefendingUnitsLeftWhenDefenderWon();
    defenderLeftWhenDefenderWon.setText(
        Double.isNaN(avgDefIfDefWon)
            ? "N/A"
            : getRelationNumberText(avgDefIfDefWon, defendersTotal));
    final double avgAttIfAttWon = results.getAverageAttackingUnitsLeftWhenAttackerWon();
    attackerLeftWhenAttackerWon.setText(
        Double.isNaN(avgAttIfAttWon)
            ? "N/A"
            : getRelationNumberText(avgAttIfAttWon, attackersTotal));
    roundsAverage.setText(formatValue(results.getAverageBattleRoundsFought()));
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      double tuvSwing =
          results.getAverageTuvSwing(
              attacker, mainCombatAttackers, defender, mainCombatDefenders, data);
      averageChangeInTuv.setText(formatValue(tuvSwing));
    }
    count.setText(results.getRollCount() + "");
    time.setText(formatValue(results.getTime() / 1000.0) + " s");
  }

  private static @Nonnull @NonNls String getRelationNumberText(double results, int defendersTotal) {
    return formatValue(results) + " / " + defendersTotal;
  }

  private Territory findPotentialBattleSite() {
    Territory newBattleSiteTerritory = null;
    if (this.battleSiteTerritory == null || this.battleSiteTerritory.isWater() == isLandBattle()) {
      for (final Territory t : data.getMap()) {
        if (t.isWater() == !isLandBattle()) {
          newBattleSiteTerritory = t;
          break;
        }
      }
    } else {
      newBattleSiteTerritory = this.battleSiteTerritory;
    }
    if (newBattleSiteTerritory == null) {
      throw new IllegalStateException(
          format("No territory found that is land: {0}", isLandBattle()));
    }
    return newBattleSiteTerritory;
  }

  private static String formatPercentage(final double percentage) {
    return new DecimalFormat("#%.##").format(percentage);
  }

  private static String formatValue(final double value) {
    return new DecimalFormat("#0.##").format(value);
  }

  public void setAttackerWithUnits(final GamePlayer attacker, final List<Unit> initialUnits) {
    setAttacker(attacker);
    setAttackingUnits(initialUnits);
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
                true, isLandBattle(), 1, hasMaxRounds(isLandBattle(), data), false, List.of())),
        isLandBattle(),
        battleSiteTerritory);
  }

  public void setDefenderWithUnits(final GamePlayer defender, final List<Unit> initialUnits) {
    setDefender(defender);
    setDefendingUnits(initialUnits);
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
        CollectionUtils.getMatches(
            units, Matches.unitCanBeInBattle(false, isLandBattle(), 1, false)),
        isLandBattle(),
        battleSiteTerritory);
  }

  public boolean hasAttackingUnits() {
    return attackingUnitsPanel.hasNoneZeroUnitEntries();
  }

  public boolean hasDefendingUnits() {
    return defendingUnitsPanel.hasNoneZeroUnitEntries();
  }

  private boolean isLandBattle() {
    return landBattleCheckBox.isSelected();
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
    final boolean isLand = isLandBattle();
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      final List<Unit> attackers =
          CollectionUtils.getMatches(
              attackingUnitsPanel.getUnits(), Matches.unitCanBeInBattle(true, isLand, 1, true));
      final List<Unit> defenders =
          CollectionUtils.getMatches(
              defendingUnitsPanel.getUnits(), Matches.unitCanBeInBattle(false, isLand, 1, true));
      attackerUnitsTotalNumber.setText(getTotalNumberText("Units: ", attackers.size()));
      defenderUnitsTotalNumber.setText(getTotalNumberText("Units: ", defenders.size()));
      attackerUnitsTotalTuv.setText(
          getTotalNumberText(
              "TUV: ",
              TuvUtils.getTuv(
                  attackers, getAttacker(), tuvCalculator.getCostsForTuv(getAttacker()), data)));
      defenderUnitsTotalTuv.setText(
          getTotalNumberText(
              "TUV: ",
              TuvUtils.getTuv(
                  defenders, getDefender(), tuvCalculator.getCostsForTuv(getDefender()), data)));
      final int attackHitPoints = CasualtyUtil.getTotalHitpointsLeft(attackers);
      final int defenseHitPoints = CasualtyUtil.getTotalHitpointsLeft(defenders);
      attackerUnitsTotalHitPoints.setText(getTotalNumberText("HP: ", attackHitPoints));
      defenderUnitsTotalHitPoints.setText(getTotalNumberText("HP: ", defenseHitPoints));
      final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
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
                  CombatValueBuilder.mainCombatValue()
                      .enemyUnits(defenders)
                      .friendlyUnits(attackers)
                      .side(BattleState.Side.OFFENSE)
                      .gameSequence(data.getSequence())
                      .supportAttachments(data.getUnitTypeList().getSupportRules())
                      .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                      .gameDiceSides(data.getDiceSides())
                      .territoryEffects(territoryEffects)
                      .build())
              .calculateTotalPower();
      // defender is never amphibious
      final int defensePower =
          PowerStrengthAndRolls.build(
                  defenders,
                  CombatValueBuilder.mainCombatValue()
                      .enemyUnits(attackers)
                      .friendlyUnits(defenders)
                      .side(BattleState.Side.DEFENSE)
                      .gameSequence(data.getSequence())
                      .supportAttachments(data.getUnitTypeList().getSupportRules())
                      .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(data.getProperties()))
                      .gameDiceSides(data.getDiceSides())
                      .territoryEffects(territoryEffects)
                      .build())
              .calculateTotalPower();
      attackerUnitsTotalPower.setText(getTotalNumberText("Power: ", attackPower));
      defenderUnitsTotalPower.setText(getTotalNumberText("Power: ", defensePower));
    }
  }

  private static @Nonnull String getTotalNumberText(final String label, final int totalNumber) {
    return label + totalNumber;
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

  private static boolean doesPlayerHaveUnitsOnMap(final GamePlayer player, final GameState data) {
    return data.getMap().getTerritories().stream()
        .anyMatch(Matches.territoryHasUnitsOwnedBy(player));
  }

  static boolean hasMaxRounds(final boolean isLand, final GameData data) {
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      return isLand
          ? Properties.getLandBattleRounds(data.getProperties()) > 0
          : Properties.getSeaBattleRounds(data.getProperties()) > 0;
    }
  }
}
