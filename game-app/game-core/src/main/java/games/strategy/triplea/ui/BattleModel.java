package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.delegate.power.calculator.TotalPowerAndTotalRolls;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;

final class BattleModel extends DefaultTableModel {
  private static final long serialVersionUID = 6913324191512043963L;
  private final UiContext uiContext;
  private final GameData gameData;
  // is the player the aggressor?
  private final BattleState.Side side;
  private final Collection<Unit> units;
  private final IBattle.BattleType battleType;
  private final Collection<TerritoryEffect> territoryEffects;
  private BattleModel enemyBattleModel = null;

  BattleModel(
      final Collection<Unit> units,
      final BattleState.Side side,
      final IBattle.BattleType battleType,
      final GameData data,
      final Collection<TerritoryEffect> territoryEffects,
      final UiContext uiContext) {
    super(new Object[0][0], varDiceArray(data));
    this.uiContext = uiContext;
    gameData = data;
    this.side = side;
    // were going to modify the units
    this.units = new ArrayList<>(units);
    this.battleType = battleType;
    this.territoryEffects = territoryEffects;
  }

  private static String[] varDiceArray(final GameData data) {
    // TODO Soft set the maximum bonus to-hit plus 1 for 0 based count(+2 total currently)
    final String[] diceColumns = new String[data.getDiceSides() + 1];
    {
      for (int i = 0; i < diceColumns.length; i++) {
        if (i == 0) {
          diceColumns[i] = " ";
        } else {
          diceColumns[i] = Integer.toString(i);
        }
      }
    }
    return diceColumns;
  }

  void setEnemyBattleModel(final BattleModel enemyBattleModel) {
    this.enemyBattleModel = enemyBattleModel;
  }

  void notifyRetreat(final Collection<Unit> retreating) {
    if (units.removeAll(retreating)) {
      refresh();
    }
  }

  void removeCasualties(final Collection<Unit> killed) {
    if (units.removeAll(killed)) {
      refresh();
    }
  }

  void addUnits(final Collection<Unit> units) {
    if (this.units.addAll(units)) {
      refresh();
    }
  }

  Collection<Unit> getUnits() {
    return units;
  }

  /** refresh the model from units. */
  void refresh() {
    // TODO Soft set the maximum bonus to-hit plus 1 for 0 based count(+2 total currently)
    // Soft code the # of columns

    final List<List<TableData>> columns = new ArrayList<>(gameData.getDiceSides() + 1);
    for (int i = 0; i <= gameData.getDiceSides(); i++) {
      columns.add(i, new ArrayList<>());
    }
    final List<Unit> units = new ArrayList<>(this.units);
    final TotalPowerAndTotalRolls unitPowerAndRollsMap;
    final boolean isAirPreBattleOrPreRaid = battleType.isAirBattle();
    final boolean lhtrHeavyBombers = Properties.getLhtrHeavyBombers(gameData.getProperties());
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      final CombatValue combatValue;
      if (isAirPreBattleOrPreRaid) {
        combatValue =
            CombatValueBuilder.airBattleCombatValue()
                .side(BattleState.Side.DEFENSE)
                .lhtrHeavyBombers(lhtrHeavyBombers)
                .gameDiceSides(gameData.getDiceSides())
                .build();
      } else {
        combatValue =
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(new ArrayList<>(enemyBattleModel.getUnits()))
                .friendlyUnits(units)
                .side(side)
                .gameSequence(gameData.getSequence())
                .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(lhtrHeavyBombers)
                .gameDiceSides(gameData.getDiceSides())
                .territoryEffects(territoryEffects)
                .build();
      }
      unitPowerAndRollsMap = PowerStrengthAndRolls.build(units, combatValue);
    }
    final List<UnitCategory> unitCategories =
        UnitSeparator.getSortedUnitCategories(units, gameData, uiContext.getMapData());
    for (final UnitCategory category : unitCategories) {
      final int[] shift = new int[gameData.getDiceSides() + 1];
      for (final Unit current : category.getUnits()) {
        final int strength = unitPowerAndRollsMap.getStrength(current);
        shift[strength]++;
      }
      for (int i = 0; i <= gameData.getDiceSides(); i++) {
        if (shift[i] > 0) {
          columns
              .get(i)
              .add(new TableData(UnitImageFactory.ImageKey.of(category), shift[i], uiContext));
        }
      }
      // TODO Kev determine if we need to identify if the unit is hit/disabled
    }
    // find the number of rows
    // this will be the size of the largest column
    int rowCount = 1;
    for (final List<TableData> column : columns) {
      rowCount = Math.max(rowCount, column.size());
    }
    setNumRows(rowCount);
    for (int row = 0; row < rowCount; row++) {
      for (int column = 0; column < columns.size(); column++) {
        // if the column has that many items, add to the table, else add null
        if (columns.get(column).size() > row) {
          setValueAt(columns.get(column).get(row), row, column);
        } else {
          setValueAt(TableData.NULL, row, column);
        }
      }
    }
  }

  @Override
  public boolean isCellEditable(final int row, final int column) {
    return false;
  }

  static final class TableData {
    static final TableData NULL = new TableData();
    private GamePlayer player;
    private UnitType unitType;
    private int count;
    private ImageIcon icon;

    private UiContext uiContext;

    private TableData() {}

    TableData(
        final UnitImageFactory.ImageKey imageKey, final int count, final UiContext uiContext) {
      this.player = imageKey.getPlayer();
      this.count = count;
      this.unitType = imageKey.getType();
      this.icon = uiContext.getUnitImageFactory().getIcon(imageKey);
      this.uiContext = uiContext;
    }

    void updateStamp(final JLabel stamp) {
      if (count == 0) {
        stamp.setText("");
        stamp.setIcon(null);
        stamp.setToolTipText(null);
      } else {
        stamp.setText("x" + count);
        stamp.setIcon(icon);
        MapUnitTooltipManager.setUnitTooltip(stamp, unitType, player, count, uiContext);
      }
    }
  }
}
