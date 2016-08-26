package games.strategy.triplea.ui.battledisplay;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.IBattle;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.Tuple;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class BattleModel extends DefaultTableModel {
  private static final long serialVersionUID = 6913324191512043963L;
  private final IUIContext m_uiContext;
  private final GameData m_data;
  // is the player the aggressor?
  private final boolean m_attack;
  private final Collection<Unit> m_units;
  private final Territory m_location;
  private final IBattle.BattleType m_battleType;
  private final Collection<TerritoryEffect> m_territoryEffects;
  private final boolean m_isAmphibious;
  private final Collection<Unit> m_amphibiousLandAttackers;
  private BattleModel m_enemyBattleModel = null;

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

  BattleModel(final Collection<Unit> units, final boolean attack, final IBattle.BattleType battleType, final PlayerID player,
      final GameData data, final Territory battleLocation, final Collection<TerritoryEffect> territoryEffects,
      final boolean isAmphibious, final Collection<Unit> amphibiousLandAttackers, final IUIContext uiContext) {
    super(new Object[0][0], varDiceArray(data));
    m_uiContext = uiContext;
    m_data = data;
    m_attack = attack;
    // were going to modify the units
    m_units = new ArrayList<>(units);
    m_location = battleLocation;
    m_battleType = battleType;
    m_territoryEffects = territoryEffects;
    m_isAmphibious = isAmphibious;
    m_amphibiousLandAttackers = amphibiousLandAttackers;
  }

  public void setEnemyBattleModel(final BattleModel enemyBattleModel) {
    m_enemyBattleModel = enemyBattleModel;
  }

  public void notifyRetreat(final Collection<Unit> retreating) {
    m_units.removeAll(retreating);
    refresh();
  }

  public void removeCasualties(final Collection<Unit> killed) {
    m_units.removeAll(killed);
    refresh();
  }

  public void addUnits(final Collection<Unit> units) {
    m_units.addAll(units);
    refresh();
  }

  Collection<Unit> getUnits() {
    return m_units;
  }

  /**
   * refresh the model from m_units
   */
  public void refresh() {
    // TODO Soft set the maximum bonus to-hit plus 1 for 0 based count(+2 total currently)
    // Soft code the # of columns

    final List<List<TableData>> columns = new ArrayList<>(m_data.getDiceSides() + 1);
    for (int i = 0; i <= m_data.getDiceSides(); i++) {
      columns.add(i, new ArrayList<>());
    }
    final List<Unit> units = new ArrayList<>(m_units);
    DiceRoll.sortByStrength(units, !m_attack);
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap;
    m_data.acquireReadLock();
    try {
      if (m_battleType.isAirPreBattleOrPreRaid()) {
        unitPowerAndRollsMap = null;
      } else {
        unitPowerAndRollsMap = DiceRoll.getUnitPowerAndRollsForNormalBattles(units,
            new ArrayList<>(m_enemyBattleModel.getUnits()), !m_attack, false, m_data, m_location,
            m_territoryEffects, m_isAmphibious, m_amphibiousLandAttackers);
      }
    } finally {
      m_data.releaseReadLock();
    }
    final int diceSides = m_data.getDiceSides();
    final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(units, null, false, false, false);
    for (final UnitCategory category : unitCategories) {
      int strength;
      final UnitAttachment attachment = UnitAttachment.get(category.getType());
      final int[] shift = new int[m_data.getDiceSides() + 1];
      for (final Unit current : category.getUnits()) {
        if (m_battleType.isAirPreBattleOrPreRaid()) {
          if (m_attack) {
            strength = attachment.getAirAttack(category.getOwner());
          } else {
            strength = attachment.getAirDefense(category.getOwner());
          }
        } else {
          // normal battle
          strength = unitPowerAndRollsMap.get(current).getFirst();
        }
        strength = Math.min(Math.max(strength, 0), diceSides);
        shift[strength]++;
      }
      for (int i = 0; i <= m_data.getDiceSides(); i++) {
        if (shift[i] > 0) {
          columns.get(i).add(new TableData(category.getOwner(), shift[i], category.getType(), m_data,
              category.hasDamageOrBombingUnitDamage(), category.getDisabled(), m_uiContext));
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
}
